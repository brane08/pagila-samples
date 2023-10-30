package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.seedworks.beans.Facet;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
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
        String priceFacet = """
                    select
                    sum(case when price between 0 and 1 then 1 else 0 end) as price_0_1,
                    sum(case when price between 1 and 3 then 1 else 0 end) as price_1_3,
                    sum(case when price between 3 and 10 then 1 else 0 end) as price_3_9
                from film_list
                              """;
        final List<Facet> facets = new ArrayList<>();
        db().sqlQuery(priceFacet).findEachRow((row, num) -> {
            Facet facet = new Facet("price");
            facet.addFacetValue("price_0_1", row.getInt(1));
            facet.addFacetValue("price_1_3", row.getInt(2));
            facet.addFacetValue("price_3_9", row.getInt(3));
            facets.add(facet);
        });
        String categorySql = "select category as key, count(category) as value from film_list group by category";
        facets.add(new Facet("category", db().findDto(Facet.FacetValue.class, categorySql).findList()));
        String ratingSql = "SELECT rating_txt as key, count(rating_txt) as value FROM film_list group by rating_txt";
        facets.add(new Facet("rating", db().findDto(Facet.FacetValue.class, ratingSql).findList()));
        return facets;
    }
}
