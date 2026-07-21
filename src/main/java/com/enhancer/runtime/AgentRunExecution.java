package com.enhancer.runtime;

import java.io.IOException;

/**
 * Execution port for one dispatched WorkItem: perform the approved work, persist a durable
 * RunRecord, and return its reference ("run-record/&lt;uuid&gt;"). The finalizer resolves the
 * reference to read the verification status, so implementations return only the reference.
 * The production AgentLoop-backed implementation is a named follow-on; this increment injects
 * deterministic ports in tests.
 */
public interface AgentRunExecution {
    String execute(AgentRunDispatch dispatch) throws IOException;

    /**
     * Retires execution-only artifacts after the returned RunRecord reference has been durably
     * checkpointed by the worker. Implementations with no such artifacts need no cleanup.
     *
     * <p>The operation must be idempotent because worker recovery may repeat it. A failure leaves
     * the checkpoint authoritative and is retried before execution acknowledgement; it must not
     * cause the approved work to execute again.
     */
    default void cleanupAfterCheckpoint(AgentRunDispatch dispatch) throws IOException {
        // Most execution implementations own no per-cycle transport artifacts.
    }
}
