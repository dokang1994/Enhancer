# 2026-07-22: Expose One Process-Isolated Durable Scheduler Cycle Through The CLI

Status: Accepted Decision

Context:

- The durable Scheduler can recover a filesystem queue and drive one WorkItem through the
  process-isolated AgentRun path, but that composition is reachable only from tests.
- Combining work submission with execution would require a new durable invocation manifest and
  an exact cross-bus admission-replay contract. Neither contract is accepted in the active task.
- A supported boundary must not infer storage locations, create a queue implicitly, hide a
  terminal WorkItem failure behind a successful process exit, or claim a polling service.

Decision:

- Add a `scheduler-cycle` CLI command that recovers one caller-identified existing durable queue
  and runs exactly one `DurableAgentRunWorker.processIsolated` cycle.
- Require explicit project, queue, runtime, external-effect, cycle-checkpoint, evidence,
  RunRecord, and invocation roots together with queue identity, owner identity, retry-attempt
  bound, lease duration, and child-process timeout.
- Never create a queue or admit work from this command. Missing queue state and malformed command
  inputs are usage/configuration failures.
- Report one bounded machine-readable status: `IDLE`, `VERIFIED_COMPLETED`, or `FAILED`.
  `IDLE` and `VERIFIED_COMPLETED` exit successfully; terminal Scheduler work failure uses a
  stable Scheduler-specific nonzero exit code; unexpected storage corruption or execution
  failure remains an internal error.
- Compose the real filesystem stores, system UTC clock, explicit retry policy and durations, and
  real child-process execution path. Preserve the Worker's existing cycle checkpoint recovery
  semantics rather than adding a CLI-owned checkpoint.

Rationale:

This is the smallest honest supported Scheduler entry point. It exposes the already-integrated
recovery and process-isolation boundary without silently inventing queue lifecycle, durable
submission, service-loop, or cross-store transaction guarantees.

Consequences:

- An operator can explicitly run or recover one durable Scheduler cycle and distinguish idle,
  verified completion, and terminal WorkItem failure from process exit and bounded output.
- Queue creation, work admission, polling, durable message journaling, exact cross-bus replay,
  authenticated controls, external-effect adapters, and Gate 9 remain outside this command.
- The command makes no Gate 8 Operational or whole-gate maturity promotion by itself.
