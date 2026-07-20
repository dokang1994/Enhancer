# 2026-07-15: Make RunRecord Observation Test Time-Independent

Status: Accepted Decision

Context:

- `FileSystemRunRecordStore.persist()` stamps `storedAt` with `Instant.now()`, but `RunRecordMetadataCollectorTest` hardcoded its observation time to `2026-07-15T10:01:00Z`.
- When the wall clock is past 10:01 UTC, an AVAILABLE record's stored time falls after the fixed observation time, the collector correctly drops `sourceUpdatedAt` as future, and the test fails; this defect is unrelated to the Gate 7 delivery increment that surfaced it.

Decision:

- Derive the test's observation time from the same run clock as `persist()` (`Instant.now().plusSeconds(60)`) instead of a hardcoded instant, keeping the correction confined to the test and separate from the delivery increment.

Rationale:

The observation contract is that `sourceUpdatedAt` is present only when the stored time is not after the observation time; a correct test must observe at or after the clock that stamped the record rather than assume a fixed relationship to wall-clock time. Injecting a deterministic clock into the store would be a larger production change than the defect warrants.

Consequences:

- The test now passes regardless of wall-clock time of day; a future deterministic-clock refactor of the store may replace this run-clock derivation.
