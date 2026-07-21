# 2026-07-21: Persist Bound Runtime Control Requests Without Applying Unauthenticated Transitions

Status: Accepted Decision

Context:

- `ControlPayload` already carries bounded CANCEL, PAUSE, and RESUME requests, but no
  real Gate 7 consumer connects those envelopes to Gate 8 durable runtime state.
- Gate 12 owns authenticated user-facing controls. It has no implementation, so a
  control envelope cannot currently prove that its producer may change a Goal,
  AgentRun, lease, queue, worker, or Tool execution.
- The in-process bus can retry and dead-letter a failing handler, but its journal and
  delivery idempotency are process-local. Restart-safe control input therefore needs
  idempotency in the durable consumer as well.
- The unreleased Agent runtime schema v1 has already been revised in place for exact
  WorkPayload execution input, with older local snapshots deliberately failing closed.

Decision:

- Add an immutable ordered control-request ledger to `AgentRuntimeState`, bounded to 256
  exact `MessageEnvelope` values and retained across every later lifecycle transition.
- Admit only a `ControlPayload` envelope whose logical run and correlation equal the
  Goal work message and whose causation names that exact work message. The control
  message identity must be distinct from Goal, WorkItem, work-message, and AgentRun
  identities.
- Accept requests only while the Goal is active and an AgentRun exists. Terminal Goals
  reject later control requests rather than recording impossible post-terminal intent.
- Make exact message replay idempotent across runtime restart: the same identity plus
  equal envelope returns the existing revision; the same identity with different content
  fails closed. Distinct requests retain admission order until the 256-entry bound.
- Persist the next runtime revision before exposing a newly recorded request. Add a
  Gate 7 `MessageHandler` that recovers the named Goal, records the request, and converts
  checked storage failure to handler failure so the existing bounded bus retry and
  dead-letter path remains authoritative.
- Record request only. No control request changes Goal or AgentRun status, lease/fence,
  queue state, worker behavior, Tool scope, execution input, or bus cancellation. The
  envelope producer and reason are diagnostic provenance, never authority.
- Extend the existing runtime filesystem encoding with the bounded control ledger and
  `ControlPayload` message kind. Keep schema v1 for the unreleased artifact; snapshots
  written before this field fail closed rather than being guessed or migrated.

Rationale:

A durable request ledger is the smallest honest connection between message delivery and
runtime control. It preserves the user's request and exact provenance while refusing to
pretend that unimplemented authentication exists. Runtime-level identity binding and
idempotency make bus replay safe across process restart, and storing before handler
return gives Gate 7 delivery a real durable downstream fact. Actual state transitions
remain a later connection from Gate 12's authenticated control boundary.

Consequences:

- Gate 7 gains one named real control-payload queue consumer connected to Gate 8 durable
  state and filesystem recovery.
- The current worker remains behaviorally unchanged; recorded requests await an
  authenticated consumer.
- A storage failure may be retried or dead-lettered by the existing bus policy without
  exposing an unpersisted request.
- Old unreleased schema-v1 runtime artifacts without the ledger fail closed.
- Out of scope remains authenticated application, worker/process interruption,
  pause/resume/cancel state machines, queue control, durable bus state, and control
  history beyond the bounded ledger.
