package com.github.brane08.pagila.loader;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.config.DatabaseConfig;

import java.util.List;

public final class DatabaseBootstrap {

    private DatabaseBootstrap() {
    }

    public static Database open() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5432");
        String name = env("DB_NAME", "sakila");
        String user = env("DB_USER", "postgres");
        String password = env("DB_PASSWORD", "");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(4);

        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(new HikariDataSource(hikariConfig));
        config.setDatabasePlatformName(Platform.POSTGRES.name());
        config.setPackages(List.of(
                "com.github.brane08.pagila.actor.entities",
                "com.github.brane08.pagila.film.entities",
                "com.github.brane08.pagila.rental.entities",
                "com.github.brane08.pagila.seedworks.entities",
                "com.github.brane08.pagila.store.entities"));
        return DatabaseFactory.create(config);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
