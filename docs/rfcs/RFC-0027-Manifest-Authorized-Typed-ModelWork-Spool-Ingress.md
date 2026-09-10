# RFC-0027: Manifest-Authorized Typed ModelWork Spool Ingress

Status: Accepted

## Purpose

Define the smallest supported file-spool publisher and receiver for one deterministic-
fake `ModelWorkPayload`. The boundary makes the already canonical transport-spool v2
family usable between explicit producer and Scheduler processes while keeping the
RFC-0024 submission manifest, rather than CLI text, profile data, or transport content,
as the source of queue capacity, priority, and authoritative capability.

This RFC specifies a later implementation. It does not publish or receive a point,
execute work, invoke a model, select a provider, enable a background service, or
authorize network, credentials, spend, MCP, remote transport, or a durable schema
change.

## Relationship To Existing Contracts

RFC-0018 owns the complete typed payload, payload-sensitive message-envelope v2 and
transport-spool v2 formats, manifest v3, queue v4, and the separation between
`ModelExecutionProfile.requiredCapability` and `WorkItem.requiredCapability`.
RFC-0019 through RFC-0023 own fresh preparation, suitability, exact invocation,
process execution, evidence, Model RunRecord v2, finalization, retry, and recovery.

RFC-0024 owns the deterministic-fake caller intent, derived identities, governed first-use
context and snapshot, fixed repository-owned `deterministic-echo` capability, exact
manifest construction, and manifest-first replay. RFC-0025 owns the separate supported
foreground Scheduler execution composition. RFC-0026 owns the strict complete-profile
file and the direct submission command.

RFC-0027 adds only a second RFC-0024 consumer that stops after manifest persistence and
transport publication, plus a receiver that authorizes durable Scheduler admission from
that exact manifest. It does not replace or change direct submission.

## Why A Separate Transport Path Is Selected

Direct RFC-0026 submission remains the smallest same-process operator path. A file-spool
path is still required for the bounded deployment shape in which the governed producer
and the single Scheduler queue writer are separate local processes and cannot share an
in-process call. The spool is a transport adapter for the same exact typed envelope; it
is not another source of submission authority.

The selected path is optional and explicit. An operator chooses either direct
submission or manifest-backed spool publication for a submission identity. Both may
resolve the same immutable manifest, but they must not be treated as one combined
submit-publish-execute action. No directory watcher, polling loop, daemon, remote
adapter, or automatic fallback is implied.

## Separate Supported Commands

The new commands are exactly:

- `scheduler-spool-deterministic-fake-model-work`; and
- `scheduler-receive-deterministic-fake-model-work`.

They do not widen `scheduler-submit-deterministic-fake-model-work`,
`scheduler-spool-work`, or `scheduler-receive-work`. The legacy Work receiver keeps
requiring `WorkPayload` and its existing caller-supplied capability, priority, queue,
and destination inputs; it must continue rejecting `ModelWorkPayload`.

## Typed Publisher Input

`scheduler-spool-deterministic-fake-model-work` requires exactly these single-valued
options:

- `--project-root <path>`;
- `--submission-root <path>`;
- `--transport-spool-root <path>`;
- `--task-id <bounded active task identity>`;
- `--submission-id <canonical UUID>`;
- `--max-work-items <1..4096>`;
- `--max-pending-publications <1..4096>`;
- `--producer <bounded producer identity>`;
- `--target-path <bounded project-relative prompt path>`;
- `--expected-response-sha256 <64 lowercase hexadecimal characters>`;
- `--model-execution-profile-file <project-relative profile path>`; and
- `--priority NORMAL|EXPEDITED`.

The profile reader and every caller-owned submission field use the exact RFC-0026
syntax, validation, containment, no-link, UTF-8, size, and no-default rules. Profile
validation and intrinsic request validation occur before manifest or spool access.

There is no capability, queue identity, destination, correlation identity, logical-run
identity, occurrence time, snapshot, Tool scope, execution policy, candidate, gateway,
provider, endpoint, credential, price, network, or process-configuration input.
`max-pending-publications` bounds only pending transport points and grants no queue or
execution authority.

