# 2026-07-14: Adopt V1-V3 Evolution And A Provenance-Preserving Project Brain

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
