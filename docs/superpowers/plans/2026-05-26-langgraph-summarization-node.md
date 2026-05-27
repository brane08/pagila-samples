# LangGraph Summarization Node Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `summarize` node to the LangGraph graph that trims old messages into a rolling summary when sessions grow beyond 10 messages, keeping the LLM context window manageable.

**Architecture:** After the `tools` node, a conditional edge checks `len(messages) > SUMMARIZE_THRESHOLD`. If true, a `summarize` node condenses the oldest messages using the LLM, stores the result in a new `AgentState.summary` field, and deletes those messages via `RemoveMessage`. On subsequent agent calls, the summary is injected as a second `SystemMessage`. Short sessions are unaffected.

**Tech Stack:** Python 3.13, LangGraph, `langchain_core.messages.RemoveMessage`, FastAPI, pytest + pytest-asyncio

---

## File Map

| File | Change |
|---|---|
| `genai-assistant/src/models.py` | Add `summary: str` field to `AgentState` |
| `genai-assistant/src/agent.py` | New imports, constants, `_prepare_messages`, `summarize_history`, updated `call_model`, updated graph wiring |
| `genai-assistant/tests/test_api.py` | Add `TestSummarizationNode` class with 4 tests |

---

### Task 1: Extend `AgentState` with `summary` field

**Files:**
- Modify: `genai-assistant/src/models.py`
- Test: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the failing test**

Add this to `genai-assistant/tests/test_api.py` (at the bottom, before any existing classes or after imports):

```python
class TestSummarizationNode:
    def test_agent_state_has_summary_annotation(self):
        from models import AgentState
        assert "summary" in AgentState.__annotations__
        assert AgentState.__annotations__["summary"] is str
```

- [ ] **Step 2: Run it to confirm it fails**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestSummarizationNode::test_agent_state_has_summary_annotation -v
```

Expected: `FAILED` — `AssertionError: assert 'summary' in ...`

- [ ] **Step 3: Add `summary` to `AgentState` in `models.py`**

Current content of `AgentState` (lines 18–19 of `genai-assistant/src/models.py`):

```python
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
```

Replace with:

```python
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    summary: str
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestSummarizationNode::test_agent_state_has_summary_annotation -v
```

Expected: `PASSED`

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/models.py genai-assistant/tests/test_api.py
git commit -m "feat: add summary field to AgentState"
```

---

### Task 2: Add `_prepare_messages` helper + tests

`_prepare_messages` is a pure module-level function that prepends the system prompt and optional summary as `SystemMessage`s before the message list. Extracting it makes it directly unit-testable without needing a live LLM or `build_agent`.

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Test: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the three failing tests**

Add to the `TestSummarizationNode` class in `genai-assistant/tests/test_api.py`:

```python
    def test_prepare_messages_injects_summary(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages, SYSTEM_PROMPT

        msgs = [HumanMessage(content="What films are in stock?")]
        result = _prepare_messages(msgs, "User asked about action films earlier.")

        system_msgs = [m for m in result if isinstance(m, SystemMessage)]
        assert len(system_msgs) == 2
        contents = [m.content for m in system_msgs]
        assert any(SYSTEM_PROMPT == c for c in contents)
        assert any("User asked about action films earlier." in c for c in contents)

    def test_prepare_messages_no_summary(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages, SYSTEM_PROMPT

        msgs = [HumanMessage(content="Hello")]
        result = _prepare_messages(msgs, "")

        system_msgs = [m for m in result if isinstance(m, SystemMessage)]
        assert len(system_msgs) == 1
        assert system_msgs[0].content == SYSTEM_PROMPT

    def test_prepare_messages_skips_if_system_present(self):
        from langchain_core.messages import HumanMessage, SystemMessage
        from agent import _prepare_messages

        msgs = [SystemMessage(content="Custom prompt"), HumanMessage(content="Hi")]
        result = _prepare_messages(msgs, "Some summary")

        assert result is msgs  # unchanged — no prepend
```

- [ ] **Step 2: Run to confirm all three fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestSummarizationNode -v -k "prepare_messages"
```

Expected: all 3 `FAILED` — `ImportError: cannot import name '_prepare_messages'`

- [ ] **Step 3: Add imports, constants, and `_prepare_messages` to `agent.py`**

In `genai-assistant/src/agent.py`, merge the two existing `langchain_core.messages` import lines (lines 5–6) into one:

```python
# Before (two separate lines):
from langchain_core.messages import SystemMessage
from langchain_core.messages import ToolMessage

# After (single line, alphabetical):
from langchain_core.messages import HumanMessage, RemoveMessage, SystemMessage, ToolMessage
```

After the `SYSTEM_PROMPT` string (after line 104), add:

```python
SUMMARIZE_THRESHOLD = 10
KEEP_LAST_N = 4


def _prepare_messages(
    messages: list, summary: str
) -> list:
    if any(isinstance(m, SystemMessage) for m in messages):
        return messages
    prefix = [SystemMessage(content=SYSTEM_PROMPT)]
    if summary:
        prefix.append(SystemMessage(content=f"Earlier conversation summary:\n{summary}"))
    return prefix + messages
