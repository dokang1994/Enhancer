# Current Task

## Status

Completed

## Task

Persist one machine-written development-session checkpoint outside canonical project
documents and require it at atomic implementation and verification boundaries so a
forced termination can resume without inferring the last successful step from chat.

## Task ID

persist-development-session-checkpoints

## Context

Orderly session close updates canonical documents and reduces `SESSION_HANDOFF.md`, but
forced termination or token exhaustion can prevent that procedure from running. The
working tree preserves file changes, yet it does not identify whether the interrupted
step was pending, successful, or failed, which evidence belongs to it, or which exact
action should resume.

The recovery state must not become another documentation authority. It will live below
the already ignored `.enhancer/` runtime-artifact root and bind to the active task
contract by identity and digest while retaining only execution-position facts and
artifact/evidence references.

## Justified By

- 2026-07-21: Persist Development Session Checkpoints Outside Canonical Project Documents
- 2026-07-10: Use Explicit Session Resume Protocol
- 2026-07-10: Use Repository Documents As Durable Memory

## Acceptance Criteria

- One immutable checkpoint contract records a generated run identity, active task ID and
  task-contract digest, monotonic revision, typed execution state, current step, last
  successful step, next action, bounded evidence references, and a bounded manifest of
  relative artifact paths with present/missing state and content digest.
- The task-contract digest is derived from the active task's identity, task, justification,
  acceptance, out-of-scope, and approval sections; lifecycle status, verification, and
  next-task edits do not change it, while approved-scope edits do.
- The filesystem store below `.enhancer/session-checkpoint/` publishes bounded,
  strict-UTF-8, integrity-checked state atomically and fails closed on corruption,
  oversize, unsupported schema, symbolic-link boundaries, task drift, stale revision,
  or a different active run.
- CLI start, record, show, and clear commands make the mechanism usable by repository
  sessions. Start never overwrites an active checkpoint; record is fenced by the expected
  revision; show exposes bounded recovery facts; clear requires a stable state and an
  unchanged recorded artifact manifest.
- Session start/resume inspects the checkpoint before planning. Implementation records
  intent before each mutating or verification step and outcome afterwards. Session close
  marks stability and clears only after applicable verification and document synchronization.
- A fresh process/store recovers the exact last successful step and pending next action.
  A forced-stop integration demonstrates resume without treating the checkpoint as
  verification or completion evidence.
- Focused RED/GREEN tests, CLI operation tests, the full build, strict Java lint, and
  document ownership/index checks pass with fresh output.

## Out Of Scope

- Background timers, token-budget introspection, automatic platform shutdown hooks, or
  remote checkpoint replication.
- Concurrent development sessions, checkpoint merging, automatic Git commits/stashes,
  or treating a checkpoint as verification evidence.
- Gate 8 external-effect fencing, retry through additional AgentRuns, or promotion of
  the product Agent Runtime.
- Commit, push, or merge beyond the explicit authority for this completed increment.

## Approval

Approved by the user's 2026-07-21 request to apply continuous machine-written run
checkpoints, inspect and correct code impact, run operation and regression tests, then
commit, push, and merge the completed work into `main`.

## Verification

Fresh RED/GREEN, actual-repository operation, full regression, strict-lint, and document
structural evidence is recorded in `docs/verification-log.md`.

## Next

Persist a bounded external-effect ledger with fence-checked, idempotent effect outcomes
before connecting retry through additional AgentRuns.
