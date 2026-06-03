# Agentic Features Phase 3 — Design Spec

**Date:** 2026-06-02  
**Branch:** ft/phase2  
**Scope:** genai-assistant (`src/agent.py`, `src/models.py`, `src/main.py`, `ui-angular`)

---

## Overview

Four independent agentic improvements to the LangGraph ReAct agent, implemented in order:

1. **Clarification node** — intercepts ambiguous tool calls and asks the user before proceeding
2. **Tool error recovery** — retry-once then agent-fallback on MCP tool errors
3. **Persistent user preferences** — remember store and customer email across sessions via DB
4. **Structured output** — agent emits typed JSON for list results; Angular renders as `mat-table`

---

## Feature 1 — Clarification Node

### Problem
The agent sometimes calls tools with null or placeholder args instead of asking the user for missing information.

### Graph Change
Insert a `clarify` node between `agent` and `human_review`:

```
validate → agent → clarify → human_review → tools
                ↑____________↓  (re-route when args incomplete)
```

### Implementation

**New Pydantic model:**
```python
class ClarificationCheck(BaseModel):
    needs_clarification: bool
    question: str | None = Field(default=None, description="Question to ask the user")
```

**New module-level classifier:**
```python
clarification_classifier = model.with_structured_output(ClarificationCheck)
```

