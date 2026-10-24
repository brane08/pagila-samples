package com.github.brane08.pagila.seedworks.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExprTest {

    @Test
    void stringArrayConstructorMapsThreeArgsToFieldOperatorValue() {
        QueryExpr expr = new QueryExpr(new String[]{"title", QueryExpr.OP_LK, "ACE%"});

        assertEquals("title", expr.field());
        assertEquals("like", expr.operator());
        assertEquals("ACE%", expr.value());
    }

    @Test
    void listConstructorMapsThreeArgsToFieldOperatorValue() {
        QueryExpr expr = new QueryExpr(List.of("rating", QueryExpr.OP_EQ, "G"));

        assertEquals("rating", expr.field());
        assertEquals("", expr.operator());
        assertEquals("G", expr.value());
    }

    @Test
    void constructorWithFewerThanThreeArgsFails() {
        assertThrows(IndexOutOfBoundsException.class, () -> new QueryExpr(new String[]{"a", "b"}));
        assertThrows(IndexOutOfBoundsException.class, () -> new QueryExpr(List.of("a")));
    }

    @Test
    void operatorConstantsHaveFiqlValues() {
        assertEquals("lt", QueryExpr.OP_LT);
        assertEquals("gt", QueryExpr.OP_GT);
        assertEquals("", QueryExpr.OP_EQ);
        assertEquals("like", QueryExpr.OP_LK);
        assertEquals("!", QueryExpr.OP_NE);
        assertEquals("in", QueryExpr.OP_IN);
        assertEquals("out", QueryExpr.OP_OUT);
        assertEquals("and", QueryExpr.OP_AND);
        assertEquals("or", QueryExpr.OP_OR);
    }

    @Test
    void typedValueWithUnknownTypeReturnsRawString() {
        QueryExpr expr = new QueryExpr(List.of("title", QueryExpr.OP_EQ, "ACADEMY"));

        assertEquals("ACADEMY", expr.typedValue("STRING"));
    }

    @Test
    void typedValueConvertsIntWithInvalidInputFallingBackToZero() {
        assertEquals(42, new QueryExpr(List.of("id", QueryExpr.OP_EQ, "42")).typedValue("INT"));
        assertEquals(0, new QueryExpr(List.of("id", QueryExpr.OP_EQ, "x1")).typedValue("int"));
    }

    @Test
    void typedValueConvertsNumericTypes() {
        assertEquals((short) 3, new QueryExpr(List.of("n", QueryExpr.OP_EQ, "3")).typedValue("SHORT"));
        assertEquals(9000000000L, new QueryExpr(List.of("n", QueryExpr.OP_EQ, "9000000000")).typedValue("LONG"));
        assertEquals(1.5f, new QueryExpr(List.of("n", QueryExpr.OP_EQ, "1.5")).typedValue("FLOAT"));
        assertEquals(2.25d, new QueryExpr(List.of("n", QueryExpr.OP_EQ, "2.25")).typedValue("DOUBLE"));
    }

    @Test
    void typedValueBooleanIsTrueOnlyForTrueIgnoringCase() {
        QueryExpr truthy = new QueryExpr(List.of("active", QueryExpr.OP_EQ, "True"));
        QueryExpr falsy = new QueryExpr(List.of("active", QueryExpr.OP_EQ, "no"));

        assertTrue((Boolean) truthy.typedValue("BOOLEAN"));
        assertFalse((Boolean) falsy.typedValue("BOOLEAN"));
    }

    @Test
    void inOperatorSplitsValueIntoConvertedList() {
        QueryExpr expr = new QueryExpr(List.of("id", QueryExpr.OP_IN, "1,2,3"));

        assertEquals(List.of(1, 2, 3), expr.typedValue("INT"));
    }

    @Test
    void outOperatorIsTreatedAsArrayCaseInsensitively() {
        QueryExpr expr = new QueryExpr(List.of("rating", "OUT", "G,PG"));

        assertEquals(List.of("G", "PG"), expr.typedValue("STRING"));
    }

    @Test
    void inOperatorUppercaseIsAlsoArray() {
        QueryExpr expr = new QueryExpr(List.of("id", "IN", "7"));

        assertEquals(List.of(7), expr.typedValue("INT"));
    }

    @Test
    void nonArrayOperatorKeepsCommaInValueAsString() {
        QueryExpr expr = new QueryExpr(List.of("title", QueryExpr.OP_LK, "A,B"));

        assertEquals("A,B", expr.typedValue("STRING"));
    }

    @Test
    void toStringContainsAllComponents() {
        String text = new QueryExpr(List.of("title", "lt", "x")).toString();

        assertEquals("QueryExpr{field='title', operator='lt', value='x'}", text);
    }
}
