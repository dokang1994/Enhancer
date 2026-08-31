# User continuation request on 2026-08-31 into additive Model RunRecord v2 implementation

Status: Accepted Decision

## Context

RFC-0019 is accepted and orders implementation to begin with an additive standalone
Model RunRecord value, typed persistence boundary, v2 codec, v1 golden compatibility,
and execution-guard regressions. The preceding specification task is Completed, and its
next action requires separate user authority for this RED-first implementation. The
user requested continuation on 2026-08-31.

## Decision

Authorize the first RFC-0019 implementation sequence as one bounded three-increment
Active Task: define and verify the immutable ModelRunRecord plus separate typed ports;
extend the existing filesystem RunRecord store with explicit payload-v2 dispatch,
round-trip, exact replay, integrity, and cross-kind refusal while preserving literal and
new v1 bytes; then run full regression and synchronize owning documents.

This decision keeps every current typed ModelWork execution guard and adds no production
model-record writer. It does not authorize exact task resolution, request/policy
preparation, admission invocation, candidate/local-gateway proof, Tool or gateway
execution, finalizer/result/recovery/worker wiring, submission or receive, migration,
providers, network, credentials, spend, push, merge, release, deployment, permission
change, or destructive cleanup.

## Consequences

- RunRecord v1 remains an unchanged public value and byte family.
- Model RunRecord v2 becomes a separately typed persistence family in the same
  reference namespace, not an optional extension projected through v1 APIs.
- Later Scheduler preparation cannot begin until this task is freshly verified and
  completed; later execution remains blocked by candidate and proven-local gateway
  contracts even after persistence exists.
