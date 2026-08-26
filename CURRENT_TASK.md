# Current Task

## Status

Completed

## Task

Implement RFC-0018's coordinated durable ModelWork migration as a bounded sequential
workflow while keeping typed ModelWork non-executable until separately authorized
RunRecord v2 and admission integration exist.

## Task ID

implement-coordinated-durable-model-work-migration

## Context

The pure `ModelWorkPayload`, complete profile-bearing execution input, ModelWork-only
message-envelope/process-spool v2, and current manifest v3, queue v4, and AgentRuntime
v5 dual-payload retention are implemented and verified. Migration-only ordered readers
and a complete-closure zero-write preflight are implemented. Candidate preparation and
consumer-first real-filesystem temporary cutover are also implemented and verified;
all publication-boundary crash re-entry, current-prefix preservation, candidate cleanup,
and source-drift hardening are implemented and verified. The explicit bounded operator,
legacy read-file migration/restart closure, model-only zero-write refusal, external typed
receive refusal, and existing execution guards are also verified. Current execution
paths still assume legacy RunRecord v1, so typed ModelWork remains non-executable.

## Justified By

- User continuation request on 2026-08-25 into the coordinated durable ModelWork migration implementation
- User continuation request on 2026-08-21 into the Scheduler complete-profile transport specification

## Approval

The user's explicit 2026-08-25 continuation authorizes bounded RED-first local
implementation of RFC-0018's coordinated durable migration, owning tests and documents,
checkpoint operations, fresh focused and full verification, and ordinary local commits
at verified GREEN increment boundaries. It authorizes schema and production Java
changes only within the declared workflow and build/JUnit-owned temporary migration
fixtures. It does not authorize mutation of real user artifacts or any external
delivery effect.

## Acceptance Criteria

- Submission manifest v3, Scheduler queue v4, and AgentRuntime v5 retain and recover
  both legacy `WorkPayload` and exact `ModelWorkPayload`, including every profile
  component, while capability remains an independent projection.
- Priority, dependencies, lease, status, retry and history data round-trip exactly;
  identity/content conflicts are refused before mutation, and ordinary old-schema
  readers remain fail closed.
- Existing legacy envelope/spool goldens and detached-cancellation signing bytes remain
  exact; legacy read-file spool v1 is not reframed.
- An explicit point-resolved stopped-owner plan preflights the complete named closure
  before writing. Legacy work containing `read-file`, including mixed scope and absent
  input, migrates losslessly; unprofiled legacy model work returns
  `UNMIGRATABLE_LEGACY_MODEL_WORK` / `PROFILE_REQUIRED` with zero named-root or candidate
  writes.
- Candidates are written only after successful preflight, reread and validated, then
  published result point, typed work spool v2, AgentRuntime v5, queue v4, manifests v3,
  and named ingress points. Source drift, corrupt/future/unsupported schemas, partial
  closures, and mixed-version operation fail closed.
- Crash re-entry resumes at the first old point, preserves current prefixes without
  rewrite or rollback, and treats exact current targets as non-writing
  `ALREADY_CURRENT`.
- Current typed ModelWork is refused before Scheduler model execution, child launch,
  admission, or gateway activity; external ModelWork receive remains unsupported and
  no Model RunRecord v2 claim is made.
- Focused migration, codec/golden, execution-guard, CLI, and governance tests plus the
  README-owned full Java 17 Gradle test pass freshly; owning documents and append-only
  verification evidence are synchronized.

## Out Of Scope

Model RunRecord v2, exact-task or policy sourcing, RFC-0015/RFC-0016 caller/admission
integration, candidate suitability, gateway/provider/adapter/route/endpoint/network or
remote transmission, credentials or spend, new ModelWork submission/receive/runtime
execution, real artifact cutover outside build/JUnit temporary roots, unrelated schema
migrations, repository scans for migration scope, retention cleanup, cross-store
atomicity or directory power-loss claims, capability maturity or whole-Gate promotion,
destructive actions, permission or secret changes, fetch, push, merge, release,
deployment, tags, branches, or history rewrite.

## Allowed Tools

- read-file
- write-code
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit

## Verification

Evidence is appended once per completed increment to `docs/verification-log.md` after
the declared checks complete.

## Dynamic Workflow

Workflow ID: implement-coordinated-durable-model-work-migration
Mode: Sequential
Increment Limit: 5
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed or unrelated verification, incomplete stopped-owner
proof, ambiguous closure identity, source drift, checkpoint drift, task drift, new
external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - current-schema-dual-payload-retention

