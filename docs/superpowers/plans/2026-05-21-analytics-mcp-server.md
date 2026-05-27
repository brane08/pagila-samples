# Analytics MCP Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `analytics_server.py` MCP server with four tools (overdue rentals, slow-moving inventory, revenue summary, store comparison), wire it into the LangGraph agent, and cover it with live-DB pytest tests.

**Architecture:** A third FastMCP server following the exact same pattern as `store_server.py` — lazy asyncpg pool, four `@mcp.tool()` functions, spawned via stdio. It gets registered in `agent.py`'s `MultiServerMCPClient` alongside the existing two servers; no graph changes needed.

**Tech Stack:** Python 3.13, FastMCP, asyncpg, LangGraph / langchain-mcp-adapters, pytest-asyncio (asyncio_mode=auto)

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `genai-assistant/src/analytics_server.py` | Create | Four analytics MCP tools |
| `genai-assistant/tests/conftest.py` | Modify | Inject pool into analytics_server |
| `genai-assistant/tests/test_analytics_tools.py` | Create | Live-DB tests for all four tools |
| `genai-assistant/src/agent.py` | Modify | Add analytics server to MultiServerMCPClient; update SYSTEM_PROMPT |

---

## Task 1: Create analytics_server.py skeleton and patch conftest.py

**Files:**
- Create: `genai-assistant/src/analytics_server.py`
- Modify: `genai-assistant/tests/conftest.py`

- [ ] **Step 1: Create the skeleton server**

Create `genai-assistant/src/analytics_server.py` with stub implementations:

```python
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
    """Rentals not yet returned that are past their rental_duration deadline."""
    raise NotImplementedError


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
```

- [ ] **Step 2: Patch conftest.py to inject the analytics pool**

The existing `conftest.py` is at `genai-assistant/tests/conftest.py`. Add `analytics_server` to the `inject_pool` fixture so its `_pool` is set before each test:

```python
import os
import sys

import asyncpg
import pytest_asyncio
from dotenv import load_dotenv

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
    """Create an asyncpg pool on this test's event loop and inject it into all MCP servers."""
    import analytics_server
    import film_server
    import store_server
    pool = await asyncpg.create_pool(**_DB)
    film_server._pool = pool
    store_server._pool = pool
    analytics_server._pool = pool
    yield
    film_server._pool = None
    store_server._pool = None
    analytics_server._pool = None
    await pool.close()
```

- [ ] **Step 3: Verify the import works (no DB needed yet)**

```bash
cd genai-assistant && uv run python -c "import sys; sys.path.insert(0,'src'); import analytics_server; print('OK')"
```

Expected: `OK`

---

## Task 2: Write the full test file and verify tests fail

**Files:**
- Create: `genai-assistant/tests/test_analytics_tools.py`

- [ ] **Step 1: Write test_analytics_tools.py**

Create `genai-assistant/tests/test_analytics_tools.py`:

```python
"""Tests for the 4 analytics MCP tools. Requires PostgreSQL on localhost:5432."""
import pytest
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
        row = result["by_store"][0]
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
```

- [ ] **Step 2: Run all analytics tests to verify they fail**

```bash
cd genai-assistant && uv run pytest tests/test_analytics_tools.py -v 2>&1 | head -40
```

Expected: All tests fail with `NotImplementedError`.

---

## Task 3: Implement get_overdue_rentals

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

- [ ] **Step 1: Replace the get_overdue_rentals stub with the real implementation**

Replace only the `get_overdue_rentals` function body (keep the `@mcp.tool()` decorator):

```python
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
```

- [ ] **Step 2: Run TestOverdueRentals tests**

```bash
cd genai-assistant && uv run pytest tests/test_analytics_tools.py::TestOverdueRentals -v
```

Expected: All 6 tests pass.

- [ ] **Step 3: Commit**

```bash
git add genai-assistant/src/analytics_server.py genai-assistant/tests/conftest.py genai-assistant/tests/test_analytics_tools.py
git commit -m "feat(analytics): add get_overdue_rentals MCP tool with tests"
```

---

## Task 4: Implement get_slow_moving_films

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

- [ ] **Step 1: Replace the get_slow_moving_films stub**

