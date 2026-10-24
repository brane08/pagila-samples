# Decisions

## quarkus-htmx
```yaml
module: quarkus-htmx (single Quarkus app)
port: 8001
paths: {json_api: /rest/*, html_htmx: /ui/*}
orm:
  default: data-hibernate
  switch: manual — edit one dependency in quarkus-htmx/pom.xml (data-hibernate <-> data-ebean) + producers (orm-ebean/) + SQL log category
  reason: data-ebean and data-hibernate share FQCNs; only one on the classpath
rest: films, actors, stores, rentals (JSON, /rest/*) incl. view/aggregate endpoints; HTML pages /ui, /ui/films, /ui/actors, /ui/stores, /ui/rentals
tests: REST contract tests in quarkus-htmx/src/test (need live DB)
```

## Hibernate conventions
- Sort params must be entity property names (filmId, actorId, storeId); `JPAQueryParser` applies order, no default order.
- zip columns on `customer_list`/`staff_list` are `"zip code"` (quoted).
- `Payment` must not extend `BaseModel`.
