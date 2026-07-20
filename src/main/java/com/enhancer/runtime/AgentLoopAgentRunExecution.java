package com.enhancer.runtime;

import com.enhancer.application.AgentRunFinalizer;
import com.enhancer.loop.AgentLoop;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.AgentRunController;
import com.enhancer.loop.AgentRunResult;
import com.enhancer.loop.AgentRunState;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ToolFailureClassifier;
import com.enhancer.run.FinalizedAgentRun;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.CancellationToken;
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
    private static final Duration TOOL_TIMEOUT = Duration.ofSeconds(5);

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
        WorkItem workItem = dispatch.workItem();
        ExecutionInput input = deriveExecutionInput(workItem);
        ApprovedTask approvedTask = new ApprovedTask(
                workItem.taskRevision().taskId(),
                "Execute the approved work dispatched to Goal "
                        + dispatch.goalId(),
                "AgentRun " + dispatch.agentRunId()
                        + " dispatched from WorkItem " + workItem.workItemId(),
                workItem.allowedTools(),
                workItem.taskRevision().sourceDocument());
        String logicalRunId = evidenceStore.createRun();
        ToolRequest request = new ToolRequest(
                ReadFileTool.NAME,
                logicalRunId,
                Map.of(ReadFileTool.PATH_ARGUMENT, input.targetPath()));
        ExecutionPolicy policy = new ExecutionPolicy(
                projectRoot,
                Set.of(ReadFileTool.NAME),
                Set.of(),
                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES,
                TOOL_TIMEOUT,
                CancellationToken.none());

        AgentRunResult workerRun;
        try (ToolExecutor executor = new ToolExecutor(List.of(
                new ReadFileTool(new EvidenceRecorder(evidenceStore))))) {
            workerRun = new AgentRunController(
                    executor,
                    policy,
                    ToolFailureClassifier.standard())
                    .run(
                            AgentRunState.ready(approvedTask, request),
                            new AgentLoop(MAX_ITERATIONS, STAGNATION_THRESHOLD));
        }

        Optional<VerificationRequest> verificationRequest =
                workerRun.stopReason() == AgentLoopStopReason.AWAITING_VERIFICATION
                        ? Optional.of(new VerificationRequest(
                                approvedTask,
                                request,
                                workerRun.state().lastResult().orElseThrow(),
                                input.expectedContentSha256()))
                        : Optional.empty();
        FinalizedAgentRun finalized = new AgentRunFinalizer(
                new DeterministicReadFileVerifier(evidenceStore),
                runRecordStore,
                clock)
                .finalizeRun(workerRun, verificationRequest);
        return finalized.storedRecord().reference();
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
}
