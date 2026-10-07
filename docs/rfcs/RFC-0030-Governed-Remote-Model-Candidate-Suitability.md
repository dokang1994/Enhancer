# RFC-0030: Governed Remote Model Candidate Suitability

Status: Accepted

## Purpose

Define the provider-neutral evidence and pure evaluation boundary that can prove one
exact remote candidate suitable for one exact profiled request already carrying exact
RFC-0028 outbound eligibility. The future evaluator returns either the existing opaque
RFC-0029 `RemoteModelCandidateSuitability` or one closed refusal reason.

This RFC specifies a later pure implementation. It does not acquire production facts,
construct a provider or adapter, resolve a model name or endpoint, access a credential,
authorize spend, transmit content, invoke a model, persist a proof, execute retry or
fallback, or change capability maturity.

## Relationship To Existing Contracts

RFC-0014 and RFC-0015 continue to own the complete untrusted invocation requirements
and exact profiled request. RFC-0016 continues to own fresh task, policy, capability,
timeout, and local-versus-outbound admission. RFC-0028 remains the sole outbound policy
authority and owns the exact `Eligible`, proposal, candidate, destination, content,
inspection, redaction, classification, and pricing facts. RFC-0029 remains the sole
Router and consumes only an already-created suitability proof.

RFC-0030 does not recreate any of those authorities. It answers only whether separate
governed capability evidence proves that the exact candidate in one exact `Eligible`
can satisfy every relevant RFC-0014 requirement. Its successful output is the existing
RFC-0029 proof and grants no authority beyond that proof's current meaning.

RFC-0020 and RFC-0021 remain the closed local deterministic-fake suitability and token
semantics. `ModelCandidateSuitabilityDecision.Suitable` is structurally local and must
never enter RFC-0030. The deterministic fake continues to bypass RFC-0028, RFC-0030,
and RFC-0029 unchanged.

## Governed Capability Facts

`RemoteModelCandidateCapabilityFacts` is one opaque immutable value produced only by a
separately governed source. It retains:

- the fixed capability-facts schema version;
- a bounded fact-source identity and revision;
- an immutable capacity-evidence identity;
- an observation time and exclusive expiry time;
- bounded candidate and destination identities;
- the exact model class and required capability labels evidenced by the source;
- the token-semantics identity;
- the exact price-schedule identity and exact `ModelTokenBudget` cost basis;
- maximum supported reasoning requirement and context-token capacity;
- one valid `ModelTokenBudget` containing maximum supported input, output, and total
  token capacities;
- the maximum supported caller-requested invocation timeout;
- the cost currency and a conservative maximum charge in microunits for the exact
  retained price schedule and token-budget basis; and
- the maximum model-visible data classification the candidate is approved to process.

The value uses the existing repository bounds and intrinsic validation. Its token
budget therefore preserves positive bounded components and checked
`input + output <= total` construction. Its time window preserves `observedAt <
expiresAt`, and the timeout is positive, bounded, and millisecond-precise.

The maximum invocation time means the largest caller-requested timeout the candidate
accepts. It is not observed latency, a latency objective, an uptime assertion, or a
promise that execution finishes within that duration. A different latency or quality
contract requires separate evidence and policy.

The facts contain no provider model name, endpoint, protocol, credential, secret,
adapter, callback, registry handle, mutable availability Boolean, load, score, quality
claim, prior outcome, spend grant, or network authority. Candidate and destination
identities bind evidence; they do not reconstruct the exact RFC-0028 fact objects.

Construction is package-private. The first implementation may provide test-owned
fixtures but no production creator. A later production source must be explicitly
accepted and must establish every field from independently governed observations; it
cannot copy caller assertions, request requirements, eligibility fields, provider
labels, ambient configuration, or an earlier suitability proof and call them facts.

## Exact Pure Evaluator

The future final, field-free `RemoteModelCandidateSuitabilityEvaluator` receives
exactly:

```text
ProfiledModelRequest profiledRequest
ModelOutboundRouteAdmissionDecision.Eligible eligibility
RemoteModelCandidateCapabilityFacts capabilityFacts
Instant evaluationTime
```

Every input is required. Null is a programming error, not a suitability refusal. There
is no separate candidate input: the evaluator derives the exact candidate-facts
instance only from `eligibility.proposal().candidateFacts()`. It performs no clock
read, lookup, discovery, registry access, fact acquisition, provider or gateway call,
filesystem or environment access, process or network activity, persistence, Tool call,
currency conversion, token conversion, or external effect.

