---
name: genai-assistant-architecture
description: "Python FastAPI + LangGraph agent over Pagila DB using MCP tools, OpenRouter LLM, and pgvector RAG; chat UI is the backend-served HTMX UI (/ui)"
type: project
---

Active branch: `genai-assistant` — Python AI backend in `genai-assistant/` module.
Chat UI is served by the backend itself (HTMX), not embedded in `ui-angular`.

**Why:** Demonstrates AI-first database access pattern using agentic tooling (MCP),
streaming (SSE), and vector search (RAG) over an existing relational DB.

**How to apply:** When working on this module, understand the MCP subprocess pattern
(film_server.py/store_server.py as child processes), LangGraph agent loop, SSE streaming
contract with the frontend, and the dual connection pool requirement.

## Backend (genai-assistant/src/)
- **main.py**: FastAPI app — GET /health, POST /chat (blocking), POST /chat/stream (SSE),
  GET/DELETE /sessions/{id}
- **agent.py**: LangGraph StateGraph — `validate` node (topic classifier) → `agent` node (LLM) → `human_review` → `tools` (MCP) → `summarize` (if >10 messages) → loop;
  OpenRouter gateway (default: mistralai/devstral-2512); system prompt is a decision tree
  that routes intents to specific tool chains.
  Graph shape: `START → validate → (relevant?) → agent | END`; post-tool: `tools → (len>10?) → summarize → agent | agent`
- **film_server.py**: FastMCP server (14 tools) — film search, actor search, rental lookups,
  semantic search via RAG
- **store_server.py**: FastMCP server (6 tools) — store inventory, top customers, monthly revenue
- **rag.py**: `FastEmbedEmbeddings` with `BAAI/bge-small-en-v1.5` (ONNX, no torch) → LangChain PGVector
  (collection: film_descriptions, schema: public); seed with `.venv/bin/python src/rag.py`
- **sessions.py**: Reads LangGraph's PostgreSQL checkpoint tables directly for session CRUD
- **db.py**: Two pools — asyncpg (tools + session queries, schema: public) and psycopg3
  (LangGraph checkpointer: autocommit=True, prepare_threshold=0, options="-c search_path=langgraph")

## Frontend
- **HTMX UI served by the backend** at `/ui` (`ui_routes.py`, `templates/`, `static/`, markdown via `mistune`): chat with SSE streaming, session list, tool confirm partial, analytics dashboard (`analytics_queries.py`).
- Paths are cwd-relative (`src/static`, `src/templates`): start from `genai-assistant/`.
- Angular (`ui-angular`) no longer embeds a chat: toolbar `smart_toy` icon → `AppComponent.openChat()` → `window.open(Constants.ASSISTANT_UI_URL)`. See DC-004 in `tracking/decisions.md`.

## [[genai_assistant_next_steps]]
