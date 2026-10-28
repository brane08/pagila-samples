import os
import sys

import asyncpg
import pytest_asyncio
from dotenv import load_dotenv
from sqlalchemy.ext.asyncio import create_async_engine

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../src"))

load_dotenv(os.path.join(os.path.dirname(__file__), "../.env"))

# agent.py builds ChatOpenAI at import time; tests mock the model, but the client needs a key to construct.
os.environ.setdefault("OPENROUTER_API_KEY", "test-key")

_DB = dict(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", 5432)),
    database=os.getenv("DB_NAME", "sakila"),
    user=os.getenv("DB_USER", "postgres"),
    password=os.getenv("DB_PASSWORD", "password"),
    min_size=1,
    max_size=3,
)


# Only these modules talk to Postgres; every other suite runs without a database or the embedding model.
_DB_MODULES = {
    "test_film_tools", "test_store_tools", "test_analytics_tools",
    "test_actor_tools", "test_rental_tools", "test_mcp",
}


@pytest_asyncio.fixture(autouse=True)
async def inject_pool(request):
    """For DB-backed modules: create a pool/engine on this test's event loop and inject into the MCP servers."""
    if request.module.__name__.rsplit(".", 1)[-1] not in _DB_MODULES:
        yield
        return

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
    film_server._engine = engine
    analytics_server._engine = engine

    yield

    film_server._pool = None
    analytics_server._pool = None
    store_server._engine = None
    film_server._engine = None
    analytics_server._engine = None
    await pool.close()
    await engine.dispose()
