# Reviews (RV-nnn)

## RV-001 — repo-wide D0/D1/D2 review, 2026-09-14
- Scope: whole repo; 4 parallel read-only subagents (spring-vaadin/go-web; core-api/data-ebean/data-hibernate/data-exposed/data-loader; quarkus-ebean/quarkus-hibernate/quarkus-htmx; Angular/genai-assistant/schema.sql).
- Outcome: all D0 and D1 findings fixed. D2 minors logged as BG-001..009.
- Not checked: `database/schema.sql` file only (no migration tool in repo); spring-vaadin and go-web clean at D2 only. Reviewed before the main restructure that removed quarkus-ebean, quarkus-hibernate, spring-vaadin and data-exposed.
- Findings produced: BG-001..009.

## RV-002 — data-ebean audit
- Scope: data-ebean file set, relations, BaseModel use, repo methods vs data-hibernate.
- Outcome: parity confirmed; fixes applied (default sort, RentalRepository.filterProperty, FilmRating DB value, findById load fields, Store load field manager).
- Not checked: date not recorded (imported from legacy todo).
- Findings produced: BG-010.
