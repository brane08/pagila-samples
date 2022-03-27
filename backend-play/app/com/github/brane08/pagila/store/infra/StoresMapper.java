package com.github.brane08.pagila.store.infra;

import com.github.brane08.pagila.seedworks.infra.CommonConfig;
import com.github.brane08.pagila.store.beans.AddressInfo;
import com.github.brane08.pagila.store.beans.CityInfo;
import com.github.brane08.pagila.store.beans.CountryInfo;
import com.github.brane08.pagila.store.beans.StoreInfo;
import com.github.brane08.pagila.store.entities.Address;
import com.github.brane08.pagila.store.entities.City;
import com.github.brane08.pagila.store.entities.Country;
import com.github.brane08.pagila.store.entities.Store;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface StoresMapper {
	StoresMapper INSTANCE = Mappers.getMapper(StoresMapper.class);

	StoreInfo map(Store store);

	List<StoreInfo> map(List<Store> list);

	AddressInfo map(Address address);

	CityInfo map(City city);

	CountryInfo map(Country country);
}
