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
import com.enhancer.loop.AgentLoop;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.AgentRunController;
import com.enhancer.loop.AgentRunFinalizer;
import com.enhancer.loop.AgentRunResult;
import com.enhancer.loop.AgentRunState;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.loop.ToolFailureClassifier;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.FinalizedAgentRun;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ReadFileTool;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
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
import java.time.Duration;
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
            return executeReplay((ReplayCliCommand) command, stdout);
        } catch (CliUsageException exception) {
            writeError(stderr, CliExitCode.USAGE_OR_CONFIGURATION, exception.getMessage());
            return CliExitCode.USAGE_OR_CONFIGURATION.code();
        } catch (Exception exception) {
            writeError(stderr, CliExitCode.INTERNAL_ERROR, safeMessage(exception));
            return CliExitCode.INTERNAL_ERROR.code();
        }
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

    private GovernedRunInputs governedRunInputs(RunCliCommand command) {
        try {
            ProjectContext context = new ProjectContextReader().read(command.projectRoot());
            ApprovedTask approvedTask = new ApprovedTaskReader().read(context);
            if (!approvedTask.taskId().equals(command.taskId())) {
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
        String bounded = value.length() <= MAX_DIAGNOSTIC_CHARACTERS
                ? value
                : value.substring(0, MAX_DIAGNOSTIC_CHARACTERS);
        stream.print(bounded);
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
        return safe.length() <= 512 ? safe : safe.substring(0, 512);
    }
}
