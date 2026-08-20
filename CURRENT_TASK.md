# Current Task

## Status

Completed

## Task

Implement the accepted RFC-0015 pure `ProfiledModelRequest` composition value test-
first under `com.enhancer.model`, without modifying existing request, gateway, Tool,
CLI, Scheduler, adapter, or persistence files.

## Task ID

implement-profiled-model-request-value

## Context

RFC-0013 owns the current five-component `ModelRequest` and executable gateway path.
RFC-0014 owns the complete immutable `ModelExecutionProfile`. RFC-0015 is Accepted and
defines an additive two-component composition that retains both complete values,
enforces only their model-class and invocation-time alignment, and has no runtime or
policy authority.

## Justified By

- User continuation request on 2026-08-20 into the RFC-0015 profiled model request implementation
- User continuation request on 2026-08-20 into the ModelRequest execution-profile composition specification

## Approval

The user's 2026-08-20 continuation request authorizes missing-symbol RED-first tests,
the minimum new immutable Java record under `com.enhancer.model`, focused and full Java
17 verification, development-session checkpoints, owning-document synchronization,
and ordinary local commits at verified GREEN increment boundaries under Constitution
1.2.0.

It does not authorize changes to existing `ModelRequest`, `ModelExecutionProfile`,
`ModelGateway`, Scheduler, CLI, Tool, fake or provider adapters, command or durable
schemas; runtime integration or policy evaluation; routing, provider or endpoint
selection; network or remote transmission; credentials, paid services, pricing,
tokenization, usage normalization, caching, fallback, retry, streaming, MCP, migration,
push, merge, tag, release, deployment, permission changes, or destructive cleanup.

## Acceptance Criteria

- `ProfiledModelRequest` is a public immutable record with exactly the ordered
  components `ModelRequest request` and `ModelExecutionProfile executionProfile`.
- Both complete values are required, retained unchanged, and participate in
  deterministic equality and hashing; no default, factory, nullable escape, copying,
  flattening, parsing, or serialization is introduced.
- Construction rejects unequal model-class labels by exact string equality.
- Construction accepts profile invocation time less than or equal to request timeout
  and rejects profile invocation time greater than request timeout.
- Required capability remains independent from model class, and response-character
  bounds remain independent from token budgets; deliberately unrelated valid values
  are accepted.
- Reflection guards prove the exact two-component shape and that the value implements
  no gateway, provider, Tool, policy, routing, or execution interface.
- No existing production source, caller, gateway, Tool, CLI, Scheduler, adapter,
  command schema, durable schema, or runtime behavior changes, and no network path is
  added.
- The focused test first establishes the intended missing-symbol RED, then the minimum
  implementation turns it GREEN; existing model-package and architecture tests and the
  fresh full README-owned Java 17 Markdown-sensitive regression pass.
- `git diff --check` and the staged increment diff checks are clean before completion.

## Out Of Scope

Changes to existing production files; runtime composition; task, execution, outbound,
locality, provider, destination, credential, or spend policy evaluation; routing,
provider selection, network or remote transmission, paid use, tokenizers, pricing,
usage normalization, MCP, persistence, migrations, capability maturity beyond the pure
composition value, push, merge, tag, release, and deployment.

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

## Verification

Evidence will be appended once per increment to `docs/verification-log.md` after the
declared RED/GREEN and regression checks are complete.

- Increment 1: the focused test first failed compilation only on the missing
  `ProfiledModelRequest` symbol, then passed 6 tests after the minimum record was
  added. The combined model-package and architecture selection passed 61 tests with
  zero failures, errors, or skips, and `git diff --check` was clean. Evidence is
  appended once in `docs/verification-log.md`.
- Increment 2: the fresh full README-owned Java 17 Markdown-sensitive regression passed
  925 tests across 173 result suites with zero failures and errors and 10 environment-
  dependent skips. Contract Verified maturity and lifecycle owners were synchronized
  without claiming runtime integration, and evidence is appended once in
  `docs/verification-log.md`.

## Dynamic Workflow

Workflow ID: implement-profiled-model-request-value
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed or conflicting RED classification, failed verification, task or checkpoint drift, scope expansion, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - implement-pure-profiled-request

State: Completed
Depends On: none
Scope: Add the RFC-0015 focused contract test, classify the missing-symbol RED, and implement only the new two-component immutable model-package value required to turn it GREEN.
Exit Criteria: Focused composition and model-package regression tests pass with the exact shape, alignment, independence, and authority-non-expansion contract, architecture governance passes, and the verified increment is committed locally.
Verification: Missing-symbol RED, focused composition test, complete model-package tests, architecture governance tests, and `git diff --check`.
Next Action: Run the full regression and synchronize owning lifecycle documents.

### Increment 2 - verify-and-close-profiled-request

State: Completed
Depends On: implement-pure-profiled-request
Scope: Run the fresh full Java 17 Markdown-sensitive regression, update maturity and lifecycle owners only where evidence changes them, append evidence once, and close the task.
Exit Criteria: Full regression passes, owning documents and append-only evidence are current, the final diff is clean, and the verified increment is committed locally without push.
Verification: README-owned full Gradle test task, final focused governance checks, staged diff review, and clean Git-state inspection.
Next Action: Specify the smallest separately authorized invocation-admission contract that can source complete profiles and enforce task, execution, profile, and outbound-policy intersection before gateway execution.

## Next

Specify the smallest separately authorized invocation-admission contract that can
source complete profiles and enforce task, execution, profile, and outbound-policy
intersection before gateway execution, without routing, providers, or remote
transmission.
