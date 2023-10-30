package com.github.brane08.pagila.film.entities

import com.github.brane08.pagila.actor.entities.Actors
import com.github.brane08.pagila.film.beans.FilmInfo
import com.github.brane08.pagila.seedworks.entities.Categories
import com.github.brane08.pagila.seedworks.entities.Languages
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.Year

object Films : IntIdTable(name = "film", columnName = "film_id") {
    val title: Column<String> = varchar("title", 50)
    val description: Column<String> = varchar("description", 100)
    val releaseYear: Column<Int> = integer("release_year")
    val language = reference("language_id", Languages)
    val originalLanguage = reference("original_language_id", Languages)
    val rentalDuration: Column<Int> = integer("rental_duration")
    val rate: Column<BigDecimal> = decimal("rental_rate", 4, 2)
    val length: Column<Int> = integer("length")
    val cost: Column<BigDecimal> = decimal("replacement_cost", 5, 2)
    val rating: Column<String> = varchar("rating_txt", 30)
    val lastUpdate: Column<Instant> = timestamp("last_update")

    fun mapToDto(row: ResultRow): FilmInfo {
        val info = FilmInfo()
        info.filmId = row[Films.id].value
        info.title = row[title]
        info.description = row[description]
        info.releaseYear = Year.of(row[releaseYear])
        info.rentalDuration = row[rentalDuration]
        info.rentalRate = row[rate].toDouble()
        info.length = row[length]
        info.replacementCost = row[cost].toDouble()
        info.rating = row[rating]
        info.lastUpdate = row[lastUpdate]
        return info
    }
}

object FilmActor : Table(name = "film_actor") {
    val actor = reference("actor_id", Actors)
    val film = reference("film_id", Films)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}

object FilmCategory : Table(name = "film_category") {
    val category = reference("category_id", Categories)
    val film = reference("film_id", Films)
    val lastUpdate: Column<Instant> = timestamp("last_update")
}
