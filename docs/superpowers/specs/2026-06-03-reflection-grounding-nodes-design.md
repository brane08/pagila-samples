# Reflection + Grounding Nodes Design

**Date:** 2026-06-03
**Branch:** ft/phase2
**File:** `genai-assistant/src/agent.py`, `genai-assistant/src/models.py`

---

## Goal

Add two post-answer LangGraph nodes to the existing ReAct graph:

- **Reflection** — detects incomplete answers and re-invokes the agent once with a critique.
- **Grounding** — checks the final answer against tool results; appends a visible warning if hallucinated facts are detected.

---

## Current graph (abridged)

```
START → validate → load_prefs → agent → clarify → human_review → tools
                                     ↓                              ↓
                                    END                        save_prefs → handle_errors → (summarize?) → agent
```

After `handle_errors` the agent either loops back (retrying) or falls through to `END` — there is no post-answer quality layer.

---

## Reflection Node

### Trigger

Runs on every **final answer** (no tool calls pending, i.e., `tools_condition` returns `END`). The structured check always fires; the retry only fires if `complete=False`.

### Completeness heuristic

A structured LLM call (`ReflectionCheck`) classifies the answer:

```python
class ReflectionCheck(BaseModel):
    complete: bool = Field(
        description=(
            "True if the answer fully addresses the user's question with specific data "
            "(titles, counts, prices, dates). False if it is vague, cuts off, or says "
            "'I don't have that information' when tools were available."
        )
    )
    critique: str | None = Field(
        default=None,
        description="If incomplete, a one-sentence instruction for the agent to improve its answer."
    )
```

The classifier receives the last `HumanMessage` + last `AIMessage` only (not the full history).

### Behaviour

- `complete=True` → pass through to `ground` node unchanged.
- `complete=False, retry_count < 1` → append a `SystemMessage` with the critique, increment `reflection_retry_count`, route back to `agent`.
- `complete=False, retry_count >= 1` → pass through to `ground` node (one retry max, no infinite loop).

### State addition

```python
reflection_retry_count: int   # 0 = not yet retried; 1 = already retried once
```

---

## Grounding Node

### Trigger

Always runs as the final step before `END`, after `reflect` passes the answer through.

### Ground-truth source

All `ToolMessage` objects in `state["messages"]` — the raw tool output is the ground truth.

### Hallucination check

A structured LLM call (`GroundingCheck`):

```python
class GroundingCheck(BaseModel):
    hallucinated: bool = Field(
        description=(
            "True if the AI answer contains specific facts (film titles, actor names, "
            "prices, counts, dates, store names) that do not appear in the tool results. "
            "False if every specific claim is traceable to a tool result, or if no tool "
            "results exist for this turn."
        )
    )
    warning: str | None = Field(
        default=None,
        description="If hallucinated, a one-sentence description of what could not be verified."
    )
```

The classifier receives the final `AIMessage` content + a concatenated summary of all `ToolMessage` content from this turn (truncated to 4000 chars to stay within token budget).

### Behaviour

- `hallucinated=False` → no change to state, route to `END`.
- `hallucinated=True` → mutate the last `AIMessage` by appending `\n\n⚠️ Warning: {warning}` to its content via `RemoveMessage` + new `AIMessage`. Route to `END`.

No re-run on hallucination — warning is surfaced, user can ask again.

---

## Updated graph topology

```
START → validate → load_prefs → agent → clarify → human_review → tools
                                     ↓                              ↓
                                    END (skip)                save_prefs → handle_errors → (summarize?) → agent
                                                                                                        ↓
                                                                                                       END*

* At END: agent → tools_condition → END → reflect → (complete?) → ground → END
                                                  ↘ agent (one retry)
```

Concretely, the routing change:

- `tools_condition` currently routes `END → END`. Change to `END → reflect`.
- `reflect` routes to `agent` (retry) or `ground`.
- `ground` always routes to `END`.

---

## AgentState additions

In `models.py`:

```python
reflection_retry_count: int   # initialized to 0
```

No new fields needed for grounding (uses existing `messages`).

---

## SYSTEM_PROMPT addition

No change required. The reflection critique is injected as a transient `SystemMessage` (not part of the saved summary). The grounding warning is appended directly to the AI message content.

---

## Testing

### New test class: `TestReflectionNode` (in `test_api.py`)

| Test | Scenario |
|---|---|
| `test_reflect_complete_passes_through` | `complete=True` → no retry, routes to ground |
| `test_reflect_incomplete_retries_once` | `complete=False, retry=0` → retry_count becomes 1, agent re-invoked |
| `test_reflect_no_second_retry` | `complete=False, retry=1` → passes through, no second retry |
| `test_reflect_skipped_when_tool_calls_pending` | Agent message has tool_calls → reflect not invoked |

### New test class: `TestGroundingNode` (in `test_api.py`)

| Test | Scenario |
|---|---|
| `test_ground_clean_answer_no_warning` | `hallucinated=False` → AIMessage unchanged |
| `test_ground_hallucinated_appends_warning` | `hallucinated=True` → warning suffix appended to AIMessage |
| `test_ground_no_tool_messages_skips_check` | No ToolMessages in state → grounding returns early (pass-through) |

Total new tests: 7. Expected `test_api.py` total: 38 (was 31).

---

## Architecture documentation

Alongside this implementation, update `.claude/memory/genai_assistant_architecture.md` to:
- List all graph nodes with one-line purpose and routing logic.
- List all 24 MCP tools grouped by server.
- Document `AgentState` fields.
- Record key design decisions (dual pool, SSE filter, one-retry conventions).

This replaces reading `agent.py` at session start.

---

## Out of scope

- No UI change — the warning suffix appears inline in the chat bubble as plain text.
- No new MCP tools.
- No `AgentState.grounding_passed` field — warning is self-contained in the message content.
