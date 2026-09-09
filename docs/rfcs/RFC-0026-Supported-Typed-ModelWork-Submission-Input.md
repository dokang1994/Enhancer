# RFC-0026: Supported Typed ModelWork Submission Input

Status: Accepted

## Purpose

Define the smallest supported interface that can read one complete, explicit
`ModelExecutionProfile` and durably submit one deterministic-fake `ModelWorkPayload`
through the existing RFC-0024 producer. The interface makes typed intent reachable by
the already supported RFC-0025 foreground Scheduler composition while preserving
profile/capability separation, manifest-first replay, legacy command compatibility,
and the distinction between submission and execution.

This RFC does not implement the interface, publish or receive a spool, invoke a model,
select a provider, or authorize network, credentials, spend, MCP, background work, or
remote execution.

## Relationship To Existing Contracts

RFC-0014 owns the immutable ten-component `ModelExecutionProfile` and all intrinsic
value bounds. RFC-0015 and RFC-0016 continue to own request/profile alignment and fresh
invocation admission. RFC-0017 requires a caller-specific complete-profile source and
an independently governed capability. RFC-0018 through RFC-0023 own typed transport,
durable formats, preparation, the closed deterministic fake, process execution, Model
RunRecord v2, finalization, retry, and recovery.

RFC-0024 owns the current package-local producer request, fixed repository-owned
`deterministic-echo` capability source, task/snapshot/envelope construction, exact
manifest-first replay, and durable admission. RFC-0025 owns the optional model-aware
execution configuration of `scheduler-cycle`, `scheduler-drain`, and
`scheduler-service`.

RFC-0026 adds only the profile file reader, a narrow public filesystem composition over
RFC-0024, and one separate command that maps explicit caller input into that producer.
It does not widen a legacy command or change any downstream durable value.

## Current Gap And Selected Boundary

The RFC-0024 producer is production code but package-local and reachable only from
tests. Supported Scheduler commands can execute typed work only after another source
has already admitted it. No supported interface owns the complete profile input.

RFC-0024 permits either direct typed submission or spool publication next. Direct
submission is selected first because it closes one supported input-to-durable-admission
path using the existing producer and stores. A publisher would additionally require a
retained transport point and, before end-to-end use, a manifest-authorized receiver,
acknowledgement, capacity-release, and recovery contract. Those are not required to
establish the first supported typed input and remain separate work.

The new command is exactly `scheduler-submit-deterministic-fake-model-work`. It is
distinct from and does not alter `scheduler-submit`, `scheduler-submit-generated`,
`scheduler-spool-work`, or `scheduler-receive-work`.

## Supported Command Surface

`scheduler-submit-deterministic-fake-model-work` requires exactly these single-valued
options:

- `--project-root <path>`;
- `--submission-root <path>`;
- `--queue-root <path>`;
- `--task-id <bounded active task identity>`;
- `--submission-id <canonical UUID>`;
- `--max-work-items <1..4096>`;
- `--producer <bounded producer identity>`;
- `--target-path <bounded project-relative prompt path>`;
- `--expected-response-sha256 <64 lowercase hexadecimal characters>`;
- `--model-execution-profile-file <project-relative profile path>`; and
- `--priority NORMAL|EXPEDITED`.

Missing, duplicate, unknown, malformed, or extra options fail during command
validation. Priority is explicit and has no default. There is no capability, queue,
correlation, logical-run, occurrence-time, allowed-Tool, snapshot, execution-policy,
candidate, gateway, provider, endpoint, credential, price, network, or process-
configuration input.

The submission UUID remains the caller-retained recovery identity. RFC-0024's existing
versioned transform derives the queue, correlation, and logical-run identities, and its
first-use path captures the occurrence time and Workspace snapshot. The caller does
not supply or override them.

## Complete Profile File V1

`--model-execution-profile-file` names one invocation-scoped untrusted input file. It
is not an ambient repository default, registry entry, provider configuration,
capability source, or durable identity. Every invocation supplies it explicitly,
including exact replay.

