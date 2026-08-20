# Current Task

## Status

Completed

## Task

Specify the smallest separately authorized Delivery Gate 9 composition contract that
can attach one complete `ModelExecutionProfile` to one existing `ModelRequest` while
preserving RFC-0013 compatibility and the required task, execution, and future
outbound-policy intersection.

## Task ID

specify-model-request-execution-profile-composition

## Context

RFC-0013 owns the current five-component `ModelRequest` and executable gateway path.
RFC-0014 owns a separate complete requirement profile and forbids defaults, routing,
providers, or remote authority in its first value-only implementation. That value layer
is now Contract Verified but intentionally uncomposed.

## Justified By

- User continuation request on 2026-08-20 into the ModelRequest execution-profile composition specification
- User continuation request on 2026-08-20 into the RFC-0014 model execution profile value layer implementation

## Approval

The user's 2026-08-20 continuation request authorizes a documentation-only contract
task: define and accept the smallest immutable request/profile composition, its exact
alignment and policy-intersection rules, exclusions, test-first implementation plan,
fresh Java 17 Markdown-sensitive verification, development-session checkpoints, owning-
document synchronization, and ordinary local commits at verified GREEN increment
boundaries under Constitution 1.2.0.

It does not authorize Java implementation; changes to `ModelRequest`, `ModelGateway`,
Scheduler, CLI, Tool, provider adapters, command or durable schemas; routing, provider
or endpoint selection; network or remote transmission; credentials, paid services,
pricing, tokenization, usage normalization, caching, fallback, retry, streaming, MCP,
redaction, prompt-injection handling, source attribution, quality evaluation,
migration, push, merge, tag, release, deployment, permission changes, or destructive
cleanup.

## Acceptance Criteria

- One Accepted RFC defines an immutable composition that retains one complete existing
  `ModelRequest` and one complete `ModelExecutionProfile` without changing or
  duplicating either accepted value contract and without inventing defaults.
- The contract defines the minimum fail-closed cross-value alignment needed now,
  including identical model class and a profile invocation time no greater than the
  request gateway timeout, while keeping the response-character and token contracts
  explicitly distinct.
- Required capability remains distinct from model class, and current Scheduler
  conflation is neither encoded nor legitimized.
- The composition is requirement data only: it selects no route, provider, model,
  endpoint, tokenizer, price, credential, or destination and authorizes no Tool,
  spend, network, remote transmission, or external effect.
- The contract preserves the intersection of accepted task authority,
  `ExecutionPolicy`, the composed profile, and any later accepted outbound/provider
  policy; every consumer may narrow or reject but never widen it.
- RFC-0013 `ModelRequest`, `ModelGateway`, deterministic fake, Tool, CLI, Scheduler,
  persisted schema, and runtime behavior remain unchanged by this documentation task.
- The RFC names one bounded RED-first pure-value implementation follow-up and its
  focused tests, plus later separately authorized runtime integration consumers.
- Fresh focused governance and full README-owned Java 17 Markdown-sensitive tests and
  `git diff --check` pass before completion.

## Out Of Scope

Java implementation; request, gateway, Scheduler, CLI, Tool, adapter, command, or
durable-schema changes; routing; provider, model, endpoint, tokenizer, price, credential,
or destination selection; network or remote transmission; paid use; policy evaluation
implementation; persistence or migration; capability maturity; push, merge, tag,
release, and deployment.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- subagent-read-only

## Verification

Evidence will be appended once per increment to `docs/verification-log.md` after each
declared verification is complete.

- Increment 1: RFC-0015 was accepted as the additive two-component composition after
  independent compatibility and authority reviews converged on the same minimum
  boundary. The focused governance selection passed 10 tests with zero failures,
  errors, or skips, and `git diff --check` was clean. Evidence is appended once in
  `docs/verification-log.md`.
- Increment 2: the fresh full README-owned Java 17 Markdown-sensitive regression passed
  919 tests across 172 result suites with zero failures and errors and 10 environment-
  dependent skips. Lifecycle owners were synchronized without claiming implementation
  maturity, and evidence is appended once in `docs/verification-log.md`.

## Dynamic Workflow

Workflow ID: specify-model-request-execution-profile-composition
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on authority conflict, failed verification, task or checkpoint drift, scope expansion, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - define-composition-contract

State: Completed
Depends On: none
Scope: Reconcile RFC-0013 and RFC-0014, compare the minimum compatibility-preserving composition, and add the accepted RFC plus required architecture and roadmap references.
Exit Criteria: The RFC precisely bounds the pure composition, cross-value invariants, authority non-expansion, exclusions, and RED-first follow-up; focused governance passes and the verified increment is committed locally.
Verification: Focused decision/RFC/architecture governance tests and `git diff --check`.
Next Action: Run the full regression and close the documentation lifecycle.

### Increment 2 - verify-and-close-composition-specification

State: Completed
Depends On: define-composition-contract
Scope: Run the fresh full Java 17 Markdown-sensitive regression, append evidence once, synchronize lifecycle owners only where facts changed, and close the task.
Exit Criteria: Full regression passes, owners and append-only evidence are current, the final diff is clean, and the verified increment is committed locally without push.
Verification: README-owned full Gradle test task, staged diff review, and clean Git-state inspection.
Next Action: Implement the accepted pure composition value test-first under separate user continuation authority.

## Next

Implement the accepted RFC-0015 pure `ProfiledModelRequest` composition value
test-first under `com.enhancer.model`, without modifying existing request, gateway,
Tool, CLI, Scheduler, adapter, or persistence files.
