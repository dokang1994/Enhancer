# RFC-0009: Multi-Agent Orchestration

Status: Accepted

## Maturity And Sequence

This RFC accepts the target contracts and implementation order. It does not claim that a Multi-Agent runtime exists. Multi-Agent execution remains Planned at Delivery Gate 13, and Delivery Gate 6 Workspace and Project Brain Foundation is the next implementation gate.

The dependency path is:

```text
operational single-worker run and durable RunRecord
-> immutable WorkspaceSnapshot
-> typed Event/Message Bus
-> durable Agent Runtime and Scheduler
-> provider-neutral Model Gateway and validated Skills
-> bounded Multi-Agent orchestration
```

## Purpose

Define provider-neutral orchestration contracts that allow multiple capabilities to collaborate without weakening repository authority, Tool policy, evidence integrity, independent verification, or replayability.

## Governing Principles

- The Kernel orchestration controller is the sole owner of terminal task and run state.
- Agents own bounded work items or artifacts, not approval, authority, verification, or completion.
- Repository documents and durable runtime records remain authoritative; prompts, chat, Markdown handoffs, and worker self-reports are inputs or projections.
- Every worker consumes an immutable input snapshot and an explicit authorization reference.
- Every handoff uses the common versioned Message Bus envelope.
- The Scheduler assigns work through bounded leases, budgets, and idempotent delivery.
- Producer-Reviewer workflow review never replaces the independent verifier or durable RunRecord.
- The smallest topology that can satisfy the approved task is selected first.
- Provider-specific behavior remains a removable adapter detail after Kernel policy evaluation.

## Pattern Selection

The future planner records its selection rationale in a versioned `CoordinationPlan` and uses this progression:

| Pattern | Selection condition | Required control |
|---|---|---|
| single worker | one bounded context can satisfy the task | default Gate 8 topology |
| sequential pipeline | one stage requires an earlier stage's artifact | typed, durable handoff after every stage |
| Producer-Reviewer | an artifact has explicit review criteria | bounded pass/fix/redo loop; reviewer remains distinct from verifier |
| bounded fan-out/fan-in | branches are independent and synthesis criteria are predefined | common snapshot, isolated ownership, deterministic reducer |
| expert routing | only a subset of capabilities should receive the work | recorded routing decision and fallback |
| supervisor allocation | a durable backlog changes during execution | queue ownership, leases, reassignment rules, and cumulative budgets |
| shallow hierarchy | stable subdomains require local coordination | no more than one subordinate coordination layer |

Parallelism is rejected when branches depend on each other's intermediate state, share a mutable write set, have no deterministic synthesis owner, or cannot preserve a common input snapshot.

## Capability Roster

Planner, Architect, Coder, Reviewer, Tester, Documenter, Memory, and future specialist labels describe schedulable capabilities. They are not identities, permissions, providers, or fixed prompt personalities.

A `CapabilityDescriptor` contains at least:

- stable identifier, version, digest, and provenance;
- supported capability identifiers and input/output artifact schemas;
- required Skill identifiers and versions;
- requested Tool categories and model requirements;
- verification requirements, availability, and maturity.

The selected roster is an immutable revision derived from the approved task, validated descriptors, policy, data classification, budgets, and isolation capacity. Discovery, installation, role names, or descriptor contents never grant authority. Roster changes require a new revision, a durable event, and renewed policy evaluation.

## Gate-Owned Profiles

An `OrchestrationProfile` is a read-only aggregate view over contracts owned by separate gates; it is not a new authority source:

- Gate 8 owns the scheduling, resource-budget, lifecycle-control, lease, checkpoint, and recovery profile.
- Gate 9 owns the provider-neutral `ModelExecutionProfile`, including model class, locality, context, token, cost, time, redaction, and response-validation limits.
- Gate 13 owns the versioned `CoordinationPlan`, including topology, roster, dependency graph, delegation, artifact ownership, synthesis, review, and conflict policy.

Provider adapters translate an already approved `ModelExecutionProfile` into provider options. They cannot alter the coordination plan or expand Tool, model, data, network, or external-action authority.

## Coordination Plan

A `CoordinationPlan` records at least:

- plan, version, run, goal, task, and authorization-snapshot identities;
- selected pattern and its rationale;
- input `WorkspaceSnapshot` identity and roster revision;
- acyclic dependency graph and required capability for each work item;
- work-item, artifact, and declared write-set ownership;
- synthesis owner, deterministic reduction criteria, and review policy;
- cumulative budget partitions, maximum concurrency, delegation depth, attempts, and revisions;
- conflict, timeout, cancellation, recovery, and fallback policy.

Plan changes are explicit new versions. A worker cannot silently spawn another worker, rewrite dependencies, change the common snapshot, or enlarge a budget.

## Work And Handoff Contracts

### MessageEnvelope

