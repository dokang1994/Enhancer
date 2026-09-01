# RFC-0021: Deterministic Fake Token Semantics And Capacity

Status: Accepted

## Purpose

Define the smallest truthful token unit and numeric capacity contract for the closed
deterministic-fake candidate introduced by RFC-0020. This contract is fake-specific:
it gives a later standalone suitability implementation enough fixed evidence to
evaluate the remaining profile predicates without claiming provider tokenizer
compatibility or authorizing an invocation.

This RFC changes no Java, test source, caller, Tool, gateway behavior, durable schema,
artifact, command, runtime path, or capability maturity. The current candidate remains
`deterministic-fake-v1`, reports token semantics unavailable, and continues to stop
at `TOKEN_SEMANTICS_UNAVAILABLE` until a separate RED-first implementation task.

## Relationship To Existing Contracts

RFC-0013 continues to own `ModelRequest`, `ModelResponse`, generic `ModelUsage`,
the exact fake response, and its Java-string length bounds. RFC-0014 continues to own
provider-neutral profile requirements and the invariant:

```text
maxInputTokens + maxOutputTokens <= maxTotalTokens <= minimumContextTokens
```

RFC-0015 through RFC-0019 continue to own exact request/profile identity, admission,
Scheduler preparation, policy identity, Model RunRecord v2 provenance, and retry versus
recovery ordering. RFC-0020 continues to own the closed candidate binding, pure
suitability result, first-match rejection order, and the prohibition on invocation
authority.

RFC-0021 owns only:

- one versioned deterministic-fake token unit and exact counting algorithm;
- four fixed capacity values and their proof over the closed fake response algebra;
- the candidate identity change required when those facts become available;
- the distinction between profile/candidate suitability and actual request budgets;
- the later RED-first standalone implementation contract; and
- the stop before a separately accepted same-request invocation seam.

It does not define a general tokenizer, provider usage mapping, router, invocation,
runtime disposition, or persistence format.

## Versioned Fake-Only Token Unit

The token-semantics identity is:

```text
deterministic-unicode-scalar-v1
```

One token is one Unicode scalar value represented by a well-formed Java `String`.
The counter scans the supplied string from left to right without copying, normalizing,
case-folding, line-ending conversion, encoding, decoding, or replacement:

- null is a programming error;
- the empty string counts as zero, without weakening `ModelRequest`'s nonblank rule;
- one non-surrogate UTF-16 code unit counts as one token;
- one valid high-surrogate/low-surrogate pair counts as one token;
- an isolated high surrogate, isolated low surrogate, high/high pair, or any other
  malformed surrogate sequence fails closed and produces no count;
- LF and CR each count as one, so CRLF counts as two;
- a supplementary scalar counts as one;
- combining marks, composed and decomposed text, a byte-order mark, and visually
  equivalent strings are not normalized and are counted exactly as supplied.

The future counter returns a `long`. For any well-formed Java string its result is no
greater than `String.length()`, so the current bounded inputs cannot overflow. The
implementation must still use checked arithmetic for every derived response or total
count. Diagnostics must not reproduce prompt content.

This unit is not a provider token, subword, byte, grapheme cluster, Java character
alias, billing unit, serialized UTF-8 claim, or compatibility promise for another
candidate. Its identity must accompany every fixed candidate fact that depends on it.

## Candidate Identity And Fixed Facts

`deterministic-fake-v1` permanently denotes the current closed candidate whose token
semantics are unavailable. A later implementation must not silently change that
identity's meaning. The token-aware candidate identity is:

```text
candidate identity = deterministic-fake-v2
token semantics identity = deterministic-unicode-scalar-v1
```

The model class, required capability, maximum reasoning, closed local provenance,
no-provider-charge fact, maximum `PUBLIC` classification, and exact bound gateway
remain unchanged. The candidate remains opaque, process-local, non-persistent, and
bound through the existing exact-fake factory. The new identity, semantics identity,
availability, and capacity values are repository-owned fixed getters, not constructor
arguments, fields, profile-derived values, registry data, or caller claims.

Any future change to the counting algorithm, malformed-input rule, fake rendering,
capacity value, or a fact used in the proof requires a new candidate and/or token-
semantics identity under a separate accepted contract. Existing attempts and artifacts
are never reinterpreted.

## Exact Fake Response Algebra

For one well-formed prompt:

```text
n = prompt UTF-16 code-unit length
s = prompt deterministic-unicode-scalar-v1 token count
d(n) = decimal digit count of n
```

The exact fixed-model fake response has:

```text
response UTF-16 length = n + 152 + d(n)
response token count   = s + 152 + d(n)
```

The fixed `152` covers the literal header, fixed
`model-class=deterministic-fake` line, label plus 64 lowercase hexadecimal SHA-256
characters, prompt-length label, line separators, and echo label, excluding only the
decimal digits and echoed prompt. Every fixed character is one ASCII Unicode scalar.

The proof applies only after RFC-0020 has matched the exact fake model class. It does
not generalize to the gateway port, another model class, another response rendering,
provider output, or arbitrary `ModelResponse`.

