package com.github.brane08.pagila

import com.github.brane08.pagila.actor.actorRouting
import com.github.brane08.pagila.plugins.configureDI
import com.github.brane08.pagila.plugins.configureRouting
import com.github.brane08.pagila.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused")
fun Application.module() {
    configureDI()
    configureSerialization()

    configureRouting()
    actorRouting()
}
