package com.github.brane08.pagila.actor.app;

import com.github.brane08.pagila.actor.beans.ActorViewInfo;
import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;
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
class ActorsResourceUnitTest {

    @Mock
    ActorsRepository repository;
    @Mock
    ActorMapper mapper;

    ActorsResource resource;

    @BeforeEach
    void setUp() {
        resource = new ActorsResource(repository, mapper);
    }

    @Test
    void listReturnsOkWithPagedArrayAndPassesFiqlPageInfoToRepository() {
        doReturn(new PagedList<>(List.of("ACTOR-1"), 1))
                .when(repository).page(eq(""), any(PageInfo.class), any());

        Response response = resource.list("", 2, 10, "actorId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertTrue(body.isSuccess());
        assertEquals(List.of("ACTOR-1"), body.getData());
        assertEquals(1, body.getTotalCount());
        verify(repository).page(eq(""), eq(new PageInfo(2, 10, "actorId")), any());
    }

    @Test
    void listViewsReturnsOkWithOffsetDerivedFromPage() {
        doReturn(new PagedList<>(List.of("VIEW-1"), 5))
                .when(repository).listActorViews(anyInt(), anyInt(), anyString());

        Response response = resource.listViews(2, 10, "actorId", 1);

        assertEquals(200, response.getStatus());
        PageResult<?> body = assertInstanceOf(PageResult.class, response.getEntity());
        assertEquals(List.of("VIEW-1"), body.getData());
        assertEquals(5, body.getTotalCount());
        verify(repository).listActorViews(10, 10, "actorId");
    }

    @Test
    void getByIdReturnsOkWithSingleResultWhenFound() {
        ActorViewInfo view = mock(ActorViewInfo.class);
        doReturn(Optional.of(view)).when(repository).findActorViewById(eq(7));

        Response response = resource.getById(7);

        assertEquals(200, response.getStatus());
        ApiResult<?> body = assertInstanceOf(ApiResult.class, response.getEntity());
        assertFalse(body instanceof PageResult);
        assertTrue(body.isSuccess());
        assertSame(view, body.getData());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        doReturn(Optional.empty()).when(repository).findActorViewById(anyInt());

        Response response = resource.getById(999);

        assertEquals(404, response.getStatus());
        assertNull(response.getEntity());
    }
}
