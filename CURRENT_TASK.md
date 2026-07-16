# Current Task

## Status

Completed

## Task

Add a persisted fenced single-owner lease to the Gate 8 AgentRun lifecycle without connecting worker execution.

## Task ID

gate-8-fenced-agent-run-lease

## Justified By

- 2026-07-16: Fence One AgentRun Owner Before Worker Execution

## Context

The durable Goal/AgentRun lifecycle can reach `EXECUTING`, but no owner, expiry, or fence protects that state. The next bounded increment gives one owner a time-bounded lease, rejects stale lifecycle writes, and durably returns expired execution to `READY` before any worker or external effect is introduced.

## Acceptance Criteria

- Add immutable `AgentRunLease` with bounded owner identity, positive fence token, issue time, and exclusive expiry.
- Accept lease durations from 1 millisecond through 24 hours using an injected `Clock`.
- Permit acquisition only from `READY`, move to `EXECUTING`, and increment a persisted monotonic fence token.
- Require matching owner and fence for renewal and execution completion; reject stale, mismatched, or expired writes.
- Require renewal to extend the current expiry while preserving the fence token.
- Preserve an unexpired lease and executing state across restart.
- On recovery or explicit reclaim at/after expiry, persistently clear the lease and return the AgentRun to `READY`.
- Ensure the next acquisition after reclaim receives a strictly greater fence and the former owner remains stale.
- Persist acquisition, renewal, completion, and reclaim before exposure; persistence failure must retain the previous state.
- Extend filesystem state encoding with strict validation and exact lease/fence recovery.
- Preserve the existing exact WorkItem, result matching, Verified-only completion, authority boundaries, and schema-v1 lifecycle behavior.
- Run focused runtime tests, full regression, Java 17 strict lint, actual-document self-hosting, structural/reference, and whitespace checks.

## Out Of Scope

- Worker/Tool execution or process isolation
- Heartbeat or progress telemetry
- Retry or more than one AgentRun per Goal
- Cancellation, pause/resume, reassignment, priority, fairness, or budgets
- External-effect idempotency, compensation, effect records, or effect fencing
- RunRecord lookup or Message Bus production wiring
- Multi-process locking, distributed clock-skew handling, or remote lease service
- Schema migration beyond v1, snapshot history, cleanup, or parent-directory fsync
- Commit, push, PR, merge, release, or deployment

## Approval

Approved by the user's 2026-07-16 request to continue the project from the documented fenced-lease next task.

## Verification

- RED: production compilation passed and test compilation failed with 46 aligned errors naming only the missing lease type, Clock overloads, and owner/fence lifecycle operations.
- Focused GREEN: 68 tests across 10 runtime, bus, store, queue, and package-boundary suites passed with no skips, failures, or errors.
- Full regression: 55 suites and 243 tests; 241 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`.
- Java 17 strict lint passed across all 147 production sources with `-Xlint:all -Werror`.
- Acquire, renew, complete, and reclaim persistence-failure regressions each retained the previous revision and lease.
- Cross-instance filesystem tests preserved an unexpired lease, reclaimed it at exact expiry, and recovered exact Unicode owner/fence/timestamps.
- Post-document Context Reader, Planner, Assisted Loop, package-boundary, lifecycle, and filesystem-store verification passed 31 of 32 tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Structural checks retained exactly one `Status: Specified - Next` marker at Gate 8, resolved the active task's accepted-decision reference, and passed `git diff --check`.

## Next

Connect one durable Scheduler queue claim to one durable Goal/AgentRun planning, readiness, and fenced lease-acquisition path without adding a worker or external effect.
