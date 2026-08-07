# Enhancer

Enhancer는 Cursor Clone이 아니다.

Enhancer는 AI Development Operating System을 만드는 Self-hosting 프로젝트이다. 초반에는 ChatGPT와 Codex가 프로젝트를 만들지만, 일정 수준 이후에는 Enhancer 자체가 Repository Context를 읽고 현재 상태를 파악하며 다음 Task를 제안하고 개발을 보조하는 구조를 목표로 한다.

## 30-Day Goal

30일 후 목표는 사용자가 모든 다음 작업을 직접 정하지 않아도 Enhancer가 Repository 문서를 기반으로 다음 작업을 제안할 수 있는 수준에 도달하는 것이다.

## Current Development Maturity

Enhancer today covers authority-preserving planning, bounded read-only Tool execution, durable integrity-checked evidence, Tool-result-driven Agent Loop transitions, sequential independent verification, and replayable RunRecords, exposed as a narrow vertical slice through a local CLI. Workspace and Project Brain sit above that: every governed run reports its snapshot identity, observations (documents, prior run records, the run target, and Git state), memory freshness, and bounded graph/impact counts. Diagnostics, terminal, and selection observation belong to the Gate 12 interfaces that own those sources. A reference-only message envelope and deterministic in-process delivery, including finite non-blocking pending-queue backpressure, sit alongside a transport-neutral IPC interface, a governed Work publisher over the local file-spool adapter, and one supported point receiver that carries the returned explicit Work point through the bus into an existing durable Scheduler queue; there is no directory-scanning or general-purpose durable message-bus service. The executable context reads `.ai/` before the canonical root documents, and the deterministic Planner is tested against the current Enhancer Delivery Gate Roadmap.

Enhancer is not yet the broader event-driven AI Development OS: LLM, production messaging, and multi-agent capabilities remain future gates. **Current gate maturity is in `PROJECT_STATE.md`** and the evidence behind it in `docs/verification-log.md`; this README does not restate it.

Use `ROADMAP.md` for the canonical promotion path: Specified → Contract Verified → Integrated → Operational → Released. A completed contract task must not be presented as an operational product capability.

Terminology is explicit: **self-hosting development** means Enhancer applies its governed workflow to the Enhancer repository, while **local or hybrid model execution** describes provider routing. Neither capability implies the other. V1-V3 describe product outcomes; dependency-ordered Delivery Gates define implementation and promotion order.

## Enhancer OS Direction

The target platform is event-driven rather than Chat -> Tool -> Stop. Enhancer OS is planned around Desktop, CLI, API, Workspace, Project Brain, Memory, MCP Server/Client, Agent Runtime, Event/Message Bus with IPC adapters, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.

Planner, Coder, Reviewer, Tester, and Memory roles will communicate through typed queues rather than direct Agent calls. Workspace will provide governed file, Git, diagnostic, terminal-metadata, and selection context. MCP will allow Claude Code, Cursor, VS Code, and other clients to share the same approved Tool and memory layer.

The owner's rough 20-25% foundation estimate is directional planning context, not a verified completion percentage. Current verified maturity remains defined only by `PROJECT_STATE.md` and the delivery gates.

Enhancer evolves through three product milestones:

- **V1 AI Development Experience:** Cursor-level productivity through shared CLI, editor, Desktop, API, and Workspace surfaces.
- **V2 AI Development Platform:** Agent Runtime, workflows, Skills, Memory, MCP, model routing, plugins, and marketplace foundations.
- **V3 AI Operating System:** AI Kernel, Project Brain knowledge graphs, multi-agent scheduling, hybrid local/remote models, plugin ecosystem, governed synchronization, and self-improvement.

Git and canonical Markdown remain durable truth. Project Brain adds rebuildable Decision, Architecture, Dependency, Task, and Execution graphs with provenance and freshness, enabling impact reasoning without replacing their sources.

## Open Source Operating Model

Enhancer는 문서만 만드는 프로젝트가 아니다. GitHub에서 실제 오픈소스 프로젝트 수준으로 운영할 AI Development Operating System 프로젝트이다.

프로젝트 산출물은 다음을 모두 포함한다.

- 문서
- 코드
- ADR / Decision Log
- 테스트
- 명세에 포함된 예시와 실행 가능한 테스트
- Codex Prompt
- Claude Prompt
- GPT Prompt

운영 방식:

- 문서와 코드를 Git으로 버전 관리한다.
- 챕터별 Markdown 명세를 유지한다.
- Sprint 단위로 구현한다.
- ADR을 통해 설계 변경 이유를 보존한다.
- 리뷰 후 승인한다.
- Codex가 구현하고, ChatGPT가 아키텍처 리뷰와 문서 설계를 보조한다.

## Source Of Truth

대화는 기억이 아니다. Git Repository가 기억이다.

모든 AI Agent는 작업 전에 Repository 문서를 읽고, 작업 후에는 문서를 최신 상태로 갱신해야 한다.

## Start A Session

Codex를 프로젝트 루트에서 실행한 뒤 다음 프롬프트를 사용한다.

```text
항상 .ai 폴더를 읽고 시작해.
prompts/SESSION_START.md를 읽고 실행해라.
아직 코드는 수정하지 마라.
```

`.ai/` 폴더는 AI 전용 운영 문서이다.

- `.ai/constitution.md`
- `.ai/workflow.md`
- `.ai/coding_rules.md`
- `.ai/architecture.md`
- `.ai/prompt_rules.md`
- `.ai/memory.md`
- `.ai/skill_rules.md`

