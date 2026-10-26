package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.seedworks.query.InvalidQueryException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidQueryExceptionMapperUnitTest {

    @Test
    void mapsToBadRequestWithMessage() {
        Response response = new InvalidQueryExceptionMapper()
                .toResponse(new InvalidQueryException("nope", new IllegalArgumentException()));

        assertEquals(400, response.getStatus());
        assertEquals(Map.of("error", "Unknown field: nope"), response.getEntity());
    }
}
