import os

import asyncpg
from dotenv import load_dotenv
from psycopg_pool import AsyncConnectionPool

load_dotenv()

_asyncpg_pool: asyncpg.Pool | None = None
_psycopg_pool: AsyncConnectionPool | None = None


def _pg_url() -> str:
    user = os.getenv("DB_USER", "postgres")
    password = os.getenv("DB_PASSWORD", "password")
    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "5432")
    name = os.getenv("DB_NAME", "sakila")
    return f"postgresql://{user}:{password}@{host}:{port}/{name}"


def _pg_conninfo() -> str:
    return (
        f"host={os.getenv('DB_HOST', 'localhost')} "
        f"port={os.getenv('DB_PORT', '5432')} "
        f"dbname={os.getenv('DB_NAME', 'sakila')} "
        f"user={os.getenv('DB_USER', 'postgres')} "
        f"password={os.getenv('DB_PASSWORD', 'password')}"
    )


async def init_asyncpg_pool() -> asyncpg.Pool:
    global _asyncpg_pool
    _asyncpg_pool = await asyncpg.create_pool(dsn=_pg_url(), min_size=2, max_size=10)
    return _asyncpg_pool


async def init_psycopg_pool() -> AsyncConnectionPool:
    global _psycopg_pool
    # AsyncPostgresSaver requires autocommit=True, prepare_threshold=0.
    # search_path=langgraph routes all checkpoint tables into the langgraph schema.
    _psycopg_pool = AsyncConnectionPool(
        conninfo=_pg_conninfo(),
        min_size=2,
        max_size=10,
        open=False,
        kwargs={"autocommit": True, "prepare_threshold": 0, "options": "-c search_path=langgraph"},
    )
    await _psycopg_pool.open()
    return _psycopg_pool


def get_asyncpg_pool() -> asyncpg.Pool:
    if _asyncpg_pool is None:
        raise RuntimeError("asyncpg pool not initialized — call init_asyncpg_pool() first")
    return _asyncpg_pool


def get_psycopg_pool() -> AsyncConnectionPool:
    if _psycopg_pool is None:
        raise RuntimeError("psycopg pool not initialized — call init_psycopg_pool() first")
    return _psycopg_pool


async def close_pools() -> None:
    global _asyncpg_pool, _psycopg_pool
    if _asyncpg_pool:
        await _asyncpg_pool.close()
        _asyncpg_pool = None
    if _psycopg_pool:
        await _psycopg_pool.close()
        _psycopg_pool = None
