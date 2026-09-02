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

The Gate 7 envelope contract carries references and bounded requirement data only, under `com.enhancer.bus`. `MessageEnvelope` retains the legacy `message-envelope-v1` public version marker and carries a canonical-UUID message identity, a bounded correlation identity, an optional canonical-UUID causation identity that must differ from the message identity, bounded logical-run and producer identities, an occurrence time, and one typed payload.

`MessagePayload` is sealed to exactly five kinds. The legacy work payload carries the approved task revision, a valid Workspace snapshot identity, and an immutable allowed-tool scope of 1 through 256 unique names, each bounded to 256 characters; the model-work payload carries the same revision, snapshot, and bounded Tool scope plus one mandatory bounded target path, expected-response SHA-256, and exact complete `ModelExecutionProfile`; the result payload carries the task identity, a run-record reference, and the verification status; the control payload carries a typed cancel/pause/resume signal with a bounded reason; the handoff payload carries the task revision, snapshot identity, and run-record reference. The model profile remains untrusted requirement data and no payload carries a separate authoritative capability. Possessing an envelope grants nothing, and delivery code must validate contents against repository authority rather than trust the sender.

Together with the per-name ceiling, the explicit allowed-tool cardinality ceiling gives both Tool-scope-bearing payloads a finite aggregate tool-name ceiling of 65,536 characters. The model-work target, digest, nested token/cost/time budgets, and profile labels reuse their existing bounded constructors, so payload data remains bounded or represented by evidence references.

The contract is consumed by both the deterministic in-process topic and queue delivery surface and the transport-neutral IPC boundary below. ModelWork production and durable-runtime integration remains deferred.

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

The wire format belongs to `MessageEnvelopeCodec`, not the adapter: the frame remains `[magic][bodyLength][sha-256 of body][body]` with length-prefixed strict UTF-8 strings. Work, Result, Control, and Handoff retain byte-for-byte transport-spool v1 plus message-envelope v1 encoding. Model work alone selects transport-spool v2 plus message-envelope v2, an explicit `MODEL_WORK`/`model-work-payload-v1` discriminator, canonical lexicographic Tool order, and every profile component in RFC-0014 constructor order. Decode selects the inseparable codec/envelope family first, rejects cross-family kinds, duplicates, noncanonical order, unknown versions/enums, invalid nested values, bad magic or lengths, digest mismatch, malformed UTF-8, trailing bytes, and every envelope invariant violation, and reports `CorruptedSpooledMessageException` — distinct from a plain `IOException` because corrupt stays corrupt while a filesystem condition may be transient. Occurrence time remains epoch-second plus nanosecond, and the frame contains no wall-clock or random state. Literal legacy-frame and detached-cancellation golden tests keep `MessageEnvelope.ENVELOPE_VERSION` and all cancellation canonical bytes unchanged.

The adapter owns publication only: a temporary file published by atomic move into its own freshly generated name, so resending an envelope never overwrites an earlier hop and a reader never observes a partial message.

#### Durable Work Spool Point Receiver

`DurableWorkMessageReceiver` is the bounded receiving connection between the local spool,
the real `InProcessMessageBus`, and durable Scheduler admission. The supported
`scheduler-receive-work` takes one caller-named canonical `.transport` filename under an
explicit spool root and resolves exactly one regular non-symbolic pending point or its
deterministic same-root `.received` point. It decodes the unchanged route and envelope,
requires an exact expected queue destination and `WorkPayload`, and publishes it to one
queue subscription backed by `DurableWorkItemAdmissionHandler`.

Success is reported only after that handler reaches the existing durable queue.
`ADMITTED` means the queue revision advanced; `REPLAYED` means the exact derived WorkItem
was already present and no revision changed. A reused message identity with changed
content fails closed. After successful durable admission, a pending point moves by
same-directory `ATOMIC_MOVE`, without replacement or fallback, to `.received`; an
already-acknowledged point repeats exact admission and performs no second move. Output
separates `ADMITTED`/`REPLAYED` from `ACKNOWLEDGED`/`ALREADY_ACKNOWLEDGED`.
Pre-admission failure leaves pending evidence, acknowledged re-entry is revision-free,
and `.received` no longer consumes the transport's pending-capacity count.

The receiver does not scan, order, dead-letter, or automatically delete spool files,
create a queue, execute work, or add a durable bus journal. Execution remains a separate
`scheduler-cycle`, `scheduler-drain`, or `scheduler-service` invocation. Acknowledged
evidence remains retained; automatic cleanup, a global retention bound, directory
consumption, and durable bus journal/subscription recovery remain separate unimplemented
contracts.

#### Durable Control Spool Point Receiver

The bounded Control connection reuses the local transport point, real
`InProcessMessageBus`, and existing persist-first `RuntimeControlAdmissionHandler`
without applying a control signal. A separate `scheduler-receive-control` command takes
one explicit canonical pending `.transport` filename, its spool root, an exact queue
destination, the runtime-state root, and one Goal identity. It resolves exactly one
regular non-symbolic pending point or deterministic same-directory `.received` point,
decodes the unchanged envelope, and rejects a foreign destination or non-Control payload
before runtime mutation.

The receiver publishes through one fresh queue subscription and reports success only
after the exact request is durable in the Goal ledger. A new request advances the runtime
revision; exact replay is revision-free, while identity reuse with changed content fails
closed. Only after successful admission does a pending point move by same-directory
atomic rename without replacement. Acknowledged re-entry repeats decode, binding, and
durable replay checks rather than trusting the suffix.

The supported receiver optionally composes runtime-event publication through an
all-or-none `--runtime-event-root`, `--runtime-event-publication-root`, and
`--max-pending-runtime-event-publications` group. Omitting all three preserves the
request-only construction. Supplying all three constructs
`FileSystemRuntimeEventStore` -> `RuntimeEventRecorder` ->
`FileSystemRuntimeEventPublisher` and passes the recorder through
`DurableControlMessageReceiver` to the existing event-aware handler. Partial groups or
capacity outside 1 through 4096 fail during argument parsing before point resolution.

For `CANCEL`, the supported order is Control request persistence -> event append/exact
replay -> opaque reference-point publication -> Control spool acknowledgement. Event or
publisher failure leaves the earlier durable prefix and the transport point
unacknowledged for exact re-entry with the same explicit roots. `PAUSE`/`RESUME` remain
request-only, the existing output makes no event-delivery claim, and root creation stays
lazy until a `CANCEL` reaches event recording. Consumer semantics, root migration,
cleanup/retention, and authenticated application remain separate.

This boundary records untrusted intent only. It does not authenticate or apply cancel,
pause, or resume; call bus cancellation; reclaim a lease; interrupt a worker; mutate a
Scheduler queue; scan or delete spool files; create a durable bus journal; or add
retention policy. Gate 12 remains the authenticated application owner.

#### Authenticated Cancellation Application

The first Gate 12 application boundary is `AuthenticatedCancellationApplication` over
one exact retained `CANCEL` request. A trusted `ControlRequestAuthorizer` port receives
the already canonical application Goal plus that retained envelope and returns a typed
approved or denied result. Supplying the Goal before authorization is required because
the envelope carries no Goal and an audit-backed authorizer must verify the target
before persistence. No legacy envelope-only authorization path exists. Approval binds a
canonical authorization identity, bounded actor, exact Goal and Control-message
identities, `CANCEL`, and authorization time. Envelope producer/reason, transport
acceptance, and durable admission remain diagnostic or recovery facts and cannot create
approval.

AgentRuntime schema v5 retains at most one immutable
`CancellationApplicationRecord`. First application atomically persists the approved
record with Goal `ACTIVE|RETRY_PENDING -> CANCELLED`; a current non-terminal AgentRun
also becomes `CANCELLED` and releases its lease, while an already failed latest attempt
remains failed. This terminal runtime state refuses later lifecycle, lease, result,
retry, and Control-request transitions. Exact replay resolves the retained record before
authorizer invocation and changes no revision. It does not signal or kill a process,
call Message Bus cancellation, dispose the Scheduler queue, cancel a Tool or external
effect, or define pause/resume.

After the record-bearing revision is durable, event-aware application derives
`CANCELLATION_APPLIED` at the retained application time with exact Work/Goal/AgentRun
binding, Control-message causation, producer
`authenticated-cancellation-application`, and stable `CONTROL_MESSAGE` plus
`CONTROL_APPLICATION` references. Retained-record replay repairs event append or
publication without reauthorization or another runtime revision. Credential issuance,
supported interface composition, concrete event transport, queue disposition, and
runtime schema v1-v3 migration remain separate.

The first supported composition is the authorizer-injected filesystem application
surface, not a self-authorizing CLI. `FileSystemAuthenticatedCancellationApplication`
accepts an explicit runtime-state root, injected clock, and mandatory trusted
`ControlRequestAuthorizer`, then delegates the exact Goal and retained Control-message
identities to the unchanged transition owner. Its event-free construction adds no event
artifact. Its event-aware construction requires one
`FileSystemRuntimeEventPublicationConfiguration`, which groups both explicit roots and
the bounded pending capacity and composes `FileSystemRuntimeEventStore` ->
`RuntimeEventRecorder` -> `FileSystemRuntimeEventPublisher` as one all-or-none value.

The durable order stays retained request -> trusted authorization -> terminal runtime
revision -> event append/exact replay -> opaque point publication/exact replay. Source
persistence failure reaches no event; a later event or publication failure re-enters
from the retained cancellation record without reauthorization or another runtime
revision. This facade remains under `com.enhancer.runtime` because that package already
owns the transition and adapters, while the runtime has an existing dependency on the
application package through Agent Loop execution; placing the facade in application
would create a source cycle. The future Gate 12 CLI/API/editor/Desktop adapter is the
named downstream consumer and must supply an authorizer from its own authenticated
composition root. No actor, authorization UUID, envelope metadata, CLI field, default
authorizer, credential, queue disposition, process signal, Tool/effect cancellation, or
`PAUSE`/`RESUME` authority enters through this surface.

The specified first authenticated-interface composition uses a short-lived detached
signed exact-request grant. The grant and its path remain untrusted input. A separately
provisioned operator-owned trust policy supplies the audience/trust domain, accepted
issuer and subject/action scope, public verification keys and fingerprints, fixed
algorithms, configuration and policy revisions, maximum lifetime, bounded clock skew,
key validity, and revocation effective facts. The same invocation, repository,
retained envelope, ambient username/environment, or proof cannot supply or replace that
authority. Enhancer neither issues nor retains the private signing key, password,
bearer/session token, raw proof, or signature.

Versioned domain-separated canonical signed bytes bind the intended trust domain,
canonical Goal, retained Control message, authorization, issuer, key, and issuer-scoped
subject identities, explicit `CANCEL`, issued and expiry times, policy revision, and a
SHA-256 digest of a deterministic length-framed projection of every retained envelope
and Control-payload field. The verified issuer/subject deterministically supplies the
actor and the signed issue time supplies `authorizedAt`; an injected clock supplies a
separate verification observation. Caller actor/authorization fields, producer,
reason, OS identity, unsigned files, repository content, ownership, or possession can
never create approval.

The authorization-specific immutable `CancellationAuthorizationAuditRecord`,
deterministically keyed by `authorizationId` through
`FileSystemCancellationAuthorizationAuditStore`, persists before `Approved`. Its
integrity envelope holds normalized
non-secret claims, request/proof digests, issuer/subject/actor, key ID/fingerprint and
fixed algorithm, issue/expiry/verification times, trust and policy revisions,
revocation evidence, and verifier version. Exact replay is revision-free; changed ID
reuse, corruption, or audit persistence failure fails closed. The random-reference
generic Evidence Store is not this authorization authority or replay index.

`AuditBackedSignedCancellationAuthorizer` is the reusable concrete authorizer. It
strictly parses a bounded canonical proof, verifies Ed25519 against an immutable
injected public-only `CancellationGrantTrustPolicy`, revalidates target, complete
request digest, time, lifetime, subject, key validity, and revocation on every
pre-runtime attempt, and persists or exact-resolves the audit before returning
approval. It accepts no private key or trust override and supplies no trust-policy
loader or proof producer.

The first trust-loader prerequisite is
`PinnedFileCancellationGrantTrustPolicyLoader`. Its absolute normalized policy-file
path and lowercase SHA-256 pin over the complete exact file bytes are immutable trusted
construction inputs supplied outside request parsing. The future proof command cannot
accept, discover, infer, or override either input from CLI fields, environment/JVM
properties, current directory, project repository, retained request, proof, or ambient
identity. The loader itself is not the installed pin source; a later interface
composition must bind it to protected immutable deployment metadata.

The loader performs one bounded no-follow read of an existing exact-real regular non-
symbolic file, checks the injected digest over those bytes, and parses that same snapshot
without reopening, writing, repairing, scanning, fallback, or caching. Its strict
canonical UTF-8 v1 line format fixes field order and contains configuration identity,
audience, policy revision, lifetime/skew seconds, and sorted Ed25519 key/subject entries
with X.509 SubjectPublicKeyInfo bytes, fingerprint, validity, and optional revocation.
Exact internal re-encoding rejects alternate spellings, CR/comments/blank/unknown lines,
duplicates, and trailing content. The verified file digest becomes the immutable policy
`configurationRevision`; `policyRevision` remains the signed compatibility boundary.
No private material, writer, arbitrary algorithm, or permissive default exists.

An independently protected pin, not portable owner/ACL inspection, is the approval
anchor. Modification of the public file can then only preserve the exact pinned policy
or cause denial. Deployment should still protect file and ancestor writability, but
provisioning, permission mutation, rotation, installed pin replacement, and application-
version anti-rollback are separate operations. Each future cancellation command loads a
fresh snapshot before authorizer construction, so audit-only retry observes current
pinned key/revocation facts while durable runtime replay remains unchanged.

The extended order is retained request -> current proof/trust/time/revocation
validation -> authorization audit persist/exact replay -> approved terminal runtime
revision -> event append/exact replay -> point publication/exact replay. There is no
cross-store transaction. An audit-only prefix is not evergreen approval: while runtime
is absent, retry needs the identical transient proof and current validation. A durable
runtime record remains terminal historical truth and keeps its existing authorization-
bypassing suffix recovery after expiry, rotation, or revocation; missing audit is
reported as degraded rather than fabricated from the smaller runtime record.

The first production consumer is `scheduler-apply-cancel`. It accepts runtime and audit
storage roots, canonical Goal and retained Control-message identities, and one proof
file, plus the existing optional all-or-none event publication group. It cannot accept
trust path/pin/metadata, clock, issuer/key/actor, credential, or approval overrides.
Production derives a sole installation anchor from the exact-real non-symbolic JAR
CodeSource for `EnhancerCli`, then reads only its fixed sibling
`enhancer-cancellation-trust-metadata-v1`. That strict bounded canonical UTF-8 point
contains the absolute normalized policy path and lowercase SHA-256 consumed by the
pinned loader. The JAR, metadata, and ancestor installation directories are assumed to
be deployment-protected from the runtime principal; portable ACL/owner inspection is
not approval authority, and missing or exploded installation state fails closed.

