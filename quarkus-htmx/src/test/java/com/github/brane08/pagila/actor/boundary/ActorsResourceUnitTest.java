package com.github.brane08.pagila.actor.boundary;

import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;
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
class ActorsResourceUnitTest {

    @Mock
    ActorsRepository repository;
    @Mock
    ActorMapper mapper;

    @Test
    void listQueriesRepositoryThenHitsUnboundTemplate() {
        ActorsResource resource = new ActorsResource(repository, mapper);
        doReturn(new PagedList<>(List.of("A"), 1)).when(repository).page(eq(""), any(PageInfo.class), any());

        assertThrows(UnsatisfiedLinkError.class, () -> resource.list("", 1, 20, "actorId", 1));
        verify(repository).page(eq(""), eq(new PageInfo(1, 20, "actorId")), any());
    }
}
