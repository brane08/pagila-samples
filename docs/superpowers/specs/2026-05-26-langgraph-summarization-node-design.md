# LangGraph Summarization Node — Design

**Date:** 2026-05-26
**Branch:** `genai-assistant`
**Module:** `genai-assistant/`

## Problem

Long sessions accumulate a growing `messages` list in `AgentState`. Every agent turn
sends the full list to the LLM, which wastes tokens and eventually blows the context
window. There is currently no mechanism to trim old messages.

## Goal

Add a `summarize` node to the LangGraph graph that:
- Triggers after tool execution when the message count exceeds a threshold
- Condenses old messages into a rolling `summary` string stored in `AgentState`
- Passes the summary to the LLM on subsequent turns so context is preserved
- Has zero latency impact on short sessions

## Approach

Post-tool conditional summarization (Approach A).

After the `tools` node, a conditional edge checks `len(messages) > SUMMARIZE_THRESHOLD`.
If true, the `summarize` node runs before the next `agent` call. Otherwise the graph
takes the existing direct path back to `agent`.

## State Changes

**File:** `genai-assistant/src/models.py`

Add `summary: str` to `AgentState`:

```python
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    summary: str  # rolling summary of trimmed messages; empty string by default
```

LangGraph initialises missing TypedDict keys to their zero value (`""`), so existing
checkpoints require no migration.

## New `summarize` Node

**File:** `genai-assistant/src/agent.py`

Additional imports required:

```python
from langchain_core.messages import HumanMessage, RemoveMessage
```

Constants (module-level):

```python
SUMMARIZE_THRESHOLD = 10   # trigger when message list exceeds this count
KEEP_LAST_N = 4            # messages to retain after summarisation
```

Node implementation (defined **inside `build_agent`** — it closes over `bound_model`):

```python
async def summarize_history(state: AgentState) -> dict:
    messages = state["messages"]
    existing = state.get("summary", "")
    to_trim = messages[:-KEEP_LAST_N]

    prompt = "Summarize this DVD rental assistant conversation concisely:\n"
    if existing:
        prompt += f"Previous summary: {existing}\n\n"
    prompt += "\n".join(f"{m.__class__.__name__}: {m.content}" for m in to_trim)

    response = await bound_model.ainvoke([HumanMessage(content=prompt)])
    deletes = [RemoveMessage(id=m.id) for m in to_trim]
    return {"summary": response.content, "messages": deletes}
```

`RemoveMessage` is the LangGraph deletion primitive. The `add_messages` reducer applies
the deletions, leaving only the last `KEEP_LAST_N` messages in state.

## `call_model` Changes

When `state.summary` is non-empty, inject it as a second `SystemMessage` before the
trimmed message list:

```python
async def call_model(state: AgentState) -> dict:
    messages = state["messages"]
    summary = state.get("summary", "")
    if not any(isinstance(m, SystemMessage) for m in messages):
        prefix = [SystemMessage(content=SYSTEM_PROMPT)]
        if summary:
            prefix.append(SystemMessage(content=f"Earlier conversation summary:\n{summary}"))
        messages = prefix + messages
    response = await bound_model.ainvoke(messages)
    return {"messages": [response]}
```

## Graph Wiring

**File:** `genai-assistant/src/agent.py` — `build_agent()`

Remove the existing direct `tools → agent` edge. Add the `summarize` node and a
conditional edge:

```python
graph.add_node("summarize", summarize_history)
graph.add_edge("summarize", "agent")

graph.add_conditional_edges(
    "tools",
    lambda s: "summarize" if len(s["messages"]) > SUMMARIZE_THRESHOLD else "agent",
    {"summarize": "summarize", "agent": "agent"},
)
# Remove: graph.add_edge("tools", "agent")
```

### Graph shape after change

```
START → agent → (tools_condition) → human_review → tools → (len > 10?) → summarize → agent
                                                         └──────────────────────────→ agent
              └──────────────────────────────────────────────────────────────────── END
```

The `START → agent → human_review → tools` path is unchanged. Tool confirmation UX is
unaffected.

## Testing

**File:** `genai-assistant/tests/test_api.py` (no live DB required)

### `test_summarize_history_node`

- Build a fake `AgentState` with 12 messages and an empty `summary`
- Mock `bound_model.ainvoke` to return a fixed summary string
- Call `summarize_history(state)` directly
- Assert the returned `messages` list contains exactly 8 `RemoveMessage` ops
  (covering `messages[:-4]`)
- Assert `summary` equals the mocked return value

### `test_call_model_injects_summary`

- Build state with `summary="Prior context"` and 2 messages
- Mock `bound_model.ainvoke`
- Call `call_model(state)` directly
- Assert the messages list passed to `ainvoke` includes a `SystemMessage` whose
  content contains `"Prior context"`
- Assert the `SYSTEM_PROMPT` SystemMessage is also present

## Out of Scope

- Changing `SUMMARIZE_THRESHOLD` or `KEEP_LAST_N` at runtime via API
- Using a cheaper/separate model for summarisation
- Summarising on non-tool turns (e.g. pure conversation chains)