The command injects a lazy authorizer into the shared filesystem application. Only a
new authorization attempt reads one exact-real no-follow proof snapshot, installed
metadata, and a fresh pinned public policy before signed verification and deterministic
audit. A retained terminal cancellation bypasses those transient inputs and may repair
the event/publication suffix after proof expiry, trust rotation, revocation, or source
unavailability. Success reports the bounded durable application record as
`CANCELLATION_APPLIED` without guessing first application versus replay or exposing
proof/trust material. Input and installed configuration failures exit `2`, signed-policy
denial exits `20`, and unexpected durable storage/publication failure exits `70`.
Existing spool/receive commands remain transport and admission only. Metadata/policy
writers, provisioning, permissions, rotation and application-version anti-rollback,
credentials, private-key handling, IdP/session integration, trust-store mutation, queue
disposition, process signalling, Tool/effect cancellation, and pause/resume remain
separate authorized work.

The implemented operator-maintenance state machine is separate from that runtime
surface. `CancellationTrustMaintenanceOperator` is its distinct operator-only Java main,
selected by the fixed repository-local `cancellationTrustMaintenance` Gradle `JavaExec`
task. The accepted packaging boundary adds one separately named custom distribution and
one generated operator launcher while leaving `application.mainClass` and the default
runtime distribution bound to `EnhancerCli`; it never becomes an
`EnhancerCli`/scheduler/runtime command. The generated launcher forwards only explicit
operator arguments and derives no installation, trust, permission, or authority input.
It keeps metadata v1, computes the new pin internally from one validated
canonical public-only policy snapshot, publishes an immutable content-addressed policy
first, and switches the sole fixed metadata point last through a validated same-
directory candidate and required atomic move. INSTALL refuses an existing binding;
ROTATE holds one installation-scoped nonblocking operating-system lock and requires the
expected digest of complete current metadata as compare-and-swap, rechecking it just
before the switch. Exact current rotation replays without rewrite, stale rotation is
refused, old public policies are retained, and no failure triggers fallback, rollback,
overwrite, or cleanup. The lock and CAS prevent cooperative lost updates, not a
privileged rollback: real application-version anti-rollback requires a separately
protected monotonic release/package/keystore/TPM anchor. The detailed phase and recovery
contract is in `docs/cancellation-trust-maintenance.md`. The repository implementation
and launcher behavior is verified only in test-owned temporary installation trees. Typed
finite maintenance reasons map direct-JVM results to success `0`, configuration `2`,
safe refusal `20`, or durability `70`. Packaging must use Gradle-generated Unix and
Windows start scripts with the project runtime classpath under `lib/` and preserve child
process exits; hand-written wrapper parsing or exit translation is prohibited. Assembly
under build/test-owned paths grants no real-install invocation, permission change,
cleanup, deployment, signing, publication, release, or privileged anti-rollback
authority.

The accepted future real-installation boundary separates three stable operating-system
principals. The operator can execute/read the separate launcher and write only an
operator-private public-candidate inbox; the runtime can read/execute the runtime
distribution and read fixed metadata/policies; only the privileged installer/publisher
may mutate protected final paths, apply permissions, publish, and activate an immutable
version. An explicit application path remains request data and must match a publisher-
owned allowlist. Launcher or path possession is never authority.

This split is mandatory while metadata remains beside the application JAR. On POSIX,
directory write sufficient to replace metadata also permits rename/unlink of sibling
children, so an unprivileged operator cannot safely receive direct maintenance-directory
mutation while the JAR remains protected. The current state machine therefore runs only
as the future publisher or behind its authenticated narrow broker. The pure
`com.enhancer.maintenance.installation` contract package represents one already
authorized plan, the three stable principal roles, thirteen derived artifact kinds,
eleven effective operations, the fixed revisioned permission matrix, and the exact
resolve-through-final-evidence phase order. Protected trust paths are derived from the
application JAR and policy digest; callers cannot select sibling metadata, lock, or
policy paths or broaden matrix rules. `InstallationPermissionAdapter` is only a typed
enforcement port for identity/topology resolution, staged permission application,
atomic publication, durability, post-publication recheck, and runtime-principal read-
only probing. Its bounded immutable evidence cannot authorize or represent overall
installation success. A future platform implementation must prove Windows SID/token/
DACL/reparse/volume facts or POSIX numeric UID/GID/group/mode/ACL/capability/device/inode
facts, including denied parent-directory mutation for operator and runtime. Portable
Java owner checks, usernames, ambient identity, inherited defaults, or administrator/
root labels are insufficient.

The same package now defines the schema-v2 installation transaction cursor and its
revisioned point-store port. `InstallationTransactionState` fixes the complete authorized
plan, normalized environment/filesystem identity, bounded source release, permission-
policy digest, expected-current/requested activation identities, existing required phase,
exact revision, and an immutable ordered prefix of succeeded phase-evidence identities.
Each bounded `InstallationPhaseEvidence` binds its own schema, transaction, exact phase,
pending revision, semantic SHA-256, and only for activation the observed requested
identity. Each phase alternates `PENDING` and `SUCCEEDED`; pending-to-succeeded appends
exactly one matching prefix entry, and succeeded-to-next-pending preserves the complete
prefix. Recovery classification distinguishes before final metadata, after metadata
before activation, and after activation exact replay without invoking an adapter. The
store contract exposes only create-exclusive, point resolve, and compare-and-exchange
with bounded typed refusal. `FileSystemInstallationTransactionStore` is its first
uncomposed production implementation: it accepts only a caller-provisioned pre-existing
absolute exact-real non-symbolic root and coordinates cooperating local processes with
one stable transaction-scoped nonblocking OS lock across current resolution, semantic
validation, validated forced candidate creation, required atomic publication, and post-
read. No runtime/CLI/operator/build or permission-adapter wiring exists, so this boundary
still proves neither installation success, evidence-content integrity, operational
effect recovery, publisher authority, hostile-writer exclusion, nor anti-rollback.

Store writes now return an explicit `CREATED`, `ADVANCED`, or `EXACT_REPLAY` receipt;
state idempotency is therefore separate from phase-invocation ownership. The pure
`InstallationTransactionCoordinator` consumes that receipt and executes at most one
phase per `start` or `advance` call. Only a fresh create or advance may invoke a port.
The first two phases use an injected source/preflight verifier over the supplied plan
and environment binding, `ACTIVATE` uses a distinct activation port, and the remaining
eight phases use a phase-effect port through an exhaustive closed switch. Each result
must bind the exact transaction, phase, bounded semantic evidence digest, and requested
activation identity where applicable before the pending cursor can advance to
`SUCCEEDED`. The same standalone result value is appended to the schema-v2 prefix and
must be stored by the succeeded compare-and-exchange before an outcome is returned.
Existing or exact-replayed pending state requires reconciliation without invocation;
exact terminal replay retains all eleven ordered result identities while invoking and
mutating nothing. All port implementations remain test-local fakes. The retained
semantic digests identify accepted port results but contain no evidence body or
integrity/durability proof, so this coordinator still proves only persist-first
sequential ordering and fail-stop behavior, not automatic pending recovery, exactly-
once effects, or installation success.

Pending reconciliation is a separate pure boundary rather than a coordinator fallback.
`InstallationPhaseEvidencePoint` deterministically names one transaction, required
phase, and canonical pending revision. `InstallationPhaseEvidenceResolver` accepts only
that exact point and promises a read-only revalidated result or explicit absence; it has
no listing, discovery, create, or mutation operation. `InstallationTransactionReconciler`
connects that resolver to the transaction point store: it resolves one cursor, returns
without evidence access for a succeeded state, and for a pending state either retains an
absent result unchanged or validates the returned evidence through the existing state
successor before one compare-and-exchange. Exact succeeded-write replay is classified
without phase invocation, and corrupt, foreign, unavailable, mismatched, or malformed
state fails closed. Only test-local in-memory fakes implement the evidence resolver. The
distinct `InstallationPhaseEvidencePointStore` permits only immutable semantic-evidence
create and exact read, distinguishes first creation from exact replay, and returns
absence explicitly, but likewise has only a test-local fake. It deliberately does not
implement the resolver because the current value has no independently revalidatable
evidence body. This contract therefore proves no evidence persistence, content
integrity, host observation, automatic effect recovery, exactly-once behavior, or
installation success.

The integrity-format boundary remains package-local and deterministic.
`InstallationIntegrityEnvelope` defines a schema-v1 domain-separated frame containing
record magic, envelope schema, bounded body length, SHA-256 over that complete header
plus body, and the exact body. `InstallationTransactionFileFormat` and
`InstallationPhaseEvidenceFileFormat` use different magic and payload-kind identities,
length-framed strict UTF-8, stable enum names, explicit optionals and booleans, canonical
UUIDs, and fixed field order. Transaction decode reconstructs the complete plan,
principal/environment binding, local filesystem provider/dialect, activation identities,
ordered evidence prefix, revision, phase, and status through existing constructors.
Evidence decode additionally requires the caller's exact point and rejects an otherwise
valid foreign transaction/phase/revision. Canonical decode must re-encode to identical
bytes. `InstallationRecordFileNames` derives one bounded traversal-free leaf name from
the transaction or complete evidence point without accepting a root. These package-
local codecs and names call no filesystem API themselves. The transaction codec and
transaction leaf are consumed only by `FileSystemInstallationTransactionStore`; the
evidence codec/leaf remain unconsumed by production code. The cursor store bounds reads,
requires non-symbolic regular points, creates no root, uses one stable per-transaction
lock, validates and forces same-root candidates, requires atomic moves, and validates the
published state. Exact create/CAS replay is classified under the lock without rewriting.
File `force(true)` and atomic namespace replacement do not prove parent-directory or
sudden-power-loss durability. Java path checks and OS file locks coordinate cooperating
local processes rather than supplying descriptor-relative native confinement or a
distributed/network-filesystem fence. The digest detects corruption, not authenticity:
an actor able to rewrite protected bytes can recompute it, and an older valid cursor
remains valid without an external monotonic anchor.

The Windows-specific contract boundary now exists under
`com.enhancer.maintenance.installation.windows`. Its sole production adapter accepts an
injected `WindowsInstallationPermissionGateway`; no production gateway implementation
exists. Immutable evidence separately partitions raw Windows file/directory rights and
normalized authorized transaction operations, so the publisher's minimal raw
rename/replace `DELETE` closure never authorizes typed cleanup or uninstall `DELETE`.
The adapter binds canonical SIDs and bounded token state, link-free same-volume
path/file identities, protected explicit DACL facts, exact raw and normalized access,
atomic publication, durability barriers, and read-only runtime digest probes to the
already-authorized plan. The adapter retains each successful atomic-publication target
file identity by transaction and artifact; exact replay must return that identity, and
the following durability and published-security checks accept only it rather than a
same-volume substitute or the pre-publication leaf. It contains no Windows, filesystem,
ACL, process, shell, or native-library call and is not wired to runtime, CLI, operator,
or build entry points.

Future installation uses publisher-private same-filesystem immutable version staging,
final permissions before exposure, content-addressed policy first, fixed metadata last,
a non-mutating trust-loader probe as the actual runtime principal, and activation last.
Exact replay requires source, destination, principals, permission policy, bytes, binding,
phase, and activation equality. Partial state never implies success; failure never
broadens permissions, rolls back, or deletes. The full capability matrix, platform
evidence, recovery, audit, uninstall, and anti-rollback limits are in
`docs/cancellation-trust-operator-installation-permissions.md`. No native/default
platform gateway, installer/executor, real permission enforcement, installation,
activation, deployment, cleanup, release, or privileged anti-rollback anchor is
implemented.

The supported Control producer is intentionally narrower than authenticated control.
`ControlSpoolPublisher`, exposed through one `scheduler-spool-control` point command,
resolves an existing runtime
state directly through `FileSystemAgentRuntimeStateStore`, requiring an `ACTIVE` Goal
with a current non-terminal AgentRun and never invoking runtime recovery or lease
reclamation. The retained Work envelope is the sole source of correlation,
logical-run, and causal message binding; caller input supplies only the new canonical
message identity, bounded producer and reason, occurrence time, and Control signal.
The resulting unchanged `ControlPayload` envelope goes to an explicit queue through
`FileSpoolMessageTransport`, whose result remains only hop-level `ACCEPTED`,
`BACKPRESSURED`, or `UNAVAILABLE`. A separately invoked receiver remains responsible
for Message Bus delivery, durable request admission, and acknowledgement. Publication
creates untrusted intent only and grants no cancel, pause, resume, lease, queue, worker,
or cleanup authority; authenticated interpretation and application remain Gate 12.

The governed Work-spool publisher supplies the upstream half of this supported path.
`scheduler-spool-work` derives one Work envelope only from the
governed active task, repository-memory Workspace snapshot, and explicit caller metadata,
then sends it through `FileSpoolMessageTransport` to an explicit queue destination. Its
terminal status is only the transport hop's `ACCEPTED`, `BACKPRESSURED`, or
`UNAVAILABLE`. The concrete adapter returns the one accepted canonical point filename
without changing the transport-neutral outcome contract or scanning; the existing
separately invoked point receiver remains the downstream delivery/admission boundary.
The command adds no scan, queue creation, retry, receipt, acknowledgement, worker
execution, durable bus journal, or new authority.

The bounded Result connection reuses the process-isolated worker's existing child
producer, explicit result spool point, RunRecord, and Worker finalization consumer.
`ProcessIsolatedAgentRunExecution` routes the decoded unchanged Result envelope through
a fresh `InProcessMessageBus` to one extracted exact-validation queue handler before
returning the reference. Only one `DELIVERED` outcome may expose the validated
reference; `UNROUTED`, handler failure, duplicate, or invalid outcome fails closed.
The handler has no persistence, execution,
finalization, cleanup, journal, retry, or runtime authority; transport acceptance,
delivery, RunRecord validation, and durable finalization remain separate.

#### Isolated Worker Process

`IsolatedWorkerLauncher` is the process lifecycle half of connection 3. It runs one worker in a child process and returns a typed `IsolatedWorkerOutcome`: `COMPLETED` carries an exit code, while `TIMED_OUT` and `START_FAILED` carry a bounded reason and no exit code, so a destroyed or unstartable child can never present a code that reads as a clean exit.

The authority is bounded to the JVM already running. The executable is resolved from `java.home`, canonicalized, and required to be a regular file; the child runs the current classpath; and the entry point is taken as a `Class<?>` rather than a command string, so it is necessarily already on that classpath. No caller-supplied executable, command name, or shell reaches `ProcessBuilder`. Unlike the Git adapter there is no lookup to poison, which is why the executable's location is not constrained — this project vendors its own JDK inside the project root.

The child is bounded like the Git adapter: output is discarded by the operating system rather than read, so a chatty child can neither block on a full pipe nor grow the parent's memory and nothing it prints can be mistaken for a result; the environment is stripped of `JAVA_TOOL_OPTIONS`, `_JAVA_OPTIONS`, and `JDK_JAVA_OPTIONS`, which would otherwise let an inherited setting inject JVM arguments; and a watchdog forcibly destroys a child that overruns its timeout, which is itself capped so a caller cannot disable it.

