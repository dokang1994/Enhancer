# Roadmap

## Roadmap State Model

Roadmap state describes capability maturity, not the task lifecycle in the Constitution.

| Maturity | Meaning | Required evidence |
|---|---|---|
| Specified | Responsibility, boundaries, dependencies, and exit criteria are accepted | Architecture or RFC review |
| Contract Verified | Core types and invariants exist | Focused contract tests |
| Integrated | Real upstream and downstream components are connected | Integration tests |
| Operational | A supported entry point runs the capability against a real project | End-to-end evidence and recovery instructions |
| Released | The operational capability is intentionally distributed | Release and installation evidence |

The standalone label Implemented is no longer used for capability maturity. It may describe a completed task, but it must not imply that a capability is integrated or operational.

## Current Position

Status: Delivery Gate 6 Integrated with an Operational production composition; Delivery Gate 7 Contract Verified; Delivery Gate 8 Specified - Next

Integrated capabilities:

- repository context reading with `.ai/` before canonical root documents;
- deterministic next-task proposal from the canonical Delivery Gate grammar;
- single-pass context-to-planner orchestration;
- bounded repeated-loop termination and consecutive stagnation detection;
- bounded Tool result and verification-evidence records.
- governed Tool request, policy, registry, and execution boundary;
- one allowlisted UTF-8 read-only filesystem Tool;
- bounded conversion of real Tool success and failure into `ToolResult`;
- atomic complete-evidence persistence with resolvable integrity-checked references.
- repository-derived approved tasks with Tool scope and structured failure codes;
- Tool-result-driven Agent Loop transitions with verification-wait, terminal, retry, iteration, and semantic stagnation outcomes.
- sequential independent read-file verification over inline or referenced complete evidence;
- atomic integrity-checked RunRecord persistence and restart-safe replay.

Operational repository governance:

- Constitution 1.2 Kernel;
- Document Driven Development;
- explicit lifecycle and authorization rules;
- Git-backed project memory and session handoff;
- test and evidence requirements.

Operational capability:

- first supported local `run` and `replay` CLI over the governed read-only vertical slice.

Not yet integrated or operational:

- LLM invocation;
- remaining Workspace adapters, Project Brain graph persistence, Event/Message Bus production wiring, concrete IPC adapters, broader Agent Runtime and Scheduler production paths, and Model Gateway;
- Skill runtime, plugins, MCP, interfaces, multi-agent, background execution, Cloud Sync, and self-improvement.

## Contract Continuation Rule

Foundation contracts may continue after the initial contract phase, but each new contract must:

1. name its integration consumer in the current or immediately following delivery gate;
2. define observable behavior and a failure mode;
3. include focused tests;
4. identify the integration test that will promote it beyond Contract Verified;
5. avoid opening a later platform track before its dependency gate exits.

A contract without an identified consumer remains a proposal and does not become the next implementation task.

## Product Milestones

- **V1 - AI Development Experience:** Cursor-level developer productivity through the first CLI, Workspace, and later Desktop/editor/API surfaces. V1 is an application experience over Enhancer Kernel contracts, not the project's final identity.
- **V2 - AI Development Platform:** Event/Message Bus, Agent Runtime, Scheduler, Workflow Engine, Skills, Memory, MCP, Model Router, plugin/Agent marketplace foundations, and self-hosting development workflows.
- **V3 - AI Operating System:** AI Kernel, mature Project Brain graphs, multi-agent OS, privacy-aware hybrid model routing, plugin ecosystem, governed synchronization, and self-improvement.

The repository is currently building shared foundations. These milestone labels do not override capability maturity and do not claim that V1, V2, or V3 is implemented.

Product milestones describe externally meaningful outcomes; Delivery Gates define dependency-ordered implementation and promotion. Platform foundations associated with V2 may therefore be built before every polished V1 interface is released without changing the meaning or maturity of either milestone.

## Cross-Cutting Product Journey And Evaluation Track

Status: Accepted

This track measures whether users can complete development work across multiple gates. It does not replace or reorder Delivery Gates, and passing a component test does not automatically pass a journey.

Initial canonical journeys:

- governed bug repair: reproduce -> plan -> authorize -> change -> test -> independent review/verification -> risk and diff review -> commit-ready boundary;
- bounded feature delivery: accepted goal -> scoped plan and budget -> implementation -> tests/evidence -> compatibility and rollback review -> approval-ready result;
- evidence-backed codebase explanation: repository question -> snapshot-bound inspection -> source-attributed answer with freshness/uncertainty -> no repository mutation;
- interrupted-run recovery: induced interruption -> durable checkpoint and fenced ownership decision -> resume, deduplicate, compensate, or stop -> visible recovery history.

Every journey uses versioned fixtures that name repository and policy revisions, supported interface, required approvals, budgets, expected artifacts, induced failures, and scoring rules. A journey becomes Operational only when a supported interface completes it end to end with inspectable evidence and documented recovery.

Evaluation-harness requirements:

- report task success over all attempted fixtures;
- report incorrect or unauthorized changes over all change-producing attempts;
- report retry/recovery success over all induced failure cases;
- report median and tail cost/time while retaining failed-attempt cost;
- report clarification, repair, and exceptional-authority interventions separately from mandatory approvals;
- report held-out post-verification regressions over all completed change fixtures;
- compare multi-agent results with the single-agent baseline on the same fixture version and comparable cost, time, context, and Tool budgets;
- fix fixture versions, thresholds, and scoring rules before a release evaluation, and retain run/model/policy/revision/evidence/evaluator provenance.

Release-quality rule:

- applicable Delivery Gates must satisfy their own maturity criteria;
- release-scoped canonical journeys must meet their predeclared thresholds;
- Agent confidence, reviewer self-report, a single demonstration, or cherry-picked successful runs cannot satisfy the track.

## Cross-Cutting Default Security Baseline

Status: Accepted

- Treat repository instructions, Tool/terminal output, model responses, MCP content, plugins, dependencies, and generated artifacts as untrusted data that cannot create authority.
- Preserve provenance, freshness, bounds, and instruction/data separation at ingress.
- Detect secrets and sensitive data before persistence, logs, caches, display, or external transmission; apply explicit data-classification and outbound-destination policy.
- Require least-privilege Tool scope, containment, previews, dry-run where supported, bounded execution, audit evidence, and recovery guidance.
- Require permission manifests, integrity/signature provenance, compatibility/dependency validation, isolation, malicious-package review, disablement, removal, and rollback for Skills and plugins.
- Keep local-only operation complete and make Cloud Sync opt-in; sync, MCP, model, plugin, and Tool content cannot grant execution authority.
- Gate 9 owns model/MCP transmission and attribution controls, Gate 11 Tool/extension supply-chain controls, Gate 12 approval and change-review visibility, Gate 14 cloud encryption/key/conflict controls, and Gate 16 signed reproducible distribution, SBOM, migration, offline installation, and rollback evidence.

In this roadmap, **self-hosting development** means Enhancer applies its governed context, planning, execution, evidence, and verification workflow to the Enhancer repository. **Local or hybrid model execution** means routing inference to approved on-device or remote providers. The two capabilities are independent: local inference does not prove self-hosting, and self-hosting does not require a particular model host.

## Delivery Gate 0: Foundation Safety Contracts

Status: Integrated

Delivered:

- Context Reader and structured repository context;
- deterministic Task Planner and explicit proposal state;
- Assisted Development Loop result contract;
- Agent Loop completion, failure, iteration, and stagnation exits;
- ToolResult and bounded VerificationEvidence;
- Constitution 1.2 governance.

Integration evidence:

- 6 focused foundation suites contain 35 tests covering Context, planning, Assisted Loop, repeated-loop termination, ToolResult, and VerificationEvidence contracts;
- `FoundationLifecycleIntegrationTest` passed on its first characterization run without a production change or second orchestration path;
- a governed temporary repository produces the Gate 6 Proposal without mutation, rejects execution before activation, and grants authority only after an explicit external fixture transition;
- the activated task reuses the Gate 5 CLI and Gate 1 through 4 boundaries through complete evidence, independent verification, persist-before-completion RunRecord publication, target deletion, and restart-safe replay;
- the combined Gate 0 and Gate 5 focused run passed 43 tests across 10 suites with 1 Windows symbolic-link setup skip;
- the full regression passed 98 tests across 25 suites with 2 Windows symbolic-link setup skips, no failures, and no errors;
- Java 17 production lint passed with `-Xlint:all -Werror`, and Gate 6 remains the sole `Specified - Next` product gate.

Compatibility recovery evidence:

- obsolete Phase/Ready fixtures were replaced with Delivery Gate/Specified - Next fixtures;
- the actual Enhancer `ROADMAP.md` produces the Delivery Gate 2 proposal;
- the actual repository context reads seven `.ai/` documents before eight root documents.

## Delivery Gate 1: Tool Execution Boundary

Status: Integrated

Goal:

Execute one allowlisted read-only Tool through a governed application boundary and return a real ToolResult.

Required contracts:

- ToolRequest with identity, arguments, and correlation identity;
- Tool interface;
- ExecutionPolicy with allow, deny, path, size, timeout, and cancellation rules;
- ToolExecutor that applies policy before invocation;
- read-only filesystem Tool;
- deterministic fake Tool for focused tests.

Required behavior:

- read an allowed UTF-8 file;
- deny paths outside the approved project root;
- convert success and failure into ToolResult;
- bound evidence before it enters Agent context;
- perform no shell mutation, Git operation, LLM call, or network action.

Exit criteria:

- focused tests are written before behavior;
- a real temporary file is read through ToolExecutor;
- permission denial and malformed requests are observed;
- ToolResult invariants remain valid;
- an integration test connects request, policy, Tool, result, and evidence.

Exit evidence:

- all required request, policy, executor, and read-file behaviors have focused tests;
- a real temporary UTF-8 file is read through the governed boundary;
- policy denial, malformed input, timeout, cancellation, Tool failure, and path safety produce bounded structured results;
- the symbolic-link escape test runs where the host permits link creation and is explicitly skipped where Windows privilege policy prevents setup.

## Delivery Gate 2: Evidence Persistence

Status: Integrated

Goal:

Make complete evidence references real, durable, and integrity-checkable.

