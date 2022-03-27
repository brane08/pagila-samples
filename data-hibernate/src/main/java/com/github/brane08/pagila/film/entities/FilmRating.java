package com.github.brane08.pagila.film.entities;

public enum FilmRating {
    G("G"),
    PG("PG"),
    PG13("PG-13"),
    R("R"),
    NC17("NC-17"),
    NR("NR");

    private final String rating;

    FilmRating(String rating) {
        this.rating = rating;
    }

    public static FilmRating fromValue(String rating) {
        for (FilmRating value : FilmRating.values()) {
            if (value.rating.equalsIgnoreCase(rating)) {
                return value;
            }
        }
        return NR;
    }

    public String rating() {
        return rating;
    }
}
