package com.github.brane08.pagila.seedworks.entities;

import com.github.brane08.pagila.film.entities.FilmRating;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class FilmRatingType implements UserType<FilmRating> {

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<FilmRating> returnedClass() {
        return FilmRating.class;
    }

    @Override
    public boolean equals(FilmRating x, FilmRating y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(FilmRating x) {
        return Objects.hash(x);
    }

    @Override
    public FilmRating nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String value = rs.getString(position);
        return FilmRating.fromValue(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, FilmRating value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, value.rating());
        }
    }

    @Override
    public FilmRating deepCopy(FilmRating value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(FilmRating value) {
        return deepCopy(value);
    }

    @Override
    public FilmRating assemble(Serializable cached, Object owner) {
        return deepCopy(FilmRating.fromValue((String) cached));
    }

    @Override
    public FilmRating replace(FilmRating detached, FilmRating managed, Object owner) {
        return detached;
    }
}