The file is strict UTF-8 without a BOM and is at most 4,096 bytes during consumption.
Its canonical syntax is exactly thirteen LF-terminated `key=value` lines in this order,
including the final LF:

```text
schemaVersion=model-execution-profile-v1
requiredCapability=deterministic-echo
modelClass=deterministic-fake
localityRequirement=LOCAL_ONLY
reasoningRequirement=STANDARD
minimumContextTokens=16384
tokenBudget.maxInputTokens=4096
tokenBudget.maxOutputTokens=2048
tokenBudget.maxTotalTokens=8192
costBudget.currencyCode=USD
costBudget.maxMicrounits=0
maximumInvocationTimeMillis=30000
dataClassification=PUBLIC
```

These thirteen serialized entries map to the ten RFC-0014 components; the token and
cost budget components are flattened without introducing another domain value. The
example values are illustrative input, not defaults.

The parser accepts no BOM, carriage return, NUL, blank line, comment, escape,
continuation, alternate separator, leading or trailing whitespace, reordered key,
duplicate key, unknown key, or additional line. It does not trim, case-fold, normalize,
alias, repair, infer, or substitute any value.

Numeric values use canonical ASCII decimal. Positive values match `[1-9][0-9]*`;
`costBudget.maxMicrounits` alone may also be the exact value `0`. A plus or minus sign,
leading zero, decimal point, exponent, non-ASCII digit, or overflow fails closed.
`maximumInvocationTimeMillis` maps exactly to a millisecond-precision `Duration`.

The parsed values must construct the existing RFC-0014 types unchanged:

- `schemaVersion` is exactly `model-execution-profile-v1`;
- capability and model-class labels retain their existing 256- and 64-character stable
  lower-case ASCII bounds;
- locality, reasoning, and data classification use their existing closed exact enums;
- context and each token value are between 1 and 1,000,000,000 inclusive;
- overflow-safe token validation retains
  `maxInputTokens + maxOutputTokens <= maxTotalTokens <= minimumContextTokens`;
- `currencyCode` is exactly three upper-case ASCII letters and performs no registry
  lookup;
- `maxMicrounits` is between 0 and 1,000,000,000,000,000 inclusive; and
- invocation time is between 1 and 300,000 milliseconds inclusive.

Successful parsing proves only a complete intrinsically valid requirement value. It
does not prove that the fixed candidate can satisfy it and does not perform RFC-0015,
RFC-0016, suitability, budget, Tool, or gateway work.

## Profile Path And Read Boundary

The profile path is non-empty, relative to the explicit project root, normalized
without escaping it, and resolved under the real project root. Absolute, drive-
relative, empty, traversal, missing, unreadable, directory, device, pipe, symbolic-link,
junction/reparse escape, or otherwise non-regular input fails before manifest or queue
access. The final named path itself may not be a symbolic link, and its resolved real
path must remain beneath the real project root.

The implementation reads one bounded byte snapshot with strict malformed/unmappable
UTF-8 reporting. Preflight size metadata is not trusted: growth beyond 4,096 bytes
during consumption fails. The parser consumes only that byte snapshot. It does not
search for another file, follow an environment variable, consult a registry, or fall
back to an embedded profile.

The path string is an interface source locator, not submission intent. It is not
persisted in the manifest and is not compared on replay. The complete parsed
`ModelExecutionProfile` is the semantic caller input already retained by RFC-0024, so a
different contained path with the exact same profile value is an exact replay while a
changed profile under the same submission identity fails closed.

## Authority Projection And Public Composition

The command maps the submission ID, task ID, producer, target, expected-response
digest, complete parsed profile, capacity, and priority unchanged into the RFC-0024
request. It never accepts `WorkItem.requiredCapability`. The existing closed
`DeterministicFakeModelSubmissionCapabilitySource` remains the sole source of exact
`deterministic-echo` capability for this producer.

The profile's `requiredCapability` remains untrusted requirement data and may disagree
with that source. Neither the reader nor submission command compares, rewrites, or
normalizes it. The disagreement is durably retained and later reaches the existing
RFC-0016 `REQUIRED_CAPABILITY_MISMATCH` pre-call refusal under the Scheduler attempt.

