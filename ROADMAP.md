# Roadmap

## 30-Day Target

Status: Implemented

Within 30 days, Enhancer should be able to read its own repository context, understand the current project state, and propose the next useful task without the user manually defining every step.

This first deterministic self-hosting milestone is implemented through Repository Context Reader and Task Planner. LLM-backed planning remains future work.

## Operating Target

Status: Accepted

Enhancer will be operated as a real open source project with documents, code, ADRs, tests, inline specification examples, and shared prompts. Work proceeds by Sprint, not by one large implementation pass.

## Constitution Kernel Governance

Status: Implemented

Constitution 1.1.0 defines the stable normative Kernel for document authority, lifecycle state promotion, scoped authorization, fresh verification, governed self-hosting, recovery, and protected amendments. The long-form Codex guidebook remains distributed across Architecture, RFCs, decisions, prompts, Skills, and operating documents so startup context stays bounded.

Independent verification is an accepted principle; its first sequential product implementation remains the next P1 harness slice.

## Six-Month Roadmap

Status: Accepted

### Phase 1: Month 1

- Constitution
- Architecture
- Context
- Agent Loop
- Tool

### Phase 2: Month 2

- Planner
- Skill
- Memory
- Prompt Engine

### Phase 3: Month 3

- MCP
- Plugin
- Git
- Terminal

### Phase 4: Month 4

- VSCode Extension
- CLI
- Dashboard

### Phase 5: Month 5

- Multi Agent
- Scheduler
- Background Agent

### Phase 6: Month 6

- Self Improvement
- Plugin SDK
- Open Source Release

## RFC Track

Status: Accepted

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

The operating rules in RFC-0002, RFC-0005, RFC-0007, RFC-0008, and RFC-0009 were accepted on 2026-07-14. Skill workflow implementation and runtime loading remain future work.

## Selective Agent Harness Pattern Adoption

Status: Accepted

| Priority | Pattern | Enhancer adoption point | Current state |
|---|---|---|---|
| P0 | Repeated termination and stagnation detection | Repeated Agent Loop termination slice | Implemented |
| P0 | Verification evidence contract | Tool result and verification model | Implemented |
| P1 | Independent verifier | After the single-agent loop is stable; sequential first | Ready |
| P1 | Progressive Skill loading | Skill Loader implementation | Planned |
| P2 | Artifact provenance | Plugin or template installation | Planned |
| P2 | Token and context budget | After the LLM invocation boundary | Conditional |
| P3 | Self-improvement safeguards | After Tool, review, snapshot, and rollback contracts | Principles Accepted |

Each pattern must be implemented as a provider-neutral Enhancer contract. No phase may bypass `.ai/` least-privilege Skill rules, proposal-state separation, human approval for dangerous actions, or fresh verification requirements.

## Phase 0: Project Memory

Status: Implemented

- Create durable project documents.
- Create Codex session prompts.
- Establish source-of-truth priority.
- Create Codex-ready specification documents under `docs/`.
- Create shared prompts and inline specification examples.

## Phase 1: Product Definition

Status: Implemented

- Define Enhancer's product goal.
- Choose initial runtime stack.
- Define the first self-hosting implementation task.
- Define chapter-based implementation specifications.

## Phase 2: Context Reader

Status: Implemented

- Implement the smallest feature that reads repository context documents.
- Add focused tests.
- Update architecture and state documents.

The implementation compiles and its focused tests pass with Java 17 and Gradle 8.4.

## Phase 3: Task Planner

Status: Implemented

- Generate candidate next tasks from repository context.
- Keep proposal, accepted decision, and implemented state separate.
- Add tests for planning behavior.

The deterministic Planner proposes the first ready roadmap phase only after the current task is completed and keeps proposal state explicit.

## Phase 4: Assisted Development Loop

Status: Ready

- First connect context reading and task planning in one deterministic, read-only pass with explicit terminal outcomes.
- Use the implemented repeated Agent Loop termination contract as the boundary for later execution work.
- Use the implemented verification-evidence contract as input to the next sequential independent-verifier task.
- Follow with the Tool result evidence contract and then a sequential independent verifier.
- Add prompt building and tool execution only after their owning contracts are accepted.
- Keep human approval for commits and pushes.
- Keep decisions, state, and handoffs current.

The read-only context-to-planner orchestration, bounded repeated-loop termination, and Tool result verification-evidence slices are implemented and verified. Phase 4 remains ready for the sequential independent verifier; prompt and real Tool execution remain later work.
