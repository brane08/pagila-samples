# Reviews (RV-nnn)

## RV-001 — repo-wide D0/D1/D2 review, 2026-09-14
- Scope: whole repo; 4 parallel read-only subagents (spring-vaadin/go-web; core-api/data-ebean/data-hibernate/data-exposed/data-loader; quarkus-ebean/quarkus-hibernate/quarkus-htmx; Angular/genai-assistant/schema.sql).
- Outcome: all D0 and D1 findings fixed. D2 minors logged as BG-001..009.
- Not checked: `database/schema.sql` file only (no migration tool in repo); spring-vaadin and go-web clean at D2 only. Reviewed before the main restructure that removed quarkus-ebean, quarkus-hibernate, spring-vaadin and data-exposed.
- Findings produced: BG-001..009.

## RV-002 — data-ebean audit
- Scope: data-ebean file set, relations, BaseModel use, repo methods vs data-hibernate.
- Outcome: parity confirmed; fixes applied (default sort, RentalRepository.filterProperty, FilmRating DB value, findById load fields, Store load field manager).
- Not checked: date not recorded (imported from legacy todo).
- Findings produced: BG-010.

## RV-003 — post-merge code and workflow review, 2026-10-27
- Scope: 4 read-only sonnet subagents on main b96b1ef: (1) genai film_server/pagila_tables SQLAlchemy port vs original; (2) HTMX UI (ui_routes, main.py, templates, chat.js); (3) LangGraph agent workflow (agent.py, main.py streaming/confirm); (4) Java/SQL since RV-002 (query parsers, exception mapper, StoreMapper, schema.sql).
- Outcome: (1) no regressions, only low inherited items. (2)-(4) produced BG-013..020 (4 high, 4 med) plus BG-021 (low bundle). BG-013..018 fixed 2026-10-27; BG-019..021 open.
- Not checked: nothing was executed (code reading only); the .venv was off-limits to the reviewers, so mistune's escape default, installed langgraph/ToolNode/mcp-adapter behaviour and Command(resume) semantics are unverified; real Hibernate/Ebean runtime errors; analytics_queries.py, rag.py, store_server.py and analytics_server.py; test bodies (names and structure only); provider message-ordering strictness; Angular code.
- Findings produced: BG-013..021.

## RV-004 — Opus review of genai-assistant, 2026-10-28
- Scope: 1 read-only opus subagent on main 3e7aed8 (src/ of genai-assistant): regressions of BG-013..021 fixes, LangGraph semantics, security, async lifecycle, test gaps. Opus used because the user asked for it.
- Outcome: 20 findings (3 high, 10 med, 7 low), bundled as BG-022..028. Open.
- Not checked: nothing executed; .venv off-limits so langchain-mcp-adapters content shape and provider message-ordering strictness are unverified (OR-006, OR-012 plausible only).
- Findings produced: BG-022..028.

## RV-005 — Tool-approval prompt review of genai-assistant, 2026-10-28
- Scope: 1 read-only sonnet subagent (separate from the main thread) on review-rv004 working tree: interrupt/confirm flow in agent.py, main.py, chat.js, ui_routes/templates.
- Outcome: 14 findings OR-101..114 (2 high, 6 med, 6 low), bundled as BG-029 (flow integrity) and BG-030 (UX/prompt text). Open.
- Verified OK: no path to tools except via human_review; reject defaults on missing `approved`; args shown = args executed; card uses textContent (no XSS); pending interrupts survive restart.
- Not checked: nothing executed (code reading only); multi-worker behaviour is by inspection.
- Findings produced: BG-029, BG-030.

## RV-006 — Project status and tracking-gap audit, 2026-10-28
- Scope: 1 read-only sonnet subagent over CLAUDE.md, memory notes, tracking and every module in the module map, on review-rv004.
- Outcome: no new code defects; 6 doc/tracking findings fixed (BG-031/032/033/035, DS-001, TS-003), and open work logged as BG-034, BG-036, TS-002, TS-004..006, FE-001. DC-005 records the approval design.
- Not checked: nothing was executed (counts are from grep and docs); the post-fix code was reviewed separately in RV-007.
- Findings produced: BG-031..036, DS-001, TS-002..006, FE-001.

## RV-007 — Opus final review of genai-assistant post-fix code, 2026-10-28
- Scope: 1 read-only opus subagent on the merged PR #26 code (main.py detached runs/locks, agent.py nodes, chat.js, ui_routes, servers, rag, tests). Opus used because the user asked for it.
- Outcome: 20 findings (0 high, 6 med, 14 low), bundled as BG-037..041. BG-037/038 fixed and BG-039..041 partly fixed the same day (OR-301..306, 308..311, 313 done). Verified OK: SSE payloads via json.dumps, bounded regexes, generic error text, atomic lock+task start, escaping, query_utils, store_id in SQL.
- Not checked: nothing run; venv unreadable, so langchain-mcp-adapters behaviour (per-call stdio sessions, `aclose`) is unverified; ui-angular, Java modules, database SQL, test_film_server_helpers/test_ui_render/test_query_utils, DB-backed tool tests, analytics.html, pagila_tables.
- Findings produced: BG-037..041.
