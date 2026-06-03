# Agentic Features Phase 3 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four agentic improvements to the LangGraph ReAct agent: clarification node, tool error recovery, persistent user preferences, and structured output rendering.

**Architecture:** Features 1–3 are backend-only (agent.py, models.py, main.py, schema.sql); feature 4 spans the SYSTEM_PROMPT and Angular chat component. Each feature adds nodes/edges to the LangGraph graph in `build_agent` and is independently testable. All backend tests live in `genai-assistant/tests/test_api.py` (no live DB needed — mock asyncpg pool).

**Tech Stack:** Python 3.13, FastAPI, LangGraph, asyncpg, Pydantic v2, Angular 20, Angular Material `MatTableModule`

---

## File Map

| File | Changes |
|---|---|
| `genai-assistant/src/models.py` | +`tool_retry_count`, +`preferred_store_id`, +`customer_email`, +`user_id` to `AgentState`; +`user_id` to `ChatRequest` |
| `genai-assistant/src/agent.py` | +`ClarificationCheck`, +`clarify_tool_args`, +`_after_clarify`, +`handle_tool_errors`, +`load_prefs`, +`save_prefs`; update `_prepare_messages`, `SYSTEM_PROMPT`, `build_agent` |
| `genai-assistant/src/main.py` | Update `_stream_agent_events` to emit node-injected AI messages; thread `user_id` into graph invocation |
| `database/schema.sql` | +`user_preferences` table |
| `genai-assistant/tests/test_api.py` | +12 new tests across 4 features |
| `ui-angular/src/app/chat.models.ts` | +`tableData` field to `ChatMessage`; +`StructuredTable` interface |
| `ui-angular/src/app/chat.service.ts` | JSON block detection on `done` event; pass `user_id` in requests |
| `ui-angular/src/app/agent.service.ts` | Pass `user_id` in `streamChat` / `chat` bodies |
| `ui-angular/src/app/chat/chat.component.html` | Conditional `mat-table` render |
| `ui-angular/src/app/chat/chat.module.ts` | Import `MatTableModule` |

---

## Task 1: Models — AgentState fields + ChatRequest

**Files:**
- Modify: `genai-assistant/src/models.py`

- [ ] **Step 1: Write the failing test**

In `genai-assistant/tests/test_api.py`, add after the last test class:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestAgentStateFields -v
```

Expected: `FAILED` — `AgentState` missing `tool_retry_count` etc., `ChatRequest` missing `user_id`.

- [ ] **Step 3: Implement**

Replace lines 18–20 of `genai-assistant/src/models.py`:

```python
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    summary: str
    tool_retry_count: int
    preferred_store_id: int | None
    customer_email: str | None
    user_id: str
