# Architecture

## Status

The accepted product direction is Self-hosting AI Development Operating System.

This document describes the architecture: what each component is, what it connects
to, and which boundaries it must not cross. It does not state per-gate maturity.
Current maturity is in `PROJECT_STATE.md` and the evidence behind it in
`docs/verification-log.md`; a section here that claims a gate is Integrated or
Operational is duplication and should be removed rather than updated.

## Capability Maturity Model

Roadmap capability maturity is separate from the task lifecycle defined by the Constitution:

- **Specified:** the capability has an accepted responsibility, boundary, and exit criteria.
- **Contract Verified:** core types, invariants, and focused tests exist without claiming end-to-end behavior.
- **Integrated:** the capability is connected to its real upstream and downstream collaborators in an integration test.
- **Operational:** a supported entry point can execute the capability against a real project with observable evidence and documented recovery.
- **Released:** the operational capability is intentionally distributed with release evidence.

`Implemented` MUST NOT be used by itself for roadmap capability state because it hides the difference between a contract and an operational product. `PROJECT_STATE.md` records verified current maturity; `ROADMAP.md` records the next promotion gate.

## Product Journey And Evaluation Model

Capability maturity proves that a component is specified, tested, connected, operable, or released. It does not by itself prove that a user can finish a development job. Enhancer therefore maintains a cross-cutting Product Journey and Evaluation Track alongside the delivery gates. The track never weakens a gate's technical exit criteria; a product or release claim requires both the applicable gate evidence and the applicable journey evidence.

The initial canonical journeys are:

| Journey | User-visible outcome | Required proof |
|---|---|---|
| Governed bug repair | an inspectable, commit-ready correction without an automatic commit | reproduced defect, approved plan and scope, bounded diff, relevant tests, independent review/verification, risks, and explicit commit boundary |
| Bounded feature delivery | a scoped feature reaches review-ready completion | accepted goal, authorization and budget, changed files/diff, tests and evidence, compatibility risks, approval points, and rollback or recovery guidance |
| Evidence-backed codebase explanation | the user receives an answer without repository mutation | cited repository sources, snapshot/freshness identity, uncertainty and missing evidence, no Tool-authority expansion, and no changed files |
| Interrupted-run recovery | an interrupted job resumes or stops safely without hidden duplication | durable checkpoint, state/schema version, lease/fence evidence, reclaimed-orphan decision, replay-safe effects, final status, and user-visible recovery history |

Every journey fixture is versioned and records the supported surface, repository revision, task and policy, budgets, expected approvals, expected artifacts, induced failures, and scoring rules. A journey is not Operational merely because its participating components are Operational; it must pass end to end through a supported interface.

The evaluation harness reports at least these measures with explicit denominators:

- task success rate: attempts satisfying every required journey outcome divided by all attempted fixtures;
- incorrect-change rate: change-producing attempts containing an unauthorized, out-of-scope, or functionally incorrect change divided by all change-producing attempts;
- retry and recovery success rate: induced retry/interruption cases restored to a valid resumable or terminal state divided by all induced cases;
- cost and elapsed time: median and tail values per successful attempt, with failed attempts reported separately rather than discarded;
- user intervention: clarification, repair, and exceptional-authority interventions per attempted journey, with mandatory approvals reported separately;
- post-verification regression rate: completed change fixtures that fail held-out regression checks divided by all completed change fixtures;
- multi-agent delta: quality, success, cost, time, and intervention difference against the single-agent baseline on the same fixture revision and comparable budget envelope.

Evaluation thresholds, fixture versions, and scoring rules are fixed before an evaluation run. Results retain run, model/provider, policy, code revision, evidence, and evaluator provenance. Agent confidence, reviewer pass, anecdotal demonstrations, and cherry-picked successful runs are not release evidence.

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
- Evaluation Harness: runs versioned product-journey fixtures, records comparable quality/cost/recovery evidence, and enforces release thresholds.
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

The Workspace snapshot contract is a metadata-only immutable snapshot under `com.enhancer.workspace`. `ApprovedTaskRevision` records task identity, source-document identity, and the SHA-256 revision of the approved source. `WorkspaceSourceObservation` records a typed source, stable source identity, adapter provenance, observation time, optional source-update time, explicit Available/Stale/Unavailable state, optional content digest, and bounded reason metadata.

`WorkspaceSnapshot` normalizes the absolute project root, sorts observations canonically, rejects duplicate kind/identity pairs and more than 4096 observations, and computes its own SHA-256 identity over every identity-bearing metadata field. Caller order cannot change the identity. Source payloads, Tool scope, policy, approval creation, and command authority are absent by construction.

Gate 7 message envelopes carry the same snapshot identity across handoffs.

### Gate 6 Project Brain View

`ProjectBrainView` is the read-only aggregate under `com.enhancer.brain`. It composes exactly one `WorkspaceSnapshot`, one `ProjectContext` repository memory, and one `RunRecord`, and derives everything it exposes from those inputs. It performs no collection of its own.

The view is keyed to the snapshot's canonical identity rather than computing a second one. Repository memory is projected to `RepositoryMemoryEntry` metadata of document path, read order, and a computed lowercase SHA-256 of the document content; no document content is retained. Each entry carries an explicit `MemoryFreshness` derived by comparing that digest against the snapshot's `REPOSITORY_DOCUMENT` observation with the same source identity: `SNAPSHOT_MATCHED` for equal digests, `SNAPSHOT_DIVERGED` when the snapshot observed a different or unconfirmed revision, and `NOT_OBSERVED` when the snapshot never observed the document. `RunProvenance` projects the RunRecord to logical run identity, record time, approved task identity, and verification status only; Tool requests, results, evidence bodies, and chat history are absent by construction.

The view requires the RunRecord's approved task identity and source document to equal the snapshot's `ApprovedTaskRevision` and rejects an unrelated run rather than aggregating misattributed provenance. Workspace Available, Stale, and Unavailable states pass through unchanged.

### Gate 6 Repository Memory Collection

`RepositoryMemorySnapshotCollector` is the read-only Workspace source adapter over repository memory. It derives a real `WorkspaceSnapshot` from repository memory that the Context Reader already loaded: one `AVAILABLE` `REPOSITORY_DOCUMENT` observation per document with `context-reader` provenance and a computed content digest, plus an `ApprovedTaskRevision` digested from the approved task's source document in the same memory. It reads no files itself, reuses `WorkspaceSnapshot.capture` for identity and bounds, takes its capture time as an explicit parameter, and rejects memory that lacks the approved task source document.

Because the collector observes only loaded memory, `STALE` and `UNAVAILABLE` observations first appear with real per-source adapters.

### Gate 6 Production Composition

The `EnhancerCli` `run` path composes the view in production. The CLI keeps the `ProjectContext` it already loads for task approval, collects the snapshot with a capture time taken before worker execution, composes the view after finalization with the persisted RunRecord for every outcome that produces a record, and appends `workspaceSnapshotId`, `workspaceObservations`, and a `memoryFreshness` summary to the bounded run output. No content, digest list, or evidence is printed; no command, argument, exit code, or authority was added. The RunRecord does not store the snapshot identity; carrying that identity across handoffs belongs to the Gate 7 envelope contracts.

Diagnostics, terminal-session, and active/selected-file observation are owned by Gate 12, moved there by the 2026-07-15 re-scope-and-promotion decision.

### Gate 6 Graph Projection Contract

