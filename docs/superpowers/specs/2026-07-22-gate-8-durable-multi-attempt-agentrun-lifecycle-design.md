# Gate 8 Durable Multi-Attempt AgentRun Retry — Corrected Design

- Date: 2026-07-22
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler)
- Connection: ROADMAP Gate 8 ordered connection #6, retry through additional
  AgentRuns.
- Status: schema-v2 state/storage plus finalizer split and safe worker parking implemented;
  durable retry controller and replacement execution remain later increments.
- Maturity target: Contract Verified for the state and controller contracts, then
  Integrated only after a named queue-to-runtime-to-ledger-to-worker recovery path.

## Problem

The original runtime retained one AgentRun, the original finalizer converted a failed
AgentRun directly into terminal queue failure, and the original worker cleared its cycle
checkpoint after that disposition. The implemented schema-v2 boundary now records the
failed attempt at `RETRY_PENDING`, keeps the WorkItem active, and retains the worker
checkpoint; the controller that decides and appends a replacement remains separate.

The corrected design must preserve four distinct facts:

1. execution acknowledgement moves one AgentRun to `AWAITING_VERIFICATION`;
2. RunRecord-backed result recording terminates one AgentRun attempt;
3. a durable retry decision either appends another immutable attempt or terminally
   stops the Goal;
4. Scheduler `WorkItemDisposition` is written only when the whole Goal is terminal.

An AgentRun attempt failure is therefore not a Scheduler WorkItem failure.

## Core invariants

- One Goal retains one exact WorkItem and an ordered immutable AgentRun list.
- `agentRun()` remains a latest-attempt projection; it never hides or rewrites earlier
  attempts.
- Every earlier attempt is terminal `FAILED` with its exact result message.
- The WorkItem stays active while the Goal is `ACTIVE` or `RETRY_PENDING`, including
  across failed attempts admitted for retry.
- Only terminal Goal `COMPLETED` maps to `VERIFIED_COMPLETED`; only terminal Goal
  `FAILED` maps to queue `FAILED`.
- The retry decision consumes the latest failed attempt, not a terminal
  `WorkItemDisposition`.
- A retry decision and the action it authorizes are persist-before-exposure and
  idempotently recoverable.
- External effects admit automatic retry only when the bound ledger is empty or every
  effect is `COMPENSATED`.
- Queue state, runtime state, effect ledger, RunRecord, and worker checkpoint remain
  separate durable boundaries; no cross-store transaction is claimed.

## Runtime schema v2

This is an incompatible runtime-state change and increments
`AgentRuntimeState.CURRENT_SCHEMA_VERSION` from 1 to 2. A schema-v1 artifact is rejected
as unsupported until a separately accepted migration exists. The format is not revised
in place under the same version number.

### Goal status

`RuntimeGoalStatus` becomes:

```text
ACCEPTED, ACTIVE, RETRY_PENDING, COMPLETED, FAILED
```

Transitions:

| From | To | Trigger |
|---|---|---|
| ACCEPTED | ACTIVE | begin the first AgentRun |
| ACTIVE | COMPLETED | record a Verified result for the latest attempt |
| ACTIVE | RETRY_PENDING | record a non-Verified result for the latest attempt |
| RETRY_PENDING | ACTIVE | persist an admitted decision and append a replacement attempt |
| RETRY_PENDING | FAILED | persist a refused decision and abandon automatic retry |

`COMPLETED` and `FAILED` are terminal. `RETRY_PENDING` is durable and non-terminal; it
exists so interruption between result recording and retry selection cannot be mistaken
for queue failure.

### Ordered AgentRun history

`AgentRuntimeState` stores one immutable `List<RuntimeAgentRun> agentRuns` with at most
`MAX_ATTEMPTS_PER_GOAL = 16` entries. `agentRun()` returns the last entry as an
`Optional`; `agentRuns()` returns the complete immutable list.

- `ACCEPTED` has an empty list.
- Every entry except the latest is terminal `FAILED` and carries its exact result.
- `ACTIVE` has a latest non-terminal attempt.
- `RETRY_PENDING` has a latest terminal `FAILED` attempt.
- `COMPLETED` has a latest terminal `COMPLETED` attempt with a Verified result.
- `FAILED` has a latest terminal `FAILED` attempt and a persisted refused decision for
  that attempt.
- Goal, WorkItem, work-message, AgentRun, result-message, and control-message identities
  remain distinct across the complete retained state.
- `completedAttempts()` counts terminal attempts in the list. It is 1 through 16 at
  `RETRY_PENDING`, equals the number of earlier failed entries during a new active
  attempt, and includes the latest attempt at either terminal Goal.
- `lastIssuedFenceToken` is Goal-wide and never resets; every replacement attempt
  receives a strictly greater fence than every earlier attempt.

### Durable retry-decision records

`AgentRuntimeState` also retains an immutable ordered
`List<AgentRunRetryDecisionRecord> retryDecisions`, bounded to one decision per failed
attempt and at most 16 entries. Each record contains:

