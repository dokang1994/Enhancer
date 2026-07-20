# Current Task

## Status

Completed

## Task

Split `DECISION_LOG.md` into one file per accepted decision under `docs/decisions/`, reduce the log itself to a heading-and-status index that preserves exactly what `AcceptedDecisionProjector` reads, and add `DecisionLogIndexTest` so the index and the files cannot drift apart.

## Task ID

split-decision-log-behind-index

## Justified By

- 2026-07-20: Split The Decision Log Into Per-Decision Files Behind A Heading-Only Index

## Context

`DECISION_LOG.md` was 211,121 bytes and 48% of the startup context that every session loads, growing about 2,483 bytes per decision against a hard 1 MiB ceiling in `ProjectContextReader` — 337 decisions from the point where the CLI would stop booting. An earlier assessment deferred the split as a three-class code change; reading `AcceptedDecisionProjector` showed it scans only `### ` headings and `Status: Accepted Decision` lines and never reads a body, so an index preserving those two things leaves the decision graph unchanged.

## Acceptance Criteria

- All 85 decision bodies move to `docs/decisions/` byte for byte, with headings and document order preserved exactly.
- `DECISION_LOG.md` keeps every `### <heading>` line and `Status: Accepted Decision` line, stays at its `RequiredProjectDocument` path, and retains the `## Proposals` section.
- No production source changes; the projected graph still carries every decision node and `## Justified By` still resolves.
- `DecisionLogIndexTest` fails when index and files drift in any direction and passes when they agree.
- `DocumentOwnershipTest` exempts `docs/decisions`, matching the exemption `DECISION_LOG.md` already had as an append-only record.
- Full regression passes with 0 failures and 0 errors.

## Out Of Scope

- A `SUPERSEDES` producer that would let superseded decisions be archived out of the index.
- Any database projection of the decisions, and per-decision frontmatter beyond the heading and status.
- Loading decision files into `ProjectContext`; they are deliberately outside the session-start set.
- Gate 8 sub-increments 3b and 3c, which the user is taking up in a separate session.

## Approval

Approved by the user's 2026-07-20 direction to start the split immediately, change the code to match, and verify repeatedly by several methods so no exception arises.

## Verification

Recorded in `docs/verification-log.md` under Decision Log Split Verification.

## Next

Gate 8 connection sub-increment 3b (worker process isolation) or 3c (the concrete `MessageTransport` local IPC adapter).
