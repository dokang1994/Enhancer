# 11 - Architecture

## Codex Prompt

Use this as the expanded architecture guide. The canonical summary remains `ARCHITECTURE.md`.

## System View

```text
Repository Documents
↓
Context Builder
↓
Memory
↓
Planner
↓
Task Queue
↓
Prompt Builder
↓
Agent Loop
↓
Tool System
↓
Verification
↓
Documentation Update
```

The target runtime extends this bootstrap flow into `Workspace + Project Brain -> Event/Message Bus -> Agent Runtime + Scheduler -> MCP/Model Gateway -> Skill/Plugin runtime`. Event Bus owns domain semantics, Message Bus owns delivery, and IPC is a transport adapter. Runtime Agents communicate through messages rather than direct calls.

The product evolves from V1 development experience, through V2 Agent/workflow platform, to V3 AI Operating System. The V3 Kernel manages lifecycle, context, memory, resource budgets, locks, scheduling, cancellation, recovery, policy, verification, and audit state. Project Brain provides rebuildable Decision, Architecture, Dependency, Task, and Execution graph projections while Git and canonical documents remain authoritative.

## Current Capability Maturity

This chapter describes the architecture's shape: which boundaries exist, what each one covers, and what it must not do. It does not state how far any gate has progressed.

Current capability maturity is in `PROJECT_STATE.md` and the evidence behind it in `docs/verification-log.md`. Read them for the present position; this chapter went stale claiming Gate 7 was `Specified - Next` long after Gate 8 had taken that marker, which is why the claim no longer lives here.

The boundaries themselves: Gate 0 foundation contracts, the bounded read-only Tool, complete evidence, the Tool-driven loop, sequential verification, and RunRecords, composed by Delivery Gate 5 into one supported local read-only CLI scenario. Gate 6 adds Workspace and Project Brain, with the production `run` path composing its view and graph. Gate 7 adds a versioned envelope and deterministic in-process topic/queue delivery covering idempotency, replay, failure isolation, bounded retry and dead letters, cancellation, run-to-completion ordering, and finite non-blocking pending-queue backpressure, plus a transport-neutral IPC interface. Persistence, concrete IPC adapters, and production bus wiring are not part of it.

Capability maturity uses:

- Specified;
- Contract Verified;
- Integrated;
- Operational;
- Released.

Task completion and capability maturity are separate. A completed contract task does not imply an operational subsystem.

## Current Vertical Slice

The integrated Gate 1 path is:

```text
ToolRequest
â†“
ExecutionPolicy
â†“
ToolExecutor
â†“
ReadOnlyFileTool
â†“
ToolResult
â†“
VerificationEvidence
```

The integrated Gate 3 path extends persisted Tool results through Agent Loop state and stops successful execution at `AWAITING_VERIFICATION`. Gate 4 then validates complete evidence outside the worker, permits only Verified completion, and atomically persists a replayable RunRecord. Gate 5 adds the supported CLI.

The independent verifier consumes persisted evidence from the integrated Tool path. Missing evidence remains Unverified, invalid or mismatched evidence is Rejected, and neither state can produce completion.

## Module Direction

Future modules may be split when the codebase is large enough:

```text
enhancer-core
enhancer-context
enhancer-planner
enhancer-tool
enhancer-workspace
enhancer-events
enhancer-runtime
enhancer-skill
enhancer-memory
enhancer-mcp
enhancer-model-gateway
enhancer-plugin
enhancer-cli
enhancer-api
enhancer-desktop
enhancer-extension
enhancer-web
```

Do not create all modules at the start. Begin with a simple single Gradle project unless the codebase proves otherwise.

## Clean Architecture Boundary

Use boundaries pragmatically:

- Domain: task, context, plan, tool contract
- Application: use cases such as read project context
- Infrastructure: filesystem, Git, terminal, MCP
- Interface: CLI, VSCode, web

## First Package Direction

For the first slice:

```text
com.enhancer.context
```

Candidate classes:

```text
ProjectContextReader
ProjectDocument
RequiredProjectDocument
ProjectContext
MissingProjectDocumentException
```

## Design Constraints

- Minimal code first.
- No premature modules.
- No early DDD.
- Tests must describe behavior.
- Documents must explain decisions.
- New contracts must name their current or next-gate integration consumer.
- Unit tests can promote a capability to Contract Verified, not Integrated or Operational.
- Integration requires real collaborator wiring; Operational requires a supported entry point and end-to-end evidence.

## Prompt Book

### Codex Prompt

Use this architecture guide to implement only the active task. Start with a simple single Gradle project and do not split modules until the codebase justifies it.

### Claude Prompt

Review architecture changes for premature modularization, boundary violations, missing ADRs, and conflicts with `CONSTITUTION.md`.

### GPT Prompt

Explain the current architecture in plain language and identify the next architecture decision that must be made before coding.
