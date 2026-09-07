# RFC-0025: Supported Deterministic-Fake Model-Aware Scheduler Composition

Status: Accepted

## Purpose

Define the smallest supported foreground Scheduler execution composition that can run
the already retained typed `ModelWorkPayload` through the existing RFC-0023
deterministic-fake process path. The composition supplies every current
`ModelProcessExecutionConfiguration` input from an explicit bounded source while
preserving legacy Work execution, durable recovery, optional runtime-event publication,
and all RFC-0016 through RFC-0024 authority separations.

This RFC does not implement the composition. It does not create or receive ModelWork,
define an interface profile format, select a provider, or authorize network, credential,
spend, MCP, background, or remote execution.

## Relationship To Existing Contracts

RFC-0016 owns fresh model-invocation admission. RFC-0017 requires a complete profile,
exact task and policy, and independently governed capability. RFC-0018 and RFC-0019 own
the typed Scheduler payload and additive RunRecord preparation. RFC-0020 through
RFC-0022 own the closed deterministic candidate, Unicode-scalar budget, suitability,
exact-request preparation, and one-call fake invocation seam.

RFC-0023 implements the payload-kind-aware parent/child worker and Model RunRecord v2
validation, but its model-aware `DurableAgentRunWorker` composition is package-local and
test-owned. RFC-0024 implements the governed typed producer, but deliberately gives it
no supported entry point and orders supported Scheduler composition before typed input.

The existing supported `scheduler-cycle`, `scheduler-drain`, and `scheduler-service`
commands own foreground lifecycle, queue recovery, runtime/effect/checkpoint stores,
process isolation, retry, lease, optional runtime-event publication, and bounded output.
RFC-0025 adds only an explicit way for those commands to select their already
implemented model-aware worker composition.

## Current Gap

The three supported Scheduler execution commands always call the public legacy
`DurableAgentRunWorker.processIsolated` composition. They do not construct
`ModelProcessExecutionConfiguration`, do not supply the model-aware parent validator,
and do not append the typed child configuration. A typed item can therefore be retained
by the durable formats but is not supported for execution by these commands.

Directly making the internal overload public would leave its scalar policy and store
sources ambiguous. Silently applying fixed limits would hide operator-owned execution
policy. Adding typed submission first would make work reachable before a supported
consumer exists. Replacing the legacy composition would risk changing existing Work
behavior. The selection and every input must therefore be explicit, optional as one
whole, and shared by all three commands.

## Selected Supported Surface

The existing commands keep their names and all existing arguments. They gain one
optional all-or-none model execution group:

- `--model-execution deterministic-fake-v2`;
- `--model-gateway-timeout-millis <positive bounded integer>`;
- `--model-maximum-response-characters <positive bounded integer>`;
- `--model-maximum-read-bytes <positive bounded integer>`;
- `--model-tool-timeout-millis <positive bounded integer>`; and
- optional repeatable `--model-denied-tool <bounded non-blank Tool name>` values.

`deterministic-fake-v2` is an exact case-sensitive closed value. The selector and all
four numeric configuration values are required together; absence of repeated denied
Tools means the empty set. At most 16 unique denied Tool names of at most 128 characters
each are accepted, and they are canonically sorted before the child invocation.
Supplying none of the model options selects the unchanged legacy-only composition.
Supplying a partial group, a denied Tool without the selector, an unknown selector, a
duplicate Tool, or an out-of-bound value is a usage failure before queue or store
access.

No model argument carries task identity, target, expected digest, profile, required
capability, model class, candidate identity, token semantics, prompt, response,
provider, endpoint, credential, data classification, price, or network destination.
Those values remain owned by their existing contracts or unsupported.

## Complete Configuration Sources

When the group is present, the parent constructs exactly one current
`ModelProcessExecutionConfiguration` from these sources:

| Configuration fact | Exact source |
|---|---|
| Gateway timeout | `--model-gateway-timeout-millis` |
| Maximum response characters | `--model-maximum-response-characters` |
| Maximum prompt-read bytes | `--model-maximum-read-bytes` |
| Tool timeout | `--model-tool-timeout-millis` |
| Denied Tools | Exact set of repeated `--model-denied-tool` values, or empty |

The Tool timeout must be strictly greater than the gateway timeout. The existing outer
`--process-timeout-millis` must be strictly greater than the Tool timeout. Gateway
timeout remains bounded by `ModelRequest.MAX_TIMEOUT`, response characters by
`ModelRequest.MAX_RESPONSE_LENGTH`, and prompt-read bytes by
`EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES`. Durations retain positive
millisecond precision. These validations occur at CLI parsing/composition time, before
queue recovery.

