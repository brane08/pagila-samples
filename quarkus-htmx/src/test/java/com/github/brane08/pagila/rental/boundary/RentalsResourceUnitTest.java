package com.github.brane08.pagila.rental.boundary;

import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Template-bound boundary: Templates.customerList is a @CheckedTemplate native method implemented by Quarkus.
 * Without Quarkus the call fails with UnsatisfiedLinkError after the repository query, which is asserted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalsResourceUnitTest {

    @Mock
    RentalRepository repository;

    @Test
    void customerListQueriesRepositoryThenHitsUnboundTemplate() {
        RentalsResource resource = new RentalsResource(repository);
        doReturn(new PagedList<>(List.of("C"), 1)).when(repository).listCustomerViews(anyInt(), anyInt(), anyString());

        assertThrows(UnsatisfiedLinkError.class, () -> resource.customerList(2, 5, "id"));
        verify(repository).listCustomerViews(5, 5, "id");
    }
}
