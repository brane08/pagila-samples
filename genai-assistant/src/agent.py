import os

from dotenv import load_dotenv
from langchain_core.messages import SystemMessage
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph import START, StateGraph
from langgraph.prebuilt import ToolNode, tools_condition
from psycopg_pool import AsyncConnectionPool

from models import AgentState

load_dotenv()

model = ChatOpenAI(
    model=os.getenv("OPENROUTER_MODEL", "mistralai/devstral-2512"),
    api_key=os.getenv("OPENROUTER_API_KEY"),
    base_url=os.getenv("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1"),
    temperature=0,
    default_headers={
        "HTTP-Referer": "http://localhost:8000",
        "X-Title": "Pagila Film Agent",
    },
)

SYSTEM_PROMPT = (
    "You are a helpful DVD rental store assistant with access to the Pagila film "
    "database. You can search for films, look up details, check availability, find "
    "films by category or actor, report on popular rentals, and perform semantic "
    "similarity searches when the user describes a mood, theme, or plot."
)


async def build_agent(psycopg_pool: AsyncConnectionPool):
    """Build and return (compiled_graph, mcp_client). Call once at startup."""
    client = MultiServerMCPClient({
        "pagila_films": {
            "command": "uv",
            "args": ["run", "src/film_server.py"],
            "transport": "stdio",
        }
    })

    tools = await client.get_tools()
    bound_model = model.bind_tools(tools)

    async def call_model(state: AgentState) -> dict:
        messages = state["messages"]
        if not any(isinstance(m, SystemMessage) for m in messages):
            messages = [SystemMessage(content=SYSTEM_PROMPT)] + messages
        response = await bound_model.ainvoke(messages)
        return {"messages": [response]}

    graph = StateGraph(AgentState)
    graph.add_node("agent", call_model)
    graph.add_node("tools", ToolNode(tools))
    graph.add_edge(START, "agent")
    graph.add_conditional_edges("agent", tools_condition)
    graph.add_edge("tools", "agent")

    checkpointer = AsyncPostgresSaver(psycopg_pool)
    await checkpointer.setup()

    return graph.compile(checkpointer=checkpointer), client
