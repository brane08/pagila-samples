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
    void listActors_pageSizeRespected() {
        given()
            .queryParam("size", 3)
            .queryParam("page", 1)
            .when().get("/actors")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.size()", lessThanOrEqualTo(3));
    }

    @Test
    void getActorById_returns200WithActorShape() {
        given()
            .when().get("/actors/1")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.actorId", equalTo(1))
            .body("data.firstName", notNullValue())
            .body("data.lastName", notNullValue())
            .body("data.filmInfo", notNullValue());
    }

    @Test
    void getActorById_unknownId_returns404() {
        given()
            .when().get("/actors/99999")
            .then()
            .statusCode(404);
    }

    @Test
    void listActorViews_returns200WithViewShape() {
        given()
            // RestAssured encodes '@' as '%40' by default; disable to match the literal /@view path
            .urlEncodingEnabled(false)
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
}
