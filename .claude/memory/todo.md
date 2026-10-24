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
- PENDING: data-ebean unit tests + 70% gate (needs Database mocks; Ebean not on test classpath with Hibernate)
- PENDING: Review quarkus-htmx changes on JDK 25
