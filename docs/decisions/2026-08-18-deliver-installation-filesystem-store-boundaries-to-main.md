# User request on 2026-08-18 to deliver installation filesystem store boundaries to main

Status: Accepted Decision

## Context

The installation evidence reconciliation, deterministic integrity formats, immutable
semantic-evidence point-store contract, and locked filesystem transaction-cursor store
are Completed and freshly verified in the working tree. Local `HEAD`, `main`, and
`origin/main` all resolve to `36e1967e1cb7fe0a7c4023ee537334a70c64821d` with divergence
`0 0`; no merge, rebase, cherry-pick, revert, or development checkpoint is active. The
user explicitly requested commit, push, and merge to `main`.

## Decision

Authorize exact-path staging and an ordinary commit of the completed installation
reconciliation/format/store boundary bundle plus this delivery authority on a dedicated
`codex/installation-filesystem-store-boundaries-20260818` branch. Authorize a non-force
push of that branch, a local `main` fast-forward-only merge, and a non-force push of
`main`. After delivery, authorize the minimum closure documentation, ordinary closure
commit on `main`, and non-force push needed to leave canonical task/delivery state true.

Every external Git step must remain checkpointed. A fresh full Java 17/Markdown-sensitive
regression must pass before the first commit, the exact staged manifest must be reviewed,
and final local and direct remote refs must be reread. Any non-fast-forward condition,
remote divergence, unexpected staged path, test failure, or checkpoint drift stops the
delivery without force, rebase, reset, amend, squash, or history rewriting.

## Rationale

The user supplied explicit delivery authority, while the repository operating rules
require delivery history to remain in Git and canonical documents to describe only the
state actually reached. A dedicated branch plus `--ff-only` keeps the delivery ancestry
auditable and makes divergence a refusal rather than an implicit merge decision.

## Consequences

- The verified 29-path working bundle and this decision form the exact initial 30-path
  delivery manifest.
- Network writes are limited to ordinary non-force pushes of the named branch and
  `main`; credentials are neither inspected nor changed.
- A closure commit may contain only post-delivery canonical documentation needed to
  record completed delivery and its fresh evidence.
- No force push, rebase, reset, amend, squash, synthetic merge commit, tag, release,
  deployment, cleanup, real installation, permission mutation, native gateway, or other
  external effect is authorized.
