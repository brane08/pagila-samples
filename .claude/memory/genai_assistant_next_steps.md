---
name: genai_assistant_next_steps
description: genai-assistant status 2026-10-28 (all planned features done, RV-003..005 fixes in PR #26); how to run tests; where open work lives
type: project
---

All originally planned features are **complete** (markdown rendering, `/admin/reindex`, tool confirmation, actor/store/customer cards, Angular unit tests, analytics MCP server, summarize/validate/clarify/reflect/ground nodes). Open work is only in `tracking/todo.md` (see its counts table); nothing is listed here.

## Test suites (counts as of 2026-10-28)
- genai non-DB (run from repo root): `genai-assistant/.venv/bin/python -m pytest genai-assistant/tests/test_api.py genai-assistant/tests/test_agent_graph.py genai-assistant/tests/test_agent_resilience.py genai-assistant/tests/test_film_server_helpers.py genai-assistant/tests/test_ui_render.py genai-assistant/tests/test_query_utils.py -q -p no:cacheprovider -p no:warnings --rootdir genai-assistant` → 147 pass. Use the venv python: `uv run` can fail on Intel Mac (onnxruntime wheel).
- genai DB-backed (film, store, actor, rental, analytics tools, mcp; need local Postgres, ~10 min) → 102 pass.
- Playwright e2e: 77 specs; needs `ng serve` on 4200 (no `webServer` block); last full run 2026-10-26. It mocks the API and does not cover the genai HTMX UI.
- Karma/Jasmine: `cd ui-angular && ng test` (7 specs: actors/stores/customers/films services, 3 cards).

## Design notes worth keeping
- Raw SQL in Java repositories is avoided: views in `database/schema.sql` → Ebean `@Entity` view classes → MapStruct → `.find()`.
- `quarkus-htmx/pom.xml` overrides `junit-platform-engine` to 6.0.3 (Quarkus BOM pins 1.10.1; JUnit 6 needs matching versions); root pom too.
- genai agent graph, approval flow and config: `genai_assistant_architecture.md`, CLAUDE.md, DC-005.
- Specs/plans for the analytics server and the summarize/validate nodes: `docs/superpowers/specs|plans/2026-05-2*`.

## Known not-run / open (tracked)
quarkus-htmx `-Pintegration` REST tests (TS-002), Playwright rerun (TS-004), data-loader and go-web tests (TS-005/006), local DB schema apply (BG-034).
