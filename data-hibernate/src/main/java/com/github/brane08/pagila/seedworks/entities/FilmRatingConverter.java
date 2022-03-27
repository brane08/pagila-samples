package com.github.brane08.pagila.seedworks.entities;

import com.github.brane08.pagila.film.entities.FilmRating;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FilmRatingConverter implements AttributeConverter<FilmRating, String> {
    @Override
    public String convertToDatabaseColumn(FilmRating filmRating) {
        return filmRating.rating();
    }

    @Override
    public FilmRating convertToEntityAttribute(String s) {
        return FilmRating.fromValue(s);
    }
}
