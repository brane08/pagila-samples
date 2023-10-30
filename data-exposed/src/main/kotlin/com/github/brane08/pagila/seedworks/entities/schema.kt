package com.github.brane08.pagila.seedworks.entities

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Languages : IntIdTable(name = "language", columnName = "language_id") {
    val name: Column<String> = varchar("name", 50)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Countries : IntIdTable(name = "country", columnName = "country_id") {
    val country: Column<String> = varchar("country", 50)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Cities : IntIdTable(name = "city", columnName = "city_id") {
    val city: Column<String> = varchar("city", 50)
    val country = reference("country_id", Countries)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Addresses : IntIdTable(name = "address", columnName = "address_id") {
    val address: Column<String> = varchar("address", 50)
    val address2: Column<String> = varchar("address2", 100)
    val district: Column<String> = varchar("district", 30)
    val city = reference("city_id", Cities)
    val postalCode: Column<String> = varchar("postal_code", 10)
    val phone: Column<String> = varchar("phone", 12)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Categories : IntIdTable(name = "category", columnName = "category_id") {
    val name: Column<String> = varchar("name", 50)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}