## Operational Read-Only CLI

The first supported command reads one UTF-8 file from a governed project, verifies its complete SHA-256 digest independently, persists evidence and a RunRecord, and prints only bounded metadata. `CURRENT_TASK.md` must be `In Progress`, its Task ID must match `--task-id`, and its Allowed Tools must contain `read-file`.

```powershell
$digest = (Get-FileHash -LiteralPath README.md -Algorithm SHA256).Hash.ToLowerInvariant()
.\scripts\gradle.ps1 run --args="run --project-root C:\Enhancer --task-id <active-task-id> --target-path README.md --expected-sha256 $digest --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records"
```

Replay the printed opaque reference without re-executing the Tool:

```powershell
.\scripts\gradle.ps1 run --args="replay --run-record-root C:\Enhancer\.enhancer\run-records --reference run-record/<uuid>"
```

Discover a bounded newest-first list of opaque references before choosing one to replay:

```powershell
.\scripts\gradle.ps1 run --args="run-record-list --run-record-root C:\Enhancer\.enhancer\run-records --limit 12"
```

`--limit` is required and must be from `1` through `48`. The status is `AVAILABLE` when
at least one reference is returned and `EMPTY` otherwise; both exit `0`. A missing
RunRecord root is empty and is not created. Listing never resolves or reinterprets a
record, so use `replay` on a returned reference to validate integrity and inspect its
bounded task, policy, verification, and stop metadata.

Exit codes are stable: `0` completed, `2` usage/configuration, `10` verification failed, `20` policy denied, `21` Tool failed, `30` stagnated, `31` maximum iterations, `40` terminal Scheduler work failure, and `70` internal failure. Every `run` that produces a record also reports `workspaceSnapshotId`, `workspaceObservations` (repository documents plus prior run records), a `memoryFreshness` matched/diverged/notObserved summary, and bounded Project Brain graph counts (`graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`); replay does not reproduce the snapshot identity because the RunRecord does not store it. Output is capped at 4096 characters and never includes complete file evidence. The example `.enhancer/` runtime directory is Git-ignored and is not removed by Gradle `clean`. `--evidence-root` and `--run-record-root` are explicit caller inputs and are not confined to the project root; each store refuses a symbolic-link root and only creates new UUID-named entries, so it can add files to the directory you name but cannot overwrite what is already there. For recovery, correct the reported configuration or target, retain the evidence and RunRecord roots, and use `replay` for any printed record reference before retrying with a new run.

## Submit Durable Scheduler Work

`scheduler-submit` persists one immutable submission intent and admits its exact
dependency-free work to a durable queue. It derives the approved task revision, allowed
Tools, and Workspace snapshot from the governed project, but every identity, occurrence
time, queue bound, capability, target, and expected digest remains an explicit input:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-submit --project-root C:\Enhancer --submission-root C:\Enhancer\.enhancer\submissions --queue-root C:\Enhancer\.enhancer\queue --task-id <active-task-id> --queue-id <canonical-queue-uuid> --max-work-items 256 --required-capability read-file --message-id <canonical-message-uuid> --correlation-id <correlation-id> --logical-run-id <logical-run-id> --producer local-operator --occurred-at 2026-07-22T00:00:00Z --target-path README.md --expected-sha256 <lowercase-sha256>"
```

The bounded status is `ADMITTED` when the queue revision advances and `REPLAYED` when
the exact submission is already present. `--priority` is the one optional input; it
accepts exactly `NORMAL` (the default when omitted) or `EXPEDITED`, and any other value
exits `2`. The command reports the effective `priority` on both statuses. Priority is
scheduler-only selection metadata that grants no Tool, task, or execution authority, and
it is part of the immutable submission content: reusing a message identity with a
different priority exits `2` without admitting changed work, so exact replay must reuse
the original priority. Preserve and reuse every argument to recover an interrupted
submission. Reusing a message identity with changed content or naming a task that does not
match the active repository task exits `2` without admitting changed work. The command
does not execute the work; invoke `scheduler-cycle` separately.

## Inspect Durable Scheduler Queue Status

`scheduler-status` reads one persisted queue snapshot without recovering or changing it:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-status --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --limit 12"
```

`--limit` must be from `1` through `48`. The command reports complete counts for
`READY`, `BLOCKED`, `ACTIVE`, `VERIFIED`, and `FAILED` work plus an admission-ordered
bounded identity/state/priority prefix. It also reports `maximumExpeditedBurst`,
`consecutiveExpeditedClaims`, and the optional `recoveryPreferredWorkItemId` exactly as
persisted, so the operator can inspect the selection inputs without consuming them. It
exits `0` with `AVAILABLE` or `EMPTY`; a missing queue is configuration exit `2`, and
corrupt state is internal exit `70`.

Inspection never calls queue recovery, creates no missing root, and reads no runtime,
effect, checkpoint, RunRecord, submission, or invocation store. `ACTIVE` means only that
the persisted queue snapshot contains an active slot; it does not prove that a worker is
currently alive. Use the execution commands and their retained roots for recovery.
The reported fairness and recovery fields describe the persisted snapshot only; they do
not predict which item a later claim will select.

## Inspect Durable Scheduler Recovery Status

