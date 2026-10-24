package com.github.brane08.pagila.film.boundary;

import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Template-bound boundary: Templates.list is a @CheckedTemplate native method implemented by Quarkus.
 * Without Quarkus the call fails with UnsatisfiedLinkError after the repository query, which is asserted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilmsResourceUnitTest {

    @Mock
    FilmsRepository repository;
    @Mock
    FilmMapper mapper;

    @Test
    void listQueriesRepositoryThenHitsUnboundTemplate() {
        FilmsResource resource = new FilmsResource(repository, mapper);
        doReturn(new PagedList<>(List.of("F"), 1)).when(repository).page(eq("q"), any(PageInfo.class), any());

        assertThrows(UnsatisfiedLinkError.class, () -> resource.list("q", 3, 10, "filmId", 1));
        verify(repository).page(eq("q"), eq(new PageInfo(3, 10, "filmId")), any());
    }
}
