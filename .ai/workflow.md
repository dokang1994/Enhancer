# AI Workflow

Enhancer follows Document Driven Development.

This is the operational expansion of `CONSTITUTION.md` Section 6, which states the
same sequence in seven normative steps. Where the two differ, the Constitution wins;
this file adds the executable detail, not new authority.

Before every session:

```text
Always read the .ai folder before starting work.
```

Then:

1. Read `CONSTITUTION.md` and confirm the work fits the project rules.
2. Identify the current lifecycle state and required authority.
3. Read or update `ARCHITECTURE.md`.
4. Record accepted decisions as a file under `docs/decisions/` plus a matching entry in the `DECISION_LOG.md` index; the heading text is the decision's identity and is resolved by exact string.
5. Define or confirm the current task in `CURRENT_TASK.md`.
6. For observable behavior, establish a focused RED test and classify the failure before implementation:
   - confirm the failure is caused by the missing or incorrect behavior the test is intended to expose;
   - confirm the tested behavior matches `CURRENT_TASK.md`, accepted decisions, Architecture, and repository build/runtime settings;
   - when those checks pass, proceed directly with the minimum scoped implementation without asking for redundant approval;
   - when the failure is unrelated, flaky, conflicts with repository authority or configuration, expands scope, or requires new external/destructive authority, do not implement it as part of the RED cycle; record or report it separately.
7. Implement the smallest scoped change that turns the accepted RED case GREEN. A missing production type or other intentionally unimplemented symbol is valid RED evidence when the test contract passed the classification above; it is not by itself a reason to stop.
8. Run fresh, claim-appropriate verification.
9. Promote lifecycle state only when authority and evidence support it.
10. Append the increment's verification evidence to `docs/verification-log.md`.
11. Update each affected document, writing every fact only to the document that owns it (Constitution Section 4). The next task belongs to `CURRENT_TASK.md`, capability maturity to `PROJECT_STATE.md`, delivery history to git and `CHANGELOG.md`. Delete duplicates rather than synchronizing them.
12. Reduce `SESSION_HANDOFF.md` to what is true now and would otherwise be lost with the session.
13. Commit only when explicitly required; never infer permission to push.

A contract you describe must state what it connects to. Describing a contract
correctly while leaving its connection to the next contract unstated is how the
`completion` conflict reached implementation; `ARCHITECTURE.md` records that case
under Completion Semantics.
