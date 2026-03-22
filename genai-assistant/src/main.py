import json
from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage

from agent import build_agent
from db import close_pools, get_asyncpg_pool, init_asyncpg_pool, init_psycopg_pool
from models import ChatRequest, ChatResponse, SessionListResponse
from sessions import delete_session, get_session_history, list_sessions

# ── App state ──────────────────────────────────────────────────────────────────

agent_app = None
mcp_client = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global agent_app, mcp_client
    print("Starting up: initialising DB connection pools...")
    await init_asyncpg_pool()
    psycopg_pool = await init_psycopg_pool()
    print("Building LangGraph agent...")
    agent_app, mcp_client = await build_agent(psycopg_pool)
    print("Agent ready.")
    yield
    print("Shutting down...")
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
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


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

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    _require_agent()
    result = await agent_app.ainvoke(
        {"messages": [HumanMessage(content=request.message)]},
        config={"configurable": {"thread_id": request.thread_id}},
    )
    messages = result["messages"]
    return ChatResponse(
        answer=messages[-1].content,
        tool_calls_made=_tool_names(messages),
    )


@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    _require_agent()

    async def event_generator() -> AsyncIterator[str]:
        async for event in agent_app.astream_events(
            {"messages": [HumanMessage(content=request.message)]},
            config={"configurable": {"thread_id": request.thread_id}},
            version="v2",
        ):
            kind = event["event"]

            if kind == "on_chat_model_stream":
                chunk = event["data"]["chunk"]
                if chunk.content:
                    yield f"data: {json.dumps({'type': 'token', 'content': chunk.content})}\n\n"

            elif kind == "on_tool_start":
                yield f"data: {json.dumps({'type': 'tool_start', 'tool': event['name'], 'input': event['data'].get('input')})}\n\n"

            elif kind == "on_tool_end":
                yield f"data: {json.dumps({'type': 'tool_end', 'tool': event['name']})}\n\n"

        yield 'data: {"type": "done"}\n\n'

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ── Sessions ───────────────────────────────────────────────────────────────────

@app.get("/sessions", response_model=SessionListResponse)
async def list_all_sessions():
    """List all persisted sessions, most recently active first."""
    sessions = await list_sessions(get_asyncpg_pool())
    return SessionListResponse(sessions=sessions, total=len(sessions))


@app.get("/sessions/{thread_id}")
async def get_session(thread_id: str):
    """Return the full message history for a session."""
    _require_agent()
    history = await get_session_history(agent_app, thread_id)
    if history is None:
        raise HTTPException(status_code=404, detail=f"Session '{thread_id}' not found")
    return history


@app.delete("/sessions/{thread_id}")
async def remove_session(thread_id: str):
    """Delete a session and all its checkpointed state."""
    deleted = await delete_session(get_asyncpg_pool(), thread_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"Session '{thread_id}' not found")
    return {"deleted": True, "thread_id": thread_id}


# ── Entry point ────────────────────────────────────────────────────────────────

def main():
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


if __name__ == "__main__":
    main()
