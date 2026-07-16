# Architecture

## Status

Delivery Gate 0 foundation contracts are Integrated through an authority-preserving planning-to-approved-execution lifecycle test. The self-hosting context and planning path is verified against the current `.ai/` bootstrap set and canonical Roadmap grammar. Delivery Gates 1 through 4 integrate bounded read-only Tool execution, durable integrity-checked evidence, Tool-result-driven Agent Loop transitions, sequential independent verification, and replayable RunRecords. Delivery Gate 5 makes that deliberately narrow read-only vertical slice Operational through a supported local CLI. Delivery Gate 6 is Integrated, Delivery Gate 7 messaging foundations are Contract Verified, and Delivery Gate 8 Agent Runtime and Scheduler is the next specified product gate.

The accepted product direction is Self-hosting AI Development Operating System.

## Capability Maturity Model

Roadmap capability maturity is separate from the task lifecycle defined by the Constitution:

- **Specified:** the capability has an accepted responsibility, boundary, and exit criteria.
- **Contract Verified:** core types, invariants, and focused tests exist without claiming end-to-end behavior.
- **Integrated:** the capability is connected to its real upstream and downstream collaborators in an integration test.
- **Operational:** a supported entry point can execute the capability against a real project with observable evidence and documented recovery.
- **Released:** the operational capability is intentionally distributed with release evidence.

`Implemented` MUST NOT be used by itself for roadmap capability state because it hides the difference between a contract and an operational product. `PROJECT_STATE.md` records verified current maturity; `ROADMAP.md` records the next promotion gate.

## Target Architecture

Enhancer will evolve toward these major components:

- Kernel: constitution, authorization, lifecycle, budgets, and core operating policies.
- Workspace: governed snapshots of project files, active and selected context, Git state, diagnostics, and terminal-session metadata.
- Project Brain: combines repository memory, decisions, workspace observations, and run history without erasing source provenance.
- Memory: durable repository state plus explicit runtime memory records.
- Event Bus: typed domain events and subscriptions.
- Message Bus and IPC: envelopes, queues, delivery, replay, backpressure, and transport adapters.
- Agent Runtime: Goal, Planner, Executor, Memory, Reflection, Retry, and Done state machine.
- Scheduler: queues, resumes, cancels, and budgets foreground or background runs.
- Skill Engine: validates, progressively loads, and composes reusable workflows.
- MCP Server and Client: exposes and consumes governed Tools, resources, and memory through a standard protocol.
- Model Gateway and Router: provider-neutral model requests, routing, budgets, redaction, and adapters.
- Tool System: exposes file, terminal, search, Git, browser, and external capabilities behind policy.
- Plugin SDK and Marketplace: installs traceable, owned, versioned, integrity-checked extensions.
- Desktop, CLI, API, VSCode Extension, and Web Dashboard: user-facing control surfaces over shared application boundaries.
- Cloud Sync: optional governed synchronization with encryption, conflict, ownership, and secret-exclusion rules.

## Operating System Model

Enhancer is modeled as an AI Development Operating System:

The original linear chain below is retained as early conceptual history. It is superseded by the event-driven topology that follows it.

```text
Kernel
↓
Scheduler
↓
Planner
↓
Memory
↓
Tool
↓
Skill
↓
Agent
↓
Plugin
↓
LLM
```

Cursor-like behavior is treated as an application-level capability on top of Enhancer, not the identity of Enhancer itself.

Refined target topology:

```text
Desktop | CLI | API | VSCode | Web
                  |
          Workspace + Project Brain
                  |
      Agent Runtime + Scheduler + Memory
                  |
      Event API -> Message Bus -> IPC adapters
            |          |          |
       Skill Engine  MCP Core  Plugin Runtime
            |          |          |
        Tool System + Model Gateway/Router
                  |
       Repository / Providers / Cloud Sync
```

## Event And Message Architecture

Enhancer uses one messaging model with three responsibilities:

- **Event Bus:** semantic domain event types and subscriptions, such as `GitPushObserved`, `PlanRequested`, `CodeChangeProduced`, `ReviewRequested`, `TestCompleted`, and `MergeApprovalRequested`.
- **Message Bus:** versioned envelopes, topic or queue addressing, delivery state, idempotency, correlation, causation, retry, dead-letter, replay, and backpressure.
- **IPC transport:** in-process, local process, or later remote transport for the same envelope contract.

The first implementation MUST be deterministic and in-process. Durable queues and IPC are later adapters. Agent Runtime components publish and consume messages; they do not gain authority from an event and do not directly call the next role. Every envelope preserves provenance, authorization context, run identity, schema version, and bounded payload or evidence reference.

## Workspace And Project Brain

Workspace is the current observable development environment. Its snapshots may include repository files, active and selected files, Git status and diff, diagnostics, terminal-session metadata, project configuration, and later editor state. Each source has an explicit adapter and permission boundary.

Project Brain is the reasoning-facing aggregate of canonical repository memory, Workspace snapshots, accepted decisions, RunRecords, and indexed knowledge. It preserves source identity and freshness; it MUST NOT turn transient editor state or external output into authority.

### Gate 6 Workspace Snapshot Contract

The first Gate 6 increment is a metadata-only immutable snapshot under `com.enhancer.workspace`. `ApprovedTaskRevision` records task identity, source-document identity, and the SHA-256 revision of the approved source. `WorkspaceSourceObservation` records a typed source, stable source identity, adapter provenance, observation time, optional source-update time, explicit Available/Stale/Unavailable state, optional content digest, and bounded reason metadata.

`WorkspaceSnapshot` normalizes the absolute project root, sorts observations canonically, rejects duplicate kind/identity pairs and more than 4096 observations, and computes its own SHA-256 identity over every identity-bearing metadata field. Caller order cannot change the identity. Source payloads, Tool scope, policy, approval creation, and command authority are absent by construction.

This metadata contract is Integrated: the real Context Reader feeds it through the repository-memory collector and the view, producer, and query consume it. Gate 7 message envelopes later carry the same snapshot identity across handoffs.

### Gate 6 Project Brain View

The second Gate 6 increment is the read-only `ProjectBrainView` aggregate under `com.enhancer.brain`. It composes exactly one `WorkspaceSnapshot`, one `ProjectContext` repository memory, and one `RunRecord`, and derives everything it exposes from those inputs. It performs no collection of its own.

