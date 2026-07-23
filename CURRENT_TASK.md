# Current Task

## Status

Completed

## Task

Implement the bounded evidence-bound external-effect execution contract test-first: add
the adapter port and application executor, bind stable adapter identity and durable outcome
evidence into an explicit schema-v2 effect ledger, and prove persist-before-invoke plus
fail-closed restart behavior without adding a production external adapter.

## Task ID

implement-evidence-bound-external-effect-executor

## Context

The prior Goal-scoped ledger persisted current-owner/fence-checked `PREPARED` and terminal
statuses but stored neither adapter identity nor outcome evidence. This increment added the
application executor selected by the accepted 2026-07-23 boundary: it verifies semantic
identity, prepares first, invokes once, persists redacted complete evidence, and only then
publishes an evidence-bound terminal record.

The implementation preserves the unavoidable ambiguity window. A record already at
`PREPARED` when execution begins cannot authorize automatic adapter re-entry, and failures
after preparation leave the record inspectably prepared even when the adapter may have
acted or an unbound evidence artifact may exist.

## Justified By

- 2026-07-23: Execute External Effects Through A Persist-First Evidence-Bound Adapter Boundary
- 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry

## Acceptance Criteria

- A focused RED test is written first and classified against the accepted decision,
  architecture, and current runtime settings before implementation.
- `ExternalEffectRequest` binds one bounded stable adapter identity, and the adapter port
  exposes only its identity, canonical semantic operation digest, and one invocation by
  stable idempotency key; payload and credentials remain adapter-owned and are not
  persisted.
- The external-effect ledger uses an explicit schema-v2 record in which `PREPARED` has no
  outcome evidence and every terminal status has exactly one immutable evidence reference
  and SHA-256 binding. Schema-v1 filesystem artifacts fail explicitly and successor
  validation rejects evidence removal, replacement, or rebinding.
- The application executor validates adapter identity and digest before preparation,
  persists `PREPARED` before invocation, accepts only typed terminal adapter results,
  persists complete bounded evidence before terminal publication, and reuses the ledger's
  current owner/fence check for the terminal transition.
- Exact terminal replay resolves and integrity-checks its bound evidence without invoking
  the adapter or advancing the ledger revision. A pre-existing `PREPARED` record refuses
  automatic execution.
- Focused tests cover adapter and digest mismatch before mutation; prepared-before-invoke;
  successful terminal binding; restart replay; adapter, evidence, terminal-store, and
  lease-expiry failure prefixes; and schema-v1 rejection. A named real-filesystem
  integration uses a deterministic adapter and real runtime, ledger, and Evidence Store.
- No production network/Git/cloud adapter, new Tool permission, outbound-data path,
  automatic prepared recovery, second AgentRun, polling, or universal exactly-once claim
  is added.
- Relevant focused tests and a fresh strict-lint full Gradle build pass; owning
  architecture, roadmap, state, task, handoff, changelog, and append-only verification
  documents are synchronized only where facts change.

## Out Of Scope

- A real external service or production mutation adapter, credentials, operation payload
  persistence, outbound-data policy, or Gate 11 Tool authorization.
- Automatic inspection, retry, deduplication, or compensation of a record that was already
  `PREPARED` when execution began.
- Cross-attempt effect reuse, authenticated controls, polling/background services,
  multi-process locking, schema-v1 migration, commit, push, PR, merge, release, or
  deployment.

## Approval

The user explicitly asked to continue the project on 2026-07-23, and the preceding
completed assessment selected this bounded test-first implementation as the next task.

## Allowed Tools

- read-file

## Verification

Acceptance is satisfied by the fresh RED, focused GREEN, real-filesystem integration,
schema-v1 fail-closed, document-structure, diff, and full strict-lint build evidence recorded
once in `docs/verification-log.md`.

## Next

Assess Gate 8 after the evidence-bound executor increment: reconcile its remaining
cross-gate dependencies and maturity evidence, then select the smallest bounded next task
without introducing a production external adapter before Gate 11 or authenticated control
application before Gate 12.