## Manifest-Only Preparation

The later implementation extracts one package-local RFC-0024 manifest preparation
boundary shared by direct submission and spool publication. It accepts the same exact
validated deterministic-fake request and:

1. derives the RFC-0024 submission, queue, correlation, and logical-run identities;
2. point-resolves the submission manifest before clock or repository access;
3. on first use, reads the exact active task and repository context, requires
   `model-invoke`, captures one occurrence time and Workspace snapshot, constructs
   the complete typed envelope, and constructs the exact manifest with the closed
   `deterministic-echo` capability;
4. validates every caller-owned and derived value against that manifest; and
5. stores or exact-replays the manifest before returning it.

The direct RFC-0026 composition continues from that boundary into the existing durable
submission service. The new publisher instead sends exactly:

```text
TransportMessage(
    destination = queue(manifest.queueId),
    envelope = manifest.workMessage)
```

It never opens, creates, recovers, or admits a Scheduler queue. The route is derived
from the manifest queue identity and cannot be selected by the caller.

## Publication Semantics

The unchanged `FileSpoolMessageTransport` owns publication. `ACCEPTED` means only
that one complete canonical transport point became durable for the hop.
`BACKPRESSURED` and `UNAVAILABLE` publish no point. None of these statuses means
Message Bus delivery, queue admission, execution, verification, or completion.

Manifest persistence precedes every publication attempt. Therefore a backpressured,
unavailable, or interrupted send may leave an exact manifest without a transport point.
Re-entry resolves that manifest without re-reading context or time and may safely retry
publication.

Each accepted retry intentionally creates a fresh random transport point under the
existing adapter. An uncertain response can therefore leave more than one exact point
for the same message. Receiver admission is idempotent, and each named point can be
acknowledged separately, but RFC-0027 makes no single-point or exactly-once publication
claim. A deterministic publication receipt, outbox, scan, cleanup policy, or bound on
acknowledged history requires separate durable-format and retention authority.

## Typed Receiver Input

`scheduler-receive-deterministic-fake-model-work` requires exactly:

- `--transport-spool-root <path>`;
- `--message-file <canonical UUID.transport filename>`;
- `--submission-root <path>`; and
- `--queue-root <path>`.

It accepts no project root, task, profile, capability, priority, capacity, queue
identity, destination, producer, target, digest, clock, execution configuration, or
provider input. It never scans a spool, manifest, or queue root. The named pending or
acknowledged point is the only transport locator, and the decoded message identity is
the only manifest locator.

## Manifest-Authorized Receive Order

The receiver performs this fail-closed order:

1. resolve exactly one same-root regular non-symbolic pending `.transport` or retained
   acknowledged `.received` point and reject pending/acknowledged collision;
2. read no more than the canonical transport-frame maximum plus one overflow byte
   through the existing no-follow filesystem primitive, rejecting root, ancestor, or
   final-component link/reparse indirection, then decode the canonical frame and require
   `ModelWorkPayload`;
3. point-resolve manifest v3 by the canonical envelope message identity;
4. require exact equality between `manifest.workMessage` and the transported
   envelope;
5. require the destination to equal `queue(manifest.queueId)`;
6. require the manifest capability to equal the closed deterministic-fake producer
   capability, without consulting the profile;
7. resolve or create only `manifest.queueId` with exactly
   `manifest.maxWorkItems`;
8. deliver the unchanged envelope through one fresh real Message Bus queue subscriber
   using only `manifest.requiredCapability` and `manifest.priority`;
9. require exact durable admission or exact no-revision replay; and
10. only then atomically rename a pending point to its no-replacement same-directory
    `.received` sibling.

The manifest supplies the entire envelope, queue identity, capacity, capability, and
priority. The transport supplies an independently checked copy of that envelope and its
derived route. The CLI supplies only storage locations and one exact point name.

