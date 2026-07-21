# 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution

Status: Accepted Decision

Context:

- Gate 8 drives one at-least-once WorkItem through a fenced AgentRun to a recoverable
  terminal `WorkItemDisposition` (`VERIFIED_COMPLETED` or `FAILED`), and the bounded
  fence-checked external-effect ledger records each non-read-only effect as `PREPARED`,
  `APPLIED`, `DEDUPLICATED`, `COMPENSATED`, or `REQUIRES_USER_RECOVERY`.
- Nothing yet decides whether a failed AgentRun may be retried through a further AgentRun.
  Connection #6 in the Gate 8 sequence (retry through additional AgentRuns) is deliberately
  next, and it must not launch a second AgentRun while an external effect's real-world
  outcome is unknown or explicitly awaiting a human.
- A retry launched while an effect is still `PREPARED` could hide a replayed effect; a retry
  launched while an effect is `REQUIRES_USER_RECOVERY` would proceed past a state that needs
  a person. The decision to retry must therefore exist, be deterministic, and fail closed
  before any mechanism that creates or runs a second AgentRun does.

Decision:

- Add a pure decision contract in `com.enhancer.runtime`, expressed as four immutable types:
  `AgentRunRetryPolicy` (attempt bound), `AgentRunRetryRefusalReason` (enum),
  `AgentRunRetryDecision` (value), and `AgentRunRetryDecider` (the stateless decision).
- `AgentRunRetryPolicy` bounds `maxAttempts` to 1 through 16 inclusive, counting the first
  attempt, so `maxAttempts == 1` permits no retry. No default policy is provided, because
  choosing a production retry budget belongs to the later wiring increment.
- `AgentRunRetryDecider.decide(lastDisposition, completedAttempts, policy, ledgerState)` is a
  pure function of four caller-supplied inputs. It reads no store, mutates nothing, creates,
  persists, and runs no AgentRun, and grants no authority.
- The attempt count is a caller input rather than a derived value; no durable per-WorkItem
  attempt history exists yet, and building one belongs to the wiring increment.
  `completedAttempts` is validated to 1 through 16 inclusive.
- The decision is deterministic with a fixed fail-closed precedence, first match wins:
  a non-`FAILED` disposition is `NOT_FAILED`; any `PREPARED` effect is
  `UNRESOLVED_EXTERNAL_EFFECT`; any `REQUIRES_USER_RECOVERY` effect is
  `EFFECT_REQUIRES_USER_RECOVERY`; an exhausted budget is `ATTEMPTS_EXHAUSTED`; otherwise the
  decision admits a further AgentRun.
- Safety-critical ledger reasons precede the budget check, so an unresolved or
  recovery-pending effect blocks retry regardless of remaining attempts. Only `APPLIED`,
  `DEDUPLICATED`, and `COMPENSATED` effects (or an empty ledger) are treated as resolved.

Rationale:

- Making retry a pure, testable decision separates the safety judgement from the mechanism
  that would carry it out, so the judgement can be verified in isolation before any second
  AgentRun exists. This mirrors the project's existing bounded value contracts
  (`BackpressurePolicy`, `RetryPolicy`).
- Blocking on `PREPARED` closes the hidden-replay window the external-effect ledger was built
  to expose. Blocking on `REQUIRES_USER_RECOVERY` refuses to bypass an explicit human-recovery
  state, which is stronger than the minimum the task required but is the safer default.
- Taking the attempt count and ledger state as inputs keeps the contract honest about what
  exists today: it decides, it does not resolve a ledger from a Goal or persist attempts.

Consequences:

- A later wiring increment can consult this decider to gate a second AgentRun, but it still
  needs its own durable attempt history, ledger-to-Goal resolution, integration evidence, and
  the mechanism that actually creates and drives the retry AgentRun.
- The decider does not verify that the supplied ledger belongs to the failed run's Goal;
  supplying the correct Goal's ledger is the caller's responsibility until the wiring
  increment binds it.
- No backoff, stagnation, budget beyond attempt count, priority, orphan reclamation, or
  authenticated control application is added.
- Gate 8 remains `Specified - Next`; only this bounded retry-decision sub-capability is
  promoted, and only to Contract Verified by focused evidence.
