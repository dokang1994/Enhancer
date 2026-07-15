# Project State

## Updated At

2026-07-15

## Repository State

- Repository root: `C:/Enhancer`.
- Current branch: `main` tracking `origin/main`.
- Gate 1-3 delivery commit: `3fcda4c` (`feat: integrate governed agent execution foundations`).
- Pull request #2 has been merged into `main`.
- Delivery Gates 1 through 3, self-hosting compatibility recovery, long-term vision, and documentation-alignment changes are published on `origin/main`.
- Delivery Gate 4 sequential verification, RunRecord changes, and provider-neutral Agent orchestration reference alignment are merged into and published on `origin/main` through delivery commit `f731afc`.
- Pre-operational Gradle and execution hardening is published on `origin/main` through `b504ba4`.
- Delivery Gate 5, Gate 0 integration promotion, and the RED workflow clarification are published on `origin/main` through delivery commit `ed901f3`.
- Delivery Gate 6 metadata-only WorkspaceSnapshot contract and its Contract Verified evidence are published on `origin/main` through delivery commit `c5a16b9`.
- The Gate 6 read-only `ProjectBrainView` aggregate, `RepositoryMemorySnapshotCollector`, production CLI composition, graph projection contract, and task impact query are published on `origin/main` through delivery commit `d3b6197` (`feat: integrate Gate 6 project brain foundations`).
- The Gate 6 `RunEvidenceGraphProducer`, `AcceptedDecisionProjector`, `RunRecordMetadataCollector`, store `references()` listing, and production graph composition are published on `origin/main` through delivery commit `396665b` (`feat: operationalize Gate 6 graph production`).
- The Gate 6 sub-capability promotion audit is published through documentation commits `59d0c05` and `6a75545`.
- The Gate 6 `TaskJustificationProjector` and `Justified By` reference grammar are published on `origin/main` through delivery commit `0e2be2c` (`feat: link tasks to decisions through explicit justification references`).
- The Gate 6 authority-boundary evidence, `TargetFileMetadataCollector`, and `GitWorkspaceCollector` are published on `origin/main` through delivery commit `21e6230` (`feat: complete Gate 6 workspace observation surface`).
- The Gate 6 maturity assessment, the re-scope-and-promotion, and the Gate 7 `MessageEnvelope` contract are published on `origin/main` through delivery commit `3423201` (`feat: promote Gate 6 and open Gate 7 with the message envelope contract`).
- Build system: Gradle 8.4 Wrapper with Java 17.
- Production source: 113 Java files and 6,251 lines.
- Test source: 44 Java files and 6,533 lines.
- The Gate 7 in-process delivery surface and its delivery-failure and dead-letter handling are published on `origin/main` through delivery commit `b278c53` (`feat: add Gate 7 in-process delivery with failure isolation and dead-letter`); the unrelated wall-clock test correction is published through `2a69182` (`fix: make RunRecordMetadataCollectorTest time-independent`).

## Capability Maturity

### Contract Verified

- Delivery Gate 7 versioned reference-only `MessageEnvelope` under `com.enhancer.bus`: canonical-UUID message identity, bounded correlation/run/producer identities, optional canonical-UUID causation distinct from the message identity, and one typed payload.
- Sealed four-kind payload hierarchy (work, result, control, handoff) carrying task revisions, snapshot identities, immutable allowed-tool scopes, run-record references, verification status, and typed control signals as bounded data; no content, delivery semantics, or Tool authority.
- Delivery Gate 7 in-process delivery surface `InProcessMessageBus` under `com.enhancer.bus`: synchronous single-threaded topic fan-out in registration order and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; envelopes are carried unmutated so authorization and provenance survive every hop.
- Delivery Gate 7 delivery-failure isolation and dead-letter capture: a subscriber handler that throws yields a `FAILED` outcome and an ordered immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason) while fan-out continues; a failed delivery consumes the idempotency key and is terminal, reporting `DUPLICATE` with no further dead letter on re-publish or replay.
- Automatic retry and re-delivery from the dead-letter record, cancellation propagation, ordering beyond registration, backpressure, competing queue consumers, threading, journal persistence, and IPC transport remain outside these verified contracts.

### Integrated

