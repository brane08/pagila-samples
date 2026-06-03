# Analytics Page Design

**Date:** 2026-06-03
**Branch:** ft/phase2

## Goal

Add a `GET /ui/analytics` page to the HTMX chat UI that surfaces MCP analytics data (revenue, store comparison, category chart, overdue/slow-moving film tables) with no user controls — static snapshot on load.

## Architecture

New FastAPI route in `ui_routes.py`. Analytics SQL is extracted into `analytics_queries.py` (shared between MCP server imports and the UI route — no MCP subprocess involved). Template `analytics.html` uses the same Bootstrap 5 sidebar layout as `chat.html`. Chart.js (vendored) renders the category bar chart client-side from JSON embedded in the page.

## Components

### `genai-assistant/src/analytics_queries.py` (new)
Async functions that run SQL directly via asyncpg:
- `get_revenue_summary(pool)` → `{total_revenue, payment_count, busiest_month}`
- `get_store_comparison(pool)` → list of store rows
- `get_rental_stats_by_category(pool)` → list of `{category, revenue, rental_count}`
- `get_overdue_rentals(pool)` → list of overdue rental rows
- `get_slow_moving_films(pool)` → list of film rows

The MCP `analytics_server.py` already contains these queries inline. Extract them here; `analytics_server.py` imports from `analytics_queries.py` instead.

### `genai-assistant/src/ui_routes.py` (modify)
Add `GET /ui/analytics` route:
- Acquires asyncpg pool (same pattern as existing routes)
- Calls all 5 query functions in parallel (`asyncio.gather`)
- Renders `analytics.html` with results as template context

Add "Analytics" nav link logic: both sidebar templates receive `active_page` context var so the correct link is highlighted.

### `genai-assistant/src/templates/analytics.html` (new)
Bootstrap 5 two-column layout mirroring `chat.html`:
- Left sidebar: sessions list + "Analytics" link (highlighted active); "Chat" link navigates to `/ui`
- Main area (scrollable): 4 sections in order:
  1. Revenue summary — 3 `col-md-4` stat cards
  2. Store comparison — Bootstrap table
  3. Rental stats by category — Chart.js horizontal bar chart (data embedded as JSON in `<script>`)
  4. Overdue rentals + Slow-moving films — two `col-md-6` Bootstrap tables side by side

### `genai-assistant/src/templates/chat.html` (modify)
Add "Analytics" link to sidebar pointing to `/ui/analytics`.

### `genai-assistant/src/static/vendor/chart.min.js` (new)
Chart.js v4 minified, vendored. No CDN.

## Data Flow

```
GET /ui/analytics
  → ui_routes.py route handler
  → asyncio.gather(5 × analytics_queries.py functions)
  → analytics.html rendered with all data server-side
  → Chart.js reads JSON embedded in <script id="category-data"> tag
  → static page, no further requests
```

## Error Handling

If any query fails, the route returns a 500 with a simple error page. No partial rendering — all-or-nothing on load. Query errors are logged; the asyncpg pool connection errors surface naturally via FastAPI's exception handler.

## Testing

`genai-assistant/tests/test_analytics_queries.py` — unit tests for each query function using a mock asyncpg connection (no live DB).

`genai-assistant/tests/test_api.py` — add `TestAnalyticsRoute` class: GET /ui/analytics returns 200 with expected HTML landmarks (stat cards, chart canvas, table headings).

## Assets

| File | Size (approx) | Source |
|---|---|---|
| `src/static/vendor/chart.min.js` | ~200KB | Chart.js v4 CDN download, then vendored |

No new CSS files. Bootstrap utility classes cover all layout needs.

## Out of Scope

- Date range filters or store selectors (static snapshot only)
- Store monthly revenue chart (excluded — requires interactive store selector)
- Export to CSV/PDF
- Auto-refresh
