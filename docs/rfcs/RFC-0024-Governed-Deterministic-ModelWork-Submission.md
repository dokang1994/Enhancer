# RFC-0024: Governed Deterministic ModelWork Submission

Status: Accepted

## Purpose

Define the smallest caller-specific source that can create and durably admit one
complete typed `ModelWorkPayload` for the internally Integrated RFC-0023
deterministic-fake process path. The source preserves one exact untrusted
`ModelExecutionProfile`, obtains `WorkItem.requiredCapability` independently from a
repository-owned deterministic-fake boundary, and reuses the existing manifest-first
submission and replay contracts.

This RFC defines a future internal producer. It does not implement that producer,
submit work now, expose a supported command, enable typed spool receive, or authorize a
provider, network, credential, or paid service.

## Relationship To Existing Contracts

RFC-0017 continues to require one complete profile and a separately governed
authoritative capability projection. RFC-0018 continues to own `ModelWorkPayload`, the
exact profile-bearing envelope/spool v2 family, submission manifest v3, queue v4, and
AgentRuntime v5. RFC-0019 continues to own exact task resolution, fresh invocation
preparation, Model RunRecord v2, and the rule that current external Work receive cannot
use arbitrary capability text. RFC-0020 through RFC-0022 own the fixed local candidate,
token semantics, suitability, exact-request budget, and one-call invoker. RFC-0023 owns
the internally Integrated child, parent, finalizer, retry, and recovery path.

The existing `DurableWorkSubmissionService` remains the downstream admission owner.
It persists the exact `DurableSubmissionManifest` before queue creation and delegates
admission to `DurableWorkItemAdmissionHandler`. `GeneratedSubmissionIdentities` remains
the existing source for stable queue, correlation, and logical-run identities from one
caller-retained submission UUID.

RFC-0024 owns only:

- the caller-specific deterministic-fake submission request;
- the independent fixed capability source used by that producer;
- first-use task, snapshot, envelope, and manifest construction;
- manifest-first exact replay and caller-intent comparison;
- the producer-to-existing-durable-submission connection; and
- the RED-first implementation and internal integration sequence.

It does not own process execution configuration, a user-facing profile format,
transport publication or receive, or supported Scheduler command composition.

## Current Gap

The typed process path has no production source. Production code constructs
`ModelWorkPayload` only while decoding already-existing wire bytes; test fixtures are
the only callers that create new typed envelopes. The legacy explicit and generated
submission commands create `WorkPayload`, and the external Work receiver correctly
rejects ModelWork because its caller-supplied capability is not a governed source.

The internally model-aware `DurableAgentRunWorker` composition is also not a supported
CLI composition. Directly widening a legacy command or receiver would therefore either
turn arbitrary input into capability provenance or admit work that the supported
execution command cannot safely configure.

## Selected Boundary

The first producer is deterministic-fake-specific, internal, and direct to durable
submission. It accepts one complete typed profile as data and supplies no provider,
route, endpoint, or network option. It does not use the file spool or Message Bus.

The proposed immutable request carries exactly:

- one canonical caller-retained submission UUID;
- the expected active task identity;
- one bounded producer identity;
- one bounded relative target path;
- one lowercase expected-response SHA-256;
- one exact already-valid `ModelExecutionProfile`;
- one bounded queue capacity; and
- one `NORMAL` or `EXPEDITED` Scheduler priority.

The explicit project root, manifest store, queue store, clock, Context Reader,
Approved Task reader, and Workspace collector are construction dependencies or method
inputs of the application boundary, not fields copied into the durable request. The
request contains no required-capability field, queue identity, correlation identity,
logical-run identity, occurrence time, allowed-Tool set, snapshot identity, execution
policy, candidate, gateway, or admission decision.

## Independent Deterministic Capability Source

The producer has one separately named repository-owned capability source whose sole
value for this exact boundary is `deterministic-echo`. Its source identity and value are
fixed by the repository implementation and cannot be supplied, selected, overridden,
aliased, normalized, or repaired by the request, profile, target, model class,
candidate instance, envelope, manifest, queue, CLI, environment, repository content,
or ambient configuration.

This is not a fallback constant. It is the only capability projection of a narrowly
typed deterministic-fake producer whose creation is already bounded by the active task
and later execution policy. The implementation must represent the source explicitly,
with private or otherwise closed construction, rather than accepting a general string
and comparing it after the fact.

The value is not a capability grant. The active task must still allow `model-invoke`,
and every execution attempt must still run fresh RFC-0019 preparation and RFC-0016
admission under its exact `ExecutionPolicy`.

The profile remains capable of declaring a different required capability. The producer
must not reject or repair that disagreement, because RFC-0016 owns
`REQUIRED_CAPABILITY_MISMATCH`. The source likewise must not copy from
`DeterministicFakeModelCandidate.requiredCapability()`: candidate construction occurs
later, after admission, and cannot retroactively authorize the WorkItem.

