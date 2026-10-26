package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.seedworks.query.InvalidQueryException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class InvalidQueryExceptionMapper implements ExceptionMapper<InvalidQueryException> {

    @Override
    public Response toResponse(InvalidQueryException ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", ex.getMessage()))
                .build();
    }
}
