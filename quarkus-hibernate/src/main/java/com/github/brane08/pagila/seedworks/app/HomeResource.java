package com.github.brane08.pagila.seedworks.app;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Manage a database of computers
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class HomeResource {

    @Inject
    public HomeResource() {
    }

    /**
     * Handle default path requests, redirect to computers list
     */
    @GET
    public Response index() {
        return Response.ok("Hello from App!").build();
    }
}
            
