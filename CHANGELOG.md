# Changelog

## 2026-07-14

- Published the governed Agent Loop foundation on `agent/governed-agent-loop-foundations` and opened draft PR #2.
- Restored Git metadata for the active C:\Enhancer worktree from a validated no-checkout clone without changing working files.
- Reconstructed the Git index from HEAD, verified repository identity and object integrity, and confirmed all 1,479 non-.git files remained byte-identical.
- Replaced Constitution 1.0.0 with a deduplicated, versioned 1.1.0 normative Kernel.
- Added explicit lifecycle states, scoped authorization, fresh-evidence rules, self-hosting safeguards, recovery requirements, and protected semantic-versioned amendments.
- Delegated detailed technology and component guidance to Architecture and RFCs and synchronized Agent, `.ai`, RFC-0001, and session-prompt rules.
- Verified all 15 required Constitution sections, confirmed obsolete implementation details are absent, and reran all 25 product tests successfully.
- Removed the standalone `examples/` directory and consolidated conceptual examples into specifications and executable examples into tests.
- Updated Constitution, README, architecture, roadmap, decision, state, and handoff documents for the smaller repository structure.
- Verified the unchanged product code with all 25 tests passing after removal.
- Implemented bounded Tool result verification evidence without real Tool execution.
- Added 512-character summaries, 4096-character diagnostic tails, truncation metadata, and complete-output references.
- Added explicit Tool success/failure status with optional exit-code consistency rules.
- Added 8 Tool contract tests and verified all 25 repository tests with no failures, errors, or skips.
- Confirmed the selected external agent-harness patterns are compatible with `.ai/` under staged, provider-neutral adoption.
- Added the ordered pattern adoption plan, including a sequential independent verifier, to the roadmap and decision log.
- Implemented bounded repeated Agent Loop termination with completed, failed, maximum-iteration, and stagnated reasons.
- Added immutable loop state/result contracts, configurable 20/3 defaults, deterministic termination precedence, and invariants.
- Added 9 Agent Loop tests and verified all 17 repository tests with no failures, errors, or skips.
- Accepted selective, provider-neutral adoption of high-value MoAI-ADK patterns without adding it as a dependency.
- Implemented a deterministic, read-only Assisted Development Loop that composes context reading and task planning once.
- Added explicit proposal-available and active-task-preserved outcomes with result invariants.
- Added 3 loop tests and verified all 8 repository tests with no failures, errors, or skips.
- Staged repeated-loop termination, verification evidence, Skill loading, artifact provenance, token budgets, and self-improvement for their owning roadmap slices.
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