The view is keyed to the snapshot's canonical identity rather than computing a second one. Repository memory is projected to `RepositoryMemoryEntry` metadata of document path, read order, and a computed lowercase SHA-256 of the document content; no document content is retained. Each entry carries an explicit `MemoryFreshness` derived by comparing that digest against the snapshot's `REPOSITORY_DOCUMENT` observation with the same source identity: `SNAPSHOT_MATCHED` for equal digests, `SNAPSHOT_DIVERGED` when the snapshot observed a different or unconfirmed revision, and `NOT_OBSERVED` when the snapshot never observed the document. `RunProvenance` projects the RunRecord to logical run identity, record time, approved task identity, and verification status only; Tool requests, results, evidence bodies, and chat history are absent by construction.

The view requires the RunRecord's approved task identity and source document to equal the snapshot's `ApprovedTaskRevision` and rejects an unrelated run rather than aggregating misattributed provenance. Workspace Available, Stale, and Unavailable states pass through unchanged.

This aggregate is Integrated: it is composed from really-collected snapshots, really-loaded memory, and really-persisted RunRecords by the integration path below and on the production CLI run path.

### Gate 6 Repository Memory Collection

The third Gate 6 increment is the read-only `RepositoryMemorySnapshotCollector`, the first Workspace source adapter. It derives a real `WorkspaceSnapshot` from repository memory that the Context Reader already loaded: one `AVAILABLE` `REPOSITORY_DOCUMENT` observation per document with `context-reader` provenance and a computed content digest, plus an `ApprovedTaskRevision` digested from the approved task's source document in the same memory. It reads no files itself, reuses `WorkspaceSnapshot.capture` for identity and bounds, takes its capture time as an explicit parameter, and rejects memory that lacks the approved task source document.

The repository-memory path is Integrated: an integration test connects a real governed CLI run, the real persisted RunRecord, the real Context Reader, the collector, and `ProjectBrainView.compose`, including an explicit `SNAPSHOT_DIVERGED` observation after the source document changes. Because the collector observes only loaded memory, `STALE` and `UNAVAILABLE` observations first appear with real per-source adapters.

### Gate 6 Production Composition

The `EnhancerCli` `run` path composes the view in production. The CLI keeps the `ProjectContext` it already loads for task approval, collects the snapshot with a capture time taken before worker execution, composes the view after finalization with the persisted RunRecord for every outcome that produces a record, and appends `workspaceSnapshotId`, `workspaceObservations`, and a `memoryFreshness` summary to the bounded run output. No content, digest list, or evidence is printed; no command, argument, exit code, or authority was added. The RunRecord does not store the snapshot identity; carrying that identity across handoffs belongs to the Gate 7 envelope contracts.

This makes the repository-memory composition Operational for the governed read-only CLI scenario. Delivery Gate 6 is Integrated by the 2026-07-15 re-scope-and-promotion decision: diagnostics, terminal-session, and active/selected-file observation moved to Gate 12, which owns those capabilities. Gate 7 Event Bus and IPC Foundation is now Contract Verified, and Gate 8 Agent Runtime and Scheduler is the sole `Specified - Next` product gate.

### Gate 6 Graph Projection Contract

The graph projection contract under `com.enhancer.brain` types the Project Brain graph model without producing, persisting, or querying graphs. `GraphNode` carries a bounded identity and one of five kinds (task, decision, component, artifact, execution). `GraphEdge` carries one of six endpoint-checked kinds covering the five roadmap relationship domains: `JUSTIFIED_BY` task-to-decision, `SUPERSEDES` decision-to-decision, `DEPENDS_ON` between components and artifacts, `MODIFIES` task-to-artifact, `VERIFIED_BY` artifact-to-artifact, and `RECORDED_AS` task-to-execution. Each edge kind declares its valid endpoints in the type, so a later impact query traverses meaning rather than convention.

Every element carries `GraphProvenance`: a bounded source reference, an optional lowercase SHA-256 source revision, and explicit `CURRENT`/`STALE`/`SOURCE_MISSING` freshness with derived rebuild-required status; Current and Stale require a revision and Source-Missing prohibits one. `ProjectBrainGraph.project` keys the projection to one valid Workspace snapshot identity with an explicit projection time and the `project-brain-graph-v1` version, orders nodes and edges deterministically, and rejects duplicates, self-loops, unknown endpoints, endpoint-kind violations, and more than 4096 elements per collection.

This contract is Integrated: real producers populate it exclusively and the impact query consumes it. Modifies, verified-by, justified-by, supersedes, and depends-on producers remain deferred to their own evidence sources.

### Gate 6 Task Impact Query

`TaskImpactQuery` answers the first rebuildable task-to-decision-to-code-to-test question over exactly one projected graph. From the queried task node it traverses only the named chain — `JUSTIFIED_BY` to decisions, `MODIFIES` to artifacts, `VERIFIED_BY` from those modified artifacts to their verifying artifacts, and `RECORDED_AS` to executions — and returns an immutable `TaskImpact` carrying the graph's source snapshot identity and one derived rebuild-required status. The status is true exactly when the task node, a traversed edge, or a returned node requires rebuild, so the answer says when it stops being trustworthy; unrelated stale elements do not taint it. Transitive `DEPENDS_ON` closure is deliberately deferred until real dependency projections exist.

The query is Integrated: it answers over really-produced graphs through the producer below, naming the real stored execution, on both the integration path and the production CLI run path.

### Gate 6 Run Evidence Graph Producer

`RunEvidenceGraphProducer` is the first graph producer. From one Workspace snapshot and one task-matched stored run record it projects only what that evidence proves: a task node from the approved task revision, one artifact node per repository document/file observation with the observation state mapped one-to-one to element freshness (Available to Current, Stale to Stale, Unavailable to Source-Missing), an execution node carrying the stored envelope SHA-256 and durable reference, and a single `RECORDED_AS` edge. It never emits decision, modifies, verified-by, justified-by, supersedes, or depends-on elements, because no current evidence source justifies them; each of those arrives with its own producer and decision.

The run-evidence production path is Integrated: the end-to-end integration test flows a real governed CLI run and a really-collected snapshot through the producer into a `TaskImpactQuery` answer naming the real stored execution.

### Gate 6 Decision Projection And Run Record Observation

`AcceptedDecisionProjector` parses accepted decisions from the decision log's own `Status: Accepted Decision` lines in already-loaded repository memory into `DECISION` nodes. Freshness is snapshot-relative: a matching observed digest is `CURRENT`; a diverged or unobserved document is `STALE`, because currency cannot be proven without a matching observation.

`TaskJustificationProjector` links tasks to decisions only through the optional `## Justified By` section of the active task document, whose bullets must name accepted-decision headings exactly. Resolved references become `JUSTIFIED_BY` edges with task-document provenance and snapshot-relative freshness; unresolved, duplicate, empty, or non-bullet references are rejected rather than skipped, and an absent section honestly claims no justification.

