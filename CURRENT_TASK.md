# Current Task

## Status

Completed

## Task

Implement the first Delivery Gate 6 WorkspaceSnapshot contract with deterministic identity, approved-task revision provenance, explicit source freshness/availability, and bounded metadata-only observations.

## Task ID

gate-6-workspace-snapshot-contract

## Context

Delivery Gates 0 through 4 are Integrated and Gate 5 is Operational. Delivery Gate 6 is the sole `Specified - Next` product gate and owns the immutable common input snapshot that future Project Brain views, message envelopes, and workers will reference.

The first increment establishes only the provider-neutral snapshot contract. It does not collect files, invoke Git, read diagnostics, access terminal contents, build graphs, or grant Tool authority. The immediate Gate 6 consumer is a later `ProjectBrainView` aggregation increment; Gate 7 message envelopes are the next-gate consumer of the same snapshot identity.

## Acceptance Criteria

- Add an immutable `ApprovedTaskRevision` carrying task identity, source-document identity, and a lowercase SHA-256 source revision.
- Add typed Workspace source kinds covering repository documents/files, active/selected files, Git status/diff, diagnostics, terminal-session metadata, and RunRecord metadata.
- Add explicit `AVAILABLE`, `STALE`, and `UNAVAILABLE` source states.
- Add immutable source observations with bounded source identity and provenance, observation time, optional source-update time, optional content digest, and bounded unavailability reason.
- Require Available and Stale observations to carry a valid digest and prohibit Unavailable observations from carrying one.
- Add an immutable `WorkspaceSnapshot` with normalized absolute project root, capture time, approved-task revision, deterministically ordered observations, and a canonical lowercase SHA-256 snapshot identity computed by production code.
- Make snapshot identity independent of caller collection order and sensitive to every identity-bearing metadata field.
- Reject duplicate source kind/identity pairs and more than 4096 observations.
- Store metadata and digests only; do not add source content, diffs, diagnostic payloads, terminal output, secrets, Tool allowlists, or execution authority.
- Keep public collections immutable and validate all null, blank, length, digest, and temporal invariants.
- Add focused RED tests before production types exist, confirm the expected missing-contract failure, then implement the minimum Java 17 contract.
- Name the next Gate 6 `ProjectBrainView` aggregate as the integration consumer; do not claim Gate 6 as Integrated or Operational from contract tests.
- Run focused Workspace tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Preserve Gate 6 as the sole `Specified - Next` gate while recording the Workspace snapshot sub-capability as Contract Verified only after fresh evidence passes.

## Out Of Scope

- Filesystem, Git, editor, diagnostic, terminal, RunRecord, or Project Brain adapters
- File content, Git diff content, terminal output, diagnostic payloads, embeddings, graphs, or indexes
- Snapshot persistence, cache, encryption, signing, cleanup, or remote synchronization
- Tool permission, task approval, policy expansion, command execution, or mutation
- Event/Message Bus, IPC, Agent Runtime, Scheduler, MCP, Model Gateway, Skills, plugins, multi-agent execution, or background execution
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the request to continue with the repository-defined next project work.

## Allowed Tools

- read-file

## Verification Plan

- Write focused Workspace contract tests before production Workspace types exist.
- Confirm focused compilation fails only because the selected Gate 6 types are missing.
- Implement the minimum immutable contracts and canonical identity computation.
- Run focused Workspace tests and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Confirm Gate 6 remains the only `Specified - Next` marker and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Added the provider-neutral `com.enhancer.workspace` package with immutable `ApprovedTaskRevision`, `WorkspaceSourceObservation`, and `WorkspaceSnapshot` contracts plus typed source-kind and source-state enums.
- Enforced bounded non-blank identities and provenance, lowercase SHA-256 digests, state-specific digest/reason rules, and observation-time consistency.
- Normalized snapshot roots to absolute paths, copied and canonically sorted observations, rejected duplicate kind/identity pairs and collections above 4096 entries, and computed a versioned canonical SHA-256 identity over all snapshot metadata.
- Kept source payloads, adapters, Tool authority, task approval, persistence, and Project Brain aggregation outside the contract.
- Recorded `WorkspaceSnapshot` as Contract Verified while preserving Delivery Gate 6 as `Specified - Next`.

## Verification

- RED: the first focused Workspace compile failed with 79 expected missing-symbol errors before production Workspace types existed.
- Focused GREEN: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.workspace.*'` passed 10 of 10 tests across 3 suites with no failures, errors, or skips.
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 108 tests across 28 suites: 106 passed, 2 existing Windows symbolic-link setup tests skipped, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning.
- Java 17 production compilation passed with `-Xlint:all -Werror` and no warning or error.
- Final repository-structure, self-hosting, and diff checks are recorded in `PROJECT_STATE.md` and `SESSION_HANDOFF.md`.

## Next Task

Integrate the Contract Verified WorkspaceSnapshot into a minimal read-only ProjectBrainView with repository-memory and RunRecord provenance, subject to separate explicit task activation.
