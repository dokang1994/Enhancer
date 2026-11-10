# RFC-0035: Protected Remote Model Route Aggregation And Selection

Status: Accepted

## Purpose

Define the provider-neutral, same-invocation composition that consumes one bounded
caller-ordered collection of exact RFC-0034 preparation results, refuses without
silently dropping any failed preparation, constructs the existing RFC-0029
`ModelRouteOptionSet`, and invokes the existing pure `ModelRouter` exactly once.

This RFC specifies that composition only. It does not implement it, construct or call
an RFC-0034 preparer in production, derive candidate preference, execute a selected
route, construct a provider or adapter, access a credential, transmit content,
authorize spend, persist provenance, retry, fall back, cache, or change capability
maturity.

## Relationship To Existing Contracts

RFC-0028 remains the sole outbound-policy authority and owner of each exact
`Eligible`. RFC-0030 remains the sole remote-suitability proof creator. RFC-0033
remains the protected capability-fact acquisition bridge. RFC-0034 remains the sole
same-invocation constructor of one prepared RFC-0029 option from those boundaries.
RFC-0035 neither recreates nor reevaluates any of them.

RFC-0029 remains the sole owner of bounded option-set validation, exact request and
proof binding, freshness, duplicate route identity, closed routing refusals, and
ordinal-zero selection. RFC-0035 preserves exact inputs and delegates those checks
once; it does not copy Router logic, repair an option, suppress a Router refusal, or
create `SelectedModelRoute` itself.

The permitted path is:

```text
caller declares one bounded ordered preparation-result collection
-> RFC-0035 validates collection shape without changing order
-> first RFC-0034 acquisition/suitability refusal stops selection unchanged
-> all Prepared results contribute their exact options in the same order
-> existing RFC-0029 ModelRouteOptionSet is constructed once
-> existing pure ModelRouter receives exact request + set + same evaluationTime once
-> RFC-0035 preserves the exact Router decision
-> selection retains the exact ordinal-zero Prepared result for binding provenance
-> composition stops before route-execution preparation
```

## Field-Free Composition And Exact Public Input

The future final `ProtectedRemoteModelRouteSelectionComposer` is field-free. Its
selection call receives exactly:

```text
ProfiledModelRequest profiledRequest
List<ProtectedRemoteModelRouteOptionPreparationResult> preparationResults
Instant evaluationTime
```

Every input is required. Null input or a null collection element is a programming
error, not a selection refusal. The collection contains zero through 256 results; more
than RFC-0029's `ModelRouteOptionSet.MAXIMUM_OPTIONS` is a programming error before any
result is interpreted. The composition takes a defensive immutable snapshot for the
duration of the call and retains no collection afterward.

The call accepts no binding or catalog path, pin, candidate or destination selector,
eligibility, capability facts, suitability proof, raw route option, option set,
comparator, score, weight, random source, provider priority, registry, clock, endpoint,
credential, spend grant, retry policy, cache, fallback, or execution input.

The field-free composition creates one field-free `ModelRouter` only after every
non-programming result check succeeds. It does not construct, own, or invoke
`ProtectedRemoteModelRouteOptionPreparer`. Test seams cannot add a second production
Router, source, evaluator, clock, or ordering authority.

## Caller Order And Collection Completeness

Collection order is caller-declared preference data under RFC-0029. RFC-0035 preserves
the exact ordinal of every supplied result and never sorts, scores, filters, partitions,
deduplicates, truncates, appends, or substitutes an entry. Order grants no outbound,
provider, spend, transmission, or execution authority.

RFC-0035 can prove only the completeness of the collection it receives. It cannot
discover candidates or prove that an upstream caller supplied every candidate intended
by some external policy. Any future production caller must separately bind one declared
attempt plan to one result per ordered candidate and is not authorized here. No current
production caller may omit a refusal, reorder results, or convert a failed preparation
into a smaller selectable collection.

