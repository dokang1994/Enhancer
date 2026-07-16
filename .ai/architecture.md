# AI Architecture Notes

The canonical architecture document is `ARCHITECTURE.md`.

Current direction:

- Enhancer is a self-hosting AI Development Operating System.
- Delivery Gate 0 Context, planning, Assisted Loop, repeated-loop safety, ToolResult, VerificationEvidence, and governance contracts are Integrated through an authority-preserving lifecycle test; the bounded read-only Tool, persisted evidence, Tool-driven Agent Loop, verification, and RunRecord boundaries are Integrated, and their first supported read-only CLI composition is Operational.
- Executable repository context loads the seven governed `.ai/` documents before the eight canonical root documents.
- The Planner follows the canonical Delivery Gate / Specified - Next grammar and is regression-tested against the actual Enhancer Roadmap.
- Capability maturity is Specified, Contract Verified, Integrated, Operational, or Released; do not use Implemented alone as a roadmap state.
- Delivery Gate 1 integrates ToolRequest, Tool, ExecutionPolicy, ToolExecutor, and one allowlisted read-only filesystem Tool.
- Delivery Gate 2 integrates atomic complete-evidence persistence, resolvable references, and integrity validation.
- Delivery Gate 3 connects approved work and real Tool results to the bounded Agent Loop and stops success at `AWAITING_VERIFICATION`.
- Gate 3 hardening derives structured task approval from active repository context, uses typed Tool failure codes and semantic evidence digests, and restricts state construction.
- Delivery Gate 4 integrates sequential independent read-file verification, verified-only completion, controller-bound execution policy, and atomic lifecycle-valid replayable RunRecords.
- Pre-operational hardening isolates each Tool invocation, constrains representable timeouts, integrity-protects complete Evidence and RunRecord envelopes, strictly preserves UTF-8, bounds and contains startup context, and removes the Gradle 9 JUnit runtime deprecation.
- Evidence uses a truthful `EvidenceStoragePolicy` for the enforced per-artifact content bound; no time-based retention, expiry, or cleanup is claimed until separately authorized and designed.
- Delivery Gate 5 provides the supported `EnhancerCli` `run` and `replay` commands with bounded output, stable exit codes, verified-only completion, and durable replay.
- Gate 0 integration proves planning -> explicit external activation -> verified Gate 5 execution and replay without automatic Proposal approval or a second production orchestrator.
- Delivery Gate 6 Workspace and Project Brain Foundation is Integrated by the 2026-07-15 user-approved re-scope decision: diagnostics, terminal-session, and active/selected-file observation moved to Gate 12, which owns those capabilities. Its snapshot, view, graph, query, projector, and collector sub-capabilities are Integrated, and the production CLI `run` path composes the view and graph Operationally. The snapshot identity is not stored in the RunRecord; Gate 7 envelopes own cross-handoff identity.
- Delivery Gate 7 Event Bus and IPC Foundation is the sole `Specified - Next` product gate. Its reference-only `MessageEnvelope` and deterministic single-threaded `InProcessMessageBus` are Contract Verified under `com.enhancer.bus`: topic fan-out, single-consumer queues, idempotency, journal replay, failure isolation, bounded retry and dead-letter re-delivery, correlation cancellation, run-to-completion cascade ordering, and finite non-blocking pending-queue backpressure preserve the envelope and grant no authority. Replay-caused publications inherit non-journaling mode; refused overload consumes no delivery state and may be retried. The transport-neutral IPC interface is next; persistence, threading, production wiring, and adapters remain deferred.
- The graph projection contract types five node kinds and six endpoint-checked edge kinds (JUSTIFIED_BY, SUPERSEDES, DEPENDS_ON, MODIFIES, VERIFIED_BY, RECORDED_AS) over the Decision/Architecture/Dependency/Task/Execution domains with element provenance and derived rebuild status, keyed to one snapshot identity; `TaskImpactQuery` answers the task-to-decision-to-code-to-test chain with snapshot-traceable immutable results, and transitive DEPENDS_ON closure is deferred by decision.
- The Gate 6 run-evidence production path is Integrated: `RunEvidenceGraphProducer` projects evidence-only task/artifact/execution nodes and one RECORDED_AS edge from one snapshot and one task-matched stored run record, with observation states mapped one-to-one to element freshness; it never emits edges no evidence justifies.
- Workspace RunRecord observation is bounded to the 256 most recent filesystem records; full listing and point replay remain intact, selected corrupt records stay explicitly unavailable, and no retention deletion is implied.
- The Gate 6 production graph composition is Operational: the CLI `run` path observes a 256-record recent execution window, the run's target file, safe Git index/untracked/deleted metadata, and an explicit unavailable tracked-worktree diff; it merges accepted decisions and explicit justification edges and reports bounded graph/impact counts. Inputs are preflighted before execution, and post-persist reporting cannot change the durable run exit code.
- CLI graph inputs available before execution are preflighted before evidence creation; duplicate document/target paths collapse to one target-preferred artifact. Once a finalized RunRecord is persisted, reconstructible Project Brain reporting is degradable and cannot change its durable exit code.
- `GitWorkspaceCollector` holds the only external command authority, explicitly user-granted and decision-scoped: one fixed filter-free `ls-files` metadata invocation through a canonical absolute executable outside the observed project, no shell or inherited `GIT_*` overrides, project-confined discovery, watchdog timeout, and digest-only retention. Tracked worktree diff is explicitly UNAVAILABLE because verified Git comparison commands can execute required clean filters. The authority-boundary exit criterion is pinned by `WorkspaceAuthorityBoundaryIntegrationTest`; diagnostics/terminal/selection adapters remain owned by later gates.
- The long-term OS substrate is Workspace and Project Brain -> Event/Message Bus with IPC adapters -> Agent Runtime and Scheduler -> MCP/Model Gateway -> Skill Engine and Plugin Marketplace.
- Runtime Agents communicate by typed messages rather than direct Agent-to-Agent calls once the bus boundary exists.
- Agent orchestration escalates from one worker to sequential work and only later to Producer-Reviewer, bounded fan-out/fan-in, supervisor allocation, or shallow hierarchy; one Kernel coordinator owns terminal run state.
- Every worker handoff preserves the approved task revision, common immutable Workspace snapshot, authorization, correlation, causation, budgets, and artifact or evidence references. Heartbeats and quality signals are diagnostic only.
- MCP is a core server/client interoperability layer for governed Tools, resources, Workspace views, and memory.
- Product milestones are V1 AI Development Experience, V2 AI Development Platform, and V3 AI Operating System; they do not replace maturity gates.
- Product milestones describe outcomes; dependency-ordered Delivery Gates may build platform foundations before every V1 interface is polished.
- Self-hosting development of Enhancer and local or hybrid model execution are separate capabilities; neither implies the other.
- Git and canonical documents remain authoritative while Project Brain supplies rebuildable Decision, Architecture, Dependency, Task, and Execution graph projections.
- Agent plugins, Skills, Tools, and workflows are separate extension types; marketplace installation never grants execution authority.
- The future Model Router defaults sensitive code to approved local models and requires policy authority for remote transmission.
- New contracts must name their current or next-gate integration consumer.
- Archon `263cf365` and meta-harness `ccab9a6` are pinned orchestration design references only. Selected patterns are restated as provider-neutral Enhancer contracts in their owning roadmap gate; their packages, prompts, file queues, Skill layouts, and provider commands are not dependencies.
- Repository documents are product inputs.
