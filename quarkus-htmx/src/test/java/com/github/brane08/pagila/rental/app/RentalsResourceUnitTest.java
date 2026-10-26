package com.github.brane08.pagila.rental.app;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PageResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalsResourceUnitTest {

    @Mock
    RentalRepository repository;
    @Mock
    RentalMapper mapper;

    RentalsResource resource;

    @BeforeEach
    void setUp() {
        resource = new RentalsResource(repository, mapper);
    }

    @Test
    void getCustomerReturnsOkWhenFoundElseNotFound() {
        CustomerViewInfo c = new CustomerViewInfo();
        doReturn(Optional.of(c)).when(repository).findCustomerView(7);
        doReturn(Optional.empty()).when(repository).findCustomerView(8);

        assertEquals(200, resource.getCustomer(7).getStatus());
        assertEquals(404, resource.getCustomer(8).getStatus());
    }

    @Test
    void pageReturnsOkWithPagedArray() {
        doReturn(new PagedList<>(List.of("RENTAL-1"), 1))
                .when(repository).page(eq("x"), any(PageInfo.class), any());

        Response response = resource.page("x", 1, 20, "rentalId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("RENTAL-1"), body.getData());
        assertEquals(1, body.getTotalCount());
    }

    @Test
    void listCustomersReturnsOkWithOffsetDerivedFromPage() {
        doReturn(new PagedList<>(List.of("CUST-1"), 3))
                .when(repository).listCustomerViews(anyInt(), anyInt(), anyString());

        Response response = resource.listCustomers(2, 5, "id", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("CUST-1"), body.getData());
        assertEquals(3, body.getTotalCount());
        verify(repository).listCustomerViews(5, 5, "id");
    }
}
