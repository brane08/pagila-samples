import asyncio
import hashlib
import json
import logging
import os
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import AsyncIterator

from typing import Annotated

from fastapi import FastAPI, HTTPException, Path as PathParam
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from fastapi.staticfiles import StaticFiles
from langchain_core.messages import HumanMessage

from langgraph.types import Command

from agent import RECURSION_LIMIT, build_agent
from db import close_pools, get_asyncpg_pool, init_asyncpg_pool, init_psycopg_pool
from models import ChatRequest, ChatResponse, ConfirmRequest, ReindexStatus, SessionListResponse
from sessions import delete_session, get_session_history, list_sessions
from ui_routes import ui_router

# ── App state ──────────────────────────────────────────────────────────────────

agent_app = None
mcp_client = None
_reindex_job: ReindexStatus = ReindexStatus(status="idle")
_reindex_task: asyncio.Task | None = None
_thread_locks: dict[str, asyncio.Lock] = {}
_run_tasks: set[asyncio.Task] = set()
logger = logging.getLogger(__name__)
RUN_TIMEOUT_S = float(os.environ.get("RUN_TIMEOUT_S", "300"))
SHUTDOWN_GRACE_S = 10.0
ThreadId = Annotated[str, PathParam(pattern=r"^[A-Za-z0-9_-]{1,64}$")]


def _require_single_worker() -> None:
    """Thread locks and the approval re-checks live in this process; several workers would silently bypass them."""
    for var in ("WEB_CONCURRENCY", "UVICORN_WORKERS"):
        workers = os.environ.get(var, "1")
        if workers.isdigit() and int(workers) > 1:
            raise RuntimeError(
                f"{var}={workers}: genai-assistant keeps per-thread locks in process memory and must run "
                "as a single worker."
            )