The evaluator neither creates nor independently authenticates RFC-0028 eligibility.
It preserves the exact `Eligible` instance, checks every eligibility field it relies
on, and leaves RFC-0029's complete eligibility revalidation unchanged. A forged,
expired, or cross-bound public `Eligible` therefore cannot produce a usable selected
route merely by passing RFC-0030.

## Closed Decision And Refusal Order

`RemoteModelCandidateSuitabilityDecision` is a sealed decision with exactly two
shapes:

```text
Suitable(RemoteModelCandidateSuitability suitability)
Refused(RemoteModelCandidateSuitabilityRefusalReason reason)
```

`Refused` retains only one reason. It contains no request, prompt, outbound content,
candidate or destination text, governed facts, price, policy, exception, source error,
path, endpoint, or credential.

After programming-error checks, evaluation returns the first matching reason in this
repository order:

1. `SOURCE_CONTENT_BINDING_MISMATCH`;
2. `CANDIDATE_FACTS_INVALID_OR_STALE`;
3. `CAPABILITY_FACTS_INVALID_OR_STALE`;
4. `CANDIDATE_BINDING_MISMATCH`;
5. `DESTINATION_BINDING_MISMATCH`;
6. `PROFILE_BINDING_MISMATCH`;
7. `PRICING_FACTS_INVALID_OR_STALE`;
8. `PRICING_BINDING_MISMATCH`;
9. `TOKEN_SEMANTICS_MISMATCH`;
10. `REASONING_REQUIREMENT_UNSUPPORTED`;
11. `CONTEXT_CAPACITY_INSUFFICIENT`;
12. `INPUT_TOKEN_CAPACITY_INSUFFICIENT`;
13. `OUTPUT_TOKEN_CAPACITY_INSUFFICIENT`;
14. `TOTAL_TOKEN_CAPACITY_INSUFFICIENT`;
15. `INVOCATION_TIME_UNSUPPORTED`;
16. `DATA_CLASSIFICATION_UNSUPPORTED`;
17. `COST_CURRENCY_MISMATCH`; and
18. `COST_CEILING_EXCEEDED`.

Unknown schema versions, unsupported enum values, malformed retained relationships,
ambiguous time, checked-arithmetic failure, or evidence that cannot be revalidated
fails closed at the earliest applicable reason. Combined failures return only the
earliest reason; no later result overwrites it.

## Binding And Freshness Rules

`SOURCE_CONTENT_BINDING_MISMATCH` requires the eligibility proposal's exact outbound
content source digest to equal the digest of the request prompt. The evaluator does not
render or retain either prompt in its decision.

Candidate, capability, and pricing facts are each fresh only when:

```text
observedAt <= evaluationTime < expiresAt
```

Equality at observation is fresh; equality at expiry is stale. Their fixed schema
versions and intrinsic relationships must also be valid. The candidate-facts check
precedes capability freshness, and pricing is checked only after candidate,
capability, route, and profile bindings.

Capability candidate and destination identities must equal those in the exact
eligibility proposal's candidate facts, destination policy, and matching allow rule.
Candidate facts, capability facts, pricing facts, and the request profile must agree on
model class where they carry it. Candidate facts, capability facts, and the profile
must agree on required capability. A different but equal reconstructed candidate-facts
value is not accepted as the exact candidate input because the evaluator obtains and
the successful proof retains only the instance inside the exact eligibility.

The evaluator does not infer an endpoint, provider model, or route from any identity.
The identities are compared as bounded opaque values only.

## Requirement And Capacity Rules

Reasoning and classification use the existing closed repository enum order. The
capability maximum reasoning must be at least the profile reasoning requirement. The
capability maximum classification must be at least RFC-0028
`Eligible.effectiveClassification()`, not merely the profile classification. Enum
reordering or inserting an intermediate meaning is a material compatibility change.

Context, input-token, output-token, and total-token capacity are four independent
comparisons. Each capability maximum must be at least its corresponding RFC-0014
requirement. The retained maximum token budget must remain internally valid; total
capacity does not compensate for insufficient input or output capacity.

The capability maximum invocation time must be at least the profile maximum invocation
time. Because it is a supported caller-requested timeout ceiling, this direction does
not assert a latency guarantee.

