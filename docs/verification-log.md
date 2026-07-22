# Verification Log

Append-only evidence behind the state claimed in `PROJECT_STATE.md`: per-increment
RED/GREEN results, focused and full regression counts, lint status, and promotion
outcomes.

`PROJECT_STATE.md` holds current verified state and the maturity judgment that rests
on it. This file holds the evidence that judgment was formed from. Entries are in
append order, are written once, and are never revised afterwards. A superseded entry
stays as written; the current position is read from `PROJECT_STATE.md`, not from here.

## Gate 1 Verification

- Test-first RED: Gate 1 tests initially failed compilation because the production types did not exist.
- Focused command: `.\scripts\gradle.ps1 cleanTest test --tests "com.enhancer.tool.*"`.
- Focused result: build successful; 21 tests discovered, 20 passed, 1 skipped, 0 failures, 0 errors.
- Full command: `.\scripts\gradle.ps1 cleanTest test`.
- Full result: build successful; 10 suites, 38 tests, 37 passed, 1 skipped, 0 failures, 0 errors.
- The skipped test creates an escaping symbolic link. This Windows host denied link creation; the test remains active and exercises the real-path containment behavior on hosts with link permission.
- Traversal, absolute-path, malformed-path, directory, missing-file, oversized-file, invalid-UTF-8, policy, cancellation, timeout, exception, and invalid-result cases passed locally.
- `ReadFileTool` performs no write operation; mutation, shell, Git, network, and LLM behavior remain outside Gate 1.

## Self-Hosting Compatibility Recovery

- Test-first RED produced 7 expected focused failures against the old context and Planner behavior.
- Focused Context, Planner, and Assisted Loop verification: 3 suites, 12 tests, all passed with no skips.
- Actual repository context verification loaded 15 documents with `.ai/constitution.md` first.
- The actual Enhancer `AssistedDevelopmentLoop` selected `Delivery Gate 2: Evidence Persistence` and mapped its required capabilities and exit criteria.
- Full regression: 10 suites, 42 tests, 41 passed, 1 existing symbolic-link setup skip, 0 failures, and 0 errors.

## Gate 2 Verification

- Test-first RED: focused tests failed compilation with 44 missing Gate 2 symbols after the fixture encoding was corrected.
- Gate 2 focused verification: 4 suites, 9 tests, all passed with no skips.
- Complete Tool verification: 10 suites, 30 tests, 29 passed and 1 existing symbolic-link setup skip.
- Full regression: 14 suites, 51 tests, 50 passed, 1 existing symbolic-link setup skip, 0 failures, and 0 errors.
- Persistence and resolution were verified across separate `FileSystemEvidenceStore` instances using the same root.
- Atomic publication left no `.pending-*` artifact after successful writes.
- Corruption coverage includes invalid envelope, declared-length mismatch, SHA-256 mismatch, and invalid UTF-8.
- A large real file produced bounded `VerificationEvidence` plus a reference resolving to the complete original content.
- Retention cleanup is specified by policy but is not automatically executed.

## Gate 3 Verification

- Test-first RED: focused compilation failed with 33 missing Gate 3 symbols after correcting one unrelated test API assumption.
- Focused verification: 3 suites, 16 tests, all passed with no skips.
- Full regression after roadmap synchronization: 16 suites, 58 tests, 57 passed, 1 existing symbolic-link setup skip, 0 failures, and 0 errors.
- A governed temporary repository connects startup context, repository-derived approval, `ReadFileTool`, persisted evidence, and the Agent Loop transition in one integration test.
- Separate actual-Enhancer regressions verified the 15-document startup context and canonical Roadmap proposal path before Gate 5; Gate 5 now supplies the supported actual-worktree run.
- Real Tool success reaches `AWAITING_VERIFICATION`, not `COMPLETED`.
- Terminal failure reaches `FAILED`; repeated identical retryable results reach `STAGNATED` through the existing threshold.
- A denied fake Git mutation Tool records zero invocations and leaves its `.git/HEAD` sentinel unchanged.
- The controller cannot register Tools, create requests, approve work, or broaden the immutable execution policy.
- Hardening RED: 37 expected missing-symbol/API compilation errors against the prior Gate 3 implementation.
- Hardening focused verification: 6 suites, 24 tests, all passed with no skips.
- Hardening full regression after final document synchronization: 17 suites, 63 tests, 62 passed, 1 existing symbolic-link setup skip, 0 failures, and 0 errors.
- Active repository context now produces the exact structured approval consumed by the run, and out-of-scope Tool requests are rejected before execution.
- Identical content with changing summary and evidence reference reaches `STAGNATED` through stable content identity.
- Tool result status/failure-code consistency and non-public run-state construction are executable invariants.
- Historical Gate 3 expanded-roadmap self-hosting verification: Planner and Assisted Development Loop suites passed 8 of 8 tests and selected Gate 4 at that time.
- Structural review found sequential Delivery Gates 0 through 16 and exactly one `Specified - Next` marker.

## Pre-Operational Foundation Hardening Verification

- During that historical task, the change remained hardening over Integrated Delivery Gates 1 through 4 and Gate 5 remained the sole `Specified - Next` capability.
- Focused RED verification ran 28 tests and produced the 7 expected failures for timeout starvation, duration representation, Evidence timestamp tampering, RunRecord timestamp tampering and malformed Unicode, startup-document size, and read evidence-capability classification; 2 Windows symbolic-link setup tests were skipped.
- Focused GREEN verification ran the same 28 tests: 26 passed, 2 symbolic-link setup tests skipped, 0 failures, and 0 errors.
- Final full command: `.\scripts\gradle.ps1 --no-daemon cleanTest test --warning-mode all`.
- Final full result: 21 suites, 90 tests, 88 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- The final Gradle output contained no automatic test-framework implementation dependency deprecation; `testRuntimeClasspath` contains `junit-platform-launcher:1.10.2` through the JUnit 5.10.2 BOM.
- The standard full suite passed without `JAVA_TOOL_OPTIONS`; an explicit `-PtestTmpDir=build/tmp/junit-override` test invocation also passed.
- Java 17 production lint compiled all 64 source files with `javac -Xlint:all -Werror` and no warning or error.
- `git diff --check` passed.

## Gate 4 Verification

- Test-first RED: focused compilation failed with 87 missing Gate 4 production symbols.
- Initial Gate 4 focused verification: 3 suites, 12 tests, all passed with no skips.
- Hardened focused verification including terminal request retention and failed/limited run records: 4 suites, 19 tests, all passed with no skips.
- Gate 4 plus self-hosting Planner verification after roadmap promotion: 6 suites, 27 tests, all passed with no skips.
- Final full regression after documentation synchronization: 20 suites, 77 tests, 76 passed, 1 existing symbolic-link setup skip, 0 failures, and 0 errors.
- Real `ReadFileTool` output flows through evidence persistence, independent digest verification, verified-only completion, atomic RunRecord persistence, and replay through a new store instance.
- Expected-content mismatch, missing evidence, corrupted evidence, and RunRecord persistence failure cannot return completion.
- Failed, stagnated, and maximum-iteration worker runs remain non-completed and are recorded with verification Not Performed.
- RunRecord replay preserves the external expected digest as well as result and decision evidence.
- Gate 4 hardening policy-binding RED failed compilation with 15 expected missing API errors.
- Gate 4 hardening lifecycle RED ran 16 tests with 4 expected failures, and result-construction RED ran 1 test with 1 expected failure.
- Hardened focused verification passed 24 of 24 tests across 5 suites.
- Hardened full regression passed 81 of 82 tests across 21 suites with 1 existing symbolic-link setup skip, 0 failures, and 0 errors.

## Gate 5 Verification

- Test-first RED: focused CLI compilation failed with 45 expected missing-symbol errors before production CLI types existed.
- Focused command: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.cli.*'`.
- Focused result: 3 suites, 7 tests, 0 failures, 0 errors, and 0 skips.
- The temporary-project integration covers verified completion, bounded output without complete evidence, durable replay, digest mismatch, task mismatch, and persisted/replayed Tool failure.
- Full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Full result: 24 suites, 97 tests, 95 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors; no Gradle deprecation was reported.
- Java 17 production lint passed with `-Xlint:all -Werror` and no warning or error.
- Actual-repository smoke read `README.md` under active task `gate-5-first-operational-cli`, independently verified the externally computed digest, and returned `COMPLETED`, exit code `0`, and RunRecord `run-record/0ff7b4ea-5d4a-4698-b9f7-642169262737`.
- The `.enhancer/` record survived a subsequent Gradle `clean`; restart-safe replay returned the same task, allowed policy, worker verification wait, final completion, Verified decision, and one iteration without re-executing the Tool.

## Gate-By-Gate Focused Verification

- Gate 0 contracts: 6 suites, 35 tests, 34 passed, 1 Windows symbolic-link setup skip, 0 failures, and 0 errors.
- Gate 1 Tool boundary: 4 suites, 15 tests, 14 passed, 1 Windows symbolic-link setup skip, 0 failures, and 0 errors.
- Gate 2 evidence persistence: 4 suites, 10 tests, all passed with no skips.
- Gate 3 Agent/Tool integration: 3 suites, 9 tests, all passed with no skips.
- Gate 4 verification and RunRecord: 4 suites, 21 tests, all passed with no skips.
- Gate 5 CLI: 3 suites, 7 tests, all passed with no skips, followed by actual-repository run and replay success.
- Gates are dependency-ordered maturity increments, not six independent applications: Gate 0, Gate 1, and Gate 2 contracts can be exercised in isolation, while Gate 3 composes Gates 0 through 2, Gate 4 composes the worker path with verification and records, and Gate 5 is the supported cumulative entry point.

## Gate 0 Integration Promotion Verification

- `FoundationLifecycleIntegrationTest` was added before any production edit and passed on its first characterization run.
- Planning over a governed temporary repository produced the Gate 6 Proposal while byte-for-byte preserving all required documents.
- CLI execution before explicit activation returned usage/configuration failure and created neither evidence nor RunRecord storage.
- Only external test-fixture setup created an active ApprovedTask; no production component converted the Proposal into authority.
- The activated task reused Gate 5 and Gate 1 through 4 through large complete evidence, independent verification, Verified-only completion, durable RunRecord resolution, target deletion, and replay without Tool re-execution.
- No production correction or second orchestration path was needed.
- Combined focused verification passed 43 tests across 10 suites with 1 Windows symbolic-link setup skip, 0 failures, and 0 errors.
- Full regression passed 98 tests across 25 suites: 96 passed, 2 Windows symbolic-link setup tests skipped, 0 failures, and 0 errors.
- Gradle `--warning-mode all` emitted no deprecation warning, and Java 17 production lint passed with `-Xlint:all -Werror`.
- Gate 6 remains the sole `Specified - Next` marker and `git diff --check` passed.

## Gate 6 WorkspaceSnapshot Contract Verification

- Test-first RED: focused Workspace compilation failed with 79 expected missing-symbol errors before production contracts existed.
- Focused GREEN: 3 suites, 10 tests, all passed with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 28 suites, 108 tests, 106 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror`.
- After task completion and document synchronization, focused Context Reader, Planner, and Assisted Loop self-hosting verification passed 14 of 15 tests with 1 existing Windows symbolic-link setup skip and selected Gate 6 as the next proposal boundary.
- Contract tests cover bounds, state invariants, temporal consistency, collection immutability, deterministic order-independent identity, field sensitivity, and duplicate rejection.
- The result promotes only the Workspace snapshot sub-capability to Contract Verified. Gate 6 remains `Specified - Next` pending a read-only Project Brain consumer.

## Gate 6 ProjectBrainView Verification

- Test-first RED: focused Project Brain compilation failed with 19 expected missing-symbol errors naming only `ProjectBrainView`, `RepositoryMemoryEntry`, `RunProvenance`, and `MemoryFreshness`; no error came from the existing Workspace, Context, or RunRecord contracts.
- Focused GREEN: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.brain.*' --tests 'com.enhancer.workspace.*'` passed 4 suites and 15 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 29 suites, 113 tests, 111 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- The 2 skips are `ProjectContextReaderTest.rejectsARequiredDocumentSymbolicLinkOutsideTheRealProjectRoot` and `ReadFileToolIntegrationTest.rejectsASymbolicLinkThatEscapesTheRealProjectRoot`, which need a link-creation privilege this Windows host lacks.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror` on javac 17.0.19.
- Tests cover composition without content, derived memory freshness across matched/diverged/unobserved documents, pass-through of Available/Stale/Unavailable states, approved-task mismatch rejection for both task identity and source document, null rejection, and collection immutability.
- The result promotes only the `ProjectBrainView` sub-capability to Contract Verified. Gate 6 remains `Specified - Next`: no adapter collects a snapshot from live sources, no production path composes the view, and the graph projections and impact query in the Gate 6 scope are not implemented.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Gate 6 Repository Memory Collection Verification

- Test-first RED: the focused compile failed with 6 expected missing-symbol errors, all naming only the absent `RepositoryMemorySnapshotCollector`.
- Focused GREEN: Workspace, Project Brain, and integration suites passed 7 suites and 20 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 31 suites, 117 tests, 115 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror`.
- `WorkspaceCollectionIntegrationTest` connected a real governed CLI run, the real persisted RunRecord, the real Context Reader, the collector, and the composed view: all 15 documents were `SNAPSHOT_MATCHED`, run provenance was `VERIFIED` under the matching task, and editing the active task document made exactly `CURRENT_TASK.md` report `SNAPSHOT_DIVERGED` against the earlier snapshot.
- The result promotes the repository-memory collection-to-view path to Integrated. Gate 6 remains `Specified - Next`: no production caller composes the view during an actual repository run, and Git, diagnostics, selection, and terminal adapters plus graph projections and the impact query are not implemented.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Gate 6 Production Composition Verification

- Test-first RED: both focused CLI composition tests failed with the expected missing-output assertion (`output does not contain workspaceSnapshotId=`) while the underlying runs still completed and were recorded; no other behavior changed.
- Focused GREEN: CLI, Workspace, Project Brain, and integration suites passed 11 suites and 29 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 32 suites, 119 tests, 117 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror`.
- Actual repository `run`: `README.md` under active task `gate-6-production-brain-composition` returned exit code 0, `COMPLETED`, `VERIFIED`, one iteration, RunRecord `run-record/ca604c7c-23e8-4b1c-8aa2-38fb6bfed5cf`, `workspaceSnapshotId=b729514d272701e8f46d32b282f24570a75147470a6b82c0bd21bb0e97e9f39f`, `workspaceObservations=15`, and `memoryFreshness=matched=15,diverged=0,notObserved=0`.
- Actual repository `replay` of the same record returned exit code 0 with unchanged replay output; the RunRecord does not store the snapshot identity by accepted decision.
- The composition covers completed and failed-but-recorded outcomes; freshness is trivially all-matched on this path because the same loaded memory is both snapshot source and comparison input.
- The result promotes the production repository-memory composition to Operational for the governed read-only CLI scenario. Gate 6 remains `Specified - Next` pending its remaining adapters, graph projections, and impact query.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Gate 6 Graph Projection Contract Verification

- Test-first RED: the first focused compile failed with 100 expected missing-symbol errors naming only the seven intentionally absent graph types.
- Focused GREEN: the Project Brain suites passed 2 suites and 9 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 33 suites, 123 tests, 121 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror`.
- Contract tests cover deterministic order-independent projection, collection immutability, endpoint-kind acceptance and rejection for all six edge kinds, duplicate-node, duplicate-edge, self-loop, unknown-endpoint, snapshot-identity, and bound rejection, and freshness/revision provenance invariants with derived rebuild status.
- The result promotes only the graph projection contract to Contract Verified. Gate 6 remains `Specified - Next`: no producer, impact query, traversal, or persistence exists, and no end-to-end graph evidence was claimed.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Gate 6 Task Impact Query Verification