Required capabilities:

- EvidenceStore interface and filesystem implementation;
- unique run and evidence identifiers;
- content length and digest metadata;
- atomic write behavior;
- reference resolution and existence verification;
- truthful storage policy, with retention and cleanup requiring a separate lifecycle decision.

Exit criteria:

- truncated Tool output is persisted and resolvable;
- corrupted or missing evidence is rejected;
- working output tails remain bounded;
- integration tests cover write, read, corruption, and missing-reference behavior.

Exit evidence:

- UUID run and evidence identities are generated and persisted in opaque contained references;
- one versioned artifact atomically stores creation time, UTF-8 byte length, SHA-256 digest, and complete output;
- a new store instance resolves valid evidence and rejects malformed, missing, oversized, length-mismatched, digest-mismatched, and invalid-UTF-8 artifacts;
- short output remains unpersisted while truncated output receives a resolvable reference;
- a large real file passes through request, policy, `ReadFileTool`, `EvidenceRecorder`, store, and resolution without unbounded Agent context.

## Delivery Gate 3: Agent Loop And Tool Integration

Status: Integrated

Goal:

Make one Agent Loop iteration produce and consume a real ToolResult.

Required capabilities:

- AgentRunState with approved task, pending request, last result, and progress key;
- AgentRunController that owns orchestration without owning Tool implementation;
- deterministic mapping from ToolResult to next loop state;
- retry classification for retryable and terminal failures;
- existing maximum-iteration and stagnation exits.

Exit criteria:

- Context, approved task, Tool request, execution, and loop transition run in one integration test;
- terminal Tool failure cannot be reported as completion;
- repeated identical progress reaches STAGNATED;
- the worker cannot mutate Git or authorize its own external action.

Exit evidence:

- `AgentRunState` carries the approved task, pending request, last Tool result, loop status, and deterministic progress key;
- `AgentRunController` consumes a prebuilt executor, immutable policy, and external retry classifier without creating Tool authority;
- real read-only Tool success stops at `AWAITING_VERIFICATION` rather than claiming completion;
- terminal failure reaches `FAILED`, while four identical retryable results reach the existing three-transition `STAGNATED` threshold;
- a governed temporary repository supplies canonical startup documents, an approved task, `ReadFileTool` input, persisted evidence, and the loop transition in one integration test;
- separate actual-Enhancer regressions verify startup-context ordering and canonical Roadmap planning, but no supported full run against the actual worktree is claimed before the operational CLI gate;
- a deny-over-allow policy prevents an otherwise mutating fake Git Tool from being invoked and leaves its sentinel unchanged.

Hardening task: Completed

- derive structured approved work and Tool scope from active repository context;
- replace prose-dependent failure policy with structured failure codes;
- make progress semantic across changing evidence locations;
- restrict Agent run state construction to governed factories and controller transitions.

Hardening evidence:

- active `CURRENT_TASK.md` context produces an immutable `ApprovedTask` with task identity, approval provenance, and Tool scope;
- out-of-scope requests are rejected before controller execution and still remain subject to immutable execution policy;
- every failed Tool result has a structured failure code, and the standard classifier retries only timeout and explicit temporary failure;
- complete evidence capture provides a SHA-256 content identity while opaque references and prose summaries are excluded from progress;
- identical content stored under changing references reaches `STAGNATED`;
- `AgentRunState` has no public constructor and exposes only the ready factory to callers.

## Delivery Gate 4: Sequential Verification And Run Record

Status: Integrated

Goal:

Verify an Agent result outside the worker step and preserve a replayable run record.

Required capabilities:

- VerificationRequest and VerificationDecision;
- sequential IndependentVerifier interface;
- deterministic verifier for the first scenario;
- RunRecord containing inputs, policy decision, ToolResult, evidence, verification, iterations, and stop reason;
- RunRecordStore with replay and diagnostic reads.

Exit criteria:

- worker output cannot mark itself verified;
- missing or invalid evidence produces an unverified or failed decision;
- completed state requires a successful independent decision;
- a persisted run can be inspected without chat history;
- integration tests cover pass, fail, missing evidence, and stagnation.

Exit evidence:

- `VerificationRequest`, typed decisions, and a sequential `IndependentVerifier` bind approved task, executed request, Tool result, and external expected digest;
- the deterministic read verifier checks inline content or resolves truncated output through `EvidenceStore` and recomputes complete SHA-256 identity;
- missing evidence remains Unverified, while corrupted, structurally invalid, or mismatched evidence is Rejected;
- only a Verified decision creates `COMPLETED`, and RunRecord persistence must succeed before completion is returned;
- failed, stagnated, and maximum-iteration runs persist with verification Not Performed;
- RunRecords contain task, request, policy snapshot and decision, Tool result and evidence, expected digest, verification, iterations, and worker/final stop reasons;
- a versioned binary SHA-256 envelope is atomically published and replayed through a new filesystem-store instance;
- focused Gate 4 verification covers verified, rejected, missing, corrupted, persistence-failure, failed, stagnated, and iteration-limited paths.

Hardening task: Completed

- bind the immutable execution policy to the worker result so finalization cannot substitute audit context;
- reject RunRecord lifecycle combinations that the governed Agent path cannot produce;
- preserve Verified-only completion and persist-before-return behavior.

Hardening evidence:

- `AgentRunResult` retains the controller-owned `ExecutionPolicy` and cannot be publicly constructed;
- finalization derives the persisted policy decision from the worker result without a second policy argument;
- `RunRecord` rejects lifecycle combinations outside verification-wait, verified completion, failed, stagnated, and iteration-limited paths;
- focused hardening verification passed 24 of 24 tests.

## Delivery Gate 5: First Operational CLI

Status: Operational

Goal:

Expose the connected read-only Agent run through a supported local command.

Required capabilities:

- minimal Java CLI entry point over the existing Context, Tool, verification, and RunRecord boundaries;
- explicit project-root, task, target-path, expected-digest, evidence-root, and RunRecord-root inputs;
- stable exit codes and bounded diagnostic output for every final stop reason;
- documented local recovery and record-inspection commands.

First operational scenario:

1. select a project root;
2. load repository context;
3. accept an explicitly approved read-only task;
4. read one allowed project file through the Tool boundary;
5. persist complete evidence;
6. verify the expected result sequentially;
7. stop with an explicit reason;
8. print and persist the RunRecord location.

Exit criteria:

- a documented CLI command runs against a temporary and the actual Enhancer repository;
- exit codes distinguish completion, verification failure, policy denial, stagnation, and internal failure;
- no shell mutation, commit, push, or LLM is required;
- end-to-end tests and a manual smoke test pass;
- recovery and diagnostic instructions are documented.

Operational Milestone 1 is reached only when Delivery Gate 5 exits.

Exit evidence:

- `com.enhancer.cli.EnhancerCli` exposes explicit `run` and `replay` commands through the Gradle application entry point;
- the command derives the active `ApprovedTask`, permits only `read-file`, independently verifies complete content, and persists a RunRecord before reporting completion;
- stable exit codes distinguish completion, usage/configuration, verification, policy, Tool, stagnation, iteration, and internal outcomes;
- CLI output is bounded to 4096 characters and never prints complete file evidence;
- 7 focused CLI tests passed, including temporary-project success, verification mismatch, Tool failure persistence, replay, argument validation, and exit-code mapping;
- the full Gradle regression passed 97 tests across 24 suites with 2 Windows symbolic-link setup skips and no failures or errors;
- a manual actual-repository `README.md` run and restart-safe replay completed with exit code 0 and a Verified decision.
- bounded recent RunRecord discovery is exposed separately through `run-record-list`; one
  explicit 1-48 limit returns the existing store's newest-first opaque references without
  resolving records or creating a missing root, and a selected result composes with
  `replay` for integrity-checked inspection.

## Delivery Gate 6: Workspace And Project Brain Foundation

Status: Integrated

Current increment:

- Contract Verified and Integrated lost-acknowledgement recovery: process-isolated
  RunRecords use one versioned Goal/AgentRun-derived identity; exact point persistence
  is replay-safe and changed reuse fails closed. A real child JVM fixture interrupts
  after record persistence and before result publication, then proves a fresh parent
  point-resolves the bound record without scanning, duplicate execution, or a second
  RunRecord.
- Integrated: the metadata-only immutable `WorkspaceSnapshot` contract with canonical identity, typed source metadata, explicit freshness/availability, deterministic ordering, and bounded observations, connected to the real Context Reader and its downstream consumers;
- Integrated: the read-only `ProjectBrainView` composing one real snapshot, repository-memory metadata with derived freshness, and real RunRecord provenance under a matching approved task;
- Integrated: the repository-memory path from a real governed run and really-loaded repository memory through `RepositoryMemorySnapshotCollector` into the composed view, including explicit divergence detection;
- Integrated: the endpoint-checked graph projection contract and the task-to-decision-to-code-to-test impact query, populated and answered exclusively through the real producer chain naming the real stored execution;
- Integrated: accepted-decision projection from the decision log's own status lines and run-record metadata observation over the real store with explicit corruption surfacing;
- Integrated: the explicit `Justified By` task-document reference grammar projected into `JUSTIFIED_BY` edges, with the first real reference resolved on the actual repository;
- Integrated: target-file observation with real pre-run containment-checked digests, plus decision-scoped Git index/untracked/deleted metadata through a canonical absolute executable outside the observed project; unsafe tracked-worktree comparison is explicitly unavailable because adversarial tests proved Git can execute repository clean filters;
- Evidenced: the authority-boundary exit criterion is pinned by characterization — observed documents cannot grant Tool permission, widen policy, or be mutated by composition;
- Operational: the production CLI `run` path composes the view and produced graph for every recorded run, observing a 256-record recent execution window, the run target, and safe Git index state, merging decision nodes and justification edges, and reporting bounded snapshot, freshness, graph, and impact metadata; graph inputs are preflighted and post-persist reporting cannot change the durable exit code;
- assessed and promoted: the recorded gate maturity assessment mapped every scope item and exit criterion to evidence or to its later-gate blocker, and the user-approved re-scope decision moved diagnostics, terminal-session, and active/selected-file observation to Gate 12, which owns those capabilities;
- deferred to owning gates: diagnostics/selection/terminal observation integrations (Gate 12), per-file Git metadata, payload capture, modifies/verified-by producers, graph persistence, and messaging.

