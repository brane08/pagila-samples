package com.helios.mn.pagila.web

import com.helios.mn.pagila.services.ActorService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces


@Controller("/try")
@Produces(MediaType.APPLICATION_JSON)
class PingController {

    @Get("/ping")
    fun index(): HttpResponse<*> {
        return HttpResponse.ok("{\"message\":\"ok\"}")
    }
}