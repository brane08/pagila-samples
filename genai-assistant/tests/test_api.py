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
        yield {"event": "on_chat_model_stream", "data": {"chunk": _chunk("Hello")}, "name": "llm"}
        yield {"event": "on_chat_model_stream", "data": {"chunk": _chunk(" world")}, "name": "llm"}

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
