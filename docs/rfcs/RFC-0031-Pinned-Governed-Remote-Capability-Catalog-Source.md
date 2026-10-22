# RFC-0031: Pinned Governed Remote Capability Catalog Source

Status: Accepted

## Purpose

Define the first provider-neutral production acquisition contract for one exact
`RemoteModelCandidateCapabilityFacts` value. The future source reads a bounded,
canonical local catalog whose absolute path and whole-file SHA-256 digest were
independently provisioned, resolves one exact candidate/destination record, checks its
freshness at an explicit time, and returns either the RFC-0030 facts or one closed
refusal reason.

The digest proves which exact catalog bytes the operator approved. It does not prove
that the catalog's remote-world claims are objectively true, current beyond their
declared window, or provider-attested. Truthful production use therefore also depends
on an out-of-scope governed provisioner establishing every capability value from
independent evidence.

This RFC specifies a later implementation. It does not provision or rotate a catalog,
construct a provider or adapter, access a credential, use a network, authorize spend,
evaluate suitability, construct a route option, invoke a model, persist facts, or
change capability maturity.

## Relationship To Existing Contracts

RFC-0014 and RFC-0015 continue to own the complete untrusted invocation requirements
and exact profiled request. RFC-0016 owns fresh task, policy, capability, timeout, and
local-versus-outbound admission. RFC-0028 remains the sole outbound policy authority
and owns the exact `Eligible`, proposal, candidate, destination, content, inspection,
redaction, classification, and pricing facts. RFC-0030 remains the sole suitability
evaluator, and RFC-0029 remains the sole Router.

RFC-0031 supplies only independently pinned capability facts for an exact candidate
and destination. It neither recreates eligibility nor infers a provider, endpoint, or
route. Its later same-invocation connection is strictly:

```text
exact RFC-0028 Eligible
-> exact candidate and destination selectors
-> RFC-0031 acquisition at evaluationTime
-> RFC-0030 evaluation with the same request, Eligible, facts, and evaluationTime
-> RFC-0029 route-option construction
```

Selectors are untrusted lookup keys only. A successful acquisition grants no provider,
credential, network, transmission, spend, execution, persistence, retry, fallback, or
Router-caller authority. The deterministic fake continues to bypass RFC-0028 through
RFC-0031 unchanged.

## Source Boundary And Trusted Inputs

The future final source is conceptually named
`PinnedFileRemoteModelCandidateCapabilityFactSource`. Its only trusted construction
inputs are:

- one absolute normalized catalog path whose exact real location was selected by
  protected external composition; and
- one independently provisioned lowercase 64-character whole-file SHA-256 pin.

Null inputs, a relative or non-normalized path, or a malformed pin are programming or
composition errors and fail construction. The path and pin must not come from request
content, a profiled request, eligibility, candidate or provider metadata, repository
content, a working directory, CLI fields, environment variables, JVM properties,
filesystem discovery, or another ambient identity. No such input can override them.

The pin is the approved snapshot identity and becomes the facts' `sourceRevision`.
The catalog cannot declare or replace its own revision. Giving an attacker control of
both path and pin is self-approval, not independent governance, so production
composition must protect them together outside this source contract.

Acquisition receives exactly:

```text
String candidateIdentity
String destinationIdentity
Instant evaluationTime
```

All are required. Null or intrinsically malformed selectors and a null evaluation time
are programming errors. Selectors use the repository's bounded stable lowercase-label
grammar and are compared case-sensitively. The source performs no clock read; the same
explicit `evaluationTime` is later passed unchanged to RFC-0030.

## Defensive Snapshot Acquisition

Every acquisition rereads exactly one snapshot. There is no cache, search, repair,
rewrite, alternate directory, latest-file convention, registry, fallback, or grace
period. The implementation must:

1. require an exact-real parent directory and exact-real regular catalog file;
2. reject symbolic links, junction or reparse indirection, and non-regular files;
3. open the exact path for one no-follow read and require a size from 1 byte through
   4 MiB;
4. read exactly the declared byte count, detect early EOF, extra bytes, or size drift,
   and never reopen the file during the acquisition;
5. calculate SHA-256 over that byte array and compare it in constant time with the
   independently supplied pin before parsing;
6. strictly decode, parse, validate, and canonicalize the same byte array; and
7. map a selected record only after the entire catalog is valid.

The digest converts a concurrent replacement into refusal unless the resulting bytes
are exactly the approved snapshot. It does not assert stronger filesystem immutability
or atomicity. Hashing, parsing, validation, lookup, and mapping must all use the single
defensive byte snapshot.

