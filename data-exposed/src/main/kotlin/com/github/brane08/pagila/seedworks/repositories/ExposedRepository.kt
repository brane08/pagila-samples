package com.github.brane08.pagila.seedworks.repositories

import com.github.brane08.pagila.seedworks.beans.PageInfo
import com.github.brane08.pagila.seedworks.beans.PagedList
import java.util.*

open class ExposedRepository<T, ID> : Repository<T, ID> {

	override fun findById(id: ID): Optional<T> {
		TODO("Not yet implemented")
	}

	override fun page(queryPart: String, request: PageInfo): PagedList<T> {
		TODO("Not yet implemented")
	}

	override fun count(queryPart: String): Int {
		TODO("Not yet implemented")
	}
}