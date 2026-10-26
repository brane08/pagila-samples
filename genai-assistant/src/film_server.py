import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP

from analytics_queries import rental_stats_by_category as _rental_stats_by_category
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


# ── Actor tools ────────────────────────────────────────────────────────────────

@mcp.tool()
async def search_actors(name: str, limit: int = 10) -> list[dict]:
    """Search actors by partial first or last name. Returns actor_id, full name, and film count."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT a.actor_id,
                   a.first_name || ' ' || a.last_name AS full_name,
                   COUNT(fa.film_id) AS film_count
            FROM actor a
            LEFT JOIN film_actor fa ON a.actor_id = fa.actor_id
            WHERE (a.first_name || ' ' || a.last_name) ILIKE $1
               OR a.first_name ILIKE $1
               OR a.last_name ILIKE $1
            GROUP BY a.actor_id, a.first_name, a.last_name
            ORDER BY a.last_name, a.first_name
            LIMIT $2
            """,
            f"%{name}%", limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def get_actor_filmography(actor_id: int) -> dict:
    """
    Get a full filmography for an actor — all their films with title, category,
    rating, year, and rental rate. Complements get_film_details for the actor angle.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        actor = await conn.fetchrow(
            "SELECT actor_id, first_name || ' ' || last_name AS full_name FROM actor WHERE actor_id = $1",
            actor_id,
        )
        if not actor:
            return {"error": f"Actor {actor_id} not found"}

        rows = await conn.fetch(
            """
            SELECT f.film_id, f.title, f.release_year, f.rating::text,
                   f.rental_rate, c.name AS category
            FROM film f
            JOIN film_actor fa ON f.film_id = fa.film_id
            JOIN film_category fc ON f.film_id = fc.film_id
            JOIN category c ON fc.category_id = c.category_id
            WHERE fa.actor_id = $1
            ORDER BY f.title
            """,
            actor_id,
        )
        return {
            "actor_id": actor["actor_id"],
            "name": actor["full_name"],
            "films": [dict(r) for r in rows],
        }


@mcp.tool()
async def list_top_actors(limit: int = 10) -> list[dict]:
    """List the most prolific actors ranked by number of films they appear in."""
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT a.actor_id,
                   a.first_name || ' ' || a.last_name AS full_name,
                   COUNT(fa.film_id) AS film_count
            FROM actor a
            JOIN film_actor fa ON a.actor_id = fa.actor_id
            GROUP BY a.actor_id, a.first_name, a.last_name
            ORDER BY film_count DESC, a.last_name
            LIMIT $1
            """,
            limit,
        )
        return [dict(r) for r in rows]


# ── Rental tools ───────────────────────────────────────────────────────────────

@mcp.tool()
async def get_customer_current_rentals(email: str) -> dict:
    """
    Look up what a customer currently has rented (not yet returned) by their email address.
    Returns customer info and list of films currently checked out.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        customer = await conn.fetchrow(
            """
            SELECT customer_id, first_name || ' ' || last_name AS full_name, email
            FROM customer WHERE email ILIKE $1
            """,
            email,
        )
        if not customer:
            return {"error": f"No customer found with email matching '{email}'"}

        rows = await conn.fetch(
            """
            SELECT f.film_id, f.title, f.rating::text,
                   r.rental_date, r.rental_id,
                   s.store_id
            FROM rental r
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
            JOIN store s ON i.store_id = s.store_id
            WHERE r.customer_id = $1
              AND r.return_date IS NULL
            ORDER BY r.rental_date DESC
            """,
            customer["customer_id"],
        )
        return {
            "customer_id": customer["customer_id"],
            "name": customer["full_name"],
            "email": customer["email"],
            "current_rentals": [dict(r) for r in rows],
        }


@mcp.tool()
async def get_rental_stats_by_category() -> list[dict]:
    """
    Rental count and total revenue per film category, ordered by revenue descending.
    Bridges film category data with actual rental business performance.
    """
    pool = await _get_pool()
    return await _rental_stats_by_category(pool)


@mcp.tool()
async def get_recently_returned_films(limit: int = 10, store_id: int | None = None) -> list[dict]:
    """
    List films recently returned to stores — useful for 'what's back in stock'.
    Optionally filter by store_id. Returns film, return time, and which store.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        query = """
            SELECT f.film_id, f.title, f.rating::text,
                   r.return_date, i.store_id
            FROM rental r
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
            WHERE r.return_date IS NOT NULL
        """
        args: list = []
        if store_id is not None:
            args.append(store_id)
            query += f" AND i.store_id = ${len(args)}"
        query += f" ORDER BY r.return_date DESC LIMIT ${len(args) + 1}"
        args.append(limit)

        rows = await conn.fetch(query, *args)
        return [dict(r) for r in rows]


if __name__ == "__main__":
    mcp.run(transport="stdio")
