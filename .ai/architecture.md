# AI Architecture Notes

The canonical architecture document is `ARCHITECTURE.md`.

Current direction:

- Enhancer is a self-hosting AI Development Operating System.
- Repository Context Reader, deterministic Task Planner, Assisted Development Loop, repeated-loop safety, and Tool evidence are Contract Verified; the bounded read-only Tool, persisted evidence, and Tool-driven Agent Loop boundaries are Integrated but not Operational.
- Executable repository context loads the seven governed `.ai/` documents before the eight canonical root documents.
- The Planner follows the canonical Delivery Gate / Specified - Next grammar and is regression-tested against the actual Enhancer Roadmap.
- Capability maturity is Specified, Contract Verified, Integrated, Operational, or Released; do not use Implemented alone as a roadmap state.
- Delivery Gate 1 integrates ToolRequest, Tool, ExecutionPolicy, ToolExecutor, and one allowlisted read-only filesystem Tool.
- Delivery Gate 2 integrates atomic complete-evidence persistence, resolvable references, and integrity validation.
- Delivery Gate 3 connects approved work and real Tool results to the bounded Agent Loop and stops success at `AWAITING_VERIFICATION`.
- Gate 3 hardening derives structured task approval from active repository context, uses typed Tool failure codes and semantic evidence digests, and restricts state construction.
- Delivery Gate 4 integrates sequential independent read-file verification, verified-only completion, controller-bound execution policy, and atomic lifecycle-valid replayable RunRecords.
- The next architecture slice is Delivery Gate 5 First Operational CLI.
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
