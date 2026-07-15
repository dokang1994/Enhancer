# Current Task

## Status

Completed

## Task

Audit and promote the Contract Verified Gate 6 sub-capabilities to Integrated where fresh evidence shows real upstream and downstream components connected through integration tests, without changing production code or claiming Gate 6 itself beyond Specified - Next.

## Task ID

gate-6-sub-capability-integration-promotion

## Context

Six Gate 6 sub-capabilities are recorded as Contract Verified: the `WorkspaceSnapshot` contract, the `ProjectBrainView` aggregate, the graph projection contract, the `TaskImpactQuery`, the `AcceptedDecisionProjector`, and the `RunRecordMetadataCollector`. Since those records were written, later increments connected each of them to real upstream and downstream components: the real Context Reader, the real CLI run pipeline, the real filesystem RunRecord store, and really-produced graphs, exercised by integration tests and two actual-repository governed runs.

The Roadmap state model defines Integrated as "real upstream and downstream components are connected", evidenced by integration tests. Following the Gate 0 promotion precedent, this task is an audit: map each Contract Verified sub-capability to the specific integration evidence that connects it, re-run that evidence fresh, and promote exactly what the evidence supports. If the audit finds a sub-capability without a real connecting path, it stays Contract Verified and the gap is reported.

## Acceptance Criteria

- Record an accepted decision defining the promotion criteria and the per-sub-capability evidence mapping.
- Re-run fresh focused verification for every connecting suite: workspace, brain, run, CLI, and integration.
- Re-run the full Gradle regression with `--warning-mode all`, fresh XML inspection, and Java 17 `-Xlint:all -Werror`.
- Promote to Integrated only sub-capabilities whose fresh evidence connects real upstream and downstream components; report any that do not qualify.
- Change no production code and no test unless the audit exposes a defect; a promotion claim must not be manufactured by new code written for this task.
- Preserve Delivery Gate 6 as the sole `Specified - Next` product gate and preserve the Operational records of the production view and graph composition.
- Synchronize `PROJECT_STATE.md`, `ROADMAP.md`, `ARCHITECTURE.md`, `SESSION_HANDOFF.md`, `.ai/architecture.md`, and `CHANGELOG.md` with the promoted maturity.
- Run `git diff --check` and confirm exactly one `Specified - Next` gate status marker remains.

## Out Of Scope

- New features, contracts, adapters, producers, or queries
- Production or test code changes except audit-exposed defect corrections
- Gate 6 gate-level promotion beyond `Specified - Next`
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the explicit request to run the promotion procedure for the Contract Verified sub-capabilities.

## Allowed Tools

- read-file

## Verification Plan

- This is a promotion audit; test-first does not apply because no behavior changes. The equivalent verification is: fresh focused suite runs mapped to each promotion claim, the full regression with fresh XML inspection, Java 17 `-Xlint:all -Werror`, structural marker checks, and `git diff --check`.
- Evidence mapping to be validated fresh:
  - `WorkspaceSnapshot`: `WorkspaceCollectionIntegrationTest` connects the real Context Reader and collector upstream to the view, producer, and impact query downstream; the CLI suites connect it on the production path.
  - `ProjectBrainView`: `WorkspaceCollectionIntegrationTest` and `EnhancerCliBrainCompositionTest` compose it from a real snapshot, real memory, and the real persisted RunRecord of a real governed run.
  - Graph projection contract: `WorkspaceCollectionIntegrationTest` and `EnhancerCliGraphCompositionTest` populate it exclusively through the real producer chain.
  - `TaskImpactQuery`: the same integration and CLI suites answer it over really-produced graphs naming the real stored execution.
  - `AcceptedDecisionProjector`: `EnhancerCliGraphCompositionTest` parses a real decision log through the real Context Reader and merges the nodes into the production graph output.
  - `RunRecordMetadataCollector`: `RunRecordMetadataCollectorTest` runs against the real filesystem store, and the `EnhancerCliGraphCompositionTest` second run observes a really-persisted prior record on the production path.

## Implementation Result

- Recorded the promotion criteria and per-sub-capability evidence mapping as an accepted decision.
- Changed no production or test code; the audit found no defect, so the diff contains documentation only.
- Promoted all six audited sub-capabilities to Integrated: `WorkspaceSnapshot`, `ProjectBrainView`, the graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector`, each connected to real upstream and downstream components by evidence that predates this task.
- Preserved Delivery Gate 6 as the sole `Specified - Next` product gate and the existing Operational records for the production view and graph composition.
- Synchronized `PROJECT_STATE.md`, `ROADMAP.md`, `ARCHITECTURE.md`, `SESSION_HANDOFF.md`, `.ai/architecture.md`, and `CHANGELOG.md`.

## Verification

- Fresh focused verification across the workspace, brain, run, CLI, and integration suites passed 19 suites and 59 tests with no skips, failures, or errors, confirmed against fresh XML output.
- The named connecting suites each passed fresh: `WorkspaceCollectionIntegrationTest` (1 test), `EnhancerCliBrainCompositionTest` (2), `EnhancerCliGraphCompositionTest` (1), `RunRecordMetadataCollectorTest` (5), `RunEvidenceGraphProducerTest` (3), `AcceptedDecisionProjectorTest` (4), and `TaskImpactQueryTest` (4).
- Fresh full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 38 suites and 140 tests: 138 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production compilation passed with `-Xlint:all -Werror`.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 6; `git diff --check` passed.
- Not claimed: Gate 6 gate-level promotion. The reference grammar, modifies/verified-by producers, remaining source adapters, persistence, and full gate exit criteria remain unevidenced.

## Next Task

Activate a separate Gate 6 increment: a task-to-decision reference grammar with its `JUSTIFIED_BY` projection, or the next read-only source adapter (a Git status/diff adapter additionally requires an explicit decision on external command authority).