`IsolatedWorkerMain` is the child entry point. It reads one work message from a spool through the adapter above, runs the same Gate 1-4 pipeline as the in-process execution port, persists the RunRecord, publishes a correlated `ResultPayload` to a separate result spool, and exits with a stable code. The exit code reports lifecycle completion only; the RunRecord reference returns in the result envelope.

The child Work-ingress connection inserts one fresh
`InProcessMessageBus` queue between transport decode and that unchanged execution port.
`IsolatedWorkMessageHandler` constructs the same WorkItem from the unchanged envelope and
parent-supplied identity/capability, executes it, resolves its persisted RunRecord status,
and exposes reference/status only after handler success. The child publishes to the
decoded transport message's own destination and proceeds only after exactly one
`DELIVERED` result, so a foreign route becomes `UNROUTED` before execution or Result
publication. This adds no retry/dead-letter policy, cancellation, topic, durable journal,
directory discovery, cleanup, or authority.

Isolation is what makes termination possible. The in-process ceiling of 64 live workers contains stuck code but cannot stop it, and that ceiling still governs in-process execution. `ProcessIsolatedAgentRunExecution` wires the launcher and file-spool adapter into the execution port, and `DurableAgentRunWorker.processIsolated` is the production composition that selects it with the real self-JVM launcher while sharing one durable queue instance between dispatch and finalization.

The adapter promises no ordering across separately spooled messages: the contract is per hop, and a spool directory has no ordering. Each adapter instance is one-directional by construction; process-isolated execution composes one work spool and one result spool under a per-cycle invocation root. No CLI, durable bus, or supported messaging entry point constructs that path.

#### Process-Isolated Execution

`ProcessIsolatedAgentRunExecution` is the second production `AgentRunExecution`. Before launch it accepts a pre-existing work entry only after decoding the sole message and matching both `queue("work")` and the complete dispatched envelope; foreign work and several work or result messages fail closed without starting a child. Re-entry checks the result spool first, so an already-published valid result returns without another execution.

A returned envelope is a claim, never authority. Its destination must be exactly `queue("isolated-worker-result")`; correlation, logical-run, causation, payload kind, and task identity must bind to the dispatched work; the reference must resolve in the shared store; and the RunRecord must match the work's task, source document, read-file target, verification-bearing expected digest, and claimed status. `DurableAgentRunFinalizer` remains the final authority and reuses the same task/source binding. Store roots are launcher configuration, not payload data.

Per-cycle work/result spools remain until `DurableAgentRunWorker` has persisted the returned RunRecord reference in its cycle-intent checkpoint. The worker then invokes the execution port's idempotent post-checkpoint cleanup before execution acknowledgement. Process-isolated cleanup deletes only the exact Goal/AgentRun invocation tree and an empty Goal parent, never the invocation root, Evidence, or RunRecords. Cleanup failure leaves the checkpoint intact; restart retries cleanup without child re-execution. Failed, corrupt, timed-out, or incomplete execution retains its current spool for recovery or diagnosis, and no time-based cleanup scheduler exists.

The child-persisted/result-not-published recovery window uses one versioned,
domain-separated RunRecord UUID derived from the already-checkpointed canonical Goal and
AgentRun identities. `FileSystemRunRecordStore` point-persists that identity, returns an
exact existing record without rewriting it, and rejects changed-content reuse. Before
launch, an absent result spool causes the parent to resolve only that deterministic
reference; a matching record passes the same task/source/target/digest binding checks and
is returned without another child execution. Missing remains ordinary first execution,
while corrupt or foreign identity reuse fails closed. No store scan, second sidecar,
schema migration, cleanup policy, or universal exactly-once claim is added.

### Gate 7 Runtime Integration Preparation

`WorkMessagePublisher` is the first authority-preserving application boundary that connects real Gate 6 input to the Gate 7 bus. It accepts one matching repository-derived `ApprovedTask` and `WorkspaceSnapshot`, derives the existing `WorkPayload` task revision, snapshot identity, and allowed-Tool scope, constructs a versioned envelope from explicit deterministic metadata, and publishes it to an explicit in-process queue. Task-identity, source-document, or pre-snapshot-time mismatch is rejected before bus admission.

`WorkItemAdmissionHandler` is the matching Gate 7-to-Gate 8 adapter. It retains the delivered envelope unchanged inside one `WorkItem` using injected identity generation, required capability, and downstream sink. It creates no approval, storage, ordering, execution, or Scheduler semantics. A named integration test connects the real Context Reader and Workspace collector through the real bus, journal, and replay path to this admission boundary and proves unchanged authorization/provenance projections plus duplicate-free replay.

`DurableWorkItemAdmissionHandler` is the separate persist-first consumer that connects
this path to `DurableSingleWorkerSchedulerQueue`. It maps the canonical message UUID
through a fixed one-to-one domain transform, producing one stable canonical WorkItem
identity distinct from its source message, retains the exact envelope, adds no
dependencies or authority, and calls idempotent durable admission before handler success. Checked
storage failure becomes handler failure under the bus retry/dead-letter policy; the
queue's persist-before-exposure behavior prevents a failed write from appearing in
memory. Same-bus replay remains duplicate-free. A fresh bus re-delivering the exact
envelope is a no-revision success against the queue-owned admission history; changed
content under the already-persisted identity fails closed rather than adding a second
item.

The work-admission path exercises `WorkPayload`, message/correlation/run/producer
identity, queue delivery, journaling, replay, and duplicate suppression.
Process-isolated execution separately supplies one named `MessageTransport` work/result
path with non-empty result causation. The supported Control producer/receiver path now
connects `ControlPayload` from exact active-Goal binding through the file spool and real
Message Bus to durable request admission. Handoff payload, topic delivery, and
failure/retry/dead-letter, cancellation, re-entrant ordering, and backpressure branches
still have no named real upstream-to-downstream production connection. Those missing
connections remain required before Gate 7 can be promoted as a whole.

## Agent Runtime Model

The target runtime is a persisted, event-driven state machine:

```text
Goal -> Planner -> Queue -> Executor -> Evidence
     -> Memory -> Reflection -> Retry | Verification -> Done
```

Planner, Coder, Reviewer, Tester, and Memory are roles or workers behind message contracts, not hard-coded direct-call chains. Single-agent sequential execution is implemented first. Multi-agent concurrency begins only after queue, idempotency, cancellation, RunRecord, recovery, and independent verification are operational.

### Gate 8 WorkItem Admission Contract

The Gate 8 admission contract is an immutable scheduler-facing `WorkItem` under `com.enhancer.runtime`. A work-item identity is a canonical UUID distinct from the Gate 7 message identity because logical work and one delivery attempt are different identities. The item retains exactly one existing `MessageEnvelope` carrying legacy `WorkPayload` or exact `ModelWorkPayload` plus one bounded required-capability name; logical run identity, approved task revision, Workspace snapshot identity, and allowed Tools are projections of that unchanged envelope rather than caller-supplied copies. Typed ModelWork retention grants no execution authority.

Admission rejects non-work payloads, malformed or reused identities, and blank or oversized capabilities. It creates no approval, Tool permission, state transition, dependency, queue, lease, persistence, or execution authority. The dependency-ready single-worker Scheduler queue is its first consumer, and the separate durable Goal/AgentRun lifecycle below retains the same exact WorkItem. Budgets, cancellation, leases, worker execution, and broader recovery remain later Gate 8 increments.

### Gate 8 Dependency-Ready Single-Worker Queue

`QueuedWork` retains one exact `WorkItem`, an immutable dependency set of at most 256 canonical work identities, and Scheduler-only `NORMAL` or `EXPEDITED` priority metadata. Its two-argument construction defaults to `NORMAL`; priority changes no WorkItem, message, Tool, task, or execution authority. A work item cannot depend on itself, repeat a dependency, or reference work that the same run-scoped queue has not already admitted. Requiring dependency-first admission prevents cycles in this initial topology without claiming arbitrary graph-cycle analysis or forward-reference support.

`SingleWorkerSchedulerQueue` admits at most 4096 work items per run-scoped instance, rejects duplicate work identities, preserves admission order, and forms the complete admission-ordered set of dependency-ready pending candidates for selection. It has exactly one active slot: another claim returns empty until the matching active identity is explicitly completed, and only completion releases dependent work. The queue retains each `WorkItem` and its Gate 7 envelope by identity and creates no task approval, Tool authority, verification result, or execution outcome.

The base queue implementation is deliberately in-memory and single-threaded: claim is not a lease, completion is not a durable AgentRun terminal state, and there is no failure/retry/cancellation, priority admission input, budget, timeout, fence, orphan recovery, worker execution, or production wiring. The separate durable wrapper below supplies schema-v4 queue state and restart recovery without changing those limits.

`SchedulerPrioritySelector` is a pure selector over a complete, bounded,
admission-ordered list of ready candidates with exactly `NORMAL` and `EXPEDITED`
priority. It preserves oldest-ready order within each class, permits expedited
precedence only below a bounded consecutive burst, forces the oldest ready normal
candidate when that burst is exhausted, and caps progress when only expedited work is
ready. It validates all candidates before selection, reads no queue or clock, persists
nothing, and grants no authority. The non-reserved
`SingleWorkerSchedulerQueue.claimNext` path supplies every dependency-ready candidate,
activates exactly the selector result, and adopts its next progress. Schema v3 persists
queued priority, the configured burst, fairness progress, and the one-shot recovery
preference. Recovery consumes that exact preference before selection and leaves progress
unchanged.

### Gate 8 Durable Queue State And Restart Recovery

Each durable queue has a caller-supplied canonical identity and records one immutable schema-v4 snapshot containing its revision, capacity, logical run binding, configured maximum expedited burst, consecutive expedited-claim progress, optional one-shot recovery-preferred WorkItem identity, total admission order, every exact priority-bearing admitted `QueuedWork` in that order, ordered pending work, optional active work, and verified/failed terminal identities. Exact history survives terminal disposition and retains the scheduler-facing WorkItem data and unchanged Gate 7 envelope, including task revision, Workspace snapshot identity, allowed-Tool scope, message provenance, execution input, capability, dependencies, and Scheduler priority. The retained envelope may carry legacy `WorkPayload` or exact `ModelWorkPayload`; the latter keeps every profile component while `WorkItem.requiredCapability` remains independent. The history and status partition must name the same admissions with exact live-work content, a recovery preference must name exact pending work and cannot coexist with active work, and one queue accepts work from only one logical run.

Every successful enqueue, claim, and disposition is staged on a copy and atomically persisted before the transition becomes visible in memory. An ordinary claim publishes the selected active WorkItem and selector-derived fairness progress in the same state update; persistence failure exposes neither. Strict `enqueue` continues to reject every duplicate identity. The separate durable admission operation persists a new exact value before success, treats an equal prior admission as a no-revision success even after terminal recovery, and rejects changed capability, envelope, authorization, provenance, execution input, priority, or dependencies under the same identity. `DurableWorkItemAdmissionHandler` alone uses that idempotent boundary, so same-bus duplicate suppression remains distinct from fresh-bus queue replay. The filesystem adapter keeps one bounded integrity-checked binary snapshot per queue, uses strict UTF-8, refuses unsupported schema versions or structural corruption, does not overwrite an existing queue during creation, and requires updates to advance exactly one revision while retaining the prior exact admission history as a prefix. Each update also acquires one stable queue-scoped operating-system file lock without waiting and retains it across current-state resolution, validation, and atomic publication; contention is a typed failure and the empty lock artifact records no queue or authority state. Persistence or lock failure leaves the previous durable and in-memory revision authoritative.

Durability sits behind the `SchedulerQueueStore` port — `create`, `update`, and `resolve` over one `SchedulerQueueState` snapshot — with `FileSystemSchedulerQueueStore` as its only implementation. The port grants no execution authority and must reject missing or invalid state rather than invent defaults. It is the seam a different durability substrate would replace, and it is deliberately narrower than the queue: the store persists and returns state, while readiness, the single active slot, and disposition remain in `DurableSingleWorkerSchedulerQueue` above it. A process-isolated worker or an out-of-process scheduler therefore changes which implementation is wired, not the queue contract. The same-instance rule still applies whatever the implementation: the dispatcher and finalizer handed to a worker must wrap one queue instance, because the queue's in-memory revision advances with each persisted mutation and a stale second instance fails the store's exactly-one-revision-advance check.

Because this queue boundary has no lease or worker ownership, restart recovery moves a previously active item back into pending order, records its identity as the one-shot recovery preference, and persists that transition before returning the queue. The next claim selects that exact ready pending item before ordinary priority selection, clears the preference, and leaves fairness progress unchanged because it replays the interrupted durable claim. The item may therefore be offered again under at-least-once execution semantics. Exact admission replay prevents a second queue item but does not deduplicate external effects. The filesystem update lock prevents cooperating local processes from overwriting one another; it is not a distributed lock, runtime ownership lease, cross-store transaction, or network-filesystem guarantee. Schema-v1 queue artifacts remain unsupported; schema v2 requires the explicit maintenance migration below, while history compaction/cleanup, leases/fencing, worker execution, effect records, failure/retry/cancellation policy, broader multi-process coordination, and snapshot history remain deferred.

The existing explicit stopped-Scheduler `scheduler-migrate-queue` compatibility
boundary converts one caller-identified schema-v2 queue directly to current v4. It
retains queue identity, revision, capacity,
logical run, exact admission and pending order, active work, terminal partition, every
WorkItem and envelope, and assigns `NORMAL`, maximum burst `4`, progress `0`, and no
migration-time recovery preference. `FileSystemSchedulerQueueStore` validates and
rereads a same-directory v4 candidate under the queue-scoped writer lock, refuses
source-byte drift, and atomically replaces only the unchanged validated source. Typed
`ABSENT` and `ALREADY_CURRENT` outcomes do not create or rewrite the queue artifact;
corrupt, future, drifted, or pre-publication-failed input preserves the authoritative
source and removes the candidate when possible. Ordinary resolution and recovery never
migrate as a side effect.

`SchedulerQueueStatus` is the pure read-only projection of one resolved persisted
snapshot. It preserves admission order and maps the queue-owned status partition to
`VERIFIED`, `FAILED`, and `ACTIVE`; pending work is `READY` exactly when every dependency
is in the verified-completion set and `BLOCKED` otherwise. Each admission retains its
exact persisted `NORMAL`/`EXPEDITED` priority, and the queue projection retains the
maximum expedited burst, consecutive expedited-claim progress, and optional one-shot
recovery preference that affect selection. The projection invokes no store and cannot
recover, claim, complete, fail, or create work. It therefore reports persisted selection
state as observation without predicting a next claim or claiming that a worker is live.

### Gate 8 Durable Submission Intent And Queue Creation

`DurableSubmissionManifest` is the immutable pre-queue intent for one dependency-free
work submission. Its identity is the canonical message UUID, and its exact value binds
the target queue UUID, fixed queue capacity, bounded required capability, and unchanged
`MessageEnvelope` carrying `WorkPayload` or exact `ModelWorkPayload`. The manifest grants no execution authority and
does not duplicate a mutable admission status.

