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
