---
name: genai_assistant_next_steps
description: Analytics MCP server complete (2026-05-25); all 7 tasks done, 50 tests passing
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

## What's next (after analytics server)

- Customer detail card (same actor/store Angular pattern)
- Films service unit tests
- Integration/API tests for quarkus-ebean endpoints
