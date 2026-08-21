# Current Task

## Status

Completed

## Task

Define and accept RFC-0017 as the smallest explicit provider-neutral caller input and
source contract that can supply every RFC-0014 model execution-profile requirement and
the independent authoritative required capability without defaults, Scheduler
conflation, routing, or remote transmission.

## Task ID

specify-complete-model-invocation-input

## Context

RFC-0014 through RFC-0016 and their pure Java contracts are Accepted and Contract
Verified. The current direct CLI has neither a complete profile nor an authoritative
capability source. The Scheduler retains `WorkItem.requiredCapability` but has no
complete profile source and temporarily reuses that capability as RFC-0013 model class.
Both paths must remain unchanged and unsupported until explicit source and later
integration contracts exist.

## Justified By

- User continuation request on 2026-08-21 into the complete model invocation input specification
- User continuation request on 2026-08-21 into the RFC-0016 model invocation admission implementation

## Approval

The 2026-08-21 continuation authorizes one documentation-only RFC-0017 specification,
read-only bounded architecture/source reviews, owning architecture/RFC-index/decision/
task/changelog/verification synchronization, fresh Java 17 Markdown-sensitive
verification, development-session checkpoints, and ordinary local commits at verified
GREEN increment boundaries. It authorizes no Java, command, schema, persistence, CLI,
Scheduler, Tool, gateway, adapter, or runtime change; no caller migration, defaulting,
inference, routing, provider or network behavior; and no push, merge, release,
deployment, external effect, or destructive cleanup.

## Acceptance Criteria

- RFC-0017 names the minimum explicit input values and exact authority provenance for
  one future invocation without duplicating or weakening RFC-0013 through RFC-0016.
- Every RFC-0014 field is supplied explicitly through one complete validated profile;
  missing values, partial profiles, defaults, fallbacks, unknown enum values, and
  implicit or ambient lookup fail closed.
- The authoritative required capability remains a distinct unchanged projection from
  the governed caller and cannot be copied from profile data, inferred from model class,
  or accepted as arbitrary direct-CLI authority.
- Compatibility and source ownership for the current direct CLI, Scheduler WorkItem,
  submission, Tool request, gateway, and durable schemas are explicit; all remain
  unchanged and unsupported until later implementation/migration authority.
- The contract adds no provider candidate, route, endpoint, destination, credential,
  tokenizer, price, network, transmission, spend, or gateway-execution authority and
  does not make an admission decision persistable or replayable.
- Architecture, RFC index, accepted-decision index, changelog, task cursor, and
  append-only verification evidence are synchronized, with focused governance and the
  full README-owned Java 17 regression passing freshly.

## Out Of Scope

Java implementation; changes to existing source, CLI or Scheduler commands, Tool
arguments, request/gateway/adapters, submission/queue/runtime/spool schemas, persistence
or migrations; complete caller migration; admission/runtime wiring; model suitability;
routing, providers, endpoints, destinations, network or remote transmission;
credentials, paid services, pricing, tokenizers, usage normalization, MCP, release,
deployment, push, merge, history rewrite, permission changes, and destructive cleanup.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- subagent-readonly

## Verification

- Increment 1: two bounded read-only reviews converged on a source-obligation contract
  over existing values and were reconciled as recommendations, not verification
  evidence. Focused decision, ownership, workflow, approved-task, justification, and
  planner governance passed 21 tests across 6 suites with zero failures, errors, or
  skips; `git diff --check` was clean and every changed path was Markdown.
- Increment 2: the fresh unfiltered README-owned Java 17 `test --no-daemon` task passed
  936 tests across 174 suites with 10 existing environment-dependent skips, zero
  failures, and zero errors. No Java source or capability maturity changed.

## Dynamic Workflow

Workflow ID: specify-complete-model-invocation-input
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on authority conflict, source ambiguity, incompatible existing
contract, failed governance or regression verification, task drift, checkpoint drift,
new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - define-and-accept-rfc-0017

State: Completed
Depends On: none
Scope: Reconcile independent read-only source/authority reviews, define and accept the
minimum RFC-0017 contract, and synchronize architecture, RFC index, decision, changelog,
task, and append-only focused verification evidence.
Exit Criteria: The RFC resolves every acceptance criterion without implementation or
schema change, focused governance passes, the diff is documentation-only and clean,
evidence is appended once, and the verified increment is committed locally.
Verification: RFC/decision/architecture/document-ownership/dynamic-workflow governance
tests and `git diff --check`.
Next Action: Run the full regression after committing the accepted specification.

### Increment 2 - verify-and-close-rfc-0017

State: Completed
Depends On: define-and-accept-rfc-0017
Scope: Run the full README-owned Java 17 regression, synchronize only changed lifecycle
owners, rerun final Markdown governance, and close the task.
Exit Criteria: Full and final governance verification pass, no implementation or
maturity claim appears, the closure is committed locally, and the worktree/checkpoint
reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Commit the verified closure and clear the stable checkpoint.

## Next

Define and accept the versioned Scheduler complete-profile source and transport
contract covering typed model work input, message/submission/queue/runtime retention,
recovery, migration, and no-default cutover without implementation.
