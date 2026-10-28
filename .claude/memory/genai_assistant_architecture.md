---
name: genai-assistant-architecture
description: "Python FastAPI + LangGraph agent over Pagila DB using MCP tools, OpenRouter LLM, and pgvector RAG; chat UI is the backend-served HTMX UI (/ui); approval flow bound to confirm_id"
type: project
---

Python AI backend in `genai-assistant/`. Chat UI is served by the backend itself (HTMX), not embedded in `ui-angular`.

**Why:** Demonstrates AI-first database access using agentic tooling (MCP), streaming (SSE), and vector search (RAG) over an existing relational DB.

**How to apply:** Understand the MCP subprocess pattern (three FastMCP servers as child processes), the LangGraph loop, the SSE contract with `static/chat.js`, the dual connection pool requirement, and the single-worker rule.

## Backend (genai-assistant/src/)
- **main.py**: FastAPI app. `/health`, `/chat` (blocking; auto-rejects tool calls), `/chat/stream` (SSE), `/chat/pending/{id}`, `/chat/confirm/{id}/stream`, `/admin/reindex` (POST 202 + GET), `/sessions` CRUD. Per-thread `asyncio.Lock` (in-process, so single worker; startup refuses `WEB_CONCURRENCY>1`). Streamed runs are detached tasks feeding a queue, so a client disconnect does not cancel a resume. Approvals carry a `confirm_id` (hash of the proposed calls).
- **agent.py**: `build_agent` (MCP client + `AsyncPostgresSaver`) calls `build_graph(tools, checkpointer, asyncpg_pool, chat_model)`. Graph: `START → validate → (relevant?) load_prefs → agent`; `agent → (tool_calls?) clarify → human_review (interrupt) → tools → save_prefs → handle_errors → agent | summarize | END` and `agent → reflect → (incomplete?) agent | ground → END`. Classifier calls (topic, clarification, reflection, grounding) fail open. A reflection retry removes the draft and passes the critique via `reflection_critique` state, not a message. `handle_errors` ends the turn after two consecutive tool-error rounds. Prefs are saved only from explicit user statements. `AUTO_APPROVE_TOOLS` is an opt-in allowlist.
- **MCP servers** (stdio child processes): `film_server.py` (14 tools), `store_server.py` (6), `analytics_server.py` (4: overdue rentals, slow movers, revenue summary, store comparison via `analytics_queries.py`). SQLAlchemy Core over `pagila_tables.py`. `query_utils.py` holds `like`/`clamp` (LIKE escaping, limit ≤ 100) shared by all three.
- **rag.py**: `FastEmbedEmbeddings` `BAAI/bge-small-en-v1.5` (ONNX, no torch) → LangChain PGVector (collection `film_descriptions`); cached store, blocking calls via `asyncio.to_thread`; seed with `.venv/bin/python src/rag.py`.
- **sessions.py**: reads the `langgraph.*` checkpoint tables directly (schema-qualified) for session CRUD.
- **db.py**: two pools: asyncpg (tools, sessions; schema public) and psycopg3 (checkpointer: autocommit, prepare_threshold=0, `search_path=langgraph`).
- **models.py**: `AgentState`, request/response models (`ChatRequest` validates message length and thread/user id pattern; `ConfirmRequest{approved, confirm_id}`).

## Frontend
- **HTMX UI served by the backend** at `/ui` (`ui_routes.py`, `templates/`, `static/chat.js`, markdown via `mistune` with `escape=True`, images shown as alt text only). Paths are anchored to `__file__`, so any cwd works. Chat with SSE streaming, session list, confirm card built in `chat.js` (`restorePendingCard`, `confirmTool`), analytics dashboard.
- UI routes validate thread ids; deleting a session is refused (409) while a run holds its lock.
- Angular (`ui-angular`) has no chat: toolbar `smart_toy` icon → `Constants.ASSISTANT_UI_URL`. See DC-004; approval design in DC-005.

## Tests
Real-graph tests use `build_graph` with a scripted chat model and `InMemorySaver` (`tests/test_agent_graph.py`). Classifier stubs replace the module-level `classifier`, `clarification_classifier`, `reflection_classifier`, `grounding_classifier`.

## [[genai_assistant_next_steps]]
