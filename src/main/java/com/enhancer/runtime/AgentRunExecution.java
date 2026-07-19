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
}
