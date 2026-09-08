# Current Task

## Status

In Progress

## Task

Implement RFC-0025 RED-first as the smallest supported deterministic-fake model-aware
Scheduler composition while preserving legacy execution and the absence of supported
typed ModelWork ingress.

## Task ID

implement-supported-model-aware-scheduler-composition

## Context

RFC-0025 is accepted and specifies an optional all-or-none deterministic-fake execution
group shared by `scheduler-cycle`, `scheduler-drain`, and `scheduler-service`. The
existing RFC-0023 typed worker and RFC-0024 producer are Integrated only through a
test-owned path; supported Scheduler commands still construct the legacy-only worker.
The completed specification task names RED-first implementation as the next separately
authorized work, and the user requested continuation on 2026-09-08.

## Justified By

- User continuation request on 2026-09-08 into RFC-0025 supported model-aware Scheduler composition implementation
- User continuation request on 2026-09-07 into supported model-aware Scheduler composition specification
- User continuation request on 2026-09-07 into RFC-0024 governed deterministic ModelWork submission implementation

## Approval

The user's 2026-09-08 continuation authorizes the minimum RED-first Java/test
implementation of RFC-0025: the exact optional CLI group, one bounded public
deterministic-fake Scheduler configuration value, the combined model-context/runtime-
event worker composition, shared cycle/drain/service selection, and real supported CLI
integration from test-seeded RFC-0024 durable intent. It authorizes architecture,
compact mirror, Project State, Roadmap, task, decision/index, verification, handoff, and
Changelog synchronization and ordinary local GREEN commits.

It authorizes no typed submission or spool publication/receive, complete-profile
interface format, legacy submission/receiver widening, provider/router/registry,
endpoint, remote model transmission, network, credentials, pricing or spend, MCP,
durable schema version or migration, new runtime-event kind, cancellation propagation,
background service, push, merge, release, deployment, permission change, destructive
cleanup, or external effect.

## Acceptance Criteria

- `scheduler-cycle`, `scheduler-drain`, and `scheduler-service` accept the exact
  RFC-0025 optional all-or-none deterministic-fake model execution group; omission
  preserves the current legacy-only composition, while partial, unknown, duplicate, or
  invalid input fails during CLI validation before queue/store access.
- The group names exactly one closed execution selector plus bounded gateway timeout,
  maximum response characters, maximum prompt-read bytes, Tool timeout, and a bounded
  repeatable denied-Tool set. It accepts no capability, profile, task, target, digest,
  candidate, endpoint, provider, credential, price, or network value.
- One bounded public deterministic-fake Scheduler configuration value exposes only the
  accepted scalar/set inputs and converts internally to the package-private process
  configuration without exposing candidate, gateway, policy, cancellation, or store
  authority.
- The model-aware composition reuses the command's exact project, queue, runtime,
  external-effect, cycle-checkpoint, evidence, RunRecord, invocation, owner, retry,
  lease, process-timeout, clock, and optional runtime-event sources. The same
  `FileSystemRunRecordStore` and evidence root serve the existing v1/v2 resolver and
  model validation; no sidecar or second record format is introduced.
- Enabling the group selects the existing payload-kind-aware worker and permits legacy
  Work and typed ModelWork to retain their existing distinct paths. It does not create,
  publish, receive, discover, or rewrite queued work and does not infer model authority
  from legacy payloads.
- Typed execution preserves RFC-0016 through RFC-0024 ordering and identity, performs at
  most one deterministic-fake call per AgentRun, publishes only verified Model
  RunRecord v2, retains crash/retry/finalization recovery, and exposes pre-call refusal
  only through the existing error/recovery prefix without inventing a terminal durable
  refusal.
- The existing optional runtime-event publication group remains composable with the
  model-aware worker and retains its exact event authority. RFC-0025 adds no model event
  kind and does not silently disable an already selected recorder.
- Supported command output remains unchanged for both modes; prompt, response, profile,
  denied-Tool content, evidence content, and credentials are never printed.
- Existing message/spool v2, manifest v3, queue v4, runtime v5, pending-finalization v2,
  RunRecord v1/Model RunRecord v2, runtime-event, and legacy command bytes remain
  sufficient and unchanged.
- Real filesystem/JVM CLI integration proves verified typed completion through cycle,
  drain, and service, a denied-`model-invoke` pre-call refusal, model-plus-runtime-event
  compatibility, legacy behavior, exact recovery, and source locality from test-seeded
  intent only.
- RFC/decision indexes, Architecture and compact mirror, Project State, Roadmap, task
  cursor, Changelog, and append-only verification evidence are synchronized according
  to document ownership. Focused Markdown-sensitive governance, `git diff --check`, and
  the full README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Typed ModelWork submission, publisher, receiver, Message Bus or runtime-event ingress;
