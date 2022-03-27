package com.github.brane08.pagila.film.beans;


import java.io.Serializable;
import java.time.Instant;

public record FilmCategoryInfo(Long filmId, Long categoryId, Instant lastUpdate) implements Serializable {
}
