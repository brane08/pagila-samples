# HTMX Chat UI Design

**Date:** 2026-06-03
**Branch:** ft/phase2
**Module:** `genai-assistant`

---

## Goal

Replace the Angular `MatDialog` chat UI with a full-page HTMX chat interface served directly by FastAPI at `GET /ui`. No Angular dependency required to use the chat. The existing JSON API (`/chat/stream`, `/sessions`, etc.) is untouched.

---

## Architecture

FastAPI serves a Jinja2 HTML page. The existing JSON API endpoints are called directly by the browser:

- **Session sidebar interactions** → HTMX (`hx-get`, `hx-delete`)
- **Chat streaming** → vanilla JS `fetch` + `ReadableStream` (~80 lines)
- **Markdown rendering** → `marked.js` (CDN) client-side on stream completion
- **History rendering** → Python `mistune` server-side when loading a session

```
Browser
  │
  ├── GET /ui                        → chat.html (Jinja2)
  ├── GET /ui/partials/sessions      → session_list.html (HTMX)
  ├── GET /ui/partials/history/{id}  → message_list.html (HTMX)
  ├── DELETE /ui/sessions/{id}       → session_list.html (HTMX)
  │
  ├── POST /chat/stream              → SSE (existing, JSON events)
  └── POST /chat/confirm/{id}/stream → SSE (existing, JSON events)
```

---

## File Structure

```
genai-assistant/src/
  main.py                        ← add StaticFiles mount, ui_router
  ui_routes.py                   ← new: all /ui HTML routes
  templates/
    chat.html                    ← full page layout
    partials/
      session_list.html          ← sidebar session items
      message_list.html          ← chat history (session switch)
      tool_confirm.html          ← approve/reject form
  static/
    chat.css                     ← bubbles, sidebar, typing indicator
    chat.js                      ← fetch SSE handler + scroll + table render
```

`ui_routes.py` is a new file so HTML routes stay cleanly separated from the JSON API in `main.py`. `main.py` includes it via `app.include_router(ui_router)`.

---

## New Routes

All routes are in `ui_routes.py`, mounted on the `app` in `main.py`.

| Method | Path | Returns | Notes |
|---|---|---|---|
| `GET` | `/ui` | `chat.html` | Full page. Accepts `?thread_id=` query param. |
| `GET` | `/ui/partials/sessions` | `session_list.html` | HTMX sidebar refresh. |
| `GET` | `/ui/partials/history/{thread_id}` | `message_list.html` | Load session history. Returns 404 if not found. |
| `DELETE` | `/ui/sessions/{thread_id}` | `session_list.html` | Delete + return updated list. Returns 404 if not found. |

The existing JSON API routes are not modified.

---

## Templates

### `chat.html` — full page

Two-column layout:
- Left: sessions sidebar (`<div id="sessions-sidebar">`) with `hx-get="/ui/partials/sessions" hx-trigger="load" hx-swap="innerHTML"`
- Right: chat area
  - Header with title and current thread ID
  - Message area (`<div id="chat-messages">`) — populated on load from history or empty
  - Tool confirm area (`<div id="tool-confirm">`) — hidden by default, shown by JS on `tool_confirm` event
  - Input: `<textarea>` + Send button (wired in `chat.js`, not an HTMX form)

Includes in `<head>`:
- `marked.js` from CDN (`https://cdn.jsdelivr.net/npm/marked/marked.min.js`)
- `/static/chat.css`
- `/static/chat.js` (deferred)
- HTMX from CDN

`thread_id` is passed as a JS variable via an inline `<script>` tag in the template:
```html
<script>
  const THREAD_ID = "{{ thread_id }}";
  const USER_ID = ""; // set from localStorage by chat.js on load
</script>
```

### `partials/session_list.html`

Repeating `<li>` items, each with:
- Session ID (truncated), last active time
- `hx-get="/ui/partials/history/{{ s.thread_id }}"` on click, `hx-target="#chat-messages"`, `hx-swap="innerHTML"`
- `hx-delete="/ui/sessions/{{ s.thread_id }}"` on delete button, `hx-target="#sessions-sidebar"`, `hx-swap="innerHTML"`

### `partials/message_list.html`

