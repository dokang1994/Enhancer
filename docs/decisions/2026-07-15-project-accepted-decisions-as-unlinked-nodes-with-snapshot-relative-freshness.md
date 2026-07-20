# 2026-07-15: Project Accepted Decisions As Unlinked Nodes With Snapshot-Relative Freshness

Status: Accepted Decision

Context:

- Produced graphs contain no decision nodes because no producer parses decision evidence.
- The decision log already records accepted decisions under dated `### ` headings with an explicit `Status: Accepted Decision` line, and repository memory carries that document.
- No document grammar links the active task to the decisions that justify it, so `JUSTIFIED_BY` edges cannot be evidenced without inventing linkage.
- A decision node whose provenance cannot say whether the underlying document changed since the snapshot would silently go stale.

Decision:

- Add `AcceptedDecisionProjector` under `com.enhancer.brain`, projecting decision nodes from the `DECISION_LOG.md` document in one loaded `ProjectContext`.
- Treat a section as an accepted decision only when its `Status:` line reads exactly `Accepted Decision`; skip proposals and other statuses.
- Use the heading text as node identity, the decision log path and computed document SHA-256 as provenance, and document order for emission.
- Derive freshness against the snapshot's `DECISION_LOG.md` observation: same digest is `CURRENT`; a differing digest or an unobserved document is `STALE`, because currency cannot be proven without a matching observation.
- Emit no edges; task-to-decision linkage requires an explicit reference grammar adopted through its own decision.
- Reject duplicate accepted-decision headings instead of merging them.

Rationale:

The decision log's own status line is the only machine-readable acceptance evidence in the repository, so parsing exactly that line projects decisions without interpretation. Marking unobserved documents `STALE` keeps the projection honest: a node the snapshot cannot vouch for must not claim currency.

Consequences:

- Impact queries still return empty decision lists until a linkage grammar and its projector exist.
- Renaming a decision heading changes the node identity; stable decision identifiers would need their own convention.
- The parser depends on the documented heading and status conventions of this repository's decision log.