The later implementation keeps the existing RFC-0024 request, service, and capability
source package-local and exposes one narrow public deterministic-fake ModelWork
submission facade. Its production construction accepts only explicit project,
submission, and queue roots and internally selects `Clock.systemUTC()`, the real
repository readers, stores, and snapshot collector. A package-private injected
construction may support tests. The facade method accepts exactly the existing eight
semantic request components and returns the existing `DurableSubmissionResult`. No
caller-selectable source, store implementation, capability, context, or ambient
singleton is added.

## First Use, Replay, And Refusal Ordering

Command syntax, profile-path containment, bounded file reading, profile parsing, and
intrinsic request validation finish before any manifest or queue access. The command
then delegates exactly once to RFC-0024.

On first use, the existing producer order remains authoritative:

```text
derive identities
-> resolve absent manifest
-> load governed context and exact active task
-> require model-invoke
-> validate target containment
-> capture one clock value and repository-memory snapshot
-> construct exact ModelWork envelope and manifest
-> persist manifest
-> create or resolve exact-capacity queue
-> durably admit once
```

On replay, the producer resolves the manifest before consulting the clock, Context
Reader, Approved Task reader, or Workspace collector, compares every semantic
caller-owned input and the fixed capability, then delegates to exact durable replay.
The profile file is necessarily read first to reconstruct caller intent; this is not a
repository-authority or clock recapture. Exact replay changes no manifest bytes or
queue revision. Changed task, producer, target, digest, profile, capacity, priority,
derived identity, payload kind, or fixed capability under the same submission UUID
fails before a new admission.

Intrinsic command/profile/request failures and active-task/target mismatches are usage
or configuration failures with exit code `2`. Durable I/O, corruption, unsupported
artifact, and internal failures retain exit code `70`. Diagnostics use the existing
4,096-character bound and never include complete profile content, prompt content,
response content, evidence, child arguments, credentials, or secrets.

A syntactically and intrinsically valid but unsatisfied profile is not a submission
failure. RFC-0024 deliberately retains it so fresh execution can produce the existing
typed pre-call refusal. Submission success therefore means only exact durable intent
and admission, never candidate suitability, invocation, verification, completion, or
future retry success.

## Supported Output And Operator Recovery

Successful output uses the existing generated-submission vocabulary and order:

```text
status=ADMITTED|REPLAYED
exitCode=0
submissionId=<canonical UUID>
queueId=<derived canonical UUID>
correlationId=<derived canonical UUID>
logicalRunId=<derived canonical UUID>
occurredAt=<manifest occurrence time>
queueRevision=<persisted revision>
priority=NORMAL|EXPEDITED
manifestCreated=true|false
queueCreated=true|false
workAdmitted=true|false
workspaceSnapshotId=<retained snapshot identity>
```

Output is derived from the returned result and point-resolved exact manifest. It does
not print the profile path, any profile field, target content, response digest,
capability, Tool scope, or model-execution configuration.

Recovery repeats the same `scheduler-submit-deterministic-fake-model-work` command with
semantically identical caller input and then invokes `scheduler-cycle`,
`scheduler-drain`, or `scheduler-service` separately with RFC-0025's complete model-
execution group and the derived queue identity. Submission performs no Scheduler cycle.
Execution performs no submission. No wrapper, directory scan, polling loop, daemon, or
implicit continuation combines the two effects.

## Compatibility And Schema Sufficiency

No durable schema changes are required. The current formats already retain the exact
semantic value and recovery identity:

- message-envelope and transport-spool v2 retain complete typed ModelWork;
- submission manifest v3 retains the queue, capacity, priority, independent fixed
  capability, and exact profile-bearing envelope;
- Scheduler queue v4 and AgentRuntime v5 retain the exact WorkItem;
- pending-finalization v2 retains execution recovery references;
- Model RunRecord v2 retains exact model provenance and lifecycle; and
- existing Result, timeout, and runtime-event formats retain their current facts.

