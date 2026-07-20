# 2026-07-16: Keep Durable Run Outcome Primary Across Project-Brain Composition

Status: Accepted Decision

Context:

- The CLI currently persists a finalized RunRecord before composing Project Brain output, but any later graph exception escapes to the top-level internal-error handler. A durable `COMPLETED`/`VERIFIED` run can therefore return exit 70.
- Required documents targeted by the run appear as both `REPOSITORY_DOCUMENT` and `REPOSITORY_FILE`; graph projection currently flattens both identities into duplicate artifact nodes. Duplicate accepted-decision headings and the 256-vs-1024 identifier-bound mismatch provide additional post-persist failure triggers.
- Project Brain counts are derived diagnostics. They must not redefine the already-finalized governed execution outcome.

Decision:

- Project accepted decisions and task-justification edges and preflight the graph's non-execution inputs before creating evidence, invoking a Tool, or persisting a RunRecord. Invalid project metadata is a usage/configuration failure with no durable run.
- Collapse multiple repository observations of the same source identity into one artifact node, preferring the target-specific `REPOSITORY_FILE` observation over the general repository-document observation.
- Align graph node identifiers with the Workspace source identifier bound of 1024 characters.
- After persistence, treat Project Brain view, graph, query, and diagnostic composition as optional reporting. Catch a reporting runtime failure, emit bounded `brainStatus=UNAVAILABLE` metadata, and return the exit code derived from the durable RunRecord unchanged.

Rationale:

The transaction boundary is the durable RunRecord. Validation that can be performed from the snapshot and repository memory belongs before external work, while failures in reconstructible diagnostics after that boundary must degrade diagnostics, not rewrite execution history. Identity collapse preserves one graph artifact per repository path without discarding the more specific target observation.

Consequences:

- A malformed or oversized Project Brain input fails before Tool execution and persistence.
- A post-persist brain-reporting defect remains observable in bounded output while replay and the CLI exit code stay consistent with the durable record.
- Graph projection remains rebuildable and non-authoritative; no RunRecord schema or Tool authority changes.
