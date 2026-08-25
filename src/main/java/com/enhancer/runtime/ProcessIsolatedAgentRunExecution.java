package com.enhancer.runtime;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeadLetter;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportOutcome;
import com.enhancer.run.MissingRunRecordException;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs one dispatched WorkItem in a child process and returns the persisted RunRecord reference.
 *
 * <p>Work travels out through a spool and the result travels back through a second one, both
 * under an invocation root private to this Goal and AgentRun. The spool adapter deliberately
 * neither claims nor deletes a message and the child reads the single message it finds, so
 * isolation between cycles comes from that namespace rather than from the read; exactly one
 * valid message is expected in each direction and anything else fails closed.
 *
 * <p>The returned result is a claim, never authority. Before a reference is returned this class
 * checks that the result envelope correlates to the work that was dispatched, that its payload is
 * exactly a {@link ResultPayload}, that the reference resolves in the same shared
 * {@link RunRecordStore} the finalizer uses, that the resolved record binds to the dispatched
 * task, source, and execution input, and that the status the child claimed equals the resolved
 * record's own status. {@code DurableAgentRunFinalizer} remains the final authority and still
 * derives the runtime terminal state and queue disposition from the record.
 *
 * <p>On re-entry an already-published valid result is returned without launching a second child.
 * If publication was lost after persistence, the deterministic AgentRun-bound reference is
 * point-resolved and validated through the same binding checks before execution is skipped.
 */
public final class ProcessIsolatedAgentRunExecution implements AgentRunExecution {
    private static final String EVENT_PRODUCER_ID =
            "process-isolated-agent-run-execution";

    private final Path invocationRoot;
    private final Path projectRoot;
    private final Path evidenceRoot;
    private final Path runRecordRoot;
    private final RunRecordStore runRecordStore;
    private final WorkerProcessLauncher launcher;
    private final Duration timeout;
    private final ProcessTimeoutFactStore timeoutStore;
    private final Clock clock;
    private final Optional<RuntimeEventRecorder> eventRecorder;

    public ProcessIsolatedAgentRunExecution(
            Path invocationRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            RunRecordStore runRecordStore,
            WorkerProcessLauncher launcher,
            Duration timeout) {
        this(
                invocationRoot,
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                runRecordStore,
                launcher,
                timeout,
                new FileSystemProcessTimeoutFactStore(
                        absolute(invocationRoot, "invocationRoot")
                                .resolve(".process-timeouts")),
                Clock.systemUTC(),
                Optional.empty());
    }

