# RFC-0022: Deterministic Fake Exact-Request Budget And Invocation Seam

Status: Accepted

## Purpose

Define the smallest closed boundary that can validate one exact admitted request
against its own profile budgets and then invoke the exact
`DeterministicFakeModelCandidate` gateway without rereading or reconstructing the
request. This contract is specific to the closed `deterministic-fake-v2` candidate
and consumes RFC-0021's Unicode-scalar semantics and checked response algebra.

This RFC changes no Java, test source, caller, Tool, gateway behavior, durable schema,
artifact, command, runtime path, or capability maturity. No invocation occurs until a
separate RED-first implementation task.

## Relationship To Existing Contracts

RFC-0013 continues to own `ModelRequest`, `ModelResponse`, `ModelUsage`,
`ModelGatewayException`, the exact fake response, and its character bounds.
RFC-0014 through RFC-0016 continue to own the complete execution profile, exact
request/profile composition, and task/policy/capability/locality admission. RFC-0017
through RFC-0019 continue to own fresh caller sourcing, typed ModelWork transport,
exact Scheduler preparation, same-policy identity, Model RunRecord v2 provenance, and
pre-reference retry versus post-reference recovery. RFC-0020 and RFC-0021 continue to
own the exact candidate binding, standalone suitability, fake token unit, checked
response algebra, and fixed candidate capacities.

RFC-0022 owns only:

- one invocation-local actual-request budget decision over an exact `Suitable`;
- retention of one exact `ExecutionPolicy` for the later same-policy call;
- the closed malformed/input/response-length/output/total refusal order;
- one private-construction budget-ready value that is the sole invoker input;
- the exact request and candidate-bound gateway accessor path;
- closed pre-call policy refusal and gateway-failure outcomes;
- the standalone RED-first implementation sequence with no production caller; and
- the connection contract for later Model RunRecord v2 execution integration.

It does not define a general model router, provider invocation API, tokenizer, usage
mapping, ToolResult, evidence writer, result verifier, runtime disposition, retry
controller, persistence format, producer, receiver, or CLI.

## Current Gap

`ModelCandidateSuitabilityDecision.Suitable` proves only that the candidate's fixed
envelope can satisfy the profile's declared maxima. It does not count the exact prompt
or prove that the predicted fake response fits the request's independent character
ceiling and the profile's actual token budget.

The current public `ModelInvokeTool` cannot close that gap. It accepts string Tool
arguments, resolves or rereads a prompt, constructs a new `ModelRequest`, and holds a
constructor-supplied generic gateway. Reusing it after suitability would lose exact
request and candidate-gateway identity. It therefore remains unchanged and is not the
new seam.

The exact RFC-0019 `SchedulerModelInvocationPreparation` already retains the policy
instance used for admission beside the exact profiled request. A future integration
must pass that same object into this boundary; it must not reconstruct an equivalent
policy.

## Exact Preparation Input

One field-free `DeterministicFakeExactRequestPreparation` later receives exactly:

```text
ModelCandidateSuitabilityDecision.Suitable suitable
ExecutionPolicy executionPolicy
```

Null is a programming error. The operation accepts neither a raw `Admitted`, raw
`ModelRequest`, candidate, gateway, prompt, token count, response prediction, nor
budget number as a separate input. The request and candidate are reachable only
through:

```text
exact request =
  suitable.admitted().profiledRequest().request()

exact profile =
  suitable.admitted().profiledRequest().executionProfile()

exact gateway =
  suitable.candidate().gateway()
```

Requiring `Suitable` prevents the normal API from skipping candidate evaluation.
Neither `Admitted` nor `Suitable` is an unforgeable authority token because both
are public value types. A production integration must construct and consume them in
one fresh RFC-0019 -> RFC-0016 -> RFC-0020/0021 -> RFC-0022 call chain and must never
accept a serialized, cached, reconstructed, or caller-supplied decision.

The standalone preparation can retain the exact policy it receives, but it cannot
prove that this object was historically used by RFC-0016 because `Admitted` and
`Suitable` do not retain policy. That proof belongs to later Scheduler composition:
it must pass `SchedulerModelInvocationPreparation.executionPolicy()` by reference and
verify that the RFC-0022 result retains the same instance. Changing the existing
`Admitted` or `Suitable` shapes or introducing a model-to-runtime dependency is
rejected.

## Closed Exact-Request Decision

The later sealed decision has exactly two opaque final variants:

```text
Ready(
  exact Suitable suitable,
  exact ExecutionPolicy executionPolicy,
  long inputTokens,
  long predictedResponseUtf16Length,
  long predictedOutputTokens,
  long predictedTotalTokens
)

Refused(
  exact Suitable suitable,
  exact ExecutionPolicy executionPolicy,
  one DeterministicFakeExactRequestRejectionReason reason
)
```

