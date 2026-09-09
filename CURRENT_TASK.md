# Current Task

## Status

In Progress

## Task

Implement RFC-0026 RED-first as the smallest supported deterministic-fake ModelWork
submission input while preserving capability independence, legacy commands, durable
formats, and separate Scheduler execution.

## Task ID

implement-supported-model-work-submission-input

## Context

RFC-0026 is accepted and defines the strict project-contained complete-profile file,
the separate `scheduler-submit-deterministic-fake-model-work` command, and a narrow
public filesystem facade over the package-local RFC-0024 producer. RFC-0025 already
provides the supported model-aware Scheduler consumer, but supported typed intent is
still absent. The completed specification task named RED-first implementation as
separately authorized work, and the user requested continuation on 2026-09-09.

## Justified By

- User continuation request on 2026-09-09 into RFC-0026 supported typed ModelWork submission implementation
- User continuation request on 2026-09-09 into supported typed ModelWork submission specification
- User continuation request on 2026-09-08 into RFC-0025 supported model-aware Scheduler composition implementation

## Approval

The user's 2026-09-09 continuation authorizes the minimum RED-first Java/test
implementation of RFC-0026: one strict bounded complete-profile file reader, the exact
separate CLI command and immutable command value, a narrow public filesystem facade
over RFC-0024, first-use/replay/recovery integration, supported submission-to-RFC-0025
operator-path evidence, and source/locality/legacy hardening. It authorizes
Architecture, compact mirror, Project State, Roadmap, task, decision/index,
verification, handoff, README usage/recovery, and Changelog synchronization and
ordinary local GREEN commits.

It authorizes no spool publisher or receiver, legacy submission/receiver widening,
caller capability input or inference, profile defaults or partial input,
provider/router/registry, endpoint, remote transmission, network, credentials, pricing
or spend, MCP, durable schema version or migration, runtime-event change, implicit
execution, background service, push, merge, release, deployment, permission change,
destructive cleanup, or external effect.

## Acceptance Criteria

- A production profile reader accepts only one explicit project-relative, real-root-
  contained, no-link regular file and enforces the RFC-0026 4,096-byte consumption
  ceiling, strict UTF-8, exactly thirteen ordered LF-terminated entries, canonical
  decimal syntax, and every existing RFC-0014 value/relationship bound.
- The reader rejects absolute/drive-relative/traversal/escaping, missing, directory,
  symbolic or otherwise unsafe paths; malformed UTF-8, BOM, CR, NUL, oversize/growth;
  missing, partial, duplicate, reordered, unknown, extra, whitespace, comment, escape,
  noncanonical numeric, overflow, defaulted, inferred, registry, environment, and
  fallback input before manifest or queue access.
- `scheduler-submit-deterministic-fake-model-work` requires exactly the RFC-0026
  project/submission/queue/task/submission/producer/target/expected-response/profile/
  capacity/priority inputs. It accepts no capability, derived identity, occurrence
  time, Tool scope, snapshot, model execution, provider, endpoint, credential, price,
  network, or policy value and does not alter legacy command parsing.
- One narrow public deterministic-fake ModelWork submission facade composes the real
  filesystem stores, repository readers, snapshot collector, and system UTC clock over
  the existing package-local RFC-0024 request/service. Injectable sources and the fixed
  capability source remain package-local.
- The CLI reads and validates the complete profile once before store access, delegates
  once, retains the profile unchanged, and never compares or repairs its capability.
  The independently fixed `deterministic-echo` WorkItem capability remains the only
  producer capability source, and mismatch stays available to RFC-0016.
- First use, manifest-only and empty-queue recovery, exact replay, context/clock non-
  recapture, stable derived identities, and caller-intent conflict behavior remain
  RFC-0024 exact. Profile path/raw bytes are not persisted or compared; typed profile
  equality is the replay contract.
- Successful output is bounded to the RFC-0026 identifier/status/revision/priority/
  creation/admission/snapshot fields. Errors are bounded and value-redacted. No profile
  path/field, prompt/response, capability, Tool scope, evidence, child argument,
  credential, or execution/completion claim is printed.
- Real filesystem/JVM integration proves the new supported submit command followed by
  separate model-configured cycle, drain, and service completion, one submitted
  profile/capability mismatch reaching no-effect pre-call refusal, and exact recovery
  without duplicate admission, invocation, evidence, RunRecord, or disposition.
- Existing `scheduler-submit`, `scheduler-submit-generated`, `scheduler-spool-work`,
  `scheduler-receive-work`, direct `model-invoke`, legacy payloads, and message/spool
  v2, manifest v3, queue v4, runtime v5, pending-finalization v2, Model RunRecord v2,
  Result, timeout, and runtime-event formats remain behavior- and byte-compatible.
- RFC/decision indexes, Architecture and compact mirror, Project State, Roadmap, task
  cursor, README, Changelog, and append-only verification evidence are synchronized by
  document owner. Focused and full README-owned Java 17 regression plus
  `git diff --check` pass freshly before completion.

## Out Of Scope