```

Replace lines 8–10 of `genai-assistant/src/models.py`:

```python
class ChatRequest(BaseModel):
    message: str
    thread_id: str = "default"
    user_id: str = "anonymous"
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestAgentStateFields -v
```

Expected: 3 passed.

- [ ] **Step 5: Commit**

```bash
cd genai-assistant && git add src/models.py tests/test_api.py
git commit -m "feat: add tool_retry_count, prefs, user_id to AgentState and ChatRequest"
```

---

## Task 2: DB Schema — user_preferences table

**Files:**
- Modify: `database/schema.sql`

- [ ] **Step 1: Add the table**

Append to `database/schema.sql`:

```sql
-- Patch: persistent user preferences for genai-assistant
CREATE TABLE IF NOT EXISTS public.user_preferences (
    user_id TEXT PRIMARY KEY,
    preferred_store_id INT,
    customer_email TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

- [ ] **Step 2: Apply to local DB**

```bash
psql -U postgres -d sakila -f database/schema.sql
```

Expected: output includes `CREATE TABLE` (or `NOTICE: relation already exists` if run before).

- [ ] **Step 3: Commit**

```bash
git add database/schema.sql
git commit -m "feat: add user_preferences table for persistent chat prefs"
```

---

## Task 3: Clarification Node

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Modify: `genai-assistant/tests/test_api.py`

### Step 1: Write the failing tests

Add to `genai-assistant/tests/test_api.py`:

```python
# ── Clarification node ─────────────────────────────────────────────────────────

class TestClarificationNode:
    def test_after_clarify_routes_to_human_review_when_tool_calls_remain(self):
        from langchain_core.messages import AIMessage
        import agent

        class FakeToolCall:
            pass

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
        from langchain_core.messages import AIMessage, RemoveMessage
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestClarificationNode -v
```

Expected: 5 FAILED — `ClarificationCheck`, `clarify_tool_args`, `_after_clarify` not defined yet.

- [ ] **Step 3: Implement in agent.py**

Add after the `classifier = model.with_structured_output(TopicCheck)` line (line 130):

```python
class ClarificationCheck(BaseModel):
    needs_clarification: bool = Field(
        description="True if any required tool argument is null, empty, or missing from the conversation context."
    )
    question: str | None = Field(
        default=None,
        description="The specific question to ask the user to obtain the missing argument value."
    )

CLARIFICATION_PROMPT = (
    "You are a tool argument validator for a DVD rental database assistant. "
    "Given a tool name and its arguments, determine if any required argument is null or missing. "
    "Required arguments that are commonly missing: 'email' for customer tools, 'store_id' when a specific store is needed. "
    "If an argument is null/None/empty, set needs_clarification=true and write a specific question to ask the user. "
    "If all required arguments have values, set needs_clarification=false."
)

clarification_classifier = model.with_structured_output(ClarificationCheck)


def _after_clarify(state: AgentState) -> str:
    last = state["messages"][-1]
    if isinstance(last, AIMessage) and not getattr(last, "tool_calls", None):
        return END
    return "human_review"
```

Add the `clarify_tool_args` function after `validate_input` (after line 189):

```python
async def clarify_tool_args(state: AgentState) -> dict:
    import json as _json
    last = state["messages"][-1]
    if not getattr(last, "tool_calls", None):
        return {}
    tool_call = last.tool_calls[0]
    result: ClarificationCheck = await clarification_classifier.ainvoke([
        SystemMessage(content=CLARIFICATION_PROMPT),
        HumanMessage(content=f"Tool: {tool_call['name']}\nArgs: {_json.dumps(tool_call['args'])}"),
    ])
    if result.needs_clarification:
        return {"messages": [
            RemoveMessage(id=last.id),
            AIMessage(content=result.question or "Could you provide more details?"),
        ]}
    return {}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestClarificationNode -v
```

Expected: 5 passed.

- [ ] **Step 5: Wire into graph in build_agent**

In `build_agent`, replace line 251:
```python
# Before:
graph.add_conditional_edges("agent", tools_condition, {"tools": "human_review", END: END})

# After:
graph.add_node("clarify", clarify_tool_args)
graph.add_conditional_edges("agent", tools_condition, {"tools": "clarify", END: END})
graph.add_conditional_edges("clarify", _after_clarify, {"human_review": "human_review", END: END})
```

- [ ] **Step 6: Update _stream_agent_events in main.py to emit clarify questions**

In `main.py`, update `_stream_agent_events` to detect non-streamed AIMessages from the clarify node. Replace the function body:

```python
async def _stream_agent_events(input_or_command, config: dict) -> AsyncIterator[str]:
    """Shared SSE generator: streams token/tool events, then emits done or tool_confirm."""
    agent_streamed_any = False
    async for event in agent_app.astream_events(input_or_command, config=config, version="v2"):
        kind = event["event"]
        if kind == "on_chat_model_stream":
            if event.get("metadata", {}).get("langgraph_node") == "agent":
                chunk = event["data"]["chunk"]
                if chunk.content:
                    agent_streamed_any = True
                    yield f"data: {json.dumps({'type': 'token', 'content': chunk.content})}\n\n"
        elif kind == "on_tool_start":
            yield f"data: {json.dumps({'type': 'tool_start', 'tool': event['name'], 'input': event['data'].get('input')})}\n\n"
        elif kind == "on_tool_end":
            yield f"data: {json.dumps({'type': 'tool_end', 'tool': event['name']})}\n\n"

    state = await agent_app.aget_state(config)
    if any(task.interrupts for task in state.tasks):
        interrupts = [i for task in state.tasks for i in task.interrupts]
        interrupt_value = interrupts[0].value
        thread_id = config["configurable"]["thread_id"]
        yield f"data: {json.dumps({'type': 'tool_confirm', 'thread_id': thread_id, 'tool_calls': interrupt_value.get('tool_calls', [])})}\n\n"
    else:
        if not agent_streamed_any:
            msgs = state.values.get("messages", []) if state.values else []
            if msgs:
                from langchain_core.messages import AIMessage as _AI
                last = msgs[-1]
                if isinstance(last, _AI) and not getattr(last, "tool_calls", None) and last.content:
                    yield f"data: {json.dumps({'type': 'token', 'content': last.content})}\n\n"
        yield 'data: {"type": "done"}\n\n'
```

- [ ] **Step 7: Run full test suite**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: all tests pass (previous 33 + 5 new = 38).

- [ ] **Step 8: Commit**

```bash
cd genai-assistant && git add src/agent.py src/main.py tests/test_api.py
git commit -m "feat: add clarification node to intercept missing tool args"
```

---

## Task 4: Tool Error Recovery Node

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Modify: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the failing tests**

Add to `genai-assistant/tests/test_api.py`:

```python
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
        assert any(isinstance(m, RemoveMessage) and m.id == "tm-1" for m in msgs)
        hint_msgs = [m for m in msgs if isinstance(m, ToolMessage)]
        assert hint_msgs and "alternative" in hint_msgs[0].content.lower()

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
        assert result.get("tool_retry_count") == 0
        assert "messages" not in result or not result["messages"]

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
```

Note: `RemoveMessage` is already imported in `agent.py` — add `from langchain_core.messages import RemoveMessage` to the test import block at top of file if not present.

- [ ] **Step 2: Add RemoveMessage to test imports**

At the top of `tests/test_api.py`, the imports section (around line 10), add:
```python
from langchain_core.messages import RemoveMessage
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestToolErrorRecovery -v
```

Expected: 4 FAILED — `handle_tool_errors` not defined.

- [ ] **Step 4: Implement handle_tool_errors in agent.py**

Add after `clarify_tool_args` (before `human_review`):

```python
async def handle_tool_errors(state: AgentState) -> dict:
    import json as _json
    messages = state["messages"]
    retry_count = state.get("tool_retry_count", 0)

    tool_messages = [m for m in messages if isinstance(m, ToolMessage)]
    error_messages = []
    for tm in tool_messages:
        try:
            parsed = _json.loads(tm.content) if isinstance(tm.content, str) else {}
            if isinstance(parsed, dict) and "error" in parsed:
                error_messages.append(tm)
        except (_json.JSONDecodeError, TypeError):
            pass

    if not error_messages:
        return {"tool_retry_count": 0}

    if retry_count == 0:
        replacements = []
        for tm in error_messages:
            replacements.append(RemoveMessage(id=tm.id))
            replacements.append(ToolMessage(
                content="Tool call failed. Please try an alternative approach.",
                tool_call_id=tm.tool_call_id,
                name=tm.name,
            ))
        return {"tool_retry_count": 1, "messages": replacements}

    return {"tool_retry_count": 0}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestToolErrorRecovery -v
```

Expected: 4 passed.

- [ ] **Step 6: Wire into graph in build_agent**

In `build_agent`, replace the lambda edge from `tools` (the `add_conditional_edges` block at lines 253–257):

```python
# Before:
graph.add_conditional_edges(
    "tools",
    lambda s: "summarize" if len(s["messages"]) > SUMMARIZE_THRESHOLD else "agent",
    {"summarize": "summarize", "agent": "agent"},
)

# After:
graph.add_node("handle_errors", handle_tool_errors)
graph.add_edge("tools", "handle_errors")
graph.add_conditional_edges(
    "handle_errors",
    lambda s: "summarize" if len(s["messages"]) > SUMMARIZE_THRESHOLD else "agent",
    {"summarize": "summarize", "agent": "agent"},
)
```

- [ ] **Step 7: Run full test suite**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: all tests pass (38 + 4 new = 42).

- [ ] **Step 8: Commit**

```bash
cd genai-assistant && git add src/agent.py tests/test_api.py
git commit -m "feat: add tool error recovery node with retry-once then fallback"
```

---

## Task 5: Persistent User Preferences Nodes

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Modify: `genai-assistant/src/main.py`
- Modify: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the failing tests**

Add to `genai-assistant/tests/test_api.py`:

```python
# ── User preferences nodes ─────────────────────────────────────────────────────

class TestUserPreferencesNodes:
    @pytest.mark.asyncio
    async def test_load_prefs_hydrates_state(self, monkeypatch):
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
    async def test_load_prefs_noop_for_unknown_user(self, monkeypatch):
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
    async def test_save_prefs_upserts_new_store_id(self, monkeypatch):
        import agent
        from langchain_core.messages import ToolMessage

        mock_pool = MagicMock()
        mock_conn = AsyncMock()
        mock_conn.execute = AsyncMock()
        mock_pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=mock_conn),
            __aexit__=AsyncMock(return_value=None),
        ))

        tm = ToolMessage(content='{"store_id": 1, "city": "Lethbridge"}', tool_call_id="tc1", name="list_stores")
        state = {"messages": [tm], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": None, "customer_email": None, "user_id": "user-1"}
        result = await agent.save_prefs(state, mock_pool)
        mock_conn.execute.assert_called_once()
        assert result["preferred_store_id"] == 1

    @pytest.mark.asyncio
    async def test_save_prefs_skips_when_store_id_unchanged(self, monkeypatch):
        import agent
        from langchain_core.messages import ToolMessage

        mock_pool = MagicMock()
        mock_conn = AsyncMock()
        mock_conn.execute = AsyncMock()
        mock_pool.acquire = MagicMock(return_value=AsyncMock(
            __aenter__=AsyncMock(return_value=mock_conn),
            __aexit__=AsyncMock(return_value=None),
        ))

        tm = ToolMessage(content='{"store_id": 1}', tool_call_id="tc1", name="list_stores")
        state = {"messages": [tm], "summary": "", "tool_retry_count": 0,
                 "preferred_store_id": 1, "customer_email": None, "user_id": "user-1"}
        await agent.save_prefs(state, mock_pool)
        mock_conn.execute.assert_not_called()

    def test_prepare_messages_injects_preference_context(self):
        import agent

        messages = [HumanMessage(content="show me films")]
        result = agent._prepare_messages(messages, "", preferred_store_id=2, customer_email="jane@example.com")
        sys_msgs = [m for m in result if isinstance(m, SystemMessage)]
        combined = " ".join(m.content for m in sys_msgs)
        assert "preferred store" in combined.lower() or "store" in combined.lower()
        assert "jane@example.com" in combined
```

Add `from langchain_core.messages import HumanMessage, SystemMessage` near the top of the test imports if not already present.

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestUserPreferencesNodes -v
```

Expected: 5 FAILED — `load_prefs`, `save_prefs` not defined, `_prepare_messages` signature mismatch.

- [ ] **Step 3: Implement load_prefs and save_prefs in agent.py**

Update `_prepare_messages` signature (line 139):

```python
def _prepare_messages(
    messages: list, summary: str,
    preferred_store_id: int | None = None,
    customer_email: str | None = None,
) -> list:
    if any(isinstance(m, SystemMessage) for m in messages):
        return messages
    prefix = [SystemMessage(content=SYSTEM_PROMPT)]
    if summary:
        prefix.append(SystemMessage(content=f"Earlier conversation summary:\n{summary}"))
    if preferred_store_id is not None or customer_email:
        parts = []
        if preferred_store_id is not None:
            parts.append(f"preferred store ID = {preferred_store_id}")
        if customer_email:
            parts.append(f"known customer email = {customer_email}")
        prefix.append(SystemMessage(content=f"User context: {'; '.join(parts)}"))
    return prefix + messages
```

Update `call_model` inside `build_agent` to pass preferences (in Task 6).

Add `load_prefs` and `save_prefs` as module-level functions after `handle_tool_errors`. They accept the asyncpg pool as a second argument (passed via closure in `build_agent`):

```python
async def load_prefs(state: AgentState, pool) -> dict:
    user_id = state.get("user_id", "anonymous")
    if user_id == "anonymous":
        return {}
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT preferred_store_id, customer_email FROM public.user_preferences WHERE user_id = $1",
            user_id,
        )
    if not row:
        return {}
    updates = {}
    if row["preferred_store_id"] is not None:
        updates["preferred_store_id"] = row["preferred_store_id"]
    if row["customer_email"] is not None:
        updates["customer_email"] = row["customer_email"]
    return updates