The graph projection contract under `com.enhancer.brain` types the Project Brain graph model without producing, persisting, or querying graphs. `GraphNode` carries a bounded identity and one of five kinds (task, decision, component, artifact, execution). `GraphEdge` carries one of six endpoint-checked kinds covering the five roadmap relationship domains: `JUSTIFIED_BY` task-to-decision, `SUPERSEDES` decision-to-decision, `DEPENDS_ON` between components and artifacts, `MODIFIES` task-to-artifact, `VERIFIED_BY` artifact-to-artifact, and `RECORDED_AS` task-to-execution. Each edge kind declares its valid endpoints in the type, so a later impact query traverses meaning rather than convention.

Every element carries `GraphProvenance`: a bounded source reference, an optional lowercase SHA-256 source revision, and explicit `CURRENT`/`STALE`/`SOURCE_MISSING` freshness with derived rebuild-required status; Current and Stale require a revision and Source-Missing prohibits one. `ProjectBrainGraph.project` keys the projection to one valid Workspace snapshot identity with an explicit projection time and the `project-brain-graph-v1` version, orders nodes and edges deterministically, and rejects duplicates, self-loops, unknown endpoints, endpoint-kind violations, and more than 4096 elements per collection.

Modifies, verified-by, justified-by, supersedes, and depends-on producers remain deferred to their own evidence sources.

### Gate 6 Task Impact Query

`TaskImpactQuery` answers the first rebuildable task-to-decision-to-code-to-test question over exactly one projected graph. From the queried task node it traverses only the named chain — `JUSTIFIED_BY` to decisions, `MODIFIES` to artifacts, `VERIFIED_BY` from those modified artifacts to their verifying artifacts, and `RECORDED_AS` to executions — and returns an immutable `TaskImpact` carrying the graph's source snapshot identity and one derived rebuild-required status. The status is true exactly when the task node, a traversed edge, or a returned node requires rebuild, so the answer says when it stops being trustworthy; unrelated stale elements do not taint it. Transitive `DEPENDS_ON` closure is deliberately deferred until real dependency projections exist.

### Gate 6 Run Evidence Graph Producer

`RunEvidenceGraphProducer` projects graph elements from stored evidence. From one Workspace snapshot and one task-matched stored run record it projects only what that evidence proves: a task node from the approved task revision, one artifact node per repository document/file observation with the observation state mapped one-to-one to element freshness (Available to Current, Stale to Stale, Unavailable to Source-Missing), an execution node carrying the stored envelope SHA-256 and durable reference, and a single `RECORDED_AS` edge. It never emits decision, modifies, verified-by, justified-by, supersedes, or depends-on elements, because no current evidence source justifies them; each of those arrives with its own producer and decision.

### Gate 6 Decision Projection And Run Record Observation

`AcceptedDecisionProjector` parses accepted decisions from the decision log's own `Status: Accepted Decision` lines in already-loaded repository memory into `DECISION` nodes. Freshness is snapshot-relative: a matching observed digest is `CURRENT`; a diverged or unobserved document is `STALE`, because currency cannot be proven without a matching observation.

`TaskJustificationProjector` links tasks to decisions only through the optional `## Justified By` section of the active task document, whose bullets must name accepted-decision headings exactly. Resolved references become `JUSTIFIED_BY` edges with task-document provenance and snapshot-relative freshness; unresolved, duplicate, empty, or non-bullet references are rejected rather than skipped, and an absent section honestly claims no justification.

`RunRecordMetadataCollector` observes at most the 256 most recent stored records through `recentReferences(limit)`: the filesystem store performs one directory scan and bounded newest-selection over no-follow modification metadata, then only the selected payloads are resolved. Each becomes one `RUN_RECORD` observation with `run-record-store` provenance, the envelope SHA-256 as content digest, and the stored time as source-update time; a selected record that fails integrity resolution becomes an explicit `UNAVAILABLE` observation. Complete lexicographic `references()` and point replay remain available, no artifact is deleted, and a future durable index may replace the residual linear directory scan.

### Gate 6 Target File And Git Observation

`TargetFileMetadataCollector` observes the governed run's target file pre-run as a `REPOSITORY_FILE` observation with a streamed containment-checked SHA-256 and `target-file-reader` provenance; missing or over-64-MiB targets are explicit `UNAVAILABLE` observations, while absolute, traversal, escaping, or non-regular targets are configuration errors surfaced before execution.

External command authority exists in exactly two places, each granted and scoped by its own accepted decision. `GitWorkspaceCollector` is the only one that runs a configured external program; `IsolatedWorkerLauncher` can only re-run the JVM this process is already running.

`GitWorkspaceCollector` is scoped as follows. It resolves Git only from absolute PATH entries, canonicalizes the candidate, rejects executables inside the real observed project root, and otherwise emits `UNAVAILABLE` rather than invoking a name. Its sole enabled command is fixed filter-free index metadata (`git ls-files --stage --deleted --others --exclude-standard`) with no shell or inherited `GIT_*` overrides; tracked worktree diff is explicitly `UNAVAILABLE` because adversarial verification proved that status, `ls-files --modified`, and `diff-files --raw` can all execute required clean filters. The command uses `--no-optional-locks`, an invocation-scoped fsmonitor disable, discarded stderr, a watchdog-enforced five-second timeout, a four-MiB output cap, and repository discovery confined to the project root via a collector-owned `GIT_CEILING_DIRECTORIES`. Only its SHA-256 output digest is retained as `GIT_STATUS` metadata. The authority-boundary exit criterion is pinned by `WorkspaceAuthorityBoundaryIntegrationTest`: adversarial tool-grant text in observed documents cannot widen task or policy scope, appear in bounded output, or mutate any document.

### Gate 6 Production Graph Composition

The CLI `run` path composes the graph in production: the RunRecord store is constructed before collection so prior records are observed into the snapshot, accepted-decision nodes and resolved `Justified By` edges from the same loaded memory are merged into the run-evidence graph through additional-observation, additional-node, and additional-edge overloads, and the task impact query is answered in process. The output reports bounded `graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`, and `impactDecisions` counts only. Snapshot identity intentionally reflects prior run-record observations, so identical trees with different run histories produce different snapshot identities.

Graph metadata that is available from the snapshot and repository memory is projected and structurally preflighted before evidence creation or Tool execution. Repository-document and target-file observations sharing one path collapse to one artifact node with the target-specific observation preferred, and graph node identities share the Workspace 1024-character source bound. After the finalized RunRecord is persisted, Project Brain view/graph/query composition is optional diagnostics: a runtime failure emits bounded `brainStatus=UNAVAILABLE` metadata and cannot replace the durable record-derived exit code with an internal error.

Impact answers carry executions and explicitly justified decisions; modifies and verified-by evidence does not exist yet.

## Gate 7 Message Envelope Contract

The Gate 7 envelope contract is reference-only, under `com.enhancer.bus`. `MessageEnvelope` is versioned (`message-envelope-v1`) and carries a canonical-UUID message identity, a bounded correlation identity, an optional canonical-UUID causation identity that must differ from the message identity, bounded logical-run and producer identities, an occurrence time, and one typed payload.

`MessagePayload` is sealed to exactly four kinds. The work payload carries the approved task revision, a valid Workspace snapshot identity, and an immutable allowed-tool scope of 1 through 256 unique names, each bounded to 256 characters; the result payload carries the task identity, a run-record reference, and the verification status; the control payload carries a typed cancel/pause/resume signal with a bounded reason; the handoff payload carries the task revision, snapshot identity, and run-record reference. Authorization is carried as data, never created: possessing an envelope grants nothing, and delivery code must validate contents against repository authority rather than trust the sender.

Together with the per-name ceiling, the explicit allowed-tool cardinality ceiling gives the only collection-bearing payload a finite aggregate tool-name ceiling of 65,536 characters, satisfying the Roadmap exit criterion that payloads are bounded or replaced by evidence references.

