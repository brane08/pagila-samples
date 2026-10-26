---
name: Project Overview
description: pagila-samples multi-module demo — module map, key versions, actual DB name (sakila)
type: project
---

Multi-module educational project demonstrating the same DVD rental business domain across many frameworks and languages.

**Why:** Showcase tradeoffs between ORM/framework choices, languages, and UI approaches using the Pagila/Sakila sample dataset.

**How to apply:** When suggesting changes or features, identify which module is in scope. The `genai-assistant` branch is the active AI feature branch.

## Module Map
- **core-api**: Shared Java DTOs/Mappers (MapStruct)
- **database**: PostgreSQL schema patches (`./database/schema.sql`)
- **data-ebean**: Ebean ORM entities (Java 25)
- **data-hibernate**: Hibernate JPA entities (Java 25)
- **quarkus-htmx**: Quarkus 3 consolidated app, port 8001: JSON `/rest/*` + HTMX/Qute `/ui/*`; Hibernate default, Ebean via manual switch
- **go-web**: Go REST API (minimal stdlib)
- **ui-angular**: Angular 21 film browser SPA + embedded AI chat (port 4200)
- **genai-assistant**: Python FastAPI + LangGraph + MCP agent (port 8000)

## Key Versions
- Java: 25
- Quarkus: 3.39.3
- Ebean: 19.5.0
- Hibernate: 7.4.7.Final
- Angular: 21 (signals, OnPush, rxResource)
- Python: 3.13

## Database
- **DB name**: `sakila` (not "pagila" — folder name is misleading)
- **App schema**: `public`
- **LangGraph schema**: `langgraph`
- **Embeddings**: `BAAI/bge-small-en-v1.5` via FastEmbed (ONNX, no torch) in `langchain_pg_embedding`
