# Current Task

## Status

Completed

## Task

Implement the RFC-0013 Delivery Gate 9 model gateway minimum vertical slice
test-first: the `com.enhancer.model` leaf package with the provider-neutral
`ModelGateway` port, immutable bounded request/response/usage records, the typed
failure contract, the injected credential-supplier boundary, the deterministic fake
as the only executed gateway, one bounded never-invoked provider adapter shape, and
the `model-invoke` Tool composed into the existing executor, promoted by one governed
CLI run persisting a lifecycle-valid replayable RunRecord.

## Task ID

implement-gate-9-model-gateway-minimum-slice

## Context

RFC-0013 is accepted with this implementation recorded as its follow-up task. The
existing Tool port already provides isolation, bounded failure conversion, policy
allowlisting, evidence capture, and RunRecord persistence, so the slice attaches at
that seam without touching the scheduler or runtime packages. The deterministic fake
requires no credential, so the slice runs completely in local-only mode. The shared
five-second CLI tool timeout becomes a per-tool composition value during this task,
and the gateway timeout must fit strictly inside the `model-invoke` tool timeout.

## Justified By

- User continuation request on 2026-08-19 into the RFC-0013 model gateway implementation
- Accept RFC-0013 defining the Delivery Gate 9 model gateway minimum slice

## Approval

The accepted continuation decision authorizes test-first source and test authoring
for the `com.enhancer.model` package and the `com.enhancer.cli` composition, focused
and full verification, development-session checkpoints, document synchronization,
and ordinary local commits at verified GREEN increment boundaries under Constitution
1.2.0.

It does not authorize push, merge, tag, release, deployment, network connection,
credential, paid-service invocation, MCP, routing, caching, streaming, real provider
invocation, force push, rebase, reset, amend, squash, destructive cleanup, or any
change to the `loop`, `run`, `verification`, `runtime`, or `bus` packages.

## Acceptance Criteria

- `ModelGateway`, `ModelRequest`, `ModelResponse`, `ModelUsage`,
  `ModelGatewayException`, and `ModelFailureCode` exist as immutable bounded
  contracts in `com.enhancer.model`, and provider wire formats cannot reach any
  persisted type.
- `DeterministicFakeModelGateway` is the only executed gateway; its response is a
  pure function of its input, and it refuses a response exceeding the declared
  response-length budget with a typed failure.
- Credentials enter only through an injected supplier port with no default
  provider, no environment scanning, and no logged, displayed, or persisted value;
  one package-private provider adapter shape compiles, maps `ModelRequest` to one
  remote HTTP message API and back, and is never invoked by tests, builds, or
  continuous integration.
- `ModelInvokeTool` executes under the name `model-invoke` only when the approved
  task scope and the execution policy both allow it, maps every gateway failure to
  a bounded typed `ToolResult` failure code, requires the gateway timeout strictly
  inside its per-tool policy timeout, and persists response text through the
  existing evidence envelope.
- Model output is treated as untrusted data: a response crafted as a directive is
  persisted verbatim as evidence and grants no authority, widens no scope, and
  alters no document, task, or policy.
- The promoting integration test is one governed CLI run executing `model-invoke`
  against the deterministic fake that atomically persists a lifecycle-valid
  replayable RunRecord whose evidence reference resolves, with digest-integrity
  verification of the persisted response evidence.
- No test opens a network connection, the build stays hermetic under
  `-Xlint:all -Werror`, and a fresh full Java 17 Markdown-sensitive regression
  passes before the task completes.

## Out Of Scope

MCP Server and MCP Client, model routing and locality policy, response caching,
fallback, streaming, per-model quality evaluation, cost budgets beyond the declared
timeout and response-length stub, real provider invocation, paid service use,
credential storage mechanics beyond the injected supplier boundary, prompt-injection
resistance and redaction pipelines, any change to the `loop`, `run`, `verification`,
`runtime`, or `bus` packages, push, merge, tag, release, and deployment.

## Allowed Tools

- read-file
- write-docs
- write-code
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit

## Dynamic Workflow

Workflow ID: implement-gate-9-model-gateway-minimum-slice

Mode: Sequential

Increment Limit: 3

Selection Rule: Select the first dependency-ready Pending increment in document order.