Although RFC-0034 names one or more prepared options as the positive future path, an
empty collection is valid here so the composition remains total over RFC-0029's exact
zero-through-256 input contract. It constructs the existing empty
`ModelRouteOptionSet`, invokes the Router once, and preserves the Router's exact
collection-wide `NO_ROUTE_OPTIONS` refusal. RFC-0035 adds no parallel empty-input
reason.

## Preparation-Refusal Dominance

After complete programming-error validation, RFC-0035 examines results in ascending
caller ordinal. The first `AcquisitionRefused` or `SuitabilityRefused` result is
returned by exact instance and suppresses option-set construction and Router activity.
Later results are not used to choose a different candidate. A refusal is never dropped,
flattened, relabeled, retried, replaced, or reconstructed.

Prepared options before or after the first refusal do not change that outcome. Router
validation is unnecessary because no route can be selected from an incomplete declared
attempt. The preparation refusal's lowest ordinal is safe bounded metadata; no option,
request, digest, path, fact, or candidate text is exposed by the composition result.

Acquisition and suitability keep their RFC-0034 distinction. RFC-0035 does not merge
their reasons into a new enum, reinterpret their internal precedence, or inspect a
binding digest to decide whether selection may continue.

## Exact Option Aggregation

Only when every result is RFC-0034 `Prepared` does RFC-0035 construct one
`ModelRouteOptionSet`. Its list contains each exact `Prepared.option()` instance in the
same ordinal order. The composition does not copy, recreate, refresh, compare, repair,
or prevalidate an option and does not use binding digests as selection inputs.

RFC-0029 remains responsible for request/content/profile/candidate/destination binding,
eligibility and suitability freshness, duplicate candidate-destination identity, whole-
set validation, and first-applicable refusal order. A caller cannot bypass those checks
with a binding digest, preparation success, order, equality, or a previously selected
route.

## Exact One-Call Router Sequence

For every call, the future composition performs this deterministic sequence:

1. require the exact profiled request, result collection, evaluation time, bounded
   cardinality, and every non-null element;
2. take one defensive immutable ordered snapshot;
3. return the first exact RFC-0034 acquisition or suitability refusal by lowest ordinal;
4. otherwise construct one `ModelRouteOptionSet` from all exact prepared options in
   unchanged order;
5. call one field-free `ModelRouter.evaluate` exactly once with the exact profiled
   request, that option set, and the unchanged evaluation time;
6. if the Router refuses, return its exact `ModelRouteSelectionDecision.Refused`; or
7. if the Router selects, return its exact `ModelRouteSelectionDecision.Selected` with
   the exact ordinal-zero RFC-0034 `Prepared` result whose option is the selected
   route's exact option instance.

The composition reads no clock. The same explicit evaluation time used by every
RFC-0034 preparation in the declared attempt must reach RFC-0035 and the Router, but
RFC-0035 cannot prove how an unauthorized caller obtained its inputs. A future caller
must preserve that same-invocation identity and time contract.

## Closed Composition Result

`ProtectedRemoteModelRouteSelectionResult` is one sealed immutable result with exactly
four shapes:

```text
Selected(ModelRouteSelectionDecision.Selected selection,
         ProtectedRemoteModelRouteOptionPreparationResult.Prepared selectedPreparation)
AcquisitionRefused(
         ProtectedRemoteModelRouteOptionPreparationResult.AcquisitionRefused refusal,
         int optionOrdinal)
SuitabilityRefused(
         ProtectedRemoteModelRouteOptionPreparationResult.SuitabilityRefused refusal,
         int optionOrdinal)
RoutingRefused(ModelRouteSelectionDecision.Refused refusal)
```

Each shape retains the exact upstream instance. `Selected` is an opaque final subtype
whose package-private construction is owned only by the composition; callers cannot
associate a different prepared result or valid-looking binding digest with a Router
selection. The other shapes likewise grant no construction authority. The two
preparation-refusal ordinals
must be zero through 255. `Selected` requires the Router-selected ordinal to be zero and
the exact selected option instance to be `selectedPreparation.option()`. It therefore
associates the selected route with the existing redacted binding digest without copying
the digest or widening `SelectedModelRoute`. `RoutingRefused` adds no second reason or
ordinal and preserves RFC-0029's exact decision.