async def save_prefs(state: AgentState, pool) -> dict:
    import json as _json
    import re as _re
    user_id = state.get("user_id", "anonymous")
    if user_id == "anonymous":
        return {}

    messages = state["messages"]
    tool_messages = [m for m in messages if isinstance(m, ToolMessage)]

    new_store_id = state.get("preferred_store_id")
    new_email = state.get("customer_email")
    changed = False

    for tm in tool_messages:
        try:
            parsed = _json.loads(tm.content) if isinstance(tm.content, str) else {}
            if isinstance(parsed, dict):
                if "store_id" in parsed and parsed["store_id"] != state.get("preferred_store_id"):
                    new_store_id = int(parsed["store_id"])
                    changed = True
                if "email" in parsed and parsed["email"] != state.get("customer_email"):
                    new_email = str(parsed["email"])
                    changed = True
        except (_json.JSONDecodeError, TypeError, ValueError):
            pass

    if not changed:
        return {}

    async with pool.acquire() as conn:
        await conn.execute(
            """INSERT INTO public.user_preferences (user_id, preferred_store_id, customer_email, updated_at)
               VALUES ($1, $2, $3, NOW())
               ON CONFLICT (user_id) DO UPDATE
               SET preferred_store_id = EXCLUDED.preferred_store_id,
                   customer_email = EXCLUDED.customer_email,
                   updated_at = NOW()""",
            user_id, new_store_id, new_email,
        )
    result = {}
    if new_store_id != state.get("preferred_store_id"):
        result["preferred_store_id"] = new_store_id
    if new_email != state.get("customer_email"):
        result["customer_email"] = new_email
    return result
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestUserPreferencesNodes -v
```

Expected: 5 passed.

- [ ] **Step 5: Wire into build_agent**

Update `build_agent` signature and internals:

```python
async def build_agent(psycopg_pool: AsyncConnectionPool, asyncpg_pool=None):
```

Inside `build_agent`, after `bound_model = model.bind_tools(tools)`:

```python
    async def call_model(state: AgentState) -> dict:
        messages = _prepare_messages(
            state["messages"], state.get("summary", ""),
            preferred_store_id=state.get("preferred_store_id"),
            customer_email=state.get("customer_email"),
        )
        response = await bound_model.ainvoke(messages)
        return {"messages": [response]}

    async def _load_prefs_node(state: AgentState) -> dict:
        if asyncpg_pool is None:
            return {}
        return await load_prefs(state, asyncpg_pool)

    async def _save_prefs_node(state: AgentState) -> dict:
        if asyncpg_pool is None:
            return {}
        return await save_prefs(state, asyncpg_pool)
