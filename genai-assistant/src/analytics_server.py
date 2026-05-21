import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP

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
    async with pool.acquire() as conn:
        query = """
            SELECT r.rental_id,
                   f.film_id,
                   f.title,
                   cu.email AS customer_email,
                   i.store_id,
                   r.rental_date::date AS rental_date,
                   (CURRENT_DATE - (r.rental_date::date + f.rental_duration))::int AS days_overdue
            FROM rental r
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
            JOIN customer cu ON r.customer_id = cu.customer_id
            WHERE r.return_date IS NULL
              AND CURRENT_DATE > r.rental_date::date + f.rental_duration
        """
        args: list = []
        if store_id is not None:
            args.append(store_id)
            query += f" AND i.store_id = ${len(args)}"
        args.append(limit)
        query += f" ORDER BY days_overdue DESC LIMIT ${len(args)}"
        rows = await conn.fetch(query, *args)
        return [dict(r) for r in rows]


@mcp.tool()
async def get_slow_moving_films(store_id: int | None = None, days: int = 90, limit: int = 20) -> list[dict]:
    """Films in inventory with no rentals in the last `days` days."""
    raise NotImplementedError


@mcp.tool()
async def get_revenue_summary() -> dict:
    """Global revenue totals across all stores plus a per-store breakdown."""
    raise NotImplementedError


@mcp.tool()
async def get_store_comparison() -> list[dict]:
    """Side-by-side key metrics for both stores in a single row per store."""
    raise NotImplementedError


if __name__ == "__main__":
    mcp.run(transport="stdio")
