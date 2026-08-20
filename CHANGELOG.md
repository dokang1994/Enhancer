# Changelog

## 2026-08-20 - Implement The Model Execution Profile Pure Value Layer

- Implemented RFC-0014 test-first as six immutable public values under
  `com.enhancer.model`: the exact versioned `ModelExecutionProfile`, token and cost
  budgets, and closed locality, reasoning, and data-classification vocabularies.
- Enforced stable capability/model-class labels, positive bounded overflow-safe token
  relationships, context fit, explicit integer currency microunits, and positive
  millisecond-precise invocation time, with deterministic record equality and an exact
  reflection guard against authority, provider, request, and result fields.
- Kept RFC-0013 gateway, Tool, CLI, Scheduler, provider-adapter, and durable-schema
  behavior unchanged; no routing, network, credential, paid-service, pricing, or
  tokenizer behavior was added.

## 2026-08-19 - Accept The Gate 9 Model Execution Profile Specification

- Added and accepted RFC-0014 defining the immutable, versioned, provider-neutral
  `ModelExecutionProfile` requirement value across capability, model class, locality,
  reasoning, context, token, cost, time, and data classification.
- Fixed fail-closed vocabularies, bounds, cross-field invariants, deterministic value
  semantics, and the distinction between token budgets, RFC-0013's response-character
  bound, and the deterministic fake's character-based usage accounting.
- Recorded the accepted continuation decision and connected the contract to Gate 9
  architecture and RFC indexes while keeping Java implementation, routing, providers,
  remote transmission, credentials, paid services, and maturity promotion outside the
  slice.

## 2026-08-19 - Compose Model Invocations Into The Scheduler Execution Path

- Gave the `model-invoke` Tool a governed `prompt-path` prompt source read with
  the same containment, bounded-size, and strict UTF-8 rules as governed
  read-file work, with exactly one prompt source per request.
- Derived the Scheduler execution pipeline from each WorkItem's allowed-tool
  scope: `read-file` scopes keep the original pipeline unchanged, `model-invoke`
  scopes execute the deterministic fake with the declared execution input as
  governed prompt document and expected response digest and the required
  capability as the model-class label, and unknown scopes or model work without a
  declared input fail closed; the isolated-result validation and invocation
  recovery status apply the same scope-derived expectation.
- Accepted `model-invoke`-scoped tasks at the governed submission surfaces
  (requiring at least one executable tool) and delivered the promoting
  real-filesystem integration test: governed CLI submission plus one real
  child-process Scheduler cycle to `VERIFIED_COMPLETED` with a resolvable
  RunRecord and evidence reference and no second execution on re-entry, with no
  schema change and no network, credential, or paid-service use.

## 2026-08-19 - Implement The Gate 9 Model Gateway Minimum Slice

- Added the `com.enhancer.model` leaf package implementing RFC-0013 test-first: the
  provider-neutral `ModelGateway` port with immutable bounded
  request/response/usage records, the four-code typed failure contract, the
  default-free injected credential-supplier boundary, the deterministic fake as the
  only executed gateway, and the package-private never-invoked HTTP message-API
  provider adapter shape.
- Added the `model-invoke` Tool composed into the existing executor with required
  bounded arguments, strict gateway-inside-policy timeout validation, evidence
  capture through the existing envelope, bounded typed failure mapping, and the
  untrusted-output invariant, plus the digest-integrity verifier for its results.
- Added the governed `model-invoke` CLI command over the existing controller,
  loop, finalizer, and RunRecord store, made the shared five-second CLI tool
  timeout a per-tool composition value, and delivered the promoting integration
  test proving one governed run against the deterministic fake persists a
  lifecycle-valid replayable RunRecord whose evidence reference resolves. No
  network, credential, or paid service is involved.

## 2026-08-18 - Amend Commit Cadence To Verified GREEN Increment Boundaries

- Amended Constitution Sections 7 and 13 (version 1.2.0) so an ordinary local commit
  exists for each verified GREEN increment boundary of the approved Active Task,
  while any other commit, and all push, merge, release, and deployment authority,
  remain explicit and non-transitive.
- Synchronized `AGENTS.md`, `.ai/workflow.md`, and the Constitution version
  references in `PROJECT_STATE.md`, `ROADMAP.md`, and RFC-0001; reviewed the
  `.ai/constitution.md` mirror unchanged; recorded the accepted amendment decision.

## 2026-08-18 - Add Host-Independent Continuous Verification

- Added `.github/workflows/verify.yml`, which runs the complete Java 17
  Markdown-sensitive Gradle test task on a Temurin 17 Linux host for every `main`
  push and pull request, cancels superseded runs, and uploads test reports on
  failure.
- Restored the executable bit on `gradlew` in the Git index so the wrapper runs on
  POSIX continuous-integration hosts.

## 2026-08-18 - Accept The Gate 9 Model Gateway Minimum Slice Specification

- Added RFC-0013: Model Gateway defining the minimum Delivery Gate 9 vertical slice:
  a provider-neutral gateway port with immutable bounded records in a new
  `com.enhancer.model` leaf package, a deterministic fake as the only executed
  gateway, a never-invoked provider adapter shape, and a `model-invoke` Tool reusing
  the existing isolation, policy, evidence, and RunRecord paths.
- Recorded the accepted decision, registered RFC-0013 in the RFC index and the
  roadmap RFC track, and kept real provider invocation, credentials, paid services,
  MCP, routing, caching, fallback, streaming, and evaluation excluded pending their
  own authority.

## 2026-08-18 - Open Recommendation Track And Freeze Installation Derivatives

- Recorded the accepted 2026-08-18 project-analysis recommendation track decision and
  opened its four-increment sequential Dynamic Workflow: installation freeze, Delivery
  Gate 9 minimum-slice specification, continuous-integration verification job, and the
  constitutional commit-cadence amendment.
- Recorded the accepted decision freezing installation-subsystem derivative work
  (evidence body/reference schemas, host revalidation, production resolver/store,
  permission and native composition, packaging, retention, anti-rollback) until its
  Delivery Gate 16 consumers exist, while keeping delivered installation code, tests,
  and defect-fix authority unchanged.

## 2026-08-18 - Add Locked Installation Cursor Storage Boundary

- Added the first uncomposed `FileSystemInstallationTransactionStore` over a caller-
  provisioned pre-existing exact-real root, with bounded no-follow point reads, stable
  per-transaction nonblocking OS locks, forced and decoded same-root candidates,
  required atomic create/replace, and post-publication validation.
- Preserved create/CAS exact replay without rewriting, added typed lock contention, and
  covered cross-instance resolution/contention, revision and transition refusal,
  corruption, oversize, foreign binding, non-regular artifacts, and invalid roots in
  JUnit-owned temporary trees.
- Added a separate pure immutable semantic-evidence point-store port with create,
  exact read, first-create/exact-replay receipts, and bounded typed failures. No
  production evidence store or resolver was added because the record has no independently
  revalidatable evidence body.
- Kept permission/native adapter composition, real installation paths, directory/sudden-
  power-loss durability, descriptor-relative confinement, authenticity, anti-rollback,
  automatic recovery, cleanup, and external delivery absent.

## 2026-08-18 - Define Installation Integrity File Formats

- Added distinct deterministic schema-v1 transaction and phase-evidence envelopes with
  bounded bodies, strict UTF-8, canonical fields, domain-separated header/body SHA-256,
  and typed schema/corruption/size/foreign/noncanonical refusals.
- Reconstructed the complete schema-v2 cursor through existing invariants, required an
  exact expected point for evidence decode, and added pure bounded cursor/evidence leaf
  names with a local provider/dialect binding.
- Added focused corruption, cross-kind, schema, truncation, trailing, size, Unicode,
  foreign-point/dialect, deterministic round-trip, and canonical re-encoding tests while
  keeping filesystem I/O, store/resolver implementations, evidence bodies, durability,
  authenticity, locking/CAS, rollback protection, and installation effects absent.

## 2026-08-18 - Define Pure Installation Evidence Reconciliation

- Added an immutable exact phase-evidence point and read-only resolver port with typed
  unsupported, corrupt, foreign, and unavailable failures.
- Added a separate pure reconciler that converts only exact revalidated pending evidence
  into one succeeded compare-and-exchange, classifies absent and exact-replay outcomes,
  and leaves succeeded or terminal state unchanged without phase invocation.
- Added fake-only contract tests for exact, absent, foreign, unavailable, terminal,
  replay, malformed-receipt, and canonical-point behavior while keeping evidence bodies,
  persistence/integrity, production stores/adapters, host effects, and installation
  success absent.

## 2026-08-18 - Deliver Installation Transaction Contracts To Main

- Committed the verified Windows publication-identity hardening and pure installation
  transaction state, coordination, and phase-evidence-prefix contracts as
  `bb7d0ba0050462efac387e530e9ff58573fac538` on the explicit delivery branch.
- Pushed the delivery branch without force, fast-forward merged `main` from `3abe4c5`
  to `bb7d0ba` with `--ff-only`, and advanced `origin/main` through a non-force push.
- Direct remote-ref verification confirmed the delivery branch and `origin/main` at the
  same commit with local divergence `0 0`. No force push, history rewrite, release,
  deployment, real installation, native gateway, or permission mutation occurred.

## 2026-08-14 - Bind Pure Installation Phase Evidence Prefix

- Added a schema-v1 bounded semantic phase-evidence value and upgraded the pure
  transaction cursor to schema v2 with an exact immutable succeeded-evidence prefix.
- Required pending success to append one transaction/phase/revision/digest binding,
  preserved that history across next-phase transitions, and rejected activation drift,
  missing entries, reorder, truncation, replacement, and extra history.
- Connected every coordinator port result to the succeeded store replacement; exact
  terminal replay retains eleven ordered identities without invoking a port or mutating
  the store.
- Kept evidence bodies/references, integrity and durable persistence, production stores
  and ports, pending reconciliation, permission-adapter composition, native/filesystem/
  security-state access, real installation effects, and external delivery absent.

## 2026-08-14 - Define Pure Installation Transaction Coordination

- Distinguished fresh transaction-store creation/advancement from exact replay so only
  the caller that newly persists a pending phase receives invocation ownership.
- Added a pure one-phase-at-a-time coordinator with distinct source/preflight and
  activation ports, an exhaustive named phase-effect port, bounded result binding, and
  typed reconciliation/failure behavior.
- Added fake-only tests for all eleven ordered phases, pending-before-call observation,
  one-phase execution, failure retention, result mismatch, store failure, reconciliation,
  and invocation-free terminal replay.
- Kept phase-evidence persistence, production stores/ports, permission-adapter
  composition, native/filesystem/security-state access, real installation effects,
  activation, automatic retry, threads, runtime/CLI/operator/build wiring, and
  operational recovery absent.

## 2026-08-14 - Define Pure Installation Transaction Recovery Contracts

- Added a schema-v1 immutable transaction cursor that binds the exact installation plan
  to normalized environment/filesystem, release, permission-policy, activation,
  phase/status, and exact revision facts.
- Added a platform-neutral create/resolve/compare-and-exchange store port with bounded
  typed refusals and a test-only in-memory fake proving exact replay, changed-identity
  conflict, stale CAS refusal, strict pending/succeeded order, and metadata/activation
  recovery classification.
- Kept production persistence, serialization/integrity, coordination, native gateways,
  filesystem/security-state access, installation effects, activation, and runtime/CLI/
  operator/build wiring absent; the fake is not durability or operational-recovery
  evidence.

## 2026-08-14 - Bind Windows Publication Identity Through Post-Publication Verification

- Retained the exact target file identity returned by each successful fake-gateway
  atomic publication, keyed by transaction and artifact, without changing the public
  gateway or evidence APIs.
- Required durability and published-security recheck to bind to that retained identity;
  same-volume substitution, pre-publication identity reuse, and changed publication
  replay now fail with their existing typed reasons.
- Added focused replacement, drift, and replay regression coverage while preserving zero
  production gateway implementations and zero Windows/filesystem/ACL/process/shell/native
  calls or real installation effects.

## 2026-08-13 - Deliver The Windows Installation Permission Boundary To Main

- Committed the verified operator distribution, platform-neutral installation permission
  contracts, and fake-gateway Windows adapter boundary as
  `e49c847c2c30fe2ac5a03cc3ed7f052c6afa0d09` on a local delivery branch.
- Fast-forward merged `main` from `3e19243` to `e49c847` with `--ff-only` and advanced
  `origin/main` through a non-force push. No synthetic merge commit, force push, rebase,
  reset, or history rewrite occurred.
- Fresh fetch verified `HEAD`, `main`, and `origin/main` equal with divergence `0 0`.
  Delivery did not perform a real installation, Windows security inspection or mutation,
  deployment, release, tag, cleanup, or external message beyond the requested Git push.

## 2026-08-13 - Add The Windows Installation Permission Adapter Boundary

- Added immutable Windows SID/token, volume/file identity, path/reparse, DACL, raw-right,
  publication, durability, and runtime-probe evidence plus an injected native-gateway
  port with no production implementation.
- Added the sole production `WindowsInstallationPermissionAdapter`, which validates
  exact plan identities and converts only complete fake-gateway-verified Windows facts
  into the platform-neutral evidence contract. Raw publisher rename/replace `DELETE`
  closure remains separate from typed `DELETE`; operator/runtime mutation stays denied.
- Added fake-gateway and architecture tests for all gateway stages, deterministic replay,
  identity/access/topology drift, finite fail-stop reasons, and zero filesystem/ACL/
  process/shell/native/runtime/CLI/operator/build coupling. No real Windows inspection,
  permission change, installation, commit, push, or merge was performed.

## 2026-08-13 - Implement Pure Installation Permission Contracts

- Added a validated already-authorized cancellation-trust installation plan with three
  stable principal roles, derived protected artifact identities, exact digests, fixed
  permission-policy revision, and deterministic policy-before-metadata-before-probe-
  before-activation order.
- Added an exhaustive immutable artifact/effective-access matrix plus a platform-neutral
  `InstallationPermissionAdapter`, bounded evidence values, publication modes, and finite
  fail-closed reasons. The port cannot authorize or represent overall installation
  success.
- Added fake-adapter and architecture tests for validation, matrix denial, stage-specific
  refusal, deterministic replay, and runtime/CLI separation. No platform adapter,
  filesystem call, installer, real permission change, installation, deployment, release,
  commit, push, or merge was performed.

## 2026-08-13 - Specify Operator Installation And Permission Separation

- Specified separate installer/publisher, operator, and runtime principals. The operator
  may request an allowlisted application and write only a private candidate inbox; the
  privileged publisher alone may stage immutable versions, enforce permissions, publish
  protected trust artifacts, probe as runtime, and activate last.
- Defined platform-neutral enforcement plus Windows SID/token/DACL and POSIX numeric
  UID/GID/mode/ACL/capability evidence, an artifact capability matrix, exact transaction
  replay/recovery, audit evidence, and fail-closed publication ordering.
- Explicitly rejected direct unprivileged sibling-metadata mutation, portable Java owner
  checks as authority, partial-file success, automatic rollback/deletion, and local
  metadata/lock/CAS as privileged anti-rollback. No installer, permission mutation, real
  installation, deployment, cleanup, release, commit, push, or merge occurred.

## 2026-08-13 - Package The Cancellation Trust Maintenance Operator

- Added one separate Gradle custom distribution named
  `enhancer-cancellation-trust-maintenance`, with a Gradle-generated Unix/Windows
  launcher pair and the shared project-JAR/runtime classpath under `lib/`. The default
  `EnhancerCli` application distribution remains unchanged.
- Added copied-layout subprocess tests proving configuration `2`, safe refusal `20`,
  durability `70`, INSTALL success, and exact ROTATE replay only against JUnit-owned
  temporary installations. ZIP and TAR each contain only the operator launcher pair and
  project runtime JARs.
- No real installation was selected and no OS installation, permission change,
  deployment, signing, publication, release, cleanup, commit, push, or merge occurred.

## 2026-08-13 - Deliver Cancellation Trust Maintenance To Main

- Delivered the installed cancellation-trust binding, supported cancellation CLI,
  operator-only INSTALL/ROTATE state machine, and separate typed operator launcher in
  ordinary commit `24bcadd089aacce00fde693026a5191c0de3f60c`.
- Advanced `origin/main` linearly from `3367c98` to `24bcadd` through a non-force push.
  The work was already committed directly on `main`, so no separate branch merge or
  synthetic merge commit was required.
- Delivery did not invoke maintenance on a real installation or add installed launcher
  packaging, permission changes, cleanup, deployment, external anti-rollback, tag, or
  release work.

## 2026-08-12 - Add The Separate Cancellation Trust Operator Main

- Added `CancellationTrustMaintenanceOperator` as a distinct Java main outside
  `EnhancerCli`, scheduler, runtime, audit, and event authority. Its exact install/rotate
  grammar rejects unknown, duplicate, reordered, relative, control-bearing, overlong,
  and operation-incompatible inputs before state-machine invocation.
- Added finite typed maintenance reasons and bounded stack-free output. Direct JVM
  success exits `0`, configuration exits `2`, safe existing-binding/lock/stale refusal
  exits `20`, and durability/unexpected failure exits `70` without parsing or exposing
  exception details.
- Added a repository-local `cancellationTrustMaintenance` Gradle `JavaExec` selector
  while preserving `EnhancerCli` as the application main and adding no installed start
  script/distribution. All invocations were isolated temporary-tree tests; no real
  installation, permission, cleanup, deployment, commit, push, or merge occurred.

## 2026-08-12 - Implement Installed Cancellation Trust Maintenance

- Added an unexposed `com.enhancer.maintenance` INSTALL/ROTATE state machine outside
  runtime and CLI authority. It derives the policy digest from one bounded canonical
  public-only snapshot, publishes immutable content-addressed policy first, and switches
  canonical fixed metadata last through required atomic moves.
- Added a persistent stateless nonblocking installation lock, initial/final metadata
  CAS, no-write exact replay including lost-success-response recovery, production-reader
  revalidation, collision refusal, and retained non-authoritative candidates with no
  fallback, rollback, overwrite, scan, or cleanup.
- Added isolated temporary-tree failure injection, contention, stale/drift, replay,
  corruption/private/symbolic-path, collision, retention, snapshot-invariant, and
  runtime-separation tests. No production launcher, real installation write, permission
  change, deployment, cleanup, commit, push, or merge was performed.

## 2026-08-12 - Specify Installed Cancellation Trust Maintenance

- Specified a future operator-only public trust maintenance boundary separate from
  `EnhancerCli`, `scheduler-*`, runtime requests, proofs, audit, AgentRuntime, and events.
  Metadata v1 remains unchanged and the new pin must be computed internally from one
  exact validated canonical public-policy snapshot.
- Fixed policy-first/fixed-metadata-last atomic publication, content-addressed retained
  policies, INSTALL refusal, ROTATE nonblocking lock plus expected-current metadata CAS,
  exact replay, stale-writer refusal, and an old-or-exact-new failure matrix with no
  automatic fallback, rollback, overwrite, cleanup, or private material.
- Corrected the stale current-state limitation that still described the installed trust
  binding and supported cancellation CLI as absent. This specification added no writer,
  installation mutation, permission/security-control change, credential handling,
  rotation execution, privileged anti-rollback claim, commit, or external delivery.

## 2026-08-12 - Bind Installed Cancellation Trust And Compose The CLI

- Added a strict bounded installation metadata loader anchored only to the exact-real
  non-symbolic Enhancer application JAR and its fixed trust-metadata sibling. The
  canonical metadata supplies the absolute policy path and independently protected
  lowercase SHA-256 without CLI, environment/JVM, working-directory, repository,
  request, proof, or ambient-identity discovery.
- Added `scheduler-apply-cancel` over the existing pinned policy, signed exact-request
  authorizer, deterministic authorization audit, filesystem application, and optional
  event publisher. A lazy authorizer preserves terminal replay without transient proof
  or trust reads; success exposes bounded durable metadata, denial exits `20`, invalid
  input/installation configuration exits `2`, and unexpected durable failure exits `70`.
- Added bounded no-follow proof-file reading and focused coverage for metadata format/
  anchor failures, trust-override rejection, signed application, denial/configuration
  isolation, and event suffix recovery after proof and policy deletion. No metadata or
  policy writer, installer, private key, credential, queue/process/Tool/effect
  cancellation, `PAUSE`/`RESUME`, commit, or external delivery was added.

## 2026-08-12 - Deliver The Pinned Cancellation Trust Policy Loader To Main

- Delivered the independently pinned canonical public-only cancellation trust-policy
  loader and exact authorization-audit replay correction in ordinary commit
  `c3fc29313b862d4b35361cea05b8eb66259c37a5`.
- Advanced `origin/main` linearly from `59d644d` to `c3fc293` through a non-force push;
  a fresh fetch proved exact local/remote/fetched/merge-base identity, zero divergence,
  and no synthetic merge commit.
- Delivery synchronization changes only the owning task, changelog, and append-only
  verification evidence. Protected installed pin metadata and
  `scheduler-apply-cancel` remain the next separately authorized task.

## 2026-08-11 - Implement The Pinned Cancellation Trust Policy Loader

- Added `PinnedFileCancellationGrantTrustPolicyLoader`, requiring an absolute normalized
  exact-real policy file plus an independently provisioned complete-file lowercase
  SHA-256. It performs one bounded no-follow read, verifies and parses the same byte
  snapshot, and accepts only its strict canonical UTF-8 public-only Ed25519 v1 format.
