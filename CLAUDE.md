# pagila-samples — Claude Code Guide

Multi-module demo project showing the same DVD rental domain (Pagila PostgreSQL DB) implemented
across many languages and frameworks. Active feature branch: **`genai-assistant`**.

## Session Memory

Persistent memory lives in **`.claude/memory/`** (version-controlled, shared across machines).

| File | Contents |
|---|---|
| `.claude/memory/MEMORY.md` | Index — loaded automatically each session |
| `.claude/memory/project_overview.md` | Module map, key versions, project purpose |
| `.claude/memory/genai_assistant_architecture.md` | FastAPI + LangGraph + MCP + Angular chat design |
| `.claude/memory/genai_assistant_next_steps.md` | Completed features, test suite status, what's next |

On a new machine, symlink the system memory path to this directory so Claude Code writes here:

```bash
PROJ="$(git rev-parse --show-toplevel)"
SLUG="${PROJ//\//-}"          # /data/incubator/... → -data-incubator-...
SYS_MEM="$HOME/.claude/projects/${SLUG}/memory"
mkdir -p "$(dirname "$SYS_MEM")"
rm -rf "$SYS_MEM"
ln -s "${PROJ}/.claude/memory" "$SYS_MEM"
```

---

## Module Map

| Module | Language | Stack | Notes |
|---|---|---|---|
| `core-api` | Java 25 | MapStruct DTOs | Shared beans/mappers used by all Java modules |
| `database` | SQL | PostgreSQL | Schema patches on top of base Pagila (see below) |
| `data-ebean` | Java 25 | Ebean ORM 17.5 | Entities for all 15 tables + 7 views |
| `data-hibernate` | Java 25 | Hibernate 7.3 | Parallel entity set |
| `data-exposed` | Kotlin 2.1 | Exposed 1.2 SQL DSL | Kotlin alternative |
| `quarkus-ebean` | Java 25 | Quarkus 3.35 + Ebean | JSON REST API |
| `quarkus-hibernate` | Java 25 | Quarkus 3.35 + Hibernate | JSON REST API |
| `quarkus-htmx` | Java 25 | Quarkus + HTMX + Qute | Server-side rendered HTML |
| `spring-vaadin` | Java 25 | Spring Boot 3.5 + Vaadin 25 | Skip in ebean-scope reviews |
| `go-web` | Go | stdlib net/http | Minimal REST API |
| `ui-angular` | TypeScript | Angular 20 | Film browser SPA + embedded AI chat |
| `genai-assistant` | Python 3.13 | FastAPI + LangGraph + MCP | AI agent backend |

---

## Key Versions

- Java: 25
- Quarkus: 3.35.2
- Ebean: 17.5.0
- Hibernate: 7.3.3.Final
- Kotlin Exposed: 1.2.0
- Angular: 20 (with signals, OnPush, `rxResource`)
- Python: 3.13 (pyproject.toml `requires-python = ">=3.11,<3.14"`)

---

## Database

The base Pagila schema is the standard PostgreSQL Pagila distribution.
**`./database/schema.sql` contains local patches applied on top** — always check this
before changing entity column names. Current patches:

- Adds `rating_txt varchar(10)` to `film` table (populated from `rating::varchar`)
- Recreates `film_list` and `nicer_but_slower_film_list` views to include `rating_txt`
- Widens `language.name` to `varchar(20)`

The `rating_txt` column exists specifically because Ebean cannot natively handle PostgreSQL's
`mpaa_rating` custom enum type. Entities map to `rating_txt`, not `rating`.

---

## Build Commands

```bash
# Compile all Java modules
mvn compile -pl core-api,data-ebean,quarkus-ebean,quarkus-htmx -q

# Run quarkus-ebean in dev mode
cd quarkus-ebean && mvn quarkus:dev

# Run quarkus-htmx in dev mode
cd quarkus-htmx && mvn quarkus:dev

# Run genai-assistant
cd genai-assistant && uv sync && uv run src/main.py

# Index film embeddings (run once after DB setup)
cd genai-assistant && uv run src/rag.py

# Run Angular UI
cd ui-angular && ng serve
```

---

## Architecture: genai-assistant

FastAPI app (`localhost:8000`) backed by a LangGraph ReAct agent.

```
POST /chat           → blocking invoke
POST /chat/stream    → SSE stream (token / tool_start / tool_end / done events)
GET  /sessions       → list all persisted conversation threads
GET  /sessions/{id}  → full message history
DELETE /sessions/{id}
```

**MCP servers** (`film_server.py`, `store_server.py`) are spawned as child processes over
stdio at startup. Each has its own asyncpg pool. The Angular UI (`ui-angular`) opens the
chat as a `MatDialog` from the toolbar button (smart_toy icon).

**Two connection pools** are required:
- `asyncpg` — tool queries and session management
- `psycopg3` — LangGraph's `AsyncPostgresSaver` (requires `autocommit=True`, `prepare_threshold=0`)

