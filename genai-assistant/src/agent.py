import os

from dotenv import load_dotenv
from langchain_core.messages import AIMessage, HumanMessage, RemoveMessage, SystemMessage, ToolMessage
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph import END, START, StateGraph
from langgraph.prebuilt import ToolNode, tools_condition
from langgraph.types import Command, interrupt
from psycopg_pool import AsyncConnectionPool
from pydantic import BaseModel, Field

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

## Structured output
When returning a list of 2 or more items, output a JSON code block using one of these exact types,
followed by a one-line plain-text summary. Use no other format for lists.

Types and required item fields:
- film_list    → {"type":"film_list","items":[{"title":"...","rating":"...","rental_rate":0.00,"length":0}],"total":N}
- actor_list   → {"type":"actor_list","items":[{"first_name":"...","last_name":"...","film_count":0}],"total":N}
- rental_list  → {"type":"rental_list","items":[{"title":"...","rental_date":"...","return_date":"...","is_outstanding":false}],"total":N}
- customer_list→ {"type":"customer_list","items":[{"first_name":"...","last_name":"...","email":"...","store_id":0}],"total":N}
- store_list   → {"type":"store_list","items":[{"store_id":0,"city":"...","manager":"..."}],"total":N}
"""

SUMMARIZE_THRESHOLD = 10
KEEP_LAST_N = 4


class TopicCheck(BaseModel):
    relevant: bool = Field(
        description=(
            "True ONLY if the message asks about data in the Sakila DVD rental database: "
            "films, actors, rentals, store inventory, customers, payments, staff, or categories. "
            "False for general knowledge, SQL syntax, programming help, real-world movie news, "
            "weather, cooking, or anything not stored in the Sakila database."
        )
    )

VALIDATION_PROMPT = (
    "You are a topic classifier for a Sakila DVD rental database assistant. "
    "The assistant ONLY answers questions about data stored in the Sakila database: "
    "films, actors, rental history, store inventory, customers, payments, staff, and categories. "
    "Return relevant=true ONLY for questions that query or explain Sakila data. "
    "Return relevant=false for: general knowledge, SQL syntax help, programming questions, "
    "real-world movie reviews or recommendations outside the database, database administration, "
    "weather, cooking, or anything not directly about Sakila data."
)

classifier = model.with_structured_output(TopicCheck)


class ClarificationCheck(BaseModel):
    needs_clarification: bool = Field(
        description="True if any required tool argument is null, empty, or missing from the conversation context."
    )
    question: str | None = Field(
        default=None,
        description="The specific question to ask the user to obtain the missing argument value."
    )

CLARIFICATION_PROMPT = (
    "You are a tool argument validator for a DVD rental database assistant. "
    "Given a tool name and its arguments, determine if any required argument is null or missing. "
    "Required arguments that are commonly missing: 'email' for customer tools, 'store_id' when a specific store is needed. "
    "If an argument is null/None/empty, set needs_clarification=true and write a specific question to ask the user. "
    "If all required arguments have values, set needs_clarification=false."
)

clarification_classifier = model.with_structured_output(ClarificationCheck)


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


def _after_clarify(state: AgentState) -> str:
    last = state["messages"][-1]
    if isinstance(last, AIMessage) and not getattr(last, "tool_calls", None):
        return END
    return "human_review"


def _after_validate(state: AgentState) -> str:
    if not state["messages"]:
        return "agent"
    return END if isinstance(state["messages"][-1], AIMessage) else "agent"


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


async def summarize_history(state: AgentState) -> dict:
    messages = state["messages"]
    if len(messages) <= KEEP_LAST_N:
        return {}
    existing = state.get("summary", "")
    keep_from = len(messages) - KEEP_LAST_N
    while keep_from > 0 and not isinstance(messages[keep_from], HumanMessage):
        keep_from -= 1
    to_trim = messages[:keep_from]
    if not to_trim:
        return {}

    prompt = "Summarize this DVD rental assistant conversation concisely:\n"
    if existing:
        prompt += f"Previous summary: {existing}\n\n"
    prompt += "\n".join(
        f"{type(m).__name__}: {m.content if isinstance(m.content, str) else str(m.content)}"
        for m in to_trim
    )

    response = await model.ainvoke([HumanMessage(content=prompt)])
    deletes = [RemoveMessage(id=m.id) for m in to_trim]
    return {"summary": response.content, "messages": deletes}


async def validate_input(state: AgentState) -> dict:
    if not state["messages"]:
        return {}
    last = state["messages"][-1]
    result: TopicCheck = await classifier.ainvoke([
        SystemMessage(content=VALIDATION_PROMPT),
        HumanMessage(content=str(last.content)),
    ])
    if not result.relevant:
        return {"messages": [AIMessage(content=(
            "I'm a Sakila DVD rental assistant — I can only answer questions about "
            "films, actors, rentals, store inventory, customers, and payments in the Sakila database. "
            "Is there something about the rental data I can help you with?"
        ))]}
    return {}


async def clarify_tool_args(state: AgentState) -> dict:
    import json as _json
    last = state["messages"][-1]
    if not getattr(last, "tool_calls", None):
        return {}
    tool_call = last.tool_calls[0]
    result: ClarificationCheck = await clarification_classifier.ainvoke([
        SystemMessage(content=CLARIFICATION_PROMPT),
        HumanMessage(content=f"Tool: {tool_call['name']}\nArgs: {_json.dumps(tool_call['args'])}"),
    ])
    if result.needs_clarification:
        return {"messages": [
            RemoveMessage(id=last.id),
            AIMessage(content=result.question or "Could you provide more details?"),
        ]}
    return {}


async def handle_tool_errors(state: AgentState) -> dict:
    import json as _json
    messages = state["messages"]
    retry_count = state.get("tool_retry_count", 0)

    tool_messages = [m for m in messages if isinstance(m, ToolMessage)]
    error_messages = []
    for tm in tool_messages:
        try:
            parsed = _json.loads(tm.content) if isinstance(tm.content, str) else {}
            if isinstance(parsed, dict) and "error" in parsed:
                error_messages.append(tm)
        except (_json.JSONDecodeError, TypeError):
            pass

    if not error_messages:
        return {"tool_retry_count": 0}

    if retry_count == 0:
        replacements = []
        for tm in error_messages:
            replacements.append(RemoveMessage(id=tm.id))
            replacements.append(ToolMessage(
                content="Tool call failed. Please try an alternative approach.",
                tool_call_id=tm.tool_call_id,
                name=tm.name,
            ))
        return {"tool_retry_count": 1, "messages": replacements}

    return {"tool_retry_count": 0}


async def load_prefs(state: AgentState, pool) -> dict:
    user_id = state.get("user_id", "anonymous")
    if user_id == "anonymous":
        return {}
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT preferred_store_id, customer_email FROM public.user_preferences WHERE user_id = $1",
            user_id,
        )
    if not row:
        return {}
    updates = {}
    if row["preferred_store_id"] is not None:
        updates["preferred_store_id"] = row["preferred_store_id"]
    if row["customer_email"] is not None:
        updates["customer_email"] = row["customer_email"]
    return updates


async def save_prefs(state: AgentState, pool) -> dict:
    import json as _json
    user_id = state.get("user_id", "anonymous")
    if user_id == "anonymous":
        return {}

    messages = state["messages"]
    tool_messages = [m for m in messages if isinstance(m, ToolMessage)]

    new_store_id = state.get("preferred_store_id")
    new_email = state.get("customer_email")
    changed = False

    for tm in tool_messages:
        try:
            parsed = _json.loads(tm.content) if isinstance(tm.content, str) else {}
            if isinstance(parsed, dict):
                if "store_id" in parsed and parsed["store_id"] != state.get("preferred_store_id"):
                    new_store_id = int(parsed["store_id"])
                    changed = True
                if "email" in parsed and parsed["email"] != state.get("customer_email"):
                    new_email = str(parsed["email"])
                    changed = True
        except (_json.JSONDecodeError, TypeError, ValueError):
            pass

    if not changed:
        return {}

    async with pool.acquire() as conn:
        await conn.execute(
            """INSERT INTO public.user_preferences (user_id, preferred_store_id, customer_email, updated_at)
               VALUES ($1, $2, $3, NOW())
               ON CONFLICT (user_id) DO UPDATE
               SET preferred_store_id = EXCLUDED.preferred_store_id,
                   customer_email = EXCLUDED.customer_email,
                   updated_at = NOW()""",
            user_id, new_store_id, new_email,
        )
    result = {}
    if new_store_id != state.get("preferred_store_id"):
        result["preferred_store_id"] = new_store_id
    if new_email != state.get("customer_email"):
        result["customer_email"] = new_email
    return result


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


async def build_agent(psycopg_pool: AsyncConnectionPool, asyncpg_pool=None):
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
        messages = _prepare_messages(
            state["messages"], state.get("summary", ""),
            preferred_store_id=state.get("preferred_store_id"),
            customer_email=state.get("customer_email"),
        )
        response = await bound_model.ainvoke(messages)
        return {"messages": [response]}

    async def _load_prefs_node(state: AgentState) -> dict:
        if asyncpg_pool is None:
            return {}
        return await load_prefs(state, asyncpg_pool)

    async def _save_prefs_node(state: AgentState) -> dict:
        if asyncpg_pool is None:
            return {}
        return await save_prefs(state, asyncpg_pool)

    graph = StateGraph(AgentState)
    graph.add_node("agent", call_model)
    graph.add_node("human_review", human_review)
    graph.add_node("tools", ToolNode(tools))
    graph.add_node("summarize", summarize_history)
    graph.add_edge("summarize", "agent")
    graph.add_node("reflect", reflect_answer)
    graph.add_conditional_edges("reflect", _after_reflect, {"agent": "agent", "ground": "ground"})
    graph.add_node("ground", ground_answer)
    graph.add_edge("ground", END)
    graph.add_node("validate", validate_input)
    graph.add_edge(START, "validate")
    graph.add_conditional_edges(
        "validate",
        _after_validate,
        {"agent": "load_prefs", END: END},
    )
    graph.add_node("load_prefs", _load_prefs_node)
    graph.add_edge("load_prefs", "agent")
    graph.add_node("clarify", clarify_tool_args)
    graph.add_conditional_edges("agent", tools_condition, {"tools": "clarify", END: "reflect"})
    graph.add_conditional_edges("clarify", _after_clarify, {"human_review": "human_review", END: END})
    graph.add_conditional_edges("human_review", _after_review, {"tools": "tools", "agent": "agent"})
    graph.add_node("save_prefs", _save_prefs_node)
    graph.add_edge("tools", "save_prefs")
    graph.add_node("handle_errors", handle_tool_errors)
    graph.add_edge("save_prefs", "handle_errors")
    graph.add_conditional_edges(
        "handle_errors",
        lambda s: "summarize" if len(s["messages"]) > SUMMARIZE_THRESHOLD else "agent",
        {"summarize": "summarize", "agent": "agent"},
    )

    checkpointer = AsyncPostgresSaver(psycopg_pool)
    await checkpointer.setup()

    return graph.compile(checkpointer=checkpointer), client
