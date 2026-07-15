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
- Delivery Gate 5 provides the supported `EnhancerCli` `run` and `replay` commands with bounded output, stable exit codes, verified-only completion, and durable replay.
- Gate 0 integration proves planning -> explicit external activation -> verified Gate 5 execution and replay without automatic Proposal approval or a second production orchestrator.
- Delivery Gate 6 Workspace and Project Brain Foundation remains the sole next product gate. Its `WorkspaceSnapshot` and `ProjectBrainView` contracts are Contract Verified, the repository-memory path is Integrated through `RepositoryMemorySnapshotCollector`, and the production CLI `run` path composes the view Operationally with bounded snapshot identity, observation count, and memory freshness. The snapshot identity is not stored in the RunRecord; Gate 7 envelopes own cross-handoff identity.
- The Gate 6 graph projection contract is Contract Verified: five node kinds, six endpoint-checked edge kinds (JUSTIFIED_BY, SUPERSEDES, DEPENDS_ON, MODIFIES, VERIFIED_BY, RECORDED_AS) over the Decision/Architecture/Dependency/Task/Execution domains, element provenance with source, optional SHA-256 revision, explicit freshness, and derived rebuild status, keyed to one snapshot identity.
- The Gate 6 task impact query is Contract Verified: `TaskImpactQuery` answers the task-to-decision-to-code-to-test chain over one projected graph with snapshot-traceable immutable results and rebuild status derived from every traversed element; transitive DEPENDS_ON closure is deferred by decision. No producer projects real repository evidence yet. The next increment is the first graph producer or the next read-only source adapter; a Git adapter needs an explicit external-command authority decision.
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
