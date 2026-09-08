import os

from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP
from sqlalchemy import and_, case, func, or_, select
from sqlalchemy.engine import URL
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from store_tables import (
    address,
    category as category_t,
    city,
    customer,
    film,
    film_category,
    inventory,
    payment,
    rental,
    staff,
    store,
)

load_dotenv()

mcp = FastMCP("PagilaStores")

_engine: AsyncEngine | None = None


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


# ── Tools ──────────────────────────────────────────────────────────────────────

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


if __name__ == "__main__":
    mcp.run(transport="stdio")