- Derived `CancellationGrantTrustPolicy.configurationRevision` from the exact file
  digest and kept policy revision distinct. The loader has no writer, discovery,
  fallback, cache, private-key field, CLI binding, or installed metadata source.
- Tightened authorization-audit exact replay so current trust-configuration revision
  and revocation facts cannot reuse an older authorization identity while retry
  observation time remains revision-free.
- Added real-filesystem coverage for path, pin, format, bounds, key material,
  rotation/rollback, exact audit provenance, authorizer integration, and current
  revocation denial without audit/runtime/event mutation. Protected installed pin
  binding and `scheduler-apply-cancel` remain separate work.
- Fresh focused verification ran 10 tests (9 passed and 1 Windows symbolic-link setup
  skipped), architecture/document governance passed 11 of 11 tests, and the Java 17
  `clean build` ran 761 tests across 146 suites (751 passed, 10 environment-dependent
  skips, 0 failures, 0 errors) with all 8 Gradle tasks executed.

## 2026-08-11 - Deliver Authenticated Cancellation To Main

- Delivered the authorizer-injected filesystem cancellation application, detached
  signed exact-request grant verifier, public-only trust policy, deterministic audit
  store, and audit-backed authorizer in ordinary commit
  `5dd221773ed7e0a98ec5508dbd1a3334ba003f21`.
- Advanced `origin/main` linearly from `6b62632` to `5dd2217` through a non-force push;
  a fresh fetch proved exact local/remote/merge-base identity and no synthetic merge.
- Synchronized the owning delivery task, changelog, and append-only delivery evidence
  in the authorized follow-up evidence boundary without changing implementation,
  architecture, maturity, roadmap position, or host-only handoff facts.

## 2026-08-11 - Implement The Detached Signed Cancellation Authorizer Core

- Added a bounded canonical detached cancellation grant and exact retained-request
  digest, immutable public-only Ed25519 trust policy, current signature/target/request/
  policy/time/lifetime/subject/key/revocation verification, and deterministic actor
  derivation.
- Added the integrity-checked deterministic authorization audit point and audit-backed
  authorizer, preserving first observation on exact replay and failing closed on
  malformed proof, changed authorization reuse, corruption, or persistence failure.
- Added real-filesystem recovery coverage for audit-before-runtime ordering, valid
  audit-only replay, exact-expiry refusal, denial isolation, and unchanged runtime/event
  state on authorization or audit failure.
- Kept production trust-policy loading, proof production/private keys, credentials,
  IdP/session integration, CLI composition, queue/process behavior, pause/resume,
  commit, and push outside this increment.

## 2026-08-11 - Specify The First Authenticated Cancellation Interface Composition

- Selected a short-lived detached signed exact-request grant, verified against
  separately provisioned operator-owned public trust policy, as the first admissible
  source for an interface-composed `ControlRequestAuthorizer`.
- Fixed the complete retained-request, Goal, `CANCEL`, authorization, issuer/key/
  subject, policy, issue/expiry, clock, rotation, revocation, and self-approval denial
  boundaries without introducing a credential or interface implementation.
- Specified a deterministic non-secret authorization audit point before runtime
  application, current validation for an audit-only retry, and preservation of the
  existing terminal-runtime event/publication recovery semantics.
- Named a separately authorized production CLI cancellation-application command as the
  first future consumer; verifier, trust configuration, audit store, CLI, credentials,
  IdP/session integration, queue/process behavior, and pause/resume remain unimplemented.

## 2026-08-11 - Compose The Authenticated Cancellation Filesystem Application API

- Added the supported `FileSystemAuthenticatedCancellationApplication` with mandatory
  runtime root, injected clock, and trusted `ControlRequestAuthorizer`, delegating the
  exact Goal and retained Control identity to the existing terminal transition owner.
- Added the all-or-none `FileSystemRuntimeEventPublicationConfiguration` for optional
  concrete event-store, recorder, and bounded reference-point publisher composition.
- Added real-filesystem coverage for approved `CANCELLATION_APPLIED` publication,
  byte- and revision-stable replay without reauthorization, denial isolation,
  event-free omission, and configuration bounds. No CLI or credential adapter, queue
  disposition, process signal, Tool/effect cancellation, commit, or push was added.

## 2026-08-07 - Deliver Result-Side Scheduler Runtime Events To Main

- Delivered the Result-side Scheduler runtime-event composition and exact publication
  recovery in ordinary commit `4d4996541795581608f3417e1e73f87243ddb3cd`.
- Advanced `origin/main` linearly from `f7d8168` to `4d49965` through a non-force push;
  a fresh fetch proved exact local/remote/merge-base identity and no synthetic merge.
- Synchronized the owning delivery task, changelog, and append-only delivery evidence
  in the authorized follow-up evidence boundary without changing implementation,
  architecture, maturity, roadmap position, or host-only handoff facts.

## 2026-08-07 - Compose Result-Side Runtime Events Across Scheduler Commands

- Reused the existing optional Scheduler filesystem event recorder in
  `DurableAgentRunFinalizer` across cycle, drain, and service without adding a CLI
  option, schema, event kind, or transition authority.
- Added a capacity-one real-filesystem recovery integration proving durable Result,
  verification, queue disposition, and termination ordering across all three commands;
  acknowledgement and exact re-entry add no child execution, RunRecord, source/event
  revision, or disposition.
- Updated retry and lease-timeout integrations to assert the full multi-owner event
  sequences introduced by finalizer composition while preserving event-free omission.

## 2026-08-07 - Deliver Retry Event And Terminal Result Recovery To Main

- Delivered the retry runtime-event Scheduler composition and terminal Result
  recovery-order correction in ordinary commit
  `f4e9c08f2f62de93d72e271d87ca5683a53aaab6`.
- Advanced `origin/main` linearly from `9822d72` to `f4e9c08` through a non-force push;
  a fresh fetch proved exact local/remote/merge-base identity and no synthetic merge.
- Synchronized the owning delivery task, changelog, and append-only delivery evidence
  in the authorized follow-up evidence boundary without changing implementation,
  architecture, maturity, roadmap position, or host-only handoff facts.

## 2026-08-06 - Repair Terminal Result Recovery Ordering

- Made `DurableAgentRunWorker` exact-replay a checkpointed latest `COMPLETED` or
  `FAILED` Result before retry control or terminal queue disposition recovery.
- Added focused mismatch cases proving a changed checkpoint RunRecord reference fails
  before retry decisions, terminal dispositions, execution, or checkpoint clearing,
  while the established active-work requeue preference remains intact.
- Preserved the existing finalizer API, retry policy, queue/runtime/checkpoint schemas,
  and event-free supported Scheduler construction. Finalizer recorder composition across
  cycle, drain, and service remains a separate next task.

## 2026-08-06 - Compose Retry Runtime Events Across Scheduler Commands

- Reused the existing optional Scheduler filesystem event configuration across cycle,
  drain, and service for retry decision and replacement-start publication without a new
  CLI option, root, schema, or publisher.
- Made the process-isolated Worker conditionally construct its retry controller with the
  same recorder and injected clock, preserving event-free omission and source-first
  exact replay from the existing decision and replacement checkpoints.
- Added a named real-filesystem CLI integration that drives two failed attempts through
  admitted decision, retry start, and final refusal on all three commands, retaining two
  RunRecords, one failed queue disposition, three opaque event points, and no pending
  cycle checkpoint. Finalizer and other event owners, commit, push, release, deployment,
  cleanup, and retention remain separate.

## 2026-08-05 - Consolidate Development Workflow Policy Ownership In AGENTS.md

- Declared `AGENTS.md` the single AI-agent entrypoint (this repository intentionally
  has no `CLAUDE.md`) and added working rules stating that the document-ownership
  guards make every Markdown edit test-relevant and that build/setup/test commands are
  owned by `README.md`.
- Moved development-session checkpoint command usage into `AGENTS.md` and reduced the
  duplicated `.ai/workflow.md` and `README.md` checkpoint, dynamic-workflow, and
  adaptive-subagent sections to references to their owning `AGENTS.md` sections.
- Recorded the consolidation and its delivery authorization as accepted decisions and
  delivered the documentation-only change directly to `main`.

## 2026-08-05 - Deliver Lease Timeout Scheduler Composition To Main

- Delivered the verified lease-timeout runtime-event publication composition through
  ordinary commit `7769c34` and a non-force direct-main update.
- Confirmed by fresh fetch that local and remote `main` contain the same linear commit,
  so no synthetic branch or merge commit was needed to satisfy the requested merge.
- Retained the existing Gate 8 maturity and excluded release, deployment, tag, cleanup,
  retention, history rewrite, and unrelated runtime-event owner work.

## 2026-08-05 - Compose Lease Timeout Publication Across Scheduler Commands

- Reused the existing optional Scheduler filesystem event configuration across cycle,
  drain, and service for AgentRuntime lease recovery without adding a CLI option.
- Threaded the same recorder through WorkItem-matched dispatcher recovery and the
  worker's direct runtime-recovery sites while leaving finalizer, retry, Tool timeout,
  verification, cancellation, stagnation, and terminal owners event-free.
- Added deterministic real-filesystem coverage for one retained LEASE timeout event and
  opaque point from every supported Scheduler command, plus missing event/point repair
  and acknowledged-point replay without another source revision, execution, or
  RunRecord. Commit, push, merge, release, deployment, cleanup, retention, and another
  owner composition remain separate.

## 2026-08-05 - Deliver Accumulated Runtime Event Work To Main

- Delivered the verified read-only publication-point resolver, deterministic
  acknowledgement/capacity release, and process-timeout publication composition for
  Scheduler cycle, drain, and service together on `main`.
- Used ordinary direct-main integration from an aligned local/remote baseline and a
  non-force push; no temporary branch, synthetic merge commit, history rewrite,
  release tag, deployment, or additional runtime-event owner entered the delivery.

## 2026-08-05 - Compose Process Timeout Publication Across Scheduler Commands

- Added one optional all-or-none runtime-event store root, publication root, and bounded
  capacity group to `scheduler-cycle`, `scheduler-drain`, and `scheduler-service` while
  preserving their existing event-free invocation behavior.
- Routed the resulting filesystem recorder through the shared worker construction only
  to `ProcessIsolatedAgentRunExecution`, retaining fact -> PROCESS timeout event ->
  opaque point ordering without composing retry, finalizer, runtime-recovery, lease/
  Tool-timeout, or terminal-disposition owners.
- Added real self-JVM evidence across all three commands plus exact acknowledged replay,
  point resolution, retained checkpoint, and no RunRecord or terminal queue disposition.
  Commit, push, other owner composition, background service, external delivery,
  cleanup, and retention remain separate.

## 2026-08-05 - Release Runtime Event Publication Capacity By Acknowledgement

- Added `FileSystemRuntimeEventPointAcknowledger` to fully revalidate one explicit
  pending or retained acknowledged point and exact event before a same-root atomic
  rename or revision-free `ALREADY_ACKNOWLEDGED` replay.
- Extended `FileSystemRuntimeEventPublisher` to recognize exact retained
  `.runtime-event-received` state before capacity evaluation, count only pending points,
  prevent pending recreation, and fail closed on conflicting or invalid state.
- Added the supported `runtime-event-acknowledge` CLI and real-filesystem evidence for
  first acknowledgement, lost-response replay, pending-capacity release, unchanged
  event/point content and revision, and nonmutating invalid-state failure.
- Kept handler delivery, event application, consumer offsets, acknowledged-history
  bounds, deletion, scans, cleanup/retention, additional owner composition, commit, and
  delivery separate.

## 2026-08-05 - Resolve Runtime Event Publication Points Read-Only

- Added `FileSystemRuntimeEventPointReader` to validate one explicit regular
  non-symbolic schema-v1 publication point, deterministic filename and reference
  grammar, then resolve the exact event from only its bounded Goal stream.
- Added the supported `runtime-event-read` CLI with explicit event/publication roots and
  point filename plus bounded typed identity, kind, time, provenance, stream-revision,
  and reference-count output.
- Proved exact repeated reads and missing/corrupt failures leave event and publication
  artifacts unchanged. Acknowledgement, capacity release, scans, event application,
  cleanup/retention, additional owner composition, commit, and delivery remain separate.

## 2026-08-04 - Let Primary Agents Select Development Subagents Adaptively

- Replaced one-off development-subagent topology selection with a repository-wide
  primary-Agent decision based on independent work, risk/quality benefit, latency, and
  coordination cost, while retaining single-agent execution for local or coupled work.
- Bounded the first policy to read-only children, maximum concurrency three, depth one,
  fixed increment/task dispatch limits, primary-only mutation/evidence/lifecycle
  ownership, mandatory join, and safe single-agent fallback.
- Kept Dynamic Workflow selection sequential and separated host development delegation
  from Gate 13 product/runtime multi-agent capability and every underlying privileged-
  action authority boundary.

## 2026-08-04 - Compose Runtime Event Publication In The Supported Control Receiver

- Used the approved five-increment Dynamic Workflow and joined three bounded read-only
  subagent analyses before the primary Agent selected the existing Control receiver as
  the narrowest supported composition boundary.
- Added an optional all-or-none `scheduler-receive-control` group for the runtime-event
  store root, publication root, and capacity. Existing invocations remain request-only;
  complete configuration constructs the concrete store, publisher, and recorder for
  `CANCELLATION_REQUEST_RECORDED` without changing MessageEnvelope or control authority.
- Proved durable request-event-publication-acknowledgement ordering, exact `.received`
  replay, and capacity-failure recovery. Consumers, other event-owner and Scheduler
  composition, authenticated application, cleanup, retention, commit, and delivery
  remain separate.

## 2026-08-04 - Publish Runtime Event References As Bounded Filesystem Points

- Added `FileSystemRuntimeEventPublisher` as the first concrete implementation of the
  existing opaque-reference port, without changing the four-kind MessageEnvelope or
  resolving/copying a runtime-event body.
- Added deterministic SHA-256 point identities, capacity bounds from 1 through 4096,
  schema-v1 strict-UTF-8 integrity envelopes, forced same-root candidates, atomic
  non-replacing publication, and exact replay before capacity evaluation.
- Proved corruption, symbolic/non-regular points, unusable roots, and capacity refusal
  fail closed while retained exact points remain replayable. Supported Scheduler/CLI
  composition, consumers, acknowledgement, routing, scan, cleanup, retention, and
  cross-process coordination remain separate.

## 2026-08-04 - Apply Authenticated Cancellation And Record Its Runtime Event

- Added a trusted `ControlRequestAuthorizer` port and typed approved/denied decisions;
  existing Control-envelope producer, reason, transport acceptance, and durable
  admission remain non-authoritative.
- Evolved AgentRuntime to schema v4 with one immutable authorization-bound
  `CancellationApplicationRecord`, terminal `CANCELLED` Goal/current-AgentRun state,
  lease removal, retry-pending cancellation, exact replay without reauthorization, and
  strict filesystem recovery/prefix validation.
- Connected event-aware `CANCELLATION_APPLIED` recording after source persistence with
  retained application time, exact Work/Goal/AgentRun provenance, Control causation,
  stable message/application references, missing-event repair, and publication-failure
  exact replay. Process signalling, cancelled queue disposition, credential/interfaces,
  `PAUSE`/`RESUME`, concrete event transport, and earlier-schema migration remain
  separate.

## 2026-08-04 - Persist And Connect Lease Timeout Runtime Events

- Evolved AgentRuntime state to schema v3 with a bounded exact-prefix
  `LeaseTimeoutRecord` ledger appended atomically with expired
  `EXECUTING -> READY` reclamation and retained across later runtime progress.
- Added event-aware recovery that records `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.LEASE` only after the record-bearing runtime revision is durable,
  using retained expiry, Work causation, and a stable Goal/AgentRun/fence reference.
- Proved missing-event repair and publication-failure exact replay without another
  runtime revision. Earlier runtime-schema migration, automatic post-reclaim execution,
  concrete publication, supported CLI event composition, and external delivery remain
  separate.

## 2026-08-04 - Persist And Connect Process Timeout Runtime Events

- Added a deterministic, integrity-checked process-timeout fact and filesystem point
  store under the process invocation root, preserving exact Work/Goal/AgentRun binding,
  occurrence, configured timeout, bounded reason, semantic digest, and rewrite-free
  restart replay.
- Changed `ProcessIsolatedAgentRunExecution` to persist a typed watchdog timeout before
  exposing failure and to resolve it before spooling or launching on re-entry; start
  failure, completed failure, and successful execution create no timeout fact.
- Added event-aware derivation of `TIMEOUT_DETECTED` with
  `RuntimeTimeoutKind.PROCESS`, Work-message causation, and a stable process-timeout
  reference, including missing-event repair and exact replay after publication failure.
  AgentRun lifecycle/retry policy and supported CLI event publication remain unchanged.

## 2026-08-04 - Connect Persisted Tool Timeout Runtime Event Recording

- Selected the bound persisted RunRecord `TIMED_OUT` Tool failure as the first
  authoritative timeout source, while deferring process and lease timeouts until each
  retains its own typed durable fact and transition owner.
- Extended event-aware `DurableAgentRunFinalizer.recordAgentRunResult` to derive
  `TIMEOUT_DETECTED` with `RuntimeTimeoutKind.TOOL` only after the matching Result and
  separate verification fact are durable, using RunRecord occurrence, Result causation,
  and stable Result-message plus RunRecord references.
- Proved verification/timeout/stagnation ordering, non-timeout exclusion, exact replay,
  and Result-persistence failure isolation without changing a schema or adding concrete
  publication, supported Worker/CLI composition, or Gate 8 promotion.

## 2026-08-03 - Enable Document-Driven Dynamic Increment Workflows

- Added an optional two-through-sixteen increment queue inside the single
  `CURRENT_TASK.md` Active Task, with sequential mode, stable identities, explicit
  dependencies, bounded states, exit/verification requirements, deterministic successor
  selection, and fail-closed stop conditions.
- Connected the contract across repository Agent instructions, compact AI workflow and
  Architecture mirrors, implementation/session prompts, and user-facing README guidance
  without amending the Constitution or granting new external-action authority.
- Added `DynamicWorkflowDocumentTest` to enforce the live workflow grammar and required
  instruction connections while keeping runtime Workflow Engine, parallel/background
  work, automatic approval/delivery, and multi-agent execution out of scope.

## 2026-08-03 - Connect Stagnation Runtime Event Recording

- Extended event-aware `DurableAgentRunFinalizer.recordAgentRunResult` to derive
  `STAGNATION_DETECTED` only after a bound `STAGNATED` RunRecord reaches a durable Result
  transition.
- Preserved verification as a separate earlier fact and bound stagnation identity to
  stable Result-message and RunRecord references, with occurrence and iterations from
  the RunRecord and threshold three from the current default Agent Loop policy.
- Proved exact event/publication repair after later runtime progress and retained
  verification-only behavior for non-stagnated records without changing source schemas,
  choosing a timeout owner, or adding supported publication wiring.

## 2026-08-03 - Connect Retry Started Runtime Event Recording

- Extended event-aware `DurableAgentRunRetryController.beginAdmittedRetry` to derive
  `RETRY_STARTED` only after the caller-checkpointed replacement AgentRun is durable.
- Bound event identity to the stable admitted retry decision and replacement AgentRun,
  retaining the previous failed attempt, causal Result, exact Work provenance, and
  first persisted occurrence without depending on a later mutable runtime revision.
- Proved decision-before-start order, replacement-store failure isolation, publisher
  recovery, and missing-event repair after later replacement readiness while preserving
  legacy event-free construction and refused abandonment behavior.

## 2026-08-03 - Connect Retry Decision Runtime Event Recording

- Added optional event-aware `DurableAgentRunRetryController` construction while
  preserving the existing event-free path.
- Derived `RETRY_DECISION_RECORDED` only after durable decision persistence from the
  exact failed-attempt binding, decision outcome, causal Result, stable retry-decision
  identity, and decision-bearing runtime revision.
- Proved source-failure isolation, event-append repair, later-clock publisher recovery
  with first-occurrence reuse, exact no-revision replay, and separation from
  `RETRY_STARTED` without adding schema, concrete publisher, or Worker/CLI wiring.

## 2026-08-03 - Deliver Runtime Event Owner Connections To Main

- Committed the three verified runtime-event owner connections and their owned tests,
  decisions, and project-document synchronization as `f3eecc8` on the dedicated
  `codex/runtime-event-owner-connections-20260803` branch.
- Pushed the branch, fast-forward merged it into `main`, and pushed `main` without
  force, rebase, history rewriting, branch deletion, release, tag, or deployment.
- Reconciled the fetched remote baseline before delivery and retained the separate next
  task for `RETRY_DECISION_RECORDED` retry-controller integration.

## 2026-08-03 - Connect Terminal WorkItem Runtime Event Recording

- Connected event-aware `DurableAgentRunFinalizer` construction to derive
  `WORK_ITEM_TERMINATED` only after the target WorkItem is durably present in the
  matching verified-completed or failed queue partition.
- Used a stable queue/WorkItem/disposition reference so event identity survives later
  whole-queue revisions, and added recorder recovery of the first persisted occurrence
  time without weakening exact-content replay checks.
- Proved missing-event repair after later queue progress, revision-free repeat
  publication under a later clock, queue-persistence failure isolation, and recovery
  after publisher failure without changing the queue schema or adding a concrete
  publisher.

## 2026-08-03 - Connect Verification Runtime Event Recording

- Connected event-aware `DurableAgentRunFinalizer` construction to derive
  `VERIFICATION_RECORDED` only after the RunRecord-backed Result transition is durable.
- Bound deterministic repair to the retained Result message and RunRecord reference,
  preserving occurrence time, verification status, causation, and exact runtime work
  provenance even after later runtime revisions.