```python
@mcp.tool()
async def get_slow_moving_films(store_id: int | None = None, days: int = 90, limit: int = 20) -> list[dict]:
    """
    Films present in inventory that have had zero rentals in the last `days` days,
    or have never been rented. Ordered by days_since_rented descending (never-rented first).
    Optionally filter by store_id.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        args: list = [days]
        where = ""
        if store_id is not None:
            args.append(store_id)
            where = f"WHERE i.store_id = ${len(args)}"
        args.append(limit)
        limit_param = f"${len(args)}"

        query = f"""
            SELECT f.film_id,
                   f.title,
                   f.rating::text AS rating,
                   i.store_id,
                   COUNT(DISTINCT i.inventory_id)::int AS copies_in_stock,
                   MAX(r.rental_date)::date AS last_rented,
                   CASE WHEN MAX(r.rental_date) IS NOT NULL
                        THEN EXTRACT(DAY FROM NOW() - MAX(r.rental_date))::int
                        ELSE NULL END AS days_since_rented
            FROM inventory i
            JOIN film f ON i.film_id = f.film_id
            LEFT JOIN rental r ON i.inventory_id = r.inventory_id
            {where}
            GROUP BY f.film_id, f.title, f.rating, i.store_id
            HAVING MAX(r.rental_date) < NOW() - ($1 * INTERVAL '1 day')
                OR MAX(r.rental_date) IS NULL
            ORDER BY days_since_rented DESC NULLS FIRST
            LIMIT {limit_param}
        """
        rows = await conn.fetch(query, *args)
        return [dict(r) for r in rows]
```

- [ ] **Step 2: Run TestSlowMovingFilms tests**

```bash
cd genai-assistant && uv run pytest tests/test_analytics_tools.py::TestSlowMovingFilms -v
```

Expected: All 7 tests pass.

- [ ] **Step 3: Commit**

```bash
git add genai-assistant/src/analytics_server.py
git commit -m "feat(analytics): add get_slow_moving_films MCP tool"
```

---

## Task 5: Implement get_revenue_summary

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

- [ ] **Step 1: Replace the get_revenue_summary stub**

```python
@mcp.tool()
async def get_revenue_summary() -> dict:
    """
    Global revenue totals across all stores: total revenue, total rentals, average per rental,
    busiest month (YYYY-MM), and a per-store revenue breakdown.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        global_row = await conn.fetchrow("""
            WITH pay AS (
                SELECT p.amount, p.payment_date, i.store_id
                FROM payment p
                JOIN rental r ON p.rental_id = r.rental_id
                JOIN inventory i ON r.inventory_id = i.inventory_id
            ),
            monthly AS (
                SELECT DATE_TRUNC('month', payment_date) AS month,
                       SUM(amount) AS month_revenue
                FROM pay
                GROUP BY DATE_TRUNC('month', payment_date)
                ORDER BY month_revenue DESC
                LIMIT 1
            )
            SELECT COUNT(pay.*)::int AS total_rentals,
                   ROUND(SUM(pay.amount)::numeric, 2) AS total_revenue,
                   ROUND(AVG(pay.amount)::numeric, 2) AS avg_per_rental,
                   TO_CHAR(m.month, 'YYYY-MM') AS busiest_month,
                   ROUND(m.month_revenue::numeric, 2) AS busiest_month_revenue
            FROM pay, monthly m
            GROUP BY m.month, m.month_revenue
        """)

        store_rows = await conn.fetch("""
            SELECT i.store_id,
                   ROUND(SUM(p.amount)::numeric, 2) AS revenue,
                   COUNT(p.payment_id)::int AS rental_count,
                   ROUND(AVG(p.amount)::numeric, 2) AS avg_per_rental
            FROM payment p
            JOIN rental r ON p.rental_id = r.rental_id
            JOIN inventory i ON r.inventory_id = i.inventory_id
            GROUP BY i.store_id
            ORDER BY i.store_id
        """)

        return {
            "total_revenue": float(global_row["total_revenue"]),
            "total_rentals": global_row["total_rentals"],
            "avg_per_rental": float(global_row["avg_per_rental"]),
            "busiest_month": global_row["busiest_month"],
            "busiest_month_revenue": float(global_row["busiest_month_revenue"]),
            "by_store": [
                {
                    "store_id": r["store_id"],
                    "revenue": float(r["revenue"]),
                    "rental_count": r["rental_count"],
                    "avg_per_rental": float(r["avg_per_rental"]),
                }
                for r in store_rows
            ],
        }
```

- [ ] **Step 2: Run TestRevenueSummary tests**

```bash
cd genai-assistant && uv run pytest tests/test_analytics_tools.py::TestRevenueSummary -v
```

Expected: All 9 tests pass.

- [ ] **Step 3: Commit**

```bash
git add genai-assistant/src/analytics_server.py
git commit -m "feat(analytics): add get_revenue_summary MCP tool"
```