`RunRecordMetadataCollector` observes at most the 256 most recent stored records through `recentReferences(limit)`: the filesystem store performs one directory scan and bounded newest-selection over no-follow modification metadata, then only the selected payloads are resolved. Each becomes one `RUN_RECORD` observation with `run-record-store` provenance, the envelope SHA-256 as content digest, and the stored time as source-update time; a selected record that fails integrity resolution becomes an explicit `UNAVAILABLE` observation. Complete lexicographic `references()` and point replay remain available, no artifact is deleted, and a future durable index may replace the residual linear directory scan.

### Gate 6 Target File And Git Observation

`TargetFileMetadataCollector` observes the governed run's target file pre-run as a `REPOSITORY_FILE` observation with a streamed containment-checked SHA-256 and `target-file-reader` provenance; missing or over-64-MiB targets are explicit `UNAVAILABLE` observations, while absolute, traversal, escaping, or non-regular targets are configuration errors surfaced before execution.

`GitWorkspaceCollector` holds the only external command authority in the codebase, explicitly granted and scoped by accepted decision. It resolves Git only from absolute PATH entries, canonicalizes the candidate, rejects executables inside the real observed project root, and otherwise emits `UNAVAILABLE` rather than invoking a name. Its sole enabled command is fixed filter-free index metadata (`git ls-files --stage --deleted --others --exclude-standard`) with no shell or inherited `GIT_*` overrides; tracked worktree diff is explicitly `UNAVAILABLE` because adversarial verification proved that status, `ls-files --modified`, and `diff-files --raw` can all execute required clean filters. The command uses `--no-optional-locks`, an invocation-scoped fsmonitor disable, discarded stderr, a watchdog-enforced five-second timeout, a four-MiB output cap, and repository discovery confined to the project root via a collector-owned `GIT_CEILING_DIRECTORIES`. Only its SHA-256 output digest is retained as `GIT_STATUS` metadata. The authority-boundary exit criterion is pinned by `WorkspaceAuthorityBoundaryIntegrationTest`: adversarial tool-grant text in observed documents cannot widen task or policy scope, appear in bounded output, or mutate any document.

### Gate 6 Production Graph Composition

The CLI `run` path composes the graph in production: the RunRecord store is constructed before collection so prior records are observed into the snapshot, accepted-decision nodes and resolved `Justified By` edges from the same loaded memory are merged into the run-evidence graph through additional-observation, additional-node, and additional-edge overloads, and the task impact query is answered in process. The output reports bounded `graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`, and `impactDecisions` counts only. Snapshot identity intentionally reflects prior run-record observations, so identical trees with different run histories produce different snapshot identities.

Graph metadata that is available from the snapshot and repository memory is projected and structurally preflighted before evidence creation or Tool execution. Repository-document and target-file observations sharing one path collapse to one artifact node with the target-specific observation preferred, and graph node identities share the Workspace 1024-character source bound. After the finalized RunRecord is persisted, Project Brain view/graph/query composition is optional diagnostics: a runtime failure emits bounded `brainStatus=UNAVAILABLE` metadata and cannot replace the durable record-derived exit code with an internal error.

This makes the production graph composition Operational for the governed read-only CLI scenario. Impact answers carry executions and explicitly justified decisions; modifies and verified-by evidence does not exist yet.

## Gate 7 Message Envelope Contract

The first Gate 7 increment is the reference-only envelope contract under `com.enhancer.bus`. `MessageEnvelope` is versioned (`message-envelope-v1`) and carries a canonical-UUID message identity, a bounded correlation identity, an optional canonical-UUID causation identity that must differ from the message identity, bounded logical-run and producer identities, an occurrence time, and one typed payload.

`MessagePayload` is sealed to exactly four kinds. The work payload carries the approved task revision, a valid Workspace snapshot identity, and an immutable allowed-tool scope of 1 through 256 unique names, each bounded to 256 characters; the result payload carries the task identity, a run-record reference, and the verification status; the control payload carries a typed cancel/pause/resume signal with a bounded reason; the handoff payload carries the task revision, snapshot identity, and run-record reference. Authorization is carried as data, never created: possessing an envelope grants nothing, and delivery code must validate contents against repository authority rather than trust the sender.

The explicit allowed-tool cardinality ceiling closes the payload-bound gap found by the Gate 7 maturity assessment. Together with the existing per-name ceiling, the only collection-bearing payload has a finite aggregate tool-name ceiling of 65,536 characters, so the Roadmap exit criterion that payloads are bounded or replaced by evidence references is satisfied at Contract Verified maturity. The fresh follow-up assessment promoted Gate 7 without selecting or requiring a concrete IPC adapter.

This contract is Contract Verified and consumed by both the deterministic in-process topic and queue delivery surface and the transport-neutral IPC boundary below. Together with those delivery contracts, it completes the Contract Verified Gate 7 foundation; production integration remains deferred.

## Gate 7 In-Process Delivery

The second Gate 7 increment is `InProcessMessageBus` under `com.enhancer.bus`: a synchronous, single-threaded, deterministic delivery surface over `MessageEnvelope`. A `DeliveryDestination` is a typed `DeliveryDestinationKind` (`TOPIC` or `QUEUE`) plus a bounded name; a topic publication fans out to every subscriber in registration order, and a queue publication is delivered point-to-point to a single consumer, rejecting a second consumer. Each publication returns an immutable ordered list of per-subscriber `DeliveryOutcome`s carrying a `DeliveryStatus` of `DELIVERED`, `DUPLICATE`, or `UNROUTED`.

Delivery is idempotent per `(destination, subscriber, message identity)`: re-publishing the same envelope invokes the handler at most once and reports `DUPLICATE`. Every publication is appended to an ordered immutable journal of `JournaledMessage` entries, and `replay` re-dispatches a journal deterministically without appending to it, reproducing the original outcomes on a fresh bus and producing only `DUPLICATE` with no duplicate side effect when replayed against a bus that already processed them. The bus carries whole envelopes without mutation, so authorization and provenance survive every hop; it never creates authority.

