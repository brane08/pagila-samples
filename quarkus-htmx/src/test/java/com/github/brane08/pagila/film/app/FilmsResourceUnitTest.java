package com.github.brane08.pagila.film.app;

import com.github.brane08.pagila.film.beans.FilmInfo;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
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

import java.util.Collections;
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
class FilmsResourceUnitTest {

    @Mock
    FilmsRepository repository;
    @Mock
    FilmMapper mapper;

    FilmsResource resource;

    @BeforeEach
    void setUp() {
        resource = new FilmsResource(repository, mapper);
    }

    @Test
    void listReturnsOkWithPagedArray() {
        doReturn(new PagedList<>(List.of("FILM-1", "FILM-2"), 2))
                .when(repository).page(eq("title"), any(PageInfo.class), any());

        Response response = resource.list("title", 1, 20, "filmId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("FILM-1", "FILM-2"), body.getData());
        assertEquals(2, body.getTotalCount());
        verify(repository).page(eq("title"), eq(new PageInfo(1, 20, "filmId")), any());
    }

    @Test
    void countReturnsOkWithEmptyDataAndTotal() {
        doReturn(42).when(repository).count("comedy");

        Response response = resource.count("comedy");

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(Collections.emptyList(), body.getData());
        assertEquals(42, body.getTotalCount());
    }

    @Test
    void facetsReturnsOkWithRepositoryFacets() {
        doReturn(List.of("rating", "category")).when(repository).facets("");

        Response response = resource.facets();

        assertEquals(200, response.getStatus());
        assertEquals(List.of("rating", "category"), ((ApiResult<?>) response.getEntity()).getData());
    }

    @Test
    void listNicerViewsReturnsOkWithOffsetDerivedFromPage() {
        doReturn(new PagedList<>(List.of("NICER-1"), 9))
                .when(repository).offsetOfNicerFilms(anyInt(), anyInt(), anyString());

        Response response = resource.listNicerViews(3, 5, "filmId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("NICER-1"), body.getData());
        assertEquals(9, body.getTotalCount());
        verify(repository).offsetOfNicerFilms(10, 5, "filmId");
    }

    @Test
    void salesByCategoryReturnsOkWithAllSales() {
        doReturn(List.of("sales-row")).when(repository).allSalesByCategory();

        Response response = resource.salesByCategory();

        assertEquals(200, response.getStatus());
        assertEquals(List.of("sales-row"), ((ApiResult<?>) response.getEntity()).getData());
    }

    @Test
    void getByIdReturnsOkWhenFound() {
        FilmInfo film = mock(FilmInfo.class);
        doReturn(Optional.of(film)).when(repository).findById(eq(3), any());

        Response response = resource.getById(3);

        assertEquals(200, response.getStatus());
        ApiResult<?> body = assertInstanceOf(ApiResult.class, response.getEntity());
        assertSame(film, body.getData());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        doReturn(Optional.empty()).when(repository).findById(eq(404), any());

        Response response = resource.getById(404);

        assertEquals(404, response.getStatus());
        assertNull(response.getEntity());
    }

    @Test
    void listActorsReturnsOkWithRepositoryActors() {
        doReturn(List.of("ACTOR-A")).when(repository).listActors(eq(3));

        Response response = resource.listActors(3);

        assertEquals(200, response.getStatus());
        assertEquals(List.of("ACTOR-A"), ((ApiResult<?>) response.getEntity()).getData());
    }

    @Test
    void listViewsReturnsOkWithPageInfoFromParameters() {
        doReturn(new PagedList<>(List.of("VIEW-1"), 7))
                .when(repository).listFilms(any(PageInfo.class));

        Response response = resource.listViews("", 2, 10, "filmId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("VIEW-1"), body.getData());
        assertEquals(7, body.getTotalCount());
        verify(repository).listFilms(new PageInfo(2, 10, "filmId"));
    }
}
