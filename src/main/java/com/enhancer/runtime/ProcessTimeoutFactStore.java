package com.enhancer.runtime;

import java.io.IOException;
import java.util.Optional;

/** Point persistence for one exact process-timeout fact per Goal and AgentRun. */
public interface ProcessTimeoutFactStore {
    ResolvedProcessTimeoutFact persist(ProcessTimeoutFact fact) throws IOException;

    Optional<ResolvedProcessTimeoutFact> find(String goalId, String agentRunId)
            throws IOException;

    ResolvedProcessTimeoutFact resolve(String reference) throws IOException;
}