The tight count and per-name ceilings keep child process arguments finite on supported
hosts. Denying unrelated names grants nothing because the typed attempt's execution
policy permits only `model-invoke`; retaining the exact bounded set nevertheless keeps
the current configuration contract and its canonical child arguments intact. Including
`model-invoke` proves the explicit RFC-0016 execution-policy refusal path. An empty or
non-matching denied set cannot grant task authority: the active task must independently
allow `model-invoke`.

The remaining sources are fixed or already explicit:

- the queued `ModelWorkPayload` remains the sole complete-profile, target, digest,
  snapshot, task-revision, and allowed-Tool source;
- `WorkItem.requiredCapability` remains the sole authoritative capability source and
  was fixed by the RFC-0024 manifest producer for that path;
- `ExactActiveTaskResolver` freshly resolves the governed repository task;
- the deterministic fake gateway, candidate, token counter, suitability evaluator,
  exact-request preparer, and invoker remain repository-owned closed implementations;
- the existing command `project-root`, `queue-root`, `runtime-root`,
  `external-effect-root`, `cycle-checkpoint-root`, `evidence-root`, `run-record-root`,
  `invocation-root`, owner, retry, lease, outer process timeout, and system clock remain
  unchanged sources;
- the command's one `FileSystemRunRecordStore` serves both RunRecord v1 and Model
  RunRecord v2 interfaces at the same root;
- model validation uses the same evidence root with the existing maximum evidence
  storage bound; and
- child-local invocation uses `CancellationToken.none()` because authenticated
  Scheduler-to-model cancellation propagation is not yet defined. This is an explicit
  absence of cancellation authority, not an inference from payload or ambient state.

No environment variable, repository file other than governed task/prompt inputs,
profile field, manifest rewrite, candidate, clock, or ambient singleton supplies any
configuration value.

## Composition And Payload Dispatch

The shared Scheduler composition selects one of two worker constructions:

1. with no model group, construct the exact current legacy worker; or
2. with the complete model group, construct the existing payload-kind-aware worker with
   one model consumer context and the existing legacy consumer path.

The second construction is model-aware rather than model-only. Legacy `WorkPayload`
continues through RunRecord v1 and its existing child arguments. `ModelWorkPayload`
continues through the RFC-0023 typed child arguments, Model RunRecord v2 store and
validator, and exact payload-kind finalization/recovery. Neither kind is projected into
the other. The composition does not scan, create, submit, publish, receive, discover,
reorder, or rewrite queue content.

Model mode is a supported execution choice, not a capability or submission grant. A
later supported typed producer or publisher must still durably establish the complete
RFC-0024 manifest and queue intent before these execution commands can consume it.

## Runtime-Event Compatibility

The existing optional runtime-event configuration remains independent and all-or-none.
Model mode may be combined with it. The selected worker must receive the same single
`RuntimeEventRecorder` instance already supplied to dispatcher recovery, retry,
finalization, and the supported Scheduler execution paths.

RFC-0025 introduces no model-specific event kind and does not reinterpret event
payloads. Omitting runtime-event configuration remains event-free. Supplying model mode
must never silently discard, replace, or duplicate an explicitly selected recorder.
This requires an additive internal composition path accepting both the model consumer
context and optional recorder; it does not change event schemas or publication roots.

## Execution, Refusal, And Recovery

Typed work preserves the RFC-0023 sequence: claim and fenced runtime preparation,
exact Work spool, child-local fresh task/prompt/policy/admission/candidate/suitability/
Ready chain, at most one fake gateway call per AgentRun, same-policy Tool/evidence,
independent response verification, Model RunRecord v2 publication, parent binding,
checkpoint, cleanup, runtime finalization, retry, and terminal queue disposition.

Denied `model-invoke`, capability mismatch, timeout nesting failure retained in the
typed profile, prompt failure, suitability failure, and other pre-call refusals make no gateway call
and create no Model RunRecord, Tool evidence, Result, retry decision, or queue
disposition. RFC-0023 deliberately has no durable terminal pre-call-refusal record, so
the existing active execution/checkpoint prefix remains recoverable and the supported
command fails through its bounded execution/internal-error boundary. RFC-0025 does not
label that prefix `IDLE`, complete it, or invent a new durable reason.

Completed v2 references continue to outrank timeout and suppress reinvocation. Typed
retry uses a distinct AgentRun and deterministic record identity. Corrupt, foreign,
changed, cross-kind, symbolic, non-regular, partial, or several spool/record points fail
closed. Existing recovery/status commands remain read-only and payload-kind-aware; they
gain no new option or interpretation in this RFC.

## Supported Output

Cycle, drain, and service retain their current statuses, exit codes, queue facts, cycle
counts, and RunRecord count. The implementation may add exactly one bounded line,
`modelExecution=DISABLED|DETERMINISTIC_FAKE_V2`, so an operator can distinguish the
selected composition. It must not print prompt or response text, profile contents,
policy details, evidence content, child arguments, credentials, or a claim that model
work was submitted or invoked.