    public ProcessIsolatedAgentRunExecution(
            Path invocationRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            RunRecordStore runRecordStore,
            WorkerProcessLauncher launcher,
            Duration timeout,
            RuntimeEventRecorder eventRecorder) {
        this(
                invocationRoot,
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                runRecordStore,
                launcher,
                timeout,
                new FileSystemProcessTimeoutFactStore(
                        absolute(invocationRoot, "invocationRoot")
                                .resolve(".process-timeouts")),
                Clock.systemUTC(),
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    ProcessIsolatedAgentRunExecution(
            Path invocationRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            RunRecordStore runRecordStore,
            WorkerProcessLauncher launcher,
            Duration timeout,
            ProcessTimeoutFactStore timeoutStore,
            Clock clock) {
        this(
                invocationRoot,
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                runRecordStore,
                launcher,
                timeout,
                timeoutStore,
                clock,
                Optional.empty());
    }

    ProcessIsolatedAgentRunExecution(
            Path invocationRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            RunRecordStore runRecordStore,
            WorkerProcessLauncher launcher,
            Duration timeout,
            ProcessTimeoutFactStore timeoutStore,
            Clock clock,
            RuntimeEventRecorder eventRecorder) {
        this(
                invocationRoot,
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                runRecordStore,
                launcher,
                timeout,
                timeoutStore,
                clock,
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    private ProcessIsolatedAgentRunExecution(
            Path invocationRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            RunRecordStore runRecordStore,
            WorkerProcessLauncher launcher,
            Duration timeout,
            ProcessTimeoutFactStore timeoutStore,
            Clock clock,
            Optional<RuntimeEventRecorder> eventRecorder) {
        this.invocationRoot = absolute(invocationRoot, "invocationRoot");
        this.projectRoot = absolute(projectRoot, "projectRoot");
        this.evidenceRoot = absolute(evidenceRoot, "evidenceRoot");
        this.runRecordRoot = absolute(runRecordRoot, "runRecordRoot");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
        this.launcher = Objects.requireNonNull(launcher, "launcher must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        this.timeoutStore = Objects.requireNonNull(
                timeoutStore, "timeoutStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventRecorder = Objects.requireNonNull(
                eventRecorder, "eventRecorder must not be null");
    }

    @Override
    public String execute(AgentRunDispatch dispatch) throws IOException {
        Objects.requireNonNull(dispatch, "dispatch must not be null");
        WorkItem workItem = dispatch.workItem();
        AgentLoopAgentRunExecution.requireLegacyExecutableWork(workItem);
        Path cycleRoot = cycleRoot(dispatch);

        Optional<ResolvedProcessTimeoutFact> recoveredTimeout = timeoutStore.find(
                dispatch.goalId(), dispatch.agentRunId());
        if (recoveredTimeout.isPresent()) {
            ResolvedProcessTimeoutFact resolved = recoveredTimeout.orElseThrow();
            requireTimeoutBinding(resolved.fact(), dispatch);
            recordTimeoutEvent(resolved, dispatch);
            throw timeoutFailure(resolved.fact());
        }

        Optional<String> recovered = publishedResult(cycleRoot, workItem);
        if (recovered.isPresent()) {
            return recovered.orElseThrow();
        }
        Optional<String> pointRecovered = pointRecoveredResult(dispatch, workItem);
        if (pointRecovered.isPresent()) {
            return pointRecovered.orElseThrow();
        }

        spoolWork(cycleRoot, workItem);
        IsolatedWorkerOutcome outcome = launcher.run(
                IsolatedWorkerMain.class,
                List.of(
                        cycleRoot.toString(),
                        projectRoot.toString(),
                        evidenceRoot.toString(),
                        runRecordRoot.toString(),
                        workItem.workItemId(),
                        workItem.requiredCapability(),
                        dispatch.goalId(),
                        dispatch.agentRunId()),
                timeout);

        if (outcome.status() == IsolatedWorkerStatus.TIMED_OUT) {
            ProcessTimeoutFact fact = ProcessTimeoutFact.create(
                    clock.instant(),
                    eventBinding(dispatch),
                    dispatch.agentRunId(),
                    timeout,
                    outcome.reason().orElseThrow());
            ResolvedProcessTimeoutFact persisted = timeoutStore.persist(fact);
            recordTimeoutEvent(persisted, dispatch);
            throw timeoutFailure(persisted.fact());
        }
        if (outcome.status() != IsolatedWorkerStatus.COMPLETED) {
            throw new IOException("the isolated worker did not complete: "
                    + outcome.status() + " (" + outcome.reason().orElse("no reason") + ")");
        }
        int exitCode = outcome.exitCode().orElseThrow();
        if (exitCode != IsolatedWorkerMain.EXIT_RESULT_PUBLISHED) {
            throw new IOException(
                    "the isolated worker exited " + exitCode + " without publishing a result");
        }
        return publishedResult(cycleRoot, workItem).orElseThrow(() -> new IOException(
                "the isolated worker reported success but published no valid result"));
    }

    private void recordTimeoutEvent(
            ResolvedProcessTimeoutFact source,
            AgentRunDispatch dispatch) throws IOException {
        if (eventRecorder.isEmpty()) {
            return;
        }
        ProcessTimeoutFact fact = source.fact();
        eventRecorder.orElseThrow().recordAndPublish(RuntimeEvent.create(
                fact.occurredAt(),
                fact.binding(),
                fact.agentRunId(),
                Optional.of(dispatch.workItem().workMessage().messageId()),
                EVENT_PRODUCER_ID,
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.PROCESS),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.PROCESS_TIMEOUT,
                        source.reference(),
                        Optional.of(source.sha256())))));
    }

    private RuntimeEventBinding eventBinding(AgentRunDispatch dispatch) {
        WorkItem workItem = dispatch.workItem();
        return new RuntimeEventBinding(
                dispatch.goalId(),
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
    }

    private void requireTimeoutBinding(
            ProcessTimeoutFact fact, AgentRunDispatch dispatch) throws IOException {
        ProcessTimeoutFact expected = ProcessTimeoutFact.create(
                fact.occurredAt(),
                eventBinding(dispatch),
                dispatch.agentRunId(),
                timeout,
                fact.reason());
        if (!fact.equals(expected)) {
            throw new IOException(
                    "the persisted process timeout does not match the dispatched work");
        }
    }

    private IOException timeoutFailure(ProcessTimeoutFact fact) {
        return new IOException("the isolated worker did not complete: "
                + IsolatedWorkerStatus.TIMED_OUT
                + " (" + fact.reason() + ")");
    }

    /**
     * Removes the per-cycle transport namespace only after the worker has durably checkpointed
     * the returned RunRecord reference. Evidence and RunRecords are separate roots and are never
     * touched. A missing tree is already retired; symbolic-link cycle boundaries fail closed.
     */
    @Override
    public void cleanupAfterCheckpoint(AgentRunDispatch dispatch) throws IOException {
        Objects.requireNonNull(dispatch, "dispatch must not be null");
        Path cycleRoot = cycleRoot(dispatch);
        Path goalRoot = cycleRoot.getParent();
        rejectSymbolicBoundary(invocationRoot, "invocation root");
        rejectSymbolicBoundary(goalRoot, "Goal invocation root");
        rejectSymbolicBoundary(cycleRoot, "AgentRun invocation root");
        if (Files.exists(cycleRoot, LinkOption.NOFOLLOW_LINKS)) {
            deleteTree(cycleRoot);
        }
        try {
            Files.deleteIfExists(goalRoot);
        } catch (DirectoryNotEmptyException siblingCycleExists) {
            // Another AgentRun namespace under the Goal is not owned by this cleanup.
        }
    }

    private Path cycleRoot(AgentRunDispatch dispatch) {
        return invocationRoot
                .resolve(dispatch.goalId())
                .resolve(dispatch.agentRunId());
    }

    private static void rejectSymbolicBoundary(Path path, String label) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a symbolic link");
        }
    }

