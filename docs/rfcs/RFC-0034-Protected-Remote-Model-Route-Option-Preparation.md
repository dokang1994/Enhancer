# RFC-0034: Protected Remote Model Route Option Preparation

Status: Accepted

## Purpose

Define the one-option, same-invocation composition that connects RFC-0033 protected
capability-fact acquisition to RFC-0030 remote suitability evaluation and constructs
one RFC-0029 `ModelRouteOption` only after both boundaries succeed.

This RFC specifies an uncomposed preparation boundary only. It does not implement the
boundary, construct an option set, call `ModelRouter`, select or execute a route,
construct a provider or adapter, access a credential, transmit content, authorize
spend, persist provenance, execute retry or fallback, or change capability maturity.

## Relationship To Existing Contracts

RFC-0028 remains the sole outbound-policy authority and owns the exact `Eligible`,
proposal, candidate, destination, content, classification, pricing, purpose, and
retention facts. RFC-0034 accepts an existing exact `Eligible`; it never creates,
reconstructs, refreshes, or weakens one.

RFC-0033 remains the sole protected fixed-binding acquisition bridge. It owns the
construction-time binding path, fresh canonical before/after samples, exact RFC-0031
delegation, stable binding digest, and closed binding/source refusals. RFC-0034 neither
reads a binding or catalog nor constructs capability facts.

RFC-0030 remains the sole creator of `RemoteModelCandidateSuitability`. RFC-0034 calls
the existing field-free evaluator with exact inputs and preserves its closed refusal.
It performs no suitability check itself.

RFC-0029 remains the sole Router and owner of option and selection semantics. RFC-0034
constructs one existing `ModelRouteOption`; it does not construct
`ModelRouteOptionSet`, declare preference order, validate a collection, invoke
`ModelRouter`, or create `SelectedModelRoute`.

The permitted path is:

```text
protected composition fixes one RFC-0033 binding path
-> prepare receives exact ProfiledModelRequest + exact RFC-0028 Eligible + evaluationTime
-> candidate/destination selectors derive only from that exact eligibility
-> RFC-0033 acquires once from the stable protected binding
-> exact acquired facts enter RFC-0030 once with unchanged request/eligibility/time
-> exact RFC-0030 suitability proof enters one RFC-0029 ModelRouteOption
-> preparation stops before option-set aggregation and Router selection
```

## Construction Authority And Public Input

The future final preparer has one production construction input: the absolute normalized
fixed binding path already required by RFC-0033. Construction creates one
`ProtectedRemoteCapabilityCatalogFactSource` bound to that path and one field-free
`RemoteModelCandidateSuitabilityEvaluator`. Test-only package-private injection may
observe interactions but cannot become a second public construction or production fact
source.

The binding path cannot come from or be overridden by a prepare call, request,
eligibility, proposal, candidate, destination, profile, catalog content, CLI argument,
environment variable, JVM property, repository, working directory, provider label, or
ambient identity. RFC-0033 continues to validate it and sample it freshly on every
acquisition.

Each prepare call accepts exactly:

- one exact `ProfiledModelRequest`;
- one exact `ModelOutboundRouteAdmissionDecision.Eligible`; and
- one explicit `Instant evaluationTime`.

Every input is required. Null is a programming error, not a preparation refusal. The
call accepts no candidate or destination selector, binding path, catalog path, pin,
capability facts, suitability proof, route option, option set, clock, provider,
credential, endpoint, spend grant, retry policy, cache, or execution input.

## Selector Derivation

The preparer derives the candidate identity only from:

```text
eligibility.proposal().candidateFacts().candidateIdentity()
```

It derives the destination identity only from:

```text
eligibility.proposal().destinationPolicyFacts().destinationIdentity()
```

Those bounded identities are passed unchanged to RFC-0033 with the exact caller-supplied
evaluation time. A caller cannot repeat or override them. The preparer does not inspect
an allow rule to choose another candidate, infer an endpoint, scan a catalog, or repair
an eligibility mismatch; RFC-0030 and RFC-0029 retain their existing exact cross-binding
validation.

## Exact Same-Invocation Sequence

For every call, the future preparer performs the following sequence once:

1. validate required programming inputs;
2. derive the candidate and destination identities from the exact eligibility;
3. call the bound RFC-0033 source exactly once with those identities and the exact
   evaluation time;
4. if RFC-0033 refuses, return the exact acquisition refusal and stop;
5. if RFC-0033 acquires, call the field-free RFC-0030 evaluator exactly once with the
   exact profiled request, exact eligibility, exact acquired facts instance, and the
   same evaluation time;
6. if RFC-0030 refuses, return the exact suitability reason with the acquired binding
   digest and stop; or