The bus also isolates delivery failures under a bounded retry policy: the bus is constructed with an immutable `RetryPolicy` (1 through 10 attempts; the no-argument constructor keeps a single attempt), and a handler `RuntimeException` is retried immediately and synchronously, with no delay between attempts, until it succeeds or the policy is exhausted. Success within the policy is an ordinary `DELIVERED`. Exhaustion records a `FAILED` `DeliveryOutcome` for that subscriber, captures an immutable `DeadLetter` (destination, subscriber, unmodified envelope, a bounded reason derived from the last failure, and the failed attempt count) into an ordered `deadLetters()` record, and continues delivering to the remaining subscribers. A failed delivery consumes the idempotency key, so re-publishing or replaying it reports `DUPLICATE` and adds no further dead letter.

The dead-letter record is the sole re-delivery authority: `redeliver(DeadLetter)` accepts only a dead letter the bus currently records, re-invokes the subscription's handler under the same bounded policy, resolves the entry on success (`DELIVERED`, entry removed), and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason (`FAILED`). Re-delivery never appends to the journal and never releases the consumed idempotency key.

Cancellation is scoped to the envelope's own `correlationId`, the identity the envelope contract already defines for grouping related messages across hops. `cancel(correlationId)` is idempotent and monotonic — there is no resume — and `isCancelled` reports it. Cancellation is admission control that runs before subscription lookup, idempotency, and dispatch, so a refusal dominates both `UNROUTED` and `DUPLICATE`: the delivery reports a scope-level `CANCELLED` outcome naming no subscription, invokes no handler, consumes no idempotency key, creates no dead letter, and is not journaled. Journaling a refused publication would make a fresh-bus replay produce a side effect that never originally happened, so the journal records exactly the publications that were admitted. The refusal propagates to every path: `replay` skips a cancelled entry while live correlations still deliver, and `redeliver` refuses a cancelled dead letter while retaining its record.

Because the bus never reads a payload to decide delivery, `ControlSignal.CANCEL` remains a consumer semantic: a handler that receives a `CANCEL` `ControlPayload` may call `cancel` itself. `PAUSE` and `RESUME` likewise have no bus behavior.

Ordering is established by running each publication to completion. The bus holds a pending queue and a single drain loop: a top-level `publish` or `replay` drains the queue to exhaustion and returns the whole ordered cascade, while a publication made from inside a handler only enqueues and reports the scope-level `ENQUEUED` status. Without this, synchronous dispatch nests — a handler that publishes during its own delivery has its child delivered in full before the parent's fan-out finishes, so every subscriber registered after the publishing one observes the effect before its cause. Draining from a queue also removes unbounded stack growth from a deep cascade.

Admission — the cancellation check and the journal append — happens inside the drain loop rather than at the publishing call. That keeps the journal's order equal to the bus's own total delivery order, preserves the invariant that the journal records exactly what was admitted, and lets a correlation cancelled mid-cascade refuse entries still queued behind it. A fan-out itself stays atomic: a cancellation raised during one cannot stop it. Every `publish`, including an already-cancelled re-entrant publication, enters this queue and admission path. `publish` and `replay` share submission and draining; a handler publication caused by a replayed entry inherits that entry's non-journaling mode, so replay never grows the live journal through a cascade. An `Error` escaping a drain abandons the cascade entirely rather than leaking queued entries into a later publication.

This delivery, its failure handling, its bounded retry and explicit re-delivery, its cancellation propagation, and its run-to-completion ordering are Contract Verified. The transport-neutral IPC interface below carries the same destination and envelope without changing these semantics. Backoff or delayed retry, priority ordering, competing queue consumers, threading, persistence, and concrete transport adapters remain later increments over this surface.

### Gate 7 Pending-Queue Backpressure

The run-to-completion pending queue is bounded by immutable `BackpressurePolicy` with a capacity from 1 through 4096 and a finite default. Because a re-entrant publisher is executing inside the single-threaded drain, the bus never blocks it: capacity exhaustion reports the scope-level `BACKPRESSURED` status immediately. Refused work is not admitted, journaled, dispatched, deduplicated, dead-lettered, or cancelled and may be explicitly retried later. Accepted work remains FIFO. Replay accepts the deterministic prefix that fits the configured capacity and reports the refused suffix while retaining replay's non-journaling behavior. This contract is Contract Verified. The policy bounds pending publications only; retention bounds, threading, persistence, scheduling, and IPC remain separate concerns.

### Gate 7 Transport-Neutral IPC Boundary

`TransportMessage` carries exactly one existing `DeliveryDestination` and one existing `MessageEnvelope` without copying or reinterpreting either. Provider-neutral `MessageTransport.send` accepts that immutable route and envelope and returns a `TransportOutcome` whose `TransportStatus` is `ACCEPTED`, `BACKPRESSURED`, or `UNAVAILABLE`. Accepted outcomes carry no reason; non-acceptance carries a bounded diagnostic reason.

Transport acceptance is deliberately not Message Bus delivery. `ACCEPTED` means only that the configured adapter accepted responsibility for attempting one hop; it does not mean a receiving bus admitted, journaled, dispatched, or delivered the envelope. A transport refusal consumes no bus journal, idempotency, cancellation, failure, or dead-letter state, and higher-level scheduling owns any retry timing. The interface contains no provider endpoint, serialization, protocol, authentication, lifecycle, threading, persistence, or authority type. This boundary is Contract Verified; no concrete adapter exists and no process boundary has been crossed.

## Agent Runtime Model

The target runtime is a persisted, event-driven state machine:

```text
Goal -> Planner -> Queue -> Executor -> Evidence
     -> Memory -> Reflection -> Retry | Verification -> Done
```

Planner, Coder, Reviewer, Tester, and Memory are roles or workers behind message contracts, not hard-coded direct-call chains. Single-agent sequential execution is implemented first. Multi-agent concurrency begins only after queue, idempotency, cancellation, RunRecord, recovery, and independent verification are operational.

## Agent Orchestration Contract

Enhancer escalates orchestration only when the simpler topology cannot satisfy the approved work: one worker, sequential pipeline, Producer-Reviewer, bounded fan-out/fan-in, expert routing or supervisor allocation, and finally a hierarchy with at most one subordinate coordination layer. A role is a capability assignment, not a fixed personality, provider, prompt, or process.

Every orchestration topology preserves these invariants:

