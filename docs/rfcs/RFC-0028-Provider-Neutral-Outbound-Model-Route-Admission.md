# RFC-0028: Provider-Neutral Outbound Model Route Admission

Status: Accepted

## Purpose

Define the provider-neutral, fresh, default-deny policy boundary that must precede any
remote model route. The boundary intersects the exact active task, execution policy,
profiled request, authoritative capability, and independently governed candidate,
destination, purpose, retention, content-inspection/redaction, classification, and
pricing facts for one exact route proposal.

This RFC specifies a later pure implementation. It does not create a second candidate,
construct a Router or provider adapter, inspect or redact content, acquire pricing or
credentials, authorize spend, open a network connection, invoke a model, persist a
decision, or change capability maturity.

## Relationship To Existing Contracts

RFC-0013 continues to own `ModelRequest`, `ModelGateway`, the deterministic fake,
`ModelInvokeTool`, the inert package-private HTTP adapter shape, and bounded responses
and typed failures. RFC-0014 continues to own the complete untrusted execution-profile
requirements. RFC-0015 continues to retain the exact request/profile pair. RFC-0016
continues to own task, execution-policy, capability, timeout, and absent-outbound-policy
admission. RFC-0019 through RFC-0027 continue to own exact Scheduler preparation,
closed fake suitability and invocation, typed execution, provenance, retry/recovery,
submission, and supported local ingress.

RFC-0028 does not change any of those types or paths. It defines only the missing
outbound route-policy continuation for a future remote proposal. The existing
deterministic-fake route remains local, closed, and unchanged.

## Why Route Admission Precedes Routing

A Router chooses among candidates; it cannot be allowed to manufacture the authority
that makes a candidate eligible. A provider adapter knows wire details; it cannot
decide whether repository content may leave the governed boundary. A profile describes
requirements; it cannot approve disclosure or spend. An endpoint, provider label,
credential, free-looking price, or absence of an obvious secret is likewise not
authority.

Every remote proposal must therefore be admitted before it can enter future Router
selection. The decision is exact to one candidate-destination pair and one invocation
attempt. Candidate ordering, fallback, or cache state cannot bypass it.

## Exact Fresh Authority Inputs

The later evaluator receives and retains these exact required values:

1. the active `ApprovedTask`;
2. the active `ExecutionPolicy` that would govern `model-invoke`;
3. one complete `ProfiledModelRequest`;
4. the unchanged authoritative required-capability projection from the governed caller;
5. one invocation-local outbound route proposal containing the independently governed
   facts defined below; and
6. one trusted evaluation time used only for bounded fact freshness.

Every input is mandatory. Null is a programming error rather than a policy refusal.
The evaluator performs no ambient lookup, registry discovery, filesystem or network
access, credential resolution, pricing acquisition, content transformation,
persistence, Tool call, or gateway call.

The task, policy, profiled request, and capability must be freshly resolved for the
same attempt. Boolean projections, raw allow sets, task text, Tool arguments, profile
fields, candidate metadata, repository content, environment variables, and prior
admission results cannot replace those authority objects.

## RFC-0016 Guarded Continuation

Evaluation first applies RFC-0016 to the exact first four authority inputs. Its closed
order and meaning remain unchanged:

- `TASK_TOOL_NOT_ALLOWED`, `EXECUTION_POLICY_TOOL_NOT_ALLOWED`,
  `REQUIRED_CAPABILITY_MISMATCH`, and
  `GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY` stop immediately with the corresponding
  RFC-0028 refusal;
- an RFC-0016 `Admitted` result means the profile is `LOCAL_ONLY`; an outbound proposal
  refuses with `PROFILE_REQUIRES_LOCAL_EXECUTION`; and
- only `Rejected(OUTBOUND_POLICY_REQUIRED)` may continue to the outbound predicates.