Every work, result, event, or control handoff uses the common Gate 7 envelope. It preserves at least:

- `schemaVersion`, `messageId`, `messageType`, `runId`, `goalId`, and `workItemId`;
- producer identity and target queue or required capability;
- `correlationId`, `causationId`, `idempotencyKey`, and stream sequence;
- authorization-context and provenance references;
- payload schema, version, content identity, or bounded evidence reference;
- attempt, occurrence time, and deadline.

Large content is stored behind integrity-checked artifact or evidence references. A handoff file may project this state for a human, but it is not the queue, lock, authorization token, or canonical runtime record.

### WorkItem

A `WorkItem` contains at least:

- run, goal, task, work-item, parent, and dependency identities;
- required capability and immutable input-snapshot reference;
- authorization reference and effective Tool scope;
- expected artifact contract and verification-plan reference;
- bounded budget, deadline, maximum attempts, state, and state version.

### DelegationRecord

A delegation records parent and child work identities, target capability, bounded scope, Tool and Skill subset, budget slice, deadline, input/output contract, and authorization and causation references. Delegation cannot reset retries or budgets, create new permission, or exceed the plan's depth and concurrency limits.

### WorkerLease And Heartbeat

A lease or heartbeat preserves worker, assignment, lease, and run identities; fencing token; observed state; progress key; checkpoint reference; cumulative usage; sequence; and emission or expiry time.

A heartbeat is liveness telemetry. It is not progress, authority, verification, or completion evidence. A stale heartbeat triggers reconciliation of lease, process, and durable work state; it does not by itself prove failure.

### RunControlCommand

Authenticated control clients may propose typed commands such as `PAUSE`, `RESUME`, `CANCEL`, `REPRIORITIZE`, `REASSIGN`, and `ADD_WORK`. A command records command, run, and target identities; requester; authorization reference; reason code; expected state version; and bounded payload.

`ADD_WORK`, worker creation, or any scope, budget, permission, data, network, or external-action expansion remains a Proposal until the normal authority path approves it.

## Scheduler And Recovery

The Scheduler and Message Bus must:

- validate dependency identities and reject cycles before dispatch;
- issue one valid fenced lease per work-item attempt;
- make assignment, expiry, pause, resume, cancellation, timeout, and reassignment observable;
- suppress duplicate Tool side effects across delivery and replay through idempotency contracts;
- use structured retryable, terminal, cancelled, timed-out, dead-lettered, and blocked states;
- keep retries, revisions, concurrency, delegation depth, context, time, token, cost, and external calls bounded and cumulative;
- checkpoint and resume from durable state without chat history;
- preserve every attempt and decision in the Execution Graph and RunRecord;
- stop, pause, or replan when ownership, isolation, authority, or deterministic synthesis is ambiguous.

Replay does not create fresh authority. Re-execution rechecks current cancellation and policy state while preserving the original authorization and causation lineage.

## Workspace Isolation And Synthesis

Parallel branches consume the same immutable snapshot. They must be read-only or use explicitly isolated workspace or branch adapters with declared, non-overlapping write sets.

Every fan-in has a named synthesis owner and predefined reduction criteria. All branch artifacts, failures, timeouts, and conflicts remain visible. The reducer cannot silently overwrite, discard a losing result, or choose a winner using an unrecorded subjective score.

## Review And Verification

A `ReviewDecision` binds the original approved request, common snapshot, produced artifact, criteria version, producer, reviewer, typed result, reasons, evidence, revision count, and remaining limit.

Workflow review may return `PASS`, `FIX`, `REDO`, or `REJECT`. The producer and reviewer are distinct, and revision exhaustion stops explicitly. Reviewer `PASS` does not mean Gate 4 `VERIFIED`.

Only the independent verifier can produce the applicable verification decision. Only verified finalization followed by durable RunRecord persistence can produce `COMPLETED`.

Quality gradients, confidence, prompt adherence, activity labels, and estimated progress are diagnostic observations. They may prioritize inspection or propose intervention, but they cannot drive lifecycle promotion or replace objective criteria and evidence.

## Authority And Budget Invariants

- Effective authority is the intersection of task approval, `ExecutionPolicy`, parent delegation, selected Skill permission, and current data or external-action policy. No stage takes their union.
- Child budget allocations plus the parent's retained budget cannot exceed the parent's remaining cumulative budget.
- Retry, replay, reassignment, and worker creation do not reset consumed budget.
- Dependency and causation graphs remain acyclic.
- Duplicate delivery or replay cannot duplicate Tool side effects.
- Plan, roster, snapshot, or policy changes create new versions and durable events.
- Conflicts, stale ownership, and exhausted limits stop, pause, or replan; they never cause silent overwrite or fabricated completion.
- Final completion always requires an independent Verified decision and a durable RunRecord.

## Gate Ownership