- one Kernel-owned coordinator is the sole writer of terminal task and run state;
- every dispatched worker receives the same immutable `WorkspaceSnapshot` identity and approved task revision unless a later explicitly recorded event creates a new revision;
- branch ownership, expected artifacts, synthesis criteria, conflict policy, and time, cost, context, and Tool budgets are fixed before parallel dispatch;
- every handoff uses a versioned envelope carrying run, task, message, correlation, causation, producer, schema, authorization, input-snapshot, and artifact or evidence-reference identity;
- the Scheduler, not a prompt, owns dependency readiness, cycle rejection, work leases, duplicate suppression, retry, timeout, cancellation, pause, resume, reassignment, dead-letter, replay, and recovery;
- typed control commands may pause, resume, cancel, reprioritize, reassign, request mediation, or propose injected work, but they cannot approve new scope or broaden external-action authority;
- `WorkerHeartbeat`, quality, confidence, and prompt-adherence observations are diagnostic telemetry only and cannot prove progress, verification, completion, or release;
- a producing worker or reviewer may request bounded revision, but only the independent verification and durable RunRecord boundary may promote a run to `COMPLETED`;
- provider-specific CLI flags, prompts, retries, and recovery heuristics remain removable adapter details behind the provider-neutral Model Gateway and policy boundary.

The dependency-ordered landing points are:

| Contract or pattern | Owning gate |
|---|---|
| immutable common input snapshot and provenance | Gate 6 |
| typed handoff, control/event envelope, idempotency, replay, and transport-neutral delivery | Gate 7 |
| dependency graph, leases, heartbeat ingestion, sequential worker, Scheduler, and recovery | Gate 8 |
| provider-neutral execution profile, routing, and model/context/cost budgets | Gate 9 |
| validated workflow-pattern selection, Skill composition, and durable artifact schemas | Gate 10 |
| authenticated user-facing run controls | Gate 12 |
| dynamic capability roster, Producer-Reviewer, bounded fan-out/fan-in, supervisor allocation, and background work | Gate 13 |
| baseline-first autonomous experiment ledger with fixed evaluation and rollback | Gate 15 |

Archon commit `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16` and meta-harness commit `ccab9a677878f72b3316de464c99b36f56a3f2e7` are pinned design references for this contract. Their packages, provider commands, Skill layouts, prompts, shared-worktree assumptions, quality scores, and file-based queues are not Enhancer runtime dependencies or sources of authority.

## MCP, Skill, And Model Boundaries

MCP is a core interoperability layer, not a late plugin detail. The MCP Server exposes approved Enhancer Tools, resources, Workspace views, and memory; the MCP Client consumes external servers through the same policy, evidence, and verification pipeline.

Skills are validated workflow packages whose metadata loads before full instructions. Skills may compose into explicit chains, but composition intersects rather than unions Tool permissions. The Model Gateway remains provider-neutral and routes bounded requests without allowing model output to grant authority.

## Product Evolution: V1 To V3

- **V1 - AI Development Experience:** Cursor-level productivity through CLI, editor, Desktop, and API surfaces backed by Workspace awareness. Enhancer remains a shared engine below those interfaces, not an IDE identity.
- **V2 - AI Development Platform:** Agent Runtime, Event/Message Bus, Workflow Engine, Skills, Memory, MCP, Model Gateway, plugins, marketplace foundations, and self-hosting development workflows.
- **V3 - AI Operating System:** AI Kernel, Project Brain knowledge graphs, multi-agent scheduling, privacy-aware hybrid model routing, full plugin ecosystem, governed Cloud Sync, and self-improvement safeguards.

These milestones describe product outcomes. Internal dependency gates may implement Kernel or platform foundations before a polished V1 interface is released.

Delivery Gates, not V1-V3 labels, define implementation order and capability promotion. A V2 platform foundation may be required internally before all V1 control surfaces are polished; this does not make V2 Operational or V1 Released.

## Self-Hosting And Model-Hosting Terminology

**Self-hosting development** means Enhancer uses its own governed repository context, planning, execution, evidence, verification, and recovery workflow to improve Enhancer. **Local model hosting** means running an approved model on the user's infrastructure, while **hybrid model execution** routes work across approved local and remote providers. These are separate dimensions: local inference alone is not self-hosting, and self-hosting remains provider-neutral.

## AI Kernel Responsibilities

The Kernel is the authority-preserving control plane below every interface. It owns:

- Agent and workflow lifecycle;
- memory, context, and resource-budget allocation;
- locks, leases, idempotency, and concurrency coordination;
- scheduling, queueing, cancellation, timeout, pause, resume, and recovery;
- policy, approvals, secrets boundaries, and data classification;
- event routing and durable run/audit identity;
- verification gates and terminal state transitions.

The Kernel does not implement every Agent or framework. Java, Python, Spring, Vue, React, Android, AWS, Security, and similar capabilities enter as governed Agent plugins, Skills, Tools, or combinations of them.

## Project Brain Graph Model

Git and canonical repository documents remain authoritative durable memory. Project Brain adds rebuildable graph projections with source, timestamp, version, and confidence metadata:

- **Decision Graph:** proposals, accepted decisions, supersession, constraints, and affected artifacts;
- **Architecture Graph:** systems, modules, components, ownership, interfaces, and dependency direction;
- **Dependency Graph:** file, symbol, package, module, build, service, data, and deployment dependencies;
- **Task Graph:** user intent, goals, plans, subtasks, blockers, approvals, issues, and delivery gates;
- **Execution Graph:** events, Agents, Skills, Tools, models, evidence, verification, commits, PRs, tests, bugs, and outcomes.

Graph edges enable questions such as which decision justifies a change, which modules and tests are affected, and which execution introduced a regression. A graph index cannot silently overwrite its source. Stale or missing projections are explicit and rebuildable from repository and RunRecord evidence.

## Agent, Skill, Tool, And Workflow Separation

- **Agent plugin:** a schedulable role or capability worker, such as Architect, Spring, Oracle, AWS, Security, Reviewer, or Tester.
- **Skill:** a validated, progressively loaded workflow recipe such as creating a Spring REST API with controller, DTO, entity, repository, service, tests, and API documentation.
- **Tool:** a policy-governed external capability such as reading files, running tests, operating Git, or invoking an API.
- **Workflow:** an event-driven state machine that composes Agents, Skills, Tools, memory, verification, rollback, and approval gates.

Marketplace installation never implies execution approval. Installed Agents and Skills declare capabilities and permissions; the active task and Kernel policy select the allowed subset.

## Intent, Workflow, And Git Boundary

The target user interaction may be one sentence such as "Implement login." Enhancer Shell compiles it through Intent Understanding into an inspectable Goal, dependency analysis, Project Brain queries, plan, resource budget, Agent schedule, execution graph, verification plan, and RunRecord.

Workflows may represent Issue -> Branch -> Develop -> Test -> Review -> Commit -> Push -> PR -> Merge. Local reversible stages can run within approved scope. Commit, push, PR creation, merge, deployment, and other external or destructive stages require explicit approval or an equally explicit pre-authorized policy. The user need not micromanage prompts or models, but the Kernel must preserve control and auditability.

