"""Tests for the original 8 film MCP tools."""
import pytest
from film_server import (
    get_film_availability,
    get_film_details,
    get_top_rented_films,
    list_categories,
    list_films_by_actor,
    list_films_by_category,
    search_films,
)

# Pagila dataset constants — these rows exist in every standard pagila install
FILM_ID_ACADEMY_DINOSAUR = 1
FILM_ID_NONEXISTENT = 999_999


class TestSearchFilms:
    async def test_partial_title_match(self):
        results = await search_films("ACADEMY")
        assert len(results) >= 1
        titles = [r["title"] for r in results]
        assert any("ACADEMY" in t for t in titles)

    async def test_result_keys(self):
        results = await search_films("ACADEMY")
        row = results[0]
        assert {"film_id", "title", "description", "rating", "length", "rental_rate"} <= row.keys()

    async def test_case_insensitive(self):
        lower = await search_films("academy")
        upper = await search_films("ACADEMY")
        assert {r["film_id"] for r in lower} == {r["film_id"] for r in upper}

    async def test_no_match_returns_empty(self):
        results = await search_films("ZZZZZZ_NO_SUCH_FILM")
        assert results == []

    async def test_limit_respected(self):
        results = await search_films("a", limit=3)
        assert len(results) <= 3


class TestGetFilmDetails:
    async def test_known_film_returns_full_record(self):
        result = await get_film_details(FILM_ID_ACADEMY_DINOSAUR)
        assert "error" not in result
        assert result["title"] == "ACADEMY DINOSAUR"
        assert result["film_id"] == FILM_ID_ACADEMY_DINOSAUR

    async def test_includes_actors_and_categories(self):
        result = await get_film_details(FILM_ID_ACADEMY_DINOSAUR)
        assert isinstance(result["actors"], list)
        assert len(result["actors"]) > 0
        assert isinstance(result["categories"], list)
        assert len(result["categories"]) > 0

    async def test_includes_language(self):
        result = await get_film_details(FILM_ID_ACADEMY_DINOSAUR)
        assert result.get("language") == "English"

    async def test_nonexistent_film_returns_error(self):
        result = await get_film_details(FILM_ID_NONEXISTENT)
        assert "error" in result


class TestListFilmsByCategory:
    async def test_returns_films_for_valid_category(self):
        results = await list_films_by_category("Action")
        assert len(results) > 0

    async def test_result_keys(self):
        results = await list_films_by_category("Action")
        row = results[0]
        assert {"film_id", "title", "rating", "length", "rental_rate"} <= row.keys()

    async def test_case_insensitive(self):
        lower = await list_films_by_category("action")
        upper = await list_films_by_category("Action")
        assert {r["film_id"] for r in lower} == {r["film_id"] for r in upper}

    async def test_unknown_category_returns_empty(self):
        results = await list_films_by_category("NotARealCategory")
        assert results == []

    async def test_limit_respected(self):
        results = await list_films_by_category("Action", limit=5)
        assert len(results) <= 5


class TestListFilmsByActor:
    async def test_returns_films_for_known_actor(self):
        results = await list_films_by_actor("PENELOPE")
        assert len(results) > 0

    async def test_result_keys(self):
        results = await list_films_by_actor("PENELOPE")
        row = results[0]
        assert {"film_id", "title", "rating", "actor"} <= row.keys()

    async def test_actor_name_in_results(self):
        results = await list_films_by_actor("PENELOPE GUINESS")
        assert all("PENELOPE" in r["actor"] for r in results)

    async def test_unknown_actor_returns_empty(self):
        results = await list_films_by_actor("ZZZZZZ_NO_SUCH_ACTOR")
        assert results == []


class TestGetTopRentedFilms:
    async def test_returns_films(self):
        results = await get_top_rented_films()
        assert len(results) > 0

    async def test_result_keys(self):
        row = (await get_top_rented_films())[0]
        assert {"film_id", "title", "rental_count"} <= row.keys()

    async def test_sorted_descending_by_rental_count(self):
        results = await get_top_rented_films(limit=5)
        counts = [r["rental_count"] for r in results]
        assert counts == sorted(counts, reverse=True)

    async def test_limit_respected(self):
        results = await get_top_rented_films(limit=3)
        assert len(results) <= 3


class TestListCategories:
    async def test_returns_all_categories(self):
        categories = await list_categories()
        # Standard pagila has 16 categories
        assert len(categories) == 16

    async def test_returns_strings(self):
        categories = await list_categories()
        assert all(isinstance(c, str) for c in categories)

    async def test_contains_expected_categories(self):
        categories = await list_categories()
        assert "Action" in categories
        assert "Comedy" in categories
        assert "Horror" in categories


class TestGetFilmAvailability:
    async def test_known_film_returns_stores(self):
        result = await get_film_availability(FILM_ID_ACADEMY_DINOSAUR)
        assert "error" not in result
        assert result["film_id"] == FILM_ID_ACADEMY_DINOSAUR
        assert "title" in result
        assert isinstance(result["stores"], list)

    async def test_stores_have_copy_counts(self):
        result = await get_film_availability(FILM_ID_ACADEMY_DINOSAUR)
        for store in result["stores"]:
            assert "store_id" in store
            assert "total_copies" in store
            assert "available_copies" in store
            assert store["total_copies"] >= store["available_copies"]

    async def test_nonexistent_film_returns_unknown_title(self):
        result = await get_film_availability(FILM_ID_NONEXISTENT)
        assert result["title"] == "Unknown"
        assert result["stores"] == []
