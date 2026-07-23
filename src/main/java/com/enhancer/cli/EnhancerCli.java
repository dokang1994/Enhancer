package com.enhancer.cli;

import com.enhancer.brain.AcceptedDecisionProjector;
import com.enhancer.brain.GraphNode;
import com.enhancer.brain.GraphEdge;
import com.enhancer.brain.GraphNodeKind;
import com.enhancer.brain.MemoryFreshness;
import com.enhancer.brain.ProjectBrainGraph;
import com.enhancer.brain.ProjectBrainView;
import com.enhancer.brain.RepositoryMemoryEntry;
import com.enhancer.brain.RunEvidenceGraphProducer;
import com.enhancer.brain.TaskImpact;
import com.enhancer.brain.TaskImpactQuery;
import com.enhancer.brain.TaskJustificationProjector;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.loop.AgentLoop;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.AgentRunController;
import com.enhancer.application.AgentRunFinalizer;
import com.enhancer.loop.AgentRunResult;
import com.enhancer.loop.AgentRunState;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.loop.ToolFailureClassifier;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.FinalizedAgentRun;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.runtime.AgentRunRetryPolicy;
import com.enhancer.runtime.DurableAgentRunWorker;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableSubmissionManifest;
import com.enhancer.runtime.DurableSubmissionResult;
import com.enhancer.runtime.DurableWorkSubmissionService;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemExternalEffectLedgerStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import com.enhancer.runtime.ForegroundSchedulerDrain;
import com.enhancer.runtime.GeneratedInputSubmissionService;
import com.enhancer.runtime.GeneratedSubmissionIdentities;
import com.enhancer.runtime.GeneratedSubmissionRequest;
import com.enhancer.runtime.MissingSchedulerQueueStateException;
import com.enhancer.runtime.SchedulerDrainResult;
import com.enhancer.runtime.SchedulerQueueState;
import com.enhancer.runtime.SchedulerQueueStatus;
import com.enhancer.runtime.SchedulerRecoveryStatus;
import com.enhancer.runtime.SchedulerRecoveryStatusReader;
import com.enhancer.runtime.WorkItemDisposition;
import com.enhancer.session.DevelopmentSessionCheckpoint;
import com.enhancer.session.DevelopmentSessionCheckpointConflictException;
import com.enhancer.session.DevelopmentSessionCheckpointInspection;
import com.enhancer.session.DevelopmentSessionCheckpointManager;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ReadFileTool;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.text.UnicodeText;
import com.enhancer.verification.DeterministicReadFileVerifier;
import com.enhancer.verification.VerificationRequest;
import com.enhancer.workspace.GitWorkspaceCollector;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import com.enhancer.workspace.RunRecordMetadataCollector;
import com.enhancer.workspace.TargetFileMetadataCollector;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceObservation;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EnhancerCli {
    public static final int MAX_DIAGNOSTIC_CHARACTERS = 4096;

    private static final Duration TOOL_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_ITERATIONS = 5;
    private static final int STAGNATION_THRESHOLD = 3;
    private final BrainComposer brainComposer;

    public EnhancerCli() {
        this.brainComposer = this::composeBrain;
    }

    EnhancerCli(BrainComposer brainComposer) {
        this.brainComposer = Objects.requireNonNull(
                brainComposer, "brainComposer must not be null");
    }

    public static void main(String[] arguments) {
        System.exit(new EnhancerCli().execute(arguments, System.out, System.err));
    }

    public int execute(String[] arguments, PrintStream stdout, PrintStream stderr) {
        Objects.requireNonNull(stdout, "stdout must not be null");
        Objects.requireNonNull(stderr, "stderr must not be null");

        try {
            CliCommand command = CliArguments.parse(arguments);
            if (command instanceof RunCliCommand run) {
                return executeRun(run, stdout);
            }
            if (command instanceof ReplayCliCommand replay) {
                return executeReplay(replay, stdout);
            }
            if (command instanceof RunRecordListCliCommand list) {
                return executeRunRecordList(list, stdout);
            }
            if (command instanceof SchedulerStatusCliCommand status) {
                return executeSchedulerStatus(status, stdout);
            }
            if (command
                    instanceof SchedulerRecoveryStatusCliCommand recovery) {
                return executeSchedulerRecoveryStatus(
                        recovery, stdout);
            }
            if (command instanceof SchedulerCycleCliCommand cycle) {
                return executeSchedulerCycle(cycle, stdout);
            }
            if (command instanceof SchedulerDrainCliCommand drain) {
                return executeSchedulerDrain(drain, stdout);
            }
            if (command instanceof SchedulerSubmitCliCommand submit) {
                return executeSchedulerSubmit(submit, stdout);
            }
            if (command instanceof GeneratedSubmitCliCommand generated) {
                return executeGeneratedSubmit(generated, stdout);
            }
            if (command instanceof CheckpointStartCliCommand start) {
                return executeCheckpointStart(start, stdout);
            }
            if (command instanceof CheckpointRecordCliCommand record) {
                return executeCheckpointRecord(record, stdout);
            }
            if (command instanceof CheckpointShowCliCommand show) {
                return executeCheckpointShow(show, stdout);
            }
            return executeCheckpointClear((CheckpointClearCliCommand) command, stdout);
        } catch (CliUsageException exception) {
            writeError(stderr, CliExitCode.USAGE_OR_CONFIGURATION, exception.getMessage());
            return CliExitCode.USAGE_OR_CONFIGURATION.code();
        } catch (DevelopmentSessionCheckpointConflictException exception) {
            writeError(stderr, CliExitCode.USAGE_OR_CONFIGURATION, exception.getMessage());
            return CliExitCode.USAGE_OR_CONFIGURATION.code();
        } catch (Exception exception) {
            writeError(stderr, CliExitCode.INTERNAL_ERROR, safeMessage(exception));
            return CliExitCode.INTERNAL_ERROR.code();
        }
    }

    private int executeCheckpointStart(
            CheckpointStartCliCommand command,
            PrintStream stdout) throws IOException {
        DevelopmentSessionCheckpoint checkpoint;
        try {
            checkpoint = new DevelopmentSessionCheckpointManager(command.projectRoot()).start(
                    command.step(),
                    command.nextAction(),
                    command.artifacts());
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    "checkpoint input is invalid: " + safeMessage(exception),
                    exception);
        }
        writeCheckpoint(stdout, checkpoint);
        return 0;
    }

    private int executeCheckpointRecord(
            CheckpointRecordCliCommand command,
            PrintStream stdout) throws IOException {
        DevelopmentSessionCheckpoint checkpoint;
        try {
            checkpoint = new DevelopmentSessionCheckpointManager(command.projectRoot()).record(
                    command.runId(),
                    command.expectedRevision(),
                    command.state(),
                    command.step(),
                    command.nextAction(),
                    command.evidenceReferences(),
                    command.artifacts());
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    "checkpoint input is invalid: " + safeMessage(exception),
                    exception);
        }
        writeCheckpoint(stdout, checkpoint);
        return 0;
    }

    private int executeCheckpointShow(
            CheckpointShowCliCommand command,
            PrintStream stdout) throws IOException {
        Optional<DevelopmentSessionCheckpointInspection> inspection =
                new DevelopmentSessionCheckpointManager(command.projectRoot()).inspect();
        if (inspection.isEmpty()) {
            writeBounded(stdout, "checkpointStatus=EMPTY\n");
            return 0;
        }
        DevelopmentSessionCheckpointInspection current = inspection.orElseThrow();
        DevelopmentSessionCheckpoint checkpoint = current.checkpoint();
        writeBounded(stdout, String.join("\n",
                "checkpointStatus=ACTIVE",
                "runId=" + checkpoint.runId(),
                "taskId=" + safeValue(checkpoint.taskId()),
                "taskContractSha256=" + checkpoint.taskContractSha256(),
                "revision=" + checkpoint.revision(),
                "state=" + checkpoint.state(),
                "currentStep=" + safeValue(checkpoint.currentStep()),
                "lastSuccessfulStep=" + safeValue(
                        checkpoint.lastSuccessfulStep().orElse("")),
                "nextAction=" + safeValue(checkpoint.nextAction()),
                "evidenceReferences=" + checkpoint.evidenceReferences().size(),
                "artifacts=" + checkpoint.artifacts().size(),
                "taskContractMatches=" + current.taskContractMatches(),
                "artifactMismatches=" + current.artifactMismatches().size()) + "\n");
        return 0;
    }

    private int executeCheckpointClear(
            CheckpointClearCliCommand command,
            PrintStream stdout) throws IOException {
        new DevelopmentSessionCheckpointManager(command.projectRoot()).clear(
                command.runId(),
                command.expectedRevision());
        writeBounded(stdout, "checkpointStatus=CLEARED\n");
        return 0;
    }

    private void writeCheckpoint(
            PrintStream stdout,
            DevelopmentSessionCheckpoint checkpoint) {
        writeBounded(stdout, String.join("\n",
                "checkpointStatus=ACTIVE",
                "runId=" + checkpoint.runId(),
                "taskId=" + safeValue(checkpoint.taskId()),
                "revision=" + checkpoint.revision(),
                "state=" + checkpoint.state(),
                "currentStep=" + safeValue(checkpoint.currentStep()),
                "lastSuccessfulStep=" + safeValue(
                        checkpoint.lastSuccessfulStep().orElse("")),
                "nextAction=" + safeValue(checkpoint.nextAction()),
                "evidenceReferences=" + checkpoint.evidenceReferences().size(),
                "artifacts=" + checkpoint.artifacts().size()) + "\n");
    }

    private int executeRun(RunCliCommand command, PrintStream stdout) throws IOException {
        GovernedRunInputs inputs = governedRunInputs(command);
        ApprovedTask approvedTask = inputs.approvedTask();
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(command.runRecordRoot());
        Instant capturedAt = Instant.now();
        List<WorkspaceSourceObservation> additionalObservations = new ArrayList<>(
                new RunRecordMetadataCollector().observe(runRecordStore, capturedAt));
        try {
            additionalObservations.add(new TargetFileMetadataCollector().observe(
                    command.projectRoot(),
                    command.targetPath(),
                    capturedAt));
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    "target-path is invalid: " + safeMessage(exception),
                    exception);
        }
        additionalObservations.addAll(new GitWorkspaceCollector().observe(
                command.projectRoot(),
                capturedAt));
        WorkspaceSnapshot snapshot = new RepositoryMemorySnapshotCollector().collect(
                command.projectRoot(),
                capturedAt,
                approvedTask,
                inputs.repositoryMemory(),
                additionalObservations);
        List<GraphNode> decisions;
        List<GraphEdge> justificationEdges;
        try {
            decisions = new AcceptedDecisionProjector().project(
                    snapshot,
                    inputs.repositoryMemory());
            justificationEdges = new TaskJustificationProjector().project(
                    snapshot,
                    inputs.repositoryMemory(),
                    decisions);
            new RunEvidenceGraphProducer().preflight(
                    snapshot,
                    Instant.now(),
                    decisions,
                    justificationEdges);
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    "project brain inputs are invalid: " + safeMessage(exception),
                    exception);
        }
        FileSystemEvidenceStore evidenceStore = new FileSystemEvidenceStore(
                command.evidenceRoot(),
                new EvidenceStoragePolicy(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES));
        String logicalRunId = evidenceStore.createRun();
        ToolRequest request = new ToolRequest(
                ReadFileTool.NAME,
                logicalRunId,
                Map.of(ReadFileTool.PATH_ARGUMENT, command.targetPath()));
        ExecutionPolicy policy = new ExecutionPolicy(
                command.projectRoot(),
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
                                command.expectedSha256()))
                        : Optional.empty();
        FinalizedAgentRun finalized = new AgentRunFinalizer(
                new DeterministicReadFileVerifier(evidenceStore),
                runRecordStore)
                .finalizeRun(workerRun, verificationRequest);
        CliExitCode exitCode = exitCode(finalized.record());
        List<String> output = new ArrayList<>(List.of(
                "status=" + finalized.stopReason(),
                "exitCode=" + exitCode.code(),
                "workerStopReason=" + finalized.record().workerStopReason(),
                "verificationStatus=" + finalized.verification().status(),
                "verificationCode=" + finalized.verification().code(),
                "iterations=" + finalized.record().iterations(),
                "runRecordRoot=" + safeValue(command.runRecordRoot().toString()),
                "runRecordReference=" + finalized.storedRecord().reference()));
        try {
            BrainSummary summary = brainComposer.compose(new BrainCompositionInput(
                    snapshot,
                    inputs.repositoryMemory(),
                    finalized,
                    decisions,
                    justificationEdges,
                    approvedTask.taskId()));
            output.add("brainStatus=AVAILABLE");
            output.add("workspaceSnapshotId=" + summary.snapshotId());
            output.add("workspaceObservations=" + summary.workspaceObservations());
            output.add("memoryFreshness=" + summary.memoryFreshness());
            output.add("graphNodes=" + summary.graphNodes());
            output.add("graphEdges=" + summary.graphEdges());
            output.add("graphDecisions=" + summary.graphDecisions());
            output.add("impactExecutions=" + summary.impactExecutions());
            output.add("impactDecisions=" + summary.impactDecisions());
        } catch (RuntimeException exception) {
            output.add("brainStatus=UNAVAILABLE");
            output.add("brainReason=" + safeValue(safeMessage(exception)));
        }
        writeBounded(stdout, String.join("\n", output) + "\n");
        return exitCode.code();
    }

    private BrainSummary composeBrain(BrainCompositionInput input) {
        ProjectBrainView view = ProjectBrainView.compose(
                input.snapshot(),
                input.repositoryMemory(),
                input.finalized().record());
        ProjectBrainGraph graph = new RunEvidenceGraphProducer().produce(
                input.snapshot(),
                new ResolvedRunRecord(
                        input.finalized().storedRecord(),
                        input.finalized().record()),
                Instant.now(),
                input.decisions(),
                input.justificationEdges());
        TaskImpact impact = new TaskImpactQuery().query(graph, input.taskId());
        return new BrainSummary(
                view.snapshotId(),
                view.workspaceObservations().size(),
                freshnessSummary(view),
                graph.nodes().size(),
                graph.edges().size(),
                countDecisions(graph),
                impact.executions().size(),
                impact.decisions().size());
    }

    private long countDecisions(ProjectBrainGraph graph) {
        return graph.nodes().stream()
                .filter(node -> node.kind() == GraphNodeKind.DECISION)
                .count();
    }

    private String freshnessSummary(ProjectBrainView view) {
        int matched = 0;
        int diverged = 0;
        int notObserved = 0;
        for (RepositoryMemoryEntry entry : view.repositoryMemory()) {
            if (entry.freshness() == MemoryFreshness.SNAPSHOT_MATCHED) {
                matched++;
            } else if (entry.freshness() == MemoryFreshness.SNAPSHOT_DIVERGED) {
                diverged++;
            } else {
                notObserved++;
            }
        }
        return "matched=" + matched + ",diverged=" + diverged + ",notObserved=" + notObserved;
    }

    private int executeReplay(ReplayCliCommand command, PrintStream stdout) throws IOException {
        ResolvedRunRecord resolved;
        try {
            resolved = new FileSystemRunRecordStore(command.runRecordRoot())
                    .resolve(command.reference());
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException("reference is invalid", exception);
        }
        RunRecord record = resolved.record();
        CliExitCode exitCode = exitCode(record);
        writeBounded(stdout, String.join("\n",
                "status=" + record.finalStopReason(),
                "exitCode=" + exitCode.code(),
                "runRecordReference=" + resolved.metadata().reference(),
                "recordedAt=" + record.recordedAt(),
                "taskId=" + safeValue(record.approvedTask().taskId()),
                "toolName=" + safeValue(record.toolRequest().toolName()),
                "policyStatus=" + record.policyDecision().status(),
                "workerStopReason=" + record.workerStopReason(),
                "finalStopReason=" + record.finalStopReason(),
                "verificationStatus=" + record.verification().status(),
                "verificationCode=" + record.verification().code(),
                "iterations=" + record.iterations()) + "\n");
        return exitCode.code();
    }

    private int executeSchedulerCycle(
            SchedulerCycleCliCommand command,
            PrintStream stdout) throws IOException {
        SchedulerExecution execution =
                schedulerExecution(command, "scheduler-cycle");
        DurableSingleWorkerSchedulerQueue queue = execution.queue();
        Optional<WorkItemDisposition> disposition =
                execution.worker().runOneCycle(command.leaseDuration());
        String status = disposition.map(Enum::name).orElse("IDLE");
        CliExitCode exitCode = disposition
                .filter(value -> value == WorkItemDisposition.FAILED)
                .map(ignored -> CliExitCode.SCHEDULER_WORK_FAILED)
                .orElse(CliExitCode.COMPLETED);
        writeBounded(stdout, String.join("\n",
                "status=" + status,
                "exitCode=" + exitCode.code(),
                "queueId=" + queue.queueId(),
                "queueRevision=" + queue.revision(),
                "pendingWorkItems=" + queue.pendingCount(),
                "completedWorkItems=" + queue.completedWorkItemIds().size(),
                "failedWorkItems=" + queue.failedWorkItemIds().size(),
                "runRecords=" + execution.runRecordStore().references().size()) + "\n");
        return exitCode.code();
    }

    private int executeRunRecordList(
            RunRecordListCliCommand command,
            PrintStream stdout) throws IOException {
        List<String> references = new FileSystemRunRecordStore(
                command.runRecordRoot()).recentReferences(command.limit());
        List<String> output = new ArrayList<>();
        output.add("status=" + (references.isEmpty() ? "EMPTY" : "AVAILABLE"));
        output.add("exitCode=0");
        output.add("requestedLimit=" + command.limit());
        output.add("returnedRecords=" + references.size());
        for (int index = 0; index < references.size(); index++) {
            output.add("runRecordReference." + (index + 1)
                    + "=" + references.get(index));
        }
        writeBounded(stdout, String.join("\n", output) + "\n");
        return 0;
    }

    private int executeSchedulerStatus(
            SchedulerStatusCliCommand command,
            PrintStream stdout) throws IOException {
        SchedulerQueueState state;
        try {
            state = new FileSystemSchedulerQueueStore(
                    command.queueRoot()).resolve(command.queueId());
        } catch (MissingSchedulerQueueStateException exception) {
            throw new CliUsageException(
                    "queue configuration is invalid: "
                            + safeMessage(exception),
                    exception);
        }
        SchedulerQueueStatus status =
                SchedulerQueueStatus.project(state);
        int returned = Math.min(
                command.limit(), status.workItems().size());
        List<String> output = new ArrayList<>(List.of(
                "status=" + (status.workItems().isEmpty()
                        ? "EMPTY" : "AVAILABLE"),
                "exitCode=0",
                "queueId=" + status.queueId(),
                "queueRevision=" + status.revision(),
                "maxWorkItems=" + status.maxWorkItems(),
                "totalWorkItems=" + status.workItems().size(),
                "readyWorkItems=" + status.count(
                        SchedulerQueueStatus.WorkState.READY),
                "blockedWorkItems=" + status.count(
                        SchedulerQueueStatus.WorkState.BLOCKED),
                "activeWorkItems=" + status.count(
                        SchedulerQueueStatus.WorkState.ACTIVE),
                "verifiedWorkItems=" + status.count(
                        SchedulerQueueStatus.WorkState.VERIFIED),
                "failedWorkItems=" + status.count(
                        SchedulerQueueStatus.WorkState.FAILED),
                "requestedLimit=" + command.limit(),
                "returnedWorkItems=" + returned));
        for (int index = 0; index < returned; index++) {
            SchedulerQueueStatus.WorkStatus work =
                    status.workItems().get(index);
            output.add("workItem." + (index + 1)
                    + "=" + work.workItemId() + "," + work.state());
        }
        writeBounded(stdout, String.join("\n", output) + "\n");
        return 0;
    }

    private int executeSchedulerRecoveryStatus(
            SchedulerRecoveryStatusCliCommand command,
            PrintStream stdout) throws IOException {
        SchedulerRecoveryStatus status;
        try {
            status = new SchedulerRecoveryStatusReader(
                    new FileSystemSchedulerQueueStore(
                            command.queueRoot()),
                    new FileSystemAgentRuntimeStateStore(
                            command.runtimeRoot()),
                    new FileSystemPendingFinalizationStore(
                            command.cycleCheckpointRoot()),
                    new FileSystemRunRecordStore(
                            command.runRecordRoot()))
                    .read(command.queueId());
        } catch (MissingSchedulerQueueStateException exception) {
            throw new CliUsageException(
                    "queue configuration is invalid: "
                            + safeMessage(exception),
                    exception);
        }
        List<String> output = new ArrayList<>(List.of(
                "status=" + status.phase(),
                "exitCode=0",
                "queueId=" + status.queueId(),
                "queueRevision=" + status.queueRevision(),
                "checkpointPresent=" + status.goalId().isPresent(),
                "workerLiveness=UNKNOWN"));
        status.goalId().ifPresent(value ->
                output.add("goalId=" + value));
        status.agentRunId().ifPresent(value ->
                output.add("agentRunId=" + value));
        status.replacementAgentRunId().ifPresent(value ->
                output.add("replacementAgentRunId=" + value));
        status.runtimeRevision().ifPresent(value ->
                output.add("runtimeRevision=" + value));
        status.goalStatus().ifPresent(value ->
                output.add("goalStatus=" + value));
        status.agentRunStatus().ifPresent(value ->
                output.add("agentRunStatus=" + value));
        if (status.runtimeRevision().isPresent()) {
            output.add("agentRunAttempts="
                    + status.agentRunAttempts());
        }
        status.queueWorkState().ifPresent(value ->
                output.add("queueWorkState=" + value));
        status.runRecordReference().ifPresent(value ->
                output.add("runRecordReference=" + value));
        status.runRecordVerificationStatus().ifPresent(value ->
                output.add("runRecordVerificationStatus=" + value));
        writeBounded(stdout, String.join("\n", output) + "\n");
        return 0;
    }

    private int executeSchedulerDrain(
            SchedulerDrainCliCommand command,
            PrintStream stdout) throws IOException {
        SchedulerExecution execution =
                schedulerExecution(command, "scheduler-drain");
        SchedulerDrainResult result = new ForegroundSchedulerDrain(
                execution.worker()).drain(
                        command.maxCycles(),
                        command.leaseDuration());
        CliExitCode exitCode = result.failed() == 1
                ? CliExitCode.SCHEDULER_WORK_FAILED
                : CliExitCode.COMPLETED;
        DurableSingleWorkerSchedulerQueue queue = execution.queue();
        writeBounded(stdout, String.join("\n",
                "status=" + result.stopReason(),
                "exitCode=" + exitCode.code(),
                "queueId=" + queue.queueId(),
                "queueRevision=" + queue.revision(),
                "cyclesInvoked=" + result.cyclesInvoked(),
                "verifiedCompletedCycles=" + result.verifiedCompleted(),
                "failedCycles=" + result.failed(),
                "pendingWorkItems=" + queue.pendingCount(),
                "completedWorkItems=" + queue.completedWorkItemIds().size(),
                "failedWorkItems=" + queue.failedWorkItemIds().size(),
                "runRecords=" + execution.runRecordStore().references().size()) + "\n");
        return exitCode.code();
    }

    private SchedulerExecution schedulerExecution(
            SchedulerExecutionCliCommand command,
            String commandName) throws IOException {
        DurableSingleWorkerSchedulerQueue queue;
        DurableAgentRunWorker worker;
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(command.runRecordRoot());
        try {
            queue = DurableSingleWorkerSchedulerQueue.recover(
                    command.queueId(),
                    new FileSystemSchedulerQueueStore(command.queueRoot()));
            worker = DurableAgentRunWorker.processIsolated(
                    queue,
                    new FileSystemAgentRuntimeStateStore(command.runtimeRoot()),
                    new FileSystemExternalEffectLedgerStore(command.externalEffectRoot()),
                    new FileSystemPendingFinalizationStore(command.cycleCheckpointRoot()),
                    command.projectRoot(),
                    command.evidenceRoot(),
                    command.runRecordRoot(),
                    command.invocationRoot(),
                    runRecordStore,
                    command.ownerId(),
                    Clock.systemUTC(),
                    command.processTimeout(),
                    AgentRunRetryPolicy.of(command.maxAttempts()));
        } catch (MissingSchedulerQueueStateException exception) {
            throw new CliUsageException(
                    "queue configuration is invalid: " + safeMessage(exception),
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    commandName + " input is invalid: " + safeMessage(exception),
                    exception);
        }
        return new SchedulerExecution(queue, worker, runRecordStore);
    }

    private int executeSchedulerSubmit(
            SchedulerSubmitCliCommand command,
            PrintStream stdout) throws IOException {
        GovernedRunInputs inputs = governedRunInputs(
                command.projectRoot(), command.taskId());
        WorkspaceSnapshot snapshot;
        DurableSubmissionResult result;
        try {
            snapshot = new RepositoryMemorySnapshotCollector().collect(
                    command.projectRoot(),
                    command.occurredAt(),
                    inputs.approvedTask(),
                    inputs.repositoryMemory());
            MessageEnvelope workMessage = new MessageEnvelope(
                    command.messageId(),
                    command.correlationId(),
                    Optional.empty(),
                    command.logicalRunId(),
                    command.producer(),
                    command.occurredAt(),
                    new WorkPayload(
                            snapshot.approvedTaskRevision(),
                            snapshot.snapshotId(),
                            inputs.approvedTask().allowedTools(),
                            Optional.of(new WorkPayload.ExecutionInput(
                                    command.targetPath(),
                                    command.expectedSha256()))));
            DurableSubmissionManifest manifest = new DurableSubmissionManifest(
                    command.queueId(),
                    command.maxWorkItems(),
                    command.requiredCapability(),
                    workMessage);
            result = new DurableWorkSubmissionService(
                    new FileSystemSubmissionManifestStore(command.submissionRoot()),
                    new FileSystemSchedulerQueueStore(command.queueRoot()))
                    .submit(manifest);
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    "scheduler-submit input is invalid: " + safeMessage(exception),
                    exception);
        }

        String status = result.workAdmitted() ? "ADMITTED" : "REPLAYED";
        writeBounded(stdout, String.join("\n",
                "status=" + status,
                "exitCode=0",
                "submissionId=" + result.submissionId(),
                "queueId=" + result.queueId(),
                "queueRevision=" + result.queueRevision(),
                "manifestCreated=" + result.manifestCreated(),
                "queueCreated=" + result.queueCreated(),
                "workAdmitted=" + result.workAdmitted(),
                "workspaceSnapshotId=" + snapshot.snapshotId()) + "\n");
        return 0;
    }

    private int executeGeneratedSubmit(
            GeneratedSubmitCliCommand command,
            PrintStream stdout) throws IOException {
        FileSystemSubmissionManifestStore manifestStore =
                new FileSystemSubmissionManifestStore(command.submissionRoot());
        DurableSubmissionResult result;
        try {
            GeneratedSubmissionRequest request = new GeneratedSubmissionRequest(
                    command.submissionId(),
                    command.maxWorkItems(),
                    command.requiredCapability(),
                    command.producer(),
                    command.taskId(),
                    command.targetPath(),
                    command.expectedSha256());
            result = new GeneratedInputSubmissionService(
                    manifestStore,
                    new FileSystemSchedulerQueueStore(command.queueRoot()),
                    Clock.systemUTC())
                    .submit(request, (identities, occurredAt) ->
                            generatedWorkMessage(command, identities, occurredAt));
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(
                    "scheduler-submit-generated input is invalid: "
                            + safeMessage(exception),
                    exception);
        }

        // The manifest is the sole generated-input recovery record; read the generated
        // identities, occurrence time, and snapshot back from it for auditable output.
        MessageEnvelope workMessage =
                manifestStore.resolve(command.submissionId()).workMessage();
        WorkPayload work = (WorkPayload) workMessage.payload();
        String status = result.workAdmitted() ? "ADMITTED" : "REPLAYED";
        writeBounded(stdout, String.join("\n",
                "status=" + status,
                "exitCode=0",
                "submissionId=" + result.submissionId(),
                "queueId=" + result.queueId(),
                "correlationId=" + workMessage.correlationId(),
                "logicalRunId=" + workMessage.logicalRunId(),
                "occurredAt=" + workMessage.occurredAt(),
                "queueRevision=" + result.queueRevision(),
                "manifestCreated=" + result.manifestCreated(),
                "queueCreated=" + result.queueCreated(),
                "workAdmitted=" + result.workAdmitted(),
                "workspaceSnapshotId=" + work.snapshotId()) + "\n");
        return 0;
    }

    private MessageEnvelope generatedWorkMessage(
            GeneratedSubmitCliCommand command,
            GeneratedSubmissionIdentities identities,
            Instant occurredAt) {
        GovernedRunInputs inputs = governedRunInputs(
                command.projectRoot(), command.taskId());
        WorkspaceSnapshot snapshot = new RepositoryMemorySnapshotCollector().collect(
                command.projectRoot(),
                occurredAt,
                inputs.approvedTask(),
                inputs.repositoryMemory());
        return new MessageEnvelope(
                identities.submissionId(),
                identities.correlationId(),
                Optional.empty(),
                identities.logicalRunId(),
                command.producer(),
                occurredAt,
                new WorkPayload(
                        snapshot.approvedTaskRevision(),
                        snapshot.snapshotId(),
                        inputs.approvedTask().allowedTools(),
                        Optional.of(new WorkPayload.ExecutionInput(
                                command.targetPath(),
                                command.expectedSha256()))));
    }

    private GovernedRunInputs governedRunInputs(RunCliCommand command) {
        return governedRunInputs(command.projectRoot(), command.taskId());
    }

    private GovernedRunInputs governedRunInputs(Path projectRoot, String taskId) {
        try {
            ProjectContext context = new ProjectContextReader().read(projectRoot);
            ApprovedTask approvedTask = new ApprovedTaskReader().read(context);
            if (!approvedTask.taskId().equals(taskId)) {
                throw new CliUsageException(
                        "task-id does not match the active repository task");
            }
            if (!approvedTask.allows(ReadFileTool.NAME)) {
                throw new CliUsageException("active task does not allow read-file");
            }
            return new GovernedRunInputs(approvedTask, context);
        } catch (CliUsageException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new CliUsageException(
                    "project configuration is invalid: " + safeMessage(exception),
                    exception);
        }
    }

    private record GovernedRunInputs(
            ApprovedTask approvedTask,
            ProjectContext repositoryMemory) {
    }

    private record SchedulerExecution(
            DurableSingleWorkerSchedulerQueue queue,
            DurableAgentRunWorker worker,
            FileSystemRunRecordStore runRecordStore) {
    }

    @FunctionalInterface
    interface BrainComposer {
        BrainSummary compose(BrainCompositionInput input);
    }

    record BrainCompositionInput(
            WorkspaceSnapshot snapshot,
            ProjectContext repositoryMemory,
            FinalizedAgentRun finalized,
            List<GraphNode> decisions,
            List<GraphEdge> justificationEdges,
            String taskId) {
    }

    record BrainSummary(
            String snapshotId,
            int workspaceObservations,
            String memoryFreshness,
            int graphNodes,
            int graphEdges,
            long graphDecisions,
            int impactExecutions,
            int impactDecisions) {
    }

    private CliExitCode exitCode(RunRecord record) {
        Optional<ToolFailureCode> failureCode = record.toolResult().failureCode();
        return CliExitCode.from(
                record.finalStopReason(),
                record.verification().status(),
                failureCode);
    }

    private void writeError(PrintStream stream, CliExitCode exitCode, String message) {
        writeBounded(stream, String.join("\n",
                "status=ERROR",
                "exitCode=" + exitCode.code(),
                "message=" + safeValue(message)) + "\n");
    }

    private void writeBounded(PrintStream stream, String value) {
        stream.print(UnicodeText.prefix(
                value,
                MAX_DIAGNOSTIC_CHARACTERS));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static String safeValue(String value) {
        String safe = Objects.requireNonNullElse(value, "")
                .replace('\r', ' ')
                .replace('\n', ' ');
        return UnicodeText.prefix(safe, 512);
    }
}
