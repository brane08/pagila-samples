from unittest.mock import AsyncMock, MagicMock

import asyncpg
import pytest
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage

import agent
import sessions


def _failing(monkeypatch, attr):
    clf = MagicMock()
    clf.ainvoke = AsyncMock(side_effect=RuntimeError("llm down"))
    monkeypatch.setattr(agent, attr, clf)


@pytest.mark.asyncio
async def test_clarify_fails_open(monkeypatch):
    _failing(monkeypatch, "clarification_classifier")
    ai = AIMessage(content="", id="a", tool_calls=[{"name": "t", "args": {}, "id": "1"}])
    assert await agent.clarify_tool_args({"messages": [ai]}) == {}


@pytest.mark.asyncio
async def test_reflect_fails_open(monkeypatch):
    _failing(monkeypatch, "reflection_classifier")
    state = {"messages": [HumanMessage(content="q"), AIMessage(content="a", id="a")],
             "reflection_retry_count": 0}
    assert await agent.reflect_answer(state) == {}


@pytest.mark.asyncio
async def test_ground_fails_open(monkeypatch):
    _failing(monkeypatch, "grounding_classifier")
    state = {"messages": [HumanMessage(content="q"),
                          ToolMessage(content="x", tool_call_id="1", name="t"),
                          AIMessage(content="a", id="a")]}
    assert await agent.ground_answer(state) == {}


@pytest.mark.asyncio
async def test_summarize_fails_open(monkeypatch):
    monkeypatch.setattr(agent, "model", MagicMock(ainvoke=AsyncMock(side_effect=RuntimeError("down"))))
    msgs = [HumanMessage(content="m0", id="h0")]
    msgs += [HumanMessage(content=f"m{i}", id=f"h{i}") for i in range(1, agent.KEEP_LAST_N + 2)]
    assert await agent.summarize_history({"messages": msgs, "summary": ""}) == {}


class TestRepairDangling:
    def _ai(self, *ids):
        return AIMessage(content="", id="ai", tool_calls=[{"name": "t", "args": {}, "id": i} for i in ids])

    def test_inserts_cancelled_result_right_after_ai(self):
        msgs = [HumanMessage(content="q"), self._ai("1"), HumanMessage(content="next")]
        out = agent._repair_dangling(msgs)
        assert [type(m).__name__ for m in out] == ["HumanMessage", "AIMessage", "ToolMessage", "HumanMessage"]
        assert out[2].tool_call_id == "1"

    def test_only_missing_ids_are_filled(self):
        done = ToolMessage(content="ok", tool_call_id="1", name="t")
        out = agent._repair_dangling([self._ai("1", "2"), done, HumanMessage(content="x")])
        ids = [m.tool_call_id for m in out if isinstance(m, ToolMessage)]
        assert ids == ["1", "2"] or sorted(ids) == ["1", "2"]
        assert len(ids) == 2

    def test_complete_history_unchanged(self):
        msgs = [HumanMessage(content="q"), self._ai("1"),
                ToolMessage(content="ok", tool_call_id="1", name="t"), AIMessage(content="done")]
        assert agent._repair_dangling(msgs) == msgs


class TestSessionsSchema:
    @pytest.mark.asyncio
    async def test_list_sessions_queries_langgraph_schema(self):
        pool = MagicMock()
        pool.fetch = AsyncMock(return_value=[])
        await sessions.list_sessions(pool)
        assert "langgraph.checkpoints" in pool.fetch.call_args[0][0]

    @pytest.mark.asyncio
    async def test_delete_session_uses_langgraph_schema_and_missing_table_is_false(self):
        pool = MagicMock()
        conn = MagicMock()
        conn.execute = AsyncMock(side_effect=asyncpg.UndefinedTableError("missing"))
        conn.transaction = MagicMock(return_value=MagicMock(
            __aenter__=AsyncMock(return_value=None), __aexit__=AsyncMock(return_value=False)))
        pool.acquire = MagicMock(return_value=MagicMock(
            __aenter__=AsyncMock(return_value=conn), __aexit__=AsyncMock(return_value=False)))
        assert await sessions.delete_session(pool, "t1") is False
        assert "langgraph.checkpoint_writes" in conn.execute.call_args[0][0]
