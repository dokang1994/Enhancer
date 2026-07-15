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

Status: Delivery Gate 5 Operational; Delivery Gate 6 Specified - Next

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
- Workspace, Project Brain, Event/Message Bus, IPC, Agent Runtime, Scheduler, and Model Gateway;
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
- retention and cleanup policy specification.

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

Status: Specified - Next

Current increment:

- Integrated: the metadata-only immutable `WorkspaceSnapshot` contract with canonical identity, typed source metadata, explicit freshness/availability, deterministic ordering, and bounded observations, connected to the real Context Reader and its downstream consumers;
- Integrated: the read-only `ProjectBrainView` composing one real snapshot, repository-memory metadata with derived freshness, and real RunRecord provenance under a matching approved task;
- Integrated: the repository-memory path from a real governed run and really-loaded repository memory through `RepositoryMemorySnapshotCollector` into the composed view, including explicit divergence detection;
- Integrated: the endpoint-checked graph projection contract and the task-to-decision-to-code-to-test impact query, populated and answered exclusively through the real producer chain naming the real stored execution;
- Integrated: accepted-decision projection from the decision log's own status lines and run-record metadata observation over the real store with explicit corruption surfacing;
- Operational: the production CLI `run` path composes the view and the produced graph for every recorded run, observing prior run records, merging decision nodes, and reporting bounded snapshot, freshness, graph, and impact metadata, evidenced by actual-repository runs;
- next increment: a task-to-decision reference grammar with `JUSTIFIED_BY` projection, or the next read-only source adapter;
- deferred: Git/diagnostics/selection/terminal adapters, payload capture, modifies/verified-by producers, persistence, and messaging.

Dependencies:

- the first operational read-only run and RunRecord are available.

Scope:

- immutable WorkspaceSnapshot and source freshness metadata;
- one common immutable input-snapshot identity and approved task revision for every later worker handoff;
- repository files plus active and selected file context;
- read-only Git status and diff adapters;
- diagnostics and terminal-session metadata adapters without command authority;
- Project Brain view combining repository memory, workspace observations, decisions, and run history with provenance.
- graph projection contracts for Decision, Architecture, Dependency, Task, and Execution relationships;
- first rebuildable task-to-decision-to-code-to-test impact query.

Exit criteria:

- one snapshot can explain which files, Git state, diagnostics, selection, and documents informed a run;
- stale and unavailable sources are explicit;
- Workspace observations cannot override repository authority or grant Tool permission;
- snapshot size and sensitive-data boundaries are enforced.
- graph nodes and edges retain source, freshness, version, and rebuild status.

## Delivery Gate 7: Event Bus And IPC Foundation

Status: Planned

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

Status: Planned

Dependencies:

- operational verification, RunRecord, Workspace, and messaging foundations.

Scope:

- persisted Goal and AgentRun state machine;
- Goal -> Planner -> Executor -> Memory -> Reflection -> Retry -> Done transitions;
- Scheduler queues, dependency validation, cycle rejection, fenced leases, idempotency, budgets, cancellation, pause, resume, reassignment, and recovery;
- Planner, Coder, Reviewer, Tester, and Memory worker roles behind message contracts;
- single-agent sequential worker first, without multi-agent concurrency.
- Dependency Analyzer and Verification Engine as Kernel services;
- resource budgets, locks, leases, and recovery checkpoints.

Exit criteria:

- a run survives interruption and resumes from durable state;
- workers communicate through the bus rather than direct Agent calls;
- retry, stagnation, timeout, cancellation, verification, and completion are explicit events;
- runtime scheduling cannot expand task or Tool authority.

## Delivery Gate 9: Model Gateway And MCP Core

Status: Planned

Dependencies:

- the event-driven single-agent runtime is operational.

Scope:

- provider-neutral ModelRequest, response, usage, and routing contracts;
- provider-neutral execution profiles for capability, model class, locality, reasoning, context, token, cost, time, and data-classification requirements;
- Model Router with deterministic fake plus explicitly selected provider adapters;
- timeout, cancellation, token, context, cost, redaction, and response-validation budgets;
- MCP Server exposing governed Tools, resources, Workspace views, and memory;
- MCP Client consuming external servers through existing policy, evidence, verification, and RunRecord boundaries.
- privacy-aware routing across approved local and remote providers using data classification, capability, cost, latency, context, and availability.

Exit criteria:

- Claude Code, Cursor, VS Code, or another MCP client can inspect an approved Enhancer resource without bypassing policy;
- an external MCP Tool follows the same evidence and verification path as a native Tool;
- provider or protocol failure produces explicit runtime events and stop reasons;
- model output and MCP content cannot grant authority.
- sensitive-code fixtures remain local and remote adapters receive only policy-approved data.

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

Exit criteria:

- every extension uses the common policy, event, evidence, verification, and RunRecord pipeline;
- destructive and external actions require explicit authority;
- installed artifacts can be traced, upgraded, disabled, removed, and rolled back safely.

## Delivery Gate 12: Desktop, CLI, API And Editor Interfaces

Status: Planned

Dependencies:

- stable operational runtime, Workspace, MCP, and plugin APIs.

Scope:

- Desktop application;
- production CLI and local API;
- VSCode Extension and web dashboard;
- Workspace, run, event, evidence, task, approval, Skill, MCP, and model views;
- authenticated typed pause, resume, cancel, reprioritize, reassign, mediation, and injected-work proposal controls;
- consistent control surfaces without duplicated runtime policy.
- Enhancer Shell and Intent Understanding that compile one user request into an inspectable Goal, plan, authorization scope, execution graph, and verification plan.

Exit criteria:

- every interface invokes shared application boundaries;
- users can inspect and control runs without hidden authority changes;
- active file, selection, diagnostics, Git, and terminal metadata enter Workspace through explicit adapters.

## Delivery Gate 13: Multi-Agent And Background Execution

Status: Planned

Dependencies:

- operational single-agent runtime, scheduler, messaging, recovery, and independent verification.

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

Scope:

- versioned SDK and compatibility policy;
- installation, upgrade, migration, and deployment guides;
- CI/CD and packaged Desktop, CLI, API, extensions, and server components;
- contributor, security, protocol, and marketplace documentation.

Exit criteria:

- clean-machine installation is verified;
- release artifacts have provenance and checksums;
- compatibility and migration behavior are tested and documented;
- release checks pass in CI.

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

Future detailed RFC work is required for Workspace and Project Brain, Event/Message Bus and IPC, Agent Runtime and Scheduler, MCP and Model Gateway, and Cloud Sync before their implementation gates become active.

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
