import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP

from analytics_queries import (
    overdue_rentals as _overdue_rentals,
    revenue_summary as _revenue_summary,
    slow_moving_films as _slow_moving_films,
    store_comparison as _store_comparison,
)

load_dotenv()

mcp = FastMCP("PagilaAnalytics")

_pool: asyncpg.Pool | None = None


async def _get_pool() -> asyncpg.Pool:
    global _pool
    if _pool is None:
        _pool = await asyncpg.create_pool(
            host=os.getenv("DB_HOST", "localhost"),
            port=int(os.getenv("DB_PORT", 5432)),
            database=os.getenv("DB_NAME", "sakila"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", "password"),
            min_size=1,
            max_size=5,
        )
    return _pool


@mcp.tool()
async def get_overdue_rentals(store_id: int | None = None, limit: int = 20) -> list[dict]:
    """
    Rentals not yet returned that are past their rental_duration deadline.
    days_overdue is how many days beyond the due date the item is.
    Optionally filter by store_id. Ordered most overdue first.
    """
    pool = await _get_pool()
    rows = await _overdue_rentals(pool, limit=limit)
    if store_id is not None:
        rows = [r for r in rows if r["store_id"] == store_id]
    return rows


@mcp.tool()
async def get_slow_moving_films(store_id: int | None = None, days: int = 90, limit: int = 20) -> list[dict]:
    """
    Films present in inventory that have had zero rentals in the last `days` days,
    or have never been rented. Returns one row per (film, store) pair.
    days_since_rented is None for films that have never been rented.
    Ordered by days_since_rented descending, never-rented films first.
    Optionally filter by store_id.
    """
    pool = await _get_pool()
    rows = await _slow_moving_films(pool, days=days, limit=limit)
    if store_id is not None:
        rows = [r for r in rows if r["store_id"] == store_id]
    return rows


@mcp.tool()
async def get_revenue_summary() -> dict:
    """
    Global revenue totals across all stores: total revenue, total rentals, average per rental,
    busiest month (YYYY-MM), and a per-store revenue breakdown.
    """
    pool = await _get_pool()
    return await _revenue_summary(pool)


@mcp.tool()
async def get_store_comparison() -> list[dict]:
    """
    Side-by-side key metrics for both stores: manager, city, total revenue, rental count,
    unique customers, average rental rate across stocked inventory, and outstanding rentals.
    One row per store, ordered by store_id.
    """
    pool = await _get_pool()
    return await _store_comparison(pool)


if __name__ == "__main__":
    mcp.run(transport="stdio")
