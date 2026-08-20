# RFC-0016: Model Invocation Admission

Status: Accepted

## Purpose

Define the smallest provider-neutral, deterministic authority-intersection boundary
for one complete `ProfiledModelRequest` before any model gateway execution. Admission
checks the approved task, active execution policy, authoritative required capability,
profile locality, and timeout relationship. It does not construct a profile, evaluate
model suitability, select a route or provider, authorize outbound transmission, or
invoke a gateway.

RFC acceptance does not imply capability maturity. Maturity remains owned by
`PROJECT_STATE.md` alone.

## Relationship To Existing Gate 9 Contracts

RFC-0013 continues to own `ModelRequest`, `ModelGateway`, the deterministic fake,
provider-adapter shape, Tool, CLI, Scheduler composition, and response-character safety
stub. RFC-0014 continues to own the complete model execution requirements. RFC-0015
continues to own the exact aligned request/profile pair. This RFC changes none of
those contracts or their current callers.

Admission receives one already valid `ProfiledModelRequest`. It never creates,
completes, looks up, infers, defaults, repairs, or falls back from a missing profile.

## Minimum Pure Contract

`ModelInvocationAdmission` is a stateless final class under `com.enhancer.model` with
no fields and one operation:

```java
ModelInvocationAdmissionDecision evaluate(
        ProfiledModelRequest profiledRequest,
        ApprovedTask approvedTask,
        ExecutionPolicy executionPolicy,
        String authoritativeRequiredCapability)
```

Every argument is required. Null is a caller programming error and fails immediately;
it is not converted into a policy rejection. Evaluation stores nothing, reads no
registry or ambient state, and calls no gateway, provider, filesystem, network, Tool,
router, credential supplier, persistence adapter, or paid service.

Passing the exact `ApprovedTask` and `ExecutionPolicy` preserves their authority
sources and existing validation. Boolean or raw-set projections are rejected because
they detach the decision from the real task and policy values. A separate input/context
record is unnecessary because it would add a storable aggregate without a new
invariant.

## Explicit Sources

The caller supplies the complete `ProfiledModelRequest` as data. Neither the profile
nor any Tool argument is an authority source.

`approvedTask` must be the exact active approved task governing the invocation.
Admission evaluates `approvedTask.allows(ModelInvokeTool.NAME)`; task scope and
execution-policy scope are conjunctive, never a union.

`executionPolicy` must be the exact active policy that the same Tool invocation would
use. Admission evaluates `executionPolicy.allows(ModelInvokeTool.NAME)` and compares
the exact `Duration` values without millisecond conversion. The existing
`ToolExecutor` remains authoritative for mutable cancellation checks, isolation,
worker timeout, and result validation immediately around Tool execution. The existing
`ModelInvokeTool` remains authoritative for prompt-path containment and read bounds.

`authoritativeRequiredCapability` is a separate unchanged projection from the
governed caller. It cannot be copied from `ModelExecutionProfile.requiredCapability`,
inferred from `ModelRequest.modelClass`, supplied as an arbitrary direct-CLI argument,
or synthesized by admission. In a later Scheduler integration, its only currently
available source is the active `WorkItem.requiredCapability`. The admission API does
not accept `WorkItem` itself, preserving the `runtime -> model` dependency direction.

The current direct CLI has neither a complete profile nor an authoritative capability
source. The current Scheduler has the capability source but no complete profile
source. Both therefore remain unsupported admission callers until separate accepted
input and integration contracts provide every required value explicitly.

## Deterministic Decision

`ModelInvocationAdmissionDecision` is a sealed interface with exactly two permitted
nested record implementations:

- `Admitted(ProfiledModelRequest profiledRequest)`, retaining the exact input instance;
- `Rejected(ModelInvocationRejectionReason reason)`, retaining exactly one closed
  reason.

`ModelInvocationRejectionReason` contains exactly these constants in repository order:

