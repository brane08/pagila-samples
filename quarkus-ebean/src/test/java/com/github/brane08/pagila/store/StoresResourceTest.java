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
