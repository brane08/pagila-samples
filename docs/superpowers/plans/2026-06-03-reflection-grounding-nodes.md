# Reflection + Grounding Nodes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two post-answer LangGraph nodes — `reflect_answer` (detects incomplete answers and retries once) and `ground_answer` (detects hallucinated facts and appends a visible warning) — to the existing ReAct graph in `genai-assistant`.

**Architecture:** `reflect_answer` runs after every final agent answer (no tool calls pending), uses a structured `ReflectionCheck` LLM call, and injects a critique `SystemMessage` for one retry if incomplete. `ground_answer` always runs after reflect, uses a structured `GroundingCheck` LLM call against all `ToolMessage` content in state, and appends a `⚠️ Warning:` suffix to the AIMessage if hallucination is detected. No re-run on hallucination — warning is surfaced inline.

**Tech Stack:** Python 3.13, LangGraph `StateGraph`, Pydantic `BaseModel`, `langchain-core` messages, `pytest-asyncio`, existing `model.with_structured_output()` pattern.

**Spec:** `docs/superpowers/specs/2026-06-03-reflection-grounding-nodes-design.md`

---

## File map

| File | Change |
|---|---|
| `genai-assistant/src/models.py` | Add `reflection_retry_count: int` to `AgentState` |
| `genai-assistant/src/agent.py` | Fix `_prepare_messages`; add `ReflectionCheck`, `REFLECTION_PROMPT`, `reflection_classifier`, `reflect_answer`, `_after_reflect`, `GroundingCheck`, `GROUNDING_PROMPT`, `grounding_classifier`, `ground_answer`; update graph wiring |
| `genai-assistant/tests/test_api.py` | Add `TestReflectionNode` (4 tests) and `TestGroundingNode` (3 tests) |

---

### Task 1: Add `reflection_retry_count` to `AgentState`

**Files:**
- Modify: `genai-assistant/src/models.py`
- Test: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the failing test**

Append this class to `tests/test_api.py` (after `TestAgentStateFields`):

```python
# ── Reflection node ────────────────────────────────────────────────────────────

class TestReflectionNode:
    def test_agent_state_has_reflection_retry_count(self):
        from models import AgentState
        state: AgentState = {
            "messages": [],
            "summary": "",
            "tool_retry_count": 0,
            "reflection_retry_count": 0,
            "preferred_store_id": None,
            "customer_email": None,
            "user_id": "anonymous",
        }
        assert state["reflection_retry_count"] == 0
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestReflectionNode::test_agent_state_has_reflection_retry_count -v
```

Expected: `FAILED` — `TypedDict` complains or field is simply missing.

- [ ] **Step 3: Add field to `AgentState` in `models.py`**

In `genai-assistant/src/models.py`, add `reflection_retry_count` after `tool_retry_count`:

```python
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    summary: str
    tool_retry_count: int
    reflection_retry_count: int
    preferred_store_id: int | None
    customer_email: str | None
    user_id: str
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestReflectionNode::test_agent_state_has_reflection_retry_count -v
```

Expected: `PASSED`

- [ ] **Step 5: Commit**

```bash
cd genai-assistant && git add src/models.py tests/test_api.py && git commit -m "feat: add reflection_retry_count to AgentState"
```

---

### Task 2: Implement `reflect_answer` node (TDD)

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Modify: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write 3 more failing tests**

Add these 3 tests inside `TestReflectionNode` in `tests/test_api.py` (after the state field test):

```python
    @pytest.mark.asyncio
    async def test_reflect_complete_passes_through(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ReflectionCheck(complete=True, critique=None)
        )
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [
                HumanMessage(content="What films do you have?"),
                AIMessage(content="We have 1000 films.", id="ai-1"),
            ],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_reflect_incomplete_retries_once(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ReflectionCheck(complete=False, critique="List specific film titles.")
        )
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [
                HumanMessage(content="What action films do you have?"),
                AIMessage(content="We have some action films.", id="ai-1"),
            ],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result["reflection_retry_count"] == 1
        assert "messages" in result
        assert len(result["messages"]) == 1
        assert isinstance(result["messages"][0], SystemMessage)
        assert "List specific film titles." in result["messages"][0].content

    @pytest.mark.asyncio
    async def test_reflect_no_second_retry(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.ReflectionCheck(complete=False, critique="Still vague.")
        )
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [
                HumanMessage(content="What films?"),
                AIMessage(content="Some films.", id="ai-1"),
            ],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 1,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_reflect_noop_when_no_ai_message(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock()
        monkeypatch.setattr(agent, "reflection_classifier", mock_clf)

        state = {
            "messages": [HumanMessage(content="Tell me films")],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.reflect_answer(state)
        assert result == {}
        mock_clf.ainvoke.assert_not_called()
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestReflectionNode -v
```

