# quarkus-ebean ActorsResource Integration Tests — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add five `@QuarkusTest` integration tests for `ActorsResource` verifying HTTP status and JSON response shape against the live sakila database.

**Architecture:** Single test class using Quarkus test runner + REST-assured. Quarkus boots the full app; REST-assured queries it. No mocks, no test profile — the default `application.yml` (localhost:5432/sakila) is used as-is. Implementation already exists; tasks write and verify tests only.

**Tech Stack:** Quarkus 3.35.2 `@QuarkusTest`, REST-assured, JUnit 5, Hamcrest Matchers (all already in `pom.xml` test scope).

---

### Task 1: Write list + pagination tests

**Files:**
- Create: `quarkus-ebean/src/test/java/com/github/brane08/pagila/actor/ActorsResourceTest.java`

- [ ] **Step 1: Create the test class with tests 1 and 2**

Create `quarkus-ebean/src/test/java/com/github/brane08/pagila/actor/ActorsResourceTest.java`:

```java
package com.github.brane08.pagila.actor;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ActorsResourceTest {

    @Test
    void listActors_returns200WithPagedShape() {
        given()
            .when().get("/actors")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data", notNullValue())
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].actorId", notNullValue())
            .body("data[0].firstName", notNullValue())
            .body("data[0].lastName", notNullValue())
            .body("data[0].lastUpdate", notNullValue());
    }

    @Test
    void listActors_paginationRespected() {
        given()
            .queryParam("size", 3)
            .queryParam("page", 1)
            .when().get("/actors")
            .then()
            .statusCode(200)
            .body("data.size()", lessThanOrEqualTo(3));
    }
}
```

- [ ] **Step 2: Run the two tests against the live DB**

Prerequisite: PostgreSQL sakila DB running on localhost:5432.

```bash
cd quarkus-ebean && mvn test -Dtest="ActorsResourceTest#listActors_returns200WithPagedShape+listActors_paginationRespected"
```

Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

---

### Task 2: Add get-by-ID and 404 tests

**Files:**
- Modify: `quarkus-ebean/src/test/java/com/github/brane08/pagila/actor/ActorsResourceTest.java`

- [ ] **Step 1: Add tests 3 and 4 inside `ActorsResourceTest`**

Add these two methods inside the class after `listActors_paginationRespected`:

```java
    @Test
    void getActorById_returns200WithActorShape() {
        given()
            .when().get("/actors/1")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.actorId", notNullValue())
            .body("data.firstName", notNullValue())
            .body("data.lastName", notNullValue());
    }

    @Test
    void getActorById_unknownId_returns404() {
        given()
            .when().get("/actors/99999")
            .then()
            .statusCode(404);
    }
```

- [ ] **Step 2: Run the two new tests**

```bash
cd quarkus-ebean && mvn test -Dtest="ActorsResourceTest#getActorById_returns200WithActorShape+getActorById_unknownId_returns404"
```

Expected: `BUILD SUCCESS`, 2 tests run, 0 failures.

---

### Task 3: Add actor view list test, run all five, commit

**Files:**
- Modify: `quarkus-ebean/src/test/java/com/github/brane08/pagila/actor/ActorsResourceTest.java`

- [ ] **Step 1: Add test 5 inside `ActorsResourceTest`**

Add this method after `getActorById_unknownId_returns404`, before the closing `}` of the class:

```java
    @Test
    void listActorViews_returns200WithViewShape() {
        given()
            .when().get("/actors/@view")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data", notNullValue())
            .body("data.size()", greaterThan(0))
            .body("totalCount", greaterThan(0))
            .body("data[0].actorId", notNullValue())
            .body("data[0].firstName", notNullValue())
            .body("data[0].lastName", notNullValue())
            .body("data[0].filmInfo", notNullValue());
    }
```

- [ ] **Step 2: Run all five tests**

```bash
cd quarkus-ebean && mvn test -Dtest=ActorsResourceTest
```

Expected: `BUILD SUCCESS`, 5 tests run, 0 failures.

- [ ] **Step 3: Commit**

```bash
git add quarkus-ebean/src/test/java/com/github/brane08/pagila/actor/ActorsResourceTest.java
git commit -m "test: ActorsResource @QuarkusTest integration tests (5 tests, envelope+shape)"
```
