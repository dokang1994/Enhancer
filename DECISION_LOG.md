# Decision Log

Accepted decisions, one file each under `docs/decisions/`. This index carries the
canonical heading and acceptance status of every decision; the context, decision,
rationale, and consequences live in the linked file.

The heading text is the decision's identity. `CURRENT_TASK.md`'s `## Justified By`
bullets and the Project Brain decision graph both resolve against it by exact string,
so a heading must never be edited after acceptance. Add a new decision by appending
its file and an entry here; `DecisionLogIndexTest` fails the build if the two drift.

## Accepted Decisions

### 2026-07-24: Migrate The Pending-Finalization Checkpoint Before Other Gate 8 State

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-24-migrate-the-pending-finalization-checkpoint-before-other-gate-8-state.md)

### 2026-07-24: Assess Gate 8 Maturity Against Every Exit Criterion

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-24-assess-gate-8-maturity-against-every-exit-criterion.md)

### 2026-07-24: Project Invocation-Spool Recovery Through The Checkpoint-Correlated Cycle

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-24-project-invocation-spool-recovery-through-the-checkpoint-correlated-cycle.md)

### 2026-07-23: Project External-Effect Recovery Through The Checkpoint-Correlated Goal

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-project-external-effect-recovery-through-the-checkpoint-correlated-goal.md)

### 2026-07-23: Correlate Scheduler Recovery Prefixes Through A Read-Only Checkpoint-Anchored Projection

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-correlate-scheduler-recovery-prefixes-through-a-read-only-checkpoint-anchored-projection.md)

### 2026-07-23: Project Persisted Scheduler Queue State Through A Read-Only Status Command

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-project-persisted-scheduler-queue-state-through-a-read-only-status-command.md)

### 2026-07-23: Discover Recent RunRecords Through A Bounded Read-Only CLI Command

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-discover-recent-runrecords-through-a-bounded-read-only-cli-command.md)

### 2026-07-23: Serialize Filesystem Scheduler Queue Updates With A Non-Blocking Cross-Process Lock

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-serialize-filesystem-scheduler-queue-updates-with-a-non-blocking-cross-process-lock.md)

### 2026-07-23: Drain Ready Scheduler Work Through A Bounded Foreground Command

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-drain-ready-scheduler-work-through-a-bounded-foreground-command.md)

### 2026-07-23: Execute External Effects Through A Persist-First Evidence-Bound Adapter Boundary

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-23-execute-external-effects-through-a-persist-first-evidence-bound-adapter-boundary.md)

### 2026-07-22: Reuse The Immutable Submission Manifest As The Sole Generated-Input Recovery Record

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-reuse-the-immutable-submission-manifest-as-the-sole-generated-input-recovery-record.md)

### 2026-07-22: Expose Durable Submission As A Separate Explicit CLI Command

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-expose-durable-submission-as-a-separate-explicit-cli-command.md)

### 2026-07-22: Persist Submission Intent Before Creating The Scheduler Queue

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-persist-submission-intent-before-creating-the-scheduler-queue.md)

### 2026-07-22: Retain Exact Work Admission History In The Durable Queue

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-retain-exact-work-admission-history-in-the-durable-queue.md)

### 2026-07-22: Expose One Process-Isolated Durable Scheduler Cycle Through The CLI

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-expose-one-process-isolated-durable-scheduler-cycle-through-the-cli.md)

### 2026-07-22: Connect Work Admission To The Durable Scheduler Queue Through A Persist-First Handler

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-connect-work-admission-to-the-durable-scheduler-queue.md)

### 2026-07-22: Separate Retryable AgentRun Failure From Terminal WorkItem Disposition

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-separate-retryable-agentrun-failure-from-terminal-workitem-disposition.md)

### 2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-22-decide-bounded-agentrun-retry-on-attempt-budget-and-external-effect-resolution.md)

### 2026-07-21: Persist Fence-Checked External Effect Outcomes Before AgentRun Retry

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-21-persist-fence-checked-external-effect-outcomes-before-agentrun-retry.md)

