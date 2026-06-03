---
name: genai-assistant-architecture
description: "Python FastAPI + LangGraph agent over Pagila DB using MCP tools, OpenRouter LLM, and pgvector RAG; Angular chat UI fully implemented as MatDialog in ui-angular"
type: project
---

Active branch: `ft/phase2` / `genai-assistant`.
Angular chat UI is embedded **inside `ui-angular`** (not a separate module).

**Why:** Demonstrates AI-first database access pattern using agentic tooling (MCP),
streaming (SSE), and vector search (RAG) over an existing relational DB.

---

## File map (genai-assistant/src/)

| File | Role |
|---|---|
| `main.py` | FastAPI app — endpoints, SSE streaming, startup wiring |
| `agent.py` | LangGraph graph — all nodes, routing logic, `build_agent()` |
| `models.py` | `AgentState`, `ChatRequest/Response`, session/admin models |
| `db.py` | Dual asyncpg + psycopg3 pools |
| `sessions.py` | Session CRUD over LangGraph checkpoint tables |
| `film_server.py` | FastMCP server — film, actor, rental tools (14 tools) |
| `store_server.py` | FastMCP server — store tools (6 tools) |
| `analytics_server.py` | FastMCP server — analytics/reporting tools (4 tools) |
| `rag.py` | FastEmbed + PGVector seed script |

---

## AgentState fields (models.py)

| Field | Type | Purpose |
|---|---|---|
| `messages` | `list[BaseMessage]` | Full message history (add_messages reducer) |
| `summary` | `str` | Rolling summary injected as SystemMessage when non-empty |
| `tool_retry_count` | `int` | Counts tool error retries; reset to 0 on success |
| `preferred_store_id` | `int \| None` | Loaded from user_preferences; injected into system context |
| `customer_email` | `str \| None` | Loaded from user_preferences; injected into system context |
| `user_id` | `str` | Passed in ChatRequest; used for preference lookup (default: "anonymous") |
| `reflection_retry_count` | `int` | Counts reflection retries; max 1 |

---

## LangGraph graph — full node reference

### Graph topology

```
START → validate → load_prefs → agent ──(tool_calls?)──→ clarify ──(missing args?)──→ END
                                     ↓                                               ↓
                                    END (off-topic)                          human_review ──(approved?)──→ tools
                                                                                                           ↓
                                                                                               save_prefs → handle_errors
                                                                                                           ↓
                                                                                               (len>10?) → summarize → agent
                                                                                                        ↓
                                                                                                       agent
                (no tool_calls — final answer)
                agent ──→ reflect ──(incomplete, retry<1?)──→ agent
                                  ↓ (complete or retry≥1)
                                ground ──→ END
```

### Node reference

| Node | Function | Routing output |
|---|---|---|
| `validate` | `validate_input` — structured `TopicCheck`; rejects off-topic queries with a polite AIMessage | `agent` (relevant) / `END` (off-topic) |
| `load_prefs` | `load_prefs` — reads `user_preferences` table via asyncpg; populates `preferred_store_id`, `customer_email` | → `agent` (always) |
| `agent` | `call_model` — `bound_model.ainvoke(_prepare_messages(...))` | `tools_condition`: → `clarify` (tool_calls) / `reflect` (final answer) |
| `clarify` | `clarify_tool_args` — structured `ClarificationCheck`; removes the tool-call AIMessage and replaces with a question if args are missing | → `human_review` (args ok) / `END` (question sent to user) |
| `human_review` | `human_review` — `interrupt()` pauses graph; waits for `POST /chat/confirm/{id}/stream` with `{approved: bool}` | → `tools` (approved) / `agent` (rejected — injects ToolMessage "rejected") |
| `tools` | `ToolNode(tools)` — executes MCP tool calls | → `save_prefs` (always) |
| `save_prefs` | `save_prefs` — upserts `user_preferences` if store_id or email changed in tool results | → `handle_errors` (always) |
| `handle_errors` | `handle_tool_errors` — detects `{"error": ...}` in ToolMessages; replaces with "try alternative" message; max 1 retry | → `summarize` (len>10) / `agent` |
| `summarize` | `summarize_history` — trims oldest messages into `state["summary"]`; keeps last 4, walks back to HumanMessage boundary | → `agent` (always) |
| `reflect` | `reflect_answer` — structured `ReflectionCheck(complete, critique)`; runs only when agent produces final answer | → `ground` (complete / retry≥1) / `agent` (incomplete + retry<1) |
| `ground` | `ground_answer` — structured `GroundingCheck(hallucinated, warning)`; appends `⚠️ Warning:` suffix to AIMessage if hallucinated | → `END` (always) |

### Key routing constants

```python
SUMMARIZE_THRESHOLD = 10   # message count above which summarize node runs
KEEP_LAST_N = 4            # messages kept after summarization
```

### `_prepare_messages` helper

Called by `call_model` before every LLM invoke. Prepends:
1. `SystemMessage(SYSTEM_PROMPT)` — always
2. `SystemMessage(f"Earlier conversation summary:\n{summary}")` — if summary non-empty
3. `SystemMessage(f"User context: preferred store ID = {id}; known customer email = {email}")` — if either pref set