The profile input file is transient command input, not another durable store or
sidecar. Existing legacy commands, payload bytes, migration behavior, output, and
recovery remain unchanged because none recognizes the new command or file format.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add failing profile-reader tests for exact thirteen-line parsing, every RFC-0014
   bound and relationship, 4,096-byte consumption, strict UTF-8/LF syntax, containment,
   link/reparse refusal, and missing/partial/duplicate/unknown/default/fallback absence;
2. add failing CLI value/parser and pre-store tests for the exact new command, option
   set, explicit priority, absence of capability input, and bounded redacted failures;
3. expose the minimum public RFC-0024 filesystem facade and connect the command without
   widening any legacy surface;
4. add real-filesystem tests for first admission, exact replay without clock/context or
   durable revision changes, every caller-intent conflict, manifest/queue recovery
   prefixes, invalid input before root access, and bounded output;
5. add a real JVM operator-path integration from supported typed submission to each
   RFC-0025 Scheduler execution command, including verified completion, one retained
   profile/capability mismatch refusal, exact post-reference recovery, and legacy
   compatibility; and
6. run focused RFC-0014-through-RFC-0026, durable-format, Scheduler CLI, source/
   locality, architecture, and full Markdown-sensitive Java 17 regression.

Observable behavior is test-first. Each RED must be classified against the active
task, accepted RFCs, current formats, and legacy behavior. Every GREEN increment is an
ordinary local commit under the later implementation task. Push, merge, release, and
deployment remain separate authority.

## Rejected Alternatives

- Widening a legacy submission, publisher, or receiver is rejected because it merges
  payload contracts and risks treating arbitrary capability text as governed source.
- Publisher-first is rejected because it cannot complete supported admission without a
  separate manifest-authorized typed receiver and acknowledgement/recovery contract.
- Ten or thirteen profile CLI flags are rejected because they flatten one indivisible
  value into an ambiguous command surface and make partial/default behavior easier.
- JSON, YAML, Java properties, comments, unordered keys, escaping, and optional fields
  are rejected for v1 because their libraries and permissive variants enlarge
  canonicalization, duplicate-key, alias, and unknown-field risk without current need.
- An absolute, external, discovered, environment-selected, registry-selected, or
  provider-selected profile file is rejected as an ambient or uncontained source.
- Supplying, copying, or prechecking authoritative capability from the profile is
  rejected because it would let untrusted requirements self-certify and hide RFC-0016
  mismatch.
- Persisting the profile path or raw input file is rejected because manifest v3 already
  durably retains the exact typed profile needed by execution and recovery.
- Combining submission and Scheduler execution is rejected because admission,
  execution, verification, and completion are distinct recoverable facts.

## Exclusions

- Java or test implementation in this specification task;
- spool publisher, receiver, Message Bus or runtime-event ingress, acknowledgement,
  directory discovery, cleanup, or retention;
- changes to legacy submission, generated submission, spool, receiver, execution, or
  direct `model-invoke` commands;
- caller capability input, capability inference, profile defaults, registry, fallback,
  environment lookup, or provider configuration;
- candidate/provider/router selection, endpoint, remote transmission, network,
  credentials, pricing, spend, cache, streaming, or MCP;
- new durable schema, migration, sidecar, RunRecord provenance, event kind,
  cancellation propagation, or terminal pre-call-refusal record;
- background service, supervisor, concurrency, multi-agent execution, release,
  deployment, permission change, destructive cleanup, or external effect; and
- push or merge.

## Prompt Book

### Prompt: Implement Supported Typed ModelWork Submission Input

Implement RFC-0026 RED-first. Add the strict bounded complete-profile file reader, the
separate `scheduler-submit-deterministic-fake-model-work` command, and the minimum
public filesystem facade over RFC-0024; preserve the fixed independent capability,
exact manifest-first replay, current durable schemas, legacy commands, separate
RFC-0025 execution, and bounded redacted output. Do not add a typed publisher or
receiver, capability input, profile defaults, provider/network/credential/spend
authority, schema changes, implicit execution, push, or merge.
