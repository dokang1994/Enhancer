# Gate 8 Attempt-Level AgentRun Retry Decision — Corrected Design

- Date: 2026-07-22
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler)
- Connection: ROADMAP Gate 8 ordered connection #6, retry through additional
  AgentRuns.
- Status: accepted contract correction; production implementation remains a later
  test-first increment.
- Maturity target: no promotion in this documentation increment. Contract Verified
  requires corrected focused tests and implementation.

## Problem

The first retry-decision slice used terminal `WorkItemDisposition.FAILED` as the
signal that the latest AgentRun failed. That type is owned by the Scheduler queue:
recording it moves a WorkItem into the terminal failed partition, releases the active
slot, and permanently prevents that WorkItem from satisfying dependencies. It is not
an attempt-level result and therefore cannot be the input to a decision that must run
while the same WorkItem is still active.

The first slice also treated `APPLIED` and `DEDUPLICATED` effects as sufficient for
automatic retry. Those outcomes make the prior remote outcome explicit, but they do
not prove that re-running the WorkItem will skip or safely deduplicate that effect.
`ExternalEffectRequest` is bound to one exact AgentRun, and the current ledger rejects
reuse of an idempotency key with a request bound to a different AgentRun. Until a
separate cross-attempt effect-execution contract exists, only an empty ledger or a
ledger whose every effect is `COMPENSATED` is safe for automatic retry.

This corrected contract keeps retry eligibility pure and deterministic while making
its input attempt-scoped and its external-effect rule fail closed. It creates,
persists, and runs no AgentRun, mutates no store, and grants no authority.

## Boundary: attempt outcome, not WorkItem disposition

The decider consumes the exact failed `RuntimeAgentRun`, not a Scheduler
`WorkItemDisposition`:

```java
public AgentRunRetryDecision decide(
        RuntimeAgentRun lastAttempt,
        int completedAttempts,
        AgentRunRetryPolicy policy,
        ExternalEffectLedgerState ledgerState)
```

The later durable retry controller must resolve all four inputs from one Goal and one
persisted revision before calling the decider. The pure decider validates the bindings
that are visible in its values:

- `lastAttempt`, `policy`, and `ledgerState` are non-null;
- `lastAttempt.status()` must be terminal `FAILED`; any other status is refused as
  `NOT_FAILED`;
- `ledgerState.goalId()` must equal `lastAttempt.goalId()`;
- every ledger record must name the same Goal and WorkItem as `lastAttempt`;
- `completedAttempts` is in `1 .. AgentRunRetryPolicy.MAX_ATTEMPTS` and includes
  `lastAttempt`.

The controller additionally proves that `lastAttempt` is the latest AgentRun retained
by the durable runtime and that the ledger was resolved through that same Goal. A
caller-supplied unrelated empty ledger must never be accepted by production wiring.

## Value contracts

### `AgentRunRetryPolicy`

```java
public record AgentRunRetryPolicy(int maxAttempts) {
    public static final int MAX_ATTEMPTS = 16;
    public static AgentRunRetryPolicy of(int maxAttempts);
}
```

`maxAttempts` counts the first attempt and is bounded to 1 through 16 inclusive.
`maxAttempts == 1` permits no retry. There is no production default; selecting a
budget belongs to the supported composition that wires the controller.

### `AgentRunRetryRefusalReason`

```java
public enum AgentRunRetryRefusalReason {
    NOT_FAILED,
    UNRESOLVED_EXTERNAL_EFFECT,
    EFFECT_REQUIRES_USER_RECOVERY,
    NON_COMPENSATED_EXTERNAL_EFFECT,
    ATTEMPTS_EXHAUSTED
}
```

- `NOT_FAILED`: the supplied attempt is not terminal `FAILED`.
- `UNRESOLVED_EXTERNAL_EFFECT`: at least one effect is `PREPARED`, so its remote
  outcome is unknown.
- `EFFECT_REQUIRES_USER_RECOVERY`: at least one effect explicitly requires a person.
- `NON_COMPENSATED_EXTERNAL_EFFECT`: at least one effect is `APPLIED` or
  `DEDUPLICATED`; the outcome is known but no current contract proves another attempt
  will avoid repeating it.
- `ATTEMPTS_EXHAUSTED`: `completedAttempts >= maxAttempts`.

### `AgentRunRetryDecision`

