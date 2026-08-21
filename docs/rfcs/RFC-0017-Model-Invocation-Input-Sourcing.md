# RFC-0017: Model Invocation Input Sourcing

Status: Accepted

## Purpose

Define the minimum caller-side source obligations for constructing one complete
provider-neutral model invocation input before RFC-0016 admission. The contract uses
the existing RFC-0013 through RFC-0016 values directly. It does not add a context
aggregate, source registry, parser, transport schema, runtime integration, route,
provider, or gateway permission.

RFC acceptance does not imply capability maturity. Maturity remains owned by
`PROJECT_STATE.md` alone.

## Relationship To Existing Contracts

RFC-0013 continues to own the exact five-component `ModelRequest`, gateway port,
deterministic fake, Tool, current direct CLI, current Scheduler composition, and the
response-character safety stub. RFC-0014 continues to own the complete ten-component
`ModelExecutionProfile`. RFC-0015 continues to own exact request/profile alignment.
RFC-0016 continues to own the pure task, execution-policy, capability, timeout, and
locality admission decision.

This RFC adds no Java value because those contracts already contain every value needed
at the pure boundary. It defines which values a future caller must source, how their
provenance remains separate, and when absence must fail closed.

## Minimum Per-Invocation Source Contract

For one invocation, a future caller must resolve all of these values explicitly:

1. one complete already-valid RFC-0013 `ModelRequest`;
2. one complete already-valid RFC-0014 `ModelExecutionProfile`;
3. the exact active `ApprovedTask` governing that invocation;
4. the exact `ExecutionPolicy` that will govern the same Tool execution; and
5. one authoritative required-capability projection obtained unchanged from a
   separately named governed caller source.

The caller then uses only the existing composition and admission contracts:

```java
ProfiledModelRequest profiledRequest =
        new ProfiledModelRequest(request, executionProfile);

ModelInvocationAdmissionDecision decision = admission.evaluate(
        profiledRequest,
        approvedTask,
        executionPolicy,
        authoritativeRequiredCapability);
```

These steps are invocation-scoped and ordered. The complete inputs must exist before
composition; composition must succeed before admission; admission must be evaluated
fresh before any later candidate-suitability or gateway boundary. A caller cannot
replace any step with a boolean, partial map, inferred value, cached decision, or
ambient lookup.

## Complete Profile Source Obligation

The profile source supplies exactly one already-valid `ModelExecutionProfile` as one
indivisible value. Every RFC-0014 component is explicit:

1. `schemaVersion`;
2. `requiredCapability`;
3. `modelClass`;
4. `localityRequirement`;
5. `reasoningRequirement`;
6. `minimumContextTokens`;
7. the complete `ModelTokenBudget`;
8. the complete `ModelCostBudget`;
9. `maximumInvocationTime`; and
10. `dataClassification`.

The source may not omit, default, infer, normalize, repair, or synthesize any component.
It may not replace the typed value with optional fragments, a generic string map, an
opaque JSON field, Tool arguments, environment variables, a registry lookup, a
repository-wide singleton, or provider configuration. Unknown schema or enum values,
partial budgets, and invalid labels or bounds fail through the existing RFC-0014
validation before composition.

The profile is caller-supplied untrusted requirement data. Successful construction
proves only completeness and intrinsic validity. It creates no task, Tool, execution,
network, destination, provider, credential, transmission, spend, persistence, or
external-effect authority.

## Independent Capability Source Obligation

`authoritativeRequiredCapability` is a source-of-truth projection for the required
capability label within an already governed invocation. It is not a capability grant.
It must originate independently of the profile and be passed unchanged to RFC-0016.

The caller must not:

- copy it from `ModelExecutionProfile.requiredCapability`;
- infer it from `ModelRequest.modelClass` or any provider/model name;
- accept a profile, prompt, Tool argument, environment variable, arbitrary direct-CLI
  string, manifest field, queue file, or serialized envelope as self-authenticating
  authority;
- normalize, translate, alias, repair, or default it; or
- pre-certify equality in a new wrapper that prevents RFC-0016 from returning
  `REQUIRED_CAPABILITY_MISMATCH`.

RFC-0016 remains the owner of exact capability/profile comparison. The independent
source and the profile deliberately remain capable of disagreeing so that admission can
represent the mismatch explicitly.

## Caller-Specific Sources

### Direct CLI

The current direct `model-invoke` CLI owns prompt, model-class, timeout, response-
character limit, and expected-response digest inputs. It resolves an `ApprovedTask` and
constructs an `ExecutionPolicy`, but it owns neither a complete profile source nor a
governed authoritative capability source.

