package com.github.brane08.pagila.seedworks.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldDefTest {

    @Test
    void twoArgConstructorUsesNameAsFieldAndIsNotArray() {
        FieldDef def = new FieldDef("title", "STRING");

        assertEquals("title", def.name());
        assertEquals("title", def.field());
        assertEquals("STRING", def.type());
        assertFalse(def.array());
    }

    @Test
    void threeArgConstructorSetsArrayFlagAndUsesNameAsField() {
        FieldDef def = new FieldDef("id", "INT", true);

        assertEquals("id", def.field());
        assertTrue(def.array());
    }

    @Test
    void canonicalConstructorAllowsFieldToDifferFromName() {
        FieldDef def = new FieldDef("filmId", "INT", "film_id", false);

        assertEquals("filmId", def.name());
        assertEquals("film_id", def.field());
    }

    @ParameterizedTest
    @CsvSource({
            "INT, 42, 42",
            "int, 7, 7",
            "INT, abc, 0",
            "SHORT, 3, 3",
            "LONG, 9000000000, 9000000000",
            "FLOAT, 1.5, 1.5",
            "DOUBLE, 2.25, 2.25",
    })
    void numericTypesAreConvertedOrDefaultToZero(String type, String input, String expected) {
        Object value = new FieldDef("f", type).asValue(input);

        assertEquals(parseExpected(type, expected), value);
    }

    private static Object parseExpected(String type, String expected) {
        return switch (type.toUpperCase()) {
            case "SHORT" -> Short.parseShort(expected);
            case "LONG" -> Long.parseLong(expected);
            case "FLOAT" -> Float.parseFloat(expected);
            case "DOUBLE" -> Double.parseDouble(expected);
            default -> Integer.parseInt(expected);
        };
    }

    @Test
    void booleanTypeIsTrueOnlyForTrueIgnoringCase() {
        FieldDef def = new FieldDef("active", "BOOLEAN");

        assertEquals(true, def.asValue("true"));
        assertEquals(true, def.asValue("TRUE"));
        assertEquals(false, def.asValue("yes"));
        assertEquals(false, def.asValue("1"));
    }

    @Test
    void unknownTypeReturnsRawString() {
        FieldDef def = new FieldDef("title", "STRING");

        assertEquals("ACADEMY", def.asValue("ACADEMY"));
    }

    @Test
    void emptyStringForStringTypeIsReturnedUnchanged() {
        assertEquals("", new FieldDef("title", "STRING").asValue(""));
    }

    @Test
    void arrayDefSplitsOnCommaAndConvertsEachItem() {
        FieldDef def = new FieldDef("id", "INT", true);

        Object value = def.asValue("1,2,x,4");

        assertInstanceOf(List.class, value);
        assertEquals(List.of(1, 2, 0, 4), value);
    }

    @Test
    void arrayDefOfStringsKeepsItems() {
        FieldDef def = new FieldDef("rating", "STRING", true);

        assertEquals(List.of("G", "PG"), def.asValue("G,PG"));
    }

    @Test
    void arrayDefWithSingleItemReturnsSingleElementList() {
        assertEquals(List.of(5), new FieldDef("id", "INT", true).asValue("5"));
    }
}
