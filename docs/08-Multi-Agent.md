# 08 - Multi Agent

## Status And Sequence

This chapter is accepted implementation guidance for a future capability. Multi-Agent execution remains **Planned** at Delivery Gate 13. Delivery Gate 6 Workspace and Project Brain Foundation is **Integrated**; Delivery Gate 7 Event Bus and IPC Foundation is the sole **Specified - Next** gate, with its in-process delivery contracts verified and backpressure next.

The prerequisite path is:

```text
Operational single-agent run and RunRecord
-> immutable WorkspaceSnapshot
-> typed Event/Message Bus
-> durable Agent Runtime and Scheduler
-> Model Gateway and Skill Engine
-> bounded Multi-Agent orchestration
```

## Goal

Multiple Agents collaborate through governed roles, typed work, durable artifacts, and explicit verification without uncontrolled parallel chatter or hidden authority changes.

The Kernel orchestration controller owns terminal task and run state. An Agent may own production or synthesis of an artifact, but no Agent owns the authority to approve new scope, broaden Tools, verify its own result, or mark the run completed.

## Pattern Selection

Choose the smallest topology that preserves quality and makes every handoff inspectable.

| Pattern | Use when | Additional requirement |
|---|---|---|
| single worker | one bounded context can complete the task | default for Gate 8 |
| sequential pipeline | each stage consumes the prior artifact | typed handoff after every stage |
| Producer-Reviewer | a produced artifact needs explicit workflow review | bounded pass/fix/redo loop; reviewer is not the independent verifier |
| bounded fan-out/fan-in | branches are independent and synthesis criteria are known | common snapshot, isolated ownership, deterministic reducer |
| expert routing | only a subset of capabilities should run | explicit, recorded routing decision and fallback |
| supervisor allocation | the backlog changes during execution | durable queue, reassignment policy, leases, and budgets |
| shallow hierarchy | a goal has stable subdomains with local coordination | at most one subordinate coordination layer |

Parallelism is not a quality feature by itself. Do not fan out work whose branches depend on each other's intermediate state, would write the same artifact, or lack a deterministic synthesis owner.

## Capability Roles And Roster

Candidate capabilities include Planner, Architect, Coder, Reviewer, Tester, Documenter, and Memory. These are schedulable capabilities, not fixed personalities or provider-specific prompts.

A future dynamic roster is derived from the intersection of:

- approved task requirements;
- installed and validated Agent or Skill metadata;
- current ExecutionPolicy and data-classification policy;
- model, context, cost, time, and concurrency budgets;
- available isolated workspaces and required verifier separation.

Discovery, installation, or role assignment never grants authority. The roster becomes an immutable run snapshot before dispatch. Adding a worker or capability during a run requires a typed control decision, a new roster revision, and renewed policy evaluation.

## Orchestration Profile

An `OrchestrationProfile` is a provider-neutral run plan. It records:

- selected topology and capability roster revision;
- approved task and immutable input-snapshot identities;
- dependency graph and artifact ownership;
- expected outputs and synthesis or reduction criteria;
- Tool, model, context, token, cost, time, retry, and concurrency budgets;
- isolation, conflict, recovery, and verification plan;
- explicit approval references for any controlled stage.

The profile cannot replace or broaden `ExecutionPolicy`. Provider adapters may translate an approved model class, reasoning budget, or locality requirement into provider options only after Kernel policy intersects task, Skill, Tool, and data authority.

## Queue And Handoff Contract

Every handoff uses the common versioned Message Bus envelope, not a second peer-to-peer protocol. Its typed work payload preserves at least:

- run, task, work-item, message, correlation, and causation identities;
- producer identity, assigned capability, and schema version;
- approved-task revision, authorization reference, and policy decision reference;
- common `WorkspaceSnapshot` identity and required dependency versions;
- expected output type, artifact ownership, deadline, and budget;
- artifact or evidence references plus content identity;
- retry count, structured failure code, and idempotency key.

Large payloads are stored behind integrity-checked artifact or evidence references. Markdown handoff files may be human-readable projections, but files, prompts, and Agent messages are not authoritative queues or control signals.

## Scheduler And Recovery Rules

The Scheduler and Message Bus own control mechanics:

- validate dependency references and reject cycles before dispatch;
- lease work to one owner for a bounded period and make lease expiry observable;
- suppress duplicate delivery effects through idempotency identities;
- distinguish retryable, terminal, cancelled, timed-out, dead-lettered, and blocked work using typed states;
- bound retries and revisions and preserve every attempt in the Execution Graph and RunRecord;
- support pause, resume, cancellation, reassignment, replay, and recovery from durable state;
- prevent a worker, Skill, model, or control client from expanding task or Tool authority;
- stop safely when ownership, isolation, recovery, or synthesis becomes ambiguous.

Fan-out branches consume the same immutable snapshot and must be read-only or isolated by an explicit workspace/branch adapter. Two workers cannot concurrently mutate the same owned path or artifact. A named reducer or synthesis worker consumes preserved branch outputs and reports conflicts rather than silently choosing a winner.

## Control And Telemetry

