# Current Task

## Status

Completed

## Task

Expose a bounded read-only external-effect recovery-status projection anchored to the
Goal correlated by the existing Scheduler recovery projection.

## Task ID

gate-8-read-only-scheduler-external-effect-status

## Context

`scheduler-recovery-status` now identifies one stable checkpoint-anchored Goal and its
durable Scheduler prefix without mutation. Operators still cannot tell whether that
Goal has no effect ledger yet, an empty ledger, ambiguous `PREPARED` intent, an explicit
user-recovery outcome, a non-compensated applied/deduplicated outcome, or only
compensated effects.

The effect ledger and Evidence Store are separate durable boundaries. The checkpointed
Goal is the only permitted join anchor for this command. Terminal status is trustworthy
only when its exact evidence reference resolves with the recorded digest, and no read
can establish whether an external system actually changed beyond that retained
adapter-established evidence.

## Justified By

- 2026-07-23: Project External-Effect Recovery Through The Checkpoint-Correlated Goal
- 2026-07-23: Correlate Scheduler Recovery Prefixes Through A Read-Only Checkpoint-Anchored Projection
- 2026-07-23: Execute External Effects Through A Persist-First Evidence-Bound Adapter Boundary
- 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution
- 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry

## Acceptance Criteria

- Add a runtime-owned pure projection with typed phases for no correlated Goal,
  pre-runtime ledger absence, ledger-creation pending, empty ledger, prepared recovery,
  explicit user recovery, non-compensated effects, and all-compensated effects.
- Reuse `SchedulerRecoveryStatusReader` as the sole queue/checkpoint/runtime/RunRecord
  correlation policy; do not duplicate that binding policy in the new reader or CLI.
- Resolve an effect ledger only by the checkpoint-correlated Goal and never scan an
  effect root when the checkpoint is absent.
- Validate exact Goal, WorkItem, and historical AgentRun membership for every effect.
- Resolve every terminal effect's exact Evidence Store reference and verify that its
  stored digest matches the ledger binding; expose no evidence content.
- Take a bounded second Scheduler recovery sample and effect-ledger sample and fail
  explicitly when the correlated Scheduler observation, ledger presence, or ledger
  revision changes.
- Add a separate `scheduler-external-effect-status` command requiring the existing
  Scheduler recovery roots plus explicit external-effect and evidence roots and a
  1-through-8 output limit.
- Report the conservative aggregate phase, complete status counts, verified terminal
  evidence count, and a bounded ledger-ordered identity/status prefix within the
  existing 4096-character output ceiling.
- Treat a missing ledger as a valid prefix only before execution has advanced beyond
  `RUNTIME_RECORDED`; missing referenced evidence, corruption, inconsistent bindings,
  and concurrent drift are bounded internal failures.
- Preserve every existing command and its arguments, all persistence schemas, recovery,
  retry/effect policy, adapter authority, and worker behavior.
- Prove phase precedence and inconsistent bindings test-first, plus reader drift/evidence
  checks and real-filesystem CLI non-creation, immutability, corruption, representative
  phases, and bounded output.
- Run focused compile/tests, Scheduler/effect/CLI regressions, the full strict-lint
  build, structural document tests, and diff checks with fresh evidence.
- Synchronize architecture, state, roadmap, task, handoff, changelog, README, decision
  index, and append-only verification documents only where owned facts change.

## Out Of Scope

- Adapter invocation, automatic replay, compensation, deduplication, retry decisions,
  queue/runtime/checkpoint/effect/evidence mutation, or worker-process liveness claims.
- External-system probing, credentials, payload/evidence content output, effect-store
  scanning, filtering, history, pagination, cleanup, retention, migration, locks,
  cross-store transactions, automatic retries, waiting, polling, or service lifecycle.
- Invocation-spool status, control application, production external adapters, model/MCP/
  plugin/multi-agent behavior, commit, push, merge, release, or deployment.

## Approval

The user explicitly asked on 2026-07-23 to continue. The completed Scheduler recovery
status task named this checkpoint-anchored external-effect recovery assessment as the
next task.

## Allowed Tools

- read-file

## Verification

Verified on 2026-07-23:

- The projection/CLI contract RED produced 25 expected missing-symbol errors before
  implementation.
- The reader RED was reduced to 9 expected missing-symbol errors after correcting two
  test accessor names.
- Focused projection, reader, argument, and CLI tests passed all 27 tests across 4
  suites.
- Scheduler, external-effect, evidence, retry, worker, and CLI regressions passed all
  113 tests across 26 suites.
- Post-synchronization structure and feature checks ran 43 tests across 8 suites:
  42 passed, one existing Windows symbolic-link privilege case skipped, and no
  failures or errors occurred.
- Fresh `clean build --no-build-cache --warning-mode all --quiet` ran 533 tests across
  103 suites: 530 passed, three existing Windows symbolic-link privilege cases
  skipped, and no failures or errors occurred.
- Production/test source counts are 236/104, and the implementation performs no
  external-system probe, mutation, retry, adapter invocation, commit, push, or merge.

## Next

Assess read-only invocation-spool recovery status anchored to the same correlated Goal
and AgentRun after external-effect status is verified.
