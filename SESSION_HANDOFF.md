# Session Handoff

- The cross-cutting Product Journey and Evaluation Track is now specified with four canonical journeys and a fifth priority for a versioned evaluation/release-quality harness; it changes no Delivery Gate maturity.
- Scheduler reliability now truthfully specifies at-least-once delivery plus idempotency, fenced leases, checkpoints, state migration, orphan reclamation, and replay-safe/compensatable effects rather than universal exactly-once execution.
- Interfaces now share one Run/approval/verification/evidence/control API with CLI first, VS Code second, and Desktop later; Gate 12 owns one change-centered review projection.
- The layered default-security model treats repository, Tool/terminal, model, MCP, plugin, dependency, and generated content as untrusted data and assigns concrete enforcement to the owning gates without amending the Constitution.
- Gate 7 now has one named integration-preparation path: a real repository-approved task and Gate 6 Workspace snapshot flow through `WorkMessagePublisher`, the real in-process queue/journal/replay path, and `WorkItemAdmissionHandler` into the unchanged Gate 8 `WorkItem`.
- Gate 7 remains Contract Verified pending a separate full Integrated maturity assessment. Gate 8 remains the sole `Specified - Next` gate, and its immutable `WorkItem` admission sub-capability is Contract Verified.
- A concrete IPC adapter was correctly excluded from the promotion prerequisite: endpoint, serialization, authentication, threading, persistence, and production wiring remain deferred integration work.
- Fresh combined evidence includes 42 focused tests, a 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 122 production sources.
- The Gate 7 IPC contract, assessments, payload correction, promotion, and Gate 8 next-marker transition are published through `16c7f5d`; the WorkItem and integration-preparation increments are uncommitted and have no commit or push authority.

## Updated At

2026-07-16

## Completed Work

- Specified the cross-cutting product journeys, evaluation metrics/harness, truthful Scheduler delivery model, shared interface order, change-centered UX, and layered security ownership; strengthened Gate 13 and Gate 16 measured-quality exit criteria without code or maturity changes.
- Added and verified the authority-preserving Gate 7 integration-preparation path: real Context Reader and Workspace inputs flow through one bounded work publisher, the actual in-process queue/journal/replay behavior, and one injected WorkItem admission handler without a concrete IPC adapter or Scheduler behavior.
- Implemented and Contract Verified immutable Gate 8 `WorkItem` admission over one unchanged Gate 7 work envelope, with separate identity, bounded capability metadata, projection-only accessors, and no authority or scheduler behavior.
- Promoted Gate 7 to Contract Verified after mapping every scope item and exit criterion to fresh evidence, and advanced the sole `Specified - Next` marker to Gate 8 without production code or concrete-adapter work.
- Updated only the actual-Roadmap Planner and Assisted Loop expectations from Gate 7 to Gate 8 after the marker move produced the expected focused RED.
- Completed the test-first Gate 7 work-payload correction: public `WorkPayload.MAX_ALLOWED_TOOLS = 256` accepts the exact boundary and rejects 257 while preserving immutable scope copying and all existing envelope semantics.
- Completed the Gate 7 maturity assessment without production or test changes; its original finding was five fully verified scope items and three fully verified exit criteria, with the payload cardinality gap recorded for the separate correction that has now closed it. Integrated and Operational claims remain unsupported.
- Implemented the seventh Gate 7 increment: Contract Verified finite non-blocking pending-queue backpressure through immutable `BackpressurePolicy` (1-4096), scope-level `BACKPRESSURED` refusal with no delivery-state side effects, FIFO admission, explicit retryability, and deterministic bounded replay.
- Corrected the Gate 7 replay cascade: handler publications caused by replay inherit non-journaling mode, and every publication reaches drain-owned cancellation admission.
- Eliminated Git observer command-execution vectors by using one canonical absolute, project-external Git executable for filter-free index metadata and explicitly disabling tracked-worktree diff observation.
- Synchronized canonical and compact current-state guidance with PR #3 merge commit `52987f2`, Gate 6 Integrated, Gate 7 Specified - Next, and backpressure as the next separately activated increment.

