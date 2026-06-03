# Analytics Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /ui/analytics` — a static Bootstrap 5 dashboard showing revenue cards, store comparison, category bar chart, and overdue/slow-moving film tables.

**Architecture:** Extract SQL from `analytics_server.py` and `film_server.py` into a new `analytics_queries.py` module. The UI route calls those functions directly (no MCP subprocess). Chart.js v4 renders the category chart client-side from JSON embedded in the template.

**Tech Stack:** FastAPI + Jinja2 · asyncpg · Bootstrap 5.3 · Chart.js v4 · pytest + AsyncMock

---

## File Map

| Action | Path | Purpose |
|--------|------|---------|
| Create | `src/analytics_queries.py` | 5 async query functions that accept an asyncpg pool |
| Modify | `src/analytics_server.py` | Import & delegate to analytics_queries (removes duplicated SQL) |
| Modify | `src/film_server.py` | Import rental_stats_by_category from analytics_queries |
| Modify | `src/ui_routes.py` | Add `GET /ui/analytics` route + imports |
| Create | `src/templates/analytics.html` | Dashboard template |
| Modify | `src/templates/chat.html` | Restructure sidebar to add Analytics nav link |
| Create | `src/static/vendor/chart.min.js` | Vendored Chart.js v4 |
| Create | `tests/test_analytics_queries.py` | Unit tests for each query function (mock pool) |
| Modify | `tests/test_api.py` | Add `TestAnalyticsRoute` class |

---

## Task 1: `analytics_queries.py` — shared SQL functions

**Files:**
- Create: `genai-assistant/src/analytics_queries.py`
- Test: `genai-assistant/tests/test_analytics_queries.py`

Working directory for all commands: `genai-assistant/`

- [ ] **Step 1: Write the failing tests**

```python
# tests/test_analytics_queries.py
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
```

