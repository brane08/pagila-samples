package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.JPAQueryParser;
import com.github.brane08.pagila.seedworks.repositories.JPARepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class FilmsRepository extends JPARepository<Film, Integer> {

    private final FilmMapper mapper;

    public FilmsRepository(EntityManager em, FilmMapper mapper) {
        super(em);
        this.mapper = mapper;
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
        JPAQueryParser<FilmView> parser = new JPAQueryParser<>(em(), "", FilmView.class);
        List<FilmView> list = parser.getResults(request);
        long count = parser.getCount();
        return new PagedList<>(mapper.filmViewsToInfo(list), (int) count);
    }
}
