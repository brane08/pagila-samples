package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.Facet;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Answers.CALLS_REAL_METHODS;

class RepositoryTest {

    @Test
    void maxPageSizeConstantIsOneThousand() {
        assertEquals(1000, Repository.MAX_PAGE_SIZE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageDelegatesToPageOffsetWithComputedOffset() {
        Repository<String, Integer> repo = mock(Repository.class, CALLS_REAL_METHODS);
        PagedList<String> expected = new PagedList<>(List.of("a"), 1);
        doReturn(expected).when(repo).pageOffset("q", 20, 10, "name");

        PagedList<String> actual = repo.page("q", new PageInfo(3, 10, "name"));

        assertSame(expected, actual);
        verify(repo).pageOffset("q", 20, 10, "name");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageOnFirstPageUsesZeroOffset() {
        Repository<String, Integer> repo = mock(Repository.class, CALLS_REAL_METHODS);
        doReturn(new PagedList<>(List.of(), 0)).when(repo).pageOffset(any(), anyInt(), anyInt(), any());

        repo.page(null, new PageInfo(1, 25, "id"));

        verify(repo).pageOffset(null, 0, 25, "id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void facetsDefaultReturnsEmptyListWithoutTouchingPageOffset() {
        Repository<String, Integer> repo = mock(Repository.class, CALLS_REAL_METHODS);

        List<Facet> facets = repo.facets("anything");

        assertTrue(facets.isEmpty());
        verify(repo, never()).pageOffset(any(), anyInt(), anyInt(), any());
    }
}
