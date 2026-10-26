package com.github.brane08.pagila.rental.repositories;

import com.github.brane08.pagila.film.repositories.JpaQueryStubs;
import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.entities.CustomerView;
import com.github.brane08.pagila.rental.entities.Rental;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalRepositoryTest {

    @Mock
    private EntityManager em;

    private JpaQueryStubs jpa;
    private RentalRepository repository;

    @BeforeEach
    void setUp() {
        jpa = new JpaQueryStubs(em);
        repository = new RentalRepository(em);
    }

    @Test
    void findCustomerView_returnsMappedViewOrEmpty() {
        CustomerView view = new CustomerView();
        view.setId(7);
        view.setName("MARY SMITH");
        jpa.rows(CustomerView.class, List.of(view));

        assertEquals(7L, repository.findCustomerView(7).orElseThrow().getId());
        verify(jpa.typed(CustomerView.class)).setMaxResults(1);
    }

    @Test
    void listCustomerViews_mapsRowsAndCount() {
        CustomerView view = new CustomerView();
        view.setId(1);
        view.setName("MARY SMITH");
        view.setZipCode("35200");
        view.setSid(1);
        jpa.rows(CustomerView.class, List.of(view));
        jpa.count(599);

        PagedList<CustomerViewInfo> page = repository.listCustomerViews(0, 25, "name");

        assertEquals(599, page.totalCount());
        CustomerViewInfo info = page.list().getFirst();
        assertEquals(1L, info.getId());
        assertEquals("MARY SMITH", info.getName());
        assertEquals("35200", info.getZipCode());
        assertEquals(1L, info.getSid());
        verify(jpa.typed(CustomerView.class)).setFirstResult(0);
        verify(jpa.typed(CustomerView.class)).setMaxResults(25);
        verify(jpa.cb()).asc(any());
    }

    @Test
    void listCustomerViews_descendingOrderAndEmptyResult() {
        jpa.rows(CustomerView.class, List.of());
        jpa.count(0);

        PagedList<CustomerViewInfo> page = repository.listCustomerViews(50, 10, "-name");

        assertTrue(page.list().isEmpty());
        assertEquals(0, page.totalCount());
        verify(jpa.typed(CustomerView.class)).setFirstResult(50);
        verify(jpa.typed(CustomerView.class)).setMaxResults(10);
        verify(jpa.cb()).desc(any());
    }

    @Test
    void exposesEntityClassAndFilterProperty() {
        assertEquals(Rental.class, repository.entityClass());
        assertEquals("rentalId", repository.filterProperty());
    }
}
