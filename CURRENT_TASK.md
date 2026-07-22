# Current Task

## Status

Completed

## Task

Introduce the coherent Gate 8 schema-v2 multi-attempt runtime boundary: retain immutable
AgentRun and retry-decision history, enforce exact prefixes in filesystem persistence,
separate attempt-result recording from terminal queue disposition, and park the current
worker safely at `RETRY_PENDING` without performing a replacement attempt.

## Task ID

add-schema-v2-history-and-park-retry-pending

## Context

Before this increment, the corrected pure decider was complete but the runtime stored one
schema-v1 AgentRun and the finalizer converted every failed attempt directly into terminal
Scheduler failure. Adding schema-v2 history alone would have made a failed attempt
`RETRY_PENDING` while the old finalizer still calls `failActive`, leaving runtime and queue
state contradictory.

The user approved resolving that sequencing conflict by combining schema-v2 state and
storage, the minimum finalizer split, and worker parking in one bounded increment. The
retry controller and replacement-attempt execution remain later work.

## Justified By

- 2026-07-22: Separate Retryable AgentRun Failure From Terminal WorkItem Disposition
- 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution
- 2026-07-17: Record RunRecord-Backed Result-Path Finalization Connecting Verified Outcome To Runtime Terminal State And Queue Disposition
- 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

## Acceptance Criteria

- `AgentRuntimeState.CURRENT_SCHEMA_VERSION` is 2 and schema-v1 runtime artifacts fail
  explicitly as unsupported; no migration or in-place schema-v1 reinterpretation is added.
- `RuntimeGoalStatus` includes durable non-terminal `RETRY_PENDING` and runtime state
  retains immutable ordered `agentRuns()` and `retryDecisions()` lists bounded to 16,
  while `agentRun()` remains the latest-attempt projection and `completedAttempts()` is
  derived from terminal history.
- State validation preserves exact Goal/WorkItem binding, globally unique runtime and
  message identities, terminal failed prefixes, exact results, Goal-wide monotonic fences,
  control-request validity, and status/history/decision consistency.
- `AgentRunRetryDecisionRecord` immutably records the failed attempt, completed-attempt and
  policy bounds, external-effect ledger revision/count/semantic SHA-256, and exact typed
  decision. Exact decision replay is revision-free and changed input for the same attempt
  fails closed.
- Failed attempt result recording produces AgentRun `FAILED` plus Goal `RETRY_PENDING`
  without queue mutation; admitted decision plus a distinct replacement identity appends
  one `PLANNING` attempt, while a refused decision permits terminal Goal abandonment.
- `FileSystemAgentRuntimeStateStore` encodes schema v2 and independently rejects history
  or decision truncation, rewrite, reordering, invalid append, prior-result replacement,
  stale revision, fence regression/jump, control-prefix rewrite, corruption, trailing data,
  oversize, unsupported schema, and symbolic-link storage boundaries.
- Finalization exposes result recording separately from terminal disposition. It never
  fails or completes the Scheduler WorkItem for an `ACTIVE` or `RETRY_PENDING` Goal and
  applies exactly one matching disposition only for terminal Goal `COMPLETED` or `FAILED`.
- The current worker stops safely at `RETRY_PENDING`, retains its durable cycle intent and
  RunRecord reference, reports no terminal `FAILED` disposition, and performs no second
  AgentRun. Existing Verified completion behavior remains unchanged.
- Focused RED is classified before implementation; focused state/store/finalizer/worker
  tests, restart recovery, the full Gradle build with strict Java lint, document structural
  checks, and diff checks pass with fresh evidence.

## Out Of Scope

- `DurableAgentRunRetryController`, external-effect ledger resolution or digest selection,
  replacement identity generation/checkpointing, actual replacement execution, retry-loop
  recovery, automatic compensation, or cross-attempt effect idempotency.
- Schema-v1 migration, user override, delay/backoff, token/time budgets, priority/fairness,
  authenticated control application, multi-process locking, distributed clock handling,
  multi-agent execution, release, or deployment.
- Commit, push, PR, or merge unless separately requested.

## Approval

The user explicitly approved the combined schema-v2, finalizer-split, and worker-parking
direction on 2026-07-22 after the sequencing conflict was reported and explained.

## Verification

Fresh evidence is recorded under `Schema-V2 AgentRun History And Retry-Pending Parking
Verification` in `docs/verification-log.md`. Focused state/store/finalizer/worker coverage
passed 36 tests; the final strict-lint build passed 80 suites and 418 tests with 3
existing Windows privilege skips; final document/package structure passed 8 tests
across 3 suites; and `git diff --check` exited 0 with no output.

## Next

Implement `DurableAgentRunRetryController` over the exact Goal ledger and persisted decision
record, without yet wiring replacement-attempt execution into the worker loop.
