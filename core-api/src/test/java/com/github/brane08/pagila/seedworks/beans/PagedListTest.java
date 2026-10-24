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
    void totalPageCountDividesTotalByPageLength() {
        // Current behaviour: plain integer division (5 / 2 = 2), Math.round is a no-op on int.
        assertEquals(2, new PagedList<>(List.of("a", "b"), 5).totalPageCount());
        assertEquals(5, new PagedList<>(List.of("a"), 5).totalPageCount());
    }

    @Test
    void totalPageCountOnEmptyPageThrowsArithmeticException() {
        // Current behaviour: division by list.size() == 0. Flagged for review.
        PagedList<String> empty = new PagedList<>(List.of(), 0);

        assertThrows(ArithmeticException.class, empty::totalPageCount);
    }
}
