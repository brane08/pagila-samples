package com.github.brane08.pagila.seedworks.beans;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void constructorIsSuccessfulAndExposesDataAndTotal() {
        PageResult<String> result = new PageResult<>(List.of("a"), 3);

        assertTrue(result.isSuccess());
        assertEquals(List.of("a"), result.getData());
        assertEquals(3, result.getTotalCount());
    }

    @Test
    void emptyPageKeepsZeroTotal() {
        PageResult<String> result = new PageResult<>(List.of(), 0);

        assertEquals(0, result.getTotalCount());
    }
}
