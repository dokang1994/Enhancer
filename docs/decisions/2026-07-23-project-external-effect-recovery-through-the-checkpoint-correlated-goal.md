# 2026-07-23: Project External-Effect Recovery Through The Checkpoint-Correlated Goal

Status: Accepted Decision

## Context

The read-only Scheduler recovery projection can identify one durable cycle and its exact
checkpoint-named Goal, AgentRun, queue work, and optional RunRecord. It deliberately
does not read the Goal's external-effect ledger. Operators therefore cannot distinguish
an execution prefix before ledger creation from an empty ledger, ambiguous prepared
intent, an explicit user-recovery outcome, a non-compensated applied or deduplicated
effect, or a fully compensated history.

The effect ledger, runtime, checkpoint, and Evidence Store are independent durable
boundaries. An effect-store scan would invent correlation, and a terminal ledger status
without its exact integrity-checked evidence would overstate what the adapter
established. Existing `scheduler-recovery-status` callers must not acquire new required
arguments.

## Decision

Add a runtime-owned read-only external-effect recovery projection and a separate
`scheduler-external-effect-status` CLI command.

The new reader will reuse `SchedulerRecoveryStatusReader` as the sole
queue/checkpoint/runtime/RunRecord correlation policy. If that projection has no Goal,
the reader will not access the effect or Evidence stores. Otherwise it will point-resolve
only the checkpoint-correlated Goal's ledger, validate the Goal and exact runtime
WorkItem plus historical AgentRun membership of every effect, and resolve every terminal
outcome's exact Evidence Store reference to verify the bound digest without returning
evidence content.

The projection will report one conservative aggregate phase:

- no correlated Goal;
- ledger not yet recorded before runtime;
- ledger creation pending at the runtime-recorded prefix;
- empty ledger;
- prepared effect requiring recovery;
- explicit user recovery required;
- non-compensated applied or deduplicated effect recorded; or
- all effects compensated.

Precedence follows the retry decider's safety order: `PREPARED`, then
`REQUIRES_USER_RECOVERY`, then `APPLIED`/`DEDUPLICATED`, with all-compensated reported
only when every non-empty record is `COMPENSATED`. A missing ledger after the Scheduler
prefix advances beyond `RUNTIME_RECORDED` is inconsistent.

Because no cross-store transaction exists, the reader will take a bounded second
Scheduler projection and effect-ledger sample. Observed correlation, ledger-presence, or
ledger-revision drift fails explicitly. The CLI will require the existing Scheduler
recovery roots plus explicit external-effect and Evidence roots and a 1-through-8 output
limit, retaining complete counts and a bounded ledger-ordered identity/status prefix.

## Rationale

Composition preserves the already-tested checkpoint binding instead of duplicating it
or accepting an unrelated caller-supplied Goal. A separate command preserves every
existing CLI contract while allowing operators to opt into the extra roots and evidence
reads. Verifying terminal evidence prevents a durable status from being reported when
its proof is missing or corrupt, while conservative precedence aligns inspection with
the existing retry refusal policy.

## Consequences

- Operators can see why automatic retry is unsafe without invoking an adapter or
  mutating any durable state.
- An absent checkpoint never causes effect-store or Evidence Store discovery.
- A concurrently changing Scheduler or ledger may make one inspection fail; the caller
  may invoke the read-only command again.
- The projection does not prove external-system state, authorize replay, choose
  compensation, inspect credentials or payload content, or add an automatic recovery
  path.
- Invocation-spool inspection remains separate work.
