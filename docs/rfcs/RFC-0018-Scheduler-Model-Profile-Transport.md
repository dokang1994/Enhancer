# RFC-0018: Scheduler Model Profile Transport

Status: Accepted

## Purpose

Define the minimum typed, versioned, and recoverable Scheduler source for one complete
RFC-0014 `ModelExecutionProfile`. The contract carries that exact untrusted value from
durable submission through message, queue, AgentRuntime, process isolation, retry, and
recovery while preserving the active `WorkItem.requiredCapability` as the independent
RFC-0016 projection.

This RFC specifies a future coordinated migration and implementation boundary. It does
not change current Java, commands, schemas, artifacts, or runtime behavior, and it does
not enable model execution. RFC acceptance does not imply capability maturity.

## Relationship To Existing Contracts

RFC-0013 continues to own `ModelRequest`, the gateway port, the deterministic fake, and
the response-character ceiling. RFC-0014 continues to own all ten components and
validation of `ModelExecutionProfile`. RFC-0015 continues to own request/profile model-
class and timeout alignment. RFC-0016 continues to own task, Tool, capability, timeout,
and locality admission. RFC-0017 continues to own caller-source separation and fresh
per-invocation evaluation.

RFC-0018 owns only the Scheduler-specific typed retention, canonical serialization,
durable schema family, migration, cutover, replay, and recovery obligations needed to
make one complete profile available to a later caller. Retention proves neither
authority nor admission.

## Typed Model Work

The future Gate 7 payload algebra adds a fifth kind:

```text
ModelWorkPayload
    ApprovedTaskRevision taskRevision
    String snapshotId
    Set<String> allowedTools
    ModelInvocationExecutionInput executionInput

ModelInvocationExecutionInput
    String targetPath
    String expectedResponseSha256
    ModelExecutionProfile executionProfile
```

All four payload components and all three execution-input components are mandatory.
The target and digest use the existing `WorkPayload.ExecutionInput` bounds. The profile
is the exact complete RFC-0014 value and is not flattened, copied component by
component into another public value, made optional, or replaced by a map, blob,
reference, digest, registry key, or ambient lookup.

`ModelWorkPayload` is a new `com.enhancer.bus` value and the fifth permitted
`MessagePayload`. This introduces one narrow future dependency from `bus` to the pure,
immutable `model` value. The model package remains unaware of bus or runtime, so the
dependency remains acyclic.

The existing `WorkPayload` and optional `WorkPayload.ExecutionInput` remain unchanged.
They retain the read-file path, including the current absent-input fallback. Adding an
optional profile there is rejected because it would represent missing-profile model
work, irrelevant-profile read work, and nested partial states.

Payload kind is the execution discriminator. New `ModelWorkPayload` requires
`allowedTools` to contain `model-invoke`; a task may also permit `read-file`, but the
current read-file-first scope precedence must not steal a typed model invocation.
`WorkPayload` remains the read-file representation. A legacy `WorkPayload` that names
only `model-invoke` does not become profiled work merely because its Tool scope looks
like model work.

## Capability And Authority Separation

`ModelWorkPayload` and `ModelInvocationExecutionInput` contain no authoritative-
capability field. The durable shape remains:

```text
active WorkItem.requiredCapability       // separate governed projection
active WorkItem.workMessage.payload      // untrusted ModelWorkPayload/profile
```

The profile's `requiredCapability` remains untrusted requirement data. A payload,
codec, manifest, queue, runtime record, migration, or constructor must neither copy
that label into `WorkItem.requiredCapability` nor enforce equality as self-
certification. A later caller passes the exact active WorkItem projection unchanged to
RFC-0016, where mismatch remains observable as
`REQUIRED_CAPABILITY_MISMATCH`.

Likewise, capability is not model class. A later caller must source
`ModelRequest.modelClass` from the retained profile's model-class requirement and let
RFC-0015 enforce exact equality. The current Scheduler mapping from required capability
to model class remains legacy behavior and is not accepted by this RFC.

The WorkItem, profile, manifest, envelope, and queue retain data inside an already
governed lifecycle; none creates a capability grant. They add no provider, endpoint,
destination, credential, network, remote-transmission, spend, Tool, or gateway
authority.

