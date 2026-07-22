# Current Task

## Status

Completed

## Task

Expose one recoverable process-isolated `DurableAgentRunWorker` cycle through a supported
local `scheduler-cycle` CLI command over an already-existing durable Scheduler queue.

## Task ID

expose-durable-scheduler-cycle-cli

## Context

The real work-message path now reaches `DurableSingleWorkerSchedulerQueue`, and the
downstream Worker composes durable runtime, external-effect ledger, checkpoint,
process-isolated execution, RunRecord finalization, retry, and terminal disposition.
No supported caller constructs that composition; every execution integration still
invokes Java test fixtures directly.

Combining submission and execution in one first command would require a new durable
invocation manifest and exact cross-bus admission replay contract so a restarted command
could distinguish new submission, interrupted admission, active execution, and an
already-terminal queue. The smallest honest entry point therefore recovers one
caller-prepared queue and runs exactly one Worker cycle without creating or admitting
work.

## Justified By

- 2026-07-21: Select The Process-Isolated Durable Worker And Retire Spools After Checkpoint
- 2026-07-22: Connect Work Admission To The Durable Scheduler Queue Through A Persist-First Handler
- 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

## Acceptance Criteria

- `scheduler-cycle` requires explicit project, queue, runtime, external-effect,
  cycle-checkpoint, evidence, RunRecord, and invocation roots; canonical queue identity;
  bounded owner identity, attempt count, lease duration, and process timeout. It infers
  no repository or user-profile storage path.
- The command recovers an existing `DurableSingleWorkerSchedulerQueue` and constructs
  `DurableAgentRunWorker.processIsolated` with one shared queue instance, real filesystem
  stores, system UTC clock, explicit retry policy, and bounded durations. It never
  creates a queue or admits a WorkItem.
- One invocation runs exactly one recoverable Worker cycle. Empty work reports `IDLE`
  with exit 0; Verified completion reports `VERIFIED_COMPLETED` with exit 0; terminal
  failed disposition reports `FAILED` through one new stable Scheduler-specific non-zero
  exit code. Output is bounded and reports no evidence content or secret material.
- Missing queue/configuration and malformed numeric/duration input return bounded
  usage/configuration failure. Corruption, execution, or unexpected storage failure is
  not misreported as success.
- A named CLI integration prepares work through the production durable admission path,
  runs the real child-process Worker through the command, resolves the persisted
  RunRecord, observes the terminal queue disposition and cleared cycle checkpoint, and
  proves restart-safe recovery from an already persisted Worker prefix.
- Focused argument/CLI/integration tests, the full Gradle build, strict Java lint,
  structural-document checks, and diff checks pass with fresh evidence.

## Out Of Scope

- Queue creation, work submission, durable Message Bus journal, exact cross-bus
  admission history, worker polling/service loops, concurrent commands, background
  execution, authenticated control application, or arbitrary dependency submission.
- External adapter invocation/evidence, compensation, cross-attempt effect identity,
  queue/runtime schema migration, priority/fairness, distributed coordination, Gate 9,
  or whole-Gate Operational promotion.
- Commit, push, PR, merge, release, or deployment unless separately requested.

## Approval

The user explicitly asked to continue the project on 2026-07-22. The completed durable
admission task named the minimal supported Scheduler entry-point assessment as next; the
assessment selected this recovery-only one-cycle command.

## Verification

Focused GREEN passed the argument and CLI integration suites, including a real child
JVM. After document synchronization, a fresh full `build --rerun-tasks` passed all 7
build tasks and 84 suites/451 tests: 448 passed, 3 existing privilege-dependent Windows
skips, 0 failures, and 0 errors under build-enforced Java 17 strict lint. A separate
fresh structural/self-hosting rerun passed 20 tests across 5 suites: 19 passed, 1 existing
Windows privilege skip, 0 failures, and 0 errors. `git diff --check` produced no output.

## Next

After this task, assess the remaining Gate 8 Operational blockers, including exact
cross-bus admission recovery and an end-user submission manifest, without starting
authenticated controls, external-effect adapters, or Gate 9.