The contract is consumed by both the deterministic in-process topic and queue delivery surface and the transport-neutral IPC boundary below. Production integration remains deferred.

## Gate 7 In-Process Delivery

`InProcessMessageBus` under `com.enhancer.bus` is a synchronous, single-threaded, deterministic delivery surface over `MessageEnvelope`. A `DeliveryDestination` is a typed `DeliveryDestinationKind` (`TOPIC` or `QUEUE`) plus a bounded name; a topic publication fans out to every subscriber in registration order, and a queue publication is delivered point-to-point to a single consumer, rejecting a second consumer. Each publication returns an immutable ordered list of per-subscriber `DeliveryOutcome`s carrying a `DeliveryStatus` of `DELIVERED`, `DUPLICATE`, or `UNROUTED`.

Delivery is idempotent per `(destination, subscriber, message identity)`: re-publishing the same envelope invokes the handler at most once and reports `DUPLICATE`. Every publication is appended to an ordered immutable journal of `JournaledMessage` entries, and `replay` re-dispatches a journal deterministically without appending to it, reproducing the original outcomes on a fresh bus and producing only `DUPLICATE` with no duplicate side effect when replayed against a bus that already processed them. The bus carries whole envelopes without mutation, so authorization and provenance survive every hop; it never creates authority.

The bus also isolates delivery failures under a bounded retry policy: the bus is constructed with an immutable `RetryPolicy` (1 through 10 attempts; the no-argument constructor keeps a single attempt), and a handler `RuntimeException` is retried immediately and synchronously, with no delay between attempts, until it succeeds or the policy is exhausted. Success within the policy is an ordinary `DELIVERED`. Exhaustion records a `FAILED` `DeliveryOutcome` for that subscriber, captures an immutable `DeadLetter` (destination, subscriber, unmodified envelope, a bounded reason derived from the last failure, and the failed attempt count) into an ordered `deadLetters()` record, and continues delivering to the remaining subscribers. A failed delivery consumes the idempotency key, so re-publishing or replaying it reports `DUPLICATE` and adds no further dead letter.

The dead-letter record is the sole re-delivery authority: `redeliver(DeadLetter)` accepts only a dead letter the bus currently records, re-invokes the subscription's handler under the same bounded policy, resolves the entry on success (`DELIVERED`, entry removed), and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason (`FAILED`). Re-delivery never appends to the journal and never releases the consumed idempotency key.

Cancellation is scoped to the envelope's own `correlationId`, the identity the envelope contract already defines for grouping related messages across hops. `cancel(correlationId)` is idempotent and monotonic — there is no resume — and `isCancelled` reports it. Cancellation is admission control that runs before subscription lookup, idempotency, and dispatch, so a refusal dominates both `UNROUTED` and `DUPLICATE`: the delivery reports a scope-level `CANCELLED` outcome naming no subscription, invokes no handler, consumes no idempotency key, creates no dead letter, and is not journaled. Journaling a refused publication would make a fresh-bus replay produce a side effect that never originally happened, so the journal records exactly the publications that were admitted. The refusal propagates to every path: `replay` skips a cancelled entry while live correlations still deliver, and `redeliver` refuses a cancelled dead letter while retaining its record.

Because the bus never reads a payload to decide delivery, `ControlSignal.CANCEL` remains a consumer semantic: a handler that receives a `CANCEL` `ControlPayload` may call `cancel` itself. `PAUSE` and `RESUME` likewise have no bus behavior.

Ordering is established by running each publication to completion. The bus holds a pending queue and a single drain loop: a top-level `publish` or `replay` drains the queue to exhaustion and returns the whole ordered cascade, while a publication made from inside a handler only enqueues and reports the scope-level `ENQUEUED` status. Without this, synchronous dispatch nests — a handler that publishes during its own delivery has its child delivered in full before the parent's fan-out finishes, so every subscriber registered after the publishing one observes the effect before its cause. Draining from a queue also removes unbounded stack growth from a deep cascade.

Admission — the cancellation check and the journal append — happens inside the drain loop rather than at the publishing call. That keeps the journal's order equal to the bus's own total delivery order, preserves the invariant that the journal records exactly what was admitted, and lets a correlation cancelled mid-cascade refuse entries still queued behind it. A fan-out itself stays atomic: a cancellation raised during one cannot stop it. Every `publish`, including an already-cancelled re-entrant publication, enters this queue and admission path. `publish` and `replay` share submission and draining; a handler publication caused by a replayed entry inherits that entry's non-journaling mode, so replay never grows the live journal through a cascade. An `Error` escaping a drain abandons the cascade entirely rather than leaking queued entries into a later publication.

This delivery, its failure handling, its bounded retry and explicit re-delivery, its cancellation propagation, and its run-to-completion ordering are Contract Verified. The transport-neutral IPC interface below carries the same destination and envelope without changing these semantics. Backoff or delayed retry, priority ordering, competing queue consumers, threading, persistence, and concrete transport adapters remain later increments over this surface.

### Gate 7 Pending-Queue Backpressure

The run-to-completion pending queue is bounded by immutable `BackpressurePolicy` with a capacity from 1 through 4096 and a finite default. Because a re-entrant publisher is executing inside the single-threaded drain, the bus never blocks it: capacity exhaustion reports the scope-level `BACKPRESSURED` status immediately. Refused work is not admitted, journaled, dispatched, deduplicated, dead-lettered, or cancelled and may be explicitly retried later. Accepted work remains FIFO. Replay accepts the deterministic prefix that fits the configured capacity and reports the refused suffix while retaining replay's non-journaling behavior. The policy bounds pending publications only; retention bounds, threading, persistence, scheduling, and IPC remain separate concerns.

### Gate 7 Transport-Neutral IPC Boundary

`TransportMessage` carries exactly one existing `DeliveryDestination` and one existing `MessageEnvelope` without copying or reinterpreting either. Provider-neutral `MessageTransport.send` accepts that immutable route and envelope and returns a `TransportOutcome` whose `TransportStatus` is `ACCEPTED`, `BACKPRESSURED`, or `UNAVAILABLE`. Accepted outcomes carry no reason; non-acceptance carries a bounded diagnostic reason.

Transport acceptance is deliberately not Message Bus delivery. `ACCEPTED` means only that the configured adapter accepted responsibility for attempting one hop; it does not mean a receiving bus admitted, journaled, dispatched, or delivered the envelope. A transport refusal consumes no bus journal, idempotency, cancellation, failure, or dead-letter state, and higher-level scheduling owns any retry timing. The interface contains no provider endpoint, serialization, protocol, authentication, lifecycle, threading, persistence, or authority type.

#### File Spool Adapter

`FileSpoolMessageTransport` is the first implementation. It encodes one `TransportMessage` and writes it to its own file under a configured spool directory that a peer process reads, mapping the three statuses to conditions it can actually observe: a durably spooled message is `ACCEPTED`, capacity exhaustion measured against a `BackpressurePolicy` is `BACKPRESSURED`, and an unusable spool root is `UNAVAILABLE`. A refused message spools nothing.

The wire format belongs to `MessageEnvelopeCodec`, not the adapter: the frame is `[magic][bodyLength][sha-256 of body][body]`, with length-prefixed strict UTF-8 strings, all four payload kinds, and an unknown kind rejected on both encode and decode. Decoding is fail-closed on bad magic, invalid lengths, digest mismatch, malformed UTF-8, trailing bytes, and any envelope invariant violation, and reports `CorruptedSpooledMessageException` — distinct from a plain `IOException` because a corrupt message stays corrupt and should be dead-lettered, while a filesystem condition may be transient. Occurrence time is carried as epoch-second plus nanosecond, since rounding an `Instant` to milliseconds would rewrite provenance the receiver is meant to trust. The frame holds no wall-clock or random state, so one message always encodes to identical bytes and a peer may deduplicate on content.

