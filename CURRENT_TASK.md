# Current Task

## Status

In Progress

## Task

Implement RFC-0027 manifest-authorized typed `ModelWork` spool ingress through six
sequential RED-first increments while preserving RFC-0026 direct submission, legacy
Work commands, current durable formats, and bounded at-least-once recovery.

## Task ID

implement-rfc-0027-manifest-authorized-model-work-spool-ingress

## Context

RFC-0027 is accepted and the completed specification task names its RED-first
implementation as the next work. The current RFC-0024 service combines exact manifest
preparation with direct queue admission, the existing spool transport already encodes
typed ModelWork v2 but creates a fresh bounded point per accepted send, and the legacy
receiver accepts caller capability and priority only for WorkPayload. No supported
manifest-only typed publisher or manifest-authorized typed receiver exists.

The user requested continuation on 2026-09-10. The repository is clean on local
`main`, which is two RFC-0027 specification commits ahead of `origin/main`, and the
development checkpoint is empty.

## Justified By

- User continuation request on 2026-09-10 into manifest-authorized typed ModelWork spool ingress specification
- User continuation request on 2026-09-09 into RFC-0026 supported typed ModelWork submission implementation
- User continuation request on 2026-08-21 into the Scheduler complete-profile transport specification

## Approval

The user's 2026-09-10 continuation authorizes the minimum RFC-0027 implementation:
extract the shared package-local deterministic-fake manifest preparation boundary;
add the exact typed publisher parser/value and filesystem composition; add a separate
manifest-authorized typed receiver and its four-option CLI; harden bounded no-follow
transport point reading, acknowledgement, replay, crash-prefix, and duplicate-point
behavior; connect a real publisher -> receiver -> separately invoked RFC-0025 operator
path; update applicable architecture, state, Roadmap, README, task, accepted
implementation decision/index, verification, handoff, and Changelog documents; run
focused and full README-owned Java 17 verification; and create ordinary local commits
at each verified GREEN increment.

This approval does not authorize widening `scheduler-spool-work`,
`scheduler-receive-work`, or RFC-0026 direct submission behavior; capability,
destination, queue, derived identity, time, snapshot, Tool scope, policy, candidate, or
provider input outside RFC-0027; profile defaults or inference; a new durable schema,
receipt, outbox, scan, dead-letter, cleanup, retention, or cross-store transaction;
background consumption; provider/router/registry, endpoint, remote transport, network,
credentials, pricing or spend; MCP; push, merge, release, deployment, permission
change, destructive cleanup, or other external effect.

## Acceptance Criteria

- RFC-0024 manifest construction, first-use ordering, exact replay, fixed
  `deterministic-echo` source, and caller-intent validation live in one shared
  package-local preparation boundary used by both direct submission and typed
  publication. RFC-0026 direct submission retains exact behavior and durable bytes.
- `scheduler-spool-deterministic-fake-model-work` implements exactly the RFC-0027
  twelve-option all-required surface, validates profile and intrinsic request before
  durable access, persists or exact-replays the manifest before transport publication,
  publishes only the manifest-derived queue route and exact envelope, never touches the
  queue, and reports bounded non-secret accepted/backpressured/unavailable outcomes.
- Exact publisher replay does not recapture context or time. An uncertain accepted
  retry may create another exact bounded random point, while refusal creates no partial
  point and manifest-only recovery remains explicit.
- A separate deterministic-fake typed receiver accepts exactly spool root, canonical
  message filename, submission root, and queue root. It accepts no capability, priority,
  capacity, queue, destination, profile, task, producer, clock, policy, candidate, or
  provider authority and never widens the legacy Work receiver.
- The receiver point-resolves exactly one pending or acknowledged same-root regular
  point, uses bounded no-follow reading with link/reparse and overflow refusal, requires
  canonical ModelWork v2, resolves manifest v3 by message identity, and validates exact
  envelope, derived route, closed capability, and queue capacity before admission.
- The receiver creates or recovers only the manifest queue, delivers through one fresh
  real Message Bus subscriber using only manifest capability and priority, requires
  exact durable admission or no-revision replay, then atomically acknowledges. Every
  documented crash prefix, collision, corrupt/foreign/conflicting state, ACK failure,
  acknowledged replay, capacity release, and duplicate point converges without
  unauthorized mutation or duplicate queue work.
- A real filesystem/JVM operator path proves typed publisher -> receiver -> separately
  invoked RFC-0025 cycle, drain, and service completion plus retained capability
  mismatch refusal and legacy compatibility.
- Existing message/spool v1/v2, manifest v3, queue v4, runtime v5, checkpoint v2, Model
  RunRecord v2, Result, and runtime-event formats remain unchanged. No exactly-once
  publication, scan, remote trust, provider/network/credential/spend, background
  execution, or implicit Scheduler execution claim is introduced.
- Each observable increment establishes and classifies aligned RED evidence before the
  minimum GREEN implementation, appends fresh evidence once, synchronizes document
  owners, passes focused verification and `git diff --check`, and is committed locally.
  Full Markdown-sensitive Java 17 regression passes before task closure.

## Out Of Scope

Legacy Work command or payload changes; RFC-0026 behavior changes beyond internal
behavior-preserving delegation; caller capability/destination/queue/derived identity/
time/snapshot/policy/provider authority; profile defaults or inference; deterministic
single-point or exactly-once publication; new durable schema, migration, receipt,
outbox, sidecar, consumer offset, scan, dead-letter, cleanup, retention, or cross-store
transaction; background polling/service; provider/router/registry, endpoint, remote
transport, network, credentials, pricing or spend; MCP; new runtime event, cancellation
propagation, or RunRecord provenance; push, merge, release, deployment, permission
change, destructive cleanup, and external effects.

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

