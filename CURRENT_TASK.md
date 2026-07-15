# Current Task

## Status

Completed

## Task

Add the first rebuildable task-to-decision-to-code-to-test impact query over the Contract Verified Project Brain graph projection, returning an immutable snapshot-traceable impact result with derived rebuild status and no traversal beyond the named chain.

## Task ID

gate-6-task-impact-query

## Context

Delivery Gate 6 remains the sole `Specified - Next` product gate. Its graph projection contract is Contract Verified with typed endpoint-checked edges, and the roadmap names the "first rebuildable task-to-decision-to-code-to-test impact query" as that contract's consumer. No query, traversal, producer, or persistence exists.

This increment adds only the query: given a projected `ProjectBrainGraph` and a task node identity, it answers which decisions justify the task, which artifacts the task modifies, which artifacts verify those modified artifacts, and which executions recorded the task. The result is rebuildable evidence: it carries the source snapshot identity of the graph it was answered from and an explicit derived rebuild-required status, so a consumer knows when the answer is stale. Real graph producers remain a later increment; the query is exercised against contract-constructed graphs.

## Acceptance Criteria

- Add an immutable `TaskImpact` result carrying the queried task node identity, the graph's source snapshot identity, justifying decision nodes, modified artifact nodes, verifying artifact nodes, recording execution nodes, and a derived rebuild-required status.
- Add a `TaskImpactQuery` that answers over one `ProjectBrainGraph` by traversing only `JUSTIFIED_BY`, `MODIFIES`, `VERIFIED_BY` from modified artifacts, and `RECORDED_AS` edges from the queried task.
- Reject a null graph or task identity, an unknown task identity, and a node identity whose kind is not task.
- Return empty immutable collections, not errors, for a task with no edges.
- Deduplicate verifying artifacts shared by several modified artifacts and exclude `VERIFIED_BY` edges of artifacts the task does not modify.
- Keep result ordering deterministic, derived from the graph's canonical element ordering.
- Derive rebuild-required as true exactly when the task node, any traversed edge, or any returned node carries provenance requiring rebuild.
- Add no producer, parser, persistence, cache, index, transitive dependency traversal, or Tool authority.
- Add focused RED tests before production types exist, classify the failure, then implement the minimum Java 17 change.
- Run focused Project Brain tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Record the query as Contract Verified only after fresh evidence passes and keep Gate 6 `Specified - Next`.

## Out Of Scope

- Graph producers, document/code/Git/RunRecord parsers, or projection generation
- Transitive `DEPENDS_ON` impact propagation and any reverse-dependency closure
- Graph or query-result persistence, cache, index, embeddings, or confidence scoring
- Git status/diff, diagnostics, selection, or terminal adapters
- Tool permission, task approval, policy expansion, command execution, or mutation
- Event/Message Bus, IPC, Agent Runtime, Scheduler, MCP, Model Gateway, Skills, plugins, multi-agent execution, or background execution
- CLI surface changes
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the request to continue with the repository-defined next Gate 6 increment.

## Allowed Tools

- read-file

## Verification Plan

- Write focused impact-query tests before the production types exist.
- Confirm focused compilation fails only because the selected query types are missing, and classify the failure.
- Implement the minimum immutable query and result types.
- Run focused Project Brain tests and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Added `TaskImpactQuery` and the immutable `TaskImpact` result under `com.enhancer.brain`.
- Answered the named chain over exactly one projected graph: `JUSTIFIED_BY` decisions, `MODIFIES` artifacts, `VERIFIED_BY` verifying artifacts of those modified artifacts only, and `RECORDED_AS` executions.
- Carried the graph's source snapshot identity in the result and derived one rebuild-required status that is true exactly when the task node, any traversed edge, or any returned node requires rebuild; unrelated stale elements do not taint the answer.
- Kept result ordering derived from the graph's canonical element ordering, deduplicated verifying artifacts shared by several modified artifacts, returned empty immutable collections for an edgeless task, and rejected null, unknown, and non-task identities.
- Added no producer, parser, transitive dependency closure, persistence, cache, index, or Tool authority.
- Recorded the impact query as Contract Verified while preserving Delivery Gate 6 as `Specified - Next`.

## Verification

- RED: the first focused compile failed with 9 expected missing-symbol errors naming only the absent `TaskImpactQuery` and `TaskImpact`; no error came from existing contracts.
- Focused GREEN: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.brain.*'` passed 3 suites and 13 tests with no skips, failures, or errors, confirmed against fresh XML output.
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 34 suites and 127 tests: 125 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production compilation passed with `-Xlint:all -Werror`.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 6; `git diff --check` passed.
- Not run: no producer projects a graph from real repository evidence, so the query has no end-to-end evidence over the actual project; it is proven against contract-constructed graphs only.

## Next Task

Activate a separate Gate 6 increment: the first graph producer that projects real repository evidence (documents, RunRecords, snapshot observations) into the graph contract so the impact query can answer about the actual project, or the next read-only source adapter. A Git status/diff adapter additionally requires an explicit decision on external command authority.
