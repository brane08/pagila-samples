---
name: quarkus-ebean-remaining-resource-tests
description: FilmsResourceTest, StoresResourceTest, RentalsResourceTest — same pattern as ActorsResourceTest
type: project
date: 2026-05-27
---

# quarkus-ebean Remaining Resource Integration Tests — Design

## Goal

Add `@QuarkusTest` integration tests for `FilmsResource`, `StoresResource`, and `RentalsResource`,
following the `ActorsResourceTest` pattern established in commit `e788ae9b`.

## Applies Established Pattern

- `@QuarkusTest` against live `sakila` DB (localhost:5432)
- Envelope + shape assertions: HTTP status, `success==true`, field presence on `data[0]` / `data`
- No hardcoded data values (except IDs known to exist: film 1, store 1)
- `\d+` regex on `{id}` path params to prevent routing ambiguity with `@`-prefixed routes
- `.urlEncodingEnabled(false)` on any test calling a `@`-prefixed path

## Production Code Fixes Required

Both resources have path param / literal route ambiguity — same issue fixed in `ActorsResource`:

- `FilmsResource`: `@Path("{film_id}")` → `@Path("{film_id: \\d+}")`. Route `@view`, `@nicer-view` must come before `{film_id}` or rely on the regex.
- `StoresResource`: `@Path("/{storeId}")` → `@Path("/{storeId: \\d+}")`. Routes `@sales-by-store`, `@staff`, `@view` must be declared before or rely on the regex.
- `RentalsResource`: no `{id}` param — no fix needed.

## Response Envelopes

Most endpoints: `{"success": true, "data": [...], "totalCount": N}` (paged)  
`ApiResult.single(x)`: `{"success": true, "data": {...}}`  
`GET /films/{film_id}`: **Non-standard** — returns `Optional<Film>` serialized directly (no ApiResult wrapper). Jackson serializes as the Film entity fields directly.

## FilmsResourceTest

**File:** `src/test/java/com/github/brane08/pagila/film/FilmsResourceTest.java`

### Tests (9)

| # | Method | Endpoint | Key assertions |
|---|---|---|---|
| 1 | `listFilms_returns200WithPagedShape` | `GET /films` | 200, success, data array, totalCount>0, first item has `title`, `description`, `rating` |
| 2 | `listFilms_pageSizeRespected` | `GET /films?size=3&page=1` | 200, success, `data.size()<=3` |
| 3 | `getFilmById_returns200WithTitle` | `GET /films/1` | 200, `title` notNull (no ApiResult wrapper — Film entity serialized directly) |
| 4 | `getFilmCount_returns200WithCount` | `GET /films/count` | 200, success, `data` empty array, `totalCount>0` |
| 5 | `listFilmFacets_returns200WithFacetShape` | `GET /films/facets` | 200, success, `data.size()>0`, `data[0].property` notNull |
| 6 | `listFilmViews_returns200WithViewShape` | `GET /films/@view` | 200, success, data array, totalCount>0, `data[0].title` + `data[0].category` notNull; `urlEncodingEnabled(false)` |
| 7 | `listFilmActors_returns200WithActorShape` | `GET /films/1/actors` | 200, success, `data.size()>0`, `data[0].actorId` notNull |
| 8 | `listNicerFilmViews_returns200WithViewShape` | `GET /films/@nicer-view` | 200, success, data array, totalCount>0, `data[0].title` + `data[0].category` notNull; `urlEncodingEnabled(false)` |
| 9 | `listSalesByCategory_returns200WithSalesShape` | `GET /films/@sales-by-category` | 200, success, `data.size()>0`, `data[0].category` notNull, `data[0].totalSales` notNull; `urlEncodingEnabled(false)` |

### Bean field mapping

