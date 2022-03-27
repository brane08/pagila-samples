package com.github.brane08.pagila.rental.entities

import com.github.brane08.pagila.store.entities.Customers
import com.github.brane08.pagila.store.entities.Inventory
import com.github.brane08.pagila.store.entities.Staffs
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant

object Rentals : IntIdTable(name = "rental", columnName = "rental_id") {
	val rentalDate: Column<Instant> = timestamp("rental_date")
	val inventory = reference("inventory_id", Inventory)
	val customer = reference("customer_id", Customers)
	val returnDate: Column<Instant> = timestamp("return_date")
	val store = reference("staff_id", Staffs)
	val lastUpdate: Column<Instant> = timestamp("last_update")
}

object Payments : IntIdTable(name = "payment", columnName = "payment_id") {
	val customer = reference("customer_id", Customers)
	val staff = reference("staff_id", Staffs)
	val rental = reference("rental_id", Inventory)
	val amount: Column<BigDecimal> = decimal("amount", 5, 2)
	val paymentDate: Column<Instant> = timestamp("payment_date")
}
