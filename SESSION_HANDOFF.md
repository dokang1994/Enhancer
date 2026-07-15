# Session Handoff

## Updated At

2026-07-15

## Completed Work

- Implemented the tenth Delivery Gate 6 increment: production graph composition on the CLI `run` path — prior run records observed into the snapshot, accepted-decision nodes merged into the run-evidence graph, the task impact query answered in process, and bounded `graphNodes`/`graphEdges`/`graphDecisions`/`impactExecutions` output.
- Implemented the ninth Delivery Gate 6 increment: `RunRecordMetadataCollector` plus the store's read-only ordered `references()` listing, with corrupted or missing records surfaced as explicit `UNAVAILABLE` observations.
- Implemented the eighth Delivery Gate 6 increment: `AcceptedDecisionProjector` parsing accepted decisions from the decision log's own status lines into unlinked `DECISION` nodes with snapshot-relative freshness.
- Implemented the seventh Delivery Gate 6 increment: `RunEvidenceGraphProducer`, the first graph producer, projecting evidence-only task/artifact/execution nodes and one `RECORDED_AS` edge from one snapshot and one task-matched stored run record, with one-to-one observation-state-to-freshness mapping.
- Integrated the run-evidence production path: the end-to-end test flows a real governed run and really-collected snapshot through the producer into an impact-query answer naming the real stored execution.
- Implemented the sixth Delivery Gate 6 increment: `TaskImpactQuery` and the immutable `TaskImpact` result answering the task-to-decision-to-code-to-test chain over one projected graph, with snapshot-traceable identity and rebuild status derived from every traversed element.
- Implemented the fifth Delivery Gate 6 increment: the metadata-only graph projection contract (`GraphNode`, `GraphEdge`, `GraphProvenance`, `GraphElementFreshness`, `ProjectBrainGraph`) with five node kinds, six endpoint-checked edge kinds over the five roadmap relationship domains, and snapshot-keyed versioned projections.
- Enforced provenance invariants (Current/Stale require a SHA-256 revision, Source-Missing prohibits one, rebuild status is derived) plus deterministic ordering, duplicate/self-loop/unknown-endpoint rejection, and 4096-element bounds; named the impact query as the consumer.
- Implemented the fourth Delivery Gate 6 increment: production composition of the `ProjectBrainView` on the CLI `run` path from already-loaded memory, the collected snapshot, and the persisted RunRecord, with bounded `workspaceSnapshotId`, `workspaceObservations`, and `memoryFreshness` output.
- Promoted the production repository-memory composition to Operational with an actual-repository run and unchanged replay; the snapshot identity is intentionally not stored in the RunRecord (deferred to Gate 7 envelopes).
- Implemented the third Delivery Gate 6 increment: the read-only `RepositoryMemorySnapshotCollector`, the first Workspace source adapter, deriving a real snapshot from already-loaded Context Reader memory without reading files or retaining content.
- Integrated the repository-memory path end to end through `WorkspaceCollectionIntegrationTest`: real governed CLI run, real persisted RunRecord, real Context Reader memory, collector, and composed `ProjectBrainView`, including exact `SNAPSHOT_DIVERGED` detection after the active task document changed.
- Implemented the second Delivery Gate 6 increment: the read-only `ProjectBrainView` aggregate under `com.enhancer.brain` with `RepositoryMemoryEntry`, `RunProvenance`, and `MemoryFreshness`.
- Gave the Contract Verified `WorkspaceSnapshot` its first consumer by composing one real snapshot, one real `ProjectContext`, and one real `RunRecord` behind one immutable view.
- Derived repository-memory freshness from digest comparison against snapshot observations and rejected runs that do not match the snapshot's approved task revision.
- Kept document content, Tool payloads, evidence bodies, adapters, live collection, persistence, and Tool authority outside the increment.
- Recorded `ProjectBrainView` as Contract Verified while leaving Gate 6 as the sole `Specified - Next` product gate.
- Implemented the first Delivery Gate 6 contract under `com.enhancer.workspace`.
- Added immutable approved-task revision provenance and typed metadata observations for repository documents/files, active/selected files, Git status/diff, diagnostics, terminal sessions, and RunRecords.
- Added explicit Available, Stale, and Unavailable invariants with bounded metadata, digest validation, and temporal consistency.
- Added immutable `WorkspaceSnapshot` capture with absolute normalized root, canonical ordering, duplicate and 4096-entry limits, and a versioned SHA-256 identity sensitive to every metadata field.
- Kept payload capture, adapters, persistence, Tool authority, approval creation, and Project Brain integration outside the contract.
- Recorded the snapshot sub-capability as Contract Verified while leaving Gate 6 as the sole `Specified - Next` product gate.
- Implemented Delivery Gate 5 First Operational CLI at `com.enhancer.cli.EnhancerCli`.
- Registered the Gradle `application` entry point and exposed only non-interactive `run` and `replay` commands.
- Required explicit project root, active task ID, relative target path, expected lowercase SHA-256, evidence root, and RunRecord root for execution.
- Reused the existing Context Reader, repository-derived approval, `read-file` Tool boundary, Agent Loop, independent verifier, finalizer, Evidence Store, and RunRecord Store.
- Added stable exit codes for completed, usage/configuration, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure.
- Bounded stdout and stderr to 4096 characters, sanitized line breaks, omitted complete evidence, and emitted no stack traces.
- Added restart-safe typed RunRecord replay without Tool re-execution or chat history.
- Added test-first coverage for parsing, exit-code mapping, bounded diagnostics, temporary-project completion, mismatch, task mismatch, Tool failure persistence, and replay.
- Documented invocation, exit codes, recovery, and replay in `README.md` and synchronized canonical/compact architecture, Roadmap, Project State, Current Task, Changelog, and this handoff.
- Promoted Delivery Gate 5 to Operational and Delivery Gate 6 Workspace and Project Brain Foundation to the sole `Specified - Next` gate.
- Added a RED classification gate to the AI workflow, Agent rules, and implementation prompt: aligned missing implementation proceeds directly to the minimum GREEN change, while unrelated, flaky, conflicting, scope-expanding, or newly privileged failures are reported separately.
- Promoted Gate 0 to Integrated through `FoundationLifecycleIntegrationTest` without a production correction or second orchestration path.

