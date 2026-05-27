---
name: genai_assistant_next_steps
description: LangGraph summarization + input validation nodes complete (2026-05-26); 31 test_api.py tests passing
type: project
---

All originally planned features are **complete**:

1. **Markdown rendering in chat** ✅
2. **`/admin/reindex` endpoint** ✅
3. **Tool confirmation UX** ✅
4. **Actor detail card** ✅
5. **Store detail card** ✅
6. **Angular unit tests** ✅ (actors/stores service specs + card component specs)

## Raw SQL elimination (2026-05-20)

`StoresRepository` and `FilmsRepository` rewrites — no more `sqlQuery()` or `findDto()` raw SQL.

**Approach**: Created PostgreSQL views → Ebean `@Entity` view classes → MapStruct mappings → ORM `.find()` queries.

New views in `database/schema.sql`:
- `store_view`, `store_inventory_view`, `store_rental_view`, `store_customer_view`
- `film_category_facet`, `film_rating_facet`, `film_price_facet`

New Ebean entities (`data-ebean`): `StoreView`, `StoreInventoryView`, `StoreRentalView`,
`StoreCustomerView`, `FilmCategoryFacet`, `FilmRatingFacet`, `FilmPriceFacet`

**Why:** Project convention — raw SQL in repositories is forbidden; ORM queries over views are preferred.
**How to apply:** For any new repository query, reach for a view + `@Entity` + Ebean `.find()` chain. Raw SQL is a last resort.

## JUnit platform version fix (2026-05-20)

`quarkus-ebean/pom.xml` has an explicit `<dependencyManagement>` entry overriding the Quarkus BOM:
```xml
<dependency>
  <groupId>org.junit.platform</groupId>
  <artifactId>junit-platform-engine</artifactId>
  <version>6.0.3</version>
  <scope>test</scope>
</dependency>
```
Quarkus BOM 3.35.2 pins `junit-platform-engine` to `1.10.1`, but JUnit 6 unified versioning requires `6.0.3`.
Root `pom.xml` also updated to `6.0.3`.

## Test suites — current state

### Playwright e2e: **91/91 passing** as of 2026-05-20
```bash
cd ui-angular && ng serve          # keep running
cd ui-angular && npx playwright test
```

### genai-assistant pytest: **20/20 passing** (test_api.py; no live DB needed)
```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py
```
Note: `uv run pytest` may fail on Intel Mac (onnxruntime wheel missing); use `.venv/bin/python -m pytest` instead.

### Karma/Jasmine unit tests
```bash
cd ui-angular && ng test
```
Spec files: `actors.service.spec.ts`, `stores.service.spec.ts`, actor card spec, store card spec.

## Analytics MCP server (2026-05-21) — COMPLETE ✅

A third MCP server `genai-assistant/src/analytics_server.py` with 4 tools, fully wired into the LangGraph agent. Branch: `genai-assistant`.

**Spec:** `docs/superpowers/specs/2026-05-21-analytics-mcp-server-design.md`
**Plan:** `docs/superpowers/plans/2026-05-21-analytics-mcp-server.md` (7 tasks)

**Why:** Cover the remaining Pagila schema gaps — overdue rentals, slow-moving inventory, revenue summary, store comparison. Follows the same FastMCP + asyncpg pool pattern as `film_server.py` and `store_server.py`.

### Task progress

| Task | Status | Commit |
|---|---|---|
| 1: analytics_server.py skeleton + conftest.py patch | ✅ Done | d2fc5742 |
| 2: test_analytics_tools.py (30 tests, all verified failing) | ✅ Done | 84e48b4c |
| 3: Implement `get_overdue_rentals` (6/6 tests pass) | ✅ Done | b0b1f1d5 |
| 4: Implement `get_slow_moving_films` (7/7 tests pass) | ✅ Done | 5da4d95d |
| 5: Implement `get_revenue_summary` (9/9 tests pass) | ✅ Done | 198ccc6d |
| 6: Implement `get_store_comparison` (8/8 tests pass) | ✅ Done | 410068d2 |
| 7: Wire `pagila_analytics` into agent.py + SYSTEM_PROMPT | ✅ Done | 7a1e9785 |