## Canonical Profile Representation

The `model-work-payload-v1` canonical representation writes the profile in RFC-0014
constructor order:

1. schema version;
2. required capability;
3. model class;
4. locality enum name;
5. reasoning enum name;
6. minimum context tokens;
7. input, output, and total token-budget integers;
8. currency and maximum cost microunits;
9. maximum invocation duration seconds and nanoseconds; and
10. data-classification enum name.

Integers use fixed signed widths, collections retain the existing canonical ordering,
and strings retain the existing bounded UTF-8 framing. Duration uses exact seconds and
nanoseconds so canonical decoding does not silently truncate or reinterpret precision.
The decoder reconstructs the nested RFC-0014 values and `ModelExecutionProfile`, then
requires canonical re-encoding to match the source bytes.

Unknown payload kind, profile schema, or enum; invalid label, budget, currency, or
duration; truncated or oversized framing; duplicate set element; corrupt integrity;
and trailing bytes are permanent fail-closed errors. No local-looking model label,
`LOCAL_ONLY`, zero cost, constant, old request, or current Scheduler default fills a
missing field.

Every profile component participates in value equality, canonical bytes, envelope
integrity, duplicate identity, and recovery comparison. Changing any component while
reusing a message, submission, or WorkItem identity is a conflict before queue
mutation.

## Version Family

The coordinated future representation family is:

| Owner | Current | Model-work-aware representation |
|---|---|---|
| Profile | `model-execution-profile-v1` | unchanged exact value |
| Payload algebra | four kinds | add `model-work-payload-v1` / `MODEL_WORK` |
| Model work envelope | unavailable | payload-sensitive `message-envelope-v2` |
| Model work spool | unavailable | `transport-spool-v2` |
| Submission manifest | v2 | v3 |
| Scheduler queue | v3 | v4 |
| AgentRuntime | v4 | v5 |
| Model RunRecord | unavailable in v1 | model-run payload v2 before execution |

The message and spool transition is payload-sensitive, not a global replacement.
Existing `WorkPayload`, `ResultPayload`, `ControlPayload`, and `HandoffPayload` continue
to encode with their exact `message-envelope-v1` and `transport-spool-v1` bytes.
`ModelWorkPayload` alone selects envelope v2 and spool v2. A dual reader dispatches on
the explicit codec/envelope/payload discriminators and rejects cross-family mixtures.

This restriction is mandatory because the accepted Gate 12 detached cancellation
grant includes the existing global message-envelope version in its canonical signed
bytes. RFC-0018 must not change that constant, existing cancellation signatures, or
the golden bytes of any current payload. A global envelope bump would require a
separate trust-contract migration.

Manifest v3, queue v4, and AgentRuntime v5 embed the exact message envelope and support
both unchanged legacy read-file envelopes and new model-work envelopes. They do not
repeat profile components. `WorkItem` gains no new component or independent durable
version; its validation and projections later become exhaustive over `WorkPayload` and
`ModelWorkPayload` while its capability stays separate.

Pending-finalization checkpoints, timeout facts, runtime events, result payloads, and
external-effect ledgers store identities or existing result data rather than model
input and therefore keep their current schemas. Their readers must still resolve those
identities to the exact current queue/runtime WorkItem before acting.

## Durable Source Closure

Every durable holder needed after recovery embeds the same exact envelope/profile:

- submission manifest v3 binds it to submission identity, queue identity, capacity,
  priority, and the separate required capability;
- queue v4 retains it in admission history, pending work, and active work;
- AgentRuntime v5 retains it in the active Goal and every retry-capable state; and
- process-isolated execution spools the exact model-work envelope in spool v2.

The profile may not live only in a manifest, sidecar, generic Tool arguments,
repository file, environment variable, provider configuration, registry, or separate
store. Queue execution, process isolation, and runtime recovery must not join ambient
stores to reconstruct an invocation. A reference is also insufficient because its
target could disappear or change between attempts.

The bounded recovery closure is point-resolved from explicit queue, work, message,
Goal, AgentRun, submission, and spool identities. It is never discovered by scanning
arbitrary repository or storage roots.

## Submission, Replay, And Identity

