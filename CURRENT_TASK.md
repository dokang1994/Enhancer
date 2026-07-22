# Current Task

## Status

Completed

## Task

Correct the Gate 8 retry specifications so an AgentRun attempt failure remains distinct
from terminal Scheduler WorkItem failure, external effects fail closed across attempts,
and immutable retry history, decisions, recovery, and queue finalization form one
implementable durable sequence.

## Task ID

correct-gate-8-retry-specification-boundaries

## Context

The 2026-07-22 retry-decision and durable multi-attempt lifecycle specifications were
reviewed against the current finalizer, worker, queue, runtime store, and external-effect
ledger. The review found that the proposed flow used terminal `WorkItemDisposition.FAILED`
as an attempt-level input, allowed queue failure before retry selection, treated applied
effects as automatically retry-safe without a cross-attempt effect contract, omitted
history-prefix enforcement and durable refusal evidence, revised schema v1 in place, and
retained an API name that implementation had already corrected.

This documentation increment corrects those contracts before a second AgentRun mechanism
is implemented. It does not change production behavior or promote capability maturity.

## Justified By

- 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution
- 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry
- 2026-07-16: Separate Execution Acknowledgement From Verified Queue Completion And Sequence Remaining Connections

## Acceptance Criteria

- Retry eligibility consumes an attempt-level failed AgentRun outcome, never a terminal
  Scheduler `WorkItemDisposition` that would already have released the active queue slot.
- The WorkItem remains active across retryable failed attempts; RunRecord-backed result
  recording, retry decision, replacement-attempt creation or terminal abandonment, and
  final queue disposition have one explicit recoverable order.
- Automatic retry is admitted only for an empty external-effect ledger or one whose every
  effect is `COMPENSATED`; `PREPARED`, `REQUIRES_USER_RECOVERY`, `APPLIED`, and
  `DEDUPLICATED` each refuse automatic retry with a typed reason.
- Runtime schema v2 retains one exact ordered immutable AgentRun history and typed retry
  decisions, and the filesystem store rejects history or decision truncation, rewrite,
  reordering, invalid append, stale revision, and unsupported schema.
- The specifications use the actual `isAdmitted()` accessor and require focused coverage
  for every ledger status, null input, precedence, history, recovery, and queue boundary.
- A new accepted decision records the corrected cross-boundary contract without claiming
  implementation; Architecture, Roadmap, Project State, Current Task, handoff, changelog,
  and verification evidence are synchronized only where their owning facts changed.
- Fresh document structural checks and diff checks pass.

## Out Of Scope

- Production Java changes, schema migration implementation, retry-controller or worker
  implementation, queue/runtime store mutation, or new tests for behavior not yet built.
- Automatic compensation, external-adapter invocation, proof of remote outcome, user
  override, delayed/backoff scheduling, token/time budgets, or multi-agent execution.
- Commit, push, merge, release, deployment, or other external delivery.

## Approval

Documentation correction is approved by the user's 2026-07-22 request to improve and
rewrite the reviewed specifications.

## Verification

Fresh document ownership and decision-index tests, stale-contract scans, and diff checks
passed. Evidence is recorded in `docs/verification-log.md` under
"Gate 8 Retry Specification Boundary Correction Verification".

## Next

Implement the corrected attempt-level retry decision and schema-v2 immutable AgentRun
history test-first, then split result recording from terminal queue disposition before
wiring the durable retry controller and worker path.