**`clarify` node** (`async def clarify_tool_args(state)`):
- Reads the last `AIMessage` from state; if it has no `tool_calls`, passes through immediately
- Extracts tool name + args from the pending tool call
- Calls `clarification_classifier` with the tool schema and args to detect null/missing required values
- If `needs_clarification=True`: `interrupt({"question": question})` — Angular receives this as a normal AI message via SSE
- On resume: the interrupt return value (user's answer) is appended as a `HumanMessage`, routes back to `agent`
- If `needs_clarification=False`: passes through to `human_review`

**No new `AgentState` fields** — the injected `HumanMessage` carries the clarification context forward.

**`_after_clarify` routing helper:**
```python
def _after_clarify(state) -> str:
    last = state["messages"][-1]
    return "agent" if isinstance(last, HumanMessage) else "human_review"
```

**Graph edges:**
```python
graph.add_node("clarify", clarify_tool_args)
graph.add_conditional_edges("agent", tools_condition, {"tools": "clarify", END: END})
graph.add_conditional_edges("clarify", _after_clarify, {"human_review": "human_review", "agent": "agent"})
```

### Tests
- `test_clarify_passes_through_when_args_complete` — classifier returns `needs_clarification=False`, routes to `human_review`
- `test_clarify_interrupts_when_args_missing` — classifier returns `needs_clarification=True`, interrupt called with question
- `test_clarify_skips_when_no_tool_calls` — AIMessage without tool_calls, passes through immediately

---

## Feature 2 — Tool Error Recovery

### Problem
MCP tool errors surface raw to the agent. The agent should retry once silently, then fall back to a visible error message for alternative strategy.

### Graph Change
Replace the lambda edge from `tools` with a `handle_tool_errors` node:

```
tools → handle_tool_errors → summarize/agent  (no error)
                           → agent + hint      (first error, retry_count → 1)
                           → agent + visible   (second error, retry_count → 0)
```

### `AgentState` Change
```python
tool_retry_count: int  # default 0
```

### `handle_tool_errors` node logic
1. Collect all `ToolMessage`s from the latest tool execution round
2. Check each message's content (parsed as JSON if possible) for an `"error"` key
3. **No errors**: reset `tool_retry_count = 0`, route to existing summarize/agent logic
4. **Error + `tool_retry_count == 0`**: replace error `ToolMessage` content with `"Tool call failed. Please try an alternative approach."`, set `tool_retry_count = 1`, route to `agent`
5. **Error + `tool_retry_count >= 1`**: reset `tool_retry_count = 0`, leave error `ToolMessage` visible, route to `agent`

### Tests
- `test_no_error_resets_retry_count` — clean ToolMessages, retry_count resets to 0
- `test_first_error_silently_retries` — error ToolMessage replaced with hint, retry_count set to 1, routes to agent
- `test_second_error_exposes_error` — retry_count=1 + error, reset to 0, original error visible, routes to agent
- `test_error_detection_handles_non_json_content` — non-JSON tool content treated as no error

---

## Feature 3 — Persistent User Preferences

### Problem
Users must re-identify their preferred store and email on every new session.

### DB Schema
New table in `public` schema (added to `database/schema.sql`):
```sql
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id TEXT PRIMARY KEY,
    preferred_store_id INT,
    customer_email TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### API Change
`ChatRequest` gains `user_id: str = "anonymous"`.

Angular generates a UUID v4 on first load, stores in `localStorage`, and includes it in every `/chat` and `/chat/stream` POST body.

### `AgentState` Change
```python
preferred_store_id: int | None   # default None
customer_email: str | None       # default None
user_id: str                     # default "anonymous", not checkpointed
```

`user_id` is passed per-invocation (not persisted in LangGraph checkpoint).

### Graph additions

**`load_prefs` node** (after `validate`, before `agent`):
- Reads `user_preferences` table by `state["user_id"]`
- Sets `preferred_store_id` and `customer_email` in state if found

**`save_prefs` node** (after `tools`, before `handle_tool_errors`):
- Scans latest `ToolMessage`s for newly resolved `store_id` integers and email strings
- If found and different from current state: upserts to `user_preferences`, updates state

**Graph flow:**
```
validate → load_prefs → agent → clarify → human_review → tools → save_prefs → handle_tool_errors → ...
```

### SYSTEM_PROMPT injection
`_prepare_messages` injects a preference context line when fields are populated:
```
User context: preferred store = Store 1 (Lethbridge); known customer email = jane@example.com
```

### DB connection
`load_prefs` and `save_prefs` use the existing `asyncpg` pool (passed via `build_agent`).

### Tests
- `test_load_prefs_hydrates_state` — mock asyncpg returns row, state fields populated
- `test_load_prefs_noop_for_unknown_user` — no row returned, state unchanged
- `test_save_prefs_upserts_on_new_store_id` — tool result contains store_id, upsert called
- `test_save_prefs_skips_when_unchanged` — same store_id already in state, no DB call
- `test_prepare_messages_injects_preference_context` — both fields populated, context line present

---

## Feature 4 — Structured Output

### Problem
List results (films, actors, rentals) are returned as markdown prose; Angular cannot render them as sortable/scannable tables.

### Approach
Agent-decided: the agent emits a JSON code block for list results; Angular detects and renders it.

### SYSTEM_PROMPT addition
```
When returning a list of 2 or more items, output a JSON code block using one of these types:
  film_list, actor_list, rental_list, customer_list, store_list
Format: {"type": "<type>", "items": [...], "total": <n>}
Follow the JSON block with a one-line plain-text summary.
```

### Known column sets (Angular)
| type | columns |
|---|---|
| `film_list` | title, rating, rental_rate, length |
| `actor_list` | first_name, last_name, film_count |
| `rental_list` | title, rental_date, return_date, is_outstanding |
| `customer_list` | first_name, last_name, email, store_id |
| `store_list` | store_id, city, manager |

### Angular change (`chat.component.ts`)
- After `done` SSE event, scan assembled message content for ` ```json ` blocks
- Parse JSON; if `type` matches a known type, render `<mat-table>` with fixed columns
- Non-matching JSON blocks fall through to `ngx-markdown` unchanged

No new SSE events or API changes. JSON rides inside existing `token` events.

### Tests
- `chat.spec.ts`: mock SSE stream returning a `film_list` JSON block → `mat-table` rendered
- `chat.spec.ts`: unknown JSON type → rendered as code block by ngx-markdown
- No backend tests needed (prompt-only change)

---

## Graph After All 4 Features

```
START
  → validate
  → load_prefs
  → agent
  → clarify          (new: intercept missing args)
  → human_review     (existing: user approval)
  → tools
  → save_prefs       (new: persist resolved prefs)
  → handle_tool_errors  (new: replaces lambda edge)
  → summarize / agent
  → END
```

---

## Files Changed

| File | Change |
|---|---|
| `src/agent.py` | +`ClarificationCheck`, +`clarify_tool_args`, +`handle_tool_errors`, +`load_prefs`, +`save_prefs`, update `build_agent` graph, update `_prepare_messages`, update `SYSTEM_PROMPT` |
| `src/models.py` | +`tool_retry_count`, +`preferred_store_id`, +`customer_email`, +`user_id` to `AgentState`; +`user_id` to `ChatRequest` |
| `src/main.py` | Thread `user_id` from request into graph invocation config |
| `database/schema.sql` | +`user_preferences` table |
| `ui-angular/src/.../chat.component.ts` | Generate/load `user_id`, pass in request, detect JSON blocks, render `mat-table` |
| `tests/test_api.py` | +12 new tests across 4 features |
| `ui-angular/e2e/chat.spec.ts` | +structured output table rendering tests |
