package com.github.brane08.pagila.dto

import com.github.brane08.pagila.models.Actor
import com.github.brane08.pagila.models.Category
import com.github.brane08.pagila.models.Language
import java.time.Year

class FilmDto {
	var filmId: Int? = null
	var title: String? = null
	var description: String? = null
	var releaseYear: Year? = null
	var language: Language? = null
	var originalLanguage: Language? = null
	var rentalDuration: Int? = null
	var rentalRate: Double? = null
	var length: Int? = null
	var replacementCost: Double? = null
	var rating: String? = null
	var specialFeatures: List<String> = mutableListOf()
	var categories: List<Category> = mutableListOf()
	var actors: List<Actor> = mutableListOf()
}