`SubmissionManifestStore` persists that intent before any queue creation.
`FileSystemSubmissionManifestStore` publishes one bounded schema-v3 binary artifact by
atomic move with an integrity envelope, strict UTF-8 decoding, canonical artifact naming,
and explicit missing, corrupt, oversized, trailing, and identity-mismatch failures. Exact
replay returns without rewriting; changed content under the same submission identity
fails closed.

Requested priority is Scheduler submission intent, not work or execution authority.
The immutable manifest contains exactly one `NORMAL` or `EXPEDITED` value, while its
compatibility construction remains `NORMAL`.
Manifest equality, generated caller-intent comparison, and exact queue admission must
all include that value. Ordinary resolution is v3-only; existing schema-v1
manifests require a separate submission-identity-scoped stopped-submission migration
that retains every field, assigns `NORMAL`, validates and rereads a same-directory
candidate, refuses source drift, and atomically replaces only an unchanged valid source.
`scheduler-migrate-submission-manifest` exposes that bounded operation with typed
absent, already-current, and migrated output. The migration invokes no queue recovery,
admission, claim, execution, Tool, effect, or priority input.

`DurableWorkSubmissionService` composes the three monotonic recovery prefixes: persist
the exact manifest, create the declared queue only when absent or resolve and verify its
fixed capacity before recovery, then pass the exact envelope and manifest priority through
`DurableWorkItemAdmissionHandler`. The queue remains the admission authority and its exact
history derives completion, so there is no queue-to-receipt update. A failure after either
earlier prefix is resumed by the same submission, and a fully admitted exact replay changes
neither artifact nor queue revision. This boundary is single-process and adds no
automatic identity or time generation, Scheduler execution, polling, concurrent writer
lock, external-effect adapter, or Gate 9 behavior.

`scheduler-submit` is the supported command for this boundary. The caller supplies the
project, submission, and queue roots plus every task, queue, message, correlation,
logical-run, producer, capability, capacity, occurrence-time, target, and digest input,
and one optional `--priority NORMAL|EXPEDITED` selecting the Scheduler priority.
The command resolves the matching repository-approved active task, captures one
repository-memory Workspace snapshot at the supplied occurrence time, constructs the
exact dependency-free work envelope, and calls `DurableWorkSubmissionService`. It
generates neither identity nor time and never invokes a worker, Tool, evidence store,
RunRecord store, or `scheduler-cycle`. Bounded output reports `ADMITTED` only when the
queue revision advances and `REPLAYED` for an exact already-admitted submission, and
reports the effective `priority` on both.

Manifest schema v3 and its existing compatibility migration are the verified prerequisite that both
submission commands now satisfy: `scheduler-submit` and `scheduler-submit-generated` each
accept the optional `--priority NORMAL|EXPEDITED` input, default to `NORMAL` on omission,
reject any other value, and fail before manifest or queue mutation on an invalid or
replay-conflicting priority, reporting the effective value in bounded output. The generic
Gate 7 message admission path retains its existing `NORMAL` default because the envelope
carries no separate Scheduler priority intent.

Generated submission inputs do not require a second durable invocation manifest. The
generated-input boundary uses one caller-retained canonical submission UUID as the stable
replay key and message identity, derives queue/correlation/logical-run identities through
versioned domain separation, and makes the existing `DurableSubmissionManifest` the sole
owner of the generated occurrence time and exact work envelope. First use captures and
persists that manifest before queue creation; replay resolves it before consulting a clock
or recapturing repository context and rejects caller-intent drift, including a changed
caller-owned priority. This boundary remains separate from `scheduler-cycle`, polling, and
automatic execution.

### Gate 8 Durable Goal And AgentRun Lifecycle

The runtime state is one immutable schema-v5 `AgentRuntimeState` containing exactly one `RuntimeGoal`, the Goal's exact existing `WorkItem`, an ordered immutable list of at most 16 `RuntimeAgentRun` attempts, an ordered retry-decision history, at most 256 exact lease-timeout records, and at most one authorization-bound cancellation application. Goal, AgentRun, WorkItem, and message identities are distinct canonical UUIDs, including across attempts. `agentRun()` is only the latest-attempt projection; earlier attempts remain exact. The retained WorkItem remains the sole source of approved task revision, Workspace snapshot, logical run, required capability, and allowed-Tool provenance; lifecycle state cannot add or widen authority. It may retain legacy Work or typed ModelWork, but the latter is rejected before every current RunRecord-v1 execution or child-launch path.

The Goal advances through `ACCEPTED -> ACTIVE -> COMPLETED`, `ACTIVE -> RETRY_PENDING -> ACTIVE|FAILED`, or an authorization-bound `ACTIVE|RETRY_PENDING -> CANCELLED`. Each AgentRun advances through `PLANNING -> READY -> EXECUTING -> AWAITING_VERIFICATION -> COMPLETED|FAILED`; authenticated Goal cancellation may instead move the current non-terminal attempt to `CANCELLED`, while cancellation from retry-pending preserves its already failed attempt. Skipped, reversed, repeated, mismatched, and post-terminal transitions fail. Result transition requires one exact `ResultPayload` envelope whose logical run, correlation, task, and causation match the retained work message. Only `VERIFIED` completes the Goal. A non-Verified result terminates the current attempt as `FAILED` and parks the Goal at durable non-terminal `RETRY_PENDING`; an admitted persisted decision permits one distinct replacement attempt, while a refused persisted decision permits terminal Goal abandonment.

`DurableAgentRuntime` stages every transition and persists the next revision before adopting or exposing it. `FileSystemAgentRuntimeStateStore` keeps one bounded strict-UTF-8 integrity-checked binary artifact per Goal, atomically creates or replaces it, requires revision increments of exactly one, preserves exact WorkItem, AgentRun, retry-decision, control, cancellation, lease-timeout, result, and fence prefixes, and fails closed on missing, corrupt, oversized, trailing, structurally invalid, rewritten, truncated, reordered, invalidly appended, or unsupported state. Ordinary runtime resolution accepts only schema v5; the explicit v4-to-v5 migration remains the next active increment.

`READY` is the only lease-acquisition state. Acquisition issues one bounded non-blank owner identity, a persisted monotonically increasing positive fence token, an injected-clock issue time, and an exclusive expiry from 1 millisecond through 24 hours, then moves the AgentRun to `EXECUTING`. Renewal preserves owner and fence, must extend expiry, and execution completion requires the same unexpired owner and fence. At or after expiry, explicit reclaim or runtime recovery atomically appends the exact lease-timeout record and persists `EXECUTING -> READY`, clears the lease, retains the last-issued fence, and ensures the next acquisition receives a greater fence. Acquisition, renewal, completion, and reclaim all preserve persist-before-exposure; a storage failure leaves the previous state authoritative.

Lease possession grants only lifecycle-transition authority already bounded by the retained WorkItem. Goal-wide fence monotonicity survives attempt replacement, so a new attempt cannot reuse an earlier fence. This boundary does not itself resolve the RunRecord reference, decide retry from the external-effect ledger, execute a replacement AgentRun, cancel/pause/resume, fence an external adapter invocation, coordinate processes, define distributed clock-skew handling, migrate schema v1, or claim parent-directory power-loss durability.

### Gate 8 Durable Queue-To-AgentRun Dispatch

`DurableAgentRunDispatcher` is the first Integrated connection between the durable queue and durable Goal/AgentRun lifecycle. It validates caller-controlled Goal, AgentRun, owner, and duration metadata before queue mutation; selects an already-active WorkItem or durably claims the next ready WorkItem; creates a missing Goal from that exact WorkItem or recovers a matching Goal; and advances only the missing `ACCEPTED -> PLANNING -> READY -> EXECUTING` prefix before returning an immutable `AgentRunDispatch`.

The queue and runtime stores remain separate atomic artifacts. Queue claim occurs first, so a claim failure creates no runtime state. A later runtime-store failure intentionally leaves the queue item active and retains any durable runtime prefix. Re-entry with the same WorkItem, Goal, AgentRun, and current owner resumes that prefix; an existing unexpired same-owner lease is returned without renewal or revision, while mismatched WorkItem, AgentRun, owner, or post-execution state fails closed. Runtime recovery checks exact WorkItem equality before expiry reclamation, so mismatched state is not mutated.

The filesystem integration recovers both real stores: queue recovery requeues the interrupted active item, the dispatcher claims that same admission-ordered WorkItem again, and runtime recovery returns the exact existing Unicode-bearing unexpired lease. The path does not complete the queue, invoke a Tool or worker, consume a result message, record or fence an external effect, add retry, or claim cross-store atomicity.

### Gate 8 Connection Sequence And Completion Boundary

Fence-checked execution completion and Scheduler queue completion are different facts. `DurableAgentRuntime.completeExecution` persists `EXECUTING -> AWAITING_VERIFICATION` and releases the lease; it proves that the current fenced owner finished its execution phase, not that independent verification passed. The runtime transition therefore MUST NOT directly invoke queue completion or be described as verified completion, logical completion, or dependency satisfaction.

The Scheduler queue now records a terminal `WorkItemDisposition` (`VERIFIED_COMPLETED` or `FAILED`), and only `VERIFIED_COMPLETED.satisfiesDependencies()` is true. The queue's single completion operation is split: `completeActiveVerified` adds the WorkItem to `completedWorkItemIds`, the dependency-satisfaction source used to release dependent work, while `failActive` adds it to a separate `failedWorkItemIds` set that never satisfies dependents, so a failed dependency leaves dependents blocked with an inspectable cause held in the runtime/RunRecord. The schema-v4 state partition is `pending + active + verified + failed = admissionOrder`, with verified and failed disjoint; the separate exact admission history has the same ordered identities and the queue stores disposition only, not a failure reason.

The queue item remains active while the latest AgentRun is `AWAITING_VERIFICATION`, while the Goal is `RETRY_PENDING`, and after a runtime-only authenticated cancellation until a separate queue-disposition connection exists. Queue disposition and exact admission history share the schema-v4 on-disk snapshot with exact restart recovery: a persisted terminal disposition is never re-run, only interrupted active work is requeued with its one-shot recovery preference, and incompatible schema-v1/v2/v3 snapshots fail ordinary resolution. Runtime lifecycle state is separately schema v5.

`DurableAgentRunFinalizer` connects those separate facts through two recoverable operations. `recordAgentRunResult` resolves the RunRecord reference, binds it to the Goal on `approvedTask.taskId()` and `sourceDocument()` (no source SHA exists), and persists either Goal `COMPLETED` or non-terminal `RETRY_PENDING`; it never persists the RunRecord and fails closed on missing, corrupt, mismatched, premature, or changed-reference input. `finalizeTerminalDisposition` derives queue mutation only from terminal Goal state: `COMPLETED -> completeActiveVerified`, terminal `FAILED -> failActive`, and `ACTIVE`/`RETRY_PENDING` -> no disposition. A terminal `FAILED` Goal requires a persisted refused retry decision. Because queue recovery requeues interrupted active work, terminal finalization re-claims the matching item before disposition when necessary. The legacy `finalizeAgentRun` composes the forward terminal case, while `recoverFinalization` applies only authorized post-terminal disposition. Queue and runtime remain separate durable boundaries with no cross-store transaction.

`DurableAgentRunWorker` drives connection 3 and the retry portion of connection 6. One
`runOneCycle(leaseDuration)` call drives cycle intent -> queue claim and lease -> exact
Goal-ledger creation/recovery -> persisted RunRecord reference -> execution-artifact
cleanup -> execution acknowledgement -> result recording. Verified completion records
`VERIFIED_COMPLETED` and clears the intent. A non-Verified result stops at
`RETRY_PENDING`, where the Worker invokes `DurableAgentRunRetryController` with its
explicit policy. Refusal abandons the Goal and records queue `FAILED`; admission writes a
replacement AgentRun identity into the schema-v2 `PendingFinalization` checkpoint before
append, rolls the checkpoint to that attempt without the prior reference, and continues
through the same fenced path while the WorkItem remains active. Recovery re-enters
idempotently before decision, after decision, after replacement checkpoint, after append,
after rollover, or after RunRecord checkpointing. The Goal ledger is created only at
Goal-start execution, never invented while deciding retry. Queue, runtime, ledger,
RunRecord, and checkpoint remain separate stores without a cross-store transaction.
When restart finds the checkpointed latest AgentRun already `COMPLETED` or `FAILED`, the
Worker first exact-replays `recordAgentRunResult` with that checkpoint's RunRecord
reference. Only successful Result replay may then enter retry control or terminal queue
disposition recovery and checkpoint clearing, so a missing verification, Tool-timeout,
or stagnation event remains repairable before the later side effect. A mismatched
checkpoint reference fails closed; event-free replay advances no runtime revision.
Cleanup failure retains the checkpoint and retries without re-execution; pre-reference
execution can still orphan a RunRecord under the accepted at-least-once contract. The
dispatcher and finalizer must share one queue instance. `processIsolated` selects the real
child launcher and per-cycle spools with the same explicit retry policy and ledger store;
when the optional Scheduler recorder is present, the same injected clock and recorder
also construct the finalizer so terminal Result replay repairs verification, Tool-timeout,
stagnation, and terminal-disposition publication before later checkpoint progress.
Recorder omission retains the event-free finalizer. No external-adapter behavior is added.

`scheduler-cycle` is the first supported Scheduler entry point. It is a recovery-only
one-cycle boundary over an already-existing durable queue: the caller supplies every
project/store root, queue and owner identity, retry bound, lease duration, and child
timeout. The command recovers one queue, composes `processIsolated` with the real
filesystem stores and system UTC clock, and invokes exactly one cycle. It never creates
a queue or admits work. Bounded output distinguishes `IDLE`, `VERIFIED_COMPLETED`, and
terminal `FAILED`; the failed disposition has its own non-zero exit code, while missing
queue/configuration is usage failure and corruption/execution failure remains internal.
Submission remains a separately invoked `scheduler-submit` command; `scheduler-cycle`
never creates a manifest or admits work. Neither command adds a polling service, durable
bus, or whole-Gate Operational promotion.

`ForegroundSchedulerDrain` is the bounded foreground consumer of this one-cycle
boundary. It sequentially invokes the same recoverable process-isolated cycle against one
existing queue, with an explicit positive limit no greater than the queue's 4096-item
bound. It continues only after verified completion and returns a typed
`SchedulerDrainResult` on the first idle result, failed disposition, or configured limit,
reporting cycle/completion/failure counts and the exact stop reason without claiming an
empty queue when the limit is reached.

The corresponding `scheduler-drain` surface reuses the cycle composition inputs plus the
explicit limit, remains separate from both submission commands, and leaves
`scheduler-cycle` unchanged. It adds no queue creation, admission, sleep, waiting, idle
retry, daemon, control application, or drain-progress store. Durable re-entry is derived
from the existing queue disposition and per-cycle checkpoint: a terminal item is already
persisted before the following cycle, while an interrupted current cycle resumes through
the existing checkpoint.

