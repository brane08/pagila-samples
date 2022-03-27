package com.github.brane08.pagila.actor.repositories;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.repositories.JPARepository;
import jakarta.persistence.EntityManager;


public class ActorsRepository extends JPARepository<Actor, Integer> {

    public ActorsRepository(EntityManager em) {
        super(em);
    }

    @Override
    protected Class<Actor> entityClass() {
        return Actor.class;
    }

    @Override
    protected String filterProperty() {
        return "firstName";
    }
}
