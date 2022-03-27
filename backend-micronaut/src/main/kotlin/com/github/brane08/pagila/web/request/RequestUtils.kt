package com.github.brane08.pagila.web.request

import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.micronaut.http.HttpRequest

object RequestUtils {

	fun toPage(request: HttpRequest<*>): Pageable {
		val otherProps = HashMap<String, String?>()
		var number: Int = 0
		var size: Int = 10
		val orders = mutableListOf<Sort.Order>()
		request.parameters.forEach { k, v ->

			if (k.equals("number", true)) {
				number = v?.getOrElse(0) { "0" }?.toInt()!!
			} else if (k.equals("size", true)) {
				size = v?.getOrElse(0) { "0" }?.toInt()!!
			} else if (k.equals("sort", true)) {
				v?.forEach { i ->
					i?.let {
						if (i.startsWith("-"))
							orders.add(Sort.Order.desc(i.trimStart('-')))
						else
							orders.add(Sort.Order.asc(i.trimStart('-')))
					}
				}
			} else {
				v?.firstOrNull()?.let { otherProps.put(k, it) }
			}
		}
		return Pageable.from(number, size, Sort.of(orders))
	}
}