New profiled model work enters only as a complete `ModelWorkPayload`. Durable
submission publishes manifest v3 before queue creation or admission and retains the
exact separate required-capability projection already governed by that caller.

Exact duplicate submission and message replay are revision-free successes. Reuse of
any submission, message, WorkItem, Goal, or AgentRun identity with changed target,
digest, Tool scope, capability projection, profile component, or other retained field
fails before mutation. The comparison uses decoded values and canonical bytes; a
semantically invalid alternate encoding is never normalized into an accepted replay.

A profile change is a new requirement and therefore requires new submission, message,
and work identities. Retry keeps the exact persisted profile. It never refreshes the
profile from configuration or reuses an old identity with new profile content.

The existing `scheduler-receive-work` command remains unsupported for
`ModelWorkPayload`. Its separate `--required-capability` text is arbitrary first-use
input and bypasses the governed submission source. Exact replay can detect later
changes but cannot authenticate the first value. Supporting external model-work receive
requires a later accepted capability-source/admission-intent contract keyed to the
exact message and queue; the receiver must never copy capability from the profile.

## Retry And Fresh Admission

Persistence retains requirements, not decisions. Every attempt that could reach a
model gateway must:

1. load the exact active WorkItem and retained profile;
2. resolve the exact active governed `ApprovedTask` and bind its revision to the
   WorkItem;
3. construct one exact `ExecutionPolicy` and pass that same instance to both RFC-0016
   admission and `ToolExecutor`;
4. project the exact active `WorkItem.requiredCapability` unchanged;
5. construct the exact `ModelRequest` and RFC-0015 `ProfiledModelRequest`; and
6. evaluate RFC-0016 synchronously immediately before later candidate suitability and
   gateway execution.

`Admitted` is ephemeral. It must never appear in an envelope, manifest, queue,
AgentRuntime, checkpoint, RunRecord, result, cache, lease, retry decision, or recovery
artifact.

The current Scheduler synthesizes an `ApprovedTask` inside execution and constructs a
four-second model request under a five-second Tool policy. RFC-0018 does not certify
that caller. A transported profile with a longer otherwise-valid ceiling fails
RFC-0015 or RFC-0016; it is not clamped, rewritten, or replaced by the current
constant. Later integration must resolve the exact governed task and preserve the same
policy instance across admission and Tool execution.

Recovery of an already durable verified RunRecord performs no gateway invocation and
therefore does not reuse or recreate `Admitted`. It verifies complete provenance and
resumes only the remaining deterministic finalization.

## Process Isolation And Result Validation

The parent spools only the untrusted exact `ModelWorkPayload`. It separately projects
the active WorkItem identity and required capability into fixed parent-controlled
launcher arguments, as the current isolation boundary already does. The child
reconstructs one WorkItem from those separate values and the exact spool envelope.
On retry or recovery, the parent reprojects them from current queue/runtime state,
never from the profile or spool.

Existing exact spool re-entry equality therefore covers the complete profile. A
changed profile, capability argument, WorkItem identity, or message fails before child
execution. The capability argument is an audit/input projection, not a grant or
reusable token.

Model results may continue to use the existing result-envelope bytes, causally bound to
the model-work message identity. Before accepting a child claim, the parent compares
the exact active WorkItem and future model RunRecord against:

- work, message, task, snapshot, Goal, AgentRun, and causation identities;
- `model-invoke`, target path, and expected-response digest;
- the unchanged required-capability projection;
- every complete profile component;
- request model class, timeout, and response-character ceiling;
- the exact policy snapshot, Tool result, digest, and verification status; and
- the absence of any stored admission decision.

Any mismatch fails closed before queue completion or finalization.

## RunRecord Provenance Boundary

RunRecord payload v1 does not retain the complete profile, WorkItem/message identity,
or independent capability projection needed for standalone profiled-invocation audit.
Therefore profile-aware Scheduler gateway execution remains blocked until a
model-specific RunRecord payload v2 is accepted and implemented.

That v2 record must retain the exact WorkItem and work-message identities, the
unchanged required-capability projection as audit data, the exact complete profile,
and the existing request, policy snapshot, result, digest, evidence, and verification
data. It must not retain `Admitted`. Read-file RunRecord v1 remains byte-compatible.