- the failed `agentRunId`;
- `completedAttempts` and `policy.maxAttempts()`;
- the bound external-effect ledger revision and record count;
- a lowercase SHA-256 semantic digest over the canonical ledger records observed by
  the decision;
- the exact `AgentRunRetryDecision`, including its typed refusal reason when refused.

The record contains no credentials or effect payload. It makes an admitted or refused
choice replayable and explains whether automatic retry stopped for unresolved,
user-recovery, non-compensated, or exhausted-budget reasons. Exact re-entry returns the
existing decision without a revision; a different decision for the same attempt fails
closed.

## Runtime transitions

All transitions stage a candidate, persist it, and adopt it only after storage success.

### `recordAttemptResult`

The RunRecord-backed result path records the latest `AWAITING_VERIFICATION` attempt:

- Verified → AgentRun `COMPLETED`, Goal `COMPLETED`;
- any other supported verification status → AgentRun `FAILED`, Goal
  `RETRY_PENDING`.

It does not write a Scheduler queue disposition and does not clear the worker
checkpoint. Exact result re-entry is idempotent; a different reference or result for a
terminal attempt fails closed.

### `recordRetryDecision`

Preconditions:

- Goal is `RETRY_PENDING`;
- the supplied attempt is the latest AgentRun and is terminal `FAILED`;
- the controller resolved the exact Goal ledger;
- completed-attempt count, policy, ledger revision/count/digest, and decision match the
  supplied immutable inputs.

The typed decision record persists before any replacement identity is appended or the
Goal becomes terminal.

### `beginRetryAgentRun`

Preconditions:

- Goal is `RETRY_PENDING`;
- the latest attempt has an exact persisted admitted decision;
- total attempts remain below both the structural maximum and policy maximum;
- the replacement identity is canonical and distinct from every retained runtime and
  message identity.

The transition appends one `PLANNING` AgentRun to the exact existing list and changes
Goal `RETRY_PENDING -> ACTIVE`. The prior list and retry-decision list remain exact
prefixes. The fence counter is retained.

### `abandonGoal`

Preconditions:

- Goal is `RETRY_PENDING`;
- the latest failed attempt has an exact persisted refused decision.

The transition changes Goal `RETRY_PENDING -> FAILED` and retains all attempts,
results, decisions, effect references, and fence state unchanged. It does not itself
write the queue disposition.

## Finalizer boundary

`DurableAgentRunFinalizer` must no longer combine attempt result recording and terminal
queue disposition in one unconditional method.

It exposes two recoverable operations:

```text
recordAgentRunResult(goalId, agentRunId, runRecordReference)
finalizeTerminalDisposition(goalId)
```

`recordAgentRunResult` resolves and binds the RunRecord, persists the attempt result,
and returns the new runtime projection. For a failed attempt it stops at
`RETRY_PENDING`.

`finalizeTerminalDisposition` is legal only when the Goal is terminal:

- Goal `COMPLETED` + latest run `COMPLETED` → `completeActiveVerified`;
- Goal `FAILED` + latest run `FAILED` + persisted refused decision → `failActive`;
- Goal `ACTIVE` or `RETRY_PENDING` → no disposition and no queue mutation.

`recoverFinalization` follows the same Goal-level rule. A terminal AgentRun inside a
`RETRY_PENDING` Goal is not sufficient authority to fail the WorkItem.

## Durable retry controller

`DurableAgentRunRetryController` is the sole application boundary that connects the
runtime, exact Goal ledger, corrected decider, policy, and replacement identity.

For a `RETRY_PENDING` Goal it:

1. resolves the exact latest failed attempt and immutable completed-attempt count;
2. resolves the external-effect ledger by the same Goal identity;
3. verifies every effect belongs to the Goal and WorkItem and captures the ledger
   revision/count/digest;
4. calls the corrected attempt-level decider;
5. persists the exact retry-decision record;
6. when admitted, requires a previously checkpointed replacement AgentRun identity and
   appends it;
7. when refused, terminally abandons the Goal without fabricating remote evidence.

It invokes no external adapter, compensates nothing, and cannot broaden Tool authority.

## Worker ordering

The retry-aware worker preserves this order for every attempt:

```text
cycle intent with Goal/current AgentRun identities
→ claim WorkItem if not already active
→ acquire current attempt lease
→ execute and persist RunRecord
→ checkpoint RunRecord reference
→ cleanup attempt-owned execution artifacts
→ acknowledge execution
→ record attempt result only
→ if Goal COMPLETED: finalize VERIFIED_COMPLETED disposition and clear checkpoint
→ if Goal RETRY_PENDING:
     resolve ledger and decide
     → persist retry-decision record
     → if admitted:
          checkpoint deterministic replacement AgentRun identity
          append replacement attempt
          update cycle intent to replacement identity
          continue while WorkItem remains active
     → if refused:
          abandon Goal
          finalize FAILED disposition
          clear checkpoint
```

The worker never returns terminal `FAILED` for an admitted retry. Attempt budget
exhaustion or any unsafe effect produces a refused decision before terminal queue
failure.

