package com.enhancer.runtime;

import com.enhancer.application.AgentRunFinalizer;
import com.enhancer.loop.AgentLoop;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.AgentRunController;
import com.enhancer.loop.AgentRunResult;
import com.enhancer.loop.AgentRunState;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.bus.WorkPayload;
import com.enhancer.loop.ToolFailureClassifier;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.model.DeterministicModelInvokeVerifier;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.run.FinalizedAgentRun;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.Tool;
import com.enhancer.verification.IndependentVerifier;
import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.ReadFileTool;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolRequest;
import com.enhancer.verification.DeterministicReadFileVerifier;
import com.enhancer.verification.VerificationRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * First production {@link AgentRunExecution}: runs the dispatched WorkItem through the real
 * Gate 1-4 pipeline (governed read-file Tool, persisted evidence, bounded Agent Loop,
 * independent digest verification, application finalization) and returns the persisted
 * RunRecord reference. The injected RunRecord store must be the same store the worker's
 * {@code DurableAgentRunFinalizer} resolves from, so the returned reference is resolvable and
 * the record's approved task binds to the Goal on taskId plus sourceDocument by construction.
 * A verification failure is carried in the persisted RunRecord, never thrown.
 */
public final class AgentLoopAgentRunExecution implements AgentRunExecution {
    private static final int MAX_ITERATIONS = 5;
    private static final int STAGNATION_THRESHOLD = 3;
    private static final Duration READ_FILE_TOOL_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MODEL_INVOKE_TOOL_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MODEL_GATEWAY_TIMEOUT = Duration.ofSeconds(4);
    private static final int MODEL_MAX_RESPONSE_LENGTH = 65_536;

    private final Path projectRoot;
    private final EvidenceStore evidenceStore;
    private final RunRecordStore runRecordStore;
    private final Clock clock;

    public AgentLoopAgentRunExecution(
            Path projectRoot,
            EvidenceStore evidenceStore,
            RunRecordStore runRecordStore,
            Clock clock) {
        this.projectRoot = Objects.requireNonNull(
                projectRoot, "projectRoot must not be null");
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore, "evidenceStore must not be null");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String execute(AgentRunDispatch dispatch) throws IOException {
        Objects.requireNonNull(dispatch, "dispatch must not be null");
        return executeWork(
                dispatch.workItem(), dispatch.goalId(), dispatch.agentRunId());
    }

    /**
     * Runs the pipeline for one WorkItem under a named Goal and AgentRun.
     *
     * <p>Separated from {@link #execute(AgentRunDispatch)} because a process-isolated child holds
     * no lease and cannot construct an {@link AgentRunDispatch}: the lease and queue identity are
     * the parent's concern and are never read here. Behaviour is otherwise identical, so the
     * in-process and isolated paths run the same pipeline rather than two similar ones.
     */
    String executeWork(WorkItem workItem, String goalId, String agentRunId)
            throws IOException {
        return executeWork(workItem, goalId, agentRunId, Optional.empty());
    }

    String executeWork(
            WorkItem workItem,
            String goalId,
            String agentRunId,
            String runRecordId) throws IOException {
        Objects.requireNonNull(runRecordId, "runRecordId must not be null");
        return executeWork(
                workItem, goalId, agentRunId, Optional.of(runRecordId));
    }

    private String executeWork(
            WorkItem workItem,
            String goalId,
            String agentRunId,
            Optional<String> runRecordId) throws IOException {
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(goalId, "goalId must not be null");
        Objects.requireNonNull(agentRunId, "agentRunId must not be null");
        ApprovedTask approvedTask = new ApprovedTask(
                workItem.taskRevision().taskId(),
                "Execute the approved work dispatched to Goal " + goalId,
                "AgentRun " + agentRunId
                        + " dispatched from WorkItem " + workItem.workItemId(),
                workItem.allowedTools(),
                workItem.taskRevision().sourceDocument());
        String logicalRunId = evidenceStore.createRun();
        Pipeline pipeline = selectPipeline(workItem, logicalRunId);

        AgentRunResult workerRun;
        try (ToolExecutor executor = new ToolExecutor(List.of(pipeline.tool()))) {
            workerRun = new AgentRunController(
                    executor,
                    pipeline.policy(),
                    ToolFailureClassifier.standard())
                    .run(
                            AgentRunState.ready(approvedTask, pipeline.request()),
                            new AgentLoop(MAX_ITERATIONS, STAGNATION_THRESHOLD));
        }

        Optional<VerificationRequest> verificationRequest =
                workerRun.stopReason() == AgentLoopStopReason.AWAITING_VERIFICATION
                        ? Optional.of(new VerificationRequest(
                                approvedTask,
                                pipeline.request(),
                                workerRun.state().lastResult().orElseThrow(),
                                pipeline.expectedContentSha256()))
                        : Optional.empty();
        AgentRunFinalizer finalizer = new AgentRunFinalizer(
                pipeline.verifier(),
                runRecordStore,
                clock);
        FinalizedAgentRun finalized = runRecordId.isPresent()
                ? finalizer.finalizeRun(
                        workerRun,
                        verificationRequest,
                        runRecordId.orElseThrow())
                : finalizer.finalizeRun(workerRun, verificationRequest);
        return finalized.storedRecord().reference();
    }

