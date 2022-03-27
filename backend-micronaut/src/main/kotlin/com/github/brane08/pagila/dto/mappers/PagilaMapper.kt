package com.github.brane08.pagila.dto.mappers

import com.github.brane08.pagila.dto.ActorDto
import com.github.brane08.pagila.dto.CategoryDto
import com.github.brane08.pagila.dto.FilmDto
import com.github.brane08.pagila.dto.LanguageDto
import com.github.brane08.pagila.models.Actor
import com.github.brane08.pagila.models.Category
import com.github.brane08.pagila.models.Film
import com.github.brane08.pagila.models.Language
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
	unmappedSourcePolicy = ReportingPolicy.WARN,
	unmappedTargetPolicy = ReportingPolicy.WARN,
	componentModel = "jakarta"
)
interface PagilaMapper {

	fun toFilmDto(source: Film): FilmDto

	fun toFilmDto(sources: List<Film>): List<FilmDto>

	fun toActorDto(source: Actor): ActorDto

	fun toActorDto(sources: List<Actor>): List<ActorDto>

	fun toCategoryDto(source: Category): CategoryDto

	fun toCategoryDto(sources: List<Category>): List<CategoryDto>

	fun toLanguageDto(source: Language): LanguageDto

}