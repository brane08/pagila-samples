package com.github.brane08.pagila.web

import com.github.brane08.pagila.dto.CountDto
import com.github.brane08.pagila.dto.FilmDto
import com.github.brane08.pagila.dto.response.ApiResponse
import com.github.brane08.pagila.dto.response.PageResponse
import com.github.brane08.pagila.services.FilmService
import com.github.brane08.pagila.web.request.RequestUtils
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

@Controller("/films")
@Produces(MediaType.APPLICATION_JSON)
class FilmController(private val filmService: FilmService) {

	@Get
	fun list(request: HttpRequest<String>): HttpResponse<PageResponse<FilmDto>> {
		return HttpResponse.ok(filmService.list(RequestUtils.toPage(request)));
	}

	@Get("counts")
	fun counts(): HttpResponse<ApiResponse<CountDto>> {
		return HttpResponse.ok(filmService.counts());
	}
}