Dependencies:

- the first operational read-only run and RunRecord are available.

Scope:

- immutable WorkspaceSnapshot and source freshness metadata;
- one common immutable input-snapshot identity and approved task revision for every later worker handoff;
- repository documents, repository files, and the governed run's target file;
- read-only Git status and diff adapters;
- Project Brain view combining repository memory, workspace observations, decisions, and run history with provenance.
- graph projection contracts for Decision, Architecture, Dependency, Task, and Execution relationships;
- first rebuildable task-to-decision-to-code-to-test impact query.

Exit criteria:

- one snapshot can explain which files, Git state, run history, and documents informed a run (diagnostics and selection observation moved to Gate 12 by the 2026-07-15 re-scope decision);
- stale and unavailable sources are explicit;
- Workspace observations cannot override repository authority or grant Tool permission;
- snapshot size and sensitive-data boundaries are enforced.
- graph nodes and edges retain source, freshness, version, and rebuild status.

## Delivery Gate 7: Event Bus And IPC Foundation

Status: Contract Verified

Current increment:

- Contract Verified: versioned reference-only `MessageEnvelope` with canonical message/causation identities, bounded correlation/run/producer identities, and the sealed four-kind payload hierarchy carrying task revisions, snapshot identities, authorization scopes, run-record references, verification status, and control signals as data;
- Contract Verified: deterministic in-process `InProcessMessageBus` with topic fan-out and single-consumer queue delivery, typed `DeliveryOutcome`/`DeliveryStatus` results, per-subscription idempotency, and an ordered journal that supports deterministic replay without duplicate side effects;
- Contract Verified: delivery-failure isolation and dead-letter capture — a throwing handler yields a `FAILED` outcome and an ordered immutable `DeadLetter` record while fan-out continues, and a failed delivery is idempotent with respect to publish and replay;
- Contract Verified: bounded synchronous retry and explicit dead-letter re-delivery — an immutable `RetryPolicy` (1-10 attempts) retries a failing handler immediately before dead-lettering it with its failed attempt count, and `redeliver` resolves a recorded dead letter on success or replaces it in place with the accumulated attempt count on renewed exhaustion, never touching the journal or the consumed idempotency key;
- Contract Verified: cancellation propagation — `cancel(correlationId)` is idempotent and monotonic, and a cancelled correlation is refused admission on every path (publish, replay, and re-delivery) with a scope-level `CANCELLED` outcome, no handler invocation, no idempotency key consumed, no dead letter, and nothing journaled, so the bus never interprets a payload to decide delivery;
- Contract Verified: delivery ordering — each publication runs to completion before any publication it causes is delivered, so a re-entrant publish is queued and reports `ENQUEUED` while the draining call returns the whole ordered cascade, every publication reaches drain-owned admission, replay-caused cascades inherit non-journaling mode, the journal's order remains the bus's delivery order, and a correlation cancelled mid-cascade refuses entries still queued behind it;
- Contract Verified: deterministic pending-queue backpressure — immutable `BackpressurePolicy` bounds waiting publications from 1 through 4096, capacity exhaustion reports scope-level `BACKPRESSURED` without blocking or consuming journal/idempotency/dead-letter/cancellation state, admitted work remains FIFO and retryable refusal stays explicit, and replay deterministically delivers the prefix that fits while reporting the refused suffix without journaling;
- Contract Verified: transport-neutral IPC boundary — immutable `TransportMessage` carries one existing destination and envelope to provider-neutral `MessageTransport`, while typed `TransportOutcome` reports only hop acceptance, backpressure, or unavailability and cannot masquerade as subscriber delivery;
- Contract Verified: bounded work payload authorization scope with 1 through 256 unique allowed-tool names, each at most 256 characters, giving the collection a finite aggregate ceiling while preserving immutable copying;
- promoted after fresh reassessment: all six scope items and all four exit criteria have Contract Verified evidence across 39 focused bus tests, the complete regression, and strict production lint;
- the provider-neutral transport seam completes the Contract Verified foundation; a concrete adapter remains later Integrated or Operational work rather than a promotion prerequisite;
- Integrated sub-path: one named real path derives a work envelope from a matching repository-approved task and Gate 6 Workspace snapshot, delivers it through the in-process queue, and admits the unchanged envelope as one Gate 8 `WorkItem` with duplicate-free replay;
- Contract Verified and Integrated retained-spool acknowledgement: the supported point
  receiver resolves exactly one pending `.transport` or deterministic `.received`
  artifact, validates before queue recovery, atomically acknowledges only after durable
  admission, supports revision-free acknowledged re-entry, and releases pending
  transport capacity while retaining evidence;
- Integrated maturity assessment completed: the governed Work publisher, file-spool
  transport, point receiver, in-process queue, and durable admission now form a named
  real upstream-to-downstream production connection, but Gate 7 remains Contract
  Verified because Handoff Message Bus flow, topic and
  failure/retry/dead-letter/cancellation/cascade-ordering branches lack named complete
  production connections;
- deferred: durable bus persistence, broader local-process or remote IPC adapters,
  directory consumption, cleanup/retention policy, threading, and broader production
  wiring.
- Reassessed after isolated child Work ingress: no production owner now constructs
  topic or Handoff flow, invokes cancellation or dead-letter re-delivery, or publishes
  re-entrantly. Durable journal, directory consumption, and retention still require
  accepted checkpoint/compaction, claim/restart, and destructive-cleanup policies.
  These branches remain deferred rather than receiving synthetic production callers;
  Gate 7 remains Contract Verified.

Implemented bounded connection:

- expose one governed `scheduler-spool-work` publication command over
  `FileSpoolMessageTransport`, preserving task/snapshot authorization and provenance and
  reporting only hop-level `ACCEPTED`, `BACKPRESSURED`, or `UNAVAILABLE`;
- connect its accepted artifact through the separately invoked existing
  `scheduler-receive-work` path in a named real-filesystem integration;
- add no directory scan, queue creation, retry, combined receipt/execution, result or
  handoff flow, durable bus journal, retention policy, or background lifecycle.

Implemented bounded Result connection:

- route the existing process-isolated child Result spool point through a fresh
  `InProcessMessageBus` queue to an extracted exact Result/RunRecord validation handler;
- retain the current correlated non-empty-causation envelope, RunRecord binding,
  restart re-entry, and Worker finalization sequence;
- add no second result protocol, durable journal, retry/dead-letter recovery,
  acknowledgement/retention, CLI, schema, dependency, or authority.

Implemented bounded isolated Work connection:

- route the existing decoded child Work transport message through one fresh real
  Message Bus queue before invoking the unchanged Gate 1-4 execution boundary;
- require exactly one `DELIVERED` outcome before exposing the handler's persisted
  RunRecord reference/status to Result publication;
- make a foreign Work route `UNROUTED` before execution, RunRecord persistence, or a
  Result point;
- add no retry/dead-letter policy, cancellation, topic, second Work protocol, durable
  journal, directory discovery, cleanup/retention, schema, dependency, or authority.

Implemented bounded Control connection:

- receive one explicitly named local Control spool point through a fresh real Message
  Bus queue into the existing durable Goal control-request ledger;
- validate the exact route and `ControlPayload` before runtime mutation, report only
  after durable admission, and atomically acknowledge the retained point afterward;
- keep the request untrusted and unapplied, leaving authenticated cancel/pause/resume
  behavior to Gate 12;
- add no worker interruption, queue mutation, durable bus journal, directory scan,
  cleanup/retention, Handoff, topic catalog, or multi-agent behavior.

Implemented bounded Control producer connection:

- expose one separate `scheduler-spool-control` point command that directly reads an
  existing `ACTIVE` Goal with a current non-terminal AgentRun and derives correlation,
  logical-run, and causation only from its retained Work envelope;
- publish the caller-supplied message identity, producer, occurrence time, Control
  signal, and reason through `FileSpoolMessageTransport`, reporting transport
  acceptance separately from receiver admission;
- connect an accepted point to the existing `scheduler-receive-control` command in one
  named real-filesystem integration;
- create untrusted intent only, with no authentication/application, runtime recovery,
  lease reclamation, worker interruption, queue mutation, scan, retry timing, durable
  journal, cleanup, or retention.

Dependencies:

- Workspace snapshots and durable RunRecords exist.

Scope:

- typed domain events and versioned message envelopes;
- event, message, correlation, causation, run, and producer identities;
- typed work, result, control, and handoff payloads that preserve authorization and snapshot references;
- in-process topic and queue delivery;
- idempotency, retry, cancellation, dead-letter, replay, ordering, and backpressure contracts;
- IPC transport interface for later local-process or remote adapters.

Exit criteria:

- a deterministic in-process pipeline delivers and replays a versioned event without duplicate side effects;
- payloads are bounded or replaced by evidence references;
- authorization and provenance survive every hop;
- Event Bus semantics do not depend on the eventual IPC transport.

## Delivery Gate 8: Agent Runtime And Scheduler

Status: Specified - Next

Whole-gate assessment:

- retained at `Specified - Next` after closing the pre-migration assessment's supported
  migration gap, public priority admission, non-recovery priority/fairness selection,
  priority/fairness observability, deterministic child-RunRecord recovery, lease-expiry
  recovery, disposition-acknowledgement recovery, the bounded foreground service
  connection, one supported durable spool-to-bus-to-admission point receiver, and
  post-admission retained-point acknowledgement with exact `.received` re-entry. The
  isolated child Work and parent Result paths now both cross real Message Bus queues, so
  the earlier worker-communication blocker is closed. The bounded single-agent
  Scheduler/runtime foundation is Integrated and retains Operational explicit workflows,
  and the Tool-timeout, stagnation,
  cancellation-request, verification, and terminal WorkItem transition owners now reach a
  persist-after-source recorder and injected publisher port. A concrete filesystem
  reference-point adapter implements that port, and the optional Control receiver is
  its first supported construction for cancellation-request events. The same shared
  Scheduler construction supports process timeout, lease timeout, retry decision/start,
  verification, Tool timeout, stagnation, and terminal WorkItem owners. Authenticated
  cancellation now has a separate supported authorizer-injected filesystem application
  surface with optional concrete `CANCELLATION_APPLIED` publication; authenticated
  interface adapters and cancelled queue disposition remain.
  Whole-gate promotion remains
  blocked by broader publication/consumption and later-gate budgets,
  Memory, authenticated control interfaces, production adapters, and role workers;
