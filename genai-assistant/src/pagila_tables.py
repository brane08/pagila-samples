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
