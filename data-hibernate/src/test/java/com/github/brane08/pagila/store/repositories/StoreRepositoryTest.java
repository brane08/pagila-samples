package com.github.brane08.pagila.store.repositories;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreRepositoryTest {

    private static final String ALL_SALES = "select s from SalesByStore s";
    private static final String STAFF_VIEWS = "select v from StaffView v";
    private static final String STORE_VIEWS = "select v from StoreView v order by v.storeId";
    private static final String INVENTORY =
            "select v from StoreInventoryView v where v.storeId = :storeId order by v.availableCopies desc, v.title";
    private static final String RENTALS =
            "select v from StoreRentalView v where v.storeId = :storeId order by v.rentalDate desc";
    private static final String TOP_CUSTOMERS =
            "select v from StoreCustomerView v where v.storeId = :storeId order by v.rentalCount desc, v.totalSpent desc";

    @Mock
    private EntityManager em;

    private StoreRepository repository;

    @BeforeEach
    void setUp() {
        repository = new StoreRepository(em);
    }

    @Test
    void allSalesByStore_mapsRows() {
        SalesByStore entity = new SalesByStore();
        entity.setStore("Lethbridge");
        entity.setManager("Mike Hillyer");
        entity.setTotalSales(new BigDecimal("33726.77"));
        stubQuery(ALL_SALES, SalesByStore.class, List.of(entity));

        List<SalesByStoreInfo> infos = repository.allSalesByStore();

        assertEquals(1, infos.size());
        assertEquals("Lethbridge", infos.getFirst().getStore());
        assertEquals("Mike Hillyer", infos.getFirst().getManager());
        assertEquals(new BigDecimal("33726.77"), infos.getFirst().getTotalSales());
    }

    @Test
    void allSalesByStore_empty() {
        stubQuery(ALL_SALES, SalesByStore.class, List.of());

        assertTrue(repository.allSalesByStore().isEmpty());
    }

    @Test
    void listStaffViews_mapsRows() {
        StaffView view = new StaffView();
        view.setId(1);
        view.setName("Mike Hillyer");
        view.setZipCode("17886");
        view.setSid(1);
        stubQuery(STAFF_VIEWS, StaffView.class, List.of(view));

        List<StaffViewInfo> infos = repository.listStaffViews();

        assertEquals(1, infos.size());
        assertEquals(1L, infos.getFirst().getId());
        assertEquals("Mike Hillyer", infos.getFirst().getName());
        assertEquals("17886", infos.getFirst().getZipCode());
        assertEquals(1L, infos.getFirst().getSid());
    }

    @Test
    void listStaffViews_empty() {
        stubQuery(STAFF_VIEWS, StaffView.class, List.of());

        assertTrue(repository.listStaffViews().isEmpty());
    }

    @Test
    void listStoreViews_mapsRowsOrderedByStoreId() {
        StoreView view = new StoreView();
        view.setStoreId(2);
        view.setManager("Jon Stephens");
        view.setDistrict("Alberta");
        view.setCity("Woodridge");
        TypedQuery<StoreView> query = stubQuery(STORE_VIEWS, StoreView.class, List.of(view));

        List<StoreViewInfo> infos = repository.listStoreViews();

        assertEquals(1, infos.size());
        assertEquals(2, infos.getFirst().getStoreId());
        assertEquals("Jon Stephens", infos.getFirst().getManager());
        assertEquals("Alberta", infos.getFirst().getDistrict());
        assertEquals("Woodridge", infos.getFirst().getCity());
        verify(em).createQuery(STORE_VIEWS, StoreView.class);
        verify(query, never()).setMaxResults(anyInt());
    }

    @Test
    void listStoreViews_empty() {
        stubQuery(STORE_VIEWS, StoreView.class, List.of());

        assertTrue(repository.listStoreViews().isEmpty());
    }

    @Test
    void findStoreView_returnsMappedViewWhenPresent() {
        StoreView view = new StoreView();
        view.setStoreId(1);
        view.setManager("Mike Hillyer");
        view.setAddress("47 MySakila Drive");
        when(em.find(StoreView.class, 1)).thenReturn(view);

        Optional<StoreViewInfo> found = repository.findStoreView(1);

        assertTrue(found.isPresent());
        assertEquals(1, found.get().getStoreId());
        assertEquals("47 MySakila Drive", found.get().getAddress());
    }

    @Test
    void findStoreView_returnsEmptyWhenAbsent() {
        when(em.find(StoreView.class, 9)).thenReturn(null);

        assertFalse(repository.findStoreView(9).isPresent());
    }

    @Test
    void getStoreInventory_bindsStoreIdCapsAt50AndMaps() {
        StoreInventoryView view = new StoreInventoryView();
        view.setStoreId(1);
        view.setFilmId(10);
        view.setTitle("ACADEMY DINOSAUR");
        view.setCategory("Documentary");
        view.setRating("PG");
        view.setRentalRate(new BigDecimal("0.99"));
        view.setTotalCopies(4);
        view.setAvailableCopies(2);
        TypedQuery<StoreInventoryView> query = stubQuery(INVENTORY, StoreInventoryView.class, List.of(view));

        List<StoreInventoryInfo> infos = repository.getStoreInventory(1);

        assertEquals(1, infos.size());
        StoreInventoryInfo info = infos.getFirst();
        assertEquals(10, info.getFilmId());
        assertEquals("ACADEMY DINOSAUR", info.getTitle());
        assertEquals("Documentary", info.getCategory());
        assertEquals("PG", info.getRating());
        assertEquals(new BigDecimal("0.99"), info.getRentalRate());
        assertEquals(4, info.getTotalCopies());
        assertEquals(2, info.getAvailableCopies());
        verify(query).setParameter("storeId", 1);
        verify(query).setMaxResults(50);
    }

    @Test
    void getStoreInventory_empty() {
        stubQuery(INVENTORY, StoreInventoryView.class, List.of());

        assertTrue(repository.getStoreInventory(3).isEmpty());
    }

    @Test
    void getStoreRentals_bindsStoreIdCapsAt50AndMaps() {
        StoreRentalView view = new StoreRentalView();
        view.setStoreId(2);
        view.setRentalId(16050);
        view.setTitle("BOOGIE AMELIE");
        view.setCustomer("Jane Doe");
        view.setRentalDate("2026-01-01");
        view.setReturnDate("2026-01-05");
        view.setOutstanding(true);
        TypedQuery<StoreRentalView> query = stubQuery(RENTALS, StoreRentalView.class, List.of(view));

        List<StoreRentalInfo> infos = repository.getStoreRentals(2);

        assertEquals(1, infos.size());
        assertEquals(16050, infos.getFirst().getRentalId());
        assertEquals("BOOGIE AMELIE", infos.getFirst().getTitle());
        assertEquals("Jane Doe", infos.getFirst().getCustomer());
        assertEquals("2026-01-01", infos.getFirst().getRentalDate());
        assertEquals("2026-01-05", infos.getFirst().getReturnDate());
        verify(query).setParameter("storeId", 2);
        verify(query).setMaxResults(50);
    }

    @Test
    void getStoreRentals_empty() {
        stubQuery(RENTALS, StoreRentalView.class, List.of());

        assertTrue(repository.getStoreRentals(2).isEmpty());
    }

    @Test
    void getStoreTopCustomers_bindsStoreIdCapsAt20AndMaps() {
        StoreCustomerView view = new StoreCustomerView();
        view.setStoreId(1);
        view.setCustomerId(42);
        view.setCustomer("Mary Smith");
        view.setEmail("mary.smith@example.test");
        view.setRentalCount(38);
        view.setTotalSpent(new BigDecimal("150.62"));
        TypedQuery<StoreCustomerView> query = stubQuery(TOP_CUSTOMERS, StoreCustomerView.class, List.of(view));

        List<StoreCustomerInfo> infos = repository.getStoreTopCustomers(1);

        assertEquals(1, infos.size());
        StoreCustomerInfo info = infos.getFirst();
        assertEquals(42, info.getCustomerId());
        assertEquals("Mary Smith", info.getCustomer());
        assertEquals(38, info.getRentalCount());
        assertEquals(new BigDecimal("150.62"), info.getTotalSpent());
        verify(query).setParameter("storeId", 1);
        verify(query).setMaxResults(20);
    }

    @Test
    void getStoreTopCustomers_empty() {
        stubQuery(TOP_CUSTOMERS, StoreCustomerView.class, List.of());

        assertTrue(repository.getStoreTopCustomers(1).isEmpty());
    }

    @Test
    void exposesLoadFieldsEntityClassAndFilterProperty() {
        assertEquals(List.of("manager"), repository.getLoadFields());
        assertEquals(Store.class, repository.entityClass());
        assertEquals("storeId", repository.filterProperty());
    }

    /** Stubs em.createQuery(jpql, type) to return a TypedQuery that yields the given rows. */
    private <T> TypedQuery<T> stubQuery(String jpql, Class<T> type, List<T> rows) {
        @SuppressWarnings("unchecked")
        TypedQuery<T> query = mock(TypedQuery.class);
        when(em.createQuery(jpql, type)).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(rows);
        return query;
    }
}
