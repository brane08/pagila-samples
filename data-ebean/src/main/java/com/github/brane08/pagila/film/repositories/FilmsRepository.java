package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.beans.NicerFilmViewInfo;
import com.github.brane08.pagila.film.beans.SalesByFilmCategoryInfo;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmCategoryFacet;
import com.github.brane08.pagila.film.entities.FilmPriceFacet;
import com.github.brane08.pagila.film.entities.FilmRatingFacet;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.entities.NicerFilmView;
import com.github.brane08.pagila.film.entities.SalesByFilmCategory;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.seedworks.beans.Facet;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FilmsRepository extends EbeanRepository<Film, Integer> {

    private final FilmMapper mapper;
    private final ActorMapper actorMapper;

    public FilmsRepository(Database db) {
        super(db);
        this.mapper = Mappers.getMapper(FilmMapper.class);
        this.actorMapper = Mappers.getMapper(ActorMapper.class);
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
        return offsetOfFilms(request.offset(), request.size(), request.order());
    }

    public PagedList<FilmViewInfo> offsetOfFilms(int offset, int size, String order) {
        QueryParser<FilmView> parser = buildParser("", FilmView.class);
        List<FilmView> list = parser.getResults(offset, size, order);
        long count = parser.getCount();
        return new PagedList<>(mapper.filmViewsToInfo(list), (int) count);
    }

    @Override
    public List<Facet> facets(String queryPart) {
        final List<Facet> facets = new ArrayList<>();

        FilmPriceFacet pf = db().find(FilmPriceFacet.class).findOne();
        if (pf != null) {
            Facet priceFacet = new Facet("price");
            priceFacet.addFacetValue("price_0_1", pf.getPrice01());
            priceFacet.addFacetValue("price_1_3", pf.getPrice13());
            priceFacet.addFacetValue("price_3_9", pf.getPrice39());
            facets.add(priceFacet);
        }

        List<FilmCategoryFacet> categories = db().find(FilmCategoryFacet.class).findList();
        facets.add(new Facet("category", categories.stream()
                .map(f -> new Facet.FacetValue(f.getKey(), f.getValue()))
                .collect(Collectors.toList())));

        List<FilmRatingFacet> ratings = db().find(FilmRatingFacet.class).findList();
        facets.add(new Facet("rating", ratings.stream()
                .map(f -> new Facet.FacetValue(f.getKey(), f.getValue()))
                .collect(Collectors.toList())));

        return facets;
    }

    public List<ActorInfo> listActors(int filmId) {
        Optional<Film> film = findById(filmId);
        return film.map(f -> actorMapper.actorsToInfos(f.getActors())).orElse(Collections.emptyList());
    }

    public PagedList<NicerFilmViewInfo> offsetOfNicerFilms(int offset, int size, String order) {
        QueryParser<NicerFilmView> parser = buildParser("", NicerFilmView.class);
        List<NicerFilmView> list = parser.getResults(offset, size, order);
        long count = parser.getCount();
        return new PagedList<>(mapper.nicerFilmViewsToInfo(list), (int) count);
    }

    public List<SalesByFilmCategoryInfo> allSalesByCategory() {
        List<SalesByFilmCategory> list = db().find(SalesByFilmCategory.class).findList();
        return mapper.salesByCategoryToInfos(list);
    }
}
