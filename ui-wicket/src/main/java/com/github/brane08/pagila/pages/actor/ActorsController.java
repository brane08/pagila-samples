package com.github.brane08.pagila.pages.actor;

import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import jakarta.inject.Inject;

public class ActorsController {

    @Inject
    ActorsRepository repository;
    @Inject
    ActorMapper mapper;


}