Existing legacy invocations without the model group retain byte-for-byte command
parsing requirements and output unless the implementation includes the single additive
mode line for both modes. The implementation task must choose and test one consistent
output rule across all three commands before changing output.

## Schema Sufficiency

The current formats already retain every durable fact required by this composition:

- message/spool v2 retains typed ModelWork and Result;
- submission manifest v3 retains exact WorkItem capability, priority, and envelope;
- queue v4 and runtime v5 retain the exact typed WorkItem;
- pending-finalization v2 retains deterministic execution recovery references;
- FileSystem RunRecord storage distinguishes RunRecord v1 and Model RunRecord v2; and
- existing runtime-event formats retain the already supported event facts.

The process configuration is explicit per foreground invocation and is validated again
against the returned Model RunRecord policy. It is not a durable queue authority or a
new recovery record. Exact recovery therefore requires the operator to repeat the same
model group just as existing Scheduler recovery requires the same roots, owner, limits,
and timeouts. Configuration drift against a retained typed reference fails parent
binding; it never rewrites historical state.

RFC-0025 adds no durable version, migration, sidecar, receipt, event kind, profile
parser, registry, or provider configuration.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. add CLI value/parser RED tests for the exact optional all-or-none group, closed
   selector, denied-Tool bounds, numeric bounds, strict timeout nesting, and legacy
   omission;
2. add a public production composition value or factory that exposes bounded scalar
   configuration without exposing internal authority objects, and add the combined
   model-context/runtime-event worker construction;
3. connect the shared cycle/drain/service composition to that value while retaining the
   exact legacy branch and store/root identity;
4. add real-filesystem/JVM CLI integration proving typed verified completion for cycle,
   drain, and service from test-seeded RFC-0024 durable intent, plus one pre-call
   denied-`model-invoke` refusal and one model-plus-runtime-event path;
5. prove legacy Work behavior and output compatibility, exact typed replay/recovery,
   Model RunRecord v2 binding, and source locality; and
6. run focused RFC-0016-through-RFC-0025, Scheduler CLI, durable format, recovery,
   runtime-event, architecture, and full Markdown-sensitive regression.

Observable behavior is test-first. A RED must fail only for the deliberately absent
configuration/parser/composition behavior. Existing conditional skips are not new
evidence. Every GREEN increment is committed locally under the active implementation
task; push, merge, release, and deployment remain separate authority.

## Rejected Alternatives

- A new direct `model-run` command is rejected because it would bypass the existing
  durable Scheduler lifecycle and conflate execution with later typed ingress.
- Making the package-private worker overload public unchanged is rejected because its
  authority and store sources would remain caller-ambiguous.
- Fixed hidden timeouts or read ceilings are rejected because execution policy must be
  explicit and replayable by the operator.
- An unbounded or delimiter-encoded denied-Tool list is rejected because it creates
  ambiguous parsing or unbounded child arguments; exact repeatable bounded values reuse
  the current set-valued configuration without those defects.
- Inferring capability from the profile or CLI is rejected because RFC-0016 and
  RFC-0024 require an independent authoritative source.
- Treating the model group as a complete-profile format is rejected because the queued
  payload already owns that untrusted requirement data and interface profile input is
  the next separate step.
- Disabling runtime events in model mode is rejected because it would silently remove
  an already supported Scheduler composition feature.
- Adding a durable configuration record is rejected because existing parent validation
  and exact operator re-entry are sufficient for this bounded foreground composition.

## Exclusions

- Java or test implementation in this specification task;
- typed submission, spool publication/receive, Message Bus or runtime-event ingress;
- interface profile file, schema, parser, defaults, or profile CLI fields;
- provider/router/registry, endpoint, remote transmission, network, credentials,
  pricing, spend, fallback, cache, streaming, or MCP;
- new durable schema, migration, sidecar, RunRecord provenance, event kind, or terminal
  pre-call-refusal record;
- authenticated cancellation propagation, daemon/background service, supervisor,
  concurrency, or multi-agent execution; and
- push, merge, release, deployment, permission changes, destructive cleanup, or
  external effects.

## Prompt Book

### Prompt: Implement Supported Deterministic-Fake Model-Aware Scheduler Composition

Implement RFC-0025 RED-first. Add the exact all-or-none deterministic-fake model group
to `scheduler-cycle`, `scheduler-drain`, and `scheduler-service`; construct every
existing model process input from its named source; preserve the unchanged legacy
branch, payload-kind identity, v1/v2 record distinction, recovery, and optional runtime
events; and prove the supported CLI through real JVM/filesystem tests. Do not add typed
submission or receive, a profile input format, capability input, provider/network/
credential/spend authority, durable schema changes, new events, or terminal refusal
state.