List of rendered message bubbles. Roles:
- `user` → right-aligned bubble, plain text
- `ai` → left-aligned bubble, content rendered through `mistune` server-side
- `tool` → centered badge (`⚙ tool_name`)

Structured output: before passing AI message content to `mistune`, the Python renderer detects a JSON code block matching the structured output types (`film_list`, `actor_list`, etc.) and converts it to an HTML `<table>`. The remaining text is then rendered as markdown.

### `partials/tool_confirm.html`

Simple form with tool name, JSON args (formatted), and two buttons:
- Approve: triggers `chat.js` `confirmTool(true)`
- Reject: triggers `chat.js` `confirmTool(false)`

---

## chat.js — Streaming Handler

```
sendMessage(text)
  │
  ├── append user bubble to #chat-messages
  ├── create empty AI bubble (id="streaming-bubble")
  ├── show typing indicator
  └── fetch POST /chat/stream
        │
        ├── for each SSE line in ReadableStream:
        │     token       → append to #streaming-bubble .content
        │     tool_start  → insert tool badge before bubble
        │     tool_end    → no-op
        │     tool_confirm→ renderToolConfirm(data)
        │     done        → finalizeMessage()
        │
        └── finalizeMessage()
              ├── parse structured JSON block → <table> if present
              ├── marked.parse(text) → set innerHTML
              ├── hide typing indicator
              └── enable input

confirmTool(approved)
  ├── hide #tool-confirm
  ├── show typing indicator
  └── fetch POST /chat/confirm/{THREAD_ID}/stream
        └── resume same SSE reading loop → finalizeMessage()
```

**scroll:** After every DOM mutation to `#chat-messages`, `chat.js` scrolls the container to the bottom.

**user_id:** On load, `chat.js` reads `localStorage.getItem("pagila_user_id")` or generates a UUID and stores it. Sent in every `/chat/stream` POST body.

**thread_id:** Read from the `THREAD_ID` JS variable injected by the template. A "New Session" button calls `newSession()` which generates a UUID and navigates to `/ui?thread_id={uuid}`.

---

## Server-side Structured Output Rendering

A Python helper `render_ai_message(text: str) -> str`:

1. Detect pattern: ` ```json\n{"type":"film_list",...}\n``` `
2. Parse the JSON block
3. Build an HTML `<table>` from `items` array and known column sets (same as Angular `TABLE_COLUMNS`)
4. Replace the code block in the text with the `<table>` HTML
5. Run remaining text through `mistune.html(text)`
6. Return combined HTML string

Used only in `message_list.html` (history load). Streaming messages are rendered client-side by `marked.js` + JS table detection on `done`.

---

## Dependencies

Add to `pyproject.toml`:

```toml
"mistune>=3.0",
"aiofiles>=23.0",
```

`jinja2` is already available via `fastapi[standard]`. `marked.js` and `htmx` loaded from CDN.

---

## Testing

New test class `TestUiRoutes` in `tests/test_api.py`. All tests use the existing `client` fixture (which patches `agent_app` and `get_asyncpg_pool`).

| Test | Scenario |
|---|---|
| `test_ui_root_returns_html` | `GET /ui` → 200, `text/html`, contains `chat.js` |
| `test_ui_root_accepts_thread_id_param` | `GET /ui?thread_id=abc` → 200, `abc` in response body |
| `test_ui_partials_sessions_returns_html` | `GET /ui/partials/sessions` → 200, `text/html` |
| `test_ui_partials_history_returns_html` | `GET /ui/partials/history/t1` → 200, `text/html` |
| `test_ui_delete_session_returns_html` | `DELETE /ui/sessions/t1` → 200, `text/html` |
| `test_ui_delete_unknown_session_returns_404` | `DELETE /ui/sessions/nope` → 404 |

Expected `test_api.py` total: **64** (58 existing + 6 new).

---

## Out of Scope

- Angular `ui-angular` chat component is not modified or deleted — it continues to work independently.
- No authentication or multi-user isolation beyond `user_id` from localStorage.
- No WebSocket upgrade — `fetch` SSE is sufficient.
- No dark mode toggle — uses system `prefers-color-scheme`.
- No Playwright e2e tests for the HTMX UI (Angular e2e suite is unaffected).
