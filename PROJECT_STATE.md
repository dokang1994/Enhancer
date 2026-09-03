# Project State

## Updated At

2026-09-03

## Repository State

- Repository root: `C:/Enhancer`.
- Current branch: `main` tracking `origin/main`.
- Build system: Gradle 8.4 Wrapper with Java 17.
- Production source: 464 Java files.
- Test source: 206 Java files.

Delivery history is `git log`, and per-increment delivery is described in
`CHANGELOG.md`. This section states only what is true of the working tree now;
it does not restate which commit published which increment.

## Capability Maturity

### Contract Verified

- RFC-0023 process validation is Contract Verified while typed execution remains
  disconnected. `ProcessIsolatedAgentRunExecution` preserves the legacy v1 order and
  gains a narrow internal model-validation composition that accepts only an exact
  Work-before-Result closure or deterministic v2 point recovery. It binds Goal/
  AgentRun record identity, complete WorkItem/envelope/profile, independent capability,
  prepared request limits and evidence correlation, exact policy scalars, canonical
  returned-outcome evidence/lifecycle, independent verification, and claimed status.
  A valid complete v2 outranks a process-timeout fact; missing alone permits timeout,
  while cross-kind, corrupt, foreign, changed, multiple, symbolic, or non-regular
  points fail closed. Child handling now selects payload kind explicitly but returns a
  dedicated disconnected outcome before legacy execution, evidence, record, Result,
  or launch-side model activity. Process handlers and launchers still contain no v2
  writer or model-attempt-pipeline call. Durable finalizer/worker and Scheduler
  recovery/status readers remain v1-only for Increment 4.

- The standalone RFC-0023 deterministic-fake model-attempt pipeline is Contract
  Verified and remains package-private with no process-handler or other production
  caller. It derives the exact evidence correlation, preserves the Scheduler-prepared
  task/request/policy/admission object chain through candidate suitability, exact-
  request readiness and one invocation, and stops every pre-call refusal without Tool,
  evidence, or record activity. Only returned outcomes enter a one-shot result Tool;
  it never invokes a gateway or rereads the prompt, checks response model/UTF-16/usage
  structure before evidence, lazily persists long output, and maps every gateway or
  executor failure to closed code-only evidence. Successful Tool output is independently
  verified before `ModelRunRecordFinalizer` builds one millisecond-precision lifecycle
  and point-persists only Model RunRecord v2 at the deterministic AgentRun identity.
  Verified, Rejected, Unverified, and Not Performed/failed lifecycle mappings and exact
  v2 replay are covered. The process readers are now v2-aware, while the durable
  finalizer/worker and Scheduler recovery/status readers remain v1-only and guarded
  for Increment 4.

- The first RFC-0023 implementation prerequisite is Contract Verified without a
  production caller. `AgentRunEvidenceIdentity` derives one canonical, versioned,
  domain-separated evidence run UUID from the exact Goal and AgentRun identities,
  with a fixed derivation vector distinct from the RunRecord identity domain.
  Additive `EvidenceRunNamespaceStore` keeps the existing `EvidenceStore` contract
  unchanged, while `FileSystemEvidenceStore.ensureRun` validates identity before
  filesystem activity and creates or exact-replays only the contained direct run
  directory without replacing files, symbolic paths, or Windows junctions. Existing
  evidence envelope/reference bytes and short-inline no-write behavior remain
  unchanged. The standalone attempt pipeline is now the only intentional production
  consumer of this boundary; no process handler or supported entry point reaches it.

- The RFC-0022 standalone exact-request preparation and invocation boundary under
  `com.enhancer.model` is Contract Verified. Field-free
  `DeterministicFakeExactRequestPreparation` accepts exact `Suitable` plus exact
  `ExecutionPolicy`, reads the retained prompt once, counts its well-formed Unicode
  scalars once, and applies malformed/input/response-length/output/checked-total
  checks in the accepted first-match order. Its sealed opaque private-construction
  `Ready` and `Refused` variants retain exact input identities, complete successful
  counts, or one closed reason with non-revealing rendering. Focused behavior,
  reflection, source-order, total-invariant, and locality guards cover equality/one-
  over boundaries, supplementary input, malformed positions, the tight fake maximum,
  and zero gateway/Tool/evidence/RunRecord/runtime/caller activity on preparation
  refusal. Field-free `DeterministicFakeExactRequestInvoker` accepts only `Ready`,
  rechecks policy allowlisting, strict timeout, and current cancellation in order,
  then makes at most one exact candidate-bound fake call with the exact admitted
  request. Opaque results retain the exact `Ready` plus an untrusted returned response,
  one pre-call reason, or one unchanged `ModelFailureCode`; raw exception text and
  unchecked failures are not relabeled. Focused source/reflection/interaction tests
  prove a single invocation site, code-only failure, redacted rendering, and zero
  production references outside the six RFC-0022 definition types. No production
  caller, ToolExecutor isolation, evidence/verification/Model RunRecord writer,
  schema/runtime/finalizer/retry/recovery integration, provider/network, credential,
  or spend authority exists.

- The RFC-0020/RFC-0021 standalone local-candidate boundary under
  `com.enhancer.model` is Contract Verified. Field-free
  `DeterministicFakeTokenCounter` implements fail-closed well-formed Unicode-scalar
  counting and checked exact-fake response algebra. Opaque final
  `DeterministicFakeModelCandidate` retains only one exact gateway and exposes fixed
  `deterministic-fake-v2`, `deterministic-unicode-scalar-v1`, and
  524,288/262,144/262,144/524,130 context/input/output/total facts. Field-free
  `ModelCandidateSuitability` applies the complete closed model-class, capability,
  reasoning, semantics, capacity, zero-cost, and public-classification order and can
  retain exact inputs in ephemeral `Suitable`. Reflection and source guards prove the
  five definition types have no generic gateway, I/O, Tool, provider, credential,
  evidence, RunRecord, process, runtime, persistence, or production-caller wiring.
  Gateway rendering and generic `ModelUsage` remain character-based; no schema,
  runtime, network, credential, or spend authority exists.

- The RFC-0019 Scheduler model request/policy/admission preparation boundary under
  `com.enhancer.runtime` is Contract Verified. Immutable
  `SchedulerModelInvocationLimits` retains only explicit gateway-timeout and response-
  character bounds. `SchedulerModelInvocationPreparer` freshly resolves the exact
  active task, creates one `model-invoke`-only `ExecutionPolicy` from explicit resource
  inputs, reads one typed target snapshot through the shared
  `GovernedModelPromptReader`, builds the exact profile-model-class `ModelRequest`,
  composes RFC-0015, and evaluates RFC-0016 with that same policy object and unchanged
  WorkItem capability. Its immutable result retains the exact task, policy, profiled
  request, and ephemeral decision without caching or persistence. Existing model Tool
  prompt behavior uses the same reader. No production caller, candidate/suitability
  proof, Tool/gateway activity, evidence, Model RunRecord writer, provider/network,
  credential, or spend authority exists.

- The RFC-0019 exact Scheduler active-task resolution boundary under
  `com.enhancer.runtime` is Contract Verified. `ExactActiveTaskResolver` accepts an
  explicit project root and typed ModelWork `WorkItem`, reads the complete governed
  context and active `ApprovedTask` exactly once per call through the existing readers,
  hashes the already decoded complete source content with lowercase SHA-256, and returns
  the same reader-produced task only when retained task ID, source document, digest, and
  immutable Tool set all match exactly. Legacy work is refused before filesystem I/O;
  mismatch reasons are closed and typed, while missing, inactive, malformed, oversized,
  outside-root symbolic/junction, and invalid-UTF-8 reader failures remain fail-closed.
  The resolver has no cache, store, registry, ambient lookup, writer, production caller,
  request/policy preparation, admission, candidate, Tool, gateway, provider, network,
  credential, or spend authority.

- The RFC-0019 additive model-record value and type-level persistence boundary under
  `com.enhancer.run` is Contract Verified. `ModelRunRecord` is one exact five-component
  immutable record retaining canonical WorkItem identity, independent required
  capability, exact typed ModelWork envelope and complete profile, exact prepared
  `ModelRequest`, and the existing lifecycle `RunRecord`. Its constructor binds distinct
  WorkItem/message identities, logical run, retained task identity/source/Tool scope,
  exact model Tool request/correlation/target/limits, request/profile alignment, and any
  performed-verification digest while preserving the typed expected digest for failed
  lifecycles. `ModelRunRecordStore` and `ResolvedModelRunRecord` are separate from the
  unchanged v1 ports, and `RunRecordKind` plus
  `UnsupportedRunRecordKindException` distinguish the two known kinds from corruption.
  `FileSystemRunRecordStore` implements both type-level ports over the unchanged
  envelope, reference namespace, four-MiB bound, atomic publication, opaque listing,
  and exact replay. Payload v1 has a literal decode/new-encode golden and remains byte-
  identical; canonical payload v2 retains the exact model envelope, request, and
  lifecycle values and rejects cross-kind resolution/reuse, unknown/corrupt/truncated/
  trailing/oversized/foreign/noncanonical input, or changed-content identity reuse
  without rewriting the first artifact. No production caller writes v2. Candidate
  selection, gateway execution, provider/network, credential, and spend authority
  remain absent.

- The RFC-0016 pure model invocation-admission boundary under `com.enhancer.model` is
  Contract Verified. One stateless field-free evaluator intersects the exact active
  `ApprovedTask`, exact active `ExecutionPolicy`, separately authoritative required
  capability, request timeout, and profile locality in deterministic first-match order.
  Its sealed decision retains either the exact admitted `ProfiledModelRequest` instance
  or exactly one of five closed rejection reasons. Task and policy Tool denial,
  capability mismatch, request timeout not strictly inside the execution-policy
  timeout, and `POLICY_CONSTRAINED` locality all fail closed; `LOCAL_ONLY` may reach
  only ephemeral local eligibility. Reasoning, context, token, cost, and classification
  requirements remain retained and unevaluated, and response-character and token
  magnitudes remain independent. Existing production source and runtime wiring are
  unchanged. No production caller supplies the complete profile plus independent
  authority sources, and admission grants no gateway invocation, model suitability,
  route, provider, network, remote transmission, credential, persistence, Tool, task,
  or spend authority.

- The RFC-0015 `ProfiledModelRequest` pure composition under `com.enhancer.model` is
  Contract Verified as one public immutable record retaining exactly one complete
  RFC-0013 `ModelRequest` and one complete RFC-0014 `ModelExecutionProfile`. It rejects
  missing values, unequal provider-neutral model-class labels, and profile invocation
  time greater than the request gateway timeout; equality at the timeout boundary is
  valid. Required capability remains independent from model class, and the RFC-0013
  response-character ceiling remains independent from RFC-0014 token budgets. The
  record implements no gateway, provider, Tool, policy, routing, or execution port and
  performs no task, execution, outbound, locality, destination, credential, or spend
  decision. Existing request, gateway, fake, Tool, CLI, Scheduler, adapter, command and
  durable schemas, and runtime behavior remain unchanged. No caller constructs this
  value in production, so invocation admission and gateway integration are absent.

- The RFC-0014 provider-neutral model execution-profile pure value layer under
  `com.enhancer.model` is Contract Verified. `ModelExecutionProfile` is one immutable
  versioned record retaining distinct required-capability and model-class labels,
  exact locality/reasoning/data-classification vocabularies, minimum context tokens,
  token and cost budgets, and maximum invocation time. Its nested immutable budget
  values enforce positive bounded overflow-safe token relationships and an explicit
  three-letter currency with integer microunits; the profile enforces total-context
  fit, stable lower-case hyphenated labels, the fixed
  `model-execution-profile-v1` schema, and positive millisecond-precise time of at most
  five minutes. The reflected record shape carries requirements only and has no prompt,
  response, task, Tool, provider, endpoint, destination, credential, price, tokenizer,
  route, or result component. The separate RFC-0015 pure wrapper is its only
  composition; `ModelRequest`, `ModelGateway`, Tool, CLI, Scheduler, provider-adapter,
  and durable-schema behavior remain unchanged, and no runtime admission, routing,
  provider selection, network transmission, credentials, spend authority, pricing, or
  tokenization exists.

- The RFC-0013 Delivery Gate 9 model gateway minimum slice under `com.enhancer.model`
  is Contract Verified with the deterministic fake as the only executed gateway.
  `ModelGateway` maps one immutable bounded `ModelRequest` — correlation identity,
  bounded prompt, repository-owned model-class label, and a budget stub of one
  timeout plus one maximum response length — to one bounded `ModelResponse` with
  `ModelUsage`, or fails with exactly one of `PROVIDER_UNAVAILABLE`,
  `RESPONSE_INVALID`, `BUDGET_EXCEEDED`, or `TIMED_OUT`.
  `DeterministicFakeModelGateway` responds as a pure function of its input and
  refuses a response exceeding the declared length budget. The package-private HTTP
  message-API adapter shape is compile- and reflection-bounded only: nothing
  constructs or invokes it, and it is unproven against any real provider API.
  Credentials exist only as the injected default-free `ModelCredentialSupplier`
  port; no credential value exists, and none can reach evidence or output.
  `ModelInvokeTool` executes under `model-invoke` through the existing executor
  with required bounded arguments, requires the gateway timeout strictly inside its
  per-tool policy timeout, persists response text through the existing evidence
  envelope, and maps gateway failures to typed Tool failures (`TIMED_OUT` to
  `TIMED_OUT`, `PROVIDER_UNAVAILABLE` to `TEMPORARY_FAILURE`, `RESPONSE_INVALID`
  to `INVALID_RESULT`, `BUDGET_EXCEEDED` to `TOOL_REPORTED_FAILURE`). Model output
  is untrusted data: a directive-shaped response persists verbatim as evidence and
  changes no policy, scope, or document. The governed `model-invoke` CLI command
  reuses the shared per-tool five-second timeout values, the existing controller,
  loop, finalizer, and RunRecord store, and its digest-integrity verifier accepts
  only `model-invoke` results; the promoting integration test proves one governed
  CLI run against the fake persists a lifecycle-valid replayable RunRecord whose
  oversized evidence reference resolves to the exact deterministic response. The
  tool additionally accepts exactly one prompt source per request: inline `prompt`
  or a governed `prompt-path` file read with the same containment, bounded-size,
  and strict UTF-8 rules as governed read-file work. The Scheduler execution
  boundary now derives each WorkItem's pipeline from its allowed-tool scope: a
  `read-file`-containing scope runs the original pipeline unchanged, a
  `model-invoke` scope executes the deterministic fake with the declared execution
  input as governed prompt document and expected response digest and the required
  capability as the model-class label under fixed budget values, and a scope
  naming neither executable tool, or model work without a declared input, fails
  closed before execution. The isolated-result validation and invocation recovery
  status apply the same scope-derived expectation, the governed submission
  surfaces accept any task scoped to at least one executable tool, and a
  real-filesystem integration proves governed CLI submission plus one real
  child-process Scheduler cycle to `VERIFIED_COMPLETED` with a resolvable
  RunRecord and evidence reference and re-entry creating no second execution,
  with no queue, runtime, submission, or spool schema change. No
  test opens a network connection. Real provider invocation, paid services,
  credentials, MCP, model routing, locality policy, caching, fallback, streaming,
  quality evaluation, cost budgets beyond the stub, prompt-injection resistance,
  source attribution, and redaction remain absent.