**RAG**: `sentence-transformers/all-MiniLM-L6-v2` (local, no API key) + pgvector.
Collection name: `film_descriptions`.

**Config via `.env`**: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
`OPENROUTER_API_KEY`, `OPENROUTER_BASE_URL`, `OPENROUTER_MODEL`.

---

## Entity Conventions (data-ebean / data-hibernate)

- All tables with `last_update` extend `BaseModel`.
  - `payment` table does **not** have `last_update` — `Payment` must NOT extend `BaseModel`.
- FK relationships use the **dual-mapping pattern** to preserve raw ID access alongside
  ORM navigation:
  ```java
  @Column(name = "customer_id", insertable = false, updatable = false)
  Integer customerId;                          // raw FK — keeps existing mappers compiling

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  Customer customer;                           // ORM navigation
  ```
- `spring-vaadin` is excluded from ebean-scope reviews.
- Join tables (`film_actor`, `film_category`) are handled via `@ManyToMany` in `Film.java`,
  not as separate entity classes.

---

## Angular Conventions (ui-angular)

- Lazy-loaded feature modules with `loadChildren`.
- Data loading uses `rxResource<T, R>()` from `@angular/core/rxjs-interop` (Angular 20 API).
- `ChangeDetectionStrategy.OnPush` on all components.
- `inject()` over constructor injection.
- AI chat is a `MatDialog` opened from `AppComponent.openChat()`.

---

## Testing

### Angular e2e (Playwright) — `ui-angular/`

Requires the Angular dev server running on `localhost:4200`. No real backend needed — all API
calls are intercepted with `page.route()` mocks.

```bash
# Start the dev server (keep running in a separate terminal)
cd ui-angular && ng serve

# Run all 66 e2e tests
cd ui-angular && npx playwright test

# Run a single spec file
cd ui-angular && npx playwright test e2e/chat.spec.ts

# Run tests matching a name pattern
cd ui-angular && npx playwright test --grep "SSE streaming"

# Open the HTML report after a run
cd ui-angular && npm run e2e:report
```

**Test files under `ui-angular/e2e/`:**

| File | Coverage |
|---|---|
| `navigation.spec.ts` | Toolbar links, page routing, title |
| `home.spec.ts` | Stat cards, chart headings, revenue values |
| `films.spec.ts` | Film List + Sales by Category tabs, table columns, paginator, loading bar, pagination |
| `actors.spec.ts` | Actors tab, table columns, paginator |
| `customers.spec.ts` | Customer table columns, paginator |
| `stores.spec.ts` | Sales by Store + Staff tabs, tables |
| `chat.spec.ts` | Dialog open/close, SSE streaming, keyboard shortcuts, sessions sidebar |
| `error-states.spec.ts` | API 500/network errors, empty results, no JS crash |
| `mocks.ts` | Shared `page.route()` helpers and fixture data (not a test file) |

**Playwright config:** `ui-angular/playwright.config.ts` — uses the Chromium binary at
`~/.cache/ms-playwright/chromium-1223/`. Override with env var
`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` if your Chromium is elsewhere.

---

### genai-assistant backend (pytest) — `genai-assistant/`

No running database or MCP servers required — tests patch `agent_app`, `get_asyncpg_pool`,
and session functions with mocks.

```bash
# Run all tests (existing MCP tool tests + new API tests)
cd genai-assistant && uv run pytest

# Run only the FastAPI endpoint tests
cd genai-assistant && uv run pytest tests/test_api.py -v

# Run only the MCP tool tests (needs a live PostgreSQL on localhost:5432)
cd genai-assistant && uv run pytest tests/test_film_tools.py tests/test_store_tools.py -v

# Run a specific test class
cd genai-assistant && uv run pytest tests/test_api.py::TestChatStream -v
```

**Test files under `genai-assistant/tests/`:**

| File | Coverage | DB needed? |
|---|---|---|
| `test_api.py` | `/health`, `/admin/reindex`, `/chat`, `/chat/stream`, `/chat/confirm`, `/sessions` CRUD | No |
| `test_film_tools.py` | MCP film tools (search, details, availability, actors) | Yes |
| `test_store_tools.py` | MCP store tools (inventory, customers, revenue) | Yes |
| `test_actor_tools.py` | MCP actor tools | Yes |
| `test_rental_tools.py` | MCP rental tools | Yes |
| `test_mcp.py` | MCP server startup / tool registration | Yes |

---

## genai-assistant: Pending Work (in order)

1. **Markdown rendering in chat** — AI responses contain markdown that renders as raw
   symbols. Install `ngx-markdown` in `ui-angular` and apply a markdown pipe to `msg.text`
   in `chat.component.html`.

2. **`/admin/reindex` endpoint** — Add a `POST /admin/reindex` to `main.py` that calls
   `index_films()` from `rag.py` so embeddings can be refreshed without CLI access.

3. **Tool confirmation UX** — `ToolConfirmComponent` and `ToolInfo` model already exist
   but are not wired up. Requires: LangGraph interrupt node on the backend + frontend
   polling/confirm flow before tool execution.
