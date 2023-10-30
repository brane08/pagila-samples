package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Dependent
public class RepositoryProducers {

    @PersistenceContext
    EntityManager em;

    @Produces
    @Singleton
    public FilmsRepository filmsRepository(FilmMapper mapper) {
        return new FilmsRepository(em, mapper);
    }

    @Produces
    @Singleton
    public ActorsRepository actorsRepository() {
        return new ActorsRepository(em);
    }

    @Produces
    @Singleton
    public StoreRepository storeRepository() {
        return new StoreRepository(em);
    }
}