`scheduler-recovery-status` correlates the queue with the single cycle checkpoint and
only the Goal and RunRecord named by that checkpoint:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-recovery-status --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --run-record-root C:\Enhancer\.enhancer\run-records"
```

The status is one of `NO_PENDING_CYCLE`, `INTENT_RECORDED`, `RUNTIME_RECORDED`,
`RUN_RECORD_RECORDED`, `RESULT_RECORDING_PENDING`, `RETRY_RESOLUTION_PENDING`,
`REPLACEMENT_RECORDED`, `QUEUE_DISPOSITION_PENDING`, or
`CHECKPOINT_CLEAR_PENDING`. Optional lines identify the correlated Goal, AgentRun,
replacement, queue state, runtime state, and RunRecord verification.

The command directly reads each store and never invokes recovery, clears a checkpoint,
scans runtime or RunRecord history, or creates a missing root. It takes a bounded second
sample and exits `70` rather than mixing values when the queue revision, checkpoint, or
runtime revision changes during inspection. `workerLiveness=UNKNOWN` is intentional:
the projection neither inspects a process nor interprets lease expiry. A missing queue
is configuration exit `2`; corrupt, missing referenced, inconsistent, or concurrently
changing recovery state is internal exit `70`.

## Inspect Scheduler External-Effect Recovery Status

`scheduler-external-effect-status` reuses the same checkpoint-correlated Scheduler
observation and inspects only that Goal's external-effect ledger and terminal evidence:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-external-effect-status --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --run-record-root C:\Enhancer\.enhancer\run-records --external-effect-root C:\Enhancer\.enhancer\effects --evidence-root C:\Enhancer\.enhancer\evidence --limit 8"
```

`--limit` must be from `1` through `8`. Complete counts cover `PREPARED`, `APPLIED`,
`DEDUPLICATED`, `COMPENSATED`, and `REQUIRES_USER_RECOVERY`; the bounded prefix reports
only idempotency key, durable status, and AgentRun identity. Evidence content is never
printed.

The aggregate status is `NO_CORRELATED_GOAL`, `LEDGER_NOT_RECORDED`,
`LEDGER_CREATION_PENDING`, `EMPTY_LEDGER`, `PREPARED_EFFECT_REQUIRES_RECOVERY`,
`USER_RECOVERY_REQUIRED`, `NON_COMPENSATED_EFFECT_RECORDED`, or
`ALL_EFFECTS_COMPENSATED`. Safety precedence matches retry refusal: ambiguous prepared
intent first, then explicit user recovery, then applied/deduplicated effects. Every
terminal effect must resolve to integrity-matching Evidence Store metadata.

The command never invokes an adapter, probes the external system, replays, compensates,
recovers, scans, or mutates a store. `externalSystemState=NOT_PROBED` is intentional.
Missing or corrupt referenced evidence, impossible bindings, and observed Scheduler,
runtime, or ledger drift exit `70`; a missing queue remains configuration exit `2`.

## Inspect Process-Isolated Invocation Recovery Status

`scheduler-invocation-status` reuses the checkpoint-correlated Scheduler observation
and reads only the correlated Goal/AgentRun private invocation namespace:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-invocation-status --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --run-record-root C:\Enhancer\.enhancer\run-records --invocation-root C:\Enhancer\.enhancer\invocations --limit 8"
```

The status is `NO_CORRELATED_CYCLE`, `RUNTIME_NOT_RECORDED`, `INVOCATION_ABSENT`,
`WORK_MESSAGE_ABSENT`, `WORK_MESSAGE_AWAITING_RESULT`, or
`RESULT_MESSAGE_PUBLISHED`. The 1-through-8 limit bounds reported work/result presence
metadata; payload, evidence, and RunRecord content are never printed. Corrupt, foreign,
symbolic-link, several-message, mismatched-result, or changing observations exit `70`.
The command never creates, consumes, launches, cleans, recovers, retries, scans, mutates,
or establishes child-process liveness.

## Migrate A Schema-V2 Scheduler Queue

Stop every Scheduler process that uses the queue root and identity before migration.
Then invoke the separate queue-scoped maintenance command:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-migrate-queue --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid>"
```

`ABSENT` means the named queue does not exist and creates no root. `ALREADY_CURRENT`
validates schema v3 without rewriting the queue artifact. `MIGRATED` losslessly replaces
one valid schema-v2 queue with schema v3: every identity, revision, capacity, logical-run
binding, exact admission and pending order, active item, terminal disposition, WorkItem,
envelope, capability, and dependency is retained. Existing work defaults to `NORMAL`,
maximum expedited burst `4`, fairness progress `0`, and no migration-time recovery
preference.

The command uses the queue-scoped writer lock, validates and rereads a same-directory
candidate, refuses source-byte drift, and atomically replaces only the unchanged
validated source. Corrupt, future, changed, or publication-failed input exits `70`;
every pre-publication failure leaves the source authoritative and removes the candidate
when possible. Ordinary queue status, recovery, cycle, and drain operations never
migrate schema v2 as a side effect. After `MIGRATED` or `ALREADY_CURRENT`, restart the
Scheduler and reinvoke the intended operation.

## Migrate A Legacy Scheduler Cycle Checkpoint