- `FilmInfo`: `title`, `description`, `language`, `originalLanguage`, `rating`, `lastUpdate`
- `Film` entity (raw, test 3): `title` — assert only this
- `FilmViewInfo`: `title`, `description`, `category`, `rating`, `actors`
- `NicerFilmViewInfo`: same fields as `FilmViewInfo`
- `SalesByFilmCategoryInfo`: `category`, `totalSales`
- `Facet`: `property`, `values`

## StoresResourceTest

**File:** `src/test/java/com/github/brane08/pagila/store/StoresResourceTest.java`

### Tests (8)

| # | Method | Endpoint | Key assertions |
|---|---|---|---|
| 1 | `listStores_returns200WithPagedShape` | `GET /stores` | 200, success, data array, totalCount>0, `data[0].storeId` notNull |
| 2 | `listStores_pageSizeRespected` | `GET /stores?size=1&page=1` | 200, success, `data.size()<=1` |
| 3 | `getSalesByStore_returns200WithSalesShape` | `GET /stores/@sales-by-store` | 200, success, `data.size()>0`, `data[0].store` + `data[0].totalSales` notNull; `urlEncodingEnabled(false)` |
| 4 | `listStaffViews_returns200WithStaffShape` | `GET /stores/@staff` | 200, success, `data.size()>0`, `data[0].id` + `data[0].name` notNull; `urlEncodingEnabled(false)` |
| 5 | `listStoreViews_returns200WithViewShape` | `GET /stores/@view` | 200, success, `data.size()>0`, `data[0].storeId` + `data[0].manager` notNull; `urlEncodingEnabled(false)` |
| 6 | `getStoreById_returns200WithStoreShape` | `GET /stores/1` | 200, success, `data.storeId` equalTo(1) |
| 7 | `getStoreById_unknownId_returns404` | `GET /stores/99999` | 404 |
| 8 | `getStoreInventory_returns200WithInventoryShape` | `GET /stores/1/inventory` | 200, success, `data.size()>0`, `data[0].filmId` + `data[0].title` notNull |

Note: `/stores/{storeId}/rentals` and `/stores/{storeId}/customers` endpoints exist but are omitted
to keep the test class focused. The pattern is established and they can be added later if needed.

### Bean field mapping

- `StoreInfo`: `storeId`, `manager` (nested `StaffInfo`), `address` (nested), `lastUpdate`
- `SalesByStoreInfo`: `store`, `manager`, `totalSales`
- `StaffViewInfo`: `id`, `name`, `address`, `zipCode`, `phone`, `city`, `country`, `sid`
- `StoreViewInfo`: `storeId`, `manager`, `address`, `district`, `city`
- `StoreInventoryInfo`: `filmId`, `title`, `category`, `rating`, `rentalRate`, `totalCopies`, `availableCopies`

## RentalsResourceTest

**File:** `src/test/java/com/github/brane08/pagila/rental/RentalsResourceTest.java`

### Tests (2)

| # | Method | Endpoint | Key assertions |
|---|---|---|---|
| 1 | `listRentals_returns200WithPagedShape` | `GET /rentals` | 200, success, data array, totalCount>0, `data[0].rentalDate` notNull |
| 2 | `listCustomerViews_returns200WithViewShape` | `GET /rentals/@customers` | 200, success, data array, totalCount>0, `data[0].id` + `data[0].name` + `data[0].city` notNull; `urlEncodingEnabled(false)` |

### Bean field mapping

- `RentalInfo`: `rentalDate`, `returnDate`, `lastUpdate`
- `CustomerViewInfo`: `id`, `name`, `address`, `zipCode`, `phone`, `city`, `country`, `notes`, `sid`

## Build

```bash
cd quarkus-ebean && mvn test -Dtest="FilmsResourceTest,StoresResourceTest,RentalsResourceTest"
```

## Implementation Order

1. `FilmsResource.java` routing fix + `FilmsResourceTest.java`
2. `StoresResource.java` routing fix + `StoresResourceTest.java`
3. `RentalsResourceTest.java` (no production fix needed)
4. Run all 3 new test classes together; commit
