package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.beans.NicerFilmViewInfo;
import com.github.brane08.pagila.film.beans.SalesByFilmCategoryInfo;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmCategoryFacet;
import com.github.brane08.pagila.film.entities.FilmPriceFacet;
import com.github.brane08.pagila.film.entities.FilmRating;
import com.github.brane08.pagila.film.entities.FilmRatingFacet;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.entities.NicerFilmView;
import com.github.brane08.pagila.film.entities.SalesByFilmCategory;
import com.github.brane08.pagila.seedworks.beans.Facet;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.repositories.Repository;
import com.github.brane08.pagila.testsupport.EbeanQueryStubs;
import io.ebean.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilmsRepositoryTest {

    @Mock
    private Database db;

    private EbeanQueryStubs ebean;
    private FilmsRepository repository;

    @BeforeEach
    void setUp() {
        ebean = new EbeanQueryStubs(db);
        repository = new FilmsRepository(db);
    }

    @Test
    void listFilms_convertsPageToOffsetAndMapsRows() {
        ebean.rows(FilmView.class, List.of(filmView(1, "ACADEMY DINOSAUR")));
        ebean.count(FilmView.class, 7);

        PagedList<FilmViewInfo> result = repository.listFilms(new PageInfo(2, 10, "title"));

        assertEquals(7, result.totalCount());
        assertEquals(1, result.list().size());
        assertEquals("ACADEMY DINOSAUR", result.list().getFirst().getTitle());
        assertEquals(Integer.valueOf(1), result.list().getFirst().getFilmId());
        verify(ebean.first(FilmView.class)).setFirstRow(10);
        verify(ebean.first(FilmView.class)).setMaxRows(10);
        verify(ebean.first(FilmView.class).orderBy()).asc("title");
    }

    @Test
    void offsetOfFilms_descendingPrefixOrdersByDescending() {
        ebean.count(FilmView.class, 0);

        PagedList<FilmViewInfo> result = repository.offsetOfFilms(0, 5, "-price");

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.totalCount());
        verify(ebean.first(FilmView.class)).setFirstRow(0);
        verify(ebean.first(FilmView.class)).setMaxRows(5);
        verify(ebean.first(FilmView.class).orderBy()).desc("price");
        verify(ebean.first(FilmView.class).orderBy(), never()).asc(any());
    }

    @Test
    void offsetOfFilms_multipleSortItemsMixDirections() {
        repository.offsetOfFilms(0, 5, "title,-price");

        verify(ebean.first(FilmView.class).orderBy()).asc("title");
        verify(ebean.first(FilmView.class).orderBy()).desc("price");
    }

    @Test
    void offsetOfFilms_blankOrderSkipsSorting() {
        repository.offsetOfFilms(0, 5, "");

        verify(ebean.first(FilmView.class), never()).orderBy();
    }

    @Test
    void offsetOfFilms_nullOrderThrowsNpe() {
        // Current behaviour: EbeanQueryParser.appendSort requires a non-null order string.
        assertThrows(NullPointerException.class, () -> repository.offsetOfFilms(0, 5, null));
    }

    @Test
    void offsetOfFilms_mapsFilmViewFieldsIncludingPriceAsFloat() {
        FilmView view = filmView(1, "ACADEMY DINOSAUR");
        view.setPrice(new BigDecimal("0.99"));
        view.setRating(FilmRating.PG);
        ebean.rows(FilmView.class, List.of(view));

        FilmViewInfo info = repository.offsetOfFilms(0, 1, "").list().getFirst();

        assertEquals("Documentary", info.getCategory());
        assertEquals(0.99f, info.getPrice().floatValue(), 0.001f);
        assertEquals("PENELOPE GUINESS", info.getActors());
        assertEquals("PG", info.getRating());
    }

    @Test
    void offsetOfFilms_emptyResultWithZeroCount() {
        PagedList<FilmViewInfo> result = repository.offsetOfFilms(0, 10, "");

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.totalCount());
    }

    @Test
    void offsetOfNicerFilms_mapsRowsAndCount() {
        NicerFilmView view = new NicerFilmView();
        view.setFilmId(3);
        view.setTitle("AFRICAN EGG");
        ebean.rows(NicerFilmView.class, List.of(view));
        ebean.count(NicerFilmView.class, 42);

        PagedList<NicerFilmViewInfo> result = repository.offsetOfNicerFilms(20, 10, "title");

        assertEquals(42, result.totalCount());
        assertEquals("AFRICAN EGG", result.list().getFirst().getTitle());
        verify(ebean.first(NicerFilmView.class)).setFirstRow(20);
        verify(ebean.first(NicerFilmView.class)).setMaxRows(10);
        verify(ebean.first(NicerFilmView.class).orderBy()).asc("title");
    }

    @Test
    void offsetOfNicerFilms_emptyResult() {
        PagedList<NicerFilmViewInfo> result = repository.offsetOfNicerFilms(0, 10, "");

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.totalCount());
    }

    @Test
    void allSalesByCategory_mapsEntities() {
        SalesByFilmCategory entity = new SalesByFilmCategory();
        entity.setCategory("Action");
        entity.setTotalSales(new BigDecimal("1234.50"));
        ebean.rows(SalesByFilmCategory.class, List.of(entity));

        List<SalesByFilmCategoryInfo> infos = repository.allSalesByCategory();

        assertEquals(1, infos.size());
        assertEquals("Action", infos.getFirst().getCategory());
        assertEquals(new BigDecimal("1234.50"), infos.getFirst().getTotalSales());
    }

    @Test
    void allSalesByCategory_empty() {
        assertTrue(repository.allSalesByCategory().isEmpty());
    }

    @Test
    void facets_includePriceCategoryAndRatingWhenPriceRowExists() {
        FilmPriceFacet price = new FilmPriceFacet();
        price.setId(1);
        price.setPrice01(10);
        price.setPrice13(20);
        price.setPrice39(30);
        ebean.rows(FilmPriceFacet.class, List.of(price));
        ebean.rows(FilmCategoryFacet.class, List.of(categoryFacet("Action", 5), categoryFacet("Drama", 3)));
        ebean.rows(FilmRatingFacet.class, List.of(ratingFacet("PG", 8)));

        List<Facet> facets = repository.facets("");

        assertEquals(List.of("price", "category", "rating"), properties(facets));
        Facet priceFacet = facets.get(0);
        assertEquals(List.of("price_0_1", "price_1_3", "price_3_9"), keys(priceFacet));
        assertEquals(List.of(10, 20, 30), counts(priceFacet));
        assertEquals(List.of("Action", "Drama"), keys(facets.get(1)));
        assertEquals(List.of(5, 3), counts(facets.get(1)));
        assertEquals(List.of("PG"), keys(facets.get(2)));
        assertEquals(List.of(8), counts(facets.get(2)));
    }

    @Test
    void facets_omitPriceWhenNoPriceRow() {
        ebean.rows(FilmCategoryFacet.class, List.of(categoryFacet("Comedy", 2)));
        ebean.rows(FilmRatingFacet.class, List.of(ratingFacet("R", 1)));

        List<Facet> facets = repository.facets("");

        assertEquals(List.of("category", "rating"), properties(facets));
        assertEquals(List.of("Comedy"), keys(facets.get(0)));
        assertEquals(List.of("R"), keys(facets.get(1)));
    }

    @Test
    void facets_emptyTablesStillReturnCategoryAndRatingFacets() {
        List<Facet> facets = repository.facets("");

        assertEquals(List.of("category", "rating"), properties(facets));
        assertTrue(facets.get(0).getValues().isEmpty());
        assertTrue(facets.get(1).getValues().isEmpty());
    }

    @Test
    void facets_queryPartIsIgnored() {
        ebean.rows(FilmCategoryFacet.class, List.of(categoryFacet("Comedy", 2)));

        assertEquals(properties(repository.facets("")), properties(repository.facets("rating=PG")));
    }

    @Test
    void listActors_mapsActorsOfExistingFilm() {
        Actor actor = new Actor();
        actor.setActorId(1);
        actor.setFirstName("PENELOPE");
        actor.setLastName("GUINESS");
        Film film = film(1, "ACADEMY DINOSAUR");
        film.setActors(List.of(actor));
        stubFindById(Film.class, 1, film);

        List<ActorInfo> actors = repository.listActors(1);

        assertEquals(1, actors.size());
        assertEquals("PENELOPE", actors.getFirst().firstName());
        assertEquals("GUINESS", actors.getFirst().lastName());
    }

    @Test
    void listActors_filmWithNullActorsReturnsEmptyList() {
        stubFindById(Film.class, 2, film(2, "ACE GOLDFINGER"));

        assertTrue(repository.listActors(2).isEmpty());
    }

    @Test
    void listActors_missingFilmReturnsEmptyList() {
        stubFindById(Film.class, 99, null);

        assertTrue(repository.listActors(99).isEmpty());
    }

    @Test
    void count_returnsCountQueryResult() {
        ebean.count(Film.class, 12);

        assertEquals(12, repository.count(""));
    }

    @Test
    void pageOffset_mapsRowsAndCount() {
        ebean.rows(Film.class, List.of(film(1, "A")));
        ebean.count(Film.class, 1);

        PagedList<Film> page = repository.pageOffset("", 0, 5, "title");

        assertEquals(1, page.list().size());
        assertEquals(1, page.totalCount());
        verify(ebean.first(Film.class)).setFirstRow(0);
        verify(ebean.first(Film.class)).setMaxRows(5);
    }

    @Test
    void page_appliesPageInfoOffsetAndSize() {
        ebean.rows(Film.class, List.of(film(1, "A"), film(2, "B")));
        ebean.count(Film.class, 2);

        PagedList<Film> page = repository.page("", new PageInfo(2, 1, "title"));

        assertEquals(2, page.list().size());
        assertEquals(2, page.totalCount());
        verify(ebean.first(Film.class)).setFirstRow(1);
        verify(ebean.first(Film.class)).setMaxRows(1);
    }

    @Test
    void page_withMapperAppliesMapper() {
        ebean.rows(Film.class, List.of(film(1, "A"), film(2, "B")));
        ebean.count(Film.class, 2);

        PagedList<String> page = repository.page("", new PageInfo(2, 1, "title"), Film::getTitle);

        assertEquals(List.of("A", "B"), page.list());
        assertEquals(2, page.totalCount());
        verify(ebean.first(Film.class)).setFirstRow(1);
    }

    @Test
    void pageOffset_withMapperAppliesMapper() {
        ebean.rows(Film.class, List.of(film(7, "SEVEN")));
        ebean.count(Film.class, 1);

        PagedList<String> page = repository.pageOffset("", 0, 10, "title", Film::getTitle);

        assertEquals(List.of("SEVEN"), page.list());
        assertEquals(1, page.totalCount());
    }

    @Test
    void findById_returnsFilmWhenPresent() {
        Film film = film(3, "CHAMBER ITALIAN");
        stubFindById(Film.class, 3, film);

        Optional<Film> found = repository.findById(3);

        assertTrue(found.isPresent());
        assertEquals("CHAMBER ITALIAN", found.get().getTitle());
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        stubFindById(Film.class, 4, null);

        assertFalse(repository.findById(4).isPresent());
    }

    @Test
    void findById_withMapperAppliesMapperWhenPresent() {
        stubFindById(Film.class, 3, film(3, "CHAMBER ITALIAN"));
        stubFindById(Film.class, 4, null);

        assertEquals(Optional.of("CHAMBER ITALIAN"), repository.findById(3, Film::getTitle));
        assertEquals(Optional.empty(), repository.findById(4, Film::getTitle));
    }

    @Test
    void all_returnsRowsCappedAtMaxPageSize() {
        ebean.rows(Film.class, List.of(film(1, "A")));

        assertEquals(1, repository.all().size());
        verify(ebean.first(Film.class)).setMaxRows(Repository.MAX_PAGE_SIZE);
    }

    @Test
    void all_withMapperAppliesMapper() {
        ebean.rows(Film.class, List.of(film(1, "A"), film(2, "B")));

        assertEquals(List.of("A", "B"), repository.all(Film::getTitle));
    }

    @Test
    void exposesLoadFieldsEntityClassAndFilterProperty() {
        assertEquals(List.of("categories", "language", "originalLanguage"), repository.getLoadFields());
        assertEquals(Film.class, repository.entityClass());
        assertEquals("title", repository.filterProperty());
    }

    private static FilmView filmView(int id, String title) {
        FilmView view = new FilmView();
        view.setFilmId(id);
        view.setTitle(title);
        view.setCategory("Documentary");
        view.setLength((short) 86);
        view.setActors("PENELOPE GUINESS");
        return view;
    }

    private static Film film(int id, String title) {
        Film film = new Film();
        film.setFilmId(id);
        film.setTitle(title);
        return film;
    }

    private static FilmCategoryFacet categoryFacet(String key, int value) {
        FilmCategoryFacet facet = new FilmCategoryFacet();
        facet.setKey(key);
        facet.setValue(value);
        return facet;
    }

    private static FilmRatingFacet ratingFacet(String key, int value) {
        FilmRatingFacet facet = new FilmRatingFacet();
        facet.setKey(key);
        facet.setValue(value);
        return facet;
    }

    private static List<String> properties(List<Facet> facets) {
        return facets.stream().map(Facet::getProperty).toList();
    }

    private static List<String> keys(Facet facet) {
        return facet.getValues().stream().map(Facet.FacetValue::getKey).toList();
    }

    private static List<Integer> counts(Facet facet) {
        return facet.getValues().stream().map(Facet.FacetValue::getValue).toList();
    }

    private final java.util.Map<Class<?>, io.ebean.ExpressionList<?>> findByIdWhere = new java.util.HashMap<>();

    @SuppressWarnings("unchecked")
    private <E> void stubFindById(Class<E> type, Object id, E result) {
        io.ebean.ExpressionList<E> where = (io.ebean.ExpressionList<E>) findByIdWhere.get(type);
        if (where == null) {
            io.ebean.Query<E> query = org.mockito.Mockito.mock(io.ebean.Query.class, org.mockito.Answers.RETURNS_SELF);
            where = org.mockito.Mockito.mock(io.ebean.ExpressionList.class);
            when(query.where()).thenReturn(where);
            when(db.find(type)).thenReturn(query);
            findByIdWhere.put(type, where);
        }
        io.ebean.ExpressionList<E> byId = org.mockito.Mockito.mock(io.ebean.ExpressionList.class);
        when(byId.findOne()).thenReturn(result);
        when(where.idEq(id)).thenReturn(byId);
    }
}