async def _drain_background_tasks() -> None:
    """Let in-flight runs finish (a resumed tool must not be cut off), then cancel stragglers before pools close."""
    runs = list(_run_tasks)
    if runs:
        _, pending = await asyncio.wait(runs, timeout=SHUTDOWN_GRACE_S)
        for task in pending:
            task.cancel()
        await asyncio.gather(*pending, return_exceptions=True)
    if _reindex_task is not None and not _reindex_task.done():
        _reindex_task.cancel()
        await asyncio.gather(_reindex_task, return_exceptions=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    global agent_app, mcp_client
    _require_single_worker()
    print("Starting up: initialising DB connection pools...")
    asyncpg_pool = await init_asyncpg_pool()
    psycopg_pool = await init_psycopg_pool()
    print("Building LangGraph agent...")
    agent_app, mcp_client = await build_agent(psycopg_pool, asyncpg_pool=asyncpg_pool)
    print("Agent ready.")
    yield
    print("Shutting down...")
    await _drain_background_tasks()
    if mcp_client:
        await mcp_client.aclose()
    await close_pools()


app = FastAPI(
    title="Pagila Film Agent",
    description="LangGraph agent over the Pagila DVD rental database",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.environ.get(
        "CORS_ORIGINS", "http://localhost:4200,http://localhost:8000,http://127.0.0.1:8000"
    ).split(","),
    allow_credentials=False,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["Content-Type"],
)

app.mount("/static", StaticFiles(directory=Path(__file__).parent / "static"), name="static")
app.include_router(ui_router)


# ── Helpers ────────────────────────────────────────────────────────────────────

def _tool_names(messages: list) -> list[str]:
    names = []
    for m in messages:
        for tc in getattr(m, "tool_calls", []):
            names.append(tc["name"])
    return names


def _require_agent():
    if agent_app is None:
        raise HTTPException(status_code=503, detail="Agent not ready")


# ── Health ─────────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    return {"status": "ok", "agent_ready": agent_app is not None}


# ── Chat ───────────────────────────────────────────────────────────────────────

def _config(thread_id: str) -> dict:
    return {"configurable": {"thread_id": thread_id}, "recursion_limit": RECURSION_LIMIT}


def _confirm_id(tool_calls: list) -> str:
    """Stable id of one pending approval, derived from the proposed calls; ties a confirm to the card the user saw."""
    basis = [[tc.get("id"), tc.get("name"), tc.get("args")] for tc in tool_calls if isinstance(tc, dict)]
    return hashlib.sha256(json.dumps(basis, sort_keys=True, default=str).encode()).hexdigest()[:16]


def _calls_of(interrupts: list) -> list:
    calls: list = []
    for i in interrupts:
        if isinstance(i.value, dict):
            calls.extend(i.value.get("tool_calls", []))
    return calls


async def _pending_interrupts(config: dict) -> list:
    state = await agent_app.aget_state(config)
    return [i for task in state.tasks for i in task.interrupts]


async def _pending_tool_calls(config: dict) -> list | None:
    """Tool calls (across all pending interrupts) awaiting confirmation, or None when nothing is pending."""
    interrupts = await _pending_interrupts(config)
    return _calls_of(interrupts) if interrupts else None


def _resume_command(interrupts: list, approved: bool) -> Command:
    """One decision for everything shown on the card. Several interrupts need an id-keyed resume map."""
    decision = {"approved": approved}
    if len(interrupts) > 1:
        return Command(resume={i.id: decision for i in interrupts})
    return Command(resume=decision)


def _thread_lock(thread_id: str) -> asyncio.Lock:
    return _thread_locks.setdefault(thread_id, asyncio.Lock())


def _is_busy(thread_id: str) -> bool:
    """Whether a run holds the thread; does not create a lock entry, so probing random ids cannot grow the dict."""
    lock = _thread_locks.get(thread_id)
    return lock is not None and lock.locked()


def _evict_lock(thread_id: str) -> None:
    lock = _thread_locks.get(thread_id)
    if lock is not None and not lock.locked():
        del _thread_locks[thread_id]


def _pending_detail(message: str, pending: list) -> dict:
    return {"message": message, "tool_calls": pending, "confirm_id": _confirm_id(pending)}


async def _reject_if_pending_or_busy(config: dict) -> None:
    """409 when a run is in flight, or when a tool call awaits confirmation (with the calls, so the UI can show them)."""
    thread_id = config["configurable"]["thread_id"]
    if _is_busy(thread_id):
        raise HTTPException(status_code=409, detail={"message": "Another request is running on this thread"})
    pending = await _pending_tool_calls(config)
    if pending is not None:
        raise HTTPException(
            status_code=409,
            detail=_pending_detail("A tool call is awaiting confirmation on this thread", pending),
        )


@app.get("/chat/pending/{thread_id}")
async def chat_pending(thread_id: ThreadId):
    """Tool calls awaiting confirmation, so the UI can restore the confirm card after a reload."""
    _require_agent()
    pending = await _pending_tool_calls(_config(thread_id))
    return {
        "pending": pending is not None,
        "tool_calls": pending or [],
        "confirm_id": _confirm_id(pending) if pending is not None else None,
    }


MAX_AUTO_REJECTS = 5


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    _require_agent()
    config = _config(request.thread_id)
    await _reject_if_pending_or_busy(config)
    lock = _thread_lock(request.thread_id)
    if lock.locked():
        raise HTTPException(status_code=409, detail={"message": "Another request is running on this thread"})
    try:
        async with lock:
            result = await agent_app.ainvoke(
                {"messages": [HumanMessage(content=request.message)], "user_id": request.user_id},
                config=config,
            )
            if result.get("__interrupt__"):
                # The blocking API cannot confirm tools; reject so the thread is not left paused.
                # A rejected turn can make the model propose another tool, so keep rejecting (bounded).
                for _ in range(MAX_AUTO_REJECTS):
                    result = await agent_app.ainvoke(_resume_command(result["__interrupt__"], False), config=config)
                    if not result.get("__interrupt__"):
                        break
                pending = await _pending_tool_calls(config)
                detail = (
                    _pending_detail("Tool confirmation required; use /chat/stream and /chat/confirm/{thread_id}/stream", pending)
                    if pending is not None
                    else "Tool confirmation required; use /chat/stream and /chat/confirm/{thread_id}/stream"
                )
                raise HTTPException(status_code=409, detail=detail)
    finally:
        _evict_lock(request.thread_id)
    messages = result["messages"]
    return ChatResponse(
        answer=messages[-1].content,
        tool_calls_made=_tool_names(messages),
    )


async def _stream_agent_events(input_or_command, config: dict, confirm_id: str | None = None) -> AsyncIterator[str]:
    """Shared SSE generator: holds the thread lock, streams token/tool events, then emits done or tool_confirm.

    The pending-approval state is re-checked under the lock: a confirm must match the card the user saw
    (`confirm_id`), and a new message must not start while an approval is pending.
    """
    thread_id = config["configurable"]["thread_id"]
    lock = _thread_lock(thread_id)
    if lock.locked():
        yield f"data: {json.dumps({'type': 'error', 'message': 'Another request is running on this thread.'})}\n\n"
        return
    await lock.acquire()  # not locked, so this does not wait: no other request can slip in before the task starts
    queue: asyncio.Queue[str | None] = asyncio.Queue()

    async def _run() -> None:
        # Runs detached from the HTTP response: a client disconnect must not cancel a resumed graph mid-tool,
        # or the checkpoint would not advance and the same approval would reappear (and the tool re-run).
        try:
            pending = await _pending_tool_calls(config)
            is_confirm = isinstance(input_or_command, Command)
            if is_confirm and (pending is None or (confirm_id and confirm_id != _confirm_id(pending))):
                await queue.put(f"data: {json.dumps({'type': 'error', 'message': 'That approval is no longer pending.', 'code': 'stale_confirm'})}\n\n")
            elif not is_confirm and pending is not None:
                await queue.put(f"data: {json.dumps({'type': 'error', 'message': 'A tool call is awaiting your confirmation.', 'code': 'pending'})}\n\n")
            else:
                async with asyncio.timeout(RUN_TIMEOUT_S):  # a hung tool/model call must not hold the thread forever
                    async for line in _stream_locked(input_or_command, config):
                        await queue.put(line)
        except Exception:
            logger.exception("Agent run failed")
            await queue.put(f"data: {json.dumps({'type': 'error', 'message': 'The assistant hit an error. Please try again.'})}\n\n")
        finally:
            lock.release()
            _evict_lock(thread_id)
            await queue.put(None)

    task = asyncio.create_task(_run())
    _run_tasks.add(task)  # strong ref: the loop only holds tasks weakly
    task.add_done_callback(_run_tasks.discard)
    while (line := await queue.get()) is not None:
        yield line


async def _stream_locked(input_or_command, config: dict) -> AsyncIterator[str]:
    agent_streamed_any = False
    streamed_text = ""
    streamed_opaque = False  # non-str chunk content (content blocks) cannot be compared with the final text
    try:
        async for event in agent_app.astream_events(input_or_command, config=config, version="v2"):
            kind = event["event"]
            if kind == "on_chat_model_start":
                # A new agent run (e.g. after a reflection retry or a tool round) replaces earlier draft text.
                if event.get("metadata", {}).get("langgraph_node") == "agent" and agent_streamed_any:
                    streamed_text = ""
                    streamed_opaque = False
                    agent_streamed_any = False
                    yield 'data: {"type": "reset"}\n\n'
            elif kind == "on_chat_model_stream":
                if event.get("metadata", {}).get("langgraph_node") == "agent":
                    chunk = event["data"]["chunk"]
                    if chunk.content:
                        agent_streamed_any = True
                        if isinstance(chunk.content, str):
                            streamed_text += chunk.content
                        else:
                            streamed_opaque = True
                        yield f"data: {json.dumps({'type': 'token', 'content': chunk.content})}\n\n"
            elif kind == "on_tool_start":
                yield f"data: {json.dumps({'type': 'tool_start', 'tool': event['name'], 'input': event['data'].get('input')})}\n\n"
            elif kind == "on_tool_end":
                yield f"data: {json.dumps({'type': 'tool_end', 'tool': event['name']})}\n\n"

        state = await agent_app.aget_state(config)
        if any(task.interrupts for task in state.tasks):
            interrupts = [i for task in state.tasks for i in task.interrupts]
            thread_id = config["configurable"]["thread_id"]
            calls = _calls_of(interrupts)
            yield f"data: {json.dumps({'type': 'tool_confirm', 'thread_id': thread_id, 'tool_calls': calls, 'confirm_id': _confirm_id(calls)})}\n\n"
        else:
            values = getattr(state, "values", None)
            msgs = values.get("messages", []) if values else []
            if msgs:
                from langchain_core.messages import AIMessage as _AI
                last = msgs[-1]
                if isinstance(last, _AI) and not getattr(last, "tool_calls", None) and isinstance(last.content, str) and last.content:
                    if not agent_streamed_any:
                        yield f"data: {json.dumps({'type': 'token', 'content': last.content})}\n\n"
                    elif not streamed_opaque and last.content.startswith(streamed_text):
                        if len(last.content) > len(streamed_text):
                            # ground_answer appends a warning after the model finished streaming.
                            yield f"data: {json.dumps({'type': 'token', 'content': last.content[len(streamed_text):]})}\n\n"
                    else:
                        # The final message differs from what was streamed (give-up text, clarify question
                        # replacing a tool-call draft): replace it instead of dropping it.
                        yield 'data: {"type": "reset"}\n\n'
                        yield f"data: {json.dumps({'type': 'token', 'content': last.content})}\n\n"
            yield 'data: {"type": "done"}\n\n'
    except Exception:
        logger.exception("Agent stream failed")
        yield f"data: {json.dumps({'type': 'error', 'message': 'The assistant hit an error. Please try again.'})}\n\n"


@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    _require_agent()
    config = _config(request.thread_id)
    await _reject_if_pending_or_busy(config)
    return StreamingResponse(
        _stream_agent_events(
            {"messages": [HumanMessage(content=request.message)], "user_id": request.user_id},
            config,
        ),
        media_type="text/event-stream",
    )


@app.post("/chat/confirm/{thread_id}/stream")
async def chat_confirm_stream(thread_id: ThreadId, body: ConfirmRequest):
    """Resume a paused agent run after user approves or rejects the pending tool call."""
    _require_agent()
    config = _config(thread_id)
    if _is_busy(thread_id):
        raise HTTPException(status_code=409, detail={"message": "Another request is running on this thread"})
    interrupts = await _pending_interrupts(config)
    if not interrupts:
        raise HTTPException(status_code=409, detail="No tool call is awaiting confirmation on this thread")
    pending = _calls_of(interrupts)
    if body.confirm_id and body.confirm_id != _confirm_id(pending):
        raise HTTPException(
            status_code=409,
            detail=_pending_detail("That approval is stale; a different tool call is now pending", pending),
        )
    command = _resume_command(interrupts, body.approved)
    return StreamingResponse(
        _stream_agent_events(command, config, body.confirm_id),
        media_type="text/event-stream",
    )


# ── Admin ─────────────────────────────────────────────────────────────────────

async def _run_reindex() -> None:
    global _reindex_job
    try:
        # Importing rag loads the embedding model (blocking); do it in a thread.
        rag = await asyncio.to_thread(__import__, "rag")
        await rag.index_films()
        _reindex_job = ReindexStatus(
            status="done",
            started_at=_reindex_job.started_at,
            finished_at=datetime.now(timezone.utc).isoformat(),
        )
    except Exception:
        logger.exception("Reindex failed")
        _reindex_job = ReindexStatus(
            status="error",
            started_at=_reindex_job.started_at,
            finished_at=datetime.now(timezone.utc).isoformat(),
            error="Reindex failed; see server logs",
        )


@app.post("/admin/reindex", response_model=ReindexStatus, status_code=202)
async def start_reindex():
    """Trigger a background re-indexing of film embeddings into pgvector."""
    global _reindex_job, _reindex_task
    if _reindex_job.status == "running":
        raise HTTPException(status_code=409, detail="Reindex already in progress")
    _reindex_job = ReindexStatus(
        status="running",
        started_at=datetime.now(timezone.utc).isoformat(),
    )
    _reindex_task = asyncio.create_task(_run_reindex())  # module-level ref: tasks are weakly held by the loop
    return _reindex_job


@app.get("/admin/reindex", response_model=ReindexStatus)
async def reindex_status():
    """Return the status of the last (or current) reindex job."""
    return _reindex_job


# ── Sessions ───────────────────────────────────────────────────────────────────

@app.get("/sessions", response_model=SessionListResponse)
async def list_all_sessions():
    """List all persisted sessions, most recently active first."""
    sessions = await list_sessions(get_asyncpg_pool())
    return SessionListResponse(sessions=sessions, total=len(sessions))


@app.get("/sessions/{thread_id}")
async def get_session(thread_id: ThreadId):
    """Return the full message history for a session."""
    _require_agent()
    history = await get_session_history(agent_app, thread_id)
    if history is None:
        raise HTTPException(status_code=404, detail=f"Session '{thread_id}' not found")
    return history


@app.delete("/sessions/{thread_id}")
async def remove_session(thread_id: ThreadId):
    """Delete a session and all its checkpointed state."""
    if _is_busy(thread_id):
        raise HTTPException(status_code=409, detail="A run is in progress on this thread")
    deleted = await delete_session(get_asyncpg_pool(), thread_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"Session '{thread_id}' not found")
    return {"deleted": True, "thread_id": thread_id}


# ── Entry point ────────────────────────────────────────────────────────────────

def main():
    import uvicorn
    uvicorn.run(app, host=os.environ.get("HOST", "127.0.0.1"), port=8000)


if __name__ == "__main__":
    main()
