---
name: Tool-approval prompt audit
description: Audit of the genai-assistant tool-confirmation flow (interrupt, confirm card, resume) as of 2026-10-28; invariants, evidence, residual risks
type: project
---

Scope: every tool call pauses at `human_review` (LangGraph `interrupt()`), the UI shows a confirm card, and the user approves or rejects. Reviewed in RV-005 (separate agent), fixed in BG-029/030, re-reviewed in RV-007. Design record: DC-005. Code reading only; nothing was run against a live model or browser.

## Flow
1. `agent` proposes tool calls → `clarify` → `human_review` (`interrupt` with the calls) → `tools` only on `approved`.
2. `/chat/stream` ends with SSE `tool_confirm {tool_calls, confirm_id}`; `GET /chat/pending/{id}` restores the card after a reload.
3. `POST /chat/confirm/{id}/stream {approved, confirm_id}` resumes. `/chat` (blocking) auto-rejects, bounded by `MAX_AUTO_REJECTS`, then 409.

## Invariants (with where enforced)
| Invariant | Enforcement |
|---|---|
| No path to a tool except via `human_review` | graph edges in `agent.build_graph`; `AUTO_APPROVE_TOOLS` (default empty) skips the prompt only if every call in the batch is listed |
| Reject is the default | missing `approved` is treated as reject |
| Approval is bound to what the user saw | `confirm_id` = sha256 of call id/name/args; mismatch → 409 (`stale_confirm` over SSE) |
| Pending state re-checked under the thread lock | `_stream_agent_events._run` re-reads pending after taking the lock; new message while pending → SSE `pending` |
| One run per thread | per-thread `asyncio.Lock`; `_is_busy` probes without creating entries; 409 otherwise |
| A disconnect cannot cancel a resume mid-tool | run is a detached task feeding a queue; shutdown waits up to 10 s, `RUN_TIMEOUT_S` caps it |
| Several pending interrupts handled as one decision | `_calls_of` aggregates; id-keyed resume map when more than one |
| Card shows what will execute | args rendered via `textContent` (no XSS), args box scrollable; malformed interrupt → reject-only |
| Pending approvals survive a restart | stored in the LangGraph checkpoint (Postgres) |
| Single process | startup refuses `WEB_CONCURRENCY`/`UVICORN_WORKERS` > 1 (locks are in memory) |

## Residual risks (open)
- `confirm_id` is optional in `ConfirmRequest` (OR-307): a client that omits it approves whatever is pending. UI always sends it. Tracked in BG-040.
- `/chat` checks pending outside the lock, and after a successful auto-reject it returns 409 and drops the model's answer (OR-316). Tracked in BG-040.
- API is unauthenticated by design (BG-019): anyone who can reach the port can approve a thread's pending call. Localhost bind by default (`HOST`).
- No per-call subset approval: one decision covers the whole batch (won't fix, BG-030).
- Clarify classifier can replace a tool-call message with a question (false positive hides a call); fails open on errors (won't fix).
- Not exercised against a compiled graph: `/chat/confirm` → `_resume_command` with real interrupt ids, the auto-reject loop (OR-315, BG-039).

## Not checked
Live browser behaviour (no Playwright test covers the HTMX UI), real provider message-ordering strictness, multi-worker behaviour (refused at startup, not tested).