## Privacy-Aware Model Routing

The Model Router selects providers using required capability, data classification, repository policy, locality, cost, latency, context capacity, availability, and past evidence. Sensitive code defaults to an approved local model route; remote transmission requires policy authority. Planner, coding, review, debugging, and architecture roles may use different local or remote models without changing Agent, Skill, Tool, or verification contracts.

## Specification Architecture

`docs/` contains Codex-ready architecture and implementation guides. These documents are part of the product operating system, not secondary notes.

- `docs/00-Project-Overview.md`: project identity and scope
- `docs/01-Development-Environment.md`: environment checks and bootstrap target
- `docs/02-Agent-Loop.md`: Agent Loop design
- `docs/03-Tool-System.md`: Tool System design
- `docs/04-Skill-System.md`: Skill System design
- `docs/05-Memory.md`: repository-backed memory design
- `docs/06-Planner.md`: task planning design
- `docs/07-MCP.md`: MCP integration direction
- `docs/08-Multi-Agent.md`: multi-agent collaboration model
- `docs/09-Background-Agent.md`: background agent safety model
- `docs/10-Roadmap.md`: 30-day self-hosting plan
- `docs/11-Architecture.md`: expanded architecture guide

## RFC Architecture

Major design areas are tracked in `docs/rfcs/`.

- `RFC-0001`: Constitution
- `RFC-0002`: AI Behavior Specification
- `RFC-0003`: Prompt Contract
- `RFC-0004`: Context Builder
- `RFC-0005`: Planner
- `RFC-0006`: Tool Specification
- `RFC-0007`: Skill Specification
- `RFC-0008`: Memory Specification
- `RFC-0009`: Multi Agent
- `RFC-0010`: AI Operating System
- `RFC-0011`: Plugin SDK
- `RFC-0012`: Self Improvement

## First Architecture Slice

The first product slice should be a Repository Context Reader.

Expected responsibility:

- Read the required project documents in constitution order.
- Preserve source order and document identity.
- Report missing required documents clearly.
- Produce a structured context object that later components can use.

Implemented package: `com.enhancer.context`

The slice uses immutable context records, an enum as the canonical required-document order, and a filesystem reader. The executable startup context reads the seven governed `.ai/` documents first in a stable order, followed by the eight canonical root documents. It has no Spring wiring because the current behavior does not require an application container.

## Planner Slice

The first Planner slice is implemented in `com.enhancer.planner` and consumes `ProjectContext` directly.

It is deterministic: an active `CURRENT_TASK.md` blocks a new proposal; otherwise the first `## Delivery Gate ...` section whose status is `Specified - Next` becomes one structured proposal. Required-capability or scope bullets become proposal scope, and exit-criteria bullets become acceptance criteria. Proposal state is explicit and remains separate from accepted decisions and implementation state. The slice does not call an LLM, mutate documents, rank alternatives, or execute work.

## Assisted Development Loop Slice

The first Assisted Development Loop slice is implemented in `com.enhancer.loop`. It composes `ProjectContextReader` and `RepositoryTaskPlanner` in one deterministic pass. Its result has an explicit terminal outcome: either `PROPOSAL_AVAILABLE` or `ACTIVE_TASK_PRESERVED`.

This slice reads repository state but does not mutate it. It does not repeat work, build prompts, execute tools, call an LLM, approve a proposal, or perform Git operations. Maximum-iteration and stagnation termination are implemented separately in the repeated Agent Loop slice below.

## Repeated Agent Loop Termination Slice

The repeated Agent Loop termination slice is implemented under `com.enhancer.loop`. A caller-supplied step produces the next immutable state; the loop owns only termination safety and iteration accounting.

The accepted stop reasons are `COMPLETED`, `AWAITING_VERIFICATION`, `FAILED`, `MAX_ITERATIONS`, and `STAGNATED`. Defaults are 20 maximum iterations and 3 consecutive unchanged progress keys. Terminal task status is evaluated first, followed by the maximum-iteration ceiling and then stagnation. A terminal step is not misclassified as stalled, and `MAX_ITERATIONS` wins when the ceiling and stagnation threshold coincide. `AWAITING_VERIFICATION` is the Gate 3 success boundary: it explicitly does not mean task completion.

An independent verifier will be introduced later as a sequential boundary after the single-agent loop is stable. It must not imply parallel multi-agent execution or allow a worker to verify its own result.

## Tool System Slices

The first Tool System slice is implemented under `com.enhancer.tool` as provider-neutral result and evidence records.

Every `ToolResult` carries a tool name, explicit success or failure status, an optional process exit code, and required `VerificationEvidence`. Evidence keeps a non-blank summary of at most 512 characters and the final 4096 characters of output. When output is truncated, the caller must supply a non-blank reference to complete output; Gate 2 now makes that reference durable and integrity-checkable.

Tool status and an available exit code must agree: success requires exit code zero, while failure cannot carry exit code zero. Tools without process exit codes may leave it absent. This contract bounds Agent Context growth while retaining the most recent diagnostic output and a route to full evidence.

Delivery Gate 1 adds the integrated `ToolRequest` -> `ExecutionPolicy` -> `ToolExecutor` -> `ReadFileTool` -> `ToolResult` path described below. Gate 2 extends it through `EvidenceRecorder` and `EvidenceStore`; Gate 3 connects the result to Agent Loop state.

## Executable Agent Vertical Slice

The next architecture objective is an executable vertical slice, not another isolated contract. It promotes the existing contracts through the following connected flow:

```text
CLI or test harness
→ Repository Context
→ Plan or approved task
→ Agent Run Controller
→ Tool Request and Execution Policy
→ Concrete Tool
→ Tool Result and Evidence Store
→ Sequential Independent Verifier
→ Loop State and Stop Reason
→ Durable Run Record
```

The slice is introduced in bounded increments:

1. **Tool execution boundary - Integrated:** define `ToolRequest`, `Tool`, `ExecutionPolicy`, and `ToolExecutor`; implement one read-only filesystem Tool and deterministic test doubles.
2. **Evidence persistence - Integrated:** store complete output behind `VerificationEvidence.fullOutputReference` and verify reference existence and integrity.
3. **Loop integration - Integrated:** make one Agent Loop iteration consume a Tool request and produce a `ToolResult`-backed state transition.
4. **Sequential verification - Integrated:** evaluate the result outside the worker step and prevent worker claims from self-verifying.
5. **Run record - Integrated:** persist request, decision, result, evidence, verification, and stop reason for replay and diagnosis.
6. **Runnable entry point - Operational:** expose the integrated path through the supported `EnhancerCli` `run` and `replay` commands.

