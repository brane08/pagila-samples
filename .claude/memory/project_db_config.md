---
name: project_db_config
description: Database connection details — sakila DB, public schema, langgraph schema for LangGraph checkpointer
type: project
---

Database name is **sakila** (not "pagila" — the project is named pagila-samples but the actual DB is sakila).

- **App schema**: `public` — all Pagila/Sakila tables (film, actor, store, rental, etc.) live here
- **LangGraph schema**: `langgraph` — LangGraph's `AsyncPostgresSaver` checkpointer tables live in this schema

**Why:** Standard Pagila schema is `public`; LangGraph checkpoint tables are isolated in their own schema to avoid polluting the app namespace.

**How to apply:** When writing SQL queries, migrations, or configuring LangGraph's PostgresStore/AsyncPostgresSaver, use `search_path=public` for app queries and ensure LangGraph is initialized with `schema_name="langgraph"` or equivalent. When debugging session persistence or checkpoint issues, look in the `langgraph` schema.
