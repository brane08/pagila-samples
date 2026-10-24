package com.github.brane08.pagila.store.repositories;

import com.github.brane08.pagila.seedworks.repositories.JPARepository;
import com.github.brane08.pagila.store.beans.SalesByStoreInfo;
import com.github.brane08.pagila.store.beans.StaffViewInfo;
import com.github.brane08.pagila.store.beans.StoreCustomerInfo;
import com.github.brane08.pagila.store.beans.StoreInventoryInfo;
import com.github.brane08.pagila.store.beans.StoreRentalInfo;
import com.github.brane08.pagila.store.beans.StoreViewInfo;
import com.github.brane08.pagila.store.entities.SalesByStore;
import com.github.brane08.pagila.store.entities.StaffView;
import com.github.brane08.pagila.store.entities.Store;
import com.github.brane08.pagila.store.entities.StoreCustomerView;
import com.github.brane08.pagila.store.entities.StoreInventoryView;
import com.github.brane08.pagila.store.entities.StoreRentalView;
import com.github.brane08.pagila.store.entities.StoreView;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class StoreRepository extends JPARepository<Store, Integer> {

    private final StoreMapper mapper;

    public StoreRepository(EntityManager em) {
        super(em);
        this.mapper = StoreMapper.INSTANCE;
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

    public List<SalesByStoreInfo> allSalesByStore() {
        List<SalesByStore> list = em().createQuery("select s from SalesByStore s", SalesByStore.class)
                .getResultList();
        return mapper.salesByStoresToInfos(list);
    }

    public List<StaffViewInfo> listStaffViews() {
        List<StaffView> list = em().createQuery("select v from StaffView v", StaffView.class)
                .getResultList();
        return mapper.staffViewsToInfo(list);
    }

    public List<StoreViewInfo> listStoreViews() {
        List<StoreView> list = em().createQuery("select v from StoreView v order by v.storeId", StoreView.class)
                .getResultList();
        return mapper.storeViewsToInfos(list);
    }

    public Optional<StoreViewInfo> findStoreView(int storeId) {
        return Optional.ofNullable(em().find(StoreView.class, storeId))
                       .map(mapper::storeViewToInfo);
    }

    public List<StoreInventoryInfo> getStoreInventory(int storeId) {
        List<StoreInventoryView> list = em().createQuery(
                        "select v from StoreInventoryView v where v.storeId = :storeId order by v.availableCopies desc, v.title",
                        StoreInventoryView.class)
                .setParameter("storeId", storeId)
                .setMaxResults(50)
                .getResultList();
        return mapper.storeInventoryViewsToInfos(list);
    }

    public List<StoreRentalInfo> getStoreRentals(int storeId) {
        List<StoreRentalView> list = em().createQuery(
                        "select v from StoreRentalView v where v.storeId = :storeId order by v.rentalDate desc",
                        StoreRentalView.class)
                .setParameter("storeId", storeId)
                .setMaxResults(50)
                .getResultList();
        return mapper.storeRentalViewsToInfos(list);
    }

    public List<StoreCustomerInfo> getStoreTopCustomers(int storeId) {
        List<StoreCustomerView> list = em().createQuery(
                        "select v from StoreCustomerView v where v.storeId = :storeId order by v.rentalCount desc, v.totalSpent desc",
                        StoreCustomerView.class)
                .setParameter("storeId", storeId)
                .setMaxResults(20)
                .getResultList();
        return mapper.storeCustomerViewsToInfos(list);
    }
}
