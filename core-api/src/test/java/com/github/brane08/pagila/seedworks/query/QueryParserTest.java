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

    @Test
    void parseNumberAcceptsWholeNumbersOnly() {
        assertEquals(60, QueryParser.parseNumber("length", " 60 ").intValue());
        assertEquals(2.5, QueryParser.parseNumber("rate", "2.5").doubleValue());
        assertThrows(InvalidQueryException.class, () -> QueryParser.parseNumber("length", "5abc"));
        assertThrows(InvalidQueryException.class, () -> QueryParser.parseNumber("length", ""));
        assertThrows(InvalidQueryException.class, () -> QueryParser.parseNumber("length", null));
    }

    enum Color { RED }

    @Test
    void coerceConvertsToTargetType() {
        assertEquals("x", QueryParser.coerce("f", null, "x"));
        assertEquals("x", QueryParser.coerce("f", String.class, "x"));
        assertEquals(7, QueryParser.coerce("f", Integer.class, "7"));
        assertEquals(7L, QueryParser.coerce("f", long.class, "7"));
        assertEquals((short) 7, QueryParser.coerce("f", Short.class, "7"));
        assertEquals(1.5d, QueryParser.coerce("f", Double.class, "1.5"));
        assertEquals(1.5f, QueryParser.coerce("f", Float.class, "1.5"));
        assertEquals(new java.math.BigDecimal("4.99"), QueryParser.coerce("f", java.math.BigDecimal.class, "4.99"));
        assertEquals(Boolean.TRUE, QueryParser.coerce("f", Boolean.class, "TRUE"));
        assertEquals(Boolean.FALSE, QueryParser.coerce("f", boolean.class, "false"));
        assertEquals(Color.RED, QueryParser.coerce("f", Color.class, "RED"));
        assertEquals(java.time.LocalDate.of(2024, 1, 2), QueryParser.coerce("f", java.time.LocalDate.class, "2024-01-02"));
        assertEquals(java.time.LocalDateTime.of(2024, 1, 2, 3, 4),
                QueryParser.coerce("f", java.time.LocalDateTime.class, "2024-01-02T03:04"));
        assertEquals(java.time.OffsetDateTime.parse("2024-01-02T03:04:00Z"),
                QueryParser.coerce("f", java.time.OffsetDateTime.class, "2024-01-02T03:04:00Z"));
        assertEquals("raw", QueryParser.coerce("f", java.util.UUID.class, "raw"));
    }

    @Test
    void coerceRejectsInvalidValues() {
        assertThrows(InvalidQueryException.class, () -> QueryParser.coerce("f", Integer.class, "abc"));
        assertThrows(InvalidQueryException.class, () -> QueryParser.coerce("f", Boolean.class, "maybe"));
        assertThrows(InvalidQueryException.class, () -> QueryParser.coerce("f", Color.class, "BLUE"));
        assertThrows(InvalidQueryException.class, () -> QueryParser.coerce("f", java.time.LocalDate.class, "nope"));
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
        InvalidQueryException ex = assertThrows(InvalidQueryException.class, () -> new StubParser("title"));
        assertEquals("Malformed filter: title", ex.getMessage());
    }

    @Test
    void emptySegmentFails() {
        assertThrows(InvalidQueryException.class, () -> new StubParser("title==ACE;;length=gt=5"));
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