Stop every Scheduler process that uses the cycle-checkpoint root before migration. Then
invoke the separate maintenance command:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-migrate-cycle-checkpoint --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint"
```

`ABSENT` means no checkpoint exists and creates no root. `ALREADY_CURRENT` validates an
existing schema-v2 checkpoint without rewriting it. `MIGRATED` means one valid schema-v1
checkpoint was losslessly replaced by schema v2: Goal, AgentRun, and optional RunRecord
reference are retained, and the schema-v2 replacement AgentRun identity is absent.

The command validates the complete old artifact, writes and rereads a private candidate,
refuses observed source drift, and uses same-directory atomic replacement only after the
source bytes still match. Corrupt, future-version, changed, or publication-failed input
exits `70`; every failure before successful replacement leaves the source artifact
authoritative and removes the private candidate when possible. The command creates no
backup and provides no live old-writer, rollback, or parent-directory power-loss
guarantee. After `MIGRATED` or `ALREADY_CURRENT`, restart the Scheduler and reinvoke the
original recovery command.

## Recover One Durable Scheduler Cycle

`scheduler-cycle` recovers an already-existing durable Scheduler queue and runs exactly
one process-isolated Worker cycle. It does not create a queue, submit work, or poll. All
storage roots, identities, retry bounds, and durations are explicit caller inputs:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-cycle --project-root C:\Enhancer --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --external-effect-root C:\Enhancer\.enhancer\effects --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records --invocation-root C:\Enhancer\.enhancer\invocations --owner-id local-scheduler --max-attempts 2 --lease-millis 300000 --process-timeout-millis 30000"
```

The bounded result status is `IDLE`, `VERIFIED_COMPLETED`, or `FAILED`. Idle and
verified completion exit `0`; terminal failed work exits `40`. Missing queue state or
malformed input exits `2`, while corrupt state and unexpected execution/storage errors
exit `70`. Preserve every named root to resume a checkpointed cycle after interruption.
On restart, a checkpoint whose latest AgentRun already retains a terminal Result is
revalidated through that exact RunRecord reference before retry control or terminal
queue disposition. Reference drift fails closed with the checkpoint retained; correct
the conflicting artifact rather than resubmitting the work.

When the parent watchdog receives a typed process timeout, it persists one bound fact at
`<invocation-root>/.process-timeouts/<goal>/<agent-run>.process-timeout` before the
command exposes the execution error. Preserve that invocation root: exact reinvocation
resolves the same fact and fails without launching another child. The supported CLI does
not publish events by default. To publish the currently supported Scheduler-owned
process-timeout, lease-timeout, retry-decision, retry-start, verification, Tool-timeout,
stagnation, and terminal WorkItem facts from
`scheduler-cycle`, `scheduler-drain`, or `scheduler-service`, append the complete group
`--runtime-event-root <event-root>`,
`--runtime-event-publication-root <point-root>`, and
`--max-pending-runtime-event-publications <1..4096>`. Supply all three or none. The
shared composition writes the retained timeout fact first, then exact-appends
`RuntimeTimeoutKind.PROCESS`; lease recovery likewise persists its exact timeout record
before the derived event. Retry control persists each decision before
`RETRY_DECISION_RECORDED` and the replacement AgentRun before `RETRY_STARTED`.
Finalization persists or exact-replays the Result before verification, optional Tool
timeout and stagnation; terminal queue disposition persists before
`WORK_ITEM_TERMINATED`. Every event publishes only its opaque reference. Re-entry repairs
retained prefixes without another child, lease reclaim, retry decision, replacement
AgentRun, Result transition, RunRecord, or queue disposition, and an exact acknowledged
point is not recreated. One cycle can create several pending points; if the caller-
selected capacity fills between them, acknowledge a retained point and repeat the same
command to continue exact recovery. This optional mode does not publish authenticated
cancellation-application events.

AgentRuntime schema v4 retains the bounded lease-timeout and retry-decision histories
used by this supported recovery path. The same schema can
atomically apply one exact retained `CANCEL` only through a trusted
`ControlRequestAuthorizer`, then derive and repair `CANCELLATION_APPLIED`; envelope
metadata alone never authenticates. Runtime schemas v1 through v3 are unsupported and
require separate migration work.

## Drain Ready Scheduler Work

`scheduler-drain` uses the same recovery inputs as `scheduler-cycle` but invokes
sequential cycles in the foreground until it observes idle work, a failed disposition,
or the explicit cycle limit. It does not create a queue, submit work, wait for future
work, or poll:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-drain --project-root C:\Enhancer --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --external-effect-root C:\Enhancer\.enhancer\effects --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records --invocation-root C:\Enhancer\.enhancer\invocations --owner-id local-scheduler --max-attempts 2 --lease-millis 300000 --process-timeout-millis 30000 --max-cycles 8"
```

`--max-cycles` must be from `1` through `4096`. The bounded stop status is `IDLE`,
`FAILED`, or `LIMIT_REACHED`; `cyclesInvoked` includes the final idle or failed cycle.
The command continues only after `VERIFIED_COMPLETED`. `FAILED` exits `40`; `IDLE` and
`LIMIT_REACHED` exit `0`. `LIMIT_REACHED` does not prove that the queue is empty, so
another drain requires a new explicit operator invocation. Preserve every named root
when reinvoking after interruption so the existing per-cycle checkpoint can recover.

## Receive One Durable Work Spool

`scheduler-receive-work` point-resolves one retained transport artifact, publishes its
unchanged Work envelope through the real Message Bus into an existing durable Scheduler
queue, and acknowledges the point only after durable admission. It does not scan the
spool, create a queue, or execute work:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-receive-work --transport-spool-root C:\Enhancer\.enhancer\transport --message-file <canonical-message-file>.transport --destination-name scheduler-work --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --required-capability read-file-worker --priority NORMAL"
```

