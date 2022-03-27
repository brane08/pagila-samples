package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.config.DatabaseConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;

import javax.sql.DataSource;
import java.util.List;

@Dependent
public class RepositoryProducers {

    @Produces
    @ApplicationScoped
    public Database ebeanDatabase(DataSource dataSource) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(dataSource);
        config.setDatabasePlatformName(Platform.POSTGRES.name());
        config.setPackages(List.of("com.github.brane08.pagila.actor.entities",
                "com.github.brane08.pagila.film.entities", "com.github.brane08.pagila.rental.entities",
                "com.github.brane08.pagila.seedworks.entities", "com.github.brane08.pagila.store.entities"));
        return DatabaseFactory.create(config);
    }

    @Produces
    @Singleton
    public FilmsRepository filmsRepository(Database db) {
        return new FilmsRepository(db);
    }

    @Produces
    @Singleton
    public ActorsRepository actorsRepository(Database db) {
        return new ActorsRepository(db);
    }

    @Produces
    @Singleton
    public StoreRepository storeRepository(Database db) {
        return new StoreRepository(db);
    }

    @Produces
    @Singleton
    public RentalRepository rentalRepository(Database db) {
        return new RentalRepository(db);
    }
}