```

Add nodes to graph (after `validate` node):

```python
    graph.add_node("load_prefs", _load_prefs_node)
    graph.add_edge("validate", "load_prefs")    # replaces direct validate → agent edge
    # update validate conditional edges to go to load_prefs not agent:
```

Update the `validate` conditional edges:

```python
    # Before:
    graph.add_conditional_edges("validate", _after_validate, {"agent": "agent", END: END})

    # After:
    graph.add_conditional_edges("validate", _after_validate, {"agent": "load_prefs", END: END})
    graph.add_edge("load_prefs", "agent")
```

Add `save_prefs` node before `handle_errors`:

```python
    graph.add_node("save_prefs", _save_prefs_node)
    graph.add_edge("tools", "save_prefs")        # replaces direct tools → handle_errors edge
    graph.add_edge("save_prefs", "handle_errors")
```

Remove the earlier direct `graph.add_edge("tools", "handle_errors")` added in Task 4.

- [ ] **Step 6: Update main.py to pass asyncpg_pool and user_id**

In `main.py`, update the `build_agent` call in `lifespan`:

```python
    agent_app, mcp_client = await build_agent(psycopg_pool, asyncpg_pool=await init_asyncpg_pool())
```

Wait — `init_asyncpg_pool()` is already called at startup. Update `lifespan`:

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    global agent_app, mcp_client
    print("Starting up: initialising DB connection pools...")
    asyncpg_pool = await init_asyncpg_pool()
    psycopg_pool = await init_psycopg_pool()
    print("Building LangGraph agent...")
    agent_app, mcp_client = await build_agent(psycopg_pool, asyncpg_pool=asyncpg_pool)
    print("Agent ready.")
    yield
    print("Shutting down...")
    if mcp_client:
        await mcp_client.aclose()
    await close_pools()
```

