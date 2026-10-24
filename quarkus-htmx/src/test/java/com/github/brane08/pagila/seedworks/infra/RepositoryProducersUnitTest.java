package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryProducersUnitTest {

    @Mock
    EntityManager em;
    @Mock
    FilmMapper filmMapper;

    RepositoryProducers producers;

    @BeforeEach
    void setUp() {
        producers = new RepositoryProducers();
        producers.em = em;
    }

    @Test
    void filmsRepositoryIsCreatedFromEntityManagerAndMapper() {
        FilmsRepository repository = producers.filmsRepository(filmMapper);
        assertNotNull(repository);
        assertInstanceOf(FilmsRepository.class, repository);
    }

    @Test
    void actorsRepositoryIsCreated() {
        assertInstanceOf(ActorsRepository.class, producers.actorsRepository());
    }

    @Test
    void storeRepositoryIsCreated() {
        assertInstanceOf(StoreRepository.class, producers.storeRepository());
    }

    @Test
    void rentalRepositoryIsCreated() {
        assertInstanceOf(RentalRepository.class, producers.rentalRepository());
    }
}