- Proved missing-event repair, revision-free repeat publication, Result persistence
  failure isolation, and recovery after publisher failure while keeping
  `WORK_ITEM_TERMINATED` separate from this increment.

## 2026-08-03 - Connect Cancellation Request Runtime Event Recording

- Added `RuntimeEventRecorder`, an opaque deterministic publication reference, and a
  publisher port that receives no event body and is invoked only after append or exact
  replay succeeds.
- Connected event-aware Control admission to derive
  `CANCELLATION_REQUEST_RECORDED` only after the exact `CANCEL` request is durable,
  preserving retained Goal, WorkItem, AgentRun, task, snapshot, run, correlation,
  message, occurrence-time, and runtime-revision provenance.
- Proved missing-event repair, revision-free replay with repeat publication, source
  failure isolation, and request-only `PAUSE`/`RESUME` behavior without changing the
  four-kind MessageEnvelope or adding a concrete publisher adapter.

## 2026-07-31 - Implement The Runtime Event Value And Store

- Added the immutable eight-kind `runtime-event-v1` value, sealed kind-specific
  details, exact runtime provenance binding, bounded typed authoritative references,
  and deterministic domain-separated event UUIDs.
- Added a 4096-event append-only per-Goal stream with monotonic revisions, exact
  revision-free replay, and fail-closed changed-identity, foreign-binding, prefix, and
  overflow validation.
- Added `RuntimeEventStore` and an atomic integrity-checked strict-UTF-8 filesystem
  adapter that rejects missing, corrupt, oversized, trailing, unsupported-schema, and
  symbolic-root artifacts.
- Kept transition recording, publication, MessageEnvelope evolution, authenticated
  control, budgets, Memory, production adapters, workers, migration, scan, retention,
  cleanup, and cross-store coordination out of this increment.

## 2026-07-31 - Specify Goal-Bound Durable Runtime Events

- Defined a finite Gate 8 event taxonomy that separates retry decision and start,
  stagnation and timeout detection, cancellation request and authenticated application,
  verification, and terminal Scheduler disposition.
- Bound every event to deterministic Goal/AgentRun/WorkItem identity, existing
  task/snapshot/run/correlation provenance, kind-specific detail, and bounded
  authoritative references without copying content or granting authority.
- Required authoritative state to persist before append-only event recording and
  opaque-reference publication, with exact replay and duplicate-publication
  deduplication by event identity.
- Kept the existing four-kind MessageEnvelope unchanged and deferred concrete
  publication wiring, authenticated controls, budgets, Memory, adapters, and role
  workers to separately authorized work.

## 2026-07-29 - Retain Gate 8 And Select Explicit Runtime Events

- Reassessed every Gate 8 scope item and exit criterion after both process-isolated Work
  and Result crossed real Message Bus queues.
- Classified the bounded single-agent Scheduler/runtime foundation as Integrated while
  retaining the whole gate at `Specified - Next`.
- Confirmed durable interruption recovery, authority preservation, supported
  duplicate/lost-ack/lease/migration recovery, and recorded external-effect outcomes.
- Assigned budgets, Memory, authenticated controls, production adapters, and role
  workers to Gates 9 through 13 and selected the Gate 8-owned explicit runtime-event
  contract as the next bounded task.

## 2026-07-29 - Stop Adding Unowned Gate 7 Connections

- Reassessed every remaining Gate 7 topic, Handoff, reliability, durable-journal,
  directory-consumption, and retention branch after isolated child Work ingress.
- Confirmed that no production owner exists for topic, Handoff, cancellation,
  dead-letter re-delivery, re-entrant ordering, or in-process pending backpressure.
- Deferred durable journaling, directory consumption, and retention until their
  checkpoint/compaction, claim/restart, cleanup-authority, and audit/replay policies are
  accepted.
- Kept Gate 7 at Contract Verified and selected a fresh Gate 8 maturity reassessment
  using the current process-isolated Work/Result Message Bus evidence.

## 2026-07-29 - Route Isolated Worker Work Through The Message Bus

- Added `IsolatedWorkMessageHandler` to construct the exact parent-identified WorkItem,
  invoke the unchanged Gate 1-4 execution boundary, and expose the persisted RunRecord
  reference/status only after handler success.
- Routed the decoded child Work transport message through a fresh
  `InProcessMessageBus` queue and required exactly one `DELIVERED` outcome before Result
  publication.
- Made a foreign Work destination reach `UNROUTED` before execution, RunRecord
  persistence, or Result publication while preserving the existing child exit codes,
  spool format, identities, evidence, RunRecord, restart, and parent Result-validation
  contracts.
- Focused and adjacent process-isolated verification passed 23 tests with no skips,
  failures, or errors. The fresh strict full build passed 644 tests across 127 suites:
  640 passed, four Windows privilege-dependent symbolic-link cases skipped, and zero
  failures or errors.

## 2026-07-29 - Publish Goal-Bound Control Intent Without Applying It

- Added `ControlSpoolPublisher` and `scheduler-spool-control` to read one existing
  active/non-terminal Goal without recovery and derive correlation, logical-run, and
  causation solely from its retained Work envelope.
- Preserved caller ownership of the new message identity, producer, occurrence time,
  signal, and reason while reporting only file-spool `ACCEPTED`, `BACKPRESSURED`, or
  `UNAVAILABLE`.
- Connected the accepted point to the separate existing Control receiver in a named
  real-filesystem integration, proving exact durable request admission and atomic
  acknowledgement without authentication or application.
- Focused and adjacent regression passed 40 tests, and the fresh strict full build
  passed 643 tests across 126 suites: 639 passed, four Windows privilege-dependent
  symbolic-link cases skipped, and zero failures or errors.

## 2026-07-29 - Receive Durable Control Spools Without Applying Them

- Added `scheduler-receive-control` for one explicitly named pending or acknowledged
  local transport point.
- Connected the unchanged `ControlPayload` through the real Message Bus to the existing
  durable Goal request ledger, distinguishing newly recorded intent from exact
  revision-free replay.
- Reused the Work receiver's exact point validation and same-directory atomic
  acknowledgement pattern, moving a pending point only after durable persistence.
- Kept Control authentication, application, bus cancellation, worker interruption,
  lease or queue mutation, directory consumption, cleanup, and durable journaling out
  of the connection.
- The fresh strict full build passed 638 tests across 124 suites: 634 passed, four
  Windows privilege-dependent symbolic-link cases skipped, and zero failures or errors.

## 2026-07-28 - Route Isolated Worker Results Through The Message Bus

- Extracted exact isolated Result and RunRecord validation into a bounded queue handler
  with no persistence, execution, finalization, or cleanup authority.
- Routed the unchanged decoded Result through a fresh `InProcessMessageBus` and returned
  the reference only after exactly one `DELIVERED` outcome.
- Preserved child production, file-spool format, restart re-entry, RunRecord authority,
  and all existing foreign-route/identity/payload/binding/status refusals.
- Focused coverage passed all 18 isolated-execution cases, and the fresh strict full
  build passed 632 tests across 122 suites with four Windows privilege-dependent skips
  and no failures.

## 2026-07-28 - Select The Existing Isolated Result Path As The Next Gate 7 Connection

- Reassessed remaining Result, Handoff, topic, reliability, durable-journal, and
  retention branches after governed Work publication.
- Selected the existing isolated-worker Result producer/spool/RunRecord/finalizer path
  for a bounded Message Bus queue connection with no new store, schema, or authority.
- Corrected current maturity text that still described `MessageTransport` as lacking a
  named real production connection after the governed publisher was implemented.
- Fresh structural checks passed all 11 tests, and the strict full build passed 632
  tests across 122 suites with four Windows privilege-dependent skips and no failures.

## 2026-07-28 - Publish Governed Work Through The Supported Spool Point Path

- Added `scheduler-spool-work` to construct one active-task-authorized,
  repository-snapshot-bound Work envelope and publish it through
  `FileSpoolMessageTransport`.
- Preserved the transport-neutral outcome while returning the accepted canonical point
  filename from the concrete adapter, so the separate receiver needs no directory scan.
- Connected publication to separate receipt, durable admission, and atomic
  acknowledgement in a real-filesystem integration; covered backpressure and unavailable
  refusal without extra or partial files.
- The fresh strict full build passed 632 tests across 122 suites: 628 passed, four
  Windows symbolic-link privilege-dependent cases skipped, and zero failures or errors.

## 2026-07-28 - Select Governed Work Spool Publication As The Next Gate 7 Connection

- Reassessed result, handoff, topic, retry/dead-letter, cancellation, ordering,
  backpressure, transport-publication, durable-journal, and retention branches after
  post-admission Work spool acknowledgement.
- Selected a separate governed `scheduler-spool-work` publication path whose accepted
  artifact is consumed by the existing acknowledged point receiver and whose output
  preserves hop-level `ACCEPTED`/`BACKPRESSURED`/`UNAVAILABLE` semantics.
- Deferred branches without a current owning consumer and kept directory scanning,
  cleanup/retention, durable bus persistence, background lifecycle, remote transport,
  schema, dependency, authority, commit, push, merge, release, and deployment out of the
  assessment.
- Corrected current-state descriptions that still said the completed point receiver
  retained pending `.transport` evidence without acknowledgement.

## 2026-07-28 - Acknowledge Durable Work Spools After Admission

- Extended `scheduler-receive-work` to resolve exactly one pending `.transport` point
  or deterministic same-root `.received` point before queue recovery.
- Added post-admission same-directory atomic acknowledgement without replacement or
  fallback, exact acknowledged-point re-entry, separate spool status output, and
  pending-capacity release while retaining acknowledgement evidence.
- Added real-filesystem RED-to-GREEN coverage for first acknowledgement, exact replay,
  capacity release, ambiguous-point refusal before active-queue recovery, and symbolic
  acknowledged-point refusal.
- The fresh full build passed 627 tests across 121 suites: 623 passed, four Windows
  symbolic-link privilege-dependent cases skipped, and zero failures or errors.
- Added no directory scan, cleanup, retention policy, durable bus journal, queue
  creation, combined execution, schema, dependency, authority, commit, push, release,
  or deployment.

## 2026-07-28 - Select Spool Acknowledgement Before Durable Bus Persistence

- Compared the two remaining Gate 7 recovery connections after the supported Work point
  receiver and selected explicit retained-spool acknowledgement first.
- Defined a follow-up same-root atomic pending-to-acknowledged rename only after durable
  admission, acknowledged-point re-entry after a lost result, exact route/payload
  revalidation, and pending-capacity release.
- Deferred durable bus journaling until publication, subscription, retry, dead-letter,
  cancellation, recovery ordering, and truncation have named consumers and policy.
- Added no production/test behavior, spool mutation, cleanup, retention claim, schema,
  dependency, authority, commit, push, release, or deployment.

## 2026-07-28 - Deliver The Durable Work Spool Receiver

- Committed the completed receiver increment on
  `feat/gate-8-durable-work-spool-receiver` as `3f59aeb`, pushed that branch, and merged
  it into `main` through merge commit `1481223`.
- Pushed the merged `main` to `origin/main` after confirming the remote base was
  unchanged.

## 2026-07-28 - Connect One Durable Work Spool To The Scheduler

- Added the separate `scheduler-receive-work` point command: one caller-named canonical
  regular non-symbolic transport artifact is decoded, route/payload checked, and
  published unchanged through `InProcessMessageBus` to durable Scheduler admission.
- Added bounded `ADMITTED`/`REPLAYED`, queue revision, WorkItem identity, and priority
  output. Exact re-receipt is revision-free; changed identity content, foreign routes,
  non-Work payloads, missing/symbolic points, and corrupt frames fail before admission.
- Added a real-filesystem integration from `FileSpoolMessageTransport` through the
  receiver and a separately invoked process-isolated `scheduler-service`, proving one
  verified completion and no second queue revision, AgentRun, or RunRecord after exact
  re-receipt.
- Retained every spool artifact and added no acknowledgement/deletion/rename, scan,
  queue creation, combined execution wrapper, durable bus journal, schema, dependency,
  authority, commit, push, release, or deployment.

## 2026-07-28 - Reassess Gate 8 After The Bounded Service Connection

- Retained whole-Gate 8 at `Specified - Next` after reconciling the Integrated bounded
  foreground service with every remaining scope item and exit criterion.
- Identified the next missing supported connection as durable message-bus-to-worker
  operation jointly owned by Gates 7 and 8.
- Kept authenticated controls, model/context budgets, Memory runtime, production
  adapters, and background/supervisor topology with Gates 9 through 13.
- Confirmed that existing point recovery and expired-lease reclamation do not authorize
  a general orphan scanner or cleanup/retention policy. Added no runtime behavior, schema,
  dependency, or authority.

## 2026-07-28 - Connect The Bounded Scheduler Service CLI

- Added the separate foreground `scheduler-service` command over
  `BoundedSchedulerService`, reusing every explicit process-isolated cycle recovery input
  and requiring finite total-cycle, consecutive-idle, and idle-wait bounds.
- Added bounded typed stop/count, queue, and RunRecord output with the existing Scheduler
  work-failed exit. The invoking thread's interrupt state supplies the local lifecycle
  stop signal.
- Added real-filesystem integration evidence for empty bounded idle termination,
  persisted cycle-intent restart, and expired executing-lease reclamation under the same
  Goal/AgentRun with a greater fence and one terminal disposition.
- Added no thread, daemon, supervisor, service checkpoint, authenticated control,
  queue/admission, broader orphan scanner, external adapter, schema, commit, push,
  release, or deployment.

## 2026-07-28 - Implement Bounded Scheduler Service Lifecycle Contract

- Added caller-driven `BoundedSchedulerService` over the existing recoverable one-cycle
  worker with finite total-cycle, consecutive-idle, and idle-wait policy bounds.
- Added typed stop reasons and exact invoked/verified/idle/failed counts, including local
  stop checks before each cycle, idle reset after verified work, first-failure stop, and
  interruption restoration without a later cycle.
- Added focused RED-to-GREEN contract tests. No thread, supported command/API, durable
  service progress, authenticated control, external adapter, queue/admission, schema,
  commit, push, release, or deployment was added.

## 2026-07-28 - Reassess Remaining Gate 8 Connection Gaps

- Reconciled the previous whole-gate assessment with completed priority admission,
  fairness selection and observability, supported migration, and named
  lost-acknowledgement recovery evidence.
- Separated the remaining blockers by owner: Gate 8 service/orphan recovery, Gate 12
  authenticated controls, Gate 11 production external adapters, Gates 9/10 budgets and
  Memory runtime, and Gate 13 role-based message workers.
- Selected no implementation silently because every remaining path changes a materially
  different authority, operating, or orchestration boundary.

## 2026-07-28 - Surface Scheduler Priority And Fairness Status

- Extended the pure `SchedulerQueueStatus` projection with each admission's persisted
  `NORMAL`/`EXPEDITED` priority plus the queue's maximum expedited burst, consecutive
  expedited-claim progress, and optional recovery-preferred identity.
- Extended bounded `scheduler-status` output with the same read-only selection state and
  an admission-ordered identity/state/priority prefix.
- Left queue selection, recovery, execution, schema, authority, polling, commit, push,
  release, and deployment unchanged.

## 2026-07-28 - Connect scheduler-submit-generated Optional Priority Input And Output

- Added the same optional `--priority NORMAL|EXPEDITED` input to the generated-input
  `scheduler-submit-generated` command, defaulting to `NORMAL` on omission and rejecting
  any other value.
- Persisted the caller-owned priority into the generated manifest on first use and
  compared it against the stored manifest on replay before consulting the clock or
  recapturing repository context, failing closed on a changed priority; reported the
  effective `priority` in bounded output.
- Left the generic message-admission `NORMAL` default, queue selection/fairness, schema,
  authority, dependencies, commit, push, merge, release, and deployment unchanged.

## 2026-07-28 - Connect scheduler-submit Optional Priority Input And Output

- Added an optional `--priority NORMAL|EXPEDITED` input to the explicit
  `scheduler-submit` command, defaulting to `NORMAL` on omission and rejecting any other
  value before manifest or queue mutation.
- Propagated the selected priority through the immutable submission manifest into exact
  queue admission and reported the effective `priority` in bounded `ADMITTED`/`REPLAYED`
  output; a replay-conflicting priority fails closed like other changed content.
- Left `scheduler-submit-generated`, the generic message-admission `NORMAL` default,
  `WorkItem`/payload/Tool authority, schema, dependencies, commit, push, merge, release,
  and deployment unchanged.

## 2026-07-27 - Deliver Submission Manifest Schema V2 Priority Migration

- Verified local `main` and `origin/main` shared base `406ea06` with divergence `0 0`
  before delivery; no topic branch existed, so no artificial merge commit was created.
- Fresh strict delivery verification passed 589 tests across 117 suites with zero
  failures or errors and three existing Windows symbolic-link privilege skips.
- Committed the implementation, tests, accepted decisions, and synchronized documents
  as `b6f7505`, then pushed local `main` to `origin/main` without force.
- Fresh remote inspection confirmed `refs/heads/main` at
  `b6f75051b4fb1f2bc8bb1a574e5526afec4acc88`.

## 2026-07-27 - Persist Submission Priority In Manifest Schema V2

- Advanced immutable submission manifests to schema v2 with exact
  `NORMAL`/`EXPEDITED` Scheduler priority and preserved existing constructors and
  submission commands as `NORMAL`.
- Propagated stored priority through durable submission into dependency-free exact
  queue admission, including replay conflict refusal for a changed priority.
- Added candidate-first schema-v1-to-v2 migration assigning `NORMAL` and the bounded
  `scheduler-migrate-submission-manifest` command with typed absent/current/migrated
  outcomes.
- Kept `WorkItem`, envelope/payload authority, public submission priority input,
  execution, commit, push, merge, release, and deployment unchanged.

## 2026-07-27 - Assess Scheduler Priority Admission

- Mapped generic durable message admission, explicit submission, and generated-input
  submission against exact queue and manifest replay.
- Selected `DurableSubmissionManifest`, not `WorkItem` or `WorkPayload`, as the owner
  of caller-requested `NORMAL`/`EXPEDITED` Scheduler priority.
- Required manifest schema v2 and an explicit stopped-submission schema-v1 migration
  assigning `NORMAL` before either submission command exposes optional priority input.
- Kept current production admission, manifest schema, CLI behavior, Tool authority,
  commit, push, merge, release, and deployment unchanged.

## 2026-07-27 - Connect Durable Priority-Aware Queue Claims

- Connected the pure `SchedulerPrioritySelector` to the non-recovery
  `SingleWorkerSchedulerQueue.claimNext` path over the complete admission-ordered set
  of dependency-ready candidates.
- Persisted the selected active WorkItem and next consecutive-expedited progress in one
  durable transition, with persistence failure leaving both unchanged.
- Preserved one-shot recovery claim precedence without double-counting fairness,
  same-priority admission order, dependency readiness, `WorkItem` authority, and the
  existing schema-v3 format.
- Kept priority admission input, aging, additional priority classes, commit, push,
  merge, release, and deployment unchanged.

## 2026-07-27 - Add Priority-Aware Queue Schema V3 And Migration

- Advanced durable Scheduler queues to schema v3 with exact queued priority, bounded
  expedited-burst configuration and progress, and a one-shot recovery-preferred
  WorkItem identity.
- Preserved existing `NORMAL` construction and FIFO ordinary claims while ensuring an
  interrupted active WorkItem is durably requeued and reclaimed before FIFO without
  advancing fairness twice.
- Added candidate-first, source-drift-refusing schema-v2-to-v3 migration plus the
  explicit stopped-Scheduler `scheduler-migrate-queue` command.
- Kept `WorkItem`, priority admission input, non-recovery priority selection, Scheduler
  authority, commit, push, merge, release, and deployment unchanged.

## 2026-07-27 - Assess Priority-Aware Queue Schema V3 Migration

- Mapped queue schema v2 losslessly to a proposed v3 with default `NORMAL` priority,
  maximum expedited burst `4`, zero fairness progress, and unchanged queue history and
  status partition.
- Added a required one-shot recovery reservation to the design so restart reclaims the
  previously active WorkItem before priority selection and does not count the same
  durable claim twice.
- Selected the v3 state/filesystem codec and explicit candidate-first stopped-Scheduler
  migration as the next bounded prerequisite. No production code, schema, CLI,
  scheduling behavior, authority, or maturity changed in this assessment.

## 2026-07-27 - Add Pure Scheduler Priority And Fairness Selection

- Added bounded `NORMAL`/`EXPEDITED` Scheduler priority and a pure selector over
  admission-ordered ready candidates.
- Added oldest-ready class ordering, a bounded expedited burst, forced normal
  selection after burst exhaustion, reset/capped fairness progress, and fail-closed
  input validation.
- Kept `WorkItem`, `QueuedWork`, queue persistence, CLI, recovery, and authority
  unchanged; durable `claimNext` integration remains a separate schema/migration task.

## 2026-07-27 - Assess The First Priority And Fairness Connection

- Mapped priority placement against immutable work authority, `QueuedWork`, current
  FIFO readiness, exact durable admission history, and queue schema-v2 recovery.
- Selected a pure deterministic `NORMAL`/`EXPEDITED` selector with admission-order
  tie-breaking and a bounded expedited burst as the smallest first contract.
- Deferred queue integration until a separate schema/migration task can persist queued
  priority and fairness progress; changed no production behavior, schema, CLI,
  maturity, authority, or external state.

## 2026-07-27 - Deliver Deterministic RunRecord Recovery

