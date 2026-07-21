# Gate 8 Durable Multi-Attempt AgentRun Lifecycle — Design

- Date: 2026-07-22
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler)
- Connection: ROADMAP Gate 8 ordered connection #6 (retry through additional
  AgentRuns), **second slice, first sub-increment**: the durable runtime lifecycle
  foundation that a later retry controller/worker consumes.
- Maturity target: Contract Verified (a multi-attempt state machine, its persistence,
  and its restart recovery invariants exist; no decider wiring, no worker, no
  re-execution).

## Problem

The bounded fence-checked external-effect ledger and the pure `AgentRunRetryDecider`
now exist, but the runtime cannot hold a second AgentRun. `AgentRuntimeState` is
schema-v1 with exactly one `RuntimeAgentRun` (`beginAgentRun` throws "schema v1
permits exactly one AgentRun"), and a non-verified result drives the Goal to terminal
`FAILED`. There is no way to begin another AgentRun for the same WorkItem, and no
durable record of how many attempts have been made.

Retry through additional AgentRuns therefore needs a durable lifecycle foundation
first: one Goal must retain a bounded ordered history of its terminal attempts, be
able to begin a fresh AgentRun after a failed attempt, expose a completed-attempt
count the decider can consume, and still be able to reach an explicit terminal
failure when retries stop.

This increment adds only that lifecycle and its persistence/recovery. It wires in no
decider, drives no worker, re-executes nothing, and reconciles nothing with the
Scheduler queue.

## Approach decision: retry-pending Goal status (Approach 2)

Two models were considered (2026-07-22 review):

- **Approach 1 — retry from terminal FAILED:** leave `recordResult`/finalizer
  unchanged and let a new `beginRetryAgentRun` move the Goal `FAILED -> ACTIVE`.
  Smallest blast radius, but it reactivates a terminal state, bending the stated
  forward-only invariant, and leaves `FAILED` ambiguous between "done" and
  "awaiting retry".
- **Approach 2 — retry-pending Goal status (chosen):** a non-verified result puts the
  Goal in a new **non-terminal** `RETRY_PENDING` status. The AgentRun still reaches
  per-attempt terminal `FAILED` (with its result), so the finalizer — which derives
  the queue disposition from the *AgentRun* status, not the Goal status — is
  unchanged. `beginRetryAgentRun` moves `RETRY_PENDING -> ACTIVE`; a new `abandonGoal`
  moves `RETRY_PENDING -> FAILED` (terminal). This preserves forward-only and makes
  "retryable" versus "abandoned" explicit.

Approach 2 is chosen for lifecycle correctness. The key enabling fact is that
`DurableAgentRunFinalizer.applyQueueDisposition` reads
`runtime.agentRun().orElseThrow().status()` and `recoverFinalization` reads
`run.status().isTerminal()` — both key off the AgentRun status. Keeping the AgentRun
terminal `FAILED` while changing only the Goal status means the queue mapping is
untouched.

A third option — a separate per-Goal attempt-history store distinct from
`AgentRuntimeState` — was rejected: the runtime state already owns the AgentRun
lifecycle, so its attempt history belongs in the same durable boundary.

## State model

### `RuntimeGoalStatus`

Add `RETRY_PENDING`: `ACCEPTED, ACTIVE, RETRY_PENDING, COMPLETED, FAILED`.

Forward-only Goal transitions:

| From | To | Trigger |
|---|---|---|
| ACCEPTED | ACTIVE | `beginAgentRun` (first attempt) |
| ACTIVE | COMPLETED | `recordResult` with a Verified result |
| ACTIVE | RETRY_PENDING | `recordResult` with a non-Verified result (**was ACTIVE -> FAILED**) |
| RETRY_PENDING | ACTIVE | `beginRetryAgentRun` |
| RETRY_PENDING | FAILED | `abandonGoal` |

`COMPLETED` and `FAILED` remain terminal. `RETRY_PENDING` and terminal `FAILED` both
have a current AgentRun in terminal `FAILED` with a result; they are distinguished
**only** by the Goal status.

### `AgentRuntimeState` attempt history

Add `List<RuntimeAgentRun> attemptHistory` — the superseded terminal attempts, in
admission order. `agentRun` remains the single current/latest attempt.

- Structural cap: total attempts (`attemptHistory.size()` + the one current
  `agentRun`) at most `MAX_ATTEMPTS_PER_GOAL = 16`, so `attemptHistory.size() <= 15`
  while a current run exists. This keeps `completedAttempts()` inside the decider's
  `1..16` input bound. The cap is a structural ceiling, not the retry policy (the
  policy still lives in `AgentRunRetryPolicy`, and a controller will normally stop
  earlier via the decider).
- Every history entry is a terminal `FAILED` `RuntimeAgentRun` carrying its result
  message, belonging to the same Goal and WorkItem, with an `agentRunId` unique across
  the whole history plus the current run.

`completedAttempts()` = `attemptHistory.size()` + (`agentRun` present and terminal
? 1 : 0). At `RETRY_PENDING` (current `FAILED`) this is `attemptHistory.size() + 1`;
during a fresh in-flight attempt it is `attemptHistory.size()`; at `COMPLETED` it is
`attemptHistory.size() + 1`.

## Transitions (all fail-closed, persist-before-exposure)

### `recordResult` (changed)

A non-Verified result now sets the Goal to `RETRY_PENDING` instead of `FAILED`; the
AgentRun still terminates `FAILED` with its result message. A Verified result is
unchanged (AgentRun `COMPLETED`, Goal `COMPLETED`). The attempt history is not
touched here — the just-failed run remains the current `agentRun`.

### `beginRetryAgentRun(newAgentRunId)` (new)

- Preconditions: Goal status `RETRY_PENDING`; current `agentRun` present with status
  `FAILED`; total attempts `< MAX_ATTEMPTS_PER_GOAL`.
- `newAgentRunId`: canonical UUID; distinct from `goalId`, `workItemId`, the work
  message id, and **every** prior attempt id (history and current). Any collision
  fails closed.
- Effect: append the current `FAILED` run to `attemptHistory`; create a new
  `RuntimeAgentRun` in `PLANNING` (new id, same goal/workItem, no lease, no result) as
  the current `agentRun`; Goal `RETRY_PENDING -> ACTIVE`.
- `lastIssuedFenceToken` is **not** reset; fence tokens stay monotonic across
  attempts.

### `abandonGoal()` (new)

- Precondition: Goal status `RETRY_PENDING`.
- Effect: Goal `RETRY_PENDING -> FAILED` (terminal). The current `FAILED` run and the
  history are unchanged. This is the explicit "retries stopped" terminal a controller
  calls when the decider refuses further retry. No reason is stored (the queue stores
  disposition only, and the audit lives in the retained attempt results).

## Invariant changes (`AgentRuntimeState.validateStructure`)

- Validate `attemptHistory`: each entry terminal `FAILED` with a result message,
  belonging to the Goal's WorkItem; all `agentRunId` values unique across history plus
  current; size within the cap.
- Goal status ↔ current run:
  - `ACCEPTED` ⇔ no current run, empty history, revision 0, fence 0 (unchanged).
  - `ACTIVE` ⇔ current run non-terminal.
  - `RETRY_PENDING` ⇔ current run terminal `FAILED` with a result.
  - `FAILED` (terminal) ⇔ current run terminal `FAILED` with a result.
  - `COMPLETED` ⇔ current run `COMPLETED` with a Verified result.
- Fence invariant relaxed: "a `PLANNING` run cannot follow an issued fence" applies
  **only when the history is empty** (the first attempt). A retry `PLANNING` run
  (non-empty history) may follow an issued fence (`lastIssuedFenceToken > 0`).
- "post-execution / terminal state requires an issued fence" is retained.

## Persistence (`FileSystemAgentRuntimeStateStore`)

- Serialize `attemptHistory` as a length-prefixed list using the existing
  `RuntimeAgentRun` encoding, and add the `RETRY_PENDING` Goal-status value.
- Schema v1 is revised in place. A snapshot written without the history section fails
  closed on load (matching the control-request-ledger precedent:
  "schema v1 revised in place so older snapshots without the ledger fail closed").
  Runtime state is per-logical-run and transient, so no cross-version persisted state
  exists in practice.
- A fresh store instance recovers the exact ordered history, the current run, the Goal
  status (including `RETRY_PENDING`), the monotonic `lastIssuedFenceToken`, and the
  revision.

## `DurableAgentRuntime`

Add persist-before-exposure operations `beginRetryAgentRun(String newAgentRunId)` and
`abandonGoal()`, plus accessors `completedAttempts()` and `attemptHistory()`. Existing
operations are unchanged except that `recordResult`'s non-Verified path now yields a
`RETRY_PENDING` Goal (through the changed `AgentRuntimeState.recordResult`).

## Verification plan

Test-first, focused, mirroring the runtime suite style.

- `AgentRuntimeStateTest` / `DurableAgentRuntimeTest` (extend):
  - a non-Verified result yields AgentRun `FAILED` and Goal `RETRY_PENDING` (updated
    from the previous `FAILED` expectation);
  - `beginRetryAgentRun` from `RETRY_PENDING` archives the failed run into history,
    starts a `PLANNING` run with a fresh id, sets the Goal `ACTIVE`, keeps
    `lastIssuedFenceToken` monotonic, and advances the revision;
  - a retry `PLANNING` run may carry `lastIssuedFenceToken > 0`;
  - `beginRetryAgentRun` is rejected from `COMPLETED`, from `ACTIVE`, when the current
    run is not `FAILED`, when the new id collides with any prior attempt/identity, and
    when the attempt cap is reached;
  - `abandonGoal` from `RETRY_PENDING` reaches terminal `FAILED`; it is rejected from
    any other Goal status;
  - `completedAttempts()` returns the expected count across a fresh attempt,
    `RETRY_PENDING`, a second attempt, and `COMPLETED`;
  - the full lifecycle still runs (`beginAgentRun -> markReady -> acquireLease ->
    completeExecution -> recordResult`) for both a first attempt and a retry attempt.
- `FileSystemAgentRuntimeStateStoreIntegrationTest` (extend): persist a multi-attempt
  state (non-empty history + `RETRY_PENDING`, and separately a retry `ACTIVE` state),
  recover it through a fresh store instance, and assert exact history order, current
  run, Goal status, fence token, and revision; a snapshot missing the history section
  fails closed.
- Existing tests that assert Goal `FAILED` after a non-Verified result are updated to
  `RETRY_PENDING` (enumerated during planning via a repository search). The finalizer,
  worker, queue, and decider are expected to need **no** change because they key off
  the AgentRun status and the queue disposition; the plan verifies this by search and
  by the unchanged full regression.
- Full `gradlew build` (strict Java 17 `-Xlint:all -Werror`) and the document
  structural checks pass with fresh output; evidence appended to
  `docs/verification-log.md`.

## Consumer and continuation

The named consumer (ROADMAP Contract Continuation Rule) is the retry controller/worker
of the next sub-increment, recorded in `CURRENT_TASK.md` Next: resolve the Goal's
external-effect ledger, read `completedAttempts()`, call `AgentRunRetryDecider`, and on
an admitted decision call `beginRetryAgentRun` and drive the new attempt (or call
`abandonGoal` on refusal), with named integration evidence and queue reconciliation.

## Risks

- Changing `recordResult`'s Goal status (`FAILED -> RETRY_PENDING`) could affect any
  existing recovery path or test that reads the Goal terminal status as "done". The
  plan enumerates every reader via repository search (`DurableAgentRunWorker`,
  finalizer, integration tests) and confirms each keys off AgentRun status or queue
  disposition, updating only the tests that assert the old Goal status.
- Schema fail-closed makes a pre-change runtime snapshot unloadable. This is accepted
  and consistent with the control-request-ledger precedent; runtime state is transient
  per logical run.

## Out of scope

- Wiring `AgentRunRetryDecider`, building a retry controller, driving a retry through
  the worker, reconciling a failed-then-retried WorkItem with the Scheduler queue, or
  consulting the external-effect ledger.
- Re-execution, backoff or delay, budgets beyond the structural attempt cap,
  stagnation, priority/fairness, orphan reclamation, and authenticated
  cancel/pause/resume.
- Any change to `DurableAgentRunFinalizer`, `DurableAgentRunWorker`, the Scheduler
  queue, or `AgentRunRetryDecider`.
- Commit, push, PR, merge, release, or deployment without a new explicit request.