The input name must be a canonical UUID plus `.transport`. Exactly one regular
non-symbolic pending file or deterministic same-root `.received` file must exist and
contain the exact expected queue route and a `WorkPayload`. `ADMITTED` means durable
queue admission advanced its revision; `REPLAYED` means the exact WorkItem was already
durably admitted. `ACKNOWLEDGED` reports the post-admission atomic move to `.received`;
`ALREADY_ACKNOWLEDGED` reports exact retained-point re-entry without another move. Use
the original `.transport` input name for uncertain-result replay. Invoke
`scheduler-cycle`, `scheduler-drain`, or `scheduler-service` separately only when
execution is intended.

## Receive One Durable Control Spool

`scheduler-spool-control` derives one untrusted Control intent from an existing active
Goal with a current non-terminal AgentRun and publishes it to one explicit local spool:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-spool-control --runtime-root C:\Enhancer\.enhancer\runtime --goal-id <canonical-goal-uuid> --transport-spool-root C:\Enhancer\.enhancer\transport --destination-name runtime-controls --max-pending-publications 256 --message-id <new-canonical-message-uuid> --producer local-operator --occurred-at 2026-07-29T04:00:00Z --signal PAUSE --reason review-before-continuing"
```

The command reads runtime state directly and never performs runtime recovery or lease
reclamation. Correlation, logical-run, and causation come only from the Goal's retained
Work envelope; the caller owns the new message identity, producer, time, signal, and
reason. `ACCEPTED` means only that the file-spool hop retained the intent and returns
its exact `messageFile`. `BACKPRESSURED` and `UNAVAILABLE` return no point and create no
durable request. `CANCEL`, `PAUSE`, and `RESUME` remain untrusted data.

Pass an accepted filename to the separate receiver. `scheduler-receive-control`
point-resolves one retained transport artifact, publishes
its unchanged Control envelope through the real Message Bus, persists the exact request
in an existing active Goal ledger, and acknowledges the point only after persistence:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-receive-control --transport-spool-root C:\Enhancer\.enhancer\transport --message-file <canonical-message-file>.transport --destination-name runtime-controls --runtime-root C:\Enhancer\.enhancer\runtime --goal-id <canonical-goal-uuid>"
```

The input name must be a canonical UUID plus `.transport`. Exactly one regular
non-symbolic pending file or deterministic same-root `.received` file must exist and
carry the expected queue route plus a `ControlPayload` bound to that Goal's current Work.
`RECORDED` means the durable request revision advanced; `REPLAYED` means the exact
request was already present. `ACKNOWLEDGED` reports the post-persistence atomic move,
while `ALREADY_ACKNOWLEDGED` reports exact retained-point re-entry. Use the original
`.transport` input name after an uncertain result. A missing Goal or invalid request
leaves a pending point unacknowledged.

To additionally publish the opaque cancellation-request runtime-event reference, add
the complete optional publication group:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-receive-control --transport-spool-root C:\Enhancer\.enhancer\transport --message-file <canonical-message-file>.transport --destination-name runtime-controls --runtime-root C:\Enhancer\.enhancer\runtime --goal-id <canonical-goal-uuid> --runtime-event-root C:\Enhancer\.enhancer\runtime-events --runtime-event-publication-root C:\Enhancer\.enhancer\runtime-event-publications --max-pending-runtime-event-publications 256"
```

The three publication options are all-or-none, and capacity must be from `1` through
`4096`; partial or invalid configuration fails before creating artifacts. Only
`CANCEL` records and publishes `CANCELLATION_REQUEST_RECORDED`; `PAUSE` and `RESUME`
remain request-only and do not create either optional root. Ordering is durable request,
runtime event, opaque publication point, then spool acknowledgement. If event or point
publication fails, retain the same roots and original `.transport` input name: exact
re-entry repairs the missing suffix without rewriting the request, event, or existing
publication point. The publication directory is a bounded point surface, not an event
body consumer, scanner, acknowledgement queue, or authenticated control channel.

Read one explicitly named publication point through the separate read-only consumer:

```powershell
.\scripts\gradle.ps1 run --args="runtime-event-read --runtime-event-root C:\Enhancer\.enhancer\runtime-events --runtime-event-publication-root C:\Enhancer\.enhancer\runtime-event-publications --publication-file <64-lowercase-hex>.runtime-event-reference"
```

The command validates the point's regular non-symbolic schema-v1 integrity envelope,
its deterministic filename, the canonical `runtime-event/<goal>/<event>` grammar, and
the exact event in that Goal's retained stream. It reports bounded event identity,
kind, time, producer, Goal/AgentRun, task/snapshot/run/correlation provenance, stream
revision, and authoritative-reference count. It scans neither root and does not create
a missing event root. Exact repeated reads and failures leave both artifacts unchanged.
This command does not acknowledge or rename the point, release publication capacity,
apply the event, mutate runtime state, or claim delivery. Keep the explicit point until
it is explicitly acknowledged or a separately governed retention contract exists.

Acknowledge that exact resolved observation with the original pending filename:

```powershell
.\scripts\gradle.ps1 run --args="runtime-event-acknowledge --runtime-event-root C:\Enhancer\.enhancer\runtime-events --runtime-event-publication-root C:\Enhancer\.enhancer\runtime-event-publications --publication-file <64-lowercase-hex>.runtime-event-reference"
```

The command repeats the full point and event validation before atomically renaming the
pending point to the deterministic `.runtime-event-received` sibling. It reports
`ACKNOWLEDGED` on the first success and `ALREADY_ACKNOWLEDGED` when the same original
filename is retried after an uncertain response. The publisher recognizes that retained
sibling before capacity evaluation, does not recreate the pending point, and counts only
pending `.runtime-event-reference` files against capacity. Point/event contents and the
event-stream revision do not change. This acknowledgement proves only that this exact
observation boundary completed; it does not run a handler, apply an event, mutate
runtime state, delete evidence, scan either root, or define cleanup/retention.

These commands publish and record untrusted intent only. They do not authenticate or
apply cancel, pause, or resume, call Message Bus cancellation, interrupt a worker,
reclaim a lease, or mutate the Scheduler queue.

## Run The Bounded Scheduler Service

`scheduler-service` uses the same explicit recovery inputs as `scheduler-cycle` and runs
the finite service lifecycle on the invoking foreground thread. It may wait and check the
existing queue again, but it does not create a queue, submit work, daemonize, create a
thread, or persist separate service progress:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-service --project-root C:\Enhancer --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --external-effect-root C:\Enhancer\.enhancer\effects --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records --invocation-root C:\Enhancer\.enhancer\invocations --owner-id local-scheduler --max-attempts 2 --lease-millis 300000 --process-timeout-millis 30000 --max-cycles 64 --max-consecutive-idle-cycles 8 --idle-wait-millis 1000"
```

