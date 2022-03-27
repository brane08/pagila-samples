package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.mapstruct.factory.Mappers;

@Factory
public class MapperProducers {

    @Singleton
    public FilmMapper filmMapper() {
        return Mappers.getMapper(FilmMapper.class);
    }

    @Singleton
    public ActorMapper actorMapper() {
        return Mappers.getMapper(ActorMapper.class);
    }

    @Singleton
    public StoreMapper storeMapper() {
        return Mappers.getMapper(StoreMapper.class);
    }

    @Singleton
    public RentalMapper rentalMapper() {
        return Mappers.getMapper(RentalMapper.class);
    }
}
