# Current Task

## Status

Completed

## Task

Assess whether a separately durable Scheduler invocation manifest is required to offer
replay-safe generated identities and occurrence time without coupling submission,
execution, or polling, and record the smallest accepted next boundary.

## Task ID

assess-durable-scheduler-invocation-manifest

## Context

The supported two-command operator workflow is recovery-safe only when the operator
retains every explicit `scheduler-submit` identity and its occurrence time. Generating
those values inside the current command would create a pre-submission crash window: a
restart could generate different values before the immutable submission manifest exists.

A second durable manifest could close that window, but it could also duplicate authority
already owned by `DurableSubmissionManifest`, introduce an unresolved invocation identity,
or blur the deliberate separation between submission and `scheduler-cycle`. The repository
must decide the owner, exact persisted prefix, replay key, and consumer before adding a
contract or command.

## Justified By

- 2026-07-22: Expose Durable Submission As A Separate Explicit CLI Command
- 2026-07-22: Persist Submission Intent Before Creating The Scheduler Queue

## Acceptance Criteria

- The assessment identifies the exact interruption window that generated identities/time
  would create before durable submission intent exists and distinguishes it from the
  already-covered manifest -> queue -> admission recovery prefixes.
- At least the following alternatives are compared against repository authority,
  idempotency, ownership, and recovery: keep every input explicit; add a separate durable
  invocation manifest; or widen/repurpose the existing submission manifest.
- The selected outcome names one stable replay key, the facts it may own, its immediate
  consumer, and the boundaries it must not cross, or explicitly defers the capability with
  a concrete reason and re-entry condition.
- Any material accepted direction is recorded as a matching decision file and
  `DECISION_LOG.md` entry; architecture, roadmap, state, and next-task documents are changed
  only where their owning facts actually change.
- Structural document checks and the relevant fresh Gradle verification pass, and the
  increment's evidence is appended once to `docs/verification-log.md`.

## Out Of Scope

- Implementing a new manifest/store/CLI before the assessment accepts its identity,
  ownership, persistence order, and recovery contract.
- Combining submission with `scheduler-cycle`, automatic execution, polling, queue
  watching, background services, concurrent writers, or multi-process locking.
- Authenticated controls, external adapter effects, Gate 9, schema migration/cleanup,
  commit, push, PR, merge, release, or deployment.

## Approval

The user explicitly asked to continue the project on 2026-07-22, and the preceding
completed task named this durable invocation-manifest assessment as the next bounded
increment.

## Allowed Tools

- read-file

## Verification

Acceptance is satisfied by a source- and decision-backed assessment, matching decision
index/file structure when a direction is accepted, repository document-ownership checks,
and a fresh strict-lint Gradle build. Append-only results and limitations are recorded in
`docs/verification-log.md`.

Fresh verification passed 15 focused structural/context tests (14 passed and one existing
Windows symbolic-link setup case skipped), `git diff --check`, the stale-claim search, and
the full strict-lint Gradle build across 89 suites/465 tests (462 passed, three existing
Windows symbolic-link setup cases skipped, zero failures or errors).

## Next

Implement the bounded generated-input submission application boundary test-first: use one
caller-retained canonical submission UUID as the replay/message key, derive the remaining
identities through versioned domain separation, resolve an existing submission manifest
before consulting the clock or repository context, and feed the exact manifest into the
existing submission service without adding a second store or invoking `scheduler-cycle`.