- The installation transaction cursor now has one uncomposed concrete local-filesystem
  store. `FileSystemInstallationTransactionStore` accepts only a caller-provisioned,
  pre-existing absolute exact-real non-symbolic root, resolves one bounded no-follow
  regular-file point, and holds a stable transaction-scoped nonblocking OS lock across
  create/CAS resolution, validation, forced same-root candidate validation, required
  atomic publication, and post-read. Exact replay does not rewrite; stale revision,
  invalid successor, contention, corruption/schema/capacity, unavailable root, and
  uncertain publication remain typed. JUnit temporary-tree tests are evidence for
  cooperating local-process semantics only. Java path checks are not descriptor-relative
  native confinement, file force plus atomic rename is not parent-directory or sudden-
  power-loss durability, and no permission protection, authenticity, anti-rollback,
  production installer, CLI/operator/runtime wiring, or real installation mutation
  exists. The separate `InstallationPhaseEvidencePointStore` is still a pure semantic
  create/read port with no production implementation; it does not implement the evidence
  resolver because no independently revalidatable evidence body exists.

- The Windows cancellation-trust installation permission adapter boundary under
  `com.enhancer.maintenance.installation.windows` is Contract Verified with an injected,
  unimplemented native gateway. It validates canonical plan SIDs, bounded token groups
  and privileges, link-free same-volume path/file identities, protected explicit DACL
  evidence, exact raw Windows-right and normalized-operation partitions, atomic
  publication, durability barriers, and exact runtime metadata/policy probes. A
  successful publication retains the exact resulting target file identity; durability
  and published-security recheck accept only that identity, and changed replay or later
  same-volume identity drift fails at its typed boundary. One
  production adapter implementation and zero production gateway implementations exist;
  fake-gateway tests and architecture guards prove the package has no filesystem, ACL,
  process, shell, native-library, runtime, CLI, operator, or build wiring. Publisher raw
  rename/replace `DELETE` closure is distinct from and does not authorize typed
  `DELETE`; operator/runtime mutation and delete remain denied. Real Windows evidence
  collection, permission enforcement, publication, durability, probing, and installation
  remain unsupported.

- Delivery Gate 8 deterministic runtime-event publication-point acknowledgement under
  `com.enhancer.runtime`: `FileSystemRuntimeEventPointAcknowledger` accepts the original
  canonical pending filename and resolves exactly one pending or deterministic
  `.runtime-event-received` point without scanning. Both states revalidate the regular
  non-symbolic integrity envelope, deterministic identity, canonical Goal/event,
  retained stream, exact event, and derived reference. First acknowledgement performs
  one same-root non-replacing atomic rename; retained re-entry returns
  `ALREADY_ACKNOWLEDGED` without mutation. The publisher exact-replays either state
  before capacity evaluation, counts only pending points, and never recreates an exact
  acknowledged point. Conflicting, corrupt, foreign, symbolic, or missing state fails
  closed. The retained receipt proves observation only, not handler delivery or event
  application, and is not deleted by this contract.

- Delivery Gate 8 read-only runtime-event publication-point consumer under
  `com.enhancer.runtime`: `FileSystemRuntimeEventPointReader` accepts one explicit
  canonical `.runtime-event-reference` filename, scans neither caller-owned root,
  validates the regular non-symbolic schema-v1 integrity point and deterministic
  SHA-256 filename, parses canonical Goal/event identities, point-resolves exactly one
  bounded Goal stream, and returns only the exact event plus stream revision after
  re-deriving the same publication reference. Missing, corrupt, malformed, symbolic,
  foreign, or mismatched state fails closed, and repeated resolution creates, rewrites,
  renames, acknowledges, or deletes nothing.

- Delivery Gate 8 immutable runtime-event and append-only store contract under
  `com.enhancer.runtime`: `RuntimeEvent` exposes exactly eight `runtime-event-v1`
  kinds with a sealed detail per kind, exact Goal/WorkItem/AgentRun plus
  task/snapshot/run/correlation provenance, optional causal UUID, bounded producer,
  occurrence time, and one through four typed authoritative references without
  content, credentials, Tool scope, or transition authority. Event UUIDs are
  deterministic and domain-separated over kind, Goal/AgentRun, and the complete
  ordered reference identity. `RuntimeEventStream` retains one immutable per-Goal and
  WorkItem binding, at most 4096 exact events, and a monotonic append revision.
  `FileSystemRuntimeEventStore` exact-replays without rewriting or advancing,
  atomically publishes bounded strict-UTF-8 SHA-256 envelopes, and fails closed on
  changed identity reuse, foreign binding, prefix rewrite, overflow, missing,
  corrupt, oversized, trailing, unsupported-schema, or symbolic-root state. Focused
  real-filesystem evidence verifies the store. `RuntimeEventRecorder`, its opaque
  publication reference, and its publisher port enforce append/exact replay before
  publication. `FileSystemRuntimeEventPublisher` is the first concrete local adapter:
  it point-persists only the validated opaque reference under a deterministic SHA-256
  name in a capacity-bounded schema-v1 integrity envelope, exact-replays without
  rewrite before capacity evaluation, and rejects corrupt, mismatched, symbolic, or
  unusable points. Supported compositions are the optional Control receiver
  cancellation-request path and the process-, lease-, retry-, verification-, Tool-
  timeout-, stagnation-, and terminal-event Scheduler execution path described below.
  MessageEnvelope evolution, scanning, event application, cleanup, retention,
  migration, cross-store transaction, and authenticated-cancellation application
  composition remain absent.
- Gate 7 isolated-worker Work Message Bus ingress under `com.enhancer.runtime`:
  `IsolatedWorkerMain` publishes the unchanged decoded transport message to its carried
  destination through one fresh `InProcessMessageBus` with exactly one `queue("work")`
  subscription. `IsolatedWorkMessageHandler` constructs the exact parent-identified
  WorkItem, invokes the unchanged Gate 1-4 execution boundary, resolves the persisted
  RunRecord status, and exposes reference/status only after handler success. The child
  proceeds only after exactly one `DELIVERED` outcome; a foreign route is `UNROUTED`
  before execution, RunRecord persistence, or Result publication. It adds no retry,
  dead-letter, cancellation, topic, durable journal, discovery, cleanup, or authority.
- Gate 7 untrusted Control point publication under `com.enhancer.runtime` and
  `com.enhancer.cli`: `ControlSpoolPublisher` directly resolves one existing runtime
  state, requires an `ACTIVE` Goal with a current non-terminal AgentRun, and derives
  correlation, logical-run, and causation only from its retained Work envelope.
  `scheduler-spool-control` carries the caller's canonical message identity, bounded
  producer/reason, occurrence time, and signal through `FileSpoolMessageTransport`,
  reporting only `ACCEPTED`, `BACKPRESSURED`, or `UNAVAILABLE` and the accepted point
  filename. It performs no runtime recovery, lease reclamation, request admission,
  acknowledgement, authentication, or application.
- Gate 7-to-Gate 8 durable Control spool point reception under
  `com.enhancer.runtime` and `com.enhancer.cli`: `scheduler-receive-control` resolves
  one explicit canonical regular non-symbolic pending `.transport` or deterministic
  acknowledged `.received` point, validates the exact expected queue route and
  `ControlPayload` before runtime mutation, publishes the unchanged envelope through
  `InProcessMessageBus` to `RuntimeControlAdmissionHandler`, and distinguishes newly
  `RECORDED` durable intent from revision-free `REPLAYED` intent. Pending input moves
  atomically to `.received` only after persistence, and missing Goal state leaves it
  pending. An optional all-or-none event-store root, publication root, and publication
  capacity group composes `FileSystemRuntimeEventStore`,
  `FileSystemRuntimeEventPublisher`, and `RuntimeEventRecorder`; for `CANCEL` only, it
  preserves request, event, publication point, then spool acknowledgement order and
  exact recovery from any retained prefix. Omitting the group preserves request-only
  behavior. The connection records untrusted intent only and applies no control signal.
- Gate 7-to-Gate 8 durable Work spool point reception under `com.enhancer.runtime` and
  `com.enhancer.cli`: `scheduler-receive-work` resolves one explicit canonical regular
  non-symbolic pending `.transport` or deterministic acknowledged `.received` point,
  validates the exact expected queue route and `WorkPayload`, publishes the unchanged
  envelope through `InProcessMessageBus` to `DurableWorkItemAdmissionHandler`, and
  reports durable `ADMITTED` or revision-free `REPLAYED` separately from
  `ACKNOWLEDGED` or `ALREADY_ACKNOWLEDGED`. A real-filesystem integration proves spool
  send, post-admission atomic acknowledgement, released pending capacity, separately
  invoked process-isolated service completion, and exact acknowledged re-entry without
  a second queue revision, AgentRun, or RunRecord. It performs no scan, queue creation,
  execution, cleanup, retention, durable bus journaling, or authority expansion.
- Gate 8 deterministic AgentRun-bound RunRecord point recovery: one versioned
  Goal/AgentRun-derived UUID names the process-isolated attempt's RunRecord.
  `FileSystemRunRecordStore` point-persistence is atomic, exact-replay idempotent, and
  changed-content fail-closed. An absent result spool point-resolves only that reference
  and applies the existing task/source/target/digest binding before skipping execution.
- Read-only Scheduler invocation-spool recovery status under `com.enhancer.runtime`:
  `SchedulerInvocationRecoveryStatus` projects the checkpoint-correlated Goal/AgentRun
  namespace as no cycle, runtime not recorded, absent invocation, absent work, validated
  work awaiting result, or validated published result. Its reader reuses
  `SchedulerRecoveryStatusReader`, validates exact work/result and RunRecord bindings,
  rejects corrupt, foreign, symbolic, several-message, and changed bounded samples, and
  performs no scan, creation, consumption, process launch, cleanup, recovery, retry, or
  mutation.

- Read-only Scheduler external-effect recovery status under `com.enhancer.runtime`:
  `SchedulerExternalEffectRecoveryStatus` conservatively classifies the
  checkpoint-correlated Goal as uncorrelated, pre-ledger, ledger-creation pending,
  empty, prepared-recovery, explicit user-recovery, non-compensated, or
  all-compensated. It validates every effect against the exact runtime WorkItem and
  retained AgentRun history. `SchedulerExternalEffectRecoveryStatusReader` reuses the
  existing Scheduler correlation policy, point-resolves no effect without a checkpoint,
  integrity-checks every terminal Evidence Store binding, and refuses observed
  Scheduler/runtime/ledger drift after a bounded second sample.
- Read-only Scheduler recovery status under `com.enhancer.runtime`:
  `SchedulerRecoveryStatus` projects nine durable cycle phases from one queue snapshot,
  the optional checkpoint-anchored AgentRuntime, and its optional checkpointed
  RunRecord. Exact checkpoint, attempt/replacement, WorkItem/admission, terminal result,
  and task/source bindings fail closed. `SchedulerRecoveryStatusReader` performs no
  recovery or scan and refuses observed queue/checkpoint/runtime drift after a bounded
  second sample; the result remains a stable sequential observation rather than an
  atomic snapshot or liveness claim.
- Read-only Scheduler queue status under `com.enhancer.runtime`: `SchedulerQueueStatus`
  preserves exact admission order and classifies the complete persisted partition as
  `READY`, `BLOCKED`, `ACTIVE`, `VERIFIED`, or `FAILED`, with pending readiness derived
  from the existing completed-dependency rule. Each admission retains its exact persisted
  priority, and the projection retains maximum expedited burst, consecutive expedited
  progress, and the optional recovery preference. It is pure and owns no store, recovery,
  mutation, execution, next-claim prediction, or liveness authority.
