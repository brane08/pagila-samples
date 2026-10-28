import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP
from sqlalchemy import and_, case, func, or_, select
from sqlalchemy.engine import URL
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from analytics_queries import rental_stats_by_category as _rental_stats_by_category
from pagila_tables import actor, category as category_t, customer, film, film_actor, film_category, inventory, language, rental, store
from query_utils import LIKE_ESCAPE, MAX_LIMIT, clamp, like  # noqa: F401  (MAX_LIMIT re-exported)
from rag import semantic_search

load_dotenv()

mcp = FastMCP("PagilaFilms")

_pool: asyncpg.Pool | None = None
_engine: AsyncEngine | None = None


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


def _sqlalchemy_url() -> URL:
    return URL.create(
        "postgresql+asyncpg",
        username=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", "password"),
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", 5432)),
        database=os.getenv("DB_NAME", "sakila"),
    )


async def _get_engine() -> AsyncEngine:
    global _engine
    if _engine is None:
        _engine = create_async_engine(_sqlalchemy_url(), pool_size=1, max_overflow=4)
    return _engine


_LIKE_ESCAPE = LIKE_ESCAPE
_clamp = clamp
_like = like


# ── Tools ──────────────────────────────────────────────────────────────────────

@mcp.tool()
async def search_films(title: str, limit: int = 10) -> list[dict]:
    """Search films by partial title match. Returns title, rating, length, rental_rate."""
    engine = await _get_engine()
    stmt = (
        select(
            film.c.film_id,
            film.c.title,
            film.c.description,
            film.c.release_year,
            film.c.rating,
            film.c.length,
            film.c.rental_rate,
            film.c.rental_duration,
        )
        .where(film.c.title.ilike(f"%{_like(title)}%", escape=_LIKE_ESCAPE))
        .order_by(film.c.title)
        .limit(_clamp(limit))
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


@mcp.tool()
async def get_film_details(film_id: int) -> dict:
    """Get full details for a film including language, categories, and actors."""
    engine = await _get_engine()
    film_stmt = (
        select(
            film.c.film_id,
            film.c.title,
            film.c.description,
            film.c.release_year,
            film.c.rating,
            film.c.length,
            film.c.rental_rate,
            film.c.rental_duration,
            film.c.replacement_cost,
            language.c.name.label("language"),
        )
        .select_from(film)
        .join(language, film.c.language_id == language.c.language_id)
        .where(film.c.film_id == film_id)
    )
    categories_stmt = (
        select(category_t.c.name)
        .select_from(category_t)
        .join(film_category, category_t.c.category_id == film_category.c.category_id)
        .where(film_category.c.film_id == film_id)
    )
    actors_stmt = (
        select(actor.c.first_name.concat(" ").concat(actor.c.last_name).label("name"))
        .select_from(actor)
        .join(film_actor, actor.c.actor_id == film_actor.c.actor_id)
        .where(film_actor.c.film_id == film_id)
        .order_by(actor.c.last_name)
    )
    async with engine.connect() as conn:
        film_row = (await conn.execute(film_stmt)).mappings().first()
        if not film_row:
            return {"error": f"Film {film_id} not found"}
        categories = [r[0] for r in (await conn.execute(categories_stmt)).all()]
        actors = [r[0] for r in (await conn.execute(actors_stmt)).all()]

    return {
        **dict(film_row),
        "categories": categories,
        "actors": actors,
    }


@mcp.tool()
async def list_films_by_category(category: str, limit: int = 10) -> list[dict]:
    """List films belonging to a specific category (e.g. Action, Comedy, Horror)."""
    engine = await _get_engine()
    stmt = (
        select(film.c.film_id, film.c.title, film.c.rating, film.c.length, film.c.rental_rate)
        .select_from(film)
        .join(film_category, film.c.film_id == film_category.c.film_id)
        .join(category_t, film_category.c.category_id == category_t.c.category_id)
        .where(category_t.c.name.ilike(_like(category), escape=_LIKE_ESCAPE))
        .order_by(film.c.title)
        .limit(_clamp(limit))
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


@mcp.tool()
async def list_films_by_actor(actor_name: str, limit: int = 10) -> list[dict]:
    """Find all films featuring an actor by partial name match."""
    engine = await _get_engine()
    full_name = actor.c.first_name.concat(" ").concat(actor.c.last_name)
    stmt = (
        select(film.c.film_id, film.c.title, film.c.rating, film.c.length, full_name.label("actor"))
        .select_from(film)
        .join(film_actor, film.c.film_id == film_actor.c.film_id)
        .join(actor, film_actor.c.actor_id == actor.c.actor_id)
        .where(full_name.ilike(f"%{_like(actor_name)}%", escape=_LIKE_ESCAPE))
        .order_by(film.c.title)
        .limit(_clamp(limit))
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


@mcp.tool()
async def get_top_rented_films(limit: int = 10) -> list[dict]:
    """Return the most frequently rented films across all stores."""
    engine = await _get_engine()
    rental_count = func.count(rental.c.rental_id)
    stmt = (
        select(film.c.film_id, film.c.title, film.c.rating, rental_count.label("rental_count"))
        .select_from(film)
        .join(inventory, film.c.film_id == inventory.c.film_id)
        .join(rental, inventory.c.inventory_id == rental.c.inventory_id)
        .group_by(film.c.film_id, film.c.title, film.c.rating)
        .order_by(rental_count.desc())
        .limit(_clamp(limit))
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


@mcp.tool()
async def list_categories() -> list[str]:
    """List all available film categories in the database."""
    engine = await _get_engine()
    stmt = select(category_t.c.name).order_by(category_t.c.name)
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [row[0] for row in result.all()]


@mcp.tool()
async def get_film_availability(film_id: int) -> dict:
    """Check how many copies of a film are available across all stores."""
    engine = await _get_engine()
    stmt = (
        select(
            store.c.store_id,
            func.count(inventory.c.inventory_id).label("total_copies"),
            func.sum(
                case(
                    (
                        or_(rental.c.rental_id.is_(None), rental.c.return_date.is_not(None)),
                        1,
                    ),
                    else_=0,
                )
            ).label("available_copies"),
        )
        .select_from(inventory)
        .join(store, inventory.c.store_id == store.c.store_id)
        .outerjoin(
            rental,
            and_(inventory.c.inventory_id == rental.c.inventory_id, rental.c.return_date.is_(None)),
        )
        .where(inventory.c.film_id == film_id)
        .group_by(store.c.store_id)
        .order_by(store.c.store_id)
    )
    title_stmt = select(film.c.title).where(film.c.film_id == film_id)
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        stores = [dict(row) for row in result.mappings()]
        title_row = (await conn.execute(title_stmt)).first()

    return {
        "film_id": film_id,
        "title": title_row[0] if title_row else "Unknown",
        "stores": stores,
    }


@mcp.tool()
async def semantic_film_search(query: str, limit: int = 5) -> list[dict]:
    """
    Semantic similarity search over film descriptions.
    Use this when the user describes a plot, mood, theme, or vibe
    rather than an exact title — e.g. 'a film about redemption in space'
    or 'something funny with a road trip'.
    """
    return await semantic_search(query, k=_clamp(limit))


# ── Actor tools ────────────────────────────────────────────────────────────────

@mcp.tool()
async def search_actors(name: str, limit: int = 10) -> list[dict]:
    """Search actors by partial first or last name. Returns actor_id, full name, and film count."""
    engine = await _get_engine()
    full_name = actor.c.first_name.concat(" ").concat(actor.c.last_name)
    stmt = (
        select(
            actor.c.actor_id,
            full_name.label("full_name"),
            func.count(film_actor.c.film_id).label("film_count"),
        )
        .select_from(actor)
        .outerjoin(film_actor, actor.c.actor_id == film_actor.c.actor_id)
        .where(
            or_(
                full_name.ilike(f"%{_like(name)}%", escape=_LIKE_ESCAPE),
                actor.c.first_name.ilike(f"%{_like(name)}%", escape=_LIKE_ESCAPE),
                actor.c.last_name.ilike(f"%{_like(name)}%", escape=_LIKE_ESCAPE),
            )
        )
        .group_by(actor.c.actor_id, actor.c.first_name, actor.c.last_name)
        .order_by(actor.c.last_name, actor.c.first_name)
        .limit(_clamp(limit))
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


@mcp.tool()
async def get_actor_filmography(actor_id: int) -> dict:
    """
    Get a full filmography for an actor — all their films with title, category,
    rating, year, and rental rate. Complements get_film_details for the actor angle.
    """
    engine = await _get_engine()
    full_name = actor.c.first_name.concat(" ").concat(actor.c.last_name)
    actor_stmt = select(actor.c.actor_id, full_name.label("full_name")).where(actor.c.actor_id == actor_id)
    films_stmt = (
        select(
            film.c.film_id,
            film.c.title,
            film.c.release_year,
            film.c.rating,
            film.c.rental_rate,
            category_t.c.name.label("category"),
        )
        .select_from(film)
        .join(film_actor, film.c.film_id == film_actor.c.film_id)
        .join(film_category, film.c.film_id == film_category.c.film_id)
        .join(category_t, film_category.c.category_id == category_t.c.category_id)
        .where(film_actor.c.actor_id == actor_id)
        .order_by(film.c.title)
    )
    async with engine.connect() as conn:
        actor_row = (await conn.execute(actor_stmt)).mappings().first()
        if not actor_row:
            return {"error": f"Actor {actor_id} not found"}
        films = [dict(row) for row in (await conn.execute(films_stmt)).mappings()]

    return {
        "actor_id": actor_row["actor_id"],
        "name": actor_row["full_name"],
        "films": films,
    }


@mcp.tool()
async def list_top_actors(limit: int = 10) -> list[dict]:
    """List the most prolific actors ranked by number of films they appear in."""
    engine = await _get_engine()
    full_name = actor.c.first_name.concat(" ").concat(actor.c.last_name)
    film_count = func.count(film_actor.c.film_id)
    stmt = (
        select(actor.c.actor_id, full_name.label("full_name"), film_count.label("film_count"))
        .select_from(actor)
        .join(film_actor, actor.c.actor_id == film_actor.c.actor_id)
        .group_by(actor.c.actor_id, actor.c.first_name, actor.c.last_name)
        .order_by(film_count.desc(), actor.c.last_name)
        .limit(_clamp(limit))
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


# ── Rental tools ───────────────────────────────────────────────────────────────

@mcp.tool()
async def get_customer_current_rentals(email: str) -> dict:
    """
    Look up what a customer currently has rented (not yet returned) by their email address.
    Returns customer info and list of films currently checked out.
    """
    engine = await _get_engine()
    full_name = customer.c.first_name.concat(" ").concat(customer.c.last_name)
    customer_stmt = select(
        customer.c.customer_id, full_name.label("full_name"), customer.c.email
    ).where(customer.c.email.ilike(_like(email), escape=_LIKE_ESCAPE))

    async with engine.connect() as conn:
        customer_row = (await conn.execute(customer_stmt)).mappings().first()
        if not customer_row:
            return {"error": f"No customer found with email matching '{email}'"}

        rentals_stmt = (
            select(
                film.c.film_id,
                film.c.title,
                film.c.rating,
                rental.c.rental_date,
                rental.c.rental_id,
                store.c.store_id,
            )
            .select_from(rental)
            .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
            .join(film, inventory.c.film_id == film.c.film_id)
            .join(store, inventory.c.store_id == store.c.store_id)
            .where(rental.c.customer_id == customer_row["customer_id"], rental.c.return_date.is_(None))
            .order_by(rental.c.rental_date.desc())
        )
        rows = [dict(row) for row in (await conn.execute(rentals_stmt)).mappings()]

    return {
        "customer_id": customer_row["customer_id"],
        "name": customer_row["full_name"],
        "email": customer_row["email"],
        "current_rentals": rows,
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
    engine = await _get_engine()
    stmt = (
        select(film.c.film_id, film.c.title, film.c.rating, rental.c.return_date, inventory.c.store_id)
        .select_from(rental)
        .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
        .join(film, inventory.c.film_id == film.c.film_id)
        .where(rental.c.return_date.is_not(None))
        .order_by(rental.c.return_date.desc())
        .limit(_clamp(limit))
    )
    if store_id is not None:
        stmt = stmt.where(inventory.c.store_id == store_id)
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]


if __name__ == "__main__":
    mcp.run(transport="stdio")
