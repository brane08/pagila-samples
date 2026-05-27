import os

import asyncpg
from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP

load_dotenv()

mcp = FastMCP("PagilaStores")

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
async def list_stores() -> list[dict]:
    """
    List all stores with store ID, manager name, address, city, and district.
    Use this as the starting point for any store-related query to discover store IDs.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT s.store_id,
                   st.first_name || ' ' || st.last_name AS manager,
                   a.address, a.district, c.city
            FROM store s
            JOIN staff st ON s.manager_staff_id = st.staff_id
            JOIN address a ON s.address_id = a.address_id
            JOIN city c ON a.city_id = c.city_id
            ORDER BY s.store_id
            """
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def get_store_inventory(store_id: int, category: str = "", limit: int = 20) -> list[dict]:
    """
    List films stocked at a specific store with total copies and available copies.
    Optionally filter by category. Use this to answer 'what can I rent at store X?'
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        query = """
            SELECT f.film_id, f.title, f.rating::text, f.rental_rate,
                   c.name AS category,
                   COUNT(i.inventory_id) AS total_copies,
                   SUM(CASE WHEN r.rental_id IS NULL OR r.return_date IS NOT NULL
                            THEN 1 ELSE 0 END) AS available_copies
            FROM film f
            JOIN film_category fc ON f.film_id = fc.film_id
            JOIN category c ON fc.category_id = c.category_id
            JOIN inventory i ON f.film_id = i.film_id AND i.store_id = $1
            LEFT JOIN rental r ON i.inventory_id = r.inventory_id
                AND r.return_date IS NULL
            WHERE 1=1
        """
        args: list = [store_id]
        if category:
            args.append(category)
            query += f" AND c.name ILIKE ${len(args)}"
        query += f"""
            GROUP BY f.film_id, f.title, f.rating, f.rental_rate, c.name
            ORDER BY f.title
            LIMIT ${len(args) + 1}
        """
        args.append(limit)
        rows = await conn.fetch(query, *args)
        return [dict(r) for r in rows]


@mcp.tool()
async def get_store_rentals(store_id: int, limit: int = 20) -> list[dict]:
    """
    Recent rental activity at a store — film title, customer name, rental date,
    and whether the item has been returned. Use to check store throughput or find
    outstanding rentals.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT r.rental_id, f.title,
                   cu.first_name || ' ' || cu.last_name AS customer,
                   cu.email AS customer_email,
                   r.rental_date, r.return_date,
                   CASE WHEN r.return_date IS NULL THEN true ELSE false END AS is_outstanding
            FROM rental r
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
            JOIN customer cu ON r.customer_id = cu.customer_id
            WHERE i.store_id = $1
            ORDER BY r.rental_date DESC
            LIMIT $2
            """,
            store_id, limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def get_store_top_customers(store_id: int, limit: int = 10) -> list[dict]:
    """
    Customers who rent most frequently from a specific store, ranked by rental count
    and total spend. Use to identify loyal or high-value customers at a location.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT cu.customer_id,
                   cu.first_name || ' ' || cu.last_name AS customer,
                   cu.email,
                   COUNT(r.rental_id) AS rental_count,
                   COALESCE(SUM(p.amount), 0) AS total_spent
            FROM customer cu
            JOIN rental r ON cu.customer_id = r.customer_id
            JOIN inventory i ON r.inventory_id = i.inventory_id
            LEFT JOIN payment p ON r.rental_id = p.rental_id
            WHERE i.store_id = $1
            GROUP BY cu.customer_id, cu.first_name, cu.last_name, cu.email
            ORDER BY rental_count DESC, total_spent DESC
            LIMIT $2
            """,
            store_id, limit,
        )
        return [dict(r) for r in rows]


@mcp.tool()
async def get_customer_store_payments(customer_email: str, store_id: int) -> dict:
    """
    Full payment history for a customer at a specific store — each payment with
    amount, date, and which film it was for. Use when a customer has questions
    about their account at a particular location.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        customer = await conn.fetchrow(
            "SELECT customer_id, first_name || ' ' || last_name AS full_name, email FROM customer WHERE email ILIKE $1",
            customer_email,
        )
        if not customer:
            return {"error": f"No customer found with email matching '{customer_email}'"}

        rows = await conn.fetch(
            """
            SELECT p.payment_id, p.amount, p.payment_date,
                   f.title AS film,
                   r.rental_date, r.return_date
            FROM payment p
            JOIN rental r ON p.rental_id = r.rental_id
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
            WHERE p.customer_id = $1
              AND i.store_id = $2
            ORDER BY p.payment_date DESC
            """,
            customer["customer_id"], store_id,
        )
        total = sum(r["amount"] for r in rows)
        return {
            "customer_id": customer["customer_id"],
            "name": customer["full_name"],
            "email": customer["email"],
            "store_id": store_id,
            "payment_count": len(rows),
            "total_paid": float(total),
            "payments": [dict(r) for r in rows],
        }


@mcp.tool()
async def get_store_monthly_revenue(store_id: int) -> list[dict]:
    """
    Month-by-month revenue breakdown for a store, ordered most-recent first.
    Use to spot trends — busy months, slow periods, or year-over-year patterns.
    """
    pool = await _get_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT DATE_TRUNC('month', p.payment_date)::date AS month,
                   COUNT(p.payment_id) AS transaction_count,
                   SUM(p.amount) AS revenue
            FROM payment p
            JOIN rental r ON p.rental_id = r.rental_id
            JOIN inventory i ON r.inventory_id = i.inventory_id
            WHERE i.store_id = $1
            GROUP BY DATE_TRUNC('month', p.payment_date)
            ORDER BY month DESC
            """,
            store_id,
        )
        return [dict(r) for r in rows]


if __name__ == "__main__":
    mcp.run(transport="stdio")
