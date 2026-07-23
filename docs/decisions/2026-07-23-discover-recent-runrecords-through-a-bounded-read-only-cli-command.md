# 2026-07-23: Discover Recent RunRecords Through A Bounded Read-Only CLI Command

Status: Accepted Decision

Context:

- `run`, `scheduler-cycle`, and `scheduler-drain` persist RunRecords, and `replay` can
  inspect one integrity-checked record when its opaque reference is already known.
- Process-isolated Scheduler execution discards child output by design. Cycle and drain
  output report only the number of records, so an operator told to inspect retained
  RunRecord evidence after a failure has no supported way to discover the reference.
- Queue inspection would help distinguish pending and blocked work, but readiness belongs
  to the Scheduler queue rather than the CLI and requires a separate projection contract.
  Scheduler checkpoint inspection also requires correlating queue, runtime, checkpoint,
  and RunRecord state rather than reading one existing store boundary.
- `RunRecordStore.recentReferences(limit)` already owns bounded newest-first discovery and
  does not resolve, reinterpret, or mutate records.

Decision:

- Add a separate `run-record-list` CLI command requiring explicit
  `--run-record-root` and `--limit` inputs.
- Bound the CLI limit from 1 through 48 so the command can print every returned canonical
  reference within the existing 4096-character CLI output ceiling.
- Reuse `FileSystemRunRecordStore.recentReferences(limit)` as the sole discovery source.
  Preserve its newest-first ordering and do not duplicate filesystem ordering, record
  integrity, verification, or runtime policy in the CLI.
- Report bounded machine-readable `AVAILABLE` or `EMPTY` status, successful exit code,
  requested limit, returned count, and each opaque reference. A missing store directory
  is empty and must not be created.
- Do not resolve records while listing. A discoverable corrupt artifact remains a
  reference whose integrity failure is surfaced by the existing `replay` command; listing
  must not silently reinterpret or suppress it.
- The implementation consumer is a real-filesystem CLI integration that persists real
  RunRecords, proves bounded newest-first discovery and replay of a returned reference,
  and proves empty inspection creates no directory.

Alternatives considered:

- Add only global or per-command help: useful documentation, but it does not expose the
  retained evidence the operator is currently unable to locate.
- Add `scheduler-status` first: deferred because a useful status must distinguish ready,
  blocked, active, verified, and failed work without moving Scheduler policy into the CLI.
- Expose the cycle checkpoint first: deferred because a checkpoint is only one recovery
  prefix and cannot be interpreted safely without matching queue and runtime state.
- Print every RunRecord reference from `scheduler-drain`: rejected because it couples
  generic evidence discovery to one execution command and can exceed bounded output.
- Add bounded recent RunRecord discovery: selected because it closes the documented
  inspectability gap through an existing read-only store contract and composes directly
  with `replay`.

Rationale:

The missing capability is discovery, not another record format or policy decision. A
small read-only list command makes already-persisted evidence reachable while leaving
record validation and detailed inspection with `replay`. The explicit limit preserves
bounded output and the existing store preserves ordering.

Consequences:

- Operators can find recent references after foreground Scheduler execution and replay
  only the records they choose.
- Listing observes one caller-named RunRecord root and grants no task, execution, queue,
  recovery, or Tool authority.
- Queue-state inspection, Scheduler recovery projection, richer filtering, record search,
  pagination, shared Application API extraction, and interactive help remain separate
  work.
