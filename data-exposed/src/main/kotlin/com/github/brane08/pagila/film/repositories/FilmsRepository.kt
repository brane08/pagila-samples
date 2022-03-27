package com.github.brane08.pagila.film.repositories

import com.github.brane08.pagila.actor.beans.ActorInfo
import com.github.brane08.pagila.seedworks.beans.PageInfo
import com.github.brane08.pagila.seedworks.beans.PagedList
import com.github.brane08.pagila.seedworks.repositories.Repository
import java.util.*

class FilmsRepository: Repository<ActorInfo, Int> {
    override fun findById(id: Int?): Optional<ActorInfo> {
        TODO("Not yet implemented")
    }

    override fun page(queryPart: String?, request: PageInfo?): PagedList<ActorInfo> {
        TODO("Not yet implemented")
    }

    override fun count(queryPart: String?): Int {
        TODO("Not yet implemented")
    }
}