7. if RFC-0030 succeeds, construct exactly one `ModelRouteOption` from the exact
   profiled request, exact eligibility, and exact suitability proof, and return it with
   the acquired binding digest.

RFC-0033 acquisition refusal dominates and suppresses every evaluator or option action.
RFC-0030 refusal suppresses option construction. No result is parsed from prose,
relabeled as success, retried, replaced, or passed to another candidate.

The same evaluation time crosses acquisition, capability-fact freshness, suitability,
and the future Router call. RFC-0034 does not read a clock. A later Router consumer must
use the prepared option directly in the same invocation and with the same explicit time;
it must not cache, reconstruct, or persist the option or proof.

## Closed Preparation Result

`ProtectedRemoteModelRouteOptionPreparationResult` is one sealed immutable result with
exactly three shapes:

```text
Prepared(ModelRouteOption option, String bindingSha256)
AcquisitionRefused(ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refusal)
SuitabilityRefused(RemoteModelCandidateSuitabilityRefusalReason reason,
                   String bindingSha256)
```

`Prepared` retains the exact newly constructed option and the lowercase RFC-0033
binding SHA-256. `AcquisitionRefused` retains the exact RFC-0033 refusal instance,
including its exact nested RFC-0031 reason only where RFC-0033 permits one.
`SuitabilityRefused` retains the exact RFC-0030 reason and the binding digest from the
successful RFC-0033 acquisition.

The binding digest is invocation-local provenance. It is not catalog truth, freshness,
permission evidence, anti-rollback, route eligibility, selection, provider availability,
transmission, spend, or execution authority. This RFC does not widen
`ModelRouteOption`, `RemoteModelCandidateSuitability`, `SelectedModelRoute`, or any
durable schema to carry it.

The result contains no raw capability facts outside the existing opaque suitability
proof, path, pin, binding or catalog bytes, prompt, outbound content, findings,
redaction details, policy contents, endpoint, credential, secret, raw price, capacity,
timestamp, exception, or source error. Rendering exposes only the result kind and closed
reason identities; option and digest values remain redacted.

## Refusal Precedence

After programming errors, the preparation stage order is:

1. exact RFC-0033 acquisition refusal;
2. exact RFC-0030 suitability refusal; or
3. prepared option.

RFC-0033 and RFC-0030 keep their own internal first-applicable refusal orders unchanged.
RFC-0034 adds no flattened combined reason enum and does not reinterpret an upstream
failure. If an acquisition refusal exists, no suitability refusal can exist for that
call. If suitability refuses, no option exists.

Expected checked acquisition failures are already closed inside RFC-0033. Unexpected
runtime or programming defects use the repository's normal internal-error boundary;
they are not embedded in results or safe rendering.

## Lifetime, Retry, And Recovery

Preparation results, binding provenance, suitability proofs, and route options belong
to one invocation. They are never serialized, persisted, cached, replayed, placed in a
RunRecord, or reconstructed from identifiers. Every later attempt reacquires the fixed
binding through RFC-0033, reevaluates suitability through RFC-0030, and constructs a
fresh option under the caller's separately accepted bounded policy.

RFC-0034 performs no retry or fallback. It never changes candidate, destination, path,
pin, request, eligibility, evaluation time, or facts after refusal. A caller may begin a
new explicit attempt only through a later accepted policy that repeats all authoritative
inputs and evidence.

After a future durable invocation result exists, recovery may validate separately
accepted historical provenance and finish deterministic finalization but must not use
RFC-0034 to reroute or retransmit. Durable binding or selected-route provenance requires
a separate schema and compatibility decision.

## Exact Stopping Boundary And Future Consumers

RFC-0034 stops after one prepared `ModelRouteOption`. It does not know how many options
exist, assign an ordinal, accept caller preference, construct a collection, deduplicate
route identity, select ordinal zero, score, compare, load balance, or choose fallback.

The first future consumer may place one or more independently prepared exact options
into `ModelRouteOptionSet` and invoke the existing pure `ModelRouter` with the same exact
profiled request and evaluation time. That aggregation and selection boundary requires
separate acceptance because caller ordering is selection policy and every option must
retain independent exact acquisition and suitability provenance.

A still-later route-execution preparation boundary may consume an exact
`SelectedModelRoute` only after separate provider construction, credential, external-
effect, spend when needed, request composition, response validation, cancellation,
retry, provenance, and recovery contracts are accepted. RFC-0034 authorizes none of
those effects.

## Safe Observability

Permitted observations are the preparation result kind, exact closed upstream refusal
reason, and bounded non-secret source/revision/candidate/destination identifiers already
available through the accepted upstream objects. Absolute paths, pins, binding or
catalog bytes, prompts, outbound content, policy contents, findings, redaction details,
raw prices/capacities/times, endpoints, credentials, secrets, and exceptions remain
redacted.

