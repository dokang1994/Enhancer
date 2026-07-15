# Decision Log

## Accepted Decisions

### 2026-07-15: Answer The First Impact Query Over One Graph With Explicit Rebuild Status And No Transitive Closure

Status: Accepted Decision

Context:

- The graph projection contract is Contract Verified and the roadmap names the task-to-decision-to-code-to-test impact query as its first consumer.
- No producer projects real graphs yet, so the query can only be proven against contract-constructed projections.
- Transitive dependency propagation over `DEPENDS_ON` would answer a broader "what else is affected" question, but its semantics (direction, depth, cycles) deserve their own decision once real dependency projections exist.
- A query answer detached from its graph's snapshot identity could not say when it must be recomputed.

Decision:

- Add `TaskImpactQuery` and the immutable `TaskImpact` result under `com.enhancer.brain`, answering over exactly one `ProjectBrainGraph`.
- Traverse only the named chain from the queried task: `JUSTIFIED_BY` to decisions, `MODIFIES` to artifacts, `VERIFIED_BY` from those modified artifacts to verifying artifacts, and `RECORDED_AS` to executions.
- Carry the graph's source snapshot identity in the result and derive one rebuild-required status that is true exactly when the task node, any traversed edge, or any returned node requires rebuild.
- Keep result ordering derived from the graph's canonical ordering, deduplicate shared verifying artifacts, return empty collections for an edgeless task, and reject unknown or non-task identities.
- Defer transitive `DEPENDS_ON` closure, multi-graph answering, producers, persistence, caching, and indexing to separate approved tasks.

Rationale:

The named chain is the exact question the roadmap commits to answering first, and answering it over one snapshot-keyed graph keeps the result reproducible: the same graph always yields the same answer, and the snapshot identity plus rebuild status say precisely when that answer stops being trustworthy. Deriving one aggregate rebuild flag from element provenance keeps staleness visible without inventing a second freshness model.

Consequences:

- The query is only as complete as the projected graph; missing projections yield empty results rather than inferred impact.
- Impact through dependency chains is not yet visible; that closure arrives with real dependency projections and its own decision.
- Producers must later construct graphs from repository evidence before the query answers anything about the actual project.

### 2026-07-15: Constrain The First Graph Projection Contract To Typed Endpoint-Checked Metadata Keyed To One Snapshot

Status: Accepted Decision

Context:

- The Gate 6 scope requires graph projection contracts for Decision, Architecture, Dependency, Task, and Execution relationships, and its exit criteria require nodes and edges to retain source, freshness, version, and rebuild status.
- The Architecture defines Project Brain graphs as rebuildable projections that must never silently overwrite their authoritative sources.
- No projection producer, query, or persistence exists yet; the first consumer is the task-to-decision-to-code-to-test impact query in a later increment.
- An unconstrained stringly-typed graph would accept relationships that the impact query could not interpret and that no source could justify.

Decision:

- Add the projection contract under `com.enhancer.brain` with five node kinds (task, decision, component, artifact, execution) and six endpoint-checked edge kinds: `JUSTIFIED_BY` task-to-decision, `SUPERSEDES` decision-to-decision, `DEPENDS_ON` between components and artifacts, `MODIFIES` task-to-artifact, `VERIFIED_BY` artifact-to-artifact, and `RECORDED_AS` task-to-execution.
- Give every node and edge an immutable provenance of bounded source reference, optional lowercase SHA-256 source revision, and an explicit freshness state (`CURRENT`, `STALE`, `SOURCE_MISSING`) whose rebuild-required status is derived, not stored separately.
- Require a source revision for Current and Stale provenance and prohibit it for Source-Missing provenance, mirroring the Workspace observation digest rules.
- Key each `ProjectBrainGraph` to one valid source snapshot identity with an explicit projection time and a versioned projection identifier, so projections are traceable and rebuildable rather than free-floating.
- Enforce deterministic ordering, duplicate and self-loop rejection, unknown-endpoint rejection, and 4096-entry bounds in the contract itself.
- Defer projection producers, the impact query, traversal, persistence, rebuild execution, and confidence metadata to separate approved tasks.

Rationale:

Endpoint-kind checking makes the six relationships carry their meaning in the type system, so a later impact query can traverse task-to-decision-to-code-to-test chains without interpreting conventions. Deriving rebuild status from freshness avoids two fields that could contradict each other. Keying to the snapshot identity reuses the already-verified content-addressed boundary instead of inventing a second projection identity source.

Consequences:

- Producers must later justify every node and edge from repository documents, RunRecords, or snapshot observations through separate approved tasks.
- The five graph domains share one contract; a domain needing a new relationship must extend the edge taxonomy through a recorded decision rather than reusing a loosely-fitting kind.
- Graph identity intentionally does not exist yet; equality is structural, and a persisted graph identity would arrive with persistence work.

### 2026-07-15: Compose The Project Brain View On The Existing CLI Run Path Without Widening Its Surface

Status: Accepted Decision

Context:

- The repository-memory path is Integrated in a temporary-project test, but no production caller composes the `ProjectBrainView` during an actual governed run.
- The `EnhancerCli` `run` command already loads the full `ProjectContext` to derive and validate the approved task, then discards it.
- Reloading memory after the run would create a second read of the same sources and could disagree with the memory that actually informed approval.
- Persisting the snapshot identity would change the RunRecord schema, which carries its own compatibility and replay obligations.

Decision:

- On the `run` path, keep the already-loaded `ProjectContext`, collect the `WorkspaceSnapshot` through `RepositoryMemorySnapshotCollector` with a capture time taken before worker execution, and compose the `ProjectBrainView` after finalization with the persisted `RunRecord`.
- Compose for every run outcome that produces a record, since the view explains what informed the run regardless of how the run stopped.
- Report only bounded metadata in the existing output: the snapshot identity, the observation count, and a matched/diverged/notObserved freshness summary; never document content, digest lists, or evidence.
- Keep exit codes, existing output lines, persist-before-report ordering, replay behavior, and the output bound unchanged, and add no command, argument, or authority.
- Defer persisting the snapshot identity in the RunRecord to the Gate 7 envelope work that already owns cross-handoff identity.

Rationale:

Composing from the memory that produced the approved task makes the reported snapshot describe exactly the inputs the run was approved against, at no additional authority or read cost. Reporting identity and freshness counts keeps the bounded-diagnostics contract intact while making the composition externally observable, which is what an Operational claim needs.

Consequences:

- The reported freshness is trivially all-matched unless repository documents change between context load and composition, because the same in-memory context is both the snapshot source and the comparison input; real divergence reporting arrives when snapshots persist or sources are re-observed.
- Replay does not reproduce the snapshot identity because the RunRecord does not store it; that linkage is explicitly deferred.
- A composition failure after the record is persisted surfaces as an internal error while the durable record remains replayable.

### 2026-07-15: Collect The First Real Workspace Snapshot From Already-Loaded Repository Memory

Status: Accepted Decision

Context:

- The `WorkspaceSnapshot` and `ProjectBrainView` contracts are Contract Verified, but every snapshot so far was hand-written by tests, so no real source informs the aggregate.
- The two named paths toward Gate 6 integration are an explicit source adapter or a production composition path; the adapter is the smaller increment.
- The Context Reader already loads bounded, containment-checked, UTF-8-validated repository documents, and the Gate 0 lifecycle test already produces a real persisted RunRecord in a governed temporary project.
- Giving the first collector its own filesystem access would duplicate the Context Reader's containment and bounds enforcement and widen the authority surface without need.

Decision:

- Add a read-only `RepositoryMemorySnapshotCollector` under `com.enhancer.workspace` that derives a snapshot from a project root, an explicit caller-supplied capture time, an `ApprovedTask`, and an already-loaded `ProjectContext`.
- Derive the `ApprovedTaskRevision` inside the collector by digesting the approved task's source document content out of the same loaded memory, so the revision provably describes the memory that was actually read; reject a memory without that document.
- Emit one `AVAILABLE` `REPOSITORY_DOCUMENT` observation per loaded document with the document path as source identity, `context-reader` provenance, and a computed lowercase SHA-256 content digest; retain no content.
- Reuse `WorkspaceSnapshot.capture` for ordering, duplicates, bounds, and canonical identity instead of adding a second identity computation.
- Take the capture time as an explicit parameter rather than reading a clock inside the collector, keeping collection deterministic and testable.
- Prove the end-to-end path in an integration test that combines a real governed CLI run, the real Context Reader, the collector, and the view, including a divergence check after the source document changes.
- Defer Git, diagnostics, selection, terminal, and RunRecord-source adapters, and any production composition path, to separate approved tasks.

