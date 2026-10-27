# Bug items

## BACKLOG

## DEFERRED
- DEFERRED: ui-angular npm audit: undici via @angular/build, dev-only, fix needs Angular 22 major bump (see findings/bugs.md#BG-012) | logged: 2026-10-26 | started: - | closed: - | revisit: Angular 22 upgrade

## DONE
- DONE: BG-021 bundle: ILIKE escaping + limit clamp, thread_id tojson/validation, chat.js error handling, page/size clamp, ui_history no longer imports main (503), user_preferences store FK (NOT VALID, schema.sql, not applied to local DB), save_prefs best-effort. Not done (won't fix / low value): film.rating cast, engine/pool disposal (short-lived MCP process, as BG-008), `;` in filter values, user_preferences owner/migration path (see findings/bugs.md#BG-021) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: strict Locale.ROOT number parse -> InvalidQueryException (400) in JPA+Ebean parsers, JPA rejects plural-attribute fields, page/size clamped in FiqlQueryBean; follow-up: Ebean field/sort validation via io.ebean.plugin.BeanType (public API, 400 on unknown property), JPA eq coerced to field Java type (core-api 82, data-hibernate 144, data-ebean 190, quarkus-htmx 37 pass) (see findings/bugs.md#BG-020) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: bind 127.0.0.1 (HOST), CORS limited to localhost (CORS_ORIGINS), thread/user id + message validation, transactional delete_session, generic error text; /admin and /sessions stay unauthenticated by design (see findings/bugs.md#BG-019) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: chat UI output escaped server+client (mistune escape=True, table cells, marked renderer, tool badge/confirm via textContent), ai->assistant role fixed (genai tests 100 pass) (see findings/bugs.md#BG-013) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: 409 on new input with pending interrupt and on confirm without one, /chat interrupt path, generic SSE error (see findings/bugs.md#BG-014) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: validate_input skips short follow-ups and fails open on classifier error (see findings/bugs.md#BG-015) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: handle_tool_errors current-turn only, in-place replace by id, error text kept (see findings/bugs.md#BG-016) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: recursion_limit=40 in request configs, ToolNode handle_tool_errors, reflection counter reset (see findings/bugs.md#BG-017) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: save_prefs current-turn + validation, summary tool text truncated/data-only, grounding current-turn tail; clarify_tool_args checks all tool calls, SSE sends the grounding warning (see findings/bugs.md#BG-018) | logged: 2026-10-27 | started: 2026-10-27 | closed: 2026-10-27
- DONE: genai-assistant static/template paths anchored to __file__ (see findings/bugs.md#BG-011) | logged: 2026-10-26 | started: 2026-10-26 | closed: 2026-10-26
- DONE: data-ebean audit leftovers: totalPageCount, QueryParser '=' in values, Actor/Address equals null-id, parser silent drops (unknown operator / malformed segment -> InvalidQueryException -> 400 in JPA + Ebean parsers), StaffInfo.storeId mapped via staffToInfo in both StoreMappers (core-api 79, data-hibernate 140, data-ebean 189, quarkus-htmx 37 tests pass, JaCoCo gates met) (see findings/bugs.md#BG-010) | logged: 2026-10-26 | started: 2026-10-26 | closed: 2026-10-26
- DONE: unknown FIQL field returns 400: InvalidQueryException in JPAQueryParser + mapper in quarkus-htmx (core-api 78, data-hibernate 139, quarkus-htmx 37 tests pass, JaCoCo gates met) (see findings/bugs.md#BG-006) | logged: 2026-09-14 | started: 2026-10-26 | closed: 2026-10-26
- DONE: drop unused mapper fields in Stores/RentalsResource and their unit tests (see findings/bugs.md#BG-004) | logged: 2026-09-14 | started: 2026-10-26 | closed: 2026-10-26
- DONE: guard schema.sql language ALTER in a DO block; not yet applied to the local DB (see findings/bugs.md#BG-007) | logged: 2026-09-14 | started: 2026-10-26 | closed: 2026-10-26
- DONE: add OnPush to app, films, stores, actors, tool-confirm components (see findings/bugs.md#BG-009) | logged: 2026-09-14 | started: 2026-10-26 | closed: 2026-10-26
- WONT-DO: FilmView columnDefinition missing paren; not present in current code (see findings/bugs.md#BG-001) | logged: 2026-09-14 | started: - | closed: 2026-10-26
- WONT-DO: JPARepository redundant override; none found (see findings/bugs.md#BG-002) | logged: 2026-09-14 | started: - | closed: 2026-10-26
- WONT-DO: JPAQueryParser.getCount already uses cb.count(root) (see findings/bugs.md#BG-003) | logged: 2026-09-14 | started: - | closed: 2026-10-26
- WONT-DO: FilmsResource boundary has no such imports or film_id path; app layer already has `\d+` (see findings/bugs.md#BG-005) | logged: 2026-09-14 | started: - | closed: 2026-10-26
- WONT-DO: dispose store_server AsyncEngine; mcp.run owns the loop, process is short-lived (see findings/bugs.md#BG-008) | logged: 2026-09-14 | started: - | closed: 2026-10-26

## Notes
BG-001..009 line refs predate the main merge. 2026-10-26 re-check: BG-001/002/003/005 not reproducible. Verified after BG-004/009: quarkus-htmx 36 tests pass, ui-angular typecheck + 31 unit tests pass.
