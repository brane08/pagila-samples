package com.github.brane08.pagila.store.repositories;

import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import com.github.brane08.pagila.store.entities.Store;
import io.ebean.Database;

import java.util.List;

public class StoreRepository extends EbeanRepository<Store, Integer> {

    public StoreRepository(Database db) {
        super(db);
    }

    @Override
    protected List<String> getLoadFields() {
        return List.of("currentStaff");
    }

    @Override
    protected Class<Store> entityClass() {
        return Store.class;
    }

    @Override
    protected String filterProperty() {
        return "storeId";
    }
}
