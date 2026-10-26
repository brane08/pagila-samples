import pytest
from unittest.mock import AsyncMock, MagicMock


def _make_pool(fetchrow_return=None, fetch_return=None):
    mock_conn = AsyncMock()
    if fetchrow_return is not None:
        mock_conn.fetchrow.return_value = fetchrow_return
    if fetch_return is not None:
        mock_conn.fetch.return_value = fetch_return
    mock_pool = MagicMock()
    mock_pool.acquire.return_value.__aenter__ = AsyncMock(return_value=mock_conn)
    mock_pool.acquire.return_value.__aexit__ = AsyncMock(return_value=None)
    return mock_pool


@pytest.mark.asyncio
async def test_revenue_summary_returns_expected_keys():
    from analytics_queries import revenue_summary
    pool = _make_pool(
        fetchrow_return={
            "total_rentals": 100,
            "total_revenue": 1000.00,
            "avg_per_rental": 10.00,
            "busiest_month": "2005-08",
            "busiest_month_revenue": 200.00,
        },
        fetch_return=[{
            "store_id": 1, "revenue": 600.00,
            "rental_count": 60, "avg_per_rental": 10.0,
        }],
    )
    result = await revenue_summary(pool)
    assert result["total_rentals"] == 100
    assert result["total_revenue"] == 1000.00
    assert result["busiest_month"] == "2005-08"
    assert len(result["by_store"]) == 1


@pytest.mark.asyncio
async def test_revenue_summary_empty_returns_zeros():
    from analytics_queries import revenue_summary
    pool = _make_pool(fetchrow_return={
        "total_rentals": 0, "total_revenue": 0,
        "avg_per_rental": 0, "busiest_month": None,
        "busiest_month_revenue": None,
    })
    result = await revenue_summary(pool)
    assert result["total_revenue"] == 0.0
    assert result["busiest_month"] is None
    assert result["by_store"] == []


@pytest.mark.asyncio
async def test_revenue_summary_converts_decimal_to_float():
    from decimal import Decimal
    from analytics_queries import revenue_summary
    pool = _make_pool(
        fetchrow_return={
            "total_rentals": 50,
            "total_revenue": Decimal("500.00"),
            "avg_per_rental": Decimal("10.00"),
            "busiest_month": "2005-07",
            "busiest_month_revenue": Decimal("150.00"),
        },
        fetch_return=[],
    )
    result = await revenue_summary(pool)
    assert isinstance(result["total_revenue"], float)
    assert isinstance(result["avg_per_rental"], float)
    assert isinstance(result["busiest_month_revenue"], float)


@pytest.mark.asyncio
async def test_store_comparison_returns_floats():
    from analytics_queries import store_comparison
    pool = _make_pool(fetch_return=[{
        "store_id": 1, "manager": "Mike Hillyer", "city": "Lethbridge",
        "total_revenue": 30000.0, "rental_count": 7000,
        "unique_customers": 300, "avg_rental_rate": 2.99,
        "outstanding_rentals": 10,
    }])
    result = await store_comparison(pool)
    assert len(result) == 1
    assert isinstance(result[0]["total_revenue"], float)


@pytest.mark.asyncio
async def test_rental_stats_by_category_returns_floats():
    from analytics_queries import rental_stats_by_category
    pool = _make_pool(fetch_return=[
        {"category": "Action", "rental_count": 500, "total_revenue": 2500.0},
    ])
    result = await rental_stats_by_category(pool)
    assert result[0]["category"] == "Action"
    assert isinstance(result[0]["total_revenue"], float)


@pytest.mark.asyncio
async def test_overdue_rentals_returns_list():
    from analytics_queries import overdue_rentals
    pool = _make_pool(fetch_return=[{
        "rental_id": 1, "film_id": 10, "title": "ACADEMY DINOSAUR",
        "customer_email": "mary@example.org",
        "store_id": 1, "rental_date": "2005-05-24", "days_overdue": 5,
    }])
    result = await overdue_rentals(pool)
    assert result[0]["days_overdue"] == 5


@pytest.mark.asyncio
async def test_slow_moving_films_returns_list():
    from analytics_queries import slow_moving_films
    pool = _make_pool(fetch_return=[{
        "film_id": 5, "title": "AFRICAN EGG", "rating": "G",
        "store_id": 1, "copies_in_stock": 2,
        "last_rented": None, "days_since_rented": None,
    }])
    result = await slow_moving_films(pool)
    assert result[0]["title"] == "AFRICAN EGG"