The result is not a routing authority minted by RFC-0035. The selected route remains
the only opaque RFC-0029 selection value, and it still grants no provider, credential,
transmission, spend, invocation, persistence, retry, fallback, or recovery authority.

## Refusal Precedence

After required programming-error checks, the composition's closed precedence is:

1. lowest-ordinal RFC-0034 `AcquisitionRefused` or `SuitabilityRefused`, preserving the
   exact concrete kind at that ordinal;
2. exact RFC-0029 `ModelRouteSelectionDecision.Refused`; or
3. exact RFC-0029 `ModelRouteSelectionDecision.Selected` associated with the exact
   ordinal-zero `Prepared` result.

RFC-0029 keeps its existing nine-reason internal order. Empty input therefore reaches
Router refusal at collection ordinal `-1`; malformed bindings, stale proofs, and
duplicate route identity retain their Router-owned ordinals. Unexpected programming or
runtime defects use the repository's internal-error boundary and never become prose or
exception-bearing selection results.

## Lifetime, Retry, And Recovery

Preparation results, the option set, Router decision, selected preparation, binding
provenance, and selected route belong to one invocation. RFC-0035 never serializes,
persists, caches, replays, transmits, or reconstructs them from identifiers. It performs
no retry or fallback and never promotes another ordinal after preparation or routing
refusal.

A later explicit attempt must repeat every authoritative request preparation, RFC-0016
evaluation, RFC-0028 eligibility, RFC-0033 acquisition, RFC-0030 suitability, RFC-0034
option preparation, and RFC-0035 selection under its separately accepted policy and
fresh explicit time. Failure, expiry, catalog rotation, binding change, price change,
content change, destination change, or provider availability never reuses this call's
set or decision.

After a future exact durable invocation result exists, recovery may validate separately
accepted historical provenance and finish deterministic finalization but must not use
RFC-0035 to reroute or retransmit. Durable binding or selected-route provenance requires
a separate schema and compatibility decision.

## Exact Stopping Boundary And Future Consumer

RFC-0035 stops after the exact pure Router decision. It does not construct or invoke an
RFC-0034 preparer, derive or validate a candidate plan, select a provider adapter,
resolve an endpoint, acquire a credential, compose a provider request, inspect response
content, authorize spend, transmit, retry, fall back, persist provenance, or update a
RunRecord.

The first future consumer of an exact `SelectedModelRoute` remains a separately
accepted route-execution preparation boundary. That boundary must independently define
provider construction, credential and secret ownership, external-effect and spend
authority when needed, request composition, response validation, timeout and
cancellation, provenance, retry/fallback policy, recovery, and durable compatibility
before any adapter may be constructed or invoked.

## Safe Observability

Permitted rendering is limited to the RFC-0035 result kind, closed upstream reason
identity, and safe bounded ordinal. `Selected` rendering exposes neither the selected
route nor the selected preparation. Binding digests, options, requests, prompts,
outbound content, paths, pins, catalog bytes, facts, policy contents, findings,
redaction details, prices, capacities, times, endpoints, credentials, secrets,
exceptions, and source errors remain redacted.

Possessing collection order, a prepared option, binding digest, option set, Router
decision, or selected route grants no new authority.

## Rejected Shortcuts

- Accepting only successful options is rejected because the composition could not
  prove that a supplied preparation refusal was not silently dropped inside its own
  boundary.
- Letting RFC-0035 call RFC-0034 preparers is rejected because candidate-plan and
  protected-path construction policy require a separate production caller contract.
- Sorting, scoring, filtering, truncating, deduplicating, or selecting the first
  prepared result is rejected because it changes caller intent or bypasses RFC-0029.
- Invoking the Router before all preparation results are known successful is rejected
  because a partial set would create implicit fallback.
