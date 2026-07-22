# Current Task

## Status

Completed

## Task

Promote the generated-input Scheduler submission to an Operational sub-path with an actual
Enhancer-repository smoke run and documented recovery, keeping submission separate from
execution and adding no polling, wrapper command, or automatic execution.

## Task ID

promote-generated-input-submission-operational

## Context

The `scheduler-submit-generated` boundary and CLI were Contract Verified and Integrated but
had no actual-repository smoke run or operator recovery documentation. This increment ran
the generated-input submission and a separate recovery cycle against the real repository
over one shared explicit queue root and documented the generated-input recovery actions
alongside the existing explicit two-command workflow.

## Justified By

- 2026-07-22: Reuse The Immutable Submission Manifest As The Sole Generated-Input Recovery Record
- 2026-07-22: Expose Durable Submission As A Separate Explicit CLI Command
- 2026-07-22: Expose One Process-Isolated Durable Scheduler Cycle Through The CLI

## Acceptance Criteria

- An actual Enhancer-repository smoke run invokes `scheduler-submit-generated` and then a
  separate `scheduler-cycle` over one shared explicit queue root and derived queue identity,
  observing `ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE` with one retained manifest,
  one RunRecord, and no duplicate execution.
- The README documents the generated-input command and the generated-input recovery actions
  (submission interruption before and after manifest persistence, exact replay, verified
  completion, and idle re-entry) alongside the existing explicit two-command workflow.
- Submission stays separate from execution and polling; no wrapper, polling loop, or
  automatic execution is added, and the explicit `scheduler-submit` command is unchanged.
- Roadmap, state, changelog, and verification documents record the Operational sub-path
  promotion only where their owning facts change.

## Out Of Scope

- Any polling loop, wrapper command, background service, or automatic execution.
- New production behavior, schema change, second durable store, external adapter effects,
  authenticated control application, Gate 9, release, or deployment.

## Approval

The user explicitly asked to continue the project and to complete the increment on 2026-07-22.

## Allowed Tools

- read-file

## Verification

Acceptance is satisfied by a real-repository smoke run, README recovery documentation,
repository document-ownership checks, and a fresh strict-lint Gradle build. Append-only
results are recorded in `docs/verification-log.md`.

The real-repository smoke run reading `README.md` observed `ADMITTED` (queue revision 1),
`VERIFIED_COMPLETED` (one RunRecord, one completed WorkItem), `REPLAYED` (identical
occurrence time and Workspace snapshot, unchanged queue revision), and `IDLE`, with exactly
one retained submission manifest and one RunRecord and no duplicate execution. The full
strict-lint Gradle build passed across 91 suites / 474 tests (471 passed, three existing
Windows symbolic-link setup cases skipped, zero failures or errors), and `git diff --check`
produced no output.

## Next

Design and record the smallest bounded external-effect adapter execution increment
(ordered connection #5): let an owning Tool/adapter execute one durable external effect with
a stable idempotency key and establish its applied, deduplicated, compensated, or
requires-user-recovery outcome against the existing bounded fence-checked ledger, without
adding a second AgentRun, unauthenticated control, or a universal exactly-once claim.