### 2026-07-21: Persist Development Session Checkpoints Outside Canonical Project Documents

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-21-persist-development-session-checkpoints-outside-canonical-project-documents.md)

### 2026-07-21: Persist Bound Runtime Control Requests Without Applying Unauthenticated Transitions

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-21-persist-bound-runtime-control-requests-without-applying-unauthenticated-transitions.md)

### 2026-07-21: Select The Process-Isolated Durable Worker And Retire Spools After Checkpoint

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-21-select-the-process-isolated-durable-worker-and-retire-spools-after-checkpoint.md)

### 2026-07-20: Return Isolated Worker Results Through A Correlated Per-Cycle Spool With RunRecord As Authority

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-return-isolated-worker-results-through-a-correlated-per-cycle-spool.md)

### 2026-07-20: Isolate The Worker In A Bounded Self-JVM Child Process

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-isolate-the-worker-in-a-bounded-self-jvm-child-process.md)

### 2026-07-20: Carry The First Transport Hop Through A Local File Spool

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-carry-the-first-transport-hop-through-a-local-file-spool.md)

### 2026-07-20: Close The Audit Gaps And Make The Ownership Guard Run On Documentation Changes

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-close-the-audit-gaps-and-make-the-ownership-guard-run-on-documentation-changes.md)

### 2026-07-20: Split The Decision Log Into Per-Decision Files Behind A Heading-Only Index

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-split-the-decision-log-into-per-decision-files-behind-a-heading-only-index.md)

### 2026-07-20: Enforce Document Ownership With A Structural Test And State The Store Write-Root Contract Exactly

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-enforce-document-ownership-with-a-structural-test-and-state-the-store-write-root.md)

### 2026-07-20: Give Every Project Fact One Owning Document And Separate Verification Evidence From Current State

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-give-every-project-fact-one-owning-document-and-separate-verification-evidence-f.md)

### 2026-07-20: Record Caller-Supplied WorkPayload Execution Input Enabling Arbitrary-Target Worker Execution

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-record-caller-supplied-workpayload-execution-input-enabling-arbitrary-target-wor.md)

### 2026-07-20: Record AgentLoop-Backed Execution Port Running The Approved Source Document Through The Gate 1-4 Pipeline Without A Payload Schema Change

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-20-record-agentloop-backed-execution-port-running-the-approved-source-document-thro.md)

### 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-17-record-in-process-scheduler-worker-driving-one-recoverable-claim-to-disposition.md)

### 2026-07-17: Record RunRecord-Backed Result-Path Finalization Connecting Verified Outcome To Runtime Terminal State And Queue Disposition

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-17-record-runrecord-backed-result-path-finalization-connecting-verified-outcome-to.md)

### 2026-07-17: Record Durable Queue Terminal Disposition Distinguishing Verified Completion From Failure

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-17-record-durable-queue-terminal-disposition-distinguishing-verified-completion-fro.md)

### 2026-07-16: Separate Execution Acknowledgement From Verified Queue Completion And Sequence Remaining Connections

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-separate-execution-acknowledgement-from-verified-queue-completion-and-sequence-r.md)

### 2026-07-16: Bridge One Durable Queue Claim Into One Recoverable Leased AgentRun

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-bridge-one-durable-queue-claim-into-one-recoverable-leased-agentrun.md)

### 2026-07-16: Fence One AgentRun Owner Before Worker Execution

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-fence-one-agentrun-owner-before-worker-execution.md)

### 2026-07-16: Persist One Goal And One AgentRun Lifecycle Before Adding Worker Ownership

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-persist-one-goal-and-one-agentrun-lifecycle-before-adding-worker-ownership.md)

### 2026-07-16: Make Runtime Persistence And Verification Dependencies Acyclic

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-make-runtime-persistence-and-verification-dependencies-acyclic.md)

### 2026-07-16: Bound In-Process Tool Isolation Capacity

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-bound-in-process-tool-isolation-capacity.md)

### 2026-07-16: Harden Text And File Bounds During Production Operations

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-harden-text-and-file-bounds-during-production-operations.md)

