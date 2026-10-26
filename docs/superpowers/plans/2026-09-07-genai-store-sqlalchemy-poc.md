# genai-assistant store_server SQLAlchemy Migration (POC) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `genai-assistant/src/store_server.py`'s raw asyncpg SQL (string-built queries, `ILIKE`, `::text` casts, `||` concat, `DATE_TRUNC`) with SQLAlchemy Core expressions, as a proof of concept for the broader "favour ORM-based queries for DB portability" goal (future MSSQL support).

**Architecture:** Add a new `store_tables.py` module declaring plain SQLAlchemy Core `Table` objects for the 11 tables `store_server.py` touches (no reflection, no ORM mapped classes — this file's tools are functional queries, not domain CRUD). Swap `store_server.py`'s local asyncpg pool for a local SQLAlchemy `AsyncEngine` (`postgresql+asyncpg` driver — no new DB driver needed). Rewrite each of the 6 `@mcp.tool()` functions to build its query with `select()`/`func`/`case`/`.ilike()`/`.concat()` instead of hand-built SQL strings. One function, `get_store_monthly_revenue`, uses Postgres-only `DATE_TRUNC`; since SQLAlchemy Core has no portable equivalent, that one aggregates in Python instead (fetch `payment_date`+`amount` rows, bucket by month client-side) — documented here so this isn't a placeholder decision.

**Tech Stack:** Python 3.13, SQLAlchemy 2.x (`sqlalchemy[asyncio]`), asyncpg (unchanged as the underlying DBAPI driver — only the query-building layer changes), pytest + pytest-asyncio (existing integration suite against a live `sakila` DB is the regression gate).

**Spec:** None — this plan was written directly per the session's Superpowers policy (large-scope work goes straight to `writing-plans`, brainstorming skipped). Rationale and scope are captured inline in this header instead of a separate spec doc.

## Global Constraints

- Do not change any `@mcp.tool()` function's name, signature, docstring, or return shape — `tests/test_store_tools.py` asserts exact dict keys and existing MCP tool consumers (the LangGraph agent) depend on stable tool contracts.
- `tests/test_store_tools.py` runs unmodified as the acceptance gate for every task — it requires a live PostgreSQL `sakila` DB on `localhost:5432` (per `genai-assistant/tests/conftest.py`); do not mock around it.
- Scope is `store_server.py` only — do not touch `film_server.py`, `analytics_server.py`, `rag.py`, or `sessions.py` in this plan (they keep raw asyncpg for now; separate future work).
- New dependency: add `"sqlalchemy[asyncio]>=2.0"` to `genai-assistant/pyproject.toml`'s `dependencies` list. No other dependency changes.
- Preserve the existing case-insensitive category/email matching behavior exactly (`ILIKE` semantics) — use SQLAlchemy's `.ilike()`, which compiles to `ILIKE` on Postgres and to `LOWER(x) LIKE LOWER(y)` on dialects without native `ILIKE` (e.g. MSSQL) — this is the actual portability payoff to call out when this POC is reviewed.

---

## File Structure

- Create: `genai-assistant/src/store_tables.py` — SQLAlchemy Core `Table` declarations for `store`, `staff`, `address`, `city`, `film`, `category`, `film_category`, `inventory`, `rental`, `customer`, `payment`. Pure metadata, no logic.
- Modify: `genai-assistant/src/store_server.py` — replace `_pool`/`_get_pool()`/raw SQL bodies with `_engine`/`_get_engine()` and Core `select()` queries.
- Modify: `genai-assistant/tests/conftest.py` — the `inject_pool` fixture currently injects a shared asyncpg pool into `store_server._pool`; it must instead create a SQLAlchemy `AsyncEngine` and inject it into `store_server._engine`, alongside the unchanged asyncpg pool injection for `film_server`/`analytics_server`.
- Modify: `genai-assistant/pyproject.toml` — add the `sqlalchemy[asyncio]` dependency.

---

### Task 1: Add SQLAlchemy dependency

**Files:**
- Modify: `genai-assistant/pyproject.toml`

**Interfaces:**
- Produces: `sqlalchemy` (with `asyncio` extra, providing `sqlalchemy.ext.asyncio.create_async_engine`) importable from any module in `genai-assistant/src`.

- [ ] **Step 1: Add the dependency**

In `genai-assistant/pyproject.toml`, in the `[project]` `dependencies` list, add (keep alphabetical position — after `python-dotenv`, before `sse-starlette`):

```toml
    "sqlalchemy[asyncio]>=2.0",
```

- [ ] **Step 2: Sync the environment**

Run: `cd genai-assistant && uv sync`
Expected: resolves and installs `sqlalchemy` with no dependency conflicts.

- [ ] **Step 3: Verify import**

Run: `cd genai-assistant && uv run python -c "from sqlalchemy.ext.asyncio import create_async_engine; from sqlalchemy import select; print('ok')"`
Expected: prints `ok`.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/pyproject.toml genai-assistant/uv.lock
git commit -m "chore(genai-assistant): add sqlalchemy[asyncio] dependency"
```

---

### Task 2: Declare Core Table metadata for store_server's tables

**Files:**
- Create: `genai-assistant/src/store_tables.py`

**Interfaces:**
- Produces: module-level `Table` objects — `store`, `staff`, `address`, `city`, `film`, `category`, `film_category`, `inventory`, `rental`, `customer`, `payment` — each with the columns listed below, importable as `from store_tables import store, staff, address, city, film, category, film_category, inventory, rental, customer, payment`.

- [ ] **Step 1: Write the table metadata**

```python
"""SQLAlchemy Core table metadata for the tables store_server.py queries.

Plain Table declarations (not reflected, not ORM-mapped) — store_server's
tools are functional read queries, not domain CRUD, so Core's expression
language is enough without an ORM mapping layer.
"""
from sqlalchemy import Column, DateTime, Integer, MetaData, Numeric, String, Table

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

film = Table(
    "film",
    metadata,
    Column("film_id", Integer, primary_key=True),
    Column("title", String),
    Column("rating", String),
    Column("rental_rate", Numeric),
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

- [ ] **Step 2: Verify it imports cleanly**

Run: `cd genai-assistant && uv run python -c "import sys; sys.path.insert(0, 'src'); import store_tables; print(len(store_tables.metadata.tables))"`
Expected: prints `11`.

- [ ] **Step 3: Commit**

```bash
git add genai-assistant/src/store_tables.py
git commit -m "feat(genai-assistant): add SQLAlchemy Core table metadata for store_server"
```

---

### Task 3: Swap the connection layer and convert `list_stores`

**Files:**
- Modify: `genai-assistant/src/store_server.py`

**Interfaces:**
- Consumes: `store`, `staff`, `address`, `city` from `store_tables` (Task 2).
- Produces: `async def _get_engine() -> AsyncEngine` — module-level lazy singleton, mirrors the old `_get_pool()` shape so later tasks in this file can call it the same way.

- [ ] **Step 1: Run the baseline test to confirm it currently passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestListStores -v`
Expected: PASS (4 tests) — this is the safety net; it must still pass after the rewrite.

- [ ] **Step 2: Replace the pool with an engine and convert `list_stores`**

In `genai-assistant/src/store_server.py`, replace the top of the file (imports through `_get_pool`) with:

```python
import os

from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from store_tables import address, city, staff, store

load_dotenv()

mcp = FastMCP("PagilaStores")

_engine: AsyncEngine | None = None


def _sqlalchemy_url() -> str:
    user = os.getenv("DB_USER", "postgres")
    password = os.getenv("DB_PASSWORD", "password")
    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "5432")
    name = os.getenv("DB_NAME", "sakila")
    return f"postgresql+asyncpg://{user}:{password}@{host}:{port}/{name}"


async def _get_engine() -> AsyncEngine:
    global _engine
    if _engine is None:
        _engine = create_async_engine(_sqlalchemy_url(), pool_size=1, max_overflow=4)
    return _engine
```

Then replace the `list_stores` function body:

```python
@mcp.tool()
async def list_stores() -> list[dict]:
    """
    List all stores with store ID, manager name, address, city, and district.
    Use this as the starting point for any store-related query to discover store IDs.
    """
    engine = await _get_engine()
    stmt = (
        select(
            store.c.store_id,
            staff.c.first_name.concat(" ").concat(staff.c.last_name).label("manager"),
            address.c.address,
            address.c.district,
            city.c.city,
        )
        .select_from(store)
        .join(staff, store.c.manager_staff_id == staff.c.staff_id)
        .join(address, store.c.address_id == address.c.address_id)
        .join(city, address.c.city_id == city.c.city_id)
        .order_by(store.c.store_id)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

Leave the other 5 `@mcp.tool()` functions untouched for now (still using `pool = await _get_pool()`) — this task deliberately leaves the file in a mixed state; Task 9 removes `_get_pool` entirely once every function is converted. To keep this task compiling standalone, temporarily keep a `_get_pool()` shim below the new code:

```python
import asyncpg

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
```

(Put the `import asyncpg` line back at the top of the file alongside the other imports rather than inline — inline shown here only to mark what's temporary.)

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestListStores -v`
Expected: PASS (4 tests), same as Step 1.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): convert list_stores to SQLAlchemy Core"
```

---

### Task 4: Convert `get_store_inventory`

**Files:**
- Modify: `genai-assistant/src/store_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 3), `film`, `category`, `film_category`, `inventory`, `rental` from `store_tables`.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreInventory -v`
Expected: PASS (8 tests).

- [ ] **Step 2: Replace the function body**

Add to the `store_tables` import line: `film, category as category_t, film_category, inventory, rental` (rename `category` on import since the function's own parameter is also named `category`):

```python
from store_tables import (
    address,
    category as category_t,
    city,
    film,
    film_category,
    inventory,
    rental,
    staff,
    store,
)
```

Replace `get_store_inventory`:

```python
@mcp.tool()
async def get_store_inventory(store_id: int, category: str = "", limit: int = 20) -> list[dict]:
    """
    List films stocked at a specific store with total copies and available copies.
    Optionally filter by category. Use this to answer 'what can I rent at store X?'
    """
    engine = await _get_engine()
    stmt = (
        select(
            film.c.film_id,
            film.c.title,
            film.c.rating,
            film.c.rental_rate,
            category_t.c.name.label("category"),
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
        .select_from(film)
        .join(film_category, film.c.film_id == film_category.c.film_id)
        .join(category_t, film_category.c.category_id == category_t.c.category_id)
        .join(
            inventory,
            and_(film.c.film_id == inventory.c.film_id, inventory.c.store_id == store_id),
        )
        .outerjoin(
            rental,
            and_(inventory.c.inventory_id == rental.c.inventory_id, rental.c.return_date.is_(None)),
        )
        .group_by(film.c.film_id, film.c.title, film.c.rating, film.c.rental_rate, category_t.c.name)
        .order_by(film.c.title)
        .limit(limit)
    )
    if category:
        stmt = stmt.where(category_t.c.name.ilike(category))
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

Add `case, and_, or_, func` to the `sqlalchemy` import:

```python
from sqlalchemy import and_, case, func, or_, select
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreInventory -v`
Expected: PASS (8 tests), including `test_category_filter_case_insensitive` — this is the `.ilike()` portability check.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): convert get_store_inventory to SQLAlchemy Core"
```

---

### Task 5: Convert `get_store_rentals`

**Files:**
- Modify: `genai-assistant/src/store_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 3), `rental`, `inventory`, `film`, `customer` from `store_tables`.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreRentals -v`
Expected: PASS (6 tests).

- [ ] **Step 2: Replace the function body**

Add `customer` to the `store_tables` import line. Replace `get_store_rentals`:

```python
@mcp.tool()
async def get_store_rentals(store_id: int, limit: int = 20) -> list[dict]:
    """
    Recent rental activity at a store — film title, customer name, rental date,
    and whether the item has been returned. Use to check store throughput or find
    outstanding rentals.
    """
    engine = await _get_engine()
    stmt = (
        select(
            rental.c.rental_id,
            film.c.title,
            customer.c.first_name.concat(" ").concat(customer.c.last_name).label("customer"),
            customer.c.email.label("customer_email"),
            rental.c.rental_date,
            rental.c.return_date,
            case((rental.c.return_date.is_(None), True), else_=False).label("is_outstanding"),
        )
        .select_from(rental)
        .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
        .join(film, inventory.c.film_id == film.c.film_id)
        .join(customer, rental.c.customer_id == customer.c.customer_id)
        .where(inventory.c.store_id == store_id)
        .order_by(rental.c.rental_date.desc())
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreRentals -v`
Expected: PASS (6 tests), including `test_is_outstanding_is_boolean`.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): convert get_store_rentals to SQLAlchemy Core"
```

---

### Task 6: Convert `get_store_top_customers`

**Files:**
- Modify: `genai-assistant/src/store_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 3), `customer`, `rental`, `inventory`, `payment` from `store_tables`.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreTopCustomers -v`
Expected: PASS (5 tests).

- [ ] **Step 2: Replace the function body**

Add `payment` to the `store_tables` import line. Replace `get_store_top_customers`:

```python
@mcp.tool()
async def get_store_top_customers(store_id: int, limit: int = 10) -> list[dict]:
    """
    Customers who rent most frequently from a specific store, ranked by rental count
    and total spend. Use to identify loyal or high-value customers at a location.
    """
    engine = await _get_engine()
    rental_count = func.count(rental.c.rental_id)
    total_spent = func.coalesce(func.sum(payment.c.amount), 0)
    stmt = (
        select(
            customer.c.customer_id,
            customer.c.first_name.concat(" ").concat(customer.c.last_name).label("customer"),
            customer.c.email,
            rental_count.label("rental_count"),
            total_spent.label("total_spent"),
        )
        .select_from(customer)
        .join(rental, customer.c.customer_id == rental.c.customer_id)
        .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
        .outerjoin(payment, rental.c.rental_id == payment.c.rental_id)
        .where(inventory.c.store_id == store_id)
        .group_by(customer.c.customer_id, customer.c.first_name, customer.c.last_name, customer.c.email)
        .order_by(rental_count.desc(), total_spent.desc())
        .limit(limit)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        return [dict(row) for row in result.mappings()]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreTopCustomers -v`
Expected: PASS (5 tests).

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): convert get_store_top_customers to SQLAlchemy Core"
```

---

### Task 7: Convert `get_customer_store_payments`

**Files:**
- Modify: `genai-assistant/src/store_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 3), `customer`, `payment`, `rental`, `inventory`, `film` from `store_tables`.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetCustomerStorePayments -v`
Expected: PASS (7 tests).

- [ ] **Step 2: Replace the function body**

```python
@mcp.tool()
async def get_customer_store_payments(customer_email: str, store_id: int) -> dict:
    """
    Full payment history for a customer at a specific store — each payment with
    amount, date, and which film it was for. Use when a customer has questions
    about their account at a particular location.
    """
    engine = await _get_engine()
    customer_stmt = select(
        customer.c.customer_id,
        customer.c.first_name.concat(" ").concat(customer.c.last_name).label("full_name"),
        customer.c.email,
    ).where(customer.c.email.ilike(customer_email))

    async with engine.connect() as conn:
        customer_row = (await conn.execute(customer_stmt)).mappings().first()
        if not customer_row:
            return {"error": f"No customer found with email matching '{customer_email}'"}

        payments_stmt = (
            select(
                payment.c.payment_id,
                payment.c.amount,
                payment.c.payment_date,
                film.c.title.label("film"),
                rental.c.rental_date,
                rental.c.return_date,
            )
            .select_from(payment)
            .join(rental, payment.c.rental_id == rental.c.rental_id)
            .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
            .join(film, inventory.c.film_id == film.c.film_id)
            .where(payment.c.customer_id == customer_row["customer_id"], inventory.c.store_id == store_id)
            .order_by(payment.c.payment_date.desc())
        )
        rows = [dict(r) for r in (await conn.execute(payments_stmt)).mappings()]

    total = sum(r["amount"] for r in rows)
    return {
        "customer_id": customer_row["customer_id"],
        "name": customer_row["full_name"],
        "email": customer_row["email"],
        "store_id": store_id,
        "payment_count": len(rows),
        "total_paid": float(total),
        "payments": rows,
    }
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetCustomerStorePayments -v`
Expected: PASS (7 tests), including `test_case_insensitive_email`.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): convert get_customer_store_payments to SQLAlchemy Core"
```

---

### Task 8: Convert `get_store_monthly_revenue` (Python-side month bucketing)

**Files:**
- Modify: `genai-assistant/src/store_server.py`

**Interfaces:**
- Consumes: `_get_engine()` (Task 3), `payment`, `rental`, `inventory` from `store_tables`.

**Why this one differs:** the original query uses Postgres's `DATE_TRUNC('month', ...)`, which SQLAlchemy Core has no dialect-portable equivalent for (there is no generic `func.date_trunc` translation table across backends). Rather than leave a raw-SQL escape hatch, this task fetches the raw `(payment_date, amount)` pairs through Core and buckets them by month in Python — genuinely portable, and the sakila `payment` table per store is small enough (~8k rows total) that this is not a performance concern.

- [ ] **Step 1: Run the baseline test**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreMonthlyRevenue -v`
Expected: PASS (6 tests).

- [ ] **Step 2: Replace the function body**

```python
@mcp.tool()
async def get_store_monthly_revenue(store_id: int) -> list[dict]:
    """
    Month-by-month revenue breakdown for a store, ordered most-recent first.
    Use to spot trends — busy months, slow periods, or year-over-year patterns.
    """
    engine = await _get_engine()
    stmt = (
        select(payment.c.payment_date, payment.c.amount)
        .select_from(payment)
        .join(rental, payment.c.rental_id == rental.c.rental_id)
        .join(inventory, rental.c.inventory_id == inventory.c.inventory_id)
        .where(inventory.c.store_id == store_id)
    )
    async with engine.connect() as conn:
        result = await conn.execute(stmt)
        rows = result.all()

    buckets: dict = {}
    for payment_date, amount in rows:
        month_key = payment_date.date().replace(day=1)
        bucket = buckets.setdefault(month_key, {"transaction_count": 0, "revenue": 0})
        bucket["transaction_count"] += 1
        bucket["revenue"] += amount

    return [
        {"month": month, **data}
        for month, data in sorted(buckets.items(), reverse=True)
    ]
```

- [ ] **Step 3: Run the test again to confirm it still passes**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py::TestGetStoreMonthlyRevenue -v`
Expected: PASS (6 tests), including `test_sorted_most_recent_first`.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/store_server.py
git commit -m "refactor(genai-assistant): convert get_store_monthly_revenue to SQLAlchemy Core + Python bucketing"
```

---

### Task 9: Remove the leftover asyncpg pool, update the test fixture, run the full suite

**Files:**
- Modify: `genai-assistant/src/store_server.py`
- Modify: `genai-assistant/tests/conftest.py`

**Interfaces:**
- Consumes: `_engine`/`_get_engine()` (Task 3, now the only connection mechanism in the file).
- Produces: none further — this closes out the file's migration.

- [ ] **Step 1: Remove the leftover `_get_pool` shim**

In `genai-assistant/src/store_server.py`, delete the `import asyncpg` line, the `_pool: asyncpg.Pool | None = None` line, and the `_get_pool()` function added as a temporary shim in Task 3 (by this point every `@mcp.tool()` function uses `_get_engine()` instead).

- [ ] **Step 2: Update the test fixture to inject the engine instead of a pool**

In `genai-assistant/tests/conftest.py`, replace the fixture:

```python
import os
import sys

import asyncpg
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
    min_size=1,
    max_size=3,
)


@pytest_asyncio.fixture(autouse=True)
async def inject_pool():
    """Create a connection pool/engine on this test's event loop and inject into all MCP servers."""
    import analytics_server
    import film_server
    import store_server

    pool = await asyncpg.create_pool(**_DB)
    film_server._pool = pool
    analytics_server._pool = pool

    engine = create_async_engine(
        f"postgresql+asyncpg://{_DB['user']}:{_DB['password']}@{_DB['host']}:{_DB['port']}/{_DB['database']}",
        pool_size=1,
        max_overflow=2,
    )
    store_server._engine = engine

    yield

    film_server._pool = None
    analytics_server._pool = None
    store_server._engine = None
    await pool.close()
    await engine.dispose()
```

- [ ] **Step 3: Run the full store test file**

Run: `cd genai-assistant && uv run pytest tests/test_store_tools.py -v`
Expected: PASS, all 36 tests.

- [ ] **Step 4: Run the full test suite to confirm no cross-file breakage**

Run: `cd genai-assistant && uv run pytest -v`
Expected: PASS, no regressions in `test_film_tools.py`, `test_analytics_tools.py`, `test_rental_tools.py`, `test_actor_tools.py`, `test_mcp.py`, `test_api.py`.

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/store_server.py genai-assistant/tests/conftest.py
git commit -m "refactor(genai-assistant): drop asyncpg pool from store_server, inject SQLAlchemy engine in tests"
```

---

## Outcome / Known Gaps (for whoever reviews this POC)

- `store_server.py` is now fully SQLAlchemy Core — no raw SQL strings, no `ILIKE`/`::text`/`||`/`DATE_TRUNC` left in this file.
- `film_server.py`, `analytics_server.py`, `rag.py`, `sessions.py` are unchanged and still Postgres-coupled (out of scope here) — same conversion pattern applies if this POC is accepted.
- Two structural blockers remain regardless of query layer and are **not** addressed by this plan: `rag.py`'s `pgvector` dependency, and `sessions.py`'s use of LangGraph's `AsyncPostgresSaver` (Postgres-specific checkpoint backend).
