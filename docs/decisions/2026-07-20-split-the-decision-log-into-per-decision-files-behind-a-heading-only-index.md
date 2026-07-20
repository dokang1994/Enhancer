# 2026-07-20: Split The Decision Log Into Per-Decision Files Behind A Heading-Only Index

Status: Accepted Decision

Context:

- `DECISION_LOG.md` had grown to 211,121 bytes across 85 accepted decisions and was 48% of the 439,497-byte startup context that `ProjectContextReader` loads on every run. It was the single largest required document, larger than every other one except `ARCHITECTURE.md` combined with `ROADMAP.md`.
- The cost is paid every session and grows monotonically at roughly 2,483 bytes per decision, while the reasoning it carries is almost never needed at session start.
- `ProjectContextReader.MAX_DOCUMENT_BYTES` is a hard 1 MiB ceiling. At the observed size only 337 further decisions fit before the CLI stops booting; at the rate the log grew over its first ten days that is weeks to a few months away, so this was a dated failure rather than a vague concern.
- An earlier assessment deferred the split on the grounds that it required changing `AcceptedDecisionProjector`, `RequiredProjectDocument`, and `TaskJustificationProjector` plus their tests. Reading `AcceptedDecisionProjector` showed that assessment was wrong: it scans only `### ` headings and `Status: Accepted Decision` lines and never reads a decision body.
- The heading text is load-bearing beyond presentation. It is the decision graph's node identity, and `TaskJustificationProjector` resolves `CURRENT_TASK.md`'s `## Justified By` bullets against it by exact string, rejecting an unresolved reference rather than skipping it.
- Splitting into files alone would not reduce the startup cost. If the required-document set loaded `docs/decisions/**`, the same bytes would arrive by a different path; the saving comes from what stays in the loaded set, not from the file count.

Decision:

- Move each accepted decision's body to `docs/decisions/<date>-<slug>.md`, opening with the exact heading as a level-1 title so the heading text survives byte for byte.
- Reduce `DECISION_LOG.md` to an index that keeps every `### <heading>` line, its `Status: Accepted Decision` line, and a link to the file. This preserves exactly the two things the projector reads, so the decision graph is unchanged and no production code changes.
- Keep `DECISION_LOG.md` at its existing path in `RequiredProjectDocument`, so the required-document set, the reading order, and every governance reference stay valid. The per-decision files are deliberately outside that set: they are consulted on demand, not loaded at session start.
- Bound slugs to 80 characters with numeric disambiguation on collision, keeping the deepest path clear of the Windows `MAX_PATH` ceiling.
- Add `DecisionLogIndexTest` asserting that index and files correspond one to one by exact heading, that both sides carry the acceptance status, that no decision file uses the level-3 heading the index reserves for identity, that paths stay under the ceiling, and that every `## Justified By` bullet resolves against the index.
- Exempt `docs/decisions` in `DocumentOwnershipTest`, for the same reason `DECISION_LOG.md` was exempt: the files are append-only records stating what was true when each decision was accepted.

Rationale:

The projector's own narrowness is what makes this cheap. Because it reads identity and status rather than reasoning, identity and status are the only things the loaded document must keep, and everything else can move without the graph noticing. Deleting the bodies from the startup path rather than restructuring the code that reads them is the smallest change that pays the actual interest, and the guard test converts the new invariant from a convention into a build failure before the first drift rather than after it.

Consequences:

- The startup context falls from 439,497 to 248,063 bytes, a 43% reduction, and `DECISION_LOG.md` falls from 48% of it to 7%.
- Headroom against the 1 MiB ceiling rises from 337 to roughly 4,454 further decisions, retiring the dated failure.
- Adding a decision now means adding a file and an index entry. Doing only one fails the build, naming the heading and the side that is missing.
- The decision bodies are no longer in repository memory, so a future feature that wants to reason over decision text must read the files explicitly rather than expect them in `ProjectContext`.
- No production source changed; `AcceptedDecisionProjector`, `TaskJustificationProjector`, `RequiredProjectDocument`, and `ProjectContextReader` are untouched and the projected graph still carries all 85 decision nodes.
- Out of scope: a `SUPERSEDES` producer that would let superseded decisions be archived, any database projection of the decisions, and per-decision frontmatter or metadata beyond the heading and status.