## Recovery matrix

| Durable prefix after interruption | Recovery action |
|---|---|
| RunRecord reference checkpointed; attempt still EXECUTING | retry cleanup/acknowledgement without re-execution |
| attempt AWAITING_VERIFICATION | re-record the same result reference |
| Goal RETRY_PENDING; no decision | resolve the same ledger revision inputs and decide |
| decision persisted and admitted; replacement id checkpointed but not appended | append that exact replacement id |
| replacement appended | resume that exact latest attempt; do not append another |
| decision persisted and refused; Goal not terminal | call `abandonGoal` |
| Goal COMPLETED or FAILED; queue still active/requeued | apply the matching terminal disposition |
| terminal queue disposition recorded; checkpoint remains | verify matching disposition and clear checkpoint |

Every recovery action is idempotent over the preceding durable prefix. No recovery path
uses a terminal AgentRun alone as queue-failure authority.

## Persistence rules

`FileSystemAgentRuntimeStateStore` encodes schema v2 with the complete AgentRun and
retry-decision lists. Its update boundary must enforce, independently of runtime helper
methods:

- exactly one revision advance;
- unchanged Goal identity and exact WorkItem;
- exact AgentRun-history prefix, permitting only one valid append or one valid latest
  attempt transition per revision;
- no history truncation, rewrite, reordering, result replacement, or identity reuse;
- exact retry-decision prefix, permitting at most one valid append for the latest failed
  attempt;
- unchanged prior result messages and retry decisions;
- control-request prefix monotonicity;
- a fence that stays current or advances exactly one and never decreases;
- bounded strict UTF-8, integrity envelope, atomic publication, and rejection of
  trailing, corrupt, oversized, or unsupported state.

A fresh store recovers supplementary Unicode metadata, every result, decision,
identity, fence, and revision exactly. Schema-v1 migration is not inferred.

## External-effect rule across attempts

Automatic retry is allowed only when the bound ledger is empty or every existing effect
is terminal `COMPENSATED`. `PREPARED`, `REQUIRES_USER_RECOVERY`, `APPLIED`, and
`DEDUPLICATED` each stop automatic retry.

A compensated prior effect may be represented by a new effect identity in a later
attempt because the prior remote effect was explicitly undone. Reusing one logical
idempotency identity across different AgentRun bindings requires a separately accepted
adapter/ledger contract and is not inferred here.

## Verification plan

Implementation proceeds test-first in dependency order.

1. **Corrected pure decision:** attempt-level input, exact binding, every ledger status,
   null and bound checks, precedence, and `isAdmitted()` API.
2. **Schema-v2 runtime state:** ordered list, latest projection, completed-attempt count,
   `RETRY_PENDING`, Goal-wide fence monotonicity, identity uniqueness, decision records,
   admitted append, refused abandonment, and the 16-attempt ceiling.
3. **Filesystem state:** restart recovery plus explicit rewrite, truncation, reordering,
   invalid append, stale revision, unsupported-v1, corrupt, oversized, and Unicode cases.
4. **Finalizer split:** failed result stops at `RETRY_PENDING`; recovery cannot fail the
   queue until Goal `FAILED`; Verified completion remains unchanged.
5. **Controller:** exact Goal/ledger binding, empty/all-compensated admission, every
   refusal reason, exact replay, changed-input rejection, deterministic replacement id,
   and persisted decision before action.
6. **Worker integration:** failure followed by successful second attempt, budget
   exhaustion, unsafe effect refusal, interruption at every recovery-matrix boundary,
   stale fence rejection, no duplicate execution after reference checkpointing, active
   queue retention across retries, and one final queue disposition.
7. **Regression:** full Gradle build, strict Java 17 lint, document ownership and
   decision-index checks, plus named filesystem integration evidence.

Current capability maturity and its fresh evidence are owned by `PROJECT_STATE.md` and
`docs/verification-log.md`; this design does not promote the controller or worker retry
integration before their own evidence exists.

## Implementation increments

The design may be delivered in bounded tasks, but each task must leave current behavior
coherent:

1. correct the pure decider and its accepted decision;
2. add schema-v2 history and decision records with prefix-enforcing storage, split
   finalization, and park the current worker safely at `RETRY_PENDING`;
3. add the durable retry controller over the exact ledger and persisted decision;
4. wire replacement-attempt execution and retry-aware worker recovery integration.

An increment that changes failed results to `RETRY_PENDING` without also preventing
immediate queue `failActive` is not coherent and must not be merged.

## Out of scope

- Automatic compensation or adapter execution, and treating ledger state as proof of
  remote outcome.
- Cross-attempt reuse of an external-effect idempotency key.
- User-authorized override, authenticated pause/resume/cancel application, delay or
  backoff, token/time budgets, priority/fairness, multi-process locking, distributed
  clock-skew protocol, multi-agent execution, release, or deployment.
- Runtime schema-v1 migration; schema v1 fails explicitly until a migration is accepted
  and implemented.