- Test-first RED: the first focused compile failed with 9 expected missing-symbol errors naming only the absent `TaskImpactQuery` and `TaskImpact`.
- Focused GREEN: the Project Brain suites passed 3 suites and 13 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 34 suites, 127 tests, 125 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror`.
- Query tests cover the full deterministic chain with shared-test deduplication and unrelated-edge exclusion, rebuild-status derivation from stale nodes and stale edges with unrelated staleness ignored, empty-result behavior, result immutability, and null/unknown/non-task rejection.
- The result promotes only the impact query to Contract Verified against contract-constructed graphs. Gate 6 remains `Specified - Next`: no producer projects real repository evidence and no persistence exists.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Gate 7 Message Envelope Contract Verification

- Test-first RED: after replacing a Java 17 preview switch pattern in the test with an instanceof/sealedness check, the focused compile failed with 38 expected missing-symbol errors naming only the seven intentionally absent bus types.
- Focused GREEN: the bus suite passed 4 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 43 suites, 156 tests, 154 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Contract tests cover identity carriage, exact four-kind sealedness via `getPermittedSubclasses`, immutable authorization data with empty-scope and invalid-digest rejection, and invalid-identity/self-causation rejection.
- The result promotes only the envelope contract to Contract Verified. Gate 7 remains `Specified - Next`: no envelope has crossed a real hop because no delivery, topic, queue, or transport exists.

## Gate 7 In-Process Delivery Verification

- Test-first RED: the focused test compile failed with 54 expected missing-symbol errors naming only the five intentionally absent delivery types (`DeliveryDestination`, `DeliveryOutcome`, `DeliveryStatus`, `InProcessMessageBus`, `JournaledMessage`); no error came from any non-bus file, classifying it as aligned missing implementation.
- Focused GREEN: the bus suite passed 11 tests (`InProcessMessageBusTest` 7, `MessageEnvelopeTest` 4) with no skips, failures, or errors, confirmed against fresh XML.
- Full regression with `--warning-mode all`: 44 suites, 163 tests, 161 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning and Java 17 production compilation of all 112 sources passed with `-Xlint:all -Werror`.
- Contract tests cover topic fan-out order, single-consumer queue delivery with second-consumer rejection, unrouted reporting, per-message idempotency, deterministic journal replay reproducing outcomes on a fresh bus without duplicate side effects, authorization and provenance survival across the hop, and destination/subscriber/journal invariants.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- The result promotes the in-process delivery surface to Contract Verified. Gate 7 remains `Specified - Next`: no production caller wires the bus into the CLI or Agent Loop, and no transport exists, so no envelope crossed a real process boundary.
- Separate correction: `RunRecordMetadataCollectorTest` was made time-independent (observation time derived from the run clock rather than a hardcoded instant), fixing a pre-existing wall-clock-dependent failure unrelated to this increment.

## Gate 7 Delivery Failure And Dead-Letter Verification

- Test-first RED: the focused test compile failed with 8 expected errors naming only the intentionally absent `DeliveryStatus.FAILED` constant, `DeadLetter` type, and `deadLetters()` accessor; no error came from any non-bus file, classifying it as aligned missing implementation.
- Focused GREEN: the bus suite passed with `InProcessMessageBusTest` at 10 tests and no skips, failures, or errors, confirmed against fresh XML.
- Full regression with `--warning-mode all`: 44 suites, 166 tests, 164 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning and Java 17 production compilation of all 113 sources passed with `-Xlint:all -Werror`.
- Contract tests cover handler-failure isolation with continued fan-out and a captured dead letter, a failed delivery treated as idempotent and terminal without automatic retry, and ordered, immutable dead letters with a null-message reason falling back to the exception type.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- The result promotes delivery-failure isolation and dead-letter capture to Contract Verified. Gate 7 remains `Specified - Next`: no production caller wires the bus into the CLI or Agent Loop, so no failure has been dead-lettered on a real path.

## Gate 7 Bounded Retry And Re-Delivery Verification

- Test-first RED: the focused test compile failed with 16 expected errors naming only the intentionally absent `RetryPolicy` type, `InProcessMessageBus(RetryPolicy)` constructor, `redeliver` operation, `DeadLetter.attempts()` accessor, and five-component `DeadLetter` constructor; no error came from any non-bus file and production compilation passed, classifying it as aligned missing implementation.
- Focused GREEN: the bus suite passed with `InProcessMessageBusTest` at 15 tests (10 prior plus 5 new) and `MessageEnvelopeTest` at 4, with no skips, failures, or errors, confirmed against fresh XML.
- Full regression with `--warning-mode all`: 44 suites, 171 tests, 169 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning and Java 17 production compilation of all 114 sources passed with `-Xlint:all -Werror`.
- Contract tests cover in-policy retry success without a dead letter, policy exhaustion with the failed attempt count recorded and fan-out continuing, explicit re-delivery resolving the dead letter while the journal and consumed idempotency key stay untouched, a failed re-delivery accumulating attempts in place with the latest reason, and policy-bound, null, foreign-dead-letter, and attempt-count invariants.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- The result promotes bounded synchronous retry and explicit dead-letter re-delivery to Contract Verified. Gate 7 remains `Specified - Next`: no production caller wires the bus into the CLI or Agent Loop, so no re-delivery has recovered a failure on a real path.

## Gate 7 Cancellation Propagation Verification

- Test-first RED: the focused test compile failed with 19 expected errors naming only the intentionally absent `DeliveryStatus.CANCELLED` constant and the `cancel(String)`/`isCancelled(String)` operations; production compilation passed and no error came from any non-bus file, classifying it as aligned missing implementation.
- Focused GREEN: the bus suite passed with `InProcessMessageBusTest` at 20 tests (15 prior plus 5 new) and `MessageEnvelopeTest` at 4, with no skips, failures, or errors, confirmed against fresh XML.
- Full regression with `--warning-mode all`: 44 suites, 176 tests, 174 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning and Java 17 production compilation of all 114 sources passed with `-Xlint:all -Werror`.
- Contract tests cover a cancelled publication refused with no handler invocation, no dead letter, and nothing journaled; cancellation dominating both `UNROUTED` and an already-consumed `DUPLICATE` key; replay refusing a cancelled entry while a live correlation in the same journal still delivers; re-delivery refused with the dead-letter record retained; and idempotent, monotonic, correlation-scoped cancellation with null, blank, and `CANCELLED`-outcome-subscriberId invariants.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- The result promotes cancellation propagation to Contract Verified. Gate 7 remains `Specified - Next`: no production caller wires the bus into the CLI or Agent Loop, so no run has been cancelled on a real path.

## Gate 7 Delivery Ordering Verification

- Test-first RED, first pass: the focused test compile failed with 8 expected errors naming only the intentionally absent `DeliveryStatus.ENQUEUED` constant and `isScopeLevel()` accessor; production compilation passed and no error came from any non-bus file.
- Test-first RED, second pass: after adding only the two intentionally absent symbols so the suite could run, three focused tests failed behaviourally against the real defect rather than a missing type — `deliversACascadeOnlyAfterTheCurrentFanOutCompletes` and `refusesAQueuedPublicationCancelledDuringTheCascade` both observed `[first, child, second]` where `[first, second, child]` was required, proving a cascaded child really was delivered inside the parent's fan-out, and `reportsAReEntrantPublicationAsEnqueued` observed `DELIVERED` where `ENQUEUED` was required. Classified as aligned missing implementation.
- Focused GREEN: the bus suite passed with `InProcessMessageBusTest` at 25 tests (20 prior plus 5 new) and `MessageEnvelopeTest` at 4, with no skips, failures, or errors, confirmed against fresh XML.
- Full regression with `--warning-mode all`: 44 suites, 181 tests, 179 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning and Java 17 production compilation of all 114 sources passed with `-Xlint:all -Werror`.
- Contract tests cover a cascade delivered only after the current fan-out completes with the draining call reporting the whole cascade and the journal in admission order, a re-entrant publication reporting scope-level `ENQUEUED`, FIFO ordering of cascaded publications, a correlation cancelled mid-cascade refusing the queued child without journaling it while the in-flight fan-out completes atomically, and scope-level classification with `ENQUEUED`/`DELIVERED` subscriberId invariants.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed for tracked and newly added files.
- The result promotes run-to-completion delivery ordering to Contract Verified. Gate 7 remains `Specified - Next`: no production caller wires the bus into the CLI or Agent Loop, so no cascade has been ordered on a real path.

## Gate 6 Re-Scope And Promotion Verification

- The user approved the assessment's Option B recommendation on 2026-07-15; the accepted decision records the re-scope and its rationale.
- Roadmap changes: Gate 6 status Integrated with the evidence summary retained, the three editor-dependent observation items moved to Gate 12 as Workspace observation integrations, exit criteria annotated with the re-scope, Gate 7 status Specified - Next, and the Current Position updated.
- The two actual-roadmap test contracts were updated to Gate 7 in the same change (`RepositoryTaskPlannerTest`, `AssistedDevelopmentLoopTest`); no production code changed.
- Focused planner and Assisted Loop suites passed 8 tests against the actual repository roadmap with no skips, failures, or errors.
- Fresh full regression: 42 suites, 152 tests, 150 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Exactly one `Specified - Next` gate status marker remains, now at Gate 7; `git diff --check` passed.

## Gate 6 Maturity Assessment

Fresh supporting evidence for this assessment: 42 suites, 152 tests, 150 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors, with Java 17 `-Xlint:all -Werror` passing and no Gradle deprecation warning.

Scope-item disposition:

- Immutable `WorkspaceSnapshot` and source freshness metadata: Integrated (`WorkspaceCollectionIntegrationTest`, production CLI suites).
- Common immutable input-snapshot identity and approved task revision: Integrated; every production run reports the canonical snapshot identity.
- Repository files and documents: Integrated for the fifteen canonical documents and the run's target file; active and selected file context has no adapter because no editor integration exists before the Gate 12 interface work.
- Read-only Git status and diff adapters: Integrated at digest granularity under decision-scoped external command authority; per-file metadata would be a separate decision.
- Diagnostics and terminal-session metadata adapters: no adapter; no diagnostic provider or terminal integration exists before the Gate 8-12 runtime and interface work. The source kinds are already typed in the contract.
- Project Brain view with provenance: Operational on the production CLI path.
- Graph projection contracts for the five relationship domains: Integrated.
- Task-to-decision-to-code-to-test impact query: Integrated for the task, decision, and execution legs; the code and test legs return empty honestly because `MODIFIES`/`VERIFIED_BY` evidence sources require write and test Tools from later gates.

Exit-criterion disposition:

- "One snapshot can explain which files, Git state, diagnostics, selection, and documents informed a run": evidenced for documents, files, Git state, and run history (23 observations on the latest actual-repository run); diagnostics and selection are blocked on later-gate capabilities.
- "Stale and unavailable sources are explicit": evidenced; real `UNAVAILABLE` observations exist for corrupted records, non-repository roots, and missing targets, and graph elements carry real `STALE` freshness. Observation-level `STALE` awaits a re-observing adapter by design.
- "Workspace observations cannot override repository authority or grant Tool permission": evidenced by `WorkspaceAuthorityBoundaryIntegrationTest`.
- "Snapshot size and sensitive-data boundaries are enforced": evidenced by contract bounds, digest-only retention, and no-content output assertions.
- "Graph nodes and edges retain source, freshness, version, and rebuild status": evidenced by the graph contract and its tests.

Recommendation (requires explicit user approval; the gate status is unchanged by this assessment):

- Option A: keep Gate 6 `Specified - Next` until diagnostics, terminal, and selection adapters exist; those depend on Gate 8-12 capabilities, so the gate would stay open across several gates.
- Option B (recommended): re-scope by accepted decision — move diagnostics, terminal-session, and active/selected-file observation to the gates that own those capabilities, keeping the already-typed source kinds; declare Gate 6 Integrated as the Workspace and Project Brain foundation; advance `Specified - Next` to Gate 7 Event Bus and IPC Foundation.
- Stub adapters that observe nothing real are rejected as dishonest evidence.

## Gate 6 Git Workspace Adapter Verification

- Test-first RED: the focused compile failed with 6 expected missing-symbol errors naming only the absent `GitWorkspaceCollector`.
- GREEN exposed and fixed one real defect: without a discovery ceiling, a temporary non-repository directory inside this workspace observed the enclosing Enhancer repository as `AVAILABLE`; `GIT_CEILING_DIRECTORIES` confines observation to the project's own working tree. An initial piped-stderr design hung one suite run; discarding stderr, disabling fsmonitor, `--no-optional-locks`, and a watchdog eliminated the hang, which did not recur.
- Focused GREEN: workspace, CLI, brain, and integration suites passed 21 suites and 62 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 42 suites, 152 tests, 150 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Actual repository `run` on `README.md`: `workspaceObservations=23` (15 documents, 5 prior run records, 1 target file, 2 `AVAILABLE` Git observations), `graphNodes=67`, `graphDecisions=49`.
- The external command authority is scoped to this collector by accepted decision; the exit-criterion gap "which Git state informed a run" is closed at digest granularity, and per-file metadata remains a separate increment.

## Gate 6 Target File Observation Verification

- Test-first RED: the focused compile failed with 4 expected missing-symbol errors naming only the absent `TargetFileMetadataCollector`.
- Focused GREEN: workspace, CLI, brain, and integration suites passed 20 suites and 59 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 41 suites, 149 tests, 147 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Collector tests cover streamed-digest observation, missing-target `UNAVAILABLE` with reason, and rejection of absolute, traversal, non-regular, blank, and null inputs; containment violations surface as CLI usage errors before execution.
- Actual repository `run` on `README.md`: `workspaceObservations=20` (15 documents, 4 prior run records, 1 target file) and `graphNodes=65` including the target as an `ARTIFACT` node.
- The exit-criterion gap "which files informed a run" is closed for the run target; arbitrary-file coverage beyond the target remains future scope.

## Gate 6 Authority Boundary Evidence Verification

- Characterization, not test-first: `WorkspaceAuthorityBoundaryIntegrationTest` passed on its first run (2 tests), so no production correction was required.
- Adversarial `Allowed Tools` grant text in every observed non-task document did not widen the persisted approved task or policy scope beyond the task document's declared `read-file`, did not appear in bounded output, and no repository document changed by a byte across collection, composition, and the governed run.
- A task document allowing only `write-file` remained rejected as a configuration error with no records created.
- Full regression with `--warning-mode all`: 40 suites, 146 tests, 144 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- The exit criterion "Workspace observations cannot override repository authority or grant Tool permission" is now pinned by regression evidence.

## Gate 6 Task Justification Reference Verification

- Test-first RED: the focused compile failed with 6 expected missing-symbol errors naming only the absent `TaskJustificationProjector`.
- Focused GREEN: brain, CLI, workspace, and integration suites passed 18 suites and 54 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 39 suites, 144 tests, 142 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Projector tests cover reference resolution with task-document provenance, absent-section and unobserved-freshness behavior, empty/non-bullet/unresolved/duplicate rejection, null rejection, and a justifying decision surfacing in an impact answer.
- The production CLI test resolves a temp-project reference (`graphEdges=2`, `impactDecisions=1`), and the actual-repository run resolved this task's own reference: 18 observations, `graphNodes=63`, `graphDecisions=46`, `impactExecutions=1`, `impactDecisions=1`.
- The result records the justification reference path as Integrated on the production composition. Gate 6 remains `Specified - Next`: remaining adapters, modifies/verified-by producers, persistence, and full gate exit criteria are not evidenced.

## Gate 6 Sub-Capability Integration Promotion Verification

- The promotion audit changed no production or test code; the diff for this task contains documentation only.
- Fresh focused verification across the workspace, brain, run, CLI, and integration suites passed 19 suites and 59 tests with no skips, failures, or errors.
- The named connecting suites each passed fresh: `WorkspaceCollectionIntegrationTest` (1), `EnhancerCliBrainCompositionTest` (2), `EnhancerCliGraphCompositionTest` (1), `RunRecordMetadataCollectorTest` (5), `RunEvidenceGraphProducerTest` (3), `AcceptedDecisionProjectorTest` (4), and `TaskImpactQueryTest` (4).
- Fresh full regression with `--warning-mode all`: 38 suites, 140 tests, 138 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- All six audited sub-capabilities qualified: each is connected to real upstream and downstream components by evidence that predates the promotion task, so `WorkspaceSnapshot`, `ProjectBrainView`, the graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector` are Integrated.
- Gate 6 remains `Specified - Next`: the reference grammar, modifies/verified-by producers, remaining source adapters, and persistence are unimplemented, and gate-level exit criteria are not fully evidenced.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Gate 6 Production Graph Composition Verification

