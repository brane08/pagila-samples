# Task Status — quarkus-htmx

## Setup
- PENDING: Verify quarkus-htmx on JDK 25 + live DB (package/augmentation passes on JDK 21 override; no runtime run)

## Implementation
- DONE: /ui/films HTML page + nav link; deferred REST endpoints added (films count/facets/@nicer-view/@sales-by-category, actors/@view, stores @view/@staff/@sales-by-store, rentals + rentals/@customers)

## Tests
- PENDING: Run REST contract tests against live DB
- DONE: All REST tests enabled (none @Disabled); still unrun
- DONE: Unit tests + JaCoCo 70% line gate (core-api 100%, data-hibernate 99%, quarkus-htmx 73%; JDK21 override); POJOs/interfaces excluded; surefire parallel on

## Review
- DONE: data-ebean audit — file set, relations, BaseModel use, repo methods match data-hibernate; no tests/gate yet
- DONE: data-ebean unit tests (185 incl. core-api) + JaCoCo gate (100% line); fixed default sort + RentalRepository.filterProperty
- DONE: FilmRating mapped as DB value (PG-13); Ebean findById applies load fields; Store load field manager; CLAUDE.md versions synced
- PENDING: Remaining flagged behaviours (PagedList.totalPageCount, parser silent drops, Actor/Address equals null id, StaffInfo ids unmapped)
- PENDING: Review quarkus-htmx changes on JDK 25
