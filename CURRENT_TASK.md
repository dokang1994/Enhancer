# Current Task

## Status

Completed

## Task

Adopt an explicit optional Justified By section in the active task document and project it into JUSTIFIED_BY task-to-decision edges, so impact answers carry justifying decisions backed by a machine-readable reference instead of inference.

## Task ID

gate-6-task-justification-references

## Justified By

- 2026-07-15: Link Tasks To Decisions Only Through An Explicit Justified By Section

## Context

Produced graphs carry decision nodes and impact answers name executions, but the decisions list is always empty because no evidence links the active task to the decisions that justify it. Every earlier increment refused to invent that linkage; this increment supplies the missing evidence source: an optional `## Justified By` section in `CURRENT_TASK.md` whose bullets name accepted-decision headings exactly.

A new `TaskJustificationProjector` parses that section from already-loaded repository memory, matches each reference against the projected accepted-decision nodes, and emits `JUSTIFIED_BY` edges from the task with task-document provenance and snapshot-relative freshness. The producer gains an additional-edges overload, and the CLI merges the edges and reports an `impactDecisions` count. This task document itself carries the first real reference.

## Acceptance Criteria

- Add a `TaskJustificationProjector` under `com.enhancer.brain` projecting `JUSTIFIED_BY` edges from one snapshot, one `ProjectContext`, and the projected accepted-decision nodes.
- Parse only an optional `## Justified By` section of the task source document; when absent, return no edges; when present, require at least one bullet and reject non-bullet or blank content.
- Require every reference to match a projected accepted-decision node identity exactly; reject unmatched and duplicate references.
- Give each edge task-document provenance: the task source document path, its computed SHA-256, and freshness derived against the snapshot's observation of that document.
- Extend `RunEvidenceGraphProducer` with an additional-edges overload, keeping all enforcement in `ProjectBrainGraph.project`.
- On the CLI `run` path, project justifications after finalization, merge them into the produced graph, and append an `impactDecisions` count to the bounded output.
- Keep commands, arguments, exit codes, replay, the `ApprovedTask` record, and the RunRecord schema unchanged.
- Add focused RED tests before the projector exists, classify the failure, then implement the minimum Java 17 change.
- Run focused brain, CLI, workspace, and integration tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Execute one actual-repository governed `run` whose output shows this task's own reference resolved into a justifying decision.
- Promote only the maturity that fresh evidence supports and keep Gate 6 `Specified - Next`.

## Out Of Scope

- Changes to `ApprovedTaskReader`, the `ApprovedTask` record, or any RunRecord or Evidence schema
- `SUPERSEDES`, `MODIFIES`, `VERIFIED_BY`, or `DEPENDS_ON` projection
- Git status/diff, diagnostics, selection, or terminal adapters
- Graph or impact persistence, cache, or index
- Tool permission, task approval, policy expansion, command execution, or mutation
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the request to continue with the repository-defined next Gate 6 increment.

## Allowed Tools

- read-file

## Verification Plan

- Write focused projector and CLI tests before the production types exist, and confirm the expected failure.
- Implement the minimum projector, producer overload, and CLI change.
- Run focused brain, CLI, workspace, and integration suites and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Run one governed `run` against this actual repository and confirm the reported justification counts.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Adopted the optional `## Justified By` task-document section and added `TaskJustificationProjector` under `com.enhancer.brain`: bullets naming accepted-decision headings exactly are projected into `JUSTIFIED_BY` edges from the task node.
- Rejected present-but-empty sections, non-bullet content, duplicate references, and references matching no projected accepted decision; an absent section returns no edges.
- Gave each edge task-document provenance with computed SHA-256 and snapshot-relative freshness, mirroring the decision projector's rules.
- Extended `RunEvidenceGraphProducer` with an additional-edges overload; all enforcement remains in `ProjectBrainGraph.project`.
- Merged justification edges into the production graph on the CLI `run` path and appended a bounded `impactDecisions` count; commands, exit codes, replay, `ApprovedTask`, and the RunRecord schema are unchanged.
- This task document carries the first real reference, resolved on the actual repository.

## Verification

- RED: the focused compile failed with 6 expected missing-symbol errors naming only the absent `TaskJustificationProjector`.
- Focused GREEN: brain, CLI, workspace, and integration suites passed 18 suites and 54 tests with no skips, failures, or errors, confirmed against fresh XML output.
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 39 suites and 144 tests: 142 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production compilation passed with `-Xlint:all -Werror`.
- Projector tests cover resolution, provenance, absent-section and unobserved-freshness behavior, empty/non-bullet/unresolved/duplicate rejection, null rejection, and end-to-end surfacing of a justifying decision in an impact answer.
- The production CLI test resolves a temp-project reference: `graphEdges=2` and `impactDecisions=1`.
- Actual repository `run` on `README.md`: exit code 0, `COMPLETED`, `VERIFIED`, RunRecord `run-record/4eec43bf-62e8-4ed8-bc2b-b94a67ff432b`, 18 observations (15 documents plus 3 prior run records), `graphNodes=63`, `graphEdges=2`, `graphDecisions=46`, `impactExecutions=1`, and `impactDecisions=1` — this task's own `Justified By` reference resolved into its justifying decision.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 6; `git diff --check` passed.

## Next Task

Activate a separate Gate 6 increment, subject to explicit activation. Candidates: the next read-only source adapter (a Git status/diff adapter additionally requires an explicit decision on external command authority), or Gate 6 gate-level exit-criteria evidence including the authority-boundary enforcement checks.
