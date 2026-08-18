# Amend the Constitution so verified GREEN increment boundaries authorize ordinary commits

Status: Accepted Decision

## Context

The Constitution required a commit only when explicitly requested, and the operating
documents repeated that rule. In practice this left several fully verified task cycles
uncommitted in one working tree: the 2026-08-18 recovery inspections repeatedly found
15-, 22-, and 29-path verified bundles existing only as uncommitted working-tree state
on a single Windows host, where one failure could have destroyed days of verified work.
The 2026-08-18 project analysis identified this as a durable risk, and the user
selected amending the commit-cadence rule as the fourth recommendation, approving the
amendment through the accepted recommendation-track decision.

## Decision

Amend Constitution Section 13 so that an ordinary local commit exists for each
verified GREEN increment boundary of the approved Active Task, while any other commit
still requires explicit authority. Amend the Section 7 explicit-authority list so it
covers committing outside such a boundary rather than all committing. Push, merge,
release, and deployment authority are unchanged and remain explicit; approval remains
non-transitive; session close still implies no commit or push permission of its own.
The Constitution version advances from 1.1.0 to 1.2.0 as a Minor change, and
`AGENTS.md`, `.ai/workflow.md`, and the version references in `PROJECT_STATE.md`,
`ROADMAP.md`, and RFC-0001 are synchronized in the same task.

## Rationale

A verified GREEN increment boundary is exactly the state the repository's own
lifecycle calls Implemented-and-Verified; leaving it uncommitted preserves no
additional safety while concentrating loss risk on one host. Commit authority at that
boundary is narrow — it requires an approved Active Task, fresh verification, and an
ordinary non-amending commit — so the change adds durability without weakening any
external-effect boundary.

## Consequences

- Verified increment work is durably recorded in Git at each boundary instead of
  accumulating as uncommitted working-tree state.
- Push, merge, release, deployment, and history-rewriting authority are unchanged.
- The `.ai/constitution.md` mirror was reviewed and needs no change because it
  carries only the pointer to the canonical Constitution.
- A fresh full Java 17 Markdown-sensitive regression is required in the same task
  because every governed Markdown surface changed.
