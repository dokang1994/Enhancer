# AI Workflow

Enhancer follows Document Driven Development.

This is the operational expansion of `CONSTITUTION.md` Section 6, which states the
same sequence in seven normative steps. Where the two differ, the Constitution wins;
this file adds the executable detail, not new authority.

Before every session:

```text
Always read the .ai folder before starting work.
```

After the governed documents are loaded, inspect the machine-written development-session
checkpoint with the local CLI. It lives under `.enhancer/session-checkpoint/`, references
the active task contract, and is recovery metadata rather than repository authority or
verification evidence. Reconcile it with Git state before planning or editing.

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
   Record checkpoint intent before each mutating or verification step and its success or
   failure immediately afterward. A forced stop must leave the pending step and the last
   successful step distinguishable without chat history.
8. Run fresh, claim-appropriate verification.
9. Promote lifecycle state only when authority and evidence support it.
10. Append the increment's verification evidence to `docs/verification-log.md`.
11. Update each affected document, writing every fact only to the document that owns it (Constitution Section 4). The next task belongs to `CURRENT_TASK.md`, capability maturity to `PROJECT_STATE.md`, delivery history to git and `CHANGELOG.md`. Delete duplicates rather than synchronizing them.
12. Reduce `SESSION_HANDOFF.md` to what is true now and would otherwise be lost with the session.
13. Commit only when explicitly required; never infer permission to push.
14. Keep the checkpoint through authorized delivery actions, then mark it Stable and
    clear it only after canonical documents, verification, and intended Git state agree.

### Adaptive Development Subagent Delegation

The primary Agent selects the smallest execution topology inside the user request and
Active Task. For each non-trivial task, compare expected quality, risk-reduction, and
latency benefit with coordination cost. Select bounded read-only subagents only when the
work splits into concrete independent questions, separate component/document reviews,
alternative comparisons, or independent risk/test-surface analysis. Keep one Agent for
local, sequential, tightly coupled, overlapping-write, or ambiguous work.

Before dispatch, record each role's scope, sources, output, join condition, conflict
policy, and least Tool/context/time bound. Use at most three concurrent children, one
delegation level, three dispatches in one increment, and six in one Active Task. The
primary Agent remains the only mutation, checkpoint, Git, evidence-validation,
synthesis, and lifecycle owner. Reports are recommendations rather than evidence; join
or stop every child and validate repository sources plus fresh checks directly.

Delegation never widens scope, Tools, permissions, budgets, external/destructive
authority, or lifecycle state. Stop and return to one Agent on authority conflict,
artifact drift, overlapping ownership, failed join, exhausted bounds, or unsafe
synthesis. This host development rule is separate from Gate 13 runtime orchestration.

### Dynamic Increment Workflow

When `CURRENT_TASK.md` contains `## Dynamic Workflow`, treat it as bounded execution
structure inside the single approved task, never as a second authority source:

1. Validate `Sequential` mode, the declared two-through-sixteen increment limit, stable
   unique increment identities, dependency references, and at most one `In Progress`
   entry.
2. Work only on the `In Progress` increment. If none is active, select the first ordered
   `Pending` increment whose dependencies are `Completed` after reading their declared
   fresh verification evidence.
3. Keep every increment inside the parent Task, Approval, Acceptance Criteria, Allowed
   Tools, and Out Of Scope envelope. Stop for user approval before adding an increment,
   widening scope, or acquiring new external/destructive authority.
4. Record checkpoint intent and outcome around every mutation or verification. The
   workflow cursor is canonical task context; the checkpoint is recovery position; the
   append-only verification log is evidence. Do not substitute one for another.
5. Mark an increment `Completed` only after its exit criteria and declared verification
   pass. Then either promote the deterministic dependency-ready successor or, when all
   required increments are complete, close the parent task through the normal document
   synchronization and Definition of Done.
6. Stop without selecting another increment on failure, block, stagnation, exhausted
   bounds, task drift, insufficient authority, or unsafe recovery.

Dynamic increment selection stays sequential. The adaptive policy above may parallelize
independent read-only inspection inside the selected increment, but this document
workflow does not implement the Gate 10 Workflow Engine, background execution, Gate 13
multi-agent runtime, or automatic approval/delivery.

A contract you describe must state what it connects to. Describing a contract
correctly while leaving its connection to the next contract unstated is how the
`completion` conflict reached implementation; `ARCHITECTURE.md` records that case
under Completion Semantics.