It therefore remains unsupported by RFC-0017. Adding ten profile flags or a
`--required-capability` flag would add arbitrary caller data, not the missing authority
provenance, and would introduce parsing/default ambiguity. This RFC does not change the
command or permit a fallback to its legacy unprofiled path.

### Scheduler

For a future Scheduler integration, the only currently named capability projection is
the exact `requiredCapability` of the active `WorkItem`. It is usable only inside that
same already-governed WorkItem, task, and execution-policy lifecycle. `WorkItem` itself
creates no authority, and a submission manifest, retained envelope, queue artifact, or
runtime record proves retention rather than authority.

The Scheduler currently has no complete profile source. Its model path also temporarily
copies `WorkItem.requiredCapability` into `ModelRequest.modelClass`. RFC-0017 neither
accepts nor legitimizes that conflation. A future caller must preserve two independent
relationships:

```text
active WorkItem.requiredCapability
    == ModelExecutionProfile.requiredCapability

ModelRequest.modelClass
    == ModelExecutionProfile.modelClass
```

The first relationship is evaluated by RFC-0016. The second is enforced by RFC-0015.
Neither label may source the other.

The existing `WorkPayload.ExecutionInput` contains only a target path and expected
digest. It is governed work-input provenance, not a complete model profile or model
capability authority source. The current Scheduler remains unsupported by RFC-0017
until a separately accepted caller-specific source and transport contract supplies the
complete profile without defaults.

## Request And Profile Composition

The caller supplies the exact request and exact profile to `ProfiledModelRequest`.
Construction retains both unchanged and enforces:

```text
request.modelClass == profile.modelClass

profile.maximumInvocationTime <= request.timeout
```

RFC-0016 then enforces the strict execution-policy relationship:

```text
profile.maximumInvocationTime
    <= request.timeout
    < executionPolicy.timeout
```

The profile must not be flattened into the current `ToolRequest` string map. The
request's response-character ceiling remains independent from profile token budgets;
neither is derived from or converted to the other. A cost ceiling remains a requirement
and not spend approval. Classification remains untrusted caller data and not disclosure
permission.

## Failure And Absence Semantics

Missing, partial, invalid, unresolved, or ambiguously sourced profile or capability
input fails closed before `ProfiledModelRequest`, admission, Tool execution, gateway,
adapter, network, credential, paid service, or other external effect.

Failure ownership remains precise:

- invalid or incomplete profile values fail existing RFC-0014 construction;
- request/profile model-class or time misalignment fails existing RFC-0015
  construction;
- a missing independently governed source prevents the caller from invoking admission;
- capability disagreement and the other ordinary authority/policy conditions return
  the existing RFC-0016 typed rejection; and
- null references remain caller programming errors rather than policy rejections.

There is no fallback to constants, a legacy request, another caller, a registry,
provider metadata, or a previously admitted decision.

## Invocation Lifecycle

RFC-0017 creates no new identity, durable aggregate, authorization token, or replay
contract. A retry or re-entry must resolve the applicable governed caller sources and
perform fresh RFC-0015 composition and RFC-0016 admission for the same invocation
boundary.

An RFC-0016 `Admitted` decision remains ephemeral and must not be cached, persisted,
serialized, transmitted, replayed, or presented as reusable gateway authority. This
RFC does not authorize persistence of profiles either. A future persisted profile source
would remain untrusted data and requires its own provenance, versioning, integrity,
migration, replay, and recovery contract.

## Compatibility And Durable Boundaries

This specification changes none of the following:

- `ModelRequest`, `ProfiledModelRequest`, `ModelInvocationAdmission`, or
  `ModelGateway`;
- `ToolRequest` arguments or `ModelInvokeTool`;
- direct CLI commands or defaults;
- `WorkItem` or `WorkPayload`;
- submission request or manifest schemas;
- message-envelope or transport-spool formats;
- Scheduler queue or AgentRuntime schemas;
- RunRecord or evidence formats; or
- deterministic fake or provider-adapter behavior.

Current model callers remain behavior-compatible and unchanged, but they are not
RFC-0016/RFC-0017-integrated callers.

Adding a complete profile to durable Scheduler work is not a schema-free Java change.
Execution and recovery consume the retained WorkItem through the message envelope,
submission manifest, queue, and AgentRuntime stores. A future design must version and
verify every affected representation together or name an equally durable single source
that recovery can resolve without ambient cross-store lookup. Manifest-only profile
storage is insufficient. Existing artifacts without a profile cannot be upgraded by
inventing one.

Any such source, serialization, schema, migration, recovery, cutover, and compatibility
work requires a separate accepted RFC and Active Task.