- Committed the deterministic Goal/AgentRun-bound RunRecord recovery implementation,
  recovery fixtures, accepted decision, and owning documents as `81cffd9`.
- Pushed the dedicated feature branch, fast-forwarded local `main`, and pushed
  `origin/main` without force; fresh remote references matched the implementation
  commit.
- Used no rebase, history rewrite, merge commit, force operation, tag, release,
  deployment, pull-request mutation, or branch deletion.

## 2026-07-27 - Verify Recovery After Disposition Before Checkpoint Clear

- Added a worker fixture that forces checkpoint clearing to fail after a verified
  terminal queue disposition has persisted.
- Proved a fresh worker reports the existing disposition and clears the retained exact
  intent without another execution, RunRecord, effect outcome, runtime revision, or
  queue revision.
- The recovery fixture passed against the existing production path; no runtime,
  persistence schema, CLI, authority, or maturity change was required.

## 2026-07-27 - Reassess Lost Acknowledgements After Lease Recovery

- Compared the remaining result, terminal-disposition, checkpoint-clear,
  Evidence-before-RunRecord, and unresolved-effect interruption prefixes.
- Selected a worker-level disposition-persisted/checkpoint-clear-failed fixture as the
  smallest remaining Gate 8-owned evidence connection.
- Kept orphan Evidence in retention scope and unresolved `PREPARED` effects behind
  explicit adapter recovery; changed no production behavior, schema, CLI, maturity,
  authority, or external state.

## 2026-07-27 - Verify Worker Recovery After Lease Expiry

- Added a worker-level fixture that expires the execution lease after the RunRecord
  reference is checkpointed.
- Proved fresh-worker reclamation and greater-fence convergence with one execution,
  one RunRecord, no effect outcome, one verified queue disposition, and checkpoint
  cleanup.
- The test was GREEN against the existing production path; no runtime, schema, CLI,
  authority, or maturity change was required.

## 2026-07-27 - Assess Remaining Gate 8 Lost-Acknowledgement Prefixes

- Mapped interruption prefixes from cycle intent through execution, RunRecord
  publication, lease reclamation, finalization, queue disposition, and checkpoint
  clearing against current source and named tests.
- Confirmed that the remaining smallest Gate 8-owned evidence gap is a worker-level
  lease-expiry recovery fixture after RunRecord-reference checkpointing.
- Kept pre-RunRecord orphan Evidence as retention work and terminal-disposition/
  checkpoint-clear replay as an already idempotent path; changed no production
  behavior, schema, CLI, maturity, authority, or external state.

## 2026-07-27 - Recover Lost Process-Isolated RunRecord Acknowledgements

- Added deterministic Goal/AgentRun-bound RunRecord identities with atomic exact-replay
  point persistence and changed-content refusal.
- Recovered the child-persisted/result-not-published prefix by validating the
  point-resolved record and skipping duplicate child execution.
- Added a real child-JVM restart fixture proving one RunRecord and no store scan or
  second sidecar.

## 2026-07-27 - Assess Orphaned RunRecord Lost Acknowledgement

- Traced the process-isolated recovery order across child RunRecord persistence,
  result-spool publication, and parent checkpoint reference persistence.
- Confirmed that recovery is deterministic after result publication or reference
  checkpointing, but the current random RunRecord identity leaves the earlier
  child-persisted/result-not-published prefix uncorrelated and re-executable.
- Selected an AgentRun-derived deterministic RunRecord identity with fail-closed point
  recovery and result reconstruction/republication as the smallest follow-up
  connection; rejected both store scanning and a second post-persistence sidecar.
- Changed no production behavior, durable schema, CLI, capability maturity, authority,
  or external state.

## 2026-07-24 - Verify Migrated Scheduler Cycle Recovery

- Added a named real-filesystem integration fixture for the supported
  pending-finalization schema migration followed by the real `scheduler-cycle` command.
- Proved that a migrated post-RunRecord-reference prefix reaches exactly one verified
  queue disposition without creating an invocation spool, another RunRecord, or another
  external-effect outcome.
- Proved that the empty effect ledger remains byte-identical and the converted cycle
  checkpoint clears after finalization.
- Changed no production behavior, durable schema, migration command, or whole-Gate 8
  maturity.

## 2026-07-24 - Assess The First Gate 8 Migration Connection

- Mapped the supported pending-finalization migration command, store contract, and
  named tests to the Gate 8 migration exit-criterion slice.
- Confirmed that exact conversion, failure preservation, idempotence, source-drift
  refusal, and current-schema checkpoint resolution are Integrated.
- Kept the whole criterion partial because no fixture yet resumes a migrated
  post-RunRecord-reference checkpoint through the real Scheduler cycle and proves that
  process execution, RunRecord publication, and effects remain singular.
- Selected that bounded migration-to-cycle recovery fixture as the smallest remaining
  Gate 8-owned dependency without changing runtime behavior or whole-gate maturity.

## 2026-07-24 - Add Supported Pending-Finalization State Migration

- Added lossless schema-v1-to-v2 pending-finalization migration with typed absent,
  already-current, and migrated outcomes while normal recovery remains fail-closed.
- Added candidate write/read validation, source-byte drift refusal, atomic replacement,
  and cleanup that preserves the original artifact on every pre-publication failure.
- Added the separate stopped-Scheduler `scheduler-migrate-cycle-checkpoint` command with
  bounded output and documented recovery.
- Added exact conversion, non-writing idempotence, corruption/future-version,
  source-drift, candidate-failure, original-byte, CLI, and normal-recovery coverage.

## 2026-07-24 - Define First Gate 8 State Migration Boundary

- Selected the pending-finalization checkpoint as the first supported migration target
  because its schema-v1 meaning embeds losslessly in schema v2.
- Fixed the old-to-current mapping, explicit stopped-Scheduler maintenance boundary,
  candidate-first atomic publication, source-drift refusal, and original-byte
  preservation contract.
- Kept ordinary recovery fail-closed and deferred queue, runtime, and external-effect
  migration until their missing history or evidence has a separately accepted policy.
- Changed no production code, artifact schema, runtime behavior, authority, external
  state, commit, push, release, or deployment.

## 2026-07-24 - Assess Whole-Gate 8 Maturity

- Audited every Gate 8 scope item and exit criterion against named current production
  connections and fresh evidence.
- Retained `Specified - Next`; Operational submission/cycle sub-paths do not satisfy
  missing authenticated controls, migration, priority/fairness, role-based workers,
  broader lost-acknowledgement, or production-adapter requirements.
- Selected a bounded supported state-version migration boundary as the next Gate 8-owned
  dependency without changing runtime behavior or schema.

## 2026-07-24 - Add Read-Only Scheduler Invocation Recovery Status

- Added the checkpoint-correlated invocation-spool projection and bounded stable reader.
- Added `scheduler-invocation-status` with explicit Scheduler and invocation roots plus
  bounded metadata output.
- Added pure phase, corrupt/several-message, drift, result-to-RunRecord, non-creation,
  immutability, and output-bound coverage without process launch or spool mutation.

## 2026-07-23 - Add Read-Only Scheduler External-Effect Recovery Status

- Added runtime-owned `SchedulerExternalEffectRecoveryStatus` with conservative phases
  for no correlated Goal, pre-ledger prefixes, empty history, ambiguous preparation,
  explicit user recovery, non-compensated effects, and all-compensated effects.
- Added `SchedulerExternalEffectRecoveryStatusReader`, which reuses the existing
  checkpoint-correlated Scheduler projection, resolves only that Goal's ledger,
  validates exact WorkItem and retained AgentRun bindings, verifies every terminal
  Evidence Store digest, and refuses observed Scheduler/runtime/ledger drift.
- Added the separate bounded `scheduler-external-effect-status` command with explicit
  Scheduler, effect, and Evidence roots plus a 1-through-8 ledger prefix. Complete counts
  and identities are reported without evidence content or external-system claims.
- Added phase-precedence, binding, drift, evidence-integrity, argument, and
  real-filesystem CLI coverage for non-creation, immutable artifacts, corruption, and
  bounded output.
- Added no adapter invocation, replay, compensation, retry decision, recovery, scan,
  persistence/schema mutation, external-system probe, commit, push, release, or
  deployment.

## 2026-07-23 - Add Read-Only Scheduler Recovery Status

- Added runtime-owned `SchedulerRecoveryStatus` with nine checkpoint-anchored durable
  phases spanning intent, AgentRuntime, RunRecord, result, retry, replacement, queue
  disposition, and checkpoint clearing.
- Added `SchedulerRecoveryStatusReader`, which directly reads the queue, optional
  checkpoint-named Goal, and optional checkpointed RunRecord, validates exact
  cross-store bindings, and refuses observed queue/checkpoint/runtime drift after a
  bounded second sample.
- Added the separate `scheduler-recovery-status` command with explicit queue, runtime,
  cycle-checkpoint, and RunRecord roots. Bounded output reports correlated identities and
  states plus `workerLiveness=UNKNOWN`.
- Added focused phase/drift contracts and real-filesystem CLI coverage for no-checkpoint,
  intent, runtime, checkpointed RunRecord, immutable artifacts, missing-root
  non-creation, corruption, and bounded output.
- Added no recovery, store scan, state/schema mutation, process or lease-liveness claim,
  repair, retry, waiting, polling, commit, push, release, or deployment.

## 2026-07-23 - Add Read-Only Scheduler Queue Status

- Added runtime-owned `SchedulerQueueStatus`, preserving admission order and classifying
  every persisted work item as ready, blocked, active, verified, or failed from the
  existing queue dependency and disposition contracts.
- Added `scheduler-status` with explicit queue root, canonical queue identity, and 1-48
  output limit. It reports complete state counts plus a bounded admission-ordered prefix.
- Kept inspection strictly read-only by resolving the queue snapshot directly instead of
  invoking recovery. Real-filesystem tests prove unchanged artifact bytes, timestamp,
  revision, and active slot, plus empty, missing, corrupt, and maximum-output behavior.
- Added no queue/runtime schema change, recovery, worker-liveness claim, cross-store
  interpretation, execution, submission, waiting, polling, commit, push, release, or
  deployment.

## 2026-07-23 - Add Bounded Recent RunRecord Discovery

- Added the separate read-only `run-record-list` command with explicit RunRecord root and
  1-48 reference limit. It reports available/empty status and the exact newest-first
  opaque references supplied by the existing store.
- Kept discovery separate from inspection: listing resolves no record and creates no
  missing root, while the existing `replay` command remains responsible for integrity and
  lifecycle validation.
- Added test-first argument coverage and a real-filesystem CLI integration proving a
  bounded recent prefix over real persisted records, replay of a discovered reference,
  empty-root non-creation, and maximum-size bounded output without artifact resolution.
- Added no RunRecord schema or persistence change, record contents in listing output,
  queue/runtime/checkpoint policy, write authority, cleanup, commit, push, release, or
  deployment.

## 2026-07-23 - Implement Bounded Foreground Scheduler Drain

- Added `ForegroundSchedulerDrain` with typed idle, failed, and limit stop reasons plus
  exact invoked/verified/failed cycle counts. The 1-4096 bound is checked before work,
  verified completion is the only continuation condition, and no extra cycle runs after
  a stop or limit.
- Added the separate `scheduler-drain` CLI command by sharing the existing
  process-isolated `scheduler-cycle` composition inputs and adding `--max-cycles`.
  Bounded output distinguishes `IDLE`, `FAILED`, and `LIMIT_REACHED`; terminal work
  failure retains exit `40`, while idle and limit stops exit `0`.
- Added focused drain contracts and real-filesystem child-process integrations for
  multiple ready and dependency-linked items, per-cycle checkpoint recovery, an exact
  limit with pending work retained, terminal failure, and missing-queue refusal. Updated
  operator recovery documentation and corrected the README's stale local-IPC description.
- Added no submission/execution wrapper, queue creation or admission, sleep, waiting,
  polling/service lifecycle, control application, progress store, production external
  adapter, commit, push, release, or deployment.

## 2026-07-23 - Serialize Filesystem Scheduler Queue Updates

- Added one stable queue-scoped lock artifact and a non-blocking operating-system file lock
  around each `FileSystemSchedulerQueueStore.update` read-validate-publish transaction.
- Added typed `ConcurrentSchedulerQueueUpdateException` refusal so a competing local JVM or
  overlapping store instance cannot wait indefinitely or overwrite a committed revision.
- Added a real child-JVM contention fixture proving refusal leaves queue revision/content
  unchanged, plus stale-store regression coverage proving a committed update remains
  authoritative.
- Preserved queue schema, snapshot contents, atomic publication, creation, resolution,
  exact replay, and persist-before-exposure semantics. Added no distributed lock,
  cross-store transaction, waiting, polling, drain command, commit, push, or deployment.

## 2026-07-23 - Select A Bounded Foreground Scheduler Drain

- Reassessed Gate 8 after the evidence-bound external-effect executor and kept production
  adapters, authenticated control application, and multi-agent handoff assigned to their
  owning later gates.
- Compared a bounded foreground drain with background polling/service operation, schema
  migration, and exact-history compaction. Selected a finite drain over the existing
  recoverable one-cycle Worker because it has an immediate multi-item queue consumer and
  requires no waiting, service lifecycle, migration consumer, or retention policy.
- Recorded a separate `scheduler-drain` boundary that will process only already-ready work,
  continue only after verified completion, and stop on idle, failure, or an explicit
  at-most-4096 cycle limit while keeping submission and `scheduler-cycle` unchanged.
- Added no production or test behavior, CLI command, persistence schema, polling, external
  invocation, maturity promotion, commit, push, release, or deployment.

## 2026-07-23 - Implement Evidence-Bound External-Effect Execution

- Added the bounded `ExternalEffectAdapter` port and `DurableExternalEffectExecutor`, which
  validate adapter identity and semantic digest, persist `PREPARED` before one invocation,
  persist redacted complete Evidence Store content, and publish an evidence-bound terminal
  outcome only through the existing current-owner/fence check.
- Revised the external-effect ledger to schema v2: requests retain stable adapter identity,
  prepared records carry no outcome evidence, terminal records require one immutable
  evidence reference and SHA-256, semantic retry digests cover the new fields, and schema-v1
  filesystem artifacts fail explicitly.
- Added test-first contract and real-filesystem integration coverage for prepared visibility,
  exact restart replay without reinvocation or revision, identity/digest mismatch before
  mutation, immutable terminal evidence, schema-v1 rejection, and adapter, evidence,
  terminal-store, and lease-expiry failure prefixes.
- Added no production external adapter, network/Git/cloud call, credential or payload
  persistence, Tool authority, automatic prepared recovery, second AgentRun, polling,
  commit, push, release, or deployment.

## 2026-07-23 - Select An Evidence-Bound External-Effect Adapter Execution Boundary

- Assessed direct ledger invocation, an application executor with a transient adapter
  result, and an application executor with a durable evidence-bound terminal outcome
  against authority, idempotency, restart replay, and interruption recovery.
- Selected a separate application executor that verifies stable adapter and semantic
  operation identity, persists `PREPARED` before one adapter invocation, persists redacted
  complete outcome evidence before terminal publication, and binds the terminal status to
  its evidence reference and digest through an explicit ledger schema revision.
- Kept unresolved prepared work fail-closed: an already prepared record never authorizes
  automatic execution, terminal replay resolves evidence without another invocation, and
  adapter/evidence/write/lease failure leaves the effect prepared. No implementation,
  production external adapter, new Tool authority, external call, or exactly-once claim
  was added.

## 2026-07-22 - Promote Generated-Input Scheduler Submission To An Operational Sub-Path

- Ran an actual Enhancer-repository smoke run of `scheduler-submit-generated` followed by a
  separate `scheduler-cycle` over one shared queue root and the derived queue identity,
  observing `ADMITTED` (queue revision 1), `VERIFIED_COMPLETED` (one RunRecord, one completed
  WorkItem), `REPLAYED` (identical occurrence time and Workspace snapshot, unchanged queue
  revision), and `IDLE`, with exactly one retained submission manifest and one RunRecord and
  no duplicate execution.
- Documented the `scheduler-submit-generated` command and its generated-input recovery
  actions (submission interruption before and after manifest persistence, exact replay,
  conflict fail-closed, verified completion, and idle re-entry) in the README alongside the
  existing explicit two-command workflow.
- Recorded the Operational sub-path promotion in the roadmap and state documents. No polling
  loop, wrapper command, automatic execution, production behavior, schema change, commit-time
  runtime artifact, release, or deployment was added, and the explicit `scheduler-submit`
  command is unchanged.

## 2026-07-22 - Implement Replay-Safe Generated-Input Scheduler Submission

- Added `GeneratedInputSubmissionService`, a replay-safe application boundary that takes one
  caller-retained canonical submission UUID and derives the queue, correlation, and
  logical-run identities through fixed versioned domain-separated UUID transforms
  (`GeneratedSubmissionIdentities`), so the same key always names the same generated work.
- Resolved the existing `DurableSubmissionManifest` before consulting the clock or the
  repository snapshot: an absent manifest captures the occurrence time on first use and
  persists through the existing `DurableWorkSubmissionService`, while a present manifest
  reuses its exact occurrence time and envelope and fails closed on any caller-owned
  intent conflict (task, capacity, capability, producer, target, digest). Introduced a
  typed `MissingSubmissionManifestException` so absence is distinguished from corruption.
- Exposed the boundary as the separate `scheduler-submit-generated` CLI command, which
  generates identities and occurrence time and prints the derived queue/correlation/
  logical-run identities and snapshot for auditing; the explicit `scheduler-submit`
  command is unchanged. Submission remains separate from `scheduler-cycle`, execution,
  and polling, and no second durable store was added.
- Verified test-first with focused boundary tests, CLI argument tests, and a named
  real-filesystem CLI integration proving first-use generation, fresh-instance exact
  replay without manifest or queue-revision change, conflict fail-closed, and first-use
  task-mismatch refusal, plus the full strict-lint Gradle build.

## 2026-07-22 - Select Single-Manifest Recovery For Generated Submission Inputs

- Assessed the interruption window created by generating Scheduler identities and
  occurrence time before durable submission intent exists.
- Rejected a second invocation manifest because it would duplicate the exact envelope and
  occurrence time already owned by `DurableSubmissionManifest`; selected one stable
  caller-retained submission UUID, versioned derived identities, and resolve-before-clock
  replay over the existing manifest.
- Recorded the accepted architecture and next bounded implementation without changing
  production behavior, the explicit `scheduler-submit` command, cycle execution, polling,
  commit, push, merge, release, or deployment.

## 2026-07-22 - Prove The Explicit Two-Command Scheduler Operator Workflow

- Added a named real-filesystem CLI integration that invokes `scheduler-submit` and
  `scheduler-cycle` through separate fresh CLI instances over one shared queue, proving
  pending-before-cycle behavior, real child-process verified completion, retained
  manifest/runtime/effect/RunRecord evidence, exact replay, idle re-entry, and no
  duplicate execution.
- Documented the supported two-command sequence, shared versus command-specific roots,
  independent statuses and exit codes, and recovery actions for submission interruption,
  cycle interruption, verified completion, terminal failure, and idle state.
- Confirmed the sequence against the actual Enhancer repository as
  `ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE` with one RunRecord. No production
  code, wrapper command, generated identity/time, polling, commit, push, merge, release,
  or deployment was added.

## 2026-07-22 - Expose Durable Submission As An Explicit CLI Command

- Added `scheduler-submit` with explicit project/submission/queue roots, task and message
  identities, occurrence time, queue bound, capability, target, and digest inputs; it
  derives the approved task revision, allowed Tools, and repository-memory snapshot from
  the governed project without generating identity or time.
- Connected the command to the persist-first durable submission service and reported
  bounded `ADMITTED` or exact-replay `REPLAYED` status with stable manifest/queue prefix
  outcomes.
- Proved real-filesystem first admission, fresh-instance replay without manifest bytes or
  queue revision changes, changed-content and task-mismatch refusal without queue
  mutation, and pending work without execution. Submission remains separate from
  `scheduler-cycle`; no worker, Tool, polling, external adapter, commit, push, merge,
  release, or deployment was added.

## 2026-07-22 - Persist Durable Submission Intent Before Queue Admission

- Added an immutable submission manifest binding one message identity to the target queue
  identity/capacity, required capability, and exact work envelope, with bounded
  integrity-checked atomic filesystem persistence and no-rewrite exact replay.
- Added a restart-safe application boundary that persists intent first, creates only an
  absent queue or verifies existing capacity before recovery, and admits through the
  existing exact durable work handler without a mutable receipt.
- Proved real-filesystem recovery after manifest persistence and empty queue creation,
  exact full replay without a queue revision, changed-content refusal, and capacity-drift
  refusal before recovery mutation. No submission CLI, execution, polling, external
  adapter, Gate 9, commit, push, merge, release, or deployment was added.

## 2026-07-22 - Retain Exact Durable Work Admission History

- Advanced Scheduler queue persistence to schema v2 with every exact `QueuedWork`
  retained in immutable admission order through verified or failed terminal disposition;
  filesystem updates reject history rewrite or truncation.
- Added persist-first idempotent durable admission: exact replay is a no-revision success,
  while changed capability, envelope, authorization, provenance, execution input, or
  dependencies under the same WorkItem identity fail closed; strict Scheduler `enqueue`
  remains unchanged.
- Connected the production work-message handler to that boundary and proved a real
  filesystem/process-isolated cycle can restart both queue and bus, replay the exact
  envelope, and preserve terminal disposition, queue revision, and RunRecord count.
- Explicitly rejected schema-v1 queue artifacts without adding migration, submission,
  queue creation, polling, compaction, external-effect execution, or Gate 8 promotion.

