# RFC-0023: Typed ModelWork Process Execution And Model RunRecord V2 Finalization

Status: Accepted

## Purpose

Define the smallest deterministic-fake production connection from one retained typed
`ModelWorkPayload` through fresh Scheduler preparation, candidate suitability,
exact-request budget evaluation and invocation, closed ToolResult/evidence mapping,
independent response verification, Model RunRecord v2 publication, parent result
validation, durable finalization, retry, and crash recovery.

This RFC connects existing contracts. It does not implement the connection, enable a
typed ModelWork producer or receiver, expose a supported entry point, add a provider,
perform a model or Tool call, or change capability maturity.

## Relationship To Existing Contracts

RFC-0018 continues to own the exact typed ModelWork envelope/profile through durable
transport, submission, queue, and runtime formats. RFC-0019 continues to own Model
RunRecord v2, exact active-task resolution, explicit Scheduler request and policy
inputs, fresh request/policy/admission preparation, deterministic AgentRun-bound record
identity, and the pre-reference versus post-reference recovery split. RFC-0020 through
RFC-0022 continue to own the exact deterministic-fake candidate, token semantics and
capacity, suitability, actual-request budget decision, and exact candidate-bound
invoker.

Existing Gate 3 through Gate 8 boundaries continue to own `ToolRequest`, `ToolResult`,
`EvidenceRecorder`, `IndependentVerifier`, process isolation, Work/Result transport,
AgentRuntime, pending finalization, retry effects, queue disposition, and deterministic
runtime Result publication. A model gateway return is untrusted input and none of
these downstream owners may be skipped.

RFC-0023 owns only:

- the child-local composition order that connects those accepted boundaries;
- exact identity and policy flow across that order;
- closed pre-call, returned-response, gateway-failure, Tool, and verification outcomes;
- one model-specific lifecycle construction and v2-only publication boundary;
- payload-kind-aware parent, finalizer, status, and recovery validation;
- crash-prefix precedence and the one-invocation-per-AgentRun retry topology;
- the sufficiency limits of current durable schemas; and
- the bounded RED-first implementation sequence.

It does not define general model routing, provider adapters, tokenizers, submission,
receive, CLI, MCP, network, credentials, price, fallback, cache, or streaming.

## Current Gap

Typed ModelWork is retained durably, and every value boundary through the exact fake
invoker is Contract Verified, but production execution remains intentionally blocked.
The current process child and handlers reject ModelWork. The parent execution,
`DurableAgentRunWorker`, durable finalizer, and Scheduler recovery/status readers
resolve only RunRecord v1. `ModelInvokeTool` rereads or reconstructs the request, owns
a generic gateway, and can retain raw exception text, so it cannot be reused for the
identity-preserving connection.

The current application `AgentRunFinalizer` also cannot first persist v1 at the
deterministic AgentRun identity and then wrap it as v2. Cross-kind identity reuse is
correctly rejected, and its lifecycle logical-run choice differs from the Model
RunRecord v2 work-message binding. The model path therefore needs a v2-only lifecycle
construction/publication boundary rather than a v1 projection.

## Supported Topology

The first connection is deterministic-fake-only and process-isolated. Payload kind,
not allowed-Tool precedence, selects it. A typed ModelWork envelope that also contains
`read-file` cannot enter the legacy path.

For one Goal and AgentRun, the parent:

1. retains the exact runtime `WorkItem` and pending-finalization identities;
2. derives `AgentRunRecordIdentity.reference(goalId, agentRunId)`;
3. point-resolves that identity as Model RunRecord v2 before launch;
4. when missing only, spools the unchanged work envelope and launches one child with
   the exact WorkItem identity, independent capability, explicit roots, and scalar
   Scheduler/process limits; and
5. accepts only one fully bound Result claim or one fully bound point-recovered record.

The parent never serializes `ExecutionPolicy`, `Admitted`, `Suitable`, `Ready`, a
candidate, a gateway, or an invocation result. Java reference identity is meaningful
only inside the executing child. The parent validates explicit values and durable
bindings, not cross-process object identity.

## Child-Local Identity-Preserving Chain

On every record-missing execution entry, the child performs exactly this order:

1. reconstruct the exact `WorkItem` from the unchanged spooled envelope plus the
   parent-controlled WorkItem identity and independent required capability;
