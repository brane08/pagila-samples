# quarkus-ebean Remaining Resource Integration Tests — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `@QuarkusTest` integration tests for `FilmsResource` (9 tests), `StoresResource` (8 tests), and `RentalsResource` (2 tests), following the `ActorsResourceTest` pattern.

**Architecture:** One `@QuarkusTest` class per resource. Each class runs against the live `sakila` DB (localhost:5432). Assertions verify HTTP status, `success==true`, field presence on first item — no hardcoded data values except IDs known to exist. Two resources (`Films`, `Stores`) need a `\d+` regex added to their `{id}` path param to prevent routing ambiguity with `@`-prefixed routes. Pattern established in `ActorsResourceTest` (commit `e788ae9b`).

**Tech Stack:** Quarkus 3.35.2 `@QuarkusTest`, REST-assured, JUnit 5, Hamcrest Matchers (all already in `pom.xml` test scope). Working directory: `/Users/bhushanr/incubator/samples/pagila-samples`.

---

### Task 1: FilmsResource routing fix + FilmsResourceTest (9 tests)

**Files:**
- Modify: `quarkus-ebean/src/main/java/com/github/brane08/pagila/film/app/FilmsResource.java`
- Create: `quarkus-ebean/src/test/java/com/github/brane08/pagila/film/FilmsResourceTest.java`

- [ ] **Step 1: Fix routing ambiguity in FilmsResource.java**

`FilmsResource` has `@Path("{film_id}")` (no `\d+` constraint). Without the constraint, REST-assured's URL-encoded `@` (`%40view`) matches `{film_id}`, breaking `@view` and `@nicer-view` tests. Add the constraint to both path-param methods.

In `quarkus-ebean/src/main/java/com/github/brane08/pagila/film/app/FilmsResource.java`, change:

```java
    @GET
    @Path("{film_id}")
    public Response getById(@PathParam("film_id") int filmId) {
```
to:
```java
    @GET
    @Path("{film_id: \\d+}")
    public Response getById(@PathParam("film_id") int filmId) {
```

And change:
```java
    @GET
    @Path("/{film_id}/actors")
    public Response listActors(@PathParam("film_id") int filmId) {
```
to:
```java
    @GET
    @Path("/{film_id: \\d+}/actors")
    public Response listActors(@PathParam("film_id") int filmId) {
```

- [ ] **Step 2: Create FilmsResourceTest.java**

Create `quarkus-ebean/src/test/java/com/github/brane08/pagila/film/FilmsResourceTest.java`:

```java
package com.github.brane08.pagila.film;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class FilmsResourceTest {

    @Test
    void listFilms_returns200WithPagedShape() {
        given()
            .when().get("/films")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].title", notNullValue())
            .body("data[0].description", notNullValue())
            .body("data[0].rating", notNullValue());
    }

    @Test
    void listFilms_pageSizeRespected() {
        given()
            .queryParam("size", 3)
            .queryParam("page", 1)
            .when().get("/films")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", lessThanOrEqualTo(3));
    }

    @Test
    void getFilmById_returns200WithTitle() {
        // GET /films/{id} returns the Film entity directly (no ApiResult wrapper)
        given()
            .when().get("/films/1")
            .then()
            .statusCode(200)
            .body("title", notNullValue());
    }

    @Test
    void getFilmCount_returns200WithCount() {
        given()
            .when().get("/films/count")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("totalCount", greaterThan(0));
    }

    @Test
    void listFilmFacets_returns200WithFacetShape() {
        given()
            .when().get("/films/facets")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].property", notNullValue());
    }

    @Test
    void listFilmViews_returns200WithViewShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@view path
            .when().get("/films/@view")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].title", notNullValue())
            .body("data[0].category", notNullValue());
    }

    @Test
    void listFilmActors_returns200WithActorShape() {
        given()
            .when().get("/films/1/actors")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].actorId", notNullValue());
    }

    @Test
    void listNicerFilmViews_returns200WithViewShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@nicer-view path
            .when().get("/films/@nicer-view")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].title", notNullValue())
            .body("data[0].category", notNullValue());
    }

    @Test
    void listSalesByCategory_returns200WithSalesShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@sales-by-category path
            .when().get("/films/@sales-by-category")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].category", notNullValue())
            .body("data[0].totalSales", notNullValue());
    }
}
```

- [ ] **Step 3: Run FilmsResourceTest against the live DB**

Prerequisite: PostgreSQL sakila DB running on localhost:5432.

```bash
cd /Users/bhushanr/incubator/samples/pagila-samples/quarkus-ebean && mvn test -Dtest=FilmsResourceTest
```

Expected: `BUILD SUCCESS`, 9 tests run, 0 failures.

---

### Task 2: StoresResource routing fix + StoresResourceTest (8 tests)

**Files:**
- Modify: `quarkus-ebean/src/main/java/com/github/brane08/pagila/store/app/StoresResource.java`
- Create: `quarkus-ebean/src/test/java/com/github/brane08/pagila/store/StoresResourceTest.java`

- [ ] **Step 1: Fix routing ambiguity in StoresResource.java**

`StoresResource` has `@Path("/{storeId}")` competing with `/@sales-by-store`, `/@staff`, `/@view`. Without `\d+`, REST-assured's URL-encoded `@` would match the store ID param.

In `quarkus-ebean/src/main/java/com/github/brane08/pagila/store/app/StoresResource.java`, change:

