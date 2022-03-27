package com.github.brane08.pagila.seedworks.mapper;

import com.github.brane08.pagila.seedworks.beans.AddressInfo;
import com.github.brane08.pagila.seedworks.beans.CityInfo;
import com.github.brane08.pagila.seedworks.beans.CountryInfo;
import com.github.brane08.pagila.seedworks.entities.Address;
import com.github.brane08.pagila.seedworks.entities.City;
import com.github.brane08.pagila.seedworks.entities.Country;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = CommonConfig.class)
public interface CommonMapper {

    CommonMapper INSTANCE = Mappers.getMapper(CommonMapper.class);

    AddressInfo addressToInfo(Address address);

    CityInfo cityToInfo(City city);

    CountryInfo countryToInfo(Country country);
}