2. derive one pure canonical evidence-run/correlation identity in a repository-owned
   namespace from the Goal and AgentRun identities, without touching the evidence
   store;
3. call `SchedulerModelInvocationPreparer.prepare` once with the explicit invocation
   limits and policy inputs;
4. retain its exact task `T`, one exact `ExecutionPolicy P`, exact
   `ProfiledModelRequest PR`, and admission decision `A`;
5. only for `Admitted A`, bind one exact final `DeterministicFakeModelGateway G` into
   one exact repository-owned `DeterministicFakeModelCandidate C`;
6. evaluate RFC-0020/RFC-0021 suitability once and continue only with exact
   `Suitable S`;
7. call RFC-0022 preparation with exact `S` and the same `P` by reference and continue
   only with exact `Ready R`;
8. call `DeterministicFakeExactRequestInvoker.invoke(R)` once; and
9. only for a gateway-returned `Succeeded` or `GatewayFailed` result, materialize the
   lifecycle through the exact model Tool/evidence boundary described below.

The required child-side identity chain is:

```text
P == preparation.executionPolicy()
A.profiledRequest() == PR
S.admitted() == A
S.candidate() == C
C.gateway() == G
R.suitable() == S
R.executionPolicy() == P
invocationResult.ready() == R
```

Suitability does not accept a policy parameter; it traverses the exact admitted value
while the same preparation tuple retains `P`. The same `P` is then passed by reference
to exact-request preparation, the invoker through `Ready`, and the child-local Tool
control call. The outer process launcher receives only explicit scalar limits. It must
not be described as transporting Java object identity.

No step rereads the task or prompt, reconstructs the request or policy, replaces the
candidate or gateway, converts capability into model class, or uses an ambient default.

## Correlation And Evidence-Run Allocation

RFC-0019 requires the exact `ModelRequest` correlation to come from the attempt's
logical evidence run, while accepted pre-call refusals require zero evidence activity.
The current `EvidenceStore.createRun()` combines identity allocation with a filesystem
write, so it cannot be called before admission, suitability, budget, and invocation
eligibility are known.

The implementation must add a pure canonical evidence-run identity function and a
separate exact, contained, idempotent namespace-ensure operation. The identity is
derived from Goal and AgentRun in a domain distinct from the deterministic record
identity. It becomes both `ModelRequest.correlationId()` and
`ToolRequest.correlationId()`. Namespace creation occurs lazily only when an evidence
artifact must be persisted after a returned invocation outcome. Short inline evidence
need not create a directory. Exact replay must preserve an existing valid namespace
and fail closed on a non-directory, symbolic, foreign, or invalid point.

This is an additive API and containment change, not a durable message, evidence,
record, runtime, queue, result, checkpoint, or manifest format migration. Random
preparation-time `createRun()` and an empty evidence directory on refusal are rejected.

The work-envelope logical-run identity remains distinct. The nested lifecycle
`RunRecord.logicalRunId()` equals the exact work-envelope logical run as required by
`ModelRunRecord`; it is not replaced by the request/evidence correlation.

## Closed Pre-Call Outcomes

These outcomes stop before gateway return and create no Tool request execution,
`ToolResult`, evidence namespace or artifact, Model RunRecord, transport Result,
reference checkpoint, durable runtime Result, queue disposition, or retry decision:

- exact active-task, source, prompt, or RFC-0015 construction failure;
- RFC-0016 admission rejection;
- RFC-0020/RFC-0021 candidate rejection;
- RFC-0022 exact-request budget refusal; and
- RFC-0022 invoker policy, timeout, or cancellation refusal.

The child reports only a closed nonpersistent exit family to the parent and exits
nonzero. Raw prompt, response, path, exception, provider, or environment text is not
transported. The parent does not fabricate a failed ToolResult or RunRecord for an
invocation that did not return an outcome.

This intentionally preserves the prior zero-activity decision. Current durable schemas
cannot represent a terminal pre-call refusal because `ResultPayload` requires a record
reference. The pending Goal/AgentRun prefix therefore remains active and recoverable;
the durable retry controller is not entered. A later explicit re-entry performs the
entire fresh chain again, while an operator may correct the governed input or use the
existing authorized cancellation path. The implementation must not create an automatic
tight retry loop. A durable refusal reason or automatic terminal disposition requires
a separately accepted message/runtime schema and migration contract.