Rationale:

Deriving observations from memory the Context Reader already loaded keeps the collector free of filesystem authority and reuses the hardened containment path instead of duplicating it. Deriving the task revision from the same memory closes the gap where a caller could claim a revision digest unrelated to what was actually read. An explicit capture time avoids a hidden clock dependency, which keeps snapshots reproducible in tests and later replayable.

Consequences:

- The collector observes only what the Context Reader loads; unfetched sources are absent from the snapshot rather than marked `STALE` or `UNAVAILABLE`, and those states first appear with real per-source adapters.
- Observation time equals capture time for every document because loading time is not tracked per document in `ProjectContext`.
- The end-to-end evidence integrates the repository-memory path only; Gate 6 stays `Specified - Next` until its remaining scope exists.

### 2026-07-15: Compose The First ProjectBrainView As A Derived Read-Only Aggregate Keyed To One Snapshot

Status: Accepted Decision

Context:

- The Gate 6 `WorkspaceSnapshot` contract is Contract Verified but has no consumer, so Gate 6 cannot claim Integrated maturity.
- Architecture defines Project Brain as the reasoning-facing aggregate of repository memory, Workspace snapshots, decisions, and RunRecords that preserves source identity and freshness.
- The Context Reader already produces `ProjectContext` repository memory with document content, and the RunRecord Store already produces verified `RunRecord` run history.
- Carrying document content, Tool payloads, or evidence bodies into the aggregate would widen the sensitive-data boundary that the snapshot contract deliberately closed.

Decision:

- Add the first Project Brain aggregate as `ProjectBrainView` under a new provider-neutral `com.enhancer.brain` package that depends on `com.enhancer.workspace`, `com.enhancer.context`, and `com.enhancer.run`.
- Compose the view from exactly one `WorkspaceSnapshot`, one `ProjectContext`, and one `RunRecord`; the view derives its content and never collects sources itself.
- Key the view to the snapshot's canonical identity rather than recomputing a second identity.
- Project repository memory to document path, read order, and a computed lowercase SHA-256 of the document content; retain no document content in the aggregate.
- Derive explicit repository-memory freshness by comparing each document digest against the snapshot's `REPOSITORY_DOCUMENT` observation with the same source identity: equal digests are `SNAPSHOT_MATCHED`, differing digests are `SNAPSHOT_DIVERGED`, and an unobserved document is `NOT_OBSERVED`.
- Require the RunRecord's approved task identity and source document to equal the snapshot's `ApprovedTaskRevision`; reject a mismatched or unrelated run instead of silently aggregating it.
- Project the RunRecord to metadata-only provenance of logical run identity, record time, approved task identity, and verification status; exclude Tool requests, results, evidence bodies, and chat history.
- Expose the snapshot's Available, Stale, and Unavailable observations unchanged rather than collapsing or defaulting them.
- Add no persistence, graph projection, source adapter, or Tool authority in this increment.

Rationale:

Deriving the aggregate from existing verified inputs proves the snapshot contract is consumable without inventing a second collection path or a competing identity. Comparing loaded repository memory against the snapshot's observed digests turns divergence into an explicit, inspectable state rather than an unnoticed inconsistency, which is what the Gate 6 exit criterion about explaining a run actually requires. Requiring the run and snapshot to name the same approved task keeps provenance honest, because an aggregate that mixes unrelated work would misattribute the evidence it presents.

Consequences:

- The view is only as complete as the snapshot it is given; a snapshot without observations yields `NOT_OBSERVED` memory freshness rather than an error.
- Repository-memory digests are computed from already-loaded `ProjectContext` content, so this increment adds no new filesystem access or Tool authority.
- Freshness derivation is metadata comparison only; it does not prove that either revision is correct, authorized, or safe.
- Later graph projections and source adapters must extend this aggregate through separate approved tasks.
- Gate 6 may claim Integrated maturity only if fresh evidence shows the real snapshot, Context, and RunRecord contracts connected through this view.

### 2026-07-15: Start Gate 6 With A Metadata-Only Content-Addressed Workspace Snapshot

Status: Accepted Decision

Context:

- Gate 6 owns the immutable common input snapshot and approved task revision that later Project Brain, messaging, and worker handoffs require.
- Repository files, Git state, diagnostics, selection, terminal-session metadata, and RunRecords have different adapters and permissions that are not yet implemented.
- Capturing source content in the foundational contract would expand sensitive-data and memory boundaries before an actual consumer or adapter exists.

Decision:

- Begin Gate 6 with provider-neutral immutable metadata contracts under `com.enhancer.workspace`.
- Represent approved work with task identity, source-document identity, and a lowercase SHA-256 source revision; this is provenance and not Tool authority.
- Represent Workspace inputs as typed observations with source kind, source identity, adapter provenance, observation time, optional source-update time, explicit Available/Stale/Unavailable state, optional SHA-256 content identity, and bounded diagnostic reason.
- Store no source payload in the first snapshot contract.
- Compute a canonical SHA-256 snapshot identity in production from the normalized project root, capture time, task revision, and deterministically ordered observation metadata.
- Reject duplicate sources, inconsistent availability/digest combinations, invalid temporal relationships, excessive item counts, and unbounded strings.
- Name the next Gate 6 Project Brain aggregate as the immediate integration consumer and Gate 7 message envelopes as the next-gate identity consumer.
- Keep Gate 6 `Specified - Next` while this sub-capability advances only to Contract Verified.

Rationale:

A content-addressed metadata boundary gives every later adapter and worker one stable snapshot identity without prematurely retaining source payloads or creating command authority. Explicit unavailable and stale states prevent absence from being confused with freshness, while deterministic ordering keeps identity independent of caller collection order.

Consequences:

- Source adapters must later provide bounded metadata and digests through separate approved tasks.
- Snapshot equality and identity do not prove that source content is safe, trusted, or authorized; provenance and repository rules remain authoritative.
- A future ProjectBrainView must consume this contract before the Workspace capability can be called Integrated.

### 2026-07-15: Promote Gate 0 Only Through An Authority-Preserving Lifecycle Integration Audit

Status: Accepted Decision

Context:

- Gate 0 remains Contract Verified even though later Gates consume its Context, planning, loop, result, evidence, and governance contracts.
- The original Gate 0 limitation says the pieces do not execute one connected Agent run, while Gate 5 now provides an Operational read-only run over most of those boundaries.
- Planner proposals deliberately cannot approve themselves, so a valid integration test must not create an automatic Proposal-to-execution authority path.

Decision:

- Prepare one bounded Gate 0 promotion task that inventories each contract's real consumer and adds an end-to-end lifecycle integration test over a governed temporary repository.
- Split the test into a read-only planning phase and an execution phase separated by an explicit external test-fixture activation of `CURRENT_TASK.md`.
- Prove that planning leaves repository authority unchanged, execution before activation is rejected, and the activated read-only task reaches verified completion and restart-safe RunRecord replay through existing production boundaries.
- Reuse the Gate 5 CLI composition rather than adding a second production orchestrator.
- Allow the new characterization test to be initially GREEN if existing behavior already satisfies the accepted integration contract; create a RED cycle only for a genuine aligned behavior gap.
- Promote Gate 0 from Contract Verified to Integrated only after focused, consumer, full-regression, lint, structural, and documentation evidence passes.
- Keep Gate 6 as the sole `Specified - Next` product gate throughout this maturity-reconciliation task.

Rationale:

Integration maturity requires evidence that real collaborators are connected, not artificial new code or an authority shortcut. The explicit fixture transition models the human or governed approval boundary while allowing the full planning-to-verified-run lifecycle to be tested without mutating the actual repository.

Consequences:

- Gate 0 may be promoted without a new user interface because Operational entry is already owned by Gate 5.
- A passing characterization test can justify a documentation-only maturity promotion when it proves that existing downstream integrations satisfy every Gate 0 contract.
- Any uncovered defect remains subject to the active task, RED classification, and minimum-change rules.

