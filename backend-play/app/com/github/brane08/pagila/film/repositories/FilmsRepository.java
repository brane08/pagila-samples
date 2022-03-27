package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.seedworks.infra.DatabaseExecutionContext;
import com.github.brane08.pagila.seedworks.repositories.Repository;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.List;

public class FilmsRepository extends Repository<Film> {
	@Inject
	public FilmsRepository(JPAApi jpaApi, DatabaseExecutionContext context) {
		super(jpaApi, context, Film.class, "title");
	}

	@Override
	protected List<String> getLoadFields() {
		return List.of("categories", "language", "originalLanguage");
	}
}
