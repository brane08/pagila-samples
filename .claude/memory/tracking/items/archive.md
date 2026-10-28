# Archive

## Archived from legacy todo.md on 2026-10-26
Imported from the flat `.claude/memory/todo.md` (pre-pattern, no IDs or dates). Section in brackets.

- DONE: [Setup] OpenRewrite plugin (rewrite:dryRun/run, UpgradeToJava21) in root pom; applied
- DONE: [Setup] UpgradeToJava21 applied (List.get(0)->getFirst(), redundant compiler source/target removed); code level stays 25
- DONE: [Impl] ui-angular upgraded to Angular 21.2.x (build, 19 unit, 91 e2e pass); npm install clean without legacy-peer-deps
- DONE: [Impl] Removed spring-vaadin, data-exposed and Kotlin/Exposed/Ktor pom entries
- DONE: [Impl] /ui/films HTML page + nav link; deferred REST endpoints added (films count/facets/@nicer-view/@sales-by-category, actors/@view, stores @view/@staff/@sales-by-store, rentals + rentals/@customers)
- DONE: [Tests] Playwright e2e 91 passed; Ebean swap passes 24 REST tests on live DB (needs RepositoryProducersUnitTest removed); data-loader + SlowQueryApp run OK
- DONE: [Tests] JDK 25 build (enforcer on) + 24 REST contract tests vs live PG16 sakila + app smoke (/rest, /ui pages 200); local PG16 needs shims for Pagila master (uuidv7, VIRTUAL col)
- DONE: [Tests] All REST tests enabled (none @Disabled)
- DONE: [Tests] Unit tests + JaCoCo 70% line gate (core-api 100%, data-hibernate 99%, quarkus-htmx 73%; JDK21 override); POJOs/interfaces excluded; surefire parallel on
- DONE: [Review] data-ebean audit — file set, relations, BaseModel use, repo methods match data-hibernate
- DONE: [Review] data-ebean unit tests (185 incl. core-api) + JaCoCo gate (100% line); fixed default sort + RentalRepository.filterProperty
- DONE: [Review] FilmRating mapped as DB value (PG-13); Ebean findById applies load fields; Store load field manager; CLAUDE.md versions synced
- DONE: [Review] fixed PagedList.totalPageCount, QueryParser '=' in values, Ebean size<=0/bare '-' sort/locale parse, Actor/Address equals null-id (tests updated)
- DONE: [Review] customer detail card (GET /rest/rentals/@customers/{id}, Angular card, e2e), films+customers service specs (30 karma, 95 e2e)
- DONE: [Review] review fixes (direct find lookups, card error states for actor/customer, e2e 404 tests, drop stray report)

## Archived from items/bugs.md on 2026-10-28
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
