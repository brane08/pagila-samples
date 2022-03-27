package com.github.brane08.pagila

import com.github.brane08.pagila.actor.actorRouting
import com.github.brane08.pagila.actor.repositories.ActorsRepository
import com.github.brane08.pagila.film.repositories.FilmsRepository
import com.github.brane08.pagila.plugins.configureDI
import com.github.brane08.pagila.plugins.configureRouting
import com.github.brane08.pagila.plugins.configureSerialization
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import org.kodein.di.ktor.di
import javax.sql.DataSource

fun main(args: Array<String>): Unit =
	io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused")
fun Application.module() {
	configureDI()
	configureSerialization()

	configureRouting()
	actorRouting()
}
