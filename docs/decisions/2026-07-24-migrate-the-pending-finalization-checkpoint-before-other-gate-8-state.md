# 2026-07-24: Migrate The Pending-Finalization Checkpoint Before Other Gate 8 State

Status: Accepted Decision

Context:

- Gate 8 requires a supported state-version migration path, but its four schema-v2
  durable artifacts do not have equivalent compatibility boundaries.
- Scheduler queue schema v1 lacks the exact retained `QueuedWork` values for terminal
  admissions that schema v2 requires. Agent runtime schema v1 lacks attempt and retry
  decision history and gives failed terminal state different meaning. External-effect
  ledger schema v1 lacks the adapter identity and terminal Evidence Store binding that
  schema v2 requires.
- Pending-finalization schema v1 already contains every value required by schema v2:
  Goal identity, AgentRun identity, and the optional RunRecord reference. Schema v2 adds
  only the optional replacement AgentRun identity, whose truthful value for every v1
  artifact is absent.
- Automatic migration during ordinary worker recovery would combine maintenance with
  execution and could turn an unsupported artifact into an implicit mutation.

Decision:

- Make `FileSystemPendingFinalizationStore` schema v1 to schema v2 the first supported
  Gate 8 state-version migration. Decode the exact bounded schema-v1 envelope, preserve
  its Goal identity, AgentRun identity, and optional RunRecord reference, and construct
  schema v2 with `replacementAgentRunId = Optional.empty()`.
- Expose migration only through a separate explicit maintenance command over the
  caller-supplied cycle-checkpoint root. Ordinary `resolve`, `scheduler-cycle`,
  `scheduler-drain`, and read-only status commands continue to reject schema v1 and
  never migrate as a side effect.
- Require the Scheduler process that owns the checkpoint to be stopped for the migration.
  The command may inspect only the fixed pending-finalization artifact and its private
  candidate file; it must not discover or mutate queue, runtime, ledger, evidence,
  RunRecord, submission, or invocation state.
- Validate the original envelope exactly as a normal durable artifact: regular-file and
  no-link boundary, size bound, magic, declared length, SHA-256, strict UTF-8, canonical
  identities, optional-field structure, and complete consumption. Corrupt, structurally
  invalid, future-version, or otherwise unsupported input fails closed.
- Treat an absent checkpoint and a valid current-schema checkpoint as explicit
  revision-free outcomes. Neither outcome creates or rewrites an artifact.
- Encode and validate the complete schema-v2 candidate before replacement. Publish it
  with the store's existing same-directory atomic-replace requirement only after
  confirming that the source bytes still equal the validated input.
- Until that final atomic replacement succeeds, the schema-v1 artifact remains the sole
  authority. Any read, validation, candidate-write, drift, or publication failure removes
  only the private candidate when possible and leaves the original path and bytes
  unchanged. After the atomic replacement succeeds, the schema-v2 artifact is the sole
  authority; no backup or rollback artifact is implied.
- Make migration idempotence, failure preservation, source-drift refusal, and normal
  post-migration recovery part of the implementation test contract. Do not claim
  concurrent old-version writer safety, parent-directory power-loss durability, or
  migration for any other store.

Alternatives considered:

- Migrate Scheduler queue state first: rejected because terminal schema-v1 admissions
  retain identities but not the exact authorization and provenance-bearing values needed
  by schema v2, so a general conversion would invent or discard authority.
- Migrate Agent runtime state first: rejected because failed schema-v1 state cannot
  acquire schema-v2 retry history or a retry decision without inventing lifecycle facts.
- Migrate the external-effect ledger first: rejected because missing adapter identity and
  terminal evidence cannot be reconstructed safely.
- Migrate automatically during worker recovery: rejected because it hides a durable
  mutation inside execution and gives an operator no separate failure boundary.

Rationale:

The pending-finalization checkpoint is the only current schema-v1 artifact whose complete
meaning embeds losslessly in schema v2. Starting there establishes explicit, atomic,
failure-preserving migration mechanics without inventing queue history, retry decisions,
adapter identity, or evidence.

Consequences:

- The next implementation task can add one bounded migration path and test it without
  changing ordinary recovery semantics or any other durable schema.
- Gate 8 still lacks an implemented supported migration until that task is verified, and
  whole-gate maturity does not change from this decision alone.
- Queue, runtime, and external-effect migrations require separate accepted compatibility
  policies for information that their schema-v1 artifacts do not retain.
