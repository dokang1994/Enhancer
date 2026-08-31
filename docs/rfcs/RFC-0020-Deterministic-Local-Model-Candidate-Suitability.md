# RFC-0020: Deterministic Local Model Candidate Suitability

Status: Accepted

## Purpose

Define the minimum invocation-local candidate and suitability boundary immediately
after RFC-0016 admission. The first candidate binds one exact
`DeterministicFakeModelGateway` instance to closed repository-owned local provenance,
then evaluates every profile requirement that current evidence can truthfully support.

This RFC deliberately exposes the remaining token-semantics gap as a typed rejection.
It authorizes no Tool or gateway invocation and changes no Java, durable schema,
artifact, command, caller, or capability maturity.

## Relationship To Existing Contracts

RFC-0013 continues to own `ModelGateway`, `ModelRequest`, `ModelResponse`,
`ModelUsage`, the deterministic fake, and `ModelInvokeTool`. RFC-0014 continues to own
the complete profile and its provider-neutral token requirements. RFC-0015 continues to
bind the exact request and profile. RFC-0016 continues to own task, policy, capability,
timeout, and outbound-locality admission. RFC-0019 continues to own exact Scheduler
preparation, Model RunRecord v2 provenance, and pre-record versus post-record recovery.

RFC-0020 owns only:

- the first closed, explicit, invocation-local proven-local candidate binding;
- deterministic comparison of the exact admitted profile with candidate facts;
- exact admitted/candidate/gateway object retention in an ephemeral result;
- typed refusal where current evidence cannot prove a retained requirement; and
- the boundary that remains before any same-request Tool/gateway invocation seam.

It does not select among candidates, define a registry, route to a provider, normalize
tokens or usage, authorize spend or transmission, or invoke a gateway.

## Current Gap

`SchedulerModelInvocationPreparation` can retain an exact RFC-0016 `Admitted` value,
the same prepared request/profile, and the exact execution policy. `Admitted` means
only local eligibility. It does not prove that an actual gateway instance is local or
that a model candidate satisfies reasoning, context, token, cost, and classification
requirements.

The deterministic fake currently has strong local implementation evidence: it is a
final repository-owned class, uses no credential, filesystem, process, adapter, or
network boundary, and computes a bounded response from one request. Those facts do not
come from caller input. In contrast, its `ModelUsage` units and request bounds are
character-based test accounting, not RFC-0014 token semantics. No tokenizer or proven
context/input/output/total-token capacity exists. Inventing numeric token capacity from
a fixture, model label, prompt length, or character ceiling would be false evidence.

## Closed Proven-Local Candidate

The first implementation introduces one opaque final
`DeterministicFakeModelCandidate`. It is not a record and has no public constructor
accepting candidate metadata. One repository-owned factory operation accepts only an
exact `DeterministicFakeModelGateway`, retains that same object instance, and supplies
all other facts internally.

Conceptually:

```text
DeterministicFakeModelCandidate
    exact DeterministicFakeModelGateway gateway
    candidate identity = deterministic-fake-v1
    model class = deterministic-fake
    capability = deterministic-echo
    maximum reasoning = MINIMAL
    locality provenance = CLOSED_IN_PROCESS_FAKE
    token semantics = UNAVAILABLE
    provider charge = NONE
    maximum model-visible classification = PUBLIC
```

These are conservative repository contracts for a deterministic echo fixture, not
provider claims. The candidate does not accept or retain a generic `ModelGateway`,
gateway supplier, class name, `isLocal` flag, locality enum supplied by a caller,
provider/model name, endpoint, destination, credential, price, tokenizer, route, or
profile-derived fact.

The local proof is structural and closed:

- the binding factory parameter is the exact final fake type, not the gateway port;
- the candidate retains and later returns that same gateway object by identity;
- no caller can replace or widen its fixed facts;
- architecture tests constrain the fake and candidate away from provider-adapter,
  credential, filesystem, process, and network dependencies; and
- adding another candidate kind or weakening this closed set requires a new accepted
  contract and fresh evidence.

The candidate is process-local. It is never serialized, cached, persisted,
reconstructed from a class name, sent through parent/child IPC, or treated as durable
proof. A child process that may later invoke a model must bind its own exact local
gateway instance after fresh RFC-0019 preparation.

## Suitability Algebra

One field-free `ModelCandidateSuitability` receives exactly:

```text
ModelInvocationAdmissionDecision.Admitted admitted
DeterministicFakeModelCandidate candidate
```

Both values are required. Null is a programming error, not a policy rejection. The
API accepts the exact admitted subtype rather than a Boolean or an arbitrary profiled
request. It performs no lookup, selection, I/O, persistence, Tool call, or gateway
call.

The sealed result has exactly two shapes:

```text
Suitable(exact Admitted admitted, exact candidate)
Rejected(one closed reason)
```

`Suitable` retains both input instances without copying or reconstruction. Through the
admitted value it retains the exact prepared `ProfiledModelRequest`; through the
candidate it retains the exact bound gateway. It remains ephemeral eligibility only.

## Deterministic First-Match Evaluation

The initial evaluator checks in this order:

1. profile model class must equal `deterministic-fake`, otherwise
   `MODEL_CLASS_UNSUPPORTED`;
2. profile required capability must equal `deterministic-echo`, otherwise
   `REQUIRED_CAPABILITY_UNSUPPORTED`;
3. profile reasoning requirement must be `MINIMAL`, otherwise
   `REASONING_REQUIREMENT_UNSUPPORTED`;
4. candidate token semantics and capacity evidence must be available, otherwise
   `TOKEN_SEMANTICS_UNAVAILABLE`;
5. minimum context tokens must fit proven context capacity, otherwise
   `CONTEXT_CAPACITY_INSUFFICIENT`;
6. maximum input, output, and total tokens must each fit their corresponding proven
   capacities, otherwise the first corresponding
   `INPUT_TOKEN_CAPACITY_INSUFFICIENT`, `OUTPUT_TOKEN_CAPACITY_INSUFFICIENT`, or
   `TOTAL_TOKEN_CAPACITY_INSUFFICIENT` reason;
7. the profile must retain a zero-microunit free-only ceiling, otherwise
   `FREE_ONLY_COST_REQUIRED`;
8. the profile classification must be no higher than the candidate maximum, otherwise
   `DATA_CLASSIFICATION_UNSUPPORTED`; and
9. only then return `Suitable` with the exact inputs.

The current deterministic candidate has `token semantics = UNAVAILABLE`. Therefore the
initial implementation always stops at step 4 after any earlier mismatch. Steps 5
through 9 define the ordered continuation but remain unreachable until a separate
accepted deterministic token-semantics and capacity contract supplies real evidence.
RFC-0020 does not assign placeholder numbers or turn character counts into tokens.

The candidate's no-provider-charge fact and the profile's zero-microunit requirement
avoid pricing and currency conversion. A positive ceiling remains neither spend
authority nor evidence of a paid route, but this first boundary narrows to explicitly
free-only work. `PUBLIC` is the only accepted classification because local execution
alone does not prove clearance for internal, confidential, or restricted content.

## Meaning Of Suitable And Rejected

Neither result creates authority. In particular, `Suitable` is not permission to call
`ModelGateway`, register or execute a Tool, select a provider or route, disclose data,
read credentials, spend funds, persist evidence, or publish a Model RunRecord. It is
never serialized, cached, replayed, transmitted, or used as an authorization token.

`Rejected` is ordinary fail-closed eligibility data. It creates no synthetic
`ToolResult`, evidence, RunRecord, runtime disposition, retry decision, or queue
mutation. The later runtime-integration contract must define those lifecycle mappings
without claiming that an invocation occurred.

## Exact Future Invocation Ordering

A future invocation attempt must preserve this order and identity:

1. perform fresh RFC-0019 exact task, policy, prompt, request, profile, and RFC-0016
   preparation;
2. require the exact `Admitted` value from that preparation;
3. bind one exact local fake gateway candidate explicitly in the executing process;
4. evaluate suitability and retain the exact admitted and candidate instances;
5. only after token semantics and every later predicate can pass, present
   `profiledRequest.request()`, the same RFC-0019 `ExecutionPolicy`, and the candidate's
   exact gateway object to one separately accepted invocation seam; and
6. preserve existing Tool isolation, cancellation, timeout, evidence, independent
   verification, and Model RunRecord v2 finalization rules.

No prompt file is read twice, no request is reconstructed from string Tool arguments,
and no candidate or gateway is substituted after suitability.

The current public `ModelInvokeTool` cannot satisfy that identity contract. It accepts
a `ToolRequest`, privately rereads or resolves the prompt, parses string arguments,
constructs a new `ModelRequest`, and holds a constructor-bound generic gateway without
the suitability result. It remains unchanged. A later RED-first contract must define a
single same-request/same-policy/same-gateway execution seam before typed ModelWork
guards can be removed.

## Retry, Process, And Recovery Boundary

Before a Model RunRecord v2 reference is durably checkpointed, every actual retry must
repeat exact task resolution, one prompt snapshot, request/policy preparation,
RFC-0015, RFC-0016, local candidate binding, and suitability freshly. No earlier
admitted or suitable value crosses an attempt or process boundary.