| Gate | Owned contract |
|---|---|
| 6 | immutable input snapshot, freshness, provenance, data classification, and task/dependency references |
| 7 | one versioned envelope, delivery, idempotency, replay, dead-letter, ordering, backpressure, and typed handoff/control payloads |
| 8 | `WorkItem`, dependency queue, assignment, lease/fencing, heartbeat ingestion, checkpoint, lifecycle control, cumulative budgets, and single worker first |
| 9 | provider-neutral `ModelExecutionProfile`, routing, locality, redaction, model budgets, and response validation |
| 10 | validated capability, Skill, and Workflow metadata; progressive loading; permission intersection; memory provenance |
| 11 | install, provenance, compatibility, trust, and revocation for Agents and plugins |
| 12 | authenticated inspection and run-control interfaces as projections over Kernel state |
| 13 | pattern selection, `CoordinationPlan`, roster, delegation, Producer-Reviewer, bounded fan-out/fan-in, synthesis, conflict handling, and background execution |
| 15 | immutable evaluation baseline, bounded candidate experiments, before/after evidence, independent decision, and tested rollback |

No later gate may move its authority into a prompt, provider adapter, dashboard, or worker role.

## Reference Alignment

This RFC selectively translates ideas from these commit-pinned references:

- [Archon `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16`](https://github.com/martino-vigiani/Archon/tree/263cf3658a7cadefa0c5fbe82cc527a00ffb4c16) (MIT): dynamic capability rosters, centralized configuration, dependency-ready work, pause/resume/cancel/reassign controls, heartbeat and stuck diagnostics, and resumable operational visibility.
- [meta-harness `ccab9a677878f72b3316de464c99b36f56a3f2e7`](https://github.com/SaehwanPark/meta-harness/tree/ccab9a677878f72b3316de464c99b36f56a3f2e7) (Apache-2.0): topology selection, deterministic handoffs, Producer-Reviewer, bounded fan-out, supervisor patterns, explicit normal/failure scenarios, and removable provider heuristics.

They are references, not runtime, package, installer, schema, prompt, or governance dependencies. Any future copied or adapted code must receive a separate license and compatibility review.

Enhancer explicitly rejects:

- provider CLI subprocesses as the core runtime boundary;
- file-polling JSON or `_workspace` files as the canonical bus or state store;
- generated `.agents`, `.codex`, or replacement `AGENTS.md` rules as project authority;
- shared-worktree parallel mutation;
- worker-selected unleased work or unapproved worker creation;
- optional QA that can bypass independent verification;
- subjective scores as state-transition or completion evidence;
- unlimited timeouts, retries, revisions, context, delegation, or concurrency;
- silent ring-buffer loss of audit events or branch output;
- external workflow phases that replace the Constitution lifecycle or Delivery Gates.

## First Implementation Slice

The first Gate 13 slice is a sequential queue-mediated flow after its prerequisite gates are operational:

```text
Planner -> Queue -> Coder -> Queue -> Reviewer -> Queue -> Tester -> Memory
```

Every arrow is a durable Message Bus delivery. The slice uses one common snapshot, one work owner at a time, bounded workflow review, one Kernel terminal-state owner, independent verification, and a replayable RunRecord. Parallel fan-out is a later increment.

## Verification Scenarios

Tests must cover at least:

- single worker is the default and unjustified fan-out is rejected;
- every handoff preserves snapshot, task revision, authority, provenance, correlation, causation, and cumulative budgets;
- dependency cycles and overlapping mutable ownership are rejected before dispatch;
- only one fenced lease can commit an attempt and stale owners cannot publish effects;
- duplicate delivery and replay do not duplicate Tool side effects;
- timeout, cancellation, dead-letter, pause, resume, reassignment, interruption, and recovery are explicit;
- injected work and roster changes remain unapproved until normal authority accepts them;
- delegation, Skill, role, and provider selection cannot broaden authority;
- all fan-out artifacts and conflicts reach the deterministic reducer;
- reviewer pass and self-reported quality cannot create Verified or Completed state;
- bounded retry or revision exhaustion stops without fabricated completion;
- independent verification and durable RunRecord persistence remain mandatory.

## Prompt Book

### Codex Prompt

Implement Multi-Agent support only in its active Delivery Gate. Start with the sequential queue-mediated slice, preserve one Kernel state owner, and introduce parallelism only after durable delivery, leases, replay, recovery, authority intersection, and independent verification pass.

### Claude Prompt

Review ownership, snapshot drift, duplicate effects, authority expansion, reviewer/verifier confusion, shared-workspace conflicts, unbounded delegation, provider leakage, and recovery ambiguity.

### GPT Prompt

Explain the selected topology, capability roster, common snapshot, typed handoffs, cumulative budgets, control commands, failure policy, and why a simpler topology would or would not satisfy the approved task.
