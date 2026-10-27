# genai-mcp-sqlalchemy-phase2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert all 12 raw-asyncpg tools in `film_server.py` and all 4 in `analytics_server.py` to SQLAlchemy Core, mirroring the merged `store_server.py` POC (PR #12), including the Python-side-bucketing pattern for non-portable Postgres SQL.

**Architecture:** Extend the shared table-metadata module (`store_tables.py` → renamed `pagila_tables.py`) with `language`, `actor`, `film_actor` tables and the missing `film` columns. Each tool is converted one at a time: build the equivalent `select()`/`join()` via SQLAlchemy Core against the shared engine, verify against its existing pytest coverage, commit. Three analytics queries use genuinely non-portable constructs (`CURRENT_DATE` arithmetic, `NOW() - INTERVAL`, `DATE_TRUNC`) — for these, fetch the minimal rows via Core and do the date arithmetic/bucketing in Python, following the precedent set by `store_server.get_store_monthly_revenue`.

**Tech Stack:** Python 3.13, SQLAlchemy Core (async, `postgresql+asyncpg` dialect), pytest + pytest-asyncio, live sakila/Pagila Postgres DB.

**Spec:** No separate spec doc — this plan's Goal/Architecture sections are the spec, informed directly by reading `film_server.py`, `analytics_server.py`, `store_tables.py`, and all four corresponding test files (`test_film_tools.py`, `test_actor_tools.py`, `test_rental_tools.py`, `test_analytics_tools.py`) in full, plus a live-DB schema check for `language`/`actor`/`film_actor`.

## Global Constraints

- Tool function signatures, docstrings, and return shapes must not change — only the query implementation.
- No raw SQL strings in converted functions — SQLAlchemy Core expressions only.
- `_sqlalchemy_url()` must use `sqlalchemy.engine.URL.create()` (not f-string interpolation) — this was a post-review fix in the store_server POC; build it in from the start this time.
- `film.c.rating` stays `String` (not the Postgres `mpaa_rating` enum) — asyncpg returns it as `str` natively; this already works in the merged `store_tables.py` declarations.
- `get_customer_current_rentals`'s email lookup must remain an exact-value `.ilike()` match (no `%...%` wrapping) — `test_partial_email_no_match` requires a partial email to NOT match.
- `get_revenue_summary`'s `busiest_month` must be an exact `"YYYY-MM"` string (`test_busiest_month_format`), not a `date`/`datetime`.
- `semantic_film_search` in `film_server.py` is out of scope — it has no direct DB access, delegates entirely to `rag.py`.
- Scope is limited to `film_server.py`, `analytics_server.py`, `pagila_tables.py` (formerly `store_tables.py`), and `tests/conftest.py`.

---

## File Structure