## Canonical Catalog Grammar

The v1 catalog is strict UTF-8 without a BOM, uses LF only, contains no blank lines or
comments, and ends with exactly one LF. It has no whitespace normalization, escaping,
unknown fields, omitted fields, alternate field order, sparse indices, or trailing
content. Its exact line grammar is:

```text
enhancer-remote-model-candidate-capability-catalog-v1
catalogIdentity=<stable-label>
recordCount=<canonical-count-0-through-256>
record.0.schemaVersion=remote-model-candidate-capability-facts-v1
record.0.capacityEvidenceIdentity=<stable-label>
record.0.observedAt=<canonical-Instant>
record.0.expiresAt=<canonical-Instant>
record.0.candidateIdentity=<stable-label>
record.0.destinationIdentity=<stable-label>
record.0.modelClass=<stable-model-class>
record.0.requiredCapability=<stable-capability>
record.0.tokenSemanticsIdentity=profile-token-budget-v1
record.0.priceScheduleIdentity=<stable-label>
record.0.tokenBudgetBasis.maxInputTokens=<canonical-positive-long>
record.0.tokenBudgetBasis.maxOutputTokens=<canonical-positive-long>
record.0.tokenBudgetBasis.maxTotalTokens=<canonical-positive-long>
record.0.maximumReasoningRequirement=<MINIMAL|STANDARD|EXTENDED>
record.0.maximumContextTokens=<canonical-positive-long>
record.0.maximumTokenBudget.maxInputTokens=<canonical-positive-long>
record.0.maximumTokenBudget.maxOutputTokens=<canonical-positive-long>
record.0.maximumTokenBudget.maxTotalTokens=<canonical-positive-long>
record.0.maximumInvocationTimeMillis=<canonical-positive-long>
record.0.costCurrencyCode=<uppercase-three-letter-code>
record.0.maximumCostMicrounits=<canonical-nonnegative-long>
record.0.maximumDataClassification=<PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED>
```

The complete record block repeats for every contiguous index from zero through
`recordCount - 1`. A zero-record catalog contains only the header fields and is valid;
every lookup against it returns `RECORD_NOT_FOUND`.

Canonical integers are `0` or a nonzero decimal digit followed by decimal digits, with
no sign or leading zero. Positive fields additionally reject zero. An instant is
canonical only when `Instant.parse(value).toString()` equals the original value. After
typed parsing, the implementation re-encodes the complete catalog and requires exact
byte equality with the read snapshot.

Every label except model class uses the repository stable lowercase hyphenated-label
grammar and is at most 256 characters; model class uses the same grammar and is at most
64 characters. Records are strictly ordered by ASCII `candidateIdentity`, then ASCII
`destinationIdentity`. Each pair is unique. Duplicate, unsorted, malformed, or
semantically invalid records invalidate the whole snapshot even when they were not the
requested record; first-wins or selected-row-only validation is forbidden.

## Complete Field Mapping And Validation

One selected record maps every RFC-0030 facts field without defaults or conversion:

| `RemoteModelCandidateCapabilityFacts` field | Authoritative source |
| --- | --- |
| `schemaVersion` | exact `record.i.schemaVersion` constant |
| `factSourceIdentity` | snapshot `catalogIdentity` |
| `sourceRevision` | verified whole-file SHA-256 pin |
| `capacityEvidenceIdentity` | `record.i.capacityEvidenceIdentity` |
| `observedAt`, `expiresAt` | matching record fields |
| `candidateIdentity`, `destinationIdentity` | matching record fields, equal to both selectors |
| `modelClass`, `requiredCapability` | matching record fields |
| `tokenSemanticsIdentity` | exact `profile-token-budget-v1` record constant |
| `priceScheduleIdentity` | matching record field |
| `tokenBudgetBasis` | its three matching record components |
| `maximumReasoningRequirement` | exact closed enum record value |
| `maximumContextTokens` | matching record field |
| `maximumTokenBudget` | its three matching record components |
| `maximumInvocationTime` | `Duration.ofMillis(maximumInvocationTimeMillis)` |
| `costCurrencyCode`, `maximumCostMicrounits` | matching record fields |
| `maximumDataClassification` | exact closed enum record value |

All current value bounds and intrinsic relationships apply. Context and token values
are positive and at most `1_000_000_000`. Each token budget is overflow-safe and
requires input plus output not to exceed total. Both token-budget totals must not
exceed `maximumContextTokens`, and every cost-basis component must not exceed the
matching maximum-capacity component. Invocation time is 1 through 300,000 milliseconds
with millisecond precision. Cost is 0 through `1_000_000_000_000_000` microunits and
currency is exactly three uppercase letters. Observation must precede exclusive
expiry. No caller default, provider assertion, unit conversion, filesystem metadata,
or inferred value may fill a field.

