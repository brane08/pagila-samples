# Decisions (DC-nnn)

## DC-001 — quarkus-htmx as the single Quarkus app
Status: ACCEPTED (logged 2026-10-26, imported from legacy decisions.md)
```yaml
module: quarkus-htmx (single Quarkus app)
port: 8001
paths: {json_api: /rest/*, html_htmx: /ui/*}
orm:
  default: data-hibernate
  switch: manual — edit one dependency in quarkus-htmx/pom.xml (data-hibernate <-> data-ebean) + producers (orm-ebean/) + SQL log category
  reason: data-ebean and data-hibernate share FQCNs; only one on the classpath
rest: films, actors, stores, rentals (JSON, /rest/*) incl. view/aggregate endpoints; HTML pages /ui, /ui/films, /ui/actors, /ui/stores, /ui/rentals
tests: REST contract tests in quarkus-htmx/src/test (need live DB)
```

## DC-002 — Hibernate conventions
Status: ACCEPTED (logged 2026-10-26, imported from legacy decisions.md)
- Sort params must be entity property names (filmId, actorId, storeId); `JPAQueryParser` applies order, no default order.
- zip columns on `customer_list`/`staff_list` are `"zip code"` (quoted).
- `Payment` must not extend `BaseModel`.

## DC-003 — adopt the global tracking pattern
Status: ACCEPTED 2026-10-26
Replaced flat `.claude/memory/todo.md` (PENDING/DOING/DONE/BLOCKED under Setup/Implementation/Tests/Review) with `tracking/`. Rejected: keeping the flat file (no IDs, dates or findings links; D2 minors had nowhere to live).

## DC-004 — chat UI is the backend HTMX UI; Angular chat removed
Status: ACCEPTED 2026-10-26
Adopted ft/phase2's Angular half: removed chat/sessions components, chat.service, agent.service, chat.models, tool-confirm, chat.spec (19 e2e) and the 3 chat error-state e2e tests; toolbar button opens `Constants.ASSISTANT_UI_URL` (http://localhost:8000/ui) in a new tab. Dropped ngx-markdown, marked, and the Forms/Dialog/FormField/Input/ProgressSpinner module imports. Rejected: keeping both UIs (duplicated chat logic), dropping the Angular half. Added `assistant-link.spec.ts` for the new button behaviour.

## DC-005 — genai tool approval is bound to a confirm_id; single worker
Status: ACCEPTED 2026-10-28 (from RV-004/RV-005, BG-025/029)
- Every pending approval carries `confirm_id` (hash of the proposed calls). `/chat/confirm` rejects a mismatch (409) and re-checks pending state under the per-thread lock; the UI re-fetches `/chat/pending` after any confirm error.
- Streamed runs are detached from the HTTP response so a disconnect cannot cancel a resumed graph mid-tool.
- Thread locks are in-process, so the app refuses to start with `WEB_CONCURRENCY>1`. Rejected: Postgres advisory locks (more moving parts than this demo needs).
- Every tool call is confirmed by default; `AUTO_APPROVE_TOOLS` is an explicit opt-in allowlist. Rejected: per-call subset approval and user/thread auth binding (API is unauthenticated by design, BG-019).
