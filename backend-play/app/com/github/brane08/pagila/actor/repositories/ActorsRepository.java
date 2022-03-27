package com.github.brane08.pagila.actor.repositories;

import com.github.brane08.pagila.actor.entities.Actor;
import play.db.jpa.JPAApi;
import com.github.brane08.pagila.seedworks.infra.DatabaseExecutionContext;
import com.github.brane08.pagila.seedworks.repositories.Repository;

import javax.inject.Inject;

public class ActorsRepository extends Repository<Actor> {

	@Inject
	public ActorsRepository(JPAApi jpaApi, DatabaseExecutionContext context) {
		super(jpaApi, context, Actor.class, "firstName");
	}
}