Update `/chat` endpoint to pass `user_id` in initial state:

```python
@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    _require_agent()
    result = await agent_app.ainvoke(
        {"messages": [HumanMessage(content=request.message)], "user_id": request.user_id},
        config={"configurable": {"thread_id": request.thread_id}},
    )
    messages = result["messages"]
    return ChatResponse(
        answer=messages[-1].content,
        tool_calls_made=_tool_names(messages),
    )
```

Update `/chat/stream` endpoint similarly:

```python
@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    _require_agent()
    config = {"configurable": {"thread_id": request.thread_id}}
    return StreamingResponse(
        _stream_agent_events(
            {"messages": [HumanMessage(content=request.message)], "user_id": request.user_id},
            config,
        ),
        media_type="text/event-stream",
    )
```

- [ ] **Step 7: Run full test suite**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: all tests pass (42 + 5 new = 47).

- [ ] **Step 8: Commit**

```bash
cd genai-assistant && git add src/agent.py src/main.py tests/test_api.py
git commit -m "feat: add persistent user preferences with load/save pref nodes"
```

---

## Task 6: Structured Output — Backend SYSTEM_PROMPT

**Files:**
- Modify: `genai-assistant/src/agent.py`

- [ ] **Step 1: Add structured output rules to SYSTEM_PROMPT**

