# RFC-0019: Scheduler Model RunRecord And Admission Preparation

Status: Accepted

## Purpose

Define the additive Model RunRecord payload v2 and the exact Scheduler task, request,
policy, and capability sources required to evaluate RFC-0015 and RFC-0016 for one typed
`ModelWorkPayload`. The contract closes the provenance and preparation gap between
RFC-0018's durable retention family and a later candidate-suitability boundary while
keeping every current typed ModelWork execution guard in place.

This RFC specifies a future implementation and integration boundary. It changes no Java
source, binary schema, artifact, command, or runtime behavior and does not authorize a
gateway invocation. RFC acceptance does not imply capability maturity.

## Relationship To Existing Contracts

RFC-0013 continues to own `ModelRequest`, the gateway port, the deterministic fake, and
the response-character ceiling. RFC-0014 continues to own the complete untrusted
`ModelExecutionProfile`. RFC-0015 continues to own request/profile model-class and
timeout alignment. RFC-0016 continues to own the pure task, policy, capability,
timeout, and locality admission decision. RFC-0017 continues to own caller-source
separation and fresh evaluation. RFC-0018 continues to own typed ModelWork retention,
the current durable schema family, migration, cutover, and independent
`WorkItem.requiredCapability` projection.

RFC-0019 owns only:

- the additive standalone model-invocation RunRecord representation and persistence
  discriminator;
- exact active-task resolution against the WorkItem's retained revision;
- explicit Scheduler request-limit and execution-policy sources;
- the invocation-scoped preparation order through RFC-0015 and RFC-0016;
- the boundary between fresh pre-execution evaluation and post-record deterministic
  recovery; and
- the implementation sequence that must remain blocked before candidate suitability.

It does not define a provider candidate, prove that the deterministic fake is local or
suitable, map a provider, or authorize Tool or gateway execution.

## Current Incompatibility

The current `RunRecord` payload v1 retains one `ApprovedTask`, `ToolRequest`, static
policy decision, Tool result, expected digest, evidence, verification, and lifecycle
stops. It does not retain the WorkItem identity, work-message identity, independent
required-capability projection, or complete execution profile. A runtime lookup cannot
replace standalone audit because the runtime artifact may change, disappear, or belong
to a different recovery prefix.

The current Scheduler model branch is legacy `WorkPayload` behavior. It synthesizes an
`ApprovedTask` from WorkItem projections, uses `WorkItem.requiredCapability` as
`ModelRequest.modelClass`, and constructs a four-second request under a five-second
Tool policy. Those choices are not RFC-0015/RFC-0016 sources for typed ModelWork.

Typed ModelWork therefore remains rejected by the in-process execution boundary,
process-isolated parent, and child before evidence, RunRecord, spool-result, admission,
or gateway activity. This RFC does not remove those guards.

## Additive Record Algebra

The existing public `RunRecord` remains the read-file and legacy lifecycle record. It
does not gain optional profile, message, WorkItem, capability, admission, provider, or
route fields.

The future model-specific value is additive:

```text
ModelRunRecord
    String workItemId
    String requiredCapability
    MessageEnvelope workMessage
    ModelRequest modelRequest
    RunRecord lifecycleRecord
```

`workMessage` must carry one exact `ModelWorkPayload`. Together, the canonical
WorkItem identity, unchanged independent capability, and exact envelope reconstruct the
complete Scheduler work value without introducing a `run -> runtime` package
dependency. The envelope retains message, logical-run, correlation, causation, task
revision, snapshot, Tool scope, target, expected-response digest, and the complete
profile. `modelRequest` retains the exact bounded prompt snapshot, profile-derived
model class, Scheduler-owned timeout, response-character ceiling, and correlation
identity that passed preparation. `lifecycleRecord` retains the existing exact task,
Tool request, policy snapshot, result, evidence, digest, verification, iteration, and
stop semantics.

The value validates structural binding without allowing untrusted profile data to
self-certify authority:

