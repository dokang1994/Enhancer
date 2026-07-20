# 2026-07-14: Make Enhancer An Event-Driven Interoperable AI Operating Platform

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
