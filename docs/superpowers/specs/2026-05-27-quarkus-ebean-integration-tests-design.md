---
name: quarkus-ebean-integration-tests
description: ActorsResource @QuarkusTest integration tests establishing pattern for all resources
type: project
date: 2026-05-27
---

# quarkus-ebean Integration Tests — Design

## Goal

Add `@QuarkusTest` integration tests for `ActorsResource` using REST-assured. These tests establish
the canonical pattern; remaining resources (Films, Stores, Rentals) follow the same structure.

## Scope

**First resource:** `ActorsResource` (`/actors`).  
**Future resources:** Films, Stores, Rentals — not in this plan.

## Test Strategy

- **`@QuarkusTest`** — Quarkus boots the full app against the local `sakila` DB (`localhost:5432`).
- **Assertion depth: envelope + shape** — verify HTTP status, `success==true`, `data` field type and
  presence of expected JSON keys on the first item. No specific row values asserted.
- **No test profile or Testcontainers** — default `application.yml` is sufficient.
- **Tool:** REST-assured (already in `pom.xml` `test` scope).

## Response Envelope

Paged list endpoints (`GET /actors`, `GET /actors/@view`):
```json
{ "success": true, "data": [...], "totalCount": N }
```

Single-item endpoint (`GET /actors/{id}`):
```json
{ "success": true, "data": { ... } }
```

## Test Class

**File:** `src/test/java/com/github/brane08/pagila/actor/ActorsResourceTest.java`  
**Annotation:** `@QuarkusTest`

### Tests

| # | Method | Endpoint | Assertions |
|---|---|---|---|
| 1 | `listActors_returns200WithPagedShape` | `GET /actors` | 200, `success==true`, `data` is array, `totalCount > 0`, first item has `actorId`, `firstName`, `lastName`, `lastUpdate` |
| 2 | `listActors_paginationRespected` | `GET /actors?size=3&page=1` | 200, `data` array size ≤ 3 |
| 3 | `getActorById_returns200WithActorShape` | `GET /actors/1` | 200, `success==true`, `data.actorId` not null, `data.firstName` not null, `data.lastName` not null |
| 4 | `getActorById_unknownId_returns404` | `GET /actors/99999` | 404 |
| 5 | `listActorViews_returns200WithViewShape` | `GET /actors/@view` | 200, `success==true`, `data` is array, `totalCount > 0`, first item has `actorId`, `firstName`, `lastName`, `filmInfo` |

### Bean shapes

`ActorInfo` (record): `actorId`, `firstName`, `lastName`, `lastUpdate`  
`ActorViewInfo` (class): `actorId`, `firstName`, `lastName`, `filmInfo`

## Test Resource Config

No additional config file needed. The existing `application.yml` targets `sakila` on `localhost:5432`.

## Build

```bash
cd quarkus-ebean && mvn test
```

Tests run via `maven-surefire-plugin` (already configured). The `ExampleResourceTest` is `@Disabled`
and will not interfere.

## Pattern for Future Resources

1. One `@QuarkusTest` class per resource, named `<Resource>ResourceTest`.
2. Package mirrors the resource package (e.g. `...film.` for `FilmsResource`).
3. Tests: list (paged shape), pagination param, get-by-id (happy + 404), any sub-collection endpoints.
4. Assertions: HTTP status + `success==true` + field presence on `data[0]` or `data`. No hardcoded values.