## Returned Invocation Outcome And Tool Boundary

The exact invoker runs in the already isolated child process. Its `Refused` outcome
stops as above. Only `Succeeded` and `GatewayFailed` are handed to a one-shot typed
model result-materialization Tool through the existing `ToolExecutor` with the same
child-local `P` and an exact `model-invoke` `ToolRequest`. That Tool is not
`ModelInvokeTool`: it retains the exact invocation result, never invokes a gateway,
never reads the prompt, and never reconstructs `ModelRequest`.

The `ToolRequest` carries exactly the four v2-bound arguments:

```text
prompt-path
model-class
timeout-millis
max-response-length
```

They are projected from the retained work and exact request and must equal the values
later required by `ModelRunRecord`. The Tool adapter rejects any different request,
policy instance, Ready chain, Tool name, or argument set before producing a durable
result.

The closed gateway-failure mapping is:

| `ModelFailureCode` | `ToolFailureCode` |
|---|---|
| `TIMED_OUT` | `TIMED_OUT` |
| `PROVIDER_UNAVAILABLE` | `TEMPORARY_FAILURE` |
| `RESPONSE_INVALID` | `INVALID_RESULT` |
| `BUDGET_EXCEEDED` | `TOOL_REPORTED_FAILURE` |

A gateway failure produces a failed `ToolResult` with code-only bounded evidence,
verification `NOT_PERFORMED`, no nested expected digest, and worker/final stop
`FAILED`. The typed expected-response digest remains in the unchanged ModelWork
envelope. No raw `ModelGatewayException` message, cause, stack, path, prompt, or
provider text is persisted.

The model path sanitizes every `ToolExecutor`-owned failure into a stable code-only
model result before lifecycle publication. Tool isolation-capacity, policy,
cancellation, timeout, interruption, invalid-result, and execution failures retain
their closed `ToolFailureCode`; generic exception diagnostics never enter evidence or
the record. Expected evidence I/O failure is likewise code-only. Unchecked invariant
failures may stop the child, but they do not acquire a record or queue transition by
being relabeled with raw text.

One AgentRun performs at most one gateway call. The generic Agent Loop does not retry
the model Tool inside that AgentRun. Durable retry, when eligible, creates a replacement
AgentRun and therefore repeats fresh preparation.

## Successful Response Validation And Evidence

`Succeeded` means only that the exact fake gateway returned. Before response text is
captured, the materializer requires:

- response model class equals the exact request model class;
- response UTF-16 length equals `Ready.predictedResponseUtf16Length()`;
- generic fake `ModelUsage.inputUnits()` equals `request.prompt().length()`;
- generic fake `ModelUsage.outputUnits()` equals `response.text().length()`; and
- every current `ModelResponse` bound remains valid.

The usage checks preserve the gateway's existing character-based contract. They must
not compare `ModelUsage` with the RFC-0021 Unicode-scalar token counts or claim provider
token semantics.

Only a structurally valid response is captured as untrusted `VerificationEvidence`.
Long output is persisted under the exact lazily ensured evidence run before any record
publication. `DeterministicModelInvokeVerifier` then independently resolves and hashes
the Tool evidence against the typed expected-response digest without rereading the
prompt. The result is never self-verification by the gateway or Tool.

The lifecycle mapping is:

| Tool/verification outcome | Worker stop | Final stop | Nested expected digest |
|---|---|---|---|
| successful Tool, `VERIFIED` | `AWAITING_VERIFICATION` | `COMPLETED` | exact typed digest |
| successful Tool, `REJECTED` | `AWAITING_VERIFICATION` | `AWAITING_VERIFICATION` | exact typed digest |
| successful Tool, `UNVERIFIED` | `AWAITING_VERIFICATION` | `AWAITING_VERIFICATION` | exact typed digest |
| failed Tool, `NOT_PERFORMED` | `FAILED` | `FAILED` | absent |

`NOT_PERFORMED` is invalid for a successful verification-waiting result. Performed
verification is invalid for a failed Tool result.

## Model-Specific Lifecycle And V2 Publication

