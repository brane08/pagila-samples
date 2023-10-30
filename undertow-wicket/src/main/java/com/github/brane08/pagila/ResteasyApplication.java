package com.github.brane08.pagila;

import com.github.brane08.pagila.rest.HomeResource;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

public class ResteasyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(HomeResource.class);
        return classes;
    }
}