Stop Conditions: Stop on failed verification, governance-test failure that cannot be
resolved inside the selected increment, task/checkpoint drift, scope expansion,
network or credential requirement, subagent bound exhaustion, or insufficient
authority.

### Increment 1 - model-gateway-port-and-deterministic-fake

State: Completed

Depends On: none

Scope: Author the `com.enhancer.model` contracts RED-first: `ModelRequest`,
`ModelResponse`, `ModelUsage`, `ModelFailureCode`, `ModelGatewayException`, the
`ModelGateway` port, the injected credential-supplier port, the
`DeterministicFakeModelGateway`, and the bounded package-private provider adapter
shape that is compiled but never invoked.

Exit Criteria: Focused unit tests cover record bounds, the fake gateway round trip,
determinism, and budget refusal; the adapter shape compiles without any test, build,
or network invocation; focused governance tests pass.

Verification: Focused `com.enhancer.model` unit tests plus the architecture
governance suites, and `git diff --check`, before the increment commit.

Next Action: Implement the `model-invoke` Tool over the gateway port.

### Increment 2 - model-invoke-tool-with-bounded-failure-mapping

State: Completed

Depends On: model-gateway-port-and-deterministic-fake

Scope: Implement `ModelInvokeTool` RED-first against the existing `Tool` port:
argument validation, approved-scope and policy gating through the existing executor,
strict gateway-inside-policy timeout validation, evidence capture of response text
through the existing envelope, bounded mapping of every `ModelFailureCode` to a
typed `ToolResult` failure, and the untrusted-output invariant.

Exit Criteria: Focused unit tests cover success evidence, budget refusal, timeout
mapping, failure-code mapping, policy denial, and the untrusted-output invariant
using only the deterministic fake and stub gateways; focused governance tests pass.

Verification: Focused `com.enhancer.model` and `com.enhancer.tool` unit tests plus
the architecture governance suites, and `git diff --check`, before the increment
commit.

Next Action: Compose the governed `model-invoke` CLI run.

### Increment 3 - governed-model-invoke-cli-run

State: Completed

Depends On: model-invoke-tool-with-bounded-failure-mapping

Scope: Compose the slice into one governed CLI command: per-tool timeout values in
the CLI composition, a digest-integrity verifier for `model-invoke` results, the
`model-invoke` command over the existing controller, loop, finalizer, and RunRecord
store, the promoting integration test proving the persisted RunRecord replays and
its evidence reference resolves, and synchronized owning documents.

Exit Criteria: The promoting integration test passes, the replayed RunRecord is
lifecycle-valid with resolvable evidence, owning documents are synchronized once,
and a fresh full Java 17 Markdown-sensitive regression passes.

Verification: Full Java 17 Markdown-sensitive Gradle regression including the new
focused and integration tests, `git diff --check`, and staged-boundary review before
the final increment commit.

Next Action: Record the follow-up task after the slice completes.

## Verification

Increment evidence is appended once per increment to `docs/verification-log.md`
when the increment's exit criteria and declared verification are satisfied.

- Increment 1: the RED-first `com.enhancer.model` contracts, deterministic fake,
  and never-invoked adapter shape passed 16 focused model tests and the 13 focused
  governance tests with zero failures, errors, or skips, and `git diff --check` was
  clean. Evidence is appended once in `docs/verification-log.md`.
- Increment 2: the RED-first `ModelInvokeTool` with strict timeout validation,
  bounded failure mapping, and the untrusted-output invariant passed 25 focused
  model tests and the 13 focused governance tests with zero failures, errors, or
  skips, and `git diff --check` was clean. Evidence is appended once in
  `docs/verification-log.md`.
- Increment 3: the digest-integrity model verifier, the governed `model-invoke`
  CLI command with per-tool timeout values, and the promoting integration test
  passed, and the fresh full Java 17 Markdown-sensitive regression completed with
  899 tests, 10 environment-dependent skips, and zero failures or errors. Evidence
  is appended once in `docs/verification-log.md`.

## Next

Define the next bounded Delivery Gate 9 slice: compose `model-invoke` into the
durable Scheduler execution path's allowed-tool scope so a queued WorkItem can
execute one governed model invocation through the same evidence, verification, and
RunRecord boundaries, without adding routing, MCP, or any real provider.