## Proven Capacities

The token-aware candidate has exactly these capacities:

```text
maximum context tokens = 524,288
maximum input tokens   = 262,144
maximum output tokens  = 262,144
maximum total tokens   = 524,130
```

The meanings and proofs are distinct:

- input capacity is the greatest scalar count possible in an already-valid
  `ModelRequest.prompt()`; scalar count is at most the existing 262,144 UTF-16
  code-unit request bound;
- output capacity is the greatest scalar count possible in an already-valid
  `ModelResponse.text()`; scalar count is at most the existing 262,144 UTF-16
  code-unit response bound;
- context capacity is the independent input-plus-output scalar envelope,
  `262,144 + 262,144 = 524,288`; and
- total capacity is the tight maximum combined scalar count of one successful exact
  fake exchange. An ASCII prompt has `s = n`. At `n = 261,986`, six decimal digits
  make the response exactly `262,144` tokens and the combined count exactly
  `524,130`. At `n = 261,987`, the exact response length is `262,145` and the
  existing gateway refuses it.

For non-ASCII well-formed input, `s <= n`, so it cannot exceed that tight total.
All values fit below RFC-0014's 1,000,000,000 bound. The later implementation must
derive and test the response formula with `Math.addExact`-equivalent checked
arithmetic; it may not trust `ModelUsage`, ambient memory, CPU, a model label, or an
unchecked literal as proof.

Context and total capacity deliberately differ. RFC-0014 permits a model's nominal
context envelope to exceed its stricter per-invocation total ceiling. Setting both to
`524,130` would make `TOTAL_TOKEN_CAPACITY_INSUFFICIENT` unreachable because every
valid profile already requires `maxTotalTokens <= minimumContextTokens` and context is
checked first. The four facts remain independently testable.

## Profile Suitability Ordering

The later standalone implementation preserves RFC-0020's exact first-match order. Once
the exact candidate reports the accepted semantics, evaluation continues:

1. reject minimum context above `524,288`;
2. reject maximum input above `262,144`;
3. reject maximum output above `262,144`;
4. reject maximum total above `524,130`;
5. require the existing zero-microunit free-only ceiling;
6. require the existing `PUBLIC` classification ceiling; and
7. only then return `Suitable` retaining the exact admitted and candidate instances.

The existing closed rejection reasons and order do not change. Threshold and combined
first-match tests must make every reason reachable, including a total-only mismatch
whose input, output, and minimum context remain within their corresponding candidate
capacities.

`Suitable` means only that the exact candidate's fixed capability envelope can satisfy
the profile's declared requirements. It remains ephemeral non-authority and performs no
request counting, Tool or gateway call, evidence capture, persistence, or lifecycle
transition.

## Actual Request Budget Boundary

Candidate/profile suitability and actual invocation budgeting are different decisions.
The RFC-0020 evaluator has no actual-request budget rejection vocabulary and must not
silently acquire one.

A separately accepted same-request seam must, before any gateway activity:

- count the exact admitted `profiledRequest.request().prompt()` under the semantics
  above and fail closed on malformed surrogate input;
- compute the exact predicted response UTF-16 length and fake-token count from that
  same request without rereading, normalizing, copying, or reconstructing the prompt;
- require actual input tokens within `profile.maxInputTokens`;
- require predicted response UTF-16 length within the exact request's unchanged
  `maxResponseLength`;
- require predicted output tokens within `profile.maxOutputTokens`;
- require checked actual input plus predicted output within
  `profile.maxTotalTokens`; and
- pass the exact request, same RFC-0019 `ExecutionPolicy` instance, and exact
  candidate-bound gateway to the separately accepted invocation boundary.

That later contract must define its own closed typed refusal algebra and ordering.
Failure produces no gateway, Tool, evidence, RunRecord, runtime, queue, or retry effect.
No value is clamped, repaired, normalized, or converted into a new `ModelRequest`.

Therefore a profile may be candidate-suitable while one concrete request is still
budget-invalid. `Suitable` never promises invocation success and never substitutes for
actual request validation.

## ModelUsage Separation

`ModelUsage` remains the RFC-0013 generic bounded pair of `inputUnits` and
`outputUnits`. The current fake continues to report Java `String.length()` values.
Those values differ from scalar tokens for supplementary characters and are neither
input to suitability nor evidence for the capacity proof.

This RFC adds no usage normalization or mapping. Even where ASCII or BMP values happen
to be equal, current usage units do not become provider tokens, fake-token authority,
billing data, or a general gateway contract.

## Retry, Recovery, And Persistence

Before a Model RunRecord v2 reference is durably checkpointed, every future actual
retry repeats exact task resolution, prompt snapshot, request/policy preparation,
RFC-0015/RFC-0016, candidate binding, token counting, suitability, and actual budget
validation freshly. No count, tokenized copy, `Admitted`, or `Suitable` value crosses
an attempt or process boundary.

