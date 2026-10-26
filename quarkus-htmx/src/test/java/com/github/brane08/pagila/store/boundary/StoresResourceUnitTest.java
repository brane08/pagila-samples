package com.github.brane08.pagila.store.boundary;

import com.github.brane08.pagila.store.beans.StoreViewInfo;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Template-bound boundary. The @CheckedTemplate native methods are implemented by Quarkus at build
 * time, so without Quarkus any call that reaches them throws UnsatisfiedLinkError. These tests cover
 * the repository interaction up to that point and the NotFound branches, which do not touch templates.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoresResourceUnitTest {

    @Mock
    StoreRepository repository;

    StoresResource resource;

    @BeforeEach
    void setUp() {
        resource = new StoresResource(repository);
    }

    @Test
    void storeListQueriesRepositoryBeforeTemplateBinding() {
        doReturn(List.<StoreViewInfo>of()).when(repository).listStoreViews();
        assertThrows(UnsatisfiedLinkError.class, () -> resource.storeList());
        verify(repository).listStoreViews();
    }

    @Test
    void salesStaffAndDetailQueryRepositoryBeforeTemplateBinding() {
        doReturn(List.of()).when(repository).allSalesByStore();
        doReturn(List.of()).when(repository).listStaffViews();
        doReturn(Optional.of(mock(StoreViewInfo.class))).when(repository).findStoreView(eq(1));
        doReturn(List.of()).when(repository).getStoreInventory(eq(1));

        assertThrows(UnsatisfiedLinkError.class, () -> resource.salesByStore());
        assertThrows(UnsatisfiedLinkError.class, () -> resource.staffList());
        assertThrows(UnsatisfiedLinkError.class, () -> resource.storeDetail(1));
        verify(repository).allSalesByStore();
        verify(repository).listStaffViews();
        verify(repository).getStoreInventory(eq(1));
    }

    @Test
    void storeDetailInventoryRentalsAndCustomersThrowNotFoundWhenStoreMissing() {
        doReturn(Optional.empty()).when(repository).findStoreView(anyInt());

        assertThrows(NotFoundException.class, () -> resource.storeDetail(99));
        assertThrows(NotFoundException.class, () -> resource.storeInventory(99));
        assertThrows(NotFoundException.class, () -> resource.storeRentals(99));
        assertThrows(NotFoundException.class, () -> resource.storeCustomers(99));

        verify(repository, never()).getStoreInventory(anyInt());
        verify(repository, never()).getStoreRentals(anyInt());
        verify(repository, never()).getStoreTopCustomers(anyInt());
    }
}