The child uses either a separated pure lifecycle builder or a model-specific finalizer.
It must not call the current v1-persisting `AgentRunFinalizer` at the deterministic
identity. The model boundary constructs one complete nested `RunRecord` with:

- work-envelope logical-run identity;
- millisecond timestamp precision;
- exact resolved task;
- exact prepared `model-invoke` Tool request;
- a policy decision snapshot derived from the same `P`;
- sanitized exact ToolResult;
- the verification mapping above; and
- exactly one iteration.

It then constructs `ModelRunRecord` from the exact WorkItem identity, independent
capability, complete unchanged ModelWork envelope, exact `ModelRequest`, and that
lifecycle record. It persists only v2 at
`AgentRunRecordIdentity.recordId(goalId, agentRunId)`. Publication is atomic and exact
replay is non-writing. A v1 artifact, changed v2 content, corrupt content, unknown kind,
or foreign identity fails closed without overwrite.

Evidence, when required, is durable before the v2 record. The complete v2 record is
durable before the child publishes its transport Result. The Result payload carries
the exact deterministic reference and the nested lifecycle verification status.

The child transport Result and the later deterministic runtime Result are distinct:

- the isolated-child Result uses the existing transport identity and producer and is
  the parent's completion claim; and
- the durable-finalizer Result uses its deterministic AgentRun-derived identity and is
  the runtime state transition input.

Neither message is the record, and neither can replace record resolution.

## Parent Validation And Reference Checkpoint

Before returning a reference, the parent requires:

- exactly one regular non-symbolic work point at `queue("work")`, with complete
  envelope equality to the runtime WorkItem;
- exactly one Result at `queue("isolated-worker-result")`;
- Result correlation and logical run equal the Work envelope, causation equal the Work
  message identity, and task identity equal the retained task revision;
- Result reference exactly equal
  `AgentRunRecordIdentity.reference(goalId, agentRunId)`;
- `ModelRunRecordStore.resolveModel`, never v1 projection;
- exact record reference/identity, WorkItem identity, independent capability, complete
  work envelope, profile, task revision, Tool scope, target, and expected digest;
- exact request model class, timeout, response ceiling, prompt snapshot, evidence
  correlation, Tool request arguments, and work-logical-run binding;
- exact persisted policy values from the explicit child composition inputs;
- internally valid ToolResult/evidence/verification lifecycle; and
- claimed Result status equal to the nested verification status.

Goal and AgentRun are bound by the deterministic record identity rather than duplicated
inside `ModelRunRecord`. Exact candidate, policy object, Ready, counts, response object,
and invocation-result identities remain child-side proofs; the current record must not
be claimed to persist them.

The worker checkpoints the validated reference before any spool cleanup or execution
acknowledgement. Cleanup then removes only the exact Goal/AgentRun spool tree
idempotently. If reference checkpointing fails, both spool directions remain and
re-entry validates the existing result or point record without invoking again.

A Result without its Work point before the reference checkpoint is invalid. After the
reference is checkpointed, the worker skips execution and does not require either spool
point; their absence may be the result of successful idempotent cleanup.

## Payload-Kind-Aware Finalization And Recovery Readers

Before the first reachable v2 writer, all current v1-only consumers must receive a
closed payload-kind-aware path while preserving their existing v1 behavior:

- `ProcessIsolatedAgentRunExecution`;
- `IsolatedWorkMessageHandler`;
- `IsolatedResultMessageHandler`;
- `IsolatedWorkerMain`;
- `DurableAgentRunWorker.processIsolated`;
- `DurableAgentRunFinalizer`;
- `SchedulerRecoveryStatusReader` and `SchedulerRecoveryStatus`; and
- `SchedulerInvocationRecoveryStatusReader`.

The shared `FileSystemRunRecordStore` may be supplied through both narrow v1 and v2
ports. No consumer may obtain a model lifecycle by calling `RunRecordStore.resolve`
and projecting the nested record.

The durable finalizer exact-replays the validated model Result, writes its deterministic
runtime Result, and then owns state and queue effects:

- `VERIFIED`: AgentRun and Goal complete; queue becomes `VERIFIED_COMPLETED`;
- `REJECTED`, `UNVERIFIED`, or `NOT_PERFORMED`: AgentRun fails; Goal becomes
  `RETRY_PENDING`; the WorkItem remains active; and
