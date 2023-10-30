package com.github.brane08.pagila.seedworks.repositories

import com.github.brane08.pagila.seedworks.beans.PagedList
import java.util.*

open class ExposedRepository<T, ID> : Repository<T, ID> {

    override fun findById(id: ID): Optional<T> {
        TODO("Not yet implemented")
    }

    override fun pageOffset(queryPart: String?, offset: Int, size: Int, order: String?): PagedList<T> {
        TODO("Not yet implemented")
    }

    override fun count(queryPart: String): Int {
        TODO("Not yet implemented")
    }

    override fun all(): MutableList<T> {
        TODO("Not yet implemented")
    }
}