- `genai-assistant/src/store_tables.py` → renamed to `genai-assistant/src/pagila_tables.py`, extended with `language`, `actor`, `film_actor` tables and the missing `film` columns (`description`, `release_year`, `language_id`, `rental_duration`, `replacement_cost`). This module is now shared by all three MCP servers.
- `genai-assistant/src/store_server.py` — only its import line changes (`from store_tables import ...` → `from pagila_tables import ...`).
- `genai-assistant/src/film_server.py` — gains `_sqlalchemy_url()`/`_get_engine()` (Task 2), each of its 13 in-scope tools converted one per task, `_pool`/`_get_pool()` shim removed last (Task 19).
- `genai-assistant/src/analytics_server.py` — gains `_sqlalchemy_url()`/`_get_engine()` (Task 15), each of its 4 tools converted one per task, `_pool`/`_get_pool()` shim removed last (Task 19).
- `genai-assistant/tests/conftest.py` — `inject_pool` fixture updated incrementally: Task 2 adds `film_server._engine` injection (kept alongside the `_pool` injection, mirroring the store_server POC's Task-3 ruling to pull the engine-reset forward rather than deferring it), Task 15 adds `analytics_server._engine`, Task 19 removes the `asyncpg` pool entirely (confirmed via grep that no other module — `db.py`, `main.py`, `sessions.py` — depends on `film_server._pool`/`analytics_server._pool`; they use their own independent `db.py`-owned asyncpg pool).

---

### Task 1: Extend and rename the shared table metadata module

**Files:**
- Rename: `genai-assistant/src/store_tables.py` → `genai-assistant/src/pagila_tables.py`
- Modify: `genai-assistant/src/store_server.py` (import line only)

**Interfaces:**
- Produces: `language`, `actor`, `film_actor` `Table` objects; extended `film` table with `description`, `release_year`, `language_id`, `rental_duration`, `replacement_cost` columns (all consumed by Tasks 3, 5, 8, 10, 16-18).
- Consumes: nothing new.

- [ ] **Step 1: Rename the file**

Run: `git mv genai-assistant/src/store_tables.py genai-assistant/src/pagila_tables.py`

- [ ] **Step 2: Replace the file's contents**

```python
"""SQLAlchemy Core table metadata for the tables store_server.py, film_server.py,
and analytics_server.py query.

Plain Table declarations (not reflected, not ORM-mapped) — these MCP tools are
functional read queries, not domain CRUD, so Core's expression language is enough
without an ORM mapping layer.
"""
from sqlalchemy import Column, DateTime, Integer, MetaData, Numeric, String, Table, Text

metadata = MetaData()

store = Table(
    "store",
    metadata,
    Column("store_id", Integer, primary_key=True),
    Column("manager_staff_id", Integer),
    Column("address_id", Integer),
)

staff = Table(
    "staff",
    metadata,
    Column("staff_id", Integer, primary_key=True),
    Column("first_name", String),
    Column("last_name", String),
)

address = Table(
    "address",
    metadata,
    Column("address_id", Integer, primary_key=True),
    Column("address", String),
    Column("district", String),
    Column("city_id", Integer),
)

city = Table(
    "city",
    metadata,
    Column("city_id", Integer, primary_key=True),
    Column("city", String),
)

language = Table(
    "language",
    metadata,
    Column("language_id", Integer, primary_key=True),
    Column("name", String),
)

# `rating` is declared String though the Postgres column type is the `mpaa_rating`
# enum — asyncpg returns it as a plain str, so no explicit cast/codec is needed here.
film = Table(
    "film",
    metadata,
    Column("film_id", Integer, primary_key=True),
    Column("title", String),
    Column("description", Text),
    Column("release_year", Integer),
    Column("language_id", Integer),
    Column("rental_duration", Integer),
    Column("rental_rate", Numeric),
    Column("length", Integer),
    Column("replacement_cost", Numeric),
    Column("rating", String),
)

category = Table(
    "category",
    metadata,
    Column("category_id", Integer, primary_key=True),
    Column("name", String),
)

film_category = Table(
    "film_category",
    metadata,
    Column("film_id", Integer, primary_key=True),
    Column("category_id", Integer, primary_key=True),
)

actor = Table(
    "actor",
    metadata,
    Column("actor_id", Integer, primary_key=True),
    Column("first_name", String),
    Column("last_name", String),
)

film_actor = Table(
    "film_actor",
    metadata,
    Column("film_id", Integer, primary_key=True),
    Column("actor_id", Integer, primary_key=True),
)

inventory = Table(
    "inventory",
    metadata,
    Column("inventory_id", Integer, primary_key=True),
    Column("film_id", Integer),
    Column("store_id", Integer),
)

rental = Table(
    "rental",
    metadata,
    Column("rental_id", Integer, primary_key=True),
    Column("inventory_id", Integer),
    Column("customer_id", Integer),
    Column("rental_date", DateTime),
    Column("return_date", DateTime),
)

customer = Table(
    "customer",
    metadata,
    Column("customer_id", Integer, primary_key=True),
    Column("first_name", String),
    Column("last_name", String),
    Column("email", String),
)

payment = Table(
    "payment",
    metadata,
    Column("payment_id", Integer, primary_key=True),
    Column("customer_id", Integer),
    Column("rental_id", Integer),
    Column("amount", Numeric),
    Column("payment_date", DateTime),
)
```

- [ ] **Step 3: Update store_server.py's import**

In `genai-assistant/src/store_server.py`, change:
```python
from store_tables import (
```
to:
```python
from pagila_tables import (
```
(the imported names on the following lines are unchanged).

- [ ] **Step 4: Run the store test file to confirm nothing broke**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_store_tools.py -v`
Expected: PASS, all 37 tests.

- [ ] **Step 5: Sanity-check the new table declarations import cleanly**

Run: `cd genai-assistant && .venv/bin/python3 -c "from pagila_tables import language, actor, film_actor, film; print(sorted(c.name for c in film.columns))"`
Expected: prints a list including `description`, `film_id`, `language_id`, `length`, `rating`, `release_year`, `rental_duration`, `rental_rate`, `replacement_cost`, `title`.

- [ ] **Step 6: Commit**

```bash
git add genai-assistant/src/pagila_tables.py genai-assistant/src/store_tables.py genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): rename store_tables to pagila_tables, add language/actor/film_actor + film columns

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 2: film_server.py setup (`_sqlalchemy_url`/`_get_engine`) + convert `search_films`