`BoundedSchedulerService` is the caller-driven waiting contract over the same
`DurableAgentRunWorker.runOneCycle` boundary. `SchedulerServicePolicy` requires finite
1-through-4096 total-cycle and consecutive-idle limits plus a positive idle wait no
greater than one hour. The service checks a caller-owned local stop signal before every
sequential cycle, waits only after a non-terminal idle cycle, resets consecutive-idle
progress after verified work, and stops on the first failed disposition. A total-cycle
limit takes precedence when it is reached on the same cycle as the idle limit.
`SchedulerServiceResult` reports exact invoked, verified, idle, and failed counts with a
typed stop reason; an interrupted wait restores the thread interrupt flag and returns
without another cycle. The contract creates no thread or process lifecycle, command or
API surface, durable service progress, authenticated control authority, queue or work,
external adapter, or recovery authority beyond the existing per-cycle state. A supported
service consumer is the separate foreground `scheduler-service` command. It reuses every
explicit one-cycle composition input plus the finite service policy, supplies the
invoking thread's interrupt state as the local stop signal, and reports typed counts,
queue state, and stable exits. Real-filesystem integrations resume a persisted
cycle-intent prefix and reclaim an expired executing lease under the same Goal/AgentRun
with a greater fence and one terminal disposition. The CLI creates no thread, daemon,
supervisor, service checkpoint, queue/admission, or authenticated control authority.
Durable/supervised background lifecycle and broader orphan discovery/reclamation remain
later connections.

The separate `scheduler-status` surface resolves one caller-identified queue snapshot
directly through `FileSystemSchedulerQueueStore`, then formats the runtime-owned
`SchedulerQueueStatus` projection. An explicit 1-through-48 limit bounds the
admission-ordered identity/state/priority prefix while counts cover the complete queue.
The bounded output also reports the persisted maximum expedited burst, consecutive
expedited-claim progress, and optional recovery-preferred identity. Status never calls
`DurableSingleWorkerSchedulerQueue.recover`, so inspecting an active snapshot cannot
requeue work, consume the recovery preference, or advance the revision. It reads no
runtime, effect, cycle-checkpoint, RunRecord, submission, or invocation store;
cross-store recovery interpretation remains a separate contract.

`SchedulerRecoveryStatus` is that separate read-only cross-store contract. The single
`PendingFinalization` record is its only join anchor: without it, runtime and RunRecord
stores are not scanned. With it, `SchedulerRecoveryStatusReader` directly resolves the
named Goal and optional RunRecord, validates exact checkpoint/AgentRun/replacement,
WorkItem/admission, terminal result/reference, and RunRecord task/source bindings, and
projects one durable phase from no pending cycle through intent, runtime, RunRecord,
result recording, retry resolution, replacement, queue disposition, or checkpoint
clearing.

The stores still share no transaction. The reader therefore takes one bounded second
sample and refuses output when the queue revision, checkpoint value, or referenced
runtime revision changed. This is a stable sequential observation, not an atomic
multi-store snapshot. The separate `scheduler-recovery-status` command requires explicit
queue, runtime, cycle-checkpoint, and RunRecord roots, creates and mutates none of them,
and reports `workerLiveness=UNKNOWN`; it does not inspect a process, evaluate lease
expiry, apply recovery, clear a checkpoint, scan histories, or authorize execution.

`SchedulerExternalEffectRecoveryStatus` extends that read-only observation without
changing it. `SchedulerExternalEffectRecoveryStatusReader` first obtains the existing
checkpoint-correlated Scheduler projection; without a correlated Goal it does not read
the effect or Evidence stores. With one, it point-resolves only that Goal's ledger,
validates every request against the runtime WorkItem and retained AgentRun history, and
resolves every terminal outcome's exact Evidence Store reference to verify its recorded
digest without exposing content. Missing ledger is a valid durable prefix only before
the Scheduler projection advances beyond `RUNTIME_RECORDED`.

The reader accepts output only after a bounded second Scheduler projection and ledger
sample preserve the correlated observation, ledger presence, and ledger revision.
Aggregate precedence follows retry safety: any `PREPARED` intent requires ambiguous
effect recovery first, then `REQUIRES_USER_RECOVERY`, then `APPLIED` or `DEDUPLICATED`;
only a non-empty all-`COMPENSATED` ledger is fully compensated. The separate
`scheduler-external-effect-status` command requires the Scheduler recovery roots plus
explicit effect and Evidence roots and a 1-through-8 ledger-prefix limit. It creates,
recovers, scans, invokes, retries, compensates, or mutates nothing and makes no claim
about an external system beyond integrity-checked adapter-established evidence.

`SchedulerInvocationRecoveryStatus` is the separate read-only transport-artifact
observation. `SchedulerInvocationRecoveryStatusReader` first obtains the existing
checkpoint-correlated Scheduler projection and reads no invocation path without its Goal
and AgentRun. With a recorded runtime, it derives only that pair's private invocation
namespace and validates at most one work and result spool message against the exact
runtime WorkItem. A result message remains a claim until its destination, correlation,
logical run, causation, task, execution input, verification status, and referenced
RunRecord binding all match. The reader takes a bounded second Scheduler/runtime/spool
sample and refuses observed drift; corrupt, foreign, symbolic-link, or several-message
spools fail closed. The separate `scheduler-invocation-status` command requires the
existing Scheduler recovery roots, one explicit invocation root, and a 1-through-8
message prefix limit. It creates, consumes, launches, cleans, recovers, retries, scans,
or mutates nothing and makes no child-liveness claim.

`AgentLoopAgentRunExecution` is the first production implementation of that port: it drives the Integrated Gate 1-4 pipeline (governed `read-file` `ToolExecutor`, `EvidenceRecorder`-persisted evidence, the bounded `AgentRunController`/`AgentLoop`, `DeterministicReadFileVerifier`, and the application `AgentRunFinalizer`) against the approved task's own source document — the `read-file` target is `taskRevision().sourceDocument()` and the expected content SHA-256 is `taskRevision().sourceSha256()` — and returns the persisted `run-record/<uuid>` reference. The `ApprovedTask` is constructed directly from the WorkItem's fields (no `ApprovedTaskReader`, no `In Progress` coupling), so the runtime finalizer's taskId-plus-sourceDocument binding holds by construction; the port must persist through the same `RunRecordStore` the worker's finalizer resolves from. A digest mismatch or Tool failure is carried in a persisted non-`VERIFIED` RunRecord, never thrown, and is real drift detection; the runtime result boundary records it as a failed attempt at `RETRY_PENDING` without a terminal queue disposition. The derivation of `(targetPath, expectedContentSha256)` from the WorkItem sits behind one private seam.