Both cycle limits must be from `1` through `4096`. `--idle-wait-millis` must be positive
and no greater than `3600000`. The bounded status is `STOP_REQUESTED`, `INTERRUPTED`,
`FAILED`, `CYCLE_LIMIT`, or `IDLE_LIMIT`; a total-cycle limit wins when both limits are
reached on the same idle cycle. `FAILED` exits `40`; every other bounded stop exits `0`.
The invoking thread's interrupt state is the local lifecycle stop signal, not an
authenticated pause/resume/cancel control. Console termination behavior remains
platform- and supervisor-dependent, so preserve every named root and reinvoke the same
command after an uncertain stop. The durable cycle checkpoint and runtime lease/fence
recover interrupted or expired work; no service-level checkpoint exists.

## Run The Explicit Two-Command Scheduler Workflow

The supported operator workflow keeps submission separate from every execution command.
There is no submission-and-execution wrapper; polling occurs only through an explicit
finite `scheduler-service` invocation:

1. Choose and retain every `scheduler-submit` argument before the first invocation.
2. Invoke `scheduler-submit` and stop if it exits nonzero. `ADMITTED` and `REPLAYED` both
   mean the exact work is durably present; neither status executes it.
3. After separately deciding to execute, invoke `scheduler-cycle` for exactly one cycle,
   `scheduler-drain` for immediately-ready work, or `scheduler-service` for bounded idle
   polling, using the same `--project-root`, `--queue-root`, and `--queue-id`. Preserve
   every execution-specific root for recovery.
4. Interpret the execution result independently. Another command invocation occurs only
   through another explicit operator action.

The queue root and identity are the handoff between the commands. Submission roots and
all submission identities/time must be retained for exact replay. Runtime, effect,
checkpoint, evidence, RunRecord, and invocation roots belong to cycle recovery and must
not be replaced after an interrupted cycle. Exact submission replay also requires the
governed repository documents used to derive the task revision and Workspace snapshot to
remain unchanged; changed authority or snapshot content under the same message identity
fails closed.

| Observed state | Operator action |
|---|---|
| Submission interrupted or produced no trusted result | Reinvoke `scheduler-submit` with every original argument against the unchanged governed project. Accept `ADMITTED` or `REPLAYED`; do not invoke the cycle while submission remains an error. |
| `ADMITTED` or `REPLAYED` | The work is durable but not necessarily executed. Use `scheduler-status` when queue inspection is needed, then invoke `scheduler-cycle`, `scheduler-drain`, or the finite `scheduler-service` separately only when execution is intended. |
| Queue inspection, recovery, cycle, or drain reports unsupported queue schema v2 | Stop every Scheduler process using that queue root and identity. Run `scheduler-migrate-queue` once with the retained root and queue UUID. Restart and reinvoke only after `MIGRATED` or `ALREADY_CURRENT`; on exit `70`, retain the unchanged source and do not execute the queue. |
| Cycle, drain, or recovery inspection reports an unsupported pending-finalization schema | Stop every Scheduler process using that cycle-checkpoint root. Run `scheduler-migrate-cycle-checkpoint` once against the retained root. Restart and reinvoke recovery only after `MIGRATED` or `ALREADY_CURRENT`; on exit `70`, retain the unchanged source and do not execute the cycle. |
| Cycle, drain, or service interrupted or exits `70` | Preserve the queue and every execution root. Run `scheduler-recovery-status` with those retained roots to identify the durable prefix without changing it. When it reports a Goal, run `scheduler-external-effect-status` before considering retry so prepared, user-recovery, applied/deduplicated, and compensated histories remain explicit; if a RunRecord reference is present, inspect it with `replay`. Correct only the reported environmental problem, then reinvoke the same execution command so the worker checkpoint and lease/fence can recover. Do not resubmit work to repair execution. |
| Cycle reports `VERIFIED_COMPLETED` | The WorkItem is terminally verified. Exact submission replay remains a no-op, and a later cycle for an otherwise empty queue reports `IDLE`. |
| Cycle reports `FAILED` and exits `40` | The WorkItem is terminally failed. Discover retained evidence with `run-record-list`, inspect a selected record with `replay`, and retain runtime state; resubmitting the same identity is not a retry. New work requires separately approved inputs and a new message identity. |
| Cycle reports `IDLE` | No ready work was executed. Do not loop automatically; use `scheduler-status` to distinguish an empty queue from blocked work and verify the queue root/identity and preceding submission result. |
| Drain reports `LIMIT_REACHED` | The requested number of cycles completed, but ready work may remain. Use `scheduler-status`, then explicitly invoke another cycle or drain only when intended. |
| Drain reports `FAILED` and exits `40` | The first terminal work failure stopped the drain. Use `run-record-list` and `replay` to inspect retained RunRecord evidence alongside runtime state before deciding whether separately approved new work is required. |
| Drain reports `IDLE` | The final cycle found no ready work. The command does not wait for later submissions or blocked dependencies to become ready. |
| Service reports `CYCLE_LIMIT` or `IDLE_LIMIT` | The finite service invocation ended normally. `CYCLE_LIMIT` does not prove the queue is empty; inspect with `scheduler-status` before a separately authorized next invocation. |
| Service reports `STOP_REQUESTED` or `INTERRUPTED` | The local foreground lifecycle stopped without adding authenticated control state. Preserve all roots and inspect recovery status before reinvoking if a cycle may have been interrupted. |
| Service reports `FAILED` and exits `40` | The first terminal work failure stopped the service. Inspect retained RunRecord and runtime evidence before deciding on separately approved new work. |

