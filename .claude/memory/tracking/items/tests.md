# Test items

## BACKLOG
- BACKLOG: enable and run the quarkus-htmx @QuarkusTest REST tests with -Pintegration against a live DB; record the result (see findings/tests.md#TS-002) | logged: 2026-10-28 | started: - | closed: -
- BACKLOG: rerun Playwright e2e and ui-angular unit specs (needs ng serve on 4200) (see findings/tests.md#TS-004) | logged: 2026-10-28 | started: - | closed: -
- BACKLOG: data-loader tests: ActorLoader determinism, CLI argument errors, additive load (see findings/tests.md#TS-005) | logged: 2026-10-28 | started: - | closed: -
- BACKLOG: go-web tests or a smoke test, plus a short README (see findings/tests.md#TS-006) | logged: 2026-10-28 | started: - | closed: -

## DONE
- DONE: DB-backed genai tool tests rerun after the BG-027 change: 102 passed (film, store, actor, rental, analytics, mcp) (see findings/tests.md#TS-003) | logged: 2026-10-28 | started: 2026-10-28 | closed: 2026-10-28
- DONE: confirm quarkus-htmx review on JDK 25: mvn and java are Temurin 25.0.3, enforcer requireJavaVersion=25 (see findings/tests.md#TS-001) | logged: 2026-10-26 | started: 2026-10-26 | closed: 2026-10-26

## Notes
2026-10-26: Playwright e2e 77/77 pass (incl. assistant-link.spec.ts); playwright.config.ts no longer hardcodes a chromium revision path (it broke on Playwright upgrades). genai-assistant pytest 83/83 pass (test_api, test_ui_render, test_analytics_queries), run from repo root. conftest sets a dummy OPENROUTER_API_KEY (agent.py builds ChatOpenAI at import).
Not run: genai MCP tool tests (test_film_tools etc., need live PG), full uv sync on Intel Mac (onnxruntime 1.26 has no x86_64 mac wheel; venv installed with `uv pip install --only-binary :all:` + mcp==1.27.0 pinned, since unpinned `mcp` resolves to 2.x which breaks `mcp.server.fastmcp`).
