# Test gaps (TS-nnn)

| ID | Gap | Status |
|---|---|---|
| TS-001 | quarkus-htmx changes not explicitly reviewed on JDK 25. JDK 25 build (enforcer on) and 24 REST contract tests vs live PG16 sakila already passed; confirm nothing else is needed. | FIXED 2026-10-26: Maven runs on Temurin 25.0.3, enforcer requires 25, no JDK override anywhere in poms |
| TS-002 | The 4 quarkus-htmx `@QuarkusTest` REST classes (Films, Actors, Stores, Rentals) are tagged `integration` and excluded from surefire; no recorded run with `-Pintegration` against a live DB. Only listed as "Next options" in CLAUDE.md. | OPEN |
| TS-003 | DB-backed genai tool tests (film, store, actor, rental, analytics, mcp) not rerun after the BG-027 query_utils/rag change. | FIXED 2026-10-28: 102 passed against local Postgres |
| TS-004 | Playwright e2e (77) and ui-angular unit specs not rerun since 2026-10-26; the suite has no `webServer` block, so it needs `ng serve` on 4200. It does not cover the genai HTMX UI. | OPEN |
| TS-005 | data-loader has no tests (ActorLoader determinism with --seed, CLI argument errors, TableLoader additive behaviour). | OPEN |
| TS-006 | go-web has no tests, docs or tracking. | OPEN |
