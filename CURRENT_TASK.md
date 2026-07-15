# Current Task

## Status

Completed

## Task

Add the read-only Git status and diff Workspace adapter under the explicitly approved external command authority: two fixed read-only git invocations whose bounded output is digested into GIT_STATUS and GIT_DIFF observations, with every failure surfaced as an explicit Unavailable observation.

## Task ID

gate-6-git-workspace-adapter

## Context

The Gate 6 exit criteria require a snapshot to explain the Git state that informed a run, and the scope names read-only Git status and diff adapters. Executing an external command is a new authority category for this repository; the user explicitly approved it for this adapter ("3번 승인할게") on 2026-07-15, scoped to read-only git observation only.

The `GitWorkspaceCollector` executes exactly two fixed-argument commands — `git status --porcelain` and `git diff` — in the project root with no shell, a hard timeout, and a bounded output cap, and stores only a SHA-256 digest of each output as observation metadata. Output content, file lists, and diffs are never retained. Any failure — git missing, not a repository, timeout, oversized output, non-zero exit — becomes an explicit `UNAVAILABLE` observation with a bounded reason, so a run on a host without git degrades honestly instead of failing.

## Acceptance Criteria

- Add a `GitWorkspaceCollector` under `com.enhancer.workspace` observing one project root at an explicit observation time and returning exactly two observations: `GIT_STATUS` for `working-tree` and `GIT_DIFF` for `working-tree-diff`, both with `git-cli` provenance.
- Execute only the two fixed read-only commands with no shell interpretation, the project root as working directory, a five-second timeout per command, and a four-MiB output cap.
- Emit `AVAILABLE` with the SHA-256 of the raw command output for a successful invocation; retain no output content.
- Emit `UNAVAILABLE` with a bounded reason for launch failure, non-zero exit, timeout (with the process destroyed), or output exceeding the cap.
- Never execute any mutating git command; the collector must contain no write, fetch, push, or config invocation.
- On the CLI `run` path, include both observations in the collected snapshot; non-repository temp projects therefore observe them as `UNAVAILABLE`.
- Keep commands, arguments, exit codes, replay, and persistence schemas unchanged; update affected CLI test expectations for the two additional observations.
- Add focused RED tests before the collector exists, classify the failure, then implement the minimum Java 17 change; guard git-dependent tests with an availability assumption.
- Run focused workspace, CLI, brain, and integration tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Execute one actual-repository governed `run` and record the observed Git state evidence.

## Out Of Scope

- Any mutating, remote, or configuration git invocation
- Retaining status or diff content, file lists, or per-file metadata
- Parsing porcelain output into structured observations (a later increment may add bounded per-file metadata under its own decision)
- Tool permission, task approval, or policy expansion beyond this adapter
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15: external read-only command authority for this Git adapter was explicitly granted in the session message "3번 승인할게" following the request to proceed with all remaining actionable increments.

## Allowed Tools

- read-file

## Verification Plan

- Write focused collector tests before the production type exists and confirm the expected failure.
- Implement the minimum collector and CLI integration.
- Run focused workspace, CLI, brain, and integration suites and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Run one governed `run` against this actual repository and inspect the Git observations.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker and run `git diff --check`.
- Synchronize all affected project documents.

## Implementation Result

- Added `GitWorkspaceCollector` under `com.enhancer.workspace` executing exactly two fixed read-only commands with no shell, the project root as working directory, a five-second watchdog-enforced timeout, and a four-MiB output cap, emitting `GIT_STATUS` and `GIT_DIFF` observations with `git-cli` provenance and digest-only retention.
- Confined repository discovery to the project root via `GIT_CEILING_DIRECTORIES` after the first test run exposed that an enclosing repository (this workspace) was being observed from temporary directories.
- Hardened the invocation against hangs and hidden writes: `--no-optional-locks` and invocation-scoped `core.fsmonitor=false` prevent index writes and daemon children, stderr is discarded instead of piped, and a watchdog destroys any invocation outliving the timeout. The initial piped-stderr design hung the suite once; the hang was eliminated by this hardening and did not recur.
- Surfaced every failure (launch, non-zero exit, timeout, oversized output, non-repository root) as an explicit `UNAVAILABLE` observation with a bounded reason.
- Included both observations in the CLI-collected snapshot; non-repository temp projects observe them as `UNAVAILABLE` by design, and no command, exit code, replay, or persistence schema changed.

## Verification

- RED: the focused compile failed with 6 expected missing-symbol errors naming only the absent `GitWorkspaceCollector`.
- One real defect was caught and fixed during GREEN: without a discovery ceiling, a temporary non-repository directory inside this workspace observed the enclosing Enhancer repository as `AVAILABLE`; `GIT_CEILING_DIRECTORIES` fixed the semantics, and the associated stderr-pipe hang was eliminated by the hardening above.
- Focused GREEN: workspace, CLI, brain, and integration suites passed 21 suites and 62 tests with no skips, failures, or errors, confirmed against fresh XML output.
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 42 suites and 152 tests: 150 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production compilation passed with `-Xlint:all -Werror`.
- Git-dependent tests are guarded by a git-availability assumption; digests are deterministic for an unchanged tree and change when the tree changes.
- Actual repository `run` on `README.md`: exit code 0, `COMPLETED`, `VERIFIED`, RunRecord `run-record/4f0d3da1-e8a8-412f-a513-79338d47b2b7`, `workspaceObservations=23` (15 documents, 5 prior run records, 1 target file, 2 Git observations), `graphNodes=67`, `graphDecisions=49`.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 6; `git diff --check` passed.
- `impactDecisions=0` on the evidence run is honest: this task document carries no `Justified By` section.

## Next Task

Activate a separate Gate 6 increment, subject to explicit activation. Candidates: bounded per-file Git status metadata under its own decision, or an assessment of Gate 6 gate-level maturity against the full exit criteria. Diagnostics, terminal, and selection adapters remain blocked on capabilities from later gates.