The first operational scenario remains deliberately small: read a temporary repository file through an allowlisted Tool, retain bounded evidence, independently verify the expected result, stop explicitly, and persist a run record. Shell mutation, LLM calls, commits, pushes, and multi-agent routing remain outside that first scenario.

No new foundation contract SHOULD be added unless it has an identified integration consumer in the current or immediately following delivery gate.

### Delivery Gate 0 Integration Boundary

Gate 0 integration is evidence over existing runtime layers. Its Context Reader, Planner, Assisted Development Loop, repeated Agent Loop, ToolResult, VerificationEvidence, and governance contracts have downstream consumers across Gates 1 through 5. `FoundationLifecycleIntegrationTest` makes those relationships observable in one governed temporary-repository lifecycle.

The lifecycle has two phases separated by authority rather than hidden orchestration. A Completed task allows the read-only Assisted Development Loop to produce the current Roadmap Proposal while leaving every repository document unchanged. Execution before activation is rejected without evidence or RunRecord storage. Only an explicit external test-fixture transition creates an `In Progress` task containing task identity, approval evidence, and Tool scope. The resulting execution reuses the Gate 5 CLI and existing Gate 1 through 4 boundaries through independently verified completion, durable RunRecord persistence, target deletion, and restart-safe replay.

No production component turns a Proposal into approval, mutates `CURRENT_TASK.md`, or infers Tool authority. The characterization test passed on its first run, proving the existing composition without a production correction or second orchestration path.

### Delivery Gate 1 Boundary

The first `ToolRequest` uses a non-blank Tool name, a non-blank correlation identity, and an immutable string argument map. `ToolExecutor` resolves the request against a unique in-process Tool registry and applies `ExecutionPolicy` before invocation.

`ExecutionPolicy` owns the normalized project root, explicit allow and deny Tool-name sets, maximum readable bytes, positive timeout, and cancellation token. Deny takes precedence over allow. Cancellation is checked before and after invocation, and execution runs behind a bounded timeout.

The first concrete Tool is `ReadFileTool`. It accepts only a relative path, resolves the real target path, rejects traversal and symbolic-link escape outside the real project root, requires a regular file, enforces the size limit before reading, and decodes UTF-8 strictly. Its no-argument Gate 1 mode returns no fictional complete-output reference and therefore fails structurally if oversized output would truncate; the Gate 2 constructor supplies `EvidenceRecorder` for larger successful reads.

Policy denial, unknown Tool, cancellation, timeout, malformed arguments, path escape, missing file, size overflow, invalid UTF-8, and unexpected Tool exceptions are represented as bounded failure `ToolResult` values. The Gate 1 boundary itself does not persist full evidence and does not authorize mutation; Gate 2 adds only governed evidence-root writes.

### Delivery Gate 2 Boundary

Gate 2 introduces `EvidenceStore`, `FileSystemEvidenceStore`, stored and resolved evidence records, an explicit `EvidenceStoragePolicy`, and `EvidenceRecorder`. The storage policy enforces only the per-artifact content bound production actually applies; it makes no expiry or deletion claim. The filesystem store generates UUID run and evidence identities and exposes opaque references in the form `evidence/<run-id>/<evidence-id>`.

Each evidence artifact is one versioned binary envelope containing its creation time, UTF-8 byte length, SHA-256 digest, and full output bytes. Persistence writes a temporary file in the final run directory and publishes it with an atomic move. A host that cannot provide the atomic move fails the write rather than silently weakening the contract.

Resolution validates reference grammar and containment, file size, envelope header, declared length, SHA-256 digest, and strict UTF-8 decoding before returning content. Missing artifacts and corrupted artifacts use separate checked failure types. Maximum stored bytes are explicit policy; retention and automatic or destructive cleanup have no implemented contract.

`EvidenceRecorder` stores output only when the bounded `VerificationEvidence` tail is truncated. A persistence-enabled `ReadFileTool` uses the request correlation identity as a previously created evidence run identity, allowing one real request to return a resolvable complete-output reference. `ExecutionPolicy` and evidence storage share the initial 64 MiB absolute implementation ceiling, while callers configure lower operational limits. The no-argument Tool remains available for the bounded Gate 1 path. Gate 2 does not add Agent Loop, verifier, CLI, Git, terminal, network, or LLM behavior.

### Delivery Gate 3 Boundary

Gate 3 introduces `AgentRunState` and `AgentRunController`. Run state carries an externally approved task, a caller-created pending request, the last Tool result, loop status, and a deterministic progress key. The controller owns only orchestration: it receives an existing `ToolExecutor`, immutable `ExecutionPolicy`, and external `ToolFailureClassifier`; it cannot register Tools, create or approve work, or broaden Tool authority.

A successful Tool result transitions to `AWAITING_VERIFICATION`, never directly to `COMPLETED`. A terminal failure transitions to `FAILED`. A retryable failure retains its pending request and remains `RUNNING`. Canonical request/result fingerprints make identical retry outcomes reuse the existing stagnation and maximum-iteration exits without inspecting human-readable diagnostic prose.

The existing bounded loop engine is shared by the original minimal state and the richer run state. Production capability remains read-only except for the evidence store's governed artifact writes. Gate 3 adds no Git, shell, network, browser, LLM, approval, independent verification, or RunRecord authority.

#### Gate 3 Hardening Boundary

`ApprovedTaskReader` converts the active `CURRENT_TASK.md` inside `ProjectContext` into a structured `ApprovedTask`. The document must provide a stable task ID, `In Progress` status, task description, explicit approval evidence, and an allowed Tool-name list. This is repository provenance supplied by the human-governed task document; it is not a signature and cannot override `ExecutionPolicy`. `AgentRunState.ready` rejects a request outside the approved Tool scope.

Every failed `ToolResult` carries a structured `ToolFailureCode`; successful results carry none. `ToolExecutor` assigns boundary-specific codes, and the standard retry classifier retries only timeout and explicitly temporary failures. Diagnostic summaries remain human-facing and are never parsed for control decisions.

`VerificationEvidence.capture` records a SHA-256 digest of complete output. Agent progress uses stable task, request, result, failure-code, exit-code, length, and content-digest fields while excluding opaque evidence locations and prose summaries. Therefore re-persisting identical content does not reset stagnation.

`AgentRunState` is an immutable final class with a private constructor. Public callers can create only a ready state from `ApprovedTask` and an in-scope request; controller-owned package transitions create retry, failure, and verification-wait states. Gate 4 is the immediate consumer of these hardened contracts.

