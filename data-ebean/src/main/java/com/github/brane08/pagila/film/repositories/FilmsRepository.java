package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.List;

public class FilmsRepository extends EbeanRepository<Film, Integer> {

    private final FilmMapper mapper;

    public FilmsRepository(Database db) {
        super(db);
        this.mapper = Mappers.getMapper(FilmMapper.class);
    }

    @Override
    protected List<String> getLoadFields() {
        return List.of("categories", "language", "originalLanguage");
    }

    @Override
    protected Class<Film> entityClass() {
        return Film.class;
    }

    @Override
    protected String filterProperty() {
        return "title";
    }

    public PagedList<FilmViewInfo> listFilms(final PageInfo request) {
        QueryParser<FilmView> parser = buildParser("", FilmView.class);
        List<FilmView> list = parser.getResults(request);
        long count = parser.getCount();
        return new PagedList<>(mapper.filmViewsToInfo(list), (int) count);
    }
}