- refused durable retry: Goal is terminally failed, then queue becomes `FAILED`.

Retry admission uses the existing immutable attempt count and Goal effect ledger. An
admitted retry creates and checkpoints one replacement AgentRun identity, preserves the
failed attempt/reference, and runs the complete fresh child chain. A refusal records
the existing terminal effect before queue disposition. No model-specific retry schema
is added.

## Crash Prefixes And Precedence

| Durable prefix | Required recovery |
|---|---|
| Goal/AgentRun checkpointed; runtime absent | Re-drive existing dispatcher with the same identities; empty queue clears the intent. |
| Runtime active; no reference and no complete deterministic v2 | Recover the existing lease/fence, then a later explicit execution entry repeats fresh task, prompt, policy, admission, candidate, budget, and invocation eligibility. |
| Gateway returned or evidence exists; v2 absent | At-least-once boundary: orphan evidence grants no transition and a later entry may invoke again. |
| Complete exact v2 exists; Result spool absent | Point-resolve, fully validate, and return the record without child launch or invocation. |
| Valid Result exists; reference absent | Validate Work, Result, and v2 closure; checkpoint reference before cleanup or acknowledgement. |
| Reference checkpoint fails | Retain spool; re-entry validates existing Result/v2 and does not invoke. |
| Reference checkpointed | Skip execution, task/prompt reads, admission, suitability, budget, Tool, gateway, and record write; retry cleanup, acknowledgement, and finalization only. |
| Runtime awaiting verification | Resolve exact v2, exact-replay deterministic runtime Result, and continue finalization. |
| AgentRun already terminal | Validate exact historical Result/reference before retry or queue effects; changed reference fails closed. |
| Replacement AgentRun checkpointed | Fresh preparation under the new AgentRun; no prior ephemeral decision or policy crosses the boundary. |
| Terminal queue disposition persisted; checkpoint clear failed | Replay terminal observation only and retry clear; never invoke or rewrite the record. |

An exact valid complete v2 record takes precedence over a persisted process-timeout
fact because the record is the stronger persist-before-exposure boundary. Corrupt,
cross-kind, changed, or foreign content fails closed and is not hidden by timeout.
Only when the deterministic record is missing may the timeout fact govern the
no-reference process failure. The parent cannot fabricate v2 from a killed child.

Before a complete v2 record, active-task drift fails fresh resolution and prompt drift
becomes part of a newly prepared exact request. After a complete v2 record, current
task or prompt drift cannot invalidate historical provenance, cause a reread, or
trigger another invocation.

## Schema Sufficiency And Limits

The current durable formats are sufficient for the minimum returned-response and
gateway/Tool-failure connection:

- Model RunRecord payload v2 retains exact work, profile, request, lifecycle, evidence,
  and verification provenance;
- `ResultPayload` retains opaque record reference and verification status;
- message/spool v2 retains typed ModelWork and Result;
- pending-finalization v2 retains Goal, AgentRun, optional reference, and replacement;
- AgentRuntime v5 retains typed work and generic Result; and
- Scheduler queue v4 retains the exact typed envelope and current disposition.

No migration or new durable version is required for this minimum. Additive execution
ports, pure/lazy evidence-run allocation, payload-kind dispatch, and v2 validators are
required before the writer becomes reachable.

The current v2 deliberately does not persist candidate identity, fake token-semantics
identity, actual/predicted token counts, `Admitted`, `Suitable`, `Ready`, exact policy
object identity or cancellation state, invocation result, structured response model or
usage fields, or a pre-call refusal reason. The first implementation is therefore
limited to the one code-closed deterministic fake and digest-based output verification.
Persisting any omitted provenance, or durably terminalizing a pre-call refusal,
requires a separately accepted record/message/runtime compatibility and migration
contract. None may be silently added to v2.

## Required RED-First Implementation Sequence

A later implementation task remains sequential:

1. add failing source/architecture and behavior tests for payload-kind precedence,
   unchanged v1 behavior, the pure/lazy evidence-run identity boundary, and zero-write
   pre-call refusal;
2. add the child-local deterministic-fake composition through exact `Ready` and one
   invoker call, with same-instance interaction tests and no production reachability;
