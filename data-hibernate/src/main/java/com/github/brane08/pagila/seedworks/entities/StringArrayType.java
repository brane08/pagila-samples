package com.github.brane08.pagila.seedworks.entities;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Arrays;
import java.util.Objects;

public class StringArrayType implements UserType<String[]> {

    public String[] assemble(Serializable cached, Object owner) throws HibernateException {
        return deepCopy((String[]) cached);
    }

    public String[] deepCopy(String[] value) throws HibernateException {
        if (value == null) {
            return null;
        }
        return Arrays.copyOf(value, value.length);
    }

    public Serializable disassemble(final String[] value) throws HibernateException {
        return deepCopy(value);
    }

    @Override
    public boolean equals(final String[] o1, final String[] o2) throws HibernateException {
        return Arrays.equals(o1, o2);
    }

    @Override
    public int hashCode(final String[] values) throws HibernateException {
        return Objects.hashCode(values);
    }

    @Override
    public String[] nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        Array array = rs.getArray(position);
        return array != null ? (String[]) array.getArray() : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String[] value, int position, WrapperOptions options) throws HibernateException, SQLException {
        if (value != null) {
            options.getSession().doWork(connection -> {
                Array array = connection.createArrayOf("text", (String[]) value);
                st.setArray(position, array);
            });
        } else {
            st.setNull(position, getSqlType());
        }
    }

    public boolean isMutable() {
        return true;
    }

    public String[] replace(final String[] original, final String[] target, final Object owner) throws HibernateException {
        return original;
    }

    @Override
    public int getSqlType() {
        return Types.ARRAY;
    }

    @Override
    public Class<String[]> returnedClass() {
        return String[].class;
    }
}