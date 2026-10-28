"""Real-graph tests: the compiled LangGraph with a scripted chat model, fake tools and an in-memory checkpointer.

No network, DB or MCP processes. The classifier nodes read module-level classifiers, which are stubbed here.
"""
from typing import Any

import pytest
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.outputs import ChatGeneration, ChatResult
from langchain_core.tools import tool
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import Command
from pydantic import Field

import agent
from sessions import get_session_history


class ScriptedModel(BaseChatModel):
    """Returns the scripted AIMessages in order and records the messages of every call."""

    script: list[Any]
    seen: list[Any] = Field(default_factory=list)

    @property
    def _llm_type(self) -> str:
        return "scripted"

    def bind_tools(self, tools, **kwargs):
        return self

    def _generate(self, messages, stop=None, run_manager=None, **kwargs):
        self.seen.append(list(messages))
        return ChatResult(generations=[ChatGeneration(message=self.script.pop(0))])


class Stub:
    """Classifier stand-in: yields results in order (the last one repeats); an Exception instance is raised."""

    def __init__(self, *results):
        self.results = list(results)
        self.calls = 0

    async def ainvoke(self, _messages):
        result = self.results[min(self.calls, len(self.results) - 1)]
        self.calls += 1
        if isinstance(result, Exception):
            raise result
        return result


TOOL_RUNS: list[str] = []


@tool
async def search_films(title: str) -> str:
    """Search films by title."""
    TOOL_RUNS.append(title)
    return '{"title": "Jaws", "rating": "PG"}'


def _tool_call_msg(title="Jaws"):
    return AIMessage(content="", tool_calls=[{"name": "search_films", "args": {"title": title}, "id": "call-1"}])


@pytest.fixture
def env(monkeypatch):
    TOOL_RUNS.clear()
    stubs = {
        "classifier": Stub(agent.TopicCheck(relevant=True)),
        "clarification_classifier": Stub(agent.ClarificationCheck(needs_clarification=False)),
        "reflection_classifier": Stub(agent.ReflectionCheck(complete=True)),
        "grounding_classifier": Stub(agent.GroundingCheck(hallucinated=False)),
    }
    for name, stub in stubs.items():
        monkeypatch.setattr(agent, name, stub)

    def build(script):
        model = ScriptedModel(script=list(script))
        graph = agent.build_graph([search_films], InMemorySaver(), chat_model=model)
        return graph, model, stubs

    return build


def _cfg(thread="t1"):
    return {"configurable": {"thread_id": thread}, "recursion_limit": agent.RECURSION_LIMIT}


def _ask(text="Tell me about Jaws"):
    return {"messages": [HumanMessage(content=text)], "user_id": "anonymous"}


@pytest.mark.asyncio
async def test_approve_runs_tool_and_answers(env):
    graph, model, _ = env([_tool_call_msg(), AIMessage(content="Jaws is rated PG.")])
    result = await graph.ainvoke(_ask(), _cfg())

    interrupt_value = result["__interrupt__"][0].value
    assert interrupt_value["tool_calls"][0]["name"] == "search_films"
    assert TOOL_RUNS == []

    result = await graph.ainvoke(Command(resume={"approved": True}), _cfg())
    assert TOOL_RUNS == ["Jaws"]
    assert result["messages"][-1].content == "Jaws is rated PG."
    assert any(isinstance(m, ToolMessage) and "PG" in m.content for m in result["messages"])


@pytest.mark.asyncio
async def test_auto_approve_allowlist_skips_the_prompt(env, monkeypatch):
    monkeypatch.setenv("AUTO_APPROVE_TOOLS", "search_films, list_stores")
    graph, _, _ = env([_tool_call_msg(), AIMessage(content="Jaws is rated PG.")])
    result = await graph.ainvoke(_ask(), _cfg("auto1"))

    assert "__interrupt__" not in result
    assert TOOL_RUNS == ["Jaws"]
    assert result["messages"][-1].content == "Jaws is rated PG."


@pytest.mark.asyncio
async def test_auto_approve_needs_every_call_listed(env, monkeypatch):
    monkeypatch.setenv("AUTO_APPROVE_TOOLS", "list_stores")
    graph, _, _ = env([_tool_call_msg(), AIMessage(content="unused")])
    result = await graph.ainvoke(_ask(), _cfg("auto2"))

    assert result["__interrupt__"]
    assert TOOL_RUNS == []


@pytest.mark.asyncio
async def test_reject_skips_tool_and_model_continues(env):
    graph, model, _ = env([_tool_call_msg(), AIMessage(content="Okay, I won't look that up.")])
    await graph.ainvoke(_ask(), _cfg())
    result = await graph.ainvoke(Command(resume={"approved": False}), _cfg())

    assert TOOL_RUNS == []
    rejections = [m for m in result["messages"] if isinstance(m, ToolMessage)]
    assert rejections and "rejected" in rejections[0].content.lower()
    assert result["messages"][-1].content == "Okay, I won't look that up."
    # Every tool call has a result, so the second model request is well-formed.
    assert any(isinstance(m, ToolMessage) for m in model.seen[1])


@pytest.mark.asyncio
async def test_classifier_errors_fail_open(env):
    graph, _, stubs = env([AIMessage(content="We have many films.")])
    boom = RuntimeError("classifier down")
    for name in ("classifier", "reflection_classifier", "grounding_classifier"):
        stubs[name].results = [boom]

    result = await graph.ainvoke(_ask("what films do you have in stock"), _cfg())
    assert result["messages"][-1].content == "We have many films."
    assert stubs["classifier"].calls == 1 and stubs["reflection_classifier"].calls == 1


@pytest.mark.asyncio
async def test_reflection_retry_replaces_draft_and_passes_critique_transiently(env):
    graph, model, stubs = env([AIMessage(content="Vague draft."), AIMessage(content="Specific answer.")])
    stubs["reflection_classifier"].results = [
        agent.ReflectionCheck(complete=False, critique="Name the films."),
        agent.ReflectionCheck(complete=True),
    ]
    result = await graph.ainvoke(_ask(), _cfg())

    contents = [m.content for m in result["messages"]]
    assert "Vague draft." not in contents
    assert contents[-1] == "Specific answer."
    assert not any(isinstance(m, SystemMessage) for m in result["messages"])
    retry_request = model.seen[1]
    assert any(isinstance(m, HumanMessage) and "Name the films." in m.content for m in retry_request)
    assert not any(isinstance(m, HumanMessage) and "[Reviewer note]" in m.content for m in result["messages"])


@pytest.mark.asyncio
async def test_off_topic_message_is_refused_without_calling_the_model(env):
    graph, model, stubs = env([])
    stubs["classifier"].results = [agent.TopicCheck(relevant=False)]
    result = await graph.ainvoke(_ask("write me a poem"), _cfg())

    assert "Sakila" in result["messages"][-1].content
    assert model.seen == []


@pytest.mark.asyncio
async def test_history_renders_user_tool_and_assistant_turns(env):
    graph, _, _ = env([_tool_call_msg(), AIMessage(content="Jaws is rated PG.")])
    await graph.ainvoke(_ask(), _cfg("hist1"))
    await graph.ainvoke(Command(resume={"approved": True}), _cfg("hist1"))

    history = await get_session_history(graph, "hist1")
    roles = [m.role for m in history.messages]
    assert roles[0] == "user"
    assert roles[-1] == "assistant"
    assert "Jaws is rated PG." in history.messages[-1].content
