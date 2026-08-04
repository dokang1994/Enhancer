package com.enhancer.runtime;

import java.io.IOException;

/**
 * Durable boundary for one Goal's schema-v4 lifecycle, control, cancellation, and timeout history.
 */
public interface AgentRuntimeStateStore {
    void create(AgentRuntimeState initialState) throws IOException;

    void update(AgentRuntimeState nextState) throws IOException;

    AgentRuntimeState resolve(String goalId) throws IOException;
}
