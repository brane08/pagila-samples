# Bug items

## DONE
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