- Bounded recent RunRecord discovery through the CLI: `run-record-list` requires one explicit RunRecord root and a 1-48 limit, reuses `FileSystemRunRecordStore.recentReferences` without resolution or policy reinterpretation, and reports bounded `AVAILABLE`/`EMPTY` status, requested/returned counts, and newest-first opaque references. Missing roots remain absent, malformed bounds fail before store access, and record integrity plus lifecycle inspection remain owned by `replay`.
- Delivery Gate 8 bounded foreground Scheduler drain under `com.enhancer.runtime`: `ForegroundSchedulerDrain` invokes the existing `DurableAgentRunWorker.runOneCycle` sequentially with an explicit 1-4096 cycle bound, continues only after `VERIFIED_COMPLETED`, and returns `SchedulerDrainResult` with exact invoked/verified/failed counts and `IDLE`, `FAILED`, or `LIMIT_REACHED` stop reason. It performs no probe after a stop or limit and adds no sleep, waiting, polling, queue creation/admission, control application, progress store, or recovery authority beyond the existing queue disposition and per-cycle checkpoint.
- Delivery Gate 8 bounded Scheduler service lifecycle under `com.enhancer.runtime`: caller-driven `BoundedSchedulerService` invokes the existing `DurableAgentRunWorker.runOneCycle` sequentially under immutable 1-4096 total-cycle and consecutive-idle limits plus a positive idle wait capped at one hour. It checks a local stop signal before every cycle, waits only after a non-terminal idle result, resets consecutive-idle progress after verified work, stops on the first failure, restores interruption, and returns typed exact counts. It creates no thread, supported entry point, durable service progress, authenticated control, queue/admission, external adapter, or new recovery authority.
- Delivery Gate 8 replay-safe generated-input submission boundary under `com.enhancer.runtime`: `GeneratedInputSubmissionService` takes one caller-retained canonical submission UUID and derives the queue, correlation, and logical-run identities through fixed versioned domain-separated one-to-one UUID transforms (`GeneratedSubmissionIdentities`, `DERIVATION_VERSION = 1`), so the same key always names the same generated work across fresh stores. It resolves the existing `DurableSubmissionManifest` before consulting the clock or recapturing repository context: an absent manifest (`MissingSubmissionManifestException`) captures the occurrence time on first use through a `SubmissionEnvelopeFactory` and persists through the existing `DurableWorkSubmissionService`, while a present manifest reuses its exact occurrence time, envelope, and current default `NORMAL` priority and fails closed on any caller-owned intent conflict. No second durable store is added and no `scheduler-cycle`, execution, or polling is invoked; the caller-retained request now carries an optional `NORMAL`/`EXPEDITED` priority that first use persists and replay compares against the stored manifest before consulting the clock or recapturing repository context.
- Delivery Gate 8 durable submission intent and queue-creation boundary under `com.enhancer.runtime`: `DurableSubmissionManifest` immutably binds one canonical message identity to the target queue identity, fixed capacity, required capability, exact legacy Work or typed ModelWork envelope, and exact `NORMAL`/`EXPEDITED` Scheduler priority. `FileSystemSubmissionManifestStore` publishes one bounded schema-v3 integrity artifact before queue creation, retains the exact canonical ModelWork envelope/profile without flattening capability, treats exact replay as a no-rewrite success, and rejects missing, corrupt, oversized, trailing, identity-mismatched, or changed-content/priority reuse. Existing submission construction stays legacy-only; typed ModelWork retention grants no execution or admission authority.
- Delivery Gate 8 legacy stopped-submission compatibility migration under `com.enhancer.runtime` and `com.enhancer.cli`: the existing point operation retains every schema-v1 field, assigns `NORMAL`, and emits current schema v3 with truthful source/target reporting. A separate migration-only reader now inspects v1, v2, or v3 without writing; ordinary resolution remains v3-only.
- Delivery Gate 8 coordinated durable migration preflight, consumer-first cutover, and bounded operator under `com.enhancer.runtime`/`com.enhancer.cli`: one explicit bounded plan names an externally held stopped-owner fence, every manifest and runtime identity, the queue identity, and optional spool and binding points. Preflight uses ordered readers, validates exact manifest/queue/runtime WorkItem equality and stable source bytes, gives read-file precedence for mixed legacy scope, returns non-writing `ALREADY_CURRENT` for an exact current closure, and refuses unprofiled model work as `UNMIGRATABLE_LEGACY_MODEL_WORK` / `PROFILE_REQUIRED` before candidates or targets exist. After READY, the cutover resnapshots named points, writes and rereads all same-directory runtime/queue/manifest candidates before publication, revalidates fence and binding bytes, validates result/work spools without rewriting their payload-sensitive current wire family, atomically publishes runtime then queue then manifests after exact source-byte checks, validates ingress without rewrite, and requires a final `ALREADY_CURRENT` complete closure. Real-filesystem temporary integration proves all six post-publication crash boundaries resume at the first old store without rewriting current-prefix bytes or mtimes; fence, binding, three spool, and three store drift points preserve the changed source and every later target; failed operations remove remaining candidates; and exact current replay writes nothing. `scheduler-migrate-durable-closure` accepts only caller-enumerated roots, repeatable submission/Goal identities, exact optional points, and a digest-bound bounded fence file; it performs no discovery and reports migrated/current or the typed refusal pair. A legacy read-file closure migrates and restarts through fresh current readers, unprofiled legacy model work and external typed ModelWork receive remain zero-write refusals, and execution guards remain before RunRecord-v1 execution/child launch. No ModelWork submission, admission, RunRecord v2, gateway, provider, network, or execution authority is added.
- Delivery Gate 8 durable AgentRun retry controller under `com.enhancer.runtime`: `DurableAgentRunRetryController` is the persist-first application boundary over the existing runtime and exact Goal external-effect ledger. It requires `RETRY_PENDING` plus the latest failed attempt, derives completed attempts from immutable runtime history, invokes the pure decider, and records the decision with the ledger revision/count and a deterministic versioned length-framed semantic SHA-256 over every ordered request field and status. Exact decision replay is revision-free; a changed policy or ledger fails against the immutable attempt-bound record. A caller-checkpointed canonical replacement identity may append only an admitted AgentRun, while a refused decision may terminally abandon the Goal; both post-decision actions recover idempotently after persistence failure. The controller executes no replacement, mutates no queue, acquires no lease, invokes no Tool/adapter, and creates no external-effect evidence or cross-store transaction.
- Delivery Gate 8 corrected attempt-level retry decision under `com.enhancer.runtime`: pure `AgentRunRetryDecider.decide(lastAttempt, completedAttempts, policy, ledgerState)` consumes the exact `RuntimeAgentRun` rather than terminal Scheduler `WorkItemDisposition`, binds the ledger Goal and every effect WorkItem to that attempt, and keeps the 1–16 attempt bound. Deterministic first-match precedence refuses a non-`FAILED` attempt (`NOT_FAILED`), `PREPARED` (`UNRESOLVED_EXTERNAL_EFFECT`), `REQUIRES_USER_RECOVERY` (`EFFECT_REQUIRES_USER_RECOVERY`), `APPLIED`/`DEDUPLICATED` (`NON_COMPENSATED_EXTERNAL_EFFECT`), then exhausted budget (`ATTEMPTS_EXHAUSTED`); only empty or all-`COMPENSATED` history admits automatic retry. It reads no store, creates/persists/runs no AgentRun, mutates no queue/runtime/lease/fence/ledger state, and adds no durable history, schema, finalizer split, controller, worker, CLI, or authority.
- Delivery Gate 8 evidence-bound external-effect execution under `com.enhancer.runtime`: one separate schema-v2 ledger per Goal retains at most 256 ordered `ExternalEffectRequest` values, each binding a stable bounded idempotency key, adapter identity, operation name, and semantic SHA-256 to the exact Goal, AgentRun, and WorkItem without payload content, credentials, or Tool authority. `PREPARED` carries no outcome evidence; every terminal `APPLIED`, `DEDUPLICATED`, `COMPENSATED`, or `REQUIRES_USER_RECOVERY` record carries exactly one immutable Evidence Store reference and SHA-256. `DurableExternalEffectExecutor` validates adapter identity and digest before mutation, persists `PREPARED` before one invocation, persists redacted complete evidence before the current-owner/fence-checked terminal update, resolves exact terminal replay without another invocation or revision, and refuses automatic execution from pre-existing preparation. Adapter, evidence, terminal-store, and lease-expiry failures retain the durable prepared prefix. The bounded filesystem store rejects schema-v1 artifacts and enforces monotonic request, status, and evidence history; a named real-filesystem integration connects the runtime lease, ledger, executor, deterministic adapter, and Evidence Store. No production adapter, external call, retry/second AgentRun, cross-store transaction, multi-process lock, migration, cleanup, or parent-directory power-loss claim is added.
- Delivery Gate 8 durable runtime control-request admission under `com.enhancer.runtime`: `AgentRuntimeState` retains an immutable ordered ledger of at most 256 exact `ControlPayload` envelopes, admitted only for an active Goal/AgentRun when logical-run, correlation, and work-message causation bind to the retained WorkItem. Exact restart replay is idempotent, identity reuse with different content and runtime-identity collisions fail closed, later lifecycle revisions retain the exact prefix, and schema-v5 filesystem persistence occurs before exposure while incompatible schema-v1-v4 runtime artifacts fail explicitly. The request itself changes no Goal/AgentRun status, lease, fence, queue, Tool scope, worker, or bus cancellation state; application remains the separate authenticated boundary below.
- Delivery Gate 8 process-isolated execution (connection 3d) under `com.enhancer.runtime`: `ProcessIsolatedAgentRunExecution` is the second production `AgentRunExecution`. It spools the work envelope to a `work/` spool under an invocation root private to the Goal and AgentRun, launches `IsolatedWorkerMain` through the `WorkerProcessLauncher` port with the project, evidence, and RunRecord roots as parent-supplied arguments, and returns the persisted RunRecord reference read back from a `result/` spool. The child runs the same Gate 1-4 pipeline as the in-process path through the package-private `AgentLoopAgentRunExecution.executeWork` seam, so the two paths do not drift without widening the public execution boundary. Re-entry reuses work only after decoding the sole message and matching the exact work destination and complete dispatched envelope; foreign work and several work or result messages fail before launch. The decoded Result now publishes through a fresh real `InProcessMessageBus` queue to `IsolatedResultMessageHandler`; exactly one `DELIVERED` outcome may expose the reference, while `UNROUTED`, failed, duplicate, or invalid delivery fails closed. The result remains a claim: the handler requires matching correlation/logical-run/causation/task identities, an exact `ResultPayload`, a reference resolvable in the shared `RunRecordStore`, a RunRecord bound to the dispatched task, source document, read-file target, and (when verification was performed) expected digest, plus agreement between claimed and recorded verification status. A non-completed launcher outcome or non-zero exit fails closed. Re-entry returns an already-published valid result through the same bus/handler path or a matching deterministic point-resolved record without launching a second child. Store roots never come from payload data. Its idempotent post-checkpoint cleanup removes only the exact Goal/AgentRun spool tree; retry, cancellation, and concurrent cycles sharing one invocation root do not exist.
- Delivery Gate 8 isolated worker process (connection 3b) under `com.enhancer.runtime`: `IsolatedWorkerLauncher` runs one worker in a child process and returns a typed `IsolatedWorkerOutcome` — `COMPLETED` with an exit code, or `TIMED_OUT`/`START_FAILED` with a bounded reason and no exit code, so a destroyed child cannot read as a clean exit. The executable is resolved from `java.home`, canonicalized, and required to be a regular file; the child runs the current classpath and an entry point taken as a `Class<?>`, so no caller-supplied executable, command name, or shell reaches `ProcessBuilder` and there is no lookup to poison. Output is discarded by the operating system, `JAVA_TOOL_OPTIONS`/`_JAVA_OPTIONS`/`JDK_JAVA_OPTIONS` are stripped, the timeout is capped so a caller cannot disable the watchdog, and an overrunning child is forcibly destroyed. `IsolatedWorkerMain` is the child entry point and reads one message from a spool, so the boundary is proven by a real message crossing it. Nothing constructs the launcher in production.
- Delivery Gate 8 local IPC transport adapter (connection 3c) under `com.enhancer.bus`: `FileSpoolMessageTransport` is the first concrete `MessageTransport`, writing one encoded route and envelope to its own file under a configured spool directory a peer reads — durably spooled is `ACCEPTED`, capacity exhaustion against a `BackpressurePolicy` is `BACKPRESSURED`, an unusable root is `UNAVAILABLE`, and a refusal spools nothing. `MessageEnvelopeCodec` owns the frame separately so the format is verifiable without a filesystem: the four legacy payloads retain exact transport-spool/message-envelope v1 bytes, ModelWork alone uses the canonical v2 family, occurrence time remains epoch-second plus nanosecond, deterministic encoding uses no wall-clock or random state, and fail-closed decoding reports `CorruptedSpooledMessageException`, distinct from `IOException` because corrupt is permanent while a filesystem error may be transient. The adapter promises no ordering across separately spooled messages and is one-directional per instance; existing connection 3d composes only legacy Work and Result instances, and no durable or supported runtime ingress accepts ModelWork.
- Delivery Gate 8 WorkPayload execution input under `com.enhancer.bus`/`com.enhancer.runtime`: `WorkPayload` carries an optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)` projected by `WorkItem.executionInput()` and retained inside the shared envelope representation in queue schema v4 and runtime schema v5. Typed ModelWork instead projects only `modelExecutionInput()`; the two shapes are exhaustive and never default or flatten into one another.
- Delivery Gate 8 AgentLoop-backed execution port under `com.enhancer.runtime`: `AgentLoopAgentRunExecution` is the first production `AgentRunExecution` — one `execute(dispatch)` call assembles the Gate 1-4 pipeline (governed `read-file` `ToolExecutor` with `EvidenceRecorder`-persisted evidence, bounded `AgentRunController`/`AgentLoop` at the CLI reference bounds, `DeterministicReadFileVerifier`, application `AgentRunFinalizer`) against the payload-declared execution input or its approved-source fallback and returns the persisted `run-record/<uuid>` reference. The `ApprovedTask` is built directly from the WorkItem so the runtime finalizer's taskId-plus-sourceDocument binding holds by construction; the port must persist through the same `RunRecordStore` the worker's finalizer resolves from. A digest mismatch or Tool failure is carried in a persisted non-`VERIFIED` RunRecord and is recorded by the runtime as a failed attempt at `RETRY_PENDING`, never thrown. Its package-private `executeWork` seam lets the isolated child reuse the same pipeline without creating a public lease-free execution API. The execution port itself adds no write Tool, retry controller, control application, or authority.
- Delivery Gate 8 durable Scheduler worker (connection sub-increment 3a) under `com.enhancer.runtime`: `DurableAgentRunWorker.runOneCycle(leaseDuration)` drives one scheduling cycle over the existing `DurableAgentRunDispatcher`, `DurableAgentRuntime`, and `DurableAgentRunFinalizer` in the recoverable order cycle-intent (ids) -> queue claim + lease -> RunRecord persisted (ref) -> intent updated with ref -> execution-artifact cleanup -> `completeExecution` -> result recording -> terminal disposition when authorized. A worker-owned single-record durable cycle-intent checkpoint (`PendingFinalization`/`PendingFinalizationStore`/`FileSystemPendingFinalizationStore`: bounded, strict-UTF-8, digest-checked, atomically published, fail-closed) is written before the claim so restart re-entry supplies the same identities; the reference persists before cleanup and acknowledgement. Verified completion records `VERIFIED_COMPLETED` and clears the intent. A non-Verified result records a failed attempt, parks the Goal durably at `RETRY_PENDING`, retains the intent and RunRecord reference, returns empty, and neither fails the active WorkItem nor executes a second AgentRun. Cleanup failures likewise retain the checkpoint for no-reexecution recovery. Empty-queue cycles leave no durable trace, while pre-reference-checkpoint execution can orphan an earlier RunRecord under the accepted at-least-once contract. The retry controller and replacement-attempt execution remain outside this worker increment.
- Delivery Gate 8 RunRecord-backed result-path finalization under `com.enhancer.runtime`: `DurableAgentRunFinalizer` composes the durable queue, `AgentRuntimeStateStore`, and `RunRecordStore` but separates `recordAgentRunResult` from `finalizeTerminalDisposition`. Result recording resolves but never persists the RunRecord, binds it to the Goal on `taskId` plus `sourceDocument`, and records a Verified attempt as Goal `COMPLETED` or a non-Verified attempt as Goal `RETRY_PENDING`; missing/corrupt/mismatched records fail closed while the run remains recoverable. Terminal disposition is derived only from terminal Goal state (`COMPLETED -> completeActiveVerified`, terminal `FAILED -> failActive` after a persisted refused retry decision), re-claims a recovery-requeued item when necessary, and performs no queue mutation for `ACTIVE` or `RETRY_PENDING`. The legacy forward method composes these steps for terminal results, and `recoverFinalization` applies only authorized post-terminal disposition. Queue and runtime remain separate stores with no cross-store transaction.
- Delivery Gate 8 durable queue terminal disposition under `com.enhancer.runtime`: a terminal `WorkItemDisposition` (`VERIFIED_COMPLETED`, `FAILED`) where only verified completion satisfies dependencies, a disjoint `failedWorkItemIds` set extending the partition invariant to `pending + active + verified + failed`, and the `completeActiveVerified`/`failActive` split across the in-memory queue, durable wrapper, and schema-v4 filesystem store with exact restart recovery; failed work never satisfies dependents and the queue stores disposition only, not a failure reason.
- Delivery Gate 8 schema-v5 durable Goal/AgentRun lifecycle and fenced ownership under `com.enhancer.runtime`: one exact WorkItem-backed Goal retains the complete bounded AgentRun, retry-decision, lease-timeout, control, cancellation, result, and fence history. Its WorkItem may retain exact legacy Work or typed ModelWork through the shared canonical envelope codec; ordinary schema-v1-v4 resolution fails closed, while separate read-only migration inspection decodes only v4 or v5 and retains exact source bytes. Typed ModelWork is rejected before the current RunRecord-v1 executor and process child launch, so retention creates no model execution authority.
- Delivery Gate 8 durable queue state and restart recovery under `com.enhancer.runtime`: canonical queue identity and one-logical-run binding, immutable schema-v4 snapshots retaining every exact priority-bearing `QueuedWork`, dependency, terminal partition, fairness/recovery value, independent capability, and legacy Work or typed ModelWork envelope/profile. The bounded integrity-checked atomic filesystem store preserves exact admission history, uses the stable non-blocking queue writer lock, and rejects stale writers, history rewrite, identity/content reuse, and ordinary schema-v1-v3 resolution.
- Delivery Gate 8 legacy stopped-Scheduler queue compatibility migration under `com.enhancer.runtime` and `com.enhancer.cli`: the existing point operation retains schema-v2 content, assigns the prior schema-v3 defaults, and emits current schema v4 with truthful source/target reporting. It remains candidate-validated and source-drift-safe; a separate migration-only reader now inspects v2, v3, or v4 without creating the queue lock or a candidate.
- Delivery Gate 8 dependency-ready single-worker queue under `com.enhancer.runtime`: immutable priority-bearing queued work with at most 256 unique canonical earlier-admitted dependencies, a run-scoped 4096-admission ceiling, duplicate rejection, complete admission-ordered ready-candidate selection, one active slot, and matching explicit completion; no lease, worker execution, priority admission input, or authority.
- Delivery Gate 8 pure priority/fairness selector under `com.enhancer.runtime`:
  `SchedulerPrioritySelector` validates at most 4096 unique canonical ready candidates
  before selecting, supports only `NORMAL` and `EXPEDITED`, preserves admission order
  within each class, caps the expedited burst at 256, forces oldest-ready normal work
  after burst exhaustion, resets progress on normal selection, and keeps expedited work
  selectable when no normal candidate is ready. It reads no queue/store/clock, persists
  no priority or progress itself and grants no authority. Non-recovery `claimNext`
  supplies every dependency-ready candidate and the durable wrapper atomically persists
  the selected active WorkItem with the selector-derived next progress.
- Delivery Gate 8 immutable `WorkItem` retention under `com.enhancer.runtime`: one canonical work identity distinct from the retained Gate 7 message identity, one bounded independent required capability, and approved task, Workspace snapshot, logical run, immutable Tool scope, and payload-specific execution input exposed as exhaustive projections of one unchanged legacy Work or typed ModelWork envelope. ModelWork retention grants no execution authority.
- Delivery Gate 7 versioned bounded-data `MessageEnvelope` under `com.enhancer.bus`: canonical-UUID message identity, bounded correlation/run/producer identities, optional canonical-UUID causation distinct from the message identity, and one typed payload.
- Sealed five-kind payload hierarchy (work, model work, result, control, handoff). The new Contract Verified `ModelWorkPayload` retains one mandatory bounded target, expected-response digest, and exact complete immutable `ModelExecutionProfile` beside the revision, snapshot, and immutable 1-through-256 Tool scope, requires exact `model-invoke` membership, keeps profile capability independent from model class and Tool name, and creates no content, delivery semantics, capability grant, or Tool/provider/network/spend authority.
- Delivery Gate 7 in-process delivery surface `InProcessMessageBus` under `com.enhancer.bus`: synchronous single-threaded topic fan-out in registration order and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; envelopes are carried unmutated so authorization and provenance survive every hop.
- Delivery Gate 7 delivery-failure isolation and dead-letter capture: a subscriber handler that throws yields a `FAILED` outcome and an ordered immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason, failed attempt count) while fan-out continues; a failed delivery consumes the idempotency key, reporting `DUPLICATE` with no further dead letter on re-publish or replay.
- Delivery Gate 7 bounded synchronous retry and explicit dead-letter re-delivery: an immutable `RetryPolicy` (1-10 attempts; the default bus keeps a single attempt) retries a failing handler immediately with no delay before dead-lettering it, and `redeliver` accepts only a currently recorded dead letter, resolves it on success, and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason, never appending to the journal or releasing the consumed idempotency key.
- Delivery Gate 7 cancellation propagation: `cancel(correlationId)` is idempotent and monotonic with no resume, and a cancelled correlation is refused admission before subscription lookup, idempotency, and dispatch on every path — publish, replay, and re-delivery — reporting a scope-level `CANCELLED` outcome that names no subscription, invoking no handler, consuming no idempotency key, creating no dead letter, and appending nothing to the journal; the bus reads no payload to decide delivery, so `ControlSignal.CANCEL` stays a consumer semantic.
- Delivery Gate 7 run-to-completion delivery ordering: a pending queue and a single drain loop replace nested dispatch, so a publication made from inside a handler is queued and reports the scope-level `ENQUEUED` status while the draining top-level `publish` or `replay` returns the whole ordered cascade; delivery order equals publication order, no subscriber observes an effect before its cause, every publication reaches drain-owned admission, replay-caused cascades inherit non-journaling mode, a correlation cancelled mid-cascade refuses entries queued behind it while an in-flight fan-out stays atomic, and an `Error` abandons the cascade entirely.
- Delivery Gate 7 deterministic pending-queue backpressure: immutable `BackpressurePolicy` bounds waiting publications from 1 through 4096 with a finite default; capacity exhaustion reports scope-level `BACKPRESSURED` without blocking, journaling, handler invocation, idempotency consumption, dead-letter creation, or cancellation mutation; accepted work remains FIFO and replay delivers the prefix that fits while reporting the refused suffix without growing the live journal.
- Delivery Gate 7 transport-neutral IPC boundary: immutable `TransportMessage` carries one existing destination and envelope unchanged through provider-neutral `MessageTransport`; `TransportOutcome` distinguishes hop-level `ACCEPTED`, `BACKPRESSURED`, and `UNAVAILABLE` from Message Bus delivery and bounds refusal reasons without consuming bus state.
- Delivery Gate 7 is Contract Verified after fresh reassessment: `WorkPayload.allowedTools` bounds both each name and collection cardinality, and all six scope items plus all four exit criteria remain supported by focused contract evidence.
- Backoff or delayed retry, pause/resume, run-scoped or causation-graph cancellation, priority ordering, competing queue consumers, threading, journal persistence, remote IPC adapters, and any ModelWork submission, receive, Scheduler execution, admission, gateway, or provider integration remain outside these verified contracts.

### Integrated

- Delivery Gate 8 Result-side Scheduler runtime-event publication: the optional
  recorder already shared by `scheduler-cycle`, `scheduler-drain`, and
  `scheduler-service` is now supplied with the Worker's injected clock to
  `DurableAgentRunFinalizer`. A named real-filesystem CLI integration proves
  `VERIFICATION_RECORDED -> WORK_ITEM_TERMINATED` across all three commands and a
  capacity-one failure after the durable Result and queue disposition. Acknowledgement
  plus exact command re-entry publishes the retained termination fact and clears the
  checkpoint without another child, RunRecord, runtime/queue/event revision, or
  disposition. The retry integration proves the six-fact interleaving across two
  failed attempts; focused finalizer evidence retains verification-before-Tool-timeout,
  Tool-timeout-before-stagnation, and exact repair coverage. Omitted event options stay
  event-free; no CLI option, schema, or transition authority changed.

- Delivery Gate 8 retry decision/start Scheduler runtime-event publication: the
  optional recorder already shared by `scheduler-cycle`, `scheduler-drain`, and
  `scheduler-service` is now supplied with the Worker's injected clock to
  `DurableAgentRunRetryController`. The controller persists or exact-replays each
  attempt-bound decision before `RETRY_DECISION_RECORDED` and the checkpointed
  replacement AgentRun before `RETRY_STARTED`. A named real-filesystem CLI integration
  proves the admitted-decision -> retry-started -> refused-decision sequence through all
  three commands, with exact common binding, six bounded opaque points, two retained
  RunRecords, one failed queue disposition, and a cleared cycle checkpoint. Omitted
  event options remain event-free; the shared finalizer composition now places
  verification before each retry decision and termination after the refused decision.

- Delivery Gate 8 deterministic runtime-event acknowledgement CLI: the supported
  `runtime-event-acknowledge` command requires explicit event/publication roots and the
  original pending filename, composes the filesystem acknowledger and event store, and
  reports `ACKNOWLEDGED` or `ALREADY_ACKNOWLEDGED` plus the same bounded typed event
  metadata as the read-only command. Named real-filesystem coverage proves first
  acknowledgement, lost-response replay, pending-capacity release, publisher replay,
  fail-closed invalid/conflicting state, and unchanged point/event contents,
  timestamps, and stream revision. It performs no handler delivery, event application,
  cleanup, retention, runtime mutation, or Gate promotion.

- Delivery Gate 8 read-only runtime-event point CLI: the supported
  `runtime-event-read` command requires explicit event/publication roots and one exact
  point filename, composes the real filesystem reader and event store, and reports only
  bounded event identity, kind, time, producer, Goal/AgentRun, task/snapshot/run/
  correlation, stream-revision, and authoritative-reference-count metadata. Named
  real-filesystem coverage proves exact repeatable reads leave point and stream bytes,
  timestamps, and revision unchanged, while missing event state does not create a root
  and corrupt points remain byte-identical. It makes no acknowledgement, delivery,
  application, transition-authority, or Gate-promotion claim.

- Delivery Gate 12 authenticated cancellation application and runtime-event path:
  `AuthenticatedCancellationApplication` resolves one exact retained `CANCEL` request
  and delegates authentication/authorization to an injected trusted
  `ControlRequestAuthorizer`; envelope producer, reason, transport, and admission never
  grant authority. A matching approval atomically persists one
  `CancellationApplicationRecord` with Goal/current-AgentRun terminal cancellation in
  AgentRuntime schema v4 before exposure. Denial, non-CANCEL input, or binding drift
  leaves the runtime unchanged, while exact retained-record replay bypasses
  reauthorization and advances no revision. Event-aware construction then derives
  `CANCELLATION_APPLIED` from retained application time, Control causation, exact Work
  binding, and stable Control-message/application references. Named filesystem evidence
  proves missing-event repair and publication-failure replay. The supported shared
  `FileSystemAuthenticatedCancellationApplication` now requires one explicit runtime
  root, injected clock, and trusted authorizer; its separate event-aware constructor
  accepts one all-or-none `FileSystemRuntimeEventPublicationConfiguration` and composes
  the concrete event store, recorder, and reference-point publisher. Real-filesystem
  coverage proves approved publication, exact replay without reauthorization or byte/
  revision change, denial isolation, event-free omission, and capacity bounds.
  `AuditBackedSignedCancellationAuthorizer` is the first concrete implementation: one
  bounded canonical detached grant is verified with Ed25519 against an immutable
  injected public-only `CancellationGrantTrustPolicy`, then one deterministic
  integrity-checked `CancellationAuthorizationAuditRecord` is persisted before
  approval. Exact retry preserves the first audit bytes, current validation denies
  expired or revoked audit-only prefixes, and changed identity reuse, malformed proof,
  corruption, or audit failure fails closed. Exact retry also requires the current
  trust-configuration revision and revocation fact to match the retained audit while
  preserving the first verification observation time. The Contract Verified
  `PinnedFileCancellationGrantTrustPolicyLoader` loads one absolute normalized exact-
  real regular non-symbolic file through one bounded no-follow read, verifies an
  independently injected complete-file SHA-256, strictly parses and internally
  re-encodes the same canonical UTF-8 public-only Ed25519 policy bytes, and derives
  `configurationRevision` from that digest. It has no writer, discovery, fallback,
  cache, or private material. The supported production `scheduler-apply-cancel` CLI now
  binds that loader only through a strict bounded fixed metadata sibling of the exact-
  real installed Enhancer JAR CodeSource. It accepts runtime/audit storage, canonical
  Goal/retained-Control identities, a bounded no-follow proof file, and the optional
  all-or-none event group, while rejecting request-selected trust, actor, clock, issuer,
  key, credential, or approval overrides. Lazy authorizer composition preserves
  terminal replay without proof/trust reads and can repair optional event publication.
  Named real-filesystem tests prove signed proof -> audit -> runtime application,
  denial/configuration isolation, and suffix recovery after proof/policy deletion.
  The separate `com.enhancer.maintenance` state machine is Contract Verified in
  isolated temporary installation trees: INSTALL and ROTATE derive one public policy
  digest internally from the canonical snapshot, publish immutable content-addressed
  policy before canonical fixed metadata, serialize cooperative writers with a
  persistent stateless lock, reject stale CAS, and exact-replay without rewrite,
  fallback, rollback, overwrite, scan, or cleanup. The
  operator Java main and repository-local Gradle selector are Contract Verified with
  strict arguments, bounded public output, typed direct-JVM exit `2`/`20`/`70`, and zero
  runtime CLI coupling. Separate custom distribution packaging is also Contract
  Verified: Gradle-generated Unix/Windows scripts, installed build layout, ZIP, and TAR
  contain only the operator launcher pair plus runtime JARs, and copied-layout subprocess
  tests preserve exit `2`/`20`/`70`, INSTALL, and exact ROTATE replay under JUnit-owned
  temporary trees. Real OS/application installation, installer/provisioning and
  permission mutation, deployment/release, real-install rotation/application anti-rollback,
  private signing material, credential issuance, proof production, API/editor/Desktop
  authentication adapters, process signalling, queue disposition, Tool/effect
  cancellation, `PAUSE`/`RESUME`, and event consumption remain absent.
  The installation/permission boundary is Contract Verified at the pure Java contract
  layer: separate installer/publisher, operator, and runtime principals; publisher-only
  protected-path mutation; one derived installation plan, fixed revisioned artifact/
  effective-access matrix and phase order, bounded evidence/failures, and an unwired
  `InstallationPermissionAdapter` port. Fake-adapter and architecture tests cover the
  contract without filesystem calls. Platform-specific Windows SID/DACL or POSIX UID/
  GID/mode/ACL enforcement, immutable staging, policy/metadata publication, runtime-
  identity probing, activation, production transaction persistence, installer
  composition, real
  installation, deployment, uninstall/cleanup, release, and privileged anti-rollback
  remain absent.
  The platform-neutral schema-v2 transaction cursor and revisioned point-store port are
  also Contract Verified with a test-only in-memory fake. They bind the exact plan,
  normalized environment/filesystem, release, permission-policy, activation, ordered
  pending/succeeded phase, revision, and immutable ordered succeeded phase-evidence
  prefix; reject changed identity, history rewrite, and stale CAS; and classify the
  metadata/activation recovery regions. Each prefix entry carries only a bounded
  semantic result identity, not evidence content or integrity/durability proof. This is
  state/store grammar, not durable persistence, restart recovery, installation-success
  evidence, or a production store implementation.
  Store mutation receipts and the pure one-phase transaction coordinator are now also
  Contract Verified. `CREATED`/`ADVANCED` alone grant one fake port invocation after a
  pending state is stored; exact replay or existing pending returns reconciliation,
  source/preflight and activation use distinct ports, other phases use the closed effect
  port, and terminal replay is invocation- and mutation-free. All implementations are
  test-local fakes. A succeeded store write now retains the exact returned phase-
  evidence identity and terminal replay retains all eleven ordered bindings. Evidence
  bodies, integrity-protected persistence, durable effect recovery, automatic pending
  replay, exactly-once installation, production store/port implementations, existing
  permission-adapter composition, and installation success remain absent.
  Exact point evidence reconciliation is Contract Verified through test-local fakes.
  `InstallationPhaseEvidencePoint` binds one transaction/phase/canonical pending
  revision, the read-only resolver exposes only exact revalidation or absence with typed
  failure, and the separate pure reconciler performs at most one validated pending-to-
  succeeded CAS. Missing or foreign evidence, resolver/store failure, and malformed
  receipts leave the pending state without false success; succeeded and terminal state
  invoke neither resolver nor phase port and never advance automatically. This is a
  value/port/application contract only: evidence bodies, real content verification,
  serialization/integrity, durable transaction/evidence persistence, filesystem/native
  observation, production recovery, permission-adapter composition, and installation
  success remain absent.
  The pure transaction/evidence filesystem byte formats are Contract Verified without
  a filesystem adapter. Distinct schema-v1 domain envelopes bind magic, schema, bounded
  length, complete-header-plus-body SHA-256, payload kind, and deterministic canonical
  content. The cursor format reconstructs the complete schema-v2 state with strict
  UTF-8, stable enum names, explicit optionals/booleans, current provider/dialect, and
  exact evidence prefix; the evidence format additionally rejects a valid foreign point.
  Pure bounded leaf names bind the transaction or complete evidence point. Tests cover
  initial/mid/terminal round trip, activation evidence, cross-kind input, corruption,
  unsupported schema, truncation/trailing data, oversized length, malformed Unicode/
  UTF-8, foreign point and path dialect, and canonical re-encoding. These package-local
  codecs contain no filesystem call or store/resolver implementation. Their SHA-256 is
  corruption detection only, not authenticity, durable publication, locking/CAS, or
  rollback protection; evidence bodies and real revalidation remain absent.
- Delivery Gate 8 lease-timeout fact and runtime-event path: AgentRuntime schema v4
  retains at most 256 ordered exact `LeaseTimeoutRecord` values. Expired
  `EXECUTING -> READY` reclamation atomically appends the current AgentRun, owner,
  fence, issue, expiry, and observation facts; the filesystem store preserves the
  ledger as an exact prefix and validates the append against the reclaim transition.
  Event-aware `DurableAgentRuntime` derives `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.LEASE` only after that revision is durable, using retained lease
  expiry, Work-message causation, producer `durable-agent-runtime`, and stable
  `agent-runtime/<goal>/lease-timeout/<agent-run>/<fence>` reference. Retained-record
  replay repairs missing events and publication failures without another runtime
  revision. The shared `scheduler-cycle`, `scheduler-drain`, and `scheduler-service`
  composition now supplies its existing optional filesystem recorder to WorkItem-
  matched dispatcher recovery and direct worker recovery, preserving event-free
  omission and exact point repair; the same recorder also reaches retry control and
  finalization through their own source-first contracts. Earlier runtime schemas,
  automatic post-reclaim execution, scans, cleanup, and retention remain absent.
- Delivery Gate 8 process-timeout fact and runtime-event path:
  `ProcessIsolatedAgentRunExecution` now point-resolves a deterministic
  `process-timeout/<goal>/<agent-run>` fact before spooling or launch. A fresh typed
  `IsolatedWorkerStatus.TIMED_OUT` outcome persists the first post-outcome occurrence,
  exact Work/Goal/AgentRun binding, positive timeout, and bounded launcher reason in the
  integrity-checked `FileSystemProcessTimeoutFactStore` before exposing failure. Exact
  re-entry rejects changed binding/content and fails again without another child. The
  event-aware construction then derives `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.PROCESS`, Work-message causation, and the fact reference plus
  semantic digest; missing-event and publication-failure recovery exact-replay without
  another fact or event revision. `scheduler-cycle`, `scheduler-drain`, and
  `scheduler-service` now accept one optional all-or-none event-root, publication-root,
  and bounded-capacity group. Their shared execution composition constructs one
  filesystem recorder and injects it into process-isolated execution, AgentRuntime
  recovery, retry control, and finalization; omission preserves the
  prior event-free path. Start failure, completed failure, and success create no
  process-timeout fact. No AgentRun lifecycle/retry policy, RunRecord, MessageEnvelope,
  cancellation application, or additional event owner is added.
- Delivery Gate 8 Tool-timeout runtime-event path: event-aware
  `DurableAgentRunFinalizer.recordAgentRunResult` resolves a bound persisted RunRecord,
  persists or exact-replays the matching Result transition, records the separate
  verification fact, and only then derives `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.TOOL` when the exact Tool failure code is `TIMED_OUT`. The event
  retains the RunRecord occurrence time, Result causation, exact runtime Work binding,
  and stable Result-message plus RunRecord references. Named focused evidence proves
  verification-before-timeout ordering, timeout-before-separate-stagnation ordering,
  non-timeout exclusion, Result-persistence failure isolation, missing-event repair,
  and exact replay after publication failure. The supported Scheduler Worker/CLI
  composition now supplies the concrete filesystem recorder to this owner.
- Delivery Gate 8 stagnation runtime-event path: event-aware
  `DurableAgentRunFinalizer.recordAgentRunResult` resolves the bound RunRecord and
  persists or exact-replays the matching Result transition before deriving
  `STAGNATION_DETECTED` only for worker stop reason `STAGNATED`. The RunRecord supplies
  the retained occurrence time and total iterations, the current default Agent Loop
  policy supplies threshold three, and ordered Result-message plus RunRecord references
  keep identity stable after later runtime revisions. The existing
  `VERIFICATION_RECORDED` event remains earlier and separate. Named filesystem evidence
  proves exact repair after stagnation publication failure and later runtime progress,
  while non-stagnated records retain verification-only behavior. No source schema,
  timeout owner, or new event kind is added; the supported Scheduler Worker/CLI
  composition now supplies the concrete filesystem recorder to this owner.
- Delivery Gate 8 retry-started runtime-event path: event-aware
  `DurableAgentRunRetryController.beginAdmittedRetry` persists or exact-replays the
  caller-checkpointed replacement AgentRun before deriving one deterministic
  `RETRY_STARTED` event from the replacement, previous failed attempt, exact retained
  Work binding, causal failed Result, admitted decision, and stable replacement-AgentRun
  identity. The source has no retained transition time, so the injected
  post-persistence clock supplies the first candidate and recorder replay preserves the
  first event occurrence. Named filesystem evidence proves decision-before-start order,
  replacement-persistence isolation, publisher recovery after a later `READY` revision,
  and missing-event repair after later runtime progress without another runtime or event
  revision. The supported Worker/CLI composition now supplies the concrete adapter when
  the complete event option group is present; omission remains event-free, and refused
  abandonment creates no retry-started fact.
- Delivery Gate 8 retry-decision runtime-event path: the event-aware
  `DurableAgentRunRetryController` persists or exact-replays the admitted or refused
  attempt-bound decision before deriving one deterministic `RETRY_DECISION_RECORDED`
  event from the exact Goal, WorkItem, failed AgentRun, task, snapshot, run,
  correlation, causal Result message, decision outcome, stable retry-decision identity,
  and decision-bearing runtime revision. The source has no retained transition time, so
  an injected post-persistence clock supplies the first candidate and
  `RuntimeEventRecorder` reuses the first persisted occurrence during publication
  recovery. Named filesystem evidence proves exact replay without either stream
  revision advancing, runtime-persistence isolation, event-append recovery, and
  publisher-failure recovery under a later clock. The supported Worker/CLI composition
  now supplies the concrete adapter and continues into the separate retry-started fact
  for an admitted decision; omission preserves the event-free controller.
- Delivery Gate 8 terminal WorkItem runtime-event path: the event-aware
  `DurableAgentRunFinalizer` applies or re-enters the exact verified-completed or failed
  queue partition and confirms the target before deriving `WORK_ITEM_TERMINATED` from
  the retained runtime binding, Result causation, disposition, and stable
  queue/WorkItem/disposition reference. The reference remains stable as the whole queue
  revision advances. For this source without a retained transition timestamp,
  `RuntimeEventRecorder` reuses the first persisted occurrence for the same event ID and
  still delegates all other changed-content refusal to the exact event store. Named
  filesystem evidence proves missing-event repair after later queue revisions,
  publication-failure recovery under a later clock, revision-free replay, and queue-store
  failure isolation. No queue schema is added; the supported Scheduler Worker/CLI
  composition now supplies the concrete filesystem recorder to this owner.
- Delivery Gate 8 verification runtime-event path: the event-aware
  `DurableAgentRunFinalizer` persists or exact-replays the RunRecord-backed Result
  transition before deriving one deterministic `VERIFICATION_RECORDED` event from the
  retained Goal, WorkItem, AgentRun, task, snapshot, run, correlation, Result message,
  occurrence time, verification status, causal Result identity, and RunRecord reference.
  The Result-message and RunRecord references keep event identity stable after later
  runtime revisions. Named filesystem integration proves missing-event repair,
  revision-free replay and repeat publication, Result-transition persistence failure
  isolation, and event/publisher-failure recovery. The supported Scheduler Worker/CLI
  composition now supplies the concrete filesystem recorder to this owner.
- Delivery Gate 8 cancellation-request runtime-event path: the event-aware
  `RuntimeControlAdmissionHandler` persists or exact-replays the bound Control request
  before deriving one deterministic `CANCELLATION_REQUEST_RECORDED` event from the exact
  Goal, WorkItem, current AgentRun, task, snapshot, run, correlation, Control message,
  occurrence time, and runtime revision. `RuntimeEventRecorder` appends or exact-replays
  the event before passing only its `runtime-event/<goal>/<event>` reference to the
  injected publisher port. Named filesystem integration proves missing-event repair,
  revision-free event replay, duplicate reference publication, request-store failure
  isolation, and request-only `PAUSE`/`RESUME`. The supported optional
  `scheduler-receive-control` event configuration now supplies explicit caller-owned
  event and publication roots plus bounded capacity for this owner only; partial or
  invalid configuration fails before artifacts, and exact `.received` re-entry repairs
  a retained event or publication point without another request/event revision.
- Delivery Gate 8 bounded single-agent Scheduler/runtime foundation: durable Goal and
  AgentRun lifecycle, dependency-aware queueing, priority/fairness, fenced execution,
  process-isolated Work/Result Message Bus crossings, retry, terminal verification,
  supported migrations, restart/lost-acknowledgement recovery, and explicit
  external-effect outcomes compose through named filesystem and child-process paths.
  Explicit submission/cycle workflows are Operational sub-paths, but the whole gate is
  not promoted: retry-decision, retry-started, Tool/process/lease-timeout, stagnation,
  cancellation-request/application, verification, and terminal WorkItem owners reach
  the recorder and an injected publisher port. The concrete filesystem reference-point
  adapter is optionally composed by the supported Control receiver for cancellation-
  request events and by Scheduler cycle/drain/service for process timeout, lease
  timeout, retry decision/start, verification, Tool timeout, stagnation, and terminal
  WorkItem events. The supported authorizer-injected filesystem application surface
  separately composes authenticated cancellation and optional
  `CANCELLATION_APPLIED` publication without adding an interface adapter.
  Budgets, Memory, broader authenticated control interfaces,
  production adapters, and role workers remain owned by Gates 9 through 13.
- Gate 7 isolated-worker Work/Result Message Bus path: the real child JVM receives the
  parent-spooled Work transport point, routes it through the fresh Work queue into the
  unchanged Gate 1-4 execution boundary, persists one RunRecord, and publishes the
  correlated Result point that the parent routes through its existing fresh validation
  queue. Named child-process coverage proves the valid path and foreign Work-route
  refusal before any RunRecord or Result artifact.
- Gate 7-to-Gate 8 Control spool producer/receiver CLI path: one named real-filesystem
  integration derives a Control envelope from an existing active Goal's exact retained
  Work binding, publishes it through `FileSpoolMessageTransport`, passes the explicit
  accepted point through the separately invoked supported receiver and real Message Bus,
  persists it in the existing durable Goal request ledger, and atomically acknowledges
  it. Inactive runtime and hop-level backpressure create no partial point or durable
  request; authentication and control application remain outside this connection.
- Gate 8 child-persisted/result-not-published recovery: a real child JVM persists the
  deterministic RunRecord while a blocked result spool forces publication failure; a
  fresh parent then resolves that one record, launches no second child, creates no
  duplicate RunRecord, and returns into the existing checkpoint/finalization path.
- Gate 8 migration-to-cycle recovery: one named real-filesystem CLI integration starts
  from an admitted active WorkItem, matching executing AgentRun, one persisted Verified
  RunRecord, an unchanged empty external-effect ledger, and a schema-v1
  pending-finalization checkpoint carrying that reference. The supported maintenance
  command migrates it to schema v2, then the real process-isolated `scheduler-cycle`
  composition finalizes exactly one verified queue disposition without creating an
  invocation spool or another RunRecord/effect outcome, and clears the checkpoint.
- Gate 8 read-only Scheduler invocation-status CLI: `scheduler-invocation-status`
  composes the checkpoint-correlated Scheduler projection with one explicit invocation
  root and a 1-through-8 bound. Real-filesystem coverage proves no-cycle non-creation,
  absent invocation, validated work/result phases, corruption refusal, immutable queue
  and spool artifacts, bounded output, and no child-liveness claim or mutation.

- Gate 8 read-only Scheduler external-effect recovery-status CLI:
  `scheduler-external-effect-status` composes the existing checkpoint-correlated
  Scheduler projection with one Goal ledger and its terminal evidence through explicit
  roots. It reports complete five-status counts plus an at-most-8 ledger-ordered prefix
  without evidence content or external-system claims. Named real-filesystem coverage
  proves uncorrelated, intent, runtime, empty, prepared, and user-recovery prefixes,
  missing-root non-creation, immutable artifacts, evidence corruption refusal, drift
  rejection, and bounded output without adapter invocation or mutation.
- Gate 8 read-only Scheduler recovery-status CLI: `scheduler-recovery-status` uses the
  cycle checkpoint as the sole cross-store anchor, directly reads only the explicit
  queue/runtime/checkpoint/RunRecord roots, and reports a bounded typed durable phase
  with `workerLiveness=UNKNOWN`. Named real-filesystem coverage proves no-checkpoint,
  intent-only, runtime, and checkpointed-RunRecord prefixes, missing-root non-creation,
  immutable artifacts, corrupt-state refusal, exact binding, and stable-sample drift
  rejection without invoking any recovery or mutation.
- Gate 8 read-only Scheduler queue status CLI: `scheduler-status` resolves one explicit
  filesystem queue snapshot without recovery, reports complete five-state counts plus a
  bounded admission-ordered identity/state/priority prefix and the persisted maximum
  expedited burst, consecutive expedited claims, and optional recovery preference, and
  reads no runtime, effect, checkpoint, RunRecord, submission, or invocation store. A
  named real-filesystem integration proves mixed priority, all five states, exact
  order/counts, unchanged artifact bytes and timestamp, unchanged revision, active slot,
  fairness progress, and recovery preference, empty status, missing-root non-creation,
  corruption failure, and maximum bounded output.
- RunRecord discovery-to-replay CLI path: a named real-filesystem integration persists three integrity-checked RunRecords, fixes their observation order, lists the exact bounded newest-first prefix, and successfully replays a returned reference. Empty-root inspection creates no directory, while a maximum 48-reference fixture stays within bounded output and proves listing does not resolve or suppress artifacts.
- Gate 8 bounded foreground Scheduler drain CLI: the separate `scheduler-drain` command reuses every `scheduler-cycle` composition input plus `--max-cycles`, recovers one existing durable queue, and runs the real process-isolated Worker only while verified work continues. Bounded output and stable exits distinguish `IDLE`, `FAILED`, and `LIMIT_REACHED` with queue and cycle counts. Named real-filesystem child-process integrations prove two ready/dependency-linked items followed by idle, recovery from a persisted cycle-intent prefix, an exact one-cycle limit leaving later work pending, terminal failure stopping before later work, and missing-queue usage failure without queue creation. Submission remains separate; no polling/service behavior or whole-Gate promotion is added.
- Gate 8 bounded foreground Scheduler service CLI: the separate `scheduler-service` command reuses every `scheduler-cycle` composition input plus explicit finite total-cycle, consecutive-idle, and idle-wait bounds, runs `BoundedSchedulerService` on the invoking thread, and reports typed stop/count, queue, and RunRecord data with stable exits. Named real-filesystem process-isolated integrations prove empty bounded idle termination, fresh-command recovery from a persisted cycle intent, and reclamation of an expired executing lease under the same Goal/AgentRun with a greater fence, one AgentRun, one RunRecord, and one verified queue disposition. It creates no queue/work, thread, daemon, supervisor, service checkpoint, authenticated control, or broader orphan policy.
- Gate 7-to-Gate 8 durable Work spool receiver CLI: the separate
  `scheduler-receive-work` command admits only one explicitly named retained transport
  artifact through the real Message Bus into one existing durable queue. It reports the
  effective priority and stable WorkItem identity, atomically acknowledges pending input
  only after admission, retains `.received` evidence for exact replay, releases pending
  transport capacity, and leaves every execution command separate.
- Gate 8 durable submission recovery path: one named real-filesystem integration interrupts after durable manifest persistence and after empty queue creation, restarts with fresh store/service instances, and converges each prefix on one exact pending WorkItem. Full exact replay changes no manifest artifact or queue revision; changed envelope content fails before queue mutation, and an existing capacity mismatch is rejected before queue recovery can requeue active work.
- Gate 8 durable submission CLI: `scheduler-submit` resolves the matching repository-approved active task, captures its repository-memory Workspace snapshot at the explicit occurrence time, and passes one explicit dependency-free work envelope to `DurableWorkSubmissionService`. Every root, identity, queue bound, capability, time, target, and digest remains caller-supplied, plus one optional `--priority NORMAL|EXPEDITED` selecting the Scheduler priority; omission defaults to `NORMAL`, any non-exact value fails usage before mutation, and a replay-conflicting priority fails closed like other changed content. A named real-filesystem CLI integration proves first admission, fresh-instance exact replay without artifact or revision change, changed-content refusal, task mismatch refusal, pending queue state without execution, and the effective-priority input/output/persistence path for both an explicit `EXPEDITED` submission and the defaulted `NORMAL` omission. Bounded output distinguishes `ADMITTED` from `REPLAYED` and reports the effective `priority` on both; no worker, Tool, RunRecord, evidence, cycle, polling, or whole-Gate Operational promotion is added.
- Gate 8 generated-input submission CLI: the separate `scheduler-submit-generated` command takes one caller-retained submission UUID and the caller-owned intent, generates the queue/correlation/logical-run identities and occurrence time, and captures the repository-memory snapshot only on first use inside the envelope factory. It accepts the same optional `--priority NORMAL|EXPEDITED` input (default `NORMAL`, other values rejected), persists it into the manifest on first use, compares it against the stored manifest on replay before consulting the clock or recapturing context, and reports the effective `priority` in bounded output. A named real-filesystem CLI integration proves first-use generation with the derived identities in bounded output, fresh-instance exact replay without manifest bytes or queue-revision change, conflict fail-closed under the same submission UUID including a changed priority, effective-priority input/output/persistence for an `EXPEDITED` submission and the defaulted `NORMAL` omission, and first-use task-mismatch refusal. No worker, cycle, or polling is added.
- Gate 8 recovery-only Scheduler CLI: `scheduler-cycle` recovers one caller-identified existing durable queue, composes `DurableAgentRunWorker.processIsolated` with explicit filesystem roots, system UTC clock, bounded retry/lease/child-timeout policy, and runs exactly one recoverable cycle. Bounded output and stable exits distinguish `IDLE`, `VERIFIED_COMPLETED`, terminal Scheduler `FAILED`, usage/configuration, and internal corruption/execution failure. A named integration prepares work through `DurableWorkItemAdmissionHandler`, resumes an already persisted cycle-intent prefix, launches the real child JVM, resolves one RunRecord, observes terminal queue disposition, and verifies checkpoint cleanup. The command creates no queue, admits no work, polls nothing, and makes no Gate 8 Operational claim.
- Gate 7-to-Gate 8 durable work admission: `DurableWorkItemAdmissionHandler` retains one exact delivered work envelope, derives a stable canonical WorkItem UUID through a fixed bijective domain transform, creates dependency-free `NORMAL` `QueuedWork`, and reports success only after `DurableSingleWorkerSchedulerQueue` persists it. Checked storage failure remains visible through bus retry/dead-letter without in-memory exposure. A named real-filesystem integration admits through the production handler, runs the process-isolated Scheduler cycle to terminal disposition, restarts queue and bus, and accepts exact replay without another queue revision, WorkItem, RunRecord, or dead letter. Same-bus replay remains duplicate-free; changed content under the same message/WorkItem identity fails closed.
- Gate 8 retry-aware Worker recovery (connection #6): `DurableAgentRunWorker` now creates or recovers one exact empty Goal ledger before first execution, records/replays the durable controller decision at `RETRY_PENDING`, checkpoints a canonical replacement AgentRun identity before append, rolls the cycle intent to that attempt without the prior RunRecord reference, and continues through the existing fenced execution/finalization path while the WorkItem remains active. On restart, a checkpointed latest `COMPLETED` or `FAILED` AgentRun exact-replays its retained Result binding before retry control or terminal queue disposition; a mismatched reference fails before those side effects, while exact replay advances no runtime revision. When the optional Scheduler recorder is present, that replay repairs Result-side events before the later side effects. Refusal abandons the Goal and produces one queue `FAILED` disposition. Schema-v2 `PendingFinalization` preserves the replacement phase and rejects incompatible v1 artifacts. Focused recovery covers the five retry prefixes, unresolved-effect refusal, and terminal-Result mismatch ordering; a named real-filesystem queue/runtime/ledger/checkpoint/RunRecord path proves first-attempt failure followed by Verified replacement completion. No external adapter, compensation, cross-attempt effect-key reuse, backoff, or authenticated control is added.
- Gate 7-to-Gate 8 durable control-request queue path: `RuntimeControlAdmissionHandler` consumes an exact control envelope from the real in-process queue, recovers the named durable Goal, and persists the bounded request before returning. Store I/O becomes a handler failure under the bus's existing bounded retry/dead-letter behavior, and a fresh bus replay against a fresh filesystem-store instance remains idempotent at the durable consumer. This integrates request delivery only; producer and reason remain diagnostic data and no unauthenticated state transition is applied.
- Gate 8 process-isolated durable worker composition: `DurableAgentRunWorker.processIsolated` selects `ProcessIsolatedAgentRunExecution` with the real self-JVM launcher, the caller-supplied filesystem artifact roots and durable stores, and one queue instance shared by dispatcher and finalizer. A named filesystem integration drives a real WorkItem through the child JVM, both spool directions, Gate 1-4 execution, RunRecord resolution, runtime terminal state, and queue disposition. The returned reference is checkpointed before the owned Goal/AgentRun spool is retired; a cleanup failure keeps the checkpoint and retries cleanup without child re-execution. The recovery-only CLI now selects this composition without adding submission, polling, or a whole-gate Operational claim.
- Gate 8 worker-over-real-execution path under `com.enhancer.runtime`: `DurableAgentRunWorker` wired with the real `AgentLoopAgentRunExecution` over one shared `FileSystemRunRecordStore` and real filesystem queue/runtime/checkpoint/evidence stores drives a verified claim and its dependent to `VERIFIED_COMPLETED` with really persisted, resolvable RunRecords and a cleared checkpoint. A digest-mismatch claim records the failed attempt at `RETRY_PENDING`, keeps the WorkItem active and its dependent blocked, retains the cycle intent/reference, and makes the next cycle return empty without executing again (`FileSystemAgentLoopWorkerIntegrationTest`). Execution remains read-only and uses either the payload-declared target/digest or the approved-source fallback.
- Gate 8 durable queue-to-AgentRun dispatch under `com.enhancer.runtime`: one existing active WorkItem or newly persisted ready claim flows through `DurableAgentRunDispatcher` into the exact matching durable Goal, named AgentRun planning/readiness prefix, and current fenced lease; partial runtime persistence is recoverable by idempotent re-entry, both filesystem stores recover the same WorkItem and lease, mismatches fail closed before runtime mutation where applicable, and no worker, queue completion, result, effect, or cross-store transaction is claimed.
- Runtime package boundary: neutral verification lifecycle values in `com.enhancer.kernel`, application-layer AgentRun finalization, package-private AgentRunState completion behind an explicit transition port, and an enforced acyclic application/run/verification/loop/kernel dependency direction with unchanged RunRecord schema and behavior.
- Gate 1 in-process Tool isolation capacity: default ToolExecutor instances share a process-wide ceiling of 64 actual live workers, hold capacity until real thread termination, and fail closed with typed terminal evidence before creating another thread when saturated; this bounds accumulation but does not terminate stuck code or replace future process isolation.
- Runtime text and mutable-file resource boundaries: valid supplementary Unicode survives every bounded Evidence/Tool/CLI/Workspace truncation point, while governed file and persisted-artifact reads enforce configured byte ceilings during consumption rather than trusting mutable preflight size metadata.
- Gate 7-to-Gate 8 work-message queue path: one repository-derived approved task and real Gate 6 Workspace snapshot flow through `WorkMessagePublisher`, the real in-process queue, journal, and replay behavior into `WorkItemAdmissionHandler` and the unchanged Gate 8 `WorkItem`; this named path is Integrated, while Gate 7 as a whole remains Contract Verified because the other payload, destination, reliability, causation, and transport branches lack named real production connections.
- Delivery Gate 6 metadata-only immutable Workspace snapshot contract under `com.enhancer.workspace`: approved task source revision provenance, typed source metadata, explicit Available/Stale/Unavailable states, deterministic ordering, bounds, temporal validation, and versioned canonical SHA-256 identity; connected to the real Context Reader upstream and the view, producer, and query downstream through `WorkspaceCollectionIntegrationTest` and the production CLI suites.
- Delivery Gate 6 read-only `ProjectBrainView` aggregate: composed from one real snapshot, one real `ProjectContext`, and the real persisted `RunRecord` of a real governed run, with derived memory freshness and approved-task mismatch rejection.
- Delivery Gate 6 graph projection contract: five typed node kinds, six endpoint-checked edge kinds over the Decision, Architecture, Dependency, Task, and Execution relationship domains, snapshot-keyed versioned projections, and element provenance with derived rebuild status; populated exclusively through the real producer chain in integration and production-CLI tests.
- Delivery Gate 6 `TaskImpactQuery`: answers the task-to-decision-to-code-to-test chain over really-produced graphs, naming the real stored execution, with deduplication, modified-artifact-restricted `VERIFIED_BY` traversal, and rebuild status derived from every traversed element; transitive `DEPENDS_ON` closure remains deferred by decision.
- Delivery Gate 6 `AcceptedDecisionProjector`: accepted decisions parsed from a real decision log through the real Context Reader and merged into the production graph output.
- Delivery Gate 6 `RunRecordMetadataCollector` and store `references()` listing: observations produced against the real filesystem RunRecord store, with a really-persisted prior record observed on the production CLI path and corruption surfaced as explicit `UNAVAILABLE`.
- Delivery Gate 6 `TaskJustificationProjector` and the optional `Justified By` task-document section: explicit references to accepted-decision headings projected into `JUSTIFIED_BY` edges with task-document provenance and snapshot-relative freshness, with strict rejection of empty, non-bullet, duplicate, or unresolved references; the first real reference resolved on the actual repository through the production CLI path.
- Gate 6 boundaries that remain outside these integrations: source payloads, Git/diagnostics/selection/terminal adapters, graph persistence, confidence metadata, and modifies/verified-by/supersedes/depends-on projection, each requiring its own evidence source.

- Delivery Gate 6 repository-memory Workspace path: `RepositoryMemorySnapshotCollector` derives a real snapshot from really-loaded Context Reader memory, and the composed `ProjectBrainView` explains a real governed run including explicit divergence detection.
- The collector reads no files, retains no content, derives the `ApprovedTaskRevision` from the same loaded memory, and reuses `WorkspaceSnapshot.capture` for identity and bounds.
- Delivery Gate 0 authority-preserving foundation lifecycle integration.
- Repository Context Reader with seven `.ai/` documents followed by eight canonical root documents.
- Deterministic Task Planner using Delivery Gate/Specified - Next grammar and explicit proposal state.
- Single-pass Assisted Development Loop.
- Repeated Agent Loop completion, failure, iteration, and stagnation exits.
- Bounded `ToolResult` and `VerificationEvidence` invariants.
- Delivery Gate 1 bounded read-only Tool Execution Boundary.
- Immutable `ToolRequest` with correlation identity and arguments.
- Immutable `ExecutionPolicy` with deny-over-allow policy, project root, size, timeout, and cancellation boundaries.
- Unique in-process `ToolExecutor` registry with bounded structured failure conversion.
- `ReadFileTool` request-to-policy-to-executor-to-real-file-to-result flow.
- Relative-path, traversal, real-path containment, regular-file, size, and strict UTF-8 checks.
- Delivery Gate 2 atomic complete-evidence persistence and restart-safe resolution.
- UUID run/evidence identities, opaque references, creation time, UTF-8 byte length, and SHA-256 metadata.
- Missing, malformed, oversized, length-mismatched, digest-mismatched, and invalid-UTF-8 evidence rejection.
- Large `ReadFileTool` output connected through `EvidenceRecorder` to a resolvable full-output reference.
- Delivery Gate 3 Tool-result-driven Agent Loop integration.
- `AgentRunState` with approved task, pending request, last result, explicit status, and deterministic progress key.
- `AgentRunController` orchestration over an existing executor, immutable policy, and external failure classifier.
- Successful Tool execution stops at `AWAITING_VERIFICATION`; retryable and terminal failures remain distinct.
- Existing maximum-iteration and stagnation exits operate over real Tool results.
- Repository-derived `ApprovedTask` identity, approval evidence, and Tool-name scope.
- Structured Tool failure codes and a standard retry policy without prose parsing.
- SHA-256 evidence content identity and semantic progress independent of storage references.
- Private Agent run construction with public ready-state creation only.
- Delivery Gate 4 sequential independent verification and durable RunRecord replay.
- Typed Verified, Rejected, Unverified, and Not Performed decisions with structured reason codes.
- Deterministic read verifier over inline or integrity-checked referenced complete evidence.
- Executed Tool request retention across worker terminal states.
- Verified-only completion through `AgentRunFinalizer` outside the worker controller.
- Immutable policy snapshot and decision recorded with task, request, Tool result, expected digest, verification, iterations, and stop reasons.
- Atomic versioned RunRecord envelopes with SHA-256 integrity and restart-safe replay.
- Controller-bound execution policy retained in the non-publicly constructible `AgentRunResult`.
- RunRecord lifecycle validation that rejects policy-history substitution and impossible worker, verification, result, and stop-reason combinations.
- Gradle 9-compatible explicit JUnit Platform Launcher runtime and workspace-local default test temporary storage.
- Invocation-isolated Tool workers that prevent interruption-ignoring timeout starvation.
- Millisecond-positive and nanosecond-representable execution-policy timeouts.
- Complete-envelope Evidence and RunRecord integrity digests covering version, timestamp, declared length, and content/payload.
- Strict RunRecord UTF-8 encoding and bounded, real-root-contained, strict UTF-8 startup-context loading.

### Operational

- Delivery Gate 5 first supported local CLI over the integrated read-only vertical slice.
- `EnhancerCli run` requires explicit governed project, active task identity, target, expected digest, evidence root, and RunRecord root inputs.
- `EnhancerCli replay` resolves integrity-checked records without Tool re-execution or chat history.
- `EnhancerCli run-record-list` discovers a caller-bounded newest-first prefix of opaque
  references from an explicit retained RunRecord root without resolving records or creating
  a missing root. A real Enhancer-repository smoke run returned 3 references from the 10
  retained artifacts, and the named CLI integration proves a returned reference composes
  with `replay`.
- Stable process exit codes, 4096-character diagnostics, verified-only completion, and persist-before-report behavior.
- Gate 8 explicit two-command Scheduler operator workflow: the supported
  `scheduler-submit` command durably admits governed work without execution, and a later
  separately invoked `scheduler-cycle` runs at most one recoverable process-isolated
  cycle over the same explicit queue root and identity. A named real-filesystem CLI
  integration and an actual Enhancer-repository smoke run prove
  `ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE`, one retained manifest, one
  RunRecord, no duplicate execution, checkpoint cleanup, and documented recovery. This
  is an Operational sub-path, not a whole-Gate 8 promotion or polling service.
- Gate 8 generated-input Scheduler submission workflow: the supported
  `scheduler-submit-generated` command durably admits governed work from one caller-retained
  submission UUID, generating the queue/correlation/logical-run identities and occurrence
  time, and a later separately invoked `scheduler-cycle` runs at most one recoverable cycle
  over the derived queue identity. A named real-filesystem CLI integration and an actual
  Enhancer-repository smoke run reading `README.md` prove
  `ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE` with identical replay occurrence
  time and Workspace snapshot, one retained manifest, one RunRecord, no duplicate execution,
  and documented generated-input recovery. This is an Operational sub-path, not a whole-Gate
  8 promotion or polling service; the explicit `scheduler-submit` command is unchanged.

### Operational Governance

- Constitution 1.2 Kernel and Document Driven Development.
- Explicit lifecycle, authorization, fresh-evidence, self-hosting, recovery, and amendment rules.
- Git-backed project memory and session handoff.
- Machine-written development-session recovery checkpoints below the ignored
  `.enhancer/session-checkpoint/` boundary: active-task contract binding, monotonic
  single-writer revisions, pending/succeeded/failed/stable execution positions,
  evidence references, working-artifact digests, atomic integrity-checked recovery, and
  start/record/show/clear CLI operations. The checkpoint is recovery metadata and cannot
  promote task, verification, maturity, or delivery state.
- Document-driven dynamic increment workflows under the single `CURRENT_TASK.md`
  authority: an optional bounded queue carries two through sixteen pre-authorized
  sequential increments with stable identities, dependencies, exit criteria,
  verification, stop conditions, deterministic successor selection, and at most one
  active increment. An executable structural guard checks the live grammar and governed
  instruction connections. This is development governance only; it adds no runtime
  Workflow Engine, automatic approval or delivery, background execution, or Gate 13
  runtime dispatch.
- Adaptive development-subagent governance: the primary Agent evaluates non-trivial
  tasks and selects the smallest useful host-session topology inside the existing user
  request and Active Task. The first policy permits at most three concurrent read-only
  children at one delegation level with fixed per-increment/task dispatch bounds,
  primary-only mutation/evidence/lifecycle ownership, mandatory join/stop, and explicit
  single-agent fallback conditions. It changes no Gate 13 runtime maturity or underlying
  task, Tool, privileged-action, or lifecycle authority.
- RED failures are classified against active task authority, accepted decisions, Architecture, and repository settings before aligned missing implementation proceeds to the minimum GREEN change.
- Java 17 strict lint (`-Xlint:all -Werror`) is enforced by the build across production and test sources, so `./gradlew build` refuses a warning rather than relying on a manual invocation being remembered.

## Accepted Product Direction

- Enhancer OS is an event-driven AI development platform, not a Chat -> Tool -> Stop wrapper.
- The target platform includes Desktop, CLI, API, Workspace, Project Brain, Memory, MCP Server/Client, Agent Runtime, Event/Message Bus with IPC adapters, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.
- Event Bus defines domain semantics, Message Bus defines delivery, and IPC is a transport adapter for the same versioned envelope.
- Runtime Agents will communicate through queues rather than direct Agent-to-Agent calls after the messaging boundary exists.
- Agent orchestration escalates only as needed from one worker to sequential work, Producer-Reviewer, bounded fan-out/fan-in, expert routing or supervisor allocation, and at most one subordinate coordination layer.
- One Kernel coordinator owns terminal run state; every worker shares an immutable input snapshot and approved task revision through typed versioned handoffs with bounded authority, budgets, evidence, and recovery state.
- Archon `263cf365` and meta-harness `ccab9a6` are pinned design references, not runtime, prompt, Skill, storage, provider, or governance dependencies.
- Workspace will expose governed file, Git, diagnostic, terminal-metadata, and selection snapshots; Project Brain will combine them with repository memory and RunRecords while preserving provenance.
- The owner's rough 20-25% foundation estimate is qualitative planning context, not verified maturity or completion evidence.
- Product milestones are V1 AI Development Experience, V2 AI Development Platform, and V3 AI Operating System.
- Product milestones describe user-visible outcomes, while Delivery Gates define dependency-ordered implementation and promotion; their numbering is not a claim that every V1 surface precedes all V2 foundations.
- The AI Kernel target owns Agent/workflow lifecycle, context and memory resources, locks and leases, scheduling, cancellation, recovery, policy, verification gates, and audit state.
- Project Brain will expose rebuildable Decision, Architecture, Dependency, Task, and Execution graph projections while Git and canonical documents remain authoritative.
- Agent plugins, Skills, Tools, and workflows are distinct extension types with separate authority and provenance.
- The Model Router target selects approved local or remote providers using capability, data classification, policy, cost, latency, context, and availability; sensitive code defaults local.
- Self-hosting development means applying Enhancer's governed workflow to its own repository; local or hybrid model execution is a separate provider-routing capability.

## Not Yet Integrated Or Operational

- Prompt and LLM invocation.
- Remaining Workspace adapters, Project Brain graph persistence, Event/Message Bus production wiring, concrete IPC adapters, broader Agent Runtime and Scheduler production paths, and Model Gateway.
- Project Brain graph storage and impact reasoning, Dependency Analyzer, Workflow Engine, Agent Marketplace, and privacy-aware hybrid model routing.
- Skill loading runtime, plugins, MCP, multi-agent, background execution, Cloud Sync, and governed self-improvement.
- CI/CD and released distribution.

## Current Delivery Position

- Delivery Gate 0: Integrated.
- Delivery Gate 1: Integrated.
- Delivery Gate 2: Integrated.
- Delivery Gate 3: Integrated.
- Delivery Gate 4: Integrated.
- Delivery Gate 5: Operational.
- Delivery Gate 6: Integrated by the 2026-07-15 re-scope-and-promotion decision; diagnostics, terminal-session, and active/selected-file observation moved to Gate 12.
- Delivery Gate 7: Contract Verified after a fresh Integrated maturity assessment. The
  work-message queue/journal/replay/idempotency path now has a named durable
  Scheduler-queue consumer, the durable control-request queue path is Integrated,
  connection 3d gives `MessageTransport` one named local work/result-spool consumer with
  both child Work execution and parent Result validation routed through fresh Message
  Bus queues, and
  one supported point receiver carries an explicit retained Work spool through the real
  bus into durable admission and acknowledges it only afterward by deterministic atomic
  rename, with exact acknowledged-point replay and pending-capacity release.
  A separate governed publisher now constructs the authorized snapshot-bound Work
  envelope, exposes its accepted point reference without discovery, and has a named
  real-filesystem publisher-to-receiver-to-admission integration. Backpressure and
  unavailable refusal paths create no additional or partial publication.
  Separate supported Control producer and receiver commands now derive one exact intent
  from an existing active Goal, carry its accepted local transport artifact through the
  real bus into the durable request ledger, and acknowledge it only afterward without
  authenticating or applying the request.
  The governed file-spool transport publisher-to-receiver path is also Integrated and
  preserves hop-level refusal separately from delivery and admission. Handoff Message
  Bus flow, authenticated interface adapters and queue disposition, topic,
  cancellation/cascade-ordering/backpressure, durable bus persistence, cleanup and
  retention policy, and reliability branches beyond the named control retry/dead-letter
  path remain contract-only.
- The post-Work-ingress reassessment found no additional production owner for topic,
  Handoff, cancellation, dead-letter re-delivery, re-entrant ordering, or in-process
  pending backpressure. Durable journaling still lacks subscriber checkpoints,
  truncation/compaction ownership, and cross-store recovery ordering; directory
  consumption lacks claim/ordering/concurrency/restart policy; retention lacks bounded
  destructive authority and audit/replay policy. Gate 7 therefore remains Contract
  Verified while its named Work, Result, and Control sub-path maturities remain
  unchanged.
- Delivery Gate 8: Specified - Next; the previously recorded queue, runtime, execution,
  recovery, priority, migration, retry, effect, drain, service, and supported CLI
  sub-path maturities remain unchanged. One retained Gate 7 transport artifact now has a
  Contract Verified and Integrated point path through the real Message Bus to durable
  Scheduler admission, with separate process-isolated service completion and
  duplicate-free exact re-receipt. The retained point now has Contract Verified and
  Integrated acknowledgement with revision-free `.received` re-entry and released
  pending transport capacity. Durable bus journaling and cleanup/retention remain Gate 7
  protocol work. Production external adapters,
  authenticated interface adapters and remaining typed controls, model/context
  budgets, Memory runtime,
  background/supervisor topology, and broader production wiring remain owned by Gates 9
  through 13.
- Gate 8 remains `Specified - Next` after closing the pre-migration assessment's
  supported-migration, priority-admission, non-recovery fairness-selection, and
  priority/fairness-observability gaps. Durable lifecycle, sequential process-isolated
  execution, recovery inspection, authority preservation, migration-to-cycle recovery,
  deterministic child-RunRecord recovery, lease-expiry recovery, disposition-
  acknowledgement recovery, and several restart/idempotency paths have named evidence.
  The supported bounded service and point spool-to-admission gaps are now closed.
  Whole-gate blockers remain separated by owner: Gate 7 owns durable bus journaling,
  remaining reliability connections, and cleanup/retention policy; Gate 12 owns
  authenticated interface adapters, queue disposition, and remaining typed controls;
  Gate 11 owns production
  external-effect adapters; Gates 9 and 10 own model/context budgets and Memory runtime;
  and Gate 13 owns background/supervisor topology and role-based workers. Existing
  point recovery and expired-lease reclamation satisfy the accepted correctness
  prefixes without authorizing a general orphan scanner or cleanup policy.
- Gate 8 lease-expiry recovery now has one named worker-level fixture over the
  post-RunRecord-reference prefix. The first cycle persists exactly one reference,
  expires before execution acknowledgement, and leaves the AgentRun reclaimable at
  `READY`; a fresh worker acquires a greater Goal-wide fence, skips execution, reaches
  one verified queue disposition, retains one RunRecord and an empty effect ledger, and
  clears the checkpoint. This adds evidence only and does not promote whole-gate
  maturity.
- Gate 8 disposition-acknowledgement recovery now has one named worker-level fixture.
  A verified terminal queue disposition persists before a forced checkpoint-clear
  failure; the retained intent preserves the exact Goal, AgentRun, and RunRecord, and a
  fresh worker reports the existing disposition while changing no execution count,
  RunRecord/effect outcome, runtime revision, or queue revision before clearing the
  checkpoint. This adds evidence only and changes no runtime mechanism or whole-gate
  maturity.
- The pending-finalization Gate 8 state-version migration capability is Contract Verified and
  Integrated through the real filesystem and explicit CLI:
  `FileSystemPendingFinalizationStore.migrateSchemaV1ToCurrent` losslessly preserves the
  schema-v1 Goal, AgentRun, and optional RunRecord-reference values, leaves the new
  replacement AgentRun identity absent, and returns `ABSENT`, `ALREADY_CURRENT`, or
  `MIGRATED`. It validates and rereads a private v2 candidate, rejects source drift, and
  atomically replaces only after the original bytes still match; every earlier failure
  cleans the candidate and preserves the source. The separate stopped-Scheduler
  `scheduler-migrate-cycle-checkpoint` command is the only supported entry point for
  that checkpoint, while normal recovery remains v1 fail-closed.
- The queue schema-v2-to-v3 migration capability is Contract Verified and Integrated
  through the real filesystem store and `scheduler-migrate-queue`. It preserves the
  complete v2 queue value, applies the fixed priority/fairness defaults, refuses source
  drift, cleans failed candidates, leaves ordinary v2 resolution fail-closed, and
  recovers an active migrated WorkItem through the one-shot preferred claim.
- The supported Gate 8 migration exit-criterion slice is satisfied for both declared
  migrations. The named pending-finalization migration-to-cycle fixture proves that a
  converted post-RunRecord-reference prefix re-enters the real Scheduler cycle, reaches
  one verified disposition from the retained reference, creates no invocation spool or
  additional RunRecord/effect outcome, leaves the effect artifact byte-identical, and
  clears the checkpoint. The queue migration fixture proves exact v2 conversion and
  active-work recovery without inventing priority history. Queue schema v1 plus runtime
  and external-effect schema v1 remain intentionally unsupported because their missing
  current-contract information cannot be reconstructed; this bounded satisfaction does
  not promote Gate 8 as a whole.
- Gate 6 `WorkspaceSnapshot`, `ProjectBrainView`, graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector` sub-capabilities: Integrated through the fresh promotion audit `gate-6-sub-capability-integration-promotion`, each connected to real upstream and downstream components by named integration evidence.
- Gate 6 `TaskJustificationProjector` and the `Justified By` reference grammar: Integrated; the first real reference resolved on the actual repository through the production composition.
- Gate 6 authority boundary: the exit criterion "Workspace observations cannot override repository authority or grant Tool permission" is pinned by `WorkspaceAuthorityBoundaryIntegrationTest`.
- Gate 6 `TargetFileMetadataCollector`: Integrated on the production CLI path; the run target is observed pre-run with a real containment-checked digest.
- Gate 6 `GitWorkspaceCollector`: Integrated on the production CLI path; one canonical absolute project-external Git executable may collect filter-free index/untracked/deleted metadata, while tracked-worktree diff is explicitly unavailable because verified comparison paths can execute clean filters.
- Gate 6 repository-memory path (real governed run -> real Context Reader memory -> collector -> composed view with divergence detection): Integrated through `WorkspaceCollectionIntegrationTest`.
- Gate 6 run-evidence graph production path (real governed run -> real snapshot -> producer -> impact-query answer naming the real stored execution): Integrated through the extended `WorkspaceCollectionIntegrationTest`.
- Gate 6 production view composition: Operational for the governed read-only CLI scenario; each run observes at most 256 recent records and reports bounded snapshot metadata when post-persist reporting is available.
- Gate 6 production graph composition: Operational for the governed read-only CLI scenario; graph inputs are preflighted before work, duplicate document/target artifacts collapse, and post-persist view/graph/query failure cannot change the durable RunRecord-derived exit code.
- Enhancer has one Operational read-only scenario; the broader Agent Runtime remains planned.
- Gate 0 integration audit is verified without a production correction or second orchestrator and does not displace Gate 6.