Manifest integrity is not generic authentication. The receiver accepts this manifest
only because the command is deterministic-fake-specific and the exact artifact must
have been created or replayed through the closed RFC-0024 preparation boundary. A
future general, remote, provider-selected, or multi-capability receiver requires its own
authenticated producer and trust contract.

## Capability And Profile Separation

The complete profile remains untrusted requirements data. Its capability label is
never copied into the manifest authority field, compared as pre-approval, normalized,
or used to choose a receiver. The receiver does not accept capability text and does not
derive authority from message, route, profile, model class, candidate, producer, path,
environment, or repository content.

The exact manifest `requiredCapability` becomes the unchanged WorkItem projection.
Profile disagreement remains durable and observable only when RFC-0016 performs fresh
pre-call admission during a later, separately invoked RFC-0025 execution command.

## Recovery Prefixes

The supported recovery table is:

| Durable prefix | Re-entry outcome |
|---|---|
| Before manifest | No spool or queue exists; first use may begin again. |
| Manifest only | Publisher exact-replays intent and may retry one transport send. |
| Manifest plus one or more pending points | Each explicit point can be received independently; queue admission occurs at most once. |
| Queue created but work absent | Receiver resumes exact admission from the manifest and point. |
| Work admitted but point pending | Receiver exact-replays admission without queue revision, then acknowledges that point. |
| Point acknowledged after admission | Re-entry revalidates point, manifest, route, queue, and admission, reports `ALREADY_ACKNOWLEDGED`, and mutates nothing. |
| Output lost after publisher acceptance | Operator uses the returned point if retained; otherwise an exact publisher retry may create another bounded point. No scan is authorized. |
| Output lost after receive acknowledgement | The same requested `.transport` name resolves its `.received` sibling and exact-replays. |

Publication and receiver commands own no concurrent background lifecycle. The operator
must finish receive recovery before invoking a Scheduler worker against that queue.
RFC-0027 does not claim safe simultaneous queue recovery by an unrelated process.

Missing, malformed, unsupported, corrupt, cross-family, wrong-kind, wrong-route,
foreign, changed, noncanonical, outside-root, symbolic/reparse, non-regular, collision,
manifest-missing, manifest-mismatch, capability-mismatch, queue-capacity mismatch,
queue-content conflict, and acknowledgement collision states fail before the next
mutation. A point is never acknowledged on failed authorization or admission.

## Output And Failure Classification

Both commands use bounded stable key-value output and bounded redacted diagnostics.
They do not print profile fields or paths, capability, Tool scope, target content,
target digest, prompt, expected response, manifest bytes, credentials, or full
arguments.

The publisher reports transport status, exit code, submission and queue identities,
optional message filename, bounded refusal reason, manifest-created status, and
Workspace snapshot identity. The receiver reports admission/replay status, spool
acknowledgement status, exit code, submission, queue, message and WorkItem identities,
queue revision, retained priority, and acknowledged filename.

Command syntax, intrinsic values, profile input, point-name containment, route/payload
contract, and semantic identity conflicts are usage/configuration failures. Corrupt or
unsupported durable bytes, filesystem I/O, atomic publication/rename failure, and
store failure remain internal failures. `BACKPRESSURED` and `UNAVAILABLE` remain
ordinary explicit transport outcomes with no partial point.

## Durable Format Sufficiency

No new durable field or schema is required. Message-envelope v2 and transport-spool v2
already carry the exact typed envelope. Manifest v3 already binds the envelope to queue
identity, capacity, independent capability, and priority. Queue v4 already provides
exact idempotent typed admission. Runtime v5, process spool v2, pending-finalization v2,
Model RunRecord v2, Result, and runtime-event formats are downstream and unchanged.

The acknowledged filename is retained transport state, not an admission receipt or
general consumer offset. The queue itself is the durable admission fact. Stronger
exactly-once publication, durable discovery, consumer offsets, dead-letter state,
cleanup/retention, remote authentication, or cross-store atomicity would require a
separate accepted compatibility decision.

## Required RED-First Implementation Sequence