`WorkPayload` now carries an optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)`: the port's seam prefers the declared input and falls back to the approved task's own source document when it is absent, so a WorkItem can execute an arbitrary governed target through the same contained read-file, evidence, verification, and RunRecord pipeline while the `ApprovedTask` binding stays the source document (exactly as the CLI separates `CURRENT_TASK.md` from `target-path`). The input is explicit caller authority data supplied through a `WorkMessagePublisher` overload — snapshot observations are evidence, not approval authority, so they never derive it. Both filesystem serializers retain it inside the shared durable envelope representation; queue schema v4 and runtime schema v5 embed it, with incompatible snapshots failing closed. Multiple inputs, payload-carried plans or Tool-call scripts, and write Tools remain out of scope.

`RuntimeControlAdmissionHandler` is the bounded Gate 7-to-Gate 8 request connection for control envelopes. It recovers one named Goal and records an exact `ControlPayload` envelope only while that Goal and its AgentRun are active, after matching logical run, correlation, and work-message causation and rejecting runtime-identity collisions. `AgentRuntimeState` retains at most 256 requests in admission order; exact message replay is a no-revision duplicate, identity reuse with different content fails closed, and every later lifecycle state retains the exact ledger prefix. `FileSystemAgentRuntimeStateStore` encodes the full envelopes in schema v5, requires the ledger to stay prefix-monotonic on update, and publishes the new revision atomically before the handler returns. Checked storage failure becomes handler failure so the existing bus retry/dead-letter contract remains visible. Incompatible schema-v1-v4 runtime payloads fail explicitly.

This boundary records an untrusted request, not an accepted transition. The envelope producer and control reason are diagnostic provenance and cannot pause, resume, cancel, release a lease, mutate the queue, interrupt a worker, expand Tool scope, or change bus cancellation. Gate 12 must authenticate and authorize a later application path before any of those state changes can exist.

### Gate 8 Pending-Finalization State Migration Boundary

The first supported Gate 8 state-version migration targets only the
`FileSystemPendingFinalizationStore` schema-v1 checkpoint. Its Goal identity, AgentRun
identity, and optional RunRecord reference map exactly into schema v2, whose added
replacement AgentRun identity is absent for every v1 value. This is the only current
schema-v1-to-v2 conversion that neither invents nor discards durable information: queue
v1 lacks exact terminal admission values, runtime v1 lacks attempt and retry-decision
history, and external-effect v1 lacks adapter identity and terminal evidence binding.

`FileSystemPendingFinalizationStore.migrateSchemaV1ToCurrent` is the explicit
maintenance operation over the caller-named cycle-checkpoint root while its Scheduler is
stopped. Ordinary `findPending`, worker recovery, Scheduler cycles and drains, and
read-only status surfaces continue to reject schema v1 and never migrate as a side
effect. The operation reads only the fixed pending-finalization artifact, validates the
complete bounded integrity envelope and v1 payload, maps `replacementAgentRunId` to
empty, and returns typed `ABSENT`, `ALREADY_CURRENT`, or `MIGRATED`. The first two
outcomes do not create or rewrite an artifact.

The complete v2 candidate is encoded and validated in a private same-directory file.
Immediately before the existing atomic-replace publication boundary, the source bytes
must still equal the validated input. Until replacement succeeds, the original v1 path
and bytes remain authoritative; read, validation, candidate-write, drift, or publication
failure removes only the candidate when possible. Successful replacement makes the v2
artifact authoritative without promising a backup, rollback copy, parent-directory
power-loss durability, concurrent old-version writer safety, or migration of any other
store.

The separate `scheduler-migrate-cycle-checkpoint` command takes only the explicit
cycle-checkpoint root and exposes those three bounded outcomes. Corrupt, future-version,
or concurrently changed source state exits through the normal internal-error boundary;
tests over real filesystem artifacts prove exact conversion with and without a RunRecord
reference, current/absent non-writing behavior, ordinary v1 rejection, candidate cleanup,
source-drift refusal, byte-identical corrupt-input failure, and normal v2 recovery after
migration.

### Gate 8 Durable External-Effect Ledger

`DurableExternalEffectLedger` records external-effect intent and adapter-established
outcomes without invoking an external system. One schema-v2 ledger belongs to one exact
Goal and retains at most 256 ordered effects. An `ExternalEffectRequest` binds a bounded
stable idempotency key, adapter identity, and semantic operation SHA-256 to the Goal, its
current AgentRun, the retained WorkItem, and a bounded operation name; payload content and
credentials do not enter the ledger.

Preparation validates the exact executing AgentRun and its matching unexpired owner and
fence against `AgentRuntimeStateStore`, then persists `PREPARED` before returning it to a
caller that may invoke an adapter. Terminal recording repeats the same lease check and
may transition one prepared effect exactly once to an evidence-bound `APPLIED`,
`DEDUPLICATED`, `COMPENSATED`, or `REQUIRES_USER_RECOVERY`. `PREPARED` carries no outcome
evidence; every terminal record carries exactly one immutable Evidence Store reference and
SHA-256. Exact request and outcome replay returns the existing record without a revision;
key reuse with changed bound data, a stale or expired lease, identity mismatch, and
terminal status or evidence replacement fail closed.

`ExternalEffectLedgerStore` is a separate atomic boundary from runtime state.
`FileSystemExternalEffectLedgerStore` publishes one bounded strict-UTF-8 integrity
envelope per Goal, enforces exactly-one revision advancement plus append-only preparation
or one-way prepared-to-terminal history, and rejects missing, corrupt, oversized,
trailing, unsupported, symbolic-link-root, or non-monotonic state. An unresolved
`PREPARED` record survives restart and never authorizes automatic replay. Schema-v1
artifacts are explicitly unsupported; migration is separate work. The retry connection
consumes this ledger, while the application executor below supplies and verifies outcome
evidence without moving adapter authority into the ledger.

### Gate 8 External-Effect Adapter Execution Boundary

`DurableExternalEffectExecutor` is the application-layer boundary around the durable
ledger, one `ExternalEffectAdapter` port, and the existing `EvidenceStore`. The ledger
does not invoke an adapter or acquire Tool authority. The adapter owns opaque operation
input and credentials; the executor requires a bounded
stable adapter identity and verifies the adapter's canonical semantic digest against the
prepared request before any ledger mutation. Operation payload and credentials do not
enter the ledger or evidence merely because the effect is durable.

The schema-v2 effect ledger records the contract explicitly. `PREPARED` retains
the exact Goal, AgentRun, WorkItem, idempotency key, adapter, operation, and digest without
outcome evidence. A terminal record binds exactly one typed `APPLIED`, `DEDUPLICATED`,
`COMPENSATED`, or `REQUIRES_USER_RECOVERY` status to the same adapter plus one resolvable
Evidence Store reference and evidence SHA-256. Terminal successor validation preserves
that binding; schema-v1 artifacts are rejected rather than reinterpreted.

Execution order is validate -> persist `PREPARED` -> invoke adapter once -> persist
redacted complete outcome evidence -> re-check the current owner/fence -> persist the
evidence-bound terminal record. Exact terminal replay resolves and integrity-checks the
bound evidence and never invokes the adapter or advances a revision. A record already at
`PREPARED` when the call begins never authorizes automatic execution. Adapter, evidence,
terminal-write, or lease failure leaves it prepared; evidence written before a failed
terminal publication may be orphaned but grants no state transition.

The named filesystem integration connects a real executing runtime lease,
the real ledger and Evidence Store, the application executor, and a deterministic adapter
across every persistence prefix and restart replay without adding production external
authority. Real network, Git, cloud, or other mutation adapters remain Gate 11 work and
require their own Tool policy, secret/outbound-data controls, and recovery evidence. This
boundary is at-least-once and ambiguity-preserving, not a universal exactly-once claim.

### Gate 8 Corrected Multi-Attempt Retry Boundary

AgentRun attempt failure and Scheduler WorkItem failure are separate lifecycle facts.
The retry decision consumes the exact latest failed `RuntimeAgentRun`; it never consumes
or fabricates terminal `WorkItemDisposition.FAILED`. The queue keeps the WorkItem active
while its Goal is `ACTIVE` or `RETRY_PENDING`, and a queue disposition is written only
after the whole Goal becomes `COMPLETED` or terminal `FAILED`.

RunRecord-backed result recording and queue finalization are separate recoverable steps.
A non-Verified attempt result terminates that AgentRun as `FAILED` and moves the Goal to
durable non-terminal `RETRY_PENDING`. The bound ledger and immutable attempt count then
produce one typed retry decision that persists before either a checkpointed replacement
AgentRun is appended or the Goal is abandoned. Recovery from `RETRY_PENDING` must not
derive queue failure from the terminal attempt alone.

Automatic retry is fail-closed against external effects: only an empty Goal ledger or
one whose every effect is `COMPENSATED` admits another attempt. `PREPARED`,
`REQUIRES_USER_RECOVERY`, `APPLIED`, and `DEDUPLICATED` refuse automatic retry until a
separate cross-attempt adapter/idempotency contract exists. A known outcome is not by
itself proof that re-execution is safe.

`DurableAgentRunRetryController` is the sole application boundary for this decision and
its immediate runtime transition. It resolves the existing runtime and Goal ledger,
derives the latest failed attempt and immutable completed-attempt count, and records the
typed decision with the ledger revision, record count, and a versioned length-framed
semantic SHA-256 before any action. Exact decision replay is revision-free. A separately
checkpointed canonical replacement identity may then append only the admitted AgentRun,
or a refused decision may abandon the Goal; both action re-entries are idempotent. The
controller has no queue, worker, Tool, adapter, lease, or execution authority.

The event-aware process-isolated Worker composition passes its one optional recorder
and injected clock into this retry controller. The supported `scheduler-cycle`,
`scheduler-drain`, and `scheduler-service` commands therefore publish
`RETRY_DECISION_RECORDED` only after the decision-bearing runtime revision and
`RETRY_STARTED` only after the checkpointed replacement AgentRun. Decision-publication
failure re-enters from `RETRY_PENDING`; start-publication failure re-enters from the
retained replacement checkpoint and `ACTIVE` Goal. Exact replay advances neither source
nor event revision. Omitting the event option group preserves the event-free controller.

The runtime payload is schema v2 with an ordered immutable AgentRun list, a latest
projection, Goal-wide monotonic fences, and an ordered typed retry-decision ledger.
Persistence enforces exact history and decision prefixes and rejects truncation,
rewrite, reordering, invalid append, stale revision, and unsupported schema. Schema-v1
migration is a separate task; the incompatible payload is not revised in place under the
same version number.

### Gate 8 Durable Runtime Event Contract

The Gate 8 event contract is a derived, reference-oriented observation of durable
runtime facts. It is not another state machine and grants no transition authority.
Canonical AgentRuntime state, RunRecords, retained Control messages, and Scheduler queue
state remain authoritative. An event may be recorded only after the state or evidence it
describes is durable.

The finite `runtime-event-v1` taxonomy deliberately keeps detection, decision,
application, verification, and terminal disposition separate:

| Event kind | Fact represented | Detection and recording owner |
|---|---|---|
| `RETRY_DECISION_RECORDED` | an admitted or refused retry decision is durable | `DurableAgentRunRetryController`, after the decision-bearing runtime revision |
| `RETRY_STARTED` | the admitted replacement AgentRun is durable | the retry controller/Worker re-entry boundary, after the replacement runtime revision |
| `STAGNATION_DETECTED` | a persisted RunRecord carries `STAGNATED` | Agent Loop detects; RunRecord-backed result finalization records |
| `TIMEOUT_DETECTED` | a durable fact records a Tool, process, or lease timeout | Tool uses the finalizer after a bound RunRecord carries `ToolFailureCode.TIMED_OUT`; process uses `ProcessIsolatedAgentRunExecution` after its bound `ProcessTimeoutFact` is point-persisted; lease uses event-aware `DurableAgentRuntime` only after reclaim atomically appends a bound `LeaseTimeoutRecord` with the `EXECUTING -> READY` runtime revision |
| `CANCELLATION_REQUEST_RECORDED` | a bound `CANCEL` Control request is durable and unapplied | `RuntimeControlAdmissionHandler`, after exact request admission |
| `CANCELLATION_APPLIED` | an authenticated cancellation transition is durable | `AuthenticatedCancellationApplication`, after the authorization-bound runtime cancellation revision |
| `VERIFICATION_RECORDED` | a RunRecord-backed Result transition is durable | `DurableAgentRunFinalizer`, after the AgentRun result transition |
| `WORK_ITEM_TERMINATED` | the queue durably records `VERIFIED_COMPLETED` or `FAILED` | `DurableAgentRunFinalizer`, after terminal queue disposition |

`RuntimeEvent` carries a version, deterministic domain-separated canonical event UUID,
kind, occurrence time, WorkItem/Goal/AgentRun identities, the approved task revision and
Workspace snapshot identity projected from the retained WorkItem, logical-run and
correlation identities, an optional causal message or prior-event UUID, a bounded
producer identity, one sealed kind-specific detail value, and one through four bounded
authoritative references. A reference names a typed runtime revision, retry decision,
Control or Result message, RunRecord or evidence artifact, or queue revision and may
carry its integrity digest where the source contract supplies one. Event bodies never
copy source content, credentials, approval, Tool scope, or mutable policy.

The event UUID derives from a versioned domain plus event kind, Goal and AgentRun
identities, and the complete ordered authoritative-reference identity. The same durable
fact therefore re-derives the same event across restart; a different fact cannot
silently reuse its identity. Occurrence time comes from the authoritative source when it
has one, otherwise from the transition owner's injected clock after that transition is
durable. It is data, never an ordering or lease authority.

One implemented `RuntimeEventStream` belongs to one exact Goal and retained WorkItem.
Schema v1 is bounded to 4096 ordered events and carries one monotonic stream revision.
The implemented `RuntimeEventStore` append operation persists an exact new prefix
before exposure.
Exact event replay is revision-free; changed content under an existing identity, a
foreign Goal/WorkItem/AgentRun or task/snapshot/logical-run/correlation binding, an
invalid kind/detail pair, prefix truncation/rewrite, overflow, corruption, or an
unsupported schema fails closed. `FileSystemRuntimeEventStore` uses the existing
strict-UTF-8, bounded integrity-envelope, non-symbolic-root, candidate/atomic-publication
rules. It adds no scan, cleanup, retention, compaction, migration, cross-store or
cross-process transaction, or power-loss directory-durability claim.

Durable ordering is source transition -> deterministic event append/exact replay ->
reference publication. The implemented `RuntimeEventRecorder` enforces that ordering
and invokes a `RuntimeEventPublisher` port only with the deterministic opaque
`runtime-event/<goal>/<event>` reference after append. If the source transition is
durable but event recording or publication is not acknowledged, the existing
transition-specific checkpoint or exact message re-entry resolves the source again,
derives the same event identity, exact-replays the append, and may publish again.
Consumers therefore deduplicate by event identity. Transport `ACCEPTED` still means
only hop acceptance.

When an authoritative transition retains no occurrence timestamp, the transition owner
supplies an injected post-source time for the first candidate. The recorder's explicit
first-occurrence recovery operation resolves only the same deterministic event identity,
reuses its already-persisted occurrence time, and submits the reconstructed value through
the unchanged exact-append check. Every other field remains candidate-derived, so changed
content under that identity still fails closed. If no such event exists, the candidate's
post-source time becomes the first durable occurrence time.

The first transition-owner consumer is the event-aware construction of
`RuntimeControlAdmissionHandler`. It persists or exact-replays the bound Control request
first. For `CANCEL` only, it then derives `CANCELLATION_REQUEST_RECORDED` from the exact
retained Goal, WorkItem, current AgentRun, task, snapshot, logical-run, correlation,
Control message, occurrence time, and persisted runtime revision before invoking the
recorder. An exact Control replay repairs a source-persisted/event-missing prefix or
republishes an event-persisted reference without advancing either source revision.
`PAUSE` and `RESUME` remain request-only, and source persistence failure reaches neither
the event store nor publisher port.

The second transition-owner consumer is the event-aware construction of
`DurableAgentRunFinalizer`. `recordAgentRunResult` first persists or exact-replays the
RunRecord-backed Result transition. It then derives `VERIFICATION_RECORDED` from the
retained Result occurrence time, verification status, and causal message identity plus
the exact Goal, WorkItem, AgentRun, task, snapshot, logical-run, correlation,
Result-message, and RunRecord bindings. The ordered Result-message and RunRecord
references determine event identity; a later runtime revision therefore cannot change
the verification fact during repair. Result persistence failure reaches neither the
event store nor publisher, while an event or publisher failure leaves the Result
transition available for exact re-entry. Terminal queue disposition remains a separate
durable boundary handled by the next connection.

The third transition-owner consumer is the same event-aware finalizer after terminal
queue disposition. `finalizeTerminalDisposition` first applies or re-enters the exact
`VERIFIED_COMPLETED` or `FAILED` partition and confirms that the target WorkItem is
present there. Only then does it derive `WORK_ITEM_TERMINATED` with the retained Result
message as causation and a stable
`scheduler-queue/<queue>/work-item/<work>/disposition/<value>` reference to the queue's
monotonic terminal fact. That reference does not use the mutable whole-queue revision,
so later claims, admissions, or dispositions re-derive the same event identity. Because
the queue retains no transition time, the first candidate uses the injected clock after
the disposition is durable; publication or later-clock re-entry recovers that first
persisted occurrence time before exact replay. Queue persistence failure reaches neither
the event store nor publisher, while event or publisher failure leaves the terminal
queue partition available to `recoverFinalization`.

The fourth transition-owner consumer is the event-aware construction of
`DurableAgentRunRetryController`. `recordDecision` first persists or exact-replays the
attempt-bound admitted or refused `AgentRunRetryDecisionRecord`. It then derives
`RETRY_DECISION_RECORDED` from the retained failed Result causation, decision outcome,
exact Goal, WorkItem, failed AgentRun, task, snapshot, logical-run, and correlation
binding plus ordered stable references to
`agent-runtime/<goal>/retry-decision/<agent-run>` and the decision-bearing runtime
revision. The decision record retains no occurrence time, so the first candidate uses
the injected clock only after decision persistence and publisher-failure re-entry
recovers the first persisted event occurrence before exact replay. Runtime persistence
failure reaches neither event store nor publisher; event or publisher failure leaves
the decision available through exact `recordDecision` re-entry. `RETRY_STARTED` remains
separate until a replacement AgentRun is durable, and post-decision admitted/refused
actions create no additional event through this connection.

The fifth transition-owner consumer is the same event-aware retry controller at the
Worker-reentered `beginAdmittedRetry` boundary. It first appends or exact-replays the
caller-checkpointed replacement AgentRun, then derives `RETRY_STARTED` with that
replacement as the event AgentRun, the prior failed AgentRun in its sealed detail, and
the retained failed Result message as causation. Ordered stable references to
`agent-runtime/<goal>/retry-decision/<previous-agent-run>` and
`agent-runtime/<goal>/agent-run/<replacement-agent-run>` identify the admitted decision
and monotonic replacement fact without binding replay to a later mutable runtime
revision. Because replacement append retains no transition time, the first candidate
uses the injected post-persistence clock; publisher-failure replay restores the first
event occurrence, while a missing event after later replacement status progress uses
the recovery candidate time and the same stable identity. Replacement persistence
failure reaches neither event store nor publisher. Refused decisions and terminal
abandonment create no `RETRY_STARTED` fact through this connection.

The sixth transition-owner consumer is the event-aware finalizer during
`recordAgentRunResult`. It first resolves and binds the RunRecord, persists or
exact-replays the matching Result transition, and records the separate
`VERIFICATION_RECORDED` fact. When the same RunRecord carries worker stop reason
`STAGNATED`, it then derives `STAGNATION_DETECTED` from the RunRecord's retained
occurrence time and iteration count plus the current default Agent Loop stagnation
threshold of three. The retained Result supplies causation, while ordered stable
Result-message and RunRecord references determine identity without a later mutable
runtime revision. Exact Result re-entry resolves the source again and exact-replays both
events, so event or publication failure remains recoverable after later runtime
revisions. Missing, corrupt, mismatched, or Result-transition persistence failure
reaches no stagnation event, and a non-stagnated RunRecord creates only its separate
verification event. This connection changes no RunRecord, runtime, event, or message
schema and does not select a timeout owner.

The initial timeout source is the same bound persisted RunRecord, but only when its
exact Tool result carries `ToolFailureCode.TIMED_OUT`. The event-aware finalizer remains
the transition owner: after the matching Result transition and its separate
`VERIFICATION_RECORDED` fact are durable, it records `TIMEOUT_DETECTED` with
`RuntimeTimeoutKind.TOOL`, the RunRecord occurrence time, Result causation, and stable
Result-message and RunRecord references. When the RunRecord also records `STAGNATED`,
the timeout fact precedes the separate stagnation fact. Exact Result re-entry repairs or
republishes the same event without another runtime revision.

Process watchdog timeout is owned by `ProcessIsolatedAgentRunExecution`, which alone
receives the exact dispatch, configured timeout, and typed
`IsolatedWorkerStatus.TIMED_OUT` outcome. It persists one bound `ProcessTimeoutFact`
under deterministic reference `process-timeout/<goal>/<agent-run>` before exposing
failure. The point store retains the first post-outcome occurrence time, exact
runtime-event binding, AgentRun, positive configured timeout, bounded launcher reason,
and a semantic digest; identical re-entry is rewrite-free while changed reuse, foreign
binding, corruption, or unsupported state fails closed. The event-aware execution then
records `TIMEOUT_DETECTED` with `RuntimeTimeoutKind.PROCESS`, Work-message causation,
producer `process-isolated-agent-run-execution`, and the process-timeout
reference/digest. A persisted fact is checked before spooling or launch, so restart
repairs or republishes the exact event and exposes the same failure without another
child. Start failure, non-zero completion, invalid result publication, and successful
execution create no such fact. This connection changes no AgentRun lifecycle or retry
policy and adds no scan, retention, cleanup, migration, or cross-store transaction.
The supported `scheduler-cycle`, `scheduler-drain`, and `scheduler-service` commands
share one optional all-or-none runtime-event store root, publication root, and capacity
group. The shared Scheduler composition constructs at most one filesystem recorder and
passes it to process-isolated execution, the AgentRuntime recovery performed by the
shared worker and dispatcher, the Worker's retry controller, and
`DurableAgentRunFinalizer`; omitted configuration remains event-free. The checkpointed
terminal Result is exact-replayed through finalization before retry or queue-disposition
recovery, preserving verification -> optional Tool timeout -> optional stagnation before
later retry facts and queue disposition -> termination afterward. Cancellation
application receives no recorder through this boundary.

Lease-expiry recovery is owned by `DurableAgentRuntime` and the same AgentRuntime state
that owns leases and reclamation. Current schema v5 retains the bounded ordered ledger
of exact `LeaseTimeoutRecord` values. Reclaim appends the current AgentRun, owner,
fence, issue, expiry, and observation facts while transitioning `EXECUTING -> READY` in
one durable revision; unexpired or non-executing recovery adds none. The filesystem
store enforces an exact prefix and validates a single append against that transition.
Event-aware recovery derives `TIMEOUT_DETECTED` with `RuntimeTimeoutKind.LEASE`,
occurrence at the retained lease expiry, Work-message causation, producer
`durable-agent-runtime`, and
stable reference `agent-runtime/<goal>/lease-timeout/<agent-run>/<fence>` only after
that runtime revision is durable. Replaying the bounded retained ledger repairs missing
events or publication failure after later runtime progress without another source
revision. The shared supported Scheduler composition supplies its optional recorder to
both WorkItem-matched dispatcher recovery and direct worker recovery, so cycle, drain,
and service can materialize the exact event and opaque point without changing reclaim
authority. Earlier runtime schemas, automatic post-reclaim execution, scans, cleanup,
retention, and other owner composition remain separate.

The implemented first concrete adapter is `FileSystemRuntimeEventPublisher`, which
implements the existing port without using `MessageEnvelope`. It publishes exactly one
validated opaque reference into a caller-owned local root under deterministic point
name `sha256(reference-utf8).runtime-event-reference`. The schema-v1 point is a bounded
integrity envelope containing fixed magic and version, declared strict-UTF-8 reference
length, SHA-256 digest, and the reference itself; it contains no event body, credential,
authority, route, acknowledgement field, or application state. Capacity is
caller-bounded from 1 through 4096 pending points.

Publication forces a same-root candidate and atomically moves it without replacement.
Before capacity evaluation, an existing deterministic pending point or its retained
`sha256(reference-utf8).runtime-event-received` sibling is fully validated. Exact state
replays without rewrite and an acknowledged sibling prevents pending-point recreation;
both siblings present, or a symbolic, non-regular, oversized, malformed, unsupported,
digest-invalid, or reference-mismatched point, fails closed. Only pending
`.runtime-event-reference` files consume capacity. Instance-local serialization bounds
concurrent calls through one adapter; no cross-process lock, directory fsync, scan,
cleanup, retention, or cross-store transaction is claimed. Adapter success means only
local point acceptance. Source transition -> event append/exact replay ->
reference-point publication remains the durable order, and a publication failure
remains recoverable from the already-durable event.

The existing `MessageEnvelope` remains sealed to Work, Result, Control, and Handoff.
Runtime events must not be coerced into one of those meanings. A concrete Message Bus
publisher requires a separate accepted Gate 7 wire-schema evolution. The current
publisher remains a caller-supplied port in the named integration paths. Supported
producer compositions are the optional cancellation-request path in
`scheduler-receive-control` and the shared Scheduler execution path for process, lease,
retry, verification, Tool-timeout, stagnation, and terminal-disposition facts. Message
Bus adaptation and supported authenticated-cancellation application remain separate.

The first downstream consumer is deliberately read-only. `FileSystemRuntimeEventPointReader`
takes one caller-named canonical `.runtime-event-reference` file under an explicit
publication root and one `RuntimeEventStore`. It scans neither root. The reader requires
a regular non-symbolic point, decodes the existing bounded strict-UTF-8 integrity
envelope, proves that the filename is the deterministic SHA-256 name of the decoded
`runtime-event/<goal>/<event>` reference, point-resolves only that canonical Goal's
stream, finds the exact event inside its bounded prefix, and re-derives the same
reference from the event before returning `RuntimeEventPointResolution`.

The separate supported `runtime-event-read` command supplies the event root,
publication root, and exact point filename explicitly and reports only bounded typed
identity, kind, time, provenance, stream-revision, and reference-count metadata.
Repeated success and every failure leave both roots unchanged; missing, corrupt,
symbolic, malformed, foreign, or mismatched state fails closed. This consumer does not
acknowledge, rename, delete, scan, apply, route, retry, or grant authority. Durable
consumer receipts, cleanup/retention, Message Bus delivery, and event application
require their own contracts.

The separate `FileSystemRuntimeEventPointAcknowledger` accepts that same original
canonical pending filename and resolves exactly one pending point or its deterministic
acknowledged sibling without scanning. Both states repeat the reader's full point,
reference, Goal-stream, exact-event, and derived-reference validation. First
acknowledgement atomically renames the point in the same root without replacement to
`.runtime-event-received`; exact acknowledged re-entry validates again and returns
`ALREADY_ACKNOWLEDGED` without another mutation. The retained acknowledged point is a
receipt only for this exact observation boundary. It does not prove handler delivery or
event application, and this contract never deletes the event or point.

The supported `runtime-event-acknowledge` command composes that boundary from explicit
event/publication roots and the original pending filename, reports acknowledgement
status and the same bounded event metadata, and preserves point/event contents and the
event-stream revision. The existing `runtime-event-read` command remains pending-only
and read-only. Cleanup, retention, bounded acknowledged history, arbitrary handlers,
consumer identity/offsets, Message Bus delivery, and event application remain separate.

Which connections exist today is stated in `PROJECT_STATE.md`; the cross-boundary connection sequence remains dependency ordered:

| Order | Connection | Owning boundary | Required durable ordering |
|---|---|---|---|
| 1 | terminal queue disposition | Gate 8 Scheduler | distinguish verified completion from failure before changing the dependency-satisfaction set |
| 2 | RunRecord-backed result finalization | Gate 7 result delivery + Gate 8 runtime | durable RunRecord resolution -> matching `ResultPayload` -> persisted AgentRun/Goal terminal state -> matching queue disposition |
| 3 | process-isolated worker and local IPC | Gate 7 transport + Gate 8 worker runtime + Gate 11 Tool controls | worker cycle-intent persists before the claim; the RunRecord reference persists before spool cleanup and acknowledgement; exact work/result route and record binding fail closed; transport acceptance never means bus delivery or work completion |
| 4 | durable cancel/pause/resume | Gate 7 control delivery + Gate 8 request state + Gate 12 authenticated application | persist the bound request before handler success; later application must persist accepted control state before exposure and cannot create scope or authority |
| 5 | external-effect commit and adapter evidence | Gate 8 Scheduler, with the owning Tool/adapter gate | persist prepared intent under the current fence -> invoke through an authorized adapter -> persist the evidenced applied, deduplicated, compensated, or user-recovery outcome |
| 6 | retry and replacement AgentRuns | Gate 8 Scheduler | persist attempt result without queue failure -> bind exact Goal ledger and attempt count -> persist typed decision -> append a checkpointed immutable AgentRun or terminally abandon Goal -> write one final queue disposition |
| 7 | typed handoff and multi-agent execution | Gate 13 over Gates 7 and 8 | require an Operational single-agent baseline, isolated ownership, deterministic synthesis, and one Kernel terminal-state coordinator |

Each cross-store step persists its earlier authoritative artifact before the later derived artifact. Recovery re-enters idempotently from the durable prefix; it does not claim an atomic transaction. Authenticated control application, production external adapters and their Gate 11 controls, and handoff coordination remain unimplemented until their own bounded tasks and fresh integration evidence exist.

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

**Option A — keep the queue item active through verification.** Accepted, and in force. It is the smallest change consistent with the schema-v4 queue and single-worker design, preserves Verified-only completion without a new intermediate queue state, keeps crash recovery and cross-store ordering provable, and prevents another WorkItem from starting while the current result is unresolved. Its cost is real: verification latency occupies the single Scheduler slot, and a slow or unavailable verifier blocks unrelated ready work in that queue.

**Option B — add a non-terminal awaiting-verification queue state.** Deferred, not rejected. It releases the execution slot while verification proceeds and permits another independent WorkItem to execute without falsely satisfying dependencies. It requires a durable queue-state and schema change with recovery rules for the waiting set, separate execution and verification capacity limits and backpressure, ordering and fairness between the two stages, cancellation/timeout/restart/orphan behaviour for both, and an explicit rule that waiting work stays outside the dependency-satisfaction set. Reconsider only once the terminal-disposition and result paths are Contract Verified and verification throughput is a demonstrated bottleneck.

**Option C — mark the queue completed at execution acknowledgement.** Rejected. It is simple and releases capacity, but it lets dependent work start before independent verification and makes a worker receipt equivalent to completion authority, which conflicts with the Constitution-backed verification model, Gate 3/4 behaviour, the runtime AgentRun states, and the Gate 8 Verified-only terminal contract.

## Gate 9 Model Gateway Boundary

The RFC-0013 minimum slice places all model invocation behind one provider-neutral
port in the `com.enhancer.model` leaf package. `ModelGateway.invoke` maps one
immutable bounded `ModelRequest` — correlation identity, bounded UTF-8 prompt, a
repository-owned model-class label rather than a provider model name, and a budget
stub of one timeout plus one maximum response length — to one bounded
`ModelResponse` with a `ModelUsage` unit count, or fails with one typed
`ModelGatewayException` carrying exactly one of `PROVIDER_UNAVAILABLE`,
`RESPONSE_INVALID`, `BUDGET_EXCEEDED`, or `TIMED_OUT`.

`DeterministicFakeModelGateway` is the only executed gateway: its response is a pure
function of the request, so persisted evidence digests stay reproducible. One
package-private HTTP message-API adapter shape bounds where provider wire formats
may exist; nothing constructs it, and executing a real provider requires its own
accepted decision naming destination, purpose, and data classification. Credentials
exist only as the injected default-free `ModelCredentialSupplier` port and can never
reach evidence, logs, or persisted types.

`ModelInvokeTool` composes the gateway into the existing Tool executor under the
name `model-invoke`, reusing isolation, policy allowlisting, cancellation, the
per-tool timeout, evidence capture, and RunRecord persistence unchanged. Each
request declares exactly one prompt source: the inline `prompt` argument or a
governed `prompt-path` file read with the same containment, bounded-size, and
strict UTF-8 rules as governed read-file work. The declared gateway timeout must
fit strictly inside the tool's policy timeout, every gateway failure maps to a
bounded typed `ToolResult` failure code, and model output is untrusted data: it
grants no authority, widens no scope, and alters no document, task, or policy. The
governed `model-invoke` CLI command completes the vertical slice through the
existing controller, loop, digest-integrity verification, and durable RunRecord
path. Model routing, MCP, caching, fallback, streaming, evaluation, and remote
transmission controls remain later Gate 9 scope.

The Scheduler execution boundary derives each WorkItem's pipeline from its
allowed-tool scope: a scope containing `read-file` runs the original governed
read-file pipeline unchanged, any other scope containing `model-invoke` executes
the deterministic fake through `ModelInvokeTool` — the declared execution input's
target path is the governed prompt document, its expected SHA-256 is the expected
response digest verified by `DeterministicModelInvokeVerifier`, and the WorkItem's
required capability is the model-class label — and a scope naming neither
executable tool fails closed before any execution. Model-scoped work requires a
declared execution input, because the source-document fallback digest names the
document rather than a response. The isolated-result validation and the read-only
invocation recovery status apply the same scope-derived expectation, and the
governed submission surfaces accept any task scoped to at least one executable
tool. No queue, runtime, submission, or spool schema changed for this boundary.

RFC-0014 defines the next Gate 9 value boundary: one immutable, versioned
`ModelExecutionProfile` carries a required capability, model class, locality and
reasoning requirements, minimum context, token, cost, and invocation-time budgets,
and data classification. The value is untrusted requirement data and cannot select a
provider, authorize transmission or spend, expose credentials, or widen task, Tool,
or `ExecutionPolicy` authority. Its immediate consumer is a pure model-package value
layer only. A later separately authorized composition must attach a complete profile
explicitly to the request path and intersect it with task, execution, and outbound
policy; defaults, routing, providers, and remote execution remain outside this
boundary.

RFC-0015 defines the minimum compatibility-preserving composition as one immutable
`ProfiledModelRequest` retaining one complete RFC-0013 `ModelRequest` and one complete
RFC-0014 `ModelExecutionProfile`. It is additive: the five-component request, gateway,
fake, Tool, CLI, Scheduler, adapters, and schemas stay unchanged. Construction requires
exactly aligned model-class labels and a profile invocation-time ceiling no greater
than the request gateway timeout. Response-character and token ceilings remain
deliberately incomparable, and required capability remains distinct from model class.
The composition proves alignment only; it performs no policy evaluation and grants no
route, provider, network, credential, transmission, spend, Tool, or task authority. A
later separately authorized runtime consumer must intersect task authority,
`ExecutionPolicy`, the complete profile, and outbound/provider policy before any
gateway or adapter integration.

RFC-0016 defines the next pure Gate 9 boundary before any gateway execution. A
stateless `ModelInvocationAdmission` evaluates one complete `ProfiledModelRequest`,
the exact active `ApprovedTask`, the exact active `ExecutionPolicy`, and a separately
sourced authoritative required capability. It requires both task and policy to allow
`model-invoke`, exact capability/profile agreement, and the full profile-time-at-most-
gateway-time-strictly-within-policy relationship. Evaluation is deterministic with one
closed first-match rejection reason. With no outbound/provider policy, `LOCAL_ONLY`
may pass as local eligibility while `POLICY_CONSTRAINED` fails closed; an admitted
decision is not gateway permission, provider suitability, remote authority, or a
persistable token. The current CLI and Scheduler lack all required explicit sources,
so runtime wiring, candidate suitability, routing, providers, and transmission remain
separate contracts.

RFC-0017 defines the minimum caller-side source obligations upstream of that pure
boundary without adding a new aggregate or port. One future caller must resolve an
exact complete `ModelRequest`, one indivisible already-valid `ModelExecutionProfile`,
the exact active `ApprovedTask` and `ExecutionPolicy`, and an unchanged authoritative
required-capability projection from a separately named governed source. Profile data
cannot self-certify capability authority, and missing or partial input cannot fall back
to constants, Tool arguments, a registry, ambient lookup, or a legacy path. The current
direct CLI has neither required source; the current Scheduler can later project the
active `WorkItem.requiredCapability` but has no complete profile source and must not
reuse capability as model class. Both remain unchanged and unsupported. Persisting a
profile through Scheduler work would require a separate versioned message, submission,
queue, runtime, recovery, and migration contract; RFC-0017 changes no Java or durable
schema and grants no candidate, gateway, provider, route, network, credential,
transmission, or spend authority.

RFC-0018 defines the Scheduler-specific durable source as a fifth typed
`ModelWorkPayload` carrying mandatory target path, expected-response digest, and one
exact complete `ModelExecutionProfile`. The existing `WorkPayload` stays the unchanged
read-file representation, while payload kind selects new model work even when task
scope also permits read-file. The profile remains untrusted and contains no capability
authority; the exact active `WorkItem.requiredCapability` stays a separate governed
projection and must remain capable of disagreeing until RFC-0016 evaluates it.

The transport uses a payload-sensitive model-work representation rather than a
global version replacement: existing Work, Result, Control, and Handoff envelopes and
spool frames retain their exact v1 bytes, including the Gate 12 cancellation-signing
input, while only model work uses envelope/spool v2. Submission manifest v3, Scheduler
queue v4, and AgentRuntime v5 embed that exact envelope/profile so retry and process
recovery require no manifest-only, registry, or ambient lookup. This durable retention
family is current. Migration-only readers inspect manifest v1-v3, queue v2-v4, and
AgentRuntime v4-v5 without widening ordinary current-only resolution. One explicit
point-resolved plan validates an externally held stopped-owner fence, the complete named
manifest/queue/runtime closure, optional spool and binding points, exact cross-store
WorkItem content, and stable source bytes before returning `READY` or non-writing
`ALREADY_CURRENT`. Legacy read-file work, including mixed Tool scope and absent input,
is retained losslessly; unprofiled model work refuses as
`UNMIGRATABLE_LEGACY_MODEL_WORK` / `PROFILE_REQUIRED`, and partial, invalid, mismatched,
corrupt, future, or drifted closures refuse before candidates or targets are written.
After a successful preflight, `CoordinatedDurableMigrationCutover` resnapshots every
named source, prepares and rereads same-directory current-schema candidates for each
legacy AgentRuntime, queue, and manifest, and revalidates the stopped-owner fence plus
immutable binding points before publication. Result and work spool points are first
validated without replacement because their payload-sensitive legacy or ModelWork wire
family is already current; current-schema candidates then replace AgentRuntime, queue,
and manifests atomically after exact source-byte rechecks, followed by the same no-write
validation of named ingress points. A final complete preflight must resolve the closure
as `ALREADY_CURRENT`. Crash-boundary re-entry, already-current prefix preservation, and
exhaustive source-drift behavior are current: every ordered publication exposes a
package-private post-publication interruption seam, re-entry skips candidate creation
and publication for each exact current prefix, resumes at the first old store, and
never rolls back or rewrites that prefix. Fence, binding, immutable spool, runtime,
queue, manifest, and ingress drift all return the typed source-invalid refusal while
preserving the changed source and every later target, and failed operations remove
their remaining candidates. Ordinary current-only readers still reject the old suffix
of a mixed closure; only the explicit migration path may resume it. No cross-store
atomicity or directory power-loss property is claimed.
The supported `scheduler-migrate-durable-closure` CLI constructs that path only from
explicit roots, repeatable submission/Goal identities, and exact optional spool and
binding files. It verifies a non-empty bounded fence file against a caller-supplied
SHA-256 before constructing the plan, performs no filesystem discovery, and reports
`MIGRATED`, non-writing `ALREADY_CURRENT`, or the typed refusal pair. This operator
surface creates no submission, receive, admission, RunRecord, Tool, gateway, provider,
network, or model-execution authority.
Profile-aware execution remains blocked pending candidate suitability, a proven-local
gateway boundary, and later runtime/finalization/recovery wiring.

RFC-0019 defines that next additive provenance and preparation boundary without
enabling execution. Existing `RunRecord` payload v1 and its public v1-only store/resolve
path remain unchanged. The implemented separate `ModelRunRecord` retains one canonical
WorkItem identity, the unchanged independent capability projection, the exact ModelWork
envelope, the exact prepared `ModelRequest` including its bounded prompt snapshot, and
the existing lifecycle record. It structurally binds those values while deliberately
leaving WorkItem and profile capability independent. Separate model store/resolve ports
and a closed two-kind mismatch vocabulary prevent v2 from being projected through the
v1 type-level boundary. The shared filesystem store now dispatches payload version 1
to the exact legacy record and canonical payload version 2 to the model record while
retaining the same outer envelope, four-MiB bound, artifact/reference namespace,
atomic publication, opaque listing, and exact replay. Each typed resolver rejects the
other known kind, unknown or noncanonical input remains corruption, and cross-kind or
changed-content identity reuse leaves the first artifact unchanged. Literal and newly
encoded v1 payloads are byte-identical. No current production caller writes v2.

Before a typed model attempt, `ExactActiveTaskResolver` accepts only an explicit project
root and typed ModelWork `WorkItem`. On every call it loads the complete governed
context once through `ProjectContextReader`, resolves the exact `In Progress`
`ApprovedTask` once through `ApprovedTaskReader`, hashes the already decoded complete
source content as lowercase SHA-256, and requires exact retained task ID, source path,
digest, and immutable Tool-set equality. Legacy work and each mismatch fail with a
closed typed reason; reader failures retain their existing type. Successful resolution
returns the same reader-produced task instance and adds no cache, store, registry,
ambient lookup, or caller. It neither uses the WorkItem source path to select authority
nor rereads the source after context capture.

`SchedulerModelInvocationPreparer` is the standalone invocation-scoped implementation
of the next boundary. It receives immutable `SchedulerModelInvocationLimits` plus
explicit policy inputs, resolves the exact active task first, constructs one
`ExecutionPolicy` allowing only `model-invoke`, reads the typed target once through
`GovernedModelPromptReader`, builds `ModelRequest` with the exact profile model class,
composes RFC-0015, and evaluates RFC-0016 with that same policy object and unchanged
WorkItem capability. `GovernedModelPromptReader` owns the previously private model Tool
file-read logic unchanged, so current real-path containment, regular-file, streaming
byte ceiling, mutable-growth, and strict-UTF-8 behavior has one implementation shared
with `ModelInvokeTool`.

The immutable `SchedulerModelInvocationPreparation` retains the exact resolved task,
same policy, exact profiled request, and ephemeral decision for the separately accepted
next boundary. An admitted decision must retain that same profiled-request instance.
Every call rereads task and prompt authority and reevaluates admission; no cache,
registry, persistence, production caller, candidate, Tool execution, gateway, evidence,
or Model RunRecord writer exists. The exact prompt/request must reach any later
invocation without a second mutable-file read.

Preparation stops at the absent candidate-suitability boundary even when RFC-0016
returns `Admitted`; the deterministic fake is not locality or suitability proof. Before
a v2 record exists, a later actual retry repeats exact task/request/policy sourcing and
fresh admission. After an exact v2 reference exists, recovery validates complete
WorkItem/message/capability/profile/request/policy/result provenance and resumes only
deterministic finalization without another task lookup, admission, Tool, or gateway
invocation. Runtime disposition for pre-execution refusal, candidate/local-gateway
proof, execution/finalizer/recovery wiring, typed submission or receive, providers,
network, credentials, and spend remain separately accepted work.

RFC-0020 and RFC-0021 implement a closed local-candidate boundary without enabling
invocation. One opaque final `DeterministicFakeModelCandidate` factory accepts only an
exact final `DeterministicFakeModelGateway`, retains that same gateway instance, and
supplies fixed repository-owned deterministic-echo, token-semantics, capacity, cost,
and classification facts. It accepts no generic gateway, caller-provided metadata or
capacity, registry, provider, route, endpoint, credential, price, tokenizer, or ambient
default. The candidate and its suitability result are process-local and non-persistent.

The token-aware candidate identity is `deterministic-fake-v2`;
`deterministic-fake-v1` permanently retains its historical token-unavailable meaning.
The field-free `DeterministicFakeTokenCounter` implements fake-only
`deterministic-unicode-scalar-v1`: it counts well-formed Java-string Unicode scalars as
`long`, performs no normalization or encoding, and fails closed on malformed
surrogates without reproducing input. Its package-private fake-response algebra uses
checked arithmetic. It is not a provider token or a mapping from generic `ModelUsage`.

For an exact fake prompt with UTF-16 length `n`, scalar count `s`, and decimal digit
count `d(n)`, the response has UTF-16 length `n + 152 + d(n)` and scalar count
`s + 152 + d(n)`. The accepted fixed capacities are 524,288 context, 262,144 input,
262,144 output, and the tight 524,130 combined total. An ASCII prompt of 261,986 units
produces the exact 262,144-unit response ceiling and combined total; one more unit
exceeds the existing response bound. Context remains the larger independent input-plus-
output envelope so RFC-0020's context and total rejection reasons stay distinct under
RFC-0014's profile invariant.

The field-free `ModelCandidateSuitability` receives the exact RFC-0016 `Admitted`
value and exact candidate. It checks model class, capability, reasoning, token
availability, context, input, output, total, zero-cost, and public-classification facts
in the closed first-match order, then returns a reachable ephemeral `Suitable` retaining
the exact inputs. Suitability compares declared profile requirements with fixed
candidate facts only and performs no actual-request counting. Source and reflection
guards keep all five definition types free of I/O, Tool, generic-gateway, provider,
credential, evidence, RunRecord, runtime, and production-caller wiring.

A separately accepted same-request seam must count and
validate the exact admitted prompt and predicted fake response against request and
profile budgets before any gateway activity, while retaining the same request, policy,
candidate, and gateway identities. The gateway's `deterministic-fake-v1` rendering,
generic character-based `ModelUsage`, `ModelInvokeTool`, Scheduler preparation,
schemas, execution guards, runtime, and recovery remain unchanged.

RFC-0022 specifies that seam without implementing it. A future field-free
`DeterministicFakeExactRequestPreparation` accepts only exact `Suitable` plus the
RFC-0019 policy instance, counts the retained request prompt once, and applies
malformed-input, actual-input, predicted response UTF-16 length, predicted output, and
checked-total comparisons in order. Only its private-construction opaque `Ready`
variant can enter the field-free invoker; request, policy, candidate, and gateway are
derived through that retained identity chain rather than supplied again.

The total-budget reason stays as a defensive stable branch even though RFC-0014's
`maxInput + maxOutput <= maxTotal` invariant makes it unreachable after both
individual checks for every valid current profile. The future invoker rechecks the
retained policy allowlist, strict timeout relationship, and current cancellation before
one exact candidate-bound gateway call. Its opaque result distinguishes returned
untrusted response, pre-call refusal, and unchanged `ModelFailureCode` without raw
exception text. This standalone seam supplies no ToolExecutor isolation, evidence,
verification, RunRecord, retry, runtime, or production-caller authority; later typed
ModelWork process integration must prove the exact Scheduler policy identity and own
those effects.

## Agent Orchestration Contract

### Development-Time Adaptive Subagent Delegation

Repository development sessions may use host-provided subagents as an execution
topology inside the existing user request and Active Task. The primary Agent evaluates
non-trivial work and selects the smallest topology whose independent analysis,
alternative comparison, risk review, or test-surface review produces a material
quality, safety, or latency benefit over coordination cost. Local, sequential, tightly
coupled, overlapping-write, or ambiguous work remains single-agent.

The initial contract is read-only: at most three children execute concurrently, depth
is one, each assignment has one concrete scope and join result, and the primary Agent
owns all repository mutation, checkpoint/Git state, authority reconciliation, raw-
evidence validation, synthesis, and lifecycle claims. Reports are untrusted
recommendations. Delegation cannot enlarge scope, Tools, permissions, budget,
external/destructive authority, or lifecycle state; every child is joined or stopped
before increment/session completion.

This policy may be used with or without a document-driven Dynamic Workflow. Workflow
increment selection remains sequential, while independent read-only inspection may run
inside the selected increment. This host development-session contract is not the Gate
13 `CoordinationPlan`, typed Handoff/runtime worker orchestration, background execution,
or a product capability-maturity claim. RFC-0009 and the product connection sequence
remain unchanged.

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
- `RFC-0013`: Model Gateway
- `RFC-0014`: Model Execution Profile
- `RFC-0015`: Profiled Model Request
- `RFC-0016`: Model Invocation Admission
- `RFC-0017`: Model Invocation Input Sourcing
- `RFC-0018`: Scheduler Model Profile Transport
- `RFC-0019`: Scheduler Model RunRecord And Admission Preparation
- `RFC-0020`: Deterministic Local Model Candidate Suitability
- `RFC-0021`: Deterministic Fake Token Semantics And Capacity
- `RFC-0022`: Deterministic Fake Exact-Request Budget And Invocation Seam

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

Gate 5 selects `com.enhancer.cli.EnhancerCli` as the first supported local entry point and exposes `run`, `replay`, and bounded `run-record-list` inspection. The Gradle application entry point is a thin composition boundary over the existing Context Reader, repository-derived `ApprovedTask`, read-only Tool execution, sequential verifier, finalizer, Evidence Store, and RunRecord Store.

The implemented command is intentionally non-interactive. Every final worker outcome that reaches finalization is persisted before its stable exit code is returned; configuration errors and internal failures remain bounded diagnostics rather than fabricated records. This is the first Operational scenario, not the future multi-interface CLI of Gate 12.

`run` requires explicit project root, task identity, relative target path, expected SHA-256 digest, evidence root, and RunRecord root. The supplied task identity must equal the active task read from the governed project context; it does not create or approve a task. The command registers only `read-file`, applies the existing 64 MiB ceiling and a five-second timeout, and performs at most five loop iterations with the existing three-transition stagnation threshold.

Process results use stable exit codes for completed, usage/configuration error, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure. Diagnostics are bounded and never include complete target content. A finalized run prints its opaque RunRecord reference and storage root.

`replay` requires an explicit RunRecord root and reference, resolves the integrity-checked record through `FileSystemRunRecordStore`, and prints bounded typed task, request, policy, verification, and stop metadata. It does not re-execute the Tool or reinterpret the record as repository authority.

`run-record-list` requires an explicit RunRecord root and a 1-through-48 limit. It
projects the exact newest-first prefix returned by
`FileSystemRunRecordStore.recentReferences`, reports available/empty status and opaque
references only, and never creates a missing root, resolves a record, validates record
content, or changes ordering policy. Detailed integrity and lifecycle inspection remains
the separate `replay` responsibility. The CLI-specific limit keeps the complete listing
inside the shared 4096-character output boundary.

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

### Document-Driven Dynamic Increment Workflow Boundary

`CURRENT_TASK.md` remains the single Active Task and next-task authority. When one
explicitly approved task contains two through sixteen related bounded increments, its
optional `## Dynamic Workflow` section may describe their execution graph without
creating another task ledger. The parent Task, Task ID, Approval, Acceptance Criteria,
Allowed Tools, and Out Of Scope sections are the fixed authority envelope; increment
entries can narrow that envelope but cannot add scope, Tools, external actions, or
approval.