## Repository State

- Root: `C:/Enhancer`.
- Branch: `main`, tracking `origin/main`.
- Published Gate 6 delivery commit: `c5a16b9` (`feat: add Gate 6 workspace snapshot contract`).
- Gate 5, Gate 0 integration promotion, and the RED workflow clarification are committed and published on `origin/main`.
- The Gate 6 WorkspaceSnapshot implementation and synchronized documents are committed and published on `origin/main`.
- `gate-6-workspace-snapshot-contract` is Completed; its record is preserved in commit `c5a16b9`, `CHANGELOG.md`, and `PROJECT_STATE.md`.
- `gate-6-project-brain-view-integration` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-repository-memory-snapshot-collection` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-run-evidence-graph-producer`, `gate-6-accepted-decision-projection`, and `gate-6-run-record-metadata-observation` are Completed; their records are preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `CURRENT_TASK.md` is Completed for `gate-6-production-graph-composition`.
- The first five Gate 6 increments (view, collector, production composition, graph contract, impact query) are published on `origin/main` through delivery commit `d3b6197`.
- The four later increments (run-evidence producer, decision projection, run-record observation with store listing, production graph composition) are committed and published on `origin/main` through delivery commit `396665b`.
- The actual-repository evidence runs persisted `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` and `run-record/69977403-1cfb-45ba-ba0f-9239ad26a8c1` under the Git-ignored `.enhancer/run-records` directory.
- The actual-repository evidence run persisted `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` under the Git-ignored `.enhancer/run-records` directory.

## Fresh Verification

