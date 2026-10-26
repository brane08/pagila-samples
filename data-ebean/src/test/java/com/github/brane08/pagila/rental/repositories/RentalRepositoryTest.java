package com.github.brane08.pagila.rental.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.entities.CustomerView;
import com.github.brane08.pagila.rental.entities.Rental;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.repositories.Repository;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Junction;
import io.ebean.OrderBy;
import io.ebean.Query;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalRepositoryTest {

    @Mock
    private Database db;
    @Mock
    private Query<CustomerView> customerQuery;
    @Mock
    private OrderBy<CustomerView> customerOrderBy;
    @Mock
    private Query<Rental> rentalQuery;
    @Mock
    private OrderBy<Rental> rentalOrderBy;

    private RentalRepository repository;

    /** Exposes protected members that RentalRepository does not override. */
    static class Probe extends RentalRepository {
        Probe(Database db) {
            super(db);
        }

        Database exposeDb() {
            return db();
        }

        List<String> exposeLoadFields() {
            return getLoadFields();
        }
    }

    @BeforeEach
    void setUp() {
        repository = new RentalRepository(db);
        wire(CustomerView.class, customerQuery, customerOrderBy);
        wire(Rental.class, rentalQuery, rentalOrderBy);
    }

    /** Wires db.createQuery(type) to a query whose where()/orderBy()/paging chain is stubbed. */
    @SuppressWarnings("unchecked")
    private <T> void wire(Class<T> type, Query<T> query, OrderBy<T> orderBy) {
        ExpressionList<T> where = mock(ExpressionList.class);
        Junction<T> junction = mock(Junction.class);
        when(db.createQuery(type)).thenReturn(query);
        when(query.where()).thenReturn(where);
        when(where.and()).thenReturn(junction);
        when(query.orderBy()).thenReturn(orderBy);
        when(query.setFirstRow(anyInt())).thenReturn(query);
        when(query.setMaxRows(anyInt())).thenReturn(query);
    }

    private static Rental rental(int id) {
        Rental rental = new Rental();
        rental.setRentalId(id);
        return rental;
    }

    private static CustomerView customerView(int id, String name) {
        CustomerView view = new CustomerView();
        view.setId(id);
        view.setName(name);
        view.setZipCode("35200");
        view.setSid(1);
        return view;
    }

    @Test
    void findCustomerView_returnsMappedViewOrEmpty() {
        when(db.find(CustomerView.class, 7)).thenReturn(customerView(7, "MARY SMITH"));

        assertEquals(7L, repository.findCustomerView(7).orElseThrow().getId());
        assertTrue(repository.findCustomerView(8).isEmpty());
    }

    // ---- listCustomerViews ----

    @Test
    void listCustomerViews_mapsRowsAndCount() {
        when(customerQuery.findList()).thenReturn(List.of(customerView(1, "MARY SMITH")));
        when(customerQuery.findCount()).thenReturn(599);

        PagedList<CustomerViewInfo> page = repository.listCustomerViews(0, 25, "name");

        assertEquals(599, page.totalCount());
        CustomerViewInfo info = page.list().getFirst();
        assertEquals(1L, info.getId());
        assertEquals("MARY SMITH", info.getName());
        assertEquals("35200", info.getZipCode());
        assertEquals(1L, info.getSid());
        verify(customerQuery).setFirstRow(0);
        verify(customerQuery).setMaxRows(25);
        verify(customerOrderBy).asc("name");
        verify(customerOrderBy, never()).desc(anyString());
    }

    @Test
    void listCustomerViews_descendingOrderAndEmptyResult() {
        PagedList<CustomerViewInfo> page = repository.listCustomerViews(50, 10, "-name");

        assertTrue(page.list().isEmpty());
        assertEquals(0, page.totalCount());
        verify(customerQuery).setFirstRow(50);
        verify(customerQuery).setMaxRows(10);
        verify(customerOrderBy).desc("name");
        verify(customerOrderBy, never()).asc(anyString());
    }

    @Test
    void listCustomerViews_multiKeyOrderSkipsEmptySegments() {
        repository.listCustomerViews(0, 5, "sid,-name,,id");

        verify(customerOrderBy).asc("sid");
        verify(customerOrderBy).desc("name");
        verify(customerOrderBy).asc("id");
        verify(customerOrderBy, times(2)).asc(anyString());
        verify(customerOrderBy, times(1)).desc(anyString());
    }

    @Test
    void listCustomerViews_blankOrderDoesNotSort() {
        repository.listCustomerViews(0, 5, "");
        repository.listCustomerViews(0, 5, ",");

        verify(customerQuery, never()).orderBy();
    }

    @Test
    void listCustomerViews_nullOrderThrows() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> repository.listCustomerViews(0, 10, null));

        assertEquals("Sort order should not be null", ex.getMessage());
    }

    // ---- inherited Repository / MappingRepository behaviour ----

    @Test
    void entityClassAndFilterProperty() {
        assertEquals(Rental.class, repository.entityClass());
        assertEquals("rentalId", repository.filterProperty());
    }

    @Test
    void dbAccessorAndDefaultLoadFields() {
        Probe probe = new Probe(db);

        assertSame(db, probe.exposeDb());
        assertTrue(probe.exposeLoadFields().isEmpty());
    }

    @Test
    void buildParserReturnsParserForResultClass() {
        assertNotNull(repository.buildParser("", Rental.class));
    }

    @Test
    void findById_returnsEntityWhenPresent() {
        Rental found = rental(7);
        when(db.find(Rental.class, 7)).thenReturn(found);

        assertEquals(Optional.of(found), repository.findById(7));
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        assertEquals(Optional.empty(), repository.findById(7));
    }

    @Test
    void findById_withMapperAppliesMapperWhenPresent() {
        when(db.find(Rental.class, 7)).thenReturn(rental(7));

        assertEquals(Optional.of(7), repository.findById(7, Rental::getRentalId));
    }

    @Test
    void findById_withMapperReturnsEmptyWhenAbsent() {
        assertEquals(Optional.empty(), repository.findById(7, Rental::getRentalId));
    }

    @Test
    void count_delegatesToParserFindCount() {
        when(rentalQuery.findCount()).thenReturn(42);

        assertEquals(42, repository.count(""));
        verify(rentalQuery).findCount();
    }

    @Test
    void pageOffset_returnsRowsAndCountWithSort() {
        when(rentalQuery.findList()).thenReturn(List.of(rental(1), rental(2)));
        when(rentalQuery.findCount()).thenReturn(2);

        PagedList<Rental> page = repository.pageOffset("", 10, 5, "-rentalDate");

        assertEquals(2, page.list().size());
        assertEquals(2, page.totalCount());
        verify(rentalQuery).setFirstRow(10);
        verify(rentalQuery).setMaxRows(5);
        verify(rentalOrderBy).desc("rentalDate");
    }

    @Test
    void pageOffset_withMapperMapsRows() {
        when(rentalQuery.findList()).thenReturn(List.of(rental(1), rental(2)));
        when(rentalQuery.findCount()).thenReturn(2);

        PagedList<Integer> page = repository.pageOffset("", 0, 2, "rentalId", Rental::getRentalId);

        assertEquals(List.of(1, 2), page.list());
        assertEquals(2, page.totalCount());
        verify(rentalOrderBy).asc("rentalId");
    }

    @Test
    void page_convertsPageInfoToOffset() {
        repository.page("", new PageInfo(2, 5, "rentalId"));

        verify(rentalQuery).setFirstRow(5);
        verify(rentalQuery).setMaxRows(5);
        verify(rentalOrderBy).asc("rentalId");
    }

    @Test
    void page_withMapperConvertsPageInfoAndMaps() {
        when(rentalQuery.findList()).thenReturn(List.of(rental(9)));
        when(rentalQuery.findCount()).thenReturn(1);

        PagedList<Integer> page = repository.page("", new PageInfo(1, 2, "rentalId"), Rental::getRentalId);

        assertEquals(List.of(9), page.list());
        assertEquals(1, page.totalCount());
        verify(rentalQuery).setFirstRow(0);
    }

    @Test
    void all_returnsCappedFindList() {
        when(db.find(Rental.class)).thenReturn(rentalQuery);
        when(rentalQuery.findList()).thenReturn(List.of(rental(3)));

        List<Rental> all = repository.all();

        assertEquals(1, all.size());
        verify(rentalQuery).setMaxRows(eq(Repository.MAX_PAGE_SIZE));
    }

    @Test
    void all_withMapperMapsRows() {
        when(db.find(Rental.class)).thenReturn(rentalQuery);
        when(rentalQuery.findList()).thenReturn(List.of(rental(3), rental(4)));

        assertEquals(List.of(3, 4), repository.all(Rental::getRentalId));
    }

    @Test
    void facets_defaultIsEmpty() {
        assertTrue(repository.facets("").isEmpty());
    }
}
