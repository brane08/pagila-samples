package com.github.brane08.pagila.seedworks.app;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FiqlQueryBeanTest {

    @Test
    void threeArgBuildUsesDefaultSortIdAndAscendingDirection() {
        FiqlQueryBean bean = FiqlQueryBean.build("title==ACE", 2, 10);

        assertEquals("title==ACE", bean.qry);
        assertEquals(2, bean.page);
        assertEquals(10, bean.size);
        assertEquals("id", bean.sort);
        assertEquals(1, bean.direction);
    }

    @Test
    void fourArgBuildUsesGivenSortAndDefaultDirection() {
        FiqlQueryBean bean = FiqlQueryBean.build("q", 1, 5, "name");

        assertEquals("name", bean.sort);
        assertEquals(1, bean.direction);
        assertEquals(5, bean.size);
    }

    @Test
    void fiveArgBuildKeepsAllValuesIncludingDescendingDirection() {
        FiqlQueryBean bean = FiqlQueryBean.build("q", 3, 20, "rental_date", -1);

        assertEquals("q", bean.qry);
        assertEquals(3, bean.page);
        assertEquals(20, bean.size);
        assertEquals("rental_date", bean.sort);
        assertEquals(-1, bean.direction);
    }

    @Test
    void nullQueryIsStoredAsIs() {
        FiqlQueryBean bean = FiqlQueryBean.build(null, 1, 10);

        assertNull(bean.qry);
    }

    @Test
    void blankQueryIsStoredAsIs() {
        assertEquals("  ", FiqlQueryBean.build("  ", 1, 10).qry);
    }

    @Test
    void pageInfoOnFirstPageHasZeroOffset() {
        PageInfo info = FiqlQueryBean.build("q", 1, 25, "name", 1).pageInfo();

        assertEquals(new PageInfo(1, 25, "name"), info);
        assertEquals(0, info.offset());
    }

    @Test
    void pageInfoOffsetIsPageMinusOneTimesSize() {
        PageInfo info = FiqlQueryBean.build("q", 3, 10).pageInfo();

        assertEquals(3, info.page());
        assertEquals(10, info.size());
        assertEquals("id", info.order());
        assertEquals(20, info.offset());
    }
}
