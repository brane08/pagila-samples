from typing import Annotated, TypedDict

from langchain_core.messages import BaseMessage
from langgraph.graph import add_messages
from pydantic import BaseModel, Field

_ID_PATTERN = r"^[A-Za-z0-9_-]{1,64}$"


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    thread_id: str = Field(default="default", pattern=_ID_PATTERN)
    user_id: str = Field(default="anonymous", pattern=_ID_PATTERN)


class ChatResponse(BaseModel):
    answer: str
    tool_calls_made: list[str]


class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    summary: str
    tool_retry_count: int
    reflection_retry_count: int
    reflection_critique: str | None
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
    confirm_id: str | None = Field(default=None, max_length=64)