This continuation does not override, convert, or reinterpret the RFC-0016 rejection as
remote authority. It proves only that every earlier RFC-0016 predicate passed and that
the profile explicitly requires a separately governed outbound decision. A later
implementation should invoke the existing evaluator and inspect its typed result rather
than duplicate a drifting copy of those predicates.

## Invocation-Local Outbound Route Proposal

The proposal is one immutable, non-persistent aggregate retaining exact instances of
the following values. Its constructor validates their intrinsic shapes and cross-
identities but grants no authority.

### Candidate Facts

`ModelRouteCandidateFacts` is opaque and can be created only by a separately governed
candidate source. It retains:

- a bounded stable candidate identity and fact-source identity;
- a source revision, observation time, and exclusive expiry time;
- exact provider-neutral model class and required capability;
- a structurally proven `REMOTE` route kind; and
- the exact destination identity to which the candidate facts apply.

It contains no gateway, adapter, provider model name, URI, endpoint, credential,
secret, price, mutable availability flag, invocation method, or caller-supplied
`isLocal` Boolean. A local candidate never enters this outbound boundary. Process-
local, host-local, LAN, private cloud, absent credentials, and a local-looking label are
not interchangeable locality proofs.

These facts are route inputs only. They do not replace candidate suitability for
reasoning, context, token, or actual-request budgets. The future Router must require
both the candidate's separately accepted suitability evidence and RFC-0028 eligibility.

### Destination Policy Facts

`ModelDestinationPolicyFacts` is opaque and comes from a separately governed outbound
policy source. It retains a bounded destination identity, policy-source identity and
revision, observation and exclusive-expiry times, and a finite set of exact allow rules.
An empty rule set is valid and denies every route.

Each allow rule binds exactly:

- candidate identity;
- destination identity;
- the closed `MODEL_INFERENCE` purpose;
- permitted retention uses and maximum retention duration;
- maximum model-visible data classification; and
- an accepted pricing-fact source identity.

There is no wildcard, default destination, default purpose, fallback rule, substring or
prefix match, unknown-value fallback, or caller extension. A later vocabulary change
requires another accepted contract. Policy facts identify a governed destination/trust
boundary; they do not contain an endpoint or credential and do not authorize a network
connection by themselves.

### Purpose And Retention Facts

The first closed purpose vocabulary contains only `MODEL_INFERENCE`. Free-form purpose
text is diagnostic data at most and cannot select a rule.

Retention facts describe actual destination behavior as a finite set drawn from
`REQUEST_PROCESSING`, `ABUSE_MONITORING`, `SERVICE_LOGGING`, `HUMAN_REVIEW`,
`MODEL_TRAINING`, and `CACHE`, plus one bounded maximum duration. No retention is the
empty set with zero duration. Unknown, undisclosed, unbounded, contradictory, or
partially known behavior refuses.

Admission requires actual uses to be a subset of the exact allow rule and actual
duration not to exceed its maximum. A destination promise, provider marketing label,
or profile field cannot replace independently governed retention facts.

## Exact Model-Visible Content Projection

Remote eligibility applies to the complete content a future adapter could make visible
to the model, not merely a convenient substring. The first projection permits exactly
one bounded outbound prompt and forbids hidden system content, metadata, attachments,
Tool material, or adapter-added instructions. Adding another model-visible field
requires a new version and complete inspection coverage.

`ModelOutboundContent` retains:

- the lowercase SHA-256 of the exact source `ModelRequest.prompt()`;
- one exact bounded outbound prompt snapshot;
- its independently recomputed lowercase SHA-256; and
- the closed transformation status `UNCHANGED` or `REDACTED`.

For `UNCHANGED`, the outbound prompt must equal the source prompt exactly. For
`REDACTED`, the source and outbound identities must differ and the separate redaction
facts below must bind them. The value never mutates or claims to replace the existing
`ModelRequest`; it is a distinct invocation-local outbound projection. The current
same-request deterministic-fake chain remains unchanged. A later remote request/budget
composition must explicitly consume this exact projection before an adapter can exist.

