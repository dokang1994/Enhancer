# Current Task

## Status

Completed

## Task

Document provider-neutral Agent orchestration guidance derived from reviewed external harness patterns without changing the current implementation order or capability maturity.

## Task ID

multi-agent-orchestration-reference-alignment

## Context

The user approved adding useful Multi-Agent orchestration guidance from the reviewed Archon and meta-harness repositories. Enhancer already requires typed messages, durable evidence, independent verification, bounded authority, and gate-owned implementation. The documentation must translate useful patterns into those existing contracts rather than importing either project as a runtime dependency.

Reference snapshots:

- Archon commit `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16` (MIT), inspected for dynamic capability rosters, centralized execution profiles, dependency-ready work, run control, heartbeats, and operational visibility.
- meta-harness commit `ccab9a677878f72b3316de464c99b36f56a3f2e7` (Apache-2.0), inspected for orchestration-pattern selection, deterministic handoffs, Producer-Reviewer flow, supervisor rules, validation scenarios, and removable provider-specific logic.

## Acceptance Criteria

- Preserve `CONSTITUTION.md`, repository authority, lifecycle, approval, evidence, and independent-verification rules.
- Keep Delivery Gate 5 First Operational CLI as `Specified - Next`; do not claim Agent Runtime or Multi-Agent implementation.
- Record an accepted decision that treats both repositories as pinned references rather than dependencies.
- Define the smallest-to-largest orchestration progression: single worker, sequential pipeline, Producer-Reviewer, bounded fan-out/fan-in, expert routing or supervisor allocation, and shallow hierarchy only when justified.
- Require common immutable input snapshots, typed versioned handoffs, single final-state ownership, dependency validation, leases, idempotency, cancellation, replay, budgets, and preserved authorization context.
- Define provider-neutral execution profiles, diagnostic-only heartbeats and quality telemetry, typed control/intervention decisions, and verified-only completion.
- Assign every adopted pattern to its owning Delivery Gate and list rejected patterns explicitly.
- Synchronize canonical Architecture, Roadmap, RFC-0009, Multi-Agent guidance, compact AI architecture, Project State, Changelog, and Session Handoff.
- Run fresh structural, reference, Planner/Assisted Loop, and full regression checks, then review the final diff.

## Out Of Scope

- Multi-Agent runtime, scheduler, Message Bus, Workspace, Model Gateway, Skill Engine, or UI implementation
- New production or test code for orchestration
- Installing, vendoring, or copying Archon or meta-harness packages, prompts, Skills, or directory layouts
- Changing Skill schema, Tool authority, lifecycle state, or capability maturity
- LLM calls, shell or Git mutation Tools, background execution, or autonomous self-improvement
- Reordering Delivery Gates or displacing Delivery Gate 5

## Approval

Approved by the user on 2026-07-14 through the request to add constitution-compatible orchestration guidance for future Multi-Agent implementation. The user separately authorized commit, push, and merge after successful verification.

## Allowed Tools

- read-file

## Verification Plan

- Check that required orchestration invariants and both pinned reference commits appear in their canonical documents.
- Confirm Delivery Gates remain sequential from 0 through 16 and exactly one `Specified - Next` marker remains at Gate 5.
- Confirm no document promotes Agent Runtime, Message Bus, Scheduler, Skill Engine, or Multi-Agent capability beyond Planned.
- Run focused Planner and Assisted Development Loop regression tests against the canonical Roadmap.
- Run the full Gradle regression suite and inspect fresh test-result XML.
- Run Markdown link/reference checks where available, `git diff --check`, and final diff review.

## Implementation Result

- Recorded an accepted decision that pins Archon `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16` and meta-harness `ccab9a677878f72b3316de464c99b36f56a3f2e7` as design references rather than dependencies.
- Added the smallest-sufficient orchestration progression, capability-roster and profile boundaries, common immutable snapshots, typed versioned handoffs, one Kernel terminal-state owner, Scheduler recovery controls, bounded budgets, and independent-verification rules.
- Defined Producer-Reviewer as bounded workflow review rather than completion authority and kept heartbeat, quality, confidence, and prompt-adherence signals diagnostic only.
- Assigned each adopted contract to Delivery Gates 6 through 15 and expanded the Planned Gate 13 scope without changing any capability maturity or implementation order.
- Listed rejected provider, prompt, shared-worktree, file-queue, optional-verification, subjective-completion, unlimited-execution, and silent-evidence-loss patterns.
- Synchronized `ARCHITECTURE.md`, `ROADMAP.md`, `docs/rfcs/RFC-0009-Multi-Agent.md`, `docs/08-Multi-Agent.md`, `.ai/architecture.md`, `DECISION_LOG.md`, `PROJECT_STATE.md`, `CHANGELOG.md`, and `SESSION_HANDOFF.md`.
- Added no Multi-Agent production or test code and installed or copied no external packages, prompts, Skills, or directory layouts.

## Verification

- Both pinned GitHub tree links returned HTTP 200 on 2026-07-15.
- Structural checks passed: Delivery Gates 0 through 16 are sequential, Gate 5 is the sole `Specified - Next` gate, and Gates 7, 8, 9, 10, and 13 remain Planned.
- Required orchestration invariants and both pinned references are present in every required canonical/alignment document.
- Focused command: `.\scripts\gradle.ps1 cleanTest test --tests "com.enhancer.planner.RepositoryTaskPlannerTest" --tests "com.enhancer.loop.AssistedDevelopmentLoopTest"`.
- Focused result: 2 suites, 8 tests, 8 passed, 0 skipped, 0 failures, and 0 errors.
- The first full regression attempt exposed an environment limitation: 8 file-path integration tests failed because the sandboxed Java child process received `AccessDeniedException` for JUnit temporary paths under the user profile.
- Final command used the workspace-local JUnit temp root through `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=C:/Enhancer/build/tmp/junit` and ran `.\scripts\gradle.ps1 --no-daemon cleanTest test`.
- Final result: 21 suites, 82 tests, 81 passed, 1 existing symbolic-link setup skip, 0 failures, and 0 errors.
- Gradle 8.4 emitted its existing deprecation warning for future Gradle 9 compatibility.

## Next Task

Implement Delivery Gate 5 First Operational CLI after this documentation alignment is completed.
