package com.enhancer.runtime;

import java.io.IOException;

/**
 * Durable boundary for the current state of one Goal and its schema-v1 AgentRun.
 */
public interface AgentRuntimeStateStore {
    void create(AgentRuntimeState initialState) throws IOException;

    void update(AgentRuntimeState nextState) throws IOException;

    AgentRuntimeState resolve(String goalId) throws IOException;
}
