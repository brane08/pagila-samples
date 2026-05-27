"""Tests for the 3 rental MCP tools."""
from film_server import (
    get_customer_current_rentals,
    get_recently_returned_films,
    get_rental_stats_by_category,
)

# Standard pagila customer — exists in every pagila install
KNOWN_CUSTOMER_EMAIL = "mary.smith@sakilacustomer.org"


class TestGetRentalStatsByCategory:
    async def test_returns_all_categories(self):
        results = await get_rental_stats_by_category()
        # All 16 pagila categories should appear
        assert len(results) == 16

    async def test_result_keys(self):
        row = results = (await get_rental_stats_by_category())[0]
        assert {"category", "rental_count", "total_revenue"} <= row.keys()

    async def test_rental_counts_are_positive(self):
        results = await get_rental_stats_by_category()
        assert all(r["rental_count"] > 0 for r in results)

    async def test_revenue_is_non_negative(self):
        results = await get_rental_stats_by_category()
        assert all(float(r["total_revenue"]) >= 0 for r in results)

    async def test_sorted_by_revenue_descending(self):
        results = await get_rental_stats_by_category()
        revenues = [float(r["total_revenue"]) for r in results]
        assert revenues == sorted(revenues, reverse=True)

    async def test_covers_known_category(self):
        results = await get_rental_stats_by_category()
        categories = {r["category"] for r in results}
        assert "Action" in categories
        assert "Comedy" in categories


class TestGetRecentlyReturnedFilms:
    async def test_returns_films(self):
        results = await get_recently_returned_films()
        assert len(results) > 0

    async def test_result_keys(self):
        row = (await get_recently_returned_films())[0]
        assert {"film_id", "title", "rating", "return_date", "store_id"} <= row.keys()

    async def test_all_have_return_date(self):
        results = await get_recently_returned_films()
        assert all(r["return_date"] is not None for r in results)

    async def test_sorted_most_recent_first(self):
        results = await get_recently_returned_films(limit=10)
        dates = [r["return_date"] for r in results]
        assert dates == sorted(dates, reverse=True)

    async def test_limit_respected(self):
        results = await get_recently_returned_films(limit=5)
        assert len(results) <= 5

    async def test_store_filter_restricts_results(self):
        all_results = await get_recently_returned_films(limit=50)
        store_ids = {r["store_id"] for r in all_results}
        # If multiple stores exist, filtering should reduce the result set
        if len(store_ids) > 1:
            store_id = next(iter(store_ids))
            filtered = await get_recently_returned_films(limit=50, store_id=store_id)
            assert all(r["store_id"] == store_id for r in filtered)
            assert len(filtered) <= len(all_results)

    async def test_store_filter_with_valid_store(self):
        results = await get_recently_returned_films(limit=10, store_id=1)
        assert all(r["store_id"] == 1 for r in results)


class TestGetCustomerCurrentRentals:
    async def test_known_customer_found(self):
        result = await get_customer_current_rentals(KNOWN_CUSTOMER_EMAIL)
        assert "error" not in result
        assert result["email"].lower() == KNOWN_CUSTOMER_EMAIL.lower()
        assert "name" in result
        assert "customer_id" in result

    async def test_current_rentals_key_present(self):
        result = await get_customer_current_rentals(KNOWN_CUSTOMER_EMAIL)
        assert "current_rentals" in result
        assert isinstance(result["current_rentals"], list)

    async def test_rental_entries_have_required_keys(self):
        result = await get_customer_current_rentals(KNOWN_CUSTOMER_EMAIL)
        for rental in result["current_rentals"]:
            assert {"film_id", "title", "rental_date", "store_id"} <= rental.keys()

    async def test_case_insensitive_email_lookup(self):
        lower = await get_customer_current_rentals(KNOWN_CUSTOMER_EMAIL.lower())
        upper = await get_customer_current_rentals(KNOWN_CUSTOMER_EMAIL.upper())
        assert lower["customer_id"] == upper["customer_id"]

    async def test_unknown_email_returns_error(self):
        result = await get_customer_current_rentals("nobody@nothere.invalid")
        assert "error" in result

    async def test_partial_email_no_match(self):
        # Should only match exact email, not partial
        result = await get_customer_current_rentals("mary.smith")
        assert "error" in result
