package com.github.brane08.pagila.repositories

import com.github.brane08.pagila.models.Film
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.repository.CrudRepository

@Repository
interface FilmRepository : CrudRepository<Film, Int> {
	fun list(pageable: Pageable): Slice<Film>

	@Query("select count(b.filmId) FROM Film b")
	fun countAll(): Long
}