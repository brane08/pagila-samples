"""
FastAPI endpoint tests for main.py.

These tests bypass the lifespan (no real DB / MCP servers needed) by
monkey-patching `main.agent_app` / `main._reindex_job` directly and using
httpx.AsyncClient with the ASGI transport.
"""

import json
import time
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock, patch

from langchain_core.messages import HumanMessage, RemoveMessage, SystemMessage

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

    @pytest.mark.parametrize("payload", [
        {"message": "", "thread_id": "t1"},
        {"message": "x" * 4001, "thread_id": "t1"},
        {"message": "hi", "thread_id": "../etc"},
        {"message": "hi", "thread_id": "t1", "user_id": "a b"},
    ])
    async def test_rejects_invalid_request(self, client, payload):
        resp = await client.post("/chat", json=payload)
        assert resp.status_code == 422

    async def test_rejects_invalid_path_thread_id(self, client):
        resp = await client.get("/sessions/bad%20id")
        assert resp.status_code == 422


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

class _PendingState:
    tasks = [SimpleNamespace(interrupts=[SimpleNamespace(value={"tool_calls": []})])]


class _IdleState:
    tasks = []


def _pending_then_idle(client_agent):
    """The endpoint guard and the under-lock re-check see a pending interrupt; later calls (post-stream) see none."""
    client_agent.aget_state = AsyncMock(side_effect=[_PendingState(), _PendingState(), _IdleState(), _IdleState()])


