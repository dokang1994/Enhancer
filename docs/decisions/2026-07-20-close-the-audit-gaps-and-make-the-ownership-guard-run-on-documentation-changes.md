# 2026-07-20: Close The Audit Gaps And Make The Ownership Guard Run On Documentation Changes

Status: Accepted Decision

Context:

- A documentation audit left four open items: the undocumented `SchedulerQueueStore` port, the undocumented planner result types, missing coverage of `GitWorkspaceCollector`'s executable-resolution rejection branches, and a word-order gap in `DocumentOwnershipTest`.
- The guard matched only the subject-first form of a maturity claim, so `ARCHITECTURE.md`'s "retains Gate 7 at Contract Verified" survived the increment that introduced the guard. Two parenthetical verdicts in the connection-sequence table survived for the same reason.
- Replacing the pattern with plain co-occurrence of a gate and a maturity level in one sentence was tried and rejected: it flagged forward-looking conditions such as "require an Operational single-agent baseline" and the commentary explaining why a claim had been removed. Neither is a verdict, and a guard that forbids writing about maturity at all is one that gets disabled.
- A more serious defect surfaced while testing the guard adversarially. The assertions read the project's own Markdown, which Gradle does not know about, so after a documentation-only change the `test` task was up to date and the guards did not run at all. A repository the guard would reject reported a green build, which is worse than not having the guard, because the green is believed.
- `SchedulerQueueStore` is the seam a process-isolated worker or out-of-process scheduler would replace, and Gate 8 sub-increments 3b and 3c are the next work. Its implementation was documented in detail while the interface was not, so the documents described the adapter without the boundary.
- `GitWorkspaceCollector.resolveGitExecutable` holds the only external command authority in the codebase and its positive path was covered while every rejection branch was not.

Decision:

- Match maturity claims with three explicit patterns — subject-first, verb-first with an `at`/`to`/`as` connector, and a parenthetical verdict beside a gate in a table row — rather than either a single word order or bare co-occurrence.
- Declare the project's Markdown as an input to the Gradle `test` task, so a documentation-only change invalidates it and the guards actually run.
- Remove the three surviving maturity claims from `ARCHITECTURE.md` and reword the one sentence in `docs/11-Architecture.md` that reproduced a claim while explaining its removal, rather than weakening the guard to accommodate it.
- Document `SchedulerQueueStore` as the durability seam in `ARCHITECTURE.md` and `.ai/architecture.md`, stating that readiness, the active slot, and disposition stay above it so 3b and 3c swap an implementation rather than the queue contract, and restating the same-instance revision rule as implementation-independent.
- Document `TaskProposal`, `ProposalState`, and `PlanningException`, naming why the state enum has exactly one constant: a proposal cannot represent itself as accepted.
- Cover `resolveGitExecutable`'s rejection branches — candidates inside the observed project at any depth, relative and absent PATH entries, a directory named like the executable, and an absent or blank PATH — and pin `MAX_OUTPUT_BYTES` and `TIMEOUT_SECONDS` as constants, widening `TIMEOUT_SECONDS` from private to package-private for the assertion.

Rationale:

The pattern set is the honest middle: precise enough to stay quiet on conditions and commentary, broad enough that the orders a writer actually reaches for are all covered. The Gradle input declaration matters more than the pattern work — a guard that does not run is a false assurance rather than a weak one, and this one had been silently skipping since it was introduced. Documenting the port before 3b and 3c start is cheaper than documenting it afterwards, when the next session will have already had to infer the seam from its adapter.

Consequences:

- Editing any Markdown now re-runs the test task; documentation-only changes cost one test cycle they previously skipped.
- The guard catches subject-first, verb-first, and table-parenthetical claims. A maturity sentence naming no gate still passes, so review remains necessary for that narrower case.
- `GitWorkspaceCollector.TIMEOUT_SECONDS` is package-private rather than private; no behaviour changed.
- The output cap and watchdog remain asserted as constants rather than induced. Inducing either needs a purpose-built git that floods or stalls, which would add a slow platform-specific fixture for little signal; this is a deliberate limit, not an oversight.
- Out of scope: the remaining undocumented public types judged trivial in the audit, a `SUPERSEDES` producer, and Gate 8 sub-increments 3b and 3c.
