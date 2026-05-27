"""
FastAPI endpoint tests for main.py.

These tests bypass the lifespan (no real DB / MCP servers needed) by
monkey-patching `main.agent_app` / `main._reindex_job` directly and using
httpx.AsyncClient with the ASGI transport.
"""

import json
import time
from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest
import pytest_asyncio

# main.py lives in ../src — already on sys.path via conftest.py


# ── Helpers ────────────────────────────────────────────────────────────────────

@pytest_asyncio.fixture
async def client():
    """AsyncClient wired to the FastAPI app, lifespan skipped."""
    import main as m

    fake_agent = _make_fake_agent()
    fake_pool = MagicMock()
    with (
        patch.object(m, "agent_app", fake_agent),
        patch.object(m, "mcp_client", MagicMock()),
        patch.object(m, "get_asyncpg_pool", return_value=fake_pool),
    ):
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=m.app), base_url="http://test"
        ) as c:
            yield c


@pytest_asyncio.fixture
async def client_no_agent():
    """AsyncClient where agent_app is None (simulates startup not finished)."""
    import main as m

    with patch.object(m, "agent_app", None):
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=m.app), base_url="http://test"
        ) as c:
            yield c


def _make_fake_agent():
    """Return a mock LangGraph app that yields a predictable astream_events sequence."""
    agent = MagicMock()

    async def _fake_astream_events(input_or_cmd, config, version):
        yield {"event": "on_chat_model_stream", "data": {"chunk": _chunk("Hello")}, "name": "llm", "metadata": {"langgraph_node": "agent"}}
        yield {"event": "on_chat_model_stream", "data": {"chunk": _chunk(" world")}, "name": "llm", "metadata": {"langgraph_node": "agent"}}

    class _FakeState:
        tasks = []  # no interrupts

    # astream_events must be the actual async generator function, not an AsyncMock
    agent.astream_events = _fake_astream_events
    agent.aget_state = AsyncMock(return_value=_FakeState())
    agent.ainvoke = AsyncMock(return_value={"messages": [_ai_message("Answer.")]})
    return agent


class _FakeChunk:
    def __init__(self, text):
        self.content = text


def _chunk(text):
    return _FakeChunk(text)


class _AIMessage:
    def __init__(self, content):
        self.content = content
        self.tool_calls = []


def _ai_message(content):
    return _AIMessage(content)


# ── /health ────────────────────────────────────────────────────────────────────

class TestHealth:
    async def test_returns_ok(self, client):
        resp = await client.get("/health")
        assert resp.status_code == 200
        body = resp.json()
        assert body["status"] == "ok"
        assert body["agent_ready"] is True

    async def test_agent_not_ready(self, client_no_agent):
        resp = await client_no_agent.get("/health")
        assert resp.status_code == 200
        assert resp.json()["agent_ready"] is False


# ── /admin/reindex ─────────────────────────────────────────────────────────────

class TestAdminReindex:
    async def test_post_returns_202_running(self, client):
        import main as m
        from models import ReindexStatus

        # Reset to idle first
        with patch.object(m, "_reindex_job", ReindexStatus(status="idle")):
            # Prevent _run_reindex from actually running
            with patch.object(m, "_run_reindex", AsyncMock()):
                resp = await client.post("/admin/reindex")
        assert resp.status_code == 202
        body = resp.json()
        assert body["status"] == "running"
        assert body["started_at"] is not None

    async def test_get_returns_status_object(self, client):
        resp = await client.get("/admin/reindex")
        assert resp.status_code == 200
        body = resp.json()
        assert "status" in body
        assert body["status"] in ("idle", "running", "done", "error")

    async def test_concurrent_reindex_returns_409(self, client):
        import main as m
        from models import ReindexStatus

        running = ReindexStatus(
            status="running",
            started_at="2026-05-19T10:00:00+00:00",
        )
        with patch.object(m, "_reindex_job", running):
            resp = await client.post("/admin/reindex")
        assert resp.status_code == 409
        assert "progress" in resp.json()["detail"].lower()

    async def test_idle_status_before_first_run(self, client):
        import main as m
        from models import ReindexStatus

        with patch.object(m, "_reindex_job", ReindexStatus(status="idle")):
            resp = await client.get("/admin/reindex")
        assert resp.json()["status"] == "idle"


# ── /chat (blocking) ───────────────────────────────────────────────────────────