class TestChatConfirm:
    async def test_approve_returns_event_stream(self, client):
        import main as m

        _pending_then_idle(m.agent_app)
        resp = await client.post(
            "/chat/confirm/thread-123/stream",
            json={"approved": True},
        )
        assert resp.status_code == 200
        assert "text/event-stream" in resp.headers["content-type"]

    async def test_reject_returns_event_stream(self, client):
        import main as m

        _pending_then_idle(m.agent_app)
        resp = await client.post(
            "/chat/confirm/thread-123/stream",
            json={"approved": False},
        )
        assert resp.status_code == 200
        events = _parse_sse(resp.text)
        assert events[-1]["type"] == "done"

    async def test_409_when_no_interrupt_pending(self, client):
        resp = await client.post(
            "/chat/confirm/thread-123/stream",
            json={"approved": True},
        )
        assert resp.status_code == 409

    async def test_chat_stream_409_when_interrupt_pending(self, client):
        import main as m

        m.agent_app.aget_state = AsyncMock(return_value=_PendingState())
        resp = await client.post("/chat/stream", json={"message": "hi", "thread_id": "t1"})
        assert resp.status_code == 409

    async def test_chat_409_when_interrupt_pending(self, client):
        import main as m

        m.agent_app.aget_state = AsyncMock(return_value=_PendingState())
        resp = await client.post("/chat", json={"message": "hi", "thread_id": "t1"})
        assert resp.status_code == 409

    async def test_chat_409_when_run_ends_in_interrupt(self, client):
        import main as m

        m.agent_app.ainvoke = AsyncMock(return_value={"messages": [], "__interrupt__": [object()]})
        resp = await client.post("/chat", json={"message": "hi", "thread_id": "t1"})
        assert resp.status_code == 409

    async def test_409_body_carries_pending_tool_calls(self, client):
        import main as m

        calls = [{"name": "search_films", "args": {"title": "x"}, "id": "1"}]
        state = SimpleNamespace(tasks=[SimpleNamespace(interrupts=[SimpleNamespace(value={"tool_calls": calls})])])
        m.agent_app.aget_state = AsyncMock(return_value=state)
        resp = await client.post("/chat/stream", json={"message": "hi", "thread_id": "t1"})
        assert resp.status_code == 409
        assert resp.json()["detail"]["tool_calls"] == calls

    async def test_pending_endpoint_reports_calls(self, client):
        import main as m

        calls = [{"name": "t", "args": {}, "id": "1"}]
        state = SimpleNamespace(tasks=[SimpleNamespace(interrupts=[SimpleNamespace(value={"tool_calls": calls})])])
        m.agent_app.aget_state = AsyncMock(return_value=state)
        body = (await client.get("/chat/pending/t1")).json()
        assert body["pending"] is True and body["tool_calls"] == calls and body["confirm_id"] == m._confirm_id(calls)
        m.agent_app.aget_state = AsyncMock(return_value=_IdleState())
        assert (await client.get("/chat/pending/t1")).json() == {"pending": False, "tool_calls": [], "confirm_id": None}

    async def test_confirm_id_depends_on_calls(self):
        import main as m

        a = [{"name": "t", "args": {"x": 1}, "id": "1"}]
        b = [{"name": "t", "args": {"x": 2}, "id": "1"}]
        assert m._confirm_id(a) == m._confirm_id(list(a))
        assert m._confirm_id(a) != m._confirm_id(b)

    async def test_stale_confirm_id_is_rejected(self, client):
        import main as m

        calls = [{"name": "t", "args": {}, "id": "1"}]
        state = SimpleNamespace(tasks=[SimpleNamespace(interrupts=[SimpleNamespace(value={"tool_calls": calls})])])
        m.agent_app.aget_state = AsyncMock(return_value=state)
        m.agent_app.astream_events = MagicMock()
        resp = await client.post("/chat/confirm/t1/stream", json={"approved": True, "confirm_id": "not-the-card"})
        assert resp.status_code == 409
        assert resp.json()["detail"]["confirm_id"] == m._confirm_id(calls)
        m.agent_app.astream_events.assert_not_called()

    async def test_matching_confirm_id_is_accepted(self, client):
        import main as m

        calls = [{"name": "t", "args": {}, "id": "1"}]
        pending = SimpleNamespace(tasks=[SimpleNamespace(interrupts=[SimpleNamespace(value={"tool_calls": calls})])])
        m.agent_app.aget_state = AsyncMock(side_effect=[pending, pending, _IdleState(), _IdleState()])
        resp = await client.post(
            "/chat/confirm/t1/stream", json={"approved": True, "confirm_id": m._confirm_id(calls)}
        )
        assert resp.status_code == 200
        assert _parse_sse(resp.text)[-1]["type"] == "done"

    async def test_confirm_rechecked_under_lock(self, client):
        """A second approve that passed the endpoint guard but finds nothing pending gets an SSE error, no run."""
        import main as m

        m.agent_app.aget_state = AsyncMock(side_effect=[_PendingState(), _IdleState()])
        m.agent_app.astream_events = MagicMock()
        resp = await client.post("/chat/confirm/t1/stream", json={"approved": True})
        events = _parse_sse(resp.text)
        assert events[-1]["type"] == "error" and events[-1]["code"] == "stale_confirm"
        m.agent_app.astream_events.assert_not_called()

    async def test_blocking_chat_rejects_repeated_interrupts(self, client):
        import main as m

        interrupted = {"messages": [], "__interrupt__": [object()]}
        m.agent_app.ainvoke = AsyncMock(side_effect=[interrupted, interrupted, {"messages": [], "answer": 1}])
        m.agent_app.aget_state = AsyncMock(return_value=_IdleState())
        resp = await client.post("/chat", json={"message": "hi", "thread_id": "t1"})
        assert resp.status_code == 409
        assert m.agent_app.ainvoke.await_count == 3

    async def test_run_survives_client_disconnect(self, client):
        """Closing the SSE generator mid-run must not cancel the graph: it finishes and releases the lock."""
        import asyncio
        import main as m

        gate = asyncio.Event()
        finished = asyncio.Event()

        async def _events(*a, **k):
            yield {"event": "on_tool_start", "name": "t", "data": {"input": {}}}
            await gate.wait()
            yield {"event": "on_tool_end", "name": "t", "data": {}}
            finished.set()

        m.agent_app.astream_events = _events
        m.agent_app.aget_state = AsyncMock(return_value=_IdleState())
        m._thread_locks.clear()
        gen = m._stream_agent_events({"messages": []}, m._config("disc1"))
        first = await gen.__anext__()
        assert "tool_start" in first
        await gen.aclose()  # client went away
        assert m._thread_lock("disc1").locked()
        gate.set()
        await asyncio.wait_for(finished.wait(), 2)
        await asyncio.wait_for(asyncio.gather(*m._run_tasks), 2)
        assert "disc1" not in m._thread_locks

    async def test_pending_calls_aggregate_across_interrupts_and_resume_is_keyed(self):
        import main as m

        a = SimpleNamespace(id="i1", value={"tool_calls": [{"name": "a", "args": {}, "id": "1"}]})
        b = SimpleNamespace(id="i2", value={"tool_calls": [{"name": "b", "args": {}, "id": "2"}]})
        assert [c["name"] for c in m._calls_of([a, b])] == ["a", "b"]
        assert m._resume_command([a, b], True).resume == {"i1": {"approved": True}, "i2": {"approved": True}}
        assert m._resume_command([a], False).resume == {"approved": False}

    def test_multi_worker_start_is_refused(self, monkeypatch):
        import main as m

        monkeypatch.setenv("WEB_CONCURRENCY", "2")
        with pytest.raises(RuntimeError):
            m._require_single_worker()
        monkeypatch.setenv("WEB_CONCURRENCY", "1")
        m._require_single_worker()

    async def test_lock_entries_are_evicted(self, client):
        import main as m

        m._thread_locks.clear()
        m.agent_app.ainvoke = AsyncMock(return_value={"messages": [SimpleNamespace(content="ok", tool_calls=[])]})
        m.agent_app.aget_state = AsyncMock(return_value=_IdleState())
        assert (await client.post("/chat", json={"message": "hi", "thread_id": "evict1"})).status_code == 200
        assert "evict1" not in m._thread_locks

    async def test_409_when_thread_busy(self, client):
        import main as m

        lock = m._thread_lock("busy-1")
        await lock.acquire()
        try:
            for path, body in (("/chat/stream", {"message": "hi", "thread_id": "busy-1"}),
                               ("/chat", {"message": "hi", "thread_id": "busy-1"})):
                resp = await client.post(path, json=body)
                assert resp.status_code == 409
        finally:
            lock.release()

    async def test_blocking_chat_rejects_pending_tool_call_it_cannot_confirm(self, client):
        import main as m

        m.agent_app.ainvoke = AsyncMock(return_value={"messages": [], "__interrupt__": [object()]})
        resp = await client.post("/chat", json={"message": "hi", "thread_id": "t1"})
        assert resp.status_code == 409
        resume = m.agent_app.ainvoke.call_args_list[-1].args[0]
        assert resume.resume == {"approved": False}

    async def test_stream_resets_between_agent_runs_and_sends_final_warning(self, client):
        import main as m

        def chunk(text):
            return {"event": "on_chat_model_stream", "metadata": {"langgraph_node": "agent"},
                    "data": {"chunk": SimpleNamespace(content=text)}}

        start = {"event": "on_chat_model_start", "metadata": {"langgraph_node": "agent"}}

        async def _events(*a, **k):
            for e in (start, chunk("draft"), start, chunk("final")):
                yield e

        m.agent_app.astream_events = _events
        m.agent_app.aget_state = AsyncMock(
            side_effect=[_IdleState(), _IdleState(), SimpleNamespace(tasks=[], values={"messages": []})]
        )
        resp = await client.post("/chat/stream", json={"message": "hi", "thread_id": "rs1"})
        types = [e["type"] for e in _parse_sse(resp.text)]
        assert types == ["token", "reset", "token", "done"]

    async def test_stream_error_message_is_generic(self, client):
        import main as m

        async def _boom(*a, **k):
            raise RuntimeError("password=secret host=db.internal")
            yield  # pragma: no cover

        m.agent_app.astream_events = _boom
        resp = await client.post("/chat/stream", json={"message": "hi", "thread_id": "t1"})
        events = _parse_sse(resp.text)
        assert events[-1]["type"] == "error"
        assert "secret" not in resp.text

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

    async def test_delete_refused_while_run_in_flight(self, client):
        import asyncio
        import main as m

        lock = asyncio.Lock()
        await lock.acquire()
        m._thread_locks["busy2"] = lock
        try:
            with patch("main.delete_session", AsyncMock(return_value=True)) as deleter:
                resp = await client.delete("/sessions/busy2")
            assert resp.status_code == 409
            deleter.assert_not_called()
        finally:
            m._thread_locks.pop("busy2", None)

    async def test_busy_probe_does_not_create_lock_entries(self, client):
        import main as m

        m._thread_locks.clear()
        with patch("main.delete_session", AsyncMock(return_value=False)):
            await client.delete("/sessions/probe1")
        assert "probe1" not in m._thread_locks

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

    def test_prepare_messages_skips_if_system_prompt_already_present(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages, SYSTEM_PROMPT

        msgs = [SystemMessage(content=SYSTEM_PROMPT), HumanMessage(content="Hi")]
        result = _prepare_messages(msgs, "Some summary")

        assert result is msgs  # already has SYSTEM_PROMPT — no re-prepend

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

        assert result == {"tool_retry_count": 0, "reflection_retry_count": 0}
        mock_classifier.ainvoke.assert_called_once()
        call_args = mock_classifier.ainvoke.call_args[0][0]
        assert any(isinstance(m, SystemMessage) and "topic classifier" in m.content for m in call_args)
        assert any(isinstance(m, HumanMessage) and "Titanic" in m.content for m in call_args)

    @pytest.mark.asyncio
    async def test_validate_input_short_followup_skips_classifier(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=False))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {
            "messages": [
                HumanMessage(content="Show my rentals"),
                AIMessage(content="What is your email?"),
                HumanMessage(content="jane@example.com"),
            ],
            "summary": "",
        }
        assert "messages" not in await agent.validate_input(state)
        mock_classifier.ainvoke.assert_not_called()

    @pytest.mark.asyncio
    async def test_validate_input_short_message_after_refusal_is_still_checked(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=False))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {
            "messages": [
                HumanMessage(content="weather?"),
                AIMessage(content=agent.REFUSAL_MESSAGE),
                HumanMessage(content="and tomorrow?"),
            ],
            "summary": "",
        }
        result = await agent.validate_input(state)
        mock_classifier.ainvoke.assert_called_once()
        assert result["messages"][0].content == agent.REFUSAL_MESSAGE

    @pytest.mark.asyncio
    async def test_validate_input_resets_retry_counters_each_turn(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=True))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {
            "messages": [HumanMessage(content="Do you have Titanic?")],
            "summary": "", "tool_retry_count": 1, "reflection_retry_count": 1,
        }
        result = await agent.validate_input(state)
        assert result["tool_retry_count"] == 0
        assert result["reflection_retry_count"] == 0

    @pytest.mark.asyncio
    async def test_validate_input_classifier_failure_fails_open(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(side_effect=RuntimeError("llm down"))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="Do you have any films starring Tom Hanks?")], "summary": ""}
        assert "messages" not in await agent.validate_input(state)

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

    @pytest.mark.asyncio
    async def test_validation_prompt_references_sakila(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage, SystemMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=True))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="show me action films")], "summary": ""}
        await agent.validate_input(state)

        call_args = mock_classifier.ainvoke.call_args[0][0]
        sys_msg = next(m for m in call_args if isinstance(m, SystemMessage))
        assert "Sakila" in sys_msg.content
        assert "off-topic" in sys_msg.content.lower() or "not" in sys_msg.content.lower()

    @pytest.mark.asyncio
    async def test_validate_rejects_sql_syntax_question(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=False))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="How do I write a SQL JOIN?")], "summary": ""}
        result = await agent.validate_input(state)

        assert "messages" in result
        assert isinstance(result["messages"][0], AIMessage)
        assert "Sakila" in result["messages"][0].content