### 2026-07-15: Expose The Integrated Read-Only Run Through A Minimal Local CLI

Status: Accepted Decision

Context:

- Delivery Gates 1 through 4 provide repository-derived approval, governed read-only Tool execution, complete evidence persistence, independent verification, and durable RunRecord replay, but no supported entry point connects them for a user.
- Gate 5 requires explicit project, task, target, expected digest, evidence-root, and RunRecord-root inputs plus stable exit codes and documented recovery.
- The future interface suite belongs to Gate 12; the first operational command should therefore remain deliberately small and dependency-free.

Decision:

- Add the Gradle `application` entry point `com.enhancer.cli.EnhancerCli` with two commands: `run` and `replay`.
- Make `run` require six named inputs and match the explicit task identity against the `ApprovedTask` derived from the project's active `CURRENT_TASK.md`.
- Wire only the existing `read-file` flow through `ExecutionPolicy`, `ToolExecutor`, `AgentRunController`, `DeterministicReadFileVerifier`, and `AgentRunFinalizer`.
- Use fixed documented defaults of the existing 64 MiB evidence ceiling, a five-second Tool timeout, a five-iteration loop ceiling, a three-transition stagnation threshold, and a 30-day retention declaration without automatic cleanup.
- Define stable process exit codes for completion, usage/configuration failure, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure.
- Bound CLI diagnostics, print no complete target content, and report the RunRecord root and opaque reference for replay.
- Make `replay` accept only an explicit RunRecord root and opaque reference and print typed bounded metadata from the integrity-checked store.

Rationale:

This is the smallest supported control surface that proves the integrated vertical slice against a real repository without inventing a second execution path. Explicit arguments keep authority and expected results inspectable, while stable exit codes and replay make the command automatable and diagnosable.

Consequences:

- Successful Gate 5 evidence may promote the first read-only run to Operational, but it does not release a distribution or make the broader Agent Runtime Operational.
- The command does not infer task approval, expected content, storage roots, or target paths from ambient state.
- Interactive prompts, configuration discovery, shell/Git/network/LLM capabilities, and polished multi-interface behavior remain deferred.
- Gate 6 becomes the next specified capability only after temporary-project and actual-repository run/replay evidence passes.

### 2026-07-15: Harden Integrated Boundaries Before The First Operational CLI

Status: Accepted Decision

Context:

- A Gradle 9 compatibility review found that the build relies on Gradle's deprecated automatic JUnit Platform launcher injection.
- Boundary testing found that one interruption-ignoring timed-out Tool can occupy the shared worker, duration values can pass policy validation but fail execution or audit conversion, and persisted envelope metadata is outside the integrity digest.
- RunRecord UTF-8 writing can replace malformed Unicode, startup context loading does not enforce real-root containment or size bounds, and the standard test task depends on a user-profile temporary directory.
- These defects are in Integrated Gate 1 through 4 foundations that Gate 5 would expose through a supported command.

Decision:

- Complete a bounded foundation-hardening task before Gate 5 without changing capability maturity or delivery order.
- Declare the JUnit Platform launcher explicitly and configure a build-local default test temporary directory.
- Isolate Tool invocations so a timed-out invocation cannot starve later work, while retaining bounded structured results and executor shutdown ownership.
- Enforce a timeout domain that is representable in both nanoseconds for execution and positive milliseconds for policy audit records.
- Integrity-protect the complete versioned envelope metadata and payload for Evidence and RunRecords, and use strict UTF-8 encoding for persisted RunRecord strings.
- Apply the Tool boundary's real-path containment, strict decoding, and bounded-read principles to required startup documents.
- Preserve the intentional no-persistence `ReadFileTool` failure for truncated output but classify it as an execution/evidence-capability failure rather than invalid caller input.
- Keep Gate 5 as the sole `Specified - Next` capability and perform no Wrapper major-version upgrade in this task.

Rationale:

The first supported CLI must not operationalize known starvation, audit-integrity, data-preservation, path-containment, or build-compatibility defects. These changes strengthen existing contracts without adding a new product capability or broadening authority.

Consequences:

- Pre-hardening local binary Evidence and RunRecord artifacts are not promised backward compatibility; no released storage format exists yet.
- Timeout validation becomes intentionally stricter at policy construction.
- Startup context loading can reject oversized, malformed, or out-of-root required documents before planning begins.
- Gate 5 remains the next capability task after fresh hardening verification and document synchronization.

### 2026-07-14: Translate External Orchestration Patterns Into Gate-Owned Enhancer Contracts

Status: Accepted Decision

Context:

- The user requested that useful Multi-Agent orchestration lessons from Archon and meta-harness be preserved for future Enhancer implementation.
- Archon demonstrates an operational control plane with dynamic capability rosters, centralized execution profiles, dependency-aware work, heartbeats, interventions, and resumable sessions, but its provider CLI subprocesses, shared working directory, file polling, and quality-gradient completion model do not satisfy Enhancer authority or evidence rules.
- meta-harness provides a portable pattern-selection ladder, deterministic handoffs, Producer-Reviewer and supervisor guidance, normal/failure scenarios, and removable provider-specific logic, but it is a design-time meta-skill rather than a runtime with scheduling, authorization, idempotency, replay, or evidence integrity.
- The reviewed reference snapshots are Archon commit `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16` under MIT and meta-harness commit `ccab9a677878f72b3316de464c99b36f56a3f2e7` under Apache-2.0.

Decision:

- Treat both repositories as pinned reference implementations, never as hidden runtime, governance, Skill-layout, prompt, or file-format dependencies.
- Select the smallest orchestration topology that satisfies the work: one worker first; then a sequential pipeline; then Producer-Reviewer; then bounded fan-out/fan-in; and only then expert routing, supervisor allocation, or a hierarchy no deeper than one subordinate coordination layer.
- Require every parallel branch to consume the same immutable `WorkspaceSnapshot` and approved task revision. Branch ownership, expected output, synthesis criteria, budget, and conflict policy are fixed before dispatch.
- Carry handoffs through versioned Message Bus envelopes with run, task, message, correlation, causation, producer, schema, authorization, input-snapshot, and artifact/evidence-reference identity. Free-form Markdown may be an inspectable projection but cannot be the authoritative queue or control signal.
- Keep one Kernel-owned coordinator responsible for terminal task and run state. Workers may propose progress, artifacts, or follow-up work; they cannot approve tasks, broaden Tool or model authority, create final completion, or verify their own output.
- Make dependency readiness, cycle rejection, leases, duplicate suppression, cancellation, retry, timeout, dead-letter, replay, pause, resume, reassignment, and recovery Scheduler or Message Bus responsibilities rather than prompt conventions.
- Represent execution profiles as provider-neutral capability, model class, reasoning budget, context budget, Tool scope, data classification, and locality requirements. Provider adapters translate an approved profile only after Kernel policy intersects it with task, Skill, and Tool authority.
- Represent pause, resume, cancel, inject-proposal, reprioritize, reassign, mediate, and scale decisions as typed, authenticated, auditable control commands. A control command cannot silently create accepted work or external-action authority.
- Treat heartbeats, quality gradients, confidence, prompt adherence, and other worker telemetry as diagnostic observations only. They may trigger inspection or a proposal, but never lifecycle promotion, verification, completion, or release.
- Preserve independent verification and durable RunRecord finalization outside the producing worker. Producer-Reviewer revision loops are bounded and remain distinct from the independent verifier required for completion.
- Keep model-specific retries, prompt heuristics, CLI flags, and provider recovery logic behind removable adapters or reference sections so deleting one provider does not rewrite the orchestration contract.

Rationale:

The useful common pattern is observable, resumable, role-aware coordination with explicit handoffs and bounded parallelism. Enhancer already has stronger authority, evidence, verification, and replay requirements than either reference. Translating the patterns into existing Workspace, Event/Message Bus, Scheduler, Model Gateway, Skill, Verification, and RunRecord boundaries preserves those strengths while avoiding provider and storage coupling.

Consequences:

- Gate 6 owns immutable shared input snapshots and provenance; Gate 7 owns typed handoffs and control/event delivery; Gate 8 owns the durable dependency graph, leases, sequential worker, Scheduler, and recovery.
- Gate 9 owns provider-neutral execution profiles and model budgets; Gate 10 owns validated workflow-pattern and Skill selection; Gate 12 owns user-facing run controls; Gate 13 owns dynamic capability rosters, bounded fan-out/fan-in, Producer-Reviewer roles, supervisor allocation, and background execution.
- Gate 15 alone may consume autonomous experiment-ledger patterns, and only after approved snapshots, fixed evaluation, budgets, independent verification, and rollback are Operational.
- Direct peer calls, prompt-only coordination, shared-worktree parallel mutation without isolation, silent ring-buffer loss, self-reported completion, optional verification, unlimited timeouts, and file polling as the core bus are rejected.
- This decision changes documentation only. It does not promote Workspace, Message Bus, Agent Runtime, Scheduler, Model Gateway, Skill Engine, Multi-Agent, background execution, or self-improvement maturity, and it does not displace Delivery Gate 5.
- No external code or templates are copied by this decision. Any later copying must preserve applicable license, attribution, and modification notices.

### 2026-07-14: Bind Run Records To The Policy Used During Execution

Status: Accepted Decision

Context:

- Gate 4 records an immutable execution-policy snapshot, but the finalizer currently accepts an `ExecutionPolicy` separately after the worker run.
- A caller could supply a different still-allowing root, timeout, or size limit and produce an internally valid record that does not describe the actual execution.
- The public `RunRecord` constructor also permits some verification and stop-reason combinations that the governed Agent run path cannot produce.

Decision:

- Preserve the exact `ExecutionPolicy` used by `AgentRunController` in `AgentRunResult`.
- Remove the replaceable policy argument from finalization and derive the persisted `PolicyDecision` from the worker-bound policy.
- Make `RunRecord` enforce the Gate 3 and Gate 4 lifecycle: worker completion is impossible, verification is performed only after successful verification-wait, and non-verification terminal or bounded stops retain failed Tool output with verification Not Performed.

Rationale:

RunRecords are audit evidence. Their policy snapshot and lifecycle must be bound to the executed run rather than trusted as a later caller assertion. Enforcing these relationships in immutable contracts prevents Gate 5 or another future entry point from constructing replayable but historically false records.

Consequences:

- Finalization cannot substitute a different policy after Tool execution.
- Invalid persisted records are rejected during both direct construction and replay decoding.
- The change strengthens existing Gate 4 semantics without adding Tool authority, CLI behavior, LLM calls, or multi-agent execution.
- Gate 5 consumes one policy-bound worker result and no longer needs to repeat the policy at finalization.

### 2026-07-14: Complete Agent Runs Only Through External Evidence Verification And Durable Records

Status: Accepted Decision

Context:

- Gate 3 intentionally stops successful Tool output at `AWAITING_VERIFICATION` and does not preserve an externally inspectable completion record.
- A worker summary, exit status, or self-reported success cannot independently prove that the expected result was produced.
- Truncated evidence must be resolved and integrity-checked before completion, while missing evidence must remain distinguishable from proven mismatch or corruption.
- Failed, stagnated, and iteration-limited runs also need durable diagnostic history even though they never enter verification.

Decision:

- Introduce typed Verification statuses: Verified, Rejected, Unverified, and Not Performed, each constrained by a structured reason code.
- Bind every `VerificationRequest` to the approved task, executed Tool request, Tool result, and an expected SHA-256 content digest supplied outside the worker.
- Implement the first `IndependentVerifier` as a deterministic read-file verifier that recomputes complete-content identity and resolves truncated evidence through the existing `EvidenceStore`.
- Preserve the executed request in terminal Agent state instead of reconstructing it from progress hashes or diagnostic prose.
- Permit only the sequential finalization boundary to create `COMPLETED`, and only after a Verified decision.
- Persist a typed RunRecord before returning completed finalization. The record includes inputs, policy snapshot and decision, Tool result and evidence, verification, iterations, and worker/final stop reasons.
- Store RunRecords as versioned binary payloads in atomically published SHA-256 envelopes and support restart-safe replay.
- Record worker failure, stagnation, and iteration exhaustion with verification Not Performed rather than fabricating a verification attempt.

Rationale:

This is the smallest provider-neutral boundary that turns worker output into independently checked, replayable execution history. Digest comparison avoids trusting prose, reuse of `EvidenceStore` keeps complete-output integrity in one place, and persist-before-return prevents an in-memory completion from being reported without durable audit evidence.

Consequences:

- Missing evidence is Unverified; corrupted or mismatched evidence is Rejected; neither can complete a run.
- A RunRecord persistence failure prevents the finalizer from returning completion.
- The initial RunRecord format is local, bounded, versioned, and integrity-checked but not encrypted, signed, remotely replicated, or automatically deleted.
- Gate 5 can consume the finalizer and RunRecord reference through a supported CLI without changing verification authority.
- LLM verification, human review adapters, parallel reviewers, Git mutation, and distributed storage remain future work.

### 2026-07-14: Adopt V1-V3 Evolution And A Provenance-Preserving Project Brain

Status: Accepted Decision

Context:

- Repository Markdown and Git provide durable human-readable memory, but they do not directly represent the relationships among decisions, architecture, dependencies, tasks, executions, tests, bugs, commits, issues, and pull requests.
- The long-term product needs Cursor-level productivity, an Agent development platform, and finally an AI Operating System without confusing those maturity levels.
- Agents, Skills, workflows, and models have distinct responsibilities that must remain separable for plugins, marketplaces, security review, and routing.
- A one-sentence user intent should reduce human orchestration, but hidden Git publication, merge, deployment, or permission escalation would violate the Constitution.

Decision:

- Define three product milestones: **V1 AI Development Experience**, **V2 AI Development Platform**, and **V3 AI Operating System**.
- V1 provides Cursor-level productivity through CLI/editor/Desktop surfaces and Workspace awareness; it does not redefine Enhancer as an IDE or Cursor clone.
- V2 provides durable workflows, Skills, Memory, Agent Runtime, MCP, model routing, plugins, marketplace foundations, and self-hosting development support.
- V3 provides the AI Kernel, Project Brain knowledge graphs, multi-agent operating model, hybrid privacy-aware model routing, scheduler, plugin ecosystem, and governed synchronization/self-improvement.
- Define AI Kernel responsibilities as Agent lifecycle, memory/context allocation, resource budgets, locks and leases, scheduling, cancellation, policy, event routing, recovery, and audit state.
- Treat Git and canonical repository documents as authoritative records. Project Brain graphs are provenance-bearing, freshness-aware, rebuildable projections over documents, code, Git, RunRecords, issues, PRs, tests, and external metadata; they do not silently replace their sources.
- Project Brain includes Decision, Architecture, Dependency, Task, and Execution graphs, with explicit links to code, tests, bugs, commits, issues, and pull requests.
- Distinguish extension types: an Agent plugin supplies a role/capability worker, a Skill supplies a validated workflow, a Tool performs an external capability, and a Workflow composes events, Skills, Agents, Tools, verification, and approval gates.
- Add a privacy-aware Model Router that selects local or remote providers from task capability, data classification, policy, cost, latency, and availability. Sensitive content defaults to an approved local route and cannot be sent remotely without policy authority.
- The one-sentence user experience compiles intent into an inspectable goal, dependency plan, authorization scope, execution graph, verification, and audit trail. External or destructive workflow stages still require explicit or pre-authorized policy approval.

Rationale:

The differentiator is the Kernel below IDEs, not another editor shell. A graph projection enables impact reasoning while preserving the repository as recoverable memory. Separating Agents, Skills, Tools, workflows, and models prevents marketplace extensions or model output from silently gaining authority.

Consequences:

- VS Code, IntelliJ, Desktop, web, and CLI can share the same Kernel and Project Brain.
- V1, V2, and V3 are product milestones, not claims about current implementation maturity.
- Workflow automation may cover issue, branch, development, test, review, commit, push, PR, and merge, but each externally visible or destructive transition must satisfy the approval policy recorded in the run.
- Marketplace packages require provenance, signatures or integrity evidence appropriate to risk, compatibility metadata, permissions, isolation, review, disable, removal, and rollback.
- Knowledge Graph storage technology remains undecided until the Project Brain delivery gate; the contract is graph semantics and provenance, not a specific graph database.
- Local Llama or other on-device models and remote Claude, GPT, Gemini, DeepSeek, or future providers remain adapters behind the same Model Gateway.