- Test-first RED: both focused CLI graph-composition tests failed with the expected `output does not contain graphDecisions=` assertion while the runs completed.
- Focused GREEN: CLI, workspace, brain, and integration suites passed 17 suites and 50 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 38 suites, 140 tests, 138 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- The second-run CLI test proved prior-record observation: `workspaceObservations` grew from 15 to 16 while graph counts stayed evidence-exact.
- Actual repository `run` on `README.md`: exit code 0, `COMPLETED`, `VERIFIED`, RunRecord `run-record/69977403-1cfb-45ba-ba0f-9239ad26a8c1`, snapshot `d5bd10cb...a44632`, 17 observations (15 documents plus 2 prior run records), `graphNodes=61`, `graphEdges=1`, `graphDecisions=44` matching the decision log's 44 `Status: Accepted Decision` lines, and `impactExecutions=1`.
- The result promotes the production graph composition to Operational for the governed read-only CLI scenario. Gate 6 remains `Specified - Next`: decisions are unlinked, modifies/verified-by producers and further adapters do not exist, and nothing is persisted beyond the RunRecord.

## Gate 6 Run Record Metadata Observation Verification

- Test-first RED: the focused compile failed with 8 expected missing-symbol errors naming only the absent `RunRecordMetadataCollector` and `references()` store method.
- One existing anonymous test `RunRecordStore` gained the new interface method; no other existing code changed behavior.
- Focused GREEN: workspace, run, and finalizer suites passed 8 suites and 33 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 37 suites, 139 tests, 137 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Tests cover ordered listing with non-record files ignored, envelope-digest observation metadata, corrupted-record `UNAVAILABLE` surfacing without a digest, missing/empty-root behavior, snapshot composition alongside repository documents, and null rejection.
- The result promotes only the run-record observation path to Contract Verified; the CLI does not yet include these observations.

## Gate 6 Accepted Decision Projection Verification

- Test-first RED: the focused compile failed with 6 expected missing-symbol errors naming only the absent `AcceptedDecisionProjector`.
- Focused GREEN: the Project Brain suites passed 5 suites and 20 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 36 suites, 134 tests, 132 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 lint passed with `-Xlint:all -Werror`.
- Projector tests cover document-order accepted-only parsing, matched/diverged/unobserved freshness, missing-document, duplicate-heading, and null rejection, and composition of projected nodes into a `ProjectBrainGraph`.
- The result promotes only the decision projection to Contract Verified; no `JUSTIFIED_BY` linkage exists because no document grammar evidences task-to-decision references.

## Gate 6 Run Evidence Graph Producer Verification

- Test-first RED: the first focused compile failed with 6 expected missing-symbol errors naming only the absent `RunEvidenceGraphProducer`.
- Focused GREEN: Project Brain and integration suites passed 6 suites and 18 tests with no skips, failures, or errors.
- Full regression with `--warning-mode all`: 35 suites, 130 tests, 128 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production lint passed with `-Xlint:all -Werror`.
- Producer tests cover snapshot-keyed identity, the one-to-one state-to-freshness mapping including Stale and Source-Missing observations, non-repository observation skipping, execution provenance from the stored envelope digest, evidence-only edge emission, task-mismatch rejection, and null rejection.
- The extended integration test flowed a real governed CLI run and really-collected snapshot through the producer into a `TaskImpactQuery` answer naming the real stored execution reference, with empty decision, modified-artifact, and verifying-artifact results.
- The result promotes the run-evidence production path to Integrated. Gate 6 remains `Specified - Next`: decision/modifies/verified-by producers, non-repository observation projection, persistence, and further adapters do not exist.
- Gate 6 remains the sole `Specified - Next` gate status marker and `git diff --check` passed.

## Vision Documentation Verification

- At the time of the vision review, the canonical Roadmap contained sequential Delivery Gates 0 through 16 and exactly one `Specified - Next` marker at Gate 5.
- V1, V2, V3 and Decision, Architecture, Dependency, Task, and Execution graph terms are present in Architecture and Roadmap.
- Planner and Assisted Development Loop self-hosting regression passed 8 of 8 tests after the vision and roadmap update.
- Full regression passed 62 of 63 tests with 1 existing Windows symbolic-link setup skip and no failures or errors.
- New Kernel, graph, workflow, marketplace, MCP, model-routing, and multi-agent capabilities remain Planned rather than implemented.

## Documentation Alignment Verification

- Gate 3 evidence now identifies the governed temporary-repository integration test separately from actual-Enhancer Context Reader and Roadmap Planner regressions.
- No document claims a supported full Agent run against the actual worktree before the Operational CLI gate.
- Current Java 17, Gradle 8.4 Wrapper, JUnit 5, Mockito, and Git usage is separated from conditional Spring Boot, local-model, CLI, and editor integrations.
- V1-V3 product outcomes are explicitly separate from dependency-ordered Delivery Gates.
- Self-hosting development is explicitly separate from local or hybrid model execution.
- The README entry point carries the same milestone and self-hosting terminology as the canonical Architecture and Roadmap.
- The project overview now follows the `.ai/`-first bootstrap order and reports current foundation checklist state.
- That historical structural review found sequential Delivery Gates 0 through 16, exactly one `Specified - Next` marker at Gate 5, and no superseded actual-worktree Gate 3 claim.
- Full regression after the correction: 17 suites, 63 tests, 62 passed, 1 existing Windows symbolic-link setup skip, 0 failures, and 0 errors.

## Agent Orchestration Reference Alignment

- Architecture, RFC-0009, Multi-Agent guidance, the canonical Roadmap, the compact AI architecture, Decision Log, Changelog, and Session Handoff now share the provider-neutral orchestration progression and invariants.
- Gate 6 owns the common immutable snapshot; Gate 7 typed delivery and control envelopes; Gate 8 dependency scheduling, fenced leases, idempotency, heartbeat ingestion, and recovery; Gate 9 provider-neutral execution profiles; Gate 10 validated workflow metadata; Gate 12 authenticated controls; Gate 13 dynamic rosters and bounded multi-worker patterns; Gate 15 bounded experiment baselines and rollback.
- Direct peer control, prompt-only coordination, file polling as the canonical bus, shared-worktree parallel mutation, optional verification, subjective completion scores, unlimited execution, and silent evidence loss are explicitly rejected.
- Both pinned GitHub tree links returned HTTP 200 on 2026-07-15.
- That historical structural verification found sequential Delivery Gates 0 through 16, exactly one `Specified - Next` marker at Gate 5, and Planned status for Gates 7, 8, 9, 10, and 13.
- Focused Planner and Assisted Development Loop verification passed 8 of 8 tests with no skips.
- The first full regression attempt failed 8 tests because the sandboxed Java child process could not resolve JUnit temporary paths under the user profile. Reproduction showed `AccessDeniedException`; the same read path succeeded inside the workspace.
- The full regression rerun with `java.io.tmpdir=C:/Enhancer/build/tmp/junit` passed 81 of 82 tests across 21 suites with 1 existing symbolic-link setup skip, 0 failures, and 0 errors.

## Gate 7 Replay Cascade Correction Verification

- Project-review RED: focused execution ran 27 `InProcessMessageBusTest` tests and failed exactly two new cases, proving that a handler publication caused by replay appended to the live journal and that an already-cancelled re-entrant publication bypassed drain-owned admission.
- The correction makes caused publications inherit the current pending entry's journaling mode and routes every `publish` call through the queue; replay cascades remain non-journaling, while cancelled re-entrant work reports `ENQUEUED` to the handler and `CANCELLED` to the draining caller without delivery or journaling.
- Focused GREEN passed 31 bus tests. Fresh full regression passed 44 suites and 183 tests: 181 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Java 17 production lint passed with `-Xlint:all -Werror`, and `git diff --check` passed.
- The correction restores the existing Gate 7 ordering and replay contracts; it adds no backpressure, threading, persistence, IPC, production wiring, or new authority.

## Git Workspace Authority Hardening Verification

- Focused RED configured a real repository with a nonexistent `diff.external` helper and proved the existing collector attempted to execute it, returning an unavailable diff observation.
- The collector now removes inherited `GIT_*` variables before adding its own discovery ceiling and runs exactly the two previously accepted commands; diff additionally fixes `--no-ext-diff` and `--no-textconv`.
- Focused GREEN passed all 6 collector tests. Fresh full regression passed 44 suites and 186 tests: 184 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Java 17 production lint passed with `-Xlint:all -Werror` across all 114 sources.
- The change narrows existing read-only command authority; it adds no command, shell, helper, mutation, network, Tool, or cross-component authority.

## Repository State Synchronization Verification

- Verified without network or branch mutation that local `main` and `origin/main` already point to PR #3 merge commit `52987f2`; no checkout or pull was required.
- Removed current-state claims that the PR #3 retry, cancellation, and ordering increments are uncommitted or that `e74be87` is the published tip.
- Canonical and compact architecture, roadmap, multi-agent, README, Project State, Changelog, and Session Handoff summaries now agree on Gate 6 Integrated, Gate 7 Specified - Next, current bus and Git boundaries, and backpressure as the next separate increment.
- Historical verification entries remain unchanged as evidence of their at-the-time state. Exactly one `Status: Specified - Next` remains in `ROADMAP.md`, at Gate 7.
- Fresh full regression passed 44 suites and 186 tests: 184 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 strict lint and `git diff --check` passed.

## Gate 7 Pending-Queue Backpressure Verification

- Test-first RED: focused compilation failed with exactly 10 expected errors naming only the intentionally absent `BackpressurePolicy`, `DeliveryStatus.BACKPRESSURED`, and combined bus constructor; production compilation passed and no error came from another module.
- The minimum implementation adds a 1-4096 immutable pending-publication policy with a finite default, typed non-blocking refusal before admission, preserved FIFO for accepted work, retryability after refusal, and deterministic bounded replay without live journaling.
- Focused GREEN passed 30 `InProcessMessageBusTest` tests and 4 `MessageEnvelopeTest` tests with no skips, failures, or errors.
- Fresh full regression passed 44 suites and 189 tests: 187 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Java 17 production compilation of all 115 sources passed with `-Xlint:all -Werror`.
- The result promotes pending-queue backpressure to Contract Verified. Gate 7 remains `Specified - Next` because the IPC interface, adapters, persistence, and production wiring do not exist.

## Combined Reliability And Security Verification

- Git observer focused and Workspace regressions passed after adversarial required-clean-filter tests eliminated porcelain and worktree comparison paths.
- CLI/Brain regressions passed required-document targets, zero-record preflight rejection, and injected post-persist reporting failure with durable completed exit semantics.
- RunRecord/Workspace/CLI regressions passed a 259-record fixture with a 256-record observation window and no deletion.
- Windows junction cases executed for both governed reads and required context and passed; only the two privilege-dependent symbolic-link setup cases remain skipped.
- Fresh full regression passed 44 suites and 200 tests: 198 passed, 2 skipped, 0 failures, and 0 errors.
- Java 17 production lint passed `-Xlint:all -Werror` across 115 sources; forbidden legacy API/unsafe current-command searches and `git diff --check` passed.

## Gate 7 Transport-Neutral IPC Contract Verification

- Test-first RED failed compilation with 33 expected errors naming only the absent `TransportMessage`, `MessageTransport`, `TransportOutcome`, and `TransportStatus`; existing production compilation passed.
- The minimum contract carries the existing destination and envelope by identity, exposes one provider-neutral functional send operation, and reports only transport-hop acceptance or bounded non-acceptance without reusing Message Bus delivery results.
- Focused GREEN passed 38 bus tests across `MessageTransportTest`, `MessageEnvelopeTest`, and `InProcessMessageBusTest` with no skips, failures, or errors.
- Fresh full regression passed 45 suites and 204 tests: 202 passed, 2 existing symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 119 sources.
- The result promotes only the transport-neutral interface to Contract Verified. No adapter exists, no process boundary was crossed, and Gate 7 remains `Specified - Next` pending a separate maturity assessment.

## Gate 7 Maturity Assessment

Fresh supporting evidence for this documentation-only assessment: all 38 bus tests passed with no skips, failures, or errors; the full regression passed 45 suites and 204 tests (202 passed, 2 existing symbolic-link setup skips); Java 17 production lint passed `-Xlint:all -Werror` across 119 sources.

Scope-item disposition:

- Typed domain-event and versioned-envelope foundation: Contract Verified by `MessageEnvelopeTest`; the sealed work, result, control, and handoff payload kinds are the current typed semantic surface. Application-specific event catalogs remain later consumers.
- Event, message, correlation, causation, run, and producer identities: Contract Verified by canonical UUID, bounded identity, distinct self-causation, and destination-kind/name invariants.
- Work, result, control, and handoff payload provenance: the assessment found partial Contract Verified evidence because the allowed-tool collection cardinality was unbounded. The follow-up correction now bounds the scope to 1 through 256 unique names, making this item fully Contract Verified.
- In-process topic and queue delivery: Contract Verified by registration-order fan-out, one-consumer queue delivery, unrouted outcomes, and whole-envelope carriage.
- Idempotency, retry, cancellation, dead-letter, replay, ordering, and backpressure: Contract Verified by the 30-test `InProcessMessageBusTest`; no claim exceeds synchronous process-local behavior.
- IPC transport interface: Contract Verified by `MessageTransportTest`; the route and envelope cross the provider-neutral boundary unchanged and hop acceptance remains distinct from bus delivery. No concrete adapter or real process hop exists.

