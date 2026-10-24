package com.github.brane08.pagila.seedworks.beans;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageInfoTest {

    @Test
    void accessorsReturnConstructorArguments() {
        PageInfo info = new PageInfo(2, 15, "name");

        assertEquals(2, info.page());
        assertEquals(15, info.size());
        assertEquals("name", info.order());
    }

    @Test
    void firstPageOffsetIsZero() {
        assertEquals(0, new PageInfo(1, 10, "id").offset());
    }

    @Test
    void offsetIsPageMinusOneTimesSize() {
        assertEquals(10, new PageInfo(2, 10, "id").offset());
        assertEquals(30, new PageInfo(4, 10, "id").offset());
    }

    @Test
    void zeroSizeGivesZeroOffset() {
        assertEquals(0, new PageInfo(5, 0, "id").offset());
    }
}
