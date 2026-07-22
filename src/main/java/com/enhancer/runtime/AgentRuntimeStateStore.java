package com.enhancer.runtime;

import java.io.IOException;

/**
 * Durable boundary for one Goal's schema-v2 AgentRun and retry-decision history.
 */
public interface AgentRuntimeStateStore {
    void create(AgentRuntimeState initialState) throws IOException;

    void update(AgentRuntimeState nextState) throws IOException;

    AgentRuntimeState resolve(String goalId) throws IOException;
}