Exit-criterion disposition:

- "A deterministic in-process pipeline delivers and replays a versioned event without duplicate side effects": evidenced by fresh topic/queue, idempotency, and fresh-bus replay tests.
- "Payloads are bounded or replaced by evidence references": the assessment found this unsatisfied because `WorkPayload` accepted an arbitrarily large set. The follow-up correction now accepts at most 256 unique names, each at most 256 characters, so this criterion is satisfied at Contract Verified maturity.
- "Authorization and provenance survive every hop": evidenced at Contract Verified maturity by exact envelope/payload identity preservation through in-process delivery and `TransportMessage`; no IPC adapter or cross-process security claim is made.
- "Event Bus semantics do not depend on the eventual IPC transport": evidenced by the transport accepting the existing destination/envelope and returning transport-only outcomes without exposing `DeliveryOutcome` or provider types.

Maturity conclusion and options:

- Integrated or Operational promotion is unsupported: no real runtime publisher/consumer, Scheduler, production bus wiring, concrete transport, or supported messaging entry point exists.
- Option A (selected and completed): Gate 7 stayed `Specified - Next`, the bounded test-first correction added an explicit `WorkPayload.allowedTools` cardinality ceiling, and a fresh promotion assessment is now the next task.
- Option B: promote Gate 7 to Contract Verified now by treating per-entry bounds as a payload bound; rejected because it contradicts the aggregate bounded-payload exit criterion.
- Option C: build a concrete IPC adapter before promotion; rejected because it does not close the identified payload blocker and would prematurely select endpoint, serialization, authentication, and threading policy.

This assessment itself changed no production or test code and did not change Gate 7 status. The separate correction has now closed its blocker without changing that status; promotion or Gate 8 activation still requires a separate task.

## Gate 7 Work Payload Scope-Bound Verification

- Behavioral RED: the new boundary test accepted exactly 256 valid unique tool names but failed because 257 were also accepted, isolating the missing cardinality contract.
- The minimum correction exposes `WorkPayload.MAX_ALLOWED_TOOLS = 256` and rejects larger scopes before immutable copying while preserving all existing per-name and envelope semantics.
- Focused GREEN passed all 39 bus tests with no skips, failures, or errors.
- Fresh full regression passed 45 suites and 205 tests: 203 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 119 sources.
- Post-document self-hosting verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- The bounded-payload blocker closed while Gate 7 still remained `Specified - Next`; the separate promotion assessment below then changed lifecycle state without adding a concrete adapter.

## Gate 7 Contract Verified Promotion Assessment

- Fresh focused evidence passed all 39 bus tests with no skips, failures, or errors: 30 delivery tests, 5 envelope/payload tests, and 4 transport-boundary tests.
- Fresh full regression passed 45 suites and 205 tests: 203 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 119 sources.
- All six Gate 7 scope items map to focused evidence: typed envelopes, identities, bounded provenance payloads, in-process topic/queue delivery, reliability semantics, and the provider-neutral IPC interface.
- All four exit criteria map to focused evidence: deterministic replay without duplicate side effects, bounded payloads or references, unchanged authorization/provenance carriage, and transport-independent bus semantics.
- The Roadmap marker move produced one expected actual-Roadmap RED because `RepositoryTaskPlannerTest` still named Gate 7; updating only the Planner and Assisted Loop next-gate expectations to Gate 8 restored all 8 focused self-hosting tests.
- Post-completion actual-document verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and selected Gate 8 on both proposal paths.
- Gate 7 is promoted to Contract Verified and Gate 8 becomes the sole `Specified - Next` gate. No concrete adapter, process hop, persistence, threading, production wiring, or Integrated/Operational claim is added.

## Gate 8 WorkItem Admission Contract Verification

- Test-first RED preserved successful production compilation and produced exactly 11 test-compilation errors, all naming only the intentionally absent `WorkItem` contract.
- The minimum immutable admission boundary retains one unchanged Gate 7 `WorkPayload` envelope, separates work identity from delivery identity, bounds required capability metadata, and derives task, snapshot, run, and Tool scope only from the envelope.
- Focused GREEN passed all 41 `WorkItem` and Gate 7 bus tests across 4 suites with no skips, failures, or errors.
- Fresh full regression passed 46 suites and 207 tests: 205 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 120 production sources.
- Post-document self-hosting verification passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip; both proposal paths retained Gate 8 as the sole next gate.
- Only WorkItem admission is Contract Verified. Gate 8 remains `Specified - Next`, with the dependency-ready single-worker Scheduler queue as its next bounded consumer.

## Gate 7 Runtime Integration Preparation Verification

- `MessagingRuntimeIntegrationTest` loads the real governed document set, reads a real approved task, collects a real Gate 6 Workspace snapshot, publishes its bounded work envelope through the real Gate 7 in-process queue, and admits the exact journaled envelope as one Gate 8 `WorkItem`.
- The test proves mismatched task identity is rejected before journaling, task/snapshot/run/Tool provenance survives unchanged, and journal replay reports a duplicate without admitting a second work item.
- Focused verification passed all 42 integration, WorkItem, and Gate 7 bus tests across 5 suites with no skips, failures, or errors.
- Fresh full regression passed 47 suites and 208 tests: 206 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 122 production sources.
- The resumed worktree already contained both production adapters, so no missing-type RED is claimed for this increment.
- Post-document self-hosting passed 15 of 16 actual-document Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Gate 7 remains Contract Verified. This evidence prepares, but does not replace, a separate fresh maturity assessment against every Gate 7 scope item and exit criterion.

## Product Journey Evaluation And Layered Security Specification

- The Roadmap now carries a cross-cutting track beside the numbered gates. Its four initial journeys are governed bug repair, bounded feature delivery, evidence-backed codebase explanation, and interrupted-run recovery; none is claimed Operational until a supported interface completes its versioned fixture end to end.
- The fifth product priority is a repeatable evaluation and release-quality harness. It measures task success, incorrect changes, induced-failure recovery, cost/time, user intervention, held-out regression, and multi-agent delta with explicit denominators and immutable provenance.
- Scheduler reliability is specified truthfully as at-least-once delivery composed with stable logical-work/effect idempotency, fenced leases, durable checkpoints, supported state migration, orphan reclamation, and replay-safe or compensatable effects. Universal exactly-once execution is explicitly not claimed.
- Interface order is shared Run/approval/verification/evidence/control API first, CLI reference surface, VS Code second, and Desktop later as a supervisory surface. Gate 12 now requires one change-centered review over plan, files/diff, tests/evidence, risk, approvals, recovery, and commit readiness.
- The default-security baseline treats repository, Tool/terminal, model, MCP, plugin, dependency, and generated content as untrusted data. Architecture defines common provenance, secret/outbound-data, least-privilege, isolation, audit, and rollback requirements; owning gates retain concrete enforcement.
- Gate 13 multi-agent promotion now requires measured improvement over the single-agent baseline on the same versioned fixtures and comparable budgets. Gate 16 release claims additionally require predeclared journey thresholds, supported-platform installation/update/rollback evidence, reproducible signed artifacts, and SBOM verification.
- This documentation task changes no capability maturity, code, Constitution text, external authority, or release state. Numeric thresholds remain deferred until representative fixtures and baselines exist.
- Structural verification found 17 sequential Delivery Gates, exactly one `Specified - Next` marker at Gate 8, four canonical journeys, and one accepted decision resolving the active task reference.
- Metric and consistency review confirmed explicit denominators plus aligned Scheduler, interface, UX, and security language across canonical and compact documents; neither Constitution file changed.
- Actual-document self-hosting passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.

## Gate 7 Integrated Maturity Assessment

- Fresh focused evidence passed all 42 tests across 5 suites: 30 `InProcessMessageBusTest`, 5 `MessageEnvelopeTest`, 4 `MessageTransportTest`, 1 `MessagingRuntimeIntegrationTest`, and 2 `WorkItemTest`, with no skips, failures, or errors.
- Fresh full regression passed 47 suites and 208 tests: 206 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 122 sources.
- Scope 1, typed events/envelopes: `WorkPayload` is Integrated on the real path; result/control/handoff and an application event catalog remain Contract Verified only.
- Scope 2, identities: message, correlation, logical-run, and producer identities are Integrated on the work path; non-empty causation and a distinct event identity have no real connection.
- Scope 3, payload provenance: work task revision, snapshot identity, and allowed Tools survive the real hop unchanged; result/control/handoff payloads remain contract-only.
- Scope 4, in-process delivery: queue delivery is Integrated; topic fan-out remains contract-only.
- Scope 5, reliability: journal replay and duplicate suppression are Integrated on the real path; handler failure, retry/re-delivery, dead letters, cancellation, re-entrant cascade ordering, and backpressure remain contract-only.
- Scope 6, IPC transport: the provider-neutral interface is Contract Verified, but no production implementation consumes it and no process or transport hop exists.
- Exit criterion 1 is Integrated for the work queue path: one versioned message is delivered and replayed without duplicate work. Exit criteria 2 through 4 remain fully Contract Verified, with only the work-path portion of authorization/provenance carriage Integrated.
- Gate 7 therefore remains Contract Verified. A single integrated sub-path cannot promote unconnected gate-level branches.
- Post-document self-hosting passed 15 of 16 actual-document Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error; structural/reference and whitespace checks passed.

## Gate 8 Single-Worker Scheduler Queue Verification

- Test-first RED preserved successful production compilation and failed test compilation with exactly 25 errors naming only the intentionally absent `QueuedWork` and `SingleWorkerSchedulerQueue` contracts.
- The minimum implementation retains exact WorkItems, bounds dependency metadata to 256 canonical unique identities, requires dependencies to be admitted first, bounds one run-scoped queue to 4096 total admissions, and exposes one active slot with matching completion.
- Focused GREEN passed all 45 tests across 6 messaging/runtime suites with no skips, failures, or errors.
- Fresh full regression passed 48 suites and 211 tests: 209 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 124 production sources.
- The queue creates no authority and makes no persistence, durable-state, lease, recovery, or worker-execution claim.
- Gate 8 remains `Specified - Next`; only WorkItem admission and this in-memory queue are Contract Verified.
- Post-document self-hosting passed 15 of 16 actual-document Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error; structural/reference and whitespace checks passed.

## Gate 8 Durable Scheduler Queue State Verification

- Test-first RED preserved successful production compilation and failed test compilation with exactly 48 aligned errors: 47 named only the absent durable queue state/store/wrapper contracts and 1 named the absent logical-run accessor.
- Added a canonical queue identity, immutable schema-v1 snapshots, monotonic revisions, one-logical-run binding, exact WorkItem/envelope serialization, strict UTF-8, a 64 MiB artifact bound, SHA-256 envelope integrity, and atomic create/replace publication.
- Every enqueue, successful claim, and completion stages a copy and persists the next revision before adoption; injected persistence failures left the prior in-memory and durable revision unchanged.
- Recovery preserves pending/completed state, moves interrupted active work back into admission order, persists that transition, and then permits re-claim under explicit at-least-once semantics.
- Focused verification passed all 14 tests across 5 Gate 7/8 runtime and integration suites with no skips, failures, or errors.
- Fresh full regression passed 50 suites and 219 tests: 217 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 130 production sources.
- Atomic publication prevents partial visible snapshots and supports normal process restart; parent-directory metadata is not fsynced and no power-loss durability claim is made.
- Gate 8 remains `Specified - Next`; only WorkItem admission, the single-worker queue, and its durable queue-state/restart-recovery sub-capabilities are Contract Verified.

## Unicode And Bounded File Operation Verification

- The reported 4097-code-unit reproduction aligned with the code: the old VerificationEvidence suffix began at a low surrogate and strict UTF-8 encoding rejected the result.
- Test-first RED preserved production compilation and failed test compilation with exactly 10 errors naming only the absent neutral Unicode and bounded-file helper contracts.
- `UnicodeText` now bounds prefixes and suffixes without splitting a surrogate pair; VerificationEvidence, ToolExecutor, CLI, RunRecord metadata, and Git failure reasons use it.
- `BoundedFileOperations` reads or hashes at most the configured byte ceiling and probes at most one extra byte. Read operations allocate no more than the accepted ceiling.
- ReadFileTool, ProjectContextReader, TargetFileMetadataCollector, FileSystemEvidenceStore, FileSystemRunRecordStore, and FileSystemSchedulerQueueStore retain their early size checks but enforce the real bound while consuming the file.
- Core focused GREEN passed 18 of 18 tests across 4 suites. The affected CLI/Context/Workspace/store integration group passed 54 tests with 2 existing Windows symbolic-link setup skips and no failures or errors.
- Fresh full regression passed 52 suites and 226 tests: 224 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 133 production sources.
- Interrupt-ignoring Tool termination and parent-directory fsync remain open; Tool accumulation is now finitely contained and the loop/run/verification package cycle is closed by the later verified extraction.

## Tool Isolation Capacity Verification

- Test-first RED preserved production compilation and failed test compilation with exactly 3 aligned errors naming the absent shared capacity type and typed exhaustion failure.
- Default ToolExecutor instances now share a process-wide 64-worker ceiling; policy, registration, and cancellation refusal still occur before capacity acquisition.
- A worker slot is acquired before thread start and released only from the actual worker thread termination path. Timeout, interrupt, close, and shutdown do not falsely free a slot while code remains alive.
- Saturation returns terminal `ISOLATION_CAPACITY_EXHAUSTED` evidence before Tool invocation or thread creation; the standard classifier does not retry it.
- A deterministic one-slot test proved that an interrupt-ignoring timed-out Tool blocked a second executor, held exactly one live slot, and allowed execution only after the first thread really exited.
- Affected verification passed 41 tests with 1 existing symbolic-link setup skip and no failures or errors.
- Fresh full regression passed 52 suites and 227 tests: 225 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 134 production sources.
- This is bounded containment only. Process isolation, OS termination, per-extension quotas, Scheduler backpressure, health recovery, and operator controls remain absent.

## Runtime Package Cycle Extraction Verification

- Structural RED compiled production and tests, then failed exactly one boundary test listing the six current forbidden directions: one loop-to-run import, two loop-to-verification imports, and three run-to-verification imports.
- Moved VerificationDecision, VerificationStatus, and VerificationCode unchanged to `com.enhancer.kernel`; their enum constants and RunRecord serialized names did not change.
- Moved AgentRunFinalizer to `com.enhancer.application` and added `VerifiedAgentRunTransition` so AgentRunState's actual completion method remains package-private.
- Source searches found zero forbidden loop/run/kernel imports and zero references to the previous verification-value or loop-finalizer package names.
- Focused structural, verifier, finalizer, RunRecord, persistence, and CLI verification passed 27 of 27 tests.
- Fresh full regression passed 53 suites and 228 tests: 226 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 135 production sources.
- Post-document self-hosting and boundary verification passed 16 of 17 Context Reader, Planner, Assisted Loop, and package-boundary tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- The project remains one Gradle module. ApprovedTask relocation, persistence SPI extraction, and physical module separation remain deferred.

