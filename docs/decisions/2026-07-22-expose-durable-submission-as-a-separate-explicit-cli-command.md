# 2026-07-22: Expose Durable Submission As A Separate Explicit CLI Command

Status: Accepted Decision

Context:

- The immutable submission manifest and queue-creation/admission service are restart-safe
  but have no supported end-user entry point.
- `scheduler-cycle` intentionally recovers and executes one existing queue. Combining it
  with submission would hide a second external effect behind creation and blur recovery,
  output, and failure ownership.
- Governed work authority already comes from `ApprovedTaskReader` and the repository-memory
  Workspace snapshot. Message identity and occurrence time must not be generated implicitly
  if an interrupted caller is expected to replay the exact submission.

Decision:

- Add one `scheduler-submit` command that performs submission only. It never invokes a
  Scheduler worker, Tool, evidence store, RunRecord store, or `scheduler-cycle`.
- Require explicit project, submission-store, and queue-store roots; active task, queue,
  message, correlation, logical-run, and producer identities; queue capacity; required
  capability; occurrence time; target path; and expected SHA-256.
- Load the governed project and require the explicit task identity to equal the active
  approved task with `read-file` in its allowed Tool scope. Capture the repository-memory
  Workspace snapshot at the supplied occurrence time and use that same time in the work
  envelope, preserving deterministic exact replay without a second time input.
- Treat this as a root work submission with no causation identity. Retain task revision,
  snapshot identity, allowed Tools, and explicit execution input in the exact work payload.
- Route the resulting manifest through `DurableWorkSubmissionService`. Report bounded
  `ADMITTED` when the queue revision advances and `REPLAYED` when exact prior admission is
  unchanged, together with manifest/queue creation flags and stable identities/revision.
- Classify malformed inputs, repository-authority mismatch, manifest identity conflict, and
  queue-capacity mismatch as usage/configuration failures. Keep unexpected storage corruption
  or I/O failure as internal failure.

Rationale:

Explicit replay inputs let an operator reconstruct the same immutable manifest after a
process stop, while repository-derived authority prevents the command from inventing task
scope. Separating submission from execution keeps each durable external effect, recovery
prefix, and status independently observable.

Consequences:

- An operator can durably create/recover a queue admission through a supported command and
  then choose whether and when to invoke `scheduler-cycle` separately.
- The command is verbose by design; automatic UUID/time generation requires a separately
  durable operator workflow or invocation manifest and is not introduced here.
- Polling, concurrent submission, multi-process locking, authenticated controls, external
  adapter execution, Gate 9, and whole-Gate Operational promotion remain outside this task.
