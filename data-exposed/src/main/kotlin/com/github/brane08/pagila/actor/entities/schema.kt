package com.github.brane08.pagila.actor.entities

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Actors : IntIdTable(name = "actor", columnName = "actor_id") {
	val firstName: Column<String> = varchar("first_name", 50)
	val lastName: Column<String> = varchar("last_name", 50)
	val lastUpdate: Column<Instant> = timestamp("last_update")
}