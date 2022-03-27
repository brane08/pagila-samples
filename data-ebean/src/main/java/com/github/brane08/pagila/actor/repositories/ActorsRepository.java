package com.github.brane08.pagila.actor.repositories;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;


public class ActorsRepository extends EbeanRepository<Actor, Integer> {

    public ActorsRepository(Database db) {
        super(db);
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
