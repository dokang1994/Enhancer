# RFC-0014: Model Execution Profile

Status: Accepted

## Purpose

Define the provider-neutral requirement value that a future model-routing boundary can
evaluate before a request reaches the model gateway. The profile describes what an
invocation requires; it does not select a provider, authorize network transmission,
grant credential access, approve spend, or expand task or Tool authority.

Capability maturity remains owned by `PROJECT_STATE.md`. Acceptance of this contract
does not claim an integrated router, provider adapter, remote invocation, or operational
model execution.

## Relationship To RFC-0013

RFC-0013 continues to own the current minimum model-gateway port, `ModelRequest`,
`ModelResponse`, `ModelUsage`, deterministic fake, CLI adapter, Tool integration, and
its timeout and response-length safety stub. This RFC adds a separate requirement
value and does not rewrite that accepted contract.

The first implementation increment creates immutable values and tests only. It does
not change `ModelRequest`, `ModelGateway`, the CLI, Scheduler, command schemas, or
persistence. A later separately authorized composition increment must pass a complete
profile explicitly; it may not invent defaults. Routing and outbound policy remain
later contracts.

## Contract

`ModelExecutionProfile` is an immutable value with exactly these components:

1. `schemaVersion`, fixed to `model-execution-profile-v1`;
2. `requiredCapability`;
3. `modelClass`;
4. `localityRequirement`;
5. `reasoningRequirement`;
6. `minimumContextTokens`;
7. `tokenBudget`;
8. `costBudget`;
9. `maximumInvocationTime`; and
10. `dataClassification`.

Equality and hashing cover every component. The value contains no prompt, response,
task, Tool, provider, endpoint, destination, credential, price-table, tokenizer, route,
or result data.

### Capability And Model Class

`requiredCapability` and `modelClass` are distinct requirements. A capability names
the behavior required by the caller; a model class names a provider-neutral class of
model that may satisfy it. The current Scheduler's temporary use of `modelClass` as a
capability label must not become this contract.

Both labels use lower-case ASCII letters, digits, and single hyphens between non-empty
segments. `requiredCapability` is at most 256 characters and `modelClass` is at most 64
characters. Blank, leading-hyphen, trailing-hyphen, repeated-hyphen, upper-case, and
non-ASCII values fail closed.

### Locality

`localityRequirement` uses the closed vocabulary:

- `LOCAL_ONLY`: execution and model-visible data must remain on the governed local
  boundary;
- `POLICY_CONSTRAINED`: a later outbound policy may evaluate destinations, but this
  value does not itself authorize remote execution or transmission.

There is deliberately no `REMOTE_ALLOWED` value. Unknown values fail closed.

### Reasoning

`reasoningRequirement` uses the closed repository-ordered vocabulary `MINIMAL`,
`STANDARD`, and `EXTENDED`. These are requirements, not provider option names. A future
adapter may translate an already-approved requirement but may not weaken it. Unknown
values fail closed.

### Context And Token Budgets

`minimumContextTokens` and every token-budget component are positive integers no
greater than 1,000,000,000.

`ModelTokenBudget` contains `maxInputTokens`, `maxOutputTokens`, and `maxTotalTokens`.
Validation is overflow-safe and requires:

```text
maxInputTokens + maxOutputTokens <= maxTotalTokens <= minimumContextTokens
```

These are token requirements and limits. They are distinct from RFC-0013's
`maxResponseLength`, which is currently a character-count safety bound. The current
deterministic fake's `ModelUsage` values are character-based test accounting, not a
provider-token contract. A provider adapter must define tokenizer and usage semantics
under a later accepted contract.

### Cost Budget

`ModelCostBudget` contains an upper-case three-letter ISO-style `currencyCode` and
`maxMicrounits`, an integer from zero through 1,000,000,000,000,000 inclusive. One
microunit is one millionth of the named currency unit. Floating-point values are not
used.

A zero budget means free-only. A positive budget is only a ceiling requirement; it is
not spend authority. Values in different currencies cannot be compared without a later
accepted pricing and conversion contract.

### Time Budget

`maximumInvocationTime` is positive, representable at millisecond precision, and no
greater than five minutes. Sub-millisecond, zero, negative, overflowed, or larger
durations fail closed.

When a later composition contract connects the profile to the existing Tool path, it
must preserve:

```text
profile maximumInvocationTime <= gateway timeout < Tool execution-policy timeout
```

This field does not own queue, retry, workflow, or end-to-end time budgets.

### Data Classification

`dataClassification` uses the closed ordered vocabulary `PUBLIC`, `INTERNAL`,
`CONFIDENTIAL`, and `RESTRICTED`. The value is supplied by an already-approved caller;
the profile performs no content scanning or classification inference. A consumer may
raise the effective classification or reject the request, but may not lower the actual
classification. Unknown values fail closed.

## Validation And Defaults

Every component is required. There are no implicit defaults, nullable components,
unknown enum fallbacks, or permissive parsing. Construction rejects invalid values
before they reach a routing or gateway boundary.

## Authority And Policy Intersection

The profile is untrusted requirement data, not authority. It cannot add a Tool, select
a provider, open a network destination, expose credentials, approve paid use, or
authorize external effects. Effective execution constraints are the intersection of:

1. the accepted task contract;
2. the active `ExecutionPolicy`;
3. this profile; and
4. any later accepted outbound/provider policy.

A consumer may narrow or reject that intersection. It may never widen it because a
profile requests a capability, locality, reasoning level, budget, or classification.

## First Implementation Increment

The immediate follow-up adds only pure immutable values under the model package and
their tests. RED begins with missing profile symbols. GREEN covers:

- complete retention, equality, and hashing;
- fixed schema version and closed vocabularies;
- capability and model-class label validation;
- overflow-safe token relationships and limits;
- cost and duration boundaries; and
- a reflection guard against forbidden routing, provider, network, credential, prompt,
  response, and result fields.

Existing RFC-0013 gateway, Scheduler, Tool, timeout, and architecture tests remain
regression coverage. This increment can establish Contract Verified maturity at most;
it cannot establish integration, operational readiness, or release.

## Exclusions

- changes to `ModelRequest`, `ModelGateway`, Scheduler, CLI, or Tool signatures in the
  value-only increment;
- provider selection, fallback, retry, queues, or routing policy;
- provider adapters, network calls, credentials, endpoint allowlists, or remote data
  transmission;
- pricing feeds, currency conversion, tokenizers, or provider usage normalization;
- MCP transport, plugin protocol, multi-agent execution, persistence schemas, or
  migrations;
- capability-maturity, operational-readiness, or release claims.

## Prompt Book

### Prompt: Implement The Pure Value Layer

Implement the accepted RFC-0014 value objects and RED-first tests without changing the
existing gateway, Scheduler, CLI, Tool, or persistence contracts.

### Prompt: Review Authority Non-Expansion

Review the implementation for any default, field, vocabulary, or adapter behavior that
could select a provider, authorize transmission or spend, weaken classification, or
expand task or Tool authority.

### Prompt: Prepare A Later Composition Proposal

Propose, but do not implement, the smallest separately authorized contract that can
attach a complete `ModelExecutionProfile` to `ModelRequest` while preserving RFC-0013
compatibility and the required policy intersection.