## Authority Non-Expansion

The effective execution boundary remains the intersection of the accepted task, exact
active execution policy, complete untrusted profile, authoritative capability
projection, and any later accepted outbound/provider policy. Any participant may narrow
or reject; none may widen another.

RFC-0017 does not select or prove a model candidate, local gateway, route, provider,
endpoint, destination, credential, tokenizer, price, currency conversion, network
permission, transmission purpose, retention policy, spend approval, or external effect.
It does not evaluate reasoning, context, tokens, cost, or classification against a
candidate. `LOCAL_ONLY` admission remains local eligibility only, while
`POLICY_CONSTRAINED` still requires a later outbound policy.

## Rejected Alternatives

- A new `ModelInvocationInput` or admission-context record is rejected because it
  duplicates existing values, proves no source provenance, adds storage/replay surface,
  and introduces no new invariant.
- A generic `ModelInvocationInputSource` port, registry, factory, service locator, or
  ambient context is rejected because an abstraction name cannot create caller
  authority and could hide defaults or lookup.
- Re-listing the ten profile components in a record, map, CLI flags, Tool arguments, or
  opaque serialized blob is rejected because it duplicates RFC-0014 and creates
  partial/parsing/default ambiguity.
- Enforcing capability/profile equality in a new source wrapper is rejected because it
  allows untrusted profile data to self-certify and removes RFC-0016's typed mismatch.
- Extending `ModelRequest`, `ModelGateway`, or the admission decision is rejected by
  existing compatibility and authority boundaries.
- Passing `WorkItem` into the model package is rejected because it reverses the
  `runtime -> model` dependency direction.
- Adding a profile to `WorkPayload.ExecutionInput`, `WorkItem`, manifests, or current
  schemas in this increment is rejected as premature unversioned integration.
- A profile stored only in a manifest, repository file, environment variable, registry,
  or provider configuration is rejected as an ambient or incomplete recovery source.
- Persisting or replaying `Admitted` is rejected because the decision is not a token.

## Follow-Up Contract And Verification

There is no generic Java implementation increment for RFC-0017. Its existing pure
consumers already exist. The next separately authorized design should define one
caller-specific complete-profile source. If the Scheduler is selected first, that work
must specify the typed model work input, every affected message/submission/queue/runtime
schema version, migration and no-default behavior, recovery source, cutover, and
authority-preserving projection before implementation.

Future caller-specific RED-first verification must cover:

- retention of all ten exact profile components without defaults;
- refusal of missing, partial, unknown, fallback, registry, or ambient input;
- independent authoritative capability provenance and unchanged projection;
- explicit capability mismatch reaching RFC-0016 rather than being self-certified;
- exact request/profile alignment and strict execution-policy timing;
- fresh admission on retry or re-entry;
- unchanged unsupported legacy callers and schemas until their explicit migration; and
- structural guards against provider, route, endpoint, destination, credential,
  tokenizer, price, network, transmission, persistence-token, or gateway authority.

For this documentation-only acceptance increment, architecture, decision, RFC index,
document-ownership, dynamic-workflow, approved-task, task-justification, and repository-
planning governance plus `git diff --check` are the verification boundary.

## Exclusions

- Java implementation or new input/source/context types;
- changes to request, profile, admission, Tool, CLI, Scheduler, gateway, fake, adapter,
  message, submission, queue, runtime, spool, evidence, or RunRecord source;
- profile parsing, serialization, persistence, registry, defaults, fallback, inference,
  ambient lookup, migrations, recovery, or caller cutover;
- model suitability, routing, provider selection, fallback, retry policy, caching, or
  streaming;
- endpoint or destination policy, network calls, remote execution or transmission,
  credentials, paid-service use, pricing, currency conversion, tokenizers, or usage
  normalization;
- MCP, plugin protocols, prompt scanning, classification inference, redaction,
  prompt-injection resistance, attribution, quality evaluation, capability maturity,
  operational readiness, release, or deployment.

## Prompt Book

### Prompt: Specify A Scheduler Complete-Profile Source

Define, but do not implement, the smallest typed and versioned Scheduler model-work
input that retains one complete RFC-0014 profile through message, submission, queue,
runtime, recovery, and retry boundaries while keeping active
`WorkItem.requiredCapability` as the independent RFC-0016 projection and inventing no
defaults.

### Prompt: Review Source And Authority Separation

Verify that the profile remains untrusted complete requirement data, capability comes
unchanged from an independently governed active-caller source, no persisted value or
CLI string self-authenticates, and every absence fails before admission or gateway.
