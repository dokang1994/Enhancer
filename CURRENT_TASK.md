# Current Task

## Status

In Progress

## Task

Specify the next bounded Delivery Gate 9 slice as an accepted RFC-governed,
provider-neutral model execution-profile value layer over the existing gateway port.
Define capability, model class, locality, reasoning, context, token, cost, time, and
data-classification requirements without implementing routing, providers, or remote
transmission.

## Task ID

specify-provider-neutral-model-execution-profile

## Context

The RFC-0013 minimum model-gateway slice and its durable Scheduler execution path are
Completed, committed, and synchronized on `main`. The completed task records the
provider-neutral execution profile as the next bounded Gate 9 slice. The existing
`ModelRequest` carries only a model-class label plus timeout and response-length budget
stub, while the Roadmap requires an explicit provider-neutral profile before routing or
provider selection. The new contract must separate the Scheduler's current temporary
required-capability/model-class reuse, preserve the existing character response bound,
and avoid claiming that the deterministic fake's character-count usage is provider
token accounting.

## Justified By

- User continuation request on 2026-08-19 into the provider-neutral model execution profile specification
- Accept RFC-0013 defining the Delivery Gate 9 model gateway minimum slice

## Approval

The 2026-08-19 continuation request authorizes one documentation-only specification
increment: author and accept RFC-0014, record its accepted decision, update the Gate 9
architecture boundary and RFC indexes, run focused and full Markdown-sensitive Java 17
verification, use development-session checkpoints, synchronize lifecycle documents,
and create one ordinary local commit at the verified GREEN boundary under Constitution
1.2.0.

It does not authorize Java implementation, `ModelRequest` or `ModelGateway` signature
changes, routing, provider or endpoint selection, network or remote transmission,
credentials, paid-service use, MCP, caching, fallback, streaming, tokenizer or pricing
adapters, queue/runtime/submission/spool/RunRecord schema changes, migration, tag,
release, deployment, permission changes, or destructive cleanup. The user's subsequent
2026-08-19 delivery request authorizes non-force push and direct linear integration of
this completed specification into `origin/main`, plus one bounded delivery-evidence
follow-up commit and push after fresh remote verification.

## Acceptance Criteria

- RFC-0014 is Accepted and defines one immutable versioned `ModelExecutionProfile`
  value hierarchy covering required capability, model class, locality, reasoning,
  minimum context capacity, token budget, cost budget, maximum invocation time, and
  data classification.
- The RFC fixes bounded labels, closed enum vocabularies, integer units, cross-field
  invariants, deterministic equality, fail-closed validation, and the distinction
  between capability and model class. Token limits remain distinct from the existing
  response-character ceiling and from the current fake's character-based usage units.
- The profile contains requirements only. Locality, classification, and cost values do
  not grant Tool, network, remote-transmission, credential, provider, endpoint, paid-
  service, or data-release authority and cannot widen the intersection of task,
  execution-policy, and future outbound-policy constraints.
- RFC-0014 names the immediate follow-up implementation consumer and its RED-first
  contract tests while leaving router selection, adapters, actual tokenization and
  pricing, remote policy enforcement, and maturity promotion outside this task.
- `ARCHITECTURE.md`, `.ai/architecture.md`, `docs/rfcs/README.md`, `ROADMAP.md`, the
  decision index, and lifecycle documents are synchronized only where they own facts;
  `PROJECT_STATE.md` remains unchanged because specification acceptance is not
  implementation maturity.
- Focused governance tests, `git diff --check`, and a fresh full Java 17 Markdown-
  sensitive Gradle regression pass before the task is Completed.

## Out Of Scope

Java implementation, gateway invocation behavior changes, Scheduler behavior changes,
routing, provider/model/endpoint selection, network or remote transmission, credentials,
paid services, MCP, caching, fallback, streaming, tokenizer and pricing adapters,
redaction, prompt-injection resistance, source attribution, quality evaluation,
queue/runtime/submission/spool/RunRecord schema changes, migration, capability-maturity
promotion, push, merge, tag, release, and deployment.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- git-fetch
- git-push
- subagent-read-only

## Verification

- The focused governance selection completed with 10 tests, zero failures, errors, or
  skips after the required Dynamic Workflow cursor was restored.
- The fresh full Java 17 Markdown-sensitive Gradle regression completed with `BUILD
  SUCCESSFUL`: 171 result suites aggregating 910 tests, 10 environment-dependent
  skips, zero failures, and zero errors.
- `git diff --cached --check` was clean for the complete specification boundary.
  Evidence is appended once in `docs/verification-log.md`.

## Dynamic Workflow

Workflow ID: specify-model-execution-profile
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task drift, exhausted bounds, or unsafe recovery.

### Increment 1 - define-profile-contract

State: Completed
Depends On: none
Scope: Accept RFC-0014 and synchronize its decision, Gate 9 architecture boundary, and RFC indexes.
Exit Criteria: The provider-neutral value contract, validation rules, authority limits, and next implementation consumer are explicit.
Verification: Read the resulting RFC, accepted decision, architecture connections, and index registrations against the Active Task.
Next Action: Verify the synchronized repository documents and close the lifecycle boundary.

### Increment 2 - verify-and-close-profile-specification

State: Completed
Depends On: define-profile-contract
Scope: Run focused and full Markdown-sensitive verification, append evidence once, synchronize lifecycle documents, and create the required local commit.
Exit Criteria: All declared checks pass on the synchronized documents, the task is Completed, and the verified increment is committed locally without push.
Verification: Focused governance tests, git diff checks, and the README-owned full Java 17 Gradle test task.
Next Action: Implement the accepted RFC-0014 pure value layer test-first under a new Active Task.

### Increment 3 - deliver-profile-specification-to-main

State: In Progress
Depends On: verify-and-close-profile-specification
Scope: Verify local and remote main alignment, push the completed specification without force, observe external verification, record the delivery evidence, and push one bounded follow-up commit.
Exit Criteria: The intended commits are present on `origin/main`, local and remote main identities match, external verification is successful, the worktree is clean, and no synthetic merge or history rewrite occurred.
Verification: Fresh fetch and ref/merge-base comparison, non-force push output, remote ref identity, GitHub Actions conclusion, and final clean-tree inspection.
Next Action: Implement the accepted RFC-0014 pure value layer test-first under a new Active Task.

## Next

Implement the accepted RFC-0014 model execution-profile value layer test-first in
`com.enhancer.model`, without routing, providers, remote transmission, or changes to
durable Scheduler/store schemas.
