# Project Memory

Running notes on decisions, conventions, and pending work. Update as the project evolves.

---

## Active Branch

`genai-assistant` — AI agent feature on top of the main pagila-samples stack.

---

## Decisions & Conventions

### database/schema.sql is authoritative
Always check `./database/schema.sql` before changing entity column names. It contains
local patches on top of base Pagila:
- Adds `rating_txt varchar(10)` to `film` (populated from `rating::varchar`)
- Recreates `film_list` and `nicer_but_slower_film_list` views to include `rating_txt`
- Widens `language.name` to `varchar(20)`

The `rating_txt` column exists because Ebean cannot handle PostgreSQL's `mpaa_rating`
custom enum. Entities map to `rating_txt`, not `rating`.

### Dual-mapping pattern for FK relationships (data-ebean)
Keep both a raw FK integer and the ORM navigation association to avoid breaking
existing MapStruct mappers:
```java
@Column(name = "customer_id", insertable = false, updatable = false)
Integer customerId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
Customer customer;
```

### Payment does not extend BaseModel
The `payment` table has no `last_update` column. `Payment.java` must not extend
`BaseModel` or Ebean will generate a broken SELECT at runtime.

### spring-vaadin is excluded from ebean-scope reviews
When reviewing ebean-based modules, skip `spring-vaadin`.

---

## genai-assistant: Completed Work

- FastAPI backend: `/chat`, `/chat/stream` (SSE), `/sessions` CRUD, `/health`
- `/admin/reindex` (POST 202 + GET status), `/chat/confirm/{id}/stream` (tool approval)
- LangGraph ReAct agent with PostgreSQL session persistence (`AsyncPostgresSaver`)
- LangGraph `human_review` interrupt node for tool confirmation UX
- MCP servers: `film_server.py` (14 tools), `store_server.py` (6 tools)
- pgvector RAG: `sentence-transformers/all-MiniLM-L6-v2`, collection `film_descriptions`
- Angular chat UI: SSE streaming, markdown rendering (`ngx-markdown@20`), session sidebar,
  tool event badges, tool confirmation dialog (`ToolConfirmComponent` wired)
- `README.adoc` written for the genai-assistant module

---

## Testing

### Angular e2e — `ui-angular/`

```bash
cd ui-angular && ng serve          # keep running (port 4200)
cd ui-angular && npx playwright test                        # all 66 tests
cd ui-angular && npx playwright test e2e/chat.spec.ts       # single spec
cd ui-angular && npx playwright test --grep "SSE"           # pattern filter
cd ui-angular && npm run e2e:report                         # open HTML report
```

- No real backend required — all calls mocked via `page.route()` in `e2e/mocks.ts`
- Chromium path: `~/.cache/ms-playwright/chromium-1223/` (override with
  `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH`)
- Config: `ui-angular/playwright.config.ts`

### FastAPI backend — `genai-assistant/`

```bash
cd genai-assistant && uv run pytest                          # all tests
cd genai-assistant && uv run pytest tests/test_api.py -v    # API tests only (no DB)
cd genai-assistant && uv run pytest tests/test_film_tools.py -v  # MCP tools (needs DB)
```

- `tests/test_api.py` — 20 tests, no DB: `/health`, `/admin/reindex`, `/chat`,
  `/chat/stream`, `/chat/confirm`, `/sessions` CRUD
- `tests/test_film_tools.py`, `test_store_tools.py`, etc. — require live PostgreSQL

---

## genai-assistant: Pending Work (in agreed order)

- [x] **1. Markdown rendering in chat** — `ngx-markdown@20` installed; `MarkdownModule.forRoot()`
  added to `app.module.ts`; AI messages render via `<markdown [data]="msg.text">`;
  SCSS updated (removed `white-space: pre-wrap`, added code/pre/blockquote styles).

- [x] **2. `/admin/reindex` endpoint** — `POST /admin/reindex` (202, fires background task)
  and `GET /admin/reindex` (status poll) added to `main.py`. `ReindexStatus` model in
  `models.py`. `index_films()` from `rag.py` runs in an `asyncio.create_task`.

- [x] **3. Tool confirmation UX** — LangGraph `human_review` node with `interrupt()` added
  to `agent.py`; graph edges rewired through `human_review`; `POST /chat/confirm/{id}/stream`
  SSE endpoint added to `main.py`; `pendingConfirm` signal + `confirmTool()` added to
  `chat.service.ts`; `<app-tool-confirm>` wired into `chat.component.html`.

---

## Schema Gap Analysis (completed 2026-05-18)

All 15 Pagila tables and 7 views have entity classes in `data-ebean`. Fixes applied:

| File | Fix |
|---|---|
| `Payment.java` | Removed `BaseModel`, fixed `@DbPartition` property, `Float`→`BigDecimal`, added Customer/Rental FKs |
| `Customer.java` | Added `BaseModel`, added Store/Address FKs (dual-mapping), renamed `status`→`activeBool` |
| `Inventory.java` | Added `BaseModel`, added Film/Store FKs (dual-mapping) |
| `Rental.java` | Added Inventory/Customer/Staff FKs (dual-mapping) |
| `Staff.java` | Added Address FK |
| `Film.java` | `Double`→`BigDecimal` for `rentalRate` and `replacementCost` |
| `FilmView.java` | `Float`→`BigDecimal` for `price` |
| `NicerFilmView.java` | `Float`→`BigDecimal` for `price` |
