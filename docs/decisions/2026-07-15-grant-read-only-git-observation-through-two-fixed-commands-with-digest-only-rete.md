# 2026-07-15: Grant Read-Only Git Observation Through Two Fixed Commands With Digest-Only Retention

Status: Accepted Decision

Context:

- The Gate 6 exit criteria require snapshots to explain the Git state that informed a run, and the scope names read-only Git status and diff adapters.
- Executing an external command is a new authority category never used by production code in this repository; the Constitution requires explicit user authority for such expansion.
- The user explicitly approved this authority for the Git adapter on 2026-07-15 ("3번 승인할게"), scoped to read-only observation.
- Parsing git internals directly would be fragile, and retaining status or diff content would widen the sensitive-data boundary the snapshot contract deliberately closed.

Decision:

- Add `GitWorkspaceCollector` executing exactly two fixed-argument read-only commands, `git status --porcelain` and `git diff`, with no shell interpretation, the project root as working directory, a five-second per-command timeout, and a four-MiB output cap.
- Confine repository discovery to the project root itself via `GIT_CEILING_DIRECTORIES`, so the observation describes the project's own working tree and never an enclosing repository.
- Harden the invocation against hangs and hidden writes: `--no-optional-locks` and an invocation-scoped `core.fsmonitor=false` keep the observation from touching the index or spawning daemons, stderr is discarded instead of piped so it can never deadlock, and a watchdog destroys any invocation that outlives the timeout while output is being read.
- Store only the SHA-256 digest of each raw output as `GIT_STATUS`/`GIT_DIFF` observation metadata with `git-cli` provenance; never retain output, file lists, or diff content.
- Surface every failure — launch failure, non-zero exit, timeout with process destruction, oversized output — as an explicit `UNAVAILABLE` observation with a bounded reason, so hosts without git degrade honestly.
- Prohibit any mutating, remote, or configuration invocation in this collector; any future git capability requires its own decision and authority.
- Restrict this authority to this collector; it does not extend the Tool policy, the approved-task scope, or any other component.

Rationale:

Two fixed read-only commands with digest-only retention observe the working-tree state at the minimum authority and data surface that the exit criterion permits: the digest is a content-addressed identity of the Git state, sufficient to detect change and divergence, while the actual paths and diffs stay out of snapshots, graphs, and bounded output. Explicit unavailability keeps absence of git distinguishable from a clean tree.

Consequences:

- Approval is not transitive: no other component may execute external commands on the basis of this decision.
- A digest-only observation cannot say which files changed; bounded per-file metadata would be a separate increment with its own decision.
- Runs in non-repository roots or on hosts without git carry two `UNAVAILABLE` Git observations by design.