`Ready` and `Refused` have private constructors and are created only by
`DeterministicFakeExactRequestPreparation`. In particular, a public record
constructor must not let a caller forge `Ready` and bypass budget evaluation. Both
variants expose their exact input object identities through getters and copy neither
object. They are ephemeral, process-local, non-serializable, non-persistent, and grant
no authority by themselves.

The closed rejection order is:

1. scan the exact request prompt once with `DeterministicFakeTokenCounter.count`; a
   malformed surrogate returns `MALFORMED_PROMPT`;
2. if the actual scalar count exceeds `profile.tokenBudget().maxInputTokens()`,
   return `INPUT_TOKEN_BUDGET_EXCEEDED`;
3. derive predicted response UTF-16 length and fake-token count from that same prompt's
   UTF-16 length and the already computed scalar count using the RFC-0021 checked
   helpers;
4. if predicted UTF-16 length exceeds the exact
   `request.maxResponseLength()`, return
   `PREDICTED_RESPONSE_UTF16_LENGTH_BUDGET_EXCEEDED`;
5. if predicted output tokens exceed
   `profile.tokenBudget().maxOutputTokens()`, return
   `PREDICTED_OUTPUT_TOKEN_BUDGET_EXCEEDED`;
6. compute `predictedTotalTokens` exactly once with
   `Math.addExact(inputTokens, predictedOutputTokens)`;
7. if that total exceeds `profile.tokenBudget().maxTotalTokens()`, return
   `PREDICTED_TOTAL_TOKEN_BUDGET_EXCEEDED`; and
8. only then create `Ready` with the exact inputs and all four derived numbers.

No value is clamped, normalized, encoded, replaced, rounded, estimated, inferred from
`ModelUsage`, or copied into a new request. A malformed-input diagnostic may identify
a UTF-16 index but must not reproduce prompt content.

Checked-arithmetic overflow is impossible for the bounded valid `ModelRequest` and
RFC-0021 response formula. An unexpected `ArithmeticException` is therefore a
repository-invariant/programming failure that propagates before gateway activity; it
must not be disguised as an ordinary caller refusal. The checked helper overflow tests
remain the executable evidence for that behavior.

## Defensive Total-Budget Branch

The total-budget refusal is deliberately retained even though it is unreachable in the
current valid type space after the earlier individual checks. RFC-0014 enforces:

```text
maxInputTokens + maxOutputTokens <= maxTotalTokens
```

Therefore:

```text
actualInput <= maxInputTokens
predictedOutput <= maxOutputTokens

implies

actualInput + predictedOutput <= maxTotalTokens
```

The later implementation must not reorder checks, weaken `ModelTokenBudget`, use
reflection or a test-only invalid profile, or manufacture an invalid witness merely to
make `PREDICTED_TOTAL_TOKEN_BUDGET_EXCEEDED` behaviorally reachable. It must keep the
checked sum and comparison as a defensive invariant branch and verify the theorem,
exact-bound combinations, constructor refusal of invalid budgets, and source order.
A future change to the profile invariant may make the stable reason reachable without
changing this decision algebra.

## Same-Request Invocation Seam

One field-free `DeterministicFakeExactRequestInvoker` later exposes exactly:

```text
invoke(DeterministicFakeExactRequestDecision.Ready ready)
  -> DeterministicFakeExactRequestInvocationResult
```

It accepts no separate request, prompt, profile, policy, candidate, gateway, supplier,
registry, provider, route, or count. It derives the request, policy, candidate, and
gateway only from `Ready`. Before gateway activity it performs these deterministic
checks in order:

1. the retained policy still allows exact Tool name `model-invoke`, otherwise
   `EXECUTION_POLICY_TOOL_NOT_ALLOWED`;
2. the exact request timeout still fits strictly inside the retained policy timeout,
   otherwise `GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY`;
3. the retained policy cancellation token does not currently report cancellation,
   otherwise `CANCELLATION_REQUESTED`; and
4. invoke exactly once as
   `ready.suitable().candidate().gateway().invoke(exactRequest)`.

The first two checks are defensive against misuse of public upstream decision types
and a wrong policy supplied to standalone preparation. They do not replace the later
integration requirement to preserve the exact RFC-0019 policy identity. The
cancellation check is a last pre-call observation, not a claim that this low-level
seam implements Tool isolation, timeout interruption, or post-call cancellation.

The sealed invocation result has exactly three opaque final variants:

```text
Succeeded(exact Ready ready, exact ModelResponse response)

Refused(
  exact Ready ready,
  one DeterministicFakeExactRequestInvocationRejectionReason reason
)

GatewayFailed(exact Ready ready, exact ModelFailureCode failureCode)
```

`Succeeded` means only that the exact gateway call returned. The response remains
untrusted and unverified. `GatewayFailed` catches only `ModelGatewayException` and
preserves its closed `ModelFailureCode` one-to-one. It stores no raw exception
message, cause, stack, prompt, response text, path, hash, or synthetic diagnostic.
Unchecked programming failures are not broadly caught or relabeled.

All decision and result variants use non-revealing `toString()` behavior. Automatic
record rendering must not recursively expose a retained `ModelRequest.prompt()`,
`ExecutionPolicy.projectRoot()`, or `ModelResponse.text()`.

## Zero-Activity Refusal

The budget preparation has no gateway, Tool, evidence, RunRecord, filesystem, prompt
reader, runtime, queue, or retry dependency. Every budget refusal therefore produces:

```text
gateway calls = 0
Tool calls = 0
evidence writes = 0
RunRecord writes = 0
runtime/queue transitions = 0
```

The invoker accepts only an evaluator-created `Ready`; a `Refused` value cannot be
passed to it through the declared type. Invocation-policy refusal likewise occurs
before the sole gateway call. A gateway failure is an invocation outcome, not a budget
refusal and not evidence that a Tool or lifecycle transition occurred.

## Tool, Evidence, And Result Ownership

RFC-0022 does not modify or reuse public `ModelInvokeTool`, build a `ToolRequest`,
call `ToolExecutor`, capture evidence, write a Model RunRecord, verify output, or
change runtime state. The standalone invoker is not a production execution path and
does not by itself preserve ToolExecutor isolation or timeout enforcement.

A later ModelWork execution integration must run the exact invocation inside the
existing process-isolated execution and same-policy control envelope or a separately
accepted equivalent. Its typed model Tool adapter owns mapping the closed invocation
outcome to `ToolResult` and evidence. Independent verification and the finalizer own
verified completion and Model RunRecord v2 publication. Gateway success or failure
alone cannot complete or fail the Scheduler queue.

Current Model RunRecord v2 remains unchanged. Candidate identity, token-semantics
identity, counts, `Suitable`, exact-request decisions, and invocation results are not
silently added to its schema. Any provenance addition requires a separate accepted
schema and compatibility contract.

## Retry, Crash, And Recovery Boundary

The preparation and invoker contain no loop, cache, retry, idempotency, persistence, or
recovery logic. One invocation call performs at most one gateway call.

Before an exact AgentRun-bound Model RunRecord v2 reference is durably checkpointed, a
new actual attempt repeats exact task resolution, one prompt snapshot, request/policy
preparation, RFC-0015/RFC-0016, candidate binding, suitability, scalar counting, actual
budget evaluation, and invocation eligibility checks freshly. No prior prompt copy,
count, `Admitted`, `Suitable`, `Ready`, policy, or invocation result crosses an
attempt or process boundary.

A crash after gateway return but before durable record/reference publication does not
have an exactly-once guarantee from this seam. Later integration must reuse the
deterministic AgentRun-bound v2 record identity, point-resolve and validate any complete
record before re-invocation, and preserve the accepted at-least-once boundary where no
valid record exists.

After the exact v2 reference is durable, recovery performs no prompt read or count,
admission, candidate binding, suitability, budget preparation, policy check, Tool call,
gateway call, or second invocation. It resolves and validates the historical record
and resumes deterministic result/runtime/queue finalization only. A RunRecord is
historical evidence, never reusable execution authority.

## Required Implementation Sequence

Future work remains sequential:

1. implement the field-free exact-request preparation, opaque sealed decision, closed
   reasons, single-count checked algebra, defensive total invariant, exact identity,
   redacted rendering, and zero-activity refusal RED-first;
2. implement the field-free invoker and opaque closed results RED-first, using only
   evaluator-created `Ready`, the retained policy, exact request, and candidate-bound
   gateway, with no production caller;
3. separately integrate typed ModelWork process execution, ToolResult/evidence,
   response validation, Model RunRecord v2 writing, finalization, retry, and recovery;
   and
4. separately define and implement any typed ModelWork producer or receiver.

The standalone implementation may invoke the deterministic fake only in focused unit
tests and through the otherwise uncalled invoker definition. It grants no supported
entry point or capability maturity.

## Later RED-First Verification Contract

The exact-request preparation implementation must cover:

- null programming errors and exact `Suitable` plus policy identity retention in
  both decision variants;
- private construction and non-revealing rendering for `Ready` and `Refused`;
- every malformed-surrogate position and no prompt content in diagnostics;
- actual input, predicted response UTF-16 length, predicted output, and defensive total
  checks at equality and one over each reachable boundary;