Possessing a prepared option or binding digest grants no authority. The existing Router
must still validate exact identity and freshness, and no provider or execution consumer
is authorized by this specification.

## Rejected Shortcuts

- Accepting candidate, destination, path, pin, facts, proof, option, or clock overrides
  per call is rejected because it would duplicate or bypass upstream authority.
- Letting RFC-0033 call RFC-0030 or construct a route option is rejected because fact
  acquisition and consumption must remain separate.
- Letting RFC-0030 acquire facts or construct an option is rejected because its
  field-free pure evaluation boundary must remain independently testable.
- Constructing `ModelRouteOptionSet` or calling `ModelRouter` is rejected because
  aggregation order is a separate selection-policy input.
- Reconstructing equal request, eligibility, facts, suitability, or option values is
  rejected because the current contracts require exact identity preservation.
- Reading a clock, caching results, retrying acquisition, falling back to another
  binding/candidate/destination, scanning, repairing, or using the publisher lock is
  rejected.
- Persisting binding provenance by widening current route or RunRecord schemas is
  rejected until a separate compatibility decision defines durable provenance.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add the sealed three-shape result under missing-symbol RED tests, including exact
   upstream value retention, digest validation, and safe rendering;
2. add the final preparer with its path-only public construction and package-private
   interaction seam, then prove exact selector derivation, one source call, at most one
   evaluator call, exact instance/time preservation, refusal dominance, and one option
   on success;
3. narrow existing RFC-0033, RFC-0030, RFC-0028, and RFC-0029 no-caller guards only to
   the exact preparer while preserving RFC-0031 as sole facts creator, RFC-0030 as sole
   proof creator, and zero Router or external consumers; and
4. run focused RFC-0015, RFC-0028 through RFC-0034, deterministic-fake, architecture,
   source/locality/dependency, governance, and full Markdown-sensitive regression.

Implementation must not add a production construction caller. If any upstream boundary
refuses, the preparation remains refused and no remote option becomes selectable.

## Verification Contract

The later implementation must cover null programming errors; fixed-path construction;
candidate and destination derivation exclusively from exact eligibility; exact one-call
RFC-0033 delegation; unchanged evaluation time; source refusal dominance and evaluator
suppression; every RFC-0033 refusal; exact acquired-facts identity into RFC-0030; every
RFC-0030 refusal; option-construction suppression; exact request, eligibility, proof,
option, and binding-digest retention; safe rendering; repeated fresh calls; and no
cache, retry, fallback, scan, write, repair, publisher-lock, clock, option-set, Router,
provider, gateway, credential, network, spend, persistence, runtime, or external action.

Architecture and source guards must allow exactly one new production construction site
for `ProtectedRemoteCapabilityCatalogFactSource` and exactly one evaluator caller, both
inside the future preparer; allow the preparer to construct only one existing
`ModelRouteOption`; preserve zero production caller for the preparer and `ModelRouter`;
preserve RFC-0031 as the sole capability-facts creator and RFC-0030 as the sole remote-
suitability creator; and keep the deterministic fake on its existing local path.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Roadmap/Planner/Assisted-Loop/dynamic-workflow governance, reference inspection,
`git diff --check`, and the full README-owned Java 17 regression are the verification
boundary.

## Exclusions

- Java production or test implementation, source/evaluator/option/Router caller change,
  production construction, installed binding/catalog, publisher/evidence composition,
  writer, installer, operator, broker, permission adapter, platform-support claim,
  cleanup, retention, rollback, or anti-rollback anchor;
- option-set aggregation, ordering, scoring, selection, `ModelRouter` invocation,
  `SelectedModelRoute`, route-execution preparation, provider, adapter, endpoint,
  protocol, credential, secret, network, transmission, response handling, spend,
  billing, retry, fallback, cache, or durable route provenance;
- Tool, Evidence, RunRecord, Scheduler, AgentRuntime, runtime-event, durable schema,
  migration, MCP, plugin, interface, background work, capability promotion, push,
  merge, tag, release, deployment, or another external effect.

## Codex Implementation Prompt

Implement RFC-0034 only under a separately accepted Active Task, RED-first, as one
uncomposed same-invocation preparer. Derive candidate and destination only from the
exact RFC-0028 eligibility, invoke RFC-0033 exactly once, invoke RFC-0030 at most once
with exact acquired facts and unchanged request/eligibility/time, and construct one
RFC-0029 `ModelRouteOption` only on success. Add no option set, Router call, production
caller, provider, credential, network, spend, execution, persistence, retry, fallback,
cache, runtime, or MCP path.