- [ ] **Step 2: Run tests — expect ImportError (module doesn't exist yet)**

```
uv run pytest tests/test_analytics_queries.py -v
```

Expected: `ModuleNotFoundError: No module named 'analytics_queries'`

- [ ] **Step 3: Create `src/analytics_queries.py`**

```python
# src/analytics_queries.py
import asyncpg


async def revenue_summary(pool: asyncpg.Pool) -> dict:
    async with pool.acquire() as conn:
        global_row = await conn.fetchrow("""
            WITH pay AS (
                SELECT p.amount, p.payment_date, i.store_id
                FROM payment p
                JOIN rental r ON p.rental_id = r.rental_id
                JOIN inventory i ON r.inventory_id = i.inventory_id
            )
            SELECT COUNT(*)::int AS total_rentals,
                   ROUND(SUM(amount)::numeric, 2) AS total_revenue,
                   ROUND(AVG(amount)::numeric, 2) AS avg_per_rental,
                   (SELECT TO_CHAR(DATE_TRUNC('month', payment_date), 'YYYY-MM')
                    FROM pay
                    GROUP BY DATE_TRUNC('month', payment_date)
                    ORDER BY SUM(amount) DESC
                    LIMIT 1) AS busiest_month,
                   (SELECT ROUND(SUM(amount)::numeric, 2)
                    FROM pay
                    GROUP BY DATE_TRUNC('month', payment_date)
                    ORDER BY SUM(amount) DESC
                    LIMIT 1) AS busiest_month_revenue
            FROM pay
        """)
        if global_row is None or global_row["total_rentals"] == 0:
            return {
                "total_revenue": 0.0, "total_rentals": 0,
                "avg_per_rental": 0.0, "busiest_month": None,
                "busiest_month_revenue": 0.0, "by_store": [],
            }
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
            "busiest_month_revenue": float(global_row["busiest_month_revenue"] or 0.0),
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


async def store_comparison(pool: asyncpg.Pool) -> list[dict]:
    async with pool.acquire() as conn:
        rows = await conn.fetch("""
            SELECT s.store_id,
                   st.first_name || ' ' || st.last_name AS manager,
                   ci.city,
                   ROUND(COALESCE(SUM(p.amount), 0)::numeric, 2) AS total_revenue,
                   COUNT(DISTINCT r.rental_id)::int AS rental_count,
                   COUNT(DISTINCT r.customer_id)::int AS unique_customers,
                   COALESCE(ROUND(AVG(f.rental_rate)::numeric, 2), 0) AS avg_rental_rate,
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
            {
                **dict(r),
                "total_revenue": float(r["total_revenue"]),
                "avg_rental_rate": float(r["avg_rental_rate"]),
            }
            for r in rows
        ]


async def rental_stats_by_category(pool: asyncpg.Pool) -> list[dict]:
    async with pool.acquire() as conn:
        rows = await conn.fetch("""
            SELECT c.name AS category,
                   COUNT(r.rental_id)::int AS rental_count,
                   ROUND(COALESCE(SUM(p.amount), 0)::numeric, 2) AS total_revenue
            FROM category c
            JOIN film_category fc ON c.category_id = fc.category_id
            JOIN film f ON fc.film_id = f.film_id
            JOIN inventory i ON f.film_id = i.film_id
            JOIN rental r ON i.inventory_id = r.inventory_id
            LEFT JOIN payment p ON r.rental_id = p.rental_id
            GROUP BY c.name
            ORDER BY total_revenue DESC
        """)
        return [{**dict(r), "total_revenue": float(r["total_revenue"])} for r in rows]


async def overdue_rentals(pool: asyncpg.Pool, limit: int = 20) -> list[dict]:
    async with pool.acquire() as conn:
        rows = await conn.fetch("""
            SELECT r.rental_id, f.film_id, f.title,
                   cu.email AS customer_email, i.store_id,
                   r.rental_date::date AS rental_date,
                   (CURRENT_DATE - (r.rental_date::date + f.rental_duration))::int AS days_overdue
            FROM rental r
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
            JOIN customer cu ON r.customer_id = cu.customer_id
            WHERE r.return_date IS NULL
              AND CURRENT_DATE > r.rental_date::date + f.rental_duration
            ORDER BY days_overdue DESC
            LIMIT $1
        """, limit)
        return [dict(r) for r in rows]


async def slow_moving_films(pool: asyncpg.Pool, days: int = 90, limit: int = 20) -> list[dict]:
    async with pool.acquire() as conn:
        rows = await conn.fetch("""
            SELECT f.film_id, f.title, f.rating::text AS rating, i.store_id,
                   COUNT(DISTINCT i.inventory_id)::int AS copies_in_stock,
                   MAX(r.rental_date)::date AS last_rented,
                   CASE WHEN MAX(r.rental_date) IS NOT NULL
                        THEN (CURRENT_DATE - MAX(r.rental_date)::date)::int
                        ELSE NULL END AS days_since_rented
            FROM inventory i
            JOIN film f ON i.film_id = f.film_id
            LEFT JOIN rental r ON i.inventory_id = r.inventory_id
            GROUP BY f.film_id, f.title, f.rating, i.store_id
            HAVING MAX(r.rental_date) < NOW() - ($1 * INTERVAL '1 day')
                OR MAX(r.rental_date) IS NULL
            ORDER BY days_since_rented DESC NULLS FIRST
            LIMIT $2
        """, days, limit)
        return [dict(r) for r in rows]
```

- [ ] **Step 4: Run tests — expect all 6 to pass**

```
uv run pytest tests/test_analytics_queries.py -v
```

Expected: `6 passed`

- [ ] **Step 5: Commit**

```bash
git add src/analytics_queries.py tests/test_analytics_queries.py
git commit -m "feat: add analytics_queries module with 5 async SQL functions"
```

---

## Task 2: Refactor MCP servers to import from analytics_queries

**Files:**
- Modify: `genai-assistant/src/analytics_server.py`
- Modify: `genai-assistant/src/film_server.py`

No new tests — the existing `test_film_tools.py` and `test_store_tools.py` (live-DB tests) cover these MCP tools. This task only removes duplicated SQL.

- [ ] **Step 1: Replace `analytics_server.py` tool implementations**

Open `src/analytics_server.py`. Remove the SQL bodies from all 4 tool functions and replace the entire file content with:

```python
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
```

- [ ] **Step 2: Replace `rental_stats_by_category` in `film_server.py`**

Find the function `async def get_rental_stats_by_category()` (line ~333 in film_server.py). Replace only that function body. First add the import near the top of the file (after existing imports):

```python
from analytics_queries import rental_stats_by_category as _rental_stats_by_category
```

Then replace the function body:

```python
@mcp.tool()
async def get_rental_stats_by_category() -> list[dict]:
    """
    Rental count and total revenue per film category, ordered by revenue descending.
    Bridges film category data with actual rental business performance.
    """
    pool = await _get_pool()
    return await _rental_stats_by_category(pool)
```

- [ ] **Step 3: Verify import works**

```
uv run python -c "import analytics_server; import film_server; print('ok')"
```

Expected: `ok`

- [ ] **Step 4: Commit**

```bash
git add src/analytics_server.py src/film_server.py
git commit -m "refactor: delegate MCP analytics SQL to analytics_queries module"
```

---

## Task 3: `GET /ui/analytics` route

**Files:**
- Modify: `genai-assistant/src/ui_routes.py`
- Modify: `genai-assistant/tests/test_api.py`

- [ ] **Step 1: Write the failing tests**

Append `TestAnalyticsRoute` at the end of `tests/test_api.py` (after `TestUiRoutes`):

```python
class TestAnalyticsRoute:
    @pytest.mark.asyncio
    async def test_analytics_returns_200_html(self, client):
        fake_revenue = {
            "total_revenue": 67416.51, "total_rentals": 16049,
            "avg_per_rental": 4.20, "busiest_month": "2005-08",
            "busiest_month_revenue": 24072.13, "by_store": [],
        }
        with (
            patch("ui_routes.revenue_summary", AsyncMock(return_value=fake_revenue)),
            patch("ui_routes.store_comparison", AsyncMock(return_value=[])),
            patch("ui_routes.rental_stats_by_category", AsyncMock(return_value=[])),
            patch("ui_routes.overdue_rentals", AsyncMock(return_value=[])),
            patch("ui_routes.slow_moving_films", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/analytics")
        assert resp.status_code == 200
        assert "text/html" in resp.headers["content-type"]

    @pytest.mark.asyncio
    async def test_analytics_shows_revenue_total(self, client):
        fake_revenue = {
            "total_revenue": 67416.51, "total_rentals": 16049,
            "avg_per_rental": 4.20, "busiest_month": "2005-08",
            "busiest_month_revenue": 24072.13, "by_store": [],
        }
        with (
            patch("ui_routes.revenue_summary", AsyncMock(return_value=fake_revenue)),
            patch("ui_routes.store_comparison", AsyncMock(return_value=[])),
            patch("ui_routes.rental_stats_by_category", AsyncMock(return_value=[])),
            patch("ui_routes.overdue_rentals", AsyncMock(return_value=[])),
            patch("ui_routes.slow_moving_films", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/analytics")
        assert "67416.51" in resp.text

    @pytest.mark.asyncio
    async def test_analytics_has_chart_canvas(self, client):
        empty_rev = {
            "total_revenue": 0.0, "total_rentals": 0, "avg_per_rental": 0.0,
            "busiest_month": None, "busiest_month_revenue": 0.0, "by_store": [],
        }
        with (
            patch("ui_routes.revenue_summary", AsyncMock(return_value=empty_rev)),
            patch("ui_routes.store_comparison", AsyncMock(return_value=[])),
            patch("ui_routes.rental_stats_by_category", AsyncMock(return_value=[])),
            patch("ui_routes.overdue_rentals", AsyncMock(return_value=[])),
            patch("ui_routes.slow_moving_films", AsyncMock(return_value=[])),
            patch("ui_routes.get_asyncpg_pool", return_value=MagicMock()),
        ):
            resp = await client.get("/ui/analytics")
        assert "category-chart" in resp.text
```

- [ ] **Step 2: Run tests — expect 404 (route doesn't exist yet)**

```
uv run pytest tests/test_api.py::TestAnalyticsRoute -v
```

Expected: all 3 fail with `assert 404 == 200` or `AssertionError`

- [ ] **Step 3: Add route to `src/ui_routes.py`**

Add at the top of `ui_routes.py`, after the existing imports:

```python
import asyncio

from analytics_queries import (
    overdue_rentals,
    rental_stats_by_category,
    revenue_summary,
    slow_moving_films,
    store_comparison,
)
```

Add the route function at the end of `ui_routes.py`:

```python
@ui_router.get("/ui/analytics", response_class=HTMLResponse)
async def ui_analytics(request: Request):
    pool = get_asyncpg_pool()
    rev, stores, cats, overdues, slow = await asyncio.gather(
        revenue_summary(pool),
        store_comparison(pool),
        rental_stats_by_category(pool),
        overdue_rentals(pool),
        slow_moving_films(pool),
    )
    return templates.TemplateResponse(
        request,
        "analytics.html",
        {
            "revenue": rev,
            "stores": stores,
            "categories": cats,
            "overdue": overdues,
            "slow_films": slow,
        },
    )
```

- [ ] **Step 4: Run tests — expect all 3 to pass**

```
uv run pytest tests/test_api.py::TestAnalyticsRoute -v
```

Expected: `3 passed`

- [ ] **Step 5: Run full test suite to check for regressions**

```
uv run pytest tests/test_api.py tests/test_analytics_queries.py -v
```

Expected: all pass

- [ ] **Step 6: Commit**

```bash
git add src/ui_routes.py tests/test_api.py
git commit -m "feat: add GET /ui/analytics route"
```

---

## Task 4: Vendor Chart.js and create analytics.html

**Files:**
- Create: `genai-assistant/src/static/vendor/chart.min.js`
- Create: `genai-assistant/src/templates/analytics.html`

- [ ] **Step 1: Download Chart.js v4 minified**

```bash
curl -L "https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js" \
     -o genai-assistant/src/static/vendor/chart.min.js
```

Verify it downloaded (should be ~200KB):
```bash
ls -lh genai-assistant/src/static/vendor/chart.min.js
```

Expected: file exists, size ~190–220K

- [ ] **Step 2: Create `src/templates/analytics.html`**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Pagila Analytics</title>
  <link rel="stylesheet" href="/static/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="/static/vendor/bootstrap-icons.min.css">
  <link rel="stylesheet" href="/static/chat.css">
  <script src="/static/vendor/chart.min.js" defer></script>
</head>
<body class="bg-light">

<div class="d-flex vh-100 overflow-hidden">

  <!-- Sidebar -->
  <aside class="bg-white border-end d-flex flex-column flex-shrink-0" style="width:230px;min-width:180px;">
    <div class="p-2 border-bottom">
      <span class="small fw-semibold text-muted text-uppercase">Navigation</span>
    </div>
    <ul class="list-group list-group-flush">
      <li class="list-group-item border-0 p-0">
        <a href="/ui"
           class="list-group-item list-group-item-action d-flex align-items-center gap-2 px-2 py-2 border-0">
          <i class="bi bi-chat-left-text text-muted small"></i>
          <span class="small">Chat</span>
        </a>
      </li>
      <li class="list-group-item border-0 p-0">
        <a href="/ui/analytics"
           class="list-group-item list-group-item-action active d-flex align-items-center gap-2 px-2 py-2 border-0">
          <i class="bi bi-bar-chart-fill small"></i>
          <span class="small">Analytics</span>
        </a>
      </li>
    </ul>
  </aside>

  <!-- Main -->
  <main class="d-flex flex-column flex-grow-1 overflow-hidden">

    <header class="bg-primary text-white px-3 py-2 d-flex align-items-center gap-2 flex-shrink-0">
      <i class="bi bi-bar-chart-fill fs-5"></i>
      <span class="fw-semibold">Pagila Analytics</span>
    </header>

    <div class="flex-grow-1 overflow-y-auto p-3">

      <!-- Revenue summary cards -->
      <div class="row g-3 mb-4">
        <div class="col-md-4">
          <div class="card h-100 shadow-sm">
            <div class="card-body">
              <div class="text-muted small mb-1">Total Revenue</div>
              <div class="fs-4 fw-bold">${{ "%.2f" % revenue.total_revenue }}</div>
              <div class="text-muted small">avg ${{ "%.2f" % revenue.avg_per_rental }} / rental</div>
            </div>
          </div>
        </div>
        <div class="col-md-4">
          <div class="card h-100 shadow-sm">
            <div class="card-body">
              <div class="text-muted small mb-1">Total Payments</div>
              <div class="fs-4 fw-bold">{{ revenue.total_rentals | int }}</div>
            </div>
          </div>
        </div>
        <div class="col-md-4">
          <div class="card h-100 shadow-sm">
            <div class="card-body">
              <div class="text-muted small mb-1">Busiest Month</div>
              <div class="fs-4 fw-bold">{{ revenue.busiest_month or "—" }}</div>
              {% if revenue.busiest_month_revenue %}
              <div class="text-muted small">${{ "%.2f" % revenue.busiest_month_revenue }}</div>
              {% endif %}
            </div>
          </div>
        </div>
      </div>

      <!-- Store comparison -->
      <div class="card mb-4 shadow-sm">
        <div class="card-header fw-semibold small">Store Comparison</div>
        <div class="table-responsive">
          <table class="table table-sm table-bordered mb-0">
            <thead class="table-light">
              <tr>
                <th>Store</th><th>City</th><th>Manager</th>
                <th>Revenue</th><th>Rentals</th>
                <th>Customers</th><th>Outstanding</th>
              </tr>
            </thead>
            <tbody>
              {% for s in stores %}
              <tr>
                <td>{{ s.store_id }}</td>
                <td>{{ s.city }}</td>
                <td>{{ s.manager }}</td>
                <td>${{ "%.2f" % s.total_revenue }}</td>
                <td>{{ s.rental_count }}</td>
                <td>{{ s.unique_customers }}</td>
                <td>{{ s.outstanding_rentals }}</td>
              </tr>
              {% else %}
              <tr><td colspan="7" class="text-muted small fst-italic text-center">No data.</td></tr>
              {% endfor %}
            </tbody>
          </table>
        </div>
      </div>

      <!-- Category chart -->
      <div class="card mb-4 shadow-sm">
        <div class="card-header fw-semibold small">Revenue by Category</div>
        <div class="card-body" style="min-height:300px">
          <canvas id="category-chart"></canvas>
        </div>
      </div>
      <script id="category-data" type="application/json">{{ categories | tojson }}</script>
      <script>
        document.addEventListener("DOMContentLoaded", function () {
          const raw = JSON.parse(document.getElementById("category-data").textContent);
          new Chart(document.getElementById("category-chart"), {
            type: "bar",
            data: {
              labels: raw.map(d => d.category),
              datasets: [{
                label: "Revenue ($)",
                data: raw.map(d => d.total_revenue),
                backgroundColor: "rgba(13, 110, 253, 0.7)",
                borderColor: "rgba(13, 110, 253, 1)",
                borderWidth: 1,
              }]
            },
            options: {
              indexAxis: "y",
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: { x: { beginAtZero: true } },
            }
          });
        });
      </script>

      <!-- Overdue + Slow-moving -->
      <div class="row g-3">
        <div class="col-md-6">
          <div class="card h-100 shadow-sm">
            <div class="card-header fw-semibold small">Overdue Rentals</div>
            <div class="table-responsive">
              <table class="table table-sm table-bordered mb-0">
                <thead class="table-light">
                  <tr><th>Film</th><th>Customer</th><th>Store</th><th>Days Overdue</th></tr>
                </thead>
                <tbody>
                  {% for r in overdue %}
                  <tr>
                    <td>{{ r.title }}</td>
                    <td class="small text-truncate" style="max-width:120px">{{ r.customer_email }}</td>
                    <td>{{ r.store_id }}</td>
                    <td class="text-danger fw-semibold">{{ r.days_overdue }}</td>
                  </tr>
                  {% else %}
                  <tr><td colspan="4" class="text-muted small fst-italic text-center">No overdue rentals.</td></tr>
                  {% endfor %}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="card h-100 shadow-sm">
            <div class="card-header fw-semibold small">Slow-Moving Films</div>
            <div class="table-responsive">
              <table class="table table-sm table-bordered mb-0">
                <thead class="table-light">
                  <tr><th>Film</th><th>Store</th><th>Copies</th><th>Days Idle</th></tr>
                </thead>
                <tbody>
                  {% for f in slow_films %}
                  <tr>
                    <td>{{ f.title }}</td>
                    <td>{{ f.store_id }}</td>
                    <td>{{ f.copies_in_stock }}</td>
                    <td>{{ f.days_since_rented if f.days_since_rented is not none else "Never" }}</td>
                  </tr>
                  {% else %}
                  <tr><td colspan="4" class="text-muted small fst-italic text-center">No slow-moving films.</td></tr>
                  {% endfor %}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

    </div><!-- /scrollable -->
  </main>
</div>

</body>
</html>
```

- [ ] **Step 3: Smoke-test the template renders (requires running server)**

Start the server in one terminal:
```bash
cd genai-assistant && uv run src/main.py
```

In another terminal, hit the route:
```bash
curl -s http://localhost:8000/ui/analytics | grep -c "category-chart"
```

Expected: `1`

- [ ] **Step 4: Commit**

```bash
git add src/static/vendor/chart.min.js src/templates/analytics.html
git commit -m "feat: add analytics.html template and vendor Chart.js v4"
```

---

## Task 5: Add Analytics link to chat.html sidebar

**Files:**
- Modify: `genai-assistant/src/templates/chat.html`

The chat sidebar is currently the entire `<aside id="sessions-sidebar">` element, HTMX-swapped on load. To add a static nav link, split the aside: keep static nav links in the aside directly, move the HTMX target to an inner `<div id="sessions-sidebar">`.

`chat.js` already targets `#sessions-sidebar` for sidebar refresh (the `htmx.ajax` call in `readStream`). Moving the id to the inner div keeps that working.

- [ ] **Step 1: Update `src/templates/chat.html`**

Replace the entire `<aside>` block (lines 22–29):

**Old:**
```html
  <!-- Sidebar -->
  <aside id="sessions-sidebar"
         class="bg-white border-end d-flex flex-column"
         style="width:230px;min-width:180px;"
         hx-get="/ui/partials/sessions"
         hx-trigger="load"
         hx-swap="innerHTML">
    <div class="p-2 text-muted small">Loading…</div>
  </aside>
```

**New:**
```html
  <!-- Sidebar -->
  <aside class="bg-white border-end d-flex flex-column" style="width:230px;min-width:180px;">
    <div class="p-2 border-bottom">
      <a href="/ui/analytics"
         class="d-flex align-items-center gap-2 text-decoration-none text-dark rounded px-1 py-1 small">
        <i class="bi bi-bar-chart-fill text-muted"></i> Analytics
      </a>
    </div>
    <div id="sessions-sidebar"
         class="d-flex flex-column flex-grow-1 overflow-hidden"
         hx-get="/ui/partials/sessions"
         hx-trigger="load"
         hx-swap="innerHTML">
      <div class="p-2 text-muted small">Loading…</div>
    </div>
  </aside>
```

- [ ] **Step 2: Verify chat UI still works**

Start the server (`uv run src/main.py`) and open `http://localhost:8000/ui` in a browser.
Check:
- Sessions sidebar loads (session list appears)
- "Analytics" link appears above sessions
- Clicking "Analytics" navigates to `/ui/analytics`
- Sending a message refreshes the session list (sidebar refresh still works)

- [ ] **Step 3: Commit**

```bash
git add src/templates/chat.html
git commit -m "feat: add Analytics nav link to chat sidebar"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** revenue cards ✓ · store table ✓ · category chart ✓ · overdue table ✓ · slow-moving table ✓ · Analytics sidebar link ✓ · Chart.js vendored ✓
- [x] **No placeholders:** all steps have complete code
- [x] **Type consistency:** `revenue_summary`, `store_comparison`, `rental_stats_by_category`, `overdue_rentals`, `slow_moving_films` — names consistent across analytics_queries.py, ui_routes.py imports, and test patches (`ui_routes.<name>`)
- [x] **MCP store_id filtering:** moved to Python post-query in analytics_server.py (acceptable for demo, SQL did the same filtering)
- [x] **HTMX sidebar id:** `id="sessions-sidebar"` moved from `<aside>` to inner `<div>` — chat.js `htmx.ajax` call targets same id, no JS change needed