- WorkItem and message identities are canonical, distinct, and unchanged;
- the envelope kind is typed ModelWork and its logical run matches the lifecycle run;
- lifecycle task identity, source document, and Tool scope match the retained task
  revision and payload scope;
- the Tool request is exactly `model-invoke` and its correlation identity matches the
  exact model request;
- prompt target and expected-response digest match the typed execution input;
- request model class comes from the profile model class, never the independent
  capability;
- request timeout and response-character ceiling equal the explicit Scheduler-owned
  invocation limits used for preparation;
- policy, result, expected digest, evidence, verification, and lifecycle invariants
  remain those of the existing `RunRecord`; and
- no `Admitted`, rejection, provider, candidate, route, endpoint, destination,
  credential, network, transmission, price, spend, cache, or fallback value is stored.

The raw value retains the capability and profile as separate fields even when they
disagree. Its constructor does not copy one into the other or treat equality as a grant.
RFC-0016 remains the only pre-execution owner of that comparison. A future execution
finalizer may create a model record only from the exact preparation that actually
passed, but the persisted format contains provenance rather than an admission receipt.

Goal and AgentRun identities remain outside the payload because RFC-0018 does not name
them as standalone model-record fields and the existing deterministic record identity
already derives from that pair. Adding them to the payload requires separate evidence
that the additional duplication is necessary.

## Persistence And Version Dispatch

The existing RunRecord filesystem envelope, magic, timestamp, length, integrity digest,
four-MiB payload bound, artifact suffix, reference prefix, and atomic exact-replay
behavior remain unchanged. The first payload integer becomes the kind/version
discriminator:

```text
1 -> exact existing RunRecord payload v1
2 -> model-run-record-payload-v2
```

Payload v1 encoding and decoding remain byte-for-byte unchanged. Existing v1 artifacts
require no migration and newly written read-file records continue to use version 1.

Payload v2 writes, in canonical order:

1. payload version `2`;
2. canonical WorkItem identity;
3. unchanged independent required capability;
4. one bounded exact `DurableMessageEnvelopeCodec` model-work envelope frame; and
5. the exact bounded `ModelRequest` in canonical field order; and
6. every existing lifecycle field in the same canonical order and representation used
   by RunRecord v1.

The model envelope decoder must select the existing message-envelope v2 family, require
`MODEL_WORK`, and require canonical re-encoding. Unknown version or kind, cross-family
input, malformed UTF-8, invalid nested value, oversized length, digest mismatch,
truncation, noncanonical collection order, and trailing bytes fail closed.

The v1 type-level port remains narrow. An additive `ModelRunRecordStore` and
`ResolvedModelRunRecord` expose model persistence and resolution while
`RunRecordStore.resolve` continues to return only v1. The filesystem implementation may
implement both ports over the same root and reference namespace, but:

- a v1 resolver presented with v2 reports an explicit unsupported record kind and
  never projects the nested lifecycle record as a complete v1 artifact;
- a v2 resolver presented with v1 reports the inverse kind mismatch;
- an existing identity reused across v1/v2 or with changed model content fails before
  rewrite;
- exact replay returns existing metadata without byte or timestamp change; and
- opaque listing may include both kinds, but supported replay and metadata consumers
  must either dispatch explicitly or report unsupported kind rather than corruption.

No current production writer creates payload v2. CLI replay, Project Brain projection,
runtime-event derivation, and Scheduler recovery stay v1-only until their own bounded
v2-aware increments are installed before the first model writer.

## Exact Active Task Resolution

Typed ModelWork execution cannot use a synthesized description or approval string. Each
pre-execution attempt must load the governed project through the existing Repository
Context Reader and resolve `ApprovedTask` through `ApprovedTaskReader`. The task must be
`In Progress` and the loaded source must match the active WorkItem's retained
`ApprovedTaskRevision` exactly:

- task identity;
- source-document path;
- lowercase SHA-256 of the complete source document; and
- exact allowed-Tool set.

