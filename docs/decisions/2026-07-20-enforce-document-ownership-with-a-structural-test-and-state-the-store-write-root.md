# 2026-07-20: Enforce Document Ownership With A Structural Test And State The Store Write-Root Contract Exactly

Status: Accepted Decision

Context:

- The single-document ownership rule was accepted the same day but only stated in prose. A follow-up audit found six documents still asserting gate maturity: `README.md`, `docs/08-Multi-Agent.md`, `docs/10-Roadmap.md`, `docs/11-Architecture.md`, and `docs/rfcs/RFC-0009-Multi-Agent.md`.
- Every one of them claimed Gate 7 was `Specified - Next`, which stopped being true when Gate 8 took that marker. The same fact had been copied to five places and all five drifted together — the exact failure the ownership rule exists to prevent, reproduced while the rule was already written down.
- The repository already had the mechanism for this class of problem: `RuntimePackageBoundaryTest` makes an architectural constraint executable rather than aspirational.
- A separate audit finding claimed the CLI's `--evidence-root` and `--run-record-root` were an unconfined write surface contradicting a documented `.enhancer/` guarantee. Re-reading the sources showed the opposite: the 2026-07-15 Gate 5 decision requires those roots as explicit caller inputs, `README.md` calls `.enhancer/` "the example runtime directory", and the stores already refuse a symbolic-link root through `NOFOLLOW_LINKS` and only create freshly generated UUID-named entries. The design is deliberate; the defect was that no document stated the contract precisely enough to prevent that misreading.

Decision:

- Add `DocumentOwnershipTest` under `com.enhancer.architecture`, alongside the existing package-boundary test, asserting that no Markdown document outside `PROJECT_STATE.md` claims gate maturity and that no document outside `CURRENT_TASK.md` carries a `## Next Task` heading.
- Exempt the documents that legitimately carry maturity language: `PROJECT_STATE.md` as owner; `ROADMAP.md`, which owns the `Status: Specified - Next` grammar `RepositoryTaskPlanner` parses; and the append-only historical records `DECISION_LOG.md`, `CHANGELOG.md`, `docs/verification-log.md`, and `docs/superpowers/**`, whose entries were true when written and are never revised.
- Match the maturity claim on a gate-scoped subject rather than on the vocabulary alone, so a document may still define or discuss the maturity levels without asserting one.
- Replace the six stale claims with references to `PROJECT_STATE.md` instead of updating them, per the rule that a duplicate is deleted rather than synchronized.
- State the store write-root contract exactly in `ARCHITECTURE.md` and `README.md`: the roots are explicit caller inputs, deliberately not confined to the project root, and `.enhancer/` is an example layout rather than an enforced property; each store normalizes its root, refuses a symbolic-link root, and only creates new UUID-named entries, so it can add to a caller-named directory but cannot overwrite or delete what is already there. Read-side containment remains the separate and stricter boundary.
- Do not add project-root confinement to the stores. It would contradict the accepted Gate 5 input model and break the existing `CliArgumentsTest` case that deliberately uses sibling roots.

Rationale:

A governance rule that nothing checks decays at the speed of the fastest-moving document, and this one decayed within a day of being written. Making it executable costs one structural test and converts a review question into a build failure. The write-root finding is the mirror image: the code was right and the prose was imprecise, so the fix is to make the prose say what the code actually guarantees rather than to change working behaviour to match a misreading.

Consequences:

- Adding gate maturity to any document except `PROJECT_STATE.md` now fails the build with the offending file, line, and matched text.
- The test enforces the gate-scoped form of the claim; a non-gate-scoped maturity sentence can still pass, so review remains necessary for that narrower case.
- The exempt list is itself a maintained decision: adding a document to it must be justified, since exemption removes the guarantee for that file.
- The symbolic-link root refusal in both filesystem stores remains untested. A test would require symbolic-link creation privilege, which this Windows host denies and which already causes two existing skips, so it would give no signal here and is left as named follow-on work.
- Out of scope: confining store roots, the `GitWorkspaceCollector` output-cap and timeout tests, and the undocumented public types found in the same audit.
