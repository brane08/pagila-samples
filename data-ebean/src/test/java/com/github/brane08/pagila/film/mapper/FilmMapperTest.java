package com.github.brane08.pagila.film.mapper;

import com.github.brane08.pagila.film.beans.CategoryInfo;
import com.github.brane08.pagila.film.beans.FilmInfo;
import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.beans.NicerFilmViewInfo;
import com.github.brane08.pagila.film.beans.SalesByFilmCategoryInfo;
import com.github.brane08.pagila.film.entities.Category;
import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.film.entities.FilmRating;
import com.github.brane08.pagila.film.entities.FilmView;
import com.github.brane08.pagila.film.entities.Language;
import com.github.brane08.pagila.film.entities.NicerFilmView;
import com.github.brane08.pagila.film.entities.SalesByFilmCategory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmMapperTest {

    private final FilmMapper mapper = Mappers.getMapper(FilmMapper.class);

    private static Language language(String name) {
        Language language = new Language();
        language.setName(name);
        return language;
    }

    private static Category category(int id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        return category;
    }

    private static Film film() {
        Film film = new Film();
        film.setFilmId(1);
        film.setTitle("ACADEMY DINOSAUR");
        film.setDescription("A Epic Drama of a Feminist And a Mad Scientist");
        film.setReleaseYear(Year.of(2006));
        film.setLanguage(language("English"));
        film.setOriginalLanguage(null);
        film.setRentalDuration(6);
        film.setRentalRate(new BigDecimal("0.99"));
        film.setLength(86);
        film.setReplacementCost(new BigDecimal("20.99"));
        film.setRating(FilmRating.PG13);
        film.setLastUpdate(Instant.parse("2006-02-15T05:03:42Z"));
        film.setSpecialFeatures(List.of("Deleted Scenes", "Behind the Scenes", "Deleted Scenes"));
        film.setCategories(List.of(category(6, "Documentary"), category(1, "Action")));
        return film;
    }

    private static FilmView filmView(String category, BigDecimal price, FilmRating rating) {
        FilmView view = new FilmView();
        view.setFilmId(1);
        view.setTitle("ACADEMY DINOSAUR");
        view.setDescription("desc");
        view.setCategory(category);
        view.setPrice(price);
        view.setLength((short) 86);
        view.setRating(rating);
        view.setActors("PENELOPE GUINESS, NICK WAHLBERG");
        return view;
    }

    @Test
    void filmToInfoMapsScalarFields() {
        FilmInfo info = mapper.filmToInfo(film());

        assertEquals(Integer.valueOf(1), info.getFilmId());
        assertEquals("ACADEMY DINOSAUR", info.getTitle());
        assertEquals(Year.of(2006), info.getReleaseYear());
        assertEquals("English", info.getLanguage());
        assertEquals(Integer.valueOf(6), info.getRentalDuration());
        assertEquals(0.99, info.getRentalRate(), 0.0001);
        assertEquals(Integer.valueOf(86), info.getLength());
        assertEquals(20.99, info.getReplacementCost(), 0.0001);
                assertEquals("PG-13", info.getRating());
        assertEquals(Instant.parse("2006-02-15T05:03:42Z"), info.getLastUpdate());
    }

    @Test
    void filmToInfoConvertsSpecialFeaturesToDistinctSetAndCategoriesToNames() {
        FilmInfo info = mapper.filmToInfo(film());

        assertEquals(Set.of("Deleted Scenes", "Behind the Scenes"), info.getSpecialFeatures());
        assertEquals(List.of("Documentary", "Action"), info.getCategories());
    }

    @Test
    void filmToInfoUsesEmptyStringForMissingLanguagesAndEmptyCollections() {
        Film film = film();
        film.setLanguage(null);
        film.setOriginalLanguage(null);
        film.setRating(null);
        film.setRentalRate(null);
        film.setReplacementCost(null);
        film.setSpecialFeatures(null);
        film.setCategories(null);

        FilmInfo info = mapper.filmToInfo(film);

        assertEquals("", info.getLanguage());
        assertEquals("", info.getOriginalLanguage());
        assertNull(info.getRating());
        assertNull(info.getRentalRate());
        assertNull(info.getReplacementCost());
        assertNull(info.getSpecialFeatures());
        assertTrue(info.getCategories().isEmpty());
    }

    @Test
    void filmToInfoMapsOriginalLanguageWhenPresent() {
        Film film = film();
        film.setOriginalLanguage(language("Japanese"));

        assertEquals("Japanese", mapper.filmToInfo(film).getOriginalLanguage());
    }

    @Test
    void filmToInfoReturnsNullForNull() {
        assertNull(mapper.filmToInfo(null));
    }

    @Test
    void filmsToInfosMapsListAndHandlesNull() {
        List<FilmInfo> infos = mapper.filmsToInfos(List.of(film()));

        assertEquals(1, infos.size());
        assertEquals("ACADEMY DINOSAUR", infos.get(0).getTitle());
        assertTrue(mapper.filmsToInfos(null).isEmpty());
    }

    @Test
    void filmViewToInfoMapsFieldsAndConvertsPriceToFloat() {
        FilmViewInfo info = mapper.filmViewToInfo(filmView("Action", new BigDecimal("2.99"), FilmRating.G));

        assertEquals(Integer.valueOf(1), info.getFilmId());
        assertEquals("Action", info.getCategory());
        assertEquals(2.99f, info.getPrice().floatValue(), 0.001f);
        assertEquals(Short.valueOf((short) 86), info.getLength());
        assertEquals("G", info.getRating());
        assertEquals("PENELOPE GUINESS, NICK WAHLBERG", info.getActors());
    }

    @Test
    void filmViewToInfoLeavesNullPriceAndRatingNull() {
        FilmViewInfo info = mapper.filmViewToInfo(filmView("Action", null, null));

        assertNull(info.getPrice());
        assertNull(info.getRating());
    }

    @Test
    void filmViewToInfoReturnsNullForNull() {
        assertNull(mapper.filmViewToInfo(null));
    }

    @Test
    void filmViewsToInfoMapsListAndHandlesNull() {
        List<FilmViewInfo> infos = mapper.filmViewsToInfo(List.of(filmView("Drama", BigDecimal.ONE, FilmRating.R)));

        assertEquals(1, infos.size());
        assertEquals("Drama", infos.get(0).getCategory());
        assertTrue(mapper.filmViewsToInfo(null).isEmpty());
    }

    @Test
    void nicerFilmViewToInfoMapsFields() {
        NicerFilmView view = new NicerFilmView();
        view.setFilmId(2);
        view.setTitle("ACE GOLDFINGER");
        view.setDescription("desc");
        view.setCategory("Horror");
        view.setPrice(new BigDecimal("4.99"));
        view.setLength((short) 48);
        view.setRating(FilmRating.NC17);
        view.setActors("BOB FAWCETT");

        NicerFilmViewInfo info = mapper.nicerFilmViewToInfo(view);

        assertEquals(Integer.valueOf(2), info.getFilmId());
        assertEquals("ACE GOLDFINGER", info.getTitle());
        assertEquals("Horror", info.getCategory());
        assertEquals(4.99f, info.getPrice().floatValue(), 0.001f);
        assertEquals("NC-17", info.getRating());
        assertEquals("BOB FAWCETT", info.getActors());
    }

    @Test
    void nicerFilmViewToInfoReturnsNullForNull() {
        assertNull(mapper.nicerFilmViewToInfo(null));
    }

    @Test
    void nicerFilmViewsToInfoMapsListAndHandlesNull() {
        NicerFilmView view = new NicerFilmView();
        view.setFilmId(2);

        assertEquals(1, mapper.nicerFilmViewsToInfo(List.of(view)).size());
        assertTrue(mapper.nicerFilmViewsToInfo(null).isEmpty());
    }

    @Test
    void salesByCategoryToInfoMapsCategoryAndTotal() {
        SalesByFilmCategory entity = new SalesByFilmCategory();
        entity.setCategory("Sports");
        entity.setTotalSales(new BigDecimal("1234.50"));

        SalesByFilmCategoryInfo info = mapper.salesByCategoryToInfo(entity);

        assertEquals("Sports", info.getCategory());
        assertEquals(new BigDecimal("1234.50"), info.getTotalSales());
    }

    @Test
    void salesByCategoryToInfoReturnsNullForNull() {
        assertNull(mapper.salesByCategoryToInfo(null));
    }

    @Test
    void salesByCategoryToInfosMapsListAndHandlesNull() {
        SalesByFilmCategory entity = new SalesByFilmCategory();
        entity.setCategory("Sports");

        assertEquals(1, mapper.salesByCategoryToInfos(List.of(entity)).size());
        assertTrue(mapper.salesByCategoryToInfos(null).isEmpty());
    }

    @Test
    void categoryToInfoMapsIdAndName() {
        CategoryInfo info = mapper.categoryToInfo(category(6, "Documentary"));

        assertEquals(Long.valueOf(6), info.categoryId());
        assertEquals("Documentary", info.name());
    }

    @Test
    void categoryToInfoReturnsNullForNull() {
        assertNull(mapper.categoryToInfo(null));
    }

    @Test
    void yearToIntReturnsYearValue() {
        assertEquals(2006, mapper.yearToInt(Year.of(2006)));
    }

    @Test
    void categoryToStringReturnsName() {
        assertEquals("Action", mapper.categoryToString(category(1, "Action")));
    }

    @Test
    void languageToStringReturnsNameOrEmptyWhenNull() {
        assertEquals("French", mapper.languageToString(language("French")));
        assertEquals("", mapper.languageToString(null));
    }
}