The fixed pair key means one v1 record carries one exact `tokenBudgetBasis`. It can
satisfy only a profile whose budget basis matches under RFC-0030. Supporting multiple
bases for one candidate/destination pair requires a separately accepted key, catalog,
or governed pricing-computation contract.

## Exact Lookup, Freshness, And Rotation

After complete validation, lookup uses exact equality on the candidate/destination
pair. Prefix, substring, case folding, wildcard, alias, provider-label translation,
default, fallback, first match, or path derivation from selectors is forbidden.

The selected record is fresh only when:

```text
observedAt <= evaluationTime < expiresAt
```

Equality at observation is fresh; equality at expiry is stale. An expired unrelated
record does not deny a different valid exact match, because expiry is a selected-record
condition after whole-catalog structural and semantic validation. RFC-0030 independently
checks the same window at the same evaluation time before it can create suitability.

Rotation requires separately trusted composition to construct a new source with the
approved path and pin. Replacing bytes under an old pin refuses acquisition. File
timestamps, provider responses, prior success, cached facts, and stale-grace policy do
not refresh or roll back evidence.

## Closed Result And Refusal Order

The future sealed result has exactly two shapes:

```text
Acquired(RemoteModelCandidateCapabilityFacts facts)
Refused(RemoteModelCandidateCapabilityFactSourceRefusalReason reason)
```

`Acquired` contains the newly constructed exact facts and nothing else. `Refused`
contains one reason and no facts. After programming-error checks, acquisition returns
the first matching reason in this order:

1. `CATALOG_UNAVAILABLE_OR_UNSAFE`;
2. `CATALOG_OUTSIDE_SIZE_BOUND`;
3. `CATALOG_CHANGED_DURING_READ`;
4. `CATALOG_PIN_MISMATCH`;
5. `CATALOG_INVALID_UTF8`;
6. `CATALOG_SCHEMA_UNSUPPORTED`;
7. `CATALOG_NOT_CANONICAL`;
8. `CATALOG_CONTENT_INVALID`;
9. `RECORD_NOT_FOUND`; and
10. `RECORD_STALE`.

The implementation must preserve this observable precedence and never overwrite an
earlier failure. Unsupported catalog header or record facts schema maps to schema
unsupported. Wrong order, syntax, newline form, integer or instant encoding, and
re-encode mismatch map to not canonical. Intrinsic bounds, duplicate pairs, invalid
relationships, and unmappable typed values map to content invalid. Internal exceptions
are contained and mapped at the earliest applicable boundary; no partial facts escape.

## Safe Rendering And Lifetime

Source, result, and refusal rendering may expose only contract/schema version, one
closed refusal reason, and on success the bounded identifiers already allowed by
RFC-0030: source/revision, evidence, candidate/destination, model/capability, token
semantics, price schedule, currency, and enum names.

Rendering must not expose the catalog path, expected or actual pin comparison, raw
catalog text, raw capacities, raw prices, timestamps, exceptions, source errors,
request or prompt content, content digests, policy contents, endpoints, provider model
labels, credentials, or secrets.

Acquired facts remain invocation-local under RFC-0030. They are never serialized,
persisted, cached, replayed, placed in a RunRecord, or reconstructed from identifiers.
Every retry or fallback attempt reacquires fresh RFC-0016 admission, RFC-0028
eligibility and pricing, RFC-0031 facts, RFC-0030 suitability, and RFC-0029 routing.

## Provisioning Truthfulness And Failure Behavior

The reader directly establishes only the fixed parser schema, exact approved snapshot
revision, canonical record bytes, exact lookup, and explicit-time freshness. The
out-of-scope provisioner must truthfully establish capacity evidence; route, model, and
capability binding; observation and expiry; token semantics and price schedule;
conservative cost for the retained basis; supported reasoning, context, token, and
timeout maxima; and classification approval.

If that evidence or protected independent provisioning does not exist, the source
cannot truthfully produce production facts. It must refuse rather than copy values from
a request, eligibility, provider claim, prior outcome, filename, timestamp, repository,
environment, CLI, ambient state, or default. A pin mismatch, unsafe indirection,
malformed unselected row, stale selected row, or missing exact pair always yields a
closed refusal and no facts.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add the closed result and refusal enum under missing-symbol RED tests;
2. add canonical catalog parsing, exact-real bounded snapshot and pin validation under
   isolated filesystem tests, including whole-catalog invalidity and safe rendering;
