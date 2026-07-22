# Current Task

## Status

Completed

## Task

Implement the bounded generated-input submission application boundary test-first: use one
caller-retained canonical submission UUID as the replay/message key, derive the remaining
identities through versioned domain separation, resolve an existing submission manifest
before consulting the clock or repository context, and feed the exact manifest into the
existing submission service without adding a second store or invoking `scheduler-cycle`.

## Task ID

implement-generated-input-submission-boundary

## Context

The explicit `scheduler-submit` workflow is replay-safe only when the operator retains
every supplied identity and occurrence time. The accepted decision "Reuse The Immutable
Submission Manifest As The Sole Generated-Input Recovery Record" resolved to close that
window by deriving the generated identities from one caller-retained submission UUID and
reusing the existing `DurableSubmissionManifest` as the sole recovery record, resolved
before the clock or repository context is consulted.

## Justified By

- 2026-07-22: Reuse The Immutable Submission Manifest As The Sole Generated-Input Recovery Record
- 2026-07-22: Expose Durable Submission As A Separate Explicit CLI Command
- 2026-07-22: Persist Submission Intent Before Creating The Scheduler Queue

## Acceptance Criteria

- One canonical caller-retained submission UUID is the sole replay/message key, and the
  queue, correlation, and logical-run identities are derived from it through fixed,
  versioned, domain-separated one-to-one transforms so the same key always names the same
  generated work across fresh stores.
- The application boundary resolves the existing submission manifest before consulting the
  clock or recapturing repository context; an absent manifest captures the occurrence time
  and persists through the existing `DurableWorkSubmissionService`, while a present
  manifest reuses its exact occurrence time and envelope and fails closed on any
  caller-owned intent conflict.
- No second durable store is added, and submission never invokes `scheduler-cycle`,
  execution, or polling; the explicit `scheduler-submit` command remains unchanged.
- The boundary is exposed as a separate CLI command with focused boundary, argument, and a
  named real-filesystem CLI integration proving first-use generation, fresh-instance exact
  replay without manifest or queue-revision change, conflict fail-closed, and first-use
  task-mismatch refusal.
- The relevant fresh strict-lint Gradle verification passes and the increment's evidence is
  appended once to `docs/verification-log.md`.

## Out Of Scope

- Combining submission with `scheduler-cycle`, automatic execution, polling, queue
  watching, background services, concurrent writers, or multi-process locking.
- Admission-history compaction/cleanup, schema-v1 queue migration, authorized external
  adapter effects, authenticated control application, Gate 9, commit, push, PR, merge,
  release, or deployment.

## Approval

The user explicitly asked to continue the project on 2026-07-22 and approved implementing
the full increment (application boundary, CLI, and tests). The preceding completed
assessment named this generated-input submission boundary as the next bounded increment.

## Allowed Tools

- read-file

## Verification

Acceptance is satisfied by test-first focused boundary tests, CLI argument tests, a named
real-filesystem CLI integration, repository document-ownership checks, and a fresh
strict-lint Gradle build. Append-only results and limitations are recorded in
`docs/verification-log.md`.

Fresh verification passed the full strict-lint Gradle build across 91 suites / 474 tests
(471 passed, three existing Windows symbolic-link setup cases skipped, zero failures or
errors), including the new `GeneratedInputSubmissionServiceTest`, the extended
`CliArgumentsTest`, and `EnhancerCliSchedulerGeneratedSubmitIntegrationTest`, plus
`git diff --check`.

## Next

Prove the generated-input submission as an Operational sub-path: add an actual
Enhancer-repository smoke run of `scheduler-submit-generated` followed by
`scheduler-cycle` over one shared explicit queue root, and document the generated-input
recovery actions (submission interruption before and after manifest persistence, exact
replay, and idle re-entry) alongside the existing explicit two-command workflow, without
adding polling, a wrapper command, or automatic execution.