### 2026-07-16: Persist Gate 8 Queue Transitions Before Exposing State

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-persist-gate-8-queue-transitions-before-exposing-state.md)

### 2026-07-16: Start Gate 8 Scheduling With A Dependency-Ready Single-Worker Queue

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-start-gate-8-scheduling-with-a-dependency-ready-single-worker-queue.md)

### 2026-07-16: Assess Gate 7 Integrated Maturity Against Every Real Connection

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-assess-gate-7-integrated-maturity-against-every-real-connection.md)

### 2026-07-16: Add Product Journeys Evaluation And Layered Security Across Delivery Gates

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-add-product-journeys-evaluation-and-layered-security-across-delivery-gates.md)

### 2026-07-16: Prepare Gate 7 Integration Through The First Runtime Admission Path

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-prepare-gate-7-integration-through-the-first-runtime-admission-path.md)

### 2026-07-16: Start Gate 8 With Immutable WorkItem Admission Over Gate 7 Envelopes

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-start-gate-8-with-immutable-workitem-admission-over-gate-7-envelopes.md)

### 2026-07-16: Promote Gate 7 To Contract Verified And Advance Gate 8

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-promote-gate-7-to-contract-verified-and-advance-gate-8.md)

### 2026-07-16: Bound Work Payload Tool Scope Cardinality

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-bound-work-payload-tool-scope-cardinality.md)

### 2026-07-16: Separate IPC Transport Acceptance From Message-Bus Delivery

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-separate-ipc-transport-acceptance-from-message-bus-delivery.md)

### 2026-07-16: Verify Real-Path Boundaries With Junctions And Remove Fictional Evidence Retention

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-verify-real-path-boundaries-with-junctions-and-remove-fictional-evidence-retenti.md)

### 2026-07-16: Bound Workspace RunRecord Observation To A Recent Window

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-bound-workspace-runrecord-observation-to-a-recent-window.md)

### 2026-07-16: Keep Durable Run Outcome Primary Across Project-Brain Composition

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-keep-durable-run-outcome-primary-across-project-brain-composition.md)

### 2026-07-16: Restrict Git Observation To A Trusted Executable And Filter-Free Plumbing

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-restrict-git-observation-to-a-trusted-executable-and-filter-free-plumbing.md)

### 2026-07-16: Bound Pending Publications With Deterministic Non-Blocking Refusal

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-bound-pending-publications-with-deterministic-non-blocking-refusal.md)

### 2026-07-16: Order Delivery By Running Each Publication To Completion Before Its Cascade

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-order-delivery-by-running-each-publication-to-completion-before-its-cascade.md)

### 2026-07-16: Propagate Cancellation As A Terminal Correlation-Scoped Delivery Refusal

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-propagate-cancellation-as-a-terminal-correlation-scoped-delivery-refusal.md)

### 2026-07-16: Add Bounded Synchronous Retry And Explicit Dead-Letter Re-Delivery

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-16-add-bounded-synchronous-retry-and-explicit-dead-letter-re-delivery.md)

### 2026-07-15: Isolate Delivery Failures To A Terminal Dead-Letter Before Adding Retry

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-isolate-delivery-failures-to-a-terminal-dead-letter-before-adding-retry.md)

### 2026-07-15: Deliver Gate 7 In-Process Messaging As A Deterministic Journal-Replayable Bus

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-deliver-gate-7-in-process-messaging-as-a-deterministic-journal-replayable-bus.md)

### 2026-07-15: Make RunRecord Observation Test Time-Independent

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-make-runrecord-observation-test-time-independent.md)

### 2026-07-15: Start Gate 7 With Reference-Only Versioned Envelopes And Exactly Four Payload Kinds

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-start-gate-7-with-reference-only-versioned-envelopes-and-exactly-four-payload-ki.md)

### 2026-07-15: Re-Scope Editor-Dependent Observations To Gate 12 And Promote Gate 6 As The Foundation

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-re-scope-editor-dependent-observations-to-gate-12-and-promote-gate-6-as-the-foun.md)

### 2026-07-15: Grant Read-Only Git Observation Through Two Fixed Commands With Digest-Only Retention

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-grant-read-only-git-observation-through-two-fixed-commands-with-digest-only-rete.md)

