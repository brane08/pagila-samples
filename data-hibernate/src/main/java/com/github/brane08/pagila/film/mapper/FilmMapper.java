package com.github.brane08.pagila.film.mapper;

import com.github.brane08.pagila.film.beans.CategoryInfo;
import com.github.brane08.pagila.film.beans.FilmInfo;
import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.entities.Category;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.entities.Language;
import com.github.brane08.pagila.seedworks.mapper.CommonConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Year;
import java.util.List;

@Mapper(config = CommonConfig.class)
public interface FilmMapper {

    FilmMapper INSTANCE = Mappers.getMapper(FilmMapper.class);

    FilmInfo filmToInfo(Film film);

    FilmViewInfo filmViewToInfo(FilmView view);

    List<FilmViewInfo> filmViewsToInfo(List<FilmView> view);

    List<FilmInfo> filmsToInfos(List<Film> film);

    CategoryInfo categoryToInfo(Category category);

    default int yearToInt(Year value) {
        return value.getValue();
    }

    default String categoryToString(Category category) {
        return category.getName();
    }

    default String languageToString(Language language) {
        return (language != null) ? language.getName() : "";
    }
}