The capability, exact RFC-0028 pricing facts, and current fixed
`profile-token-budget-v1` semantics must match exactly. This proves only that capacity
and conservative price use the profile budget's defined units. It is not a provider
tokenizer, exact-request token count, usage normalization, or conversion proof.

## Exact Pricing And Cost Rules

RFC-0028's exact eligibility must contain pricing facts. Those facts must be fresh and
must bind the same candidate, destination, and model class. Capability facts must bind
the exact pricing `priceScheduleIdentity` and an equal `ModelTokenBudget`
`tokenBudgetBasis`. Equality of the basis is data equality; the successful suitability
proof continues to retain the exact RFC-0028 eligibility and candidate-facts instances.

Cost currency must match exactly across capability facts, pricing facts, and the
profile budget. No exchange rate or conversion is permitted. The conservative
relationships are:

```text
pricingFacts.worstCaseChargeMicrounits
    <= capabilityFacts.maximumCostMicrounits
    <= profiledRequest.executionProfile().costBudget().maxMicrounits
```

All amounts use the existing non-negative bounded microunit representation. The first
comparison prevents capability evidence from understating the exact price schedule;
the second proves fit within the request ceiling. Neither value is a payment approval,
spend grant, billing record, or permission to choose another schedule. RFC-0028 v1
currently permits `Eligible` only for zero charge, but these defensive relations remain
part of the versioned suitability contract.

## Successful Proof

Only after every check passes does `Suitable` contain a newly created
`RemoteModelCandidateSuitability`. The proof preserves the exact input
`ProfiledModelRequest`, exact input `Eligible`, and exact
`eligibility.proposal().candidateFacts()` instances. Its remaining existing fields are
copied from the validated capability facts: source identity and revision, freshness,
candidate and destination identities, token semantics, capacity evidence, model class,
required capability, capacity maxima, supported timeout, cost currency and maximum,
and maximum classification.

RFC-0030 v1 does not widen the RFC-0029 proof shape to retain the capability-facts
instance, price-schedule identity, or cost-basis value. Their exact relationships are
proved at evaluation time and the proof remains useful only in the same invocation
through RFC-0029, which revalidates the exact eligibility, pricing, scalar suitability
limits, and freshness. Persisting or replaying the proof is forbidden.

The proof means only that independently governed evidence was sufficient at the
explicit evaluation time. It does not mean the provider is currently available, the
route will succeed, the response is valid, payment is approved, or invocation is
authorized.

## Safe Rendering And Lifetime

Facts and decisions may render only their schema or contract version and bounded source,
revision, evidence, token-semantics, price-schedule, candidate, destination, model,
capability, currency, or enum identifiers needed for diagnostics. They must not render
request or outbound prompt content, content digests, findings, redaction details, task
text, policy or allow-rule contents, retention details, endpoints, provider model
labels, credentials, secrets, raw prices or capacities, exceptions, or source errors.

Capability facts, decisions, and proof are invocation-local. They are never serialized,
persisted, cached, replayed, placed in a RunRecord, or reconstructed from identifiers.
Before a durable invocation result exists, every retry or fallback attempt reacquires
fresh RFC-0016 admission, RFC-0028 eligibility and pricing, RFC-0030 capability facts
and suitability, and RFC-0029 routing. No earlier option, proof, or selected route is
promoted or reused.

After a future exact durable result reference exists, recovery may validate historical
provenance and finish deterministic finalization but must not reacquire suitability to
retransmit. Durable route provenance requires a separate schema and compatibility
decision; current Model RunRecord v2 remains unchanged.

## Future Source And Consumer Boundaries

The first RFC-0030 implementation is limited to the opaque value, sealed decision,
closed reasons, and field-free evaluator under test-owned fixtures. Source, locality,
dependency, reflection, and no-caller guards must prove that only the evaluator can
construct the proof, no production source creates capability facts, and no production
caller invokes the evaluator.

A later production fact source must be separately accepted and must define authority,
provenance, bounded acquisition, trust and refresh behavior, failure mapping, and how
candidate identity, capability, capacity, token semantics, supported timeout, price
schedule, and classification approval are independently established. It must not be
hidden inside the evaluator or Router.

The first later consumer of a successful RFC-0030 decision is RFC-0029 route-option
construction for the same invocation. A still-later route-execution preparation
boundary may consume an exact `SelectedModelRoute` only after separate credential,
external-effect, spend when needed, request composition, response validation,
provenance, cancellation, retry, and recovery contracts are accepted. RFC-0030 itself
authorizes none of those effects.

