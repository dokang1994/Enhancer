package com.enhancer.runtime;

import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.GovernedModelPromptReader;
import com.enhancer.model.ModelInvocationAdmission;
import com.enhancer.model.ModelInvocationAdmissionDecision;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/** Freshly prepares one typed Scheduler model invocation without executing it. */
public final class SchedulerModelInvocationPreparer {
    private final ExactActiveTaskResolver taskResolver;
    private final GovernedModelPromptReader promptReader;
    private final ModelInvocationAdmission admission;

    public SchedulerModelInvocationPreparer(
            ExactActiveTaskResolver taskResolver,
            GovernedModelPromptReader promptReader,
            ModelInvocationAdmission admission) {
        this.taskResolver = Objects.requireNonNull(
                taskResolver, "taskResolver must not be null");
        this.promptReader = Objects.requireNonNull(
                promptReader, "promptReader must not be null");
        this.admission = Objects.requireNonNull(
                admission, "admission must not be null");
    }

    public SchedulerModelInvocationPreparation prepare(
            Path projectRoot,
            WorkItem workItem,
            String correlationId,
            SchedulerModelInvocationLimits limits,
            Set<String> deniedTools,
            long maximumReadBytes,
            Duration toolTimeout,
            CancellationToken cancellationToken) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(limits, "limits must not be null");
        Objects.requireNonNull(deniedTools, "deniedTools must not be null");
        Objects.requireNonNull(toolTimeout, "toolTimeout must not be null");
        Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");

        ApprovedTask approvedTask = taskResolver.resolve(projectRoot, workItem);
        ExecutionPolicy executionPolicy = new ExecutionPolicy(
                projectRoot,
                Set.of(ModelInvokeTool.NAME),
                deniedTools,
                maximumReadBytes,
                toolTimeout,
                cancellationToken);
        ModelWorkPayload.ModelInvocationExecutionInput executionInput =
                workItem.modelExecutionInput().orElseThrow(() ->
                        new IllegalArgumentException(
                                "Scheduler model preparation requires typed ModelWork"));
        String prompt = promptReader.readFile(
                executionInput.targetPath(), executionPolicy);
        ModelRequest request = new ModelRequest(
                correlationId,
                prompt,
                executionInput.executionProfile().modelClass(),
                limits.gatewayTimeout(),
                limits.maximumResponseCharacters());
        ProfiledModelRequest profiledRequest = new ProfiledModelRequest(
                request,
                executionInput.executionProfile());
        ModelInvocationAdmissionDecision decision = admission.evaluate(
                profiledRequest,
                approvedTask,
                executionPolicy,
                workItem.requiredCapability());
        return new SchedulerModelInvocationPreparation(
                approvedTask,
                executionPolicy,
                profiledRequest,
                decision);
    }
}
