package com.github.brane08.pagila.store.entities

import com.github.brane08.pagila.film.entities.Films
import com.github.brane08.pagila.seedworks.entities.Addresses
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.time.Instant

object Stores : IntIdTable(name = "store", columnName = "store_id") {
	val address = reference("address_id", Addresses)
	val managerStaff = reference("manager_staff_id", Staffs)
	val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Staffs : IntIdTable(name = "staff", columnName = "staff_id") {
	val firstName: Column<String> = varchar("first_name", 50)
	val lastName: Column<String> = varchar("last_name", 50)
	val address = reference("address_id", Addresses)
	val email: Column<String> = varchar("email", 50)
	val store = reference("store_id", Stores)
	val active: Column<Boolean> = bool("active")
	val userName: Column<String> = varchar("username", 50)
	val password: Column<String> = varchar("password", 50)
	val picture: Column<ExposedBlob> = blob("picture")
	val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Inventory : IntIdTable(name = "inventory", columnName = "inventory_id") {
	val address = reference("address_id", Addresses)
	val film = reference("film_id", Films)
	val store = reference("store_id", Stores)
	val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Customers : IntIdTable(name = "customer", columnName = "customer_id") {
	val store = reference("store_id", Stores)
	val firstName: Column<String> = varchar("first_name", 50)
	val lastName: Column<String> = varchar("last_name", 50)
	val email: Column<String> = varchar("email", 50)
	val address = reference("address_id", Addresses)
	val activeVal: Column<Int> = integer("active")
	val created: Column<Instant> = timestamp("create_date")
	val lastUpdate: Column<Instant> = timestamp("last_update")
}
