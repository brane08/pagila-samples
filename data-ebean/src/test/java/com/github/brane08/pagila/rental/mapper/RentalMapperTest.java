package com.github.brane08.pagila.rental.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.beans.RentalInfo;
import com.github.brane08.pagila.rental.entities.CustomerView;
import com.github.brane08.pagila.rental.entities.Rental;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RentalMapperTest {

    private final RentalMapper mapper = Mappers.getMapper(RentalMapper.class);

    private static Rental rental(int id) {
        Rental rental = new Rental();
        rental.setRentalId(id);
        rental.setRentalDate(Instant.parse("2005-05-24T22:53:30Z"));
        rental.setInventoryId(367);
        rental.setCustomerId(130);
        rental.setReturnDate(Instant.parse("2005-05-29T20:34:53Z"));
        rental.setStaffId(1);
        rental.setLastUpdate(Instant.parse("2006-02-16T02:30:53Z"));
        return rental;
    }

    private static CustomerView customerView(int id, String name) {
        CustomerView view = new CustomerView();
        view.setId(id);
        view.setName(name);
        view.setAddress("1913 Hanoi Way");
        view.setZipCode("35200");
        view.setPhone("28303384290");
        view.setCity("Sasebo");
        view.setCountry("Japan");
        view.setNotes("active");
        view.setSid(1);
        return view;
    }

    @Test
    void rentalToInfoMapsAllFields() {
        RentalInfo info = mapper.rentalToInfo(rental(1));

        assertEquals(Integer.valueOf(1), info.getRentalId());
        assertEquals(Instant.parse("2005-05-24T22:53:30Z"), info.getRentalDate());
        assertEquals(Integer.valueOf(367), info.getInventoryId());
        assertEquals(Integer.valueOf(130), info.getCustomerId());
        assertEquals(Instant.parse("2005-05-29T20:34:53Z"), info.getReturnDate());
        assertEquals(Integer.valueOf(1), info.getStaffId());
        assertEquals(Instant.parse("2006-02-16T02:30:53Z"), info.getLastUpdate());
    }

    @Test
    void rentalToInfoKeepsNullOptionalFields() {
        Rental rental = new Rental();
        rental.setRentalId(2);

        RentalInfo info = mapper.rentalToInfo(rental);

        assertEquals(Integer.valueOf(2), info.getRentalId());
        assertNull(info.getReturnDate());
        assertNull(info.getRentalDate());
        assertNull(info.getLastUpdate());
    }

    @Test
    void rentalToInfoReturnsNullForNull() {
        assertNull(mapper.rentalToInfo(null));
    }

    @Test
    void rentalsToInfosMapsListInOrder() {
        List<RentalInfo> infos = mapper.rentalsToInfos(List.of(rental(1), rental(2)));

        assertEquals(2, infos.size());
        assertEquals(Integer.valueOf(1), infos.getFirst().getRentalId());
        assertEquals(Integer.valueOf(2), infos.get(1).getRentalId());
    }

    @Test
    void rentalsToInfosReturnsEmptyForEmptyAndNull() {
        assertTrue(mapper.rentalsToInfos(List.of()).isEmpty());
        assertTrue(mapper.rentalsToInfos(null).isEmpty());
    }

    @Test
    void customerViewToInfoMapsAllFields() {
        CustomerViewInfo info = mapper.customerViewToInfo(customerView(5, "MARY SMITH"));

        assertEquals(5L, info.getId());
        assertEquals("MARY SMITH", info.getName());
        assertEquals("1913 Hanoi Way", info.getAddress());
        assertEquals("35200", info.getZipCode());
        assertEquals("28303384290", info.getPhone());
        assertEquals("Sasebo", info.getCity());
        assertEquals("Japan", info.getCountry());
        assertEquals("active", info.getNotes());
        assertEquals(1L, info.getSid());
    }

    @Test
    void customerViewToInfoKeepsNullStringFields() {
        CustomerView view = new CustomerView();
        view.setId(9);

        CustomerViewInfo info = mapper.customerViewToInfo(view);

        assertEquals(9L, info.getId());
        assertNull(info.getName());
        assertNull(info.getZipCode());
        assertNull(info.getNotes());
    }

    @Test
    void customerViewToInfoReturnsNullForNull() {
        assertNull(mapper.customerViewToInfo(null));
    }

    @Test
    void customerViewsToInfosMapsListInOrder() {
        List<CustomerViewInfo> infos = mapper.customerViewsToInfos(
                List.of(customerView(5, "MARY SMITH"), customerView(6, "PATRICIA JOHNSON")));

        assertEquals(2, infos.size());
        assertEquals("MARY SMITH", infos.getFirst().getName());
        assertEquals("PATRICIA JOHNSON", infos.get(1).getName());
    }

    @Test
    void customerViewsToInfosReturnsEmptyForEmptyAndNull() {
        assertTrue(mapper.customerViewsToInfos(List.of()).isEmpty());
        assertTrue(mapper.customerViewsToInfos(null).isEmpty());
    }
}
