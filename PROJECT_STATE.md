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
- Build system: Gradle 8.4 Wrapper with Java 17.
- Production source: 91 Java files and 4,932 lines.
- Test source: 34 Java files and 4,827 lines.

## Capability Maturity

### Contract Verified

- Delivery Gate 6 metadata-only immutable Workspace snapshot contract under `com.enhancer.workspace`.
- Approved task source revision provenance, typed repository/editor/Git/diagnostic/terminal/RunRecord source metadata, and explicit Available/Stale/Unavailable states.
- Normalized absolute project roots, deterministic observation ordering, duplicate and 4096-entry bounds, temporal validation, and versioned canonical SHA-256 snapshot identity.
- Source payloads, adapters, authority, and persistence remain outside this verified contract.
- Delivery Gate 6 read-only `ProjectBrainView` aggregate under `com.enhancer.brain`, composed from one real `WorkspaceSnapshot`, one real `ProjectContext`, and one real `RunRecord`.
- Repository-memory projection to path, read order, and computed SHA-256 with derived `SNAPSHOT_MATCHED`, `SNAPSHOT_DIVERGED`, and `NOT_OBSERVED` freshness; document content is not retained.
- RunRecord projection to logical run identity, record time, approved task identity, and verification status; approved-task mismatch against the snapshot revision is rejected.
- Git/diagnostics/selection/terminal adapters, a production composition path, graph producers, and persistence remain outside this verified contract.
- Delivery Gate 6 graph projection contract under `com.enhancer.brain`: five typed node kinds, six endpoint-checked edge kinds over the Decision, Architecture, Dependency, Task, and Execution relationship domains, and element provenance with source reference, optional SHA-256 revision, explicit freshness, and derived rebuild status.
- `ProjectBrainGraph.project` keys each projection to one valid snapshot identity with an explicit projection time and version, orders elements deterministically, and rejects duplicates, self-loops, unknown endpoints, endpoint-kind violations, and more than 4096 elements.
- Graph producers, persistence, and confidence metadata remain outside this verified contract.
- Delivery Gate 6 task impact query: `TaskImpactQuery` answers the task-to-decision-to-code-to-test chain over one projected graph, returning an immutable `TaskImpact` with the graph's source snapshot identity and a rebuild-required status derived from every traversed element.
- The query deduplicates shared verifying artifacts, restricts `VERIFIED_BY` traversal to artifacts the task modifies, returns empty collections for edgeless tasks, and rejects unknown or non-task identities; transitive `DEPENDS_ON` closure is deferred by decision.

### Integrated

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
- Delivery Gate 6: Specified - Next.
- Gate 6 `WorkspaceSnapshot` sub-capability: Contract Verified; it is collected from really-loaded repository memory by `RepositoryMemorySnapshotCollector`.
- Gate 6 `ProjectBrainView` sub-capability: Contract Verified.
- Gate 6 repository-memory path (real governed run -> real Context Reader memory -> collector -> composed view with divergence detection): Integrated through `WorkspaceCollectionIntegrationTest`.
- Gate 6 production composition: Operational for the governed read-only CLI scenario; every recorded `run` composes the view and reports bounded snapshot identity, observation count, and memory freshness.
- Gate 6 graph projection contract: Contract Verified; consumed by the task impact query against contract-constructed graphs.
- Gate 6 task impact query: Contract Verified; no producer projects real repository evidence yet, so the query has no evidence over the actual project. The remaining Gate 6 scope keeps the gate `Specified - Next`.
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

Activate a separate Gate 6 increment: the first graph producer projecting real repository evidence (documents, RunRecords, snapshot observations) into the graph contract, or the next read-only source adapter. A Git status/diff adapter additionally requires an explicit decision on external command authority.

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
