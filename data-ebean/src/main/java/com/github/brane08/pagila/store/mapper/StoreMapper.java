package com.github.brane08.pagila.store.mapper;

import com.github.brane08.pagila.seedworks.mapper.CommonConfig;
import com.github.brane08.pagila.store.beans.SalesByStoreInfo;
import com.github.brane08.pagila.store.beans.StaffViewInfo;
import com.github.brane08.pagila.store.beans.StoreCustomerInfo;
import com.github.brane08.pagila.store.beans.StoreInfo;
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
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface StoreMapper {

    StoreInfo storeToInfo(Store store);

    List<StoreInfo> storesToInfos(List<Store> list);

    StaffViewInfo staffViewToInfo(StaffView view);

    List<StaffViewInfo> staffViewsToInfo(List<StaffView> view);

    SalesByStoreInfo salesByStoreToInfo(SalesByStore entity);

    List<SalesByStoreInfo> salesByStoresToInfos(List<SalesByStore> entities);

    StoreViewInfo storeViewToInfo(StoreView view);

    List<StoreViewInfo> storeViewsToInfos(List<StoreView> views);

    StoreInventoryInfo storeInventoryViewToInfo(StoreInventoryView view);

    List<StoreInventoryInfo> storeInventoryViewsToInfos(List<StoreInventoryView> views);

    StoreRentalInfo storeRentalViewToInfo(StoreRentalView view);

    List<StoreRentalInfo> storeRentalViewsToInfos(List<StoreRentalView> views);

    StoreCustomerInfo storeCustomerViewToInfo(StoreCustomerView view);

    List<StoreCustomerInfo> storeCustomerViewsToInfos(List<StoreCustomerView> views);
}
