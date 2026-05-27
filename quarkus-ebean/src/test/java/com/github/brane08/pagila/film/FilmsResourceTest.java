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
            .body("data.size()", equalTo(0))
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
