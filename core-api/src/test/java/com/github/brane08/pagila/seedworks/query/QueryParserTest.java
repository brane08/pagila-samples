package com.github.brane08.pagila.seedworks.query;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryParserTest {

    /** Minimal concrete parser that records the arguments of getResults and returns canned values. */
    static class StubParser extends QueryParser<String> {
        final List<String> calls = new ArrayList<>();
        int count;

        StubParser(String queryString) {
            super(queryString, String.class);
        }

        @Override
        public List<String> getResults(int offset, int size, String order) {
            calls.add(offset + "|" + size + "|" + order);
            return List.of("r");
        }

        @Override
        public int getCount() {
            return count;
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankOrNullQueryYieldsNoExpressions(String qry) {
        assertTrue(new StubParser(qry).exprs().isEmpty());
    }

    @Test
    void singleExpressionIsParsedIntoOneQueryExpr() {
        List<QueryExpr> exprs = new StubParser("title==ACE").exprs();

        assertEquals(List.of(new QueryExpr("title", "", "ACE")), exprs);
    }

    @Test
    void multipleExpressionsAreSplitOnSemicolonInOrder() {
        List<QueryExpr> exprs = new StubParser("rating==G;length=lt=90;title=like=A%").exprs();

        assertEquals(3, exprs.size());
        assertEquals(new QueryExpr("rating", "", "G"), exprs.getFirst());
        assertEquals(new QueryExpr("length", "lt", "90"), exprs.get(1));
        assertEquals(new QueryExpr("title", "like", "A%"), exprs.get(2));
    }

    @Test
    void gtAndInOperatorsAreParsed() {
        List<QueryExpr> exprs = new StubParser("rental_rate=gt=2.99;film_id=in=1,2,3").exprs();

        assertEquals("gt", exprs.getFirst().operator());
        assertEquals("in", exprs.get(1).operator());
        assertEquals("1,2,3", exprs.get(1).value());
    }

    @Test
    void negationOperatorIsParsed() {
        assertEquals(new QueryExpr("rating", "!", "R"), new StubParser("rating=!=R").exprs().getFirst());
    }

    @Test
    void trailingSemicolonIsIgnoredBySplit() {
        assertEquals(1, new StubParser("title==ACE;").exprs().size());
    }

    @Test
    void expressionWithoutEqualsSignFails() {
        assertThrows(IndexOutOfBoundsException.class, () -> new StubParser("title"));
    }

    @Test
    void equalsSignsInValueArePreserved() {
        assertEquals(new QueryExpr("a", "", "b=c"), new StubParser("a==b=c").exprs().getFirst());
    }

    @Test
    void constructorStoresResultClass() {
        StubParser parser = new StubParser("x==1");

        assertEquals(String.class, parser.resultClass());
    }

    @Test
    void getResultsWithPageInfoDelegatesUsingOffsetSizeAndOrder() {
        StubParser parser = new StubParser("");

        List<String> results = parser.getResults(new PageInfo(2, 5, "name"));

        assertEquals(List.of("r"), results);
        assertEquals(List.of("5|5|name"), parser.calls);
    }

    @Test
    void getResultsWithPageInfoOnFirstPageUsesZeroOffset() {
        StubParser parser = new StubParser("");

        parser.getResults(new PageInfo(1, 10, "id"));

        assertEquals(List.of("0|10|id"), parser.calls);
    }

    @Test
    void getCountIsDelegatedToSubclass() {
        StubParser parser = new StubParser("");
        parser.count = 42;

        assertEquals(42, parser.getCount());
    }

    @Test
    void toQueryExprsReturnsSameResultAsExprsAccessor() {
        StubParser parser = new StubParser("");
        assertSame(parser.exprs(), parser.toQueryExprs(""));
    }
}
