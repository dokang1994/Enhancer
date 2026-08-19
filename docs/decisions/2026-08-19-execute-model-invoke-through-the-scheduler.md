# User continuation request on 2026-08-19 into scheduler-executed model invocations

Status: Accepted Decision

## Context

The RFC-0013 minimum slice is delivered to `main` and observed passing on the
external verification host after the user's explicit push request. The completed
task records composing `model-invoke` into the durable Scheduler execution path's
allowed-tool scope as its next step, and the user requested that the project
continue. The Scheduler execution pipeline currently always executes `read-file`:
the WorkItem's allowed-tool scope is carried but never selects the executed tool,
and a WorkItem scoped only to `model-invoke` cannot execute at all.

## Decision

Authorize one bounded test-first task that lets a queued WorkItem execute one
governed model invocation against the deterministic fake through the same
isolation, evidence, verification, and RunRecord boundaries as read-file work,
without any queue or runtime schema change:

- `ModelInvokeTool` accepts exactly one prompt source: the existing inline
  `prompt` argument or a new `prompt-path` argument naming a contained regular
  UTF-8 file under the policy project root, read with the same containment and
  size bounds as the governed read-file Tool.
- `AgentLoopAgentRunExecution` derives the executed pipeline from the WorkItem's
  allowed-tool scope: a scope containing `read-file` keeps the existing pipeline
  unchanged; otherwise a scope containing `model-invoke` executes the
  deterministic fake through `ModelInvokeTool`, with the declared execution
  input's target path as the prompt document, its expected SHA-256 as the expected
  response digest verified by `DeterministicModelInvokeVerifier`, the WorkItem's
  required capability as the model-class label, and fixed runtime budget values
  whose gateway timeout fits strictly inside a per-tool timeout. Model work
  without a declared execution input fails closed, because the source-document
  fallback digest names the document, not a response.
- The governed submission surfaces accept a task whose scope names `model-invoke`
  without `read-file`, requiring at least one executable tool in scope.
- The promoting integration test drives one real-filesystem Scheduler cycle over a
  submitted model WorkItem to its verified terminal disposition with a resolvable
  RunRecord and evidence reference.

Verified GREEN increment boundaries authorize ordinary local commits under
Constitution 1.2.0. This decision records that the user explicitly requested and
received the prior slice's push; any further push remains a separate explicit user
request. It authorizes no queue/runtime/spool schema change, no migration, no
MessageEnvelope or store change, no network connection, credential, paid service,
MCP, routing, caching, streaming, or real provider invocation.

## Rationale

The slice makes the already-carried allowed-tool scope meaningful at execution
time, which is the smallest step that turns the delivered model gateway into
schedulable work. Reusing the declared execution input and required capability
avoids schema evolution entirely, keeps restart recovery and replay semantics
untouched, and keeps the deterministic fake the only executed gateway so evidence
digests stay reproducible end to end, including through the process-isolated child
that reuses the same execution seam.

## Consequences

- A WorkItem's allowed-tool scope now selects its execution pipeline
  deterministically; an ambiguous or unknown scope cannot silently execute the
  wrong tool.
- Prompt provenance is governed: the prompt document lives under the project root
  and is read inside the Tool isolation boundary with bounded size.
- The required-capability field doubles as the model-class label for model work; a
  capability that is not a valid model-class label fails closed as a recorded
  invalid-request failure.
- Scheduler retry, lease, cancellation, runtime-event, and recovery semantics are
  reused unchanged for model work.
- Model routing, provider selection, MCP, and remote transmission controls remain
  later Delivery Gate 9 scope with their own authority.
