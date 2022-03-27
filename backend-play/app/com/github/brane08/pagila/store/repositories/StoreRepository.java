package com.github.brane08.pagila.store.repositories;

import com.github.brane08.pagila.seedworks.infra.DatabaseExecutionContext;
import com.github.brane08.pagila.seedworks.repositories.Repository;
import com.github.brane08.pagila.store.entities.Store;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.List;

public class StoreRepository extends Repository<Store> {

	@Inject
	public StoreRepository(JPAApi jpaApi, DatabaseExecutionContext context) {
		super(jpaApi, context, Store.class, "storeId");
	}

	@Override
	protected List<String> getLoadFields() {
		return List.of("currentStaff");
	}
}
