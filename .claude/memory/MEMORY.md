# Memory Index
- [`tracking/todo.md`](./tracking/todo.md) — tracking index + rules (items, findings, decisions, reviews)
- [Project Overview](project_overview.md) — Multi-module Java/Go/Python/Angular demo over Pagila DVD rental DB
- [GenAI Assistant Architecture](genai_assistant_architecture.md) — Python FastAPI + LangGraph + MCP + pgvector RAG; chat UI is the HTMX UI served by genai-assistant (/ui); Angular toolbar button links to it
- [GenAI Assistant Next Steps](genai_assistant_next_steps.md) — all planned genai features done; test commands and counts (147 non-DB / 102 DB-backed, 2026-10-28); open work lives in `tracking/todo.md`
- [Approval Prompt Audit](approval_prompt_audit.md) — tool-confirmation flow: invariants, enforcement points, open risks (2026-10-28)
- [DB Config](project_db_config.md) — DB: **sakila**, app schema: public, LangGraph schema: langgraph, embeddings: FastEmbed BAAI/bge-small-en-v1.5
- [Playwright Patterns](feedback_playwright_patterns.md) — LIFO route registration order, no ng-reflect-* assertions, macOS vs Linux Chromium path

## Rules
- Keep files short.
- Prefer updating existing entries.
- Use repo-local memory only.
- Do not store personal, secret, or machine-specific details.