## 2026-07-22 - Expose One Recoverable Durable Scheduler Cycle

- Added the recovery-only `scheduler-cycle` CLI over one explicitly identified existing
  durable queue, with explicit filesystem roots, owner identity, retry bound, lease, and
  child-process timeout rather than inferred storage or implicit queue creation.
- Composed the real process-isolated durable Worker and reported bounded `IDLE`,
  `VERIFIED_COMPLETED`, or terminal `FAILED` status, including a stable Scheduler failure
  exit code distinct from usage/configuration and internal errors.
- Added a named integration that admits work through the production durable handler,
  resumes a persisted Worker prefix, runs the real child JVM, resolves the RunRecord,
  observes queue disposition, and verifies checkpoint cleanup; no submission manifest,
  polling service, external adapter, or Gate 8 Operational promotion was added.

## 2026-07-22 - Connect Work Messages To The Durable Scheduler Queue

- Added a separate persist-first durable work-admission handler that maps each canonical
  message UUID to one stable distinct WorkItem identity and enqueues dependency-free work
  without adding task, Tool, or dependency authority.
- Connected real repository-approved work and a Workspace snapshot through publication,
  the in-process queue, and filesystem-backed Scheduler persistence, then recovered and
  claimed the exact WorkItem from a fresh queue instance.
- Preserved bus retry/dead-letter visibility for storage failure, same-bus duplicate
  suppression, and fail-closed fresh-bus re-delivery without claiming cross-restart
  idempotent success.

## 2026-07-22 - Integrate Retry-Aware AgentRun Worker Recovery

- Extended the cycle-intent checkpoint with a schema-v2 replacement AgentRun phase and
  persisted that identity before append, with strict phase/identity validation and
  fail-closed filesystem recovery.
- Wired the Worker and process-isolated composition to an explicit retry policy and exact
  Goal ledger, continuing admitted attempts through the fenced execution path or applying
  one terminal failed queue disposition after refusal.
- Added recovery coverage at five retry prefixes plus unresolved-effect refusal, and a
  real-filesystem first-failure/second-success integration; the initial full regression
  passed 81 suites and 440 tests with 3 existing Windows privilege skips.

## 2026-07-22 - Add Durable AgentRun Retry Controller

- Added the persist-first `DurableAgentRunRetryController` over one retry-pending runtime
  and its exact existing Goal ledger, recording typed decisions with immutable attempt,
  policy, ledger revision/count, and versioned semantic digest evidence before action.
- Added idempotent admitted-retry append using only a caller-checkpointed replacement
  identity and idempotent refused-Goal abandonment, without queue mutation, replacement
  execution, lease acquisition, Tool/adapter authority, or invented empty ledgers.
- Fresh focused verification passed 13 tests, and the initial full strict-lint regression
  passed 81 suites and 431 tests with 3 existing Windows privilege skips.

## 2026-07-22 - Add Schema-V2 AgentRun History And Safe Retry-Pending Parking

- Replaced the single-attempt runtime artifact with schema-v2 immutable AgentRun and
  retry-decision histories, a latest-attempt projection, Goal-wide fence monotonicity,
  exact-prefix filesystem enforcement, and explicit schema-v1 rejection.
- Split RunRecord-backed attempt-result recording from terminal queue disposition. A
  failed attempt now parks the Goal at `RETRY_PENDING`; the worker retains its durable
  intent/reference and active WorkItem without failing the queue or executing again.
- Fresh focused verification passed 36 tests, and the final strict-lint Gradle build
  passed 80 suites and 418 tests with 3 existing Windows privilege skips.

## 2026-07-22 - Correct Attempt-Level AgentRun Retry Decision

- Replaced terminal Scheduler `WorkItemDisposition` input with the exact latest
  `RuntimeAgentRun` and bound the supplied effect ledger to its Goal and WorkItem.
- Added fail-closed `NON_COMPENSATED_EXTERNAL_EFFECT` refusal for `APPLIED` and
  `DEDUPLICATED`; automatic retry now admits only empty or all-`COMPENSATED` history
  with remaining attempt budget.
- Expanded focused coverage to 22 tests across every attempt/effect status, mixed
  precedence, identity binding, null inputs, and attempt bounds; the fresh full build
  passed 79 suites and 409 tests with 3 existing Windows privilege skips.

## 2026-07-22 - Correct Gate 8 Multi-Attempt Retry Specifications

- Separated retryable AgentRun attempt failure from terminal Scheduler WorkItem
  disposition so an admitted retry keeps the queue item active.
- Rewrote the retry decision and durable multi-attempt specifications around an exact
  failed-attempt input, empty/all-compensated external-effect safety, schema-v2 immutable
  attempt and decision prefixes, split result/final-disposition operations, and an
  interruption-recoverable controller/worker order.
- Recorded the correction as an accepted design without changing production Java or
  promoting current capability maturity.

## 2026-07-21 - Persist Fence-Checked External Effect Outcomes

- Added one bounded schema-v1 external-effect ledger per Goal, with stable idempotency
  keys and semantic operation digests bound to the exact Goal, AgentRun, and WorkItem.
- Persisted `PREPARED` before exposure and exactly one current-owner/fence-checked
  `APPLIED`, `DEDUPLICATED`, `COMPENSATED`, or `REQUIRES_USER_RECOVERY` result. Exact
  replay is revision-free; key reuse, stale ownership, and terminal rewrite fail closed.
- Added a bounded strict-UTF-8 integrity-checked atomic filesystem store that preserves
  append/terminal history across restart and leaves unresolved preparation explicit
  rather than automatically replaying an effect.
- Added focused and real-filesystem coverage for all outcomes, bounds, stale/expired
  fences, exact replay, restart, Unicode, persistence failure, corruption, unsupported
  state, capacity, and history-rewrite refusal.
- Fresh `clean build` passed 76 suites and 379 tests with 3 existing privilege-dependent
  Windows symbolic-link setup skips, 0 failures, and 0 errors under build-enforced Java
  17 `-Xlint:all -Werror`.

## 2026-07-21 - Persist Development Session Checkpoints

- Added one machine-written development-session checkpoint below the Git-ignored
  `.enhancer/session-checkpoint/` runtime boundary. It binds a generated run identity to
  the active task contract without copying canonical task, maturity, decision, or
  verification facts.
- Added monotonic expected-revision fencing, pending/succeeded/failed/stable execution
  positions, last-successful and next-action recovery, bounded evidence references, and
  present/missing content identities for up to 256 project-relative artifacts.
- Added bounded strict-UTF-8 integrity envelopes with atomic replacement, exact task and
  writer conflict checks, real-path storage containment, corruption rejection, and
  stable-plus-artifact-match requirements before retirement.
- Added `checkpoint-start`, `checkpoint-record`, `checkpoint-show`, and
  `checkpoint-clear` CLI operations and integrated them into session start,
  implementation, resume, close, and Agent operating rules.
- Verified forced-stop recovery in a fresh manager/process, actual-repository checkpoint
  inspection, task/artifact drift rejection, Windows junction containment, and the full
  strict-lint regression.

## 2026-07-21 - Persist Bound Runtime Control Requests

- Added a bounded ordered control-request ledger to durable Goal state. Exact `ControlPayload` envelopes must bind to the retained work logical run, correlation, and causation; runtime-identity collisions, changed-content identity reuse, terminal admission, and capacity overflow fail closed.
- Made exact restart replay idempotent without advancing the runtime revision, retained the ledger across later lifecycle transitions, and enforced prefix-monotonic filesystem updates with persist-before-exposure behavior. Schema v1 was revised in place, and older payloads without the ledger fail closed.
- Added `RuntimeControlAdmissionHandler`, connecting a real Gate 7 queue to Gate 8 durable request state. Storage I/O participates in the bus's existing retry/dead-letter path; producer and reason remain diagnostic and no request changes Goal, AgentRun, lease, fence, queue, worker, Tool, or bus-cancellation state.
- Added a control-admission recovery path that deliberately does not reclaim expired leases, keeping request handling observational with respect to runtime ownership while normal runtime recovery retains expiry reclamation.
- Added focused lifecycle, corruption, restart/replay, supplementary-Unicode, storage-failure, retry, dead-letter, and expired-lease non-interference coverage.

## 2026-07-21 - Select The Process-Isolated Durable Worker

- Added `DurableAgentRunWorker.processIsolated`, the production composition selecting `ProcessIsolatedAgentRunExecution` with the real bounded self-JVM launcher, caller-supplied durable stores, and one queue instance shared by dispatch and finalization.
- Added an idempotent post-checkpoint cleanup operation to `AgentRunExecution`. The durable worker calls it only after the RunRecord reference is present in its cycle-intent checkpoint, so cleanup failure is retried after restart without executing the work again.
- Process-isolated cleanup removes only the exact Goal/AgentRun work/result spool and an empty Goal parent. It never removes the invocation root, Evidence, or RunRecords; failed or incomplete current cycles retain their spool.
- Added a real child-JVM durable-worker integration and focused recovery coverage. Full `clean build` passed 71 suites and 351 tests with 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under build-enforced `-Xlint:all -Werror`.

## 2026-07-20 - Enforce Strict Lint In The Build

- Applied `-Xlint:all -Werror` through `tasks.withType(JavaCompile).configureEach` in `build.gradle`. Every increment since Gate 1 recorded that strict lint passed, but the flags lived only in a manual javac invocation the build never ran, so a lint regression could not fail `./gradlew build`.
- Extended enforcement to test sources as well as production. They already compiled clean under the same flags, and a warning is as easy to introduce there.
- Proved the guard fires rather than assuming it: an injected raw-type declaration failed `:compileJava`, the same injection in a test failed `:compileTestJava`, and both were reverted. A no-op configuration would have passed a clean-tree check alone.
- Behaviour-preserving: 71 suites, 348 tests, 346 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors, unchanged from before the change. No production or test source was modified, and nothing required a fix under the newly enforced flags.

## 2026-07-20 - Harden Process-Isolated Execution Boundaries

- Re-entry now decodes the sole existing work message and requires both the exact `work` queue destination and complete dispatched envelope before reuse. Foreign work and multiple work or result entries fail before the launcher can run.
- Result validation now requires the exact `isolated-worker-result` queue destination and resolves the claimed reference before returning it. The resolved RunRecord must bind to the dispatched task, source document, read-file target, verification-bearing expected digest, and claimed status.
- Reused `DurableAgentRunFinalizer`'s task/source binding before the execution port returns a reference, with separate diagnostics for task and source mismatches.
- Reduced `AgentLoopAgentRunExecution.executeWork` to package-private so the isolated child can share the pipeline without exposing a public lease-free execution surface.
- Added adversarial coverage for foreign work and routes, multiple results, RunRecords for a different task/source/target/digest, and launcher non-invocation on corrupt re-entry state.
- Repaired the canonical documentation after 3d: `PROJECT_STATE.md`, `ARCHITECTURE.md`, `.ai/architecture.md`, `ROADMAP.md`, and `SESSION_HANDOFF.md` now agree that 3b/3c/3d exist while durable-worker selection and spool retention remain.
- Extended `DocumentOwnershipTest` beyond the obsolete `## Next Task` spelling to catch `## Next` and declarative next-task/next-increment prose outside `CURRENT_TASK.md`.
- Regression: 71 suites, 348 tests, 346 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors; strict lint across 167 production sources.

## 2026-07-20 - Connect Isolated Execution End To End

- Added `ProcessIsolatedAgentRunExecution` (Gate 8 connection sub-increment 3d), the second production `AgentRunExecution`. It spools the work envelope under an invocation root private to the Goal and AgentRun, launches `IsolatedWorkerMain` with the project, evidence, and RunRecord roots as parent-supplied arguments, and returns the persisted RunRecord reference read back from a result spool. Connection 3's adapter and process lifecycle are now connected.
- Rewrote `IsolatedWorkerMain` to run the Gate 1-4 pipeline and publish a matching `ResultPayload` rather than only decoding a message. It reaches the pipeline through a new `AgentLoopAgentRunExecution.executeWork` seam, so the in-process and isolated paths run one implementation instead of two similar ones; a child holds no lease and cannot construct an `AgentRunDispatch`, and the lease and queue identity were never read by the pipeline.
- The child's result is treated as a claim, never authority. Before returning a reference the parent requires matching correlation, logical-run, causation, and task identities, a payload that is exactly a `ResultPayload`, a reference that resolves in the shared `RunRecordStore`, and a claimed verification status equal to the resolved record's own. A child cannot promote its own run, and `DurableAgentRunFinalizer` stays the final authority.
- Store roots reach the child only as launcher arguments, never as payload data, because a payload that crossed a process boundary is untrusted input and must not redirect where artifacts are written.
- Re-entry returns an already-published valid result without launching a second child. A child that persisted a RunRecord and died before publishing leaves an orphan and is re-executed, which is the documented at-least-once consequence the in-process worker already accepts.
- Extracted a `WorkerProcessLauncher` port so the parent's failure paths are provable without spawning a process, matching the port-and-adapter shape already used by `AgentRunExecution`, `SchedulerQueueStore`, and `MessageTransport`.
- Nothing wires the execution into `DurableAgentRunWorker`, and nothing cleans up an invocation root. External command authority is unchanged at exactly two production files.
- Regression: 71 suites, 339 tests, 337 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors; strict lint across 167 production sources.

## 2026-07-20 - Isolate The Worker In A Bounded Child Process

- Added `IsolatedWorkerLauncher` (Gate 8 connection sub-increment 3b), the process lifecycle half of connection 3. It runs one worker in a child process and returns a typed `IsolatedWorkerOutcome`: `COMPLETED` carries an exit code, while `TIMED_OUT` and `START_FAILED` carry a bounded reason and no exit code, so a destroyed or unstartable child can never present a code that reads as a clean exit.
- Bounded the new authority to the JVM already running. The executable is resolved from `java.home`, canonicalized, and required to be a regular file; the child runs the current classpath; and the entry point is a `Class<?>` rather than a command string, so no caller-supplied executable, command name, or shell reaches `ProcessBuilder`. Unlike the Git adapter there is no lookup to poison, so the executable's location is deliberately not constrained — this project vendors its own JDK inside the project root.
- Bounded the child the way the Git adapter is bounded: output discarded by the operating system rather than read, an environment stripped of `JAVA_TOOL_OPTIONS`/`_JAVA_OPTIONS`/`JDK_JAVA_OPTIONS`, a capped timeout a caller cannot disable, and forcible destruction on overrun. Only the exit code and a bounded reason survive.
- Added `IsolatedWorkerMain`, the child entry point. It reads one message from a 3c spool and exits with a stable code for a decoded message, an empty spool, a corrupt message, or a usage error, so the boundary is proven by a real message crossing it rather than by a stub.
- External command authority now exists in exactly two places, each scoped by its own accepted decision. `GitWorkspaceCollector` remains the only one that runs a configured external program.
- Nothing wires the launcher into `AgentRunExecution` or `DurableAgentRunWorker`; running the Gate 1-4 pipeline inside the child is the named follow-on. No runtime behaviour changed.
- Regression: 70 suites, 334 tests, 332 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors; strict lint across 165 production sources.

## 2026-07-20 - Strengthen The Transport Codec Tests

- Grew `MessageEnvelopeCodecTest` from 4 cases to 11 after review against an independently written test for the same codec. No production source changed.
- Nanosecond-bearing occurrence time now runs through every round trip rather than one dedicated case, supplementary characters appear in the producer and control-reason fields as well as a target path, and a short garbage frame is rejected alongside an empty one.
- Added four cases for a peer on an incompatible format that neither test had: an unsupported codec version, an unknown destination kind, an unknown payload kind, and an unknown verification status. Added a maximum-tool-cardinality round trip and an over-cardinality rejection.
- Added `acceptsTheHandCraftedBaselineFrame`, which decodes the same hand-built frame with every field valid, so the rejection cases must differ from a decodable frame in exactly one field.
- That guard exists because mutation testing caught its absence: an earlier version of the incompatible-peer test passed against a codec whose codec-version check had been deleted, because its hand-built bodies were incomplete and failed on EOF first. After the correction, deleting the codec-version check, the envelope-version check, the nanosecond field, or the trailing-byte check each fails the suite.
- The decode-side `allowedTools` ceiling survives its mutation and is recorded as an equivalent mutant rather than a gap: `WorkPayload`'s constructor already rejects over-cardinality, so the codec check is redundant defense in depth.
- Regression: 69 suites, 327 tests, 325 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors.

## 2026-07-20 - Add The First Transport Adapter As A Local File Spool

- Added `FileSpoolMessageTransport`, the first concrete `MessageTransport` (Gate 8 connection sub-increment 3c). It writes one encoded route and envelope to its own file under a configured spool directory a peer process reads: durably spooled is `ACCEPTED`, capacity exhaustion against a `BackpressurePolicy` is `BACKPRESSURED`, an unusable root is `UNAVAILABLE`, and a refused message spools nothing.
- Each hop is published through a temporary file and an atomic move into a freshly generated name, so a reader never observes a partial message and resending an envelope never overwrites an earlier hop. The adapter promises no ordering across separately spooled messages, because the contract is per hop.
- Added `MessageEnvelopeCodec`, owning the wire format separately from publication so its cases are verifiable without a filesystem and a second adapter can reuse it. The frame is `[magic][bodyLength][sha-256 of body][body]` with length-prefixed strict UTF-8 strings and all four payload kinds.
- Occurrence time is carried as epoch-second plus nanosecond. An earlier draft used `toEpochMilli`, silently truncating an `Instant` and rewriting provenance the receiver is meant to trust; a nanosecond-bearing test proved the loss before the fix.
- The frame holds no wall-clock or random state, so one message always encodes to identical bytes and a peer may deduplicate on content. An earlier draft put spool time in the header, making two hops of one message differ.
- Added `CorruptedSpooledMessageException`, distinct from a plain `IOException`: a corrupt message stays corrupt and should be dead-lettered, while a filesystem condition may be transient and worth retrying.
- Selected the file spool over a Unix domain socket because it needs no capability the project does not already exercise and its tests are deterministic. Worker process isolation (3b) is deliberately excluded: it needs `ProcessBuilder`, and `.ai/workflow.md` step 6 forbids implementing work requiring new external authority inside a RED cycle.
- Nothing wires the adapter into production; no CLI, worker, or bus path constructs it, so no runtime behaviour changed.
- Regression: 69 suites, 320 tests, 318 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors; strict lint across 161 production sources.

## 2026-07-20 - Close The Documentation Audit Gaps

- Widened `DocumentOwnershipTest` to three explicit claim shapes — subject-first, verb-first with an `at`/`to`/`as` connector, and a parenthetical verdict beside a gate in a table row. The first version matched only subject-first, which is how `ARCHITECTURE.md`'s "retains Gate 7 at Contract Verified" and two table verdicts survived the increment that introduced the guard. Plain co-occurrence was tried and rejected: it flagged forward-looking conditions and the commentary explaining a removed claim.
- **Fixed a defect that made the guards silently not run.** `DocumentOwnershipTest` and `DecisionLogIndexTest` assert over the project's Markdown, which Gradle did not track as a task input, so `gradle test` after a documentation-only change reported the task up to date and executed neither. An injected violation produced a green build. The `test` task now declares the Markdown as an input.
- Removed the three surviving maturity claims from `ARCHITECTURE.md` and reworded the sentence in `docs/11-Architecture.md` that reproduced a claim while explaining its removal, rather than exempting it.
- Documented `SchedulerQueueStore` in `ARCHITECTURE.md` and `.ai/architecture.md` as the Gate 8 durability seam, stating that readiness, the active slot, and disposition stay in `DurableSingleWorkerSchedulerQueue` above it, so sub-increments 3b and 3c swap an implementation rather than the queue contract, and restating the same-instance revision rule as implementation-independent.
- Documented `TaskProposal`, `ProposalState`, and `PlanningException`, including why the state enum has exactly one constant: a proposal cannot represent itself as accepted.
- Added `GitWorkspaceCollector` coverage for every `resolveGitExecutable` rejection branch — candidates inside the observed project at any depth, relative and absent PATH entries, a directory named like the executable, an absent or blank PATH — plus case-insensitive PATH lookup and pinned `MAX_OUTPUT_BYTES` and `TIMEOUT_SECONDS`. `TIMEOUT_SECONDS` widened from private to package-private for the assertion; no behaviour changed.
- Regression: 67 suites, 310 tests, 308 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors.

## 2026-07-20 - Split The Decision Log Behind A Heading-Only Index

- Moved all 85 accepted decision bodies to `docs/decisions/<date>-<slug>.md`, each opening with its exact heading as a level-1 title, and reduced `DECISION_LOG.md` to an index carrying every `### <heading>` line, its `Status: Accepted Decision` line, and a link.
- No production source changed. `AcceptedDecisionProjector` reads only headings and status lines and never a decision body, so an index preserving those two things leaves the decision graph identical; `TaskJustificationProjector`, `RequiredProjectDocument`, and `ProjectContextReader` are untouched and `DECISION_LOG.md` stays at its required path.
- Startup context fell from 439,497 to 248,063 bytes (43%), with `DECISION_LOG.md` dropping from 211,121 bytes and 48% of that context to 19,687 bytes and 7%. Headroom against the 1 MiB `MAX_DOCUMENT_BYTES` ceiling rose from 337 to roughly 4,454 further decisions, retiring a dated boot failure.
- Added `DecisionLogIndexTest`: index and files must correspond one to one by exact heading, both sides must carry the acceptance status, no decision file may use the level-3 heading the index reserves for identity, paths must stay clear of the Windows `MAX_PATH` ceiling, and every `CURRENT_TASK.md` `## Justified By` bullet must resolve against the index.
- Exempted `docs/decisions` in `DocumentOwnershipTest` for the same reason `DECISION_LOG.md` was exempt: the files are append-only records of what was true at acceptance.
- Updated `AGENTS.md`, `prompts/SESSION_CLOSE.md`, `.ai/workflow.md`, `CONSTITUTION.md`, `.ai/memory.md`, and `README.md` so recording a decision means writing both the file and the index entry.
- Regression: 67 suites, 306 tests, 304 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors.

