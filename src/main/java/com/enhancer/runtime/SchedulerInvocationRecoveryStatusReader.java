package com.enhancer.runtime;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessagePayload;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.ReadFileTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Takes a bounded stable read-only sample of the checkpoint-correlated private invocation spool.
 */
public final class SchedulerInvocationRecoveryStatusReader {
    private final SchedulerRecoveryStatusReader schedulerReader;
    private final AgentRuntimeStateStore runtimeStore;
    private final RunRecordStore runRecordStore;
    private final Path invocationRoot;

    public SchedulerInvocationRecoveryStatusReader(
            SchedulerRecoveryStatusReader schedulerReader,
            AgentRuntimeStateStore runtimeStore,
            RunRecordStore runRecordStore,
            Path invocationRoot) {
        this.schedulerReader = Objects.requireNonNull(
                schedulerReader, "schedulerReader must not be null");
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
        this.invocationRoot = Objects.requireNonNull(
                invocationRoot, "invocationRoot must not be null")
                .toAbsolutePath()
                .normalize();
    }

    public SchedulerInvocationRecoveryStatus read(String queueId) throws IOException {
        SchedulerRecoveryStatus firstScheduler = schedulerReader.read(queueId);
        Optional<AgentRuntimeState> firstRuntime = resolveRuntime(firstScheduler);
        Optional<SpoolSample> firstSpool = sample(firstScheduler, firstRuntime);

        SchedulerRecoveryStatus secondScheduler = schedulerReader.read(queueId);
        Optional<AgentRuntimeState> secondRuntime = resolveRuntime(secondScheduler);
        Optional<SpoolSample> secondSpool = sample(secondScheduler, secondRuntime);

        requireStableScheduler(firstScheduler, secondScheduler);
        requireStableRuntime(firstRuntime, secondRuntime);
        if (!firstSpool.equals(secondSpool)) {
            throw changed("invocation spool observation differs between samples");
        }
        return SchedulerInvocationRecoveryStatus.project(
                firstScheduler,
                firstRuntime,
                firstSpool.map(SpoolSample::state));
    }

    private Optional<AgentRuntimeState> resolveRuntime(
            SchedulerRecoveryStatus scheduler) throws IOException {
        if (scheduler.runtimeRevision().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(runtimeStore.resolve(
                scheduler.goalId().orElseThrow()));
    }

    private Optional<SpoolSample> sample(
            SchedulerRecoveryStatus scheduler,
            Optional<AgentRuntimeState> runtime) throws IOException {
        if (scheduler.goalId().isEmpty() || scheduler.agentRunId().isEmpty()) {
            return Optional.empty();
        }
        if (runtime.isEmpty()) {
            return Optional.empty();
        }
        AgentRuntimeState state = runtime.orElseThrow();
        WorkItem work = state.goal().workItem();
        String goalId = scheduler.goalId().orElseThrow();
        String agentRunId = scheduler.agentRunId().orElseThrow();
        if (!goalId.equals(state.goal().goalId())
                || state.agentRuns().stream().noneMatch(run ->
                        agentRunId.equals(run.agentRunId()))) {
            throw new IOException("invocation runtime does not match Scheduler correlation");
        }
        rejectSymbolic(invocationRoot, "invocation root");
        Path goalRoot = invocationRoot.resolve(goalId);
        Path cycleRoot = goalRoot.resolve(agentRunId);
        rejectSymbolic(goalRoot, "Goal invocation root");
        rejectSymbolic(cycleRoot, "AgentRun invocation root");
        if (!Files.isDirectory(cycleRoot, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.of(SpoolSample.absent());
        }
        Optional<TransportMessage> workMessage = soleMessage(
                cycleRoot.resolve(IsolatedWorkerMain.WORK_SPOOL), "work");
        Optional<TransportMessage> resultMessage = soleMessage(
                cycleRoot.resolve(IsolatedWorkerMain.RESULT_SPOOL), "result");
        if (workMessage.isEmpty() && resultMessage.isPresent()) {
            throw new IOException("the invocation result exists without work");
        }
        if (workMessage.isPresent()) {
            validateWork(workMessage.orElseThrow(), work);
        }
        if (resultMessage.isPresent()) {
            validateResult(
                    resultMessage.orElseThrow(),
                    workMessage.orElseThrow(),
                    work);
        }
        return Optional.of(new SpoolSample(
                new SchedulerInvocationRecoveryStatus.InvocationSpoolState(
                        true,
                        workMessage.isPresent(),
                        resultMessage.isPresent()),
                workMessage,
                resultMessage));
    }

    private static void rejectSymbolic(Path path, String label) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a symbolic link");
        }
    }

    private static Optional<TransportMessage> soleMessage(Path root, String direction)
            throws IOException {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try (var entries = Files.list(root)) {
            List<Path> messages = entries
                    .filter(path -> path.getFileName().toString().endsWith(
                            FileSpoolMessageTransport.FILE_SUFFIX))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
            if (messages.size() > 1) {
                throw new IOException("the " + direction + " spool holds several messages");
            }
            return messages.isEmpty()
                    ? Optional.empty()
                    : Optional.of(FileSpoolMessageTransport.read(messages.get(0)));
        }
    }

