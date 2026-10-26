# Bug findings (BG-nnn)

Source of BG-001..009: RV-001 (2026-09-14). File:line refs predate the main merge of 2026-10-26; re-verify before fixing. `data-exposed` and `quarkus-hibernate` findings dropped with those modules.

## BG-001
`data-hibernate/.../film/entities/FilmView.java:23` columnDefinition `"varchar(10"` missing closing paren.
Status: WONT-DO 2026-10-26 (not present in current code)

## BG-002
`data-hibernate/.../seedworks/repositories/JPARepository.java:60-65` redundant duplicate override.
Status: WONT-DO 2026-10-26 (no duplicate found)

## BG-003
`data-hibernate/.../seedworks/query/JPAQueryParser.java` `getCount()` uses `cb.countDistinct(root)`; Ebean uses plain `findCount()`. Inconsistent semantics.
Status: WONT-DO 2026-10-26 (getCount already uses cb.count(root))

## BG-004
`quarkus-htmx/.../store/boundary/StoresResource.java:36,39-41` and `.../rental/boundary/RentalsResource.java:27,30-32` unused injected mapper field.
Status: FIXED 2026-10-26

## BG-005
`quarkus-htmx/.../film/boundary/FilmsResource.java:15,17,35` unused `Produces`/`MediaType` imports; `{film_id}` path param lacks `\d+` constraint, fragile route match.
Status: WONT-DO 2026-10-26 (boundary class has neither; app-layer resource already has `\d+`)

## BG-006
FIQL field names unvalidated (`FiqlQueryBean`, `EbeanQueryParser`, `JPAQueryParser`) and no `ExceptionMapper` in the Quarkus module: an unknown field gives an unhandled 500 instead of 400. Not SQL injection (bean property paths).
Status: FIXED 2026-10-26 (JPA path only; EbeanQueryParser not changed)

## BG-007
`database/schema.sql:63` `ALTER TABLE language ALTER COLUMN name TYPE varchar(20)` runs unconditionally on every reapply; needless rewrite once widened.
Status: FIXED 2026-10-26 (DO block; not applied to the local DB)

## BG-008
`genai-assistant/src/store_server.py:44` own `AsyncEngine` (`pool_size=1, max_overflow=4`) created lazily, never disposed. Low risk (short-lived MCP subprocess).
Status: WONT-DO 2026-10-26 (mcp.run owns the event loop; a clean dispose is fragile)

## BG-009
`ui-angular` components missing `ChangeDetectionStrategy.OnPush` (project convention): `chat/tool-confirm.component.ts` has real bindings (fix); `films`, `stores`, `actors`, `app` components are stateless shells.
Status: FIXED 2026-10-26 (all five components)

## BG-010
Remaining flagged behaviours from the data-ebean audit (RV-002). Fixed: `PagedList.totalPageCount`, QueryParser `=` in values, Actor/Address equals on null id. Parser silent drops: FIXED 2026-10-26 (unknown operator: Hibernate put a null predicate into cb.and, Ebean ignored; malformed segment threw IndexOutOfBounds; both now InvalidQueryException -> 400). StaffInfo.storeId: FIXED 2026-10-26 (it was unmapped; explicit staffToInfo with @Mapping store.storeId in both StoreMappers, tests added).
Status: FIXED 2026-10-26

## BG-011
genai-assistant `main.py` StaticFiles and `ui_routes.py` Jinja2Templates used cwd-relative `src/static` / `src/templates`; app only started from `genai-assistant/`. Now anchored to `Path(__file__).parent`. Not run: pytest unavailable on this machine.
Status: FIXED 2026-10-26

## BG-012
`npm audit` in ui-angular: transitive `undici` 7.0.0-7.30.0 (high, EventSource DoS) via direct devDependency `@angular/build` ^21.2.28 (moderate). Dev tooling only, not in the browser bundle. Only fix is `npm audit fix --force` -> `@angular/build@22` (breaking major).
Status: DEFERRED 2026-10-26 (revisit on Angular 22 upgrade)