The resolved `ApprovedTask` instance is then passed unchanged to RFC-0016 and the Agent
Loop. Goal, AgentRun, envelope producer, model output, prompt content, environment,
current directory, repository text, or caller prose cannot generate or repair its
description, approval evidence, status, Tool scope, or source binding.

Missing, completed, malformed, changed, mismatched, or ambiguously sourced task input
fails before ModelRequest construction, admission, Tool execution, evidence, RunRecord,
gateway, adapter, or other external effect. A changed task does not silently become a
new attempt under the old WorkItem identity; new governed work requires new identities.

This is a concrete Scheduler resolver over existing context and task readers, not a
generic registry, service locator, ambient context, or new source-of-authority record.

## Scheduler Request And Policy Sources

The typed payload supplies only target path, expected-response digest, and complete
profile. The active WorkItem supplies the independent capability. The remaining request
and execution-policy fields are Scheduler-owned inputs and must be explicit.

One immutable caller-provided Scheduler invocation-limits value supplies exactly:

```text
gatewayTimeout
maximumResponseCharacters
```

These values are resource bounds, not provider, network, or spend authority. They are
not parsed from the profile, prompt, Tool arguments, environment, provider metadata, or
ambient defaults. The gateway timeout must satisfy the existing ModelRequest bound, and
the response-character ceiling remains independent from profile token budgets.

The composition root constructs one exact `ExecutionPolicy` instance for the attempt
from its explicit project root, `model-invoke` allowlist, denied Tools, read ceiling,
Tool timeout, and cancellation token. Profile values cannot widen or construct that
policy. The same object instance is supplied to RFC-0016 and, only after all later
gates pass, the existing `ToolExecutor`.

The first later implementation may preserve the currently documented Scheduler-owned
four-second gateway timeout, 65,536 response-character ceiling, and five-second Tool
timeout only when they are explicit named composition inputs. A profile maximum longer
than the gateway timeout fails RFC-0015; the timeout is never clamped, rewritten, or
expanded from untrusted profile data.

## Exact Model Request Preparation

For one attempt, the exact request is constructed as follows:

- correlation identity comes from the attempt's newly created logical evidence run;
- prompt content is read once from the typed target under the same project containment,
  bounded-read, mutable-file, regular-file, and strict-UTF-8 rules owned by the current
  model Tool path;
- model class is the exact profile model class;
- timeout and maximum response characters are the explicit Scheduler invocation
  limits; and
- expected response digest remains separate verification input and is not part of the
  model request.

The one resolved prompt snapshot and exact ModelRequest must be reused by preparation
and the later invocation seam. A second file read that could observe different bytes is
not the same invocation. An implementation may extract the current prompt-resolution
logic into a shared bounded component or introduce an internal prepared-request seam,
but it may not duplicate containment logic, flatten the profile into Tool arguments, or
let `ModelInvokeTool` reconstruct a different request after admission.

The exact request is retained in Model RunRecord v2. Its prompt bound fits inside the
existing four-MiB record payload ceiling; exceeding any request or record bound fails
before a writer publishes an artifact.

## Fresh Composition And Admission

Every attempt that could later approach a gateway follows this order:

1. load the exact active WorkItem and typed model input;
2. resolve and bind the exact active governed task;
3. receive explicit Scheduler invocation limits and construct one exact active
   `ExecutionPolicy` instance;
4. resolve one bounded prompt snapshot and construct one exact `ModelRequest`;
5. compose that request with the retained profile through RFC-0015;
6. evaluate RFC-0016 with the exact resolved task, the same policy instance, and the
   unchanged `WorkItem.requiredCapability`;
7. stop at the still-unimplemented candidate-suitability boundary; and
8. only after a separately accepted candidate/local-gateway contract may a later
   integration invoke a Tool or gateway and finalize Model RunRecord v2.

RFC-0015 constructor failure and every RFC-0016 rejection occur before ToolExecutor,
gateway, evidence, or Model RunRecord publication. The typed decision remains
invocation-local. Neither `Admitted` nor `Rejected` is serialized, cached, transmitted,
replayed, or used as an authorization token.

