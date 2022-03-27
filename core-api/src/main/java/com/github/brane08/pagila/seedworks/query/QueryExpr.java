package com.github.brane08.pagila.seedworks.query;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record QueryExpr(String field, String operator, String value) {

    public static final String OP_LT = "lt";
    public static final String OP_GT = "gt";
    public static final String OP_EQ = "";
    public static final String OP_LK = "like";
    public static final String OP_NE = "!";
    public static final String OP_IN = "in";
    public static final String OP_OUT = "out";
    public static final String OP_AND = "and";
    public static final String OP_OR = "or";

    public QueryExpr(String[] args) {
        this(List.of(args));
    }

    public QueryExpr(List<String> args) {
        this(args.get(0), args.get(1), args.get(2));
    }

    private boolean isArray() {
        return OP_IN.equalsIgnoreCase(operator) || OP_OUT.equalsIgnoreCase(operator);
    }

    public Serializable typedValue(final String type) {
        if (isArray()) {
            return new ArrayList<>(Arrays.stream(value.split(",")).map(item -> convertValue(type, item)).toList());
        } else {
            return convertValue(type, value);
        }
    }

    private Serializable convertValue(String type, String value) {
        return switch (type.toUpperCase()) {
            case "INT" -> NumberUtils.toInt(value);
            case "SHORT" -> NumberUtils.toShort(value);
            case "LONG" -> NumberUtils.toLong(value);
            case "FLOAT" -> NumberUtils.toFloat(value);
            case "DOUBLE" -> NumberUtils.toDouble(value);
            case "BOOLEAN" -> "TRUE".equalsIgnoreCase(value.toUpperCase());
            default -> value;
        };
    }

    @Override
    public String toString() {
        return "QueryExpr{" +
                "field='" + field + '\'' +
                ", operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
