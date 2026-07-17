# Gate 8 Durable Queue Terminal Disposition — Design

- Date: 2026-07-17
- Task ID: `gate-8-durable-queue-terminal-disposition`
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler), sole `Specified - Next`
- Status: Design (pending implementation plan)

## Problem

The Gate 8 Scheduler queue records only one terminal fact: `completedWorkItemIds`,
a success-only set that simultaneously means "this work item finished" **and**
"this work item satisfies its dependents' dependencies." The runtime lifecycle
already distinguishes verified completion (`COMPLETED`) from failure (`FAILED`)
and reaches `AWAITING_VERIFICATION` on execution acknowledgement, but the queue
cannot represent failure at all. Its `validateStructure` invariant requires
`pending + active + completed` to exactly partition `admissionOrder`, so every
item that leaves the active slot is forced into the dependency-satisfying set.

This overloading is the documented root cause of the earlier "completion"
conflict, where one word named three different lifecycle facts:

1. worker execution completion (runtime reaches `AWAITING_VERIFICATION`);
2. verified runtime completion (an independently supported `ResultPayload`
   completes or fails the AgentRun and Goal);
3. Scheduler queue completion (`completeActive` adds the item to
   `completedWorkItemIds` and releases its dependents).

## Goal

Define a durable queue **terminal-disposition** contract so that:

- execution acknowledgement remains distinct from verified completion;
- failed work never enters the dependency-satisfaction set, so its dependents
  stay blocked with an inspectable cause;
- a later `ResultPayload` / RunRecord integration has an unambiguous, recoverable
  target for recording the terminal disposition.

## Confirmed Decisions (grounded in existing code and accepted decisions)

- **Slot policy — keep the active slot through verification (Option A).** Already
  an accepted decision (`DECISION_LOG.md:20`): releasing the slot before terminal
  verification requires a *separately accepted* non-terminal waiting-state design
  and must not reuse `completedWorkItemIds`. The single-active-slot model is
  enforced throughout (`SingleWorkerSchedulerQueue.claimNext` returns empty while
  `active != null`).
- **Failure propagation — block dependents, do not auto-propagate.** Natural
  consequence of the existing `claimNext` dependency check plus
  `DECISION_LOG.md:132` (failed/non-verified results are terminal in schema v1;
  retry is a later design introducing another AgentRun, not history mutation).
- **Schema — revise schema-v1 in place, no version bump.** Precedent at
  `DECISION_LOG.md:82` (the lease was added to schema-v1 state without bumping
  the version) and `DECISION_LOG.md:1380` (no released storage format exists yet).
  The queue snapshot is a git-ignored local `.enhancer/` artifact. Recorded with
  an explicit local-artifact compatibility accepted decision.
  - **Verified boundary:** the on-disk envelope (`FileSystemSchedulerQueueStore`)
    is length-prefixed but has no forward-compat marker; `decode` rejects trailing
    bytes (`input.available() != 0`). Appending the `failedWorkItemIds` set to
    `encode`/`decode` therefore makes any pre-existing local snapshot **fail closed**
    as `CorruptedSchedulerQueueStateException` on read. This is acceptable because
    the artifact is unreleased local dev state; the accepted decision must state it
    explicitly. `CURRENT_SCHEMA_VERSION` stays `1` (both old and new writers use
    version 1; old artifacts fail on structure, not the version check).

## Design Improvements Folded In

1. **Explicit `WorkItemDisposition` enum** `{ VERIFIED_COMPLETED, FAILED }` — makes
   the state and API self-documenting and directly closes the "completion"
   ambiguity at the type level.
2. **Split the single terminal operation.** Replace `completeActive(id)` with two
   explicit terminal operations, `completeActiveVerified(id)` and `failActive(id)`,
   so every call site names the disposition.
3. **Preserve `completedWorkItemIds` semantics exactly.** It continues to mean
   "verified-completed = dependency-satisfying." Add a separate `failedWorkItemIds`.
   The partition invariant extends to
   `pending + active + verified + failed = admissionOrder`. `claimNext`'s
   dependency check reads the verified set only, so dependents of failed work
   auto-block with no new logic.
4. **Queue stores disposition only, never failure detail.** The inspectable cause
   lives in the runtime (`AgentRun` FAILED + `ResultPayload`) / RunRecord and is
   linked by `workItemId`. Avoids splitting authority across two stores.
5. **Explicit increment boundary.** In scope: queue state model, disposition API,
   durable persistence (persist-before-expose), and restart-recovery semantics.
   Out of scope (the *next* connection): `ResultPayload`/RunRecord result wiring,
   and the dispatcher wiring that reads a terminal `AgentRun` and records the
   matching queue disposition.
6. **Documented recovery / at-least-once boundary.** Only a persisted disposition
   is authoritative. Non-terminal active work is requeued on recovery; a persisted
   terminal disposition is never re-run. The window where the runtime is FAILED but
   the queue disposition is not yet persisted (causing an at-least-once requeue and
   possible re-execution) is a known boundary closed later by the result-wiring
   increment's idempotent-suffix recovery.

## Architecture

Three collaborating layers, unchanged in their separation of concerns:

### 1. `SchedulerQueueState` (immutable state, schema-v1 revised in place)

- New field: `Set<String> failedWorkItemIds` (canonical UUIDs, bounded to
  `maxWorkItems`, disjoint from every other status set).
- `validateStructure` extended: `pending`, `active`, `completedWorkItemIds`
  (verified), and `failedWorkItemIds` must jointly and exclusively partition
  `admissionOrder`; failed ids must be admitted; active work's dependencies must
  be a subset of the **verified** set only (unchanged reference to
  `completedWorkItemIds`).