## Inspection, Redaction, And Effective Classification

Every remote proposal requires independently governed inspection facts for the exact
source prompt and the exact outbound prompt. Each inspection retains the scanner and
ruleset identities and versions, observation time, inspected content digest, derived
classification, bounded finding coverage, and a closed `CLEAR`, `REDACTION_REQUIRED`,
or `FAILED` status. Unavailable, expired, partial, failed, differently bound, or
unknown inspection refuses. Diagnostics and refusal rendering never reproduce content
or findings.

If the source inspection is `CLEAR`, the outbound projection must be `UNCHANGED` and
the outbound inspection must also be `CLEAR`. If the source inspection requires
redaction, the projection must be `REDACTED` and exact redaction facts must bind source
digest, outbound digest, redactor/ruleset identity and version, and complete coverage
of every required finding. The redacted output must then receive a fresh `CLEAR`
inspection. Missing coverage, ad hoc adapter transformation, or a stale rescan refuses.

Redaction does not itself lower classification. For unchanged content, effective
classification is the maximum of the profile, source-inspection, and outbound-
inspection classifications. For redacted content, only a complete redaction plus the
fresh independent outbound classification may replace the source classification;
effective classification is then the maximum of the profile and outbound
classification. Thus profile data cannot lower observed sensitivity and a redactor
cannot self-certify a downgrade.

The effective classification must not exceed the exact destination allow rule. Secret
or sensitive findings never become permissible merely because a route is local-looking,
free, short-lived, or otherwise suitable.

## Pricing Facts And Spend Separation

`ModelRoutePricingFacts` comes from the exact source identity named by the destination
rule and retains source revision, observation and exclusive-expiry times, exact
candidate/destination/model-class binding, token-semantics and price-schedule identity,
currency, and one checked conservative worst-case charge in integer microunits for the
profile's complete input/output/total budget basis.

Missing, stale, differently bound, unsupported, overflowed, negative, unbounded, or
ambiguous pricing refuses. Currency must exactly equal
`ModelExecutionProfile.costBudget.currencyCode`; RFC-0028 performs no conversion. The
worst-case charge must not exceed the profile ceiling.

Passing that ceiling is still not spend authority. RFC-0028 accepts no spend grant, so
any positive worst-case charge returns `SPEND_AUTHORITY_REQUIRED` even when it is below
the ceiling. Only an independently proven zero-charge route can pass the first pure
contract. A later spend-authorization contract must be separately approved and bind at
least task, candidate, destination, purpose, currency, maximum amount, attempt, and
expiry; it cannot be inferred from this RFC or added as an ambient input.

## Closed Deterministic Decision

`ModelOutboundRouteAdmissionDecision` has exactly two shapes:

```text
Eligible(exact proposal, effective classification)
Refused(one closed reason)
```

`Eligible` retains the exact proposal input instances, exact outbound content
projection, derived effective classification, and all bounded provenance/freshness
metadata. `Refused` retains only one reason. Neither rendering exposes prompt content,
findings, paths, endpoints, prices beyond approved bounded summaries, or source errors.

After programming-error checks and the RFC-0016 guarded continuation, evaluation
returns the first matching reason in this repository order:

1. `TASK_TOOL_NOT_ALLOWED`;
2. `EXECUTION_POLICY_TOOL_NOT_ALLOWED`;
3. `REQUIRED_CAPABILITY_MISMATCH`;
4. `GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY`;
5. `PROFILE_REQUIRES_LOCAL_EXECUTION`;
6. `CANDIDATE_FACTS_INVALID_OR_STALE`;
7. `DESTINATION_FACTS_INVALID_OR_STALE`;
8. `CANDIDATE_DESTINATION_MISMATCH`;
9. `CANDIDATE_MODEL_CLASS_MISMATCH`;
10. `CANDIDATE_CAPABILITY_MISMATCH`;
11. `ROUTE_NOT_ALLOWED`;
12. `PURPOSE_NOT_ALLOWED`;
13. `RETENTION_NOT_ALLOWED`;
14. `CONTENT_BINDING_MISMATCH`;
15. `CONTENT_INSPECTION_REQUIRED`;
16. `CONTENT_INSPECTION_FAILED`;
17. `REDACTION_REQUIRED`;
18. `REDACTION_INVALID_OR_INCOMPLETE`;
19. `EFFECTIVE_CLASSIFICATION_NOT_ALLOWED`;
20. `PRICING_FACTS_REQUIRED`;
21. `PRICING_FACTS_INVALID_OR_STALE`;
22. `COST_CURRENCY_MISMATCH`;
23. `COST_CEILING_EXCEEDED`;
24. `SPEND_AUTHORITY_REQUIRED`.

Unknown enum values, absent facts, equality ambiguity, expiry equality, arithmetic
uncertainty, and unsupported future versions refuse at the earliest applicable reason.
Facts are fresh only when `observedAt <= evaluationTime < expiresAt`; equality at
expiry is stale. Authority and provenance checks precede content processing, and
classification precedes economics so price can never legitimize disclosure.

## Meaning And Lifetime Of Eligible

`Eligible` means only that one exact remote proposal passed the pure policy predicates
at one trusted evaluation time. It is not a route selection, gateway permission,
credential, network capability, spend grant, provider approval, Tool result, evidence,
RunRecord, retry decision, or durable receipt.

It is invocation-local and must not be serialized, persisted, cached, replayed,
transmitted, reconstructed from identifiers, or treated as a reusable token. It
contains no gateway, adapter, credential supplier, endpoint, invocation method, or
mutable authorization callback.

Before durable completion, every retry, fallback, candidate change, destination change,
content transformation, policy revision, expiry, or pricing revision requires fresh
resolution and a new decision. After a future exact durable result reference exists,
recovery may validate retained historical provenance and finish deterministic
finalization but must not reroute, reacquire eligibility, or retransmit. Durable route
provenance requires a separate compatibility and schema decision; current Model
RunRecord v2 is unchanged.

## Local Behavior And Fallback

RFC-0028 is an outbound boundary. The existing closed deterministic fake continues
through RFC-0016 local admission, RFC-0020/0021 suitability, RFC-0022 exact-request
budgeting, and RFC-0023 execution without passing through RFC-0028.

`LOCAL_ONLY` never reaches remote evaluation. `POLICY_CONSTRAINED` is not remote
permission; it merely permits the exact guarded continuation described above. A future
local candidate still needs its own structural locality and suitability proof and does
not become remote merely because it participates in a Router.

Fallback never inherits eligibility. Each candidate-destination proposal must be
evaluated separately and freshly, and fallback cannot silently cross locality,
destination, purpose, retention, classification, freshness, currency, price, or task/
policy boundaries.

## Exact Future Consumer

The first named consumer is a separately authorized pure `ModelRouter`. It may accept
only complete `Eligible` instances plus separately proven candidate suitability and
may return only an identity-preserving `SelectedModelRoute` retaining the exact
eligibility and candidate/destination facts.

The Router may not accept a raw profile or request as a substitute, reconstruct facts,
add a candidate, discover a destination, acquire a credential, construct or invoke an
adapter, open a network connection, authorize spend, mutate content, or persist its
selection. Provider construction and invocation require later contracts after routing,
credential, external-effect, response-validation, provenance, retry, and recovery
boundaries are accepted.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add the closed provider-neutral proposal values, versions, bounds, cross-bindings,
   and refusal/eligible algebra under missing-symbol RED tests, with no production
   source or caller;
2. add the field-free evaluator and prove exact RFC-0016 guarded continuation, fact
   freshness, default-deny route rules, purpose/retention, content/redaction,
   classification, pricing, and spend-refusal order;
