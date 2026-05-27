package com.github.brane08.pagila.actor.entities

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Actors : IntIdTable(name = "actor", columnName = "actor_id") {
    val firstName: Column<String> = varchar("first_name", 50)
    val lastName: Column<String> = varchar("last_name", 50)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}