In `agent.py`, append the following to `SYSTEM_PROMPT` (before the closing `"""`):

```python
## Structured output
When returning a list of 2 or more items, output a JSON code block using one of these exact types,
followed by a one-line plain-text summary. Use no other format for lists.

Types and required item fields:
- film_list    → {"type":"film_list","items":[{"title":"...","rating":"...","rental_rate":0.00,"length":0}],"total":N}
- actor_list   → {"type":"actor_list","items":[{"first_name":"...","last_name":"...","film_count":0}],"total":N}
- rental_list  → {"type":"rental_list","items":[{"title":"...","rental_date":"...","return_date":"...","is_outstanding":false}],"total":N}
- customer_list→ {"type":"customer_list","items":[{"first_name":"...","last_name":"...","email":"...","store_id":0}],"total":N}
- store_list   → {"type":"store_list","items":[{"store_id":0,"city":"...","manager":"..."}],"total":N}
"""
```

- [ ] **Step 2: No tests needed** — this is a prompt-only change; correctness is verified end-to-end via the Angular tests in Task 7.

- [ ] **Step 3: Commit**

```bash
cd genai-assistant && git add src/agent.py
git commit -m "feat: add structured output rules to SYSTEM_PROMPT for list responses"
```

---

## Task 7: Structured Output — Angular rendering

**Files:**
- Modify: `ui-angular/src/app/chat.models.ts`
- Modify: `ui-angular/src/app/chat.service.ts`
- Modify: `ui-angular/src/app/chat/chat.component.html`
- Modify: `ui-angular/src/app/chat/chat.module.ts` (add MatTableModule)
- Modify: `ui-angular/src/app/agent.service.ts` (pass user_id)
- Modify: `ui-angular/e2e/chat.spec.ts`

- [ ] **Step 1: Write the failing e2e tests**

In `ui-angular/e2e/chat.spec.ts`, find the SSE streaming test block and add:

