package com.github.brane08.pagila.home.app;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class HomeResource {

    @Inject
    Template home;

    @GET
    public TemplateInstance home() {
        return home.instance();
    }
}
