package com.github.brane08.pagila.seedworks.query;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class QueryParser<T> {

    protected final List<QueryExpr> exprs;
    protected final Class<T> resultClass;

    protected QueryParser(String queryString, Class<T> resultClass) {
        this.exprs = toQueryExprs(queryString);
        this.resultClass = resultClass;
    }

    protected List<QueryExpr> exprs() {
        return exprs;
    }

    protected Class<T> resultClass() {
        return resultClass;
    }

    protected List<QueryExpr> toQueryExprs(String queryPart) {
        if (StringUtils.isBlank(queryPart)) {
            return Collections.emptyList();
        }
        return Arrays.stream(queryPart.split(";")).map(item -> {
            String[] args = item.split("=", 3);
            if (args.length != 3) {
                throw new InvalidQueryException("Malformed filter: " + item);
            }
            return new QueryExpr(args);
        }).toList();
    }

    /** Strict, locale-independent number parse: the whole value must be numeric, otherwise a 400-mapped exception. */
    protected static Number parseNumber(String field, String raw) {
        String value = raw == null ? "" : raw.trim();
        ParsePosition pos = new ParsePosition(0);
        Number number = value.isEmpty() ? null : NumberFormat.getInstance(Locale.ROOT).parse(value, pos);
        if (number == null || pos.getIndex() != value.length()) {
            throw new InvalidQueryException("Invalid number for " + StringUtils.abbreviate(field, 64) + ": "
                    + StringUtils.abbreviate(raw, 64));
        }
        return number;
    }

    /** Converts a filter value to the field's Java type; unknown or null types keep the raw string. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static Object coerce(String field, Class<?> type, String raw) {
        if (type == null || type == String.class || type == Object.class) {
            return raw;
        }
        try {
            if (type == Integer.class || type == int.class) return Integer.valueOf(raw.trim());
            if (type == Long.class || type == long.class) return Long.valueOf(raw.trim());
            if (type == Short.class || type == short.class) return Short.valueOf(raw.trim());
            if (type == Double.class || type == double.class) return Double.valueOf(raw.trim());
            if (type == Float.class || type == float.class) return Float.valueOf(raw.trim());
            if (type == java.math.BigDecimal.class) return new java.math.BigDecimal(raw.trim());
            if (type == Boolean.class || type == boolean.class) {
                if ("true".equalsIgnoreCase(raw.trim())) return Boolean.TRUE;
                if ("false".equalsIgnoreCase(raw.trim())) return Boolean.FALSE;
                throw new IllegalArgumentException(raw);
            }
            if (type.isEnum()) return Enum.valueOf((Class<? extends Enum>) type, raw.trim());
            if (type == java.time.LocalDate.class) return java.time.LocalDate.parse(raw.trim());
            if (type == java.time.LocalDateTime.class) return java.time.LocalDateTime.parse(raw.trim());
            if (type == java.time.OffsetDateTime.class) return java.time.OffsetDateTime.parse(raw.trim());
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException ex) {
            throw new InvalidQueryException("Invalid value for " + StringUtils.abbreviate(field, 64) + ": "
                    + StringUtils.abbreviate(raw, 64));
        }
        return raw;
    }

    public List<T> getResults(PageInfo request) {
        return getResults(request.offset(), request.size(), request.order());
    }

    public abstract List<T> getResults(int offset, int size, String order);

    public abstract int getCount();
}