---

## Task 6: Implement get_store_comparison

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`

- [ ] **Step 1: Replace the get_store_comparison stub**

```python
@mcp.tool()
async def get_store_comparison() -> list[dict]:
    """
    Side-by-side key metrics for both stores: manager, city, total revenue, rental count,
    unique customers, average rental rate across stocked inventory, and outstanding rentals.
    One row per store, ordered by store_id.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch("""
            SELECT s.store_id,
                   st.first_name || ' ' || st.last_name AS manager,
                   ci.city,
                   ROUND(COALESCE(SUM(p.amount), 0)::numeric, 2) AS total_revenue,
                   COUNT(DISTINCT r.rental_id)::int AS rental_count,
                   COUNT(DISTINCT r.customer_id)::int AS unique_customers,
                   ROUND(AVG(f.rental_rate)::numeric, 2) AS avg_rental_rate,
                   COUNT(DISTINCT CASE WHEN r.return_date IS NULL
                                       THEN r.rental_id END)::int AS outstanding_rentals
            FROM store s
            JOIN staff st ON s.manager_staff_id = st.staff_id
            JOIN address a ON s.address_id = a.address_id
            JOIN city ci ON a.city_id = ci.city_id
            LEFT JOIN inventory i ON s.store_id = i.store_id
            LEFT JOIN film f ON i.film_id = f.film_id
            LEFT JOIN rental r ON i.inventory_id = r.inventory_id
            LEFT JOIN payment p ON r.rental_id = p.rental_id
            GROUP BY s.store_id, st.first_name, st.last_name, ci.city
            ORDER BY s.store_id
        """)
        return [
            {**dict(r), "total_revenue": float(r["total_revenue"]),
             "avg_rental_rate": float(r["avg_rental_rate"])}
            for r in rows
        ]
```

- [ ] **Step 2: Run TestStoreComparison tests**

```bash
cd genai-assistant && uv run pytest tests/test_analytics_tools.py::TestStoreComparison -v
```

Expected: All 8 tests pass.

- [ ] **Step 3: Run the full analytics test file to confirm everything green**

```bash
cd genai-assistant && uv run pytest tests/test_analytics_tools.py -v
```

Expected: All 30 tests pass.

- [ ] **Step 4: Commit**

```bash
git add genai-assistant/src/analytics_server.py
git commit -m "feat(analytics): add get_store_comparison MCP tool"
```

---

## Task 7: Wire analytics_server into agent.py

**Files:**
- Modify: `genai-assistant/src/agent.py`

- [ ] **Step 1: Add pagila_analytics to MultiServerMCPClient in build_agent()**

In `agent.py`, the `MultiServerMCPClient` dict starts at line ~117. Add a third entry:

```python
    client = MultiServerMCPClient({
        "pagila_films": {
            "command": "uv",
            "args": ["run", "src/film_server.py"],
            "transport": "stdio",
        },
        "pagila_stores": {
            "command": "uv",
            "args": ["run", "src/store_server.py"],
            "transport": "stdio",
        },
        "pagila_analytics": {
            "command": "uv",
            "args": ["run", "src/analytics_server.py"],
            "transport": "stdio",
        },
    })
```

- [ ] **Step 2: Update SYSTEM_PROMPT tool count and add Analytics section**

In `SYSTEM_PROMPT`, change `"20 tools across two servers"` to `"24 tools across three servers"`.

Then append a new `## Analytics` section before `## Multi-step chains`:

```python
## Analytics
- "Are any rentals overdue?" / "what hasn't been returned?" → get_overdue_rentals; \
  add store_id if a specific store is mentioned
- "Slow movers" / "dead stock" / "what's not being rented?" → get_slow_moving_films; \
  increase days= if user wants a longer window
- "How is the business doing?" / "total revenue" / "overall performance" → get_revenue_summary
- "Compare the two stores" (high-level snapshot) → get_store_comparison; \
  use get_store_monthly_revenue for month-by-month detail on a specific store
```

- [ ] **Step 3: Verify the agent module imports cleanly**

```bash
cd genai-assistant && uv run python -c "import sys; sys.path.insert(0,'src'); import agent; print('OK')"
```

Expected: `OK` (no import errors).

- [ ] **Step 4: Run the full test suite to confirm no regressions**

```bash
cd genai-assistant && uv run pytest tests/test_api.py tests/test_analytics_tools.py -v
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add genai-assistant/src/agent.py
git commit -m "feat(analytics): wire analytics_server into LangGraph agent"
```
