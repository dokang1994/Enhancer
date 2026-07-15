# Current Task

## Status

Completed

## Task

Compose the produced Project Brain graph on the production CLI run path: include prior run-record observations in the collected snapshot, merge projected accepted-decision nodes into the run-evidence graph, answer the task impact query, and report bounded graph and impact metadata in the run output.

## Task ID

gate-6-production-graph-composition

## Context

The accepted-decision projection and run-record metadata observation are Contract Verified but have no production caller, and the CLI composes only the `ProjectBrainView`, not the graph. This is the third of the three approved increments and their named consumer.

The `run` path already loads repository memory, collects the snapshot, and persists the RunRecord. This increment constructs the RunRecord store before collection so prior records become `RUN_RECORD` observations in the snapshot, merges decision nodes from the same loaded memory into the produced graph, queries the task impact, and appends bounded counts to the existing output. No new command, argument, exit code, or authority is added.

## Acceptance Criteria

- Extend `RepositoryMemorySnapshotCollector` with an overload accepting additional caller-collected observations, keeping all existing invariants in `WorkspaceSnapshot.capture`.
- Extend `RunEvidenceGraphProducer` with an overload accepting additional evidence-backed nodes, keeping identity, ordering, and endpoint enforcement in `ProjectBrainGraph.project`.
- On the CLI `run` path, observe prior run records through `RunRecordMetadataCollector` before worker execution and include them in the collected snapshot.
- After finalization, project accepted decisions from the same loaded memory, produce the graph from the snapshot, the persisted record, and the decision nodes, and answer `TaskImpactQuery` for the active task.
- Append `graphNodes`, `graphEdges`, `graphDecisions`, and `impactExecutions` to the existing bounded run output; print no node identities, digests, or content.
- Keep existing output lines, exit codes, persist-before-report ordering, replay behavior, and the 4096-character bound unchanged.
- Add focused RED CLI tests before the behavior exists, including a second run observing the first run's record, then implement the minimum change.
- Run focused CLI, workspace, brain, and integration tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Execute one actual-repository governed `run` and record its graph and impact output as Operational evidence.
- Promote only the maturity that fresh evidence supports and keep Gate 6 `Specified - Next`.

## Out Of Scope

- Persisting graphs, snapshots, or impact results, and any RunRecord schema change
- `JUSTIFIED_BY`, `MODIFIES`, `VERIFIED_BY`, `SUPERSEDES`, or `DEPENDS_ON` projection
- New CLI commands, arguments, or exit codes
- Git status/diff, diagnostics, selection, or terminal adapters
- Tool permission, task approval, policy expansion, command execution, or mutation
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the request to continue with the next three repository-defined Gate 6 increments.

## Allowed Tools

- read-file

## Verification Plan

- Write focused CLI graph-composition tests asserting the new bounded output before the behavior exists, and confirm the expected assertion failure.
- Implement the minimum collector overload, producer overload, and CLI change.
- Run focused CLI, workspace, brain, and integration suites and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Run one governed `run` against this actual repository and inspect the reported graph and impact metadata.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Constructed the RunRecord store before snapshot collection on the CLI `run` path and observed prior records into the snapshot through `RunRecordMetadataCollector`.
- Extended `RepositoryMemorySnapshotCollector` and `RunEvidenceGraphProducer` with additional-observation and additional-node overloads, keeping all invariants in `WorkspaceSnapshot.capture` and `ProjectBrainGraph.project`.
- After finalization, projected accepted decisions from the same loaded memory, produced the graph from the snapshot, the persisted record, and the decision nodes, and answered `TaskImpactQuery` for the active task.
- Appended `graphNodes`, `graphEdges`, `graphDecisions`, and `impactExecutions` to the bounded run output; no node identities, digests, or content are printed, and commands, exit codes, and replay are unchanged.
- Recorded the production graph composition as Operational for the governed read-only CLI scenario while preserving Delivery Gate 6 as `Specified - Next`.

## Verification

- RED: both focused CLI graph-composition tests failed with the expected `output does not contain graphDecisions=` assertion while the runs completed.
- Focused GREEN: CLI, workspace, brain, and integration suites passed 17 suites and 50 tests with no skips, failures, or errors, confirmed against fresh XML output.
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 38 suites and 140 tests: 138 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production compilation passed with `-Xlint:all -Werror`.
- The second-run CLI test proved prior-record observation: `workspaceObservations` grew from 15 to 16 while graph counts stayed evidence-exact.
- Actual repository `run` on `README.md`: exit code 0, `COMPLETED`, `VERIFIED`, RunRecord `run-record/69977403-1cfb-45ba-ba0f-9239ad26a8c1`, `workspaceSnapshotId=d5bd10cb...a44632`, `workspaceObservations=17` (15 documents plus 2 prior run records), `memoryFreshness=matched=15,diverged=0,notObserved=0`, `graphNodes=61`, `graphEdges=1`, `graphDecisions=44` (matching the 44 `Status: Accepted Decision` lines), and `impactExecutions=1`.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 6; `git diff --check` passed.
- Known limitation, not a failure: decisions remain unlinked in impact answers because no task-to-decision reference grammar exists.

## Next Task

Activate a separate Gate 6 increment, subject to explicit activation. Candidates: a task-to-decision reference grammar and its `JUSTIFIED_BY` projection, or the next read-only source adapter (a Git status/diff adapter additionally requires an explicit decision on external command authority).
