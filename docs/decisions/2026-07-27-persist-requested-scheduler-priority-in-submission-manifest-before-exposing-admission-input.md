# 2026-07-27: Persist Requested Scheduler Priority In Submission Manifest Before Exposing Admission Input

Status: Accepted Decision

## Context

Queue schema v3, durable claim selection, and fairness progress now preserve and consume
`QueuedWork` priority. Every production admission path still constructs the
two-argument compatibility form and therefore admits `NORMAL`.

Priority is Scheduler selection intent. It changes neither the exact `WorkItem`, its
Gate 7 `WorkPayload`, allowed Tools, execution input, nor approval authority. The
explicit and generated submission paths already use `DurableSubmissionManifest` as the
sole immutable owner of caller intent before queue creation, but manifest schema v1
does not retain priority. Adding a CLI value without persisting it there would make an
uncertain replay unable to distinguish exact intent from changed priority.

The generic Gate 7-to-Gate 8 `DurableWorkItemAdmissionHandler` has no separate
caller-owned Scheduler intent. Its existing constructor is also used by integrations
that intentionally predate priority input.

## Decision

Requested priority belongs in `DurableSubmissionManifest` as exactly one
`SchedulerPriority`. It does not belong in `WorkItem`, `MessageEnvelope`,
`WorkPayload`, required capability, or Tool authority. A compatibility construction
path defaults to `NORMAL`.

Submission manifest schema v2 persists the requested priority as part of immutable
exact intent. Exact replay requires the same priority; changing `NORMAL` to
`EXPEDITED` or the reverse under one submission identity fails closed before queue
mutation. `DurableWorkSubmissionService` must pass the manifest priority to the
dependency-free `QueuedWork` admitted through the durable handler. The existing
message-delivery handler construction remains `NORMAL`; an explicit priority-bearing
construction may be added only for the submission composition and grants no new
authority.

Existing schema-v1 manifests map losslessly to `NORMAL`, because every production
schema-v1 submission path admitted through the handler's `NORMAL` compatibility
behavior. Ordinary resolution must accept only schema v2 after the transition. A
separate submission-identity-scoped stopped-submission migration must validate the
complete v1 artifact, prepare and reread a same-directory v2 candidate, recheck source
bytes, atomically replace only the unchanged valid source, and preserve the original
on every earlier failure. Typed absent, already-current, and migrated outcomes must not
invent intent or invoke queue recovery, admission, claims, execution, or Tools.

Only after that persistence prerequisite is Contract Verified may both explicit and
generated submission surfaces expose an optional `--priority NORMAL|EXPEDITED` input.
Omission remains `NORMAL` for compatibility. Generated replay must compare the
caller-owned requested priority with the stored manifest before consulting the clock
or recapturing repository context. Bounded success output should report the effective
priority. Invalid or conflicting input must fail before manifest or queue mutation.

## Rationale

The immutable manifest is already the owner of every caller-supplied fact needed to
resume the manifest-to-queue-to-admission prefix. Persisting priority anywhere else
would create a second recovery authority or allow replay to change selection semantics
under one identity. Defaulting historical manifests and generic message delivery to
`NORMAL` preserves existing behavior, while explicit migration keeps ordinary
resolution fail-closed and makes the format transition observable and testable.

## Consequences

- Manifest priority remains Scheduler metadata and cannot widen approval or Tool
  authority.
- Manifest schema v2 and its explicit v1 migration are prerequisites for public
  priority input.
- Exact manifest equality, generated caller-intent comparison, and exact queue
  admission all include priority.
- The first implementation task changes persistence and migration only; CLI priority
  input, generated-request priority, and output changes remain a later connection.
- Dependencies, aging, additional priority classes, per-submission burst settings, and
  priority changes after admission remain outside this boundary.
