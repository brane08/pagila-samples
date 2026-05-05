"""Tests for the 3 actor MCP tools."""
from film_server import get_actor_filmography, list_top_actors, search_actors

ACTOR_ID_PENELOPE = 1      # PENELOPE GUINESS — standard pagila row
ACTOR_ID_NONEXISTENT = 999_999


class TestSearchActors:
    async def test_finds_by_first_name(self):
        results = await search_actors("PENELOPE")
        assert len(results) >= 1
        names = [r["full_name"] for r in results]
        assert any("PENELOPE" in n for n in names)

    async def test_finds_by_last_name(self):
        results = await search_actors("GUINESS")
        assert len(results) >= 1
        assert any("GUINESS" in r["full_name"] for r in results)

    async def test_finds_by_full_name(self):
        results = await search_actors("PENELOPE GUINESS")
        assert len(results) >= 1

    async def test_result_keys(self):
        results = await search_actors("PENELOPE")
        row = results[0]
        assert {"actor_id", "full_name", "film_count"} <= row.keys()

    async def test_film_count_is_positive(self):
        results = await search_actors("PENELOPE GUINESS")
        assert results[0]["film_count"] > 0

    async def test_no_match_returns_empty(self):
        results = await search_actors("ZZZZZZ_NO_SUCH_ACTOR")
        assert results == []

    async def test_limit_respected(self):
        results = await search_actors("a", limit=2)
        assert len(results) <= 2

    async def test_case_insensitive(self):
        lower = await search_actors("penelope")
        upper = await search_actors("PENELOPE")
        assert {r["actor_id"] for r in lower} == {r["actor_id"] for r in upper}


class TestGetActorFilmography:
    async def test_known_actor_returns_record(self):
        result = await get_actor_filmography(ACTOR_ID_PENELOPE)
        assert "error" not in result
        assert result["actor_id"] == ACTOR_ID_PENELOPE
        assert "PENELOPE" in result["name"]

    async def test_films_list_is_populated(self):
        result = await get_actor_filmography(ACTOR_ID_PENELOPE)
        assert isinstance(result["films"], list)
        assert len(result["films"]) > 0

    async def test_film_entries_have_required_keys(self):
        result = await get_actor_filmography(ACTOR_ID_PENELOPE)
        film = result["films"][0]
        assert {"film_id", "title", "rating", "rental_rate", "category"} <= film.keys()

    async def test_films_sorted_by_title(self):
        result = await get_actor_filmography(ACTOR_ID_PENELOPE)
        titles = [f["title"] for f in result["films"]]
        assert titles == sorted(titles)

    async def test_nonexistent_actor_returns_error(self):
        result = await get_actor_filmography(ACTOR_ID_NONEXISTENT)
        assert "error" in result


class TestListTopActors:
    async def test_returns_actors(self):
        results = await list_top_actors()
        assert len(results) > 0

    async def test_result_keys(self):
        row = (await list_top_actors())[0]
        assert {"actor_id", "full_name", "film_count"} <= row.keys()

    async def test_sorted_descending_by_film_count(self):
        results = await list_top_actors(limit=5)
        counts = [r["film_count"] for r in results]
        assert counts == sorted(counts, reverse=True)

    async def test_limit_respected(self):
        results = await list_top_actors(limit=5)
        assert len(results) <= 5

    async def test_all_actors_have_films(self):
        results = await list_top_actors(limit=10)
        assert all(r["film_count"] > 0 for r in results)
