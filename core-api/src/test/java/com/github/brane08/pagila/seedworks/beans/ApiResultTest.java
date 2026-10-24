package com.github.brane08.pagila.seedworks.beans;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResultTest {

    @Test
    void singleArgConstructorIsSuccessful() {
        ApiResult<String> result = new ApiResult<>("data");

        assertTrue(result.isSuccess());
        assertEquals("data", result.getData());
    }

    @Test
    void twoArgConstructorKeepsSuccessFlag() {
        ApiResult<String> result = new ApiResult<>(false, "err");

        assertFalse(result.isSuccess());
        assertEquals("err", result.getData());
    }

    @Test
    void singleWrapsObjectAsSuccess() {
        ApiResult<Integer> result = ApiResult.single(7);

        assertTrue(result.isSuccess());
        assertEquals(7, result.getData());
    }

    @Test
    void countWrapsTotalInCountInfo() {
        ApiResult<CountInfo> result = ApiResult.count(42L);

        assertTrue(result.isSuccess());
        assertEquals(new CountInfo(42L), result.getData());
    }

    @Test
    void arrayWithoutTotalUsesZeroTotalCount() {
        List<String> list = List.of("a", "b");

        ApiResult<List<String>> result = ApiResult.array(list);

        assertInstanceOf(PageResult.class, result);
        assertTrue(result.isSuccess());
        assertEquals(list, result.getData());
        assertEquals(0, ((PageResult<String>) result).getTotalCount());
    }

    @Test
    void arrayWithTotalKeepsTotalCount() {
        ApiResult<List<String>> result = ApiResult.array(List.of("a"), 9);

        assertEquals(9, ((PageResult<String>) result).getTotalCount());
    }

    @Test
    void arrayFromPagedListCopiesListAndTotal() {
        PagedList<String> paged = new PagedList<>(List.of("x", "y"), 12);

        ApiResult<List<String>> result = ApiResult.array(paged);

        assertEquals(List.of("x", "y"), result.getData());
        assertEquals(12, ((PageResult<String>) result).getTotalCount());
    }
}
