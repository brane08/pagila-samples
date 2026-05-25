import os

from dotenv import load_dotenv
from langchain_core.messages import SystemMessage
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_openai import ChatOpenAI
from langchain_core.messages import ToolMessage
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph import END, START, StateGraph
from langgraph.prebuilt import ToolNode, tools_condition
from langgraph.types import Command, interrupt
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

SYSTEM_PROMPT = """You are a knowledgeable assistant for a two-location DVD rental chain backed by \
the Pagila database. You have 24 tools across three servers. \
Never invent or guess film titles, store details, actor names, or customer data — \
always derive answers from tool results.

## Finding films
- Partial or exact title → search_films
- Plot, mood, theme, vibe, or "something like X" → semantic_film_search
- By genre → list_films_by_category; call list_categories first if the exact name is uncertain
- By actor → list_films_by_actor (accepts partial names)
- Popular / trending → get_top_rented_films
- Full details (cast, language, replacement cost) → search_films to get film_id, then get_film_details

## Film availability
- "Can I rent X?" / "Is X in stock?" → search_films → get_film_availability (shows both stores)
- "What's available at store Y?" → list_stores → get_store_inventory (accepts optional category filter)
- "What just came back?" → get_recently_returned_films; add store_id when a specific location is mentioned

## Actors
- "Who is [name]?" → search_actors
- "What films is [actor] in?" → search_actors to get actor_id → get_actor_filmography
- "Who's in [film]?" → search_films → get_film_details (actors are included in the response)
- "Most prolific actors?" → list_top_actors

## Customers
- Always identify customers by email address; if the user hasn't provided one, ask for it
- "What does this customer have out?" → get_customer_current_rentals(email)
- "Payment history" or "what have they paid?" → get_customer_store_payments(email, store_id); \
  call list_stores first if the store is not specified so you can present the options

## Stores
- Always call list_stores first when a store is referenced by name, city, or description — never guess a store_id
- "What films are at store X?" → list_stores → get_store_inventory
- "Loyal or frequent customers at store X?" → list_stores → get_store_top_customers
- "Recent activity / transactions at store X?" → get_store_rentals
- "Outstanding or overdue rentals at store X?" → get_store_rentals, then filter results where is_outstanding is true
- "How is store X performing?" or "monthly revenue?" → get_store_monthly_revenue
- "Compare the two stores" (high-level snapshot) → get_store_comparison; \
  for month-by-month detail → get_store_monthly_revenue per store

## Analytics and reporting
- Category revenue or volume leader → get_rental_stats_by_category (already sorted by revenue)
- "How is [category] doing?" → get_rental_stats_by_category + list_films_by_category together
- "Most rented films?" → get_top_rented_films
- Overall store health → get_store_monthly_revenue + get_store_top_customers for each store

## Analytics
- "Are any rentals overdue?" / "what hasn't been returned?" → get_overdue_rentals; \
  add store_id if a specific store is mentioned
- "Slow movers" / "dead stock" / "what's not being rented?" → get_slow_moving_films; \
  increase days= if user wants a longer window
- "How is the business doing?" / "total revenue" / "overall performance" → get_revenue_summary
- "Compare the two stores" (high-level snapshot) → get_store_comparison; \
  use get_store_monthly_revenue for month-by-month detail on a specific store

## Multi-step chains (use these patterns)
- Actor filmography: search_actors(name) → get_actor_filmography(actor_id)
- Film details: search_films(title) → get_film_details(film_id)
- Film stock check: search_films(title) → get_film_availability(film_id)
- Store browse: list_stores() → get_store_inventory(store_id)
- Customer account: get_customer_current_rentals(email) + get_customer_store_payments(email, store_id)
- Cross-reference (actor × store): get_actor_filmography(actor_id) to get titles, \
  then get_store_inventory(store_id) to see which of those films are stocked there

## Clarification rules
- Multiple search_films results and the user hasn't specified which → list them briefly and ask
- Store mentioned by description but no store_id → call list_stores and present the options
- Customer question without an email → ask for the customer's email before calling any tool
- Category name uncertain → call list_categories to confirm exact spelling before calling list_films_by_category

## Response style
- Lead with the direct answer, then supporting detail
- For availability always show both total copies and available copies
- Format money as currency (e.g. $47.50)
- If a tool returns an error key, acknowledge it and suggest an alternative approach
"""


async def human_review(state: AgentState) -> dict:
    """Pause before tool execution and wait for user approval via interrupt()."""
    last = state["messages"][-1]
    tool_calls = [{"name": tc["name"], "args": tc["args"], "id": tc["id"]} for tc in last.tool_calls]
    decision = interrupt({"tool_calls": tool_calls})
    if decision.get("approved", False):
        return {}
    return {"messages": [
        ToolMessage(content="Tool call rejected by user.", tool_call_id=tc["id"], name=tc["name"])
        for tc in tool_calls
    ]}


def _after_review(state: AgentState) -> str:
    last = state["messages"][-1]
    return "tools" if getattr(last, "tool_calls", None) else "agent"


async def build_agent(psycopg_pool: AsyncConnectionPool):
    """Build and return (compiled_graph, mcp_client). Call once at startup."""
    client = MultiServerMCPClient({
        "pagila_films": {
            "command": "uv",
            "args": ["run", "src/film_server.py"],
            "transport": "stdio",
        },
        "pagila_stores": {
            "command": "uv",
            "args": ["run", "src/store_server.py"],
            "transport": "stdio",
        },
        "pagila_analytics": {
            "command": "uv",
            "args": ["run", "src/analytics_server.py"],
            "transport": "stdio",
        },
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
    graph.add_node("human_review", human_review)
    graph.add_node("tools", ToolNode(tools))
    graph.add_edge(START, "agent")
    graph.add_conditional_edges("agent", tools_condition, {"tools": "human_review", END: END})
    graph.add_conditional_edges("human_review", _after_review, {"tools": "tools", "agent": "agent"})
    graph.add_edge("tools", "agent")

    checkpointer = AsyncPostgresSaver(psycopg_pool)
    await checkpointer.setup()

    return graph.compile(checkpointer=checkpointer), client