Typed spool publisher, receiver, Message Bus or runtime-event ingress; widening legacy
commands; caller capability input/inference or capability precheck; profile defaults,
partial input, discovery, registry, environment, fallback, provider configuration, raw
file persistence, or path-based replay identity; candidate/provider/router, endpoint,
remote transmission, network, credentials, pricing or spend; MCP; new durable schema,
migration, sidecar, RunRecord provenance, event kind, cancellation propagation, or
terminal pre-call refusal; combined submission/execution, background service,
supervisor, concurrency, or multi-agent runtime; push, merge, release, deployment,
permissions, destructive cleanup, and external effects.

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
`docs/verification-log.md`. Each RED is classified against RFC-0014 through RFC-0026,
existing producer/CLI/durable behavior, and legacy compatibility before the minimum
production change. Subagent recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: implement-supported-model-work-submission-input
Mode: Sequential
Increment Limit: 6
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on profile grammar or containment ambiguity, capability/profile/
policy conflation, legacy command widening, implicit execution or transport ingress,
unclassified RED, durable schema widening, provider/network authority, failed
verification, checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - add-strict-model-execution-profile-reader

State: Completed
Depends On: none
Scope: Record implementation authority, add RED tests for the exact profile grammar,
value bounds, bounded read, strict UTF-8, containment and no-link behavior, then add the
minimum production reader.
Exit Criteria: Exact valid profiles project to the existing immutable values; every
RFC-0026 malformed or unsafe input fails before any submission store access.
Verification: New reader tests, RFC-0014 value tests, bounded file/path safety tests,
source guards, focused governance, and `git diff --check`.
Next Action: Commit the GREEN reader increment and select Increment 2.

### Increment 2 - add-typed-model-submission-cli-contract

State: Completed
Depends On: add-strict-model-execution-profile-reader
Scope: Add RED-first immutable command value and exact parser dispatch/options without
connecting submission behavior.
Exit Criteria: The exact required command options parse, forbidden/missing/duplicate/
unknown values fail, priority is explicit, capability is absent, and legacy commands
remain unchanged.
Verification: New CLI argument/value tests, existing CLI argument suites, architecture
source guards, focused governance, and `git diff --check`.
Next Action: Commit the GREEN CLI contract increment and select Increment 3.

### Increment 3 - expose-and-connect-filesystem-submission-facade

State: Completed
Depends On: add-typed-model-submission-cli-contract
Scope: Add the narrow public filesystem facade over RFC-0024 and connect the new CLI
execution/output path without changing existing commands.
Exit Criteria: The real closed sources are selected, profile is read once before store
access, fixed capability remains private, submission delegates once, and output/error
disclosure is exact and bounded.
Verification: Facade tests, CLI pre-store and output tests, RFC-0024 request/service
tests, locality/package guards, legacy CLI regression, and `git diff --check`.
Next Action: Commit the GREEN production connection and select Increment 4.

### Increment 4 - prove-supported-durable-submission-and-replay

State: Completed
Depends On: expose-and-connect-filesystem-submission-facade
Scope: Exercise first admission, manifest/queue interruption recovery, exact fresh-
instance replay, caller-intent conflicts, typed profile equality, and redacted errors
through the supported command on real filesystems.
Exit Criteria: Durable prefixes converge once, exact replay changes no manifest/queue
bytes or revision, path-only change with equal profile replays, semantic drift fails
without mutation, and output remains bounded.
Verification: New supported CLI integration plus submission manifest/queue/service,
profile reader, source guard, legacy compatibility, and `git diff --check` regression.
Next Action: Commit the GREEN durable integration and select Increment 5.

### Increment 5 - prove-supported-model-work-operator-path

State: Completed
Depends On: prove-supported-durable-submission-and-replay
Scope: Invoke the real new submission command followed separately by supported RFC-0025
cycle, drain, and service execution, including mismatch refusal and exact recovery.
Exit Criteria: All three paths reach verified Model RunRecord v2 completion without
duplicate effects; mismatch remains a no-effect recoverable pre-call refusal; no
wrapper, publisher, receiver, schema, or legacy drift is introduced.
Verification: Real JVM/filesystem operator integrations, RFC-0016-through-RFC-0026,
typed durable formats/recovery, Scheduler model/event, legacy CLI, source guards, and
`git diff --check` regression.
Next Action: Commit the GREEN operator-path integration and select Increment 6.

### Increment 6 - verify-and-close-rfc-0026-implementation

State: In Progress
Depends On: prove-supported-model-work-operator-path
Scope: Run the full Markdown-sensitive Java 17 regression, synchronize capability state
and owned documents, close the task/handoff, and commit closure.
Exit Criteria: Full regression passes, documents and commits are current, Git is clean,
and the stable checkpoint is cleared.
Verification: Full `.\scripts\gradle.ps1 test`, final focused RFC-0026/governance
suites, diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority for the next Gate 9 or typed spool boundary.

## Next

Complete Increment 6 with the full Markdown-sensitive regression, final owned-document
synchronization, local closure commit, and stable checkpoint reconciliation.