3. add the one-shot result-materialization Tool, response structural checks,
   code-only failure sanitization, evidence ordering, independent verifier, and
   model-specific lifecycle/v2-only publisher;
4. add child Work/Result handling and parent exact v2 validation, point recovery,
   timeout precedence, and spool-prefix tests while keeping the writer unreachable;
5. add payload-kind-aware durable finalizer, worker, Scheduler status/recovery, retry,
   source-drift, and terminal-clear tests; and
6. only after every v2 consumer is installed, enable the internal typed ModelWork
   process branch in an integration fixture without adding a producer, receiver, or
   supported user entry point.

Named evidence must extend or mirror at least:

- `SchedulerModelInvocationPreparerTest`;
- RFC-0020 through RFC-0022 behavior and identity tests;
- `EvidenceRecorderTest` and `DeterministicModelInvokeVerifierTest`;
- `ModelRunRecordTest` and filesystem model-store integration;
- `ProcessIsolatedAgentRunExecutionTest`, isolated handlers/main/launcher tests;
- `DurableAgentRunWorkerTest`, `DurableAgentRunFinalizerTest`, and durable retry tests;
- Scheduler recovery/status reader tests;
- filesystem AgentRun/AgentLoop worker integrations; and
- `ModelCandidateLocalityBoundaryTest` with a narrowly enumerated intentional caller.

Tests must cover all closed outcome mappings, one-gateway-call maximum, no same-AgentRun
Tool retry, every crash prefix, cross-kind/corrupt/foreign/change refusal, exact parent
binding, v1 compatibility, sensitive diagnostic redaction, and no external entry point.
The implementation task must classify RED against this RFC before changing production
code and keep the writer unreachable until all readers are v2-aware.

For this documentation-only task, RFC/decision/architecture/index/ownership/dynamic-
workflow/approved-task/task-justification/planner governance, `git diff --check`, and
the full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Reusing `ModelInvokeTool` is rejected because it reconstructs the request, accepts a
  generic gateway, and can retain raw exception diagnostics.
- Running a generic Agent Loop retry inside one AgentRun is rejected because every
  actual retry must repeat fresh governed preparation under a replacement AgentRun.
- Persisting v1 and wrapping it as v2 is rejected because it violates the closed
  record-kind identity and work-logical-run binding.
- Serializing policy or decision values across the process boundary is rejected
  because it cannot preserve object identity and would turn ephemeral eligibility into
  replay authority.
- Treating gateway success, Tool success, Result publication, or model text as
  verification/completion authority is rejected.
- Fabricating a ToolResult or v2 record for admission, suitability, budget, or
  pre-call invocation refusal is rejected by the accepted zero-activity contract.
- Allocating an empty evidence directory before eligibility is rejected because it is
  an evidence-store write on a refused path.
- Accepting a Result without its Work point before reference checkpointing is rejected
  because the parent cannot prove the complete transport closure.
- Allowing a process-timeout fact to hide a valid complete deterministic v2 record is
  rejected because it would re-invoke after the stronger publication boundary.
- Adding candidate, counts, Ready, response usage, or refusal provenance to current v2
  is rejected as an unapproved compatibility change.

## Exclusions

- Java or test-source implementation; actual model, gateway, Tool, evidence, RunRecord,
  runtime, queue, retry, or recovery execution in this task;
- production caller, supported entry point, typed ModelWork producer, submission,
  receiver, CLI, runtime-event ingress, or external protocol;
- existing read-file execution behavior or RunRecord v1 format/reader weakening;
- provider selection, router, registry, endpoint, remote transmission, network,
  credentials, pricing, spend, general tokenizer, fallback, cache, or streaming;
- MCP, plugin execution, broader runtime behavior, capability-maturity promotion,
  release, deployment, push, or merge.

## Prompt Book

### Prompt: Implement Typed ModelWork Process Execution

Implement RFC-0023 RED-first as a bounded internal deterministic-fake connection. Keep
the writer unreachable until every named v2 consumer and recovery reader is
payload-kind-aware; preserve exact child-local policy/request/candidate identity,
zero-write pre-call refusals, one invocation per AgentRun, code-only failure evidence,
v2-only publication, complete parent binding, and every pre/post-reference crash rule.
Do not add a producer, receiver, supported entry point, provider, network, credential,
spend, or durable schema version without separate authority.