Observable behavior is test-first. Each RED is classified against RFC-0027, the
accepted decisions, Architecture, current source behavior, and this task before the
minimum implementation. Evidence is appended once per completed increment to
`docs/verification-log.md`. Focused suites cover direct-submission compatibility,
manifest preparation, publisher/parser, transport, receiver/admission, CLI recovery,
operator-path integration, durable formats, source/locality boundaries, and governance.
Subagent recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: implement-rfc-0027-manifest-authorized-model-work-spool-ingress
Mode: Sequential
Increment Limit: 6
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on a RED that conflicts with RFC-0027 or current formats,
manifest/capability/profile conflation, legacy behavior drift, unsafe point or queue
recovery, new durable schema need, provider/network authority, failed verification,
checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - extract-shared-manifest-preparation

State: Completed
Depends On: none
Scope: Add the accepted implementation decision, establish RED tests for one shared
package-local manifest preparation boundary, and refactor RFC-0024 direct submission to
use it without changing first-use, replay, recovery, output, or durable bytes.
Exit Criteria: Direct submission delegates through the shared preparation boundary;
first use and exact replay return the exact manifest plus created/replayed status;
RFC-0024/RFC-0026 focused compatibility is GREEN and no publisher or receiver exists.
Verification: Manifest-preparation, deterministic submission service/facade/CLI,
durable submission recovery, source/locality, architecture/governance tests, and
`git diff --check`.
Next Action: Commit Increment 1 and select Increment 2.

### Increment 2 - add-typed-publisher-cli-contract

State: Completed
Depends On: extract-shared-manifest-preparation
Scope: Establish RED parser/value/source tests and add exactly the twelve all-required
typed publisher options with canonical capacity, identity, path, profile locator, and
explicit priority validation but no execution dispatch.
Exit Criteria: Parser/value behavior exactly matches RFC-0027, forbidden and legacy
options are isolated, and no publication or store access is connected.
Verification: CLI argument/value, profile reader, legacy CLI compatibility,
source/locality, architecture/governance tests, and `git diff --check`.
Next Action: Commit Increment 2 and select Increment 3.

### Increment 3 - connect-manifest-only-typed-publisher

State: Completed
Depends On: add-typed-publisher-cli-contract
Scope: Add the minimum public filesystem publisher composition and CLI execution using
shared manifest preparation followed by the unchanged file spool transport.
Exit Criteria: Manifest-before-point ordering, queue non-access, first publication,
manifest-only recovery, exact-manifest replay, duplicate accepted points,
backpressure/unavailable refusal, bounded output/redaction, and direct/legacy
compatibility are GREEN.
Verification: Publisher facade/CLI real-filesystem tests, manifest/direct submission,
transport, source/locality, architecture/governance tests, and `git diff --check`.
Next Action: Commit Increment 3 and select Increment 4.

### Increment 4 - add-manifest-authorized-typed-receiver

State: In Progress
Depends On: connect-manifest-only-typed-publisher
Scope: Establish RED domain/composition tests and implement the separate typed receiver
that validates exact manifest, envelope, route, fixed capability, capacity, and
manifest-derived admission through the real Message Bus.
Exit Criteria: Exact first admission and no-revision replay are GREEN; missing,
corrupt, unsupported, wrong-kind, foreign, changed, capability-conflicting, and
capacity-conflicting input fails before unauthorized queue mutation; legacy receiver
remains Work-only.
Verification: Typed receiver, durable queue/admission/submission, transport codec,
legacy receiver, source/locality, architecture/governance tests, and
`git diff --check`.
Next Action: Commit Increment 4 and select Increment 5.

### Increment 5 - connect-and-harden-typed-receiver-cli

State: Pending
Depends On: add-manifest-authorized-typed-receiver
Scope: Add the exact four-option CLI and bounded no-follow point/acknowledgement
composition, then prove all RFC-0027 receive crash prefixes and refusal boundaries.
Exit Criteria: Pending receive, queue-only continuation, admitted-pending replay,
acknowledged lost-response replay, capacity release, duplicate-point convergence,
link/reparse/overflow/collision/corruption refusal, ACK failure recovery, bounded
output/redaction, and no-ACK-on-failure are GREEN.
Verification: Typed receive CLI/filesystem integration, bounded file operations,
publisher/transport, legacy receive, source/locality, architecture/governance tests,
and `git diff --check`.
Next Action: Commit Increment 5 and select Increment 6.

### Increment 6 - prove-operator-path-and-close

State: Pending
Depends On: connect-and-harden-typed-receiver-cli
Scope: Add real filesystem/JVM publisher -> receiver -> separate RFC-0025 cycle/drain/
service evidence, capability-mismatch and legacy compatibility coverage, synchronize
owned documents and README recovery guidance, run focused plus full regression, and
close the task.
Exit Criteria: All three supported Scheduler commands reach the expected verified
typed outcome after separate publication/receive; refusal/recovery and legacy
compatibility pass; documentation is current; full Java 17 regression is GREEN; Git is
clean; and the checkpoint is stable and clear.
Verification: RFC-0018-through-RFC-0027, durable-format, publisher/receiver/Scheduler
CLI, real-JVM operator-path, source/locality, architecture/governance, full
`.\scripts\gradle.ps1 test`, diff/commit/status, and checkpoint reconciliation.
Next Action: Await separate authority for push, merge, release, deployment, provider,
network, background service, or later product work.

## Next

Complete Increment 4 RED-first, commit its verified GREEN boundary, and select
Increment 5.