After the exact v2 reference is durable, recovery performs no token counting, task
lookup, admission, candidate binding, suitability, Tool call, or gateway call. It
point-resolves and validates historical provenance and resumes only deterministic
finalization under RFC-0019. Candidate and token-semantics identities are not added to
the current persisted schema by this RFC.

## Required Implementation Sequence

Future work remains sequential:

1. implement the pure scalar counter, token-aware fixed `deterministic-fake-v2`
   candidate facts, and the remaining field-free suitability predicates RED-first,
   with no production caller and no invocation;
2. separately specify and implement the exact-request budget refusal and
   same-request/same-policy/same-gateway invocation seam;
3. only then integrate Model RunRecord v2 execution, finalization, result validation,
   retry, and recovery; and
4. separately define any typed ModelWork producer or receiver.

Sequence 1 may make the isolated evaluator's `Suitable` branch reachable. It grants no
authority because no production caller consumes it and every current typed ModelWork
execution guard remains in place.

## Later RED-First Verification Contract

The token/capacity implementation must cover:

- null, empty, LF, CRLF, BMP, composed/decomposed, combining-mark, byte-order-mark,
  supplementary-pair, and every malformed-surrogate position;
- no normalization, replacement, encoding, provider tokenizer, or `ModelUsage`
  dependency;
- the exact response formula against actual fake rendering across digit boundaries;
- the successful `261,986` ASCII boundary, refused `261,987` boundary, checked
  arithmetic, and all four fixed capacity values;
- `deterministic-fake-v1` remaining the unavailable identity in history and the
  token-aware candidate exposing fixed `deterministic-fake-v2` plus
  `deterministic-unicode-scalar-v1`;
- candidate shape retaining only the exact gateway instance, with no caller-supplied
  semantics or capacities;
- every RFC-0020 rejection threshold, combined first-match precedence, free-only cost,
  public classification, reachable `Suitable`, and exact identity retention;
- zero request counting, Tool/gateway/evidence/RunRecord/runtime activity or production
  caller in the standalone suitability increment; and
- unchanged `ModelUsage`, `ModelInvokeTool`, Scheduler preparation, typed ModelWork
  guards, durable schemas, and package boundaries.

The later exact-request seam needs separate RED-first evidence for actual input,
predicted output, request response-length, total-budget, malformed-input, zero-activity
refusal, exact object identities, retry, and recovery behavior.

For this documentation-only task, RFC/decision/architecture/index/ownership/dynamic-
workflow/approved-task/task-justification/planner governance, `git diff --check`, and
the full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Reusing one Java UTF-16 code unit as one fake token is rejected because it merely
  renames the current `String.length()`-based usage accounting and cannot demonstrate
  the separation RFC-0020 requires.
- Replacing malformed surrogates with a scalar is rejected because it silently changes
  untrusted input before the exact-request boundary. Counting fails closed instead.
- UTF-8 bytes are rejected for this in-process fake because they add encoding and
  malformed-replacement policy while the exact response safety bound is defined over
  Java strings. Provider subwords, graphemes, and model vocabularies add still broader
  dependencies and authority.
- Assigning all capacities to 262,144, setting context equal to total, deriving capacity
  from `ModelUsage.MAX_UNITS`, or using ambient machine resources is rejected as
  false or algebraically incomplete evidence.
- Reducing input capacity to 261,986 is rejected because it conflates the independent
  input envelope with the fake's coupled response and total limit. Actual request
  validation owns that coupling.
- Changing token availability or capacities under candidate identity
  `deterministic-fake-v1` is rejected because stable identity cannot change meaning.
- Counting the request inside suitability or adding an unplanned request-budget reason
  to RFC-0020 is rejected because it changes the closed decision algebra. That belongs
  to the later same-request seam.

## Exclusions

- Java or test-source implementation; current candidate, evaluator, request, response,
  gateway, Tool, Scheduler, caller, or schema behavior changes;
- generic or provider tokenizer, usage normalization, provider model, registry,
  discovery, selection, router, endpoint, destination, outbound policy, network or
  remote transmission;
- credentials, paid services, pricing, currency conversion, billing, spend authority;
- gateway or Tool execution, prompt reread, redaction, scanning, injection resistance,
  attribution, quality evaluation, caching, fallback, retry, or streaming;
- Model RunRecord writing, lifecycle disposition, finalizer, result validation,
  runtime/process/worker/recovery/status/caller wiring;
- typed producer, submission, receiver, CLI, durable schema, migration, MCP, capability
  maturity, release, deployment, push, or merge.

## Prompt Book

### Prompt: Implement Deterministic Fake Token Capacity

Implement sequence 1 RED-first: the pure well-formed Unicode-scalar counter, fixed
token-aware candidate v2 facts, and remaining field-free suitability predicates. Keep
all production callers and invocation paths absent.

### Prompt: Specify The Exact-Request Budget Seam

Propose, but do not implement, the closed typed actual-request budget decision and
same-request/same-policy/same-gateway invocation seam required before any caller can
consume `Suitable`.
