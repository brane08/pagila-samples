# Analytics MCP Server Design

**Date:** 2026-05-21  
**Branch:** genai-assistant  
**Status:** Approved

---

## Overview

Add a third MCP server (`analytics_server.py`) to the genai-assistant module covering four
cross-store analytics tools. Wire it into the existing LangGraph agent alongside `film_server.py`
and `store_server.py`. Total tool count goes from 20 to ~24.

---

## Architecture

`analytics_server.py` follows the same pattern as `store_server.py`:

- `FastMCP("PagilaAnalytics")` instance
- Lazy `asyncpg` pool via `_get_pool()` (same env-var pattern: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
- Four `@mcp.tool()` async functions
- `if __name__ == "__main__": mcp.run(transport="stdio")` entry point

No shared helper functions between tools — each tool owns its own SQL query. The queries are
independent enough that extracting helpers adds complexity without payback.

---

## Tools

### `get_overdue_rentals(store_id=None, limit=20) -> list[dict]`

Returns rentals that are past their `rental_duration` deadline and have not been returned.

**Overdue condition:** `NOW() > rental_date + (rental_duration || ' days')::interval AND return_date IS NULL`

**Returned fields per row:**
- `rental_id`, `film_id`, `title`, `customer_email`, `store_id`
- `rental_date` (date), `days_overdue` (int)

Optional `store_id` filter. Ordered by `days_overdue DESC`.

---

### `get_slow_moving_films(store_id=None, days=90, limit=20) -> list[dict]`

Films present in inventory that have had zero rentals in the last `days` days. Identifies dead
stock that could be rotated or promoted.

**Logic:** LEFT JOIN inventory → rental, group by film+store, filter where
`MAX(rental_date) < NOW() - INTERVAL '...'` OR rental_date IS NULL (never rented).

**Returned fields per row:**
- `film_id`, `title`, `rating`, `store_id`, `copies_in_stock`
- `last_rented` (date or null if never rented), `days_since_rented` (int or null)

Optional `store_id` filter. Ordered by `days_since_rented DESC NULLS FIRST`.

---

### `get_revenue_summary() -> dict`

Global revenue totals across all stores and a per-store breakdown.

**Returned structure:**
```json
{
  "total_revenue": 123.45,
  "total_rentals": 16044,
  "avg_per_rental": 2.91,
  "busiest_month": "2005-08",
  "busiest_month_revenue": 1234.56,
  "by_store": [
    {"store_id": 1, "revenue": 60.00, "rental_count": 8000, "avg_per_rental": 2.90},
    {"store_id": 2, "revenue": 63.45, "rental_count": 8044, "avg_per_rental": 2.91}
  ]
}
```

Single query using CTEs to compute global totals, busiest month, and per-store breakdown in one
round-trip.

---

### `get_store_comparison() -> list[dict]`

Side-by-side metrics for both stores in a single query. One row per store.

**Returned fields per row:**
- `store_id`, `manager`, `city`
- `total_revenue`, `rental_count`, `unique_customers`
- `avg_rental_rate` (average film rental rate across stocked inventory)
- `outstanding_rentals` (currently checked out, not returned)

Ordered by `store_id`.

---

## LangGraph Wiring

### `agent.py` changes

1. Add `"pagila_analytics"` to `MultiServerMCPClient`:
   ```python
   "pagila_analytics": {
       "command": "uv",
       "args": ["run", "src/analytics_server.py"],
       "transport": "stdio",
   }
   ```
   No graph structure changes — `client.get_tools()` automatically discovers the new tools.

2. Add an `## Analytics` section to `SYSTEM_PROMPT`:

   ```
   ## Analytics
   - "Are any rentals overdue?" → get_overdue_rentals; add store_id if a specific store is mentioned
   - "What's not being rented?" / "slow movers" / "dead stock" → get_slow_moving_films
   - "How is the business doing overall?" / "total revenue" → get_revenue_summary
   - "Compare the two stores" (high-level) → get_store_comparison; use get_store_monthly_revenue for month-by-month detail
   ```

3. Update the tool count comment in SYSTEM_PROMPT from "20 tools" to "24 tools".

---

## Testing

New file: `genai-assistant/tests/test_analytics_tools.py`

Pattern: matches `test_store_tools.py` — live PostgreSQL on `localhost:5432`, uses the existing
`conftest.py` asyncpg pool fixture. No mocks.

**Test classes:**

| Class | Tests |
|---|---|
| `TestOverdueRentals` | returns list; each row has required keys; days_overdue >= 0; store_id filter narrows results |
| `TestSlowMovingFilms` | returns list; each row has required keys; days_since_rented is None or >= 0; store_id filter works |
| `TestRevenueSummary` | returns dict with all top-level keys; total_revenue > 0; by_store has 2 entries; avg_per_rental > 0 |
| `TestStoreComparison` | returns 2-row list; each row has required keys; total_revenue > 0; outstanding_rentals >= 0 |

---

## Files Changed

| File | Change |
|---|---|
| `genai-assistant/src/analytics_server.py` | New file |
| `genai-assistant/src/agent.py` | Add analytics server to MultiServerMCPClient; update SYSTEM_PROMPT |
| `genai-assistant/tests/test_analytics_tools.py` | New file |
