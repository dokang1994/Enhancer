# Current Task

## Status

Completed

## Task

Prove and document one supported two-command Scheduler operator workflow that submits governed work through `scheduler-submit` and executes it only through a separately invoked `scheduler-cycle`, preserving each command's authority, output, and recovery boundary.

## Task ID

prove-explicit-scheduler-operator-workflow

## Context

The durable submission and one-cycle commands are individually supported and their
storage/recovery contracts are verified, but no named CLI integration currently connects
the two surfaces as an operator would use them. The smallest honest workflow is an
explicit sequence over shared queue and artifact roots, not a wrapper command, polling
loop, or implicit second external effect.

This increment is an integration characterization and operator-documentation task. The
first focused run may pass without production changes if the existing commands already
satisfy the accepted composition. Any failure must be classified before changing
production behavior.

## Justified By

- 2026-07-22: Expose Durable Submission As A Separate Explicit CLI Command
- 2026-07-22: Expose One Process-Isolated Durable Scheduler Cycle Through The CLI
- 2026-07-22: Persist Submission Intent Before Creating The Scheduler Queue
- 2026-07-21: Select The Process-Isolated Durable Worker And Retire Spools After Checkpoint

## Acceptance Criteria

- A named real-filesystem CLI integration invokes `scheduler-submit` and
  `scheduler-cycle` through separate fresh `EnhancerCli` instances with one shared queue
  identity and explicit roots, and proves submission leaves pending work without
  execution before the cycle command is invoked.
- The separately invoked cycle crosses the real child JVM, Evidence, RunRecord, runtime,
  external-effect ledger, cycle checkpoint, and invocation-spool boundaries to one
  `VERIFIED_COMPLETED` disposition while retaining the immutable submission manifest.
- Exact submission replay after completion reports `REPLAYED` without changing the queue
  revision or creating a second RunRecord, and a later separate cycle reports `IDLE`
  without executing work again.
- README documents the two explicit invocations, which inputs/roots must be shared, how
  to interpret each command's independent status/exit code, and recovery actions for
  interrupted submission, interrupted cycle, terminal failed work, and idle queues.
- No wrapper command, automatic UUID/time generation, polling/service loop, combined
  status, hidden cycle invocation, or new production authority is introduced unless an
  aligned characterization failure proves a smaller correction is required.
- Focused characterization, the full Gradle build with Java 17 strict lint, an
  actual-repository smoke run, structural-document checks, and final diff checks pass
  with fresh evidence.

## Out Of Scope

- A durable invocation manifest, generated identities/time, background worker service,
  repeated cycles, queue watching, concurrent writers, or multi-process locking.
- Authenticated controls, external adapter/effect execution, Gate 9, queue/runtime schema
  migration or cleanup, or whole-Gate Operational promotion.
- Commit, push, PR, merge, release, or deployment unless separately requested.

## Approval

The user explicitly asked to continue the project on 2026-07-22, and the preceding completed task named this separate two-command operator workflow assessment as the next bounded increment.

## Allowed Tools

- read-file

## Verification

Acceptance is satisfied by the named separate-command real-filesystem CLI integration,
the actual-repository operator smoke sequence, and the fresh full strict-lint build.
Append-only command results, counts, failure classification, and artifact references are
recorded in `docs/verification-log.md`.

## Next

After this task, assess whether a separately durable invocation manifest is needed to
offer replay-safe generated identities and occurrence time without coupling submission,
execution, or polling.
