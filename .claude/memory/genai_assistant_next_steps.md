---
name: genai_assistant_next_steps
description: All features implemented and tested; 91/91 Playwright e2e passing as of 2026-05-20
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

## What's next

No pending feature work. Options:
- Customer detail card (same actor/store pattern)
- Films service unit tests
- Integration/API tests for quarkus-ebean endpoints
