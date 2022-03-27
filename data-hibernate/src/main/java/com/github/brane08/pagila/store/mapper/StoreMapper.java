package com.github.brane08.pagila.store.mapper;

import com.github.brane08.pagila.seedworks.mapper.CommonConfig;
import com.github.brane08.pagila.store.beans.StaffViewInfo;
import com.github.brane08.pagila.store.beans.StoreInfo;
import com.github.brane08.pagila.store.entities.StaffView;
import com.github.brane08.pagila.store.entities.Store;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface StoreMapper {

    StoreMapper INSTANCE = Mappers.getMapper(StoreMapper.class);

    StoreInfo storeToInfo(Store store);

    List<StoreInfo> storesToInfos(List<Store> list);

    StaffViewInfo staffViewToInfo(StaffView view);

    List<StaffViewInfo> staffViewsToInfo(List<StaffView> view);
}