## Required RED-First Implementation Sequence

A later pure implementation task is sequential:

1. add the opaque capability-facts value, refusal enum, and sealed decision under
   missing-symbol RED tests;
2. add the field-free evaluator and cover every reason alone and in combined
   first-match cases, exact-instance retention, strict freshness, boundary equality,
   checked capacity, exact pricing, and safe rendering;
3. move RFC-0029 proof construction from arbitrary package-local fixtures to the exact
   evaluator success path and add source, locality, dependency, reflection,
   construction, and zero-production-caller guards; and
4. run focused RFC-0014, RFC-0016, RFC-0020, RFC-0028, RFC-0029, RFC-0030,
   deterministic-fake, architecture, and full Markdown-sensitive regression.

No implementation increment may add a production source or caller. If truthful
governed capability evidence is unavailable, success remains test-owned and no
production remote route becomes reachable.

## Verification Contract

The later pure implementation must cover null programming errors; all intrinsic
bounds; inclusive observation and exclusive expiry for candidate, pricing, and
capability facts; every route/profile/content/price binding; every refusal reason alone
and in combined precedence cases; each capacity boundary below, equal, and above the
requirement; checked token arithmetic; reasoning and classification enum extremes;
timeout direction and precision; exact currency and price-schedule binding; zero and
bounded costs; exact input identity retention; safe rendering; field-free purity; and
zero registry, Tool, gateway, provider, adapter, credential, filesystem, environment,
process, network, persistence, spend, runtime, or external activity.

Regression must prove RFC-0016, RFC-0028, and RFC-0029 behavior unchanged and prove
that the deterministic fake remains only on RFC-0020 through RFC-0027. Source guards
must allow only the evaluator to construct `RemoteModelCandidateSuitability` and must
prevent capability facts, decisions, and proofs from entering any production caller.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Roadmap/Planner/Assisted-Loop/dynamic-workflow governance, `git diff --check`,
and the full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Inferring suitability from RFC-0028 eligibility or candidate facts alone is rejected
  because policy permission and route identity do not prove capacity.
- Reusing RFC-0020 local `Suitable` is rejected because it is structurally bound to the
  deterministic fake and local admission.
- Accepting provider labels, endpoint metadata, catalog rows, availability Booleans,
  caller assertions, prior success, scores, or identifiers as proof is rejected
  because they are not independently governed capability evidence.
- Letting the evaluator or Router acquire facts is rejected because it would hide
  external effects and freshness behind a pure decision boundary.
- Automatic token-semantics or currency conversion is rejected because no approved
  conversion authority or conservative error contract exists.
- Treating a timeout ceiling as observed latency, a cost ceiling as spend authority, or
  a classification maximum as transmission permission is rejected because each would
  widen the evidence meaning.
- Combining availability, quality, latency, scoring, fallback, caching, or provider
  execution into suitability is rejected because each requires distinct governed
  facts and policy.
- Persisting, caching, serializing, or replaying facts, decisions, or proof is rejected
  because policy, price, capacity, and approval may change between attempts.

## Exclusions

- Java or test-source implementation and changes to RFC-0013 through RFC-0029 behavior;
- production capability, candidate, destination-policy, inspection, redaction,
  pricing, availability, quality, latency, tokenizer, or suitability sources;
- discovery, registry, scoring, provider or adapter construction, provider model,
  endpoint, protocol, credential, secret, network, transmission, or response handling;
- spend authorization, billing, paid service, currency or token conversion, fallback,
  caching, retry execution, or durable route provenance;
- Tool/evidence/RunRecord/runtime-event/Scheduler/finalization/recovery integration,
  durable schema or migration, MCP, plugin, interface, or background work; and
- capability promotion, push, merge, release, deployment, permission change,
  destructive cleanup, or another external effect.

## Codex Implementation Prompt

Implement RFC-0030 RED-first as an opaque immutable remote candidate capability-facts
value, closed suitability decision, and field-free evaluator. Accept only the exact
profiled request, exact RFC-0028 `Eligible`, capability facts, and explicit evaluation
time; validate every binding and capacity in the closed order; and create the existing
RFC-0029 proof only on success. Add no production source or caller, provider, adapter,
credential, network, spend, persistence, retry, fallback, runtime, or MCP path.