## Gate 8 Durable Goal And AgentRun Lifecycle Verification

- Test-first RED preserved successful production compilation and failed test compilation with exactly 64 aligned errors naming only the intentionally absent Goal/AgentRun lifecycle, durable wrapper, store, status, and checked-failure contracts.
- Added one exact-WorkItem `RuntimeGoal`, one schema-v1 `RuntimeAgentRun`, Goal `ACCEPTED -> ACTIVE -> COMPLETED|FAILED`, and AgentRun `PLANNING -> READY -> EXECUTING -> AWAITING_VERIFICATION -> COMPLETED|FAILED`.
- Terminal results must match logical run, correlation, task, and work-message causation; runtime and message identities remain distinct, `VERIFIED` alone completes, and every other verification status fails explicitly.
- `DurableAgentRuntime` persists each transition before exposure. Injected persistence failures retained the previous in-memory and durable revision.
- `FileSystemAgentRuntimeStateStore` round-tripped exact Unicode-bearing WorkItem and result envelopes across store instances and rejected existing creation, invalid revisions, missing, corrupt, trailing, unsupported-version, oversized, non-regular, and integrity-valid invalid-UTF-8 artifacts.
- Focused runtime, bus, and package-boundary verification passed 63 of 63 tests across 10 suites with no skips, failures, or errors.
- Fresh full regression passed 55 suites and 238 tests: 236 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 146 production sources.
- At completion of this lifecycle increment, Gate 8 remained `Specified - Next` and the fenced single-owner lease recorded below was the next bounded ownership contract.

## Gate 8 Fenced AgentRun Lease Verification

- Test-first RED preserved successful production compilation and failed test compilation with 46 aligned errors naming only the absent lease type, owner/fence operations, and injected-Clock overloads.
- Added immutable `AgentRunLease` with a 256-character owner bound, positive fence token, issue/expiry instants, and durations from 1 millisecond through 24 hours.
- Acquisition is allowed only from `READY`, increments the persisted last-issued fence, and moves to `EXECUTING`; renewal and execution completion require the current unexpired owner and fence.
- Explicit reclaim and recovery at the exclusive expiry persistently return the AgentRun to `READY`; the next owner receives a greater fence and the former owner remains stale.
- `DurableAgentRuntime` persists acquisition, renewal, completion, and reclaim before exposure. Injected failures for all four operations retained the previous in-memory and durable revision.
- `FileSystemAgentRuntimeStateStore` round-tripped exact Unicode-bearing leases and fence history across instances, preserved unexpired execution, reclaimed at exact expiry, and constrained each update to the current fence or an increment of exactly one.
- Focused runtime, bus, and package-boundary verification passed 68 of 68 tests across 10 suites with no skips, failures, or errors.
- Fresh full regression passed 55 suites and 243 tests: 241 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 147 production sources.
- Post-document self-hosting and boundary verification passed 31 of 32 Context Reader, Planner, Assisted Loop, package-boundary, lifecycle, and filesystem-store tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Structural/reference checks retained exactly one Gate 8 `Status: Specified - Next` marker, resolved `CURRENT_TASK.md` to its accepted decision, and passed `git diff --check`.
- At completion of the fenced-lease increment, Gate 8 remained `Specified - Next` and the queue-to-lifecycle dispatch recorded below was the next bounded integration.

## Gate 8 Durable Queue-To-AgentRun Dispatch Integration

- Test-first RED preserved successful production compilation and failed test compilation with exactly 13 aligned errors naming only the absent `DurableAgentRunDispatcher` and `AgentRunDispatch` contracts.
- Added caller-metadata preflight, active-work reuse or persist-first ready claim, exact WorkItem Goal creation/recovery, and missing-prefix advancement through named AgentRun planning, readiness, and lease acquisition.
- Repeated same-owner calls return the existing unexpired lease without queue/runtime revision changes; expiry recovery returns `READY` and a new owner receives a greater fence.
- Queue claim persistence failure creates no runtime. Failures at Goal creation, AgentRun creation, readiness, and lease acquisition leave the active claim and durable prefix available for successful re-entry.
- Existing WorkItem mismatch is checked before runtime expiry reclamation; different AgentRun, different unexpired owner, Awaiting-Verification, and terminal state all fail closed.
- Real filesystem queue/runtime stores survived cross-instance recovery: the queue requeued and reclaimed the same WorkItem while the runtime returned the exact existing Unicode-bearing lease.
- Focused verification passed 31 of 31 tests across 7 dispatcher, queue, runtime, filesystem-store, and package-boundary suites with no skips, failures, or errors.
- Fresh full regression passed 57 suites and 251 tests: 249 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors; Gradle emitted no deprecation warning.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 149 production sources.
- Post-document self-hosting and boundary verification passed 36 of 37 Context Reader, Planner, Assisted Loop, package-boundary, dispatcher, lifecycle, queue, and filesystem integration tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Structural/reference checks resolved the active task decision, retained exactly one Gate 8 `Status: Specified - Next` marker, and passed tracked/untracked whitespace validation.
- This named queue-to-lifecycle path is Integrated. Gate 8 remains `Specified - Next`; documentation review later established that fence-checked execution completion reaches only `AWAITING_VERIFICATION` and cannot be coupled directly to queue completion because queue completion satisfies dependencies.

## Gate 8 Connection Boundary Documentation Verification

- Cross-checked all seven `.ai/` bootstrap documents, the canonical Constitution and operating documents, the Gate 8 production queue/runtime contracts, and the current Roadmap sequence.
- Confirmed the conflict: `DurableAgentRuntime.completeExecution` persists `AWAITING_VERIFICATION`, while `SingleWorkerSchedulerQueue.completeActive` writes the dependency-satisfaction set. The prior direct acknowledgement wording could therefore conflate execution receipt with verified completion.
- Replaced that wording with an explicit terminal-disposition boundary and a gate-owned ordered backlog for result finalization, process worker/local IPC, controls, effects, retry, and later handoff/multi-agent work.
- Corrected the stale Roadmap RFC statement so it no longer says the already-active bounded Gate 8 track must wait for a future RFC; detailed RFC work remains required before process workers, concrete IPC production wiring, broader policies, or Operational promotion.
- Changed no production/test code, capability maturity, Constitution text, Agent rules, external authority, or release state.
- Focused actual-document verification passed 24 tests across 5 suites: 23 passed, 1 existing Windows symbolic-link setup skip, 0 failures, and 0 errors.
- Full regression passed 57 suites and 251 tests: 249 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`.
- Structural/reference checks found one Gate 8 next marker, one accepted-decision heading, one matching active-task reference, zero withdrawn directive/RFC wording occurrences, and no whitespace error.
- `SESSION_HANDOFF.md` now explains the conflict's incremental origin and preserves three next-session choices: keep the active slot through verification (recommended minimum), add a durable non-terminal verification-wait state later for throughput, or reject execution acknowledgement as queue completion.

## Gate 8 Result-Path Finalization Verification

- Added `DurableAgentRunFinalizer` connecting a resolved RunRecord to the runtime terminal state and the matching queue disposition in one recoverable, idempotent order, with `finalizeAgentRun` for the forward path and `recoverFinalization` for autonomous post-terminal recovery.
- Task 1 RED: `DurableAgentRunFinalizerTest` failed to compile with only the missing `DurableAgentRunFinalizer`; GREEN passed the verified and failed forward-path scenarios.
- The forward-path GREEN surfaced the durable queue's recovery-requeue contract: a freshly recovered queue exposes no active work, so the finalizer re-claims the requeued WorkItem before recording the disposition, matching the claim-then-dispose pattern the queue's own recovery already mandates.
- Task 2 RED: the class failed to compile with only the missing `recoverFinalization`; GREEN passed autonomous post-terminal recovery with no reference and idempotent re-finalize.
- Task 3: the fail-closed, task/document-binding, different-reference, and finalize-before-acknowledgement guards implemented in Task 1 were pinned and passed on first run.
- Focused suite passed 8 of 8 `DurableAgentRunFinalizerTest` cases with no skips, failures, or errors.
- Full regression passed 60 suites and 269 tests: 267 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`; Java 17 strict lint passed across 151 production sources.
- Structural/reference checks retained exactly one Gate 8 `Status: Specified - Next` marker, resolved `CURRENT_TASK.md` to its accepted decision, and passed `git diff --check`.
- Gate 8 remains `Specified - Next`; the RunRecord-backed result path is now Contract Verified, and the process-isolated worker plus selected local IPC (connection 3) is the next bounded integration.


## Document Single-Owner Restructure Verification

- Baseline before any document change: 65 suites, 299 tests, 297 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors under `--warning-mode all`.
- Post-change full regression under `--warning-mode all` after `cleanTest`: 65 suites, 299 tests, 297 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Identical to baseline, as required for a documentation-only change.
- No production or test source file was modified; the change touches only Markdown.
- Line accounting on the `PROJECT_STATE.md` split reconciles exactly: the original 861 lines are 657 historical lines moved to this log, 185 lines retained, and 19 lines of duplicated next-task and reading-order content deleted at source.
- `ProjectContextReader` contract re-checked directly: all 15 `RequiredProjectDocument` paths exist, are valid UTF-8, and are under the 1 MiB per-document ceiling. `PROJECT_STATE.md` fell from 122 KB to 32 KB and `SESSION_HANDOFF.md` from 76 KB to 1.9 KB.
- `AcceptedDecisionProjector` contract re-checked: `DECISION_LOG.md` carries 84 `###` headings, all unique and non-blank, with 84 matching `Status: Accepted Decision` lines.
- `TaskJustificationProjector` contract re-checked: the single `## Justified By` bullet in `CURRENT_TASK.md` exact-matches the new accepted-decision heading.
- `RepositoryTaskPlanner` contract re-checked: `ROADMAP.md` retains exactly one `Status: Specified - Next` marker (Delivery Gate 8).
- End-to-end production verification through the real CLI `run` path against the changed documents: `status=COMPLETED`, `exitCode=0`, `verificationStatus=VERIFIED`, `brainStatus=AVAILABLE`, `workspaceObservations=24`, `memoryFreshness=matched=15,diverged=0,notObserved=0`, `graphNodes=102`, `graphEdges=2`, `graphDecisions=84`, `impactExecutions=1`, `impactDecisions=1`. The 15 matched documents prove every required document loaded and digest-matched after the restructure; `graphDecisions=84` proves the real decision log parsed including the new entry; `impactDecisions=1` proves the new `Justified By` reference resolved through the production composition rather than a fixture.
- `CURRENT_TASK.md` was temporarily set to `In Progress` with a `read-file` allowed-tool scope for that single governed run and restored to `Completed` immediately afterwards; the run's evidence and RunRecord live in the Git-ignored `.enhancer/` tree.
- Structural: `git diff --check` clean.

## Document Ownership Completion Verification

- Full regression under `--warning-mode all` after `cleanTest`: 65 suites, 299 tests, 297 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Identical to the baseline, as required for a documentation-only change.
- No production or test source file was modified.
- `.ai/architecture.md` now contains zero capability-maturity verdicts; the only remaining occurrence of the maturity vocabulary is the header sentence stating that the file does not carry such verdicts, plus the vocabulary definition bullet that names the five maturity levels without asserting one.
- The required startup reading order is unchanged at the 15 `RequiredProjectDocument` paths; `docs/verification-log.md` was deliberately not added, because the log grows without bound and is evidence rather than startup context.
- Parser contracts re-checked: 84 `###` decision headings, all unique; the `## Justified By` bullet exact-matches its accepted-decision heading; `ROADMAP.md` retains exactly one `Status: Specified - Next` marker.
- End-to-end production verification through the real CLI `run` path: `status=COMPLETED`, `exitCode=0`, `verificationStatus=VERIFIED`, `brainStatus=AVAILABLE`, `memoryFreshness=matched=15,diverged=0,notObserved=0`, `graphDecisions=84`, `impactDecisions=1`. The 15 matched documents include the rewritten `.ai/architecture.md`, confirming it still loads and digest-matches through `ProjectContextReader`.
- `CURRENT_TASK.md` was temporarily set to `In Progress` with a `read-file` allowed-tool scope for that single governed run and restored to `Completed` immediately afterwards.
- Structural: `git diff --check` clean.

## Document Ownership Enforcement Verification

- RED first: `DocumentOwnershipTest.onlyProjectStateClaimsGateMaturity` failed on the unmodified repository and named six violations — `README.md:13`, `docs/08-Multi-Agent.md:5`, `docs/10-Roadmap.md:21`, `docs/10-Roadmap.md:23`, `docs/11-Architecture.md:37`, and `docs/rfcs/RFC-0009-Multi-Agent.md:7`. `onlyCurrentTaskDeclaresTheNextTask` passed on the same run, confirming the earlier restructure had already fixed next-task ownership.
- Every one of the five documents naming Gate 7 claimed it was `Specified - Next`, which stopped being true when Gate 8 took the marker. The same fact copied to five places had drifted in all five.
- GREEN after replacing the six claims with references to `PROJECT_STATE.md`: `DocumentOwnershipTest` passed 2 of 2 with no skips.
- Full regression under `--warning-mode all` after `cleanTest`: 66 suites, 301 tests, 299 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. The suite and test deltas against the previous 65/299 baseline are exactly the new structural test.
- The maturity pattern is matched on a gate-scoped subject, so a document may still define or discuss the maturity vocabulary without asserting a verdict; `CONSTITUTION.md`, `AGENTS.md`, and `.ai/architecture.md` all state the rule and pass.
- Store write-root audit finding re-examined and rejected as a defect: the 2026-07-15 Gate 5 decision requires the evidence and RunRecord roots as explicit caller inputs, `README.md` describes `.enhancer/` as the example runtime directory, both filesystem stores already refuse a symbolic-link root through `Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)`, and `FileSystemRunRecordStore.references()` filters by suffix, regular-file status, and canonical UUID, so overlapping roots are harmless. No behaviour was changed; `ARCHITECTURE.md` and `README.md` now state the contract exactly.
- Known gap left open: the symbolic-link root refusal in both stores has no test. Inducing it requires symbolic-link creation privilege this Windows host denies, which already causes the two existing skips, so a test would give no signal here.
- Adversarial verification of the guard itself, because a guard that is merged without being provoked is only a belief that it works. Three violations were injected into the working tree and the focused suite re-run:
  - `docs/07-MCP.md` (not exempt) gained "Delivery Gate 9 is Operational." -> `onlyProjectStateClaimsGateMaturity` failed and named `docs/07-MCP.md:57 -> "Gate 9 is Operational"`, with the file, line, and matched text.
  - `docs/06-Planner.md` (not exempt) gained a `## Next Task` heading -> `onlyCurrentTaskDeclaresTheNextTask` failed and named `docs/06-Planner.md:71`.
  - `CHANGELOG.md` (exempt) gained the identical maturity sentence -> not reported by either test, confirming the exemption list suppresses the append-only records without suppressing real violations.
  All three injections were then reverted and the suite returned to green, with `CHANGELOG.md`'s legitimate entries intact.
- Structural: `git diff --check` clean.

## Decision Log Split Verification

