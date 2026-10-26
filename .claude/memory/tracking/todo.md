# Tracking — index and rules

Counts per state and last-updated date per item file. No items here. Findings in `findings/`, tasks in `items/`.

| File | DOING | BLOCKED | PARTIAL | PENDING | BACKLOG | DEFERRED | DONE | Last updated |
|---|---|---|---|---|---|---|---|---|
| items/design.md | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2026-10-26 |
| items/feature.md | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2026-10-26 |
| items/bugs.md | 0 | 0 | 0 | 0 | 0 | 0 | 10 | 2026-10-26 |
| items/tests.md | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 2026-10-26 |
| items/archive.md | - | - | - | - | - | - | 15 | 2026-10-26 |

Other logs: `decisions.md` (DC-001..002), `review.md` (RV-001..002).

## Tracking pattern
Source: `~/.claude/memory/patterns/tracking-pattern.md`. Rules in force here:
- IDs: 2-letter code + 3 digits (DS design, FE feature, BG bug, TS test, DC decision, RV review), assigned in order, never renumbered.
- Item line: `- STATE: text (see findings/bugs.md#BG-014) | logged: YYYY-MM-DD | started: YYYY-MM-DD or - | closed: YYYY-MM-DD or -`
- States: DOING, BLOCKED, PARTIAL, PENDING, BACKLOG, DEFERRED, DONE, WONT-DO (WONT-DO sits in DONE section, counted in DONE).
  - PARTIAL text: "done: … / left: …". PENDING: committed, next up. BACKLOG: accepted, unscheduled. DEFERRED adds `| revisit: <date or trigger>`.
- Item file sections: DOING, BLOCKED, PARTIAL, PENDING, BACKLOG, DEFERRED, DONE, Notes. Sort by ID.
- Per task: set DONE + closed date, set the finding `Status: FIXED|WONT-DO YYYY-MM-DD`, promote at most one PENDING→DOING + started date, update this table in the same edit.
- Archive at 30 DONE items: move to `items/archive.md` under `## Archived from <file> on <date>`, lines unchanged. Findings, decisions, reviews are never archived.
- Every flaw goes in a findings log; every task links to its finding ID. Log a decision when a choice changes how we build, track or review. Log every review with what was not checked.
- Session start: read `MEMORY.md` and this file; open item files by ID or line range; grep the archive by ID, never load it.
