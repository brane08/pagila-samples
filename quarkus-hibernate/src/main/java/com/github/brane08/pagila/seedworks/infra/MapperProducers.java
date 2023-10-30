package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Dependent
public class MapperProducers {

    @Produces
    @Singleton
    public FilmMapper filmMapper() {
        return FilmMapper.INSTANCE;
    }

    @Produces
    @Singleton
    public ActorMapper actorMapper() {
        return ActorMapper.INSTANCE;
    }

    @Produces
    @Singleton
    public StoreMapper storeMapper() {
        return StoreMapper.INSTANCE;
    }
}
