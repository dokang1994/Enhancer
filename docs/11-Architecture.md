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

## Current Capability Maturity

The repository is at **Contract Verified**, not Integrated or Operational, for its core Java slices. Existing code proves Context, Planner, loop termination, and Tool evidence invariants but does not yet execute one complete Agent run.

Capability maturity uses:

- Specified;
- Contract Verified;
- Integrated;
- Operational;
- Released.

Task completion and capability maturity are separate. A completed contract task does not imply an operational subsystem.

## Next Vertical Slice

The next architecture path is:

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

Later gates extend that real ToolResult through Agent Loop integration, evidence persistence, sequential verification, RunRecord persistence, and a CLI.

The independent verifier is not implemented as an isolated record before real Tool execution. It consumes persisted evidence from the integrated path.

## Module Direction

Future modules may be split when the codebase is large enough:

```text
enhancer-core
enhancer-context
enhancer-planner
enhancer-tool
enhancer-skill
enhancer-memory
enhancer-mcp
enhancer-cli
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
