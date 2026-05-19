---
name: Project Overview
description: pagila-samples is a multi-module demo project showcasing multiple tech stacks (Java, Kotlin, Go, Python) implementing a DVD rental app over the Pagila PostgreSQL sample database
type: project
originSessionId: 06e7f22c-ae4f-4a5d-9e8a-5bf5d3ea9b75
---
Multi-module educational project demonstrating the same business domain (Pagila DVD rental DB) across many frameworks and languages.

**Why:** Showcase tradeoffs between ORM/framework choices, languages, and UI approaches using a well-known sample dataset.

**How to apply:** When suggesting changes or features, consider which module is in scope. The genai-assistant branch is the active AI feature branch.

## Module Map
- **core-api**: Shared Java DTOs/Mappers (MapStruct)
- **database**: PostgreSQL schema (Pagila + views + enhancements)
- **data-ebean**: Ebean ORM entities (Java 25)
- **data-hibernate**: Hibernate JPA entities (Java 25)
- **data-exposed**: Kotlin Exposed SQL DSL (Kotlin 2.1)
- **quarkus-ebean**: Quarkus 3 REST API (Ebean)
- **quarkus-hibernate**: Quarkus 3 REST API (Hibernate)
- **quarkus-htmx**: Quarkus + HTMX + Qute SSR web app
- **spring-vaadin**: Spring Boot 3.5 + Vaadin 25 UI
- **go-web**: Go REST API (minimal)
- **ui-angular**: Angular 20 film browser SPA
- **genai-assistant**: Python FastAPI + LangGraph + MCP agent (current branch)
- **ui-assistant**: Angular 20 AI chat frontend

## Key Versions
Java 25, Quarkus 3.34.5, Hibernate 7.2.9, Spring Boot 3.5.13, Angular 20.3.0, Python 3.11-3.13
