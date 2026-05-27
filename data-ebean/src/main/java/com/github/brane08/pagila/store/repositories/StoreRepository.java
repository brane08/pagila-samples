package com.github.brane08.pagila.store.repositories;

import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
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
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

public class StoreRepository extends EbeanRepository<Store, Integer> {

    private final StoreMapper mapper;

    public StoreRepository(Database db) {
        super(db);
        this.mapper = Mappers.getMapper(StoreMapper.class);
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
        List<SalesByStore> list = db().find(SalesByStore.class).findList();
        return mapper.salesByStoresToInfos(list);
    }

    public List<StaffViewInfo> listStaffViews() {
        List<StaffView> list = db().find(StaffView.class).findList();
        return mapper.staffViewsToInfo(list);
    }

    public List<StoreViewInfo> listStoreViews() {
        List<StoreView> list = db().find(StoreView.class).orderBy("storeId").findList();
        return mapper.storeViewsToInfos(list);
    }

    public Optional<StoreViewInfo> findStoreView(int storeId) {
        return Optional.ofNullable(db().find(StoreView.class, storeId))
                       .map(mapper::storeViewToInfo);
    }

    public List<StoreInventoryInfo> getStoreInventory(int storeId) {
        List<StoreInventoryView> list = db().find(StoreInventoryView.class)
                .where().eq("storeId", storeId)
                .orderBy("availableCopies desc, title")
                .setMaxRows(50)
                .findList();
        return mapper.storeInventoryViewsToInfos(list);
    }

    public List<StoreRentalInfo> getStoreRentals(int storeId) {
        List<StoreRentalView> list = db().find(StoreRentalView.class)
                .where().eq("storeId", storeId)
                .orderBy("rentalDate desc")
                .setMaxRows(50)
                .findList();
        return mapper.storeRentalViewsToInfos(list);
    }

    public List<StoreCustomerInfo> getStoreTopCustomers(int storeId) {
        List<StoreCustomerView> list = db().find(StoreCustomerView.class)
                .where().eq("storeId", storeId)
                .orderBy("rentalCount desc, totalSpent desc")
                .setMaxRows(20)
                .findList();
        return mapper.storeCustomerViewsToInfos(list);
    }
}
