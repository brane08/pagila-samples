package com.github.brane08.pagila.store.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.brane08.pagila.seedworks.entities.Address;
import com.github.brane08.pagila.store.beans.SalesByStoreInfo;
import com.github.brane08.pagila.store.beans.StaffViewInfo;
import com.github.brane08.pagila.store.beans.StoreCustomerInfo;
import com.github.brane08.pagila.store.beans.StoreInfo;
import com.github.brane08.pagila.store.beans.StoreInventoryInfo;
import com.github.brane08.pagila.store.beans.StoreRentalInfo;
import com.github.brane08.pagila.store.beans.StoreViewInfo;
import com.github.brane08.pagila.store.entities.SalesByStore;
import com.github.brane08.pagila.store.entities.Staff;
import com.github.brane08.pagila.store.entities.StaffView;
import com.github.brane08.pagila.store.entities.Store;
import com.github.brane08.pagila.store.entities.StoreCustomerView;
import com.github.brane08.pagila.store.entities.StoreInventoryView;
import com.github.brane08.pagila.store.entities.StoreRentalView;
import com.github.brane08.pagila.store.entities.StoreView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class StoreMapperTest {

    private final StoreMapper mapper = Mappers.getMapper(StoreMapper.class);

    private static Staff staff(Integer staffId) {
        Staff staff = new Staff();
        staff.setStaffId(staffId);
        staff.setFirstName("Mike");
        staff.setLastName("Hillyer");
        staff.setUsername("Mike");
        staff.setActive(true);
        staff.setLastUpdate(Instant.parse("2006-02-15T09:57:12Z"));
        return staff;
    }

    private static Address address(Integer addressId) {
        Address address = new Address();
        address.setAddressId(addressId);
        address.setAddress("47 MySakila Drive");
        address.setLastUpdate(Instant.parse("2006-02-15T09:45:30Z"));
        return address;
    }

    private static StaffView staffView(int id) {
        StaffView view = new StaffView();
        view.setId(id);
        view.setName("Mike Hillyer");
        view.setAddress("23 Workhaven Lane");
        view.setZipCode("");
        view.setPhone("14033335568");
        view.setCity("Lethbridge");
        view.setCountry("Canada");
        view.setSid(1);
        return view;
    }

    @Test
    void storeToInfoMapsIdLastUpdateManagerAndAddress() {
        Store store = new Store();
        store.setStoreId(1);
        store.setLastUpdate(Instant.parse("2006-02-15T09:57:12Z"));
        store.setManager(staff(1));
        store.setAddress(address(1));

        StoreInfo info = mapper.storeToInfo(store);

        assertEquals(Long.valueOf(1), info.getStoreId());
        assertEquals(Instant.parse("2006-02-15T09:57:12Z"), info.getLastUpdate());
        assertEquals(Long.valueOf(1), info.getManager().getStaffId());
        assertEquals("Mike", info.getManager().getFirstName());
        assertEquals("Hillyer", info.getManager().getLastName());
        assertEquals("Mike", info.getManager().getUsername());
        assertEquals("true", info.getManager().getActive());
        assertEquals(Instant.parse("2006-02-15T09:57:12Z"), info.getManager().getLastUpdate());
        assertEquals(Long.valueOf(1), info.getAddress().getAddressId());
        assertEquals("47 MySakila Drive", info.getAddress().getAddress());
        assertEquals(Instant.parse("2006-02-15T09:45:30Z"), info.getAddress().getLastUpdate());
    }

    @Test
    void staffToInfoMapsStoreIdFromStoreRelation() {
        Store store = new Store();
        store.setStoreId(2);
        Staff staff = staff(1);
        staff.setStore(store);

        assertEquals(Long.valueOf(2), mapper.staffToInfo(staff).getStoreId());
    }

    @Test
    void storeToInfoLeavesMissingManagerAndAddressNull() {
        Store store = new Store();
        store.setStoreId(2);

        StoreInfo info = mapper.storeToInfo(store);

        assertEquals(Long.valueOf(2), info.getStoreId());
        assertNull(info.getLastUpdate());
        assertNull(info.getManager());
        assertNull(info.getAddress());
    }

    @Test
    void storeToInfoLeavesMissingIdNullButMapsPresentNestedWithoutIds() {
        Store store = new Store();
        store.setManager(staff(null));
        store.setAddress(address(null));

        StoreInfo info = mapper.storeToInfo(store);

        assertNull(info.getStoreId());
        assertNull(info.getManager().getStaffId());
        assertNull(info.getAddress().getAddressId());
        assertEquals("47 MySakila Drive", info.getAddress().getAddress());
    }

    @Test
    void storeToInfoReturnsNullForNull() {
        assertNull(mapper.storeToInfo(null));
    }

    @Test
    void storesToInfosMapsListAndHandlesNull() {
        Store store = new Store();
        store.setStoreId(1);

        List<StoreInfo> infos = mapper.storesToInfos(List.of(store));

        assertEquals(1, infos.size());
        assertEquals(Long.valueOf(1), infos.getFirst().getStoreId());
        assertTrue(mapper.storesToInfos(null).isEmpty());
    }

    @Test
    void staffViewToInfoMapsAllFields() {
        StaffViewInfo info = mapper.staffViewToInfo(staffView(1));

        assertEquals(1L, info.getId());
        assertEquals("Mike Hillyer", info.getName());
        assertEquals("23 Workhaven Lane", info.getAddress());
        assertEquals("", info.getZipCode());
        assertEquals("14033335568", info.getPhone());
        assertEquals("Lethbridge", info.getCity());
        assertEquals("Canada", info.getCountry());
        assertEquals(1L, info.getSid());
        assertNull(mapper.staffViewToInfo(null));
    }

    @Test
    void staffViewsToInfoMapsListAndHandlesNull() {
        List<StaffViewInfo> infos = mapper.staffViewsToInfo(List.of(staffView(1)));

        assertEquals(1, infos.size());
        assertEquals("Mike Hillyer", infos.getFirst().getName());
        assertTrue(mapper.staffViewsToInfo(null).isEmpty());
    }

    @Test
    void salesByStoreToInfoMapsFields() {
        SalesByStore entity = new SalesByStore();
        entity.setStore("Lethbridge");
        entity.setManager("Mike Hillyer");
        entity.setTotalSales(new BigDecimal("33726.77"));

        SalesByStoreInfo info = mapper.salesByStoreToInfo(entity);

        assertEquals("Lethbridge", info.getStore());
        assertEquals("Mike Hillyer", info.getManager());
        assertEquals(new BigDecimal("33726.77"), info.getTotalSales());
        assertNull(mapper.salesByStoreToInfo(null));
    }

    @Test
    void salesByStoresToInfosMapsListAndHandlesNull() {
        List<SalesByStoreInfo> infos = mapper.salesByStoresToInfos(List.of(new SalesByStore()));

        assertEquals(1, infos.size());
        assertTrue(mapper.salesByStoresToInfos(null).isEmpty());
    }

    @Test
    void storeViewToInfoMapsFields() {
        StoreView view = new StoreView();
        view.setStoreId(1);
        view.setManager("Mike Hillyer");
        view.setAddress("47 MySakila Drive");
        view.setDistrict("Alberta");
        view.setCity("Lethbridge");

        StoreViewInfo info = mapper.storeViewToInfo(view);

        assertEquals(1, info.getStoreId());
        assertEquals("Mike Hillyer", info.getManager());
        assertEquals("47 MySakila Drive", info.getAddress());
        assertEquals("Alberta", info.getDistrict());
        assertEquals("Lethbridge", info.getCity());
        assertNull(mapper.storeViewToInfo(null));
    }

    @Test
    void storeViewsToInfosMapsListAndHandlesNull() {
        assertEquals(1, mapper.storeViewsToInfos(List.of(new StoreView())).size());
        assertTrue(mapper.storeViewsToInfos(null).isEmpty());
    }

    @Test
    void storeInventoryViewToInfoMapsFields() {
        StoreInventoryView view = new StoreInventoryView();
        view.setStoreId(1);
        view.setFilmId(133);
        view.setTitle("CHAMBER ITALIAN");
        view.setCategory("Action");
        view.setRating("NC-17");
        view.setRentalRate(new BigDecimal("4.99"));
        view.setTotalCopies(3);
        view.setAvailableCopies(2);

        StoreInventoryInfo info = mapper.storeInventoryViewToInfo(view);

        assertEquals(133, info.getFilmId());
        assertEquals("CHAMBER ITALIAN", info.getTitle());
        assertEquals("Action", info.getCategory());
        assertEquals("NC-17", info.getRating());
        assertEquals(new BigDecimal("4.99"), info.getRentalRate());
        assertEquals(3, info.getTotalCopies());
        assertEquals(2, info.getAvailableCopies());
        assertNull(mapper.storeInventoryViewToInfo(null));
    }

    @Test
    void storeInventoryViewsToInfosMapsListAndHandlesNull() {
        assertEquals(1, mapper.storeInventoryViewsToInfos(List.of(new StoreInventoryView())).size());
        assertTrue(mapper.storeInventoryViewsToInfos(null).isEmpty());
    }

    @Test
    void storeRentalViewToInfoMapsFieldsIncludingOutstandingFlag() {
        StoreRentalView view = new StoreRentalView();
        view.setRentalId(7);
        view.setTitle("ACADEMY DINOSAUR");
        view.setCustomer("MARY SMITH");
        view.setRentalDate("2005-05-24");
        view.setReturnDate(null);
        view.setOutstanding(true);

        StoreRentalInfo info = mapper.storeRentalViewToInfo(view);

        assertEquals(7, info.getRentalId());
        assertEquals("ACADEMY DINOSAUR", info.getTitle());
        assertEquals("MARY SMITH", info.getCustomer());
        assertEquals("2005-05-24", info.getRentalDate());
        assertNull(info.getReturnDate());
        assertTrue(info.isOutstanding());

        view.setOutstanding(false);
        assertFalse(mapper.storeRentalViewToInfo(view).isOutstanding());
        assertNull(mapper.storeRentalViewToInfo(null));
    }

    @Test
    void storeRentalViewsToInfosMapsListAndHandlesNull() {
        assertEquals(1, mapper.storeRentalViewsToInfos(List.of(new StoreRentalView())).size());
        assertTrue(mapper.storeRentalViewsToInfos(null).isEmpty());
    }

    @Test
    void storeCustomerViewToInfoMapsFields() {
        StoreCustomerView view = new StoreCustomerView();
        view.setStoreId(1);
        view.setCustomerId(130);
        view.setCustomer("MARY SMITH");
        view.setEmail("MARY.SMITH@sakilacustomer.org");
        view.setRentalCount(32);
        view.setTotalSpent(new BigDecimal("118.68"));

        StoreCustomerInfo info = mapper.storeCustomerViewToInfo(view);

        assertEquals(130, info.getCustomerId());
        assertEquals("MARY SMITH", info.getCustomer());
        assertEquals("MARY.SMITH@sakilacustomer.org", info.getEmail());
        assertEquals(32, info.getRentalCount());
        assertEquals(new BigDecimal("118.68"), info.getTotalSpent());
        assertNull(mapper.storeCustomerViewToInfo(null));
    }

    @Test
    void storeCustomerViewsToInfosMapsListAndHandlesNull() {
        assertEquals(1, mapper.storeCustomerViewsToInfos(List.of(new StoreCustomerView())).size());
        assertTrue(mapper.storeCustomerViewsToInfos(null).isEmpty());
    }
}