- `completedWorkItemIds()` keeps its name and meaning (verified-completed);
  new accessor `failedWorkItemIds()`.
- `CURRENT_SCHEMA_VERSION` stays `1`.

### 2. `SingleWorkerSchedulerQueue` (in-memory behavior)

- New in-memory `Set<String> failedWorkItemIds`.
- Replace `completeActive(String)` with the two operations below. The rename is
  clean: `completeActive` has no production caller except the `Durable...` wrapper
  (the dispatcher never calls it, consistent with "execution acknowledgement is not
  queue completion"); only the wrapper and tests are updated.
  - `completeActiveVerified(String workItemId)` — adds to verified set, clears the
    active slot (the current `completeActive` behavior, renamed for clarity);
  - `failActive(String workItemId)` — adds to `failedWorkItemIds`, clears the
    active slot, does **not** touch the verified/dependency-satisfying set.
- `claimNext` unchanged: still checks `completedWorkItemIds.containsAll(deps)`, so
  a dependency that failed is never satisfied and its dependents stay pending.
- `snapshot(...)` and the state-rehydrating constructor carry the failed set.
- `requeueActiveForRecovery` unchanged: it only ever acts on the non-terminal
  active slot.
- New read accessor `failedWorkItemIds()`; optional `WorkItemDisposition
  dispositionOf(String)` returning the terminal disposition for a completed/failed
  id (used by consumers and tests to assert the distinction).

### 3. `DurableSingleWorkerSchedulerQueue` + `FileSystemSchedulerQueueStore`

- Serialize/deserialize the new `failedWorkItemIds` set in the schema-v1 snapshot
  format (strict UTF-8, integrity-checked, atomically published, revision-checked,
  64 MiB bounded — same guards as today).
- Each disposition operation persists the new state before exposing it
  (persist-before-expose), consistent with existing enqueue/claim/complete.
- Restart recovery: verified and failed dispositions survive and are never
  re-run; only interrupted active work is requeued in admission order.
- Fails closed on missing, corrupt, oversized, structurally invalid, or
  unsupported state (unchanged posture).

### New type

- `WorkItemDisposition` enum under `com.enhancer.runtime`:
  `VERIFIED_COMPLETED`, `FAILED`, with `boolean satisfiesDependencies()`
  (`true` only for `VERIFIED_COMPLETED`).

## Data Flow

```
claimNext() -> active WorkItem (EXECUTING in runtime)
   runtime: EXECUTING -> AWAITING_VERIFICATION   (lease released; queue unchanged, slot still active)
   independent verification decides terminal outcome:
     verified   -> completeActiveVerified(id) -> verified set += id -> dependents may become ready
     not verified -> failActive(id)           -> failed set   += id -> dependents stay blocked
```

The queue never observes `AWAITING_VERIFICATION`; the slot simply stays active
until a terminal disposition is recorded. Recording the disposition from a
terminal `AgentRun` is the *next* increment, not this one.

## Error Handling

- Terminal operations require a matching active work item; calling either
  disposition with no active item or a mismatched id throws
  `IllegalStateException` (same guards as today's `completeActive`).
- A disposition operation is single-shot per active item: once the slot is
  cleared, a second disposition call finds no active item and fails.
- Persistence failures retain the prior revision and prior in-memory state
  (persist-before-expose); no partial disposition becomes visible.
- Deserialized state that violates the extended partition invariant fails closed
  via `CorruptedSchedulerQueueStateException`.

## Testing Strategy (test-first, RED classified before GREEN)

- **`SchedulerQueueStateTest`**: failed set participates in the partition; failed
  ids must be admitted; active dependencies check the verified set only; failed
  and verified sets are disjoint; over-capacity and duplicate rejection.
- **`SingleWorkerSchedulerQueueTest`**: `failActive` blocks dependents while
  `completeActiveVerified` releases them; disposition accessors return the correct
  `WorkItemDisposition`; single-shot terminal guards; recovery requeues only
  non-terminal active work.
- **`DurableSingleWorkerSchedulerQueueTest`**: persist-before-expose for both
  dispositions; failed and verified dispositions survive restart and are not
  re-run; corrupt/oversized/invalid state fails closed.
- **`FileSystemSchedulerQueueStoreIntegrationTest`**: real filesystem round-trip
  of a mixed verified/failed snapshot; exact envelope/WorkItem provenance
  preserved; atomic publication and revision checks hold.
- **`WorkItemDispositionTest`**: `satisfiesDependencies()` is true only for
  `VERIFIED_COMPLETED`.

## Out of Scope

- `ResultPayload` / RunRecord result delivery and terminal runtime persistence.
- Dispatcher wiring that reads a terminal `AgentRun` and records the queue
  disposition.
- Retry (a later design introduces another AgentRun).
- Automatic failure propagation to dependents.
- A non-terminal awaiting-verification queue state (Option B).
- Schema v2 / migration code; workers, effect fencing, external effects,
  multi-process coordination, parent-directory power-loss durability.

## Risks and Boundaries

- At-least-once requeue window (see improvement #6) remains open until the
  result-wiring increment; documented, not closed here.
- Keeping the slot occupied through verification reduces throughput for a single
  queue; accepted per Option A, revisited only if verification throughput becomes
  a demonstrated bottleneck.
- Two-set representation (verified + failed) is chosen over a
  `Map<String, WorkItemDisposition>` for a minimal diff that preserves the exact
  `completedWorkItemIds` dependency-satisfaction semantics; a map may be
  reconsidered when additional dispositions (e.g., retry states) actually arrive.
