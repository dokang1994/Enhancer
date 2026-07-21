# Gate 8 Bounded AgentRun Retry Decision — Design

- Date: 2026-07-22
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler)
- Connection: ROADMAP Gate 8 ordered connection #6 (retry through additional
  AgentRuns), first slice only.
- Maturity target: Contract Verified (a pure decision type and its focused
  invariants exist; no production wiring, no second AgentRun, no persistence).

## Problem

Gate 8 now drives one at-least-once `WorkItem` through a fenced AgentRun to a
recoverable, RunRecord-backed terminal disposition (`VERIFIED_COMPLETED` or
`FAILED`), and the bounded fence-checked external-effect ledger records whether
each non-read-only effect was `PREPARED`, `APPLIED`, `DEDUPLICATED`,
`COMPENSATED`, or left `REQUIRES_USER_RECOVERY`. Nothing yet decides whether a
**failed** AgentRun may be retried through a further AgentRun.

Connecting retry naively is unsafe: a second AgentRun launched while an external
effect is still `PREPARED` (its real-world outcome unknown) could hide a replayed
effect, and a retry launched while an effect is `REQUIRES_USER_RECOVERY` would
proceed past a state that explicitly needs a human. Retry must therefore be gated
by a deterministic, fail-closed decision before any mechanism that creates or runs
a second AgentRun exists.

This increment adds only that decision as a pure value contract. It creates no
AgentRun, runs no worker, mutates no queue, runtime, lease, fence, or ledger
state, reads no store, and grants no authority.

## Scope decision: caller-supplied inputs, no new store

The decision is a pure function of four caller-supplied inputs and mirrors the
existing bounded value-contract style (`BackpressurePolicy`,
`com.enhancer.bus.RetryPolicy`):

- the last AgentRun's terminal `WorkItemDisposition`;
- a caller-supplied count of already-completed terminal attempts for the WorkItem;
- an immutable attempt bound;
- the current `ExternalEffectLedgerState` for the Goal.

Attempt counting is a caller input rather than a derived value: no durable
per-WorkItem attempt history exists yet, and building one belongs to the later
wiring increment. Deriving attempts from prior AgentRun terminal history was
considered and rejected for this slice because that history structure does not
exist and adding it widens scope past a pure decision.

The decider consumes the ledger **state value** (already per-Goal and
self-validating) and inspects effect statuses directly. It does not re-check that
the ledger's Goal matches the failed run's Goal: supplying the correct Goal's
ledger is the caller's responsibility, exactly as other pure contracts trust their
inputs. That identity binding belongs to the wiring increment that resolves the
ledger from a Goal.

## New types (all in `com.enhancer.runtime`)

Placement in `runtime` introduces no cycle: `RuntimePackageBoundaryTest`
constrains only `loop`, `run`, and `kernel` import directions, and these types
depend only on `runtime` peers (`WorkItemDisposition`, `ExternalEffectLedgerState`,
`ExternalEffectStatus`).

### `AgentRunRetryPolicy` (record)

Immutable attempt bound.

```java
public record AgentRunRetryPolicy(int maxAttempts) {
    public static final int MAX_ATTEMPTS = 16;
    // constructor validates 1 <= maxAttempts <= MAX_ATTEMPTS
    public static AgentRunRetryPolicy of(int maxAttempts) { ... }
}
```

`maxAttempts` is the maximum number of terminal AgentRun attempts permitted for
one WorkItem, counting the first attempt. `maxAttempts == 1` therefore permits no
retry (the first attempt is the only one). No `standard()` default is provided:
choosing a production retry budget belongs to the wiring increment, and offering a
default here would imply a wired policy that does not exist.

### `AgentRunRetryRefusalReason` (enum)

```java
public enum AgentRunRetryRefusalReason {
    NOT_FAILED,
    UNRESOLVED_EXTERNAL_EFFECT,
    EFFECT_REQUIRES_USER_RECOVERY,
    ATTEMPTS_EXHAUSTED
}
```

- `NOT_FAILED` — the last disposition was not `FAILED` (e.g. `VERIFIED_COMPLETED`);
  a non-failed run is not a retry candidate.
- `UNRESOLVED_EXTERNAL_EFFECT` — at least one ledger effect is `PREPARED` (its
  real-world outcome is unknown); retrying could hide a replayed effect.
- `EFFECT_REQUIRES_USER_RECOVERY` — at least one ledger effect is
  `REQUIRES_USER_RECOVERY`; automatic retry must not proceed past a state that
  explicitly needs a human.
- `ATTEMPTS_EXHAUSTED` — `completedAttempts >= maxAttempts`.

### `AgentRunRetryDecision` (value)

```java
public final class AgentRunRetryDecision {
    public static AgentRunRetryDecision admitted();
    public static AgentRunRetryDecision refused(AgentRunRetryRefusalReason reason);
    public boolean admitted();
    public Optional<AgentRunRetryRefusalReason> refusalReason();
}
```

