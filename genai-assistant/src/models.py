from typing import Annotated, TypedDict

from langchain_core.messages import BaseMessage
from langgraph.graph import add_messages
from pydantic import BaseModel


class ChatRequest(BaseModel):
    message: str
    thread_id: str = "default"
    user_id: str = "anonymous"


class ChatResponse(BaseModel):
    answer: str
    tool_calls_made: list[str]


class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    summary: str
    tool_retry_count: int
    reflection_retry_count: int
    preferred_store_id: int | None
    customer_email: str | None
    user_id: str


# ── Session models ─────────────────────────────────────────────────────────────

class MessageRecord(BaseModel):
    role: str                     # "user" | "assistant" | "tool"
    content: str
    tool_calls: list[str] = []    # tool names called (AI messages only)
    tool_name: str | None = None  # tool that produced this result (tool messages only)


class SessionInfo(BaseModel):
    thread_id: str
    step_count: int               # number of LangGraph steps (agent + tool turns)
    last_active: str | None = None  # ISO timestamp from checkpoint metadata


class SessionListResponse(BaseModel):
    sessions: list[SessionInfo]
    total: int


class SessionHistory(BaseModel):
    thread_id: str
    messages: list[MessageRecord]
    last_active: str | None = None


# ── Admin models ───────────────────────────────────────────────────────────────

class ReindexStatus(BaseModel):
    status: str           # "idle" | "running" | "done" | "error"
    started_at: str | None = None
    finished_at: str | None = None
    error: str | None = None


class ConfirmRequest(BaseModel):
    approved: bool
