package com.github.brane08.pagila.store.repositories;

import com.github.brane08.pagila.seedworks.repositories.JPARepository;
import com.github.brane08.pagila.store.entities.Store;
import jakarta.persistence.EntityManager;

import java.util.List;

public class StoreRepository extends JPARepository<Store, Integer> {

    public StoreRepository(EntityManager em) {
        super(em);
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