- Implemented the sixth Delivery Gate 7 increment: Contract Verified run-to-completion delivery ordering on `InProcessMessageBus` — a pending queue and a single drain loop replace nested dispatch, so a publication made from inside a handler is queued and reports the scope-level `ENQUEUED` status while the draining top-level `publish` or `replay` returns the whole ordered cascade; delivery order equals publication order, no subscriber observes an effect before its cause, admission and journaling happen in the drain loop so the journal's order is the bus's delivery order, a correlation cancelled mid-cascade refuses entries queued behind it while an in-flight fan-out stays atomic, and an `Error` abandons the cascade entirely. The ordering defect was proven real behaviourally before the fix.
- Implemented the fifth Delivery Gate 7 increment: Contract Verified correlation-scoped cancellation propagation on `InProcessMessageBus` — `cancel(correlationId)` is idempotent and monotonic with no resume, and a cancelled correlation is refused admission before subscription lookup, idempotency, and dispatch on every path (publish, replay, and dead-letter re-delivery), reporting a scope-level `CANCELLED` outcome that names no subscription, invoking no handler, consuming no idempotency key, creating no dead letter, and appending nothing to the journal so fresh-bus replay stays deterministic; cancellation dominates both `UNROUTED` and `DUPLICATE`, and the bus reads no payload to decide delivery, keeping `ControlSignal.CANCEL` a consumer semantic.
- Implemented the fourth Delivery Gate 7 increment: Contract Verified bounded synchronous retry and explicit dead-letter re-delivery on `InProcessMessageBus` — an immutable `RetryPolicy` (1-10 attempts; the default bus keeps a single attempt) retries a failing handler immediately with no delay before dead-lettering it with its failed attempt count (`DeadLetter.attempts`), and `redeliver` accepts only a currently recorded dead letter, resolves it on success, and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason, never appending to the journal or releasing the consumed idempotency key.
- Implemented the third Delivery Gate 7 increment: Contract Verified delivery-failure isolation and dead-letter capture on `InProcessMessageBus` — a subscriber handler that throws yields a `FAILED` outcome and an ordered immutable `DeadLetter` while fan-out continues, and a failed delivery consumes the idempotency key and is terminal (no automatic retry), reporting `DUPLICATE` with no further dead letter on re-publish or replay.
- Implemented the second Delivery Gate 7 increment: the Contract Verified `InProcessMessageBus` under `com.enhancer.bus` — synchronous single-threaded deterministic topic fan-out and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; envelopes are carried unmutated so authorization and provenance survive every hop.
- Fixed a pre-existing wall-clock-dependent defect (unrelated to the delivery increment) in `RunRecordMetadataCollectorTest`: it hardcoded the observation time to `10:01 UTC` while `persist()` stamps `storedAt` with `Instant.now()`, so it failed whenever the wall clock passed that time; the observation time is now derived from the run clock. Recorded as a separate accepted decision.
- Implemented the first Delivery Gate 7 increment: the Contract Verified `MessageEnvelope` under `com.enhancer.bus` — versioned reference-only envelopes with canonical identities and the sealed four-kind payload hierarchy carrying authorization and snapshot references as data.
- Executed the user-approved Gate 6 re-scope and promotion: editor-dependent observations moved to Gate 12, Gate 6 promoted to Integrated, the `Specified - Next` marker advanced to Gate 7, and the two actual-roadmap test contracts updated in the same change.
- Completed the Gate 6 maturity assessment: every scope item and exit criterion mapped to fresh evidence or its later-gate blocker, with the re-scope-and-promote recommendation (Option B) recorded pending explicit user approval; documentation-only, gate status unchanged.
- Implemented the fourteenth Delivery Gate 6 increment: `GitWorkspaceCollector` under explicitly granted read-only external command authority — two fixed git invocations, digest-only retention, discovery confined to the project root, watchdog timeout, and explicit `UNAVAILABLE` on every failure; a real discovery-ceiling defect and a piped-stderr hang were caught and fixed during GREEN.
- Implemented the thirteenth Delivery Gate 6 increment: `TargetFileMetadataCollector` observing the run target pre-run with a streamed containment-checked digest, missing targets explicit `UNAVAILABLE`, and containment violations rejected as usage errors before execution.
- Implemented the twelfth Delivery Gate 6 increment: `WorkspaceAuthorityBoundaryIntegrationTest`, pinning on first run that adversarial tool-grant text in observed documents cannot widen task or policy scope, appear in bounded output, or mutate any repository document.
- Implemented the eleventh Delivery Gate 6 increment: the `Justified By` task-document reference grammar and `TaskJustificationProjector`, projecting explicit task-to-decision references into `JUSTIFIED_BY` edges with strict rejection of malformed or unresolved references, merged into the production graph with an `impactDecisions` count; the first real reference resolved on the actual repository.
- Completed the Gate 6 sub-capability integration promotion audit: all six Contract Verified sub-capabilities promoted to Integrated against named, fresh-run, pre-existing integration evidence, with no production or test code change and Gate 6 preserved as `Specified - Next`.
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
- `gate-6-production-graph-composition` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-task-justification-references` (published through `0e2be2c`), `gate-6-authority-boundary-evidence`, and `gate-6-target-file-observation` are Completed; their records are preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- `gate-6-git-workspace-adapter` is Completed and published through `21e6230`; `gate-6-maturity-assessment` is Completed with its record preserved in `PROJECT_STATE.md`.
- `gate-6-rescope-and-promotion` is Completed; its record is preserved in `CHANGELOG.md` and `PROJECT_STATE.md`.
- PR #3 published bounded retry/re-delivery, cancellation propagation, and delivery ordering through `52987f2`; replay/Git corrections followed through `2585a10`, and backpressure plus the four reliability/security corrections are published through `b3be720`, where local `main` and `origin/main` pointed before this uncommitted task.
- The Gate 7 transport-neutral IPC contract, maturity assessment, bounded work-payload correction, Contract Verified promotion, and Gate 8 next-marker synchronization are published through `16c7f5d`.
- The current uncommitted work implements and verifies the Gate 8 `WorkItem` admission contract and the named Gate 7 integration-preparation path and has no commit or push authority.
- The Gate 7 in-process delivery surface and its delivery-failure and dead-letter handling are committed and published on `origin/main` through delivery commit `b278c53`; the unrelated wall-clock test correction is published through `2a69182`.
- Local build note: this host had no JDK, so Java 17 was provisioned by junctioning `C:/Users/dokan/.jdks/corretto-17.0.14` into the Git-ignored `.tools/jdk17-runtime`; `scripts/gradle.ps1` then works normally.
- The maturity assessment, the re-scope-and-promotion, and the Gate 7 envelope contract are committed and published on `origin/main` through delivery commit `3423201`.
- The authority-boundary, target-file, and Git-adapter increments are committed and published on `origin/main` through delivery commit `21e6230`.
- The external read-only command authority for the Git adapter was explicitly granted by the user on 2026-07-15 ("3번 승인할게") and is scoped to `GitWorkspaceCollector` by accepted decision.
- The actual-repository evidence runs persisted records under the Git-ignored `.enhancer/run-records` directory, most recently `run-record/4f0d3da1-e8a8-412f-a513-79338d47b2b7`.
- The first five Gate 6 increments (view, collector, production composition, graph contract, impact query) are published on `origin/main` through delivery commit `d3b6197`.
- The four later increments (run-evidence producer, decision projection, run-record observation with store listing, production graph composition) are committed and published on `origin/main` through delivery commit `396665b`.
- The actual-repository evidence runs persisted `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` and `run-record/69977403-1cfb-45ba-ba0f-9239ad26a8c1` under the Git-ignored `.enhancer/run-records` directory.
- The actual-repository evidence run persisted `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf` under the Git-ignored `.enhancer/run-records` directory.

## Fresh Verification

- Product-track documentation: 17 sequential Delivery Gates, one Gate 8 `Specified - Next` marker, four canonical journeys, one resolved accepted decision, seven explicit-denominator metric definitions, consistent Scheduler/interface/security language, and no Constitution diff.
- Product-track actual-document self-hosting: 15 of 16 Context Reader, Planner, and Assisted Loop tests passed with 1 existing Windows symbolic-link setup skip and no failure or error.
- Runtime-path focused verification: 42 of 42 tests passed across `MessagingRuntimeIntegrationTest`, `WorkItemTest`, and all Gate 7 bus suites with no skips, failures, or errors.
- Runtime-path full result: 47 suites, 208 tests, 206 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 production compilation of all 122 sources passed with `-Xlint:all -Werror`.
- The resumed worktree already contained both production adapters, so no missing-type RED is claimed for the integration-preparation increment.
- Post-document self-hosting passed 15 of 16 actual-document Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- WorkItem RED: production compilation passed and test compilation failed with exactly 11 errors naming only the absent `WorkItem` contract.
- WorkItem focused GREEN: 41 of 41 tests passed across `WorkItemTest` and all Gate 7 bus suites with no skips, failures, or errors.
- Current full result: 46 suites, 207 tests, 205 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 production compilation of all 120 sources passed with `-Xlint:all -Werror`.
- Post-document self-hosting verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and both proposal paths retaining Gate 8.
- Promotion assessment focused evidence: all 39 Gate 7 bus tests passed with no skips, failures, or errors.
- Promotion marker RED: the first Planner/Assisted Loop run after moving the marker ran 8 tests and failed exactly the actual-Roadmap Planner assertion because it still expected Gate 7; the expectation-only correction restored all 8 tests.
- Work-payload boundary RED: the new test failed behaviorally only because 257 valid unique names were accepted; the exact 256-name boundary and all existing production compilation remained valid.
- Work-payload focused GREEN: all 39 bus tests passed with no skips, failures, or errors (`InProcessMessageBusTest` 30, `MessageEnvelopeTest` 5, `MessageTransportTest` 4).
- IPC contract RED: focused compilation failed with 33 expected errors naming only the absent `TransportMessage`, `MessageTransport`, `TransportOutcome`, and `TransportStatus`; production compilation passed.
- IPC contract focused GREEN: 38 tests across `MessageTransportTest`, `MessageEnvelopeTest`, and `InProcessMessageBusTest` passed with no skips, failures, or errors.
- Backpressure RED: focused compilation failed with exactly 10 expected errors naming only the absent `BackpressurePolicy`, `BACKPRESSURED`, and combined constructor; production compilation passed.
- Backpressure focused GREEN: 30 `InProcessMessageBusTest` tests and 4 `MessageEnvelopeTest` tests passed with no skips, failures, or errors.
- Ordering RED, first pass: 8 expected errors naming only the absent `DeliveryStatus.ENQUEUED` constant and `isScopeLevel()` accessor, with production compilation passing and no non-bus error.
- Ordering RED, second pass: after adding only those two symbols so the suite could run, three focused tests failed behaviourally against the real defect — two observed `[first, child, second]` where `[first, second, child]` was required, proving a cascaded child was delivered inside its parent's fan-out, and one observed `DELIVERED` where `ENQUEUED` was required; classified as aligned missing implementation.
- Ordering focused GREEN: `InProcessMessageBusTest` passed 25 tests (20 prior plus 5 new) and `MessageEnvelopeTest` 4 with no skips, failures, or errors, confirmed against fresh XML.
- Current full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Current full result: 45 suites, 205 tests, 203 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning; Java 17 production compilation of all 119 sources passed with `-Xlint:all -Werror`.
- Post-promotion actual-document verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and selected Gate 8 on both proposal paths.
- Post-document self-hosting verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Earlier cancellation RED: 19 expected errors naming only the absent `CANCELLED` constant and the `cancel`/`isCancelled` operations; focused GREEN passed 20 `InProcessMessageBusTest` tests and the full 176-test regression.
- Earlier retry/re-delivery RED: 16 expected errors naming only the absent `RetryPolicy` type, policy constructor, `redeliver` operation, `DeadLetter.attempts()` accessor, and five-component `DeadLetter` constructor; focused GREEN passed 15 `InProcessMessageBusTest` tests and the full 171-test regression.
- Earlier failure/dead-letter RED: 8 expected errors naming only the absent `DeliveryStatus.FAILED` constant, `DeadLetter` type, and `deadLetters()` accessor with no non-bus error; focused GREEN passed 10 `InProcessMessageBusTest` tests.
- Earlier session full result: 44 suites, 166 tests, 164 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors over 113 production sources.
- Structural: exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- Earlier delivery RED: 54 expected missing-symbol errors naming only the five absent delivery types; focused GREEN passed 11 bus tests.
- Earlier envelope RED: 38 expected missing-symbol errors (after replacing a Java 17 preview switch pattern in the test); focused GREEN passed 4 bus tests.
- Rescope verification: focused planner and Assisted Loop suites passed 8 tests against the actual roadmap proposing Gate 7; the marker moved with exactly one `Specified - Next` remaining.
- Git adapter RED: 6 expected missing-symbol errors; GREEN caught and fixed the discovery-ceiling defect; focused GREEN passed 21 suites and 62 tests.
- Target-file RED: 4 expected missing-symbol errors; focused GREEN passed 20 suites and 59 tests.
- Boundary characterization passed on first run (2 tests) with no production change.
- Earlier session full result: 42 suites, 152 tests, 150 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Actual repository `run`: task `gate-6-git-workspace-adapter`, exit code 0, `COMPLETED`, `VERIFIED`, `workspaceObservations=23` (15 documents, 5 prior run records, 1 target file, 2 `AVAILABLE` Git observations), `graphNodes=67`, `graphDecisions=49`.
- Earlier: the justification run resolved its own `Justified By` reference (`impactDecisions=1`, `graphDecisions=46`).
- Promotion audit: fresh focused verification across workspace, brain, run, CLI, and integration suites passed 19 suites and 59 tests with no skips, failures, or errors; each named connecting suite passed individually.
- Promotion audit full regression: 38 suites, 140 tests, 138 passed, 2 Windows symbolic-link setup skips, 0 failures, 0 errors; the audit diff was documentation-only.
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
- Earlier session full result: 38 suites, 140 tests, 138 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
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

- Product Journey and Evaluation Track: Accepted specification only; no journey or evaluation harness is claimed Contract Verified, Integrated, Operational, or Released.
- Gate 0: Integrated.
- Gates 1 through 4: Integrated.
- Gate 5: Operational for one governed read-only local CLI scenario.
- Gate 6: Integrated by the user-approved re-scope-and-promotion decision; the production view and graph composition remain Operational sub-capabilities.
- Gate 7: Contract Verified. One named real Workspace-to-bus-to-WorkItem path is Integrated and supplies promotion evidence, but gate-level Integrated maturity still requires a separate assessment; durable messaging, a concrete adapter, and a supported production entry point do not exist.
- Gate 8: Specified - Next; immutable `WorkItem` admission is Contract Verified, while Scheduler queues, durable Goal/AgentRun state, leases, recovery, and workers do not yet exist.
- Gate 6 repository-memory path (real governed run -> real memory -> collector -> composed view with divergence detection): Integrated.
- Gate 6 production composition: Operational for the governed read-only CLI scenario; every recorded `run` reports bounded snapshot identity, observation count, and memory freshness.
- Gate 6 `WorkspaceSnapshot`, `ProjectBrainView`, graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector`: Integrated through the fresh promotion audit against named pre-existing integration evidence.
- Gate 6 `TaskJustificationProjector` and the `Justified By` reference grammar: Integrated; the first real reference resolved on the actual repository.
- Gate 6 `TargetFileMetadataCollector` and `GitWorkspaceCollector`: Integrated on the production CLI path; Git observation runs under explicitly granted, decision-scoped read-only external command authority with inherited Git overrides removed and external diff/text-conversion helpers disabled.
- Gate 6 authority-boundary exit criterion: pinned by `WorkspaceAuthorityBoundaryIntegrationTest`.
- Gate 6 repository-memory and run-evidence production paths: Integrated.
- Gate 6 production view and graph composition: Operational for the governed read-only CLI scenario; impact answers carry executions and explicitly justified decisions, and modifies/verified-by producers do not exist.
- Gate 0 integration proves planning, explicit external activation, verified execution, persistence, and replay without changing Proposal authority.
- Remaining editor/diagnostic Workspace adapters, LLM invocation, production Event/Message Bus wiring, concrete IPC adapters, Scheduler, broader Agent Runtime, MCP, Skills, plugins, multi-agent execution, background execution, and release packaging remain unimplemented.

