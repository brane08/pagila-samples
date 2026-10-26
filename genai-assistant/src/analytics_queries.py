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
        if not global_row or global_row["total_rentals"] == 0:
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
        # one row per (film, store) pair — a film with copies at both stores appears twice
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
