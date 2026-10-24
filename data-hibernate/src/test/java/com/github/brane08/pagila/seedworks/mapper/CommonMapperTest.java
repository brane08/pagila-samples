package com.github.brane08.pagila.seedworks.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.brane08.pagila.seedworks.beans.AddressInfo;
import com.github.brane08.pagila.seedworks.beans.CityInfo;
import com.github.brane08.pagila.seedworks.beans.CountryInfo;
import com.github.brane08.pagila.seedworks.entities.Address;
import com.github.brane08.pagila.seedworks.entities.City;
import com.github.brane08.pagila.seedworks.entities.Country;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CommonMapperTest {

    private final CommonMapper mapper = Mappers.getMapper(CommonMapper.class);

    private static Country country() {
        Country country = new Country();
        country.setCountryId(20);
        country.setCountry("Canada");
        return country;
    }

    private static City city() {
        City city = new City();
        city.setCityId(300);
        city.setCity("Lethbridge");
        city.setCountry(country());
        return city;
    }

    @Test
    void instanceIsAvailable() {
        assertNotNull(CommonMapper.INSTANCE);
    }

    @Test
    void addressToInfoMapsFieldsAndNestedCity() {
        Address address = new Address();
        address.setAddressId(10);
        address.setAddress("47 MySakila Drive");
        address.setDistrict("Alberta");
        address.setPostalCode("");
        address.setPhone("");
        address.setLastUpdate(Instant.parse("2006-02-15T09:45:30Z"));
        address.setCity(city());

        AddressInfo info = mapper.addressToInfo(address);

        assertEquals(Long.valueOf(10), info.getAddressId());
        assertEquals("47 MySakila Drive", info.getAddress());
        assertEquals("Alberta", info.getDistrict());
        assertEquals(Instant.parse("2006-02-15T09:45:30Z"), info.getLastUpdate());
        CityInfo cityInfo = info.getCity();
        assertEquals(Long.valueOf(300), cityInfo.cityId());
        assertEquals("Lethbridge", cityInfo.city());
        assertEquals(new CountryInfo(20L, "Canada"), cityInfo.country());
    }

    @Test
    void addressToInfoHandlesNullAddressId() {
        Address address = new Address();
        address.setAddress("1 Nowhere Rd");

        AddressInfo info = mapper.addressToInfo(address);

        assertNull(info.getAddressId());
        assertEquals("1 Nowhere Rd", info.getAddress());
        assertNull(info.getCity());
    }

    @Test
    void addressToInfoReturnsNullForNull() {
        assertNull(mapper.addressToInfo(null));
    }

    @Test
    void cityToInfoMapsIdNameAndCountry() {
        CityInfo info = mapper.cityToInfo(city());

        assertEquals(new CityInfo(300L, "Lethbridge", new CountryInfo(20L, "Canada")), info);
    }

    @Test
    void cityToInfoHandlesNullIdAndNullCountry() {
        City city = new City();
        city.setCity("Unknown");

        CityInfo info = mapper.cityToInfo(city);

        assertEquals(new CityInfo(null, "Unknown", null), info);
    }

    @Test
    void cityToInfoReturnsNullForNull() {
        assertNull(mapper.cityToInfo(null));
    }

    @Test
    void countryToInfoMapsIdAndName() {
        assertEquals(new CountryInfo(20L, "Canada"), mapper.countryToInfo(country()));
    }

    @Test
    void countryToInfoHandlesNullIdAndNull() {
        Country country = new Country();
        country.setCountry("Atlantis");

        assertEquals(new CountryInfo(null, "Atlantis"), mapper.countryToInfo(country));
        assertNull(mapper.countryToInfo(null));
    }
}