## Next Task

Run a separate fresh Gate 7 Integrated maturity assessment against every scope item and exit criterion, including the named runtime-path evidence; do not promote the gate automatically.

## Remaining Risks

- The CLI trusts an externally supplied expected digest; its origin is explicit and auditable but not signed.
- Evidence and RunRecord envelopes detect corruption but are not encrypted, signed, remotely replicated, or automatically cleaned up.
- The CLI uses the existing 64 MiB per-artifact/in-memory ceiling, five-second Tool timeout, five-iteration loop ceiling, and three-transition stagnation threshold. Evidence has no time-based retention or automatic cleanup contract.
- Two privilege-dependent symbolic-link containment tests are skipped on this Windows host; two Windows junction tests now execute and pass against the same production real-path guards.
- Gradle remains at Wrapper 8.4. The known Gradle 9 test-runtime deprecation is removed, but an actual major Wrapper upgrade requires a separate compatibility task.
- Gate 5 is a bootstrap CLI, not the future multi-interface control surface planned for Gate 12.

## Instructions For Next Agent

1. Read `.ai/` and every canonical startup document in repository order.
2. Confirm Gate 7 is `Contract Verified`, Gate 8 is the sole `Specified - Next` gate, and `CURRENT_TASK.md` records the runtime integration preparation as Completed.
3. Inspect `git status --short` and the current `main`/`origin/main` log rather than relying on the published base hash `16c7f5d`.
4. If the host has no JDK, provision Java 17 through `.tools/jdk17` (this host already has `jdk-17.0.19+10` there) or run `scripts/setup-dev.ps1`; `scripts/gradle.ps1` then works normally.
5. The only external command authority is the decision-scoped read-only Git adapter; any new external command capability requires its own explicit user approval.
6. Run the separate Gate 7 Integrated maturity assessment before changing its status; preserve single-agent sequential execution and defer multi-agent concurrency.
7. Do not commit or push the uncommitted WorkItem and runtime-path integration work without a new explicit user request.