1. `TASK_TOOL_NOT_ALLOWED`;
2. `EXECUTION_POLICY_TOOL_NOT_ALLOWED`;
3. `REQUIRED_CAPABILITY_MISMATCH`;
4. `GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY`; and
5. `OUTBOUND_POLICY_REQUIRED`.

Evaluation returns the first matching rejection in that order:

```text
if task does not allow model-invoke
    TASK_TOOL_NOT_ALLOWED
else if execution policy does not allow model-invoke
    EXECUTION_POLICY_TOOL_NOT_ALLOWED
else if authoritative required capability
        != profile required capability
    REQUIRED_CAPABILITY_MISMATCH
else if request timeout >= execution-policy timeout
    GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY
else if profile locality == POLICY_CONSTRAINED
    OUTBOUND_POLICY_REQUIRED
else
    Admitted(exact profiled request)
```

The already constructed `ProfiledModelRequest` supplies the first time relationship;
admission supplies the strict second relationship:

```text
profile maximumInvocationTime <= request timeout < execution-policy timeout
```

Multiple violations always yield the same first reason. Policy allowlist omission and
explicit denial both map to `EXECUTION_POLICY_TOOL_NOT_ALLOWED` because
`ExecutionPolicy.allows` owns that intersection.

## Meaning Of Admitted

`Admitted` means only that all authority conditions evaluable by this pure boundary
passed and the complete request/profile pair may proceed unchanged to a later local
candidate-suitability boundary. It is not a capability, token, authorization receipt,
route, provider selection, proof that a gateway is local, proof that a model satisfies
the profile, or permission to invoke `ModelGateway`.

The decision is ephemeral. It must not be persisted, cached, replayed, transmitted, or
treated as a reusable authority token. It grants no Tool, task, network, credential,
transmission, spend, or external-effect authority.

Reasoning, context, token, cost, and data-classification requirements remain retained
and unevaluated. No provider-neutral candidate-capability or tokenizer/pricing contract
exists yet, so admission cannot claim they are satisfiable. A later consumer may narrow
or reject them but may not discard or weaken them.

## Locality And Absent Outbound Authority

There is no accepted outbound/provider policy in this slice. The current outbound
intersection therefore has one fail-closed result:

- `LOCAL_ONLY` may pass this pure authority evaluation, but still requires a later
  runtime composition to prove that the actual gateway path is local;
- `POLICY_CONSTRAINED` always returns `OUTBOUND_POLICY_REQUIRED`.

Public classification, a zero or positive cost ceiling, absent credentials, or a
local-looking model-class label cannot bypass that rejection. `POLICY_CONSTRAINED` is
not remote permission. A positive cost ceiling is not spend approval, zero cost does
not prove a route is free, and caller-supplied classification is not permission to
disclose prompt content.

A future outbound/provider policy requires its own accepted contract. It may add
destination and provider checks and further narrow admission, but it may never
reinterpret an RFC-0016 decision as remote authority.

## Capability And Budget Separation

The later Scheduler projection must preserve two different relationships:

```text
WorkItem.requiredCapability
    == ModelExecutionProfile.requiredCapability

ModelRequest.modelClass
    == ModelExecutionProfile.modelClass
```

The current Scheduler's temporary use of `WorkItem.requiredCapability` as
`ModelRequest.modelClass` is not accepted by this RFC and cannot source either side of
the other relationship.

`ModelRequest.maxResponseLength` remains a character ceiling. Profile token budgets
remain token values. Admission performs no comparison or conversion between them and
does not reuse the deterministic fake's character-based `ModelUsage` as token evidence.

## Compatibility And Rejected Alternatives

- Changing `ModelRequest` or `ModelGateway.invoke` is rejected because RFC-0013 source
  and behavior compatibility does not require it.
- Accepting `WorkItem` directly is rejected because it reverses the model/runtime
  dependency and unnecessarily couples all callers to the Scheduler.
- Accepting task/policy booleans or raw sets is rejected because it weakens binding to
  the exact authority objects.
- An input context record is rejected because it adds storage and identity surface
  without a new invariant.
