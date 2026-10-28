import asyncio
from unittest.mock import AsyncMock, MagicMock

import analytics_queries
import analytics_server
import rag
from query_utils import MAX_LIMIT, clamp, like


def test_clamp_bounds():
    assert clamp(0) == 1
    assert clamp(-5) == 1
    assert clamp(10**6) == MAX_LIMIT
    assert clamp(7) == 7


def test_like_escapes_wildcards():
    assert like("50%_off\\") == "50\\%\\_off\\\\"


def test_store_server_shares_helpers():
    import store_server

    assert store_server.clamp is clamp
    assert store_server.like is like


def _pool(conn):
    pool = MagicMock()
    pool.acquire.return_value.__aenter__ = AsyncMock(return_value=conn)
    pool.acquire.return_value.__aexit__ = AsyncMock(return_value=False)
    return pool


def test_overdue_rentals_pushes_store_id_into_sql():
    conn = MagicMock()
    conn.fetch = AsyncMock(return_value=[])
    asyncio.run(analytics_queries.overdue_rentals(_pool(conn), limit=5, store_id=2))
    sql, *params = conn.fetch.call_args.args
    assert "i.store_id = $2" in sql
    assert params == [5, 2]


def test_slow_moving_films_pushes_store_id_into_sql():
    conn = MagicMock()
    conn.fetch = AsyncMock(return_value=[])
    asyncio.run(analytics_queries.slow_moving_films(_pool(conn), days=30, limit=5, store_id=1))
    sql, *params = conn.fetch.call_args.args
    assert "i.store_id = $3" in sql
    assert params == [30, 5, 1]


def test_analytics_server_clamps_and_forwards_store(monkeypatch):
    seen = {}

    async def fake(pool, **kw):
        seen.update(kw)
        return []

    monkeypatch.setattr(analytics_server, "_get_pool", AsyncMock(return_value=object()))
    monkeypatch.setattr(analytics_server, "_overdue_rentals", fake)
    asyncio.run(analytics_server.get_overdue_rentals(store_id=2, limit=10**6))
    assert seen == {"limit": MAX_LIMIT, "store_id": 2}


def test_vector_store_is_cached(monkeypatch):
    rag.get_vector_store.cache_clear()
    monkeypatch.setattr(rag, "PGVector", MagicMock(side_effect=lambda **kw: object()))
    assert rag.get_vector_store() is rag.get_vector_store()
    assert rag.PGVector.call_count == 1
    rag.get_vector_store.cache_clear()


def test_semantic_search_runs_off_event_loop(monkeypatch):
    store = MagicMock()
    store.similarity_search_with_score.return_value = []
    monkeypatch.setattr(rag, "get_vector_store", lambda: store)
    assert asyncio.run(rag.semantic_search("q", k=3)) == []
    store.similarity_search_with_score.assert_called_once_with("q", k=3)