**Files:**
- Modify: `genai-assistant/src/film_server.py`
- Modify: `genai-assistant/tests/conftest.py`

**Interfaces:**
- Consumes: `film` from `pagila_tables` (Task 1).
- Produces: `async def _get_engine() -> AsyncEngine` — consumed by every remaining film_server.py task (3-14).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestSearchFilms -v`
Expected: PASS.

- [ ] **Step 2: Add the engine setup and convert `search_films`**

Replace the top of `genai-assistant/src/film_server.py` (imports through `_get_pool`) with:

```python
import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP
from sqlalchemy import select
from sqlalchemy.engine import URL
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from pagila_tables import film
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
```

Replace the `search_films` function body:

```python
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
        .where(film.c.title.ilike(f"%{title}%"))
        .order_by(film.c.title)
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Update conftest.py to inject the engine into film_server too**

In `genai-assistant/tests/conftest.py`, change:
```python
    store_server._engine = engine

    yield

    film_server._pool = None
    analytics_server._pool = None
    store_server._engine = None
    await pool.close()
    await engine.dispose()
```
to:
```python
    store_server._engine = engine
    film_server._engine = engine

    yield

    film_server._pool = None
    analytics_server._pool = None
    store_server._engine = None
    film_server._engine = None
    await pool.close()
    await engine.dispose()
```