The adapter owns publication only: a temporary file published by atomic move into its own freshly generated name, so resending an envelope never overwrites an earlier hop and a reader never observes a partial message.

#### Isolated Worker Process

`IsolatedWorkerLauncher` is the process lifecycle half of connection 3. It runs one worker in a child process and returns a typed `IsolatedWorkerOutcome`: `COMPLETED` carries an exit code, while `TIMED_OUT` and `START_FAILED` carry a bounded reason and no exit code, so a destroyed or unstartable child can never present a code that reads as a clean exit.

The authority is bounded to the JVM already running. The executable is resolved from `java.home`, canonicalized, and required to be a regular file; the child runs the current classpath; and the entry point is taken as a `Class<?>` rather than a command string, so it is necessarily already on that classpath. No caller-supplied executable, command name, or shell reaches `ProcessBuilder`. Unlike the Git adapter there is no lookup to poison, which is why the executable's location is not constrained — this project vendors its own JDK inside the project root.

The child is bounded like the Git adapter: output is discarded by the operating system rather than read, so a chatty child can neither block on a full pipe nor grow the parent's memory and nothing it prints can be mistaken for a result; the environment is stripped of `JAVA_TOOL_OPTIONS`, `_JAVA_OPTIONS`, and `JDK_JAVA_OPTIONS`, which would otherwise let an inherited setting inject JVM arguments; and a watchdog forcibly destroys a child that overruns its timeout, which is itself capped so a caller cannot disable it.

`IsolatedWorkerMain` is the child entry point. It reads one work message from a spool through the adapter above, runs the same Gate 1-4 pipeline as the in-process execution port, persists the RunRecord, publishes a correlated `ResultPayload` to a separate result spool, and exits with a stable code. The exit code reports lifecycle completion only; the RunRecord reference returns in the result envelope.

Isolation is what makes termination possible. The in-process ceiling of 64 live workers contains stuck code but cannot stop it, and that ceiling still governs in-process execution. `ProcessIsolatedAgentRunExecution` wires the launcher and file-spool adapter into the execution port, and `DurableAgentRunWorker.processIsolated` is the production composition that selects it with the real self-JVM launcher while sharing one durable queue instance between dispatch and finalization.

The adapter promises no ordering across separately spooled messages: the contract is per hop, and a spool directory has no ordering. Each adapter instance is one-directional by construction; process-isolated execution composes one work spool and one result spool under a per-cycle invocation root. No CLI, durable bus, or supported messaging entry point constructs that path.

#### Process-Isolated Execution

`ProcessIsolatedAgentRunExecution` is the second production `AgentRunExecution`. Before launch it accepts a pre-existing work entry only after decoding the sole message and matching both `queue("work")` and the complete dispatched envelope; foreign work and several work or result messages fail closed without starting a child. Re-entry checks the result spool first, so an already-published valid result returns without another execution.

A returned envelope is a claim, never authority. Its destination must be exactly `queue("isolated-worker-result")`; correlation, logical-run, causation, payload kind, and task identity must bind to the dispatched work; the reference must resolve in the shared store; and the RunRecord must match the work's task, source document, read-file target, verification-bearing expected digest, and claimed status. `DurableAgentRunFinalizer` remains the final authority and reuses the same task/source binding. Store roots are launcher configuration, not payload data. A child that persists a RunRecord and dies before result publication still leaves an orphan and may be re-executed under the explicit at-least-once contract.

Per-cycle work/result spools remain until `DurableAgentRunWorker` has persisted the returned RunRecord reference in its cycle-intent checkpoint. The worker then invokes the execution port's idempotent post-checkpoint cleanup before execution acknowledgement. Process-isolated cleanup deletes only the exact Goal/AgentRun invocation tree and an empty Goal parent, never the invocation root, Evidence, or RunRecords. Cleanup failure leaves the checkpoint intact; restart retries cleanup without child re-execution. Failed, corrupt, timed-out, or incomplete execution retains its current spool for recovery or diagnosis, and no time-based cleanup scheduler exists.

### Gate 7 Runtime Integration Preparation

`WorkMessagePublisher` is the first authority-preserving application boundary that connects real Gate 6 input to the Gate 7 bus. It accepts one matching repository-derived `ApprovedTask` and `WorkspaceSnapshot`, derives the existing `WorkPayload` task revision, snapshot identity, and allowed-Tool scope, constructs a versioned envelope from explicit deterministic metadata, and publishes it to an explicit in-process queue. Task-identity, source-document, or pre-snapshot-time mismatch is rejected before bus admission.

`WorkItemAdmissionHandler` is the matching Gate 7-to-Gate 8 adapter. It retains the delivered envelope unchanged inside one `WorkItem` using injected identity generation, required capability, and downstream sink. It creates no approval, storage, ordering, execution, or Scheduler semantics. A named integration test connects the real Context Reader and Workspace collector through the real bus, journal, and replay path to this admission boundary and proves unchanged authorization/provenance projections plus duplicate-free replay.

The work-admission path exercises `WorkPayload`, message/correlation/run/producer identity, queue delivery, journaling, replay, and duplicate suppression. Process-isolated execution separately supplies one named `MessageTransport` work/result path with non-empty result causation. Control and handoff payloads; topic delivery; and failure/retry/dead-letter, cancellation, re-entrant ordering, and backpressure branches still have no named real upstream-to-downstream production connection. Those missing connections remain required before Gate 7 can be promoted as a whole.

## Agent Runtime Model

The target runtime is a persisted, event-driven state machine:

```text
Goal -> Planner -> Queue -> Executor -> Evidence
     -> Memory -> Reflection -> Retry | Verification -> Done
```

Planner, Coder, Reviewer, Tester, and Memory are roles or workers behind message contracts, not hard-coded direct-call chains. Single-agent sequential execution is implemented first. Multi-agent concurrency begins only after queue, idempotency, cancellation, RunRecord, recovery, and independent verification are operational.

### Gate 8 WorkItem Admission Contract

The Gate 8 admission contract is an immutable scheduler-facing `WorkItem` under `com.enhancer.runtime`. A work-item identity is a canonical UUID distinct from the Gate 7 message identity because logical work and one delivery attempt are different identities. The item retains exactly one existing `MessageEnvelope` carrying `WorkPayload` plus one bounded required-capability name; logical run identity, approved task revision, Workspace snapshot identity, and allowed Tools are projections of that unchanged envelope rather than caller-supplied copies.

Admission rejects non-work payloads, malformed or reused identities, and blank or oversized capabilities. It creates no approval, Tool permission, state transition, dependency, queue, lease, persistence, or execution authority. The dependency-ready single-worker Scheduler queue is its first consumer, and the separate durable Goal/AgentRun lifecycle below retains the same exact WorkItem. Budgets, cancellation, leases, worker execution, and broader recovery remain later Gate 8 increments.

### Gate 8 Dependency-Ready Single-Worker Queue

`QueuedWork` retains one exact `WorkItem` and an immutable dependency set of at most 256 canonical work identities. A work item cannot depend on itself, repeat a dependency, or reference work that the same run-scoped queue has not already admitted. Requiring dependency-first admission prevents cycles in this initial topology without claiming arbitrary graph-cycle analysis or forward-reference support.

