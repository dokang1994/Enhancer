# Current Task

## Status

Completed

## Task

Connect the same optional `NORMAL`/`EXPEDITED` priority input and effective-priority output
to the generated-input `scheduler-submit-generated` CLI command, with generated replay
comparing the caller-owned requested priority against the stored manifest before consulting
the clock or recapturing repository context, without adding new authority, schema change,
queue selection change, or execution.

## Task ID

connect-scheduler-submit-generated-priority-input-and-output

## Context

The immediately preceding increment connected an optional `--priority` input and
effective-priority output to the explicit `scheduler-submit` command. Submission manifest
schema v2 already persists the priority and `DurableWorkSubmissionService` already
propagates it, so the generated-input command only needed the same caller-owned input,
first-use persistence, replay comparison, and output. The generated command previously
always admitted `NORMAL`.

## Justified By

- 2026-07-27: Persist Requested Scheduler Priority In Submission Manifest Before Exposing Admission Input

## Acceptance Criteria

- `scheduler-submit-generated` accepts one optional `--priority NORMAL|EXPEDITED`; omission
  defaults to `NORMAL`, any other value fails usage before manifest or queue mutation.
- First use persists the caller-owned priority in the generated manifest; a replay under
  the same submission UUID with a different priority fails closed before the clock or
  repository context is consulted.
- Bounded output reports the effective `priority` on both `ADMITTED` and `REPLAYED`.
- The generic message-admission `NORMAL` default, queue selection and fairness, schema,
  authority, and dependencies are unchanged.

## Out Of Scope

- Generic message-admission priority input, priority policy or defaults change, queue
  selection or fairness change, schema change, new authority, execution, polling, Gate 9,
  release, or deployment.

## Approval

The user explicitly asked to continue the project on 2026-07-28. This increment is the
generated-input half of the CLI connection already authorized by the accepted
priority-persistence decision.

## Allowed Tools

- read-file

## Verification

Acceptance was satisfied test-first. Two `CliArgumentsTest`, two
`GeneratedInputSubmissionServiceTest`, and three
`EnhancerCliSchedulerGeneratedSubmitIntegrationTest` cases were written first and failed to
compile on the absent `GeneratedSubmitCliCommand.priority()` and the eight-argument
`GeneratedSubmissionRequest`; after the minimal implementation the targeted reruns passed,
proving first-use `EXPEDITED` persists an `EXPEDITED` manifest and prints
`priority=EXPEDITED` on `ADMITTED` and exact `REPLAYED`, omission persists and prints
`NORMAL`, a same-UUID replay with a different priority fails closed before the envelope
factory runs (exit `2`, no manifest or queue-revision change), and lowercase/unknown values
are rejected.

The full strict `clean build --no-build-cache --warning-mode all` (encoding supplied
through `-Dorg.gradle.jvmargs=-Dfile.encoding=UTF-8`) passed 600 tests across 117 suites:
597 passed, three existing Windows symbolic-link privilege-dependent setup cases skipped,
and zero failures or errors, with no compiler warnings under `-Xlint:all -Werror`.
`git diff --check` produced no output and a bounded credential scan found zero matches.
Append-only evidence is in `docs/verification-log.md`.

## Next

The explicit and generated submission commands now both expose priority. Select the next
Scheduler increment from the roadmap's still-deferred selection work, for example
`scheduler-status`/`scheduler-recovery-status` surfacing the effective queue priority and
fairness state so an operator can observe how admitted priority affects selection, without
adding queue selection change, new authority, execution, or polling.