interface-owned profile file/schema/parser or profile CLI fields; legacy submission or
receiver widening; provider selection, router, registry, endpoint, remote transmission,
network, credentials, pricing or spend; MCP; new durable schema, migration, sidecar,
record provenance, event kind, cancellation propagation, or terminal pre-call refusal;
push, merge, release, deployment, permissions, destructive cleanup, and external
effects.

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

Observable behavior is RED-first. Evidence is appended once per completed increment to
`docs/verification-log.md`. Each RED is classified against RFC-0016 through RFC-0025,
existing supported Scheduler behavior, current durable formats, and v1 compatibility
before production changes. Subagent recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: implement-supported-model-aware-scheduler-composition
Mode: Sequential
Increment Limit: 6
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on configuration-source ambiguity, capability/profile/policy
conflation, implicit typed ingress, legacy behavior or output drift, runtime-event loss,
unclassified RED, durable schema widening, provider/network authority, failed
verification, checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - add-model-scheduler-cli-configuration

State: Completed
Depends On: none
Scope: Add RED-first immutable CLI/configuration values and parsing for the exact
all-or-none deterministic-fake execution group without connecting a worker.
Exit Criteria: Selector, four numeric values, at most 16 unique 128-character denied
Tools, strict timeout nesting, optional omission, and pre-store validation are GREEN.
Verification: New configuration/parser tests, existing Scheduler CLI argument tests,
architecture/source guards, and `git diff --check`.
Next Action: Commit the GREEN configuration increment and select Increment 2.

### Increment 2 - expose-bounded-model-aware-worker-composition

State: Completed
Depends On: add-model-scheduler-cli-configuration
Scope: Add the bounded public runtime configuration/factory and combined model-context/
runtime-event worker construction without changing supported command selection.
Exit Criteria: Exact source projection, private authority preservation, model-plus-event
composition, and unchanged legacy overload behavior are GREEN.
Verification: New runtime composition tests plus RFC-0023 worker/configuration, event,
source-boundary, and `git diff --check` regression.
Next Action: Commit the GREEN runtime composition increment and select Increment 3.

### Increment 3 - connect-supported-scheduler-command-selection

State: In Progress
Depends On: expose-bounded-model-aware-worker-composition
Scope: Connect the optional configuration to the shared cycle/drain/service production
composition while preserving legacy selection and exact output.
Exit Criteria: All three commands select one correct branch, reuse exact roots/stores/
recorder, reject invalid configuration before recovery, and retain legacy output.
Verification: CLI composition tests, legacy cycle/drain/service suites, parser and event
regression, source guards, and `git diff --check`.
Next Action: Commit the GREEN supported selection increment and select Increment 4.

### Increment 4 - prove-supported-typed-completion

State: Pending
Depends On: connect-supported-scheduler-command-selection
Scope: Add test-owned RFC-0024 durable intent and invoke the real supported cycle,
drain, and service CLI paths through typed verified completion.
Exit Criteria: Each command reaches exact Model RunRecord v2 verified completion and
queue disposition with no production typed ingress or output disclosure.
Verification: Real filesystem/JVM CLI integrations, Model RunRecord binding, durable
format/recovery tests, legacy compatibility, and `git diff --check`.
Next Action: Commit the GREEN completion integration and select Increment 5.

### Increment 5 - prove-refusal-recovery-and-runtime-events

State: Pending
Depends On: prove-supported-typed-completion
Scope: Prove denied-Tool pre-call refusal, exact typed recovery, model-plus-runtime-event
composition, and locality/interface exclusions.
Exit Criteria: Refusal makes no fake call/evidence/record/result/disposition, recovery
does not reinvoke, existing events remain exact, and typed ingress stays unreachable.
Verification: Refusal/recovery/event real-filesystem tests, RFC-0016-through-RFC-0025
focused suites, architecture/source guards, and `git diff --check`.
Next Action: Commit the GREEN hardening integration and select Increment 6.

### Increment 6 - verify-and-close-rfc-0025-implementation

State: Pending
Depends On: prove-refusal-recovery-and-runtime-events
Scope: Run the full Markdown-sensitive Java 17 regression, synchronize capability state
and canonical documents, close the task/handoff, and commit closure.
Exit Criteria: Full regression passes, documents and commits are current, Git is clean,
and the stable checkpoint is cleared.
Verification: Full `.\scripts\gradle.ps1 test`, final focused RFC-0025/governance
suites, diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority for an interface-owned complete-profile input and
supported typed submission or publisher contract.

## Next

Implement Increment 3 RED-first: connect the optional configuration to the shared
cycle, drain, and service production composition without changing output.
