# RFC-0029: Pure Provider-Neutral Model Router

Status: Accepted

## Purpose

Define the first pure, provider-neutral selection boundary after RFC-0028. The future
`ModelRouter` may choose only among a bounded ordered set whose every option already
has exact fresh outbound eligibility and separate exact remote candidate suitability.
It returns an identity-preserving ephemeral selection or one closed refusal.

This RFC specifies a later pure implementation. It does not add a candidate or fact
source, construct a provider or adapter, acquire a credential, authorize spend, open a
network connection, invoke a model, execute fallback, persist a selection, or change
capability maturity.

## Relationship To Existing Contracts

RFC-0013 through RFC-0015 continue to own the model request and complete untrusted
profile requirements. RFC-0016 continues to own fresh task, policy, capability,
timeout, and local-versus-outbound admission. RFC-0020 through RFC-0027 continue to own
the one closed deterministic-fake suitability, exact-request invocation, typed
execution, provenance, submission, and local spool path. RFC-0028 continues to own the
only outbound policy decision and the exact meaning and lifetime of `Eligible`.

RFC-0029 changes none of those contracts. In particular,
`ModelCandidateSuitabilityDecision.Suitable` is structurally bound to
`DeterministicFakeModelCandidate` and RFC-0016 local `Admitted`; it is not remote
suitability and cannot enter this Router. The deterministic fake continues through its
existing local-only chain and does not pass through RFC-0028 or RFC-0029.

## Why Routing Follows Eligibility And Suitability

A Router decides which already acceptable option is preferred. It cannot create
policy permission, assert that a candidate can satisfy a profile, repair expired facts,
discover a destination, or turn order into authority. Each remote option therefore
must arrive with both:

1. the exact RFC-0028 `ModelOutboundRouteAdmissionDecision.Eligible` for one proposal;
   and
2. a separate `RemoteModelCandidateSuitability` proof for the exact same request,
   profile, and candidate facts.

Eligibility answers whether the exact proposal may enter selection. Suitability
answers whether that candidate can truthfully satisfy the request requirements. Both
are necessary and neither substitutes for the other.

## Exact Router Input

The future field-free `ModelRouter` receives exactly:

```text
ProfiledModelRequest profiledRequest
ModelRouteOptionSet options
Instant evaluationTime
```

Every value is required. Null is a programming error, not a routing refusal. The
Router performs no ambient lookup, discovery, registry access, policy evaluation,
content transformation, pricing acquisition, filesystem or network access,
persistence, Tool call, gateway call, or clock read. The trusted evaluation time is an
explicit input used only for proof freshness.

The exact `ProfiledModelRequest` prevents a collection of individually plausible
options from being combined across requests. Its request prompt digest must equal the
source prompt digest retained by every option's RFC-0028 outbound content. Its exact
profile model class, required capability, reasoning, context, token, cost, timeout,
locality, and classification requirements are the requirements bound by each remote
suitability proof.

## Remote Candidate Suitability Proof

`RemoteModelCandidateSuitability` is an opaque, immutable, invocation-local proof
created only by a separately accepted pure remote-suitability evaluator. It retains:

- the exact `ProfiledModelRequest` instance;
- the exact RFC-0028 `ModelOutboundRouteAdmissionDecision.Eligible` instance;
- the exact `ModelRouteCandidateFacts` instance from one RFC-0028 proposal;
- a bounded suitability source identity and revision;
- an observation time and exclusive expiry time;
- the exact token-semantics and capacity-evidence identities used;
- the exact model class and required capability;
- the proven maximum reasoning, context, input-token, output-token, total-token,
  timeout, cost, and model-visible classification limits; and
- a contract version.

The proof's later evaluator must accept the exact eligibility instance, compare every
retained RFC-0014 requirement against independently governed candidate evidence, use
checked arithmetic, and succeed only when all requirements fit. A provider label,
endpoint, catalog row, availability Boolean, profile assertion, eligibility fields or
identifiers, prior success, or RFC-0020 local `Suitable` value cannot create this
proof.

The proof contains no gateway, adapter, provider model name, endpoint, credential,
secret, invocation callback, mutable availability, price lookup, or spend grant. It is
fresh only when `observedAt <= evaluationTime < expiresAt`; equality at expiry is
stale. The suitability evaluator and its governed evidence sources require separate
accepted tasks and are not implemented by this RFC.

## Identity-Preserving Route Option

One immutable `ModelRouteOption` retains exactly:

```text
ProfiledModelRequest profiledRequest
ModelOutboundRouteAdmissionDecision.Eligible eligibility
RemoteModelCandidateSuitability suitability
```

Construction validates intrinsic non-null shape only. Router evaluation proves all
cross-bindings without copying or reconstructing an authority or governed fact:

- the option request is the Router's exact request instance;
- suitability retains that exact request instance;
- suitability retains the option's exact eligibility instance;
- suitability retains the exact candidate-facts instance held by the eligible
  proposal;
- request model class and required capability equal both suitability and candidate
  facts;
- request source-prompt digest equals the eligible outbound-content source digest;
- candidate and destination identities equal those retained by the eligible proposal,
  candidate and destination facts, matching destination rule, pricing facts, and
  suitability provenance, while inspection provenance remains separately bound to the
  exact content digests; and
- eligibility and suitability are fresh for the exact Router evaluation time.

`Eligible` remains the original RFC-0028 subtype. The Router cannot recreate it from a
proposal, identifiers, Booleans, or stored provenance. A different but equal request,
candidate-facts value, proposal, or proof is not the exact instance and refuses.

## Bounded Ordered Option Set

`ModelRouteOptionSet` is an immutable defensive copy of zero through 256 options in
caller-declared preference order. The order is data, not authority. The set accepts no
comparator, score, weight, random source, default candidate, provider priority,
registry, lazy supplier, or mutable collection.

Route identity is the exact pair of bounded candidate identity and destination
identity. Duplicate route identity is forbidden even when eligibility, suitability,
price, revision, or ordinal differs. This prevents repeated entries from weighting a
route or disguising fallback. Empty input is an ordinary closed routing refusal so the
public Router result remains total. More than 256 options and null elements are
programming errors rejected during value construction before Router evaluation.

The Router validates every option in ordinal order before selecting any option. It
does not select the first apparently usable entry and ignore a malformed later entry.
After the complete set passes, it selects ordinal zero. A future scoring, quality,
latency, availability, cost-comparison, or policy-scoped fallback strategy requires a
new accepted contract and cannot be inferred from this deterministic first version.

## Closed Deterministic Decision

`ModelRouteSelectionDecision` has exactly two shapes:

```text
Selected(SelectedModelRoute selectedRoute)
Refused(ModelRouteSelectionRefusalReason reason, int optionOrdinal)
```

The ordinal is `-1` for collection-wide refusal and otherwise identifies the first
offending option without exposing candidate or destination text. `Refused` retains no
request, content, governed fact, exception, path, endpoint, price, or source error.

After programming-error checks, evaluation returns the first matching reason in this
repository order:

1. `NO_ROUTE_OPTIONS`;
2. `REQUEST_BINDING_MISMATCH`;
3. `SOURCE_CONTENT_BINDING_MISMATCH`;
4. `PROFILE_BINDING_MISMATCH`;
5. `CANDIDATE_BINDING_MISMATCH`;
6. `DESTINATION_BINDING_MISMATCH`;
7. `ELIGIBILITY_INVALID_OR_STALE`;
8. `SUITABILITY_INVALID_OR_STALE`;
9. `DUPLICATE_ROUTE_IDENTITY`.

For option-scoped checks, the lowest caller ordinal wins. Duplicate detection compares
each option with earlier validated ordinals; the later duplicate ordinal is reported.
Unknown contract versions, unsupported enum values, absent retained provenance,
identity ambiguity, clock ambiguity, expiry equality, or evidence that cannot be
revalidated refuses at the earliest applicable reason. The Router never drops an
invalid option, reorders the set, or continues after a refusal.

## Selected Model Route

`SelectedModelRoute` is an opaque immutable value created only by successful Router
evaluation. It retains the exact Router `ProfiledModelRequest`, exact selected
`ModelRouteOption`, exact RFC-0028 `Eligible`, exact
`RemoteModelCandidateSuitability`, exact candidate and destination fact instances,
caller ordinal zero, and Router contract version. Accessors preserve those identities.

The selected value is invocation-local and non-persistent. Its rendering may expose
only the contract version, ordinal, and bounded digests or source identities already
approved for diagnostics. It must not render prompt content, findings, task text,
policy contents, endpoints, credentials, secrets, raw prices, or source errors.

`SelectedModelRoute` means only that one already eligible and suitable option won this
pure deterministic selection. It is not provider construction, transmission or Tool
authority, a gateway call, a credential or spend grant, a durable receipt, evidence, a
RunRecord, a retry decision, or permission to use a substitute candidate.

## Freshness, Retry, And Fallback

Eligibility, suitability, the option set, and selection belong to one attempt and one
trusted evaluation time. They are never serialized, persisted, cached, replayed,
transmitted, or reconstructed from identifiers. Before an invocation result is durably
recorded, every retry repeats authoritative request preparation, RFC-0016 evaluation,
RFC-0028 eligibility, remote suitability, and routing with fresh facts.

RFC-0029 performs no fallback. A caller may request a later attempt only under a
separately accepted fallback policy. Every option in that attempt, including a former
second choice, must have its own fresh exact RFC-0028 eligibility and suitability.
Failure, expiry, provider unavailability, price change, content change, destination
change, or policy revision never promotes another option from an earlier selection.