Future control surfaces issue authenticated, typed `RunControlCommand` values for pause, resume, cancel, reprioritize, reassign, mediation request, scale request, or injected-work proposal. Commands preserve actor, time, reason, run/task identity, authorization context, and resulting policy decision.

Injected work remains a Proposal until accepted through the normal task authority path. Reassignment and scaling cannot increase Tool, model, data, or external-action permissions.

`WorkerHeartbeat`, quality gradient, confidence, prompt adherence, current activity, and estimated progress are diagnostic observations. They can trigger inspection, cancellation, or a proposal, but never prove implementation, verification, completion, or release. A stale heartbeat is not itself evidence that a worker failed; the Scheduler must reconcile the lease, process, and durable work state.

## Review And Verification Boundary

Producer-Reviewer is a workflow pattern, not the completion authority.

- The producer writes an artifact and evidence references.
- The reviewer reads the original approved request, common input snapshot, artifact, and declared acceptance criteria.
- Review returns a typed pass, fix, or redo decision with bounded revision count.
- A reviewer cannot review its own output and cannot promote lifecycle state to Verified or Completed.
- The independent verifier evaluates the applicable evidence after workflow review.
- Only verified finalization plus durable RunRecord persistence may create `COMPLETED`.

Subjective scores may prioritize review but cannot substitute for objective criteria, Tool evidence, or independent verification.

## Gate Ownership

| Gate | Orchestration responsibility |
|---|---|
| 6 | immutable shared Workspace input and provenance |
| 7 | versioned envelopes, work/control payloads, idempotency, retry, replay, and transport adapters |
| 8 | durable dependency graph, leases, heartbeat ingestion, sequential worker, Scheduler, and recovery |
| 9 | provider-neutral model requirements, routing, redaction, and budgets |
| 10 | validated pattern metadata, workflow/Skill composition, and artifact schemas |
| 11 | Agent/plugin capability provenance and compatibility |
| 12 | authenticated run inspection and control surfaces |
| 13 | dynamic capability rosters, role pipelines, Producer-Reviewer, bounded fan-out/fan-in, supervisor allocation, and background runs |
| 15 | baseline-first autonomous experimentation with fixed evaluation and rollback |

## Reference Adoption

- [Archon at `263cf365`](https://github.com/martino-vigiani/Archon/tree/263cf3658a7cadefa0c5fbe82cc527a00ffb4c16) is a reference for dynamic capability rosters, centralized execution configuration, dependency-ready queues, heartbeats, control interventions, and resumable operational visibility.
- [meta-harness at `ccab9a6`](https://github.com/SaehwanPark/meta-harness/tree/ccab9a677878f72b3316de464c99b36f56a3f2e7) is a reference for topology selection, deterministic handoffs, Producer-Reviewer and supervisor patterns, normal/failure scenarios, and removable provider-specific logic.

Enhancer does not adopt their runtime packages, provider commands, prompt sets, Skill locations, generated `AGENTS.md` rules, `_workspace` authority, shared-worktree parallel writes, file polling as the core bus, optional verification, self-reported quality completion, unlimited timeouts, or silent evidence loss.

## First Multi-Agent Slice

The first Gate 13 slice remains a sequential queue-mediated handoff:

```text
Planner -> Queue -> Coder -> Queue -> Reviewer -> Queue -> Tester -> Memory
```

Each arrow is a durable Message Bus delivery. The slice uses one common snapshot, one work owner at a time, one terminal-state coordinator, bounded review, independent verification, and a replayable RunRecord. Parallel fan-out begins only in a later Gate 13 increment after this path survives interruption and replay without duplicate side effects.

## Tests

Cover at least:

- topology selection defaults to one worker and rejects unjustified fan-out;
- every handoff preserves task revision, snapshot, authority, correlation, causation, provenance, and budgets;
- duplicate delivery and replay do not duplicate side effects;
- dependency cycles, expired leases, stale heartbeats, timeout, cancellation, dead-letter, and recovery are explicit;
- injected work remains unapproved until the normal authority path accepts it;
- roster or provider selection cannot broaden Tool or data authority;
- parallel workers cannot share mutable ownership and conflicting outputs reach the reducer visibly;
- reviewer pass cannot create Verified or Completed state;
- bounded fix/redo exhaustion stops without fabricated completion;
- final completion still requires independent evidence verification and durable RunRecord persistence;
- interruption can resume from durable state without chat history;
- session handoff and Project Brain projections retain the final outcome and unresolved work.

## Prompt Book

### Codex Prompt

Implement Multi-Agent support only in its active Delivery Gate. Start with the sequential queue-mediated slice, preserve one Kernel state owner, and add parallelism only after durable delivery, leases, replay, recovery, and independent verification pass.

### Claude Prompt

Review the Multi-Agent model for ownership ambiguity, snapshot drift, duplicate effects, authority expansion, reviewer/verifier confusion, shared-workspace conflicts, unbounded revisions, and recovery gaps.

### GPT Prompt

Explain the selected orchestration topology, capability roster, handoff contract, budgets, control commands, failure policy, and why a simpler topology would or would not satisfy the task.
