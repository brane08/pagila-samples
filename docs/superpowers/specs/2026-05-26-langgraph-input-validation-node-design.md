# LangGraph Input Validation Node — Design

**Date:** 2026-05-26
**Branch:** `genai-assistant`
**Module:** `genai-assistant/`

## Problem

The LangGraph agent currently processes every user message regardless of relevance. Off-topic queries (weather, cooking, sports, etc.) burn LLM + tool-call tokens before the agent can decline them. There is no guard at the graph entry point.

## Goal

Add a `validate` node that runs before the agent on every user turn:
- On-topic messages pass through unchanged to `agent`
- Off-topic messages receive an immediate polite rejection and skip the agent entirely
- Zero tool tokens consumed for rejected queries

## Approach

Pre-agent validation node with LLM structured output (Approach A).

`START → validate → (relevant?) → agent | END`

A module-level `classifier` (the same `model` wrapped with `with_structured_output`) makes a single fast call with a short classification prompt. No new state fields are needed — the result is transient.

## State Changes

None. The validation result does not need to persist across turns. The only state change on rejection is injecting an `AIMessage` reply, which the `add_messages` reducer appends normally.

## New `validate` Node

**File:** `genai-assistant/src/agent.py`

### Additional imports

Two changes to the import block at the top of `agent.py`:

```python
# langchain_core.messages — add AIMessage:
from langchain_core.messages import AIMessage, HumanMessage, RemoveMessage, SystemMessage, ToolMessage

# new top-level import (pydantic is already a project dependency via models.py):
from pydantic import BaseModel
```

### Pydantic model and classifier (module-level, after `KEEP_LAST_N`, before `_prepare_messages`)

```python
class TopicCheck(BaseModel):
    relevant: bool

VALIDATION_PROMPT = (
    "You are a topic classifier for a DVD rental store assistant. "
    "Return relevant=true if the message relates to: DVD films, actors, rentals, "
    "store inventory, customers, pricing, or availability. "
    "Return relevant=false for everything else."
)

classifier = model.with_structured_output(TopicCheck)
```

### Node function (module-level, after `summarize_history`, before `human_review`)

```python
async def validate_input(state: AgentState) -> dict:
    last = state["messages"][-1]
    result: TopicCheck = await classifier.ainvoke([
        SystemMessage(content=VALIDATION_PROMPT),
        HumanMessage(content=str(last.content)),
    ])
    if not result.relevant:
        return {"messages": [AIMessage(content=(
            "I'm a DVD rental assistant — I can help with films, availability, "
            "actors, stores, and customer accounts. "
            "Is there something rental-related I can help you with?"
        ))]}
    return {}
```

Off-topic → injects an `AIMessage` rejection.
On-topic → returns `{}` (state unchanged).

`classifier` uses the global unbound `model` (no tools needed for classification).

### Routing helper (module-level, after `validate_input`, before `human_review`)

```python
def _after_validate(state: AgentState) -> str:
    return END if isinstance(state["messages"][-1], AIMessage) else "agent"
```

On-topic: last message is still the `HumanMessage` → `"agent"`.
Off-topic: rejection `AIMessage` was injected → `END`.

## Graph Wiring

**File:** `genai-assistant/src/agent.py` — `build_agent()`

```python
# Add node
graph.add_node("validate", validate_input)

# Replace: graph.add_edge(START, "agent")
graph.add_edge(START, "validate")

# Add conditional routing out of validate
graph.add_conditional_edges(
    "validate",
    _after_validate,
    {"agent": "agent", END: END},
)
```

### Graph shape after change

```
START → validate → (relevant?) → agent → human_review → tools → (summarize?) → agent → END
                └─── (off-topic) ──────────────────────────────────────────────────── END
```

All existing paths (`human_review`, `summarize`, tool confirmation) are untouched.

## Testing

**File:** `genai-assistant/tests/test_api.py`

New `TestValidationNode` class (no live DB required — mock `agent.classifier`).

### `test_validate_input_on_topic`
- Patch `agent.classifier.ainvoke` to return `TopicCheck(relevant=True)`
- Call `await agent.validate_input(state)` with a HumanMessage state
- Assert result is `{}`

### `test_validate_input_off_topic`
- Patch `agent.classifier.ainvoke` to return `TopicCheck(relevant=False)`
- Assert result has `"messages"` key with a single `AIMessage`
- Assert `AIMessage.content` contains `"DVD rental assistant"`

### `test_after_validate_routes_to_agent`
- Build state with last message as `HumanMessage`
- Assert `_after_validate(state) == "agent"`

### `test_after_validate_routes_to_end`
- Build state with last message as `AIMessage` (simulates injected rejection)
- Assert `_after_validate(state) == END`

## SSE Streaming

No changes to `main.py`. The `validate` node makes no streaming LLM call (structured output uses `ainvoke`, not streaming). Off-topic paths emit `done` immediately after the `tool_confirm` check finds no interrupts and no tokens were streamed — this is correct behaviour.

## Out of Scope

- Configuring `VALIDATION_PROMPT` at runtime via API
- Borderline/ambiguous query handling (interrupt + clarify)
- Logging rejection reasons for analytics