```java
public final class AgentRunRetryDecision {
    public static AgentRunRetryDecision admitted();
    public static AgentRunRetryDecision refused(AgentRunRetryRefusalReason reason);
    public boolean isAdmitted();
    public Optional<AgentRunRetryRefusalReason> refusalReason();
}
```

`refusalReason()` is present exactly when `isAdmitted()` is false.
`refused(null)` is rejected. The `isAdmitted()` name is intentional: Java cannot
declare a static zero-argument `admitted()` factory and an instance zero-argument
`admitted()` accessor with the same signature.

## Deterministic decision order

Input validation and identity binding run first. The following first-match order is
then fixed:

1. `lastAttempt.status() != FAILED` → `refused(NOT_FAILED)`
2. any `PREPARED` effect → `refused(UNRESOLVED_EXTERNAL_EFFECT)`
3. any `REQUIRES_USER_RECOVERY` effect →
   `refused(EFFECT_REQUIRES_USER_RECOVERY)`
4. any `APPLIED` or `DEDUPLICATED` effect →
   `refused(NON_COMPENSATED_EXTERNAL_EFFECT)`
5. `completedAttempts >= policy.maxAttempts()` →
   `refused(ATTEMPTS_EXHAUSTED)`
6. otherwise, the ledger is empty or every effect is `COMPENSATED` → `admitted()`

Safety reasons precede budget exhaustion so recovery and external-effect risk remain
visible rather than being hidden behind a cheaper terminal explanation.

## Behaviour table

| Last attempt | Ledger | Budget | Decision |
|---|---|---|---|
| not FAILED | any valid bound ledger | any valid count | `NOT_FAILED` |
| FAILED | any `PREPARED` | any | `UNRESOLVED_EXTERNAL_EFFECT` |
| FAILED | no PREPARED; any `REQUIRES_USER_RECOVERY` | any | `EFFECT_REQUIRES_USER_RECOVERY` |
| FAILED | no earlier safety reason; any `APPLIED` or `DEDUPLICATED` | any | `NON_COMPENSATED_EXTERNAL_EFFECT` |
| FAILED | empty or all COMPENSATED | exhausted | `ATTEMPTS_EXHAUSTED` |
| FAILED | empty or all COMPENSATED | remaining | admitted |

## Durable consumer contract

An admitted decision is not authority to execute by itself. The later controller must:

1. recover the exact Goal and latest failed attempt;
2. resolve the Goal's ledger and validate the runtime/ledger binding;
3. compute the completed-attempt count from the immutable runtime history;
4. call this decider;
5. persist a typed retry-decision record before either appending a replacement
   AgentRun or terminally abandoning the Goal;
6. leave the Scheduler WorkItem active when a replacement attempt is admitted;
7. record terminal `WorkItemDisposition.FAILED` only after retry is refused and the
   Goal is durably terminal.

No code may synthesize `WorkItemDisposition.FAILED` merely to call the decider.

## Verification plan

Focused RED/GREEN evidence must cover:

- policy acceptance at 1 and 16 and rejection at 0 and 17;
- `isAdmitted()`, refusal reason presence, equality, and null refusal rejection;
- null `lastAttempt`, policy, and ledger inputs;
- completed-attempt counts 0, 1, 16, and 17;
- an attempt in every non-FAILED status returning `NOT_FAILED`;
- mismatched Goal and WorkItem bindings failing closed;
- empty and all-`COMPENSATED` ledgers admitting with budget remaining;
- each of `PREPARED`, `REQUIRES_USER_RECOVERY`, `APPLIED`, and `DEDUPLICATED`
  refusing with its typed reason;
- the complete precedence matrix when multiple statuses coexist and the budget is
  exhausted;
- proof that the decision reads no store and mutates none of its inputs.

Full build, strict Java 17 lint, document ownership checks, decision-index checks,
and a named integration test for the later durable consumer are separate required
evidence. The documentation correction alone does not satisfy Contract Verified.

## Out of scope

- Implementing the corrected signature or status rules in this documentation task.
- Creating, persisting, leasing, or executing a replacement AgentRun.
- Automatically compensating an effect or treating local ledger state as remote proof.
- A cross-attempt effect adapter that can safely reuse one logical idempotency identity.
- User override, delay/backoff, token/time budgets, priority, multi-agent execution,
  release, or deployment.
