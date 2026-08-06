# 2026-08-05: Consolidate Development Workflow Policy Ownership In AGENTS.md

Status: Accepted Decision

## Context

`AGENTS.md` and `README.md` both described the adaptive development subagent policy
and the document-driven dynamic increment workflow, and `README.md` owned the
development-session checkpoint command usage even though checkpoints are an agent
working rule. Constitution Section 4 gives every fact exactly one owning document and
directs duplicates to be deleted rather than synchronized. A session audit on
2026-08-05 surfaced the duplication while assessing `AGENTS.md` as the single AI-agent
entrypoint this repository intentionally uses instead of a `CLAUDE.md`.

## Decision

`AGENTS.md` owns the development-session checkpoint commands, the dynamic workflow
rules, and the adaptive development subagent policy. `README.md` keeps the three
section headings but reduces each body to a one-line reference to the owning
`AGENTS.md` section. The Gate 10/Gate 13 scope disclaimer formerly in `README.md`'s
dynamic-workflow section moves into `AGENTS.md` so no fact is lost.

## Rationale

These three policies configure how AI agents work in this repository, and `AGENTS.md`
is the declared entrypoint every agent must read. Consolidating ownership there removes
the drift risk between two prose copies, keeps `README.md` focused on human-facing
setup and operator command usage, and makes the agent entrypoint self-sufficient for
session mechanics.

## Consequences

Future edits to the checkpoint workflow, dynamic workflow contract, or subagent policy
happen only in `AGENTS.md`. `README.md` anchors remain valid for human readers and
existing links. Build, setup, test, and scheduler operator commands remain owned by
`README.md`.
