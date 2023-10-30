package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.mapstruct.factory.Mappers;

@Dependent
public class MapperProducers {

    @Produces
    @Singleton
    public FilmMapper filmMapper() {
        return Mappers.getMapper(FilmMapper.class);
    }

    @Produces
    @Singleton
    public ActorMapper actorMapper() {
        return Mappers.getMapper(ActorMapper.class);
    }

    @Produces
    @Singleton
    public StoreMapper storeMapper() {
        return Mappers.getMapper(StoreMapper.class);
    }

    @Produces
    @Singleton
    public RentalMapper rentalMapper() {
        return Mappers.getMapper(RentalMapper.class);
    }
}
