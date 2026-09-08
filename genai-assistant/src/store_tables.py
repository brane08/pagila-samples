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
