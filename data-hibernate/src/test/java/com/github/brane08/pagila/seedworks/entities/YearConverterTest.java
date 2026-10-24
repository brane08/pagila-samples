package com.github.brane08.pagila.seedworks.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Year;
import org.junit.jupiter.api.Test;

class YearConverterTest {

    private final YearConverter converter = new YearConverter();

    @Test
    void convertsYearToShortForDatabase() {
        assertEquals(Short.valueOf((short) 2006), converter.convertToDatabaseColumn(Year.of(2006)));
    }

    @Test
    void nullYearMapsToNullColumn() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertsShortColumnToYear() {
        assertEquals(Year.of(2006), converter.convertToEntityAttribute((short) 2006));
    }

    @Test
    void nullColumnMapsToNullYear() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