- [ ] **Step 4: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestSearchFilms -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/film_server.py genai-assistant/tests/conftest.py
git commit -m "refactor(genai-assistant): add SQLAlchemy engine to film_server, convert search_films

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 3: Convert `get_film_details`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `film`, `language`, `category as category_t`, `film_category`, `actor`, `film_actor` from `pagila_tables`.
- Produces: `category_t`, `film_category`, `actor`, `film_actor`, `language` become available imports for later tasks (4, 5, 8-13) to reuse (they import them independently in their own steps since each task's diff is self-contained).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestGetFilmDetails -v`
Expected: PASS.

- [ ] **Step 2: Update the import line and convert the function**

Change the import line:
```python
from pagila_tables import film
```
to:
```python
from pagila_tables import actor, category as category_t, film, film_actor, film_category, language
```

Replace the `get_film_details` function body:

```python
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
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestGetFilmDetails -v`
Expected: PASS, including the not-found case.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_film_details to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 4: Convert `list_films_by_category`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `film`, `film_category`, `category_t` from `pagila_tables` (imported in Task 3, already in the import line).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestListFilmsByCategory -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
@mcp.tool()
async def list_films_by_category(category: str, limit: int = 10) -> list[dict]:
    """List films belonging to a specific category (e.g. Action, Comedy, Horror)."""
    engine = await _get_engine()
    stmt = (
        select(film.c.film_id, film.c.title, film.c.rating, film.c.length, film.c.rental_rate)
        .select_from(film)
        .join(film_category, film.c.film_id == film_category.c.film_id)
        .join(category_t, film_category.c.category_id == category_t.c.category_id)
        .where(category_t.c.name.ilike(category))
        .order_by(film.c.title)
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestListFilmsByCategory -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert list_films_by_category to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 5: Convert `list_films_by_actor`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `film`, `film_actor`, `actor` from `pagila_tables` (already imported).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestListFilmsByActor -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
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
        .where(full_name.ilike(f"%{actor_name}%"))
        .order_by(film.c.title)
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestListFilmsByActor -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert list_films_by_actor to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 6: Convert `get_top_rented_films`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `film`, `inventory`, `rental` from `pagila_tables`.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestGetTopRentedFilms -v`
Expected: PASS.

- [ ] **Step 2: Update imports and replace the function body**

Change the `sqlalchemy` import line:
```python
from sqlalchemy import select
```
to:
```python
from sqlalchemy import func, select
```

Change the `pagila_tables` import line to add `inventory` and `rental`:
```python
from pagila_tables import actor, category as category_t, film, film_actor, film_category, inventory, language, rental
```

Replace the function body:

```python
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
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestGetTopRentedFilms -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_top_rented_films to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 7: Convert `list_categories`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `category_t` from `pagila_tables` (already imported).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestListCategories -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
@mcp.tool()
async def list_categories() -> list[str]:
    """List all available film categories in the database."""
    engine = await _get_engine()
    stmt = select(category_t.c.name).order_by(category_t.c.name)
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [row[0] for row in result.all()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestListCategories -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert list_categories to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 8: Convert `get_film_availability`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `store`, `inventory`, `rental`, `film` from `pagila_tables`.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestGetFilmAvailability -v`
Expected: PASS.

- [ ] **Step 2: Update imports and replace the function body**

Change the `sqlalchemy` import line:
```python
from sqlalchemy import func, select
```
to:
```python
from sqlalchemy import and_, case, func, or_, select
```

Change the `pagila_tables` import line to add `store`:
```python
from pagila_tables import actor, category as category_t, film, film_actor, film_category, inventory, language, rental, store
```

Replace the function body:

```python
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
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py::TestGetFilmAvailability -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_film_availability to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 9: Convert `search_actors`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `actor`, `film_actor` from `pagila_tables` (already imported).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_actor_tools.py::TestSearchActors -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
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
                full_name.ilike(f"%{name}%"),
                actor.c.first_name.ilike(f"%{name}%"),
                actor.c.last_name.ilike(f"%{name}%"),
            )
        )
        .group_by(actor.c.actor_id, actor.c.first_name, actor.c.last_name)
        .order_by(actor.c.last_name, actor.c.first_name)
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_actor_tools.py::TestSearchActors -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert search_actors to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 10: Convert `get_actor_filmography`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `actor`, `film`, `film_actor`, `film_category`, `category_t` from `pagila_tables` (already imported).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_actor_tools.py::TestGetActorFilmography -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
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
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_actor_tools.py::TestGetActorFilmography -v`
Expected: PASS, including the not-found case.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_actor_filmography to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 11: Convert `list_top_actors`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `actor`, `film_actor` from `pagila_tables` (already imported).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_actor_tools.py::TestListTopActors -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
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
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_actor_tools.py::TestListTopActors -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert list_top_actors to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 12: Convert `get_customer_current_rentals`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `customer`, `rental`, `inventory`, `film`, `store` from `pagila_tables` — `customer` is new, added this task.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_rental_tools.py::TestGetCustomerCurrentRentals -v`
Expected: PASS.

- [ ] **Step 2: Update imports and replace the function body**

Change the `pagila_tables` import line to add `customer`:
```python
from pagila_tables import actor, category as category_t, customer, film, film_actor, film_category, inventory, language, rental, store
```

Replace the function body:

```python
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
    ).where(customer.c.email.ilike(email))

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
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_rental_tools.py::TestGetCustomerCurrentRentals -v`
Expected: PASS, including `test_partial_email_no_match` (a partial email like `"mary.smith"` must NOT match — `.ilike(email)` with no `%` wrapping preserves this).

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_customer_current_rentals to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 13: Convert `get_rental_stats_by_category`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `category_t`, `film_category`, `film`, `inventory`, `rental`, `payment` from `pagila_tables` — `payment` is new, added this task.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_rental_tools.py::TestGetRentalStatsByCategory -v`
Expected: PASS.

- [ ] **Step 2: Update imports and replace the function body**

Change the `pagila_tables` import line to add `payment`:
```python
from pagila_tables import actor, category as category_t, customer, film, film_actor, film_category, inventory, language, payment, rental, store
```

Replace the function body:

```python
@mcp.tool()
async def get_rental_stats_by_category() -> list[dict]:
    """
    Rental count and total revenue per film category, ordered by revenue descending.
    Bridges film category data with actual rental business performance.
    """
    engine = await _get_engine()
    total_revenue = func.coalesce(func.sum(payment.c.amount), 0)
    stmt = (
        select(
            category_t.c.name.label("category"),
            func.count(rental.c.rental_id).label("rental_count"),
            total_revenue.label("total_revenue"),
        )
        .select_from(category_t)
        .join(film_category, category_t.c.category_id == film_category.c.category_id)
        .join(film, film_category.c.film_id == film.c.film_id)
        .join(inventory, film.c.film_id == inventory.c.film_id)
        .join(rental, inventory.c.inventory_id == rental.c.inventory_id)
        .outerjoin(payment, rental.c.rental_id == payment.c.rental_id)
        .group_by(category_t.c.name)
        .order_by(total_revenue.desc())
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_rental_tools.py::TestGetRentalStatsByCategory -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_rental_stats_by_category to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 14: Convert `get_recently_returned_films`

**Files:**
- Modify: `genai-assistant/src/film_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 2), `film`, `inventory`, `rental` from `pagila_tables` (already imported).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_rental_tools.py::TestGetRecentlyReturnedFilms -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
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
        .limit(limit)
    )
    if store_id is not None:
        stmt = stmt.where(inventory.c.store_id == store_id)
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_rental_tools.py::TestGetRecentlyReturnedFilms -v`
Expected: PASS, including the `store_id`-filtered case.

- [ ] **Step 4: Run the full film/actor/rental suite**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py tests/test_actor_tools.py tests/test_rental_tools.py -v`
Expected: PASS, all tests (film_server.py's conversion is now complete except the `_pool` shim removal, deferred to Task 19).

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/film_server.py
git commit -m "refactor(genai-assistant): convert get_recently_returned_films to SQLAlchemy Core

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 15: analytics_server.py setup (`_sqlalchemy_url`/`_get_engine`) + convert `get_store_comparison`

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`
- Modify: `genai-assistant/tests/conftest.py`

**Interfaces:**
- Consumes: `store`, `staff`, `address`, `city`, `inventory`, `film`, `rental`, `payment` from `pagila_tables`.
- Produces: `async def _get_engine() -> AsyncEngine` — consumed by Tasks 16-18.

**Why this one first:** `get_store_comparison` uses only ANSI-standard aggregates (`SUM`, `COUNT(DISTINCT ...)`, `AVG`, `CASE WHEN`) — no non-portable date/interval arithmetic — so it converts directly, same as the other three analytics tools will *not*.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetStoreComparison -v`
Expected: PASS.

- [ ] **Step 2: Add the engine setup and convert `get_store_comparison`**

Replace the top of `genai-assistant/src/analytics_server.py` (imports through `_get_pool`) with:

```python
import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP
from sqlalchemy import case, func, select
from sqlalchemy.engine import URL
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from pagila_tables import address, city, film, inventory, payment, rental, staff, store

load_dotenv()

mcp = FastMCP("PagilaAnalytics")

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
```

Replace the `get_store_comparison` function body:

```python
@mcp.tool()
async def get_store_comparison() -> list[dict]:
    """
    Side-by-side key metrics for both stores: manager, city, total revenue, rental count,
    unique customers, average rental rate across stocked inventory, and outstanding rentals.
    One row per store, ordered by store_id.
    """
    engine = await _get_engine()
    manager = staff.c.first_name.concat(" ").concat(staff.c.last_name)
    stmt = (
        select(
            store.c.store_id,
            manager.label("manager"),
            city.c.city,
            func.round(func.coalesce(func.sum(payment.c.amount), 0), 2).label("total_revenue"),
            func.count(func.distinct(rental.c.rental_id)).label("rental_count"),
            func.count(func.distinct(rental.c.customer_id)).label("unique_customers"),
            func.coalesce(func.round(func.avg(film.c.rental_rate), 2), 0).label("avg_rental_rate"),
            func.count(
                func.distinct(case((rental.c.return_date.is_(None), rental.c.rental_id)))
            ).label("outstanding_rentals"),
        )
        .select_from(store)
        .join(staff, store.c.manager_staff_id == staff.c.staff_id)
        .join(address, store.c.address_id == address.c.address_id)
        .join(city, address.c.city_id == city.c.city_id)
        .outerjoin(inventory, store.c.store_id == inventory.c.store_id)
        .outerjoin(film, inventory.c.film_id == film.c.film_id)
        .outerjoin(rental, inventory.c.inventory_id == rental.c.inventory_id)
        .outerjoin(payment, rental.c.rental_id == payment.c.rental_id)
        .group_by(store.c.store_id, staff.c.first_name, staff.c.last_name, city.c.city)
        .order_by(store.c.store_id)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [
            {**dict(row), "total_revenue": float(row["total_revenue"]),
             "avg_rental_rate": float(row["avg_rental_rate"])}
            for row in result.mappings()
        ]
```

- [ ] **Step 3: Update conftest.py to inject the engine into analytics_server too**

In `genai-assistant/tests/conftest.py`, change:
```python
    store_server._engine = engine
    film_server._engine = engine

    yield

    film_server._pool = None
    analytics_server._pool = None
    store_server._engine = None
    film_server._engine = None
    await pool.close()
    await engine.dispose()
```
to:
```python
    store_server._engine = engine
    film_server._engine = engine
    analytics_server._engine = engine

    yield

    film_server._pool = None
    analytics_server._pool = None
    store_server._engine = None
    film_server._engine = None
    analytics_server._engine = None
    await pool.close()
    await engine.dispose()
```

- [ ] **Step 4: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetStoreComparison -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/analytics_server.py genai-assistant/tests/conftest.py
git commit -m "refactor(genai-assistant): add SQLAlchemy engine to analytics_server, convert get_store_comparison

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 16: Convert `get_overdue_rentals` (Python-side date arithmetic)

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 15), `rental`, `inventory`, `film`, `customer` from `pagila_tables` — `customer` is new, added this task.

**Why this one differs:** the original query computes `(CURRENT_DATE - (r.rental_date::date + f.rental_duration))::int AS days_overdue` — Postgres `CURRENT_DATE` arithmetic with no portable Core equivalent. This task fetches all open rentals (optionally filtered by `store_id` at the SQL level, which is portable) and computes `days_overdue` in Python.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetOverdueRentals -v`
Expected: PASS.

- [ ] **Step 2: Update imports and replace the function body**

Add a `datetime` import at the top of `genai-assistant/src/analytics_server.py`:
```python
from datetime import date, timedelta
```

Change the `pagila_tables` import line to add `customer`:
```python
from pagila_tables import address, city, customer, film, inventory, payment, rental, staff, store
```

Replace the function body:

```python
@mcp.tool()
async def get_overdue_rentals(store_id: int | None = None, limit: int = 20) -> list[dict]:
    """
    Rentals not yet returned that are past their rental_duration deadline.
    days_overdue is how many days beyond the due date the item is.
    Optionally filter by store_id. Ordered most overdue first.
    """
    engine = await _get_engine()
    stmt = (
        select(
            rental.c.rental_id,
            film.c.film_id,
            film.c.title,
            customer.c.email.label("customer_email"),
            inventory.c.store_id,
            rental.c.rental_date,
            film.c.rental_duration,
        )
        .select_from(rental)
        .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
        .join(film, inventory.c.film_id == film.c.film_id)
        .join(customer, rental.c.customer_id == customer.c.customer_id)
        .where(rental.c.return_date.is_(None))
    )
    if store_id is not None:
        stmt = stmt.where(inventory.c.store_id == store_id)

    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        rows = result.mappings().all()

    today = date.today()
    overdue = []
    for row in rows:
        due_date = row["rental_date"].date() + timedelta(days=row["rental_duration"])
        days_overdue = (today - due_date).days
        if days_overdue <= 0:
            continue
        overdue.append(
            {
                "rental_id": row["rental_id"],
                "film_id": row["film_id"],
                "title": row["title"],
                "customer_email": row["customer_email"],
                "store_id": row["store_id"],
                "rental_date": row["rental_date"].date(),
                "days_overdue": days_overdue,
            }
        )

    overdue.sort(key=lambda r: r["days_overdue"], reverse=True)
    return overdue[:limit]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetOverdueRentals -v`
Expected: PASS, including the `store_id`-filtered case and `days_overdue >= 1` for every row.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/analytics_server.py
git commit -m "refactor(genai-assistant): convert get_overdue_rentals to SQLAlchemy Core + Python date math

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 17: Convert `get_slow_moving_films` (Python-side date filtering)

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 15), `inventory`, `film`, `rental` from `pagila_tables` (already imported).

**Why this one differs:** the original query's `HAVING MAX(r.rental_date) < NOW() - ($1 * INTERVAL '1 day') OR MAX(r.rental_date) IS NULL` and `ORDER BY days_since_rented DESC NULLS FIRST` have no portable Core equivalent. The `COUNT(DISTINCT ...)`/`MAX(...)` aggregation itself is portable, so it stays in Core (optionally filtered by `store_id`); the `HAVING`/date-cutoff filtering, `days_since_rented` computation, and NULLS-FIRST-descending sort move to Python.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetSlowMovingFilms -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
@mcp.tool()
async def get_slow_moving_films(store_id: int | None = None, days: int = 90, limit: int = 20) -> list[dict]:
    """
    Films present in inventory that have had zero rentals in the last `days` days,
    or have never been rented. Returns one row per (film, store) pair.
    days_since_rented is None for films that have never been rented.
    Ordered by days_since_rented descending, never-rented films first.
    Optionally filter by store_id.
    """
    engine = await _get_engine()
    stmt = (
        select(
            film.c.film_id,
            film.c.title,
            film.c.rating,
            inventory.c.store_id,
            func.count(func.distinct(inventory.c.inventory_id)).label("copies_in_stock"),
            func.max(rental.c.rental_date).label("last_rented"),
        )
        .select_from(inventory)
        .join(film, inventory.c.film_id == film.c.film_id)
        .outerjoin(rental, inventory.c.inventory_id == rental.c.inventory_id)
        .group_by(film.c.film_id, film.c.title, film.c.rating, inventory.c.store_id)
    )
    if store_id is not None:
        stmt = stmt.where(inventory.c.store_id == store_id)

    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        rows = result.mappings().all()

    today = date.today()
    cutoff = today - timedelta(days=days)
    slow = []
    for row in rows:
        last_rented = row["last_rented"]
        if last_rented is not None and last_rented.date() >= cutoff:
            continue
        days_since_rented = (today - last_rented.date()).days if last_rented is not None else None
        slow.append(
            {
                "film_id": row["film_id"],
                "title": row["title"],
                "rating": row["rating"],
                "store_id": row["store_id"],
                "copies_in_stock": row["copies_in_stock"],
                "last_rented": last_rented.date() if last_rented is not None else None,
                "days_since_rented": days_since_rented,
            }
        )

    slow.sort(key=lambda r: (r["days_since_rented"] is not None, -(r["days_since_rented"] or 0)))
    return slow[:limit]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetSlowMovingFilms -v`
Expected: PASS, including never-rented films sorting first and the `store_id`-filtered case.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/analytics_server.py
git commit -m "refactor(genai-assistant): convert get_slow_moving_films to SQLAlchemy Core + Python filtering

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 18: Convert `get_revenue_summary` (Python-side month bucketing)

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 15), `payment`, `rental`, `inventory` from `pagila_tables` (already imported).

**Why this one differs:** the original query uses a CTE with `DATE_TRUNC('month', payment_date)` + `TO_CHAR(..., 'YYYY-MM')` in a correlated subquery to find the busiest month — no portable Core equivalent. The `by_store` breakdown (`SUM`/`COUNT`/`AVG`) is portable and stays in Core. For global totals and the busiest month, this task fetches raw `(payment_date, amount)` pairs (sakila's `payment` table is ~16k rows total — not a performance concern for this POC) and buckets them by `strftime("%Y-%m")` in Python, which produces the exact string format the tests require directly (no `date`/`datetime` object to reformat).

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetRevenueSummary -v`
Expected: PASS.

- [ ] **Step 2: Replace the function body**

```python
@mcp.tool()
async def get_revenue_summary() -> dict:
    """
    Global revenue totals across all stores: total revenue, total rentals, average per rental,
    busiest month (YYYY-MM), and a per-store revenue breakdown.
    """
    engine = await _get_engine()
    store_stmt = (
        select(
            inventory.c.store_id,
            func.round(func.sum(payment.c.amount), 2).label("revenue"),
            func.count(payment.c.payment_id).label("rental_count"),
            func.round(func.avg(payment.c.amount), 2).label("avg_per_rental"),
        )
        .select_from(payment)
        .join(rental, payment.c.rental_id == rental.c.rental_id)
        .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
        .group_by(inventory.c.store_id)
        .order_by(inventory.c.store_id)
    )
    dates_stmt = select(payment.c.payment_date, payment.c.amount)

    async with engine.connect() as conn:
        store_rows = (await conn.execute(store_stmt)).mappings().all()
        date_rows = (await conn.execute(dates_stmt)).all()

    if not date_rows:
        return {
            "total_revenue": 0.0,
            "total_rentals": 0,
            "avg_per_rental": 0.0,
            "busiest_month": None,
            "busiest_month_revenue": 0.0,
            "by_store": [],
        }

    total_revenue = sum(amount for _, amount in date_rows)
    total_rentals = len(date_rows)
    avg_per_rental = total_revenue / total_rentals

    buckets: dict = {}
    for payment_date, amount in date_rows:
        month_key = payment_date.strftime("%Y-%m")
        buckets[month_key] = buckets.get(month_key, 0) + amount
    busiest_month, busiest_month_revenue = max(buckets.items(), key=lambda kv: kv[1])

    return {
        "total_revenue": round(float(total_revenue), 2),
        "total_rentals": total_rentals,
        "avg_per_rental": round(float(avg_per_rental), 2),
        "busiest_month": busiest_month,
        "busiest_month_revenue": round(float(busiest_month_revenue), 2),
        "by_store": [
            {
                "store_id": row["store_id"],
                "revenue": float(row["revenue"]),
                "rental_count": row["rental_count"],
                "avg_per_rental": float(row["avg_per_rental"]),
            }
            for row in store_rows
        ],
    }
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py::TestGetRevenueSummary -v`
Expected: PASS, including `test_busiest_month_format` (exact `"YYYY-MM"` string) and `test_store_revenues_sum_to_total` (by_store revenues sum to total within 0.01).

- [ ] **Step 4: Run the full analytics suite**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_analytics_tools.py -v`
Expected: PASS, all tests (analytics_server.py's conversion is now complete except the `_pool` shim removal, deferred to Task 19).

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/analytics_server.py
git commit -m "refactor(genai-assistant): convert get_revenue_summary to SQLAlchemy Core + Python bucketing

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

### Task 19: Remove the leftover asyncpg pools, finalize the test fixture, run the full suite

**Files:**
- Modify: `genai-assistant/src/film_server.py`
- Modify: `genai-assistant/src/analytics_server.py`
- Modify: `genai-assistant/tests/conftest.py`

**Interfaces:**
- Consumes: `_engine`/`_get_engine()` in both files (Tasks 2 and 15, now the only connection mechanism in each file).
- Produces: none further — this closes out both files' migration.

- [ ] **Step 1: Remove the leftover `_get_pool` shim from film_server.py**

In `genai-assistant/src/film_server.py`, delete the `import asyncpg` line, the `_pool: asyncpg.Pool | None = None` line, and the `_get_pool()` function (every `@mcp.tool()` function now uses `_get_engine()` instead).

- [ ] **Step 2: Remove the leftover `_get_pool` shim from analytics_server.py**

In `genai-assistant/src/analytics_server.py`, delete the `import asyncpg` line, the `_pool: asyncpg.Pool | None = None` line, and the `_get_pool()` function.

- [ ] **Step 3: Simplify the test fixture — drop the asyncpg pool entirely**

Confirmed via `grep -rn "_get_pool\|_pool\b" genai-assistant/src/*.py genai-assistant/tests/*.py` that no module outside `film_server.py`/`analytics_server.py` depends on their `_pool` attribute — `db.py` owns a fully separate asyncpg pool for `main.py`/`sessions.py`.

Replace `genai-assistant/tests/conftest.py` entirely with:

```python
import os
import sys

import pytest_asyncio
from dotenv import load_dotenv
from sqlalchemy.ext.asyncio import create_async_engine

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../src"))

load_dotenv(os.path.join(os.path.dirname(__file__), "../.env"))

_DB = dict(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", 5432)),
    database=os.getenv("DB_NAME", "sakila"),
    user=os.getenv("DB_USER", "postgres"),
    password=os.getenv("DB_PASSWORD", "password"),
)


@pytest_asyncio.fixture(autouse=True)
async def inject_pool():
    """Create a SQLAlchemy engine on this test's event loop and inject into all MCP servers."""
    import analytics_server
    import film_server
    import store_server

    engine = create_async_engine(
        f"postgresql+asyncpg://{_DB['user']}:{_DB['password']}@{_DB['host']}:{_DB['port']}/{_DB['database']}",
        pool_size=1,
        max_overflow=2,
    )
    store_server._engine = engine
    film_server._engine = engine
    analytics_server._engine = engine

    yield

    store_server._engine = None
    film_server._engine = None
    analytics_server._engine = None
    await engine.dispose()
```

- [ ] **Step 4: Run the film, actor, rental, and analytics test files**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest tests/test_film_tools.py tests/test_actor_tools.py tests/test_rental_tools.py tests/test_analytics_tools.py tests/test_store_tools.py -v`
Expected: PASS, no regressions.

- [ ] **Step 5: Run the full test suite to confirm no cross-file breakage**

Run: `cd genai-assistant && .venv/bin/python3 -m pytest -v`
Expected: PASS for all MCP tool test files; `test_mcp.py`/`test_api.py` may show the same pre-existing unrelated failures noted in the store_server POC (mcp/fastmcp version mismatch, missing `langchain_mcp_adapters`) — confirm these are identical to pre-Task-19 HEAD, not caused by this branch.

- [ ] **Step 6: Commit**

```bash
git add genai-assistant/src/film_server.py genai-assistant/src/analytics_server.py genai-assistant/tests/conftest.py
git commit -m "refactor(genai-assistant): drop asyncpg pools from film_server/analytics_server, simplify test fixture

Claude-Session: https://claude.ai/code/session_01N1a6k2KTuupGsZkyCoPMPt"
```

---

## Outcome / Known Gaps (for whoever reviews this POC)

- `store_server.py`, `film_server.py`, `analytics_server.py` are now all fully SQLAlchemy Core — no raw SQL strings, no `ILIKE`/`::text`/`||`/`DATE_TRUNC`/`CURRENT_DATE` left in any of them.
- `rag.py` (pgvector) and `sessions.py` (LangGraph's `AsyncPostgresSaver`) remain Postgres-coupled — out of scope for this plan, same as the store_server POC noted.
- The Postgres dialect is still hardcoded in each `_sqlalchemy_url()` (`postgresql+asyncpg`) — expected for a POC scoped to Postgres; the natural next step if MSSQL support is pursued for real.
