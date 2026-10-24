package com.github.brane08.pagila.store.app;

import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PageResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.store.beans.StoreInfo;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import com.github.brane08.pagila.store.repositories.StoreRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoresResourceUnitTest {

    @Mock
    StoreRepository repository;
    @Mock
    StoreMapper mapper;

    StoresResource resource;

    @BeforeEach
    void setUp() {
        resource = new StoresResource(repository, mapper);
    }

    @Test
    void listReturnsOkWithPagedArray() {
        doReturn(new PagedList<>(List.of("STORE-1"), 1))
                .when(repository).page(eq(""), any(PageInfo.class), any());

        Response response = resource.list("", 1, 20, "storeId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("STORE-1"), body.getData());
        assertEquals(1, body.getTotalCount());
    }

    @Test
    void salesByStoreStaffViewAndStoreViewReturnOk() {
        doReturn(List.of("sales")).when(repository).allSalesByStore();
        doReturn(List.of("staff")).when(repository).listStaffViews();
        doReturn(List.of("store")).when(repository).listStoreViews();

        assertEquals(List.of("sales"), data(resource.salesByStore()));
        assertEquals(List.of("staff"), data(resource.listStaff()));
        assertEquals(List.of("store"), data(resource.listView()));
    }

    @Test
    void getByIdReturnsOkWhenFound() {
        StoreInfo store = mock(StoreInfo.class);
        doReturn(Optional.of(store)).when(repository).findById(eq(2), any());

        Response response = resource.getById(2);

        assertEquals(200, response.getStatus());
        ApiResult<?> body = assertInstanceOf(ApiResult.class, response.getEntity());
        assertSame(store, body.getData());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        doReturn(Optional.empty()).when(repository).findById(eq(99), any());

        Response response = resource.getById(99);

        assertEquals(404, response.getStatus());
        assertNull(response.getEntity());
    }

    @Test
    void inventoryRentalsAndCustomersReturnOkWithRepositoryData() {
        doReturn(List.of("inv")).when(repository).getStoreInventory(eq(1));
        doReturn(List.of("rent")).when(repository).getStoreRentals(eq(1));
        doReturn(List.of("cust")).when(repository).getStoreTopCustomers(eq(1));

        assertEquals(List.of("inv"), data(resource.storeInventory(1)));
        assertEquals(List.of("rent"), data(resource.storeRentals(1)));
        assertEquals(List.of("cust"), data(resource.storeCustomers(1)));
    }

    private static Object data(Response response) {
        assertEquals(200, response.getStatus());
        return ((ApiResult<?>) response.getEntity()).getData();
    }
}