```java
    @GET
    @Path("/{storeId}")
    public Response getById(@PathParam("storeId") int storeId) {
```
to:
```java
    @GET
    @Path("/{storeId: \\d+}")
    public Response getById(@PathParam("storeId") int storeId) {
```

Also update the three sub-resource paths on the same resource to use the constraint:

```java
    @GET
    @Path("/{storeId}/inventory")
    public Response storeInventory(@PathParam("storeId") int storeId) {
```
to:
```java
    @GET
    @Path("/{storeId: \\d+}/inventory")
    public Response storeInventory(@PathParam("storeId") int storeId) {
```

```java
    @GET
    @Path("/{storeId}/rentals")
    public Response storeRentals(@PathParam("storeId") int storeId) {
```
to:
```java
    @GET
    @Path("/{storeId: \\d+}/rentals")
    public Response storeRentals(@PathParam("storeId") int storeId) {
```

```java
    @GET
    @Path("/{storeId}/customers")
    public Response storeCustomers(@PathParam("storeId") int storeId) {
```
to:
```java
    @GET
    @Path("/{storeId: \\d+}/customers")
    public Response storeCustomers(@PathParam("storeId") int storeId) {
```

- [ ] **Step 2: Create StoresResourceTest.java**

Create `quarkus-ebean/src/test/java/com/github/brane08/pagila/store/StoresResourceTest.java`:

```java
package com.github.brane08.pagila.store;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class StoresResourceTest {

    @Test
    void listStores_returns200WithPagedShape() {
        given()
            .when().get("/stores")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].storeId", notNullValue());
    }

    @Test
    void listStores_pageSizeRespected() {
        given()
            .queryParam("size", 1)
            .queryParam("page", 1)
            .when().get("/stores")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", lessThanOrEqualTo(1));
    }

    @Test
    void getSalesByStore_returns200WithSalesShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@sales-by-store path
            .when().get("/stores/@sales-by-store")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].store", notNullValue())
            .body("data[0].totalSales", notNullValue());
    }

    @Test
    void listStaffViews_returns200WithStaffShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@staff path
            .when().get("/stores/@staff")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].id", notNullValue())
            .body("data[0].name", notNullValue());
    }

    @Test
    void listStoreViews_returns200WithViewShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@view path
            .when().get("/stores/@view")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].storeId", notNullValue())
            .body("data[0].manager", notNullValue());
    }

    @Test
    void getStoreById_returns200WithStoreShape() {
        given()
            .when().get("/stores/1")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.storeId", equalTo(1));
    }

    @Test
    void getStoreById_unknownId_returns404() {
        given()
            .when().get("/stores/99999")
            .then()
            .statusCode(404);
    }

    @Test
    void getStoreInventory_returns200WithInventoryShape() {
        given()
            .when().get("/stores/1/inventory")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("data[0].filmId", notNullValue())
            .body("data[0].title", notNullValue());
    }
}
```

- [ ] **Step 3: Run StoresResourceTest against the live DB**

```bash
cd /Users/bhushanr/incubator/samples/pagila-samples/quarkus-ebean && mvn test -Dtest=StoresResourceTest
```

Expected: `BUILD SUCCESS`, 8 tests run, 0 failures.

---

### Task 3: RentalsResourceTest (2 tests) + run all + commit

**Files:**
- Create: `quarkus-ebean/src/test/java/com/github/brane08/pagila/rental/RentalsResourceTest.java`

- [ ] **Step 1: Create RentalsResourceTest.java**

`RentalsResource` has no `{id}` path param competing with `@customers`, so no production code fix is needed.

Create `quarkus-ebean/src/test/java/com/github/brane08/pagila/rental/RentalsResourceTest.java`:

```java
package com.github.brane08.pagila.rental;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class RentalsResourceTest {

    @Test
    void listRentals_returns200WithPagedShape() {
        given()
            .when().get("/rentals")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].rentalDate", notNullValue());
    }

    @Test
    void listCustomerViews_returns200WithViewShape() {
        given()
            .urlEncodingEnabled(false)
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@customers path
            .when().get("/rentals/@customers")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].id", notNullValue())
            .body("data[0].name", notNullValue())
            .body("data[0].city", notNullValue());
    }
}
```

- [ ] **Step 2: Run all three new test classes together**

```bash
cd /Users/bhushanr/incubator/samples/pagila-samples/quarkus-ebean && mvn test -Dtest="FilmsResourceTest,StoresResourceTest,RentalsResourceTest"
```

Expected: `BUILD SUCCESS`, 19 tests run, 0 failures.

- [ ] **Step 3: Commit everything**

```bash
cd /Users/bhushanr/incubator/samples/pagila-samples
git add \
  quarkus-ebean/src/main/java/com/github/brane08/pagila/film/app/FilmsResource.java \
  quarkus-ebean/src/main/java/com/github/brane08/pagila/store/app/StoresResource.java \
  quarkus-ebean/src/test/java/com/github/brane08/pagila/film/FilmsResourceTest.java \
  quarkus-ebean/src/test/java/com/github/brane08/pagila/store/StoresResourceTest.java \
  quarkus-ebean/src/test/java/com/github/brane08/pagila/rental/RentalsResourceTest.java
git commit -m "test: Films/Stores/Rentals @QuarkusTest integration tests (19 tests, envelope+shape)"
```
