# Changelog

## 2026-07-14

- Added a Gradle 8.4 Wrapper and project-local Microsoft OpenJDK 17 setup workflow.
- Added PowerShell setup and Gradle launcher scripts for reproducible Windows builds.
- Verified compilation and all 5 tests through the Wrapper.
- Accepted repository Skill authoring and least-privilege permission rules.
- Added memory distillation, test-first scope, and fresh verification evidence requirements.
- Added a Proposed-only Skill catalog without activating unimplemented Skills.
- Synchronized `.ai`, session prompts, README, architecture chapters, and related RFCs.
- Clarified that verification cycles do not require automatic commits.

## 2026-07-12

- Implemented the deterministic Repository Task Planner.
- Added structured task proposals with explicit proposal state, scope, acceptance criteria, exclusions, and risks.
- Added Planner tests for ready roadmap selection, active-task protection, and incomplete roadmap risk reporting.
- Added the Java 17 Gradle project structure for the first product slice.
- Added Gradle build output exclusions.
- Implemented the Repository Context Reader with ordered UTF-8 document loading and clear missing-document errors.
- Added JUnit 5 tests for successful context reads and missing required documents.
- Added `.editorconfig` to declare UTF-8 repository text encoding.
- Added `.gitattributes` to keep repository text normalization stable.
- Replaced the `.ai/workflow.md` Korean startup sentence with ASCII English to avoid console encoding display issues.

## 2026-07-10

- Added repository-backed project memory documents.
- Added Codex session prompt templates.
- Established source-of-truth and session handoff rules.
- Added self-hosting AI Development Operating System vision.
- Added 30-day milestone for repository-context-based task proposal.
- Created required repository folders: `docs/`, `examples/`, `.ai/`, and `src/`.
- Added AI-only operating documents under `.ai/`.
- Defined the first self-hosting implementation task: Repository Context Reader.
- Added Document Driven Development workflow as the project operating process.
- Added Codex-ready feature specification documents under `docs/`.
- Added shared coding, architecture, and review prompts.
- Added Agent Loop, Tool, and Skill concept examples.
- Created initial local Git commit for project memory bootstrap.
- Added open source operating model and long-running Sprint-based project direction.
- Added explicit ChatGPT session resume protocol and prompt.
- Added Prompt Book sections for Codex, Claude, and GPT to chapter documents.
- Renamed local branch to `main` and configured GitHub remote `origin`.
- Pushed `main` to `origin/main`.
- Documented the startup rule: always read `.ai/` before starting work.
- Recorded the `.ai/` startup rule as an accepted decision.
- Added RFC-style design track under `docs/rfcs/`.
- Added six-month AI Development OS roadmap.