## Submit Generated-Input Scheduler Work

`scheduler-submit-generated` is a replay-safe variant of `scheduler-submit` for operators
who prefer to retain one identity instead of the whole replay tuple. It takes a single
caller-retained canonical submission UUID and derives the queue, correlation, and
logical-run identities from it through fixed versioned domain-separated transforms, and it
generates the occurrence time from the clock on first use. No explicit queue, message,
correlation, logical-run identity, or occurrence time is supplied, and the explicit
`scheduler-submit` command is unchanged:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-submit-generated --project-root C:\Enhancer --submission-root C:\Enhancer\.enhancer\submissions --queue-root C:\Enhancer\.enhancer\queue --task-id <active-task-id> --submission-id <canonical-submission-uuid> --max-work-items 256 --required-capability read-file --producer local-operator --target-path README.md --expected-sha256 <lowercase-sha256>"
```

The bounded status is `ADMITTED` when the queue revision advances and `REPLAYED` when the
exact submission is already present. Like `scheduler-submit`, it accepts the one optional
`--priority NORMAL|EXPEDITED` input, which defaults to `NORMAL` on omission and exits `2`
on any other value; the priority is caller-owned intent, so it is part of the replay tuple
and a later invocation supplying a different priority under the same submission UUID fails
closed before the clock or repository context is consulted. The output prints the generated
`queueId`, `correlationId`, `logicalRunId`, `occurredAt`, effective `priority`, and
`workspaceSnapshotId` for auditing; pass the printed `queueId` to `scheduler-cycle` or
`scheduler-drain`. On the first invocation the occurrence time and governed repository
snapshot are captured, the immutable submission manifest is persisted, and the queue is
created and the work admitted. On any later invocation the manifest is resolved before the
clock or repository context is consulted, so the exact occurrence time and envelope are
reused; changing any caller-owned intent (task, capacity, capability, producer, target,
digest, or priority) under the same submission UUID exits `2` without admitting changed
work.

A real-repository smoke run reads `README.md` and observes
`ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE` with one retained manifest, one
RunRecord, and no duplicate execution. Generated-input recovery follows the same handoff as
the explicit workflow, differing only in what the operator preserves:

| Observed state | Operator action |
|---|---|
| Submission interrupted before a trusted result | Reinvoke `scheduler-submit-generated` with the same submission UUID and caller-owned intent against the unchanged governed project. If the interruption preceded manifest persistence, a fresh occurrence time is generated safely because no durable work was created; if it followed persistence, the stored time and envelope are reused. Accept `ADMITTED` or `REPLAYED`. |
| `ADMITTED` or `REPLAYED` | The work is durable but not necessarily executed. Take the printed `queueId`, use `scheduler-status` when inspection is needed, and invoke `scheduler-cycle` or `scheduler-drain` separately only when execution is intended. |
| Conflicting intent exits `2` | The submission UUID already names durable work with different caller-owned intent. Do not reuse it for changed work; choose a new submission UUID. |
| Cycle `VERIFIED_COMPLETED` then a later cycle `IDLE` | The generated work is terminally verified; exact submission replay stays a no-op and the empty queue reports `IDLE`. |

Retain only the submission UUID, the caller-owned intent, and the submission/queue roots for
exact replay; the cycle-specific roots follow the same recovery rules as the explicit
workflow. Submission remains separate from execution, and there is no wrapper or polling.

### Migrate A Schema-V1 Submission Manifest

Stop submission for the named identity and migrate exactly one retained schema-v1
manifest before replaying it:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-migrate-submission-manifest --submission-root C:\Enhancer\.enhancer\submissions --submission-id <canonical-submission-uuid>"
```

The bounded result is `MIGRATED`, `ALREADY_CURRENT`, or `ABSENT`. Migration retains the
exact schema-v1 intent, assigns `NORMAL`, and publishes schema v2 only after validating
and rereading a same-directory candidate and confirming the source bytes did not change.
It does not create or recover a queue, admit or claim work, execute a worker or Tool, or
accept priority input. Ordinary submission resolution rejects schema v1, so do not use
either submission command on that identity until the explicit migration succeeds.

## Development Session Checkpoints

The development-session checkpoint workflow and its `checkpoint-start`,
`checkpoint-record`, `checkpoint-show`, and `checkpoint-clear` commands are owned by
`AGENTS.md` (Development Session Checkpoint Commands).

## Document-Driven Dynamic Increment Workflows

The dynamic increment workflow contract is owned by `AGENTS.md` (Dynamic Workflow
Rules).