### Delivery Gate 4 Boundary

Gate 4 adds a sequential verification and finalization boundary outside `AgentRunController`. `VerificationRequest` binds the approved task, executed request, successful Tool result, and caller-supplied expected content digest. `IndependentVerifier` returns a typed `VerificationDecision`; human-readable reasons are diagnostic only and never drive completion.

The first deterministic verifier supports the read-only file scenario. It verifies non-truncated output directly, resolves truncated output through `EvidenceStore`, recomputes SHA-256 over complete UTF-8 content, and compares the computed digest with both `VerificationEvidence.contentSha256` and the expected digest. Missing evidence is Unverified. Corrupted, structurally inconsistent, or content-mismatched evidence is Rejected. Worker failure, stagnation, and iteration exhaustion are recorded with verification Not Performed.

The executed `ToolRequest` remains part of terminal `AgentRunState` so verification and audit do not reconstruct inputs from prose or hashes. Only the sequential finalizer can create a `COMPLETED` state, and only from `AWAITING_VERIFICATION` plus a Verified decision. Rejected or Unverified decisions leave the run at the verification boundary.

Every finalization attempt produces a typed `RunRecord` containing the approved task, Tool request, immutable policy snapshot and decision, Tool result and evidence, verification decision, iteration count, and worker/final stop reasons. The filesystem store writes a versioned length-prefixed binary payload inside a SHA-256 envelope and publishes it atomically. A completion result is not returned until the RunRecord is durable and replayable. Gate 4 itself added no supported CLI and therefore promoted the vertical slice only to Integrated. Gate 5 now composes those unchanged boundaries behind the supported local command.

#### Gate 4 RED Contract Hardening

The worker result retains the exact immutable `ExecutionPolicy` used by `AgentRunController`. Finalization derives its persisted policy decision from that retained policy and does not accept a second caller-supplied policy that could rewrite the audit record after execution.

`RunRecord` mirrors the governed lifecycle rather than accepting merely type-correct combinations. A worker cannot report `COMPLETED`; awaiting-verification records require a successful Tool result and a performed verification decision; failed, stagnated, and iteration-limited records require a failed Tool result and verification Not Performed. Only Verified may promote `AWAITING_VERIFICATION` to `COMPLETED`.

### Pre-Operational Foundation Hardening

Before Gate 5 exposed the integrated path through a supported command, the existing Gate 1 through 4 boundaries were hardened without changing their maturity or authority.

Each Tool invocation uses an isolated worker lifecycle. A timeout cancels and retires only that invocation so an interruption-ignoring Tool cannot prevent a later invocation from starting. `ExecutionPolicy` accepts only durations that remain positive when represented as audit milliseconds and fit the nanosecond execution API.

Evidence and RunRecord binary envelopes integrity-protect their complete version, timestamp, declared-length, and payload/content fields rather than digesting content alone. RunRecord string persistence uses strict UTF-8 encoding and rejects malformed Unicode instead of replacing it.

The Repository Context Reader applies a bounded startup-document size, strict UTF-8 decoding, and real-path containment within the real project root. The build declares its JUnit Platform runtime launcher explicitly and provides a workspace-local default test temporary directory so Gradle 9 compatibility and sandboxed test execution do not depend on implicit or user-profile state.

The no-persistence `ReadFileTool` mode still cannot return truncated evidence without a complete-output reference. That condition is an execution/evidence-capability failure, not malformed caller input.

### Delivery Gate 5 CLI Boundary

Gate 5 selects `com.enhancer.cli.EnhancerCli` as the first supported local entry point and exposes only `run` and `replay`. The Gradle application entry point is a thin composition boundary over the existing Context Reader, repository-derived `ApprovedTask`, read-only Tool execution, sequential verifier, finalizer, Evidence Store, and RunRecord Store.

The implemented command is intentionally non-interactive. Every final worker outcome that reaches finalization is persisted before its stable exit code is returned; configuration errors and internal failures remain bounded diagnostics rather than fabricated records. This is the first Operational scenario, not the future multi-interface CLI of Gate 12.

`run` requires explicit project root, task identity, relative target path, expected SHA-256 digest, evidence root, and RunRecord root. The supplied task identity must equal the active task read from the governed project context; it does not create or approve a task. The command registers only `read-file`, applies the existing 64 MiB ceiling and a five-second timeout, and performs at most five loop iterations with the existing three-transition stagnation threshold.

Process results use stable exit codes for completed, usage/configuration error, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure. Diagnostics are bounded and never include complete target content. A finalized run prints its opaque RunRecord reference and storage root.

`replay` requires an explicit RunRecord root and reference, resolves the integrity-checked record through `FileSystemRunRecordStore`, and prints bounded typed task, request, policy, verification, and stop metadata. It does not re-execute the Tool or reinterpret the record as repository authority.

## Constitution Kernel Architecture

`CONSTITUTION.md` is the stable normative Kernel, not the complete Codex guidebook. It defines identity, document authority, lifecycle states, authorization boundaries, verification principles, self-hosting safeguards, and amendment governance.

Operational procedures belong in `AGENTS.md` and `.ai/`; component contracts belong in RFCs and `docs/`; active and implemented state belong in `CURRENT_TASK.md` and `PROJECT_STATE.md`; repeatable invocations belong in prompts and validated Skills. The 300-page documentation target is distributed across this document system so every session does not need to load the entire guidebook as constitutional context.

## Architectural Principles

- Repository documents are product inputs, not only project management notes.
- Keep conceptual examples with their owning specification and executable examples in tests; do not maintain a separate `examples/` directory.
- Keep the first implementation minimal.
- Prefer Java 17, Spring Boot 3, Gradle, JUnit5, and Mockito.
- Do not introduce DDD early.
- Do not add abstractions until duplication or complexity justifies them.
- Accepted decisions belong in `DECISION_LOG.md`.
- Implemented state belongs in `PROJECT_STATE.md`.
- Repository Skills use validated `skills/<name>/SKILL.md` definitions with least-privilege capability categories.
- Proposed Skill catalog entries are design candidates, not loadable runtime inputs.
- Repository memory is distilled by promoting reusable procedures to Skills and repository-local rationale to decisions or ADRs.
- External agent harnesses are reference implementations, not runtime dependencies. Selected patterns must be restated as provider-neutral Enhancer contracts and introduced only when the owning roadmap slice is active.

## Open Architecture Questions

- Context size and token budgeting strategy are not selected yet.
- Future LLM-backed Planner input/output schema is not selected yet.
