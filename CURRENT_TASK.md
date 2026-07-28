# Current Task

## Status

Completed

## Task

Connect one durable local transport-spool Work message through the real Message Bus into
existing durable Scheduler admission, expose the point receiver through a separate CLI,
and prove the separately invoked Scheduler service completes the admitted work.

## Task ID

connect-durable-work-spool-to-scheduler-worker

## Context

`FileSpoolMessageTransport` durably accepts one routed envelope, but no supported receiver
publishes that retained artifact through `InProcessMessageBus` into
`DurableWorkItemAdmissionHandler`. Scheduler execution already has a separate supported
bounded service and must remain distinct from message receipt.

## Justified By

- 2026-07-28: Receive One Durable Work Spool Through The Message Bus Before Scheduler Admission
- 2026-07-28: Retain Gate 8 At Specified Next After The Bounded Service Connection

## Acceptance Criteria

- `scheduler-receive-work` accepts one explicit spool root and canonical transport
  filename, expected queue destination, existing queue root/identity, required
  capability, and optional exact Scheduler priority.
- The receiver rejects missing, symbolic, corrupt, foreign-destination, and non-Work
  spool artifacts before queue mutation.
- The unchanged envelope is published through a real `InProcessMessageBus` queue
  subscription backed by `DurableWorkItemAdmissionHandler`.
- Bounded output distinguishes durable `ADMITTED` from exact `REPLAYED` and reports the
  queue identity/revision, derived WorkItem identity, and effective priority.
- Focused tests are RED first and cover argument validation, point containment, route and
  payload refusal, first admission, and exact replay.
- A real-filesystem integration sends through `FileSpoolMessageTransport`, receives
  through the supported command, executes through a separate `scheduler-service`, and
  proves exact re-receipt changes no terminal queue revision and creates no second
  AgentRun or RunRecord.
- Existing transport, submit, cycle, drain, and service behavior, schemas, dependencies,
  and authority remain unchanged; the fresh strict full build passes.
- The completed increment is committed on a feature branch, pushed, merged into
  `main`, and the resulting `main` is pushed.

## Out Of Scope

- Spool deletion/rename/acknowledgement, directory scanning, durable bus journal,
  background consumer, queue creation, combined receive-and-execute wrapper, remote
  transport, authenticated controls, external adapters, multi-agent roles, schema or
  dependency changes, release, or deployment.

## Approval

The user explicitly directed continuation and authorized commit, feature-branch push,
merge into `main`, and the resulting `main` push on 2026-07-28.

## Allowed Tools

- read-file
- write-code
- write-docs
- verify

## Verification

Fresh focused tests passed for `DurableWorkMessageReceiverTest`, `CliArgumentsTest`, and
`EnhancerCliSchedulerReceiveWorkIntegrationTest`. The fresh strict
`clean build --no-daemon --rerun-tasks` passed 625 tests across 121 suites: 621 passed,
four Windows symbolic-link privilege-dependent cases skipped, and zero failures or
errors.

## Next

Assess whether a durable bus journal or an explicit spool acknowledgement/retention
protocol is the next Gate 7-owned connection.