```typescript
test('renders film_list JSON block as mat-table', async ({ page }) => {
  await page.route('**/chat/stream', async route => {
    const filmListJson = JSON.stringify({
      type: 'film_list',
      items: [
        { title: 'Academy Dinosaur', rating: 'PG', rental_rate: 0.99, length: 86 },
        { title: 'Ace Goldfinger', rating: 'G', rental_rate: 4.99, length: 48 },
      ],
      total: 2,
    });
    const body = [
      `data: ${JSON.stringify({ type: 'token', content: '```json\n' + filmListJson + '\n```\nHere are 2 films.' })}\n\n`,
      `data: ${JSON.stringify({ type: 'done' })}\n\n`,
    ].join('');
    await route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream' }, body });
  });

  await page.click('button[aria-label="Open AI chat"]');
  await page.fill('textarea', 'show me some films');
  await page.keyboard.press('Enter');
  await page.waitForSelector('mat-table', { timeout: 5000 });
  const rows = await page.locator('mat-row').count();
  expect(rows).toBe(2);
});

test('renders unknown JSON as code block not table', async ({ page }) => {
  await page.route('**/chat/stream', async route => {
    const body = [
      `data: ${JSON.stringify({ type: 'token', content: '```json\n{"foo":"bar"}\n```' })}\n\n`,
      `data: ${JSON.stringify({ type: 'done' })}\n\n`,
    ].join('');
    await route.fulfill({ status: 200, headers: { 'Content-Type': 'text/event-stream' }, body });
  });

  await page.click('button[aria-label="Open AI chat"]');
  await page.fill('textarea', 'hello');
  await page.keyboard.press('Enter');
  await page.waitForTimeout(500);
  const tables = await page.locator('mat-table').count();
  expect(tables).toBe(0);
});
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd ui-angular && npx playwright test e2e/chat.spec.ts --grep "renders film_list" -v
```

Expected: FAIL — no `mat-table` element found.

- [ ] **Step 3: Update chat.models.ts**

Add `StructuredTable` interface and `tableData` field to `ChatMessage`:

```typescript
export interface StructuredTableRow {
  [key: string]: string | number | boolean | null;
}

export interface StructuredTable {
  type: 'film_list' | 'actor_list' | 'rental_list' | 'customer_list' | 'store_list';
  items: StructuredTableRow[];
  total: number;
}

export interface ChatMessage {
  text: string;
  sender: 'user' | 'ai' | 'tool';
  name: string;
  timestamp: Date;
  isStreaming?: boolean;
  tableData?: StructuredTable;
}
```

- [ ] **Step 4: Add JSON detection in chat.service.ts**

In `chat.service.ts`, add a private helper method `_parseTableData`:

```typescript
private _parseTableData(text: string): StructuredTable | undefined {
  const KNOWN_TYPES = new Set(['film_list', 'actor_list', 'rental_list', 'customer_list', 'store_list']);
  const match = text.match(/```json\s*([\s\S]*?)```/);
  if (!match) return undefined;
  try {
    const parsed = JSON.parse(match[1].trim());
    if (parsed && KNOWN_TYPES.has(parsed.type) && Array.isArray(parsed.items)) {
      return parsed as StructuredTable;
    }
  } catch { /* not valid JSON */ }
  return undefined;
}
```

Update the `'done'` case in `_handleSseEvent`:

```typescript
case 'done':
  this._messages.update(msgs => {
    const last = msgs[msgs.length - 1];
    const tableData = this._parseTableData(last.text);
    return [...msgs.slice(0, -1), { ...last, isStreaming: false, tableData }];
  });
  this.isLoading.set(false);
  break;
```

Add `StructuredTable` to the import from `./chat.models`:
```typescript
import { ChatMessage, MessageRecord, SessionHistory, SseEvent, StructuredTable } from './chat.models';
```

- [ ] **Step 5: Add user_id to agent.service.ts**

In `agent.service.ts`, add a private `_userId` getter and pass it in request bodies:

```typescript
private get _userId(): string {
  let id = localStorage.getItem('chat_user_id');
  if (!id) {
    id = 'user-' + Math.random().toString(36).slice(2, 11) + '-' + Date.now();
    localStorage.setItem('chat_user_id', id);
  }
  return id;
}
```

Update `streamChat` body:
```typescript
body: JSON.stringify({ message, thread_id: threadId, user_id: this._userId }),
```

