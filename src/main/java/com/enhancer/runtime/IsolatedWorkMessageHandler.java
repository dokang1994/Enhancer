package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Executes one isolated-worker Work delivery through the unchanged Gate 1-4 boundary.
 * It creates no message route, persistence policy, retry policy, or execution authority.
 */
final class IsolatedWorkMessageHandler implements MessageHandler {
    private final String workItemId;
    private final String requiredCapability;
    private final String goalId;
    private final String agentRunId;
    private final AgentLoopAgentRunExecution execution;
    private final RunRecordStore runRecordStore;
    private Optional<Result> acceptedResult = Optional.empty();

    IsolatedWorkMessageHandler(
            String workItemId,
            String requiredCapability,
            String goalId,
            String agentRunId,
            AgentLoopAgentRunExecution execution,
            RunRecordStore runRecordStore) {
        this.workItemId = Objects.requireNonNull(
                workItemId, "workItemId must not be null");
        this.requiredCapability = Objects.requireNonNull(
                requiredCapability, "requiredCapability must not be null");
        this.goalId = Objects.requireNonNull(goalId, "goalId must not be null");
        this.agentRunId = Objects.requireNonNull(
                agentRunId, "agentRunId must not be null");
        this.execution = Objects.requireNonNull(execution, "execution must not be null");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
    }

    @Override
    public void handle(MessageEnvelope work) {
        Objects.requireNonNull(work, "work must not be null");
        if (acceptedResult.isPresent()) {
            throw new IllegalStateException(
                    "the isolated work handler accepted more than one delivery");
        }
        try {
            WorkItem workItem = new WorkItem(
                    workItemId, requiredCapability, work);
            String reference = execution.executeWork(
                    workItem,
                    goalId,
                    agentRunId,
                    AgentRunRecordIdentity.recordId(goalId, agentRunId));
            VerificationStatus status =
                    runRecordStore.resolve(reference).record().verification().status();
            acceptedResult = Optional.of(new Result(reference, status));
        } catch (IOException invalid) {
            throw new IllegalArgumentException(invalid.getMessage(), invalid);
        }
    }

    Optional<Result> acceptedResult() {
        return acceptedResult;
    }

    record Result(String reference, VerificationStatus status) {
        Result {
            Objects.requireNonNull(reference, "reference must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }
}
