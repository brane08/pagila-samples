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
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Query;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreRepositoryTest {

    @Mock
    private Database db;

    private StoreRepository repository;

    @BeforeEach
    void setUp() {
        repository = new StoreRepository(db);
    }

    @Test
    void allSalesByStore_mapsRowsWithoutOrdering() {
        SalesByStore entity = new SalesByStore();
        entity.setStore("Lethbridge");
        entity.setManager("Mike Hillyer");
        entity.setTotalSales(new BigDecimal("33726.77"));
        Query<SalesByStore> query = stubFindAll(SalesByStore.class, List.of(entity));

        List<SalesByStoreInfo> infos = repository.allSalesByStore();

        assertEquals(1, infos.size());
        assertEquals("Lethbridge", infos.getFirst().getStore());
        assertEquals("Mike Hillyer", infos.getFirst().getManager());
        assertEquals(new BigDecimal("33726.77"), infos.getFirst().getTotalSales());
        verify(query, never()).orderBy(anyString());
    }

    @Test
    void allSalesByStore_empty() {
        stubFindAll(SalesByStore.class, List.of());

        assertTrue(repository.allSalesByStore().isEmpty());
    }

    @Test
    void listStaffViews_mapsRows() {
        StaffView view = new StaffView();
        view.setId(1);
        view.setName("Mike Hillyer");
        view.setZipCode("17886");
        view.setSid(1);
        Query<StaffView> query = stubFindAll(StaffView.class, List.of(view));

        List<StaffViewInfo> infos = repository.listStaffViews();

        assertEquals(1, infos.size());
        assertEquals(1L, infos.getFirst().getId());
        assertEquals("Mike Hillyer", infos.getFirst().getName());
        assertEquals("17886", infos.getFirst().getZipCode());
        assertEquals(1L, infos.getFirst().getSid());
        verify(query, never()).orderBy(anyString());
    }

    @Test
    void listStaffViews_empty() {
        stubFindAll(StaffView.class, List.of());

        assertTrue(repository.listStaffViews().isEmpty());
    }

    @Test
    void listStoreViews_mapsRowsOrderedByStoreId() {
        StoreView view = new StoreView();
        view.setStoreId(2);
        view.setManager("Jon Stephens");
        view.setDistrict("Alberta");
        view.setCity("Woodridge");
        Query<StoreView> query = stubFindAll(StoreView.class, List.of(view));

        List<StoreViewInfo> infos = repository.listStoreViews();

        assertEquals(1, infos.size());
        assertEquals(2, infos.getFirst().getStoreId());
        assertEquals("Jon Stephens", infos.getFirst().getManager());
        assertEquals("Alberta", infos.getFirst().getDistrict());
        assertEquals("Woodridge", infos.getFirst().getCity());
        verify(query).orderBy(eq("storeId"));
    }

    @Test
    void listStoreViews_empty() {
        stubFindAll(StoreView.class, List.of());

        assertTrue(repository.listStoreViews().isEmpty());
    }

    @Test
    void findStoreView_returnsMappedViewWhenPresent() {
        StoreView view = new StoreView();
        view.setStoreId(1);
        view.setManager("Mike Hillyer");
        view.setAddress("47 MySakila Drive");
        when(db.find(StoreView.class, 1)).thenReturn(view);

        Optional<StoreViewInfo> found = repository.findStoreView(1);

        assertTrue(found.isPresent());
        assertEquals(1, found.get().getStoreId());
        assertEquals("47 MySakila Drive", found.get().getAddress());
    }

    @Test
    void findStoreView_returnsEmptyWhenAbsent() {
        when(db.find(StoreView.class, 9)).thenReturn(null);

        assertFalse(repository.findStoreView(9).isPresent());
    }

    @Test
    void getStoreInventory_filtersByStoreIdCapsAt50AndMaps() {
        StoreInventoryView view = new StoreInventoryView();
        view.setStoreId(1);
        view.setFilmId(10);
        view.setTitle("ACADEMY DINOSAUR");
        view.setCategory("Documentary");
        view.setRating("PG");
        view.setRentalRate(new BigDecimal("0.99"));
        view.setTotalCopies(4);
        view.setAvailableCopies(2);
        ExpressionList<StoreInventoryView> expr = stubFiltered(StoreInventoryView.class, List.of(view));

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
        verify(expr).eq(eq("storeId"), eq(1));
        verify(expr).orderBy(eq("availableCopies desc, title"));
        verify(expr).setMaxRows(50);
    }

    @Test
    void getStoreInventory_empty() {
        stubFiltered(StoreInventoryView.class, List.of());

        assertTrue(repository.getStoreInventory(3).isEmpty());
    }

    @Test
    void getStoreRentals_filtersByStoreIdCapsAt50AndMaps() {
        StoreRentalView view = new StoreRentalView();
        view.setStoreId(2);
        view.setRentalId(16050);
        view.setTitle("BOOGIE AMELIE");
        view.setCustomer("Jane Doe");
        view.setRentalDate("2026-01-01");
        view.setReturnDate("2026-01-05");
        view.setOutstanding(true);
        ExpressionList<StoreRentalView> expr = stubFiltered(StoreRentalView.class, List.of(view));

        List<StoreRentalInfo> infos = repository.getStoreRentals(2);

        assertEquals(1, infos.size());
        StoreRentalInfo info = infos.getFirst();
        assertEquals(16050, info.getRentalId());
        assertEquals("BOOGIE AMELIE", info.getTitle());
        assertEquals("Jane Doe", info.getCustomer());
        assertEquals("2026-01-01", info.getRentalDate());
        assertEquals("2026-01-05", info.getReturnDate());
        assertTrue(info.isOutstanding());
        verify(expr).eq(eq("storeId"), eq(2));
        verify(expr).orderBy(eq("rentalDate desc"));
        verify(expr).setMaxRows(50);
    }

    @Test
    void getStoreRentals_empty() {
        stubFiltered(StoreRentalView.class, List.of());

        assertTrue(repository.getStoreRentals(2).isEmpty());
    }

    @Test
    void getStoreTopCustomers_filtersByStoreIdCapsAt20AndMaps() {
        StoreCustomerView view = new StoreCustomerView();
        view.setStoreId(1);
        view.setCustomerId(42);
        view.setCustomer("Mary Smith");
        view.setEmail("mary.smith@example.test");
        view.setRentalCount(38);
        view.setTotalSpent(new BigDecimal("150.62"));
        ExpressionList<StoreCustomerView> expr = stubFiltered(StoreCustomerView.class, List.of(view));

        List<StoreCustomerInfo> infos = repository.getStoreTopCustomers(1);

        assertEquals(1, infos.size());
        StoreCustomerInfo info = infos.getFirst();
        assertEquals(42, info.getCustomerId());
        assertEquals("Mary Smith", info.getCustomer());
        assertEquals(38, info.getRentalCount());
        assertEquals(new BigDecimal("150.62"), info.getTotalSpent());
        verify(expr).eq(eq("storeId"), eq(1));
        verify(expr).orderBy(eq("rentalCount desc, totalSpent desc"));
        verify(expr).setMaxRows(20);
    }

    @Test
    void getStoreTopCustomers_empty() {
        stubFiltered(StoreCustomerView.class, List.of());

        assertTrue(repository.getStoreTopCustomers(1).isEmpty());
    }

    @Test
    void findById_delegatesToDatabaseWithStoreEntityClass() {
        Store store = new Store();
        store.setStoreId(5);
        stubFindById(Store.class, 5, store);
        stubFindById(Store.class, 6, null);

        assertEquals(Optional.of(store), repository.findById(5));
        assertFalse(repository.findById(6).isPresent());
    }

    @Test
    void exposesLoadFieldsEntityClassAndFilterProperty() {
        assertEquals(List.of("manager"), repository.getLoadFields());
        assertEquals(Store.class, repository.entityClass());
        assertEquals("storeId", repository.filterProperty());
    }

    /** Stubs db.find(type) -> query whose findList() yields rows; orderBy is a fluent no-op. */
    @SuppressWarnings("unchecked")
    private <T> Query<T> stubFindAll(Class<T> type, List<T> rows) {
        Query<T> query = mock(Query.class);
        when(db.find(type)).thenReturn(query);
        when(query.orderBy(anyString())).thenReturn(query);
        when(query.findList()).thenReturn(rows);
        return query;
    }

    /**
     * Stubs db.find(type).where() -> expression list that supports the fluent
     * eq/orderBy/setMaxRows chain and yields rows from findList().
     */
    @SuppressWarnings("unchecked")
    private <T> ExpressionList<T> stubFiltered(Class<T> type, List<T> rows) {
        Query<T> query = mock(Query.class);
        ExpressionList<T> expr = mock(ExpressionList.class);
        when(db.find(type)).thenReturn(query);
        when(query.where()).thenReturn(expr);
        when(expr.eq(anyString(), any(Object.class))).thenReturn(expr);
        when(expr.orderBy(anyString())).thenReturn(expr);
        when(expr.setMaxRows(anyInt())).thenReturn(expr);
        when(expr.findList()).thenReturn(rows);
        return expr;
    }

    private final java.util.Map<Class<?>, io.ebean.ExpressionList<?>> findByIdWhere = new java.util.HashMap<>();

    @SuppressWarnings("unchecked")
    private <E> void stubFindById(Class<E> type, Object id, E result) {
        io.ebean.ExpressionList<E> where = (io.ebean.ExpressionList<E>) findByIdWhere.get(type);
        if (where == null) {
            io.ebean.Query<E> query = org.mockito.Mockito.mock(io.ebean.Query.class, org.mockito.Answers.RETURNS_SELF);
            where = org.mockito.Mockito.mock(io.ebean.ExpressionList.class);
            when(query.where()).thenReturn(where);
            when(db.find(type)).thenReturn(query);
            findByIdWhere.put(type, where);
        }
        io.ebean.ExpressionList<E> byId = org.mockito.Mockito.mock(io.ebean.ExpressionList.class);
        when(byId.findOne()).thenReturn(result);
        when(where.idEq(id)).thenReturn(byId);
    }
}
