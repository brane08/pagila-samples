package com.github.brane08.pagila.seedworks.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.brane08.pagila.film.entities.FilmRating;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import org.junit.jupiter.api.Test;

class FilmRatingTypeTest {

    private final FilmRatingType type = new FilmRatingType();

    @Test
    void mapsToVarcharAndReturnsEnumClass() {
        assertEquals(Types.VARCHAR, type.getSqlType());
        assertEquals(FilmRating.class, type.returnedClass());
        assertFalse(type.isMutable());
    }

    @Test
    void equalsAndHashCodeFollowEnumValues() {
        assertTrue(type.equals(FilmRating.PG13, FilmRating.PG13));
        assertFalse(type.equals(FilmRating.PG13, FilmRating.R));
        assertEquals(type.hashCode(FilmRating.G), type.hashCode(FilmRating.G));
    }

    @Test
    void nullSafeGetParsesDatabaseRatingText() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("PG-13");

        assertEquals(FilmRating.PG13, type.nullSafeGet(rs, 1, null));
    }

    @Test
    void nullSafeGetFallsBackToNotRatedForUnknownText() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("XYZ");

        assertEquals(FilmRating.NR, type.nullSafeGet(rs, 1, null));
    }

    @Test
    void nullSafeSetWritesRatingText() throws Exception {
        PreparedStatement st = mock(PreparedStatement.class);

        type.nullSafeSet(st, FilmRating.PG13, 2, null);

        verify(st).setString(2, "PG-13");
    }

    @Test
    void nullSafeSetWritesNullAsVarchar() throws Exception {
        PreparedStatement st = mock(PreparedStatement.class);

        type.nullSafeSet(st, null, 2, null);

        verify(st).setNull(2, Types.VARCHAR);
    }

    @Test
    void copyDisassembleAssembleAndReplaceRoundTrip() {
        assertSame(FilmRating.R, type.deepCopy(FilmRating.R));
        assertEquals(FilmRating.R, type.assemble(type.disassemble(FilmRating.R), null));
        assertSame(FilmRating.G, type.replace(FilmRating.G, FilmRating.PG, null));
        assertNull(type.deepCopy(null));
    }
}