The v1 document grammar is deliberately sequential and bounded. It records one stable
workflow identity, `Sequential` mode, the declared increment limit, a deterministic
selection rule, explicit stop conditions, and ordered stable increment identities. Each
increment records `Pending`, `In Progress`, `Completed`, or `Blocked`, its dependency
identities, bounded scope, exit criteria, verification expectation, and next action. At
most one increment is `In Progress`. The only eligible successor is the first ordered
`Pending` increment whose dependencies are `Completed`, and it may be selected only
after their required evidence has been read.

Increment state is execution context, not a new constitutional lifecycle or evidence
store. `Completed` is valid only when the increment's exit criteria and declared fresh
verification are satisfied; the task becomes Completed only after every required
increment is Completed and the normal session-close synchronization succeeds. The
machine checkpoint continues to bind the parent task contract and records atomic
execution position; it neither selects an increment nor proves its completion.

Failure, a blocked dependency, stagnation, exhausted increment/time/cost/context bound,
task-contract drift, newly required authority, or unsafe recovery stops selection. An
unplanned increment or authority change requires explicit user approval and an accepted
task/decision update before continuation. Commit, push, merge, release, deployment,
destructive action, background execution, and Gate 13 multi-agent dispatch remain
separate authority or later-gate capabilities. The adaptive development policy above
may run bounded read-only inspection inside the selected increment; the Dynamic Workflow
does not grant that topology or widen its authority. This document contract is not the
Gate 10 Workflow Engine or Gate 13 orchestration runtime.

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

- Provider and general model context/token strategies are not selected; RFC-0021
  implements fake-only Unicode-scalar semantics and fixed capacities, while RFC-0022
  specifies only the still-unimplemented exact-request and exact-fake invocation seam.
- Future LLM-backed Planner input/output schema is not selected yet.