- Content preservation, checked by reconstructing every entry from the pre-split original and comparing to the written files: 85 of 85 bodies matched byte for byte, 0 mismatches, 0 headings missing, 0 headings added, and index heading order identical to the original document order.
- Full regression under `--warning-mode all` after `cleanTest`: 67 suites, 306 tests, 304 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. The suite and test deltas against the previous 66/301 baseline are exactly `DecisionLogIndexTest`.
- No production source changed. `AcceptedDecisionProjector` scans only `### ` headings and `Status: Accepted Decision` lines and never reads a decision body, so the index preserves everything it consumes.
- Per-suite results for every document-reading path: `AcceptedDecisionProjectorTest` 4/4, `TaskJustificationProjectorTest` 4/4, `ProjectContextReaderTest` 8 with 1 pre-existing skip, `RepositoryTaskPlannerTest` 4/4, `ApprovedTaskReaderTest` 3/3, `RunRecordMetadataCollectorTest` 6/6, `ProjectBrainViewIntegrationTest` 5/5, `WorkspaceCollectionIntegrationTest` 1/1, `RuntimePackageBoundaryTest` 1/1, `DocumentOwnershipTest` 2/2, `DecisionLogIndexTest` 5/5.
- Live production path through the real CLI `run` against the split documents: `status=COMPLETED`, `exitCode=0`, `verificationStatus=VERIFIED`, `brainStatus=AVAILABLE`, `workspaceObservations=27`, `memoryFreshness=matched=15,diverged=0,notObserved=0`, `graphNodes=104`, `graphEdges=2`, `graphDecisions=86`, `impactExecutions=1`, `impactDecisions=1`. `graphDecisions=86` proves the projector rebuilt every decision node from the index alone, and `impactDecisions=1` proves the new `Justified By` reference resolved through the production composition.
- Durable history unaffected: `replay` of `run-record/6d7c9b5f-dbef-493c-9783-5c935bfc8839` returned `COMPLETED`/`VERIFIED` with the task binding intact and no Tool re-execution.
- Startup context fell from 439,497 to 248,063 bytes (43%); `DECISION_LOG.md` fell from 211,121 bytes and 48% of that context to 19,687 bytes and 7%. Average index entry is 231 bytes against 2,483 per full decision, raising headroom against the 1 MiB `MAX_DOCUMENT_BYTES` ceiling from 337 to roughly 4,454 further decisions.
- Adversarial verification of `DecisionLogIndexTest` with five injected drifts, each caught and named:
  - decision file deleted -> `index entry has no decision file: "2026-07-10: Use Repository Documents As Durable Memory"`.
  - orphan file added -> `decision file is not in the index: docs\decisions\2099-01-01-orphan.md`.
  - file heading tampered -> reported on both sides at once, as a missing file for the original heading and an unindexed file for the tampered one.
  - acceptance status removed from a file -> `decision file omits "Status: Accepted Decision"`.
  - level-3 heading injected into a decision file -> `### 2026-07-16: Shadow Heading`.
  The path-ceiling and `Justified By` assertions passed throughout, so the guard produced no false positives. All five injections were reverted and content preservation re-verified at 85 of 85.
- `DocumentOwnershipTest` initially failed after the split because the decision bodies had moved into an unexempt directory; `docs/decisions` was added to its exemption list for the same reason `DECISION_LOG.md` already held one, and the failure was the guard behaving correctly rather than a defect.
- Structural: `git diff --check` clean.

## Documentation Audit Closure Verification

- Focused suites for the modified surfaces: `DocumentOwnershipTest` 2/2, `DecisionLogIndexTest` 5/5, `RuntimePackageBoundaryTest` 1/1, `GitWorkspaceCollectorTest` 12/12 (up from 8 with no skips), `RepositoryTaskPlannerTest` 4/4.
- Full regression under `--warning-mode all` after `cleanTest`: 67 suites, 310 tests, 308 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. The test delta against the previous 306 is exactly the four added `GitWorkspaceCollector` cases; no new suite was added.
- Guard calibration was measured rather than assumed. Replacing the pattern with plain co-occurrence of a gate and a maturity level flagged nine lines, of which six were false positives: forward-looking conditions (`require an Operational single-agent baseline`), an adjectival use, and the sentence explaining why a claim had been removed. That approach was rejected and the three explicit patterns adopted instead, which flagged four lines — three genuine claims plus the same commentary sentence, which was then reworded rather than exempted.
- Adversarial verification of the widened guard with four injections: `retains Gate 9 at Contract Verified` caught, `promotes Gate 9 to Integrated` caught, a parenthetical `(Operational) | Gate 9` table row caught, and the control `Delivery Gate 9 requires an Operational baseline before it starts` correctly not reported. All injections reverted.
- A defect in the guards' reliability was found during that adversarial pass and fixed. Because `DocumentOwnershipTest` and `DecisionLogIndexTest` assert over Markdown that Gradle did not track as a task input, `gradle test` after a documentation-only change reported the task up to date and the guards did not execute; an injected violation produced a green build. The `test` task now declares the project's Markdown as an input. Re-verified directly: with the injection present and no `cleanTest`, `compileJava` and `compileTestJava` stayed up to date while `:test` re-ran and failed on the injected claim.
- `GitWorkspaceCollector` coverage added for the rejection branches of `resolveGitExecutable`: a candidate at the observed project root and one nested beneath it are both refused with no fallback, a relative PATH entry is skipped even when a file sits there, an absent directory contributes nothing, a directory named like the executable is refused as not a regular file, an absent or blank PATH resolves to empty rather than a bare command name, and the PATH variable is matched case-insensitively. `MAX_OUTPUT_BYTES` and `TIMEOUT_SECONDS` are pinned as constants; inducing either would require a purpose-built git that floods or stalls and is deliberately not shipped.
- `TIMEOUT_SECONDS` was widened from private to package-private for that assertion. No behaviour changed.
- Structural: `git diff --check` clean; decision index and files agree at 87 each; all 15 required documents present.

## File Spool Transport Verification

- Aligned RED first: 17 test-compile errors, every one naming only the absent `FileSpoolMessageTransport`; production compiled unchanged.
- Focused GREEN after implementation: `FileSpoolMessageTransportTest` 6/6 and `MessageEnvelopeCodecTest` 4/4, no skips.
- Full regression under `--warning-mode all` after `cleanTest`: 69 suites, 320 tests, 318 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Java 17 strict lint passed across 161 production sources.
- Three defects in the first draft were found by comparing it against an independently written implementation of the same contract, and each was proven before being fixed:
  - Occurrence time was encoded with `toEpochMilli`, truncating an `Instant`. A test carrying `2026-07-20T09:00:00.123456789Z` failed against the draft and passes against epoch-second plus nanosecond.
  - The frame carried the spool time in its header and digested over it, so two hops of one message differed. The frame now holds no wall-clock or random state and `encodesDeterministically` asserts byte equality.
  - Decode failures surfaced as plain `IOException`, leaving a caller unable to separate a permanently corrupt message from a transient filesystem error. `CorruptedSpooledMessageException` is now distinct and asserted for truncation, tampering, trailing bytes, bad magic, and an empty frame.
- The wire format was split into `MessageEnvelopeCodec` so its cases are assertable without a filesystem; `MessageEnvelopeCodecTest` touches no temporary directory, while `FileSpoolMessageTransportTest` covers only publication — round trip through real files, backpressure at capacity spooling nothing further, an unusable spool root, a distinct file per hop, identical bytes for a resent message, and that `read` surfaces the typed corruption failure from a real file.
- Nothing wires the adapter into production: no CLI, worker, or bus path constructs it, so no runtime behaviour changed.
- Verified that this increment stands alone. Three files belonging to a parallel session in the same working tree were moved aside and the full regression re-run before staging, confirming the commit compiles and passes without them.
- Structural: `git diff --check` clean.

## File Spool Codec Test Strengthening Verification

- `MessageEnvelopeCodecTest` grew from 4 cases to 11 after review against an independently written test for the same codec. Full regression rose from 320 to 327 tests; 69 suites, 325 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors under `--warning-mode all`. Java 17 strict lint passed across 161 production sources. No production source changed.
- Three coverage gaps came from the comparison: nanosecond-bearing occurrence time now runs through every round trip rather than one dedicated case, supplementary characters now appear in the producer and control-reason fields as well as a target path, and a short garbage frame is rejected alongside an empty one.
- Four cases cover a peer on an incompatible format, which neither test had: an unsupported codec version, an unknown destination kind, an unknown payload kind, and an unknown verification status.
- Those four are guarded by `acceptsTheHandCraftedBaselineFrame`, which decodes the same hand-built frame with every field valid. Without it a malformed body would let the rejection cases pass by truncation instead of by the guard under test.
- That guard was added because mutation testing caught exactly that defect. An earlier version of the incompatible-peer test passed against a codec whose codec-version check had been deleted, because its hand-built bodies were incomplete and failed on EOF first.
- Mutation results after the correction, deleting one guard at a time and re-running the suite: codec-version check caught, envelope-version check caught, nanosecond field zeroed on encode caught, trailing-byte check caught.
- The decode-side `allowedTools` ceiling survived its mutation. This is an equivalent mutant rather than a coverage gap: `WorkPayload`'s own constructor rejects more than `MAX_ALLOWED_TOOLS` entries, so removing the codec's check still produces `CorruptedSpooledMessageException` through the wrapped `IllegalArgumentException`. The codec check is redundant defense in depth, and `failsClosedOnACollectionLargerThanTheContractAllows` still pins that an over-cardinality frame is refused whichever layer refuses it.
- Structural: `git diff --check` clean.

## Worker Process Isolation Verification

- Aligned RED first: 20 test-compile errors, every one naming only the four absent types (`IsolatedWorkerLauncher`, `IsolatedWorkerOutcome`, `IsolatedWorkerStatus`, `IsolatedWorkerMain`); production compiled unchanged.
- Focused GREEN: `IsolatedWorkerLauncherTest` 7/7, no skips. The suite spawns real child JVMs rather than stubbing the boundary.
- Full regression under `--warning-mode all` after `cleanTest`: 70 suites, 334 tests, 332 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Java 17 strict lint passed across 165 production sources.
- Authority boundary re-checked directly: `ProcessBuilder` appears in exactly two production files, `GitWorkspaceCollector` and `IsolatedWorkerLauncher`, each scoped by its own accepted decision. No new network, reflection, or dynamic-loading capability was added.
- Bounds proven rather than asserted in prose: a child exiting with code 7 yields `COMPLETED` carrying that code and no reason; a child sleeping ten minutes under a one-second timeout yields `TIMED_OUT` with no exit code, and the test fails if the call takes more than thirty seconds, so the watchdog must actually destroy it; a child writing eight megabytes across both streams still yields `COMPLETED` with nothing retained.
- Input guards proven: null entry point, null argument list, and null timeout are refused; a zero or over-`MAX_TIMEOUT` timeout is refused; and a null entry inside the argument list is refused rather than silently dropped.
- The process boundary is proven by a real message crossing it. This process spools a work message through the 3c adapter, the child decodes it and exits `EXIT_MESSAGE_READ`, and an empty spool, a truncated message, and a missing argument each produce their own distinct exit code.
- One test defect was found and corrected during the GREEN pass. `runsOnlyTheCurrentJvmExecutable` initially asserted that the resolved executable lies outside the project root, a property borrowed from `GitWorkspaceCollector`. It is wrong here: that check defends against a repository shipping its own program to poison a PATH lookup, and this launcher performs no lookup. It also fails on this repository, which vendors its own JDK under `.tools/` inside the project root. The assertion was replaced with one that the resolved path is the executable this process is running, and the reasoning recorded in the test so it is not reintroduced.
- Nothing wires the launcher into production: no CLI, worker, or execution port constructs it, so no runtime behaviour changed.
- Structural: `git diff --check` clean.

## Connection 3 Document Consistency Restoration Verification

- Review found three documents contradicting the merged state of connections 3b and 3c. Each was verified against the repository before being corrected rather than taken on the reviewer's word: `PROJECT_STATE.md` recorded worker process isolation and the local IPC adapter as not existing; `ROADMAP.md` still named terminal disposition, shipped as connection 1, as the next increment; and `SESSION_HANDOFF.md` recorded a single external command authority when there are now two.
- The `PROJECT_STATE.md` drift was an omission from the 3c and 3b increments themselves: both updated the source counts but neither updated the Gate 8 delivery position or the Contract Verified list.
- `DocumentOwnershipTest` structurally cannot catch this class of drift. It enforces that non-owning documents do not restate a fact, and `PROJECT_STATE.md` is the owner of capability maturity, so a stale owner is invisible to it. Recorded as a known limit of the guard rather than a defect in it.
- Corrections made: `PROJECT_STATE.md` now records both connection-3 halves as Contract Verified unwired capabilities and states that connecting them needs a decision on how a result returns; `ROADMAP.md` gains matching Contract Verified entries and names the result-return increment as next; `SESSION_HANDOFF.md` was rewritten because its whole Working Tree section still described PR #8 and #9 and named a deleted branch.
- Full regression under `--warning-mode all` after `cleanTest`: 70 suites, 334 tests, 332 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Documentation-only change; no production or test source modified.
- Structural: `ROADMAP.md` retains exactly one `Status: Specified - Next` marker; `git diff --check` clean; the one remaining absence claim in `PROJECT_STATE.md` covers effect records, retries, and production wiring, which genuinely do not exist.

## Process Isolated Execution Verification

- Focused GREEN: `ProcessIsolatedAgentRunExecutionTest` 7/7 and `IsolatedWorkerLauncherTest` 5/5, no skips.
- Full regression under `--warning-mode all` after `cleanTest`: 71 suites, 339 tests, 337 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Java 17 strict lint passed across 167 production sources.
- The end-to-end case is real rather than stubbed: the parent spools a work envelope, a child JVM runs the Gate 1-4 pipeline against a digest-matching target, and the returned reference resolves in the shared store to a RunRecord whose verification status is `VERIFIED`. That record can only exist if the child actually executed, so the assertion cannot pass without the process boundary being crossed.
- Result validation proven by tampering rather than by inspection. A result republished with a different correlation identity is refused naming the correlation identity, and a result republished claiming `REJECTED` against a record the store shows as `VERIFIED` is refused naming the claimed status. A child therefore cannot promote its own run.
- Failure paths proven through the `WorkerProcessLauncher` port without spawning a process: a `TIMED_OUT` outcome and a non-zero exit each fail closed, and a launcher that reports success while publishing nothing fails closed rather than returning an unresolvable reference.
- Recovery proven by a launcher that throws if invoked: after a completed cycle, re-entry returns the already-published reference without launching a second child, so an interrupted cycle does not orphan a second RunRecord.
- `AgentLoopAgentRunExecution` gained an `executeWork(workItem, goalId, agentRunId)` seam and `execute(dispatch)` now delegates to it. The refactor is behaviour-preserving: a process-isolated child holds no lease and cannot construct an `AgentRunDispatch`, and the lease and queue identity were never read by the pipeline. Both paths therefore run one implementation rather than two similar ones.
- `IsolatedWorkerLauncher` now implements a `WorkerProcessLauncher` port, matching the port-and-adapter shape already used by `AgentRunExecution`, `SchedulerQueueStore`, and `MessageTransport`. The two child-contract cases were moved out of the launcher suite, which now covers only process lifecycle.
- Authority boundary unchanged: `ProcessBuilder` still appears in exactly two production files.
- Store roots reach the child only as launcher arguments. No payload field can redirect where evidence or RunRecords are written.
- Structural: `git diff --check` clean.

