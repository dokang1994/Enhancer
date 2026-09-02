# Current Task

## Status

In Progress

## Task

Implement RFC-0021 sequence 1 RED-first: the pure well-formed Unicode-scalar
counter, the fixed token-aware `deterministic-fake-v2` candidate facts, and the
remaining standalone field-free suitability predicates, with no production caller
or invocation seam.

## Task ID

implement-deterministic-fake-token-capacity

## Context

RFC-0020's closed candidate and suitability evaluator are implemented and verified,
but intentionally stop at `TOKEN_SEMANTICS_UNAVAILABLE`. RFC-0021 now supplies the
accepted fake-only scalar-counting algorithm, stable v2 candidate identity, exact
response algebra, four proven capacities, and the remaining first-match predicates.
The completed delivery task recorded this implementation as the sole next action, and
the user requested continuation on 2026-09-02.

## Justified By

- User continuation request on 2026-09-02 into RFC-0021 sequence 1 implementation
- User continuation request on 2026-09-01 into deterministic fake token semantics and capacity specification

## Approval

The user's 2026-09-02 continuation authorizes the bounded RED-first standalone
implementation described by RFC-0021 sequence 1, corresponding tests, repository
document synchronization, fresh verification, and ordinary local GREEN commits. It
authorizes no production caller, actual-request budget seam, gateway or Tool
invocation, runtime/process/worker/finalizer/recovery integration, durable schema,
provider, network, credential, paid service, push, merge, release, deployment,
permission change, destructive cleanup, or external effect.

## Acceptance Criteria

- A pure counter returns `long` Unicode-scalar counts for well-formed Java strings,
  rejects null and every malformed-surrogate position, and performs no normalization,
  replacement, encoding, provider-tokenizer, or `ModelUsage` work.
- Exact fake response count derivation uses checked arithmetic and is tested against
  actual rendering across decimal digit boundaries, including the 261,986 success and
  261,987 refusal boundaries.
- The exact candidate exposes fixed `deterministic-fake-v2`,
  `deterministic-unicode-scalar-v1`, token availability, and the four RFC-0021
  capacities while retaining exactly one instance field: the bound exact gateway.
- The field-free evaluator preserves the closed first-match order, makes every
  capacity/cost/classification reason reachable, and returns `Suitable` with the exact
  admitted and candidate identities only after every fixed predicate passes.
- Production callers, actual-request counting, gateway/Tool activity, evidence,
  RunRecord/runtime effects, schemas, typed ModelWork guards, and generic `ModelUsage`
  behavior remain unchanged.
- Focused tests, architecture/governance tests, `git diff --check`, and the full
  README-owned Java 17 regression pass freshly before completion.
- Canonical documents and append-only verification evidence truthfully describe the
  implemented standalone boundary without promoting runtime capability maturity.

## Out Of Scope

Actual-request budget validation or refusal algebra; same-request/same-policy/
same-gateway invocation seam; any production caller; gateway or Tool invocation;
Scheduler execution; Model RunRecord persistence; runtime, worker, finalizer, retry,
or recovery wiring; typed ModelWork producer/receiver; provider or general tokenizer;
usage normalization; schema or migration; network, credentials, spend; push, merge,
release, deployment, permission changes, and destructive cleanup.

## Allowed Tools

- read-file
- write-code
- write-tests
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit

## Verification

Use RED-first focused tests for observable behavior. Append fresh GREEN evidence once
per completed increment to `docs/verification-log.md`; subagent recommendations are
not verification evidence.

## Dynamic Workflow

Workflow ID: implement-deterministic-fake-token-capacity
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract conflict, failed verification, malformed-input
ambiguity, production-caller or invocation drift, new authority, checkpoint drift,
exhausted bounds, or unsafe recovery.

### Increment 1 - implement-standalone-token-capacity

State: Completed
Depends On: none
Scope: Add RED tests, implement the pure scalar counter and checked fake-response
derivation, expose fixed candidate-v2 facts, and complete the standalone suitability
predicates without adding a caller or invocation.
Exit Criteria: All focused behavioral and boundary tests pass; source guards prove
the implementation remains standalone and preserves unrelated contracts.
Verification: Focused counter, gateway-algebra, candidate, suitability, and locality-
boundary tests plus `git diff --check`.
Next Action: Synchronize canonical documents, run the full regression, and close the
task in one verified follow-up increment.

### Increment 2 - verify-and-close-token-capacity

State: In Progress
Depends On: implement-standalone-token-capacity
Scope: Reconcile architecture/state/task/handoff/changelog ownership, append fresh
verification evidence once, run Markdown-sensitive governance and the full README-
owned Java 17 regression, and commit the completed standalone boundary.
Exit Criteria: All applicable checks pass freshly, canonical documents are current,
the worktree has the intended local commits only, and the checkpoint is stable and
clear.
Verification: Focused governance tests, full `./scripts/gradle.ps1 test`, diff/commit/
status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to specify the exact-request budget and
same-request invocation seam.

## Next

Synchronize canonical documents, run the full README-owned regression, and close
Increment 2 only after reading the fresh results.