# ── AgentState fields ──────────────────────────────────────────────────────────

class TestAgentStateFields:
    def test_agent_state_has_tool_retry_count(self):
        from models import AgentState
        state: AgentState = {
            "messages": [],
            "summary": "",
            "tool_retry_count": 0,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anonymous",
        }
        assert state["tool_retry_count"] == 0
        assert state["preferred_store_id"] is None
        assert state["customer_email"] is None
        assert state["user_id"] == "anonymous"

    def test_chat_request_has_user_id(self):
        from models import ChatRequest
        req = ChatRequest(message="hello", thread_id="t1", user_id="user-abc")
        assert req.user_id == "user-abc"

    def test_chat_request_user_id_defaults_to_anonymous(self):
        from models import ChatRequest
        req = ChatRequest(message="hello")
        assert req.user_id == "anonymous"


# ── Clarification node ─────────────────────────────────────────────────────────

class TestClarificationNode:
    def test_after_clarify_routes_to_human_review_when_tool_calls_remain(self):
        from langchain_core.messages import AIMessage
        import agent

        ai_msg = AIMessage(content="")
        ai_msg.tool_calls = [{"name": "get_customer_current_rentals", "args": {"email": None}, "id": "tc1"}]
        state = {"messages": [ai_msg], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "anon"}
        assert agent._after_clarify(state) == "human_review"

    def test_after_clarify_routes_to_end_when_question_added(self):
        from langchain_core.messages import AIMessage
        from langgraph.graph import END
        import agent

        ai_msg = AIMessage(content="What is the customer's email address?")
        state = {"messages": [ai_msg], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "anon"}
        assert agent._after_clarify(state) is END

    @pytest.mark.asyncio
    async def test_clarify_passes_through_when_no_tool_calls(self, monkeypatch):
        from langchain_core.messages import AIMessage
        import agent

        ai_msg = AIMessage(content="Here are the films.")
        state = {"messages": [ai_msg], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "anon"}
        result = await agent.clarify_tool_args(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_clarify_passes_through_when_args_complete(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ClarificationCheck(needs_clarification=False, question=None)
        )
        monkeypatch.setattr(agent, "clarification_classifier", mock_clf)

        ai_msg = AIMessage(content="")
        ai_msg.tool_calls = [{"name": "get_customer_current_rentals", "args": {"email": "jane@example.com"}, "id": "tc1"}]
        state = {"messages": [ai_msg], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "anon"}
        result = await agent.clarify_tool_args(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_clarify_replaces_tool_call_with_question(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ClarificationCheck(
                needs_clarification=True,
                question="What is the customer's email address?"
            )
        )
        monkeypatch.setattr(agent, "clarification_classifier", mock_clf)

        ai_msg = AIMessage(content="")
        ai_msg.id = "msg-1"
        ai_msg.tool_calls = [{"name": "get_customer_current_rentals", "args": {"email": None}, "id": "tc1"}]
        state = {"messages": [ai_msg], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "anon"}
        result = await agent.clarify_tool_args(state)

        assert "messages" in result
        assert len(result["messages"]) == 2
        assert isinstance(result["messages"][0], RemoveMessage)
        assert result["messages"][0].id == "msg-1"
        assert isinstance(result["messages"][1], AIMessage)
        assert "email" in result["messages"][1].content.lower()


# ── Tool error recovery ────────────────────────────────────────────────────────

class TestToolErrorRecovery:
    @pytest.mark.asyncio
    async def test_no_error_resets_retry_count_routes_to_agent(self):
        from langchain_core.messages import ToolMessage
        import agent

        tm = ToolMessage(content='{"film_id": 1, "title": "Jaws"}', tool_call_id="tc1", name="search_films")
        state = {
            "messages": [tm],
            "summary": "",
            "tool_retry_count": 1,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anon",
        }
        result = await agent.handle_tool_errors(state)
        assert result.get("tool_retry_count") == 0

    @pytest.mark.asyncio
    async def test_first_error_replaces_message_and_increments_retry(self):
        from langchain_core.messages import ToolMessage
        import agent

        tm = ToolMessage(content='{"error": "connection timeout"}', tool_call_id="tc1", name="search_films")
        tm.id = "tm-1"
        state = {
            "messages": [tm],
            "summary": "",
            "tool_retry_count": 0,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anon",
        }
        result = await agent.handle_tool_errors(state)
        assert result["tool_retry_count"] == 1
        msgs = result["messages"]
        assert not any(isinstance(m, RemoveMessage) for m in msgs)
        hint_msgs = [m for m in msgs if isinstance(m, ToolMessage)]
        assert hint_msgs and hint_msgs[0].id == "tm-1"
        assert "alternative" in hint_msgs[0].content.lower()
        assert "connection timeout" in hint_msgs[0].content

    @pytest.mark.asyncio
    async def test_stale_errors_from_earlier_turns_ignored(self):
        from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
        import agent

        old = ToolMessage(content='{"error": "old"}', tool_call_id="tc0", name="search_films", id="old-1")
        ok = ToolMessage(content='{"title": "Jaws"}', tool_call_id="tc1", name="search_films", id="new-1")
        state = {
            "messages": [
                HumanMessage(content="first"), AIMessage(content=""), old,
                HumanMessage(content="second"), AIMessage(content=""), ok,
            ],
            "summary": "", "tool_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.handle_tool_errors(state)
        assert result == {"tool_retry_count": 0}

    @pytest.mark.asyncio
    async def test_second_error_resets_retry_exposes_error(self):
        from langchain_core.messages import ToolMessage
        import agent

        tm = ToolMessage(content='{"error": "table not found"}', tool_call_id="tc1", name="search_films")
        state = {
            "messages": [tm],
            "summary": "",
            "tool_retry_count": 1,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anon",
        }
        result = await agent.handle_tool_errors(state)
        assert result["tool_retry_count"] == 0
        assert [m.content for m in result["messages"]] == [agent.GIVE_UP_MESSAGE]
        state["messages"] = [tm, *result["messages"]]
        assert agent._after_tool_errors(state) == agent.END

    @pytest.mark.asyncio
    async def test_content_block_and_status_errors_are_detected(self):
        from langchain_core.messages import ToolMessage
        import agent

        blocks = ToolMessage(content=[{"type": "text", "text": '{"error": "boom"}'}], tool_call_id="a", name="t")
        flagged = ToolMessage(content="Error: invoking tool failed", tool_call_id="b", name="t", status="error")
        fine = ToolMessage(content=[{"type": "text", "text": '{"title": "Jaws"}'}], tool_call_id="c", name="t")
        assert agent._tool_error(blocks) == "boom"
        assert agent._tool_error(flagged) == "Error: invoking tool failed"
        assert agent._tool_error(fine) is None

    @pytest.mark.asyncio
    async def test_short_message_after_answer_is_still_classified(self, monkeypatch):
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=False))
        monkeypatch.setattr(agent, "classifier", mock_classifier)
        state = {
            "messages": [HumanMessage(content="top films"), AIMessage(content="Here they are."), HumanMessage(content="tell a joke")],
            "summary": "",
        }
        result = await agent.validate_input(state)
        mock_classifier.ainvoke.assert_called_once()
        assert result["messages"]

    @pytest.mark.asyncio
    async def test_non_json_tool_content_treated_as_no_error(self):
        from langchain_core.messages import ToolMessage
        import agent

        tm = ToolMessage(content="plain text result", tool_call_id="tc1", name="list_categories")
        state = {
            "messages": [tm],
            "summary": "",
            "tool_retry_count": 0,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anon",
        }
        result = await agent.handle_tool_errors(state)
        assert result.get("tool_retry_count") == 0


# ── User preferences nodes ─────────────────────────────────────────────────────

class TestClarifyAllToolCalls:
    @pytest.mark.asyncio
    async def test_second_tool_call_is_checked(self, monkeypatch):
        from langchain_core.messages import AIMessage
        import agent

        answers = [agent.ClarificationCheck(needs_clarification=False),
                   agent.ClarificationCheck(needs_clarification=True, question="Which email?")]
        clf = MagicMock()
        clf.ainvoke = AsyncMock(side_effect=answers)
        monkeypatch.setattr(agent, "clarification_classifier", clf)
        ai = AIMessage(content="", id="ai-1", tool_calls=[
            {"name": "a", "args": {}, "id": "1"}, {"name": "b", "args": {"email": None}, "id": "2"}])

        result = await agent.clarify_tool_args({"messages": [ai]})

        assert clf.ainvoke.call_count == 2
        assert result["messages"][1].content == "Which email?"


class TestPrefsHardening:
    @pytest.mark.asyncio
    async def test_save_prefs_ignores_invalid_email_and_old_turns(self):
        from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
        import agent

        pool = MagicMock()
        conn = AsyncMock()
        pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=conn), __aexit__=AsyncMock(return_value=None),
        ))
        old = ToolMessage(content='{"store_id": 9}', tool_call_id="a", name="t")
        bad = ToolMessage(content='{"email": "not an email; DROP"}', tool_call_id="b", name="t")
        state = {
            "messages": [HumanMessage(content="q1"), AIMessage(content=""), old,
                         HumanMessage(content="q2"), AIMessage(content=""), bad],
            "summary": "", "tool_retry_count": 0, "preferred_store_id": None,
            "customer_email": None, "user_id": "user-1",
        }
        assert await agent.save_prefs(state, pool) == {}
        conn.execute.assert_not_called()

    @pytest.mark.asyncio
    async def test_summary_prompt_truncates_tool_output(self, monkeypatch):
        from langchain_core.messages import HumanMessage, ToolMessage
        import agent

        captured = {}

        async def fake_invoke(msgs):
            captured["prompt"] = msgs[0].content
            return MagicMock(content="sum")

        monkeypatch.setattr(agent, "model", MagicMock(ainvoke=fake_invoke))
        msgs = [HumanMessage(content="hi", id="h0"),
                ToolMessage(content="X" * 5000, tool_call_id="a", name="t", id="t0")]
        msgs += [HumanMessage(content=f"m{i}", id=f"h{i+1}") for i in range(agent.KEEP_LAST_N)]
        await agent.summarize_history({"messages": msgs, "summary": ""})
        assert captured["prompt"].count("X") <= agent.TOOL_SUMMARY_CHARS