class TestChatBlocking:
    async def test_returns_answer(self, client):
        resp = await client.post("/chat", json={"message": "Hello", "thread_id": "t1"})
        assert resp.status_code == 200
        body = resp.json()
        assert body["answer"] == "Answer."
        assert isinstance(body["tool_calls_made"], list)

    async def test_503_when_agent_not_ready(self, client_no_agent):
        resp = await client_no_agent.post("/chat", json={"message": "Hello", "thread_id": "t1"})
        assert resp.status_code == 503


# ── /chat/stream (SSE) ────────────────────────────────────────────────────────

class TestChatStream:
    async def test_returns_event_stream_content_type(self, client):
        resp = await client.post(
            "/chat/stream",
            json={"message": "Hi", "thread_id": "t2"},
        )
        assert resp.status_code == 200
        assert "text/event-stream" in resp.headers["content-type"]

    async def test_token_events_in_stream(self, client):
        resp = await client.post(
            "/chat/stream",
            json={"message": "Hi", "thread_id": "t2"},
        )
        events = _parse_sse(resp.text)
        types = [e["type"] for e in events]
        assert "token" in types

    async def test_done_event_terminates_stream(self, client):
        resp = await client.post(
            "/chat/stream",
            json={"message": "Hi", "thread_id": "t2"},
        )
        events = _parse_sse(resp.text)
        assert events[-1]["type"] == "done"

    async def test_token_content_matches_mock(self, client):
        resp = await client.post(
            "/chat/stream",
            json={"message": "Hi", "thread_id": "t2"},
        )
        events = _parse_sse(resp.text)
        tokens = [e["content"] for e in events if e["type"] == "token"]
        assert "".join(tokens) == "Hello world"

    async def test_503_when_agent_not_ready(self, client_no_agent):
        resp = await client_no_agent.post(
            "/chat/stream",
            json={"message": "Hi", "thread_id": "t2"},
        )
        assert resp.status_code == 503


# ── /chat/confirm/{thread_id}/stream ─────────────────────────────────────────

class TestChatConfirm:
    async def test_approve_returns_event_stream(self, client):
        resp = await client.post(
            "/chat/confirm/thread-123/stream",
            json={"approved": True},
        )
        assert resp.status_code == 200
        assert "text/event-stream" in resp.headers["content-type"]

    async def test_reject_returns_event_stream(self, client):
        resp = await client.post(
            "/chat/confirm/thread-123/stream",
            json={"approved": False},
        )
        assert resp.status_code == 200
        events = _parse_sse(resp.text)
        assert events[-1]["type"] == "done"

    async def test_503_when_agent_not_ready(self, client_no_agent):
        resp = await client_no_agent.post(
            "/chat/confirm/thread-123/stream",
            json={"approved": True},
        )
        assert resp.status_code == 503


# ── /sessions ─────────────────────────────────────────────────────────────────

class TestSessions:
    async def test_delete_unknown_session_returns_404(self, client):
        import main as m

        with patch("main.delete_session", AsyncMock(return_value=False)):
            resp = await client.delete("/sessions/nonexistent-thread")
        assert resp.status_code == 404

    async def test_delete_known_session_returns_deleted_true(self, client):
        with patch("main.delete_session", AsyncMock(return_value=True)):
            resp = await client.delete("/sessions/existing-thread")
        assert resp.status_code == 200
        assert resp.json()["deleted"] is True
        assert resp.json()["thread_id"] == "existing-thread"

    async def test_get_unknown_session_returns_404(self, client):
        with patch("main.get_session_history", AsyncMock(return_value=None)):
            resp = await client.get("/sessions/no-such-thread")
        assert resp.status_code == 404

    async def test_list_sessions_returns_list(self, client):
        from models import SessionInfo

        fake_sessions = [
            SessionInfo(thread_id="t1", step_count=2, last_active=None),
            SessionInfo(thread_id="t2", step_count=5, last_active="2026-05-18T10:00:00"),
        ]
        with patch("main.list_sessions", AsyncMock(return_value=fake_sessions)):
            resp = await client.get("/sessions")
        assert resp.status_code == 200
        body = resp.json()
        assert body["total"] == 2
        assert len(body["sessions"]) == 2


# ── SSE parser ─────────────────────────────────────────────────────────────────

def _parse_sse(raw: str) -> list[dict]:
    events = []
    for line in raw.splitlines():
        if line.startswith("data: "):
            try:
                events.append(json.loads(line[6:]))
            except json.JSONDecodeError:
                pass
    return events


# ── AgentState ────────────────────────────────────────────────────────────────