## 2026-07-20 - Enforce Document Ownership With A Structural Test

- Added `DocumentOwnershipTest` under `com.enhancer.architecture`, alongside the existing `RuntimePackageBoundaryTest`, asserting that gate maturity appears only in `PROJECT_STATE.md` and that `## Next Task` appears only in `CURRENT_TASK.md`. Exemptions cover the owner plus `ROADMAP.md` (which owns the `Status: Specified - Next` grammar the Planner parses) and the append-only records `DECISION_LOG.md`, `CHANGELOG.md`, `docs/verification-log.md`, and `docs/superpowers/**`.
- The test failed on the unmodified repository with six violations. All five documents naming Gate 7 claimed it was `Specified - Next` after Gate 8 had taken the marker — the same fact copied to five places had drifted in all five, one day after the ownership rule was accepted in prose. Replaced each with a reference to `PROJECT_STATE.md` rather than updating it.
- Stated the store write-root contract exactly in `ARCHITECTURE.md` and `README.md`: `--evidence-root` and `--run-record-root` are explicit caller inputs by the Gate 5 decision, deliberately not confined to the project root, with `.enhancer/` an example layout rather than an enforced property. Each store normalizes its root, refuses a symbolic-link root through `NOFOLLOW_LINKS`, and only creates freshly generated UUID-named entries, so it can add to a caller-named directory but cannot overwrite or delete what is already there. Read-side containment remains the separate and stricter boundary.
- Rejected the audit finding that proposed confining store roots to the project root: it contradicts the accepted Gate 5 explicit-input model and would break the existing `CliArgumentsTest` case that deliberately uses sibling roots. No store or CLI behaviour changed.
- Regression: 66 suites, 301 tests, 299 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors. The deltas against the previous 65/299 baseline are exactly the new structural test.

## 2026-07-20 - Complete The Document Ownership Cleanup

- Removed every capability-maturity verdict from `.ai/architecture.md`: 14 bullets carrying Contract Verified, Integrated, or Operational duplicated `PROJECT_STATE.md` and forced an edit on every maturity change. Each bullet now states what its contract is and what it connects to, with all architectural content preserved; the header states the file does not own maturity.
- Recorded in `.ai/memory.md` and `docs/05-Memory.md` that per-increment verification evidence is appended to `docs/verification-log.md`, written once and never revised, and deliberately excluded from the session-start reading order because it grows without bound.
- Left the required startup reading order unchanged: `RequiredProjectDocument` fixes that set of 15 paths, and the verification log is evidence rather than startup context.
- No production or test source changed; the full regression matches the baseline (65 suites, 299 tests, 0 failures, 0 errors).

## 2026-07-20 - Give Every Project Fact One Owning Document

- Established single-document ownership in `CONSTITUTION.md` Section 4: every fact has exactly one owning document, a document references rather than restates a fact it does not own, and a discovered duplicate is deleted rather than synchronized. Bound three rules explicitly because each had already produced a contradiction — the next task belongs to `CURRENT_TASK.md`, capability maturity to `PROJECT_STATE.md`, and delivery history to git and `CHANGELOG.md`.
- Added `docs/verification-log.md` as the append-only home for per-increment verification evidence and moved 58 historical verification and assessment sections there from `PROJECT_STATE.md`, preserving append order and content exactly; line accounting reconciles the original 861 lines as 657 moved, 185 retained, and 19 duplicated lines deleted at source.
- Reduced `PROJECT_STATE.md` from 861 to 186 lines (122 KB to 32 KB), keeping current state, the maturity judgment behind it, negative-space claims, and a new `Known Limitations` register, while preserving every hand-authored judgment no test, decision, or run record can reproduce.
- Reduced `SESSION_HANDOFF.md` from 414 to 40 lines (76 KB to 1.9 KB) after rescuing its durable content: the completion-conflict root-cause analysis and the Option A/B/C queue-capacity alternatives moved into `ARCHITECTURE.md` with the Option letters intact so the existing decision cross-reference stays resolvable.
- Stripped per-gate maturity verdicts and ordinal increment narration from `ARCHITECTURE.md`, including the `## Status` roll-up and the trailers that had to be edited on every gate transition, and removed the `Specified - Next` bookkeeping from `.ai/architecture.md`.
- Rewrote the instructions that caused the duplication: `prompts/SESSION_CLOSE.md` no longer requires `SESSION_HANDOFF.md` to restate completed work, current state, the next task, decisions, and verification commands; `AGENTS.md` and `prompts/SESSION_CLOSE.md` now append evidence to the verification log and edit state documents only where a fact changed; `.ai/workflow.md` is declared the operational expansion of Constitution Section 6 rather than a competing sequence.
- Corrected stale facts surfaced by the restructure: three documents stated three different next tasks, `SESSION_HANDOFF.md` instructed the next agent to confirm a task ID that no longer existed and named a superseded branch, its committed-state claims were false, and the recorded test-source count was 66 against an actual 65.
- Verified documentation-only impact with an unchanged full regression (65 suites, 299 tests, 297 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) and an end-to-end production CLI run over the changed documents reporting `memoryFreshness=matched=15`, `graphDecisions=84`, and `impactDecisions=1`, proving every required document loaded and the real decision log and justification reference resolved through the production composition.
- Left `DECISION_LOG.md` unsplit: separating it into per-decision files requires changes to `AcceptedDecisionProjector`, `RequiredProjectDocument`, and `TaskJustificationProjector` and is its own bounded task. No production or test source changed here.

## 2026-07-20 - Add WorkPayload Execution Input For Arbitrary-Target Execution

- Extended `WorkPayload` (`com.enhancer.bus`) with one optional caller-supplied `ExecutionInput(targetPath, expectedContentSha256)` component (`targetPath` bounded non-blank to 1024 characters, digest 64 lowercase hex) plus a three-argument convenience constructor delegating to empty, so every existing call site and the sealed payload hierarchy stay valid.
- Added the `WorkItem.executionInput()` projection and round-tripped the optional input through both filesystem serializers (`FileSystemSchedulerQueueStore`, `FileSystemAgentRuntimeStateStore`) via a presence flag after `allowedTools`, revising schema v1 in place with no version bump; pre-existing snapshots without the field fail closed on read.
- Added a `WorkMessagePublisher.publish` overload carrying `Optional<WorkPayload.ExecutionInput>` as explicit caller authority data (the existing signature delegates with empty), mirroring the CLI's `target-path`/`expected-sha256` model; snapshot-derived targets were rejected because observations are evidence, not approval authority.
- Made `AgentLoopAgentRunExecution`'s derivation seam prefer the payload-declared input and fall back to the approved source document, leaving the `ApprovedTask` construction and Goal binding unchanged, so a WorkItem now executes an arbitrary governed target through the same contained read-file, evidence, independent digest verification, and RunRecord pipeline.
- Proved the change test-first (30 aligned test-compile errors naming only the absent `ExecutionInput`, four-argument constructor, and accessor) and passed the extended payload, projection, both store round-trip, publisher-overload, port arbitrary-target, and worker end-to-end suites, then the full 65-suite/299-test regression (297 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) under `--warning-mode all` with Java 17 strict lint across 158 production sources.
- Left write/mutation Tools, multiple execution inputs, payload-carried plans/scripts, worker process isolation (3b), the local IPC adapter (3c), retry, and controls as future connections; no version bump or migration machinery was added.

## 2026-07-20 - Add AgentLoop-Backed Execution Port

- Added `AgentLoopAgentRunExecution` under `com.enhancer.runtime`, the first production implementation of the worker's `AgentRunExecution` port: one `execute(dispatch)` call assembles the Integrated Gate 1-4 pipeline (governed `read-file` `ToolExecutor` with `EvidenceRecorder`-persisted evidence, bounded `AgentRunController`/`AgentLoop` with the CLI reference bounds, `DeterministicReadFileVerifier`, and the application `AgentRunFinalizer`) and returns the persisted `run-record/<uuid>` reference.
- Executed the approved task's own source document as the governed target — `taskRevision().sourceDocument()` as the `read-file` path and `taskRevision().sourceSha256()` as the expected content SHA-256 — so the increment needs no Gate 7 envelope, queue, or runtime serialization change; a digest mismatch is real drift detection carried in the persisted RunRecord (non-`VERIFIED`, finalized to `FAILED`), never thrown.
- Constructed the `ApprovedTask` directly from the WorkItem's fields (no `ApprovedTaskReader`, no `In Progress` coupling), so the runtime finalizer's taskId-plus-sourceDocument binding holds by construction; pinned the wiring rule that the port persists through the same `RunRecordStore` the worker's `DurableAgentRunFinalizer` resolves from.
- Isolated the `(targetPath, expectedContentSha256)` derivation behind one private seam so the named follow-on `WorkPayload` execution-input extension (arbitrary targets plus its publish-time producer design) replaces only that derivation; recorded the rejected alternatives (revision-irreproducible `ApprovedTaskReader`, unresolvable `snapshotId`, envelope-splitting side store) in the accepted decision.
- Proved the port test-first (2 aligned missing-type compile errors, then 3/3 focused contract cases) and wired the real port into `DurableAgentRunWorker` over shared real filesystem stores: `FileSystemAgentLoopWorkerIntegrationTest` passed 2 of 2, driving a verified claim and its dependent to `VERIFIED_COMPLETED` with really persisted RunRecords and a digest-mismatch claim to `FAILED` with the dependent blocked.
- Passed the 16-suite/78-test runtime package suite and the full 65-suite/293-test regression (291 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) under `--warning-mode all` with Java 17 strict lint across 158 production sources.
- Left the `WorkPayload` execution-input extension, worker process isolation (3b), the concrete `MessageTransport` local IPC adapter (3c), write Tools, retry, and controls as future connections; no worker, dispatcher, runtime, finalizer, queue, or schema contract changed.

## 2026-07-20 - Add Gate 8 In-Process Scheduler Worker

