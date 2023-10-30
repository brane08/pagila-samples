package com.github.brane08.pagila.seedworks.query;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;

public record FieldDef(String name, String type, String field, boolean array) {

    public FieldDef(String name, String type) {
        this(name, type, name, false);
    }

    public FieldDef(String name, String type, boolean array) {
        this(name, type, name, array);
    }

    public Object asValue(String queryValue) {
        if (array) {
            return new ArrayList<>(Arrays.stream(queryValue.split(",")).map(this::convertValue).toList());
        } else {
            return convertValue(queryValue);
        }
    }

    private Object convertValue(String queryValue) {
        return switch (type.toUpperCase()) {
            case "INT" -> NumberUtils.toInt(queryValue);
            case "SHORT" -> NumberUtils.toShort(queryValue);
            case "LONG" -> NumberUtils.toLong(queryValue);
            case "FLOAT" -> NumberUtils.toFloat(queryValue);
            case "DOUBLE" -> NumberUtils.toDouble(queryValue);
            case "BOOLEAN" -> "TRUE".equalsIgnoreCase(queryValue.toUpperCase());
            default -> queryValue;
        };
    }
}