- Delivery Gate 6 metadata-only immutable Workspace snapshot contract under `com.enhancer.workspace`: approved task source revision provenance, typed source metadata, explicit Available/Stale/Unavailable states, deterministic ordering, bounds, temporal validation, and versioned canonical SHA-256 identity; connected to the real Context Reader upstream and the view, producer, and query downstream through `WorkspaceCollectionIntegrationTest` and the production CLI suites.
- Delivery Gate 6 read-only `ProjectBrainView` aggregate: composed from one real snapshot, one real `ProjectContext`, and the real persisted `RunRecord` of a real governed run, with derived memory freshness and approved-task mismatch rejection.
- Delivery Gate 6 graph projection contract: five typed node kinds, six endpoint-checked edge kinds over the Decision, Architecture, Dependency, Task, and Execution relationship domains, snapshot-keyed versioned projections, and element provenance with derived rebuild status; populated exclusively through the real producer chain in integration and production-CLI tests.
- Delivery Gate 6 `TaskImpactQuery`: answers the task-to-decision-to-code-to-test chain over really-produced graphs, naming the real stored execution, with deduplication, modified-artifact-restricted `VERIFIED_BY` traversal, and rebuild status derived from every traversed element; transitive `DEPENDS_ON` closure remains deferred by decision.
- Delivery Gate 6 `AcceptedDecisionProjector`: accepted decisions parsed from a real decision log through the real Context Reader and merged into the production graph output.
- Delivery Gate 6 `RunRecordMetadataCollector` and store `references()` listing: observations produced against the real filesystem RunRecord store, with a really-persisted prior record observed on the production CLI path and corruption surfaced as explicit `UNAVAILABLE`.
- Delivery Gate 6 `TaskJustificationProjector` and the optional `Justified By` task-document section: explicit references to accepted-decision headings projected into `JUSTIFIED_BY` edges with task-document provenance and snapshot-relative freshness, with strict rejection of empty, non-bullet, duplicate, or unresolved references; the first real reference resolved on the actual repository through the production CLI path.
- Gate 6 boundaries that remain outside these integrations: source payloads, Git/diagnostics/selection/terminal adapters, graph persistence, confidence metadata, and modifies/verified-by/supersedes/depends-on projection, each requiring its own evidence source.

- Delivery Gate 6 repository-memory Workspace path: `RepositoryMemorySnapshotCollector` derives a real snapshot from really-loaded Context Reader memory, and the composed `ProjectBrainView` explains a real governed run including explicit divergence detection.
- The collector reads no files, retains no content, derives the `ApprovedTaskRevision` from the same loaded memory, and reuses `WorkspaceSnapshot.capture` for identity and bounds.
- Delivery Gate 0 authority-preserving foundation lifecycle integration.
- Repository Context Reader with seven `.ai/` documents followed by eight canonical root documents.
- Deterministic Task Planner using Delivery Gate/Specified - Next grammar and explicit proposal state.
- Single-pass Assisted Development Loop.
- Repeated Agent Loop completion, failure, iteration, and stagnation exits.
- Bounded `ToolResult` and `VerificationEvidence` invariants.
- Delivery Gate 1 bounded read-only Tool Execution Boundary.
- Immutable `ToolRequest` with correlation identity and arguments.
- Immutable `ExecutionPolicy` with deny-over-allow policy, project root, size, timeout, and cancellation boundaries.
- Unique in-process `ToolExecutor` registry with bounded structured failure conversion.
- `ReadFileTool` request-to-policy-to-executor-to-real-file-to-result flow.
- Relative-path, traversal, real-path containment, regular-file, size, and strict UTF-8 checks.
- Delivery Gate 2 atomic complete-evidence persistence and restart-safe resolution.
- UUID run/evidence identities, opaque references, creation time, UTF-8 byte length, and SHA-256 metadata.
- Missing, malformed, oversized, length-mismatched, digest-mismatched, and invalid-UTF-8 evidence rejection.
- Large `ReadFileTool` output connected through `EvidenceRecorder` to a resolvable full-output reference.
- Delivery Gate 3 Tool-result-driven Agent Loop integration.
- `AgentRunState` with approved task, pending request, last result, explicit status, and deterministic progress key.
- `AgentRunController` orchestration over an existing executor, immutable policy, and external failure classifier.
- Successful Tool execution stops at `AWAITING_VERIFICATION`; retryable and terminal failures remain distinct.
- Existing maximum-iteration and stagnation exits operate over real Tool results.
- Repository-derived `ApprovedTask` identity, approval evidence, and Tool-name scope.
- Structured Tool failure codes and a standard retry policy without prose parsing.
- SHA-256 evidence content identity and semantic progress independent of storage references.
- Private Agent run construction with public ready-state creation only.
- Delivery Gate 4 sequential independent verification and durable RunRecord replay.
- Typed Verified, Rejected, Unverified, and Not Performed decisions with structured reason codes.
- Deterministic read verifier over inline or integrity-checked referenced complete evidence.
- Executed Tool request retention across worker terminal states.
- Verified-only completion through `AgentRunFinalizer` outside the worker controller.
- Immutable policy snapshot and decision recorded with task, request, Tool result, expected digest, verification, iterations, and stop reasons.
- Atomic versioned RunRecord envelopes with SHA-256 integrity and restart-safe replay.
- Controller-bound execution policy retained in the non-publicly constructible `AgentRunResult`.
- RunRecord lifecycle validation that rejects policy-history substitution and impossible worker, verification, result, and stop-reason combinations.
- Gradle 9-compatible explicit JUnit Platform Launcher runtime and workspace-local default test temporary storage.
- Invocation-isolated Tool workers that prevent interruption-ignoring timeout starvation.
- Millisecond-positive and nanosecond-representable execution-policy timeouts.
- Complete-envelope Evidence and RunRecord integrity digests covering version, timestamp, declared length, and content/payload.
- Strict RunRecord UTF-8 encoding and bounded, real-root-contained, strict UTF-8 startup-context loading.