    private static void validateWork(TransportMessage actual, WorkItem work)
            throws IOException {
        if (!actual.destination().equals(
                DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL))) {
            throw new IOException("the invocation work message has the wrong destination");
        }
        if (!actual.envelope().equals(work.workMessage())) {
            throw new IOException("the invocation work message does not match the runtime WorkItem");
        }
    }

    private void validateResult(
            TransportMessage resultMessage,
            TransportMessage workMessage,
            WorkItem work) throws IOException {
        if (!resultMessage.destination().equals(
                DeliveryDestination.queue(IsolatedWorkerMain.RESULT_DESTINATION))) {
            throw new IOException("the invocation result message has the wrong destination");
        }
        MessageEnvelope source = workMessage.envelope();
        MessageEnvelope result = resultMessage.envelope();
        requireEqual(source.correlationId(), result.correlationId(), "correlation identity");
        requireEqual(source.logicalRunId(), result.logicalRunId(), "logical run identity");
        requireEqual(source.messageId(), result.causationId().orElseThrow(() ->
                new IOException("the invocation result names no causation identity")),
                "causation identity");
        MessagePayload payload = result.payload();
        if (!(payload instanceof ResultPayload resultPayload)) {
            throw new IOException("the invocation result does not carry a ResultPayload");
        }
        requireEqual(work.taskRevision().taskId(), resultPayload.taskId(), "task identity");
        RunRecord record = runRecordStore.resolve(
                resultPayload.runRecordReference()).record();
        try {
            DurableAgentRunFinalizer.requireBinding(record, work);
        } catch (IllegalArgumentException mismatch) {
            throw new IOException(mismatch.getMessage(), mismatch);
        }
        if (!record.toolRequest().toolName().equals(ReadFileTool.NAME)) {
            throw new IOException("the RunRecord Tool request does not match isolated execution");
        }
        String target = record.toolRequest().arguments().get(ReadFileTool.PATH_ARGUMENT);
        String expectedTarget = work.executionInput()
                .map(input -> input.targetPath())
                .orElse(work.taskRevision().sourceDocument());
        String expectedDigest = work.executionInput()
                .map(input -> input.expectedContentSha256())
                .orElse(work.taskRevision().sourceSha256());
        if (!expectedTarget.equals(target)
                || (record.verification().status() != VerificationStatus.NOT_PERFORMED
                && !record.expectedContentSha256().equals(Optional.of(expectedDigest)))) {
            throw new IOException("the RunRecord execution input does not match the runtime WorkItem");
        }
        if (record.verification().status() != resultPayload.verificationStatus()) {
            throw new IOException("the invocation result verification status does not match its RunRecord");
        }
    }

    private static void requireEqual(String expected, String actual, String label)
            throws IOException {
        if (!expected.equals(actual)) {
            throw new IOException("the invocation result " + label + " does not match work");
        }
    }

    private static void requireStableScheduler(
            SchedulerRecoveryStatus first,
            SchedulerRecoveryStatus second)
            throws ConcurrentSchedulerInvocationInspectionException {
        if (!first.queueId().equals(second.queueId())
                || first.queueRevision() != second.queueRevision()
                || first.phase() != second.phase()
                || !first.goalId().equals(second.goalId())
                || !first.agentRunId().equals(second.agentRunId())
                || !first.replacementAgentRunId().equals(second.replacementAgentRunId())
                || !first.runRecordReference().equals(second.runRecordReference())
                || !first.runtimeRevision().equals(second.runtimeRevision())) {
            throw changed("Scheduler recovery observation differs between samples");
        }
    }

    private static void requireStableRuntime(
            Optional<AgentRuntimeState> first,
            Optional<AgentRuntimeState> second)
            throws ConcurrentSchedulerInvocationInspectionException {
        if (first.isEmpty() != second.isEmpty()
                || (first.isPresent() && (first.orElseThrow().revision()
                        != second.orElseThrow().revision()
                || !first.orElseThrow().goal().goalId().equals(
                        second.orElseThrow().goal().goalId())))) {
            throw changed("AgentRuntime observation differs between samples");
        }
    }

    private static ConcurrentSchedulerInvocationInspectionException changed(String reason) {
        return new ConcurrentSchedulerInvocationInspectionException(reason);
    }

    private record SpoolSample(
            SchedulerInvocationRecoveryStatus.InvocationSpoolState state,
            Optional<TransportMessage> work,
            Optional<TransportMessage> result) {
        private static SpoolSample absent() {
            return new SpoolSample(
                    new SchedulerInvocationRecoveryStatus.InvocationSpoolState(false, false, false),
                    Optional.empty(),
                    Optional.empty());
        }
    }
}
