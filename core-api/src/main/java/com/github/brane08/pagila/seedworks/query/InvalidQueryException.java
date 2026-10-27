package com.github.brane08.pagila.seedworks.query;

/** Client-supplied filter or sort is malformed, or references a field or operator that is not supported. */
public class InvalidQueryException extends IllegalArgumentException {

    public InvalidQueryException(String message) {
        super(message);
    }

    public InvalidQueryException(String field, Throwable cause) {
        super("Unknown field: " + (field != null && field.length() > 64 ? field.substring(0, 64) + "..." : field), cause);
    }
}
