package com.github.brane08.pagila.seedworks.beans;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PagedListTest {

    @Test
    void accessorsReturnConstructorArguments() {
        PagedList<String> paged = new PagedList<>(List.of("a", "b"), 5);

        assertEquals(List.of("a", "b"), paged.list());
        assertEquals(5, paged.totalCount());
    }

    @Test
    void totalPageCountRoundsUp() {
        assertEquals(3, new PagedList<>(List.of("a", "b"), 5).totalPageCount());
        assertEquals(5, new PagedList<>(List.of("a"), 5).totalPageCount());
        assertEquals(3, new PagedList<>(List.of("a"), 5).totalPageCount(2));
    }

    @Test
    void totalPageCountOnEmptyPageIsZero() {
        assertEquals(0, new PagedList<>(List.of(), 0).totalPageCount());
        assertEquals(0, new PagedList<>(List.of("a"), 5).totalPageCount(0));
    }
}
