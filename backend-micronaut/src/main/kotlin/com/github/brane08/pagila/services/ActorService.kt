package com.helios.mn.pagila.services

import jakarta.inject.Singleton
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Singleton
class ActorService(@PersistenceContext var entityManager: EntityManager) {

    fun getActors(): List<*> {
        println(entityManager.createNativeQuery("select count(*) from actor").singleResult)
        return listOf<Int>()
    }
}