## Known Limitations

Durable caveats on the state claimed above. These are properties of the current
system, not open tasks; each is retired only by a bounded increment of its own.

- The CLI trusts an externally supplied expected digest; its origin is explicit and auditable but not signed.
- Evidence and RunRecord envelopes detect corruption but are not encrypted, signed, remotely replicated, or automatically cleaned up.
- The CLI uses the existing 64 MiB per-artifact/in-memory ceiling, five-second Tool timeout, five-iteration loop ceiling, and three-transition stagnation threshold. Evidence has no time-based retention or automatic cleanup contract.
- Atomic stores do not fsync parent directories and therefore make no power-loss durability claim.
- Permanently stuck Tool workers consume isolation capacity until process restart; the runtime contains them finitely but cannot terminate them.
- Three privilege-dependent symbolic-link containment tests are skipped on this Windows host; three Windows junction tests execute and pass against the corresponding production real-path guards.
- Gradle remains at Wrapper 8.4. The known Gradle 9 test-runtime deprecation is removed, but an actual major Wrapper upgrade requires a separate compatibility task.
- Gate 5 is a bootstrap CLI, not the future multi-interface control surface planned for Gate 12.
- Failed, corrupt, timed-out, or incomplete process-isolated cycles retain their current invocation spool; successful checkpointed cycles retire it. No time-based spool cleanup service or history exists.
- Acknowledged Work transport points remain retained as `.received` evidence. No
  automatic deletion, time-based retention, global acknowledged-artifact bound, or
  directory consumer exists.