- existing queue-active, checkpoint, deterministic lost-acknowledgement point, and
  expired-lease recovery satisfy the accepted at-least-once correctness prefixes. A
  general orphan inventory or cleanup feature is not silently required and would need a
  separate scan-bound, retention, authority, and cross-store consistency decision;
- the first supported migration connection now satisfies its bounded exit-criterion
  slice: exact lossless conversion, explicit maintenance, failure preservation,
  idempotence, and a real migration-to-cycle recovery fixture are named and Integrated.
  The recovered post-RunRecord-reference prefix reaches one verified disposition
  without an invocation spool, additional RunRecord, or changed effect ledger;
- the orphaned-RunRecord lost-acknowledgement prefix after child persistence and before
  result-spool publication now recovers through an AgentRun-derived deterministic
  RunRecord identity, fail-closed point resolution, and binding validation before
  re-execution. A second sidecar and store scanning remain rejected. Authenticated
  controls, production adapters, and multi-agent roles remain owned by Gates 12, 11,
  and 13 respectively.
- the lease-expiry lost-acknowledgement slice now has named worker-level evidence
  without a new runtime mechanism: a fixture checkpoints the RunRecord reference,
  advances beyond lease expiry, observes reclaim to `READY`, and proves fresh-worker
  greater-fence convergence without another execution, RunRecord, or effect outcome.
  Terminal disposition replay and checkpoint clearing remain idempotent; orphan
  Evidence before RunRecord persistence is retention work rather than an unrecorded
  Gate 8 effect. The composition between terminal disposition and checkpoint clearing
  now also has named worker-level evidence: after the verified disposition persists,
  a forced clear failure retains the exact intent and a fresh worker removes it without
  another execution, RunRecord, effect outcome, runtime transition, or queue revision.
  Unresolved `PREPARED` external effects remain fail-closed and await their owning
  adapter/recovery policy. Further whole-gate work returns to the broader gaps already
  assigned to production adapters, authenticated interface adapters and remaining
  controls, service operation, and
  role-based workers rather than inventing another acknowledgement fixture.
- the first migration boundary is Contract Verified and Integrated: an explicit
  stopped-Scheduler maintenance command losslessly converts only the schema-v1
  pending-finalization checkpoint to schema v2 through validated candidate-first atomic
  replacement, while normal recovery remains fail-closed and other store migrations
  await separate information-recovery policies.
- the first priority/fairness selector is Contract Verified and its persistence
  prerequisite now exists: the
  pure admission-order-preserving selector distinguishes `NORMAL` and `EXPEDITED`, caps
  consecutive expedited selection, and forces the oldest ready normal candidate after
  the configured burst. `WorkItem` remains unchanged because priority is Scheduler
  metadata, not Tool authority. The non-recovery
  `SingleWorkerSchedulerQueue.claimNext` path now supplies complete ready candidates,
  activates the selector result, and persists its next fairness progress with the claim.
- the queue schema-v3 prerequisite is Contract Verified and its explicit migration
  surface is Integrated: schema v3 retains every identity, revision, capacity,
  logical-run binding, exact admission and
  pending order, active item, and terminal partition; assign `NORMAL` to every admitted
  item, maximum expedited burst `4`, consecutive expedited progress `0`, and no
  migration-time recovery reservation. Ordinary v3 recovery persists interrupted active
  work as a one-shot preferred claim and reclaims it before FIFO without counting the
  same durable claim twice. `scheduler-migrate-queue` exposes candidate-first
  stopped-Scheduler v2-to-v3 conversion with source-drift refusal and failure
  preservation; ordinary resolution remains fail-closed on v2.
- the priority-admission persistence prerequisite is Contract Verified and its explicit
  migration surface is Integrated: immutable manifest schema v2 retains exact
  `NORMAL`/`EXPEDITED` intent and durable submission passes it to exact queue admission.
  `scheduler-migrate-submission-manifest` maps one stopped schema-v1 submission to
  `NORMAL` through candidate-first atomic replacement and source-drift refusal.
  Both the explicit `scheduler-submit` and generated-input `scheduler-submit-generated`
  commands now accept one optional public `--priority NORMAL|EXPEDITED` input and report
  the effective priority, while generic durable message admission still defaults to
  `NORMAL`.

Current increment:

- Contract Verified and Integrated isolated-worker Work ingress: the decoded unchanged
  transport message publishes to its carried destination through one fresh real Message
  Bus with exactly one `queue("work")` handler. The handler constructs the exact
  parent-identified WorkItem, invokes the unchanged Gate 1-4 execution boundary, resolves
  the persisted RunRecord status, and exposes reference/status only after success. A
  foreign route is `UNROUTED` before execution, RunRecord persistence, or Result
  publication, while the named real child-process path continues through the existing
  Message-Bus-validated Result return without new protocol, state, or authority;
- Contract Verified and Integrated point-receive path: `scheduler-receive-work` resolves
  exactly one explicit retained regular non-symbolic pending or acknowledged transport
  artifact, validates its exact queue route and Work payload, publishes the unchanged
  envelope through the real Message Bus to durable admission, and reports `ADMITTED` or
  revision-free `REPLAYED` separately from `ACKNOWLEDGED` or
  `ALREADY_ACKNOWLEDGED`. Pending input moves atomically to deterministic `.received`
  only after success, acknowledged re-entry performs no second move, and the released
  pending slot accepts a later send. A separately invoked process-isolated service
  reaches verified completion; exact re-receipt adds no queue revision, AgentRun, or
  RunRecord. No scan, cleanup, retention policy, queue creation, execution wrapper, or
  durable bus journal is added;