State: Completed
Depends On: none
Scope: Establish RED tests, then make WorkItem and current manifest v3, queue v4, and
AgentRuntime v5 representations retain legacy WorkPayload or exact ModelWorkPayload
without connecting execution. Add explicit typed-ModelWork execution guards and keep
all shared envelope/profile encoding canonical.
Exit Criteria: Both payload families and all owning state round-trip exactly, profile
and capability remain separate, tamper/conflict checks precede mutation, current
execution paths refuse typed ModelWork, and legacy golden bytes remain exact.
Verification: Focused WorkItem, manifest, queue, AgentRuntime, execution-guard,
MessageEnvelope codec/spool, cancellation-golden, and governance tests followed by the
README-owned full Gradle test task.
Next Action: Select legacy-inspection-and-zero-write-preflight after recording evidence
and committing the verified GREEN increment.

### Increment 2 - legacy-inspection-and-zero-write-preflight

State: Completed
Depends On: current-schema-dual-payload-retention
Scope: Add migration-only ordered decoding and an explicit closure plan that inspects
all named points under a pre-existing stopped-owner fence, classifies legacy work, and
prepares validated candidates only after a complete zero-write preflight.
Exit Criteria: Manifest v1 through v3, queue v2 through v4, and runtime v4 through v5
are inspectable in the required order; read-file work is lossless; legacy model,
corrupt, future, partial, or cross-store mismatched closures refuse before any named-
root or candidate write.
Verification: Focused store migration and coordinated preflight tests, filesystem
entry/byte/timestamp invariance checks, candidate-absence checks, and governance tests.
Next Action: Completed with fresh evidence; select prepare-and-consumer-first-cutover.

### Increment 3 - prepare-and-consumer-first-cutover

State: Completed
Depends On: legacy-inspection-and-zero-write-preflight
Scope: Prepare and reread all candidates after preflight, revalidate immutable binding
points, and publish a complete real-filesystem temporary closure in consumer-first
order without altering checkpoint or effect-ledger bytes.
Exit Criteria: A named closure becomes exactly current in result, typed work spool,
runtime, queue, manifest, and ingress order while all payload, profile, capability,
priority, dependency, lease, retry, status, and history data remains equal.
Verification: Coordinated real-filesystem cutover integration tests plus spool and
Scheduler recovery-status regressions and governance tests.
Next Action: Completed with fresh evidence; select
crash-reentry-source-drift-hardening after committing the verified GREEN increment.

### Increment 4 - crash-reentry-source-drift-hardening

State: Completed
Depends On: prepare-and-consumer-first-cutover
Scope: Prove every publication boundary, source-drift refusal, candidate cleanup,
mixed-closure fail-closed behavior, and idempotent re-entry without rollback or
rewriting an already-current prefix.
Exit Criteria: All bounded crash points resume at the first old point; current targets
are non-writing ALREADY_CURRENT; source drift preserves the changed source and all
later targets; operation remains blocked while the closure is mixed.
Verification: Parameterized coordinator crash/re-entry tests, existing per-store drift
regressions, filesystem byte/timestamp assertions, and governance tests.
Next Action: Completed with fresh evidence; select
bounded-operator-and-recovery-closure after committing the verified GREEN increment.

### Increment 5 - bounded-operator-and-recovery-closure

State: Completed
Depends On: crash-reentry-source-drift-hardening
Scope: Expose only the explicit bounded stopped-owner migration operation, close
recovery and CLI regressions, run full verification, and synchronize owning lifecycle
documents without enabling typed ModelWork submission or execution.
Exit Criteria: A temporary read-file closure migrates and restarts with exact retained
state; unprofiled legacy model and external typed ModelWork receive refuse zero-write;
typed ModelWork cannot launch a child or reach admission/gateway; documents and Git
state truthfully represent the completed local work.
Verification: Focused CLI, receiver, isolated-execution, recovery-status, migration,
codec/golden, governance, and README-owned full Gradle test tasks plus diff and commit
inspection.
Next Action: Completed with fresh evidence and a verified local commit boundary. Await
separate authority for Model RunRecord v2 and Scheduler model-work admission integration;
request explicit delivery authority if a push is desired.

## Next

Await separate user authority for Model RunRecord v2 and Scheduler model-work admission
integration. No next Active Task is authorized; request explicit delivery authority if
a push is desired.