A later implementation task is sequential:

1. extract a shared package-local manifest-preparation boundary under RED tests, proving
   RFC-0026 direct submission and every existing replay/recovery byte remain unchanged;
2. add failing typed-publisher CLI/parser and source/locality tests for the exact option
   set, strict profile ordering, derived route, absence of capability/destination/
   derived identities, and validation before durable access;
3. connect manifest-only filesystem preparation to the unchanged spool transport and
   prove manifest-before-point ordering, accepted/backpressured/unavailable outcomes,
   exact-manifest replay, uncertain-response duplicate points, and bounded output;
4. add failing typed-receiver domain/composition tests for exact manifest point
   resolution, envelope/route/capability/capacity binding, manifest-only authority,
   fresh Message Bus delivery, exact typed queue admission, and legacy non-widening;
5. connect the four-option receiver CLI and prove every crash prefix, pending/
   acknowledged replay, capacity release, link/collision/corruption refusal, no
   acknowledgement on failure, duplicate-point convergence, and bounded redaction; and
6. run a real-filesystem/JVM publisher -> receiver -> separately invoked RFC-0025
   cycle/drain/service path plus focused RFC-0018-through-RFC-0027, durable-format,
   Scheduler CLI, architecture, source/locality, and full Markdown-sensitive Java 17
   regression.

Observable behavior is test-first. RED failures are classified against this RFC and
the active implementation task before the minimum GREEN change. Each verified GREEN
increment is an ordinary local commit. Push, merge, release, deployment, and actual
external delivery remain separate authority.

## Rejected Alternatives

- Widening either legacy Work command is rejected because arbitrary CLI capability and
  priority cannot authorize typed first-use intent.
- Publishing after direct queue admission is rejected as a false ingress path: the
  receiver would only replay work that was already reachable.
- Accepting destination or queue identity is rejected because the manifest and
  submission identity already determine the only valid route.
- Accepting capability from CLI, profile, envelope, producer text, or candidate is
  rejected because each would let untrusted input self-certify authority.
- Creating the manifest at the receiver is rejected because transport bytes are not a
  governed capability source and first-use task/snapshot authority belongs to the
  producer.
- Combining publish, receive, and execute is rejected because hop acceptance, durable
  admission, invocation, verification, and completion are distinct recoverable facts.
- Claiming exactly-once publication is rejected because the existing transport creates
  a fresh point per accepted send and has no durable outbox receipt.
- Adding a new receipt, outbox, schema, scanner, dead-letter store, or cleanup policy is
  rejected until evidence demonstrates that the at-least-once bounded path is
  insufficient.

## Exclusions

- Java or test implementation in this specification task;
- actual typed publication, receive, queue mutation, Scheduler execution, or model call;
- changes to direct submission or legacy Work command behavior;
- profile defaults, partial input, capability inference, or ambient discovery;
- provider/router/registry, endpoint, network, remote transmission, credentials,
  pricing, spend, MCP, or background processing;
- new schema, migration, receipt, outbox, sidecar, consumer offset, dead-letter,
  cleanup, retention, or cross-store transaction;
- new runtime event, cancellation propagation, RunRecord provenance, or execution
  policy; and
- push, merge, release, deployment, permission change, destructive cleanup, or other
  external effect.

## Codex Implementation Prompt

Implement RFC-0027 RED-first through the six ordered increments above. Preserve exact
RFC-0026 direct submission and all legacy Work behavior. Prepare and persist the exact
RFC-0024 manifest before publishing `queue(manifest.queueId)` plus
`manifest.workMessage`; do not touch the queue in the publisher. In the receiver,
accept only the four locator/root options, point-resolve the manifest by message ID,
require exact envelope, route, fixed-capability, and capacity binding, admit using only
manifest authority, and acknowledge only after durable admission. Prove bounded
at-least-once duplicate-point convergence without claiming exactly-once publication.
Do not add providers, network, credentials, schemas, background execution, automatic
discovery, or implicit Scheduler execution.
