package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.config.DatabaseConfig;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.util.List;

@Factory
public class RepositoryProducers {

    @Singleton
    public Database ebeanDatabase(DataSource dataSource) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(dataSource);
        config.setDatabasePlatformName(Platform.POSTGRES.name());
        config.setPackages(List.of("com.github.brane08.pagila.actor.entities",
                "com.github.brane08.pagila.film.entities", "com.github.brane08.pagila.rental.entities",
                "com.github.brane08.pagila.seedworks.entities", "com.github.brane08.pagila.store.entities"));
        return DatabaseFactory.create(config);
    }

    @Singleton
    public FilmsRepository filmsRepository(Database db) {
        return new FilmsRepository(db);
    }

    @Singleton
    public ActorsRepository actorsRepository(Database db) {
        return new ActorsRepository(db);
    }

    @Singleton
    public StoreRepository storeRepository(Database db) {
        return new StoreRepository(db);
    }

    @Singleton
    public RentalRepository rentalRepository(Database db) {
        return new RentalRepository(db);
    }
}
