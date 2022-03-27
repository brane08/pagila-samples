package com.github.brane08.pagila;

import io.micronaut.runtime.Micronaut;
import io.micronaut.serde.annotation.SerdeImport;

@SerdeImport(packageName = "com.github.brane08.pagila.actor.beans")
@SerdeImport(packageName = "com.github.brane08.pagila.film.beans")
@SerdeImport(packageName = "com.github.brane08.pagila.rental.beans")
@SerdeImport(packageName = "com.github.brane08.pagila.store.beans")
@SerdeImport(packageName = "com.github.brane08.pagila.seedworks.beans")
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}