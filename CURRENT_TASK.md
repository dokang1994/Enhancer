# Current Task

## Status

Completed

## Task

Persist a bounded external-effect ledger that prepares each effect before execution and
records one fence-checked, idempotent outcome before retry through additional AgentRuns
is connected.

## Task ID

persist-fence-checked-external-effect-ledger

## Context

Gate 8 now drives one at-least-once WorkItem through a fenced AgentRun and a recoverable
RunRecord-backed terminal disposition. A worker can still repeat after lease expiry,
process interruption, or a lost acknowledgement, and no durable record currently says
whether a non-read-only external effect was prepared, applied, deduplicated,
compensated, or left for explicit user recovery.

Retry must not be connected while that fact can disappear. The first bounded increment
therefore adds a separate persist-before-exposure ledger tied to one Goal and its current
AgentRun lease. It records effect intent and outcome only; it neither invokes an external
system nor grants Tool authority.

## Justified By

- 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry
- 2026-07-16: Separate Execution Acknowledgement From Verified Queue Completion And Sequence Remaining Connections
- 2026-07-16: Fence One AgentRun Owner Before Worker Execution
- 2026-07-16: Add Product Journeys Evaluation And Layered Security Across Delivery Gates

## Acceptance Criteria

- One immutable effect request binds a stable idempotency key and semantic operation
  digest to the exact Goal, AgentRun, and WorkItem identities without carrying external
  credentials, payload content, or new Tool authority.
- One bounded ledger retains at most 256 effects for one Goal. Each effect is durably
  `PREPARED` before exposure and may terminate exactly once as `APPLIED`, `DEDUPLICATED`,
  `COMPENSATED`, or `REQUIRES_USER_RECOVERY`.
- Prepare and terminal-outcome writes require the currently executing AgentRun's matching
  unexpired owner and fence token. A stale owner, stale fence, expired lease, wrong Goal,
  wrong AgentRun, or wrong WorkItem fails closed before ledger mutation.
- Replaying the exact request or terminal outcome is idempotent and does not advance the
  ledger revision. Reusing an idempotency key for different identities, operation name,
  or semantic digest and replacing one terminal outcome with another are rejected.
- The effect store persists each monotonic revision before exposure in a bounded,
  strict-UTF-8, integrity-checked atomic schema-v1 artifact and rejects missing, corrupt,
  oversized, trailing, unsupported, symbolic-link-root, or stale-revision state.
- A fresh runtime and effect-store instance recovers the exact ordered ledger, including
  supplementary Unicode metadata and unresolved `PREPARED` effects. Recovery never
  automatically replays an effect; unresolved intent remains explicit for a later retry
  controller or user decision.
- Named filesystem integration evidence connects a real durable executing AgentRun and
  its current fence to effect preparation, terminal recording, restart recovery, exact
  replay, stale-fence refusal, and persistence-failure behavior.
- Focused RED/GREEN tests, the full build, strict Java lint, and document structural
  checks pass with fresh output.

## Out Of Scope

- Invoking an external Tool, storing an external payload or credential, or claiming that
  a ledger record proves the remote system's state without adapter evidence.
- Retry through a second AgentRun, automatic replay, compensation execution, or automatic
  resolution of a `PREPARED` effect.
- Multi-process locking, distributed clock-skew handling, schema migration beyond v1,
  parent-directory power-loss durability, or time-based history cleanup.
- Release packaging, deployment, or external state changes beyond the explicitly
  authorized repository commit, feature-branch push, and merge into `main`.

## Approval

Implementation was approved by the user's 2026-07-21 request to continue the project
from the recorded next task. Repository delivery is additionally approved by the user's
2026-07-21 request to commit the completed increment, push its feature branch, and merge
it into `main`.

## Verification

Fresh RED/GREEN, filesystem integration, full regression, strict-lint, and document
structural evidence is recorded in `docs/verification-log.md`.

## Next

Connect bounded retry through additional immutable AgentRuns using the external-effect
ledger to prevent hidden effect replay.