Because candidate suitability is absent, even `LOCAL_ONLY` plus `Admitted` stops without
invoking the deterministic fake. `POLICY_CONSTRAINED` continues to return
`OUTBOUND_POLICY_REQUIRED`. A local-looking model class, zero cost, public
classification, missing credentials, or the deterministic fake's implementation cannot
prove a candidate or create authority.

This RFC deliberately does not define a terminal Scheduler disposition for a rejected
or candidate-blocked preparation. No production worker is wired until a later accepted
execution task defines that lifecycle mapping without fabricating a ToolResult or
RunRecord for a Tool that did not execute.

## Process Isolation And Result Binding

The later parent continues to spool the exact model-work envelope and passes the active
WorkItem identity and independent capability as separate parent-controlled launcher
arguments. The child reconstructs the exact WorkItem, freshly resolves the active task,
constructs the attempt policy and request, and uses the same child-local policy instance
for admission and ToolExecutor. Object identity is required within the executing
process; the parent does not serialize an `ExecutionPolicy` or admission decision.

Payload kind, not Tool-set precedence, selects model behavior. A typed ModelWork payload
that also allows `read-file` cannot fall into the legacy read-file branch.

A returned model result is a claim. Before accepting its reference, the parent and later
recovery readers resolve Model RunRecord v2 and compare:

- record, WorkItem, work-message, logical-run, task, snapshot, correlation, causation,
  Goal, and AgentRun bindings available from the active recovery prefix;
- unchanged capability plus every profile component;
- prompt target, expected-response digest, request model class, request timeout, and
  response-character ceiling;
- exact policy snapshot, Tool result, evidence, verification, and result-envelope
  status; and
- absence of a stored admission decision.

A v1 record, foreign v2 record, changed capability/profile/request, or partial
projection fails before runtime completion or queue disposition.

## Retry And Recovery Boundary

Before a Model RunRecord v2 reference is durably checkpointed, any later actual attempt
must reload the active task, reconstruct explicit request/policy inputs, and evaluate
RFC-0015/RFC-0016 freshly. Retry never refreshes or replaces the persisted profile and
never carries an earlier `Admitted` decision.

After an exact v2 record is durable, recovery performs no new task lookup, admission,
candidate selection, Tool execution, or gateway invocation. It point-resolves only the
deterministic AgentRun-bound reference, validates the complete record against the exact
current WorkItem and retained recovery prefix, and resumes the remaining deterministic
result/runtime/queue finalization. Current repository-task drift cannot rewrite
historical record provenance or trigger a second invocation.

This separates two facts: fresh authority is mandatory before an external attempt, while
already durable verified evidence is historical input to deterministic finalization.
Neither rule turns a RunRecord into reusable execution authority.

## Compatibility And Migration

RunRecord v1 artifacts and readers require no migration. Model payload v2 is written
only for newly created typed ModelWork attempts after every v2-aware consumer and
recovery validator is installed. Legacy `WorkPayload` model-scope records remain v1 and
uncertified; they are not upgraded, reclassified, or used as profile-aware provenance.

The existing coordinated manifest/queue/runtime migration remains unchanged because it
already retains the exact typed envelope. Model RunRecord v2 has no old model record to
migrate. Unknown, future, corrupt, partial, or cross-kind records remain preserved and
fail closed.

The supported external Work receiver remains `WorkPayload`-only. Arbitrary first-use
capability text cannot become the independent authority source for typed ModelWork.
Supporting model submission or receive requires a separate accepted governed producer
and capability-source contract.

## Required Implementation Sequence

A later Dynamic Workflow must remain sequential:

1. add the Model RunRecord value, explicit model store/resolve boundary, v2 codec, v1
   golden compatibility, and execution-guard regressions without any production writer;
2. add exact active-task resolution and revision/scope binding without execution;
3. add request/policy preparation and fresh RFC-0015/RFC-0016 evaluation that stops
   before candidate suitability and gateway activity;