## First-Use Construction

The producer first derives `GeneratedSubmissionIdentities` from the submission UUID and
point-resolves the manifest. Only `MissingSubmissionManifestException` enters first-use
construction. On first use it performs this order:

1. load the complete governed repository context once from the explicit project root;
2. resolve the exact `In Progress` `ApprovedTask` once and require the request task ID;
3. require the task's immutable allowed-Tool set to contain `model-invoke`;
4. capture one occurrence time from the injected clock;
5. collect one repository-memory `WorkspaceSnapshot` from that same context and task;
6. construct one `ModelWorkPayload` from the exact task revision, snapshot identity,
   task Tool scope, request target/digest, and unchanged complete profile;
7. construct one `MessageEnvelope` using the submission UUID as message identity, the
   derived correlation/logical-run identities, no causation, the request producer, and
   the captured occurrence time;
8. construct one `DurableSubmissionManifest` with the derived queue identity, request
   capacity and priority, the independent fixed capability, and the exact envelope; and
9. call the existing `DurableWorkSubmissionService` unchanged.

The producer does not read the prompt, construct `ModelRequest` or `ExecutionPolicy`,
evaluate admission or suitability, bind a candidate, invoke a Tool or gateway, create
evidence, persist a RunRecord, create runtime state, or run a Scheduler cycle.

Task content and Workspace observations provide provenance and Tool scope; neither
supplies the independent model capability. Profile locality, reasoning, budget, cost,
and classification are retained requirements only.

## Durable Ordering And Recovery

The existing submission order remains authoritative:

```text
exact manifest -> queue create/resolve -> exact durable admission
```

A failure before manifest persistence leaves no queue or admission. A failure after
manifest persistence re-enters from that exact intent. A failure after queue creation
recovers the exact capacity-bound queue. A failure after admission exact-replays without
another WorkItem or queue revision. No new receipt or cross-store transaction is added.

The manifest is integrity and replay state, not self-authenticating authority. Its
fixed capability value is accepted only because this producer supplied it from the
closed source before persistence. A later external receiver may consume an existing
manifest as admission intent, but defining or exposing that receiver remains separate.

## Exact Replay

Every call resolves the exact manifest before consulting the clock, Context Reader,
Approved Task reader, or Workspace collector. An existing manifest is historical
recovery authority for this submission identity and must not trigger recapture.

Replay validates:

- manifest submission/message identity equals the request submission UUID;
- queue, correlation, and logical-run identities equal the versioned derived values;
- causation remains absent and producer equals the request producer;
- queue capacity and priority equal the request;
- manifest required capability equals the closed fixed source value;
- payload kind is exactly ModelWork;
- retained task identity equals the request task identity;
- target and expected-response digest equal the request;
- the complete retained profile equals the request profile; and
- the retained task revision, snapshot, Tool scope, occurrence time, and envelope stay
  otherwise unchanged.

An exact replay delegates to the existing durable service and changes neither manifest
bytes nor queue revision. Task completion or later repository drift does not rewrite
historical intent. A caller that wants changed task, producer, target, digest, profile,
capacity, or priority intent must use a new submission identity.

Cross-kind content, a changed derived identity, a different fixed capability, missing
or malformed execution input, corrupt or unsupported manifest state, queue-capacity
drift, or changed content under the same identity fails closed before a new admission.

## Refusal And Execution Semantics

Intrinsic request/profile failure occurs before store access. On first use, missing,
inactive, mismatched, malformed, oversized, outside-root, or invalid-UTF-8 repository
context and absent `model-invoke` scope fail before manifest or queue mutation.

The producer deliberately does not pre-run RFC-0016. A complete profile whose
capability, model class, locality, reasoning, capacity, cost, time, or classification
cannot satisfy the deterministic candidate may still be retained as exact requested
work. The later fresh execution attempt owns the typed refusal. Under RFC-0023, a
pre-call refusal creates no ToolResult, evidence, Model RunRecord, Result, queue
disposition, or automatic tight retry and leaves the active prefix recoverable.

Submission persistence, queue admission, process invocation, Model RunRecord
publication, independent verification, and terminal queue disposition remain distinct
facts. A successful producer call means only that immutable intent and exact queue
admission are durable. It is not model execution or completion authority.

## Consumer And Reachability Sequence

The first implementation may connect the producer only to the existing internal
model-aware worker in test-owned temporary storage. This gives the new source a real
downstream consumer while preserving the absence of a supported entry point.

A later supported model execution path must separately define all
`ModelProcessExecutionConfiguration` sources and expose a model-aware Scheduler
composition before any supported typed submission or receive command can make work
reachable. A future typed spool receiver must use a pre-existing exact manifest for
capability and priority and must never accept either from CLI text or the profile.