- Added `DurableAgentRunWorker` under `com.enhancer.runtime` (connection sub-increment 3a): one `runOneCycle(leaseDuration)` call drives claim + fenced lease, injected execution to a durable RunRecord, fence-checked `completeExecution`, `finalizeAgentRun`, and the queue disposition in one recoverable, idempotent order, returning the cycle's `WorkItemDisposition` or empty when nothing was claimable.
- Added the worker-owned durable cycle-intent checkpoint: `PendingFinalization` (distinct canonical Goal/AgentRun UUIDs plus optional `runRecordReference`), `PendingFinalizationStore`, and the bounded, strict-UTF-8, digest-checked, atomically published, fail-closed `FileSystemPendingFinalizationStore` single-record adapter with `CorruptedPendingFinalizationException`.
- Wrote the intent before the queue claim so a restarted worker re-supplies the same identities and the dispatcher's idempotent recovery resumes the exact prefix (no second Goal, no orphaned runtime state, no dispatcher change), and persisted the reference before acknowledgement, closing the finalizer's deferred pre-terminal recovery window.
- Added `AgentRunExecution`, the injected execution port returning the RunRecord reference; the real `AgentLoop`-backed port is a named follow-on requiring a `WorkPayload` execution-input extension.
- Routed recovery by runtime state as the source of truth: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; earlier, unstarted, or missing runtime state (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference exists; execution/finalizer failures fail closed with the intent retained, and an empty-queue cycle leaves no durable trace.
- Proved each behaviour test-first (19 aligned checkpoint compile errors, 5 aligned worker compile errors, then 6 behavioural recovery failures against a deliberate resume stub) and passed 8/8 checkpoint, 9/9 worker, 2/2 filesystem end-to-end integration, and the 14-suite/73-test runtime package suites.
- Passed the full 63-suite/288-test regression (286 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) under `--warning-mode all` with Java 17 strict lint across 157 production sources.
- Left worker process isolation (3b), the concrete `MessageTransport` local IPC adapter (3c), the real execution port, retry, controls, and effect records as future connections; no dispatcher/runtime/finalizer/queue contract or schema changed.

## 2026-07-17 - Add RunRecord-Backed Result-Path Finalization

- Added `DurableAgentRunFinalizer` under `com.enhancer.runtime`, one durable idempotent coordinator over the durable queue, `AgentRuntimeStateStore`, and `RunRecordStore` with no new store or schema change.
- Drove the recoverable order resolve RunRecord -> runtime terminal (`recordResult`) -> queue disposition, deriving the disposition from the runtime terminal status (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`) so the two stores cannot diverge.
- Resolved (never persisted) the RunRecord by reference, bound it to the Goal on `taskId` plus `sourceDocument`, and carried the RunRecord's `verificationStatus` in a deterministic `ResultPayload` envelope keyed to the AgentRun identity.
- Added two entry points: `finalizeAgentRun(goalId, agentRunId, runRecordReference)` for the forward path and `recoverFinalization(goalId)` for autonomous post-terminal recovery that applies only the queue disposition and needs no reference.
- Honoured the durable queue's recovery contract by re-claiming a requeued active WorkItem before recording its terminal disposition; a disposition already in the completed/failed set is a no-op.
- Failed closed on a missing/corrupt RunRecord (run stays `AWAITING_VERIFICATION`, recoverable), rejected a RunRecord bound to a different task, rejected re-finalize with a different reference, and rejected finalize before execution acknowledgement.
- Proved each behaviour test-first (missing `DurableAgentRunFinalizer`, then missing `recoverFinalization`) and passed the full 60-suite/269-test regression (267 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) with Java 17 strict lint across 151 production sources.
- Left the Scheduler worker/Tool execution and RunRecord production (connection 3), retry through additional AgentRuns, and automatic failure propagation to dependents as future connections.

## 2026-07-17 - Add Durable Queue Terminal Disposition

- Added the terminal `WorkItemDisposition` enum (`VERIFIED_COMPLETED`, `FAILED`) where only verified completion satisfies dependencies.
- Added a separate `failedWorkItemIds` set to schema-v1 `SchedulerQueueState` and extended the partition invariant to `pending + active + verified + failed = admissionOrder` with verified and failed disjoint.
- Split the queue's single `completeActive` into `completeActiveVerified` and `failActive` across the in-memory queue, the durable persist-before-exposure wrapper, and the filesystem store; failed work never enters the dependency-satisfaction set, so its dependents stay blocked.
- Persisted the failed disposition in the schema-v1 on-disk format (revised in place, no version bump) with exact restart recovery; a persisted terminal disposition is never re-run and only interrupted active work is requeued.
- Recorded that the queue stores disposition only, not a failure reason, and that pre-existing local queue snapshots fail closed on read because the unreleased schema-v1 envelope rejects trailing bytes.
- Proved each contract test-first (missing enum, constructor arity, methods, dropped serialization) and passed the full 59-suite/261-test regression (259 passed, 2 existing Windows symbolic-link skips, 0 failures, 0 errors) with Java 17 strict lint across 150 production sources.
- Left `ResultPayload`/RunRecord result wiring, dispatcher-driven disposition recording, retry, automatic failure propagation, and a non-terminal waiting state as future connections.

## 2026-07-16 - Align Gate 8 Connection And Completion Boundaries

- Cross-checked the seven `.ai/` bootstrap documents, canonical governance and architecture documents, and the implemented Gate 8 queue/runtime state contracts.
- Corrected the conflicting next-step wording: fence-checked execution completion persists `AWAITING_VERIFICATION`, while queue completion satisfies dependencies, so the two operations cannot be coupled directly.
- Made durable terminal queue disposition the next bounded contract, with verified completion and failed disposition kept distinct before Scheduler capacity or dependency state changes.
- Added an ordered, gate-owned connection backlog for RunRecord-backed results, process-isolated workers and local IPC, controls, effects, retry, and later multi-agent handoffs.
- Corrected stale RFC and blanket-unimplemented wording without changing production code, capability maturity, Constitution text, Agent rules, or external authority.
- Passed 24 focused actual-document tests (23 passed, 1 existing Windows symbolic-link skip), the full 57-suite/251-test regression (249 passed, 2 existing skips), and structural/reference/whitespace checks with no failure or error.
- Added a next-session design brief describing the semantic collision, the recommended conservative implementation, the higher-throughput waiting-state alternative, the rejected unsafe shortcut, and the unresolved schema/failure/identity decisions.

## 2026-07-16 - Integrate Durable Queue Claims With Fenced AgentRuns

- Added `DurableAgentRunDispatcher` and immutable `AgentRunDispatch`, connecting one active or newly claimed exact WorkItem to Goal creation/recovery, named AgentRun planning/readiness, and fenced lease acquisition.
- Kept queue and runtime artifacts as separate durable boundaries: queue claim persists first, runtime prefixes persist independently, and partial runtime failures retain a recoverable active claim instead of claiming unsupported cross-store atomicity or rollback.
- Added idempotent same-owner re-entry, exact WorkItem matching before recovery mutation, strict AgentRun/owner/post-execution mismatch refusal, and expiry recovery that permits a new owner only with a greater fence.
- Verified all four runtime persistence interruption points, queue-claim failure with no runtime creation, exact filesystem restart recovery, Unicode lease preservation, and authority/provenance retention without Tool execution or queue completion.
- Proved the missing contract through 13 aligned test-compilation errors, then passed 31 focused runtime/store/boundary tests and the complete 57-suite/251-test regression (249 passed, 2 existing Windows symbolic-link skips).
- Passed Java 17 strict lint across 149 production sources; retained terminal queue disposition, workers, results, retry, effects, cross-store transactions, multi-process coordination, and power-loss directory durability as future work.

## 2026-07-16 - Fence Durable Gate 8 AgentRun Ownership

- Added immutable bounded `AgentRunLease` ownership to the durable schema-v1 AgentRun lifecycle with injected time, exclusive expiry, and a persisted monotonic last-issued fence token.
- Restricted acquisition to `READY`; renewal and execution completion require the matching current owner and fence and fail closed at or after expiry.
- Added explicit and recovery-time orphan reclamation that durably returns expired `EXECUTING` work to `READY`, retains fence history, and gives the next owner a strictly greater token.
- Extended strict-UTF-8 integrity-checked filesystem state encoding to recover exact lease timestamps, owner, fence, and aggregate fence history; store updates permit the fence to stay current or advance exactly one.
- Preserved persist-before-exposure for acquire, renew, complete, and reclaim, with regression coverage proving each injected storage failure leaves the previous revision and lease authoritative.
- Passed 68 focused runtime/bus/boundary tests, the complete 55-suite/243-test regression (241 passed, 2 existing Windows symbolic-link skips), and Java 17 strict lint across 147 production sources.
- Added no worker or Tool execution, external-effect fencing, retry, multi-process locking, distributed clock-skew protocol, schema migration, or parent-directory power-loss durability.

## 2026-07-16 - Add Durable Gate 8 Goal And AgentRun Lifecycle

- Added immutable schema-v1 `RuntimeGoal`, `RuntimeAgentRun`, and `AgentRuntimeState` over one exact existing WorkItem without widening task, snapshot, capability, logical-run, or Tool-scope provenance.
- Enforced forward-only Goal and AgentRun lifecycles, distinct canonical identities, matching work/result message provenance, and Verified-only completion with every other verification state recorded as explicit failure.
- Added `DurableAgentRuntime`, persisting every successful transition before exposure and leaving the previous durable and in-memory revision unchanged on storage failure.
- Added `FileSystemAgentRuntimeStateStore` with a 4 MiB ceiling, strict UTF-8, complete-envelope SHA-256 integrity, exact WorkItem/result-envelope recovery, atomic create/replace publication, revision checks, and fail-closed missing/corrupt/oversized/trailing/unsupported-state handling.
- Proved the missing contract through 64 aligned test-compilation errors, then passed 63 focused runtime/bus/boundary tests and the complete 55-suite/238-test regression (236 passed, 2 existing Windows symbolic-link skips).
- Passed Java 17 strict lint across 146 production sources; retained retry, leases/fencing, worker execution, effect records, RunRecord resolution, schema migration, history cleanup, multi-process coordination, and parent-directory power-loss durability as future work.

## 2026-07-16 - Remove Runtime Package Dependency Cycles

- Moved VerificationDecision, VerificationStatus, and VerificationCode unchanged from verification implementation code to neutral `com.enhancer.kernel`.
- Moved AgentRunFinalizer from `com.enhancer.loop` to `com.enhancer.application`, leaving finalization behavior and persist-before-completion semantics unchanged.
- Added `VerifiedAgentRunTransition` as the explicit application-facing port while retaining the actual AgentRunState completion method as package-private.
- Enforced an acyclic dependency direction with `RuntimePackageBoundaryTest`; loop no longer imports run or verification, run no longer imports verification, and kernel imports none of the runtime/application packages.
- Preserved RunRecord schema, stored enum names, CLI behavior, verification decisions, and replay compatibility.
- Passed 27 focused tests, the complete 53-suite/228-test regression (226 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 135 production sources.

## 2026-07-16 - Bound In-Process Tool Worker Accumulation

- Added one process-wide 64-slot live Tool isolation capacity shared by default across ToolExecutor instances.
- Held each slot until the actual worker thread terminates, so timeout, interrupt, close, and shutdown do not undercount interrupt-ignoring code.
- Added typed terminal `ISOLATION_CAPACITY_EXHAUSTED` refusal before worker/thread creation when the process ceiling is full.
- Preserved independent next invocation below the ceiling and proved deterministic saturation/recovery with an injected one-slot shared capacity.
- Passed 41 affected tests with 1 existing symbolic-link skip, the full 52-suite/227-test regression (225 passed, 2 existing skips), and Java 17 strict lint across 134 production sources.
- Kept process isolation and OS-level termination as required future boundaries; this change is containment, not forced recovery.

## 2026-07-16 - Harden Unicode And Mutable-File Resource Bounds

- Added shared Unicode-safe prefix/suffix bounding that preserves existing UTF-16 ceilings without splitting supplementary surrogate pairs.
- Applied it to VerificationEvidence tails, ToolExecutor diagnostics, CLI bounded output/values, and bounded Workspace failure reasons.
- Added bounded file read/hash operations that enforce the byte ceiling during consumption, allocate no more than the accepted read limit, and inspect at most one extra byte for overflow.
- Applied in-operation bounds to ReadFileTool, ProjectContextReader, target-file hashing, and Evidence, RunRecord, and Scheduler queue artifact resolution while preserving strict UTF-8 and typed failure behavior.
- Reproduced the Unicode defect with a 4097-code-unit valid string and proved strict UTF-8-safe output; focused tests passed 18/18, affected integration tests passed 54 with 2 existing symbolic-link skips, the full 52-suite/226-test regression passed 224 with the same 2 skips, and strict lint passed across 133 production sources.
- Recorded that stuck in-process Tool threads, the loop/run/verification package cycle, and parent-directory power-loss durability require separate work.

## 2026-07-16 - Add Durable Gate 8 Queue State And Restart Recovery

- Added immutable schema-v1 `SchedulerQueueState` over one canonical queue identity, one logical run, admission order, pending/active/completed state, WorkItems, dependencies, and unchanged Gate 7 envelope provenance.
- Added `FileSystemSchedulerQueueStore` with a 64 MiB state ceiling, strict UTF-8, complete-envelope SHA-256 integrity, atomic create/replace publication, revision checks, and fail-closed missing/corrupt/oversized/trailing/unsupported-state handling.
- Added `DurableSingleWorkerSchedulerQueue`, staging every enqueue, successful claim, and completion on a copy and persisting the next revision before exposing it.
- Added restart recovery that persists interrupted active work back into admission-ordered pending state, making the queue honestly at-least-once without claiming effect deduplication, leases, fencing, or workers.
- Passed 14 focused runtime/integration tests, the 50-suite/219-test full regression (217 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 130 production sources.
- Clarified that atomic publication prevents partial process-visible state but does not claim power-loss durability of parent-directory metadata.

## 2026-07-16 - Add The Gate 8 Single-Worker Scheduler Queue

- Added immutable `QueuedWork` over one existing `WorkItem` with at most 256 unique canonical earlier-admitted dependency identities.
- Added a deterministic run-scoped `SingleWorkerSchedulerQueue` bounded to 4096 admissions, with duplicate rejection, FIFO dependency readiness, one active slot, and matching explicit completion.
- Preserved the exact WorkItem and Gate 7 envelope without adding task approval, Tool authority, verification, worker execution, persistence, leases, retry, cancellation, priority, or recovery.
- Proved the missing contracts through a 25-error focused RED, then passed all 45 focused messaging/runtime tests, the complete regression, and Java 17 strict lint.
- Kept Gate 8 at `Specified - Next`; only WorkItem admission and the in-memory queue sub-capabilities are Contract Verified, with durable queue state and restart-safe recovery next.

## 2026-07-16 - Assess Gate 7 Integrated Maturity

- Reassessed all six Gate 7 scope items and four exit criteria against the named Gate 6-to-Gate 7-to-Gate 8 integration path and fresh contract evidence.
- Classified the real work-message queue path as Integrated: approved Workspace input crosses one unchanged `WorkPayload` envelope through queue delivery, journaling, replay, and duplicate suppression into `WorkItem` admission.
- Retained Gate 7 at Contract Verified because result/control/handoff and non-empty-causation flows, topic and reliability branches, and `MessageTransport` still lack named real production connections.
- Passed all 42 focused messaging/runtime tests, the 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 122 production sources.
- Changed no production or test behavior, capability maturity, next-gate marker, Constitution rule, external authority, or release state.

## 2026-07-16 - Add Product Journey Evaluation And Security Tracks

- Added a cross-cutting Product Journey and Evaluation Track with four initial end-to-end journeys and a fifth priority for a repeatable release-quality harness.
- Defined explicit-denominator measures for task success, incorrect changes, recovery, cost/time, intervention, held-out regression, and multi-agent delta, with versioned fixtures and thresholds fixed before evaluation.
- Replaced any implied universal exactly-once Scheduler goal with at-least-once delivery plus stable idempotency, fenced leases, checkpoints, state migration, orphan reclamation, and replay-safe or compensatable effects.
- Ordered interface delivery around one shared Run/approval/verification/evidence/control API: CLI reference surface, VS Code second, and Desktop later as supervision; made one change-centered review a Gate 12 exit criterion.
- Added a layered default-security baseline for untrusted repository/Tool/model/MCP/plugin/dependency content and assigned concrete enforcement to the owning delivery gates without amending the Constitution.
- Strengthened multi-agent and release criteria so claims require measured journey improvement and predeclared quality thresholds rather than Agent self-report or successful demonstrations alone.

## 2026-07-16 - Prepare The Gate 7 Runtime Integration Path

- Added `WorkMessagePublisher` to derive and publish one existing bounded work envelope from a matching repository-approved task and real Gate 6 Workspace snapshot without creating approval or Tool authority.
- Added `WorkItemAdmissionHandler` to retain the delivered envelope unchanged in one Gate 8 `WorkItem` through injected boundaries, without adding storage, scheduling, execution, or concrete IPC behavior.
- Added a named integration test over the real Context Reader, Workspace collector, in-process queue, journal, replay, and WorkItem admission path; it proves unchanged provenance/authorization projections and duplicate-free replay.
- Passed 42 focused tests, the 47-suite/208-test full regression (206 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 122 production sources.
- Kept Gate 7 at Contract Verified; the integration evidence is input to a separate gate-level maturity assessment rather than an automatic promotion.

## 2026-07-16 - Add Gate 8 WorkItem Admission

- Added immutable `WorkItem` admission over one unchanged Gate 7 `WorkPayload` envelope with a distinct canonical work identity and a 256-character required-capability ceiling.
- Exposed approved task revision, Workspace snapshot identity, logical run identity, and allowed Tools only as projections of the retained envelope, without creating authority, runtime state, scheduling, persistence, leases, or execution.
- Proved the missing contract through an 11-error focused RED, then passed 41 focused tests, the 46-suite/207-test full regression (205 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 120 production sources.
- Kept Gate 8 at `Specified - Next`; only the admission sub-capability is Contract Verified and the dependency-ready single-worker Scheduler queue is next.

## 2026-07-16 - Promote Gate 7 And Advance Gate 8

- Reassessed all six Gate 7 scope items and all four exit criteria against fresh contract evidence after closing the payload-cardinality blocker.
- Promoted Delivery Gate 7 to Contract Verified without claiming production messaging integration, a concrete adapter, or a real process boundary.
- Advanced Delivery Gate 8 Agent Runtime and Scheduler to the sole `Specified - Next` marker and synchronized the two actual-Roadmap self-hosting expectations.
- Verified 39 focused bus tests, the 45-suite/205-test full regression (203 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 119 production sources.
- Verified the completed document state through 16 actual-document self-hosting tests: 15 passed and 1 existing Windows symbolic-link setup case skipped, with both proposal paths selecting Gate 8.

## 2026-07-16 - Bound Gate 7 Work Payload Scope

- Added an explicit maximum of 256 unique `WorkPayload.allowedTools` names while retaining the existing 256-character per-name ceiling and immutable copying.
- Proved the gap behaviorally before the fix: a payload with 257 valid names was accepted during RED; the corrected contract accepts exactly 256 and rejects 257.
- Closed the prior maturity assessment's bounded-payload blocker without selecting a concrete IPC adapter or changing Gate 7 lifecycle status.
- Verified all 39 bus tests, the 45-suite/205-test full regression (203 passed, 2 existing symbolic-link skips), and Java 17 strict lint across 119 production sources.

## 2026-07-16 - Assess Gate 7 Messaging Maturity

- Mapped every Gate 7 scope item and exit criterion to fresh contract evidence or an explicit blocker without changing production or test code.
- Confirmed that transport adapters and production wiring are integration work rather than requirements for Contract Verified maturity.
- Found one gate-level blocker: `WorkPayload.allowedTools` has bounded entries but unbounded collection cardinality, so the bounded-payload exit criterion is not yet satisfied.
- Recommended one test-first payload-bound correction before Gate 7 promotion; rejected both premature promotion and a premature concrete IPC adapter.

## 2026-07-16 - Define The Transport-Neutral IPC Boundary

- Added immutable `TransportMessage` over the existing destination and envelope plus provider-neutral `MessageTransport`.
- Added typed `TransportOutcome`/`TransportStatus` so hop acceptance, backpressure, and unavailability remain distinct from Message Bus subscriber delivery.
- Kept adapters, endpoints, serialization, authentication, threading, persistence, scheduling, and production wiring out of the contract.
- Verified test-first with 33 expected RED missing-symbol errors, 38 focused bus tests, the 204-test full regression, and Java 17 strict lint across 119 production sources.

## 2026-07-16 - Verify Windows Real-Path Boundaries And Correct Evidence Policy

- Added Windows junction regressions that execute successfully on this host and prove both `ReadFileTool` and `ProjectContextReader` reject real paths escaping the project root.
- Replaced the unused `EvidenceRetentionPolicy`/30-day field with `EvidenceStoragePolicy`, which exposes only the enforced per-artifact content bound.
- Added no evidence deletion or expiry and passed focused plus Tool/Context/Verification/Loop regressions; the two privilege-dependent symbolic-link tests remain skipped while junction coverage passes.
- Completed the combined delivery cross-regression at 44 suites/200 tests (198 passed, 2 existing symlink skips), with both junction cases executed and Java 17 strict lint passing across 115 production sources.

## 2026-07-16 - Bound Workspace RunRecord Observation

- Added deterministic newest-first `recentReferences(limit)` while retaining complete reference listing and point replay.
- Limited Workspace collection to 256 recent records, capping full payload resolution and preventing accumulated history from exhausting the 4096-observation snapshot bound.
- Verified newest selection, invalid limits, old-record exclusion, new-record inclusion, no deletion, and RunRecord/Workspace/CLI package regressions.

## 2026-07-16 - Preserve Durable CLI Outcomes Across Brain Reporting

- Preflighted decision, justification, artifact, and graph bounds before evidence creation and Tool execution.
- Collapsed required-document target duplicates into one target-preferred artifact and aligned graph/source identifier bounds at 1024 characters.
- Made post-persist Project Brain reporting degradable with explicit bounded status while preserving the RunRecord-derived exit code.
- Verified required-document targets, pre-persist malformed-decision rejection, and injected post-persist reporting failure across focused CLI and Brain regressions.

## 2026-07-16 - Eliminate Git Observer Command Execution Vectors

- Replaced unqualified Git lookup with canonical absolute PATH resolution that rejects project-contained executables.
- Reduced Git observation to filter-free index/untracked/deleted metadata and made tracked worktree diff explicitly unavailable after adversarial tests proved status, modified-file, and raw-diff paths can execute required clean filters.
- Verified the focused 8-test collector suite and the complete Workspace package with no failures.

## 2026-07-16 - Bound Gate 7 Pending Publications

- Added immutable `BackpressurePolicy` with a finite 1-4096 pending-publication capacity and preserved the existing bus constructors through a finite default.
- Added scope-level `BACKPRESSURED` refusal without blocking or journal, handler, idempotency, dead-letter, or cancellation side effects; refused work remains explicitly retryable.
- Applied deterministic prefix admission to replay while keeping replay cascades non-journaling.
- Verified test-first with 34 focused bus tests, the 189-test full regression, and Java 17 strict lint over all 115 production sources.

## 2026-07-16 - Synchronize PR #3 And Current Repository State

- Verified local `main` and `origin/main` already match PR #3 merge commit `52987f2`; no checkout or pull was necessary.
- Replaced stale current-state claims that retry, cancellation, and ordering were uncommitted or that `e74be87` was the published tip.
- Aligned canonical and compact guidance with Gate 6 Integrated, Gate 7 Specified - Next, current delivery and Git boundaries, and backpressure next.

## 2026-07-16 - Harden Git Workspace Observation Authority

- Removed inherited `GIT_*` overrides from the two authorized Git child processes while preserving unrelated process environment.
- Disabled external diff and text-conversion helpers explicitly without adding another command or widening authority.
- Added focused security regression coverage and passed the 186-test full suite plus Java 17 strict lint.

## 2026-07-16 - Correct Gate 7 Replay Cascades

- Reproduced and fixed replay-caused publications appending to the live journal.
- Routed every publication through drain-owned admission so cancelled re-entrant work remains visible in the ordered cascade without delivery or journaling.
- Added focused regression coverage and passed the 183-test full suite plus Java 17 strict lint.

## 2026-07-16

- Added Contract Verified Gate 7 run-to-completion delivery ordering on `InProcessMessageBus`: a pending queue and a single drain loop replace nested dispatch, so a publication made from inside a handler is queued and reports the new scope-level `ENQUEUED` status while the draining top-level `publish` or `replay` returns the whole ordered cascade; delivery order now equals publication order and no subscriber observes an effect before its cause. Admission and journaling moved into the drain loop, so the journal's order is the bus's own delivery order and a correlation cancelled mid-cascade refuses entries queued behind it while an in-flight fan-out stays atomic; an `Error` abandons the cascade entirely.
- Proved the ordering defect was real before fixing it: with only the two missing symbols added so the suite could run, the focused tests observed `[first, child, second]` where `[first, second, child]` was required, confirming a cascaded child really was delivered inside its parent's fan-out.
- Added `DeliveryStatus.isScopeLevel()` covering `UNROUTED`, `CANCELLED`, and `ENQUEUED`, and reduced the `DeliveryOutcome` invariant to it.
- Verified delivery ordering test-first with 8 expected RED errors naming only the absent `ENQUEUED` constant and `isScopeLevel` accessor, then a behavioural RED pass, then 25 focused `InProcessMessageBusTest` tests, the full 181-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 114 production sources.
- Added Contract Verified Gate 7 cancellation propagation on `InProcessMessageBus`: `cancel(correlationId)` is idempotent and monotonic with no resume, and a cancelled correlation is refused admission before subscription lookup, idempotency, and dispatch on every path — publish, replay, and dead-letter re-delivery — reporting a scope-level `CANCELLED` outcome that names no subscription, invoking no handler, consuming no idempotency key, creating no dead letter, and appending nothing to the journal so replay stays deterministic; cancellation dominates both `UNROUTED` and `DUPLICATE`, and the bus reads no payload to decide delivery, keeping `ControlSignal.CANCEL` a consumer semantic a handler may act on by calling `cancel` itself.
- Generalized the `DeliveryOutcome` invariant to "a scope-level status (`UNROUTED` or `CANCELLED`) carries no subscriberId; every other status must name the subscription it targeted".
- Verified cancellation propagation test-first with 19 expected RED errors naming only the absent `CANCELLED` constant, `cancel`, and `isCancelled`, then 20 focused `InProcessMessageBusTest` tests, the full 176-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 114 production sources.
- Added Contract Verified Gate 7 bounded synchronous retry and explicit dead-letter re-delivery on `InProcessMessageBus`: an immutable `RetryPolicy` (1-10 attempts; the no-argument bus constructor keeps a single attempt) retries a failing handler immediately and with no delay until the policy is exhausted, `DeadLetter` now records the failed attempt count, and `redeliver` accepts only a currently recorded dead letter, resolves it on success, and on renewed exhaustion replaces it in place with the accumulated attempt count and latest reason — never appending to the journal or releasing the consumed idempotency key, so publish and replay still report `DUPLICATE`.
- Verified the retry and re-delivery contract test-first with 16 expected RED errors naming only the absent `RetryPolicy`, policy constructor, `redeliver` operation, and `DeadLetter` attempt count, then 15 focused `InProcessMessageBusTest` tests, the full 171-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 114 production sources.

## 2026-07-15

- Added Contract Verified Gate 7 delivery-failure isolation and dead-letter capture on `InProcessMessageBus`: a subscriber handler that throws a `RuntimeException` yields a typed `FAILED` outcome and an ordered immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason) while fan-out continues to the remaining subscribers; a failed delivery consumes the idempotency key and is terminal, so re-publishing or replaying it reports `DUPLICATE` and adds no further dead letter, deferring automatic retry and re-delivery to a later increment.
- Verified the failure handling test-first with 8 expected RED errors naming only the absent `FAILED` constant, `DeadLetter` type, and `deadLetters()` accessor, then 10 focused `InProcessMessageBusTest` tests, the full 166-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 113 production sources.
- Added the Contract Verified Gate 7 in-process delivery surface `InProcessMessageBus` under `com.enhancer.bus`: deterministic synchronous topic fan-out in registration order and single-consumer queue delivery over `MessageEnvelope`, typed `DeliveryOutcome`/`DeliveryStatus` (`DELIVERED`/`DUPLICATE`/`UNROUTED`) results, per-`(destination, subscriber, message identity)` idempotency, and an ordered immutable journal supporting deterministic replay without duplicate side effects; authorization and provenance survive every hop and no retry, dead-letter, ordering, backpressure, threading, persistence, or transport was introduced.
- Verified the delivery surface test-first with 54 expected RED missing-symbol errors naming only the five absent delivery types, then 7 focused `InProcessMessageBusTest` tests, the full 163-test regression with only the 2 existing Windows symbolic-link setup skips, and Java 17 `-Xlint:all -Werror` over all 112 production sources.
- Fixed a pre-existing wall-clock-dependent defect in `RunRecordMetadataCollectorTest` (unrelated to the delivery increment): the test hardcoded the observation time to `2026-07-15T10:01:00Z` while `persist()` stamps `storedAt` with `Instant.now()`, so an AVAILABLE record's stored time fell after the fixed observation time and its `sourceUpdatedAt` was dropped as future, failing whenever the wall clock passed 10:01 UTC; the observation time is now derived from the run clock, making the test time-independent.
- Published the Gate 6 assessment and promotion and the Gate 7 envelope contract to `origin/main` in delivery commit `3423201`.
- Added the Contract Verified Gate 7 `MessageEnvelope` contract: versioned reference-only envelopes with canonical message/causation identities, bounded correlation/run/producer identities, and a sealed four-kind payload hierarchy carrying task revisions, snapshot identities, authorization scopes, run-record references, verification status, and control signals as data.
- Verified the envelope contract test-first with 38 expected RED missing-symbol errors, then 4 focused tests and the full 156-test regression with only the 2 existing Windows symbolic-link setup skips.
- Promoted Delivery Gate 6 to Integrated through the user-approved re-scope decision: diagnostics, terminal-session, and active/selected-file observation moved to Gate 12, which owns those capabilities, and Delivery Gate 7 Event Bus and IPC Foundation became the sole `Specified - Next` product gate.
- Updated the two actual-roadmap test contracts to Gate 7 and verified the full 152-test regression with the marker at Gate 7 and no production change.
- Recorded the Gate 6 maturity assessment against fresh 152-test evidence: all evidenced scope items and exit criteria named, diagnostics/terminal/selection blockers traced to Gate 8-12 capabilities, and the re-scope-and-promote recommendation recorded pending explicit user approval.
- Published the authority-boundary evidence, target-file observation, and Git adapter increments to `origin/main` in delivery commit `21e6230`.
- Added the `GitWorkspaceCollector` under explicitly granted read-only external command authority: two fixed git invocations (status/diff) with discovery confined to the project root, watchdog-enforced timeout, discarded stderr, `--no-optional-locks` and fsmonitor disabled, digest-only retention, and every failure surfaced as explicit `UNAVAILABLE`.
- Caught and fixed a real semantic defect during GREEN: without a discovery ceiling, temporary directories observed the enclosing repository; `GIT_CEILING_DIRECTORIES` now confines observation to the project's own working tree.
- Verified the Git adapter through 62 focused tests, the full 152-test regression, and an actual-repository run observing 23 sources including 2 `AVAILABLE` Git observations.
- Added the `TargetFileMetadataCollector`: the governed run's target file is observed pre-run as a `REPOSITORY_FILE` snapshot observation with a real streamed containment-checked SHA-256, missing/oversized targets surface as explicit `UNAVAILABLE`, and containment violations fail early as usage errors; verified test-first through 59 focused tests, the full 149-test regression, and an actual-repository run observing README.md.
- Pinned the Gate 6 authority boundary with `WorkspaceAuthorityBoundaryIntegrationTest`, passing on first run: adversarial tool-grant text in observed documents cannot widen the persisted task or policy scope, appear in bounded output, or mutate any repository document, and a task without `read-file` stays rejected; full 146-test regression passed with no production change.
- Published the justification-reference increment to `origin/main` in delivery commit `0e2be2c`.
- Adopted the optional `Justified By` task-document section and the `TaskJustificationProjector`: explicit references to accepted-decision headings become `JUSTIFIED_BY` edges with task-document provenance, strict rejection of malformed or unresolved references, and a bounded `impactDecisions` count on the production run output.
- Resolved the first real justification on the actual repository: this increment's own task document reference surfaced as `impactDecisions=1` with 46 decision nodes and 18 observations.
- Verified the reference grammar test-first with 6 expected RED missing-symbol errors, then 54 focused tests and the full 144-test regression with only the 2 existing Windows symbolic-link setup skips.
- Promoted all six Contract Verified Gate 6 sub-capabilities to Integrated through a documentation-only audit: `WorkspaceSnapshot`, `ProjectBrainView`, the graph projection contract, `TaskImpactQuery`, `AcceptedDecisionProjector`, and `RunRecordMetadataCollector`, each mapped to named pre-existing integration evidence re-run fresh (59 focused tests, full 140-test regression, no failures).
- Kept Delivery Gate 6 `Specified - Next`; gate-level promotion still requires the reference grammar, remaining producers and adapters, and full exit-criteria evidence.
- Published the run-evidence producer, decision projection, run-record observation, and production graph composition increments to `origin/main` in delivery commit `396665b`.
- Composed the Project Brain graph on the production CLI `run` path: prior run records observed into the snapshot, accepted-decision nodes merged into the run-evidence graph, and the task impact query answered in process, reporting bounded `graphNodes`, `graphEdges`, `graphDecisions`, and `impactExecutions` counts.
- Promoted the production graph composition to Operational with an actual-repository run: 17 observations including 2 prior run records, 61 graph nodes, and 44 decision nodes matching the decision log's 44 accepted decisions exactly.
- Verified the composition test-first (expected missing-output RED, 50 focused tests GREEN) and passed the full 140-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the Contract Verified `RunRecordMetadataCollector` and a read-only lexicographically ordered `references()` listing on the RunRecord store: one `RUN_RECORD` observation per stored record with the envelope SHA-256 and stored time, and explicit `UNAVAILABLE` observations with bounded reasons for corrupted or missing records.
- Verified the observation path test-first with 8 expected RED missing-symbol errors, then 33 focused tests and the full 139-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the Contract Verified `AcceptedDecisionProjector`: accepted decisions parsed from the decision log's own `Status: Accepted Decision` lines into unlinked `DECISION` nodes with snapshot-relative freshness (matched digest Current, diverged or unobserved Stale) and no invented edges.
- Verified the projector test-first with 6 expected RED missing-symbol errors, then 20 focused Project Brain tests and the full 134-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the first Project Brain graph producer: `RunEvidenceGraphProducer` projects one task node, observed repository artifacts with one-to-one state-to-freshness mapping, one execution node with the stored envelope SHA-256, and one `RECORDED_AS` edge from one snapshot and one task-matched run record.
- Integrated the run-evidence production path end to end: a real governed CLI run and really-collected snapshot flow through the producer into a `TaskImpactQuery` answer naming the real stored execution.
- Refused unjustified projection by decision: no decision, modifies, verified-by, justified-by, supersedes, or depends-on elements are emitted until their evidence sources exist.
- Verified the producer test-first with 6 expected RED missing-symbol errors, then 18 focused tests and the full 130-test regression with only the 2 existing Windows symbolic-link setup skips.
- Published the five Gate 6 project brain foundation increments to `origin/main` in delivery commit `d3b6197`.
- Added the Contract Verified Gate 6 task impact query: `TaskImpactQuery` answers the first rebuildable task-to-decision-to-code-to-test chain over one projected graph with snapshot-traceable immutable results.
- Derived one rebuild-required status from every traversed node and edge so unrelated staleness does not taint the answer, deduplicated shared verifying artifacts, and rejected unknown or non-task identities.
- Verified the query test-first with 9 expected RED missing-symbol errors, then 13 focused tests and the full 127-test regression with only the 2 existing Windows symbolic-link setup skips.
- Deferred transitive `DEPENDS_ON` closure, graph producers, and persistence by recorded decision; the next producer increment gives the query real project evidence.
- Added the Contract Verified Gate 6 graph projection contract: five typed node kinds, six endpoint-checked edge kinds over the Decision, Architecture, Dependency, Task, and Execution relationship domains, and immutable element provenance with source, optional SHA-256 revision, explicit freshness, and derived rebuild status.
- Keyed each `ProjectBrainGraph` projection to one Workspace snapshot identity with an explicit projection time and version, deterministic ordering, and duplicate/self-loop/unknown-endpoint/bound rejection.
- Verified the graph contract test-first with 100 expected RED missing-symbol errors, then 9 focused tests and the full 123-test regression with only the 2 existing Windows symbolic-link setup skips.
- Named the task-to-decision-to-code-to-test impact query as the contract's consumer and kept Gate 6 `Specified - Next` with no producer, query, or persistence.
- Composed the `ProjectBrainView` on the production CLI `run` path from the already-loaded repository memory, the collected snapshot, and the persisted RunRecord, for every outcome that produces a record.
- Reported bounded `workspaceSnapshotId`, `workspaceObservations`, and `memoryFreshness` metadata in the run output without changing commands, arguments, exit codes, the RunRecord schema, or replay.
- Promoted the production repository-memory composition to Operational with an actual-repository run (`README.md`, exit code 0, snapshot identity, 15 matched documents) and its unchanged replay.
- Verified the composition test-first (expected missing-output RED, 29 focused tests GREEN) and passed the full 119-test regression with only the 2 existing Windows symbolic-link setup skips.
- Added the first Workspace source adapter: the read-only `RepositoryMemorySnapshotCollector` deriving a real `WorkspaceSnapshot` from Context Reader repository memory with computed digests, `context-reader` provenance, and an `ApprovedTaskRevision` digested from the same memory.
- Integrated the Gate 6 repository-memory path end to end: a real governed CLI run, its persisted RunRecord, really-loaded repository memory, the collector, and the composed `ProjectBrainView` with all documents `SNAPSHOT_MATCHED` and exact `SNAPSHOT_DIVERGED` detection after the active task document changed.
- Verified the collector test-first with 6 expected RED missing-symbol errors, then 20 focused tests and the full 117-test regression with only the 2 existing Windows symbolic-link setup skips.
- Kept Gate 6 `Specified - Next`: no production caller composes the view during an actual run, and Git, diagnostics, selection, and terminal adapters plus graph projections remain unimplemented.
- Added the read-only Gate 6 `ProjectBrainView` aggregate under `com.enhancer.brain`, giving the Contract Verified `WorkspaceSnapshot` its first consumer.
- Composed the view from one real Workspace snapshot, one real repository-memory `ProjectContext`, and one real `RunRecord`, keyed to the existing canonical snapshot identity.
- Projected repository memory to path, read order, and computed SHA-256 with explicit `SNAPSHOT_MATCHED`, `SNAPSHOT_DIVERGED`, and `NOT_OBSERVED` freshness, retaining no document content.
- Projected RunRecords to logical run identity, record time, approved task identity, and verification status, excluding Tool payloads, evidence bodies, and chat history.
- Rejected runs whose approved task identity or source document does not match the snapshot revision, so the aggregate cannot misattribute provenance.
- Verified the aggregate test-first with 19 expected RED missing-symbol errors, then 15 focused tests and the full 113-test regression with only the 2 existing Windows symbolic-link setup skips.
- Recorded `ProjectBrainView` as Contract Verified and kept Gate 6 `Specified - Next`, because no adapter collects a live snapshot and no production path composes the view.
- Published the Contract Verified Gate 6 WorkspaceSnapshot contract and synchronized project memory to `origin/main` in delivery commit `c5a16b9`.
- Added the first Delivery Gate 6 contract: immutable metadata-only `WorkspaceSnapshot`, approved-task revision provenance, typed source observations, explicit freshness/availability, deterministic ordering, bounded metadata, and canonical SHA-256 identity.
- Verified the Workspace contract test-first with 10 focused tests, then passed the 108-test full regression and Java 17 warning-as-error production lint without promoting Gate 6 beyond `Specified - Next`.
- Recorded the Workspace snapshot sub-capability as Contract Verified and selected a minimal read-only `ProjectBrainView` as its next integration consumer.
- Published the Operational Gate 5 CLI, Integrated Gate 0 lifecycle evidence, and RED workflow clarification to `origin/main` in commit `ed901f3`.
- Promoted Delivery Gate 0 Foundation Safety Contracts from Contract Verified to Integrated through a new authority-preserving lifecycle characterization test.
- Proved Proposal non-mutation, pre-activation rejection, external-only task activation, verified Gate 5 execution, persist-before-completion RunRecord storage, and replay after target deletion.
- The Gate 0 lifecycle test passed on its first run, so no production correction or second orchestrator was added.
- Verified 43 focused tests and the full 98-test regression suite while preserving Gate 6 as the sole `Specified - Next` product gate.
- Prepared the authority-preserving Gate 0 integration-promotion task, accepted decision, lifecycle test contract, and verification plan without changing Gate 0 maturity.
- Kept Gate 6 as the sole `Specified - Next` product gate and prohibited automatic Proposal approval or a second production orchestrator in the Gate 0 audit.
- Made Delivery Gate 5 Operational with the supported local `EnhancerCli` `run` and `replay` commands.
- Added explicit governed inputs, stable outcome exit codes, bounded diagnostics, verified-only completion, and persist-before-report RunRecord behavior.
- Added test-first CLI parsing, exit-code, temporary-project, mismatch, Tool-failure persistence, and restart-safe replay coverage.
- Verified 7 focused CLI tests and the full 97-test regression suite, with only 2 existing Windows symbolic-link setup skips.
- Completed and replayed an actual-repository `README.md` run with a Verified decision and exit code 0.
- Promoted Delivery Gate 6 Workspace and Project Brain Foundation to the sole `Specified - Next` gate.
- Clarified the RED-to-GREEN workflow: expected missing implementation proceeds when the test contract matches the active task, accepted decisions, Architecture, and repository settings; unrelated or authority-expanding failures are separated.
- Published the Gradle 9 and integrated execution-foundation hardening to `origin/main` in commit `b504ba4`.
- Removed the Gradle 9 automatic-test-framework dependency deprecation by declaring the JUnit Platform Launcher explicitly.
- Made Gradle tests use a workspace-local default temporary directory with an explicit `testTmpDir` override.
- Isolated Tool invocation workers so an interrupt-ignoring timed-out Tool cannot starve later work, and bounded timeout values to consistent execution/audit representations.
- Extended Evidence and RunRecord integrity digests across envelope version, timestamp, declared length, and payload/content metadata.
- Rejected malformed RunRecord Unicode instead of replacing it and added strict, bounded, real-root-contained startup context loading.
- Corrected oversized no-persistence read failure classification and removed all production Java serialization lint warnings.
- Added regression coverage for starvation, timeout edge values, metadata tampering, Unicode loss, startup-document size/encoding/containment, and evidence-capability errors.
- Fast-forwarded the verified Gate 4 and Agent orchestration delivery commit `f731afc` into `main` and published it to `origin/main`.
- Translated selected Archon `263cf365` and meta-harness `ccab9a6` orchestration patterns into provider-neutral Enhancer documentation without adding either repository as a dependency.
- Defined the escalation path from one worker through sequential, Producer-Reviewer, bounded fan-out/fan-in, routing or supervision, and shallow hierarchy only when justified.
- Added immutable common snapshots, typed handoffs, single terminal-state ownership, dependency and lease controls, idempotency, replay, bounded budgets, diagnostic-only telemetry, and verified-only completion invariants.
- Assigned adopted contracts to Delivery Gates 6 through 15 and expanded the Planned Gate 13 scope without changing capability maturity or displacing Gate 5 First Operational CLI.
- Added explicit rejected patterns covering prompt/file-based authority, direct peer control, shared-worktree parallel mutation, optional verification, self-reported completion, unlimited execution, and silent evidence loss.
- Verified both pinned GitHub reference links, canonical Roadmap structure, Planner behavior, and the full 82-test regression suite with the existing Windows symbolic-link setup skip.

## 2026-07-14

- Bound the exact Tool execution policy to the governed Agent run result and removed replaceable policy input from finalization.
- Restricted `AgentRunResult` construction and hardened RunRecord lifecycle invariants against impossible or historically false audit records.
- Integrated Delivery Gate 4 sequential independent verification and durable RunRecord replay.
- Added typed verification status/reason contracts and deterministic complete-content read verification over inline or referenced evidence.
- Preserved executed requests across terminal Agent state and allowed only verified finalization to create `COMPLETED`.
- Added typed policy snapshots and RunRecords with external expected digests, iterations, evidence, decisions, and worker/final stop reasons.
- Added atomic versioned SHA-256 RunRecord envelopes, strict UTF-8 replay, and missing/corruption detection.
- Added persist-before-return completion gating and durable records for failed, stagnated, and maximum-iteration runs.
- Promoted Delivery Gate 5 First Operational CLI to `Specified - Next`.
- Published the Gate 1-3 governed Agent execution foundation, self-hosting recovery, long-term vision, and documentation alignment to `origin/main` in commit `3fcda4c`.
- Corrected Gate 3 evidence wording to distinguish the governed temporary-repository integration test from separate actual-Enhancer Context and Planner regressions.
- Separated current build dependencies from planned Spring Boot, local-model, CLI, and editor integrations.
- Clarified product milestones versus dependency-ordered Delivery Gates and distinguished self-hosting development from local or hybrid model execution.
- Synchronized the project-overview startup order with the required `.ai/`-first bootstrap sequence and replaced stale foundation checklist states.
- Added V1 development-experience, V2 Agent-platform, and V3 AI-OS product milestones.
- Defined AI Kernel responsibilities and provenance-preserving Project Brain Decision, Architecture, Dependency, Task, and Execution graphs.
- Distinguished Agent plugins, Skills, Tools, and workflows and added marketplace and workflow approval boundaries.
- Added privacy-aware local/remote Model Router direction with sensitive-code-local defaults.
- Accepted the event-driven Enhancer OS target with Workspace, Project Brain, messaging/IPC, Agent Runtime, MCP, Skills, plugins, model routing, scheduling, interfaces, and governed Cloud Sync.
- Reordered future delivery gates so verification and the first CLI precede Workspace, Event Bus, runtime, MCP/model, Skill, plugin, interface, multi-agent, sync, and self-improvement tracks.
- Defined Event Bus semantics, Message Bus delivery, IPC transport separation, and queue-only Agent collaboration as the target architecture.
- Hardened Gate 3 with repository-derived `ApprovedTask` identity, explicit approval evidence, and Tool-name scope.
- Added structured Tool failure codes and a standard retry policy for timeouts and explicit temporary failures.
- Added evidence content digests and semantic progress detection independent of opaque references and prose summaries.
- Restricted `AgentRunState` construction to governed factories and controller-owned transitions.
- Integrated Delivery Gate 3 with approved Agent run state, Tool-result-driven transitions, and shared bounded termination behavior.
- Added `AWAITING_VERIFICATION` so Tool success cannot claim completion before the sequential independent verifier.
- Added external retry/terminal classification and canonical progress fingerprints for deterministic stagnation detection.
- Added governed temporary-repository Context-to-ReadFileTool-to-evidence-to-loop coverage plus terminal-failure and denied-mutation safeguards.
- Integrated Delivery Gate 2 with UUID evidence identities, atomic versioned filesystem envelopes, restart-safe resolution, and SHA-256/length/UTF-8 integrity checks.
- Added explicit maximum-content and retention-duration policy without automatic cleanup or destructive deletion.
- Connected large read-only Tool output to bounded `VerificationEvidence` and a resolvable complete-output reference through `EvidenceRecorder`.
- Added focused persistence, uniqueness, missing, corruption, invalid-encoding, and real large-file integration tests.
- Restored self-hosting planning compatibility by replacing the retired Phase/Ready parser with the canonical Delivery Gate/Specified - Next grammar.
- Added actual Enhancer Roadmap regression coverage and mapped gate scope and exit criteria into structured task proposals.
- Expanded executable startup context from eight root documents to seven `.ai/` documents followed by eight canonical root documents.
- Integrated Delivery Gate 1 with immutable Tool requests, execution policy, cancellation, timeout, a unique Tool registry, and bounded structured failure conversion.
- Added a real UTF-8 read-only file Tool with normalized and real-path containment, size limits, strict decoding, and no filesystem mutation.
- Added test-first request, policy, executor, and temporary-file integration coverage and promoted Gate 2 evidence persistence to the next task.
- Replaced ambiguous implementation roadmap labels with capability maturity and 12 dependency-ordered delivery gates.
- Redirected the next product task from an isolated verifier contract to a real read-only Tool Execution Boundary and E2E promotion track.
- Added explicit integration consumers, evidence, and exit criteria for post-foundation contracts.
- Published the delivery-gate roadmap realignment to draft PR #2.
- Published the governed Agent Loop foundation on `agent/governed-agent-loop-foundations` and opened draft PR #2.
- Restored Git metadata for the active C:\Enhancer worktree from a validated no-checkout clone without changing working files.
- Reconstructed the Git index from HEAD, verified repository identity and object integrity, and confirmed all 1,479 non-.git files remained byte-identical.
- Replaced Constitution 1.0.0 with a deduplicated, versioned 1.1.0 normative Kernel.
- Added explicit lifecycle states, scoped authorization, fresh-evidence rules, self-hosting safeguards, recovery requirements, and protected semantic-versioned amendments.
- Delegated detailed technology and component guidance to Architecture and RFCs and synchronized Agent, `.ai`, RFC-0001, and session-prompt rules.
- Verified all 15 required Constitution sections, confirmed obsolete implementation details are absent, and reran all 25 product tests successfully.
- Removed the standalone `examples/` directory and consolidated conceptual examples into specifications and executable examples into tests.
- Updated Constitution, README, architecture, roadmap, decision, state, and handoff documents for the smaller repository structure.
- Verified the unchanged product code with all 25 tests passing after removal.
- Implemented bounded Tool result verification evidence without real Tool execution.
- Added 512-character summaries, 4096-character diagnostic tails, truncation metadata, and complete-output references.
- Added explicit Tool success/failure status with optional exit-code consistency rules.
- Added 8 Tool contract tests and verified all 25 repository tests with no failures, errors, or skips.
- Confirmed the selected external agent-harness patterns are compatible with `.ai/` under staged, provider-neutral adoption.
- Added the ordered pattern adoption plan, including a sequential independent verifier, to the roadmap and decision log.
- Implemented bounded repeated Agent Loop termination with completed, failed, maximum-iteration, and stagnated reasons.
- Added immutable loop state/result contracts, configurable 20/3 defaults, deterministic termination precedence, and invariants.
- Added 9 Agent Loop tests and verified all 17 repository tests with no failures, errors, or skips.
- Accepted selective, provider-neutral adoption of high-value MoAI-ADK patterns without adding it as a dependency.
- Implemented a deterministic, read-only Assisted Development Loop that composes context reading and task planning once.
- Added explicit proposal-available and active-task-preserved outcomes with result invariants.
- Added 3 loop tests and verified all 8 repository tests with no failures, errors, or skips.
- Staged repeated-loop termination, verification evidence, Skill loading, artifact provenance, token budgets, and self-improvement for their owning roadmap slices.
- Added a Gradle 8.4 Wrapper and project-local Microsoft OpenJDK 17 setup workflow.
- Added PowerShell setup and Gradle launcher scripts for reproducible Windows builds.
- Verified compilation and all 5 tests through the Wrapper.
- Accepted repository Skill authoring and least-privilege permission rules.
- Added memory distillation, test-first scope, and fresh verification evidence requirements.
- Added a Proposed-only Skill catalog without activating unimplemented Skills.
- Synchronized `.ai`, session prompts, README, architecture chapters, and related RFCs.
- Clarified that verification cycles do not require automatic commits.

## 2026-07-12

- Implemented the deterministic Repository Task Planner.
- Added structured task proposals with explicit proposal state, scope, acceptance criteria, exclusions, and risks.
- Added Planner tests for ready roadmap selection, active-task protection, and incomplete roadmap risk reporting.
- Added the Java 17 Gradle project structure for the first product slice.
- Added Gradle build output exclusions.
- Implemented the Repository Context Reader with ordered UTF-8 document loading and clear missing-document errors.
- Added JUnit 5 tests for successful context reads and missing required documents.
- Added `.editorconfig` to declare UTF-8 repository text encoding.
- Added `.gitattributes` to keep repository text normalization stable.
- Replaced the `.ai/workflow.md` Korean startup sentence with ASCII English to avoid console encoding display issues.

## 2026-07-10

- Added repository-backed project memory documents.
- Added Codex session prompt templates.
- Established source-of-truth and session handoff rules.
- Added self-hosting AI Development Operating System vision.
- Added 30-day milestone for repository-context-based task proposal.
- Created required repository folders: `docs/`, `examples/`, `.ai/`, and `src/`.
- Added AI-only operating documents under `.ai/`.
- Defined the first self-hosting implementation task: Repository Context Reader.
- Added Document Driven Development workflow as the project operating process.
- Added Codex-ready feature specification documents under `docs/`.
- Added shared coding, architecture, and review prompts.
- Added Agent Loop, Tool, and Skill concept examples.
- Created initial local Git commit for project memory bootstrap.
- Added open source operating model and long-running Sprint-based project direction.
- Added explicit ChatGPT session resume protocol and prompt.
- Added Prompt Book sections for Codex, Claude, and GPT to chapter documents.
- Renamed local branch to `main` and configured GitHub remote `origin`.
- Pushed `main` to `origin/main`.
- Documented the startup rule: always read `.ai/` before starting work.
- Recorded the `.ai/` startup rule as an accepted decision.
- Added RFC-style design track under `docs/rfcs/`.
- Added six-month AI Development OS roadmap.
