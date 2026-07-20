package com.enhancer.runtime;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessagePayload;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportOutcome;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
 * {@link RunRecordStore} the finalizer uses, and that the status the child claimed equals the
 * resolved record's own status. {@code DurableAgentRunFinalizer} remains the final authority and
 * still derives the runtime terminal state and queue disposition from the record.
 *
 * <p>On re-entry an already-published valid result is returned without launching a second child,
 * so a cycle interrupted after the child published recovers without re-executing. A child that
 * persisted a RunRecord and died before publishing leaves an orphaned record and is re-executed,
 * which is the same at-least-once consequence the in-process worker already accepts.
 */
public final class ProcessIsolatedAgentRunExecution implements AgentRunExecution {
    private final Path invocationRoot;
    private final Path projectRoot;
    private final Path evidenceRoot;
    private final Path runRecordRoot;
    private final RunRecordStore runRecordStore;
    private final WorkerProcessLauncher launcher;
    private final Duration timeout;

    public ProcessIsolatedAgentRunExecution(
            Path invocationRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            RunRecordStore runRecordStore,
            WorkerProcessLauncher launcher,
            Duration timeout) {
        this.invocationRoot = absolute(invocationRoot, "invocationRoot");
        this.projectRoot = absolute(projectRoot, "projectRoot");
        this.evidenceRoot = absolute(evidenceRoot, "evidenceRoot");
        this.runRecordRoot = absolute(runRecordRoot, "runRecordRoot");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
        this.launcher = Objects.requireNonNull(launcher, "launcher must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    @Override
    public String execute(AgentRunDispatch dispatch) throws IOException {
        Objects.requireNonNull(dispatch, "dispatch must not be null");
        WorkItem workItem = dispatch.workItem();
        Path cycleRoot = invocationRoot
                .resolve(dispatch.goalId())
                .resolve(dispatch.agentRunId());

        Optional<String> recovered = publishedResult(cycleRoot, workItem);
        if (recovered.isPresent()) {
            return recovered.orElseThrow();
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

    /**
     * Spools the work message, or leaves an existing one in place.
     *
     * <p>Re-entry after a child died before publishing must not add a second work message: the
     * child expects exactly one and would otherwise refuse the whole cycle. A spool that already
     * holds something other than exactly one message is a corrupt cycle and fails closed.
     */
    private void spoolWork(Path cycleRoot, WorkItem workItem) throws IOException {
        Path workSpool = cycleRoot.resolve(IsolatedWorkerMain.WORK_SPOOL);
        if (IsolatedWorkerMain.soleSpooledMessage(workSpool).isPresent()) {
            return;
        }
        if (spooledMessageCount(workSpool) != 0) {
            throw new IOException("the work spool does not hold exactly one message");
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
        Optional<Path> spooled = IsolatedWorkerMain.soleSpooledMessage(
                cycleRoot.resolve(IsolatedWorkerMain.RESULT_SPOOL));
        if (spooled.isEmpty()) {
            return Optional.empty();
        }
        TransportMessage message = FileSpoolMessageTransport.read(spooled.orElseThrow());
        MessageEnvelope work = workItem.workMessage();
        MessageEnvelope result = message.envelope();

        requireEqual(work.correlationId(), result.correlationId(), "correlation identity");
        requireEqual(work.logicalRunId(), result.logicalRunId(), "logical run identity");
        requireEqual(
                work.messageId(),
                result.causationId().orElseThrow(() -> new IOException(
                        "the result envelope names no causation identity")),
                "causation identity");

        MessagePayload payload = result.payload();
        if (!(payload instanceof ResultPayload resultPayload)) {
            throw new IOException("the result envelope does not carry a ResultPayload");
        }
        requireEqual(
                workItem.taskRevision().taskId(), resultPayload.taskId(), "task identity");

        VerificationStatus recorded = runRecordStore
                .resolve(resultPayload.runRecordReference())
                .record()
                .verification()
                .status();
        if (recorded != resultPayload.verificationStatus()) {
            throw new IOException("the child claimed verification status "
                    + resultPayload.verificationStatus()
                    + " but the resolved RunRecord records " + recorded);
        }
        return Optional.of(resultPayload.runRecordReference());
    }

    private static long spooledMessageCount(Path spoolRoot) throws IOException {
        if (!Files.isDirectory(spoolRoot)) {
            return 0;
        }
        try (var paths = Files.list(spoolRoot)) {
            return paths.filter(path -> path.getFileName().toString()
                    .endsWith(FileSpoolMessageTransport.FILE_SUFFIX)).count();
        }
    }

    private static void requireEqual(String expected, String actual, String label)
            throws IOException {
        if (!expected.equals(actual)) {
            throw new IOException("the result envelope's " + label
                    + " does not match the dispatched work");
        }
    }

    private static Path absolute(Path path, String name) {
        return Objects.requireNonNull(path, name + " must not be null")
                .toAbsolutePath()
                .normalize();
    }
}
