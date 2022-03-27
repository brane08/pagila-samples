package com.github.brane08.pagila.film.infra;

import com.github.brane08.pagila.actor.infra.ActorsMapper;
import com.github.brane08.pagila.film.beans.CategoryInfo;
import com.github.brane08.pagila.film.beans.FilmInfo;
import com.github.brane08.pagila.film.entities.Category;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.Language;
import com.github.brane08.pagila.seedworks.infra.CommonConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Year;
import java.util.List;

@Mapper(config = CommonConfig.class, uses = ActorsMapper.class)
public interface FilmsMapper {
	FilmsMapper INSTANCE = Mappers.getMapper(FilmsMapper.class);

	@Mapping(target = "language", source = "language.name")
	@Mapping(target = "originalLanguage", source = "originalLanguage.name")
	FilmInfo map(Film film);

	List<FilmInfo> map(List<Film> film);

	CategoryInfo map(Category category);

	default int map(Year value) {
		return value.getValue();
	}

	default String categoryToString(Category category) {
		return category.name;
	}
}
