"""Tests for the 4 analytics MCP tools. Requires PostgreSQL on localhost:5432."""
from analytics_server import (
    get_overdue_rentals,
    get_revenue_summary,
    get_slow_moving_films,
    get_store_comparison,
)


class TestOverdueRentals:
    async def test_returns_list(self):
        results = await get_overdue_rentals()
        assert isinstance(results, list)

    async def test_result_keys(self):
        results = await get_overdue_rentals()
        if results:
            row = results[0]
            assert {"rental_id", "film_id", "title", "customer_email",
                    "store_id", "rental_date", "days_overdue"} <= row.keys()

    async def test_days_overdue_positive(self):
        results = await get_overdue_rentals()
        assert len(results) > 0
        assert all(r["days_overdue"] >= 1 for r in results)

    async def test_store_filter_returns_subset(self):
        all_results = await get_overdue_rentals()
        s1_results = await get_overdue_rentals(store_id=1)
        assert len(s1_results) <= len(all_results)
        assert all(r["store_id"] == 1 for r in s1_results)

    async def test_limit_respected(self):
        results = await get_overdue_rentals(limit=3)
        assert len(results) <= 3

    async def test_sorted_most_overdue_first(self):
        results = await get_overdue_rentals(limit=10)
        days = [r["days_overdue"] for r in results]
        assert days == sorted(days, reverse=True)


class TestSlowMovingFilms:
    async def test_returns_list(self):
        results = await get_slow_moving_films()
        assert isinstance(results, list)

    async def test_result_keys(self):
        results = await get_slow_moving_films()
        if results:
            row = results[0]
            assert {"film_id", "title", "rating", "store_id",
                    "copies_in_stock", "last_rented", "days_since_rented"} <= row.keys()

    async def test_days_since_rented_non_negative(self):
        results = await get_slow_moving_films()
        for row in results:
            if row["days_since_rented"] is not None:
                assert row["days_since_rented"] >= 0

    async def test_copies_in_stock_positive(self):
        results = await get_slow_moving_films()
        if results:
            assert all(r["copies_in_stock"] > 0 for r in results)

    async def test_store_filter_works(self):
        s1 = await get_slow_moving_films(store_id=1)
        assert all(r["store_id"] == 1 for r in s1)

    async def test_limit_respected(self):
        results = await get_slow_moving_films(limit=5)
        assert len(results) <= 5

    async def test_never_rented_films_have_null_last_rented(self):
        results = await get_slow_moving_films(days=1)
        for row in results:
            if row["last_rented"] is None:
                assert row["days_since_rented"] is None


class TestRevenueSummary:
    async def test_returns_dict(self):
        result = await get_revenue_summary()
        assert isinstance(result, dict)

    async def test_top_level_keys(self):
        result = await get_revenue_summary()
        assert {"total_revenue", "total_rentals", "avg_per_rental",
                "busiest_month", "busiest_month_revenue", "by_store"} <= result.keys()

    async def test_total_revenue_positive(self):
        result = await get_revenue_summary()
        assert result["total_revenue"] > 0

    async def test_total_rentals_positive(self):
        result = await get_revenue_summary()
        assert result["total_rentals"] > 0

    async def test_avg_per_rental_positive(self):
        result = await get_revenue_summary()
        assert result["avg_per_rental"] > 0

    async def test_by_store_has_two_entries(self):
        result = await get_revenue_summary()
        assert len(result["by_store"]) == 2

    async def test_by_store_keys(self):
        result = await get_revenue_summary()
        for row in result["by_store"]:
            assert {"store_id", "revenue", "rental_count", "avg_per_rental"} <= row.keys()

    async def test_store_revenues_sum_to_total(self):
        result = await get_revenue_summary()
        store_total = sum(s["revenue"] for s in result["by_store"])
        assert abs(store_total - result["total_revenue"]) < 0.01

    async def test_busiest_month_format(self):
        result = await get_revenue_summary()
        # Should be "YYYY-MM" format
        month = result["busiest_month"]
        assert len(month) == 7
        assert month[4] == "-"


class TestStoreComparison:
    async def test_returns_two_rows(self):
        results = await get_store_comparison()
        assert len(results) == 2

    async def test_result_keys(self):
        row = (await get_store_comparison())[0]
        assert {"store_id", "manager", "city", "total_revenue", "rental_count",
                "unique_customers", "avg_rental_rate", "outstanding_rentals"} <= row.keys()

    async def test_store_ids_are_1_and_2(self):
        results = await get_store_comparison()
        ids = {r["store_id"] for r in results}
        assert ids == {1, 2}

    async def test_total_revenue_positive(self):
        results = await get_store_comparison()
        assert all(r["total_revenue"] > 0 for r in results)

    async def test_rental_count_positive(self):
        results = await get_store_comparison()
        assert all(r["rental_count"] > 0 for r in results)

    async def test_unique_customers_positive(self):
        results = await get_store_comparison()
        assert all(r["unique_customers"] > 0 for r in results)

    async def test_outstanding_rentals_non_negative(self):
        results = await get_store_comparison()
        assert all(r["outstanding_rentals"] >= 0 for r in results)

    async def test_sorted_by_store_id(self):
        results = await get_store_comparison()
        ids = [r["store_id"] for r in results]
        assert ids == sorted(ids)
