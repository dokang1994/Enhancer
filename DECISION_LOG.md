# Decision Log

## Accepted Decisions

### 2026-07-10: Read `.ai/` Before Every AI Work Session

Status: Accepted Decision

Decision:

Every AI agent should read the `.ai/` folder before starting work. The folder contains AI-only operational documents: `constitution.md`, `workflow.md`, `coding_rules.md`, `architecture.md`, `prompt_rules.md`, and `memory.md`.

Rationale:

The root documents are canonical, but `.ai/` gives agents a compact operational entry point. This allows the user to say "항상 .ai 폴더를 읽고 시작해" and have a consistent startup rule across Codex, Claude, GPT, and future Enhancer agents.

Consequences:

- `prompts/SESSION_START.md` includes `.ai/` in the required reading order.
- `AGENTS.md` requires agents to read `.ai/` before work.
- `.ai/` must mirror operational rules without replacing root canonical documents.

### 2026-07-10: Treat Docs As A Multi-Agent Prompt Book

Status: Accepted Decision

Decision:

Each major `docs/` chapter will end with a `Prompt Book` section containing separate prompts for Codex, Claude, and GPT.

Rationale:

Enhancer is developed by multiple AI agents with different strengths. A shared chapter can guide all agents, but each agent needs role-specific instructions to reduce ambiguity.

Consequences:

- Codex prompts focus on implementation and verification.
- Claude prompts focus on architecture and risk review.
- GPT prompts focus on explanation, task framing, and session continuity.
- New chapter documents should include all three prompt types.

### 2026-07-10: Use Explicit Session Resume Protocol

Status: Accepted Decision

Decision:

New ChatGPT sessions must be resumed by providing the core repository documents, because ChatGPT cannot automatically read the user's local Enhancer repository across sessions.

Rationale:

The project depends on repository-backed memory. Without an explicit resume protocol, a new session may rely on incomplete chat memory and drift away from the source of truth.

Consequences:

- `prompts/CHATGPT_SESSION_RESUME.md` defines the required upload/paste workflow.
- `SESSION_HANDOFF.md` must remain complete enough to recover short-term state.
- Documents override chat history when conflicts occur.
- The human owner controls final approval and push.

### 2026-07-10: Operate Enhancer As A Real Open Source Project

Status: Accepted Decision

Decision:

Enhancer will be operated as a real open source project, not as a one-off chat artifact or documentation-only repository. The project will include documentation, code, ADRs, tests, examples, and shared prompts for Codex, Claude, and GPT.

Rationale:

The expected scope is too large for a single chat session. A Git-managed, chapter-based, reviewed workflow allows the project to grow over months without losing architectural consistency.

Consequences:

- Work proceeds by Sprint and small tasks.
- Documentation and code evolve together.
- ADR review is required for meaningful design changes.
- AI roles are explicit: Codex implements; ChatGPT supports architecture, backend design, agent research, documentation, and review.
- Git repository documents remain the source of truth.

### 2026-07-10: Use Codex-Ready Chapter Documents

Status: Accepted Decision

Decision:

Enhancer will maintain feature documents under `docs/` as Codex-ready prompts. Each document should describe the goal, architecture, task boundary, tests, and out-of-scope items for a major platform capability.

Rationale:

The project is too large to drive from chat history. Chapter-based Markdown specifications allow Codex, Claude, GPT, and future Enhancer agents to implement one slice at a time from repository state.

Consequences:

- `docs/` is part of the operating system for development, not passive documentation.
- New major capabilities should receive a prompt-style specification before implementation.
- Implementation should proceed sprint by sprint rather than attempting a full Cursor-like platform at once.

### 2026-07-10: Use Document Driven Development

Status: Accepted Decision

Decision:

Enhancer will follow Document Driven Development. New work must move through Constitution, Architecture, ADR / Decision Log, Task, Implementation, Test, and Documentation Update before it is considered complete.

Rationale:

Enhancer depends on repository documents as durable memory. If code changes happen before architecture, decisions, and tasks are clarified, future AI sessions will lose the reason behind the implementation.

Consequences:

- Agents must not jump directly from idea to code.
- Important architectural changes must be recorded before or during implementation.
- `CURRENT_TASK.md` remains the scope boundary for implementation.
- Documentation update is part of Definition of Done.

### 2026-07-10: Build Enhancer As A Self-Hosting AI Development OS

Status: Accepted Decision

Decision:

Enhancer is not a Cursor clone. Enhancer is a self-hosting AI Development Operating System that should eventually read its own repository context, understand project state, propose the next task, and assist its own development.

Rationale:

The project goal is not to copy Cursor's interface or behavior. The goal is to build a durable framework where AI agents can resume work from repository state and eventually help operate the project themselves.

Consequences:

- The first product slice should prioritize context reading and task planning over UI polish.
- Repository documents are product inputs, not only project management artifacts.
- The 30-day milestone is for Enhancer to propose next tasks from repository context.

### 2026-07-10: Use Repository Documents As Durable Memory

Status: Accepted Decision

Decision:

Enhancer will use repository documents as the durable memory for future ChatGPT and Codex sessions.

Rationale:

Conversation memory is unreliable across sessions. Repository files can be read, reviewed, committed, and treated as the single source of truth.

Consequences:

- Agents must read the required documents at session start.
- `SESSION_HANDOFF.md` must be updated at session close.
- Proposals must not be treated as accepted decisions until recorded here.

## Proposals

- Define the product scope for Enhancer.
- Choose the initial implementation stack.