Skipped entirely if messages already contain a SystemMessage (prevents double-injection on resume).

---

## MCP tools — full reference

### pagila_films (film_server.py) — 14 tools

| Tool | Signature | Purpose |
|---|---|---|
| `search_films` | `(title: str, limit=10)` | Partial/exact title search |
| `get_film_details` | `(film_id: int)` | Full record: cast, language, cost, description |
| `list_films_by_category` | `(category: str, limit=10)` | Films by genre |
| `list_films_by_actor` | `(actor_name: str, limit=10)` | Films by partial actor name |
| `get_top_rented_films` | `(limit=10)` | Most-rented films overall |
| `list_categories` | `()` | All category names (exact spelling) |
| `get_film_availability` | `(film_id: int)` | Copies + available copies per store |
| `semantic_film_search` | `(query: str, limit=5)` | pgvector similarity search via FastEmbed |
| `search_actors` | `(name: str, limit=10)` | Actor search by partial name |
| `get_actor_filmography` | `(actor_id: int)` | All films for an actor |
| `list_top_actors` | `(limit=10)` | Actors by film count |
| `get_customer_current_rentals` | `(email: str)` | Active + overdue rentals for a customer |
| `get_rental_stats_by_category` | `()` | Revenue + rental count per category |
| `get_recently_returned_films` | `(limit=10, store_id=None)` | Recently returned films, optionally by store |

### pagila_stores (store_server.py) — 6 tools

| Tool | Signature | Purpose |
|---|---|---|
| `list_stores` | `()` | All stores with city, manager, address |
| `get_store_inventory` | `(store_id: int, category="", limit=20)` | Films stocked at a store |
| `get_store_rentals` | `(store_id: int, limit=20)` | Recent rental activity at a store |
| `get_store_top_customers` | `(store_id: int, limit=10)` | Most active customers at a store |
| `get_customer_store_payments` | `(customer_email: str, store_id: int)` | Payment history for a customer at a store |
| `get_store_monthly_revenue` | `(store_id: int)` | Month-by-month revenue for a store |

### pagila_analytics (analytics_server.py) — 4 tools

| Tool | Signature | Purpose |
|---|---|---|
| `get_overdue_rentals` | `(store_id=None, limit=20)` | Rentals past due date |
| `get_slow_moving_films` | `(store_id=None, days=90, limit=20)` | Films not rented in N days |
| `get_revenue_summary` | `()` | Total revenue, payments, busiest month |
| `get_store_comparison` | `()` | Side-by-side snapshot of both stores |

---

## Key design decisions

| Decision | Rationale |
|---|---|
| Dual connection pools (asyncpg + psycopg3) | LangGraph checkpointer requires psycopg3 with `autocommit=True, prepare_threshold=0`; tool queries use asyncpg for performance |
| MCP servers as child processes over stdio | Each server has its own asyncpg pool; failure in one server doesn't crash the agent |
| SSE filter: `langgraph_node == "agent"` | Prevents summarize-node tokens from leaking to the UI |
| One retry max for reflection | Avoids infinite loops; second pass is "best effort" |
| Grounding warning as suffix, no re-run | User sees the uncertainty; re-running risks a different hallucination |
| `save_prefs` reads tool results, not user text | Preferences updated from structured DB data, not from what the user typed |
| `human_review` uses `interrupt()` | LangGraph native pause/resume; resume via `POST /chat/confirm/{id}/stream` |
| `rating_txt` column, not `rating` enum | Ebean cannot map PostgreSQL custom enum types; `rating_txt varchar(10)` is the workaround |

---

## FastAPI endpoints (main.py)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/health` | Liveness check |
| `POST` | `/chat` | Blocking invoke — returns full answer |
| `POST` | `/chat/stream` | SSE stream — `token` / `tool_start` / `tool_end` / `interrupt` / `done` events |
| `POST` | `/chat/confirm/{thread_id}/stream` | Resume after human_review interrupt |
| `GET` | `/sessions` | List all persisted threads |
| `GET` | `/sessions/{id}` | Full message history |
| `DELETE` | `/sessions/{id}` | Delete checkpoint |
| `POST` | `/admin/reindex` | Trigger RAG reindex (returns 202) |
| `GET` | `/admin/reindex` | Poll reindex status |

---

## Frontend (ui-angular/src/app/)

| File | Role |
|---|---|
| `chat.component.ts/html` | MatDialog host — SSE streaming, typing indicator, tool event badges, structured output mat-table |
| `chat.service.ts` | Signal-based state, `sendMessage()`, `loadSession()`, `confirmToolCall()` |
| `agent.service.ts` | HTTP client wrapping all /chat and /sessions endpoints |
| `sessions.component.ts/html` | Session sidebar — list, switch, delete, new session |
| `tool-confirm.component.ts` | Approve/reject tool calls wired into human_review interrupt |

Opened via toolbar `smart_toy` icon → `AppComponent.openChat()` → MatDialog.
`user_id` is a UUID stored in `localStorage` (key: `pagila_user_id`); sent in every ChatRequest.

## [[genai_assistant_next_steps]]
