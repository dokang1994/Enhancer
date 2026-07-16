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

- Constitution 1.1 Kernel;
- Document Driven Development;
- explicit lifecycle and authorization rules;
- Git-backed project memory and session handoff;
- test and evidence requirements.

Operational capability:

- first supported local `run` and `replay` CLI over the governed read-only vertical slice.

Not yet integrated or operational:

- LLM invocation;
- remaining Workspace adapters, Project Brain graph persistence, Event/Message Bus production wiring, concrete IPC adapters, Agent Runtime, Scheduler, and Model Gateway;
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
- Constitution 1.1 governance.

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

## Delivery Gate 6: Workspace And Project Brain Foundation

Status: Integrated

Current increment:

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
- Integrated maturity assessment completed: the work-message queue/journal/replay/idempotency path is Integrated, but Gate 7 remains Contract Verified because result/control/handoff and non-empty-causation flows, topic and failure/retry/dead-letter/cancellation/cascade-ordering/backpressure branches, and `MessageTransport` have no named real upstream-to-downstream production connection;
- deferred: any local-process or remote IPC adapter, persistence, threading, and production wiring.

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

Current increment:

- Contract Verified: immutable `WorkItem` admission over one unchanged Gate 7 work envelope, with a distinct canonical identity and bounded required capability but no scheduling or execution behavior;
- Contract Verified: immutable `QueuedWork` with up to 256 unique dependency identities plus a deterministic run-scoped `SingleWorkerSchedulerQueue` bounded to 4096 admissions, dependency-first validation, FIFO readiness, one active slot, matching completion, and no authority expansion;
- Contract Verified: canonical queue identity and single-logical-run binding, immutable schema-v1 queue snapshots, bounded integrity-checked atomic filesystem persistence, persist-before-exposure enqueue/claim/completion, fail-closed corruption/version checks, and restart recovery that requeues interrupted active work in admission order under explicit at-least-once semantics;
- Contract Verified: one exact-WorkItem `RuntimeGoal`, one schema-v1 `RuntimeAgentRun`, deterministic forward-only lifecycle transitions, matching typed terminal result envelopes, Verified-only completion, monotonic persist-before-exposure revisions, bounded integrity-checked filesystem state, and exact restart recovery without invented ownership;
- Contract Verified: a bounded fenced single-owner `AgentRunLease` acquired only from `READY`, with injected time, persisted monotonic fence tokens, matching unexpired owner/fence checks for renewal and execution completion, and durable expiry reclamation back to `READY` across restart;
- next increment: connect one durable Scheduler queue claim to one durable Goal/AgentRun planning, readiness, and lease acquisition path without Tool execution or external effects;
- deferred: general forward-reference graph/cycle handling, multiple AgentRuns and retry, cancellation/pause/resume, priority/fairness, budgets, external-effect idempotency/fencing, checkpoints beyond current snapshots, worker execution, schema migration beyond v1, power-loss directory durability, multi-process coordination, distributed clock-skew handling, and production wiring.

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

RFC acceptance does not imply Contract Verified, Integrated, Operational, or Released capability maturity.

Future detailed RFC work is required for the Agent Runtime and Scheduler, concrete IPC adapters, MCP and Model Gateway, and Cloud Sync before those implementation tracks become active.

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
