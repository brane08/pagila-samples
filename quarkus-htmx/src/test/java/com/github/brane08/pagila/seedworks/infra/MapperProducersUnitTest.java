package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MapStruct implementations are generated at compile time by mapstruct-processor in data-hibernate
 * and loaded via Mappers.getMapper, so these tests exercise real generated mapper instances.
 */
class MapperProducersUnitTest {

    private final MapperProducers producers = new MapperProducers();

    @Test
    void filmMapperIsGeneratedImplementation() {
        FilmMapper mapper = producers.filmMapper();
        assertNotNull(mapper);
        assertInstanceOf(FilmMapper.class, mapper);
    }

    @Test
    void actorMapperIsGeneratedImplementation() {
        ActorMapper mapper = producers.actorMapper();
        assertNotNull(mapper);
        assertInstanceOf(ActorMapper.class, mapper);
    }

    @Test
    void storeMapperIsGeneratedImplementation() {
        StoreMapper mapper = producers.storeMapper();
        assertNotNull(mapper);
        assertInstanceOf(StoreMapper.class, mapper);
    }

    @Test
    void rentalMapperIsGeneratedImplementation() {
        RentalMapper mapper = producers.rentalMapper();
        assertNotNull(mapper);
        assertInstanceOf(RentalMapper.class, mapper);
    }
}