    /**
     * Scope-derived pipeline selection: a scope containing {@code read-file} keeps the
     * original governed read-file pipeline unchanged, any other scope containing
     * {@code model-invoke} executes the deterministic fake gateway, and a scope naming
     * neither executable tool fails closed before any execution.
     */
    private Pipeline selectPipeline(WorkItem workItem, String logicalRunId) {
        if (workItem.allowedTools().contains(ReadFileTool.NAME)) {
            ExecutionInput input = deriveExecutionInput(workItem);
            return new Pipeline(
                    new ToolRequest(
                            ReadFileTool.NAME,
                            logicalRunId,
                            Map.of(ReadFileTool.PATH_ARGUMENT, input.targetPath())),
                    new ExecutionPolicy(
                            projectRoot,
                            Set.of(ReadFileTool.NAME),
                            Set.of(),
                            EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES,
                            READ_FILE_TOOL_TIMEOUT,
                            CancellationToken.none()),
                    new ReadFileTool(new EvidenceRecorder(evidenceStore)),
                    new DeterministicReadFileVerifier(evidenceStore),
                    input.expectedContentSha256());
        }
        if (!workItem.allowedTools().contains(ModelInvokeTool.NAME)) {
            throw new IllegalArgumentException(
                    "WorkItem scope names no executable tool: "
                            + workItem.allowedTools());
        }
        WorkPayload.ExecutionInput declared = workItem.executionInput().orElseThrow(
                () -> new IllegalArgumentException(
                        "model-scoped work requires a declared execution input"));
        return new Pipeline(
                new ToolRequest(
                        ModelInvokeTool.NAME,
                        logicalRunId,
                        Map.of(
                                ModelInvokeTool.PROMPT_PATH_ARGUMENT,
                                        declared.targetPath(),
                                ModelInvokeTool.MODEL_CLASS_ARGUMENT,
                                        workItem.requiredCapability(),
                                ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT,
                                        Long.toString(
                                                MODEL_GATEWAY_TIMEOUT.toMillis()),
                                ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT,
                                        Integer.toString(
                                                MODEL_MAX_RESPONSE_LENGTH))),
                new ExecutionPolicy(
                        projectRoot,
                        Set.of(ModelInvokeTool.NAME),
                        Set.of(),
                        EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES,
                        MODEL_INVOKE_TOOL_TIMEOUT,
                        CancellationToken.none()),
                new ModelInvokeTool(
                        new DeterministicFakeModelGateway(),
                        new EvidenceRecorder(evidenceStore)),
                new DeterministicModelInvokeVerifier(evidenceStore),
                declared.expectedContentSha256());
    }

    /**
     * Derivation seam: prefer the payload-declared execution input; otherwise the approved
     * task's own source document is the governed read-file target and its approved revision
     * digest is the expected content SHA-256. The approved task binding stays the source
     * document either way, exactly as the CLI separates the task document from the target.
     */
    private static ExecutionInput deriveExecutionInput(WorkItem workItem) {
        return workItem.executionInput()
                .map(declared -> new ExecutionInput(
                        declared.targetPath(),
                        declared.expectedContentSha256()))
                .orElseGet(() -> new ExecutionInput(
                        workItem.taskRevision().sourceDocument(),
                        workItem.taskRevision().sourceSha256()));
    }

    private record ExecutionInput(String targetPath, String expectedContentSha256) {
    }

    private record Pipeline(
            ToolRequest request,
            ExecutionPolicy policy,
            Tool tool,
            IndependentVerifier verifier,
            String expectedContentSha256) {
    }
}
