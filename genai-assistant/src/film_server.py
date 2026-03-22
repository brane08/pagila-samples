import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP

from rag import semantic_search

load_dotenv()

mcp = FastMCP("PagilaFilms")

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


# ── Tools ──────────────────────────────────────────────────────────────────────

@mcp.tool()
async def search_films(title: str, limit: int = 10) -> list[dict]:
    """Search films by partial title match. Returns title, rating, length, rental_rate."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT film_id, title, description, release_year,
                   rating::text, length, rental_rate, rental_duration
            FROM film
            WHERE title ILIKE $1
            ORDER BY title
            LIMIT $2
            """,
            f"%{title}%", limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def get_film_details(film_id: int) -> dict:
    """Get full details for a film including language, categories, and actors."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        film = await conn.fetchrow(
            """
            SELECT f.film_id, f.title, f.description, f.release_year,
                   f.rating::text, f.length, f.rental_rate, f.rental_duration,
                   f.replacement_cost, l.name AS language
            FROM film f
            JOIN language l ON f.language_id = l.language_id
            WHERE f.film_id = $1
            """,
            film_id,
        )
        if not film:
            return {"error": f"Film {film_id} not found"}

        categories = await conn.fetch(
            """
            SELECT c.name FROM category c
            JOIN film_category fc ON c.category_id = fc.category_id
            WHERE fc.film_id = $1
            """,
            film_id,
        )
        actors = await conn.fetch(
            """
            SELECT a.first_name || ' ' || a.last_name AS name
            FROM actor a
            JOIN film_actor fa ON a.actor_id = fa.actor_id
            WHERE fa.film_id = $1
            ORDER BY a.last_name
            """,
            film_id,
        )
        return {
            **dict(film),
            "categories": [r["name"] for r in categories],
            "actors": [r["name"] for r in actors],
        }


@mcp.tool()
async def list_films_by_category(category: str, limit: int = 10) -> list[dict]:
    """List films belonging to a specific category (e.g. Action, Comedy, Horror)."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT f.film_id, f.title, f.rating::text, f.length, f.rental_rate
            FROM film f
            JOIN film_category fc ON f.film_id = fc.film_id
            JOIN category c ON fc.category_id = c.category_id
            WHERE c.name ILIKE $1
            ORDER BY f.title
            LIMIT $2
            """,
            category, limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def list_films_by_actor(actor_name: str, limit: int = 10) -> list[dict]:
    """Find all films featuring an actor by partial name match."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT f.film_id, f.title, f.rating::text, f.length,
                   a.first_name || ' ' || a.last_name AS actor
            FROM film f
            JOIN film_actor fa ON f.film_id = fa.film_id
            JOIN actor a ON fa.actor_id = a.actor_id
            WHERE (a.first_name || ' ' || a.last_name) ILIKE $1
            ORDER BY f.title
            LIMIT $2
            """,
            f"%{actor_name}%", limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def get_top_rented_films(limit: int = 10) -> list[dict]:
    """Return the most frequently rented films across all stores."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT f.film_id, f.title, f.rating::text,
                   COUNT(r.rental_id) AS rental_count
            FROM film f
            JOIN inventory i ON f.film_id = i.film_id
            JOIN rental r ON i.inventory_id = r.inventory_id
            GROUP BY f.film_id, f.title, f.rating
            ORDER BY rental_count DESC
            LIMIT $1
            """,
            limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def list_categories() -> list[str]:
    """List all available film categories in the database."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT name FROM category ORDER BY name")
        return [r["name"] for r in rows]


@mcp.tool()
async def get_film_availability(film_id: int) -> dict:
    """Check how many copies of a film are available across all stores."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT s.store_id,
                   COUNT(i.inventory_id) AS total_copies,
                   SUM(CASE WHEN r.return_date IS NOT NULL OR r.rental_id IS NULL
                            THEN 1 ELSE 0 END) AS available_copies
            FROM inventory i
            JOIN store s ON i.store_id = s.store_id
            LEFT JOIN rental r ON i.inventory_id = r.inventory_id
                AND r.return_date IS NULL
            WHERE i.film_id = $1
            GROUP BY s.store_id
            ORDER BY s.store_id
            """,
            film_id,
        )
        film = await conn.fetchrow("SELECT title FROM film WHERE film_id = $1", film_id)
        return {
            "film_id": film_id,
            "title": film["title"] if film else "Unknown",
            "stores": [dict(r) for r in rows],
        }


@mcp.tool()
async def semantic_film_search(query: str, limit: int = 5) -> list[dict]:
    """
    Semantic similarity search over film descriptions.
    Use this when the user describes a plot, mood, theme, or vibe
    rather than an exact title — e.g. 'a film about redemption in space'
    or 'something funny with a road trip'.
    """
    return await semantic_search(query, k=limit)


if __name__ == "__main__":
    mcp.run(transport="stdio")
