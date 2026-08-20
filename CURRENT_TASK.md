# Current Task

## Status

In Progress

## Task

Specify the smallest separately authorized Delivery Gate 9 invocation-admission
contract that can receive an explicitly complete `ProfiledModelRequest`, bind it to
real task capability and Tool authority, and enforce the task, `ExecutionPolicy`,
profile, and outbound-policy intersection before gateway execution.

## Task ID

specify-model-invocation-admission

## Context

RFC-0013 owns the current executable gateway path, RFC-0014 owns complete model
requirements, and RFC-0015 owns their pure aligned composition. The composition is
Contract Verified but unused in production. `ApprovedTask` and `ExecutionPolicy`
separately expose Tool scope, Scheduler `WorkItem` exposes a distinct required
capability, and the direct CLI path has no complete-profile or capability source.

## Justified By

- User continuation request on 2026-08-20 into the model invocation admission specification
- User continuation request on 2026-08-20 into the RFC-0015 profiled model request implementation

## Approval

The user's 2026-08-20 continuation request authorizes a documentation-only RFC-0016
contract task: reconcile the existing authority and execution types, define the minimum
provider-neutral admission inputs, decision and typed rejections, document explicit
profile/capability sourcing and fail-closed policy intersection, run fresh Java 17
Markdown-sensitive verification, use development-session checkpoints, synchronize
owning documents, and create ordinary local commits at verified GREEN increment
boundaries under Constitution 1.2.0.

It does not authorize Java or runtime implementation; changes to existing request,
profile, gateway, Scheduler, CLI, Tool, fake or provider adapters, command or durable
schemas; routing, provider, model, endpoint or destination selection; network or remote
transmission; credentials, paid services, pricing, tokenization, usage normalization,
caching, fallback, retry, streaming, MCP, migration, push, merge, tag, release,
deployment, permission changes, or destructive cleanup.

## Acceptance Criteria

- One Accepted RFC-0016 defines a provider-neutral admission boundary strictly before
  `ModelGateway.invoke`, with a named immediate pure-contract implementation consumer
  and separately authorized runtime integration consumer.
- A caller must supply one already complete valid `ProfiledModelRequest`; admission
  creates no profile, default, inference, registry lookup, fallback, or nullable escape.
- The contract names how approved task Tool scope, active `ExecutionPolicy`, and an
  authoritative required-capability projection enter admission without encoding the
  Scheduler's current capability/model-class conflation.
- Admission requires both task and execution policy to allow `model-invoke`, requires
  the authoritative capability to equal the profile's required capability, preserves
  `profile maximumInvocationTime <= gateway timeout < execution-policy timeout`, and
  never compares response characters with tokens.
- The current absence of outbound/provider authority fails closed: `LOCAL_ONLY` can be
  evaluated as local-only requirement data, while `POLICY_CONSTRAINED` cannot authorize
  remote execution or transmission and must not be admitted without a later accepted
  outbound policy.
- Admission produces one bounded deterministic decision with closed typed rejection
  reasons; an admitted decision retains the exact profiled request but carries no
  route, provider, model name, endpoint, destination, credential, price, tokenizer,
  response, usage, or external-effect authority.
- Current RFC-0013 through RFC-0015 types and all existing gateway, fake, Tool, CLI,
  Scheduler, adapter, command-schema, durable-schema, and runtime behavior remain
  unchanged by this documentation task.
- The RFC compares rejected alternatives, defines RED-first pure-contract follow-up
  tests, and leaves caller migration, runtime integration, routing, providers, outbound
  transmission, credentials, and paid use separately authorized.
- Fresh focused governance and full README-owned Java 17 Markdown-sensitive tests and
  `git diff --check` pass before completion.

## Out Of Scope

Java or runtime implementation; modifications to existing source or schemas; caller
migration; task, Tool, Scheduler, CLI, or gateway wiring; routing or provider selection;
network or remote transmission; endpoint or destination policy; credentials; paid use;
pricing, tokenization, usage normalization, persistence, migration, capability maturity,
push, merge, tag, release, and deployment.

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

- Increment 1: RFC-0016 was accepted after independent shape and authority reviews
  converged on the same stateless evaluator, exact authority sources, five-code first-
  match rejection contract, local-only ceiling, and ephemeral admitted meaning. The
  focused governance and authority-document selection passed 21 tests with zero
  failures, errors, or skips, and `git diff --check` was clean. Evidence is appended
  once in `docs/verification-log.md`.

## Dynamic Workflow

Workflow ID: specify-model-invocation-admission
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on authority conflict, failed verification, task or checkpoint drift, scope expansion, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - define-invocation-admission-contract

State: Completed
Depends On: none
Scope: Reconcile task, capability, execution, profile, and absent outbound authority; compare minimum admission shapes; and add RFC-0016 plus required architecture and roadmap references.
Exit Criteria: The RFC precisely defines explicit sources, deterministic admission/rejection, policy intersection, authority non-expansion, compatibility, exclusions, and RED-first pure-contract follow-up; focused governance passes and the increment is committed locally.
Verification: Focused decision/RFC/architecture governance tests and `git diff --check`.
Next Action: Run the full regression and close the documentation lifecycle.

### Increment 2 - verify-and-close-invocation-admission-specification

State: In Progress
Depends On: define-invocation-admission-contract
Scope: Run the fresh full Java 17 Markdown-sensitive regression, append evidence once, synchronize lifecycle owners only where facts changed, and close the task.
Exit Criteria: Full regression passes, owners and append-only evidence are current, the final diff is clean, and the verified increment is committed locally without push.
Verification: README-owned full Gradle test task, staged diff review, and clean Git-state inspection.
Next Action: Implement the accepted pure invocation-admission contract test-first under separate user continuation authority.

## Next

Complete Increment 2: run the full README-owned Java 17 regression, synchronize only
changed lifecycle owners, and close the task without push.