3. add exact lookup, complete facts mapping, freshness, and rotation behavior under
   boundary tests;
4. add construction, package, dependency, no-ambient-input, and zero-production-caller
   guards while retaining RFC-0030's sole facts/proof construction path; and
5. run focused RFC-0014, RFC-0016, RFC-0028 through RFC-0031, deterministic-fake,
   architecture, and full Markdown-sensitive regression.

No implementation increment may add the same-invocation consumer. If protected
provisioning and truthful evidence are unavailable, no production catalog is installed
and no production remote route becomes reachable.

## Verification Contract

The later implementation must cover null and malformed programming inputs; exact-real
path checks; links, junctions, reparse points, non-regular files, missing files, open
errors, zero and boundary sizes, concurrent truncation and growth, exact and mismatched
pins, strict UTF-8, BOM and newline variants, every grammar field and order, unknown or
missing lines, canonical integers and instants, record count zero and 256, overflow,
all intrinsic field bounds and relationships, duplicates and sort order, invalid
unselected records, exact and non-exact selectors, absent records, inclusive
observation and exclusive expiry, the complete field mapping, same-time handoff, every
refusal alone and in precedence combinations, safe rendering, fresh reread, rotation,
and no cache, discovery, fallback, ambient input, provider, credential, network, spend,
persistence, runtime, or external effect.

Regression must prove RFC-0016, RFC-0028, RFC-0029, and RFC-0030 behavior unchanged;
the deterministic fake remains only on RFC-0020 through RFC-0027; the source cannot
construct suitability or route options; and no production caller acquires facts or
invokes the RFC-0030 evaluator.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Roadmap/Planner/Assisted-Loop/dynamic-workflow governance, reference inspection,
`git diff --check`, and the full README-owned Java 17 regression are the verification
boundary.

## Rejected Alternatives

- Treating a matching digest as provider attestation or factual truth is rejected; it
  proves only operator-approved exact bytes.
- Letting request, eligibility, provider labels, repository content, environment, CLI,
  working directory, or selectors choose the path, pin, or values is rejected because
  it collapses the independent trust boundary.
- Putting facts acquisition in the pure RFC-0030 evaluator or RFC-0029 Router is
  rejected because it hides filesystem and freshness effects.
- Validating only the selected row, accepting duplicate first-wins records, or deriving
  paths from selectors is rejected because malformed or attacker-controlled content
  could evade the catalog boundary.
- Discovery, latest-file selection, fallback pins, caches, grace periods, or filesystem
  timestamps as freshness are rejected because they silently change approved evidence.
- A signed catalog is not selected for v1 because signer identity, key custody,
  revocation, and installation metadata require separate governance; signing may be a
  later alternative, not an implied property of this pin.
- Provider retrieval, live probing, prior successful execution, availability, quality,
  latency, score, and mutable status are rejected because they do not establish this
  immutable capability contract and require separate effect and evidence boundaries.
- Automatic token, price, or currency conversion and multiple budget bases per pair are
  rejected because no approved conservative computation or expanded lookup contract
  exists.
- Persisting or replaying the facts is rejected because approval, capacity, policy,
  price, and freshness may change between attempts.

## Exclusions

- Java or test-source implementation, a catalog writer or installed catalog, a source
  instance or production caller, and changes to RFC-0013 through RFC-0030 behavior;
- provisioning, rotation commands, installation layout or permissions, signing keys,
  signature verification, remote retrieval, discovery, registry, cache, or fallback;
- provider or adapter construction, provider model, endpoint, protocol, credential,
  secret, network, transmission, response handling, availability, quality, latency,
  score, tokenizer, or live capacity probing;
- spend authorization, billing, paid service, token or currency conversion, route
  execution, retry execution, or durable route provenance;
- Tool/evidence/RunRecord/runtime-event/Scheduler/finalization/recovery integration,
  durable schema or migration, MCP, plugin, interface, or background work; and
- capability promotion, push, merge, release, deployment, permission change,
  destructive cleanup, or another external effect.

## Codex Implementation Prompt

Implement RFC-0031 RED-first as a final pinned-file capability-fact source with one
bounded exact byte snapshot, independently provisioned absolute normalized path and
whole-file SHA-256 pin, strict canonical grammar, complete validation before exact pair
lookup, closed ordered refusals, explicit-time freshness, complete RFC-0030 field
mapping, and safe rendering. Add no consumer, Router caller, provider, adapter,
credential, network, spend, persistence, retry, fallback, runtime, or MCP path.