RFC-0018 names this required coordinated boundary but does not define or implement the
record schema. Until it exists, current RunRecord plus runtime lookup must not be
described as complete-profile provenance, and model-work transport must not be used to
enable gateway execution.

## Legacy Classification And Migration

Ordinary future readers are current-version readers. Legacy decoding occurs only in an
explicit stopped-owner migration; normal execution never performs read-old/write-new
conversion.

Migration classifies legacy `WorkPayload` by the current deterministic execution rule:

- a payload whose Tool scope contains `read-file`, including a mixed scope, is legacy
  read-file work and may migrate losslessly;
- a payload that excludes `read-file` and contains `model-invoke` is unprofiled legacy
  model work and is not convertible; and
- a payload naming neither executable Tool remains invalid and is not repaired.

Lossless read-file migration preserves the exact payload kind, identities, timestamps,
task revision, snapshot, Tool set, optional target/digest, absent-input source fallback,
separate capability, priority, dependencies, attempts, leases, status, and history. It
reframes only the owning manifest, queue, runtime, or spool representation.

Unprofiled legacy model work never becomes `ModelWorkPayload`. Any pending, active,
retryable, recovery-preferred, or otherwise executable instance yields the typed
permanent result `UNMIGRATABLE_LEGACY_MODEL_WORK` / `PROFILE_REQUIRED` during preflight,
before the first target write. Terminal history may be retained only when every owning
contract proves it can never be admitted, retried, recovered, or finalized into new
execution. A replay-capable manifest cannot provide that proof and therefore blocks
migration. Ambiguity refuses the containing artifact without modifying it.

A later operator may create new profiled work only through a separately governed
submission with a complete profile and entirely new identities. Migration may not
borrow values from that submission or rewrite an old identity.

Existing one-step migration chains remain ordered: manifest v1 first uses the accepted
v1-to-v2 path before v2-to-v3; queue v2 first uses v2-to-v3 before v3-to-v4;
AgentRuntime v1 through v3 remain unsupported, while only v4-to-v5 is eligible.
Unknown, future, corrupt, partial, or source-drifted artifacts remain byte-preserved and
fail closed.

## Stopped-Owner Cutover And Crash Recovery

The coordinated cutover uses explicit bounded roots and keeps the affected Scheduler,
publisher, receiver, and isolated worker stopped. Before any replacement it:

1. point-resolves the complete named queue/runtime/submission/spool closure;
2. validates integrity, identities, cross-store exact envelope/profile equality, and
   legacy classification for every source;
3. prepares every same-directory candidate and rereads it; and
4. proves that no unprofiled executable model work or unsupported schema remains.

Only after complete preflight may it publish consumer-first:

1. isolated result point, if any, without changing its semantic bytes;
2. isolated work spool v2;
3. active AgentRuntime v5;
4. Scheduler queue v4;
5. submission manifests v3; and
6. explicitly named pending or acknowledged ingress spool points.

Each atomic replacement rechecks the exact source bytes and digest immediately before
publication. Pending-finalization identities and external-effect ledgers remain
unchanged and are reread for binding, not rewritten merely to synchronize versions.

Re-entry classifies an exact current target as `ALREADY_CURRENT`, resumes at the first
old point, never rolls back or rewrites a current artifact, and refuses semantic or
source drift. A crash may leave a fail-closed mixed-version prefix; all operating
commands remain stopped until the entire current-version closure rereads successfully.
No cross-store atomicity or directory-wide discovery is claimed.

Install dual readers and recovery validators before v2 model-work writers. Enable new
model-work submission only after the stopped closure is current. Never enable profiled
gateway execution until the later RunRecord v2, exact-task source, same-policy wiring,
fresh admission, candidate suitability, and local/outbound provider boundaries are
also accepted, implemented, and freshly verified.

## Compatibility

This accepted specification changes no current Java source, binary schema, artifact,
command, signature, golden byte, queue, runtime, Tool, adapter, fake, gateway, or
behavior. Existing read-file and legacy model paths remain exactly as they are today;
neither becomes RFC-0016/RFC-0018-integrated merely because this design is accepted.