class TestSummarizationNode:
    def test_agent_state_has_summary_annotation(self):
        from models import AgentState
        assert "summary" in AgentState.__annotations__
        assert AgentState.__annotations__["summary"] is str

    def test_prepare_messages_injects_summary(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages, SYSTEM_PROMPT

        msgs = [HumanMessage(content="What films are in stock?")]
        result = _prepare_messages(msgs, "User asked about action films earlier.")

        system_msgs = [m for m in result if isinstance(m, SystemMessage)]
        assert len(system_msgs) == 2
        contents = [m.content for m in system_msgs]
        assert any(SYSTEM_PROMPT == c for c in contents)
        assert any("User asked about action films earlier." in c for c in contents)
        assert result[0].content == SYSTEM_PROMPT
        assert "User asked about action films earlier." in result[1].content

    def test_prepare_messages_no_summary(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages, SYSTEM_PROMPT

        msgs = [HumanMessage(content="Hello")]
        result = _prepare_messages(msgs, "")

        system_msgs = [m for m in result if isinstance(m, SystemMessage)]
        assert len(system_msgs) == 1
        assert system_msgs[0].content == SYSTEM_PROMPT
        assert result[-1].content == "Hello"

    def test_prepare_messages_skips_if_system_present(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages

        msgs = [SystemMessage(content="Custom prompt"), HumanMessage(content="Hi")]
        result = _prepare_messages(msgs, "Some summary")

        assert result is msgs  # unchanged — no prepend

    @pytest.mark.asyncio
    async def test_summarize_history_node(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage, RemoveMessage
        import agent

        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock(
            return_value=MagicMock(content="Session summary text")
        )
        monkeypatch.setattr(agent, "model", mock_model)

        messages = [
            HumanMessage(content=f"msg {i}", id=str(i)) for i in range(12)
        ]
        state = {"messages": messages, "summary": ""}

        result = await agent.summarize_history(state)

        remove_ops = [m for m in result["messages"] if isinstance(m, RemoveMessage)]
        assert len(remove_ops) == 8  # 12 - KEEP_LAST_N(4)
        removed_ids = {m.id for m in remove_ops}
        assert removed_ids == {str(i) for i in range(8)}
        assert result["summary"] == "Session summary text"

    @pytest.mark.asyncio
    async def test_summarize_history_node_no_op_when_few_messages(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage
        import agent

        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock()
        monkeypatch.setattr(agent, "model", mock_model)

        messages = [HumanMessage(content=f"msg {i}", id=str(i)) for i in range(4)]
        state = {"messages": messages, "summary": ""}

        result = await agent.summarize_history(state)

        assert result == {}
        mock_model.ainvoke.assert_not_called()


# ── Validation node ────────────────────────────────────────────────────────────

class TestValidationNode:
    def test_after_validate_routes_to_agent(self):
        from langchain_core.messages import HumanMessage
        from langgraph.graph import END
        from agent import _after_validate

        state = {"messages": [HumanMessage(content="Do you have Titanic?")], "summary": ""}
        assert _after_validate(state) == "agent"

    def test_after_validate_routes_to_end(self):
        from langchain_core.messages import AIMessage
        from langgraph.graph import END
        from agent import _after_validate

        state = {
            "messages": [AIMessage(content="I'm a DVD rental assistant.")],
            "summary": "",
        }
        assert _after_validate(state) is END

    def test_after_validate_empty_messages_routes_to_agent(self):
        from langgraph.graph import END
        from agent import _after_validate

        state = {"messages": [], "summary": ""}
        assert _after_validate(state) == "agent"

    @pytest.mark.asyncio
    async def test_validate_input_on_topic(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage, SystemMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=True))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="Do you have Titanic?")], "summary": ""}
        result = await agent.validate_input(state)

        assert result == {}
        mock_classifier.ainvoke.assert_called_once()
        call_args = mock_classifier.ainvoke.call_args[0][0]
        assert any(isinstance(m, SystemMessage) and "topic classifier" in m.content for m in call_args)
        assert any(isinstance(m, HumanMessage) and "Titanic" in m.content for m in call_args)

    @pytest.mark.asyncio
    async def test_validate_input_off_topic(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=False))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="What's the weather like?")], "summary": ""}
        result = await agent.validate_input(state)

        assert "messages" in result
        assert len(result["messages"]) == 1
        assert isinstance(result["messages"][0], AIMessage)
        assert "DVD rental assistant" in result["messages"][0].content
        mock_classifier.ainvoke.assert_called_once()
        call_args = mock_classifier.ainvoke.call_args[0][0]
        assert any(isinstance(m, SystemMessage) and "topic classifier" in m.content for m in call_args)
        assert any(isinstance(m, HumanMessage) and "weather" in m.content for m in call_args)
