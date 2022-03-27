package com.github.brane08.pagila.services

import com.github.brane08.pagila.dto.CountDto
import com.github.brane08.pagila.dto.FilmDto
import com.github.brane08.pagila.dto.mappers.PagilaMapper
import com.github.brane08.pagila.dto.response.ApiResponse
import com.github.brane08.pagila.dto.response.PageResponse
import com.github.brane08.pagila.repositories.FilmRepository
import io.micronaut.data.model.Pageable
import jakarta.inject.Singleton
import javax.transaction.Transactional

@Singleton
class FilmService(private val filmRepository: FilmRepository, private val mapper: PagilaMapper) {

	@Transactional
	fun list(paged: Pageable): PageResponse<FilmDto> {
		return ApiResponse.paged(paged, mapper.toFilmDto(filmRepository.list(paged).content));
	}

	@Transactional
	fun counts(): ApiResponse<CountDto> {
		return ApiResponse.count(filmRepository.countAll());
	}
}