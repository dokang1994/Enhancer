# 2026-07-31: Record Runtime Events As Goal-Bound Durable Facts Before Publication

Status: Accepted Decision

## Context

Gate 8 already retains durable Goal and AgentRun history, retry decisions, control
requests, RunRecords, leases, queue dispositions, and recovery checkpoints. Those
artifacts type many runtime outcomes, but there is no one bounded event contract that
lets later consumers observe retry, stagnation, timeout, cancellation, verification,
and completion without reinterpreting prose or treating a Message Bus delivery as
runtime authority.

The missing contract cannot collapse distinct facts. A recorded cancellation request is
not an authenticated cancellation application. A Verified AgentRun result is not yet a
Scheduler queue disposition. A retry decision is not a replacement attempt. Likewise,
an event must not become a second authority that can contradict the state, RunRecord,
control message, or queue transition from which it was derived.

The existing `MessageEnvelope` is sealed to Work, Result, Control, and Handoff payloads.
Coercing runtime events into one of those payloads would change its meaning, while adding
a fifth payload kind requires a separately reviewed Gate 7 wire-schema decision.

## Decision

Define `runtime-event-v1` as a reference-oriented, Goal-bound derived-fact contract.
Canonical runtime, RunRecord, control-message, and Scheduler queue artifacts remain the
transition authorities. The transition owner records an event only after its
authoritative state or evidence is durable, then may publish the durable event reference.

The finite v1 taxonomy is:

- `RETRY_DECISION_RECORDED`: an admitted or refused retry decision is durable;
- `RETRY_STARTED`: the admitted replacement AgentRun is durable;
- `STAGNATION_DETECTED`: a persisted RunRecord records the Agent Loop stagnation stop;
- `TIMEOUT_DETECTED`: a durable runtime or RunRecord fact records a Tool, process, or
  lease timeout;
- `CANCELLATION_REQUEST_RECORDED`: a bound Control request is durable but unapplied;
- `CANCELLATION_APPLIED`: a future authenticated Gate 12 application is durable;
- `VERIFICATION_RECORDED`: a RunRecord-backed Result transition is durable; and
- `WORK_ITEM_TERMINATED`: the Scheduler queue has durably recorded
  `VERIFIED_COMPLETED` or `FAILED`.

Each event carries one deterministic domain-separated canonical UUID, the schema and
kind, occurrence time, exact WorkItem, Goal, and AgentRun identities, task revision,
Workspace snapshot, logical-run and correlation identities, optional causal message or
event UUID, bounded producer identity, a kind-specific sealed detail value, and one
through four bounded references to the authoritative state revision, message, RunRecord,
evidence, decision, or queue revision. It carries no content body, credential, approval,
Tool grant, or transition authority.

One append-only stream belongs to one Goal and its exact retained WorkItem. The stream
has a finite 4096-event ceiling, one monotonic revision, an exact event prefix, and the
same bounded integrity-envelope, strict-UTF-8, no-symbolic-root, and atomic-publication
requirements as the current filesystem stores. Exact event replay is revision-free;
reuse of an event identity with changed content, a foreign Goal/WorkItem/AgentRun
binding, a non-monotonic prefix, corruption, or an unsupported schema fails closed.
There is no cleanup, retention, compaction, migration, scan, or cross-store transaction
claim.

Durable ordering is:

1. persist the canonical runtime, RunRecord, control request, or queue transition;
2. derive the deterministic event identity from its kind and authoritative reference;
3. append or exact-replay the event;
4. publish only the opaque durable event reference.

Recovery re-enters the existing transition-specific checkpoint or exact message replay,
re-derives the same identity, and exact-replays the append before another publication
attempt. Duplicate publication is therefore possible and consumers must deduplicate by
event identity. Transport acceptance remains distinct from bus delivery and event
recording. The first implementation adds only the immutable event value and append-only
store. A later bounded task adds a recorder and publisher port, and any concrete
Message Bus adapter requires a separate accepted evolution of the four-kind envelope
schema.

Detection and application ownership remains explicit:

- Agent Loop and Tool/process/lease boundaries detect stagnation or timeout; the
  transition owner records only after a durable RunRecord or runtime fact exists.
- `DurableAgentRunRetryController` owns retry decision and replacement-state facts.
- `RuntimeControlAdmissionHandler` owns request-recorded facts only; Gate 12 owns
  authentication, authorization, application, and `CANCELLATION_APPLIED`.
- `DurableAgentRunFinalizer` owns RunRecord-backed verification facts and records
  terminal WorkItem facts only after the Scheduler queue disposition is durable.

## Rationale

Derived append-only events make runtime facts observable without creating a competing
state machine. Deterministic identities and state-first ordering fit the repository's
existing at-least-once recovery model: a crash can leave a missing or multiply published
event, but re-entry cannot invent a different fact or silently widen authority.

Separating requests, detections, decisions, applications, verification, and terminal
queue outcomes preserves the lifecycle distinctions already required by the
Constitution and Completion Semantics. Deferring the concrete Message Bus adapter avoids
silently revising Gate 7's accepted four-payload wire contract inside a Gate 8
specification.

## Consequences

- Gate 8 has a bounded accepted runtime-event specification but no runtime-event
  implementation or maturity promotion.
- The immediate implementation consumer is an immutable `RuntimeEvent` value with
  kind-specific detail and bounded authoritative references plus an append-only
  `RuntimeEventStore`; its focused RED contract must reject invalid bindings, invalid
  kind/detail combinations, overflow, changed-identity replay, and prefix rewrite.
- Existing runtime state, RunRecords, control requests, queue state, checkpoints, and
  Message Bus contracts remain unchanged.
- Authenticated cancellation application stays in Gate 12; model and context budgets,
  Memory, production adapters, and role workers stay in their existing later gates.
- Publication wiring, payload-schema evolution, retention, cleanup, migration, and
  general orphan scanning require separate accepted tasks.
