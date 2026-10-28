# Design findings (DS-nnn)

## DS-001
(RV-006 status audit, low) No env/config reference for genai-assistant: HOST, CORS_ORIGINS, WEB_CONCURRENCY (must be 1), AUTO_APPROVE_TOOLS, OPENROUTER_*, DB_* are documented nowhere, and there is no `.env.example`.
Status: FIXED 2026-10-28 (env table in CLAUDE.md; `.env.example` not added, secrets stay out of the repo)
