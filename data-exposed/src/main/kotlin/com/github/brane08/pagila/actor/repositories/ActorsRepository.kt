package com.github.brane08.pagila.actor.repositories

import com.github.brane08.pagila.actor.beans.ActorInfo
import com.github.brane08.pagila.actor.entities.Actors
import com.github.brane08.pagila.seedworks.beans.PagedList
import com.github.brane08.pagila.seedworks.repositories.Repository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ActorsRepository : Repository<ActorInfo, Int> {

    override fun findById(id: Int): Optional<ActorInfo> {
        TODO("Not yet implemented")
    }

    override fun pageOffset(queryPart: String?, offset: Int, size: Int, order: String?): PagedList<ActorInfo> {
        return transaction {
            PagedList(
                Actors.selectAll().limit(size, offset.toLong()).map { fromRow(it) }, 0
            )
        }
    }

    override fun count(queryPart: String?): Int {
        TODO("Not yet implemented")
    }

    override fun all(): MutableList<ActorInfo> {
        TODO("Not yet implemented")
    }

    private fun fromRow(row: ResultRow) =
        ActorInfo(row[Actors.id].value, row[Actors.firstName], row[Actors.lastName], row[Actors.lastUpdate])
}