    private static void deleteTree(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                    throws IOException {
                if (failure != null) {
                    throw failure;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Spools the work message, or leaves an existing one in place.
     *
     * <p>Re-entry after a child died before publishing must not add a second work message: the
     * child expects exactly one and would otherwise refuse the whole cycle. A spool that already
     * holds several messages, a foreign route, or a foreign envelope is a corrupt cycle and fails
     * closed before launch.
     */
    private void spoolWork(Path cycleRoot, WorkItem workItem) throws IOException {
        Path workSpool = cycleRoot.resolve(IsolatedWorkerMain.WORK_SPOOL);
        Optional<Path> existing = soleSpooledMessage(workSpool, "work");
        if (existing.isPresent()) {
            TransportMessage actual =
                    FileSpoolMessageTransport.read(existing.orElseThrow());
            DeliveryDestination expectedDestination =
                    DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL);
            if (!actual.destination().equals(expectedDestination)) {
                throw new IOException(
                        "the existing work message has the wrong destination");
            }
            if (!actual.envelope().equals(workItem.workMessage())) {
                throw new IOException(
                        "the existing work message does not equal the dispatched work");
            }
            return;
        }
        TransportOutcome outcome = new FileSpoolMessageTransport(
                workSpool, BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL),
                        workItem.workMessage()));
        if (!outcome.status().isAccepted()) {
            throw new IOException("the work message could not be spooled: "
                    + outcome.status() + " (" + outcome.reason().orElse("no reason") + ")");
        }
    }

    /**
     * Returns the reference from a valid published result, or empty when none is published yet.
     *
     * <p>A result that exists but fails any check is not "not published" — it is a corrupt or
     * mismatched cycle, and it fails closed rather than being silently retried.
     */
    private Optional<String> publishedResult(Path cycleRoot, WorkItem workItem)
            throws IOException {
        Optional<Path> spooled = soleSpooledMessage(
                cycleRoot.resolve(IsolatedWorkerMain.RESULT_SPOOL), "result");
        if (spooled.isEmpty()) {
            return Optional.empty();
        }
        TransportMessage message = FileSpoolMessageTransport.read(spooled.orElseThrow());
        DeliveryDestination destination =
                DeliveryDestination.queue(IsolatedWorkerMain.RESULT_DESTINATION);
        IsolatedResultMessageHandler handler =
                new IsolatedResultMessageHandler(workItem, runRecordStore);
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(destination, "isolated-result-validator", handler);
        List<DeliveryOutcome> outcomes =
                bus.publish(message.destination(), message.envelope());
        if (outcomes.size() != 1
                || outcomes.get(0).status() != DeliveryStatus.DELIVERED) {
            DeliveryStatus status = outcomes.size() == 1
                    ? outcomes.get(0).status()
                    : null;
            String reason = bus.deadLetters().stream()
                    .findFirst()
                    .map(DeadLetter::reason)
                    .orElse("no handler reason");
            throw new IOException("the result Message Bus delivery was "
                    + (status == null ? "INVALID" : status)
                    + " (" + reason + ")");
        }
        return handler.acceptedReference();
    }

    private Optional<String> pointRecoveredResult(
            AgentRunDispatch dispatch,
            WorkItem workItem) throws IOException {
        String reference = AgentRunRecordIdentity.reference(
                dispatch.goalId(), dispatch.agentRunId());
        RunRecord record;
        try {
            record = runRecordStore.resolve(reference).record();
        } catch (MissingRunRecordException missing) {
            return Optional.empty();
        }
        IsolatedResultMessageHandler.requireRunRecordBinding(record, workItem);
        return Optional.of(reference);
    }

    private static Optional<Path> soleSpooledMessage(Path spoolRoot, String direction)
            throws IOException {
        if (!Files.isDirectory(spoolRoot, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try (var paths = Files.list(spoolRoot)) {
            List<Path> spooled = paths
                    .filter(path -> path.getFileName().toString()
                            .endsWith(FileSpoolMessageTransport.FILE_SUFFIX))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
            if (spooled.size() > 1) {
                throw new IOException(
                        "the " + direction + " spool holds several messages");
            }
            return spooled.stream().findFirst();
        }
    }

    private static Path absolute(Path path, String name) {
        return Objects.requireNonNull(path, name + " must not be null")
                .toAbsolutePath()
                .normalize();
    }

}
