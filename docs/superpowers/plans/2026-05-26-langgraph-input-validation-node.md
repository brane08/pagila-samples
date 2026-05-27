# LangGraph Input Validation Node Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `validate` node that runs before the `agent` node on every user turn, using LLM structured output to classify queries and immediately reject off-topic ones without burning agent tokens.

**Architecture:** `START → validate → (relevant?) → agent | END`. A module-level `classifier` (the global `model` wrapped with `with_structured_output(TopicCheck)`) makes one fast structured-output call. On-topic queries pass through with no state change; off-topic queries inject a polite `AIMessage` rejection and route directly to `END`. The existing `agent → human_review → tools → summarize` path is untouched.

**Tech Stack:** Python 3.13, LangGraph, LangChain (`with_structured_output`), Pydantic v2, pytest + pytest-asyncio

---

## File Map

| File | Change |
|---|---|
| `genai-assistant/src/agent.py` | Add `AIMessage` import, `BaseModel` import, `TopicCheck`, `VALIDATION_PROMPT`, `classifier`, `validate_input`, `_after_validate`, updated graph wiring |
| `genai-assistant/tests/test_api.py` | Add `TestValidationNode` class (4 tests) |

---

### Task 1: Classifier infrastructure + routing helper

Add the imports, Pydantic model, classifier constant, and `_after_validate` routing helper. These are all pure/synchronous and can be tested without async mocking.

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Test: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write two failing tests**

Append a new `TestValidationNode` class at the END of `genai-assistant/tests/test_api.py`:

```python
# ── Validation node ────────────────────────────────────────────────────────────

class TestValidationNode:
    def test_after_validate_routes_to_agent(self):
        from langchain_core.messages import HumanMessage
        from langgraph.graph import END
        from agent import _after_validate

        state = {"messages": [HumanMessage(content="Do you have Titanic?")], "summary": ""}
        assert _after_validate(state) == "agent"

    def test_after_validate_routes_to_end(self):
        from langchain_core.messages import AIMessage
        from langgraph.graph import END
        from agent import _after_validate

        state = {
            "messages": [AIMessage(content="I'm a DVD rental assistant.")],
            "summary": "",
        }
        assert _after_validate(state) is END
```

- [ ] **Step 2: Run to confirm both fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestValidationNode -v
```

Expected: `FAILED` — `ImportError: cannot import name '_after_validate'`

- [ ] **Step 3: Add imports to `agent.py`**

In `genai-assistant/src/agent.py`:

**Line 4** — add `AIMessage` to the existing langchain_core import:
```python
from langchain_core.messages import AIMessage, HumanMessage, RemoveMessage, SystemMessage, ToolMessage
```

In the imports block, after `from psycopg_pool import AsyncConnectionPool` and before `from models import AgentState`, add:
```python
from pydantic import BaseModel
```

- [ ] **Step 4: Add `TopicCheck`, `VALIDATION_PROMPT`, `classifier`, and `_after_validate` to `agent.py`**

After `KEEP_LAST_N = 4` (line 106) and before `def _prepare_messages` (line 109), insert:

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


def _after_validate(state: AgentState) -> str:
    return END if isinstance(state["messages"][-1], AIMessage) else "agent"
```

- [ ] **Step 5: Run tests to verify both pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestValidationNode -v
```

Expected: `2 passed`

- [ ] **Step 6: Run full suite to check no regressions**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: `28 passed` (26 existing + 2 new)

- [ ] **Step 7: Commit**

```bash
git add genai-assistant/src/agent.py genai-assistant/tests/test_api.py
git commit -m "feat: add TopicCheck classifier and _after_validate routing helper"
```

---

### Task 2: `validate_input` node + async tests

Add the `validate_input` async node function and its two async tests.

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Test: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write two failing async tests**

Add to the `TestValidationNode` class in `genai-assistant/tests/test_api.py`:

```python
    @pytest.mark.asyncio
    async def test_validate_input_on_topic(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=True))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="Do you have Titanic?")], "summary": ""}
        result = await agent.validate_input(state)

        assert result == {}

    @pytest.mark.asyncio
    async def test_validate_input_off_topic(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_classifier = MagicMock()
        mock_classifier.ainvoke = AsyncMock(return_value=agent.TopicCheck(relevant=False))
        monkeypatch.setattr(agent, "classifier", mock_classifier)

        state = {"messages": [HumanMessage(content="What's the weather like?")], "summary": ""}
        result = await agent.validate_input(state)

        assert "messages" in result
        assert len(result["messages"]) == 1
        assert isinstance(result["messages"][0], AIMessage)
        assert "DVD rental assistant" in result["messages"][0].content
```

- [ ] **Step 2: Run to confirm both fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestValidationNode -v -k "validate_input"
```

Expected: `FAILED` — `AttributeError: module 'agent' has no attribute 'validate_input'`

- [ ] **Step 3: Add `validate_input` to `agent.py`**

After `summarize_history` (which ends around line 142) and before `human_review` (line 145), insert:

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

- [ ] **Step 4: Run tests to verify both pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestValidationNode -v -k "validate_input"
```

Expected: `2 passed`

- [ ] **Step 5: Run full suite**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: `30 passed`

- [ ] **Step 6: Commit**

```bash
git add genai-assistant/src/agent.py genai-assistant/tests/test_api.py
git commit -m "feat: implement validate_input node"
```

---

### Task 3: Wire `validate` node into the graph

Replace `graph.add_edge(START, "agent")` with the new node and conditional routing.

**Files:**
- Modify: `genai-assistant/src/agent.py`

- [ ] **Step 1: Update graph wiring inside `build_agent`**

In `genai-assistant/src/agent.py`, inside `build_agent`, find:

```python
    graph.add_edge(START, "agent")
```

Replace that single line with:

```python
    graph.add_node("validate", validate_input)
    graph.add_edge(START, "validate")
    graph.add_conditional_edges(
        "validate",
        _after_validate,
        {"agent": "agent", END: END},
    )
```

- [ ] **Step 2: Run full test suite**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: `30 passed` (0 failures)

- [ ] **Step 3: Commit**

```bash
git add genai-assistant/src/agent.py
git commit -m "feat: wire validate node into LangGraph — pre-agent topic classifier"
```

---

## Verification

After all tasks complete, the full suite must pass:

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected summary:
```
tests/test_api.py::TestValidationNode::test_after_validate_routes_to_agent PASSED
tests/test_api.py::TestValidationNode::test_after_validate_routes_to_end PASSED
tests/test_api.py::TestValidationNode::test_validate_input_on_topic PASSED
tests/test_api.py::TestValidationNode::test_validate_input_off_topic PASSED
... (existing 26 tests) ...
30 passed
```
