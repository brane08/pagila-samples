import asyncpg
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage

from models import MessageRecord, SessionHistory, SessionInfo


async def list_sessions(pool: asyncpg.Pool) -> list[SessionInfo]:
    """Return summary info for all persisted sessions, most recently active first."""
    try:
        rows = await pool.fetch(
            """
            SELECT
                thread_id,
                COUNT(*)                        AS step_count,
                MAX(checkpoint->>'ts')          AS last_active
            FROM checkpoints
            WHERE checkpoint_ns = ''
            GROUP BY thread_id
            ORDER BY MAX(checkpoint->>'ts') DESC NULLS LAST
            """
        )
    except asyncpg.UndefinedTableError:
        return []

    return [
        SessionInfo(
            thread_id=row["thread_id"],
            step_count=row["step_count"],
            last_active=row["last_active"],
        )
        for row in rows
    ]


async def get_session_history(agent_app, thread_id: str) -> SessionHistory | None:
    """Return the full message history for a session by replaying the latest state."""
    config = {"configurable": {"thread_id": thread_id, "checkpoint_ns": ""}}
    state = await agent_app.aget_state(config)

    if not state or not state.values:
        return None

    messages: list[MessageRecord] = []
    for msg in state.values.get("messages", []):
        if isinstance(msg, HumanMessage):
            messages.append(MessageRecord(role="user", content=str(msg.content)))

        elif isinstance(msg, AIMessage):
            tool_calls = [tc["name"] for tc in (msg.tool_calls or [])]
            messages.append(MessageRecord(
                role="assistant",
                content=str(msg.content) if msg.content else "",
                tool_calls=tool_calls,
            ))

        elif isinstance(msg, ToolMessage):
            messages.append(MessageRecord(
                role="tool",
                content=str(msg.content),
                tool_name=getattr(msg, "name", None),
            ))

    return SessionHistory(
        thread_id=thread_id,
        messages=messages,
        last_active=state.created_at,
    )


async def delete_session(pool: asyncpg.Pool, thread_id: str) -> bool:
    """Delete all checkpoints for a session. Returns False if the session did not exist."""
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM checkpoint_writes WHERE thread_id = $1", thread_id)
        await conn.execute("DELETE FROM checkpoint_blobs  WHERE thread_id = $1", thread_id)
        result = await conn.execute("DELETE FROM checkpoints WHERE thread_id = $1", thread_id)
    # asyncpg returns "DELETE N" — extract row count
    return int(result.split()[-1]) > 0
