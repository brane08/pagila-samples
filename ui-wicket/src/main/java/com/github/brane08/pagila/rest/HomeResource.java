package com.github.brane08.pagila.rest;

import com.github.brane08.pagila.film.repositories.FilmsRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/home")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class HomeResource {

    @Inject
    FilmsRepository repository;

    @GET
    public Response getHello() {
        return Response.ok(repository.facets("")).build();
    }
}