Expected: 1 PASS (state field test), 3-4 FAILED (`ReflectionCheck` and `reflect_answer` not defined).

- [ ] **Step 3: Fix `_prepare_messages` in `agent.py`**

The current check `if any(isinstance(m, SystemMessage) for m in messages)` would incorrectly skip SYSTEM_PROMPT injection when a critique `SystemMessage` is present. Change the check to be SYSTEM_PROMPT-specific:

In `agent.py`, replace the `_prepare_messages` function body's guard:

```python
def _prepare_messages(
    messages: list, summary: str,
    preferred_store_id: int | None = None,
    customer_email: str | None = None,
) -> list:
    if any(isinstance(m, SystemMessage) and m.content == SYSTEM_PROMPT for m in messages):
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

- [ ] **Step 4: Add `ReflectionCheck`, `REFLECTION_PROMPT`, `reflection_classifier`, `reflect_answer`, `_after_reflect` to `agent.py`**

Add after the `clarification_classifier` line (around line 161), before `_after_clarify`:

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
        description="If incomplete, a one-sentence instruction for the agent to improve its answer.",
    )


REFLECTION_PROMPT = (
    "You are a completeness checker for a DVD rental database assistant. "
    "Given a user question and the assistant's answer, determine if the answer is complete. "
    "An answer is complete if it directly addresses the question with specific data. "
    "An answer is incomplete if it is vague, generic, or claims it cannot find information "
    "without having tried the available tools. "
    "Set complete=false and write a one-sentence critique describing what is missing."
)

reflection_classifier = model.with_structured_output(ReflectionCheck)


async def reflect_answer(state: AgentState) -> dict:
    messages = state["messages"]
    last_ai = next((m for m in reversed(messages) if isinstance(m, AIMessage)), None)
    last_human = next((m for m in reversed(messages) if isinstance(m, HumanMessage)), None)
    if last_ai is None or last_human is None:
        return {}
    retry_count = state.get("reflection_retry_count", 0)
    result: ReflectionCheck = await reflection_classifier.ainvoke([
        SystemMessage(content=REFLECTION_PROMPT),
        HumanMessage(content=f"Question: {last_human.content}\n\nAnswer: {last_ai.content}"),
    ])
    if result.complete or retry_count >= 1:
        return {}
    return {
        "reflection_retry_count": retry_count + 1,
        "messages": [SystemMessage(content=f"Your previous answer was incomplete. {result.critique}")],
    }


def _after_reflect(state: AgentState) -> str:
    last = state["messages"][-1]
    return "agent" if isinstance(last, SystemMessage) else "ground"
```

- [ ] **Step 5: Run reflection tests to verify they pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestReflectionNode -v
```

Expected: 4 PASSED

- [ ] **Step 6: Verify existing tests still pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: 32 PASSED (31 existing + 1 new state field test — the 3 async reflect tests run under `TestReflectionNode` bringing total to 35 PASSED).

- [ ] **Step 7: Commit**

```bash
cd genai-assistant && git add src/agent.py tests/test_api.py && git commit -m "feat: add reflect_answer node with one-retry-on-incomplete logic"
```

---

### Task 3: Implement `ground_answer` node + full graph wiring (TDD)

**Files:**
- Modify: `genai-assistant/src/agent.py`
- Modify: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write 3 failing grounding tests**

Add this class to `tests/test_api.py` (after `TestReflectionNode`):

```python
# ── Grounding node ────────────────────────────────────────────────────────────