- Reimplementing Router checks or constructing `SelectedModelRoute` is rejected because
  RFC-0029 exclusively owns validation and selection.
- Treating binding digest equality as trust, preference, freshness, or route identity is
  rejected because the digest is invocation-local provenance only.
- Reading a clock, retrying, promoting a later ordinal, using provider availability, or
  reconstructing equal request, option, proof, decision, or preparation values is
  rejected.
- Persisting the collection, decision, selected route, or binding provenance is
  rejected until separate durable schemas and compatibility contracts exist.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add the sealed four-shape RFC-0035 result under missing-symbol RED tests for exact
   upstream retention, ordinal bounds, selected-option identity, and safe rendering;
2. add the final field-free composition under RED tests for null/257 programming
   errors, defensive order snapshot, lowest-ordinal preparation refusal, Router
   suppression, exact all-prepared option aggregation, empty-set delegation, exact
   request/time preservation, one Router call, and exact decision retention;
3. narrow existing RFC-0029 and RFC-0034 no-consumer guards only to the exact RFC-0035
   composition while preserving zero production composition caller, zero preparer
   caller, RFC-0031 as sole facts creator, RFC-0030 as sole proof creator, and the local
   deterministic fake's separate path; and
4. run focused RFC-0015, RFC-0028 through RFC-0035, deterministic-fake, architecture,
   source/locality/dependency, governance, and full Markdown-sensitive regression.

Implementation must not add a production caller. If any declared preparation refuses,
no option set or selected remote route may become reachable from that call.

## Verification Contract

The later implementation must cover null request/list/time/elements; zero, one, 256,
and 257 results; defensive copy and exact ordering; every acquisition and suitability
refusal shape at first, middle, and last ordinal; combined lowest-ordinal precedence;
Router suppression on preparation refusal; exact option identity in the set; empty-set
Router refusal; all nine Router refusal classes through the composition; exact Router
request/time inputs; exact one-call behavior; exact decision, selected route, selected
preparation, option, and binding-digest association; repeated fresh calls; safe
rendering; field-free purity; and no preparer, lookup, registry, Tool, gateway, adapter,
credential, filesystem, environment, process, network, spend, persistence, runtime, or
external activity.

Architecture and source guards must allow only the future RFC-0035 composition to
construct `ModelRouteOptionSet` from RFC-0034 preparation results and call
`ModelRouter`; preserve zero production caller for both RFC-0034 preparer and RFC-0035
composition; preserve the existing sole fact/proof/selection creators; prohibit direct
selected-route production consumers; and keep the deterministic fake isolated.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Roadmap/Planner/Assisted-Loop/dynamic-workflow governance, reference inspection,
`git diff --check`, and the full README-owned Java 17 regression are the verification
boundary.

## Exclusions

- Java production or test implementation, preparer/source/evaluator/Router behavior
  change, production preparer or composition construction/calling, installed binding or
  catalog, publisher/evidence composition, candidate discovery or plan authority;
- scoring, sorting, filtering, dynamic priority, availability, load balancing, retry,
  fallback, cache, route-execution preparation, provider, adapter, endpoint, protocol,
  credential, secret, network, transmission, response handling, spend, or billing;
- durable route/binding provenance, Tool, Evidence, RunRecord, Scheduler, AgentRuntime,
  runtime event, schema, migration, MCP, plugin, interface, background work, capability
  promotion, destructive cleanup, push, merge, tag, release, deployment, permission
  change, or another external effect.

## Codex Implementation Prompt

Implement RFC-0035 only under a separately accepted Active Task, RED-first, as one
field-free composition over an exact bounded ordered collection of RFC-0034 results.
Fail closed on the lowest-ordinal preparation refusal without building an option set;
otherwise preserve exact option order and identity, call the existing pure RFC-0029
Router once with the exact request and unchanged time, and preserve its exact decision
plus the exact selected preparation. Add no production caller, preparer invocation,
route execution, provider, credential, network, spend, persistence, retry, fallback,
cache, runtime, or MCP path.
