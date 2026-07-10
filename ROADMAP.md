# Roadmap

## 30-Day Target

Status: Accepted

Within 30 days, Enhancer should be able to read its own repository context, understand the current project state, and propose the next useful task without the user manually defining every step.

This is the first self-hosting milestone.

## Operating Target

Status: Accepted

Enhancer will be operated as a real open source project with documents, code, ADRs, tests, examples, and shared prompts. Work proceeds by Sprint, not by one large implementation pass.

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

## Phase 0: Project Memory

Status: Implemented

- Create durable project documents.
- Create Codex session prompts.
- Establish source-of-truth priority.
- Create Codex-ready specification documents under `docs/`.
- Create shared prompts and examples.

## Phase 1: Product Definition

Status: Implemented

- Define Enhancer's product goal.
- Choose initial runtime stack.
- Define the first self-hosting implementation task.
- Define chapter-based implementation specifications.

## Phase 2: Context Reader

Status: Ready

- Implement the smallest feature that reads repository context documents.
- Add focused tests.
- Update architecture and state documents.

## Phase 3: Task Planner

Status: Pending

- Generate candidate next tasks from repository context.
- Keep proposal, accepted decision, and implemented state separate.
- Add tests for planning behavior.

## Phase 4: Assisted Development Loop

Status: Pending

- Connect context reading, task planning, prompt building, and tool execution.
- Keep human approval for commits and pushes.
- Keep decisions, state, and handoffs current.
