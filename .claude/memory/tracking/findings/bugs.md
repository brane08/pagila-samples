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

## BG-013
(RV-003, high) Stored/reflected XSS in the HTMX chat UI. Server: `ui_routes.py:65` `mistune.html()` rendered with `| safe` in `message_list.html:12`, table cells at `ui_routes.py:45` unescaped (mistune default escape setting NOT verified). Client: `chat.js:102-115` `jsonBlockToTable` and `marked.parse` into innerHTML with no sanitiser; `chat.js:147` tool name and `chat.js:205-207` tool name/args unescaped (a prompt-injected tool call can forge the confirm dialog). Related bug: `message_list.html:9` matches role `ai` but `sessions.py` emits `assistant`, so AI messages vanish on history reload and the server XSS path is dead until that is fixed (fix escaping first).
Status: FIXED 2026-10-27

## BG-014
(RV-003, high) Interrupt handling in main.py/agent.py. `human_review` interrupts on every tool call, but `/chat` and `/chat/stream` accept new input on a paused thread (dangling tool_calls -> provider 400, thread bricked); blocking `/chat` returns answer="" with the thread paused; `/chat/confirm` does not check an interrupt is pending (duplicate/unknown thread replays the last AI message). LangGraph behaviour on these is assumed, not run.
Status: FIXED 2026-10-27

## BG-015
(RV-003, high) `validate_input` (agent.py:319-333) classifies only the last HumanMessage, so replies to a clarification ("bob@x.com", "store 2") are refused as off-topic.
Status: FIXED 2026-10-27

## BG-016
(RV-003, high) `handle_tool_errors` (agent.py:354-383) scans all ToolMessages in history and re-appends a replacement without the original id at the end, producing orphan tool messages (provider 400) and wrongly setting tool_retry_count. It also replaces the real error text with a constant, so the cause is hidden; non-JSON errors and list content are not detected.
Status: FIXED 2026-10-27

## BG-017
(RV-003, med) No `recursion_limit` (default 25, about 4 tool rounds at 6 supersteps each); GraphRecursionError -> 500 / stream error; retry and reject loops uncapped. ToolNode `handle_tool_errors` not set and classifier LLM calls have no fail-open, so an MCP crash or classifier failure aborts the turn and can poison the thread. `reflection_retry_count` never resets (one-shot per thread).
Status: FIXED 2026-10-27

## BG-018
(RV-003, med) Untrusted tool output is trusted: `save_prefs` writes store_id/email from any ToolMessage in history (another customer's email becomes the user's preference); summarisation promotes tool text into a system-role summary; `ground_answer` uses all-turn tool text, truncated at the head, and its warning is never sent over SSE. `clarify_tool_args` checks only tool_calls[0].
Status: FIXED 2026-10-27

## BG-019
(RV-003, med, deployment context: local demo, no auth by design) CORS `*`, uvicorn binds 0.0.0.0, unauthenticated `/admin/reindex`, `/sessions` list/read/DELETE and `/chat/confirm` for any thread; `thread_id` default `default`, `user_id` client-asserted; thread_id/user_id/message have no validation or length cap; `str(exc)` leaked in SSE and reindex status; reindex task reference not stored; delete_session not transactional; analytics page shows customer emails. Becomes real if the port is exposed beyond localhost.
Status: FIXED 2026-10-27 (auth not added)

## BG-020
(RV-003, med) Query parsers: lt/gt non-numeric -> ParseException -> RuntimeException -> 500 (not InvalidQueryException); `eq` has no type coercion (numeric/date/enum paths likely 500). Hibernate parses numbers in the default locale, Ebean uses Locale.ROOT; parse is lenient ("5abc" = 5). Ebean has no field/sort validation (400 on Hibernate, 500 on Ebean). Hibernate `field()` accepts associations and collections (`sort=actors` -> 500). Not run: findings are from reading code, and the parser tests are mock-based.
Status: FIXED 2026-10-27

## BG-021
(RV-003, low) Minor: film_server ILIKE inputs not escaped (`email="%"` matches the first customer) and `limit` unbounded; engine never disposed and both asyncpg pool and engine open in film_server; `film.rating` selected without cast (use rating_txt); `page<=0` / unbounded `size` / unused `direction` in FiqlQueryBean; `;` cannot appear in filter values; InvalidQueryException(field, cause) always says "Unknown field" and echoes input untruncated; `user_preferences` has no FK on preferred_store_id, no owner statement, no column migration path; chat.js does not handle non-2xx or SSE `error` events (input stays disabled); `ui_history` `import main` fallback can create a second app; chat.html thread_id in JS/URL needs `tojson` and UUID validation.
Status: FIXED 2026-10-27 (rating cast, disposal, `;` in values, owner/migration left)
