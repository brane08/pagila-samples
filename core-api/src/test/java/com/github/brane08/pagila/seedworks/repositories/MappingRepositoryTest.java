package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MappingRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void pageDelegatesToPageOffsetWithMapperAndComputedOffset() {
        MappingRepository<String, Integer> repo = mock(MappingRepository.class, CALLS_REAL_METHODS);
        Function<String, Integer> mapper = String::length;
        PagedList<Integer> expected = new PagedList<>(List.of(3), 1);
        doReturn(expected).when(repo).pageOffset(eq("q"), eq(10), eq(10), eq("title"), same(mapper));

        PagedList<Integer> actual = repo.page("q", new PageInfo(2, 10, "title"), mapper);

        assertSame(expected, actual);
        verify(repo).pageOffset("q", 10, 10, "title", mapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageOnFirstPageUsesZeroOffset() {
        MappingRepository<String, Integer> repo = mock(MappingRepository.class, CALLS_REAL_METHODS);
        Function<String, Integer> mapper = String::length;
        doReturn(new PagedList<>(List.of(), 0)).when(repo).pageOffset(any(), anyInt(), anyInt(), any(), any());

        repo.page("q", new PageInfo(1, 50, "id"), mapper);

        verify(repo).pageOffset("q", 0, 50, "id", mapper);
    }
}