The future implementation must preserve golden bytes for all four existing payload
kinds and detached cancellation signing, add the model-work representation without
changing current public record shapes unnecessarily, and keep profile parsing out of
generic CLI/Tool string maps.

## Rejected Alternatives

- Adding `Optional<ModelExecutionProfile>` to current `WorkPayload` or its optional
  execution input is rejected because it creates partial and mixed states and changes
  every Gate 7 consumer.
- Adding the profile or a profile reference only to `WorkItem` is rejected because the
  message identity, process spool, submission replay, and child recovery would not bind
  the value.
- Manifest-only, registry, repository, environment, sidecar, or provider lookup is
  rejected because later holders and process recovery would depend on mutable ambient
  state.
- Flattening ten profile components or serializing an opaque blob is rejected because
  it duplicates or bypasses RFC-0014 validation.
- Copying profile capability into WorkItem, using WorkItem capability as model class,
  or accepting external CLI capability text as authority is rejected.
- A global envelope/spool version replacement is rejected because it would change
  existing payload bytes and the Gate 12 signed cancellation contract.
- Silent legacy-model upgrade, same-identity profile replacement, fallback constants,
  timeout clamping, and profile refresh on retry are rejected.
- Persisting or caching `Admitted` is rejected because it is neither evidence nor an
  authorization token.
- Treating transport, `LOCAL_ONLY`, zero cost, or a local-looking label as candidate,
  provider, network, transmission, spend, or gateway authority is rejected.

## Follow-Up Implementation And Verification

A later separately authorized Dynamic Workflow should proceed RED-first and version
the complete representation family together. Required tests include:

- exact fifth-kind and mandatory-input shape, complete profile round-trip, and no
  optional, flattened, capability-authority, provider, route, credential, network,
  spend, decision, or gateway fields;
- profile-component tamper, unknown schema/enum, truncation, trailing-byte, and reused-
  identity conflicts;
- golden-byte compatibility for existing payloads and detached cancellation signing;
- manifest v3, queue v4, AgentRuntime v5, spool v2, and cross-store exact recovery;
- lossless legacy read-file migration including absent input, and zero-write preflight
  refusal of executable legacy model work;
- crash/re-entry, source-drift, `ALREADY_CURRENT`, mixed-version stop, and consumer-
  first cutover behavior;
- process-child separation of exact profile and independent capability, with parent
  tamper rejection;
- external receiver refusal without a governed capability source;
- exact profile retention across retry with fresh RFC-0015/RFC-0016 evaluation before
  every actual model attempt; and
- model RunRecord v2 standalone provenance and parent result binding without stored
  `Admitted`.

No implementation increment may enable provider or gateway execution merely because
transport tests pass. Candidate suitability, outbound/local provider proof, and any
network, credential, or paid-service behavior remain separately accepted work.

## Exclusions

- Java implementation, schema migration, artifact rewrite, command change, caller
  cutover, or current-runtime behavior change;
- model RunRecord v2 implementation, exact-task resolver, policy wiring, admission
  integration, candidate suitability, gateway or adapter execution;
- route, provider, endpoint, destination, credential, network, remote transmission,
  spend, pricing, tokenizer, usage normalization, caching, fallback, or streaming;
- MCP, prompt scanning, classification inference, redaction, quality evaluation,
  capability maturity, operational readiness, release, deployment, push, or merge.

## Prompt Book

### Prompt: Implement The Typed Model-Work Value And Golden Wire Family

Add the fifth `ModelWorkPayload` and mandatory execution input RED-first, preserve all
existing message/spool/cancellation golden bytes, and round-trip one exact complete
RFC-0014 profile through the model-work-only v2 family without adding authority.

### Prompt: Implement Coordinated Durable Model-Work Migration

Version manifest, queue, AgentRuntime, and process spool together under the stopped-
owner preflight and consumer-first crash/re-entry contract; migrate legacy read-file
work losslessly and refuse unprofiled executable legacy model work before any write.

### Prompt: Define Model RunRecord V2 And Admission Integration

Define the standalone complete-profile provenance record and exact active-task/policy
source required before any Scheduler model attempt can evaluate RFC-0015/RFC-0016 and
approach a later candidate-suitability or gateway boundary.