- Contract Verified: immutable `WorkItem` admission over one unchanged Gate 7 work envelope, with a distinct canonical identity and bounded required capability but no scheduling or execution behavior;
- Contract Verified: immutable priority-bearing `QueuedWork` with up to 256 unique dependency identities plus a deterministic run-scoped `SingleWorkerSchedulerQueue` bounded to 4096 admissions, dependency-first validation, admission-ordered priority selection with bounded expedited fairness, one active slot, one-shot exact recovery preference, matching completion, and no authority expansion;
- Contract Verified: canonical queue identity and single-logical-run binding, immutable schema-v3 queue snapshots retaining every exact priority-bearing ordered admission including terminal work plus bounded burst/progress/recovery-preference state, bounded integrity-checked atomic filesystem persistence, persist-before-exposure admission/claim/disposition/recovery, revision-free exact admission replay, changed-content identity-reuse refusal, explicit ordinary v1/v2 rejection, and exact preferred active-work replay under explicit at-least-once execution semantics;
- Contract Verified and Integrated queue migration: the queue-scoped store operation and separate `scheduler-migrate-queue` command losslessly map schema v2 to v3 through validated candidate-first atomic replacement, source-drift refusal, typed absent/current/migrated outcomes, and failure preservation without invoking recovery or execution;
- Contract Verified and Integrated submission-priority persistence: immutable manifest schema v2 stores exact `NORMAL`/`EXPEDITED` Scheduler intent, durable submission propagates it to dependency-free exact queue admission, changed-priority replay fails closed, and `scheduler-migrate-submission-manifest` explicitly maps one stopped schema-v1 manifest to `NORMAL` without queue or execution authority;
- Integrated sub-path: the real repository-derived work-message path reaches a separate persist-first durable admission handler, which derives one stable distinct WorkItem identity and admits dependency-free work through the filesystem-backed queue before success. A real process-isolated Scheduler cycle reaches terminal disposition, then fresh queue and bus instances accept the exact envelope without a second queue revision, WorkItem, RunRecord, or dead letter; changed-content identity reuse fails closed;
- Contract Verified and Integrated submission path: one immutable manifest persists the target queue identity/capacity, required capability, and exact work envelope before queue creation. The application boundary creates only an absent queue or verifies an existing capacity before recovery, then uses exact durable admission; real-filesystem interruption after either completed prefix resumes to one WorkItem, while exact replay is revision-free and changed content fails closed. The separate explicit `scheduler-submit` CLI reaches this boundary without combining execution;
- Contract Verified: one exact-WorkItem schema-v4 `RuntimeGoal` with an immutable history of at most 16 distinct `RuntimeAgentRun` attempts, attempt-bound retry decisions, at most 256 exact lease-timeout records, and at most one authorization-bound cancellation application, deterministic forward-only per-attempt transitions, matching typed result envelopes, Verified-only Goal completion, durable `RETRY_PENDING` for failed attempts, terminal authenticated cancellation, Goal-wide fences, monotonic persist-before-exposure revisions, bounded integrity-checked filesystem state, and exact-prefix restart recovery;
- Contract Verified: a bounded fenced single-owner `AgentRunLease` acquired only from `READY`, with injected time, persisted monotonic fence tokens, matching unexpired owner/fence checks for renewal and execution completion, and durable expiry reclamation back to `READY` across restart;
- Integrated sub-path: one durable queue active/ready WorkItem is connected to the exact durable Goal, named AgentRun planning/readiness prefix, and current fenced lease through idempotent persisted-prefix recovery across both filesystem stores;
- Contract Verified: a first concrete `MessageTransport` (connection 3c) writing one encoded route and envelope to a local file spool a peer reads, with the frame owned by a separate deterministic codec carrying all four payload kinds and failing closed on corruption, and no ordering promised across separately spooled messages;
- Contract Verified: an isolated worker process lifecycle (connection 3b) running one child bounded by a capped timeout, forcible destruction, discarded output, and a sanitized environment, scoped by accepted decision to re-running the current JVM only, with a child entry point that reads one spooled message so the boundary is proven by a real message crossing it;
- corrected boundary: fence-checked execution completion persists only `AWAITING_VERIFICATION`; it does not complete the queue, satisfy dependencies, or imply Verified/Completed;
- Contract Verified: process-isolated execution (connection 3d) connecting the adapter and process lifecycle into a second production `AgentRunExecution`, with exact pre-existing work identity and route checks, distinct zero/one/several result handling, the child running the same Gate 1-4 pipeline through an internal shared seam, and the returned claim checked for route, correlation, payload, task, reference resolution, RunRecord source/target/digest binding, and status agreement before a reference is returned;
- Integrated sub-path: `DurableAgentRunWorker.processIsolated` selects 3d with the real self-JVM launcher, one shared queue for dispatch/finalization, and the caller-supplied durable stores; a real filesystem integration crosses both spools and the child process through verified queue disposition;
- retention boundary: the RunRecord reference persists in the cycle-intent checkpoint before the exact Goal/AgentRun spool is retired; cleanup failure retries from that checkpoint without re-execution, while other failed or incomplete cycles retain explicit at-least-once semantics;
- Contract Verified and Integrated request path: one real Gate 7 control queue delivers into `RuntimeControlAdmissionHandler`, which binds an exact control envelope to active Goal work and persists it in a bounded, ordered, restart-idempotent Gate 8 ledger before handler success; storage failure uses the bus retry/dead-letter path, while no request applies an unauthenticated transition or changes runtime authority;
- Contract Verified: one bounded schema-v2 external-effect ledger per Goal persists stable idempotency-keyed semantic intent with adapter identity as evidence-free `PREPARED` and exactly one current-owner/fence-checked, evidence-bound terminal `APPLIED`, `DEDUPLICATED`, `COMPENSATED`, or `REQUIRES_USER_RECOVERY` outcome. Exact replay is revision-free, key/status/evidence rebinding fails closed, schema-v1 artifacts fail explicitly, filesystem history is monotonic and integrity checked, and unresolved preparation survives restart without automatic replay;
- Contract Verified and Integrated application boundary: `DurableExternalEffectExecutor` composes the fence-checked ledger, one adapter port, and the Evidence Store. It validates stable adapter identity and semantic digest before mutation, persists `PREPARED` before one invocation, persists redacted complete evidence before terminal publication, resolves exact terminal replay without another invocation or revision, and refuses automatic execution from an already prepared record. A named real-filesystem integration connects the runtime lease, ledger, deterministic adapter, and Evidence Store across success, restart replay, and failure prefixes; production external adapters remain Gate 11 work;
- Contract Verified: the corrected pure `AgentRunRetryDecider` consumes the exact latest `RuntimeAgentRun`, binds the supplied Goal ledger/effect WorkItem, and refuses non-failed attempts, unresolved, recovery-required, applied/deduplicated, and exhausted cases in fixed safety-first precedence; only empty/all-`COMPENSATED` effect history with remaining 1–16 attempt budget admits, while no AgentRun, store, queue, or authority is created or mutated (connection #6 decision contract only);
- Contract Verified continuation for connection #6: schema-v2 immutable attempt and retry-decision prefixes preserve exact history, failed results keep the WorkItem active through durable `RETRY_PENDING`, result recording is split from terminal queue disposition, and the current worker parks with its intent/reference retained instead of failing the queue or running a replacement;
- Contract Verified continuation for connection #6: `DurableAgentRunRetryController` resolves the existing retry-pending runtime and exact Goal ledger, persists the typed decision with versioned semantic ledger evidence before action, then idempotently appends only a caller-checkpointed admitted AgentRun identity or abandons a refused Goal without queue or execution authority;
- Integrated continuation for connection #6: the retry-aware Worker creates or recovers the exact Goal ledger before execution, checkpoints a replacement identity before append, executes admitted attempts through the existing fenced path, and recovers every durable retry prefix to one final verified or failed queue disposition on real filesystem stores;
- Integrated recovery-order continuation for connection #6: when a checkpoint names an
  already `COMPLETED` or `FAILED` latest AgentRun, the Worker exact-replays the retained
  Result through the finalizer before retry control or terminal queue disposition.
  Reference drift fails before those later side effects, preserving the existing
  verification, Tool-timeout, and stagnation repair seam; the shared supported
  Scheduler composition now supplies its recorder to that finalizer;
- Integrated durable submission path: an immutable manifest persists before queue creation, exact replay changes neither manifest nor queue revision, changed-content identity reuse and queue-capacity drift fail closed, and `scheduler-submit` connects the governed active task plus repository-memory snapshot to this path using only explicit roots, identities, time, capacity, capability, target, and digest inputs without executing work;
- Integrated supported entry point: `scheduler-cycle` recovers one explicitly identified existing durable queue, composes the real process-isolated Worker and filesystem stores, and runs exactly one recoverable cycle with bounded idle/verified/failed status and stable exits; it creates no queue, admits no work, and makes no Operational promotion;
- Operational sub-path: the documented operator workflow invokes `scheduler-submit` and `scheduler-cycle` separately over one shared explicit queue root/identity. A named real-filesystem CLI integration plus an actual Enhancer-repository smoke run prove admitted work remains pending before the cycle, the real child JVM reaches verified terminal disposition, exact submission replay does not revise the terminal queue or duplicate a RunRecord, and a later explicit cycle reports idle; recovery preserves each command's distinct roots, output, and failure ownership without a wrapper or polling;
- Contract Verified and Integrated generated-input submission: `GeneratedInputSubmissionService` takes one caller-retained canonical submission UUID, derives the queue, correlation, and logical-run identities through fixed versioned domain-separated transforms, and reuses the existing immutable submission manifest as the sole generated occurrence-time/envelope record. It resolves that manifest before consulting the clock or recapturing repository context, reuses the exact stored time and envelope on replay, and fails closed on caller-owned intent conflict; the separate `scheduler-submit-generated` CLI connects governed inputs to this boundary with a named real-filesystem integration, while submission stays separate from execution and polling and the explicit `scheduler-submit` command is unchanged;
- Operational sub-path: the generated-input operator workflow invokes `scheduler-submit-generated` and then a separate `scheduler-cycle` over the derived queue identity. A named real-filesystem CLI integration plus an actual Enhancer-repository smoke run reading `README.md` prove `ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE` with identical replay occurrence time and Workspace snapshot, one retained manifest, one RunRecord, and no duplicate execution; recovery is documented in the README alongside the explicit workflow, without a wrapper or polling;
- Contract Verified and Integrated foreground-drain prerequisite: each local filesystem queue update uses one stable non-blocking queue-scoped operating-system lock across resolve, revision/history validation, and atomic publication. Contention fails typed without waiting, lock artifacts carry no queue state or authority, and real child-JVM plus stale-writer tests prove that a committed transition cannot be overwritten;
- Contract Verified and Integrated bounded foreground connection: `ForegroundSchedulerDrain` and the separate `scheduler-drain` command reuse the existing process-isolated one-cycle recovery boundary over one existing queue, continue only after verified completion, and stop on the first idle result, failed disposition, or explicit at-most-4096 cycle limit. Focused contract tests pin exact stop/count semantics, while real-filesystem child-process integrations cover multiple ready and dependency-linked items, an interrupted per-cycle checkpoint, the configured limit, terminal failure, and a missing queue. The connection does not create or admit work, merge submission with execution, sleep, wait, poll for future work, daemonize, apply controls, or add another progress store;
- Contract Verified bounded service lifecycle: caller-driven `BoundedSchedulerService` reuses the durable one-cycle worker under finite total-cycle, consecutive-idle, and idle-wait limits; checks a local stop signal before every sequential cycle; waits only between bounded idle results; resets idle progress after verified work; stops on the first failure; restores interruption; and returns exact typed stop counts. It creates no thread, supported entry point, durable service progress, authenticated control, queue/admission, external adapter, or additional recovery authority;
- Integrated bounded foreground service connection: the separate `scheduler-service` command reuses every explicit process-isolated one-cycle recovery input and adds finite total-cycle, consecutive-idle, and idle-wait policy inputs. It runs on the invoking thread, uses that thread's interrupt state as its local stop signal, and reports bounded typed counts plus queue and RunRecord status. Named real-filesystem integrations resume a persisted cycle intent and reclaim an expired executing lease under the same Goal/AgentRun with a greater fence, one AgentRun, one RunRecord, and one verified disposition. No thread, daemon, supervisor, service checkpoint, authenticated control, queue/admission, or general orphan scanner is added;
- Contract Verified and Integrated read-only queue inspection: pure `SchedulerQueueStatus`
  preserves admission order and classifies each persisted admission as ready, blocked,
  active, verified, or failed while retaining its exact priority and the queue's persisted
  maximum expedited burst, consecutive expedited progress, and optional recovery
  preference; the separate `scheduler-status` command resolves the snapshot without
  recovery and reports complete counts plus an at-most-48 identity/state/priority prefix.
  Real-filesystem integration proves mixed priority, all five states, and that inspection
  changes no artifact bytes, timestamp, revision, active slot, fairness progress, or
  recovery preference, while missing and corrupt queues retain configuration/internal
  failure separation;
- Contract Verified and Integrated read-only recovery inspection:
  `SchedulerRecoveryStatus` and `scheduler-recovery-status` use the single cycle
  checkpoint as the only Goal/AgentRun/RunRecord join anchor, classify nine durable
  prefixes, validate exact cross-store bindings, and refuse queue/checkpoint/runtime
  drift after a bounded second sample. Real-filesystem integration proves representative
  prefixes, non-creation, immutable artifacts, corruption failure, bounded output, and
  explicit unknown worker liveness without recovery, scanning, or mutation;
- Contract Verified and Integrated read-only external-effect recovery inspection:
  `SchedulerExternalEffectRecoveryStatus` and `scheduler-external-effect-status` reuse
  the checkpoint-correlated Scheduler Goal, classify conservative retry-safety phases,
  validate exact WorkItem and AgentRun-history bindings, integrity-check every terminal
  evidence reference, and reject bounded-sample Scheduler/runtime/ledger drift.
  Real-filesystem integration proves representative prefixes, non-creation, immutable
  artifacts, corrupt-evidence refusal, bounded output, and no adapter invocation,
  external-system probing, or mutation;
- Contract Verified and Integrated read-only invocation-spool recovery inspection:
  `SchedulerInvocationRecoveryStatus` and `scheduler-invocation-status` reuse the
  checkpoint-correlated Scheduler Goal/AgentRun, inspect only that private invocation
  namespace, validate exact work/result transport and RunRecord bindings, and reject a
  changed bounded second sample. The command never consumes, launches, cleans, recovers,
  retries, scans, mutates, or claims worker liveness;
- Contract Verified and Integrated first state-version migration boundary:
  `FileSystemPendingFinalizationStore.migrateSchemaV1ToCurrent` and
  `scheduler-migrate-cycle-checkpoint` preserve every schema-v1 pending-finalization
  value, map the schema-v2 replacement AgentRun identity to absent, return typed
  absent/already-current/migrated outcomes, and publish only a reread validated candidate
  after source-byte equality. Real-filesystem store and CLI tests prove ordinary v1
  rejection, exact conversion, non-writing idempotence, candidate cleanup, source-drift
  refusal, corrupt-input preservation, and normal recovery;
- Integrated migration-to-cycle recovery: a named real-filesystem fixture migrates a
  schema-v1 post-RunRecord-reference checkpoint through the supported command and then
  invokes the real process-isolated `scheduler-cycle` composition. It reaches one
  verified queue disposition from the retained reference, creates no invocation spool
  or additional RunRecord/effect outcome, preserves the effect artifact bytes, and
  clears the checkpoint. This satisfies the supported-migration fixture slice without a
  second schema migration or whole-gate promotion;
- Contract-verified Gate 8 runtime-event value/store and recording boundary: the finite
  `runtime-event-v1` taxonomy, sealed detail, deterministic Goal/AgentRun/reference
  identity, bounded per-Goal stream, and integrity-checked atomic filesystem adapter
  exact-replay without making events transition authority. `RuntimeEventRecorder`
  appends or exact-replays before passing only an opaque deterministic reference to its
  publisher port;
- Integrated first transition-owner connection: event-aware Control admission records a
  `CANCELLATION_REQUEST_RECORDED` event only after the exact `CANCEL` request is durable.
  Exact Control replay repairs a missing event or republishes the same reference without
  advancing the runtime or event stream, while source failure and `PAUSE`/`RESUME` reach
  no event publisher. The optional all-or-none `scheduler-receive-control` publication
  group is the first supported concrete composition for this owner: explicit event and
  publication roots plus capacity preserve request-event-point-acknowledgement order
  and exact retained-prefix replay. The existing four-kind MessageEnvelope remains
  unchanged, and omitted configuration preserves request-only behavior;
- Integrated second transition-owner connection: event-aware
  `DurableAgentRunFinalizer` records `VERIFICATION_RECORDED` only after its
  RunRecord-backed Result transition is durable. The retained Result supplies occurrence
  time, status, and causation, while ordered Result-message and RunRecord references keep
  exact repair stable across later runtime revisions. Result persistence failure reaches
  no event or publisher; event/publication failure re-enters from the durable Result.
  Terminal queue disposition remains a separately ordered fact connected below;
- Integrated third transition-owner connection: event-aware finalization records
  `WORK_ITEM_TERMINATED` only after the queue durably retains the exact WorkItem in its
  matching completed or failed partition. A stable queue/WorkItem/disposition reference
  survives later whole-queue revisions. Because the queue retains no transition time,
  recorder re-entry restores the first persisted occurrence for the same event ID while
  exact-validating every other field. Queue-store failure reaches no event or publisher,
  and publication failure re-enters from the terminal queue fact without another event
  revision;
- Integrated fourth transition-owner connection: event-aware retry control records
  `RETRY_DECISION_RECORDED` only after the exact admitted or refused attempt-bound
  decision is durable. Stable decision identity and decision-bearing runtime revision
  references support exact event/publication repair, while runtime persistence failure
  reaches no event or publisher and replacement append remains a separate fact;
- Integrated fifth transition-owner connection: event-aware `beginAdmittedRetry`
  records `RETRY_STARTED` only after the caller-checkpointed replacement AgentRun is
  durable. Stable prior-decision and replacement-AgentRun references survive later
  replacement status revisions; first-occurrence recovery preserves publication replay,
  and replacement persistence failure reaches no event or publisher. Refused
  abandonment and supported Worker/CLI event composition remain separate;
- Integrated sixth transition-owner connection: event-aware
  `DurableAgentRunFinalizer.recordAgentRunResult` resolves the bound RunRecord and
  persists or exact-replays the matching Result transition before recording
  `STAGNATION_DETECTED` for `STAGNATED` only. The RunRecord occurrence and iterations,
  current default threshold three, causal Result, and stable Result-message/RunRecord
  references preserve exact replay after later runtime revisions. Verification remains
  a distinct earlier event; non-stagnated records add no stagnation observation, and no
  timeout owner or source schema is added;
- Integrated seventh transition-owner connection: event-aware
  `DurableAgentRunFinalizer.recordAgentRunResult` records `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.TOOL` only after a bound RunRecord carrying exact
  `ToolFailureCode.TIMED_OUT` reaches its durable Result transition and separate
  verification fact. RunRecord occurrence, Result causation, exact Work binding, and
  stable Result-message/RunRecord references preserve exact replay; timeout precedes a
  separate stagnation fact when both apply. Non-timeout input and Result persistence
  failure reach no timeout event;
- Integrated eighth transition-owner connection:
  `ProcessIsolatedAgentRunExecution` persists one bound deterministic
  `ProcessTimeoutFact` after a typed watchdog timeout and before exposing failure, then
  its event-aware construction records `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.PROCESS` from that fact's occurrence, Work causation, stable
  reference, and digest. Exact re-entry skips another child and repairs a missing event
  or failed publication; start failure, completed failure, and success add no timeout
  fact or event, and AgentRun lifecycle/retry policy stays unchanged;
- Integrated ninth transition-owner connection: AgentRuntime schema v4 atomically
  appends one bounded exact `LeaseTimeoutRecord` with expired `EXECUTING -> READY`
  reclaim. Event-aware `DurableAgentRuntime` records `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.LEASE` from retained expiry, Work causation, and the stable
  Goal/AgentRun/fence reference; retained-ledger replay repairs missing events and
  publication failure without another runtime revision;
- Integrated tenth transition-owner connection: `AuthenticatedCancellationApplication`
  accepts only a matching approval from a trusted authorizer over an exact retained
  `CANCEL`, atomically persists the authorization-bound schema-v4 terminal runtime fact,
  and then records `CANCELLATION_APPLIED` with retained application time, Control
  causation, and stable message/application references. Denial and drift do not mutate;
  retained-record replay repairs event/publication without reauthorization or another
  runtime revision;
- Implemented first concrete runtime-event publisher adapter:
  `FileSystemRuntimeEventPublisher` accepts only the opaque durable reference, writes a
  deterministic capacity-bounded schema-v1 integrity point through atomic publication,
  exact-replays before capacity evaluation, and fails closed on corrupt or foreign
  point reuse without changing MessageEnvelope;
- Integrated first supported publisher composition: `scheduler-receive-control`
  optionally constructs the filesystem event store, publisher, and recorder from one
  all-or-none caller-owned group, publishing only `CANCELLATION_REQUEST_RECORDED` before
  spool acknowledgement while retaining exact repair after capacity or publication
  failure;
- Integrated process- and lease-timeout Scheduler publication: `scheduler-cycle`,
  `scheduler-drain`, and `scheduler-service` accept the same optional all-or-none event
  store root, publication root, and capacity group. Their shared worker composition
  injects the resulting recorder into `ProcessIsolatedAgentRunExecution`, AgentRuntime
  recovery, retry control, and finalization. Watchdog and lease
  timeout owners each preserve source fact -> exact event -> opaque point ordering;
  retained re-entry repairs publication without another child, lease reclaim, source
  revision, RunRecord, retry decision, or duplicate owner source transition;
- Integrated retry Scheduler publication: the same optional Scheduler event group now
  injects its recorder and Worker clock into `DurableAgentRunRetryController`.
  Attempt-bound decision persistence precedes `RETRY_DECISION_RECORDED`, checkpointed
  replacement append precedes `RETRY_STARTED`, and the existing retry checkpoint
  branches exact-replay either fact after append, publication, or capacity failure.
  Named real-filesystem CLI evidence across cycle, drain, and service observes the
  admitted decision, replacement start, and final refused decision with two RunRecords
  and one terminal failed queue disposition. The finalizer composition now interleaves
  verification before each decision and termination after final refusal; omission
  remains event-free;
- Integrated Result-side Scheduler publication: the same optional event group supplies
  the recorder and injected clock to `DurableAgentRunFinalizer`. A capacity-one
  real-filesystem CLI fixture across cycle, drain, and service proves durable Result ->
  verification -> durable queue disposition -> termination ordering and exact
  acknowledgement/re-entry without another child, RunRecord, source/event revision, or
  disposition. Existing focused owner tests retain Tool-timeout and stagnation ordering
  and repair evidence;
- Contract Verified and Integrated first read-only consumer: one explicit
  `.runtime-event-reference` point is integrity-checked, bound to its deterministic
  filename, parsed into canonical Goal/event identities, and resolved through exactly
  one bounded Goal stream. The separate supported `runtime-event-read` command reports
  bounded typed metadata and proves repeatable reads and failures mutate neither point
  nor event artifact, create no missing root, and claim no acknowledgement or event
  application;
- Contract Verified and Integrated deterministic point acknowledgement: the separate
  `runtime-event-acknowledge` command fully revalidates one explicit pending or retained
  acknowledged point and exact event, atomically renames first success to
  `.runtime-event-received`, exact-replays lost responses, releases pending publisher
  capacity, and prevents source-owner replay from recreating pending state. It retains
  observation evidence without handler delivery, event application, deletion, scans,
  or cleanup/retention authority;
- Contract Verified authenticated-interface core: a bounded canonical short-lived
  detached exact-request grant is verified with Ed25519 against injected public-only
  trust policy and a deterministic non-secret integrity-checked authorization audit
  point persists before the existing shared cancellation application;
- Contract Verified pinned trust-loader prerequisite: one absolute normalized policy
  path and independently provisioned whole-file SHA-256 admit only one bounded no-follow
  read of the same strict canonical UTF-8 public-only Ed25519 policy bytes, with exact
  digest-derived configuration revision and no writer, discovery, fallback, or cache.
  Protected installed path/pin binding, proof production/private keys, credentials, and
  CLI composition remain unimplemented;
- deferred: real authorized external adapters, admission-history compaction/cleanup or
  schema-v1 queue migration, general orphan inventory/cleanup with an explicit retention
  and scan policy, general forward-reference graph/cycle handling, authenticated
  cancellation/pause/resume application, background/supervisor topology, time-based
  aging, broader budgets, checkpoints beyond current snapshots, schema-v1 runtime or
  effect-ledger migration, power-loss directory durability, broader multi-process and
  cross-store coordination, distributed locks and clock-skew handling, and broader
  production wiring.

Ordered connection sequence:

| Order | Connection | Owner and prerequisite |
|---|---|---|
| 1 | terminal queue disposition | Gate 8; distinguish verified completion from failure before changing dependency satisfaction |
| 2 | RunRecord-backed result finalization | Gate 7 result delivery and Gate 8 runtime; persist/resolve RunRecord, persist terminal runtime state, then persist matching queue disposition |
| 3 | process-isolated worker and local IPC | Gate 7 transport, Gate 8 worker runtime, and Gate 11 Tool controls; checkpoint the returned RunRecord reference before retiring the per-cycle spool and acknowledging execution |
| 4 | durable cancel/pause/resume | Gate 7 control delivery and Gate 8 request state now exist; Gate 12 has a shared authenticated-cancel application surface, while interface authentication, queue disposition, and pause/resume application remain |
| 5 | external-effect execution and adapter evidence | The bounded Gate 8 ledger and current-fence checks exist; the owning Tool/adapter must execute with stable effect identity and establish the applied/deduplicated/compensated/recovery outcome |
| 6 | retry through additional AgentRuns | Gate 8; separate attempt failure from terminal WorkItem disposition, preserve immutable attempt/decision history, keep the queue active across admitted retries, and bound attempts, effects, recovery, and final disposition |
| 7 | typed handoff and multi-agent execution | Gate 13; requires an Operational single-agent runtime and measured baseline |

This order records dependencies, not activation authority. Each item still requires its own accepted task, focused failure contract, fresh evidence, and named real integration path.

Dependencies:

- operational verification, RunRecord, Workspace, and messaging foundations.

Scope:

- persisted Goal and AgentRun state machine;
- Goal -> Planner -> Executor -> Memory -> Reflection -> Retry -> Done transitions;
- Scheduler queues, dependency validation, cycle rejection, fenced leases, idempotency, budgets, cancellation, pause, resume, reassignment, and recovery;
- at-least-once delivery with a stable logical-work/effect idempotency key, fence-checked state/effect commits, versioned checkpoints and state migration, explicit orphan detection/reclamation, and replay-safe or compensatable external effects;
- priority and fairness within dependency, authority, data-classification, cancellation, and cost/time budget constraints;
- Planner, Coder, Reviewer, Tester, and Memory worker roles behind message contracts;
- single-agent sequential worker first, without multi-agent concurrency.
- Dependency Analyzer and Verification Engine as Kernel services;
- resource budgets, locks, leases, and recovery checkpoints.

Exit criteria:

- a run survives interruption and resumes from durable state;
- workers communicate through the bus rather than direct Agent calls;
- retry, stagnation, timeout, cancellation, verification, and completion are explicit events;
- runtime scheduling cannot expand task or Tool authority.
- duplicate delivery, lost acknowledgement, lease expiry, restart, and supported state-version migration fixtures recover without an unrecorded duplicate effect;
- no universal exactly-once execution claim is made; each external effect is recorded as applied, deduplicated, compensated, or requiring explicit user recovery.

## Delivery Gate 9: Model Gateway And MCP Core

Status: Planned

Dependencies:

- the event-driven single-agent runtime is operational.

Scope:

- provider-neutral ModelRequest, response, usage, and routing contracts;
- provider-neutral execution profiles for capability, model class, locality, reasoning, context, token, cost, time, and data-classification requirements;
- Model Router with deterministic fake plus explicitly selected provider adapters;
- timeout, cancellation, token, context, cost, redaction, and response-validation budgets;
- versioned per-model quality evaluation, policy-scoped fallback and response caching, prompt-injection resistance, source attribution, and visible uncertainty;
- secret/sensitive-data detection plus explicit outbound destination, purpose, retention, and data-classification policy before remote transmission;
- MCP Server exposing governed Tools, resources, Workspace views, and memory;
- MCP Client consuming external servers through existing policy, evidence, verification, and RunRecord boundaries.
- privacy-aware routing across approved local and remote providers using data classification, capability, cost, latency, context, and availability.

Exit criteria:

- Claude Code, Cursor, VS Code, or another MCP client can inspect an approved Enhancer resource without bypassing policy;
- an external MCP Tool follows the same evidence and verification path as a native Tool;
- provider or protocol failure produces explicit runtime events and stop reasons;
- model output and MCP content cannot grant authority.
- sensitive-code fixtures remain local and remote adapters receive only policy-approved data.
- fallback and cache use preserve provenance and cannot silently cross locality, freshness, data-classification, or authorization boundaries;
- evaluation evidence can compare model quality, task success, cost, latency, and failure behavior on a fixed fixture revision.

## Delivery Gate 10: Skill Engine And Memory Runtime

Status: Planned

Dependencies:

- operational Agent Runtime, Model Gateway, MCP, and bounded context.

Scope:

- progressive Skill discovery, metadata-first loading, validation, and least-privilege enforcement;
- validated orchestration-pattern and workflow metadata without runtime authority;
- explicit Skill composition such as Spring -> Java -> Database -> Test;
- composition permission intersection and conflict handling;
- repository memory reads, explicit writes, and governed distillation;
- Skill and memory provenance in RunRecord and Project Brain.
- Workflow Engine composing events, Agents, Skills, Tools, verification, rollback, and approval gates;
- reusable workflows such as Spring REST API generation and Issue -> Branch -> Develop -> Test -> Review.

Exit criteria:

- only applicable Skill metadata enters initial context and full instructions load only after selection;
- composed Skills cannot broaden the approved Tool scope;
- invalid, conflicting, or proposed-only Skills cannot execute;
- memory writes preserve their canonical destination and evidence.
- workflow stages cannot bypass commit, push, PR, merge, deployment, or destructive-action approval.

## Delivery Gate 11: Extensible Tooling And Plugin Marketplace

Status: Planned

Dependencies:

- operational Tool, evidence, verification, runtime, MCP, Skill, and messaging boundaries.

Scope:

- Git and terminal Tools with explicit approval boundaries;
- Plugin SDK and local marketplace contracts;
- plugin and template installation;
- artifact provenance, ownership, version, integrity, compatibility, and rollback;
- framework integrations such as Spring, MyBatis, Oracle, React, Vue, and WebSquare.
- Agent plugin packages for language, framework, cloud, security, architecture, review, and testing roles;
- marketplace capability, permission, provenance, compatibility, integrity, isolation, disable, removal, and rollback metadata.
- permission-manifest review, signature/integrity verification, dependency resolution, malicious-package scanning, sandbox profiles, and installation audit records;
- Git, terminal, and external Tools expose command/changed-file preview, dry-run when supported, directory-scoped permission, secret redaction, isolation, and task-linked recovery evidence.

Exit criteria:

- every extension uses the common policy, event, evidence, verification, and RunRecord pipeline;
- destructive and external actions require explicit authority;
- installed artifacts can be traced, upgraded, disabled, removed, and rolled back safely.
- an extension or Tool cannot execute merely because it is popular, installed, or returned by a marketplace search; verified policy and integrity evidence control availability.

## Delivery Gate 12: Desktop, CLI, API And Editor Interfaces

Status: Planned

Dependencies:

- stable operational runtime, Workspace, MCP, and plugin APIs.

Implemented bounded shared-API prerequisite:

- `FileSystemAuthenticatedCancellationApplication` delegates one exact retained
  `CANCEL` to the existing terminal transition owner while requiring a caller-supplied
  trusted `ControlRequestAuthorizer`;
- optional all-or-none filesystem event configuration composes durable
  `CANCELLATION_APPLIED` storage and opaque reference publication with exact replay;
- no CLI/API/editor/Desktop authentication adapter, credential provider, queue
  disposition, process signal, or `PAUSE`/`RESUME` application is implied, and the
  overall gate remains Planned.

Contract Verified authenticated-interface core:

- the first composition uses a short-lived detached signed exact-request grant whose
  trust domain, Goal, complete retained Control request digest, `CANCEL`, authorization,
  issuer/key/subject, policy revision, and issue/expiry times are verified against
  separately provisioned operator-owned public trust policy;
- a deterministic non-secret authorization audit point persists before approval;
  pre-runtime retry revalidates the identical transient proof against current time,
  trust, and revocation, while an already durable terminal cancellation retains its
  existing replay and suffix-repair semantics;
- `AuditBackedSignedCancellationAuthorizer` performs current target/request/signature/
  policy/time/lifetime/subject/key/revocation validation and persists or exact-resolves
  the integrity-checked audit before approval; malformed proof, changed identity reuse,
  changed trust-configuration revision or revocation fact, corruption, and persistence
  failure fail closed;
- `PinnedFileCancellationGrantTrustPolicyLoader` is the Contract Verified read-only
  bootstrap prerequisite: it accepts one exact absolute normalized file and one
  independently provisioned complete-file SHA-256, performs one bounded no-follow read,
  parses only the same strict canonical UTF-8 public-only Ed25519 snapshot, and derives
  its configuration revision from the digest without a writer, discovery, fallback, or
  cache;
- the first production consumer is the supported `scheduler-apply-cancel` CLI. It binds
  path/pin only from a fixed strict bounded sibling of the exact installed JAR, reads one
  bounded no-follow proof, composes the signed authorizer/audit/filesystem application,
  preserves authorization-bypassing terminal replay, and optionally publishes the
  existing event. It exposes no request-selected trust, actor, key, clock, credential,
  or approval field;
- the operator-maintenance state machine is now Contract Verified as an unexposed
  library separate from runtime, with a distinct Contract Verified Java operator main
  selected by one repository-local Gradle task: it keeps metadata v1, derives the pin internally,
  publishes content-addressed policy before fixed metadata, distinguishes INSTALL
  refusal from ROTATE persistent stateless lock plus expected-current CAS, exact-replays
  current binding including lost-response recovery, retains old artifacts, and makes no
  automatic rollback or privileged anti-rollback claim;
- separate operator start-script/distribution packaging is Contract Verified through
  one fixed custom Gradle distribution, generated Unix/Windows launcher pair, build
  install layout, ZIP/TAR assembly, and copied-layout subprocess execution confined to
  JUnit-owned temporary installations; the default runtime distribution remains
  `EnhancerCli`;
- real OS/application installation, installer/provisioning and permission mutation,
  deployment/signing/publication/release, real-install rotation execution/application anti-rollback,
  proof production/private keys, credentials, IdP/session integration, API/editor/
  Desktop authentication, queue/process/Tool/effect behavior, and pause/resume remain
  unimplemented, so Gate 12 remains Planned.
- the real operator installation boundary now has a Contract Verified pure Java layer:
  one already-authorized plan, distinct publisher/operator/runtime principals, derived
  artifact identities, fixed revisioned effective-access matrix and phase order, bounded
  evidence/failures, and an unwired platform-neutral permission adapter port. It still
  now also has a Contract Verified, unwired Windows adapter boundary over an injected
  gateway, with canonical SID/token/DACL/reparse/volume/file-identity, separate raw-right
  versus typed-operation, atomic-publication, durability, and runtime-probe validation.
  It still requires a real/native Windows gateway or POSIX UID/GID/mode/ACL enforcement,
  immutable version staging, policy-first/metadata-last publication, runtime-principal
  probing, activation last, and persisted exact transaction recovery. No installer,
  real permission change, deployment, cleanup, release, or anti-rollback anchor is
  present.

Scope:

- one shared application API for Run creation/inspection, approvals, verification, evidence, typed controls, recovery, and change review;
- production CLI as the first reference surface over that API;
- VSCode Extension as the second surface for repository-context work;
- Desktop application as a later supervisory surface across runs and projects, plus a web dashboard where justified;
- Workspace, run, event, evidence, task, approval, Skill, MCP, and model views;
- Workspace observation integrations for the sources these interfaces own: diagnostics, terminal-session metadata, and active/selected file context, using the already-typed Workspace source kinds (moved from Gate 6 by the 2026-07-15 re-scope decision);
- authenticated typed pause, resume, cancel, reprioritize, reassign, mediation, and injected-work proposal controls;
- consistent control surfaces without duplicated runtime policy.
- one change-centered review projection containing goal/plan, changed files and bounded diff, tests/evidence, provenance, risks, costs, approval points, recovery/rollback state, and commit readiness;
- Enhancer Shell and Intent Understanding that compile one user request into an inspectable Goal, plan, authorization scope, execution graph, and verification plan.

Exit criteria:

- every interface invokes shared application boundaries;
- CLI, VS Code, Desktop, Web, and external clients observe the same Run, approval, verification, evidence, and control semantics without interface-specific policy forks;
- users can inspect and control runs without hidden authority changes;
- a user can review what will change, why, which evidence supports it, what remains risky, and which approval is next from one change-centered view;
- active file, selection, diagnostics, Git, and terminal metadata enter Workspace through explicit adapters.

## Delivery Gate 13: Multi-Agent And Background Execution

Status: Planned

Dependencies:

- operational single-agent runtime, scheduler, messaging, recovery, and independent verification.
- a versioned single-agent baseline from the Product Journey and Evaluation Track.

Scope:

- select the smallest sufficient topology: one worker, sequential pipeline, Producer-Reviewer, bounded fan-out/fan-in, expert routing or supervisor allocation, and shallow hierarchy only when justified;
- immutable capability-roster revisions derived from approved task, validated metadata, policy, data classification, budgets, and isolation capacity;
- Planner -> Queue -> Coder -> Queue -> Reviewer -> Queue -> Tester -> Memory pipelines;
- typed versioned handoffs over the common Message Bus with one Kernel-owned terminal-state coordinator;
- bounded delegation, Producer-Reviewer revision, deterministic synthesis, and worker/reviewer/verifier role separation;
- concurrency, cost, context, and time budgets;
- resumable background runs, diagnostic-only heartbeat and quality telemetry, and explicit conflict handling.

Exit criteria:

- Agents never require direct peer calls;
- delegation cannot broaden user authority;
- parallel branches share one immutable snapshot, have isolated ownership, and use a named deterministic reducer;
- reviewer pass, heartbeat, confidence, or self-reported quality cannot create Verified or Completed state;
- every message and result preserves provenance, evidence, causation, and run identity;
- interrupted or conflicting work can stop, resume, or roll back safely.
- multi-agent promotion demonstrates a predeclared improvement over the single-agent baseline on the same versioned journey fixtures and comparable budget envelope; additional Agents without measured benefit do not satisfy the gate.

## Delivery Gate 14: Project Brain Graph And Governed Cloud Sync

Status: Planned

Dependencies:

- stable Project Brain, Workspace, identity, event, and recovery contracts.

Scope:

- mature Decision, Architecture, Dependency, Task, and Execution graph projections;
- impact and trace queries linking decisions, code, tests, bugs, commits, issues, PRs, Agents, Skills, models, and evidence;
- opt-in synchronization of approved project memory and run metadata;
- encryption in transit and at rest;
- ownership, tenancy, secret exclusion, retention, conflict resolution, offline behavior, and audit logs;
- explicit key ownership, rotation, recovery, and revocation plus end-to-end encryption where server-side processing is not authorized;
- no automatic source publication.

Exit criteria:

- secrets and excluded local state never enter sync payloads;
- conflicts are visible and recoverable;
- local operation remains available without cloud connectivity;
- sync events cannot grant execution authority.
- graphs can be rebuilt from canonical repository, Git, and RunRecord sources and never silently overwrite them.

## Delivery Gate 15: Governed Self-Improvement

Status: Principles Accepted

Dependencies:

- Tool execution, independent verification, snapshots, rollback, budgets, scheduler, messaging, and human approval are Operational.
- the versioned evaluation harness can preserve a fixed baseline, held-out fixtures, costs, failures, and evaluator provenance.

Scope:

- immutable evaluation baseline and bounded candidate experiment ledger;
- bounded self-improvement proposal;
- before-and-after evidence;
- separate review;
- tested rollback;
- no automatic commit, push, release, permission escalation, or Constitution amendment.

Operational Milestone 2 is reached only when one human-approved self-improvement run completes and rolls back safely in a controlled test.

## Delivery Gate 16: SDK And Open Source Release

Status: Planned

Dependencies:

- stable operational runtime, protocol, interface, and plugin boundaries.
- release-scoped canonical journeys and their versioned evaluation fixtures, baselines, and predeclared thresholds.

Scope:

- versioned SDK and compatibility policy;
- installation, upgrade, migration, and deployment guides;
- Windows, macOS, and Linux support matrix, one-command or one-click installation where supported, automatic-update policy, tested rollback, offline installation, and configuration/data migration;
- CI/CD and packaged Desktop, CLI, API, extensions, and server components;
- contributor, security, protocol, and marketplace documentation.
- reproducible builds, signatures, provenance attestations, checksums, and SBOM generation/verification;

Exit criteria:

- clean-machine installation is verified;
- release artifacts have provenance and checksums;
- compatibility and migration behavior are tested and documented;
- release checks pass in CI.
- every release-scoped canonical journey meets its versioned predeclared quality threshold, and results include failures rather than only successful demonstrations;
- update rollback and offline installation are verified on every supported platform class claimed by the release.

## Six-Month Delivery Outlook

The month mapping is directional. Delivery gates, not calendar pressure, control promotion.

| Month | Primary gates | Intended outcome |
|---|---|---|
| 1 | Gates 0-3 | Foundation, governed Tool/evidence, and hardened loop integration |
| 2 | Gates 4-6 | Verification, RunRecord, first CLI, Workspace, and Project Brain |
| 3 | Gates 7-9 | Event/Message Bus, Agent Runtime, Scheduler, Model Gateway, and MCP |
| 4 | Gates 10-12 | Skill/Memory runtime, plugins, marketplace, Desktop, API, and editor interfaces |
| 5 | Gates 13-14 | Multi-agent/background execution and governed Cloud Sync |
| 6 | Gates 15-16 | Governed self-improvement, SDK, CI/CD, and open-source release preparation |

## RFC Track

Status: Accepted

- RFC-0001: Constitution
- RFC-0002: AI Behavior Specification
- RFC-0003: Prompt Contract
- RFC-0004: Context Builder
- RFC-0005: Planner
- RFC-0006: Tool Specification
- RFC-0007: Skill Specification
- RFC-0008: Memory Specification
- RFC-0009: Multi Agent
- RFC-0010: AI Operating System
- RFC-0011: Plugin SDK
- RFC-0012: Self Improvement
- RFC-0013: Model Gateway
- RFC-0014: Model Execution Profile

RFC acceptance does not imply Contract Verified, Integrated, Operational, or Released capability maturity.

Future detailed RFC work is required before process workers, concrete IPC production adapters, broader Scheduler control/effect/retry policy, MCP and Model Gateway, Cloud Sync, or Gate 8 Operational promotion become active. The already accepted bounded Gate 8 queue, lifecycle, lease, and dispatch increments remain valid and do not imply those broader capabilities.

## Selective Agent Harness Pattern Adoption

| Priority | Pattern | Delivery gate | Current maturity |
|---|---|---|---|
| P0 | Repeated termination and stagnation detection | Gate 0 | Contract Verified |
| P0 | Verification evidence contract | Gate 0 | Contract Verified |
| P1 | Independent verifier | Gate 4 | Integrated |
| P1 | Progressive Skill loading | Gate 10 | Planned |
| P2 | Artifact provenance | Gate 11 | Planned |
| P2 | Token and context budget | Gate 9 | Planned |
| P3 | Self-improvement safeguards | Gate 15 | Principles Accepted |

Every pattern must remain provider-neutral and must use the same authorization, evidence, verification, recovery, and lifecycle rules as the core runtime.