`refusalReason()` is present iff `admitted()` is false. `refused(null)` is rejected.

### `AgentRunRetryDecider` (final class)

Stateless, public no-arg constructor so it can later be injected as a port.

```java
public AgentRunRetryDecision decide(
        WorkItemDisposition lastDisposition,
        int completedAttempts,
        AgentRunRetryPolicy policy,
        ExternalEffectLedgerState ledgerState)
```

Input validation (fail closed, before any decision):

- `lastDisposition`, `policy`, `ledgerState` non-null (`NullPointerException`);
- `completedAttempts` in `1 .. AgentRunRetryPolicy.MAX_ATTEMPTS`
  (`IllegalArgumentException` otherwise). A retry decision is only meaningful after
  at least one completed attempt, and the count is bounded like every other
  runtime quantity.

Deterministic decision order (first match wins; safety-critical reasons precede
the budget check so an unresolved or recovery-pending effect always blocks
regardless of remaining budget):

1. `lastDisposition != FAILED` → `refused(NOT_FAILED)`
2. any record `status() == PREPARED` → `refused(UNRESOLVED_EXTERNAL_EFFECT)`
3. any record `status() == REQUIRES_USER_RECOVERY` →
   `refused(EFFECT_REQUIRES_USER_RECOVERY)`
4. `completedAttempts >= policy.maxAttempts()` → `refused(ATTEMPTS_EXHAUSTED)`
5. otherwise → `admitted()`

A ledger whose effects are all `APPLIED`, `DEDUPLICATED`, or `COMPENSATED` (or an
empty ledger) is treated as resolved and does not block retry.

## Behaviour summary

| lastDisposition | ledger effects | completedAttempts vs max | decision |
|---|---|---|---|
| VERIFIED_COMPLETED | any | any | REFUSED(NOT_FAILED) |
| FAILED | any PREPARED | any | REFUSED(UNRESOLVED_EXTERNAL_EFFECT) |
| FAILED | no PREPARED, any REQUIRES_USER_RECOVERY | any | REFUSED(EFFECT_REQUIRES_USER_RECOVERY) |
| FAILED | all resolved / empty | completed >= max | REFUSED(ATTEMPTS_EXHAUSTED) |
| FAILED | all resolved / empty | completed < max | ADMITTED |

## Verification plan

Test-first, focused, mirroring the runtime suite style. New
`AgentRunRetryDeciderTest` (and small direct checks for the value types):

- `AgentRunRetryPolicy`: rejects `0` and `MAX_ATTEMPTS + 1`, accepts `1` and
  `MAX_ATTEMPTS`.
- `AgentRunRetryDecision`: `admitted()` has empty reason; `refused(reason)` carries
  it; `refused(null)` rejected.
- ADMITTED: FAILED, resolved/empty ledger, `completedAttempts < maxAttempts`.
- UNRESOLVED_EXTERNAL_EFFECT: FAILED, a `PREPARED` effect present, budget
  remaining.
- EFFECT_REQUIRES_USER_RECOVERY: FAILED, a `REQUIRES_USER_RECOVERY` effect and no
  `PREPARED`, budget remaining.
- ATTEMPTS_EXHAUSTED: FAILED, resolved ledger, `completedAttempts == maxAttempts`.
- NOT_FAILED: `VERIFIED_COMPLETED` with budget remaining and empty ledger.
- Precedence: a `PREPARED` effect **and** `completedAttempts == maxAttempts` →
  `UNRESOLVED_EXTERNAL_EFFECT` (safety over budget); a `PREPARED` **and** a
  `REQUIRES_USER_RECOVERY` effect → `UNRESOLVED_EXTERNAL_EFFECT`.
- Resolved-only ledger (`APPLIED`/`DEDUPLICATED`/`COMPENSATED`) → does not block.
- `completedAttempts` bound: `0` and `MAX_ATTEMPTS + 1` rejected.

The ledger states in these tests are built through the existing prepare/record
paths so no test fabricates an invalid `ExternalEffectLedgerState`.

Then: full Gradle build, Java 17 strict lint (`-Xlint:all -Werror`), and document
structural checks pass with fresh output; evidence appended to
`docs/verification-log.md`.

## Out of scope

- Creating, persisting, or executing a second AgentRun; driving the worker; any
  queue, runtime, lease, fence, or ledger mutation.
- Deriving attempt counts from durable history; any new durable store, schema, or
  CLI command; resolving a ledger from a Goal identity.
- Backoff or delay, stagnation, budgets beyond attempt count, priority/fairness,
  orphan detection/reclamation.
- Authenticated cancel/pause/resume, external-adapter effect execution, and broader
  production wiring.
- Commit, push, PR, merge, release, or deployment without a new explicit request.
