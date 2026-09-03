package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import com.enhancer.tool.CancellationToken;

/**
 * Selects one isolated-worker Work delivery by payload kind. Legacy work enters the unchanged
 * Gate 1-4 boundary; typed work enters the deterministic-fake child-local pipeline.
 */
final class IsolatedWorkMessageHandler implements MessageHandler {
    private final String workItemId;
    private final String requiredCapability;
    private final String goalId;
    private final String agentRunId;
    private final Optional<LegacyExecution> legacyExecution;
    private final Optional<ModelExecution> modelExecution;
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
        this.legacyExecution = Optional.of(new LegacyExecution(
                execution, runRecordStore));
        this.modelExecution = Optional.empty();
    }

    IsolatedWorkMessageHandler(
            String workItemId,
            String requiredCapability,
            String goalId,
            String agentRunId,
            Path projectRoot,
            DeterministicFakeModelAttemptPipeline pipeline,
            ModelProcessExecutionConfiguration configuration) {
        this.workItemId = Objects.requireNonNull(
                workItemId, "workItemId must not be null");
        this.requiredCapability = Objects.requireNonNull(
                requiredCapability, "requiredCapability must not be null");
        this.goalId = Objects.requireNonNull(goalId, "goalId must not be null");
        this.agentRunId = Objects.requireNonNull(
                agentRunId, "agentRunId must not be null");
        this.legacyExecution = Optional.empty();
        this.modelExecution = Optional.of(new ModelExecution(
                projectRoot, pipeline, configuration));
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
            if (workItem.isModelWork()) {
                ModelExecution context = modelExecution.orElseThrow(() ->
                        new IllegalArgumentException(
                                "typed ModelWork execution configuration is unavailable"));
                DeterministicFakeModelAttemptPipeline.Outcome outcome =
                        context.pipeline().execute(
                                context.projectRoot(),
                                goalId,
                                agentRunId,
                                workItem,
                                context.configuration().invocationLimits(),
                                context.configuration().deniedTools(),
                                context.configuration().maximumReadBytes(),
                                context.configuration().toolTimeout(),
                                CancellationToken.none());
                if (!(outcome
                        instanceof DeterministicFakeModelAttemptPipeline.Outcome.Published
                                published)) {
                    throw new IllegalArgumentException(
                            "typed ModelWork stopped before durable publication");
                }
                acceptedResult = Optional.of(new Result(
                        published.storedRecord().reference(),
                        published.verificationStatus()));
                return;
            }
            LegacyExecution context = legacyExecution.orElseThrow(() ->
                    new IllegalArgumentException(
                            "legacy Work execution configuration is unavailable"));
            String reference = context.execution().executeWork(
                    workItem,
                    goalId,
                    agentRunId,
                    AgentRunRecordIdentity.recordId(goalId, agentRunId));
            VerificationStatus status =
                    context.runRecordStore().resolve(reference)
                            .record().verification().status();
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

    private record LegacyExecution(
            AgentLoopAgentRunExecution execution,
            RunRecordStore runRecordStore) {
        private LegacyExecution {
            Objects.requireNonNull(execution, "execution must not be null");
            Objects.requireNonNull(runRecordStore, "runRecordStore must not be null");
        }
    }

    private record ModelExecution(
            Path projectRoot,
            DeterministicFakeModelAttemptPipeline pipeline,
            ModelProcessExecutionConfiguration configuration) {
        private ModelExecution {
            Objects.requireNonNull(projectRoot, "projectRoot must not be null");
            projectRoot = projectRoot.toAbsolutePath().normalize();
            Objects.requireNonNull(pipeline, "pipeline must not be null");
            Objects.requireNonNull(configuration, "configuration must not be null");
        }
    }
}
