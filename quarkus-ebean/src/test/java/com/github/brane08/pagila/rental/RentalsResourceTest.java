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
