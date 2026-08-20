# RFC-0015: Profiled Model Request

Status: Accepted

## Purpose

Define the smallest provider-neutral composition that attaches one complete RFC-0014
`ModelExecutionProfile` to one complete RFC-0013 `ModelRequest` without changing
either accepted value or the current gateway path. The composition carries aligned
request data and requirements; it does not route, select a provider, evaluate policy,
authorize transmission or spend, or perform a model invocation.

RFC acceptance does not imply capability maturity. Maturity remains owned by
`PROJECT_STATE.md` alone.

## Relationship To RFC-0013 And RFC-0014

RFC-0013 continues to own the five-component `ModelRequest`, `ModelGateway`, response
and usage values, deterministic fake, provider-adapter shape, Tool, CLI, Scheduler
composition, and its timeout and response-character safety stub. This RFC changes none
of them.

RFC-0014 continues to own the complete immutable execution profile and its validation.
Every profile component remains explicit. This RFC supplies no default profile,
factory, nullable escape, inferred classification, or synthesized capability.

## Minimum Composition

`ProfiledModelRequest` is an immutable value under `com.enhancer.model` with exactly
two components, in this order:

1. `request`, one complete `ModelRequest`; and
2. `executionProfile`, one complete `ModelExecutionProfile`.

Both components are required and retained unchanged. Equality and hashing cover both
complete values. The composition duplicates no request or profile field and contains
no route, provider, provider-model name, endpoint, destination, credential, price,
tokenizer, policy decision, response, usage, result, retry, fallback, or persistence
data.

The new value is additive. It does not replace `ModelRequest`, implement
`ModelGateway`, or become an accepted argument to the existing gateway, Tool, CLI, or
Scheduler in this contract.

## Cross-Value Alignment

Construction fails closed unless both of these relationships hold:

```text
request.modelClass == executionProfile.modelClass
executionProfile.maximumInvocationTime <= request.timeout
```

Model-class equality is exact string equality over the already validated
provider-neutral labels. The time relationship preserves RFC-0014's later Tool-path
invariant without changing RFC-0013 timeout semantics:

```text
profile maximumInvocationTime <= gateway timeout < Tool execution-policy timeout
```

No other relationship is invented at this boundary:

- `request.maxResponseLength` remains a character-count safety ceiling, while the
  profile token budget remains a token requirement and ceiling. Neither is converted,
  compared, or treated as evidence for the other without a later tokenizer and usage
  contract.
- `executionProfile.requiredCapability` remains distinct from model class. It is not
  compared with `request.modelClass`, and the current Scheduler's temporary use of a
  WorkItem required-capability label as a model class is not encoded or legitimized.
- The composition does not inspect the prompt, infer or lower data classification,
  compare currencies, calculate price, or infer locality, reasoning, context, or cost
  from request content.

## Compatibility Choice

Adding the profile as a sixth `ModelRequest` record component is rejected for this
minimum contract. It would change the canonical constructor, record shape, equality,
and existing callers. Preserving the five-argument constructor would then require a
null, implicit, inferred, or synthetic profile, all forbidden by RFC-0014. Migrating
the current Tool, CLI, and Scheduler would also require profile sources they do not yet
possess and policy decisions this RFC does not authorize.

Changing `ModelGateway.invoke` or adding a profiled overload is also rejected here. It
would integrate the runtime port and adapters before an accepted caller-profile source
and policy consumer exist. A factory or validation helper that retains neither complete
value is not a composition, while an untyped pair or map weakens the contract.

The separate two-component value therefore preserves RFC-0013 source and behavior
compatibility while making the complete future request/profile pair explicit and
testable.

## Authority And Policy Intersection

`ProfiledModelRequest` is untrusted data, not an authorization decision. Successful
construction proves only the two intrinsic alignment rules above. It cannot add a
Tool, widen task scope, select a route, provider, model, endpoint, destination, or
credential, open a network connection, approve remote transmission or paid use, or
permit any external effect.

A later runtime consumer must evaluate the intersection of:

1. the accepted task contract;
2. the active `ExecutionPolicy`;
3. the complete `executionProfile`; and
4. a later accepted outbound/provider policy.

Every consumer may narrow that intersection or reject the request. None may widen it.
`LOCAL_ONLY` must remain local. `POLICY_CONSTRAINED` only permits a later policy
evaluation and is not remote authority. A positive cost ceiling is not spend approval,
and caller-supplied data classification is not permission to disclose the prompt.

## First Implementation Increment

The first separately authorized implementation adds only the pure
`ProfiledModelRequest` record and focused tests under `com.enhancer.model`. RED begins
with the missing composition symbol. GREEN covers:

- exact retention, equality, and hashing of the two complete values;
- exact ordered record components `request` and `executionProfile`, with no additional
  field or implemented gateway, provider, Tool, or policy interface;
- null rejection for both components;
- rejection of unequal model classes;
- acceptance when profile invocation time is less than or equal to request timeout and
  rejection when it is greater; and
- deliberate acceptance of unrelated response-character and token magnitudes in both
  directions.

Existing RFC-0013 and RFC-0014 model tests and architecture governance remain
regression coverage. The implementation changes no existing source file, runtime path,
schema, or network behavior and can establish Contract Verified maturity at most.

## Later Integration Consumers

Runtime integration requires another accepted contract after a complete profile source
exists for every participating caller. That contract must name how the governed Tool
request-construction boundary receives all profile fields, where the task and
`ExecutionPolicy` intersection is enforced, whether the model gateway port evolves,
and how rejection is represented before any adapter or external effect.

Provider routing, outbound policy enforcement, tokenizer and normalized usage,
pricing, credentials, and remote transmission remain independent later consumers and
each requires its own authority and verification.

## Exclusions

- changes to `ModelRequest`, `ModelGateway`, `ModelInvokeTool`, Scheduler, CLI,
  deterministic fake, provider adapter, command schema, persistence, or runtime;
- profile defaults, factories that synthesize missing requirements, caller migration,
  or parsing and serialization;
- task, execution, outbound, locality, provider, destination, credential, or spend
  policy evaluation;
- routing, provider selection, fallback, retry, caching, queues, or streaming;
- network calls, remote execution or transmission, paid-service use, or credentials;
- tokenizers, pricing feeds, currency conversion, or provider usage normalization;
- MCP, plugin protocols, redaction, prompt-injection resistance, attribution, quality
  evaluation, durable-schema migration, or capability-maturity claims.

## Prompt Book

### Prompt: Implement The Pure Composition Value

Implement `ProfiledModelRequest` and its missing-symbol RED-first tests exactly as
scoped, without modifying existing request, gateway, Tool, CLI, Scheduler, adapter, or
persistence files.

### Prompt: Review Compatibility And Authority

Verify that the record retains exactly the two complete values, rejects only the two
defined alignment failures, manufactures no profile, preserves RFC-0013 callers, and
grants no task, Tool, policy, provider, network, credential, transmission, or spend
authority.

### Prompt: Prepare Runtime Integration

Propose, but do not implement, the smallest caller-profile sourcing and policy-
intersection contract that could pass a validated `ProfiledModelRequest` toward a
gateway without defaults, routing, providers, or remote transmission.
