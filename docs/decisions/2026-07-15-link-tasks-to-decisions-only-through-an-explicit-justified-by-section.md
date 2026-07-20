# 2026-07-15: Link Tasks To Decisions Only Through An Explicit Justified By Section

Status: Accepted Decision

Context:

- Impact answers carry executions but never decisions, because no document grammar evidences which accepted decisions justify the active task; every earlier producer refused to infer that linkage.
- The task document reader ignores unknown sections, so an optional section can be adopted without touching `ApprovedTaskReader`, the `ApprovedTask` record, or the RunRecord encoding.
- A reference that silently fails to resolve would make stated justification indistinguishable from its absence.

Decision:

- Adopt an optional `## Justified By` section in the active task document whose bullets name accepted-decision headings exactly; absence means no justification is claimed.
- Add `TaskJustificationProjector` under `com.enhancer.brain`: it parses the section from the task source document in already-loaded memory, matches each reference against the projected accepted-decision node identities, and emits `JUSTIFIED_BY` edges from the task node.
- Reject a present-but-empty section, non-bullet content, duplicate references, and references that match no projected accepted decision, instead of skipping them.
- Give each edge the task source document as provenance with its computed SHA-256 and snapshot-relative freshness, mirroring the decision projector's rules.
- Merge the edges through a new additional-edges overload of `RunEvidenceGraphProducer` and report a bounded `impactDecisions` count on the CLI run path.
- Keep `ApprovedTaskReader`, `ApprovedTask`, and all persistence schemas unchanged.

Rationale:

Justification is a claim by the task's author, so the only honest evidence is the task document saying it explicitly; parsing exactly that section projects the claim without interpretation, and strict rejection keeps a typo from silently erasing a stated justification. Reusing the accepted-decision node identities as the reference vocabulary means a reference can only point at a decision the decision log actually accepted.

Consequences:

- Renaming a decision heading breaks references to it; reference updates are part of such a rename.
- An unresolvable reference fails graph composition after the RunRecord is persisted, surfacing as an internal error while the record stays replayable.
- Tasks without the section keep producing decision-empty impact answers, which remains the honest default.