## Adaptive Development Subagents

The adaptive development subagent policy is owned by `AGENTS.md` (Adaptive Development
Subagent Delegation).

## Development Setup

Windows에서 관리자 권한이나 전역 Gradle 설치 없이 개발 환경을 구성한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev.ps1
```

이 명령은 공식 Microsoft OpenJDK 17을 Git에서 제외된 `.tools/`에 구성하고 Gradle Wrapper 8.4로 전체 테스트를 실행한다. 이후에는 다음 명령을 사용한다.

```powershell
.\scripts\gradle.ps1 test
```

## Resume In A New ChatGPT Session

새 ChatGPT 세션은 도원님 PC의 로컬 `Enhancer` 저장소를 자동으로 읽을 수 없다.

따라서 새 세션을 시작할 때는 [prompts/CHATGPT_SESSION_RESUME.md](prompts/CHATGPT_SESSION_RESUME.md)를 사용한다.

필수로 전달할 파일:

- `CONSTITUTION.md`
- `AGENTS.md`
- `PROJECT_STATE.md`
- `CURRENT_TASK.md`
- `SESSION_HANDOFF.md`

설계 작업 시 추가:

- `ARCHITECTURE.md`
- `DECISION_LOG.md`

## Close A Session

작업을 종료하기 전에 다음 프롬프트를 사용한다.

```text
prompts/SESSION_CLOSE.md를 읽고 세션을 종료해라.
임의로 push하지 마라.
```

## Core Documents

각 사실은 정확히 하나의 문서가 소유한다. 다른 문서는 그 사실을 다시 적지 않고 참조한다.

- `CONSTITUTION.md`: 최상위 헌법
- `AGENTS.md`: AI Agent 작업 규칙
- `ARCHITECTURE.md`: 현재 아키텍처 (성숙도는 기술하지 않음)
- `PROJECT_STATE.md`: 검증된 현재 구현 상태, 성숙도 판단, 알려진 한계
- `docs/verification-log.md`: 그 상태의 근거가 되는 증분별 검증 기록 (append-only)
- `CURRENT_TASK.md`: 현재 단 하나의 Task와 다음 Task
- `ROADMAP.md`: 단계별 진행 계획
- `DECISION_LOG.md`: 승인된 설계 결정의 인덱스 (제목 + 승인 상태)
- `docs/decisions/`: 결정별 파일 하나씩 — 맥락·결정·근거·결과. 제목 문자열이 곧 결정의 식별자
- `SESSION_HANDOFF.md`: 지금 사실이면서 세션과 함께 사라질 내용만
- `CHANGELOG.md`: 변경 기록

## Codex-Ready Specification Documents

`docs/` 아래 문서는 설치 문서가 아니라 Codex, Claude, GPT에게 그대로 전달할 수 있는 Prompt형 프로젝트 명세서이다.

권장 진행 순서:

1. `docs/00-Project-Overview.md`
2. `docs/01-Development-Environment.md`
3. `docs/05-Memory.md`
4. `docs/11-Architecture.md`
5. `docs/02-Agent-Loop.md`
6. `docs/03-Tool-System.md`
7. `docs/04-Skill-System.md`
8. `docs/06-Planner.md`
9. `docs/07-MCP.md`
10. `docs/08-Multi-Agent.md`
11. `docs/09-Background-Agent.md`
12. `docs/10-Roadmap.md`

각 문서는 목표, 설계 기준, 구현 범위, 테스트 기준, Codex Prompt를 포함한다.

또한 각 챕터 끝에는 `Prompt Book` 섹션을 두고 `Codex Prompt`, `Claude Prompt`, `GPT Prompt`를 분리해서 제공한다.

## RFC Documents

대형 설계 주제는 `docs/rfcs/` 아래 RFC 스타일로 관리한다.

- `RFC-0001`: Constitution
- `RFC-0002`: AI Behavior Specification
- `RFC-0003`: Prompt Contract
- `RFC-0004`: Context Builder
- `RFC-0005`: Planner
- `RFC-0006`: Tool Specification
- `RFC-0007`: Skill Specification
- `RFC-0008`: Memory Specification
- `RFC-0009`: Multi Agent
- `RFC-0010`: AI Operating System
- `RFC-0011`: Plugin SDK
- `RFC-0012`: Self Improvement

RFC는 장기 설계 의도와 참조 관계를 보존하기 위한 문서이다.

## Shared Prompts

- `prompts/coding-rules.md`: 구현 Agent 공통 규칙
- `prompts/architect-rules.md`: 설계 검토 규칙
- `prompts/review-rules.md`: 코드 리뷰 규칙
- `prompts/SESSION_START.md`: 세션 시작 절차
- `prompts/IMPLEMENT_TASK.md`: 구현 절차
- `prompts/REVIEW_TASK.md`: 리뷰 절차
- `prompts/SESSION_CLOSE.md`: 세션 종료 절차
- `prompts/CHATGPT_SESSION_RESUME.md`: 새 ChatGPT 세션 재개 절차

## Required Repository Structure

```text
Enhancer/
├─ README.md
├─ CONSTITUTION.md
├─ AGENTS.md
├─ ARCHITECTURE.md
├─ PROJECT_STATE.md
├─ CURRENT_TASK.md
├─ ROADMAP.md
├─ DECISION_LOG.md
├─ SESSION_HANDOFF.md
├─ CHANGELOG.md
├─ docs/
├─ prompts/
├─ skills/
├─ .ai/
└─ src/
```