- Acknowledged Control transport points are likewise retained as `.received` evidence.
  The supported producer and receiver have no authentication, application, directory
  consumer, cleanup, or retention policy.
- Same-bus work-message replay is duplicate-free and fresh-bus exact replay is a no-revision durable admission success, but queue schema-v1 migration and exact-history compaction/cleanup do not exist. Changed-envelope identity reuse intentionally fails closed and dead-letters.
- Scheduler queue updates are serialized only for cooperating local filesystem-store writers. The lock does not coordinate runtime/effect/checkpoint stores, provide a cross-store transaction, identify the current owner, wait or recover a holder, or claim distributed/network-filesystem safety.
- Pending-finalization migration requires the owning Scheduler to be stopped. Its final
  source-byte comparison detects observed drift but does not coordinate an old-version
  writer, retain a backup, provide rollback, or add parent-directory power-loss
  durability.
- `scheduler-status` is a queue-local persisted snapshot, not a worker-liveness or
  cross-store recovery view. In particular, `ACTIVE` reports the stored slot without
  proving that its worker process is still running.
- `scheduler-recovery-status` correlates only the checkpoint-anchored queue/runtime/
  RunRecord prefix and reports worker liveness as unknown. Its bounded second sample can
  reject concurrent drift, but it is not an atomic cross-store snapshot and applies no
  repair or recovery.