## Process-Isolated Execution Hardening Verification

- Aligned RED before implementation: the two focused suites ran 17 tests and failed 9. The failures independently named foreign work identity, wrong work destination, multiple results triggering a launcher, wrong result destination, RunRecord source/target/digest mismatches, the public `executeWork` seam, and stale next-task declarations.
- Focused GREEN after implementation: `ProcessIsolatedAgentRunExecutionTest` and `DurableAgentRunFinalizerTest` passed together; `ProcessIsolatedAgentRunExecutionTest` now has 16 cases, including a real child-JVM end-to-end case and adversarial records for a different task, source document, read target, and expected digest.
- Launcher non-invocation is executable evidence, not inference: foreign work, a wrong work route, and several result files are each paired with a `WorkerProcessLauncher` that throws if called, and each request instead fails with the intended `IOException`.
- Result authority is bounded before reference return: exact result route, correlation/logical-run/causation/task payload identity, reference resolution, shared finalizer task/source binding, read-file target, verification-bearing expected digest, and claimed-versus-recorded status all pass on the real child result and fail independently when tampered.
- Document consistency guard was first widened while the stale ROADMAP and SESSION_HANDOFF declarations remained; `onlyCurrentTaskDeclaresTheNextTask` failed and named both documents. After repair, the guard accepts only `CURRENT_TASK.md`'s canonical `## Next`, while ROADMAP retains exactly one `Status: Specified - Next` maturity marker.
- Fresh full regression under `--warning-mode all` after `cleanTest`: 71 suites, 348 tests, 346 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors. Java 17 production compilation passed `-Xlint:all -Werror` across all 167 sources.
- External-command authority is unchanged: `ProcessBuilder` still appears in exactly two production files, `GitWorkspaceCollector` and `IsolatedWorkerLauncher`.
- Structural: stale 3d absence searches returned no matches; `git diff --check` clean.

## Strict Lint Build Enforcement Verification

- Gap confirmed before the change rather than taken from the review: `-Xlint` and `-Werror` appear nowhere in `build.gradle`, `scripts/`, or any CI configuration, and the repository has no `.github/` directory. Every increment's recorded "strict lint passed" line therefore rested on a manual javac invocation the build never ran.
- The gap was live, not theoretical. `./gradlew build` on the unmodified tree succeeded while applying no lint flags, so a warning introduced by any increment would have shipped green.
- Applied `-Xlint:all -Werror` through `tasks.withType(JavaCompile).configureEach`, covering test sources as well as production. Test sources were probed first and already compiled clean under the same flags, so the wider scope cost nothing and closes an equally easy regression path.
- The guard is proven to fire rather than asserted. A raw-type declaration injected into `UnicodeText` failed `:compileJava` with `error: warnings found and -Werror specified`; the same injection in `UnicodeTextTest` failed `:compileTestJava`. Both probes were reverted and the tree confirmed clean.
- Without those probes this entry would claim only that a clean tree still compiles, which a no-op configuration would also satisfy. The failure cases are what distinguish an enforced flag from a decorative one.
- Behaviour preserved: full `./gradlew clean build --warning-mode all` passed 71 suites, 348 tests, 346 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors — identical to the count before the change. No production or test source was modified.
- No existing source required a fix under the newly enforced flags. That is the evidence that the manual practice had genuinely been followed; the defect was in its enforcement, not in the code it governed.
- Scope held: one file changed. `git diff --stat` reports `build.gradle` alone.
- Structural: `git diff --check` clean.

## Process-Isolated Durable Worker Composition And Spool Retirement Verification

- Aligned RED first: the focused worker command failed at `compileTestJava` with exactly
  two missing-contract errors â€” `AgentRunExecution.cleanupAfterCheckpoint` did not
  exist and `DurableAgentRunWorker.processIsolated(...)` could not be resolved. Production
  sources were unchanged and compiled, so the failure was classified as the active
  connection-3 composition/retention gap rather than an unrelated regression.
- Focused GREEN after the minimum implementation: `DurableAgentRunWorkerTest`,
  `ProcessIsolatedAgentRunExecutionTest`, and
  `FileSystemAgentLoopWorkerIntegrationTest` passed 31 tests across 3 suites. The
  integration uses the production composition and a real child JVM, crosses the work
  and result spools, resolves the real RunRecord, records the terminal runtime and queue
  disposition, clears the checkpoint, and leaves the invocation root without cycle
  entries.
- Durable ordering is executable: a forced post-checkpoint cleanup failure leaves a
  `PendingFinalization` containing the RunRecord reference; a fresh worker retries
  cleanup and reaches `VERIFIED_COMPLETED` with the execution count still exactly one.
  The reference therefore persists before cleanup, and cleanup persists before execution
  acknowledgement.
- Cleanup scope is executable: two cleanup calls remove the exact Goal/AgentRun cycle
  idempotently while a sibling cycle and its file remain. The production implementation
  also rejects symbolic-link invocation, Goal, or AgentRun boundaries rather than
  traversing them, and never addresses the invocation, Evidence, or RunRecord roots for
  deletion.
- Fresh full `./gradlew clean build` passed 71 suites, 351 tests, 349 passed, 2 existing
  Windows symbolic-link setup skips, 0 failures, and 0 errors. All 8 build tasks executed;
  production and test compilation ran under the build-enforced `-Xlint:all -Werror`.
- Focused document consistency after synchronization: `DocumentOwnershipTest` 2/2,
  `DecisionLogIndexTest` 5/5, and `RepositoryTaskPlannerTest` 4/4, with no failures,
  errors, or skips. The accepted decision file and index heading match exactly,
  `CURRENT_TASK.md` alone owns the next task, and Gate 8 remains the sole
  `Specified - Next` marker.
- The known at-least-once boundary is unchanged: a child that persists a RunRecord and
  dies before publishing its result may leave an orphan and be re-executed. Failed,
  corrupt, timed-out, or incomplete current cycles retain their spool; no time-based
  cleanup service or Evidence/RunRecord deletion was added.
- Structural before document synchronization: `git diff --check` clean.

## Durable Runtime Control-Request Admission Verification

- Aligned RED first: the focused runtime command reached production compilation and
  failed at `compileTestJava` with 28 missing-contract errors for
  `recordControlRequest`, `controlRequests`, `MAX_CONTROL_REQUESTS`, and
  `RuntimeControlAdmissionHandler`. The failures were confined to the active durable
  control-request task and no unrelated production failure was absorbed.
- Focused GREEN passed 23 tests across `DurableAgentRuntimeTest`,
  `FileSystemAgentRuntimeStateStoreIntegrationTest`, and
  `RuntimeControlAdmissionIntegrationTest`, with no failure, error, or skip.
- The lifecycle contract is executable: requests require an active Goal/AgentRun and
  exact work logical-run/correlation/causation binding; runtime identity collisions,
  changed-content identity reuse, terminal admission, and entry 257 fail closed. Exact
  replay does not advance revision, later lifecycle transitions retain the ledger, and
  a failed store update leaves both in-memory and durable prior revisions visible.
- The filesystem contract is executable: exact ordered envelopes including
  supplementary Unicode recover through a fresh store, updates cannot remove or rewrite
  the persisted prefix, and an integrity-valid schema-v1 payload missing the new ledger
  field fails closed.
- The named connection is executable: a real in-process control queue persists through
  `RuntimeControlAdmissionHandler`; a fresh bus replays its journal into a fresh
  filesystem-store instance without duplicate state, while two simulated store failures
  exhaust `RetryPolicy.of(2)` into one two-attempt dead letter and expose no request.
- Fresh full `.\\gradlew.bat clean build --warning-mode all` passed 72 suites and 358
  tests: 356 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0
  errors. All 8 build tasks executed, and production plus test compilation ran under the
  build-enforced Java 17 `-Xlint:all -Werror`.
- Document structural coverage in that full build passed: `DocumentOwnershipTest` 2/2,
  `DecisionLogIndexTest` 5/5, and `RepositoryTaskPlannerTest` 4/4. The accepted decision
  file and index identity match, `CURRENT_TASK.md` alone owns the subsequent task, and
  Gate 8 remains the sole `Specified - Next` marker.
- Scope held: the implementation records untrusted intent only. No Goal/AgentRun status,
  lease, fence, queue state, worker behavior, Tool scope, execution input, or bus
  cancellation state changes, and authenticated application remains Gate 12 work.
- Structural after implementation and document synchronization: `git diff --check`
  clean.

### Post-Review Lease Non-Interference Addendum

- Final diff review found that the first handler implementation used ordinary runtime
  recovery, whose existing expiry behavior could reclaim an expired executing lease
  before recording a control request. A new focused test failed exactly on the status
  assertion (`READY` observed instead of the persisted `EXECUTING` state), proving an
  indirect violation of the no-lease-change acceptance criterion.
- The minimum correction added a package-private control-admission recovery path that
  loads exact durable state without expiry reclamation; normal and matching runtime
  recovery retain their existing reclamation behavior. Focused GREEN then passed all 24
  tests across the three runtime/store/control suites, including exact lease/status/fence
  preservation after the handler clock has passed lease expiry.
- Fresh post-correction `.\\gradlew.bat clean build --warning-mode all` passed 72 suites
  and 359 tests: 357 passed, 2 existing Windows symbolic-link setup skips, 0 failures,
  and 0 errors. All 8 tasks executed under build-enforced strict lint, and
  `git diff --check` remained clean.

## Development Session Checkpoint Verification

- Aligned RED first: the focused command reached production compilation and failed at
  `compileTestJava` with 27 missing-contract errors for the checkpoint manager, immutable
  state, typed lifecycle, inspection, and conflict/corruption boundaries. No unrelated
  production failure was absorbed.
- Minimum GREEN introduced the `com.enhancer.session` contract, task-contract reader,
  artifact collector, atomic filesystem store, manager, and four CLI operations. The
  first focused session/CLI/argument run passed 8 tests under build-enforced
  `-Xlint:all -Werror` after the strict compiler correctly rejected two new exceptions
  until their serialization contracts were supplied.
- Forced-stop recovery is executable: a fresh manager/store recovers the exact pending
  step, last successful step, next action, evidence reference, task-contract match, and
  artifact match. Status, Verification, and Next edits leave the task contract identity
  stable, while an acceptance-scope edit fails closed without overwriting the checkpoint.
- Single-writer and retirement behavior is executable: a different run and stale
  expected revision fail closed; start never overwrites an active run; corruption fails
  closed; clear refuses non-stable state and post-stability artifact drift.
- Real-path containment is executable on this host: the new Windows junction test passes
  and proves `.enhancer` cannot redirect checkpoint reads or writes outside the project.
  The privilege-dependent symbolic-link setup is explicitly skipped, matching the host's
  existing link-test limitation.
- Actual-repository operation passed in a new JVM process: `checkpoint-show` recovered
  run `7a6d9392-7171-4d6c-abcc-0f802d97cf61` at revision 8 with
  `STEP_PENDING`, `run-focused-tests` as the last successful step,
  `taskContractMatches=true`, and `artifactMismatches=0` across 33 changed artifacts.
- Focused post-hardening boundary verification passed 7 tests: 6 passed, 1
  privilege-dependent symbolic-link setup skipped, 0 failures, and 0 errors. The Windows
  junction case executed rather than skipping.
- Fresh final `.\\gradlew.bat clean build --warning-mode all` passed 74 suites and 368
  tests: 365 passed, 3 privilege-dependent Windows symbolic-link setup skips, 0 failures,
  and 0 errors. All 8 build tasks executed; production and test compilation ran under
  Java 17 `-Xlint:all -Werror`, and document ownership, decision-index identity, and
  canonical next-task structure passed inside the full build.
- Scope held: the checkpoint is ignored local recovery metadata. It creates no task,
  approval, verification, maturity, commit, push, merge, or product-runtime authority;
  one active local session is the explicit first-increment ceiling.

## Fence-Checked External Effect Ledger Verification

- Aligned RED first: the focused command reached production compilation and failed at
  `compileTestJava` with 66 missing-contract errors for the effect request, record,
  status, durable ledger, store, state, and corruption/missing boundaries. The failure
  was confined to the active effect-ledger task.
- Initial focused GREEN passed 8 tests. Diff review then found that the filesystem store
  enforced revision advancement but could still accept a revision-valid removal or
  terminal-outcome rewrite through its public store port. A new hardening test failed at
  the expected `IOException` assertion because the invalid initial history was accepted.
- The minimum correction made initial state empty-only and every persisted successor
  either append one `PREPARED` record or change exactly one unchanged request from
  `PREPARED` to one terminal outcome. Removal, request rewrite, terminal replacement,
  no-op revision advancement, and multi-record changes now fail closed.
- Final focused verification passed 11 tests across
  `DurableExternalEffectLedgerTest` and
  `FileSystemExternalEffectLedgerIntegrationTest`: 11 passed, 0 failed, 0 errors, and 0
  skipped. The cases cover all four terminal outcomes, exact replay, key collisions,
  capacity and field bounds, Goal/AgentRun/WorkItem binding, stale/expired fences,
  persistence failure, fresh-store restart, supplementary Unicode, unresolved
  preparation, corruption, trailing/oversized/unsupported state, and history rewrite.
- Fresh `.\\gradlew.bat clean build` passed 76 suites and 379 tests: 376 passed, 3
  privilege-dependent Windows symbolic-link setup skips, 0 failures, and 0 errors. All
  8 build tasks executed; production and test compilation ran under build-enforced Java
  17 `-Xlint:all -Werror`, and document ownership plus decision-index checks ran inside
  the full build.
- Scope held: no external adapter or Tool was invoked, no payload content or credential
  was persisted, no retry or second AgentRun was introduced, and an unresolved
  `PREPARED` record remains explicit rather than being treated as safe automatic replay.

### Post-Synchronization Structural Addendum

- After canonical document synchronization, focused structural verification passed 11
  tests: `DocumentOwnershipTest` 2/2, `DecisionLogIndexTest` 5/5, and
  `RepositoryTaskPlannerTest` 4/4, with no failure, error, or skip. The accepted decision
  file and index heading match exactly, document ownership remains valid, the completed
  task alone owns the subsequent task, and Gate 8 remains the sole `Specified - Next`
  marker.
- Post-synchronization `git diff --check` produced no output and exited 0.

## Bounded AgentRun Retry Decision Verification

- Aligned RED first for the attempt bound: the focused
  `AgentRunRetryPolicyTest` command reached `compileTestJava` and failed with
  missing-symbol errors for `AgentRunRetryPolicy`, confined to the active task. The
  minimum implementation then passed 4 tests (lower/upper bound accepted, 0 and
  `MAX_ATTEMPTS + 1` rejected).
- The decision value and reason enum passed 4 tests
  (`AgentRunRetryDecisionTest`) after a corrected boolean accessor. A first
  compilation failed because a static `admitted()` factory and a same-signature
  instance `admitted()` accessor cannot coexist in Java; the accessor was renamed
  `isAdmitted()` while the `admitted()`/`refused(reason)` factories were kept.