### 2026-07-14: Make Enhancer An Event-Driven Interoperable AI Operating Platform

Status: Accepted Decision

Context:

- The current foundation proves repository context, planning, governed Tool execution, evidence, and bounded Agent Loop transitions, but it does not yet provide an operating substrate for long-lived or multi-role work.
- A linear Chat -> Tool -> Stop design cannot support Planner -> Coder -> Reviewer -> Tester pipelines, resumable scheduling, external clients, or independent evolution of runtime components.
- Workspace awareness, reusable Skills, MCP interoperability, and model routing have become core platform requirements rather than optional editor features.
- Event Bus and IPC Message Bus responsibilities overlap unless their semantic and transport layers are explicitly separated.

Decision:

- The final product target is **Enhancer OS**, composed of Desktop, CLI, API, Workspace, Project Brain, Memory, MCP, Agent Runtime, Event Bus, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.
- Use one typed messaging architecture: the Event Bus defines domain events and subscriptions; the Message Bus provides envelopes, queues, delivery, replay, and backpressure; IPC is a transport adapter for the same envelopes across process boundaries.
- Runtime Agents MUST communicate through the bus once the runtime boundary exists. Direct Planner-to-Coder or Coder-to-Reviewer calls are not the target architecture.
- Build the Agent Runtime as a persisted state machine around Goal -> Planner -> Executor -> Memory -> Reflection -> Retry -> Done, with bounded budgets and explicit stop reasons.
- Add a first-class Workspace layer for project files, active and selected context, Git state, diagnostics, terminal-session metadata, and later editor state. Project Brain combines governed repository memory with Workspace observations; it does not replace either source.
- Treat Skills as validated, progressively loaded, composable workflows that can form chains such as Spring -> Java -> Database -> Test while preserving least-privilege Tool scope.
- Promote MCP to a core interoperability layer with both server and client boundaries so Claude Code, Cursor, VS Code, and other model clients can share governed Tools, resources, and memory.
- Keep the immediate Gate 4 verification and RunRecord dependency order. Introduce Workspace, messaging, runtime, MCP/model gateway, Skill, plugin, interface, multi-agent, sync, and self-improvement capabilities only through their delivery gates.

Rationale:

An AI operating system needs durable state, shared context, asynchronous coordination, reusable behavior, and interoperable capability exposure. Separating semantic events from transport avoids duplicate buses while allowing the first implementation to remain in-process and later gain IPC or durable queues without rewriting Agent contracts.

Consequences:

- The current sequential Agent controller remains a verified bootstrap slice and will later become a runtime worker behind bus contracts.
- Event envelopes require identity, causation, correlation, schema version, provenance, authorization context, and idempotency semantics before asynchronous execution.
- Workspace is a governed snapshot boundary, not unrestricted editor or terminal access.
- MCP, plugins, Skills, and models cannot bypass Tool policy, evidence, independent verification, RunRecord, or user approval.
- Multi-agent execution follows a stable single-agent runtime and durable messaging; it is not introduced as direct Agent-to-Agent calls.
- Cloud Sync remains opt-in and must define encryption, conflict resolution, ownership, and secret exclusion before implementation.
- The owner's qualitative assessment that roughly 20-25% of the intended foundation is established is recorded as planning context, not verified capability maturity or release progress.

### 2026-07-14: Harden Agent Runs With Repository Approval And Semantic Progress

Status: Accepted Decision

Context:

- Gate 3 integrates real Tool results, but `approvedTask` is only a non-blank string and the integration test does not derive it from repository context.
- Progress currently includes an opaque evidence reference, so identical content stored under a new UUID can appear to be progress and evade stagnation detection.
- Tool failures expose only success or failure; a real retry policy cannot distinguish timeout, cancellation, denial, invalid requests, or temporary failures without parsing prose.
- The public `AgentRunState` record constructor accepts caller-supplied progress keys and structurally valid states outside the governed transition path.

Decision:

- Introduce `ApprovedTask` and `ApprovedTaskReader`. The reader consumes `CURRENT_TASK.md` from `ProjectContext` and requires explicit `Task ID`, `Status: In Progress`, `Task`, `Approval`, and `Allowed Tools` sections.
- Bind every initial `ToolRequest` to the approved task's Tool-name scope before execution policy is applied. Repository approval evidence is explicit provenance, not a cryptographic signature or permission escalation.
- Add structured `ToolFailureCode` to every failed `ToolResult`; successful results carry no failure code. The executor assigns codes at its policy and execution boundaries.
- Provide a standard failure classifier that retries only explicitly temporary failures and timeouts; all other codes are terminal by default.
- Add an optional SHA-256 content digest to `VerificationEvidence.capture` and use it for semantic progress. Opaque storage references and human-readable summaries do not define progress identity.
- Replace the public Agent run record constructor with a final immutable class whose constructor is private. Only the initial approval factory and package-owned controller transitions can construct states.

Rationale:

These changes turn the RED scenarios into production invariants instead of test conventions. Approval, failure semantics, and progress equality become structured and deterministic, while state transitions remain controlled by the orchestration boundary.

Consequences:

- A repository task without an active status, explicit approval evidence, or Tool scope cannot start an Agent run.
- Execution policy remains an additional deny-over-allow boundary; repository approval cannot broaden it.
- Identical evidence content remains identical progress even when persisted at different references.
- Retry behavior no longer depends on diagnostic message text.
- Gate 4 can consume structured approval, failure, and evidence identities in its verifier and RunRecord.
- Signature-backed approval, argument-level authorization, identity federation, mutation Tools, independent verification, and RunRecord persistence remain deferred.

### 2026-07-14: Stop Tool Success At The Independent Verification Boundary

Status: Accepted Decision

Context:

- Delivery Gate 3 must connect a real `ToolResult` to the bounded Agent Loop without implementing the Gate 4 independent verifier.
- Treating Tool success as task completion would let worker output bypass the accepted rule that completion requires successful independent verification.
- Retry behavior must be deterministic without parsing diagnostic prose or allowing a Tool to grant itself execution authority.

Decision:

- Add immutable `AgentRunState` with an externally approved task, a caller-supplied pending `ToolRequest`, the last `ToolResult`, loop status, and deterministic progress key.
- Add `AWAITING_VERIFICATION` as an explicit terminal loop state and stop reason; a successful Tool result reaches this boundary but cannot reach `COMPLETED` in Gate 3.
- Add `AgentRunController` as the orchestration owner. It consumes an existing `ToolExecutor`, immutable `ExecutionPolicy`, and external `ToolFailureClassifier`; it does not implement Tools, create requests, expand policy, or approve tasks.
- Keep terminal failures as `FAILED`; retain the same pending request only for failures classified `RETRYABLE`.
- Derive progress keys from canonical Tool request/result content so identical retry results activate the existing maximum-iteration and stagnation rules.
- Reuse the existing bounded loop engine for both the small `AgentLoopState` contract and the richer Agent run state without weakening the 20/3 defaults or precedence rules.

Rationale:

The verification-wait boundary preserves separation of duties while still proving the real Context -> approved task -> Tool -> evidence -> loop transition. External retry classification avoids unreliable message parsing and prevents a Tool from deciding its own authority or finality.

Consequences:

- Gate 3 can stop successfully executed work without claiming it is verified or completed.
- Gate 4 becomes the only component allowed to turn an independently accepted result into `COMPLETED`.
- Repeated identical retryable failures remain bounded and observable as `STAGNATED`.
- A denied Tool remains uninvoked even if its implementation would attempt mutation; the controller cannot alter the caller's allow/deny policy.
- LLM decisions, Tool mutation, Git or network authorization, independent verification, RunRecord persistence, CLI wiring, and multi-agent routing remain deferred.

### 2026-07-14: Persist Complete Evidence As Atomic Integrity-Checked Envelopes

Status: Accepted Decision

Context:

- `VerificationEvidence` requires a complete-output reference when its 4096-character tail is truncated, but no current component makes that reference real.
- Gate 2 must provide durable resolution and corruption detection without broadening Tool authority or fabricating references.
- Separate payload and metadata files would make atomic publication across both files difficult in the first filesystem implementation.
- Automatic retention cleanup would delete data and is not required to prove the Gate 2 exit criteria.

Decision:

- Add an `EvidenceStore` boundary with a filesystem implementation that generates UUID run and evidence identities.
- Store metadata and UTF-8 payload together in one versioned binary envelope.
- Publish evidence by atomic move from a temporary file in the final run directory; do not fall back to a non-atomic move.
- Record creation time, UTF-8 byte length, and SHA-256 digest and validate all of them during resolution.
- Use opaque `evidence/<run-id>/<evidence-id>` references and reject malformed, missing, oversized, or corrupted artifacts.
- Add an explicit maximum-content and retention-duration policy, but perform no automatic cleanup in Gate 2.
- Connect a persistence-enabled `ReadFileTool` through `EvidenceRecorder`; the request correlation identity is a run identity previously created by the store.

Rationale:

A single atomic envelope is the smallest durable format that keeps reference metadata and content consistent. Digest, length, strict decoding, bounded reads, and reference containment make evidence failures observable before later verification consumes them.

Consequences:

- Truncated Tool output can carry a real reference that resolves after the Tool call.
- Short output remains in memory and does not create unnecessary evidence artifacts.
- Gate 2 detects accidental or unauthorized artifact modification but does not provide encryption, signatures, or external tamper-proof storage.
- Evidence deletion, compaction, migration, distributed storage, Agent Loop integration, independent verification, and RunRecord remain deferred.

### 2026-07-14: Restore Self-Hosting Context And Roadmap Compatibility Before Gate 2

Status: Accepted Decision

Context:

- The Planner still recognizes only the retired `## Phase ...` and `Status: Ready` grammar.
- The canonical Roadmap now uses `## Delivery Gate ...` and `Status: Specified - Next`, so the Planner cannot propose the actual next Enhancer task.
- The executable Context Reader loads only eight root documents even though repository startup governance requires `.ai/` to be read first.
- Existing tests use the retired Roadmap grammar and synthetic root-only context, so they do not detect either self-hosting regression.

Decision:

- Restore the self-hosting planning path before beginning Delivery Gate 2.
- Make the seven governed `.ai/` Markdown files explicit required context inputs before the eight canonical root documents.
- Replace the retired phase parser with the accepted Delivery Gate maturity grammar and select the first gate marked `Specified - Next`.
- Map the selected gate's required-capability or scope bullets into proposal scope and its exit criteria into proposal acceptance criteria.
- Add a regression test that reads the actual Enhancer `ROADMAP.md`, plus context-order tests that prove `.ai/` is loaded first.

Rationale:

Repository-backed memory and deterministic next-task proposal are core self-hosting promises. Later Tool and evidence work must not proceed while the front of that flow is known to reject the repository's own source of truth.

Consequences:

- Delivery Gate 2 remains next but is temporarily blocked by this foundation recovery task.
- The Planner intentionally follows the current canonical Delivery Gate grammar rather than retaining undocumented compatibility with the retired Phase/Ready format.
- Adding or removing governed `.ai/` bootstrap documents requires synchronizing the explicit executable context list.
- Dynamic Markdown interpretation, proposal ranking, LLM planning, and automatic task acceptance remain out of scope.

### 2026-07-14: Implement Gate 1 As A Bounded Read-Only Tool Boundary

Status: Accepted Decision

Context:

- Delivery Gate 0 verifies Tool result and evidence invariants but executes no Tool.
- Gate 1 must produce one real `ToolResult` without introducing evidence persistence, shell mutation, Git writes, network access, or LLM behavior.
- A path-prefix check alone is insufficient because traversal and symbolic links can escape an approved project root.
- Truncated evidence cannot truthfully reference complete output until Gate 2 provides an EvidenceStore.

Decision:

- Introduce immutable `ToolRequest`, `ExecutionPolicy`, and a minimal `Tool` interface under `com.enhancer.tool`.
- Use an in-process `ToolExecutor` registry with unique names and structured failure conversion.
- Make deny override allow and enforce cancellation before and after invocation plus a positive execution timeout.
- Implement `ReadFileTool` as the only production Tool in Gate 1.
- Require relative paths, real-path containment, regular files, strict UTF-8, and a policy size ceiling no greater than the existing 4096-character evidence boundary.
- Keep deterministic fake Tools in tests as the immediate consumer of the generic executor contract.

Rationale:

This is the smallest real external-boundary slice that exercises policy, execution, result, and evidence together. Strict path and output limits prevent the read-only first Tool from creating hidden authority or unverifiable truncated evidence.

Consequences:

- Allowed temporary project files can produce real successful `ToolResult` values.
- Denial, malformed input, traversal, missing files, invalid UTF-8, cancellation, timeout, and Tool exceptions remain observable failure results.
- The first size ceiling is intentionally conservative and may be raised only after Gate 2 persists full evidence.
- Agent Loop integration, evidence persistence, independent verification, CLI, shell, Git, network, and LLM behavior remain deferred.

### 2026-07-14: Promote Foundation Contracts Through Executable Vertical Slices

Status: Accepted Decision

Context:

- The repository has 21 production Java files and approximately 479 production lines centered on contracts and deterministic control rules.
- Context, planning, loop termination, and Tool evidence invariants are tested, but there is no application entry point, concrete Tool execution, Agent-Loop/Tool integration, evidence persistence, LLM boundary, or end-to-end runtime.
- The existing roadmap uses `Implemented` for narrow slices, which can be mistaken for operational product completeness.
- Continuing to add isolated contracts would increase skeleton breadth without proving that Enhancer can execute a useful governed workflow.

Decision:

- Use capability maturity states: Specified, Contract Verified, Integrated, Operational, and Released.
- Replace ambiguous standalone `Implemented` roadmap labels with the most precise verified maturity.
- Promote capabilities through executable vertical slices with explicit integration and operational exit gates.
- Supersede the sequential independent verifier as a standalone next task. First implement the bounded Tool execution boundary and evidence persistence, then integrate the Agent Loop and sequential verifier in the same E2E delivery track.
- Require every new foundation contract to name its current or immediately following integration consumer.

Rationale:

Control-plane safety contracts were necessary before Tool or LLM execution, but their value is realized only when they participate in an observable, recoverable run. Maturity gates prevent focused unit tests from being reported as product readiness and make the path from foundation to usable system explicit.

Consequences:

- The next product task is the first Tool Execution Boundary slice, not an isolated verifier record.
- The first operational milestone must run Context → Tool → Evidence → Verification → Stop → Run Record through a supported entry point.
- Independent verification remains mandatory but moves after real ToolResult production and evidence persistence.
- Skill, LLM, MCP, plugin, multi-agent, and self-improvement work remains gated behind the executable single-agent path.
- Roadmap, architecture guides, state, and handoff documents must use the new maturity vocabulary.

### 2026-07-14: Recover Existing Git Metadata Instead Of Initializing New History

Status: Accepted Decision

Context:

- The active C:\Enhancer directory contains the project files but no .git metadata.
- PowerShell history shows that the GitHub repository was cloned into the nested C:\Enhancer\Enhancer path.
- Windows Recycle Bin metadata identifies the deleted nested clone and its Enhancer origin, main branch, and commit history.
- Running git init in the active directory would create unrelated history and lose the existing repository relationship.

Decision:

- Prefer the existing Recycle Bin metadata, but if it is unavailable before copying, create a temporary no-checkout clone from the verified Enhancer origin and copy only its .git metadata into C:\Enhancer.
- Preserve the available recovery source until validation completes and verify that non-.git working files are unchanged.
- Do not perform checkout, reset, clean, commit, push, or other worktree reconciliation as part of recovery.

Consequences:

- Existing Git history and origin can be restored without overwriting current work; a fresh clone may be used when the deleted metadata is no longer available.
- The restored status may show substantial local differences that must be reviewed separately.
- Recovery success does not authorize committing or pushing those differences.

### 2026-07-14: Restructure The Constitution As A Versioned Kernel

Status: Accepted Decision

Decision:

