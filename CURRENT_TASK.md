# Current Task

## Status

Completed

## Task

Decide whether a failed AgentRun may be retried through a further AgentRun with a pure,
fail-closed decision over an attempt budget and the external-effect ledger, before any
mechanism that creates or runs a second AgentRun exists.

## Task ID

gate-8-agentrun-retry-decision

## Context

Gate 8 drives one at-least-once WorkItem through a fenced AgentRun to a recoverable
terminal `WorkItemDisposition`, and the bounded fence-checked external-effect ledger
records each non-read-only effect as `PREPARED`, `APPLIED`, `DEDUPLICATED`, `COMPENSATED`,
or `REQUIRES_USER_RECOVERY`. Nothing yet decides whether a failed AgentRun may be retried.

Connection #6 in the Gate 8 sequence (retry through additional AgentRuns) is next, but a
second AgentRun must not launch while an external effect's real-world outcome is unknown
(`PREPARED`) or explicitly awaiting a human (`REQUIRES_USER_RECOVERY`). This first slice
therefore adds only the decision: a pure, deterministic, fail-closed judgement. It creates,
persists, and runs no AgentRun, mutates no store, and grants no authority.

## Justified By

- 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution
- 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry

## Acceptance Criteria

- An immutable `AgentRunRetryPolicy` bounds `maxAttempts` to 1 through 16 inclusive,
  counting the first attempt, and provides no production default.
- A pure `AgentRunRetryDecider.decide(lastDisposition, completedAttempts, policy,
  ledgerState)` reads no store, creates/persists/runs no AgentRun, and mutates nothing.
- The decision is deterministic with fixed first-match precedence: a non-`FAILED`
  disposition is `NOT_FAILED`; any `PREPARED` effect is `UNRESOLVED_EXTERNAL_EFFECT`; any
  `REQUIRES_USER_RECOVERY` effect is `EFFECT_REQUIRES_USER_RECOVERY`; an exhausted budget is
  `ATTEMPTS_EXHAUSTED`; otherwise a further AgentRun is admitted.
- Safety-critical ledger reasons precede the budget check, so an unresolved or
  recovery-pending effect blocks retry regardless of remaining attempts; only `APPLIED`,
  `DEDUPLICATED`, and `COMPENSATED` effects, or an empty ledger, count as resolved.
- `completedAttempts` is validated to 1 through 16 inclusive; a null disposition, policy, or
  ledger fails closed.
- Focused RED/GREEN tests, the full build, strict Java lint, and document structural checks
  pass with fresh output.

## Out Of Scope

- Creating, persisting, or executing a second AgentRun; driving the worker; any queue,
  runtime, lease, fence, or ledger mutation.
- Deriving attempt counts from durable history; any new durable store, schema, or CLI
  command; resolving a ledger from a Goal identity.
- Backoff or delay, stagnation, budgets beyond attempt count, priority/fairness, orphan
  detection/reclamation, authenticated cancel/pause/resume, and external-adapter execution.
- Release packaging, deployment, or external state changes beyond an explicitly authorized
  repository commit, feature-branch push, and merge into `main`.

## Approval

Implementation was approved by the user's 2026-07-22 request to continue the project from
the recorded next task, and the design was reviewed and approved before implementation.
Repository delivery beyond the local feature branch requires a separate explicit request.

## Verification

Fresh RED/GREEN, full regression, strict-lint, and document structural evidence is recorded
in `docs/verification-log.md` under "Bounded AgentRun Retry Decision Verification".

## Next

Wire the decider into the durable worker or a retry controller so a FAILED AgentRun's
admitted decision actually creates and drives a second immutable AgentRun for the same
WorkItem (connection #6 second slice): resolve the Goal's external-effect ledger, supply a
durable per-WorkItem attempt count, preserve terminal history, and prove it with named
integration evidence.
