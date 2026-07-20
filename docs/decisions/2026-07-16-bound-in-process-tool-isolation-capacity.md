# 2026-07-16: Bound In-Process Tool Isolation Capacity

Status: Accepted Decision

Context:

- A Java thread that ignores interruption cannot be forcibly terminated safely in-process.
- The current ToolExecutor isolates each invocation in a fresh daemon worker so one timed-out Tool does not starve the next invocation, but every permanently stuck Tool can retain one thread indefinitely.
- Long-running Scheduler use would turn repeated malicious or broken Tools into unbounded process thread growth.
- Process isolation is the real termination boundary but is not yet available and would exceed a bounded Gate 1 hardening increment.

Decision:

- Introduce one process-wide isolation capacity shared by default across all ToolExecutor instances, bounded to 64 live isolated workers.
- Acquire one slot before creating a worker thread and release it only when that worker thread actually terminates. Timeout, interrupt, executor close, and `shutdownNow` do not pretend to release a slot while an interrupt-ignoring Tool is still running.
- Refuse a Tool invocation before thread creation when capacity is exhausted and return a typed terminal `ISOLATION_CAPACITY_EXHAUSTED` Tool failure with bounded evidence.
- Preserve invocation isolation: a timed-out worker below the global ceiling does not block an independent next invocation.
- Permit an injected smaller shared capacity only for deterministic package-level tests; production uses the single process-wide default.
- Defer process workers, OS-level termination, per-plugin quotas, Scheduler admission/backpressure integration, health recovery, and operator controls.

Rationale:

A finite fail-closed ceiling prevents unbounded daemon-thread accumulation without making a false claim that Java can kill arbitrary code. Holding the slot until actual termination ensures the accounting describes real process resource occupancy rather than timeout bookkeeping.

Consequences:

- After 64 concurrently non-terminated isolated workers, new Tool invocations fail terminally until a worker actually exits or the process restarts.
- The ceiling is containment, not recovery; a process containing permanently stuck workers still requires operator restart.
- Gate 1 Tool execution remains Integrated, and long-running Scheduler workers still require process isolation before Operational promotion.
