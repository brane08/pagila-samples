import asyncio
import json
import re
import uuid

import mistune
from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

from analytics_queries import (
    overdue_rentals,
    rental_stats_by_category,
    revenue_summary,
    slow_moving_films,
    store_comparison,
)
from db import get_asyncpg_pool
from sessions import delete_session, get_session_history, list_sessions

ui_router = APIRouter()

templates = Jinja2Templates(directory="src/templates")

_KNOWN_TYPES = {
    "film_list":     ["title", "rating", "rental_rate", "length"],
    "actor_list":    ["first_name", "last_name", "film_count"],
    "rental_list":   ["title", "rental_date", "return_date", "is_outstanding"],
    "customer_list": ["first_name", "last_name", "email", "store_id"],
    "store_list":    ["store_id", "city", "manager", "film_count"],
}

_JSON_BLOCK_RE = re.compile(r"```json\n(\{.*?\})\n```", re.DOTALL)


def _items_to_table(type_key: str, items: list[dict]) -> str:
    cols = _KNOWN_TYPES.get(type_key)
    if cols is None or not items:
        return ""
    header = "".join(f"<th>{c}</th>" for c in cols)
    rows = ""
    for item in items:
        cells = "".join(f"<td>{item.get(c, '')}</td>" for c in cols)
        rows += f"<tr>{cells}</tr>"
    return f'<table class="ai-table"><thead><tr>{header}</tr></thead><tbody>{rows}</tbody></table>'


def render_ai_message(text: str) -> str:
    def _replace(match: re.Match) -> str:
        raw = match.group(1)
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            return match.group(0)
        type_key = data.get("type", "")
        items = data.get("items", [])
        if type_key not in _KNOWN_TYPES:
            return match.group(0)
        table = _items_to_table(type_key, items)
        return table if table else match.group(0)

    processed = _JSON_BLOCK_RE.sub(_replace, text)
    return mistune.html(processed)


@ui_router.get("/ui", response_class=HTMLResponse)
async def ui_root(request: Request, thread_id: str | None = None):
    pool = get_asyncpg_pool()
    sessions = await list_sessions(pool)
    if thread_id is None:
        thread_id = str(uuid.uuid4())
    return templates.TemplateResponse(
        request,
        "chat.html",
        {"thread_id": thread_id, "sessions": sessions},
    )


@ui_router.get("/ui/partials/sessions", response_class=HTMLResponse)
async def ui_sessions(request: Request):
    pool = get_asyncpg_pool()
    sessions = await list_sessions(pool)
    return templates.TemplateResponse(
        request,
        "partials/session_list.html",
        {"sessions": sessions},
    )


@ui_router.get("/ui/partials/history/{thread_id}", response_class=HTMLResponse)
async def ui_history(request: Request, thread_id: str):
    import main as _main
    history = await get_session_history(_main.agent_app, thread_id)
    if history is None:
        raise HTTPException(status_code=404, detail="Session not found")
    messages = history.get("messages", [])
    return templates.TemplateResponse(
        request,
        "partials/message_list.html",
        {"messages": messages, "render_ai_message": render_ai_message},
    )


@ui_router.delete("/ui/sessions/{thread_id}", response_class=HTMLResponse)
async def ui_delete_session(request: Request, thread_id: str):
    pool = get_asyncpg_pool()
    deleted = await delete_session(pool, thread_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Session not found")
    sessions = await list_sessions(pool)
    return templates.TemplateResponse(
        request,
        "partials/session_list.html",
        {"sessions": sessions},
    )


@ui_router.get("/ui/analytics", response_class=HTMLResponse)
async def ui_analytics(request: Request):
    pool = get_asyncpg_pool()
    rev, stores, cats, overdues, slow = await asyncio.gather(
        revenue_summary(pool),
        store_comparison(pool),
        rental_stats_by_category(pool),
        overdue_rentals(pool),
        slow_moving_films(pool),
    )
    return templates.TemplateResponse(
        request,
        "analytics.html",
        {
            "revenue": rev,
            "stores": stores,
            "categories": cats,
            "overdue": overdues,
            "slow_films": slow,
        },
    )