`SingleWorkerSchedulerQueue` admits at most 4096 work items per run-scoped instance, rejects duplicate work identities, preserves admission order, and claims the first item whose dependencies are completed. It has exactly one active slot: another claim returns empty until the matching active identity is explicitly completed, and only completion releases dependent work. The queue retains each `WorkItem` and its Gate 7 envelope by identity and creates no task approval, Tool authority, verification result, or execution outcome.

The base queue implementation is deliberately in-memory and single-threaded: claim is not a lease, completion is not a durable AgentRun terminal state, and there is no failure/retry/cancellation, priority, budget, timeout, fence, orphan recovery, worker execution, or production wiring. The separate durable wrapper below supplies schema-v1 queue state and restart recovery without changing those limits.

### Gate 8 Durable Queue State And Restart Recovery

The next bounded increment gives each durable queue a caller-supplied canonical identity and records one immutable schema-versioned snapshot containing its revision, capacity, logical run binding, total admission order, ordered pending work, optional active work, and completed identities. Persisted work retains the exact scheduler-facing WorkItem data and unchanged Gate 7 envelope, including task revision, Workspace snapshot identity, allowed-Tool scope, message provenance, capability, and dependencies. One queue accepts work from only one logical run.

Every successful enqueue, claim, and completion is staged on a copy and atomically persisted before the transition becomes visible in memory. The filesystem adapter keeps one bounded integrity-checked binary snapshot per queue, uses strict UTF-8, refuses unsupported schema versions or structural corruption, does not overwrite an existing queue during creation, and requires updates to advance exactly one revision. Persistence failure leaves the previous durable and in-memory revision authoritative.

Durability sits behind the `SchedulerQueueStore` port — `create`, `update`, and `resolve` over one `SchedulerQueueState` snapshot — with `FileSystemSchedulerQueueStore` as its only implementation. The port grants no execution authority and must reject missing or invalid state rather than invent defaults. It is the seam a different durability substrate would replace, and it is deliberately narrower than the queue: the store persists and returns state, while readiness, the single active slot, and disposition remain in `DurableSingleWorkerSchedulerQueue` above it. A process-isolated worker or an out-of-process scheduler therefore changes which implementation is wired, not the queue contract. The same-instance rule still applies whatever the implementation: the dispatcher and finalizer handed to a worker must wrap one queue instance, because the queue's in-memory revision advances with each persisted mutation and a stale second instance fails the store's exactly-one-revision-advance check.

Because this increment has no lease or worker ownership, restart recovery moves a previously active item back into pending order and persists that recovery transition before returning the queue. The item may therefore be offered again under at-least-once semantics. This prevents hidden work loss and permanent active-state blockage but does not deduplicate external effects. Schema migration beyond v1, leases/fencing, worker execution, effect records, failure/retry/cancellation policy, multi-process coordination, and snapshot history or cleanup remain deferred.

### Gate 8 Durable Goal And AgentRun Lifecycle

The next runtime increment is one immutable schema-v1 `AgentRuntimeState` containing exactly one `RuntimeGoal`, the Goal's exact existing `WorkItem`, and at most one `RuntimeAgentRun`. Goal, AgentRun, WorkItem, and message identities are distinct canonical UUIDs. The retained WorkItem remains the sole source of approved task revision, Workspace snapshot, logical run, required capability, and allowed-Tool provenance; lifecycle state cannot add or widen authority.

The Goal advances from `ACCEPTED` to `ACTIVE` and then to `COMPLETED` or `FAILED`. Its single AgentRun advances only through `PLANNING -> READY -> EXECUTING -> AWAITING_VERIFICATION -> COMPLETED|FAILED`; skipped, reversed, repeated, mismatched, and post-terminal transitions fail. Terminal transition requires one exact `ResultPayload` envelope whose logical run, correlation, task, and causation match the retained work message. Only `VERIFIED` completes; Rejected, Unverified, and Not Performed results fail explicitly.

`DurableAgentRuntime` stages every transition and persists the next revision before adopting or exposing it. `FileSystemAgentRuntimeStateStore` keeps one bounded strict-UTF-8 integrity-checked binary artifact per Goal, atomically creates or replaces it, requires revision increments of exactly one, preserves the exact work and result envelopes, and fails closed on missing, corrupt, oversized, trailing, structurally invalid, or unsupported state.

`READY` is the only lease-acquisition state. Acquisition issues one bounded non-blank owner identity, a persisted monotonically increasing positive fence token, an injected-clock issue time, and an exclusive expiry from 1 millisecond through 24 hours, then moves the AgentRun to `EXECUTING`. Renewal preserves owner and fence, must extend expiry, and execution completion requires the same unexpired owner and fence. At or after expiry, explicit reclaim or runtime recovery persists `EXECUTING -> READY`, clears the lease, retains the last-issued fence, and ensures the next acquisition receives a greater fence. Acquisition, renewal, completion, and reclaim all preserve persist-before-exposure; a storage failure leaves the previous state authoritative.

This sub-capability and its fenced single-owner lease are Contract Verified. Lease possession grants only lifecycle-transition authority already bounded by the retained WorkItem. It does not resolve the RunRecord reference, connect a production result-message path, retry or create a second AgentRun, cancel/pause/resume, execute a worker, fence an external effect, coordinate processes, define distributed clock-skew handling, migrate beyond schema v1, retain history, or claim parent-directory power-loss durability.

### Gate 8 Durable Queue-To-AgentRun Dispatch

`DurableAgentRunDispatcher` is the first Integrated connection between the durable queue and durable Goal/AgentRun lifecycle. It validates caller-controlled Goal, AgentRun, owner, and duration metadata before queue mutation; selects an already-active WorkItem or durably claims the next ready WorkItem; creates a missing Goal from that exact WorkItem or recovers a matching Goal; and advances only the missing `ACCEPTED -> PLANNING -> READY -> EXECUTING` prefix before returning an immutable `AgentRunDispatch`.

The queue and runtime stores remain separate atomic artifacts. Queue claim occurs first, so a claim failure creates no runtime state. A later runtime-store failure intentionally leaves the queue item active and retains any durable runtime prefix. Re-entry with the same WorkItem, Goal, AgentRun, and current owner resumes that prefix; an existing unexpired same-owner lease is returned without renewal or revision, while mismatched WorkItem, AgentRun, owner, or post-execution state fails closed. Runtime recovery checks exact WorkItem equality before expiry reclamation, so mismatched state is not mutated.

The filesystem integration recovers both real stores: queue recovery requeues the interrupted active item, the dispatcher claims that same admission-ordered WorkItem again, and runtime recovery returns the exact existing Unicode-bearing unexpired lease. The path does not complete the queue, invoke a Tool or worker, consume a result message, record or fence an external effect, add retry, or claim cross-store atomicity.

### Gate 8 Connection Sequence And Completion Boundary

Fence-checked execution completion and Scheduler queue completion are different facts. `DurableAgentRuntime.completeExecution` persists `EXECUTING -> AWAITING_VERIFICATION` and releases the lease; it proves that the current fenced owner finished its execution phase, not that independent verification passed. The runtime transition therefore MUST NOT directly invoke queue completion or be described as verified completion, logical completion, or dependency satisfaction.

The Scheduler queue now records a terminal `WorkItemDisposition` (`VERIFIED_COMPLETED` or `FAILED`), and only `VERIFIED_COMPLETED.satisfiesDependencies()` is true. The queue's single completion operation is split: `completeActiveVerified` adds the WorkItem to `completedWorkItemIds`, the dependency-satisfaction source used to release dependent work, while `failActive` adds it to a separate `failedWorkItemIds` set that never satisfies dependents, so a failed dependency leaves dependents blocked with an inspectable cause held in the runtime/RunRecord. The schema-v1 state partition is `pending + active + verified + failed = admissionOrder`, with verified and failed disjoint; the queue stores disposition only, not a failure reason.