Enhancer will replace the repetitive Constitution 1.0.0 structure with Constitution 1.1.0 as a concise normative Kernel. The revised document defines normative language, document responsibilities, lifecycle states, authorization and safety boundaries, fresh verification evidence, self-hosting safeguards, failure recovery, and an explicit amendment process.

The 300-page Codex guidebook target applies to the complete repository documentation system, not to `CONSTITUTION.md` alone. Detailed procedures and component contracts remain in `AGENTS.md`, `.ai/`, `docs/`, RFCs, prompts, and Skills.

Rationale:

The previous Constitution repeated repository-memory and working-process rules while omitting the governance needed for safe self-hosting. A smaller but stronger Kernel reduces startup context, makes mandatory rules easier to locate, and prevents detailed implementation guidance from becoming constitutional debt.

Consequences:

- Constitution version increases from 1.0.0 to 1.1.0.
- `MUST`, `SHOULD`, and `MAY` receive explicit meanings.
- Proposal, Accepted Decision, Active Task, Implemented, Verified, Completed, and Released become distinct lifecycle states.
- Destructive and externally visible actions require explicit authority; secrets and external content receive safety rules.
- Constitution amendments require explicit user approval, Decision Log rationale, semantic versioning, and mirror review.
- Technology choices and detailed procedures remain changeable through Architecture, RFCs, and task documents rather than being frozen in the Kernel.

### 2026-07-14: Colocate Examples With Specifications And Tests

Status: Accepted Decision

Decision:

Enhancer will not maintain a standalone `examples/` directory. Conceptual examples belong in the `docs/` or RFC document that owns the contract, while executable examples belong in focused tests.

Rationale:

The standalone Agent Loop and Tool examples were already behind the implemented contracts. Colocation reduces duplicate descriptions, prevents conceptual samples from drifting away from code, and keeps the repository structure smaller.

Consequences:

- Remove `examples/agent-loop.md`, `examples/tool-example.md`, `examples/skill-example.md`, and the empty-directory marker.
- Do not treat examples as a separate source of truth.
- New conceptual examples must be updated with their owning specification.
- Observable executable behavior remains demonstrated and verified through tests.

### 2026-07-14: Adopt External Agent Harness Patterns Selectively

Status: Accepted Decision

Decision:

