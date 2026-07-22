# Current Task

## Status

Completed

## Task

Wire `DurableAgentRunRetryController` into `DurableAgentRunWorker` so one active WorkItem
can recoverably execute a checkpointed replacement AgentRun after an admitted decision,
or reach one terminal failed queue disposition after a refused decision.

## Task ID

wire-retry-aware-worker-recovery

## Context

The runtime, finalizer, retry decider, and durable retry controller now preserve the
correct attempt-level boundary, but the Worker still parks forever when a Goal reaches
`RETRY_PENDING`. Its cycle-intent checkpoint records only the current AgentRun and
RunRecord reference, so no durable prefix can distinguish an admitted decision before a
replacement identity is appended.

The Worker must also establish one exact empty Goal ledger before the first execution.
That creation is a normal Goal-start artifact, not a decision-time fallback: retry
decision recording must continue to fail closed if the expected ledger is missing.

## Justified By

- 2026-07-22: Separate Retryable AgentRun Failure From Terminal WorkItem Disposition
- 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution
- 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

## Acceptance Criteria

- `PendingFinalization` durably distinguishes the current AgentRun/optional RunRecord
  reference from an optional checkpointed replacement AgentRun identity. Identity and
  phase invariants reject collisions or a replacement without a completed current
  attempt reference.
- `FileSystemPendingFinalizationStore` persists the extended checkpoint in an explicit
  schema version, round-trips every phase, and continues to fail closed on unsupported,
  corrupt, truncated, oversized, trailing, or invalid state. No schema-v1 migration is
  inferred.
- The Worker and `processIsolated` composition require an explicit bounded
  `AgentRunRetryPolicy` and external-effect store. The first dispatched Goal creates one
  exact empty ledger before execution; recovery reuses an existing ledger and never
  overwrites or invents one during retry decision recording.
- After a failed attempt reaches `RETRY_PENDING`, the Worker records or replays the exact
  controller decision. A refused decision abandons the Goal, records one queue `FAILED`
  disposition, clears the checkpoint, and never executes a replacement.
- For an admitted decision, the Worker checkpoints one canonical replacement AgentRun
  identity before asking the controller to append it. It then rewrites the cycle intent
  to that replacement with no stale RunRecord reference and drives the existing active
  WorkItem through the normal fenced execution/finalization path.
- Recovery is idempotent when interrupted before decision persistence, after admitted
  decision persistence, after replacement-ID checkpointing, after replacement append,
  after cycle-intent rollover, and after the replacement RunRecord reference is
  checkpointed. No completed attempt is executed twice and no extra replacement is
  appended.
- A failed first attempt followed by a Verified second attempt produces one
  `VERIFIED_COMPLETED` disposition and releases dependents. Two failed attempts under a
  two-attempt policy produce one terminal `FAILED` disposition and leave dependents
  blocked. Goal-wide fences and immutable attempt/decision histories remain intact.
- Focused state/store/worker tests and a named real-filesystem queue/runtime/ledger/
  checkpoint/RunRecord integration prove the forward and recovery paths. The full Gradle
  build, strict Java lint, structural-document checks, and diff checks pass with fresh
  evidence.

## Out Of Scope

- External adapter invocation, automatic compensation, cross-attempt idempotency-key
  reuse, user override, delay/backoff, token/time budgets, priority/fairness, authenticated
  control application, or concurrent/multi-process workers.
- Runtime schema-v1 migration, queue or runtime schema changes, new RunRecord semantics,
  orphaned RunRecord cleanup, supported CLI wiring, multi-agent execution, release, or
  deployment.
- Commit, push, PR, or merge unless separately requested.

## Approval

The user explicitly asked to continue the project on 2026-07-22, activating the next task
recorded by the completed durable retry-controller increment.

## Verification

Focused RED reached test compilation and failed with the expected four missing-signature
errors. Expanded focused verification passed 46 tests across five controller, checkpoint,
Worker, and filesystem integration suites. Fresh `clean test` passed 81 suites and 440
tests with 3 existing Windows privilege skips, 0 failures, and 0 errors under the
build-enforced Java 17 `-Xlint:all -Werror`. Final structural-document and diff evidence
is recorded in `docs/verification-log.md`.

## Next

Assess the remaining Gate 8 single-agent runtime connection gaps after retry-aware Worker
integration, without starting authenticated controls, external-effect adapters, or Gate 9.
