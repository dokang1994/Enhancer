# Current Task

## Status

In Progress

## Task

Implement the additive Model RunRecord v2 value, typed store/resolve boundary, and
filesystem codec RED-first while preserving exact RunRecord v1 behavior and every typed
ModelWork execution guard.

## Task ID

implement-additive-model-run-record-v2

## Context

RFC-0019 is accepted and defines the first implementation step as a standalone
model-specific record plus explicit v2 persistence discrimination before any Scheduler
task resolution, request preparation, admission, candidate selection, or execution
wiring. Existing RunRecord v1 remains the read-file and legacy lifecycle record, and
typed ModelWork currently fails before every execution path. Local `main` contains the
accepted RFC and is intentionally ahead of `origin/main`; this task grants no push.

## Justified By

- User continuation request on 2026-08-31 into additive Model RunRecord v2 implementation
- User continuation request on 2026-08-31 into the Model RunRecord v2 and Scheduler admission specification

## Approval

The user's explicit 2026-08-31 continuation authorizes the first RFC-0019
implementation sequence only: RED-first Java tests, the minimum additive value and
typed persistence contracts, filesystem payload-v2 encoding/decoding over the existing
RunRecord envelope/reference namespace, v1 compatibility and execution-guard
regressions, owning implementation-state/architecture/task/changelog/verification
document synchronization, development-session checkpoints, bounded read-only subagent
reviews, and ordinary local commits at verified GREEN increment boundaries. It
authorizes no exact-task resolver, Scheduler request/policy preparation, RFC-0015/
RFC-0016 runtime invocation, candidate or gateway execution, production model-record
writer, caller cutover, artifact migration, CLI change, external receive, network,
provider, credentials, spend, push, merge, release, deployment, permission change, or
destructive cleanup.

## Acceptance Criteria

- One immutable `ModelRunRecord` retains exactly the canonical WorkItem identity,
  unchanged required capability, exact typed ModelWork envelope, exact prepared
  `ModelRequest`, and existing lifecycle `RunRecord`, with RFC-0019 structural binding
  enforced and no admission/provider/route/credential/network/spend field.
- Separate `ModelRunRecordStore` and `ResolvedModelRunRecord` contracts expose model
  persistence and resolution without widening `RunRecordStore` or
  `ResolvedRunRecord`.
- `FileSystemRunRecordStore` supports payload version 2 through the same envelope,
  artifact suffix, reference grammar, root, integrity, size, atomic-publication, and
  exact-replay rules while keeping payload version 1 encoding byte-for-byte unchanged.
- The v1 resolver rejects v2 with an explicit unsupported-kind failure and never
  projects the nested lifecycle record; the v2 resolver rejects v1, and cross-kind or
  changed-content identity reuse fails before rewrite.
- V2 round-trip preserves every WorkItem/message/profile/request/lifecycle component;
  malformed, unknown, corrupt, truncated, trailing, noncanonical, oversized, foreign,
  and structurally invalid input fails closed.
- Existing opaque reference listing may observe both kinds without resolving them;
  existing v1 writers/readers and their public behavior remain source-compatible.
- In-process, process-parent, child, external-receiver, finalizer, recovery/status,
  Project Brain, runtime-event, CLI replay, and every other current production path
  remain v1-only or explicitly guarded. No production code writes a Model RunRecord.
- Focused RED/GREEN tests, literal/new v1 golden compatibility, relevant execution-
  guard regressions, architecture/governance checks, and the full README-owned Java 17
  regression pass freshly. Owning state and append-only evidence documents are current.

## Out Of Scope

Exact active-task resolution; request/policy preparation; RFC-0015/RFC-0016 invocation;
candidate suitability or locality proof; ToolExecutor or gateway execution; model
finalizer/result/recovery/process/worker wiring; production v2 writer; submission,
receiver, CLI, Project Brain, runtime-event, or status support for v2; migration or
rewriting existing artifacts; providers, routes, endpoints, destinations, network or
remote transmission; credentials, paid services, pricing, tokenizers, caching,
fallback, streaming, MCP, capability maturity beyond evidence supported by this pure
persistence boundary, push, merge, history rewrite, release, deployment, permission
changes, and destructive cleanup.

## Allowed Tools

- read-file
- write-code
- write-tests
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- subagent-readonly

## Verification

- Increment 1: the aligned RED failed at test compilation only because the five new
  model-record value/port types did not exist. The minimum implementation added the
  exact five-component immutable `ModelRunRecord`, structurally bound typed work,
  request, profile, task, Tool, digest, and lifecycle provenance, separate typed
  persistence/resolution ports, and explicit two-kind mismatch vocabulary. Focused
  GREEN passed 18 tests across 6 suites with zero failures, errors, or skips, including
  existing `RunRecord` and runtime package-boundary regressions. `git diff --check`
  passed.

## Dynamic Workflow

Workflow ID: implement-additive-model-run-record-v2
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on v1 byte drift, ambiguous kind dispatch, incomplete standalone
provenance, execution-guard regression, task or checkpoint drift, failed verification,
new execution/external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - model-run-record-value-and-ports

State: Completed
Depends On: none
Scope: Add aligned failing tests, then implement the minimum immutable ModelRunRecord,
typed model store/resolve contracts, and explicit kind-mismatch failure vocabulary
without modifying filesystem bytes or adding a writer.
Exit Criteria: Focused value/contract tests pass, existing RunRecord tests remain GREEN,
the diff adds no runtime caller, and the verified increment is committed locally.
Verification: Focused model/run contract tests, existing RunRecord tests, architecture
guards, and `git diff --check`.
Next Action: Use the verified value/port boundary as fresh dependency evidence for
Increment 2.

### Increment 2 - filesystem-model-run-record-v2

State: In Progress
Depends On: model-run-record-value-and-ports
Scope: Add aligned failing filesystem/golden/guard tests, then implement payload-v2
encoding, decoding, typed dispatch, exact replay, and cross-kind refusal in the shared
filesystem store without a production model writer.
Exit Criteria: V2 persistence and tamper tests pass, literal and newly encoded v1 bytes
remain exact, execution guards stay GREEN, focused verification evidence is appended,
and the verified increment is committed locally.
Verification: RunRecord filesystem/golden/model-work guard suites, architecture
governance, and `git diff --check`.
Next Action: Commit the verified codec/store boundary, then select Increment 3.

### Increment 3 - verify-and-close-model-run-record-v2

State: Pending
Depends On: filesystem-model-run-record-v2
Scope: Run the full README-owned Java 17 regression, synchronize only affected current-
state, architecture, task, changelog, handoff, and append-only verification owners, run
final governance, and close the task locally.
Exit Criteria: Full and final focused verification pass, capability claims remain
bounded to the implemented persistence boundary, all three increments are committed,
and the worktree/checkpoint reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Implement exact active-task resolution and revision/scope binding under
separate user authority, without execution.

## Next

Complete Increment 2 RED-first: add filesystem payload-v2 dispatch, v1 literal/new
goldens, exact replay, and fail-closed cross-kind coverage without a production writer.
