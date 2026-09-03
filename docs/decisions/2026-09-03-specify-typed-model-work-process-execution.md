# User continuation request on 2026-09-03 into typed ModelWork process-execution specification

Status: Accepted Decision

## Context

RFC-0018 through RFC-0022 now retain typed ModelWork durably and provide Model
RunRecord v2, fresh Scheduler request/policy/admission preparation, one exact local
fake candidate, deterministic token capacity, exact-request budget evaluation, and a
same-request/same-policy/same-candidate-gateway invoker. Those boundaries remain
standalone, and the current child, parent, finalizer, worker, and recovery/status
consumers still reject or cannot resolve typed model execution. RFC-0022 named a
separate accepted connection contract as the next step, and the user requested
continuation on 2026-09-03.

## Decision

Accept RFC-0023 as the documentation-only contract for deterministic-fake typed
ModelWork process execution. One record-missing child attempt must freshly compose the
exact RFC-0019 preparation, bind one exact RFC-0020/0021 candidate, create RFC-0022
`Ready`, invoke its exact candidate-bound gateway at most once, and only then map a
returned success or gateway failure through the same-policy Tool/evidence boundary.
Response structure and digest verification precede a v2-only deterministic record;
the child Result remains a claim that the parent must resolve and bind completely.

Object identity is child-local. The parent passes explicit scalar configuration and
validates persisted values; it never serializes policy, admission, suitability, Ready,
candidate, gateway, or invocation results. A pure AgentRun-bound evidence-correlation
identity plus lazy exact namespace creation reconciles request construction with the
existing zero-evidence-write refusal rule.

Admission, suitability, budget, and invoker pre-call refusals continue to create no
ToolResult, evidence, RunRecord, Result, runtime Result, queue disposition, or retry
decision. They remain no-reference recoverable prefixes rather than fabricated failed
attempts. Returned gateway, Tool, and verification failures may publish a complete v2
record and then use the existing replacement-AgentRun durable retry contract. Any
durable terminal pre-call refusal requires a separate schema decision.

Current durable model record, envelope, Result, pending-finalization, runtime, queue,
and checkpoint schemas are sufficient for the minimum returned-outcome path. Every
v1-only parent/finalizer/status/recovery consumer must gain an explicit v2 path before
the first reachable writer. Current v2 is not widened with candidate, token-count,
Ready, response-usage, policy-object, or refusal provenance.

This decision authorizes only RFC/architecture/index/Roadmap/task/Changelog/handoff and
append-only verification documentation, focused governance, full regression
verification, and ordinary local GREEN commits. It authorizes no Java or test-source
change, actual gateway or Tool call, production caller, supported entry point,
producer/receiver, schema migration, provider/router/network, credential, spend, MCP,
push, merge, release, deployment, permission change, destructive cleanup, or external
effect.

## Consequences

- The process connection has one exact child-local identity chain and one gateway call
  maximum per AgentRun.
- A complete exact v2 record is the no-reinvocation boundary and takes precedence over
  a process-timeout fact; missing, corrupt, foreign, or changed records remain closed.
- Child transport Result and durable runtime Result remain distinct and neither
  replaces record resolution.
- Existing formats need no migration for the minimum path, but every named v2 consumer
  and the lazy evidence-run API must precede writer reachability.
- No capability maturity changes until separately authorized implementation and fresh
  integration evidence exist.
