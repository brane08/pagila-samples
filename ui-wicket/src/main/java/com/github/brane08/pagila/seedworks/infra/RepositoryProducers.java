package com.github.brane08.pagila.seedworks.infra;

import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.List;

@Dependent
public class RepositoryProducers {

    @Produces
    @Singleton
    public Database ebeanDatabase() {
        DataSourceConfig dsConfig = new DataSourceConfig();
        dsConfig.setDriver("org.postgresql.Driver");
        dsConfig.setAutoCommit(false);
        dsConfig.setUrl("jdbc:postgresql://localhost:5432/sakila");
        dsConfig.setUsername("postgres");
        dsConfig.setPassword("");
        dsConfig.setMaxConnections(7);
        dsConfig.setMinConnections(1);

        DatabaseConfig config = new DatabaseConfig();
        config.setDataSourceConfig(dsConfig);
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
