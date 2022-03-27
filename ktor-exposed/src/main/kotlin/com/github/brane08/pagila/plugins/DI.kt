package com.github.brane08.pagila.plugins

import com.github.brane08.pagila.actor.repositories.ActorsRepository
import com.github.brane08.pagila.film.repositories.FilmsRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.bindSingleton
import org.kodein.di.ktor.di
import javax.sql.DataSource

fun Application.configureDI() {
    val dbUrl = environment.config.property("database.url").getString()
    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()
    val dbDriverCls = environment.config.property("database.driverClass").getString()
    val dbConfig = HikariConfig().apply {
        driverClassName = dbDriverCls
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        minimumIdle = 1
        maximumPoolSize = 7
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    val dataSource = HikariDataSource(dbConfig)
    val database = Database.connect(dataSource)
    di {
        bindSingleton<DataSource> { dataSource }
        bindSingleton<Database> { database }
        bindSingleton<ActorsRepository> { ActorsRepository() }
        bindSingleton<FilmsRepository> { FilmsRepository() }
    }
}