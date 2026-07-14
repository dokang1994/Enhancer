# Architecture

## Status

The first five product architecture slices, Repository Context Reader, deterministic Task Planner, deterministic single-pass Assisted Development Loop, bounded repeated Agent Loop termination, and bounded Tool result verification evidence, are implemented as a simple single Gradle project.

The accepted product direction is Self-hosting AI Development Operating System.

## Target Architecture

Enhancer will evolve toward these major components:

- Kernel: constitution, AI rules, scheduler, and core operating policies.
- Prompt Engine: converts tasks into model-specific prompts.
- Context Builder: reads repository documents and code context.
- Memory: treats Git repository state as durable memory.
- Planner: proposes next tasks from current project state.
- Task Queue: tracks selected and pending work.
- Prompt Builder: creates prompts for AI agents and tools.
- Tool System: exposes file, terminal, search, Git, and external tools.
- Skill System: stores reusable agent workflows.
- Agent Loop: coordinates context, planning, tool use, verification, and handoff.
- MCP and Plugin System: integrates external capabilities.
- Agent Runtime: supports single-agent and future multi-agent operation.
- CLI, VSCode Extension, and Web Dashboard: user-facing control surfaces.
- SDK: allows plugins such as Oracle, MyBatis, Spring, WebSquare, React, and Vue.

## Operating System Model

Enhancer is modeled as an AI Development Operating System:

```text
Kernel
↓
Scheduler
↓
Planner
↓
Memory
↓
Tool
↓
Skill
↓
Agent
↓
Plugin
↓
LLM
```

Cursor-like behavior is treated as an application-level capability on top of Enhancer, not the identity of Enhancer itself.

## Specification Architecture

`docs/` contains Codex-ready architecture and implementation guides. These documents are part of the product operating system, not secondary notes.

- `docs/00-Project-Overview.md`: project identity and scope
- `docs/01-Development-Environment.md`: environment checks and bootstrap target
- `docs/02-Agent-Loop.md`: Agent Loop design
- `docs/03-Tool-System.md`: Tool System design
- `docs/04-Skill-System.md`: Skill System design
- `docs/05-Memory.md`: repository-backed memory design
- `docs/06-Planner.md`: task planning design
- `docs/07-MCP.md`: MCP integration direction
- `docs/08-Multi-Agent.md`: multi-agent collaboration model
- `docs/09-Background-Agent.md`: background agent safety model
- `docs/10-Roadmap.md`: 30-day self-hosting plan
- `docs/11-Architecture.md`: expanded architecture guide

## RFC Architecture

Major design areas are tracked in `docs/rfcs/`.

- `RFC-0001`: Constitution
- `RFC-0002`: AI Behavior Specification
- `RFC-0003`: Prompt Contract
- `RFC-0004`: Context Builder
- `RFC-0005`: Planner
- `RFC-0006`: Tool Specification
- `RFC-0007`: Skill Specification
- `RFC-0008`: Memory Specification
- `RFC-0009`: Multi Agent
- `RFC-0010`: AI Operating System
- `RFC-0011`: Plugin SDK
- `RFC-0012`: Self Improvement

## First Architecture Slice

The first product slice should be a Repository Context Reader.

Expected responsibility:

- Read the required project documents in constitution order.
- Preserve source order and document identity.
- Report missing required documents clearly.
- Produce a structured context object that later components can use.

Implemented package: `com.enhancer.context`

The slice uses immutable context records, an enum as the canonical required-document order, and a filesystem reader. It has no Spring wiring because the current behavior does not require an application container.

## Planner Slice

The first Planner slice is implemented in `com.enhancer.planner` and consumes `ProjectContext` directly.

It is deterministic: an active `CURRENT_TASK.md` blocks a new proposal; otherwise the first ready phase in `ROADMAP.md` becomes one structured proposal. Proposal state is explicit and remains separate from accepted decisions and implementation state. The slice does not call an LLM, mutate documents, rank alternatives, or execute work.

