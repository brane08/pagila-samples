# Test items

## DONE
- DONE: confirm quarkus-htmx review on JDK 25: mvn and java are Temurin 25.0.3, enforcer requireJavaVersion=25 (see findings/tests.md#TS-001) | logged: 2026-10-26 | started: 2026-10-26 | closed: 2026-10-26

## Notes
2026-10-26: Playwright e2e 77/77 pass (incl. assistant-link.spec.ts); playwright.config.ts no longer hardcodes a chromium revision path (it broke on Playwright upgrades). genai-assistant pytest 83/83 pass (test_api, test_ui_render, test_analytics_queries), run from repo root. conftest sets a dummy OPENROUTER_API_KEY (agent.py builds ChatOpenAI at import).
Not run: genai MCP tool tests (test_film_tools etc., need live PG), full uv sync on Intel Mac (onnxruntime 1.26 has no x86_64 mac wheel; venv installed with `uv pip install --only-binary :all:` + mcp==1.27.0 pinned, since unpinned `mcp` resolves to 2.x which breaks `mcp.server.fastmcp`).
