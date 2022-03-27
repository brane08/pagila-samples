package com.github.brane08.pagila.actor

import com.github.brane08.pagila.actor.repositories.ActorsRepository
import com.github.brane08.pagila.seedworks.beans.PageInfo
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

fun Application.actorRouting() {
    val di = closestDI()

    routing {
        route("/actors") {
            get {
                val repository by di.instance<ActorsRepository>()
                call.respond(repository.page("", PageInfo(1, 20, "id")))
            }
        }
    }
}