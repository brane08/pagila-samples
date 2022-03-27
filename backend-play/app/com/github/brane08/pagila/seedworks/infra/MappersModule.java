package com.github.brane08.pagila.seedworks.infra;

import com.google.inject.AbstractModule;
import com.github.brane08.pagila.actor.infra.ActorsMapper;
import com.github.brane08.pagila.film.infra.FilmsMapper;
import com.github.brane08.pagila.store.infra.StoresMapper;

public class MappersModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ActorsMapper.class).toInstance(ActorsMapper.INSTANCE);
		bind(StoresMapper.class).toInstance(StoresMapper.INSTANCE);
		bind(FilmsMapper.class).toInstance(FilmsMapper.INSTANCE);
	}
}
