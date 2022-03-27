package com.github.brane08.pagila.dto.response

import com.github.brane08.pagila.dto.CountDto
import io.micronaut.data.model.Pageable

open class ApiResponse<T>(
	var success: Boolean = true, var message: String? = null, var data: T? = null
) {

	companion object {
		fun <E> paged(current: Pageable, data: List<E>): PageResponse<E> {
			return PageResponse(current, data)
		}

		fun count(total: Long?): ApiResponse<CountDto> {
			return ApiResponse(data = CountDto(total))
		}
	}
}

class PageResponse<T>(var current: Pageable, data: List<T>) : ApiResponse<List<T>>(data = data)