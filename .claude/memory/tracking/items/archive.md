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