- deterministic combined first-match precedence;
- supplementary prompts where scalar counts differ from UTF-16 lengths;
- one scalar scan, checked response derivation, checked total addition, and no
  `ModelUsage`, normalization, encoding, replacement, or request reconstruction;
- proof that total-only refusal is unreachable under the current
  `ModelTokenBudget` invariant without weakening the reason or comparison; and
- no gateway, Tool, evidence, RunRecord, filesystem, prompt-reader, runtime, queue,
  retry, or production-caller dependency.

The invoker implementation must cover:

- acceptance of only the private-construction `Ready` type and exact ready/policy/
  suitable/admitted/profiled-request/request/candidate/gateway identity paths;
- defensive policy allowlist, strict timeout, and current cancellation refusal before
  gateway activity;
- one successful exact fake call and retention of the exact returned response;
- one-to-one closed mapping of every `ModelFailureCode` without raw exception text;
- exactly one candidate-bound `.invoke(` source call and no generic gateway input,
  request reconstruction, prompt reread/copy, normalization, encoding, Tool,
  evidence, RunRecord, runtime, retry, provider, network, credential, or spend work;
- non-revealing result rendering; and
- zero production references outside the new definition types and focused tests.

Later runtime integration needs separate RED-first evidence for exact
`SchedulerModelInvocationPreparation.executionPolicy()` identity, one prompt read,
process isolation and timeout/cancellation behavior, ToolResult/evidence ordering,
response verification, Model RunRecord v2 publication, crash points, pre-reference
retry, post-reference no-invocation recovery, finalization, and queue disposition.

For this documentation-only task, RFC/decision/architecture/index/ownership/dynamic-
workflow/approved-task/task-justification/planner governance, `git diff --check`, and
the full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Accepting raw `Admitted`, request, profile, candidate, gateway, or numeric facts is
  rejected because it can bypass suitability or substitute a different invocation.
- A public record or constructor for `Ready` is rejected because it makes successful
  budget evaluation forgeable through the normal API.
- Reusing `ModelInvokeTool` is rejected because it rereads or reconstructs the
  request and uses a constructor-bound generic gateway.
- Adding policy to the stable `Admitted` or `Suitable` shapes, or making model code
  depend on Scheduler preparation, is rejected as an unnecessary contract expansion.
- Claiming the standalone seam can prove the policy's historical admission identity is
  rejected; only later same-call-chain integration can prove that reference equality.
- Reordering total ahead of input/output, weakening `ModelTokenBudget`, or forging an
  invalid profile merely to reach total refusal is rejected because it violates the
  accepted profile and RFC-0021 order.
- Catching all exceptions, persisting raw gateway messages, or translating to
  `ToolFailureCode` inside the seam is rejected because it obscures programming
  failures and leaks responsibility from the later Tool/evidence adapter.
- Treating `Succeeded` as verified output, evidence, completion, or queue disposition
  is rejected because the gateway response remains untrusted.
- Adding retry, cache, persistence, provider routing, registry lookup, or fallback is
  rejected because invocation-local evaluation is not durable authority.

## Exclusions

- Java or test-source implementation; current request/profile/admission/candidate/
  counter/suitability/gateway/Tool behavior change;
- production caller, supported entry point, Scheduler execution, process worker,
  ToolResult/evidence, result validation, Model RunRecord writing, finalizer, runtime
  disposition, retry controller, recovery reader, status, or queue wiring;
- schema, codec, artifact, migration, producer, submission, receiver, CLI, or MCP;
- general/provider tokenizer, usage normalization, provider model, registry, discovery,
  selection, router, endpoint, destination, outbound policy, network or transmission;
- credentials, paid services, pricing, billing, currency conversion, spend, caching,
  fallback, streaming, prompt scanning, redaction, injection resistance, attribution,
  or quality evaluation; and
- capability maturity, release, deployment, push, merge, permission change, external
  effect, or destructive cleanup.

## Prompt Book

### Prompt: Implement The Exact-Request Budget And Invocation Seam

Implement RFC-0022 sequences 1 and 2 RED-first. Keep the exact-request preparation and
invoker standalone, preserve exact Suitable/policy/request/candidate/gateway identities,
and add no production caller, ToolResult/evidence writer, RunRecord writer, runtime
integration, provider, or network path.

### Prompt: Integrate Deterministic Model RunRecord v2 Execution

Propose, but do not implement, the typed ModelWork process-execution, ToolResult/
evidence, response-validation, Model RunRecord v2 finalization, retry, and recovery
connection that consumes the exact RFC-0022 boundary.