### Test suite state (as of 2026-05-25)

- `test_analytics_tools.py`: **30/30 passing** (live DB required)
- `test_api.py`: **20/20 passing** (no live DB needed)
- Combined: **50/50 passing**

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py tests/test_analytics_tools.py -v
```

### Quality notes from review

- `get_slow_moving_films`: uses `CURRENT_DATE` (not `NOW()`) in CASE expr; `days_since_rented` is `None` for never-rented films (documented in docstring)
- `get_revenue_summary`: uses scalar subqueries (not cross-join) for busiest month; `None` guard for empty payment table
- `get_store_comparison`: `COALESCE` on `AVG(f.rental_rate)` to handle zero-inventory stores

## LangGraph nodes (2026-05-26) — COMPLETE ✅

Two new LangGraph nodes added to `genai-assistant/src/agent.py`. Branch: `genai-assistant`.

**Specs:**
- `docs/superpowers/specs/2026-05-26-langgraph-summarization-node-design.md`
- `docs/superpowers/specs/2026-05-26-langgraph-input-validation-node-design.md`

**Plans:**
- `docs/superpowers/plans/2026-05-26-langgraph-summarization-node.md`
- `docs/superpowers/plans/2026-05-26-langgraph-input-validation-node.md`

### Summarization node

Trims old messages into a rolling `summary` field in `AgentState` after tool execution when message count exceeds `SUMMARIZE_THRESHOLD = 10`. Keeps last `KEEP_LAST_N = 4` messages, walking back to a `HumanMessage` boundary before slicing. Summary is injected as a second `SystemMessage` on subsequent agent calls via `_prepare_messages`.

Key additions to `agent.py`:
- `AgentState.summary: str` field (in `models.py`)
- `SUMMARIZE_THRESHOLD = 10`, `KEEP_LAST_N = 4`
- `_prepare_messages(messages, summary)` — module-level pure helper
- `summarize_history(state)` — module-level async node using global `model` (not `bound_model`)
- Graph: `graph.add_conditional_edges("tools", lambda s: "summarize" if len(s["messages"]) > SUMMARIZE_THRESHOLD else "agent", ...)`

SSE fix in `main.py`: filter `on_chat_model_stream` events by `langgraph_node == "agent"` to prevent summarize-node tokens from leaking to the UI.

### Input validation node

Classifies every user message before the agent runs using `model.with_structured_output(TopicCheck)`. Off-topic queries (weather, cooking, etc.) receive a polite `AIMessage` rejection and skip the agent entirely, consuming zero tool tokens.

Key additions to `agent.py`:
- `TopicCheck(BaseModel)`: `relevant: bool = Field(description="...")`
- `VALIDATION_PROMPT`: short topic classifier prompt
- `classifier = model.with_structured_output(TopicCheck)` (module-level)
- `validate_input(state)` — module-level async node; returns `{}` on-topic or `{"messages": [AIMessage(rejection)]}` off-topic
- `_after_validate(state)` — routing helper: `END` if last message is `AIMessage`, else `"agent"`
- Graph: `START → validate → (relevant?) → agent | END`

### Test suite state (as of 2026-05-26)

- `test_api.py`: **31/31 passing** (no live DB needed)
  - `TestSummarizationNode`: 6 tests
  - `TestValidationNode`: 5 tests (includes empty-messages guard test)
- `test_analytics_tools.py`: **30/30 passing** (live DB required)

```bash
cd genai-assistant && .venv/bin/python -m pytest tests/test_api.py -v
```

## What's next

- Customer detail card (same actor/store Angular pattern)
- Films service unit tests
- Integration/API tests for quarkus-ebean endpoints
