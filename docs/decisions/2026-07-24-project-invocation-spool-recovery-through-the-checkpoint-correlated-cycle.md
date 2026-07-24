# 2026-07-24: Project Invocation-Spool Recovery Through The Checkpoint-Correlated Cycle

Status: Accepted Decision

## Context

The read-only Scheduler recovery projection identifies one durable checkpoint cycle and
its correlated Goal and AgentRun. The process-isolated execution boundary uses a private
`<invocation-root>/<goal-id>/<agent-run-id>` namespace containing at most one work spool
message and at most one result spool message. Operators can retain an incomplete spool
for recovery or diagnosis, but cannot currently inspect that namespace without bypassing
the checkpoint correlation policy or inferring a cycle from filesystem layout.

The invocation root is independent from the queue, runtime, checkpoint, RunRecord,
Evidence Store, and external-effect ledger. A directory scan could invent correlation;
result-message presence alone is a claim rather than proof of a valid RunRecord. Existing
Scheduler recovery and external-effect-status callers must not gain required invocation
root arguments.

## Decision

Add a runtime-owned read-only invocation-spool recovery projection and a separate
`scheduler-invocation-status` CLI command.

The new reader will reuse `SchedulerRecoveryStatusReader` as the sole
queue/checkpoint/runtime/RunRecord correlation policy. It will inspect no invocation
path when that projection has no correlated Goal and AgentRun. Otherwise it will derive
only the private Goal/AgentRun invocation namespace, require ordinary non-symbolic
boundaries, and inspect the `work` and `result` spools without creating, deleting,
claiming, launching, or consuming a message.

The projection will distinguish an absent invocation namespace, an empty or malformed
spool, one correctly bound work message awaiting a child result, and one correctly bound
result message. Work and result messages must match the checkpoint-correlated WorkItem
and its exact correlation, logical-run, causation, task, destination, and AgentRun/Goal
identities. A result remains a claim: its RunRecord reference must resolve through the
same Scheduler recovery context and agree with the persisted record's task, source,
execution input, and verification status. Corrupt, foreign, several-message,
symbolic-link, or concurrently changed observations fail closed rather than being
classified as recovery state.

Because no cross-store transaction exists, the reader will take a bounded second
Scheduler projection and invocation sample and reject any changed correlation, runtime
revision, namespace presence, or spool content identity. The CLI will require the
existing Scheduler recovery roots plus one explicit invocation root and a bounded output
limit. It will report only typed phase, bounded digest/identity metadata, and no payload,
evidence, or RunRecord content.

## Rationale

Reusing the existing checkpoint anchor prevents an untrusted caller-selected filesystem
path from becoming Scheduler recovery state. Inspecting the same route and binding rules
as process-isolated execution preserves the parent boundary's “result is a claim” model,
while a separate command keeps the established recovery and external-effect interfaces
unchanged.

## Consequences

- Operators can distinguish retained transport prefixes from absent or corrupt invocation
  state without mutating the queue, runtime, checkpoint, or spool.
- A no-checkpoint observation never discovers invocation namespaces.
- Concurrent execution may cause inspection to fail explicitly; the operator may repeat
  the read-only command.
- The projection does not claim child-process liveness, execute or retry a worker,
  recover a queue, clean a spool, authorize an external effect, or prove any external
  system state.