After a future exact durable result reference exists, recovery may validate historical
provenance and finish deterministic finalization but must not reroute or retransmit.
Durable selected-route provenance requires a separate schema and compatibility
decision; current Model RunRecord v2 remains unchanged.

## Exact Future Consumers And Boundaries

The first future production consumer of `SelectedModelRoute` is not authorized here.
A separately accepted route-execution preparation boundary must require this exact
selection plus independently approved credential, external-effect, spend when needed,
request composition, response validation, provenance, timeout/cancellation, retry, and
recovery contracts before an adapter may be constructed or invoked.

The first RFC-0029 implementation remains pure and has no production caller. Existing
RFC-0028 source guards must later be narrowed intentionally so only the pure Router may
consume `Eligible`; all other production consumers and all infrastructure coupling
remain forbidden. Governed candidate and destination sources, scanners/redactors,
pricing, suitability evidence, provider adapters, credentials, networks, MCP, and
durable route provenance remain distinct later boundaries.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add the opaque remote-suitability, route-option, bounded-set, selected-route, and
   sealed-decision values under missing-symbol RED tests, with no production caller;
2. add the field-free Router and prove whole-set validation, exact identity binding,
   strict freshness, duplicate refusal, closed first-match order, and ordinal-zero
   selection;
3. add safe-rendering, reflection, source, locality, and dependency guards, and update
   the RFC-0028 no-consumer guard only for the exact pure Router dependency; and
4. run focused RFC-0014, RFC-0016, RFC-0020, RFC-0028, RFC-0029, deterministic-fake,
   architecture, source/locality, and full Markdown-sensitive regression.

The implementation cannot make remote suitability constructible from caller metadata.
If truthful governed suitability evidence is unavailable, its success fixture remains
test-owned and no production route becomes reachable.

## Verification Contract

The later pure implementation must cover both decision shapes and every refusal reason
alone and in combined first-match cases; zero, one, 256, and 257 options; defensive
copying and immutable order; every cross-binding and equal-but-not-identical
substitution; inclusive observation and exclusive expiry; all duplicate positions;
validation of later malformed entries before selection; exact instance retention;
safe rendering; field-free purity; and zero lookup, registry, Tool, gateway, adapter,
credential, filesystem, environment, process, network, persistence, spend, runtime,
or external activity.

Regression must prove RFC-0016 and RFC-0028 behavior unchanged and prove that the
deterministic fake still uses only RFC-0020 through RFC-0027. Source guards must permit
only the exact Router to consume RFC-0028 eligibility and must prevent selected routes
from entering any production caller.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Planner/Assisted-Loop/dynamic-workflow governance, `git diff --check`, and the
full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Reusing RFC-0020 `Suitable` is rejected because it proves only the exact closed local
  deterministic fake after RFC-0016 local admission.
- Accepting raw proposals, profiles, candidate metadata, endpoints, provider labels,
  Booleans, scores, or identifiers is rejected because the Router would manufacture or
  reconstruct authority and governed facts.
- Letting the Router call RFC-0028 or a suitability evaluator is rejected because fact
  acquisition and selection would be hidden in one boundary.
- Selecting the first valid option while ignoring a malformed later option is rejected
  because ordering would become a policy bypass and unsafe fallback channel.
- Sorting by caller score, candidate identity, destination, price, latency, or
  availability is rejected because those policies and comparable governed evidence do
  not yet exist.
- Silently removing duplicates or invalid entries is rejected because input mutation
  would change declared intent.
- Persisting, caching, or replaying eligibility, suitability, options, or selection is
  rejected because authority and facts may change between attempts.

## Exclusions

- Java or test-source implementation and changes to RFC-0013 through RFC-0028 behavior;
- production candidate, destination-policy, inspection, redaction, pricing,
  availability, token, capacity, quality, latency, or suitability fact sources;
- registry, discovery, scoring, load balancing, fallback/cache execution, provider or
  adapter construction, provider model, endpoint, protocol, network, or remote call;
- credential or secret access, spend authorization, billing, paid service, currency
  conversion, request transmission, response validation, or prompt-injection defense;
- Tool/evidence/RunRecord/runtime-event/Scheduler/finalization/retry/recovery
  integration, durable schema or migration, MCP, plugin, interface, or background work;
  and
- capability promotion, push, merge, release, deployment, permission change,
  destructive cleanup, or another external effect.

## Codex Implementation Prompt

Implement RFC-0029 RED-first as a pure provider-neutral Router. Require the exact
profiled request, exact RFC-0028 eligibility, and exact separately proven remote
suitability for every bounded ordered option. Validate the complete set, fail closed on
the first typed mismatch or duplicate, and select only ordinal zero while preserving
every exact identity. Add no fact source, provider, adapter, credential, network,
spend, persistence, fallback execution, or production caller.