- Composition RED: both focused CLI graph-composition tests failed with the expected `output does not contain graphDecisions=` assertion while the runs completed.
- Composition focused GREEN: CLI, workspace, brain, and integration suites passed 17 suites and 50 tests with no skips, failures, or errors.
- Actual repository `run`: `README.md`, task `gate-6-production-graph-composition`, exit code 0, `COMPLETED`, `VERIFIED`, snapshot `d5bd10cb...a44632`, 17 observations (15 documents plus 2 prior run records), `graphNodes=61`, `graphEdges=1`, `graphDecisions=44` matching the decision log exactly, `impactExecutions=1`.
- Observation RED: 8 expected missing-symbol errors naming only the absent `RunRecordMetadataCollector` and `references()`; focused GREEN passed 8 suites and 33 tests.
- Projection RED: 6 expected missing-symbol errors naming only the absent `AcceptedDecisionProjector`; focused GREEN passed 5 suites and 20 tests.
- Producer RED: the first focused compile failed with 6 expected missing-symbol errors naming only the absent `RunEvidenceGraphProducer`.
- Producer focused GREEN: Project Brain and integration suites passed 6 suites and 18 tests with no skips, failures, or errors.
- End-to-end: the extended `WorkspaceCollectionIntegrationTest` flowed a real governed run and really-collected snapshot through the producer into an impact-query answer naming the real stored execution reference.
- Impact-query RED: the first focused compile failed with 9 expected missing-symbol errors naming only the absent `TaskImpactQuery` and `TaskImpact`.
- Impact-query focused GREEN: 3 suites and 13 tests with no skips, failures, or errors.
- Graph RED: the first focused compile failed with 100 expected missing-symbol errors naming only the seven intentionally absent graph types.
- Graph focused GREEN: 2 suites and 9 tests with no skips, failures, or errors.
- Composition RED: both focused CLI composition tests failed with the expected `output does not contain workspaceSnapshotId=` assertion while the runs completed and were recorded.
- Composition focused GREEN: CLI, Workspace, Project Brain, and integration suites passed 11 suites and 29 tests with no skips, failures, or errors.
- Actual repository `run`: `README.md`, task `gate-6-production-brain-composition`, exit code 0, `COMPLETED`, `VERIFIED`, `workspaceSnapshotId=b729514d272701e8f46d32b282f24570a75147470a6b82c0bd21bb0e97e9f39f`, `workspaceObservations=15`, `memoryFreshness=matched=15,diverged=0,notObserved=0`.
- Actual repository `replay` of `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` returned exit code 0 with unchanged output.
- Collector RED: the focused compile failed with 6 expected missing-symbol errors, all naming only the absent `RepositoryMemorySnapshotCollector`.
- Collector focused GREEN: 7 suites and 20 tests with no skips, failures, or errors.
- Current full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Current full result: 38 suites, 140 tests, 138 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors, confirmed against fresh XML output.
- Current Java 17 production lint passed with `-Xlint:all -Werror`; Gradle emitted no deprecation warning.
- Current structural verification: exactly one `Specified - Next` gate status marker at Gate 6 and `git diff --check` passed.
- End-to-end: `WorkspaceCollectionIntegrationTest` connected a real governed CLI run, real RunRecord, real Context Reader memory, the collector, and the composed view; all 15 documents were `SNAPSHOT_MATCHED` and exactly `CURRENT_TASK.md` reported `SNAPSHOT_DIVERGED` after its edit.
- Not run: no production caller composes the view during an actual repository run; the end-to-end evidence is a governed temporary-project integration test.
- Project Brain RED: the first focused compile failed with 19 expected missing-symbol errors naming only `ProjectBrainView`, `RepositoryMemoryEntry`, `RunProvenance`, and `MemoryFreshness`; no error came from existing contracts.
- Project Brain focused GREEN: 4 suites, 15 tests, no skips, failures, or errors.
- Workspace RED: the first focused compile failed with 79 expected missing-symbol errors before production contracts existed.
- Workspace focused GREEN: 3 suites, 10 tests, all passed with no skips, failures, or errors.
- Post-document self-hosting verification passed 14 of 15 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Structural verification retained exactly one `Specified - Next` marker at Gate 6; `git diff --check` passed.
- RED: the first focused CLI compile failed with 45 expected missing-symbol errors.
- Focused GREEN command: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.cli.*'`.
- Focused GREEN result: 3 suites, 7 tests, 0 failures, 0 errors, 0 skips.
- Full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Full result: 24 suites, 97 tests, 95 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning.
- Java 17 production lint passed with `-Xlint:all -Werror` and no warning or error.
- Actual repository `run`: `README.md`, task `gate-5-first-operational-cli`, exit code 0, `COMPLETED`, `VERIFIED`, one worker iteration.
- Persisted record: `run-record/0ff7b4ea-5d4a-4698-b9f7-642169262737` under the Git-ignored `.enhancer/run-records` runtime directory, which Gradle `clean` does not remove.
- Actual repository `replay`: the record survived Gradle `clean` and returned exit code 0 with matching task, allowed policy, worker `AWAITING_VERIFICATION`, final `COMPLETED`, and `VERIFIED` metadata.
- Separate focused verification passed for Gate 0 (35 tests, 1 skip), Gate 1 (15 tests, 1 skip), Gate 2 (10 tests), Gate 3 (9 tests), Gate 4 (21 tests), and Gate 5 (7 tests), with no failures or errors.
- Gate 0 promotion preparation verification ran 4 context/planning/task suites and 18 tests: 17 passed, 1 Windows symbolic-link setup test skipped, and no failures or errors occurred; Roadmap retained exactly one Gate 6 next marker.
- Gate 0 lifecycle characterization passed on its first run; combined focused verification passed 43 tests across 10 suites with 1 Windows symbolic-link setup skip.
- Final full regression passed 98 tests across 25 suites: 96 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Java 17 production lint passed with `-Xlint:all -Werror`; no production correction was required for Gate 0 promotion.

