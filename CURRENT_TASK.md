# Current Task

## Status

In Progress

## Task

Implement the accepted RFC-0014 model execution-profile pure value layer test-first
under `com.enhancer.model`, without composing it into the existing gateway, Scheduler,
CLI, Tool, or persistence paths.

## Task ID

implement-model-execution-profile-value-layer

## Context

RFC-0014 is Accepted and delivered on `main`. It defines one provider-neutral,
immutable, versioned `ModelExecutionProfile` requirement value with distinct capability
and model-class labels; closed locality, reasoning, and data-classification
vocabularies; bounded context, token, cost, and invocation-time values; deterministic
value semantics; and fail-closed construction. The existing RFC-0013 gateway and
Scheduler composition remain Contract Verified with their current request, timeout,
response-character, and character-based fake-usage contracts unchanged.

## Justified By

- User continuation request on 2026-08-20 into the RFC-0014 model execution profile value layer implementation
- User continuation request on 2026-08-19 into the provider-neutral model execution profile specification

## Approval

The user's 2026-08-20 continuation request authorizes the immediate implementation
consumer named by RFC-0014: RED-first tests and the minimum pure immutable Java value
types under `com.enhancer.model`, focused and full Java 17 verification,
development-session checkpoints, owning-document synchronization, and ordinary local
commits at each verified GREEN increment boundary under Constitution 1.2.0.

It does not authorize changes to `ModelRequest`, `ModelGateway`, Scheduler, CLI, Tool,
provider adapters, command or durable schemas; routing or provider selection; network
or remote transmission; credentials or paid-service use; pricing feeds, currency
conversion, tokenizers, usage normalization, caching, fallback, streaming, MCP,
redaction, prompt-injection handling, source attribution, quality evaluation,
migration, push, merge, tag, release, deployment, permission changes, or destructive
cleanup.

## Acceptance Criteria

- `ModelExecutionProfile` is an immutable value with exactly the RFC-0014 components:
  fixed `model-execution-profile-v1` schema version, required capability, model class,
  locality requirement, reasoning requirement, minimum context tokens, token budget,
  cost budget, maximum invocation time, and data classification.
- Capability and model-class labels use their RFC bounds and lower-case hyphenated
  grammar; all components are required and invalid, null, blank, unknown, or implicit-
  default values fail closed at construction.
- Locality is exactly `LOCAL_ONLY` or `POLICY_CONSTRAINED`; reasoning is exactly
  `MINIMAL`, `STANDARD`, or `EXTENDED`; data classification is exactly `PUBLIC`,
  `INTERNAL`, `CONFIDENTIAL`, or `RESTRICTED`.
- `ModelTokenBudget` enforces positive values no greater than 1,000,000,000 and
  overflow-safe `maxInputTokens + maxOutputTokens <= maxTotalTokens <=
  minimumContextTokens` validation through the profile.
- `ModelCostBudget` enforces an upper-case three-letter currency code and integer
  microunits from zero through 1,000,000,000,000,000 inclusive; zero remains a
  free-only requirement rather than spend authority.
- Maximum invocation time is positive, millisecond-representable, and no greater than
  five minutes. Complete value retention, equality, and hashing are deterministic.
- A reflection-based contract test prevents prompt, response, task, Tool, provider,
  endpoint, destination, credential, price-table, tokenizer, route, and result fields
  from entering the profile value.
- Existing RFC-0013 model, Tool, Scheduler, CLI, timeout, and architecture tests remain
  unchanged and pass; no network connection, production adapter invocation, or schema
  change is introduced.
- Focused tests establish the intended missing-symbol RED before implementation; fresh
  focused and full Java 17 Markdown-sensitive verification and `git diff --check` pass
  before completion.

## Out Of Scope

Gateway or Scheduler composition, signature changes, routing, provider/model/endpoint
selection, remote execution or transmission, credentials, paid services, pricing or
tokenizer adapters, usage normalization, MCP, caching, fallback, retry, streaming,
redaction, prompt-injection resistance, source attribution, quality evaluation,
command/durable schemas, migration, capability maturity beyond the pure value layer,
push, merge, tag, release, and deployment.

## Allowed Tools

- read-file
- write-code
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- subagent-read-only

## Verification

Evidence will be appended once per increment to `docs/verification-log.md` after the
declared RED/GREEN and regression checks are complete.

- Increment 1: the focused test first failed compilation on the six missing RFC-0014
  value types, then passed 9 profile contract tests after the minimum implementation.
  The combined model-package and architecture governance selection passed 55 tests
  with zero failures, errors, or skips, and `git diff --cached --check` was clean.
  Evidence is appended once in `docs/verification-log.md`.

## Dynamic Workflow

Workflow ID: implement-model-execution-profile-value-layer
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed or conflicting RED classification, failed verification, task or checkpoint drift, scope expansion, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - implement-pure-profile-values

State: Completed
Depends On: none
Scope: Add the RFC-0014 RED contract tests, classify the missing-symbol failure, and implement only the immutable model-package profile values required to turn them GREEN.
Exit Criteria: Focused profile tests and existing model-package regressions pass with exact RFC validation and authority-non-expansion shape, and the verified increment is committed locally.
Verification: RED compilation evidence, focused profile/model tests, architecture governance tests, and `git diff --check`.
Next Action: Run the full regression and synchronize owning lifecycle documents.

### Increment 2 - verify-and-close-profile-values

State: In Progress
Depends On: implement-pure-profile-values
Scope: Run the fresh full Java 17 Markdown-sensitive regression, update maturity and lifecycle owners only where evidence changes them, and close the task.
Exit Criteria: Full regression passes, owning documents and append-only verification evidence are current, the final diff is clean, and the verified increment is committed locally without push.
Verification: README-owned full Gradle test task, final focused governance checks, staged diff review, and clean Git-state inspection.
Next Action: Specify the smallest separately authorized composition contract that can attach a complete profile to `ModelRequest` while preserving RFC-0013 compatibility and policy intersection.

## Next

Specify the smallest separately authorized Gate 9 composition contract that can attach
a complete `ModelExecutionProfile` to `ModelRequest` while preserving RFC-0013
compatibility and task, execution, and future outbound-policy intersection, without
routing, providers, or remote transmission.