class TestGroundingNode:
    @pytest.mark.asyncio
    async def test_ground_clean_answer_no_warning(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.GroundingCheck(hallucinated=False, warning=None)
        )
        monkeypatch.setattr(agent, "grounding_classifier", mock_clf)

        tm = ToolMessage(
            content='[{"title": "Jaws", "rating": "PG"}]',
            tool_call_id="tc1", name="search_films",
        )
        ai = AIMessage(content="We have Jaws rated PG.", id="ai-1")
        state = {
            "messages": [HumanMessage(content="find Jaws"), tm, ai],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.ground_answer(state)
        assert result == {}

    @pytest.mark.asyncio
    async def test_ground_hallucinated_appends_warning(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage, RemoveMessage, ToolMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock(
            return_value=agent.GroundingCheck(
                hallucinated=True,
                warning="Film 'Sharknado' does not appear in tool results.",
            )
        )
        monkeypatch.setattr(agent, "grounding_classifier", mock_clf)

        tm = ToolMessage(
            content='[{"title": "Jaws"}]',
            tool_call_id="tc1", name="search_films",
        )
        ai = AIMessage(content="We have Jaws and Sharknado.", id="ai-1")
        state = {
            "messages": [HumanMessage(content="find shark films"), tm, ai],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.ground_answer(state)
        assert "messages" in result
        msgs = result["messages"]
        assert any(isinstance(m, RemoveMessage) and m.id == "ai-1" for m in msgs)
        new_ai = next(m for m in msgs if isinstance(m, AIMessage))
        assert "⚠️ Warning:" in new_ai.content
        assert "Sharknado" in new_ai.content

    @pytest.mark.asyncio
    async def test_ground_no_tool_messages_skips_check(self, monkeypatch):
        from unittest.mock import AsyncMock, MagicMock
        from langchain_core.messages import AIMessage, HumanMessage
        import agent

        mock_clf = MagicMock()
        mock_clf.ainvoke = AsyncMock()
        monkeypatch.setattr(agent, "grounding_classifier", mock_clf)

        ai = AIMessage(content="Here are some films.", id="ai-1")
        state = {
            "messages": [HumanMessage(content="show films"), ai],
            "summary": "", "tool_retry_count": 0, "reflection_retry_count": 0,
            "preferred_store_id": None, "customer_email": None, "user_id": "anon",
        }
        result = await agent.ground_answer(state)
        assert result == {}
        mock_clf.ainvoke.assert_not_called()
```

- [ ] **Step 2: Run grounding tests to verify they fail**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestGroundingNode -v
```

Expected: 3 FAILED (`GroundingCheck` and `ground_answer` not defined).

- [ ] **Step 3: Add `GroundingCheck`, `GROUNDING_PROMPT`, `grounding_classifier`, `ground_answer` to `agent.py`**

Add after `_after_reflect` (before `_after_clarify`):

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
        description="If hallucinated, a one-sentence description of what could not be verified.",
    )


GROUNDING_PROMPT = (
    "You are a fact-checker for a DVD rental database assistant. "
    "Compare the AI answer against the provided tool results. "
    "Set hallucinated=true ONLY if the answer contains specific facts (film titles, actor names, "
    "prices, counts, dates, store names) that do NOT appear anywhere in the tool results. "
    "Set hallucinated=false if all specific claims are supported by tool results, "
    "or if no tool results are provided."
)

grounding_classifier = model.with_structured_output(GroundingCheck)


async def ground_answer(state: AgentState) -> dict:
    messages = state["messages"]
    last_ai = next((m for m in reversed(messages) if isinstance(m, AIMessage)), None)
    tool_messages = [m for m in messages if isinstance(m, ToolMessage)]
    if last_ai is None or not tool_messages:
        return {}
    tool_content = "\n\n".join(
        f"[{tm.name}]: {tm.content}" for tm in tool_messages
    )[:4000]
    result: GroundingCheck = await grounding_classifier.ainvoke([
        SystemMessage(content=GROUNDING_PROMPT),
        HumanMessage(content=f"Tool results:\n{tool_content}\n\nAI answer:\n{last_ai.content}"),
    ])
    if not result.hallucinated:
        return {}
    warning = result.warning or "Some claims could not be verified against tool results."
    new_content = f"{last_ai.content}\n\n⚠️ Warning: {warning}"
    return {"messages": [RemoveMessage(id=last_ai.id), AIMessage(content=new_content)]}
```

- [ ] **Step 4: Wire `reflect` and `ground` into the graph in `build_agent`**

In `agent.py`, inside `build_agent`, make these two changes:

**Change 1** — route final agent answers to `reflect` instead of `END`:

```python
# Before:
graph.add_conditional_edges("agent", tools_condition, {"tools": "clarify", END: END})

# After:
graph.add_conditional_edges("agent", tools_condition, {"tools": "clarify", END: "reflect"})
```

**Change 2** — add `reflect` and `ground` nodes (add after the `summarize` edge):

```python
graph.add_node("reflect", reflect_answer)
graph.add_conditional_edges("reflect", _after_reflect, {"agent": "agent", "ground": "ground"})
graph.add_node("ground", ground_answer)
graph.add_edge("ground", END)
```

- [ ] **Step 5: Run grounding tests to verify they pass**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py::TestGroundingNode -v
```

Expected: 3 PASSED

- [ ] **Step 6: Run the full test suite**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

Expected: **38 PASSED** (31 original + 4 reflection + 3 grounding).

- [ ] **Step 7: Commit**

```bash
cd genai-assistant && git add src/agent.py tests/test_api.py && git commit -m "feat: add ground_answer node with hallucination warning; wire reflect+ground into graph"
```

---

## Verification

After all tasks, the full graph shape is:

```
START → validate → load_prefs → agent ──(tool_calls)──→ clarify → human_review → tools
                                     ↓                                             ↓
                              (final answer)                         save_prefs → handle_errors
                                     ↓                                             ↓
                                  reflect ←────────────────────── (summarize?) ← agent
                                     ↓ (complete or retry≥1)
                                   ground → END
                                     ↑
                              (retry: agent re-runs once,
                               then routes back to reflect)
```

Run `mvn compile` is not needed (Python only). All 38 tests in `tests/test_api.py` should pass with no live DB required.
