---
name: genai-assistant-architecture
description: "Python FastAPI + LangGraph agent over Pagila DB using MCP tools, OpenRouter LLM, and pgvector RAG; Angular chat UI fully implemented as MatDialog in ui-angular"
metadata: 
  node_type: memory
  type: project
  originSessionId: 06e7f22c-ae4f-4a5d-9e8a-5bf5d3ea9b75
---

Active branch: `genai-assistant` — Python AI backend in `genai-assistant/` module.
Angular chat UI is embedded **inside `ui-angular`** (not a separate module).

**Why:** Demonstrates AI-first database access pattern using agentic tooling (MCP),
streaming (SSE), and vector search (RAG) over an existing relational DB.

**How to apply:** When working on this module, understand the MCP subprocess pattern
(film_server.py/store_server.py as child processes), LangGraph agent loop, SSE streaming
contract with the frontend, and the dual connection pool requirement.

## Backend (genai-assistant/src/)
- **main.py**: FastAPI app — GET /health, POST /chat (blocking), POST /chat/stream (SSE),
  GET/DELETE /sessions/{id}
- **agent.py**: LangGraph StateGraph — agent node (LLM) → ToolNode (MCP tools) → loop;
  OpenRouter gateway (default: mistralai/devstral-2512); system prompt is a decision tree
  that routes intents to specific tool chains
- **film_server.py**: FastMCP server (14 tools) — film search, actor search, rental lookups,
  semantic search via RAG
- **store_server.py**: FastMCP server (6 tools) — store inventory, top customers, monthly revenue
- **rag.py**: HuggingFace sentence-transformers (all-MiniLM-L6-v2, local) → LangChain PGVector
  (collection: film_descriptions); run `uv run src/rag.py` once to seed vectors
- **sessions.py**: Reads LangGraph's PostgreSQL checkpoint tables directly for session CRUD
- **db.py**: Two pools — asyncpg (tools + session queries) and psycopg3 (LangGraph checkpointer,
  needs autocommit=True, prepare_threshold=0)

## Frontend (ui-angular/src/app/)
- **Fully implemented** — real SSE streaming connected to /chat/stream
- **chat.component.ts/html**: MatDialog, SSE streaming, typing indicator, tool event badges
- **chat.service.ts**: Signal-based state, `sendMessage()` → streamChat(), `loadSession()`
- **agent.service.ts**: HTTP client wrapping all /chat and /sessions endpoints
- **sessions.component.ts/html**: Session sidebar — list, switch, delete, new session
- **tool-confirm.component.ts**: Wired into chat.component.html — approve/reject tool calls
- Opened via toolbar `smart_toy` icon → `AppComponent.openChat()` → MatDialog

## [[genai_assistant_next_steps]]