After the exact v2 reference is durable, recovery performs no task lookup, admission,
candidate binding, suitability, Tool call, or gateway call. It point-resolves and
validates the retained historical provenance, then resumes only deterministic result,
runtime, and queue finalization. RFC-0019's v2 shape remains unchanged; the sole closed
candidate is not added as a persisted provider or route field.

Any future second candidate, provider, route, or ambiguous model-class-to-candidate
mapping must revisit standalone provenance before invocation. The single closed fake
does not establish a general candidate catalog.

## Required Implementation Sequence

Future work remains sequential:

1. implement the opaque exact-fake binding, field-free evaluator, sealed result, closed
   reasons, identity/non-persistence guards, and typed `TOKEN_SEMANTICS_UNAVAILABLE`
   stop without changing callers;
2. separately define and implement truthful deterministic token semantics and proven
   context/input/output/total capacity before making `Suitable` reachable;
3. only then define and implement the same-request/same-policy/same-gateway Tool and
   Model RunRecord v2 execution/finalization/recovery path; and
4. separately define any typed ModelWork producer or receiver.

No step may infer token capacity from character bounds or skip the unavailable result
because the fake is deterministic and local.

## Verification Contract

The first RED-first implementation must cover:

- exact opaque candidate shape and retention of the exact final fake gateway instance;
- no public generic gateway, metadata, locality flag, provider, route, credential,
  tokenizer, pricing, or registry input;
- fixed candidate identity, model class, capability, reasoning, token-unavailable,
  no-provider-charge, and public-classification facts;
- exact admitted and candidate identity retention in the sealed result shape;
- null programming errors and deterministic first-match model-class, capability,
  reasoning, then token-semantics rejection;
- no reachable `Suitable` result while token semantics are unavailable;
- no invocation, Tool, evidence, RunRecord, filesystem, process, adapter, credential,
  or network activity;
- unchanged `ModelInvokeTool`, Scheduler preparation, typed ModelWork execution guards,
  durable schemas, and production callers; and
- package/reflection/source-dependency guards proving the local candidate cannot wrap
  an arbitrary gateway or acquire forbidden dependencies.

A later token-capacity increment must establish its own RED/GREEN evidence for every
remaining predicate before any suitable path exists. A later invocation integration
must prove the exact prepared request, exact policy object, exact candidate gateway,
single prompt snapshot, pre-record retry, and post-record recovery behavior.

For this documentation-only task, RFC/decision/architecture/index/ownership/dynamic-
workflow/approved-task/task-justification/planner governance, `git diff --check`, and
the full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- A caller-constructible candidate record, generic `ModelGateway` wrapper, `isLocal`
  flag, locality enum, class-name check, annotation, absent credential, model label,
  public classification, zero-looking cost, or deterministic response is rejected as
  forgeable or insufficient local proof.
- A registry, service loader, environment lookup, default candidate, provider catalog,
  router, or fallback is rejected as hidden selection and authority.
- Treating `ModelUsage`, prompt characters, response characters, or fixture numbers as
  tokens is rejected because no accepted tokenizer or usage-normalization contract
  exists.
- Assigning synthetic context or token capacities merely to make the suitable branch
  reachable is rejected as unsupported evidence.
- Copying the request, rebuilding it from `ToolRequest`, rereading `prompt-path`, or
  looking up another gateway after suitability is rejected because it changes the
  evaluated invocation.
- Persisting admitted/suitable decisions or candidate objects is rejected because
  process-local eligibility is not durable execution authority.

## Exclusions

- Java implementation, current API or schema changes, candidate selection/registry,
  router, provider adapter, provider model, endpoint, destination, outbound policy,
  network or remote transmission;
- tokenizer, token counting, usage normalization, numeric token/context capacity,
  pricing, currency conversion, credentials, paid service, or spend authority;
- Tool/gateway invocation, prompt reread, evidence, verification, Model RunRecord
  writing, lifecycle disposition, finalization, result validation, runtime/process/
  worker/recovery/status/caller wiring;
- submission, receive, CLI, durable migration, MCP, caching, fallback, streaming,
  redaction, classification inference, prompt scanning, injection resistance,
  attribution, quality evaluation, capability maturity, release, deployment, push, or
  merge.

## Prompt Book

### Prompt: Implement The Fail-Closed Local Candidate Boundary

Implement the opaque exact deterministic-fake candidate binding and stateless
suitability evaluator RED-first. Preserve exact input identities, stop truthfully at
`TOKEN_SEMANTICS_UNAVAILABLE`, and change no caller, Tool, gateway behavior, schema, or
runtime path.

### Prompt: Specify Deterministic Token Semantics

Propose, but do not implement, the smallest deterministic token-counting and capacity
contract that can make the later suitability predicates truthful without reusing
character-based `ModelUsage` or introducing provider, network, credential, or spend
authority.