- `scheduler-external-effect-status` inspects only the checkpoint-correlated Goal ledger
  and integrity-checks terminal evidence. It does not probe the external system,
  establish whether an ambiguous prepared effect occurred, authorize replay or
  compensation, or make the sequential multi-store observation atomic.
- An unresolved external-effect `PREPARED` record is intentionally not replayed automatically; an owning adapter or user must establish deduplication, compensation, application, or explicit recovery evidence. The retry controller refuses it.
- The supported Control receiver/CLI optionally composes the concrete filesystem
  publisher for cancellation-request events, while Scheduler cycle/drain/service
  compose it for process timeout, lease timeout, retry decision/start, verification,
  Tool timeout, stagnation, and terminal WorkItem events when all three explicit
  publication options are present. Explicit read-only resolution and deterministic
  observation acknowledgement exist separately; no arbitrary handler or application
  consumer is composed. Authenticated cancellation now has one supported shared
  filesystem application surface with a mandatory injected trusted authorizer and
  optional all-or-none event publication plus the installed-pinned public trust-policy
  binding and supported `scheduler-apply-cancel` CLI described above. There is still no
  operator provisioning/rotation surface, installer or permission mutation, privileged
  application-version anti-rollback anchor, credential provider, API/editor/Desktop
  authentication adapter, process signal, or cancelled queue disposition.
