# 2026-07-29: Retain Gate 8 And Specify Explicit Runtime Events Next

Status: Accepted Decision

## Context

The prior Gate 8 assessment identified message-mediated worker operation as a missing
whole-gate connection. The process-isolated child now receives Work through a real
Message Bus queue, and the parent already validates the returned Result through another
real queue. The runtime also has durable Goal and AgentRun state, dependency-aware
single-worker scheduling, fenced leases, bounded retry, explicit migrations, durable
recovery checkpoints, and evidence-bound external-effect outcomes.

Whole-gate promotion still requires every accepted scope item and exit criterion. Later
gates own model/context/cost budgets, Memory, authenticated control application,
production adapters, and background or role-based workers. Gate 8 itself still lacks one
bounded durable runtime-event contract covering the required retry, stagnation, timeout,
cancellation, verification, and completion semantics.

## Decision

Retain Delivery Gate 8 at `Specified - Next`.

Classify the bounded single-agent Scheduler/runtime foundation as Integrated, with named
Operational explicit submit-and-cycle workflows, but do not treat those sub-paths as
whole-gate maturity.

Classify the accepted Gate 8 scope as follows:

| Scope item | Assessment | Named evidence or owner |
|---|---|---|
| Persisted Goal and AgentRun state machine | Satisfied | Schema-v2 runtime history and persist-before-exposure lifecycle |
| Planner-to-Done lifecycle | Partial | Planning, execution, verification, retry, and terminal state exist; Memory and Reflection are Gate 10 work |
| Queues, dependencies, cycles, leases, idempotency, and recovery | Partial | Durable single-worker queue, backward-only dependency validation, cycle refusal, fenced leases, replay, and checkpoints exist; authenticated control application is Gate 12 and broader budgets are Gate 9 |
| At-least-once effects, migration, orphan recovery, and compensation | Partial | Stable effect identities, fence checks, explicit outcomes, supported migrations, lost-acknowledgement recovery, and bounded reclamation exist; production adapter recovery is Gate 11 and general inventory/cleanup requires a separate retention decision |
| Priority and fairness under governing constraints | Partial | Durable normal/expedited fairness preserves admitted authority; cost/time and model-context constraints are Gate 9 |
| Planner, Coder, Reviewer, Tester, and Memory roles | Unsatisfied | Role-based message workers are Gate 13 and Memory is Gate 10 |
| Single-agent sequential worker first | Satisfied | Process-isolated one-cycle, drain, and bounded foreground service paths |
| Dependency Analyzer and Verification Engine | Partial | Backward dependency validation and the Gate 4 verification engine are connected; broader dependency analysis remains a later bounded contract |
| Resource budgets, locks, leases, and checkpoints | Partial | Queue bounds, retry bounds, OS queue lock, leases, and cycle checkpoints exist; model/context/cost/time budgets are Gate 9 |

Classify the exit criteria as follows:

- satisfied: interruption and restart resume from durable queue, runtime, lease,
  checkpoint, RunRecord, and effect state;
- satisfied for the supported single-agent worker: Work and Result cross real Message Bus
  queues before execution or result exposure;
- partial: retry, timeout, verification, and completion have typed state or outcomes, but
  the required unified explicit runtime-event contract does not yet cover stagnation and
  cancellation or durably bind every transition;
- satisfied: scheduling retains the unchanged Work envelope and Tool scope and cannot
  expand task or Tool authority;
- satisfied for supported fixtures: duplicate delivery, retained-point and result
  lost-acknowledgement windows, lease expiry, restart, and supported migrations converge
  without an unrecorded duplicate effect;
- satisfied at the Gate 8 boundary: no exactly-once claim is made, and every terminal
  external effect is recorded as applied, deduplicated, compensated, or requiring user
  recovery; real adapters remain Gate 11.

Select specification of the bounded Gate 8 runtime-event taxonomy, identity/provenance,
persistence, and publication ownership as the next task. It must reuse existing state and
message contracts, assign detection/application policy to the correct later gate, and
must not implement authenticated controls, model budgets, Memory, production adapters,
or role workers.

## Rationale

The new Work connection closes the concrete message-mediated worker gap but not the
cross-gate product scope. Recording the runtime foundation separately preserves its real
integration evidence without using it to imply missing Memory, security, adapter, model,
or orchestration behavior. The explicit-event criterion is the remaining Gate 8-owned
gap that can be bounded without taking authority from later gates.

## Consequences

- Gate 8 remains `Specified - Next`.
- The bounded single-agent Scheduler/runtime foundation is Integrated; its existing
  explicit operator workflows remain Operational sub-paths.
- The obsolete deferred “supported durable message-bus-to-worker connection” is removed.
- Gate 9 is not activated because its dependency requires an operational event-driven
  single-agent runtime.
- The next task specifies the Gate 8 runtime-event contract before any event-store or
  publication implementation is authorized.
