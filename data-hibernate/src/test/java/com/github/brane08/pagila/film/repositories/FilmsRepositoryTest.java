package com.github.brane08.pagila.film.repositories;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.entities.Actor;
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
import com.github.brane08.pagila.seedworks.repositories.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Path;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilmsRepositoryTest {

    @Mock
    private EntityManager em;

    private JpaQueryStubs jpa;
    private FilmsRepository repository;

    @BeforeEach
    void setUp() {
        jpa = new JpaQueryStubs(em);
        repository = new FilmsRepository(em, FilmMapper.INSTANCE);
    }

    @Test
    void listFilms_convertsPageToOffsetAndMapsRows() {
        jpa.rows(FilmView.class, List.of(filmView(1, "ACADEMY DINOSAUR")));
        jpa.count(7);

        PagedList<FilmViewInfo> result = repository.listFilms(new PageInfo(2, 10, "title"));

        assertEquals(7, result.totalCount());
        assertEquals(1, result.list().size());
        assertEquals("ACADEMY DINOSAUR", result.list().getFirst().getTitle());
        assertEquals(Integer.valueOf(1), result.list().getFirst().getFilmId());
        verify(jpa.typed(FilmView.class)).setFirstResult(10);
        verify(jpa.typed(FilmView.class)).setMaxResults(10);
        verify(jpa.cb()).asc(any(Path.class));
    }

    @Test
    void offsetOfFilms_descendingPrefixOrdersByDescending() {
        jpa.rows(FilmView.class, List.of());
        jpa.count(0);

        PagedList<FilmViewInfo> result = repository.offsetOfFilms(0, 5, "-price");

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.totalCount());
        verify(jpa.typed(FilmView.class)).setFirstResult(0);
        verify(jpa.typed(FilmView.class)).setMaxResults(5);
        verify(jpa.cb()).desc(any(Path.class));
    }

    @Test
    void offsetOfNicerFilms_mapsRowsAndCount() {
        NicerFilmView view = new NicerFilmView();
        view.setFilmId(3);
        view.setTitle("AFRICAN EGG");
        jpa.rows(NicerFilmView.class, List.of(view));
        jpa.count(42);

        PagedList<NicerFilmViewInfo> result = repository.offsetOfNicerFilms(20, 10, "title");

        assertEquals(42, result.totalCount());
        assertEquals("AFRICAN EGG", result.list().getFirst().getTitle());
        verify(jpa.typed(NicerFilmView.class)).setFirstResult(20);
        verify(jpa.typed(NicerFilmView.class)).setMaxResults(10);
    }

    @Test
    void offsetOfNicerFilms_emptyResult() {
        jpa.count(0);

        PagedList<NicerFilmViewInfo> result = repository.offsetOfNicerFilms(0, 10, "");

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.totalCount());
    }

    @Test
    void allSalesByCategory_mapsEntities() {
        SalesByFilmCategory entity = new SalesByFilmCategory();
        entity.setCategory("Action");
        entity.setTotalSales(new BigDecimal("1234.50"));
        jpa.rows(SalesByFilmCategory.class, List.of(entity));

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
        jpa.rows(FilmPriceFacet.class, List.of(price));
        jpa.rows(FilmCategoryFacet.class, List.of(categoryFacet("Action", 5), categoryFacet("Drama", 3)));
        jpa.rows(FilmRatingFacet.class, List.of(ratingFacet("PG", 8)));

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
        jpa.rows(FilmCategoryFacet.class, List.of(categoryFacet("Comedy", 2)));
        jpa.rows(FilmRatingFacet.class, List.of(ratingFacet("R", 1)));

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
    void listActors_mapsActorsOfExistingFilm() {
        Actor actor = new Actor();
        actor.setActorId(1);
        actor.setFirstName("PENELOPE");
        actor.setLastName("GUINESS");
        Film film = film(1, "ACADEMY DINOSAUR");
        film.setActors(List.of(actor));
        when(em.find(Film.class, 1)).thenReturn(film);

        List<ActorInfo> actors = repository.listActors(1);

        assertEquals(1, actors.size());
        assertEquals("PENELOPE", actors.getFirst().firstName());
        assertEquals("GUINESS", actors.getFirst().lastName());
    }

    @Test
    void listActors_missingFilmReturnsEmptyList() {
        when(em.find(Film.class, 99)).thenReturn(null);

        assertTrue(repository.listActors(99).isEmpty());
    }

    @Test
    void count_returnsCountQueryResult() {
        jpa.count(12);

        assertEquals(12, repository.count(""));
    }

    @Test
    void pageOffset_mapsRowsAndCount() {
        jpa.rows(Film.class, List.of(film(1, "A")));
        jpa.count(1);

        PagedList<Film> page = repository.pageOffset("", 0, 5, "title");

        assertEquals(1, page.list().size());
        assertEquals(1, page.totalCount());
        verify(jpa.typed(Film.class)).setFirstResult(0);
        verify(jpa.typed(Film.class)).setMaxResults(5);
    }

    @Test
    void page_appliesPageInfoOffsetAndMapper() {
        jpa.rows(Film.class, List.of(film(1, "A"), film(2, "B")));
        jpa.count(2);

        PagedList<String> page = repository.page("", new PageInfo(2, 1, "title"), Film::getTitle);

        assertEquals(List.of("A", "B"), page.list());
        assertEquals(2, page.totalCount());
        verify(jpa.typed(Film.class)).setFirstResult(1);
        verify(jpa.typed(Film.class)).setMaxResults(1);
    }

    @Test
    void pageOffset_withMapperAppliesMapper() {
        jpa.rows(Film.class, List.of(film(7, "SEVEN")));
        jpa.count(1);

        PagedList<String> page = repository.pageOffset("", 0, 10, "title", Film::getTitle);

        assertEquals(List.of("SEVEN"), page.list());
        assertEquals(1, page.totalCount());
    }

    @Test
    void findById_returnsFilmWhenPresent() {
        Film film = film(3, "CHAMBER ITALIAN");
        when(em.find(Film.class, 3)).thenReturn(film);

        Optional<Film> found = repository.findById(3);

        assertTrue(found.isPresent());
        assertEquals("CHAMBER ITALIAN", found.get().getTitle());
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        when(em.find(Film.class, 4)).thenReturn(null);

        assertFalse(repository.findById(4).isPresent());
    }

    @Test
    void findById_withMapperAppliesMapperWhenPresent() {
        when(em.find(Film.class, 3)).thenReturn(film(3, "CHAMBER ITALIAN"));
        when(em.find(Film.class, 4)).thenReturn(null);

        assertEquals(Optional.of("CHAMBER ITALIAN"), repository.findById(3, Film::getTitle));
        assertEquals(Optional.empty(), repository.findById(4, Film::getTitle));
    }

    @Test
    void all_returnsRowsCappedAtMaxPageSize() {
        jpa.rows(Film.class, List.of(film(1, "A")));

        assertEquals(1, repository.all().size());
        verify(jpa.typed(Film.class)).setMaxResults(Repository.MAX_PAGE_SIZE);
    }

    @Test
    void all_withMapperAppliesMapper() {
        jpa.rows(Film.class, List.of(film(1, "A"), film(2, "B")));

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
}