### Operational

- Delivery Gate 5 first supported local CLI over the integrated read-only vertical slice.
- `EnhancerCli run` requires explicit governed project, active task identity, target, expected digest, evidence root, and RunRecord root inputs.
- `EnhancerCli replay` resolves integrity-checked records without Tool re-execution or chat history.
- Stable process exit codes, 4096-character diagnostics, verified-only completion, and persist-before-report behavior.

### Operational Governance

- Constitution 1.1 Kernel and Document Driven Development.
- Explicit lifecycle, authorization, fresh-evidence, self-hosting, recovery, and amendment rules.
- Git-backed project memory and session handoff.
- RED failures are classified against active task authority, accepted decisions, Architecture, and repository settings before aligned missing implementation proceeds to the minimum GREEN change.

## Accepted Product Direction

- Enhancer OS is an event-driven AI development platform, not a Chat -> Tool -> Stop wrapper.
- The target platform includes Desktop, CLI, API, Workspace, Project Brain, Memory, MCP Server/Client, Agent Runtime, Event/Message Bus with IPC adapters, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.
- Event Bus defines domain semantics, Message Bus defines delivery, and IPC is a transport adapter for the same versioned envelope.
- Runtime Agents will communicate through queues rather than direct Agent-to-Agent calls after the messaging boundary exists.
- Agent orchestration escalates only as needed from one worker to sequential work, Producer-Reviewer, bounded fan-out/fan-in, expert routing or supervisor allocation, and at most one subordinate coordination layer.
- One Kernel coordinator owns terminal run state; every worker shares an immutable input snapshot and approved task revision through typed versioned handoffs with bounded authority, budgets, evidence, and recovery state.
- Archon `263cf365` and meta-harness `ccab9a6` are pinned design references, not runtime, prompt, Skill, storage, provider, or governance dependencies.
- Workspace will expose governed file, Git, diagnostic, terminal-metadata, and selection snapshots; Project Brain will combine them with repository memory and RunRecords while preserving provenance.
- The owner's rough 20-25% foundation estimate is qualitative planning context, not verified maturity or completion evidence.
- Product milestones are V1 AI Development Experience, V2 AI Development Platform, and V3 AI Operating System.
- Product milestones describe user-visible outcomes, while Delivery Gates define dependency-ordered implementation and promotion; their numbering is not a claim that every V1 surface precedes all V2 foundations.
- The AI Kernel target owns Agent/workflow lifecycle, context and memory resources, locks and leases, scheduling, cancellation, recovery, policy, verification gates, and audit state.
- Project Brain will expose rebuildable Decision, Architecture, Dependency, Task, and Execution graph projections while Git and canonical documents remain authoritative.
- Agent plugins, Skills, Tools, and workflows are distinct extension types with separate authority and provenance.
- The Model Router target selects approved local or remote providers using capability, data classification, policy, cost, latency, context, and availability; sensitive code defaults local.
- Self-hosting development means applying Enhancer's governed workflow to its own repository; local or hybrid model execution is a separate provider-routing capability.