```

- [ ] **Step 4: Run tests to verify all three pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestSummarizationNode -v -k "prepare_messages"
```

Expected: all 3 `PASSED`

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/agent.py genai-assistant/tests/test_api.py
git commit -m "feat: add _prepare_messages helper and SUMMARIZE_THRESHOLD constants"
```

---

### Task 3: Implement `summarize_history` node + test

`summarize_history` is a module-level async function that uses the global unbound `model` (not `bound_model` — no tools needed for summarization). It produces `RemoveMessage` delete ops for all but the last `KEEP_LAST_N` messages and returns an updated `summary`.

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Test: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the failing test**

Add to `TestSummarizationNode` in `genai-assistant/tests/test_api.py`:

```python
    @pytest.mark.asyncio
    async def test_summarize_history_node(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage, RemoveMessage
        import agent

        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock(
            return_value=MagicMock(content="Session summary text")
        )
        monkeypatch.setattr(agent, "model", mock_model)

        messages = [
            HumanMessage(content=f"msg {i}", id=str(i)) for i in range(12)
        ]
        state = {"messages": messages, "summary": ""}

        result = await agent.summarize_history(state)

        remove_ops = [m for m in result["messages"] if isinstance(m, RemoveMessage)]
        assert len(remove_ops) == 8  # 12 - KEEP_LAST_N(4)
        removed_ids = {m.id for m in remove_ops}
        assert removed_ids == {str(i) for i in range(8)}
        assert result["summary"] == "Session summary text"
```

- [ ] **Step 2: Run to confirm it fails**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestSummarizationNode::test_summarize_history_node -v
```

Expected: `FAILED` — `AttributeError: module 'agent' has no attribute 'summarize_history'`

- [ ] **Step 3: Add `summarize_history` to `agent.py` (module level, after `_prepare_messages`)**

In `genai-assistant/src/agent.py`, add directly after `_prepare_messages`:

```python
async def summarize_history(state: AgentState) -> dict:
    messages = state["messages"]
    existing = state.get("summary", "")
    to_trim = messages[:-KEEP_LAST_N]

    prompt = "Summarize this DVD rental assistant conversation concisely:\n"
    if existing:
        prompt += f"Previous summary: {existing}\n\n"
    prompt += "\n".join(
        f"{type(m).__name__}: {m.content}" for m in to_trim
    )

    response = await model.ainvoke([HumanMessage(content=prompt)])
    deletes = [RemoveMessage(id=m.id) for m in to_trim]
    return {"summary": response.content, "messages": deletes}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestSummarizationNode::test_summarize_history_node -v
```

Expected: `PASSED`

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/agent.py genai-assistant/tests/test_api.py
git commit -m "feat: implement summarize_history node"
```

---

### Task 4: Update `call_model` and wire `summarize` into the graph

Two changes in `build_agent`: (1) replace the `call_model` body to use `_prepare_messages`, (2) replace `graph.add_edge("tools", "agent")` with the new conditional edge.

**Files:**
- Modify: `genai-assistant/src/agent.py`

- [ ] **Step 1: Update `call_model` inside `build_agent`**

Current `call_model` (lines ~148–153 of `genai-assistant/src/agent.py`):

```python
    async def call_model(state: AgentState) -> dict:
        messages = state["messages"]
        if not any(isinstance(m, SystemMessage) for m in messages):
            messages = [SystemMessage(content=SYSTEM_PROMPT)] + messages
        response = await bound_model.ainvoke(messages)
        return {"messages": [response]}
```

Replace with:

```python
    async def call_model(state: AgentState) -> dict:
        messages = _prepare_messages(state["messages"], state.get("summary", ""))
        response = await bound_model.ainvoke(messages)
        return {"messages": [response]}
```

- [ ] **Step 2: Update graph wiring inside `build_agent`**

Current graph edges (near end of `build_agent`):

```python
    graph.add_edge("tools", "agent")
```

Replace that single line with:

```python
    graph.add_node("summarize", summarize_history)
    graph.add_edge("summarize", "agent")
    graph.add_conditional_edges(
        "tools",
        lambda s: "summarize" if len(s["messages"]) > SUMMARIZE_THRESHOLD else "agent",
        {"summarize": "summarize", "agent": "agent"},
    )
```

- [ ] **Step 3: Run the full test suite to verify no regressions**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: all tests pass (was 20 before; now 25 with the 5 new `TestSummarizationNode` tests).

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/agent.py
git commit -m "feat: wire summarize node into LangGraph — post-tool conditional summarization"
```

---

## Verification

After all tasks complete:

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected output summary:
```
tests/test_api.py::TestSummarizationNode::test_agent_state_has_summary_annotation PASSED
tests/test_api.py::TestSummarizationNode::test_prepare_messages_injects_summary PASSED
tests/test_api.py::TestSummarizationNode::test_prepare_messages_no_summary PASSED
tests/test_api.py::TestSummarizationNode::test_prepare_messages_skips_if_system_present PASSED
tests/test_api.py::TestSummarizationNode::test_summarize_history_node PASSED
... (existing 20 tests) ...
25 passed
```