## Current Maturity

- Gate 0: Integrated.
- Gates 1 through 4: Integrated.
- Gate 5: Operational for one governed read-only local CLI scenario.
- Gate 6: Specified - Next.
- Gate 6 metadata-only `WorkspaceSnapshot` sub-capability: Contract Verified.
- Gate 6 read-only `ProjectBrainView` sub-capability: Contract Verified.
- Gate 6 repository-memory path (real governed run -> real memory -> collector -> composed view with divergence detection): Integrated.
- Gate 6 production composition: Operational for the governed read-only CLI scenario; every recorded `run` reports bounded snapshot identity, observation count, and memory freshness.
- Gate 6 graph projection contract: Contract Verified; consumed by the impact query and the run-evidence producer.
- Gate 6 task impact query: Contract Verified; it answers over really-produced graphs in the integration path.
- Gate 6 run-evidence graph production path: Integrated.
- Gate 6 accepted-decision projection and run-record metadata observation: Contract Verified; consumed by the production graph composition.
- Gate 6 production graph composition: Operational for the governed read-only CLI scenario; decisions remain unlinked in impact answers, and modifies/verified-by producers do not exist.
- Gate 0 integration proves planning, explicit external activation, verified execution, persistence, and replay without changing Proposal authority.
- Workspace source adapters and live collection, LLM invocation, Event/Message Bus, IPC, Scheduler, broader Agent Runtime, MCP, Skills, plugins, multi-agent execution, background execution, and release packaging remain unimplemented.

## Next Task

Activate a separate Gate 6 increment: a task-to-decision reference grammar with its `JUSTIFIED_BY` projection, or the next read-only source adapter. A Git status/diff adapter additionally requires an explicit user decision on external command authority. Payload capture and messaging remain later increments.

## Remaining Risks

- The CLI trusts an externally supplied expected digest; its origin is explicit and auditable but not signed.
- Evidence and RunRecord envelopes detect corruption but are not encrypted, signed, remotely replicated, or automatically cleaned up.
- The CLI uses the existing 64 MiB in-memory ceiling, five-second Tool timeout, five-iteration loop ceiling, three-transition stagnation threshold, and declared 30-day retention without cleanup.
- Two symbolic-link containment tests are skipped on this Windows host because link creation privilege is unavailable; they remain active on permitted hosts.
- Gradle remains at Wrapper 8.4. The known Gradle 9 test-runtime deprecation is removed, but an actual major Wrapper upgrade requires a separate compatibility task.
- Gate 5 is a bootstrap CLI, not the future multi-interface control surface planned for Gate 12.

## Instructions For Next Agent

1. Read `.ai/` and every canonical startup document in repository order.
2. Confirm Gate 6 is the sole `Specified - Next` gate status marker and `CURRENT_TASK.md` records `gate-6-production-graph-composition` as Completed.
3. All nine post-contract Gate 6 increments are published on `origin/main` through delivery commits `d3b6197` and `396665b`; the working tree should be clean apart from any newly activated work.
4. Activate a bounded reference-grammar or next-adapter task before editing production code; defer payload capture and messaging, and obtain explicit authority before any external command adapter.
5. Do not commit or push unless explicitly requested.