3. add reflection/source/locality guards proving the values and evaluator contain no
   gateway, adapter, endpoint, credential, filesystem, network, persistence, runtime,
   Router, or invocation dependency; and
4. run focused RFC-0014-through-RFC-0028, current deterministic-fake path, architecture,
   source/locality, and full Markdown-sensitive regression.

The pure implementation leaves `Eligible` reachable only through test-owned governed
fact fixtures. Production fact sources, content scanners/redactors, remote preparation,
Router composition, pricing acquisition, spend grants, adapters, and external effects
remain separate accepted tasks.

## Verification Contract

The later pure implementation must cover every closed reason individually and in
combined first-match cases; exact input-instance retention; empty-policy default deny;
strict expiry; model-class/capability/destination binding; no wildcard or fallback;
all retention uses and duration boundaries; unchanged and fully redacted content;
wrong-digest, partial, failed, stale, and unrescanned inspection; conservative
classification; exact currency, ceiling, checked pricing, zero-charge eligibility, and
positive-charge refusal; retry/fallback freshness; redacted rendering; and zero Tool,
gateway, adapter, credential, filesystem, network, persistence, Router, RunRecord,
runtime, or external activity.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Planner/Assisted-Loop/dynamic-workflow governance, `git diff --check`, and the
full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Extending RFC-0016 `Admitted` to mean remote permission is rejected because it would
  change an accepted local-only contract and hide the missing outbound facts.
- Requiring RFC-0016 `Admitted` before remote policy is rejected because
  `POLICY_CONSTRAINED` deliberately cannot produce that subtype.
- A Boolean `remoteAllowed`, `isLocal`, `containsSecrets`, `wasRedacted`, or `isFree`
  is rejected because it detaches a claim from identity, provenance, revision, and
  freshness.
- Caller-provided endpoint, provider name, credential, destination string, purpose
  text, retention promise, classification assertion, or price is rejected as
  self-certified authority.
- Letting a Router evaluate policy after selection is rejected because ordering or
  fallback would then become implicit authority.
- Mutating or rebuilding the existing `ModelRequest` inside admission is rejected
  because it breaks exact-request identity and hides the transmitted content.
- Treating redaction as automatic declassification is rejected; only fresh independent
  inspection of the exact outbound form can establish its classification.
- Treating a profile cost ceiling or below-ceiling quote as spend approval is rejected.
- Persisting or caching eligibility is rejected because authority and every external
  fact may change between attempts.

## Exclusions

- Java or test-source implementation;
- changes to RFC-0013 through RFC-0027 types, callers, local execution, or schemas;
- second candidate, generic suitability, Router/registry implementation, provider
  adapter construction, provider model, endpoint, protocol, network, or remote call;
- credential loading/storage, secret persistence, spend grant, billing, paid service,
  currency conversion, or pricing-feed acquisition;
- scanner/redactor implementation, request mutation, hidden model-visible content,
  prompt-injection defense, response validation, attribution, fallback/cache
  orchestration, or quality evaluation;
- Tool/evidence/RunRecord/runtime-event/finalization/retry/recovery integration, durable
  route provenance, schema, migration, MCP, plugin, interface, or background work; and
- capability promotion, push, merge, release, deployment, permission change,
  destructive cleanup, or another external effect.

## Codex Implementation Prompt

Implement the pure RFC-0028 contract RED-first through the four ordered increments.
Preserve RFC-0016 exactly: only its typed `OUTBOUND_POLICY_REQUIRED` refusal may enter
remote predicates, while every earlier refusal and local admission stop outbound
evaluation. Keep candidate, destination-policy, purpose, retention, exact outbound
content, inspection/redaction, effective classification, and pricing facts explicit,
fresh, identity-bound, and default-deny. Admit only a proven zero-charge proposal and
return ephemeral eligibility with no Router, provider, credential, network,
persistence, spend, or production caller.