## Not Yet Integrated Or Operational

- Prompt and LLM invocation.
- Workspace collection and Project Brain integration, Event/Message Bus, IPC, Agent Runtime, Scheduler, and Model Gateway.
- Project Brain graph storage and impact reasoning, Dependency Analyzer, Workflow Engine, Agent Marketplace, and privacy-aware hybrid model routing.
- Skill loading runtime, plugins, MCP, multi-agent, background execution, Cloud Sync, and governed self-improvement.
- CI/CD and released distribution.

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

## Current Delivery Position

- Delivery Gate 0: Integrated.
- Delivery Gate 1: Integrated.
- Delivery Gate 2: Integrated.
- Delivery Gate 3: Integrated.
- Delivery Gate 4: Integrated.
- Delivery Gate 5: Operational.
- Delivery Gate 6: Integrated by the 2026-07-15 re-scope-and-promotion decision; diagnostics, terminal-session, and active/selected-file observation moved to Gate 12.
- Delivery Gate 7: Specified - Next; its `MessageEnvelope` contract, its `InProcessMessageBus` deterministic topic/queue delivery with idempotency and journal replay, and its delivery-failure isolation with dead-letter capture are Contract Verified, with no automatic retry, cancellation, ordering, backpressure, persistence, or IPC transport implementation and no wiring into the CLI or Agent Loop.
- Gate 6 `WorkspaceSnapshot`, `ProjectBrainView`, graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector` sub-capabilities: Integrated through the fresh promotion audit `gate-6-sub-capability-integration-promotion`, each connected to real upstream and downstream components by named integration evidence.
- Gate 6 `TaskJustificationProjector` and the `Justified By` reference grammar: Integrated; the first real reference resolved on the actual repository through the production composition.
- Gate 6 authority boundary: the exit criterion "Workspace observations cannot override repository authority or grant Tool permission" is pinned by `WorkspaceAuthorityBoundaryIntegrationTest`.
- Gate 6 `TargetFileMetadataCollector`: Integrated on the production CLI path; the run target is observed pre-run with a real containment-checked digest.
- Gate 6 `GitWorkspaceCollector`: Integrated on the production CLI path under explicitly granted read-only external command authority; Git state is observed at digest granularity with discovery confined to the project root.
- Gate 6 repository-memory path (real governed run -> real Context Reader memory -> collector -> composed view with divergence detection): Integrated through `WorkspaceCollectionIntegrationTest`.
- Gate 6 run-evidence graph production path (real governed run -> real snapshot -> producer -> impact-query answer naming the real stored execution): Integrated through the extended `WorkspaceCollectionIntegrationTest`.
- Gate 6 production view composition: Operational for the governed read-only CLI scenario; every recorded `run` composes the view and reports bounded snapshot identity, observation count, and memory freshness.
- Gate 6 production graph composition: Operational for the governed read-only CLI scenario; every recorded `run` observes prior run records, the run target, and Git state into the snapshot, merges decision nodes and justification edges into the produced graph, and reports bounded graph and impact counts.
- Enhancer has one Operational read-only scenario; the broader Agent Runtime remains planned.
- Gate 0 integration audit is verified without a production correction or second orchestrator and does not displace Gate 6.

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

## Next Task

Activate the next Delivery Gate 7 increment under separate explicit activation: deterministic in-process topic and queue delivery over the Contract Verified envelopes with idempotency and replay contracts.

## Session Recovery

Read in repository order:

1. `.ai/`
2. `CONSTITUTION.md`
3. `AGENTS.md`
4. `ARCHITECTURE.md`
5. `PROJECT_STATE.md`
6. `ROADMAP.md`
7. `CURRENT_TASK.md`
8. `DECISION_LOG.md`
9. `SESSION_HANDOFF.md`

Do not commit or push unless the user explicitly requests it.
