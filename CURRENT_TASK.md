# Current Task

## Status

Completed

## Task

Implement Gate 8 connection sub-increment 3b: add `IsolatedWorkerLauncher`, which starts one bounded child process running the current JVM and returns a typed terminal outcome, plus `IsolatedWorkerMain` as a real child entry point that reads one message from a 3c spool so the boundary is proven by a message crossing it.

## Task ID

gate-8-worker-process-isolation

## Justified By

- 2026-07-20: Isolate The Worker In A Bounded Self-JVM Child Process

## Context

Connection 3 needs both an adapter and a process lifecycle; 3c supplied the adapter and all execution still runs in the Scheduler's own process. The architecture already records the limit that makes isolation necessary: `ToolExecutor` bounds live in-process workers at 64 and a permanently stuck worker holds capacity until process restart, because in-process containment cannot terminate running code. Spawning a process is new external authority, which `.ai/workflow.md` step 6 forbids taking inside a RED cycle; the user granted it on 2026-07-20 scoped to the current JVM only.

## Acceptance Criteria

- `IsolatedWorkerLauncher` resolves its executable from `java.home`, canonicalized and required to be a regular file, and runs the current classpath. No caller-supplied executable, command name, or shell reaches `ProcessBuilder`.
- The entry point is taken as a `Class<?>`, so the caller selects one of this project's own entry points and cannot name a program.
- A child that exits within its timeout yields `COMPLETED` with its exit code; a child that overruns is forcibly destroyed and yields `TIMED_OUT` with a bounded reason and no exit code; a child that cannot start yields `START_FAILED`.
- Child output is capped and discarded, the environment is sanitized of inherited overrides, and only the exit code and a bounded reason are retained.
- `IsolatedWorkerMain` reads one spooled message through the 3c adapter and exits with a stable code for a decoded message, an empty spool, a corrupt message, and a usage error.
- No production path constructs the launcher, so no runtime behaviour changes.
- Full regression passes with 0 failures and 0 errors, and strict lint passes across all production sources.

## Out Of Scope

- Running the real Gate 1-4 pipeline inside the child, and wiring the launcher into `AgentRunExecution` or `DurableAgentRunWorker`.
- A result-return spool, restart or supervision policy, concurrency limits across children, and cancelling a running child from another process.
- Any change to the in-process 64-worker isolation ceiling, which still governs in-process execution.

## Approval

Approved by the user's 2026-07-20 grant of process-execution authority scoped to the current JVM only, after being shown that a configured-executable scope would be wider than the increment needs.

## Verification

Recorded in `docs/verification-log.md` under Worker Process Isolation Verification.

## Next

Wire the isolated worker into the execution port so the Gate 1-4 pipeline runs in the child, which needs its own bounded task and a decision on how a result returns across the boundary.
