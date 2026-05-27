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
- **data-exposed**: Kotlin Exposed SQL DSL (Kotlin 2.1)
- **quarkus-ebean**: Quarkus 3 REST API with Ebean (port 8001)
- **quarkus-hibernate**: Quarkus 3 REST API with Hibernate
- **quarkus-htmx**: Quarkus + HTMX + Qute server-side rendered UI
- **spring-vaadin**: Spring Boot 3.5 + Vaadin 25 UI (skip in ebean-scope reviews)
- **go-web**: Go REST API (minimal stdlib)
- **ui-angular**: Angular 20 film browser SPA + embedded AI chat (port 4200)
- **genai-assistant**: Python FastAPI + LangGraph + MCP agent (port 8000)

## Key Versions
- Java: 25
- Quarkus: 3.35.2
- Ebean: 17.5.0
- Hibernate: 7.3.3.Final
- Kotlin Exposed: 1.2.0
- Angular: 20 (signals, OnPush, rxResource)
- Python: 3.13

## Database
- **DB name**: `sakila` (not "pagila" — folder name is misleading)
- **App schema**: `public`
- **LangGraph schema**: `langgraph`
- **Embeddings**: `BAAI/bge-small-en-v1.5` via FastEmbed (ONNX, no torch) in `langchain_pg_embedding`
