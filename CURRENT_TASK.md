# Current Task

## Status

Completed

## Task

Finish applying the accepted single-document ownership rule to the documents left out of the first pass: remove every capability-maturity verdict from `.ai/architecture.md` while preserving its architectural content, and record in `.ai/memory.md` and `docs/05-Memory.md` where per-increment verification evidence belongs and that it is deliberately not a session-start document.

## Task ID

docs-ownership-completion

## Justified By

- 2026-07-20: Give Every Project Fact One Owning Document And Separate Verification Evidence From Current State

## Context

The accepted decision named `.ai/architecture.md` as a document that must not state capability maturity, but its residual maturity language was left out of scope in the first increment because a bulk rewrite of dense technical prose risked losing content. Fourteen of its bullets still carried Contract Verified, Integrated, or Operational verdicts duplicating `PROJECT_STATE.md`, so the file still had to be edited on every maturity change. The memory documents also did not say where verification evidence now lives.

## Acceptance Criteria

- `.ai/architecture.md` states no capability-maturity verdict, and every architectural fact in the edited bullets is preserved.
- Its header states that the file does not own maturity and that the bullets state what each contract is and connects to.
- `.ai/memory.md` and `docs/05-Memory.md` record that per-increment verification evidence is appended to `docs/verification-log.md`, written once, never revised, and deliberately excluded from the session-start reading order because it grows without bound.
- The required startup reading order is unchanged; `docs/verification-log.md` is not added to it, because `RequiredProjectDocument` fixes that set and the log is evidence rather than context.
- No production or test source changes; the full regression matches the 65-suite/299-test baseline with 0 failures and 0 errors.

## Out Of Scope

- Splitting `DECISION_LOG.md` into per-decision files; it remains a Project Brain code change and its own bounded task.
- Generating `PROJECT_STATE.md` from its sources, and any database projection of the documents.
- The `## ... Slice` and `### Delivery Gate N Boundary` sections of `ARCHITECTURE.md`, which are stable historical scope boundaries rather than per-increment churn.
- The `docs/superpowers/plans/**` records, which are point-in-time archives and are correct as written.
- Any production or test code change, schema change, or capability maturity promotion.

## Approval

Approved by the user's 2026-07-20 request to finish the documentation cleanup after the first restructure merged through PR #9.

## Verification

Recorded in `docs/verification-log.md` under Document Ownership Completion Verification.

## Next

Gate 8 connection sub-increment 3b (worker process isolation) or 3c (the concrete `MessageTransport` local IPC adapter), which the user is taking up in a separate session.