- The pure decider passed 11 tests (`AgentRunRetryDeciderTest`) covering every
  behaviour-table row: admits a FAILED run with a resolved or empty ledger and
  remaining budget; refuses `NOT_FAILED`, `UNRESOLVED_EXTERNAL_EFFECT` (a
  `PREPARED` effect), `EFFECT_REQUIRES_USER_RECOVERY`, and `ATTEMPTS_EXHAUSTED`;
  the two safety precedences (a `PREPARED` effect beats an exhausted budget and
  beats a co-present `REQUIRES_USER_RECOVERY` effect); and the `completedAttempts`
  and null-disposition input bounds. Ledger states were built only through the
  real `ExternalEffectLedgerState` prepare/record-outcome transitions.
- Fresh `.\\gradlew.bat build` passed 79 suites and 398 tests: 395 passed, 3
  privilege-dependent Windows symbolic-link setup skips, 0 failures, and 0 errors,
  under build-enforced Java 17 `-Xlint:all -Werror`, with document ownership and
  decision-index checks inside the full build.
- Scope held: the decider creates, persists, and runs no AgentRun, mutates no
  queue/runtime/lease/fence/ledger state, resolves no ledger from a Goal, adds no
  durable store, schema, CLI, or production wiring, and grants no authority.

## Gate 8 Retry Specification Boundary Correction Verification

- Rewrote the attempt-level retry-decision specification and the durable
  multi-attempt lifecycle specification without changing production Java.
- The corrected contract separates failed AgentRun attempts from terminal Scheduler
  WorkItem disposition, keeps the queue item active through admitted retries, permits
  automatic retry only for empty or all-`COMPENSATED` effect history, and specifies
  schema-v2 immutable AgentRun and retry-decision prefixes plus recoverable finalizer,
  controller, and worker ordering.
- Fresh `.\gradlew.bat test --tests
  com.enhancer.architecture.DocumentOwnershipTest --tests
  com.enhancer.architecture.DecisionLogIndexTest` completed successfully: 7 tests
  across 2 suites, 0 skipped, 0 failures, and 0 errors. Raw XML is retained under
  `build/test-results/test/TEST-com.enhancer.architecture.DocumentOwnershipTest.xml`
  and `build/test-results/test/TEST-com.enhancer.architecture.DecisionLogIndexTest.xml`.
- `git diff --check` produced no output and exited 0.
- A stale-contract scan found no remaining specification declaration of the impossible
  instance `admitted()` accessor, terminal WorkItem disposition as retry input,
  all-APPLIED automatic safety, schema-v1 in-place lifecycle revision, or an unchanged
  finalizer assumption.
- Scope held: no production behavior, test behavior, capability maturity, runtime
  artifact, commit, push, merge, release, or deployment changed.

## Corrected Attempt-Level AgentRun Retry Decision Verification

- Aligned RED: fresh focused compilation failed with 16 errors confined to the corrected
  contract—`RuntimeAgentRun` could not be supplied to the old `WorkItemDisposition`
  signature and `NON_COMPENSATED_EXTERNAL_EFFECT` did not exist. No unrelated source,
  configuration, or test failure was present.
- Minimum GREEN changed only the pure decider, refusal enum, and focused test: the
  decision now consumes the exact attempt, validates Goal/WorkItem ledger binding,
  refuses `APPLIED`/`DEDUPLICATED`, and admits only empty/all-`COMPENSATED` history.
- Fresh focused `.\gradlew.bat test --rerun-tasks --tests
  com.enhancer.runtime.AgentRunRetryDeciderTest` passed 22 tests, 0 skipped, 0 failures,
  and 0 errors. Raw XML is
  `build/test-results/test/TEST-com.enhancer.runtime.AgentRunRetryDeciderTest.xml`.
- Fresh `.\gradlew.bat build --rerun-tasks` passed 79 suites and 409 tests: 406 passed,
  3 privilege-dependent Windows symbolic-link setup skips, 0 failures, and 0 errors
  under build-enforced Java 17 `-Xlint:all -Werror`.
- Scope held: no schema-v2 history, retry decision persistence, `RETRY_PENDING`,
  finalizer/worker/queue change, external adapter, commit, push, merge, release, or
  deployment was added.

## Schema-V2 AgentRun History And Retry-Pending Parking Verification

- The original active task sequenced schema-v2 state before finalizer splitting, but
  failed-result semantics could not remain coherent if the old finalizer immediately
  failed the queue. The conflict was reported, and the user explicitly approved one
  combined bounded increment: schema-v2 state/storage, minimum finalizer split, and safe
  worker parking. The retry controller and replacement execution remained out of scope.
- Initial focused RED reached test compilation and failed with 21 aligned missing-symbol
  errors for the schema-v2 AgentRun/decision-history contract. A second focused RED for
  the finalizer boundary failed with 2 aligned missing-method errors. Neither run exposed
  an unrelated, flaky, configuration, privilege, or scope-expanding failure.
- The first focused GREEN run executed 34 tests with 1 failure. The failure identified an
  over-strict new invariant that rejected a valid reclaimed first `READY` attempt with a
  retained issued fence. Narrowing the prohibition to an initial `PLANNING` attempt kept
  Goal-wide fence safety while preserving the established reclaim contract; all 34 then
  passed.
- The first full build executed 416 tests with 3 failures and 3 existing Windows
  privilege skips. Each failure was a stale expectation that a non-Verified result
  immediately terminally failed the Goal/queue. Updating those tests to the approved
  `RETRY_PENDING` parking contract removed no production check and absorbed no unrelated
  behavior.
- Additional prefix-hardening coverage proved that integrity-valid runtime artifacts
  cannot rewrite earlier AgentRun history or retry-decision history. Final focused
  verification passed 36 tests across the state, filesystem store, finalizer, worker,
  runtime, and real-filesystem worker suites with 0 skips, failures, or errors.
- Fresh final `.\gradlew.bat build --rerun-tasks` passed 80 suites and 418 tests: 415
  passed, 3 privilege-dependent Windows symbolic-link setup cases skipped, 0 failures,
  and 0 errors under build-enforced Java 17 `-Xlint:all -Werror`. Raw XML is retained
  under `build/test-results/test` for that run's checkpoint evidence.
- After document synchronization, fresh structural verification passed 8 tests across
  `DocumentOwnershipTest`, `DecisionLogIndexTest`, and `RuntimePackageBoundaryTest`, with
  0 skips, failures, or errors. `git diff --check` produced no output and exited 0.
- Scope held: no durable retry controller, external-effect resolution, replacement
  identity generation/checkpoint, second AgentRun execution, retry-loop recovery,
  schema-v1 migration, authenticated control application, commit, push, merge, release,
  or deployment was added.
- Evidence-retention clarification: the final full-build output and counts above were
  read before the later filtered structural rerun. Gradle then replaced
  `build/test-results/test` with the 3 structural-suite XML files, so the full-build raw
  XML is no longer retained at that path; the observed fresh command result remains the
  basis of the recorded 80-suite/418-test claim.

## Schema-V2 Runtime Pre-Delivery Documentation Reconciliation

- Pre-delivery review found four stale present-tense descriptions left from the original
  single-attempt boundary: the Gate 8 Roadmap lifecycle bullet, the completed task's
  problem context, the compact architecture's target wording, and the AgentLoop result
  connection in Architecture/Project State. They now describe schema-v2 history,
  `RETRY_PENDING`, and absence of terminal queue disposition without changing runtime
  behavior, maturity, acceptance scope, or the next task.
- Fresh `.\gradlew.bat build --rerun-tasks` passed 80 suites and 418 tests: 415 passed,
  3 privilege-dependent Windows symbolic-link setup cases skipped, 0 failures, and 0
  errors under build-enforced Java 17 `-Xlint:all -Werror`. Raw XML is retained under
  `build/test-results/test` pending the authorized Git delivery steps.
- `git diff --check` produced no output and exited 0. No decision, handoff, source code,
  test behavior, schema, commit, push, merge, release, or deployment was changed by this
  reconciliation step.

## Durable AgentRun Retry Controller

- The focused RED command
  `.\gradlew.bat test --tests com.enhancer.runtime.DurableAgentRunRetryControllerTest`
  reached test compilation and failed with seven aligned missing-symbol errors for the
  not-yet-implemented controller. No unrelated, flaky, configuration, privilege, or
  scope-expanding failure appeared.
- Focused GREEN passed 13 tests covering exact runtime/ledger binding, all five external
  effect statuses, exhausted budget, non-retry-pending rejection, semantic-digest
  determinism and order sensitivity, immutable policy replay, missing/mismatched ledger
  failure, admitted/refused action replay, and action persistence-failure recovery.
- The first fresh `.\gradlew.bat clean test` passed 81 suites and 431 tests: 428 passed,
  3 privilege-dependent Windows symbolic-link setup cases skipped, 0 failures, and 0
  errors under build-enforced Java 17 `-Xlint:all -Werror`.
- Scope held: no worker wiring, replacement identity generation/checkpoint, replacement
  execution, lease acquisition, RunRecord production, queue disposition, external
  adapter execution, control application, schema migration, commit, push, merge,
  release, or deployment was added.
- After document synchronization, fresh `.\gradlew.bat build --rerun-tasks` passed all
  7 build tasks and 81 suites/431 tests: 428 passed, 3 existing privilege-dependent
  Windows symbolic-link setup cases skipped, 0 failures, and 0 errors. This reran the
  build-enforced Java 17 `-Xlint:all -Werror` compilation and all structural guards.
- A separate fresh structural rerun passed 8 tests across `DocumentOwnershipTest`,
  `DecisionLogIndexTest`, and `RuntimePackageBoundaryTest`, with 0 skips, failures, or
  errors. `git diff --check` produced no output and exited 0 before this append.

## Retry-Aware AgentRun Worker Recovery

- Focused RED reached test compilation and failed with four aligned missing-signature
  errors: the checkpoint had no replacement identity and the Worker had no explicit
  ledger/policy wiring. No unrelated, flaky, configuration, privilege, or scope-expanding
  failure appeared.
- The first focused GREEN passed 27 tests across four Worker/checkpoint/filesystem suites.
  Expanded focused verification passed 46 tests across five suites after adding recovery
  cases for `RETRY_PENDING`, persisted decision, checkpointed replacement identity,
  appended replacement, checkpoint rollover, and unresolved `PREPARED` effect refusal.
- The named `FileSystemAgentRunWorkerIntegrationTest` path uses real queue, runtime,
  external-effect ledger, RunRecord, and schema-v2 checkpoint stores: the first attempt
  fails, the admitted replacement verifies, one final `VERIFIED_COMPLETED` disposition
  releases the dependent, and the checkpoint clears.
- Fresh `.\gradlew.bat clean test` passed 81 suites and 440 tests: 437 passed, 3 existing
  privilege-dependent Windows symbolic-link setup cases skipped, 0 failures, and 0 errors
  under build-enforced Java 17 `-Xlint:all -Werror`.
- Scope held: no external adapter invocation, automatic compensation, cross-attempt
  effect-key reuse, authenticated control application, backoff, broader budget, runtime
  or queue schema change, supported CLI, multi-process worker, commit, push, merge,
  release, or deployment was added.
- After document synchronization, fresh `.\gradlew.bat build --rerun-tasks` passed all
  7 build tasks and 81 suites/440 tests: 437 passed, 3 existing privilege-dependent
  Windows symbolic-link setup cases skipped, 0 failures, and 0 errors. This reran the
  build-enforced Java 17 `-Xlint:all -Werror` compilation and all structural guards.
- `git diff --check` produced no output and exited 0 after the final full build and before
  this append.

## Durable Work-Message Admission To The Scheduler Queue

- Focused RED reached test compilation and failed with seven aligned missing-symbol
  errors for the not-yet-implemented `DurableWorkItemAdmissionHandler`. No unrelated,
  flaky, configuration, privilege, or scope-expanding failure appeared.
- The first focused GREEN passed 4 tests across the new handler and real-filesystem
  integration suites. Expanded focused verification passed 27 tests across 7 handler,
  existing/new messaging integration, queue-state, durable-queue, and filesystem-store
  suites with 0 skips, failures, or errors.
- The named `DurableMessagingRuntimeIntegrationTest` path loads a real repository-derived
  approved task and Workspace snapshot, publishes through the real in-process queue,
  persists through `FileSystemSchedulerQueueStore`, and recovers and claims the exact
  unchanged WorkItem from a fresh durable queue instance. Same-bus replay is duplicate;
  fresh-bus re-delivery is explicitly tested as a fail-closed dead letter with one queue
  revision and one pending item rather than a second admission.
- Fresh `.\gradlew.bat build --rerun-tasks` passed all 7 build tasks and 83 suites/445
  tests: 442 passed, 3 existing privilege-dependent Windows symbolic-link setup cases
  skipped, 0 failures, and 0 errors under build-enforced Java 17 `-Xlint:all -Werror`.
- After document synchronization, a fresh structural/self-hosting rerun passed 20 tests
  across `DocumentOwnershipTest`, `DecisionLogIndexTest`,
  `RuntimePackageBoundaryTest`, `ProjectContextReaderTest`, and
  `RepositoryTaskPlannerTest`: 19 passed, 1 existing Windows symbolic-link setup case
  skipped, 0 failures, and 0 errors. `git diff --check` produced no output and exited 0
  before this append.
- Scope held: no supported Scheduler command, worker polling loop, authenticated control
  application, external adapter/effect execution, exact cross-bus admission history,
  arbitrary dependencies, queue schema migration, Gate 9, commit, push, merge, release,
  or deployment was added.

## Recoverable Process-Isolated Scheduler Cycle CLI

- Focused RED reached test compilation and failed with the aligned missing-symbol error
  for the not-yet-implemented `SchedulerCycleCliCommand`. No unrelated, flaky,
  configuration, privilege, or scope-expanding failure appeared.
- Focused GREEN passed the argument and CLI integration suites. The named
  `EnhancerCliSchedulerCycleIntegrationTest` admits work through the production durable
  handler, resumes an already persisted cycle-intent prefix, invokes the real child JVM,
  resolves one RunRecord, observes verified or failed terminal queue disposition, and
  verifies checkpoint cleanup; empty and missing-queue branches are also covered.
- The first fresh full regression passed 84 suites and 451 tests: 448 passed, 3 existing
  privilege-dependent Windows symbolic-link setup cases skipped, 0 failures, and 0
  errors under build-enforced Java 17 strict lint.
- After document synchronization, fresh `.\scripts\gradle.ps1 build --rerun-tasks`
  passed all 7 build tasks and the same 84 suites/451 tests with 3 existing skips, 0
  failures, and 0 errors. A separate fresh structural/self-hosting rerun passed 20 tests
  across `DocumentOwnershipTest`, `DecisionLogIndexTest`,
  `RuntimePackageBoundaryTest`, `ProjectContextReaderTest`, and
  `RepositoryTaskPlannerTest`: 19 passed, 1 existing Windows privilege skip, 0 failures,
  and 0 errors. `git diff --check` produced no output and exited 0 before this append.
- Scope held: the command recovers only an existing queue and runs one cycle. No queue
  creation, work submission, durable bus journal, exact cross-bus replay, polling
  service, authenticated control application, external adapter/effect execution, Gate 9,
  whole-gate Operational promotion, commit, push, merge, release, or deployment was
  added.
