# AI Workflow

Enhancer follows Document Driven Development.

Before every session:

```text
Always read the .ai folder before starting work.
```

Then:

1. Read `CONSTITUTION.md` and confirm the work fits the project rules.
2. Identify the current lifecycle state and required authority.
3. Read or update `ARCHITECTURE.md`.
4. Record accepted decisions in `DECISION_LOG.md`.
5. Define or confirm the current task in `CURRENT_TASK.md`.
6. For observable behavior, establish a focused RED test and classify the failure before implementation:
   - confirm the failure is caused by the missing or incorrect behavior the test is intended to expose;
   - confirm the tested behavior matches `CURRENT_TASK.md`, accepted decisions, Architecture, and repository build/runtime settings;
   - when those checks pass, proceed directly with the minimum scoped implementation without asking for redundant approval;
   - when the failure is unrelated, flaky, conflicts with repository authority or configuration, expands scope, or requires new external/destructive authority, do not implement it as part of the RED cycle; record or report it separately.
7. Implement the smallest scoped change that turns the accepted RED case GREEN. A missing production type or other intentionally unimplemented symbol is valid RED evidence when the test contract passed the classification above; it is not by itself a reason to stop.
8. Run fresh, claim-appropriate verification.
9. Promote lifecycle state only when authority and evidence support it.
10. Update project documents and `SESSION_HANDOFF.md`.
11. Commit only when explicitly required; never infer permission to push.