### 2026-07-15: Observe The Run Target With A Real Pre-Run Digest And Treat Containment Violations As Errors

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-observe-the-run-target-with-a-real-pre-run-digest-and-treat-containment-violatio.md)

### 2026-07-15: Pin The Workspace Authority Boundary With Characterization Evidence Instead Of New Mechanism

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-pin-the-workspace-authority-boundary-with-characterization-evidence-instead-of-n.md)

### 2026-07-15: Link Tasks To Decisions Only Through An Explicit Justified By Section

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-link-tasks-to-decisions-only-through-an-explicit-justified-by-section.md)

### 2026-07-15: Promote Gate 6 Sub-Capabilities Only Against Named Fresh Integration Evidence

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-promote-gate-6-sub-capabilities-only-against-named-fresh-integration-evidence.md)

### 2026-07-15: Compose The Production Graph From The Same Governed Inputs The Run Already Loads

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-compose-the-production-graph-from-the-same-governed-inputs-the-run-already-loads.md)

### 2026-07-15: Observe Stored Run Records As Explicit Metadata With Corruption Surfaced As Unavailable

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-observe-stored-run-records-as-explicit-metadata-with-corruption-surfaced-as-unav.md)

### 2026-07-15: Project Accepted Decisions As Unlinked Nodes With Snapshot-Relative Freshness

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-project-accepted-decisions-as-unlinked-nodes-with-snapshot-relative-freshness.md)

### 2026-07-15: Produce The First Real Graph Only From Elements The Run Evidence Actually Proves

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-produce-the-first-real-graph-only-from-elements-the-run-evidence-actually-proves.md)

### 2026-07-15: Answer The First Impact Query Over One Graph With Explicit Rebuild Status And No Transitive Closure

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-answer-the-first-impact-query-over-one-graph-with-explicit-rebuild-status-and-no.md)

### 2026-07-15: Constrain The First Graph Projection Contract To Typed Endpoint-Checked Metadata Keyed To One Snapshot

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-constrain-the-first-graph-projection-contract-to-typed-endpoint-checked-metadata.md)

### 2026-07-15: Compose The Project Brain View On The Existing CLI Run Path Without Widening Its Surface

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-compose-the-project-brain-view-on-the-existing-cli-run-path-without-widening-its.md)

### 2026-07-15: Collect The First Real Workspace Snapshot From Already-Loaded Repository Memory

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-collect-the-first-real-workspace-snapshot-from-already-loaded-repository-memory.md)

### 2026-07-15: Compose The First ProjectBrainView As A Derived Read-Only Aggregate Keyed To One Snapshot

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-compose-the-first-projectbrainview-as-a-derived-read-only-aggregate-keyed-to-one.md)

### 2026-07-15: Start Gate 6 With A Metadata-Only Content-Addressed Workspace Snapshot

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-start-gate-6-with-a-metadata-only-content-addressed-workspace-snapshot.md)

### 2026-07-15: Promote Gate 0 Only Through An Authority-Preserving Lifecycle Integration Audit

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-promote-gate-0-only-through-an-authority-preserving-lifecycle-integration-audit.md)

### 2026-07-15: Expose The Integrated Read-Only Run Through A Minimal Local CLI

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-expose-the-integrated-read-only-run-through-a-minimal-local-cli.md)

### 2026-07-15: Harden Integrated Boundaries Before The First Operational CLI

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-15-harden-integrated-boundaries-before-the-first-operational-cli.md)

### 2026-07-14: Translate External Orchestration Patterns Into Gate-Owned Enhancer Contracts

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-translate-external-orchestration-patterns-into-gate-owned-enhancer-contracts.md)

### 2026-07-14: Bind Run Records To The Policy Used During Execution

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-bind-run-records-to-the-policy-used-during-execution.md)

### 2026-07-14: Complete Agent Runs Only Through External Evidence Verification And Durable Records

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-complete-agent-runs-only-through-external-evidence-verification-and-durable-reco.md)

