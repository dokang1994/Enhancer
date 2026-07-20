# 2026-07-20: Isolate The Worker In A Bounded Self-JVM Child Process

Status: Accepted Decision

Context:

- Connection 3 in the Gate 8 backlog is process-isolated worker plus local IPC, and the Roadmap records that it requires both a separately selected adapter and a process lifecycle. Sub-increment 3c supplied the adapter; the lifecycle is still absent, so all execution remains in the Scheduler's own process.
- In-process execution has a named limit already recorded in the architecture: `ToolExecutor` contains at most 64 live isolated workers and a permanently stuck worker holds capacity until process restart, because in-process containment cannot terminate running code. Only a process boundary makes termination possible.
- Spawning a process is new external authority. The only existing external command authority is `GitWorkspaceCollector`, granted by its own decision and scoped to one fixed read-only command through a canonical absolute executable outside the project. `.ai/workflow.md` step 6 forbids implementing work that needs new external authority inside a RED cycle, so 3b could not proceed until the authority was granted and scoped.
- The user granted process-execution authority on 2026-07-20, scoped to the current JVM only.

Decision:

- Add `IsolatedWorkerLauncher`, which starts one child process and returns a typed terminal outcome. It is the process lifecycle for connection 3.
- Bound the authority to the running JVM. The executable is resolved from `java.home`, canonicalized, and required to exist as a regular file; the child runs the current classpath. No caller-supplied executable, command name, or shell reaches `ProcessBuilder`, so a repository that ships its own binary cannot be executed.
- Take the entry point as a `Class<?>` rather than a command string. A class is necessarily already on the running classpath, so the caller selects which of this project's own entry points to run and cannot name a program. Tests use their own entry point for the timeout and exit-code cases, which keeps those behaviours provable without a test-only branch inside the production worker.
- Bound the child the way the Git adapter is bounded: a watchdog timeout followed by forcible destruction, a capped and discarded output stream, an environment sanitized of inherited overrides, and retention of the exit code and a bounded diagnostic reason only. Child output is never interpreted as a result.
- Return `IsolatedWorkerOutcome` with an explicit `COMPLETED`, `TIMED_OUT`, or `START_FAILED` status. A non-completed outcome carries a bounded reason and no exit code, so a caller cannot mistake a timeout for a clean exit.
- Add `IsolatedWorkerMain` as the fixed child entry point. It reads one message from a spool directory through the 3c adapter and exits with a stable code describing what it found: the boundary is proven by a real message crossing it, not by a stub.
- Do not wire the launcher into `AgentRunExecution` or `DurableAgentRunWorker`. Running the Gate 1-4 pipeline inside the child is the named follow-on and needs its own task.

Rationale:

Resolving the executable from `java.home` rather than a configured path removes the attack the Git adapter had to defend against explicitly: there is no lookup to poison, because the process re-runs the interpreter it is already running. That is also sufficient — a worker is the same runtime doing the same work in a separate address space, not an arbitrary program — so the narrower grant costs nothing the increment needs. Keeping the launcher unwired follows the same discipline as 3c: the boundary is proven and bounded before anything depends on it, so wiring is a decision about behaviour rather than a decision about capability.

Consequences:

- The codebase now has a second external command authority. `GitWorkspaceCollector` remains the only one that runs a configured external program; this one can only re-run the current JVM.
- A stuck child can now be terminated, which the in-process 64-worker ceiling explicitly could not do. The ceiling itself is unchanged and still applies to in-process execution.
- Tests spawn real child JVMs, so the suite gains a small number of slower cases. They remain deterministic: the child is this project's own code with fixed arguments.
- The child's exit code is the only channel. A future increment that needs richer results must carry them through the spool or its own artifact, not through standard output.
- Out of scope: running the real Gate 1-4 pipeline in the child, wiring the launcher into the execution port or worker, a result-return spool, restart or supervision policy, concurrency limits across children, and cancellation of a running child from another process.