Update `chat` body:
```typescript
return this.http.post<ChatApiResponse>(`${BASE_URL}/chat`, {
  message, thread_id: threadId, user_id: this._userId,
});
```

- [ ] **Step 6: Add mat-table rendering in chat.component.html**

The column sets per type:

```typescript
// Add to chat.component.ts:
readonly TABLE_COLUMNS: Record<string, string[]> = {
  film_list: ['title', 'rating', 'rental_rate', 'length'],
  actor_list: ['first_name', 'last_name', 'film_count'],
  rental_list: ['title', 'rental_date', 'return_date', 'is_outstanding'],
  customer_list: ['first_name', 'last_name', 'email', 'store_id'],
  store_list: ['store_id', 'city', 'manager'],
};

columnsFor(type: string): string[] {
  return this.TABLE_COLUMNS[type] ?? [];
}
```

In `chat.component.html`, replace the AI message content section:

```html
<!-- Content -->
<div class="message-text">
  @if (msg.sender === 'ai') {
    @if (msg.tableData) {
      <mat-table [dataSource]="msg.tableData.items" class="mb-2">
        @for (col of columnsFor(msg.tableData.type); track col) {
          <ng-container [matColumnDef]="col">
            <mat-header-cell *matHeaderCellDef>{{ col }}</mat-header-cell>
            <mat-cell *matCellDef="let row">{{ row[col] }}</mat-cell>
          </ng-container>
        }
        <mat-header-row *matHeaderRowDef="columnsFor(msg.tableData.type)"></mat-header-row>
        <mat-row *matRowDef="let row; columns: columnsFor(msg.tableData.type)"></mat-row>
      </mat-table>
      <small class="text-muted">{{ msg.tableData.total }} result(s)</small>
    }
    <markdown [data]="msg.text"></markdown>
    @if (msg.isStreaming) {<span class="streaming-cursor">▋</span>}
  } @else {
    {{ msg.text }}
  }
</div>
```

- [ ] **Step 7: Import MatTableModule in chat.module.ts**

Locate `ui-angular/src/app/chat/chat.module.ts` and add `MatTableModule` to the imports:

```typescript
import { MatTableModule } from '@angular/material/table';

// In NgModule imports array:
MatTableModule,
```

- [ ] **Step 8: Run e2e tests**

Start the dev server in a separate terminal:
```bash
cd ui-angular && ng serve
```

Run the new tests:
```bash
cd ui-angular && npx playwright test e2e/chat.spec.ts --grep "renders" -v
```

Expected: both tests pass.

- [ ] **Step 9: Run full e2e suite to check for regressions**

```bash
cd ui-angular && npx playwright test
```

Expected: all tests pass (91+ previously).

- [ ] **Step 10: Commit**

```bash
git add ui-angular/src/app/chat.models.ts \
        ui-angular/src/app/chat.service.ts \
        ui-angular/src/app/agent.service.ts \
        ui-angular/src/app/chat/chat.component.ts \
        ui-angular/src/app/chat/chat.component.html \
        ui-angular/e2e/chat.spec.ts
git commit -m "feat: render structured JSON list responses as mat-table in chat UI"
```

---

## Task 8: Update Memory

- [ ] **Step 1: Update todo.md and memory**

Update `.claude/memory/todo.md`:

```markdown
# Task Status — Agentic Features Phase 3

## Setup
- DONE: Spec written (2026-06-02-agentic-features-phase3-design.md)
- DONE: Plan written (2026-06-02-agentic-features-phase3.md)

## Implementation
- DOING: Task 1 — Models
- PENDING: Task 2 — DB schema
- PENDING: Task 3 — Clarification node
- PENDING: Task 4 — Tool error recovery
- PENDING: Task 5 — Persistent preferences
- PENDING: Task 6 — SYSTEM_PROMPT structured output rules
- PENDING: Task 7 — Angular structured output + user_id

## Tests
- PENDING: All backend tests (test_api.py)
- PENDING: Angular e2e tests (chat.spec.ts)

## Review
- PENDING: Final test run + code review
```

- [ ] **Step 2: Commit**

```bash
git add .claude/memory/todo.md
git commit -m "chore: update todo.md for agentic features phase 3"
```