- An optional profiled request result is rejected because it loses the typed reason;
  exception-only denials are rejected because ordinary policy refusal is not an
  exceptional program failure.
- A gateway wrapper, admitting gateway, gateway instance parameter, provider registry,
  profile registry, default outbound policy, ambient context, ThreadLocal source, or
  fallback profile is rejected as premature integration or hidden authority.
- Reasoning, context, token, cost, classification, pricing, tokenizer, provider
  suitability, or route evaluation is rejected because no accepted contract can prove
  those claims yet.

## First Implementation Increment

The first separately authorized implementation adds only the pure admission evaluator,
the sealed decision, the rejection enum, and focused tests under `com.enhancer.model`.
It changes no existing production source or runtime wiring. RED begins with the missing
admission symbols. GREEN covers:

- exact admitted-instance retention and exact decision/reason shapes;
- null input rejection;
- task denial, policy allowlist omission, and explicit policy denial;
- authoritative capability mismatch;
- request timeout equal to or greater than policy timeout rejection and strict-less
  acceptance, including profile time equal to request timeout;
- `POLICY_CONSTRAINED` rejection and `LOCAL_ONLY` admission;
- deterministic first-match precedence under multiple violations;
- deliberate response-character/token independence;
- restricted classification and positive cost ceilings creating no authority or
  unrelated rejection; and
- reflection guards proving the evaluator stores no gateway, provider, runtime,
  persistence, route, destination, credential, response, usage, or result field and
  implements no execution port.

No counting gateway is needed because the evaluator API accepts no gateway. Existing
RFC-0013 through RFC-0015 model tests and architecture governance remain regression
coverage. This pure increment can establish Contract Verified maturity at most.

## Later Consumers

The immediate implementation consumer is `ModelInvocationAdmissionTest` over the pure
contract only.

A separately authorized caller-source contract must first provide every complete
profile field and authoritative capability without defaults. A separate local
candidate-suitability contract must then prove the retained reasoning, context, token,
cost, classification, and locality requirements against a known-local execution
candidate. Only after both exist may a runtime integration place admission and
suitability immediately before the single `ModelInvokeTool` gateway invocation seam
and pass the admitted value's existing `ModelRequest` to that proven-local gateway.

That integration must preserve the exact active task and `ExecutionPolicy`, perform
fresh admission synchronously, retain the existing `ToolExecutor` checks, and represent
every rejection before any gateway, adapter, network, credential, paid, persistence,
or external effect. The current CLI and Scheduler remain unchanged until then.

## Exclusions

- Java implementation or changes to current request, profile, gateway, Tool, CLI,
  Scheduler, fake, provider adapter, command schema, durable schema, or runtime;
- complete-profile input schema, parser, registry, defaults, inference, fallback, or
  caller migration;
- runtime composition, cached or persisted decisions, admission receipts, tokens, or
  replay;
- route, provider, provider model, gateway, endpoint, destination, credential, network,
  remote transmission, paid-service, pricing, tokenizer, or usage-normalization logic;
- model suitability, capability registry, reasoning/context/token evaluation, fallback,
  retry, caching, queues, or streaming;
- prompt scanning, classification inference, redaction, prompt-injection resistance,
  attribution, quality evaluation, MCP, plugin protocol, persistence, or migration;
- capability-maturity, operational-readiness, release, or deployment claims.

## Prompt Book

### Prompt: Implement The Pure Admission Contract

Implement the stateless evaluator, sealed decision, closed rejection reasons, and
missing-symbol RED-first tests exactly as scoped, without changing any existing
production source or runtime path.

### Prompt: Review Authority Non-Expansion

Verify the exact task/policy/capability/time/locality predicates and first-match order,
and confirm that `Admitted` is neither a gateway invocation permission nor route,
provider, network, credential, transmission, or spend authority.

### Prompt: Prepare Complete Profile Sourcing

Propose, but do not implement, the smallest explicit caller input contract that can
supply every RFC-0014 profile field and the independent authoritative capability
without defaults, parsing ambiguity, Scheduler conflation, routing, or remote
transmission.