class TestUserPreferencesNodes:
    @pytest.mark.asyncio
    async def test_load_prefs_hydrates_state(self):
        import agent

        mock_pool = MagicMock()
        mock_conn = AsyncMock()
        mock_conn.fetchrow = AsyncMock(return_value={
            "preferred_store_id": 2,
            "customer_email": "jane@example.com",
        })
        mock_pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=mock_conn),
            __aexit__=AsyncMock(return_value=None),
        ))

        state = {"messages": [], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "user-1"}
        result = await agent.load_prefs(state, mock_pool)
        assert result["preferred_store_id"] == 2
        assert result["customer_email"] == "jane@example.com"

    @pytest.mark.asyncio
    async def test_load_prefs_noop_for_unknown_user(self):
        import agent

        mock_pool = MagicMock()
        mock_conn = AsyncMock()
        mock_conn.fetchrow = AsyncMock(return_value=None)
        mock_pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=mock_conn),
            __aexit__=AsyncMock(return_value=None),
        ))

        state = {"messages": [], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "unknown"}
        result = await agent.load_prefs(state, mock_pool)
        assert result == {}

    @pytest.mark.asyncio
    async def test_save_prefs_upserts_new_store_id(self):
        import agent
        from langchain_core.messages import ToolMessage

        mock_pool = MagicMock()
        mock_conn = AsyncMock()
        mock_conn.execute = AsyncMock()
        mock_pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=mock_conn),
            __aexit__=AsyncMock(return_value=None),
        ))

        from langchain_core.messages import HumanMessage

        tm = ToolMessage(content='{"store_id": 7, "email": "other@example.com"}', tool_call_id="tc1", name="list_stores")
        state = {"messages": [HumanMessage(content="Hi, my store is 1 and my email is jane@example.com."), tm],
                 "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "user-1"}
        result = await agent.save_prefs(state, mock_pool)
        mock_conn.execute.assert_called_once()
        assert result == {"preferred_store_id": 1, "customer_email": "jane@example.com"}

    @pytest.mark.asyncio
    async def test_save_prefs_ignores_tool_output_without_user_statement(self):
        import agent
        from langchain_core.messages import HumanMessage, ToolMessage

        mock_pool = MagicMock()
        tm = ToolMessage(content='{"store_id": 1, "email": "x@example.com"}', tool_call_id="tc1", name="t")
        state = {"messages": [HumanMessage(content="list customers at store 1"), tm],
                 "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "user-1"}
        assert await agent.save_prefs(state, mock_pool) == {}
        mock_pool.acquire.assert_not_called()

    @pytest.mark.asyncio
    async def test_save_prefs_skips_when_store_id_unchanged(self):
        import agent
        from langchain_core.messages import ToolMessage

        mock_pool = MagicMock()
        mock_conn = AsyncMock()
        mock_conn.execute = AsyncMock()
        mock_pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=mock_conn),
            __aexit__=AsyncMock(return_value=None),
        ))

        from langchain_core.messages import HumanMessage

        tm = ToolMessage(content='{"store_id": 1}', tool_call_id="tc1", name="list_stores")
        state = {"messages": [HumanMessage(content="my store is 1"), tm], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": 1, "customer_email": None, "user_id": "user-1"}
        await agent.save_prefs(state, mock_pool)
        mock_conn.execute.assert_not_called()

    def test_prepare_messages_injects_preference_context(self):
        import agent

        messages = [HumanMessage(content="show me films")]
        result = agent._prepare_messages(messages, "", preferred_store_id=2, customer_email="jane@example.com")
        sys_msgs = [m for m in result if isinstance(m, SystemMessage)]
        combined = " ".join(m.content for m in sys_msgs)
        assert "store" in combined.lower()
        assert "jane@example.com" in combined


# ── Reflection node ────────────────────────────────────────────────────────────

class TestReflectionNode:
    def test_agent_state_has_reflection_retry_count(self):
        from models import AgentState
        assert "reflection_retry_count" in AgentState.__annotations__
        assert AgentState.__annotations__["reflection_retry_count"] is int
        state: AgentState = {
            "messages": [],
            "summary": "",
            "tool_retry_count": 0,
            "reflection_retry_count": 0,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anonymous",
        }
        assert state["reflection_retry_count"] == 0

    @pytest.mark.asyncio
    async def test_reflect_complete_passes_through(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ReflectionCheck(complete=True, critique=None)
        )
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [
                HumanMessage(content="What films do you have?"),
                AIMessage(content="We have 1000 films.", id="ai-1"),
            ],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_reflect_incomplete_retries_once(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ReflectionCheck(complete=False, critique="List specific film titles.")
        )
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [
                HumanMessage(content="What action films do you have?"),
                AIMessage(content="We have some action films.", id="ai-1"),
            ],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result["reflection_retry_count"] == 1
        assert result["reflection_critique"] == "List specific film titles."
        assert len(result["messages"]) == 1
        assert isinstance(result["messages"][0], RemoveMessage)
        assert result["messages"][0].id == "ai-1"
        assert agent._after_reflect({"messages": [], "reflection_critique": "x"}) == "agent"
        assert agent._after_reflect({"messages": [], "reflection_critique": None}) == "ground"

    @pytest.mark.asyncio
    async def test_reflect_no_second_retry(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ReflectionCheck(complete=False, critique="Still vague.")
        )
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [
                HumanMessage(content="What films?"),
                AIMessage(content="Some films.", id="ai-1"),
            ],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 1,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result == {"reflection_retry_count": 0}

    @pytest.mark.asyncio
    async def test_reflect_noop_when_no_ai_message(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock()
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [HumanMessage(content="Tell me films")],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result == {}
        mock_clf.ainvoke.assert_not_called()


# ── Grounding node ────────────────────────────────────────────────────────────

class TestGroundingNode:
    @pytest.mark.asyncio
    async def test_ground_clean_answer_no_warning(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.GroundingCheck(hallucinated=False, warning=None)
        )
        monkeypatch.setattr(agent, "grounding_classifier", mock_clf)

        tm = ToolMessage(
            content='[{"title": "Jaws", "rating": "PG"}]',
            tool_call_id="tc1", name="search_films",
        )
        ai = AIMessage(content="We have Jaws rated PG.", id="ai-1")
        state = {
            "messages": [HumanMessage(content="find Jaws"), tm, ai],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.ground_answer(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_ground_hallucinated_appends_warning(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, RemoveMessage, ToolMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.GroundingCheck(
                hallucinated=True,
                warning="Film 'Sharknado' does not appear in tool results.",
            )
        )
        monkeypatch.setattr(agent, "grounding_classifier", mock_clf)

        tm = ToolMessage(
            content='[{"title": "Jaws"}]',
            tool_call_id="tc1", name="search_films",
        )
        ai = AIMessage(content="We have Jaws and Sharknado.", id="ai-1")
        state = {
            "messages": [HumanMessage(content="find shark films"), tm, ai],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.ground_answer(state)
        assert "messages" in result
        msgs = result["messages"]
        assert any(isinstance(m, RemoveMessage) and m.id == "ai-1" for m in msgs)
        new_ai = next(m for m in msgs if isinstance(m, AIMessage))
        assert "⚠️ Warning:" in new_ai.content
        assert "Sharknado" in new_ai.content

    @pytest.mark.asyncio
    async def test_ground_no_tool_messages_skips_check(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock()
        monkeypatch.setattr(agent, "grounding_classifier", mock_clf)

        ai = AIMessage(content="Here are some films.", id="ai-1")
        state = {
            "messages": [HumanMessage(content="show films"), ai],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.ground_answer(state)
        assert result == {}


# ── /ui ────────────────────────────────────────────────────────────────────────

class TestUiRoutes:
    async def test_ui_root_returns_html(self, client):
        with (
            patch("ui_routes.list_sessions", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]
        assert "chat.js" in resp.text

    async def test_ui_root_accepts_thread_id_param(self, client):
        with (
            patch("ui_routes.list_sessions", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui?thread_id=abc-123")
        assert resp.status_code == 200
        assert "abc-123" in resp.text

    async def test_ui_partials_sessions_returns_html(self, client):
        with (
            patch("ui_routes.list_sessions", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/partials/sessions")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]

    async def test_ui_partials_history_returns_html(self, client):
        from models import MessageRecord, SessionHistory
        fake_history = SessionHistory(thread_id="t1", messages=[
            MessageRecord(role="user", content="hello there"),
            MessageRecord(role="assistant", content="**hi** back"),
        ])
        with (
            patch("ui_routes.get_session_history", AsyncMock(return_value=fake_history)),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/partials/history/t1")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]
        assert "hello there" in resp.text
        assert "<strong>hi</strong>" in resp.text

    async def test_ui_delete_session_returns_html(self, client):
        with (
            patch("ui_routes.delete_session", AsyncMock(return_value=True)),
            patch("ui_routes.list_sessions", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.delete("/ui/sessions/t1")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]

    async def test_ui_delete_unknown_session_returns_404(self, client):
        with (
            patch("ui_routes.delete_session", AsyncMock(return_value=False)),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.delete("/ui/sessions/nope")
        assert resp.status_code == 404

    async def test_ui_routes_reject_invalid_thread_ids(self, client):
        assert (await client.get("/ui/partials/history/bad.id")).status_code == 422
        assert (await client.delete("/ui/sessions/bad.id")).status_code == 422

    async def test_ui_delete_refused_while_run_in_flight(self, client):
        import asyncio
        import main as m

        lock = asyncio.Lock()
        await lock.acquire()
        m._thread_locks["busy1"] = lock
        try:
            with (
                patch("ui_routes.delete_session", AsyncMock(return_value=True)) as deleter,
                patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
            ):
                resp = await client.delete("/ui/sessions/busy1")
            assert resp.status_code == 409
            deleter.assert_not_called()
        finally:
            m._thread_locks.pop("busy1", None)


class TestAnalyticsRoute:
    @pytest.mark.asyncio
    async def test_analytics_returns_200_html(self, client):
        fake_revenue = {
            "total_revenue": 67416.51, "total_rentals": 16049,
            "avg_per_rental": 4.20, "busiest_month": "2005-08",
            "busiest_month_revenue": 24072.13, "by_store": [],
        }
        with (
            patch("ui_routes.revenue_summary", AsyncMock(return_value=fake_revenue)),
            patch("ui_routes.store_comparison", AsyncMock(return_value=[])),
            patch("ui_routes.rental_stats_by_category", AsyncMock(return_value=[])),
            patch("ui_routes.overdue_rentals", AsyncMock(return_value=[])),
            patch("ui_routes.slow_moving_films", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/analytics")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]

    @pytest.mark.asyncio
    async def test_analytics_shows_revenue_total(self, client):
        fake_revenue = {
            "total_revenue": 67416.51, "total_rentals": 16049,
            "avg_per_rental": 4.20, "busiest_month": "2005-08",
            "busiest_month_revenue": 24072.13, "by_store": [],
        }
        with (
            patch("ui_routes.revenue_summary", AsyncMock(return_value=fake_revenue)),
            patch("ui_routes.store_comparison", AsyncMock(return_value=[])),
            patch("ui_routes.rental_stats_by_category", AsyncMock(return_value=[])),
            patch("ui_routes.overdue_rentals", AsyncMock(return_value=[])),
            patch("ui_routes.slow_moving_films", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/analytics")
        assert "67416.51" in resp.text

    @pytest.mark.asyncio
    async def test_analytics_has_chart_canvas(self, client):
        empty_rev = {
            "total_revenue": 0.0, "total_rentals": 0, "avg_per_rental": 0.0,
            "busiest_month": None, "busiest_month_revenue": 0.0, "by_store": [],
        }
        with (
            patch("ui_routes.revenue_summary", AsyncMock(return_value=empty_rev)),
            patch("ui_routes.store_comparison", AsyncMock(return_value=[])),
            patch("ui_routes.rental_stats_by_category", AsyncMock(return_value=[])),
            patch("ui_routes.overdue_rentals", AsyncMock(return_value=[])),
            patch("ui_routes.slow_moving_films", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/analytics")
        assert "category-chart" in resp.text