4. separately define and implement candidate suitability plus a proven-local gateway
   boundary;
5. only then wire process execution, model-v2 finalization, result validation, and all
   recovery/status readers together; and
6. separately add a governed typed ModelWork producer or receiver if desired.

No increment may skip an earlier boundary merely because the deterministic fake has no
network implementation.

## Verification Contract

Future RED-first implementation must cover:

- exact ModelRunRecord shape, complete standalone provenance, capability/profile
  separation, and absence of decision/provider/route/credential/network/spend fields;
- literal v1 golden decoding and byte-identical new v1 encoding;
- v2 round-trip, exact replay without rewrite, cross-kind identity conflict, and
  unknown/corrupt/truncated/trailing/noncanonical refusal;
- profile-component and WorkItem/message/capability/request/policy/result tamper
  rejection;
- exact active-task success plus missing, completed, changed-digest, task-ID,
  source-path, and Tool-scope refusal before model preparation;
- profile model class rather than capability, unchanged capability projection to
  RFC-0016, explicit request limits, same-policy-instance wiring, and no timeout clamp;
- every RFC-0016 rejection with zero Tool/gateway/record activity and no cached decision;
- payload-kind precedence for mixed-scope typed ModelWork;
- parent/child separation and complete v2 result binding;
- pre-record retry performing fresh preparation and post-record recovery performing no
  second admission or invocation; and
- unchanged external-receiver, in-process, process-parent, and child execution guards
  until their owning increments are authorized.

For this documentation-only task, RFC/decision/architecture/index/ownership/dynamic-
workflow/approved-task/task-justification/planner governance, `git diff --check`, and
the full README-owned Java 17 regression are the verification boundary.

## Rejected Alternatives

- Optional model fields on `RunRecord` are rejected because they create partial states,
  alter the v1 public shape, and risk changing legacy bytes.
- Projecting v2 through `ResolvedRunRecord.record()` is rejected because v1-only
  finalizers and recovery readers could silently accept incomplete provenance.
- Persisting only a profile digest, WorkItem lookup, runtime reference, sidecar, or
  manifest reference is rejected because standalone replay would depend on mutable
  ambient state.
- Copying profile capability into WorkItem capability or using capability as model
  class is rejected as self-certification and semantic conflation.
- Synthesizing `ApprovedTask`, accepting completed or changed task documents, or using
  Goal/AgentRun prose as approval evidence is rejected.
- Profile-derived policy timeout, response-character/token conversion, timeout clamping,
  default limits, environment lookup, registry lookup, and a cached admission decision
  are rejected.
- Re-reading the prompt after admission is rejected because it can invoke a request
  different from the admitted value.
- Treating the deterministic fake, `LOCAL_ONLY`, public classification, zero cost, or
  absent credentials as candidate/locality proof is rejected.

## Exclusions

- Java implementation, binary writer/reader change, artifact migration, command change,
  caller cutover, task resolver, request factory, policy wiring, admission invocation,
  finalizer, result handler, recovery reader, process worker, submission, or receiver;
- runtime disposition for pre-execution refusal, candidate suitability, model catalog,
  route, provider, endpoint, destination, gateway execution, network or remote
  transmission;
- credentials, private keys, paid services, pricing, currency conversion, tokenizer,
  usage normalization, caching, fallback, streaming, prompt scanning, classification
  inference, redaction, prompt-injection resistance, attribution, or quality evaluation;
- MCP, plugin protocols, capability maturity, operational readiness, release,
  deployment, push, or merge.

## Prompt Book

### Prompt: Implement Additive Model RunRecord V2

Add the separate model record and explicit v2 persistence boundary RED-first, preserve
literal and newly encoded RunRecord v1 bytes, and keep every typed ModelWork execution
guard in place.

### Prompt: Implement Exact Scheduler Model Preparation

Resolve the exact active task, construct the exact model request from explicit
Scheduler limits and the retained profile, reuse one policy instance, and evaluate
RFC-0015/RFC-0016 freshly while stopping before candidate or gateway execution.