Under the current schema-v1 contracts, a queue item remains active while its AgentRun is `AWAITING_VERIFICATION`, and each terminal disposition releases the single active slot at the terminal point. Releasing the slot earlier would require a separately accepted durable waiting state that remains outside `completedWorkItemIds`. The disposition persists in the schema-v1 on-disk format (revised in place, no version bump) with exact restart recovery: a persisted terminal disposition is never re-run, only interrupted active work is requeued, and because the envelope rejects trailing bytes any pre-existing local snapshot fails closed on read.

`DurableAgentRunFinalizer` now connects those separate facts. It composes the durable queue, `AgentRuntimeStateStore`, and `RunRecordStore` and drives one recoverable, idempotent order: resolve the RunRecord by reference, bind it to the Goal on `approvedTask.taskId()` and `sourceDocument()` (no source SHA exists), persist the runtime terminal state through `recordResult`, then record the matching queue disposition. The disposition is derived from the runtime terminal status (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`), never re-derived from the RunRecord, so the two stores cannot disagree. The finalizer resolves but never persists the RunRecord (persist is non-idempotent), fails closed on a missing or corrupt record while leaving the run `AWAITING_VERIFICATION`, and rejects both a RunRecord bound to a different task and a re-finalize that supplies a different reference for an already-terminal run. Because the durable queue's recovery contract requeues an interrupted active WorkItem to pending, the finalizer re-claims that requeued item before recording the disposition. `finalizeAgentRun(goalId, agentRunId, runRecordReference)` drives the forward path; `recoverFinalization(goalId)` is autonomous post-terminal recovery that applies only the queue disposition from an already-terminal runtime and needs no reference. Queue and runtime remain separate durable boundaries with no cross-store transaction; only the pre-terminal window depends on the connection-3 worker/driver to re-supply the same reference.

`DurableAgentRunWorker` is the first driver of that whole sequence: connection 3 is split into sub-increments 3a (in-process worker), 3b (process lifecycle), 3c (a concrete `MessageTransport` local IPC adapter), and 3d (process-isolated `AgentRunExecution`). One `runOneCycle(leaseDuration)` call drives a single scheduling cycle in the authoritative order cycle-intent (ids) -> queue claim + lease -> RunRecord persisted (ref) -> intent updated with ref -> execution-artifact cleanup -> `completeExecution` -> `finalizeAgentRun` -> queue disposition -> clear intent, returning the cycle's `WorkItemDisposition` or empty when nothing was claimable. The worker owns a fourth durable store, the single-record `PendingFinalizationStore` cycle-intent checkpoint (bounded, strict-UTF-8, digest-checked, atomically published, fail-closed), written before the queue claim so a restarted worker re-supplies the same caller-owned Goal/AgentRun identities and the dispatcher's idempotent `recoverMatching` resumes the exact prefix — no second Goal, no orphaned runtime state, no dispatcher change. The `runRecordReference` is persisted into the intent before cleanup and `completeExecution`, closing the pre-terminal window the finalizer deferred and making cleanup failure recoverable without re-execution. Recovery routes by the runtime state as the source of truth: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; `EXECUTING`/`READY`/`PLANNING`, an unstarted AgentRun, or a missing runtime (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference already exists. Execution happens through the injected `AgentRunExecution` port (`execute(dispatch) -> runRecordReference`) and its default-no-op, idempotent post-checkpoint cleanup. Failures fail closed with the intent retained; a cycle that claimed nothing leaves no durable trace; re-execution before reference checkpointing can orphan the earlier RunRecord as an accepted at-least-once consequence. The dispatcher and finalizer handed to the worker must wrap the same queue instance, because the queue's in-memory revision advances with each persisted mutation and a stale second instance fails the store's exactly-one revision advance. `processIsolated` is the named production composition over the real child launcher and per-cycle work/result spools; it adds no supported Scheduler entry point.

`AgentLoopAgentRunExecution` is the first production implementation of that port: it drives the Integrated Gate 1-4 pipeline (governed `read-file` `ToolExecutor`, `EvidenceRecorder`-persisted evidence, the bounded `AgentRunController`/`AgentLoop`, `DeterministicReadFileVerifier`, and the application `AgentRunFinalizer`) against the approved task's own source document — the `read-file` target is `taskRevision().sourceDocument()` and the expected content SHA-256 is `taskRevision().sourceSha256()` — and returns the persisted `run-record/<uuid>` reference. The `ApprovedTask` is constructed directly from the WorkItem's fields (no `ApprovedTaskReader`, no `In Progress` coupling), so the runtime finalizer's taskId-plus-sourceDocument binding holds by construction; the port must persist through the same `RunRecordStore` the worker's finalizer resolves from. A digest mismatch or Tool failure is carried in the persisted RunRecord (non-`VERIFIED`, finalized to the `FAILED` disposition), never thrown, and is real drift detection: the target changed between approval and execution. The derivation of `(targetPath, expectedContentSha256)` from the WorkItem sits behind one private seam.

`WorkPayload` now carries an optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)`: the port's seam prefers the declared input and falls back to the approved task's own source document when it is absent, so a WorkItem can execute an arbitrary governed target through the same contained read-file, evidence, verification, and RunRecord pipeline while the `ApprovedTask` binding stays the source document (exactly as the CLI separates `CURRENT_TASK.md` from `target-path`). The input is explicit caller authority data supplied through a `WorkMessagePublisher` overload — snapshot observations are evidence, not approval authority, so they never derive it. Both filesystem serializers persist the optional input after `allowedTools` with a presence flag, schema v1 revised in place; pre-existing snapshots without the field fail closed. Multiple inputs, payload-carried plans or Tool-call scripts, and write Tools remain out of scope.

`RuntimeControlAdmissionHandler` is the bounded Gate 7-to-Gate 8 request connection for control envelopes. It recovers one named Goal and records an exact `ControlPayload` envelope only while that Goal and its AgentRun are active, after matching logical run, correlation, and work-message causation and rejecting runtime-identity collisions. `AgentRuntimeState` retains at most 256 requests in admission order; exact message replay is a no-revision duplicate, identity reuse with different content fails closed, and every later lifecycle state retains the exact ledger prefix. `FileSystemAgentRuntimeStateStore` encodes the full envelopes, requires the ledger to stay prefix-monotonic on update, and publishes the new revision atomically before the handler returns. Checked storage failure becomes handler failure so the existing bus retry/dead-letter contract remains visible. The schema-v1 payload is revised in place and an older payload without the ledger fails closed.

This boundary records an untrusted request, not an accepted transition. The envelope producer and control reason are diagnostic provenance and cannot pause, resume, cancel, release a lease, mutate the queue, interrupt a worker, expand Tool scope, or change bus cancellation. Gate 12 must authenticate and authorize a later application path before any of those state changes can exist.

Which connections exist today is stated in `PROJECT_STATE.md`; the cross-boundary connection sequence remains dependency ordered:

| Order | Connection | Owning boundary | Required durable ordering |
|---|---|---|---|
| 1 | terminal queue disposition | Gate 8 Scheduler | distinguish verified completion from failure before changing the dependency-satisfaction set |
| 2 | RunRecord-backed result finalization | Gate 7 result delivery + Gate 8 runtime | durable RunRecord resolution -> matching `ResultPayload` -> persisted AgentRun/Goal terminal state -> matching queue disposition |
| 3 | process-isolated worker and local IPC | Gate 7 transport + Gate 8 worker runtime + Gate 11 Tool controls | worker cycle-intent persists before the claim; the RunRecord reference persists before spool cleanup and acknowledgement; exact work/result route and record binding fail closed; transport acceptance never means bus delivery or work completion |
| 4 | durable cancel/pause/resume | Gate 7 control delivery + Gate 8 request state + Gate 12 authenticated application | persist the bound request before handler success; later application must persist accepted control state before exposure and cannot create scope or authority |
| 5 | external-effect ledger | Gate 8 Scheduler, with the owning Tool/adapter gate | fence-check and idempotently record applied, deduplicated, compensated, or user-recovery outcomes |
| 6 | retry and replacement AgentRuns | Gate 8 Scheduler | preserve terminal history and create a new immutable AgentRun under bounded policy rather than rewriting a failed run |
| 7 | typed handoff and multi-agent execution | Gate 13 over Gates 7 and 8 | require an Operational single-agent baseline, isolated ownership, deterministic synthesis, and one Kernel terminal-state coordinator |

Each cross-store step persists its earlier authoritative artifact before the later derived artifact. Recovery re-enters idempotently from the durable prefix; it does not claim an atomic transaction. Authenticated control application, effect ledger, retry policy, and handoff coordination remain unimplemented until their own bounded tasks and fresh integration evidence exist.

### Gate 8 Scheduler Delivery Semantics

The Scheduler provides at-least-once work delivery; it does not claim universal exactly-once execution across arbitrary Tools or external systems. Near-exactly-once user-visible behavior is composed from a stable idempotency key per logical work/effect, durable state transitions, fenced leases that reject stale owners, checkpointed recovery, versioned state migration, explicit orphan detection and reclamation, and replay-safe or compensatable external effects.

A worker may repeat after timeout, crash, lost acknowledgement, or lease expiry. The current fence token must accompany every state write and effect commit, stale tokens must fail closed, and the durable result must record whether an effect was applied, deduplicated, compensated, or left for user recovery. Priority and fairness cannot bypass dependency readiness, authority, data classification, cost/time budgets, or cancellation. Recovery must be testable across process restart and supported schema versions before the Scheduler is Operational.

### Completion Semantics

`Completion` names three distinct lifecycle facts. They were implemented in separate increments, and conflating them is the one modelling error this area has actually produced, so they are stated separately here:

1. **Worker execution completion:** the fenced owner has stopped executing and the runtime moves to `AWAITING_VERIFICATION`.
2. **Verified runtime completion:** an independently supported `ResultPayload` completes or fails the AgentRun and Goal.
3. **Scheduler queue completion:** the WorkItem enters `completedWorkItemIds`, releases the active slot, and allows dependent work to become ready.

Each contract is internally consistent; the failure mode is connecting fact 1 directly to fact 3 without re-checking fact 2. Three interpretations are therefore rejected outright:

- `EXECUTING -> AWAITING_VERIFICATION` must not call `completeActive`.
- A worker acknowledgement must not add a WorkItem to the dependency-satisfaction set.
- Releasing capacity must not be represented as successful completion merely because a queue happens to have only pending, active, and completed states.

The conflict was possible because the compact `.ai/architecture.md` described current contracts without an ordered connection backlog, so it could not expose a missing middle transition. A contract description that does not state what it connects to is incomplete.

### Queue Capacity During Verification: Accepted And Rejected Options

**Option A — keep the queue item active through verification.** Accepted, and in force. It is the smallest change consistent with the schema-v1 queue and single-worker design, preserves Verified-only completion without a new intermediate queue state, keeps crash recovery and cross-store ordering provable, and prevents another WorkItem from starting while the current result is unresolved. Its cost is real: verification latency occupies the single Scheduler slot, and a slow or unavailable verifier blocks unrelated ready work in that queue.

**Option B — add a non-terminal awaiting-verification queue state.** Deferred, not rejected. It releases the execution slot while verification proceeds and permits another independent WorkItem to execute without falsely satisfying dependencies. It requires a durable queue-state and schema change with recovery rules for the waiting set, separate execution and verification capacity limits and backpressure, ordering and fairness between the two stages, cancellation/timeout/restart/orphan behaviour for both, and an explicit rule that waiting work stays outside the dependency-satisfaction set. Reconsider only once the terminal-disposition and result paths are Contract Verified and verification throughput is a demonstrated bottleneck.

**Option C — mark the queue completed at execution acknowledgement.** Rejected. It is simple and releases capacity, but it lets dependent work start before independent verification and makes a worker receipt equivalent to completion authority, which conflicts with the Constitution-backed verification model, Gate 3/4 behaviour, the runtime AgentRun states, and the Gate 8 Verified-only terminal contract.

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

## Default Product Security Model

Repository instructions, source comments, Tool and terminal output, model responses, MCP content, plugins, dependencies, generated artifacts, and remote service responses are untrusted data. They may supply evidence or propose work, but they cannot grant authority, change policy, approve an action, or override the Constitution and active task.

The shared security baseline requires:

- provenance, freshness, content bounds, and data/instruction separation at every ingress;
- secret and sensitive-data detection before persistence, display, logging, caching, or external transmission;
- an explicit outbound-data policy keyed by data classification, destination, purpose, user authority, and retention;
- least-privilege Tool scope, project-root containment, command and changed-file preview, dry-run when the Tool can support it, bounded execution, audit evidence, and a named recovery path;
- MCP and model adapters that preserve source attribution, isolate provider instructions from authority, validate responses, and make fallback or cache use visible and policy-scoped;
- plugin and Skill permission manifests, integrity/signature provenance, compatibility and dependency checks, isolation, malicious-package review, disablement, removal, and rollback;
- local-only operation as a complete mode, with cloud synchronization opt-in and unable to grant execution authority.

Enforcement remains with the owning gates: Gate 8 owns runtime isolation and replay safety; Gate 9 owns model/MCP classification, redaction, outbound policy, attribution, fallback, and cache controls; Gate 11 owns Tool and extension supply-chain controls; Gate 12 owns previews, approval UX, and audit visibility; Gate 14 owns cloud encryption, keys, exclusion, and conflict recovery; Gate 16 owns signed reproducible release artifacts, SBOM, installation, update, migration, offline use, and rollback evidence.

## Shared Application API And Change-Centered UX

CLI, VS Code, Desktop, Web, and external API clients consume the same application contracts for Run creation and inspection, approvals, verification, evidence, control commands, and recovery. Interface adapters may change presentation and interaction, but they cannot duplicate or reinterpret runtime policy.

The implementation order is shared application API first, CLI as the reference surface, VS Code second for repository-context work, and Desktop later as a supervisory view across runs and projects. Web and other clients follow the same contracts rather than creating another orchestration path.

The primary user projection is a change review, not the internal Agent topology. One review presents the goal and plan, changed files and bounded diff, tests and verification evidence, source provenance, risks and unresolved questions, budget/cost/time, approval points, recovery or rollback state, and commit readiness. Internal messages, workers, and retries remain inspectable diagnostics but do not replace this user-facing explanation.

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

`RepositoryTaskPlanner` returns an `Optional<TaskProposal>`: empty when an active task must be preserved, otherwise one immutable proposal of title, reason, scope, acceptance criteria, out-of-scope items, risks, and a `ProposalState`. That enum has exactly one constant, `PROPOSAL`, which is the point — a proposal cannot represent itself as accepted, and promotion to an Active Task remains a human decision recorded in `CURRENT_TASK.md` rather than a state the Planner can reach. `PlanningException` reports a Roadmap the planner cannot read, such as no `Specified - Next` gate or more than one; it fails closed rather than guessing which gate is next.

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

Policy denial, unknown Tool, cancellation, timeout, malformed arguments, path escape, missing file, size overflow, invalid UTF-8, and unexpected Tool exceptions are represented as bounded failure `ToolResult` values. The Gate 1 boundary itself does not persist full evidence and does not authorize mutation; Gate 2 adds only evidence-root writes.

The evidence and RunRecord roots are explicit caller inputs, by the Gate 5 decision that every run input is stated rather than inferred. They are deliberately **not** confined to the project root: a caller may place either store anywhere it can write, and `.enhancer/` is the example layout the README uses, not an enforced property. What the stores do guarantee is narrower and worth stating exactly — each normalizes its root, refuses a root that is a symbolic link rather than a real directory (`NOFOLLOW_LINKS`), and only ever creates freshly generated UUID-named entries, so a store can add files to a caller-named directory but cannot overwrite or delete anything already there. Read-side containment is a separate and stricter boundary: the run target is real-path checked against the project root and a path escape is an error.

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

### Runtime Text And File Resource Boundaries

All production prefix/suffix truncation that can reach persisted evidence, Tool diagnostics, CLI output, or bounded Workspace reasons uses one UTF-16-aware boundary that never returns half of a surrogate pair. Configured limits remain expressed in Java string code units; when an exact boundary would split a supplementary character, the returned text is one code unit shorter than the maximum. Complete-content evidence digests remain computed over the untruncated valid input.

Governed file reads and hashes enforce their byte ceilings while consuming the stream, not only through a preceding `Files.size` observation. The common operation allocates no more than the accepted read ceiling and reads at most one additional byte to detect growth. `ReadFileTool`, repository startup documents, target-file hashing, and Evidence, RunRecord, and Scheduler queue artifact resolution retain their existing configured ceilings and strict UTF-8 or integrity behavior. A target that grows past its bound becomes explicitly Unavailable; the other boundaries fail through their existing checked or typed failure paths.

This correction closes Unicode-boundary and mutable-file TOCTOU resource defects. It does not make an interrupt-ignoring in-process Tool terminable or add parent-directory synchronization after atomic persistence. Long-running Tool execution requires process isolation in addition to the finite containment contract below before Scheduler workers are Operational. The package cycle identified alongside these defects is closed by the separately verified neutral lifecycle and application-finalizer extraction below. Atomic move prevents partial visible artifacts during ordinary restart but is not a claim of storage-device or power-loss durability.

### In-Process Tool Isolation Capacity

ToolExecutor uses one process-wide capacity of 64 live isolated workers shared across its default instances. Policy, registration, and pre-invocation cancellation checks occur before capacity consumption. Each admitted invocation acquires one slot before its daemon worker starts, and the slot is released only from that worker thread's actual termination path. Timeout, interrupt, executor close, or `shutdownNow` does not release accounting while Tool code continues to ignore interruption.

Capacity exhaustion refuses the invocation before thread creation with typed `ISOLATION_CAPACITY_EXHAUSTED` failure evidence. The standard failure classifier treats it as terminal rather than retrying into a saturated process. Existing invocation isolation remains: below the ceiling, one timed-out worker does not starve a separate next invocation.

This is finite containment, not termination or recovery. Permanently stuck workers hold capacity until process restart, and a saturated process cannot run more Tools. Gate 8 long-running workers still require a process boundary, OS-enforced termination, Scheduler admission/backpressure integration, and operator-visible recovery before Tool execution can be called Operational in that environment.

### Runtime Package Dependency Direction

The verified runtime packages form an acyclic source dependency graph. Neutral verification lifecycle values (`VerificationDecision`, `VerificationStatus`, and `VerificationCode`) live in `com.enhancer.kernel`. Worker state and approved-task contracts remain in `com.enhancer.loop`; verification implementations may depend on loop and kernel; RunRecord persistence may depend on loop and kernel but not verification implementations. `AgentRunFinalizer` lives in `com.enhancer.application`, the composition layer allowed to depend on loop, verification, and run persistence.

The verified direction is:

```text
application -> run, verification, loop, kernel
run         -> loop, kernel
verification-> loop, kernel
loop        -> kernel
kernel      -> none of the above
```

`VerifiedAgentRunTransition` is the explicit application-facing port to the package-private in-memory completion transition. It validates the same AWAITING_VERIFICATION and Verified-decision invariants; durable completion still exists only after application finalization persists the unchanged RunRecord schema. A source-structure regression test forbids loop-to-run, loop-to-verification, run-to-verification, and inward kernel imports. The project remains one Gradle module; ApprovedTask relocation, persistence SPI extraction, and physical module separation remain future work.

### Delivery Gate 5 CLI Boundary

Gate 5 selects `com.enhancer.cli.EnhancerCli` as the first supported local entry point and exposes only `run` and `replay`. The Gradle application entry point is a thin composition boundary over the existing Context Reader, repository-derived `ApprovedTask`, read-only Tool execution, sequential verifier, finalizer, Evidence Store, and RunRecord Store.

The implemented command is intentionally non-interactive. Every final worker outcome that reaches finalization is persisted before its stable exit code is returned; configuration errors and internal failures remain bounded diagnostics rather than fabricated records. This is the first Operational scenario, not the future multi-interface CLI of Gate 12.

`run` requires explicit project root, task identity, relative target path, expected SHA-256 digest, evidence root, and RunRecord root. The supplied task identity must equal the active task read from the governed project context; it does not create or approve a task. The command registers only `read-file`, applies the existing 64 MiB ceiling and a five-second timeout, and performs at most five loop iterations with the existing three-transition stagnation threshold.

Process results use stable exit codes for completed, usage/configuration error, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure. Diagnostics are bounded and never include complete target content. A finalized run prints its opaque RunRecord reference and storage root.

`replay` requires an explicit RunRecord root and reference, resolves the integrity-checked record through `FileSystemRunRecordStore`, and prints bounded typed task, request, policy, verification, and stop metadata. It does not re-execute the Tool or reinterpret the record as repository authority.

## Constitution Kernel Architecture

### Development Session Recovery Boundary

Development-session recovery is separate from product Agent Runtime recovery. Canonical
documents retain their existing ownership: `CURRENT_TASK.md` owns approved work and the
next task, `PROJECT_STATE.md` owns current maturity, `docs/verification-log.md` owns
promoted verification evidence, Git owns the diff and delivery history, and
`SESSION_HANDOFF.md` owns only current facts that would otherwise disappear. A session
checkpoint cannot promote or replace any of them.

One machine-written checkpoint below `.enhancer/session-checkpoint/` records only the
execution position of the repository's current development session. It binds a generated
run identity to the active task identity and a SHA-256 revision of the task's contract
sections; status, verification, and next-task sections are deliberately excluded so
normal lifecycle synchronization does not change the approved-scope identity. The state
retains a monotonic revision, typed pending/succeeded/failed/stable position, current and
last-successful step, next action, bounded evidence references, and a bounded manifest of
relative artifact paths with presence and content identity.

The filesystem adapter publishes a bounded strict-UTF-8 integrity envelope by atomic
replacement and fails closed on corruption, unsupported schema, symbolic-link storage
boundaries, task drift, stale revision, or a different run. The expected revision is the
single-writer fence. Clearing is permitted only after the checkpoint is stable and its
recorded artifact manifest still matches the working tree. A checkpoint is recovery
metadata, never verification or completion evidence; resume still reads canonical
documents, inspects Git state, and runs fresh applicable verification.

The existing local CLI exposes start, record, show, and clear operations. Session rules
write intent before mutation or verification, write outcome afterward, and clear only
after orderly verification and document synchronization. The first contract supports one
active development session per repository and adds no timers, platform shutdown hook,
multi-session merge, automatic commit/stash, remote replication, or external-effect
deduplication.

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
