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