## Assisted Development Loop Slice

The first Assisted Development Loop slice is implemented in `com.enhancer.loop`. It composes `ProjectContextReader` and `RepositoryTaskPlanner` in one deterministic pass. Its result has an explicit terminal outcome: either `PROPOSAL_AVAILABLE` or `ACTIVE_TASK_PRESERVED`.

This slice reads repository state but does not mutate it. It does not repeat work, build prompts, execute tools, call an LLM, approve a proposal, or perform Git operations. Maximum-iteration and stagnation termination are implemented separately in the repeated Agent Loop slice below.

## Repeated Agent Loop Termination Slice

The repeated Agent Loop termination slice is implemented under `com.enhancer.loop`. A caller-supplied step produces the next immutable state; the loop owns only termination safety and iteration accounting.

The accepted stop reasons are `COMPLETED`, `FAILED`, `MAX_ITERATIONS`, and `STAGNATED`. Defaults are 20 maximum iterations and 3 consecutive unchanged progress keys. Terminal task status is evaluated first, followed by the maximum-iteration ceiling and then stagnation. A completing or failing step is not misclassified as stalled, and `MAX_ITERATIONS` wins when the ceiling and stagnation threshold coincide. This slice does not execute Tools, build prompts, call an LLM, or perform verification.

An independent verifier will be introduced later as a sequential boundary after the single-agent loop is stable. It must not imply parallel multi-agent execution or allow a worker to verify its own result.

## Tool Result Verification-Evidence Slice

The first Tool System slice is implemented under `com.enhancer.tool` as provider-neutral result and evidence records without a Tool interface or external behavior.

Every `ToolResult` carries a tool name, explicit success or failure status, an optional process exit code, and required `VerificationEvidence`. Evidence keeps a non-blank summary of at most 512 characters and the final 4096 characters of output. When output is truncated, the caller must supply a non-blank reference to the complete output; persistence behind that reference remains future work.

Tool status and an available exit code must agree: success requires exit code zero, while failure cannot carry exit code zero. Tools without process exit codes may leave it absent. This contract bounds Agent Context growth while retaining the most recent diagnostic output and a route to full evidence.

## Constitution Kernel Architecture

`CONSTITUTION.md` is the stable normative Kernel, not the complete Codex guidebook. It defines identity, document authority, lifecycle states, authorization boundaries, verification principles, self-hosting safeguards, and amendment governance.

Operational procedures belong in `AGENTS.md` and `.ai/`; component contracts belong in RFCs and `docs/`; active and implemented state belong in `CURRENT_TASK.md` and `PROJECT_STATE.md`; repeatable invocations belong in prompts and validated Skills. The 300-page documentation target is distributed across this document system so every session does not need to load the entire guidebook as constitutional context.

## Architectural Principles

- Repository documents are product inputs, not only project management notes.
- Keep conceptual examples with their owning specification and executable examples in tests; do not maintain a separate `examples/` directory.
- Keep the first implementation minimal.
- Prefer Java 17, Spring Boot 3, Gradle, JUnit5, and Mockito.
- Do not introduce DDD early.
- Do not add abstractions until duplication or complexity justifies them.
- Accepted decisions belong in `DECISION_LOG.md`.
- Implemented state belongs in `PROJECT_STATE.md`.
- Repository Skills use validated `skills/<name>/SKILL.md` definitions with least-privilege capability categories.
- Proposed Skill catalog entries are design candidates, not loadable runtime inputs.
- Repository memory is distilled by promoting reusable procedures to Skills and repository-local rationale to decisions or ADRs.
- External agent harnesses are reference implementations, not runtime dependencies. Selected patterns must be restated as provider-neutral Enhancer contracts and introduced only when the owning roadmap slice is active.

## Open Architecture Questions

- CLI entry point is not selected yet.
- Context size and token budgeting strategy are not selected yet.
- Future LLM-backed Planner input/output schema is not selected yet.