### 2026-07-14: Adopt V1-V3 Evolution And A Provenance-Preserving Project Brain

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-adopt-v1-v3-evolution-and-a-provenance-preserving-project-brain.md)

### 2026-07-14: Make Enhancer An Event-Driven Interoperable AI Operating Platform

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-make-enhancer-an-event-driven-interoperable-ai-operating-platform.md)

### 2026-07-14: Harden Agent Runs With Repository Approval And Semantic Progress

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-harden-agent-runs-with-repository-approval-and-semantic-progress.md)

### 2026-07-14: Stop Tool Success At The Independent Verification Boundary

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-stop-tool-success-at-the-independent-verification-boundary.md)

### 2026-07-14: Persist Complete Evidence As Atomic Integrity-Checked Envelopes

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-persist-complete-evidence-as-atomic-integrity-checked-envelopes.md)

### 2026-07-14: Restore Self-Hosting Context And Roadmap Compatibility Before Gate 2

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-restore-self-hosting-context-and-roadmap-compatibility-before-gate-2.md)

### 2026-07-14: Implement Gate 1 As A Bounded Read-Only Tool Boundary

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-implement-gate-1-as-a-bounded-read-only-tool-boundary.md)

### 2026-07-14: Promote Foundation Contracts Through Executable Vertical Slices

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-promote-foundation-contracts-through-executable-vertical-slices.md)

### 2026-07-14: Recover Existing Git Metadata Instead Of Initializing New History

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-recover-existing-git-metadata-instead-of-initializing-new-history.md)

### 2026-07-14: Restructure The Constitution As A Versioned Kernel

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-restructure-the-constitution-as-a-versioned-kernel.md)

### 2026-07-14: Colocate Examples With Specifications And Tests

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-colocate-examples-with-specifications-and-tests.md)

### 2026-07-14: Adopt External Agent Harness Patterns Selectively

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-adopt-external-agent-harness-patterns-selectively.md)

### 2026-07-14: Use Bounded Deterministic Agent Loop Termination

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-use-bounded-deterministic-agent-loop-termination.md)

### 2026-07-14: Bound Tool Verification Evidence

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-bound-tool-verification-evidence.md)

### 2026-07-14: Use A Repository Gradle Wrapper

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-use-a-repository-gradle-wrapper.md)

### 2026-07-14: Adopt Verified Skill And Evidence Operating Rules

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-14-adopt-verified-skill-and-evidence-operating-rules.md)

### 2026-07-12: Start Planner With Deterministic Repository Proposals

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-12-start-planner-with-deterministic-repository-proposals.md)

### 2026-07-12: Implement Context Reader As A Single Java Module

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-12-implement-context-reader-as-a-single-java-module.md)

### 2026-07-10: Manage Major Designs As RFCs

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-manage-major-designs-as-rfcs.md)

### 2026-07-10: Adopt Six-Month AI Development OS Roadmap

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-adopt-six-month-ai-development-os-roadmap.md)

### 2026-07-10: Read `.ai/` Before Every AI Work Session

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-read-ai-before-every-ai-work-session.md)

### 2026-07-10: Treat Docs As A Multi-Agent Prompt Book

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-treat-docs-as-a-multi-agent-prompt-book.md)

### 2026-07-10: Use Explicit Session Resume Protocol

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-use-explicit-session-resume-protocol.md)

### 2026-07-10: Operate Enhancer As A Real Open Source Project

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-operate-enhancer-as-a-real-open-source-project.md)

### 2026-07-10: Use Codex-Ready Chapter Documents

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-use-codex-ready-chapter-documents.md)

### 2026-07-10: Use Document Driven Development

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-use-document-driven-development.md)

### 2026-07-10: Build Enhancer As A Self-Hosting AI Development OS

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-build-enhancer-as-a-self-hosting-ai-development-os.md)

### 2026-07-10: Use Repository Documents As Durable Memory

Status: Accepted Decision

[Full decision](docs/decisions/2026-07-10-use-repository-documents-as-durable-memory.md)

## Proposals

- Define the product scope for Enhancer.
- Choose the initial implementation stack.