The ordered future sequence is:

1. implement this internal producer and its manifest/queue integration;
2. define and implement an explicit supported model-aware Scheduler composition;
3. define an interface-owned complete-profile input format and supported typed
   submission or spool-publisher surface; and
4. only if transport ingress is still required, add a manifest-authorized ModelWork
   receiver while keeping the legacy Work receiver unchanged.

No step gains authority from the existence of the later step.

## Schema Sufficiency

Current formats are sufficient:

- ModelWork envelope and spool family v2 retain the complete profile;
- submission manifest v3 retains queue, capacity, priority, independent capability,
  and exact envelope;
- Scheduler queue v4 and AgentRuntime v5 retain the exact WorkItem;
- pending-finalization v2 retains attempt/reference recovery;
- Model RunRecord v2 retains exact model provenance and lifecycle; and
- Result and process-timeout formats already support RFC-0023 recovery.

RFC-0024 adds no durable field, version, migration, profile parser, sidecar, registry,
or receipt. It does not add candidate identity, token counts, `Admitted`, `Suitable`,
`Ready`, response usage, policy-object identity, or refusal provenance to Model
RunRecord v2. Any such change requires a separate compatibility decision.

## Required RED-First Implementation Sequence

A later implementation task must remain sequential:

1. add failing tests for the closed submission request, independent fixed capability
   source, absence of a request capability field, and capability/profile mismatch;
2. add failing first-use and replay tests over the existing manifest and queue stores,
   including context/clock non-consultation on replay and every caller-intent drift;
3. implement the minimum producer through unchanged `DurableWorkSubmissionService`;
4. connect one test-owned producer -> manifest -> queue -> existing internal
   model-aware process worker path for verified completion and one typed pre-call
   refusal without adding a supported caller; and
5. run full legacy/v1, durable-format, RFC-0017-through-RFC-0023, architecture, and
   Markdown governance regression before closure.

Named evidence must cover the new request/source/service suites and extend or mirror
`GeneratedInputSubmissionServiceTest`, `FileSystemSubmissionManifestStoreTest`,
`DurableWorkSubmissionServiceIntegrationTest`,
`TypedModelProcessExecutionIntegrationTest`, `ModelCandidateLocalityBoundaryTest`,
and the durable queue/runtime/recovery suites. Source guards must prove that no CLI,
spool receiver, provider, network, credential, or legacy execution path constructs the
producer.

## Rejected Alternatives

- Widening `GeneratedInputSubmissionService` is rejected because it casts to
  `WorkPayload` and changing it would merge two payload contracts and enlarge legacy
  replay risk.
- Adding `requiredCapability` to the request is rejected because arbitrary caller data
  is not the missing governed source.
- Copying capability from the profile is rejected because untrusted requirements
  cannot self-certify and RFC-0016 must retain its mismatch outcome.
- Copying capability from the candidate is rejected because candidate suitability is
  downstream of admission.
- Extending `scheduler-submit`, `scheduler-submit-generated`,
  `scheduler-spool-work`, or `scheduler-receive-work` is rejected because those are
  legacy Work surfaces with different input and recovery contracts.
- Receiver-first is rejected for this sequence because no current external producer
  can establish the required manifest provenance and no supported Scheduler command
  carries the internal typed execution configuration.
- Adding a profile file, ten CLI flags, optional profile fields, a registry, or ambient
  configuration is rejected because this RFC owns no interface input format and may not
  introduce defaults or partial profiles.
- Treating manifest integrity, queue admission, `LOCAL_ONLY`, or deterministic fake
  selection as execution authority is rejected.

## Exclusions

- Java or test-source implementation in this documentation task;
- actual submission, admission, process, Tool, gateway, evidence, RunRecord, runtime,
  retry, recovery, or queue-disposition execution;
- supported CLI/API/editor/Desktop input, direct model command, spool publisher or
  receiver, Message Bus ingress, or runtime-event ingress;
- general candidate or provider selection, router, registry, endpoint, remote
  transmission, network, credentials, pricing, spend, fallback, cache, streaming,
  general tokenizer, or usage normalization;
- profile file/schema/parser, new durable schema, migration, sidecar, receipt, or
  current v2 provenance widening;
- MCP, plugin execution, release, deployment, push, merge, permission change,
  destructive cleanup, or external effect.

## Prompt Book

### Prompt: Implement Governed Deterministic ModelWork Submission

Implement RFC-0024 RED-first as a separate internal producer. Accept one exact complete
profile, source `deterministic-echo` only from the closed repository-owned producer
boundary, preserve capability/profile disagreement for RFC-0016, resolve manifest
before clock/context, and reuse the existing manifest-first durable submission service.
Connect only to the existing test-owned internal typed process path. Do not widen a
legacy command, add a receiver or supported entry point, invoke an external provider,
or change any durable format.
