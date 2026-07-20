package com.enhancer.runtime;

import java.time.Duration;
import java.util.List;

/**
 * Port for running one worker outside this process and reporting how it ended.
 *
 * <p>An implementation owns the process mechanism and its bounds; those concerns do not enter
 * this contract. The returned outcome describes only how the child terminated. It is never
 * evidence about the work itself: a clean exit code does not mean the work succeeded, and a
 * caller must establish that from a durable artifact rather than from this result.
 */
public interface WorkerProcessLauncher {
    IsolatedWorkerOutcome run(Class<?> entryPoint, List<String> arguments, Duration timeout);
}