- `runtime-event-read` is an explicit read-only point consumer. It does not acknowledge
  or rename a publication, release publisher capacity, retain a consumer offset or
  receipt, scan for work, apply an event, or define cleanup/retention. The separate
  `runtime-event-acknowledge` command retains one exact observation receipt and releases
  pending capacity, but adds no handler, consumer identity/offset, acknowledged-history
  bound, deletion, application, or cleanup/retention policy.
- Development-session checkpoints support one active local session per repository and
  have no background timer, token-budget introspection, platform shutdown hook,
  multi-session merge, remote replication, or automatic Git commit/stash behavior.
- Schema-v4 AgentRun/decision/lease-timeout/cancellation history, split finalization, durable retry control,
  replacement identity checkpointing, replacement execution/recovery wiring, durable
  submission, the explicit two-command Scheduler workflow, and the replay-safe
  generated-input submission workflow (`scheduler-submit-generated` plus a separate
  `scheduler-cycle`) exist as Operational sub-paths, but submission and execution
  intentionally remain separate operator invocations. Cross-attempt external-effect
  idempotency, authorized adapter execution, and polling service operation are not
  implemented.

## Verification Evidence

Per-increment RED/GREEN results, regression counts, lint status, and promotion
outcomes are recorded in `docs/verification-log.md` in append order. This document
states the current position; that log holds the evidence behind it.
