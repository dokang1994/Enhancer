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

Status: Foundation Contracts Verified

Contract-verified capabilities:

- repository document context reading;
- deterministic next-task proposal;
- single-pass context-to-planner orchestration;
- bounded repeated-loop termination and consecutive stagnation detection;
- bounded Tool result and verification-evidence records.

Operational repository governance:

- Constitution 1.1 Kernel;
- Document Driven Development;
- explicit lifecycle and authorization rules;
- Git-backed project memory and session handoff;
- test and evidence requirements.

Not yet integrated or operational:

- concrete Tool execution;
- complete evidence persistence;
- Tool-result-driven Agent Loop transitions;
- sequential independent verification;
- durable run records;
- CLI or application entry point;
- LLM invocation;
- Skill runtime, plugins, MCP, multi-agent, background execution, and self-improvement.

## Contract Continuation Rule

Foundation contracts may continue after the initial contract phase, but each new contract must:

1. name its integration consumer in the current or immediately following delivery gate;
2. define observable behavior and a failure mode;
3. include focused tests;
4. identify the integration test that will promote it beyond Contract Verified;
5. avoid opening a later platform track before its dependency gate exits.

A contract without an identified consumer remains a proposal and does not become the next implementation task.

## Delivery Gate 0: Foundation Safety Contracts

Status: Contract Verified

Delivered:

- Context Reader and structured repository context;
- deterministic Task Planner and explicit proposal state;
- Assisted Development Loop result contract;
- Agent Loop completion, failure, iteration, and stagnation exits;
- ToolResult and bounded VerificationEvidence;
- Constitution 1.1 governance.

Exit evidence:

- 6 JUnit suites and 25 tests;
- explicit documented out-of-scope boundaries;
- provider-neutral contracts.

Remaining limitation:

- these pieces do not yet execute one connected Agent run.

## Delivery Gate 1: Tool Execution Boundary

Status: Specified - Next

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

## Delivery Gate 2: Evidence Persistence

Status: Specified

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

## Delivery Gate 3: Agent Loop And Tool Integration

Status: Specified

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

## Delivery Gate 4: Sequential Verification And Run Record

Status: Specified

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

## Delivery Gate 5: First Operational CLI

Status: Specified

Goal:

Expose the connected read-only Agent run through a supported local command.

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

## Delivery Gate 6: Prompt And LLM Boundary

Status: Planned

Dependencies:

- Delivery Gates 1 through 5 are Operational.

Scope:

- PromptRequest and model-neutral message contract;
- Prompt Builder from bounded Context and approved task;
- LlmClient interface;
- deterministic fake adapter;
- one explicitly selected real provider adapter;
- timeout, cancellation, token and context budgets;
- structured response validation and redaction.

Exit criteria:

- the Agent can replace a deterministic decision step with a provider call without bypassing Tool policy or verification;
- provider failure and malformed output have explicit stop reasons;
- the deterministic path remains available for tests;
- cost and context use are observable.

## Delivery Gate 7: Skill And Memory Runtime

Status: Planned

Dependencies:

- operational single-agent run and bounded LLM context.

Scope:

- progressive Skill discovery, metadata-first loading, validation, and least-privilege enforcement;
- repository memory reads and explicit writes;
- memory distillation into Decisions, State, Handoff, or validated Skills;
- Skill invocation evidence in RunRecord.

Exit criteria:

- only applicable Skill metadata enters initial context;
- full Skill instructions load on selection;
- permissions are enforced by Tool policy;
- invalid or proposed-only Skills cannot execute.

## Delivery Gate 8: Extensible Tooling

Status: Planned

Dependencies:

- operational Tool, evidence, verification, run record, and Skill boundaries.

Scope:

- Git and terminal Tools with explicit approval boundaries;
- MCP client boundary;
- plugin and template installation;
- artifact provenance, ownership, version, integrity, and rollback;
- task queue and scheduler foundations.

Exit criteria:

- every external capability uses the same policy, evidence, verification, and run-record pipeline;
- destructive and external actions require explicit authority;
- installed artifacts can be traced and safely removed or rolled back.

## Delivery Gate 9: User Interfaces

Status: Planned

Dependencies:

- stable operational CLI and runtime APIs.

Scope:

- VSCode Extension;
- web dashboard;
- run, evidence, task, and approval views;
- background status without background autonomy.

Exit criteria:

- interfaces call stable application boundaries rather than duplicate runtime policy;
- users can inspect requests, approvals, evidence, verification, and stop reasons.

## Delivery Gate 10: Multi-Agent And Background Execution

Status: Planned

Dependencies:

- operational single-agent runtime, scheduler, recovery, and independent verification.

Scope:

- bounded delegation;
- worker and verifier role separation;
- concurrency and budget limits;
- resumable background runs;
- conflict and cancellation handling.

Exit criteria:

- multi-agent execution cannot broaden user authority;
- every delegated result preserves provenance and evidence;
- interrupted work can resume or roll back safely.

## Delivery Gate 11: Governed Self-Improvement

Status: Principles Accepted

Dependencies:

- Tool execution, independent verification, snapshots, rollback, budgets, scheduler, and human approval are Operational.

Scope:

- bounded self-improvement proposal;
- before-and-after evidence;
- separate review;
- tested rollback;
- no automatic commit, push, release, permission escalation, or Constitution amendment.

Operational Milestone 2 is reached only when one human-approved self-improvement run completes and rolls back safely in a controlled test.

## Delivery Gate 12: SDK And Open Source Release

Status: Planned

Dependencies:

- stable operational runtime and plugin boundaries.

Scope:

- Plugin SDK;
- versioned compatibility policy;
- installation and upgrade guides;
- CI/CD;
- packaged CLI and extension;
- contributor and security documentation.

Exit criteria:

- clean-machine installation is verified;
- release artifacts have provenance and checksums;
- compatibility and migration behavior are documented;
- release checks pass in CI.

## Six-Month Delivery Outlook

The month mapping is directional. Delivery gates, not calendar pressure, control promotion.

| Month | Primary gates | Intended outcome |
|---|---|---|
| 1 | Gates 0-2 | Foundation contracts plus real read-only Tool and evidence storage |
| 2 | Gates 3-5 | Integrated Agent run and first operational CLI |
| 3 | Gates 6-7 | Bounded LLM, Prompt, Skill, and Memory runtime |
| 4 | Gates 8-9 | Extensible tooling and user interfaces |
| 5 | Gate 10 | Multi-agent and background operation |
| 6 | Gates 11-12 | Governed self-improvement, SDK, CI/CD, and release preparation |

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

## Selective Agent Harness Pattern Adoption

| Priority | Pattern | Delivery gate | Current maturity |
|---|---|---|---|
| P0 | Repeated termination and stagnation detection | Gate 0 | Contract Verified |
| P0 | Verification evidence contract | Gate 0 | Contract Verified |
| P1 | Independent verifier | Gate 4 | Specified |
| P1 | Progressive Skill loading | Gate 7 | Planned |
| P2 | Artifact provenance | Gate 8 | Planned |
| P2 | Token and context budget | Gate 6 | Planned |
| P3 | Self-improvement safeguards | Gate 11 | Principles Accepted |

Every pattern must remain provider-neutral and must use the same authorization, evidence, verification, recovery, and lifecycle rules as the core runtime.