Enhancer will treat [MoAI-ADK](https://github.com/modu-ai/moai-adk) and similar agent harnesses as reference implementations rather than runtime dependencies. The first adopted pattern is an explicit terminal outcome for the deterministic Assisted Development Loop that composes repository context reading and task planning. Other useful patterns will be introduced only in the roadmap slice that owns them.

Rationale:

MoAI-ADK contains useful operational patterns, including explicit stop reasons, stagnation detection, bounded verification evidence, progressive Skill loading, artifact provenance, and approval-protected self-improvement. Importing its framework, provider-specific schemas, or Git workflow would duplicate Enhancer components and weaken the current document-driven approval model. Selective, provider-neutral adoption preserves the useful semantics without coupling the products.

Consequences:

- The current slice adds no MoAI package, command, generated configuration, or runtime dependency.
- The first Assisted Development Loop is a single read-and-plan pass with explicit outcomes and no repository mutation.
- Repeated-loop termination and stagnation are implemented in a separate Agent Loop slice.
- Verification evidence belongs to the Tool System; progressive loading belongs to the Skill System; provenance belongs to Plugin and template management.
- Token budgets follow LLM integration, while self-improvement requires snapshot, approval, verification, and rollback contracts before implementation.
- Claude-specific configuration, automatic commits or pushes, and parallel multi-agent orchestration are not adopted.

Adoption sequence:

1. Implement bounded repeated-loop termination and consecutive-state stagnation detection in the Agent Loop.
2. Define a bounded verification-evidence contract with Tool results.
3. Add a sequential independent verifier after the single-agent loop is stable.
4. Add progressive Skill loading while preserving the rule that Proposed catalog entries are not loadable.
5. Add artifact provenance when Plugin or template installation exists.
6. Add provider-neutral token and context budgets only after an LLM invocation boundary exists.
7. Implement self-improvement only after snapshot, human approval, independent verification, and rollback contracts exist.

The sequence does not conflict with `.ai/` rules: each item remains a small `CURRENT_TASK.md` scope, uses test-first verification for observable behavior, preserves proposal/decision/implemented-state separation, and cannot override the Constitution. The independent verifier begins as a sequential component, not multi-agent routing.

### 2026-07-14: Use Bounded Deterministic Agent Loop Termination

Status: Accepted Decision

Decision:

The first repeated Agent Loop uses immutable state transitions and explicit `COMPLETED`, `FAILED`, `MAX_ITERATIONS`, and `STAGNATED` stop reasons. The default ceiling is 20 executed steps. Stagnation means the progress key remains unchanged for 3 consecutive executed steps; both limits are constructor-configurable for focused tests and later runtime configuration.

Rationale:

Explicit bounded exits prevent silent infinite work before Tool or LLM execution is introduced. A caller-provided deterministic step keeps the loop independently testable and avoids premature Agent, Tool, prompt, or provider abstractions.

Consequences:

- Terminal status wins over stagnation after a step.
- Maximum iteration wins when its ceiling and the stagnation threshold coincide.
- Iteration count reports executed steps, including the terminal step.
- Maximum-iteration and stagnation results retain the latest running state for diagnosis.
- Tool execution, verification evidence, independent verification, LLM calls, and multi-agent routing remain out of scope.

### 2026-07-14: Bound Tool Verification Evidence

Status: Accepted Decision

Decision:

Every future Tool result will include structured verification evidence. The first contract limits summaries to 512 characters and retained output tails to 4096 characters. Output exceeding the tail limit must be marked truncated and include a non-blank reference to the complete output. The contract records original output length without implementing persistence.

Tool result status is explicit. An optional exit code supports process-like tools while allowing file or API tools to omit it. When present, success requires exit code zero and failure requires a non-zero code.

Rationale:

Unbounded command output would consume future Agent Context and obscure the most useful recent diagnostics. Keeping a bounded tail plus a reference preserves inspectability without introducing an LLM-specific token model, filesystem policy, or concrete Tool implementation.

Consequences:

- `VerificationEvidence` is mandatory on every `ToolResult`.
- Evidence summaries and output tails are bounded before they can enter Agent Context.
- Truncated output cannot be represented without a reference to the complete evidence.
- The contract does not claim that referenced evidence has been persisted or independently verified.
- Evidence storage, real Tool execution, Agent Loop integration, and the sequential independent verifier remain separate tasks.

### 2026-07-14: Use A Repository Gradle Wrapper

Status: Accepted Decision

Decision:

Enhancer will store a Gradle Wrapper in the repository and use Java 17 as the build runtime. A global Gradle installation is not required for normal project builds.

Rationale:

The Wrapper makes local development and future CI reproducible while matching the existing Java 17 Gradle build. It also removes reliance on user-specific cached Gradle paths.

Consequences:

- Developers run `gradlew.bat` on Windows or `./gradlew` on Unix-like systems.
- Wrapper scripts, properties, and the wrapper JAR are version-controlled.
- Java 17 remains an external prerequisite and is not committed to the repository.

### 2026-07-14: Adopt Verified Skill And Evidence Operating Rules

Status: Accepted Decision

Decision:

Enhancer will adopt repository-defined Skill authoring rules, memory distillation, test-first behavior for observable feature and bug-fix changes, and fresh verification evidence before completion claims. The initial Skill catalog remains explicitly proposed until corresponding `SKILL.md` files exist. Task cycles do not force commits; commits remain controlled by repository policy and user instruction.

Rationale:

These rules strengthen repeatability and verification while preserving Document Driven Development, least privilege, proposal-state separation, and the existing human approval boundary for Git operations.

Consequences:

- RFC-0002, RFC-0005, RFC-0007, RFC-0008, and RFC-0009 describe the accepted direction.
- `.ai/skill_rules.md` defines operational authoring constraints for future Skills.
- Proposed catalog entries cannot be treated as installed or available Skills.
- `allowed-tools` uses a small documented permission vocabulary rather than undeclared tool names.
- Actual Skill workflows, loading, and runtime enforcement remain future tasks.

### 2026-07-12: Start Planner With Deterministic Repository Proposals

Status: Accepted Decision

Decision:

The first Planner consumes `ProjectContext`, blocks proposals while `CURRENT_TASK.md` is active, and otherwise proposes the first ready roadmap phase. Its output has explicit `PROPOSAL` state and structured reason, scope, acceptance criteria, out-of-scope items, and risks.

Rationale:

This reaches the first self-hosting planning behavior without introducing an LLM, hidden chat context, document mutation, or premature ranking logic.

Consequences:

- Planner behavior is deterministic and unit-testable.
- A proposal cannot be confused with an accepted decision.
- Natural-language planning, proposal ranking, persistence, and execution remain future work.

### 2026-07-12: Implement Context Reader As A Single Java Module

Status: Accepted Decision

Decision:

The first Repository Context Reader is implemented in a single Gradle Java 17 project under `com.enhancer.context`. The required document order is represented by an enum, and the returned context uses immutable records.

Rationale:

This matches the existing architecture guide and provides a stable structured input for future planning without premature modules, Spring wiring, or domain abstractions.

Consequences:

- Required startup documents have one canonical code-level order.
- Missing documents fail with a checked exception that identifies the path.
- Future context sources can build on `ProjectContext` without changing this task into a full Context Builder.

### 2026-07-10: Manage Major Designs As RFCs

Status: Accepted Decision

Decision:

Enhancer will manage major design areas as RFC-style Markdown documents under `docs/rfcs/`, starting with RFC-0001 through RFC-0012.

Rationale:

The project is large enough that design topics need stable identifiers, reviewable history, and clear references. RFC-style documents make long-term architecture easier to maintain across multiple AI agents and sessions.

Consequences:

- Major architecture changes should add or update an RFC.
- Accepted direction should still be summarized in `DECISION_LOG.md`.
- RFC statuses distinguish Draft, Accepted, Implemented, and Superseded.
- The initial RFC track covers Constitution, AI Behavior, Prompt Contract, Context Builder, Planner, Tool, Skill, Memory, Multi-Agent, AI Operating System, Plugin SDK, and Self Improvement.

### 2026-07-10: Adopt Six-Month AI Development OS Roadmap

Status: Accepted Decision

Decision:

Enhancer will use a six-month open-source roadmap that evolves from Constitution, Architecture, Context, Agent Loop, and Tool toward Planner, Skill, Memory, Prompt Engine, MCP, Plugin, Git, Terminal, VSCode Extension, CLI, Dashboard, Multi-Agent, Scheduler, Background Agent, Self Improvement, Plugin SDK, and Open Source Release.

Rationale:

The target is larger than a 30-day prototype. A six-month roadmap gives the project realistic phases while keeping the 30-day self-hosting milestone as the first checkpoint.

Consequences:

- The 30-day goal remains the first self-hosting milestone.
- Long-term architecture is tracked separately from immediate implementation tasks.
- Work remains Sprint-based and document-driven.

### 2026-07-10: Read `.ai/` Before Every AI Work Session

Status: Accepted Decision

Decision:

Every AI agent should read the `.ai/` folder before starting work. The folder contains AI-only operational documents: `constitution.md`, `workflow.md`, `coding_rules.md`, `architecture.md`, `prompt_rules.md`, and `memory.md`.

Rationale:

The root documents are canonical, but `.ai/` gives agents a compact operational entry point. This allows the user to say "항상 .ai 폴더를 읽고 시작해" and have a consistent startup rule across Codex, Claude, GPT, and future Enhancer agents.

Consequences:

- `prompts/SESSION_START.md` includes `.ai/` in the required reading order.
- `AGENTS.md` requires agents to read `.ai/` before work.
- `.ai/` must mirror operational rules without replacing root canonical documents.

### 2026-07-10: Treat Docs As A Multi-Agent Prompt Book

Status: Accepted Decision

Decision:

Each major `docs/` chapter will end with a `Prompt Book` section containing separate prompts for Codex, Claude, and GPT.

Rationale:

Enhancer is developed by multiple AI agents with different strengths. A shared chapter can guide all agents, but each agent needs role-specific instructions to reduce ambiguity.

Consequences:

- Codex prompts focus on implementation and verification.
- Claude prompts focus on architecture and risk review.
- GPT prompts focus on explanation, task framing, and session continuity.
- New chapter documents should include all three prompt types.

### 2026-07-10: Use Explicit Session Resume Protocol

Status: Accepted Decision

Decision:

New ChatGPT sessions must be resumed by providing the core repository documents, because ChatGPT cannot automatically read the user's local Enhancer repository across sessions.

Rationale:

The project depends on repository-backed memory. Without an explicit resume protocol, a new session may rely on incomplete chat memory and drift away from the source of truth.

Consequences:

- `prompts/CHATGPT_SESSION_RESUME.md` defines the required upload/paste workflow.
- `SESSION_HANDOFF.md` must remain complete enough to recover short-term state.
- Documents override chat history when conflicts occur.
- The human owner controls final approval and push.

### 2026-07-10: Operate Enhancer As A Real Open Source Project

Status: Accepted Decision

Decision:

Enhancer will be operated as a real open source project, not as a one-off chat artifact or documentation-only repository. The project will include documentation, code, ADRs, tests, inline specification examples, and shared prompts for Codex, Claude, and GPT.

Rationale:

The expected scope is too large for a single chat session. A Git-managed, chapter-based, reviewed workflow allows the project to grow over months without losing architectural consistency.

Consequences:

- Work proceeds by Sprint and small tasks.
- Documentation and code evolve together.
- ADR review is required for meaningful design changes.
- AI roles are explicit: Codex implements; ChatGPT supports architecture, backend design, agent research, documentation, and review.
- Git repository documents remain the source of truth.

### 2026-07-10: Use Codex-Ready Chapter Documents

Status: Accepted Decision

Decision:

Enhancer will maintain feature documents under `docs/` as Codex-ready prompts. Each document should describe the goal, architecture, task boundary, tests, and out-of-scope items for a major platform capability.

Rationale:

The project is too large to drive from chat history. Chapter-based Markdown specifications allow Codex, Claude, GPT, and future Enhancer agents to implement one slice at a time from repository state.

Consequences:

- `docs/` is part of the operating system for development, not passive documentation.
- New major capabilities should receive a prompt-style specification before implementation.
- Implementation should proceed sprint by sprint rather than attempting a full Cursor-like platform at once.

### 2026-07-10: Use Document Driven Development

Status: Accepted Decision

Decision:

Enhancer will follow Document Driven Development. New work must move through Constitution, Architecture, ADR / Decision Log, Task, Implementation, Test, and Documentation Update before it is considered complete.

Rationale:

Enhancer depends on repository documents as durable memory. If code changes happen before architecture, decisions, and tasks are clarified, future AI sessions will lose the reason behind the implementation.

Consequences:

- Agents must not jump directly from idea to code.
- Important architectural changes must be recorded before or during implementation.
- `CURRENT_TASK.md` remains the scope boundary for implementation.
- Documentation update is part of Definition of Done.

### 2026-07-10: Build Enhancer As A Self-Hosting AI Development OS

Status: Accepted Decision

Decision:

Enhancer is not a Cursor clone. Enhancer is a self-hosting AI Development Operating System that should eventually read its own repository context, understand project state, propose the next task, and assist its own development.

Rationale:

The project goal is not to copy Cursor's interface or behavior. The goal is to build a durable framework where AI agents can resume work from repository state and eventually help operate the project themselves.

Consequences:

- The first product slice should prioritize context reading and task planning over UI polish.
- Repository documents are product inputs, not only project management artifacts.
- The 30-day milestone is for Enhancer to propose next tasks from repository context.

### 2026-07-10: Use Repository Documents As Durable Memory

Status: Accepted Decision

Decision:

Enhancer will use repository documents as the durable memory for future ChatGPT and Codex sessions.

Rationale:

Conversation memory is unreliable across sessions. Repository files can be read, reviewed, committed, and treated as the single source of truth.

Consequences:

- Agents must read the required documents at session start.
- `SESSION_HANDOFF.md` must be updated at session close.
- Proposals must not be treated as accepted decisions until recorded here.

## Proposals

- Define the product scope for Enhancer.
- Choose the initial implementation stack.
