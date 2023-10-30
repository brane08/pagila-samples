package com.github.brane08.pagila.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.ktor.closestDI

fun Application.configureRouting() {
    val di = closestDI()
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
