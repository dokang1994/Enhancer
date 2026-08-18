# RFC-0013: Model Gateway

Status: Accepted

## Purpose

Define the minimum Delivery Gate 9 model gateway vertical slice: one provider-neutral
invocation port, one deterministic fake, one provider adapter shape, and one governed
invocation persisted through the existing evidence and RunRecord path.

RFC acceptance does not imply capability maturity; maturity is recorded by
`PROJECT_STATE.md` alone.

## Minimum Slice Scope

- A new leaf package `com.enhancer.model` owns the slice. Nothing in `loop`, `run`,
  `verification`, `runtime`, or `bus` changes.
- `ModelGateway` is the provider-neutral port: `ModelResponse invoke(ModelRequest)`
  throwing a typed `ModelGatewayException`.
- `ModelRequest`, `ModelResponse`, and `ModelUsage` are immutable bounded records.
  Provider wire formats never leak past an adapter into any persisted type.
- `DeterministicFakeModelGateway` is the only executed gateway in this slice. Its
  response is a pure function of its input, so evidence digests remain reproducible.
- One provider adapter shape exists as a package-private class boundary that maps
  `ModelRequest` to one remote HTTP message API and back. The slice compiles and
  bounds it but never invokes it in tests, builds, or continuous integration.
- `ModelInvokeTool` implements the existing `com.enhancer.tool.Tool` port under the
  name `model-invoke`, composed into the existing `ToolExecutor` list. It reuses the
  existing isolation, timeout, cancellation, policy allowlisting, evidence capture,
  and RunRecord persistence without new plumbing.
- The tool executes only when the approved task scope and the execution policy both
  allow `model-invoke`. Model output is untrusted data and cannot grant authority,
  widen scope, or alter any document, task, or policy.

## Port Contracts

- `ModelRequest` carries a correlation identifier, a bounded UTF-8 prompt, a stable
  model-class label rather than a provider model name, and a bounded budget stub of
  one timeout and one maximum response length.
- `ModelResponse` carries the bounded response text, the model-class label that
  produced it, and a `ModelUsage` of bounded input and output unit counts.
- `ModelGatewayException` carries a `ModelFailureCode` peer enum, at minimum
  `PROVIDER_UNAVAILABLE`, `RESPONSE_INVALID`, `BUDGET_EXCEEDED`, and `TIMED_OUT`.
- `ModelInvokeTool` maps every gateway failure into the existing bounded
  `ToolResult` failure contract; no gateway condition escapes as an unclassified
  exception, and provider or budget failure produces an explicit failure code
  rather than a silent retry.
- The gateway timeout must fit strictly inside the tool execution policy timeout,
  which becomes a per-tool value instead of one shared constant.

## Credential And Outbound Boundary

- Credentials enter only through an injected credential-supplier port with no default
  provider, no repository-persisted secret, no environment scanning, and no logging,
  display, or persistence of the credential value.
- The deterministic fake requires no credential; the slice therefore runs completely
  in local-only mode, which remains a complete mode.
- Supplying a real credential, enabling a paid provider, or transmitting any
  repository content to a remote model is a separate explicit user authorization
  with its own accepted decision naming destination, purpose, and data
  classification. This RFC authorizes none of them.
- Response text is persisted through the existing evidence envelope; request and
  response evidence must never contain the credential.

## Verification Plan

- Focused unit tests cover the fake gateway round trip, budget refusal, timeout
  mapping, failure-code mapping, and the untrusted-output invariant, using only the
  deterministic fake and JUnit temporary directories.
- The evidence path is verified by digest integrity: the persisted response evidence
  digest must match the recorded content identity. Content-equality verification
  against an external expected digest remains valid for the deterministic fake and
  is not weakened for nondeterministic responses.
- The promoting integration test is one governed CLI run that executes
  `model-invoke` against the deterministic fake and atomically persists a
  lifecycle-valid replayable RunRecord whose evidence reference resolves.
- No test opens a network connection; the build stays hermetic under
  `-Xlint:all -Werror`.

## Exclusions

- MCP Server and MCP Client entirely.
- Model Router selection, privacy-aware local/remote routing, and locality policy
  beyond the local-only fake.
- Response caching, fallback, streaming, and the per-model quality evaluation
  harness.
- Cost budgets beyond the declared timeout and response-length stub.
- Real provider invocation, paid service use, credentials, and secret storage
  mechanics beyond the injected supplier boundary.
- Prompt-injection resistance, source attribution, and redaction pipelines, which
  remain Delivery Gate 9 scope for later slices.

## Follow-Up Implementation

Implementation of this slice requires a separate bounded task; the next task is owned
by `CURRENT_TASK.md`. The Contract Continuation Rule applies: this slice names its
integration consumer as the governed CLI run above, defines observable behavior and
failure modes, and identifies the promoting integration test.

## Prompt Book

### Codex Prompt

Implement the minimum model gateway slice exactly as scoped: the `com.enhancer.model`
package, the provider-neutral port and records, the deterministic fake, the bounded
provider adapter shape, and the `model-invoke` Tool composed into the existing
executor, with focused tests and no network, credential, or paid-service use.

### Claude Prompt

Review the slice for authority leaks: confirm model output cannot grant authority,
credentials cannot reach evidence or logs, provider formats cannot leak past the
adapter, and every failure maps to a bounded typed code.

### GPT Prompt

Explain how a governed model invocation flows from an approved task through the tool
executor to a persisted RunRecord, and why the deterministic fake keeps the slice
verifiable without any remote provider.
