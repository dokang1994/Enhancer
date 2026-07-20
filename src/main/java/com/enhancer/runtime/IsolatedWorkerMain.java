package com.enhancer.runtime;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.CorruptedSpooledMessageException;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportOutcome;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Child-process entry point for {@link IsolatedWorkerLauncher}.
 *
 * <p>It reads the one work message its parent spooled, runs the Gate 1-4 pipeline through the
 * same {@link AgentLoopAgentRunExecution} the in-process worker uses, persists a RunRecord, and
 * publishes a matching {@link ResultPayload} to the result spool. The exit code says only whether
 * that sequence completed; the result reference travels through the spool, and the parent treats
 * it as a claim to validate against the resolved RunRecord rather than as authority.
 *
 * <p>Every input is an explicit argument. Store roots in particular are parent configuration and
 * never payload data, because a payload that crossed a process boundary is untrusted input and
 * must not be able to redirect where artifacts are written.
 */
public final class IsolatedWorkerMain {
    /** The work executed, a RunRecord was persisted, and a result was published. */
    public static final int EXIT_RESULT_PUBLISHED = 0;

    /** The argument vector was not the exact shape this entry point requires. */
    public static final int EXIT_USAGE = 2;

    /** The work spool holds no message, or more than one. */
    public static final int EXIT_WORK_ABSENT = 3;

    /** A work message exists but cannot be decoded and will stay undecodable. */
    public static final int EXIT_WORK_CORRUPT = 10;

    /** The work spool could not be read for a reason that may be transient. */
    public static final int EXIT_WORK_UNREADABLE = 20;

    /** The pipeline could not run to a persisted RunRecord. */
    public static final int EXIT_EXECUTION_FAILED = 30;

    /** A RunRecord exists but the result could not be published for the parent to read. */
    public static final int EXIT_RESULT_NOT_PUBLISHED = 40;

    static final String WORK_SPOOL = "work";
    static final String RESULT_SPOOL = "result";
    static final String RESULT_DESTINATION = "isolated-worker-result";

    private static final int ARGUMENT_COUNT = 8;

    private IsolatedWorkerMain() {
    }

    public static void main(String[] arguments) {
        System.exit(run(arguments));
    }

    static int run(String[] arguments) {
        if (arguments == null || arguments.length != ARGUMENT_COUNT) {
            return EXIT_USAGE;
        }
        Invocation invocation;
        try {
            invocation = Invocation.of(arguments);
        } catch (RuntimeException invalid) {
            return EXIT_USAGE;
        }

        TransportMessage work;
        try {
            Optional<Path> spooled = soleSpooledMessage(
                    invocation.cycleRoot().resolve(WORK_SPOOL));
            if (spooled.isEmpty()) {
                return EXIT_WORK_ABSENT;
            }
            work = FileSpoolMessageTransport.read(spooled.orElseThrow());
        } catch (CorruptedSpooledMessageException corrupt) {
            return EXIT_WORK_CORRUPT;
        } catch (IOException unreadable) {
            return EXIT_WORK_UNREADABLE;
        }

        String reference;
        VerificationStatus status;
        try {
            RunRecordStore runRecordStore =
                    new FileSystemRunRecordStore(invocation.runRecordRoot());
            WorkItem workItem = new WorkItem(
                    invocation.workItemId(),
                    invocation.requiredCapability(),
                    work.envelope());
            reference = new AgentLoopAgentRunExecution(
                    invocation.projectRoot(),
                    new FileSystemEvidenceStore(
                            invocation.evidenceRoot(),
                            new EvidenceStoragePolicy(
                                    EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)),
                    runRecordStore,
                    Clock.systemUTC())
                    .executeWork(workItem, invocation.goalId(), invocation.agentRunId());
            status = runRecordStore.resolve(reference).record().verification().status();
        } catch (IOException | RuntimeException failed) {
            return EXIT_EXECUTION_FAILED;
        }

        try {
            return publishResult(invocation, work, reference, status)
                    ? EXIT_RESULT_PUBLISHED
                    : EXIT_RESULT_NOT_PUBLISHED;
        } catch (RuntimeException unpublishable) {
            return EXIT_RESULT_NOT_PUBLISHED;
        }
    }

    /**
     * Publishes the result correlated to the work the parent dispatched. The correlation, logical
     * run, and causation identities are what let the parent prove this result belongs to the
     * cycle it launched rather than to an earlier one.
     */
    private static boolean publishResult(
            Invocation invocation,
            TransportMessage work,
            String reference,
            VerificationStatus status) {
        MessageEnvelope result = new MessageEnvelope(
                UUID.randomUUID().toString(),
                work.envelope().correlationId(),
                Optional.of(work.envelope().messageId()),
                work.envelope().logicalRunId(),
                "isolated-worker",
                Clock.systemUTC().instant(),
                new ResultPayload(
                        ((com.enhancer.bus.WorkPayload) work.envelope().payload())
                                .taskRevision().taskId(),
                        reference,
                        status));
        TransportOutcome outcome = new FileSpoolMessageTransport(
                invocation.cycleRoot().resolve(RESULT_SPOOL), BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue(RESULT_DESTINATION), result));
        return outcome.status().isAccepted();
    }

    /** Returns the single spooled message, or empty when there is none or more than one. */
    static Optional<Path> soleSpooledMessage(Path spoolRoot) throws IOException {
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
            return spooled.size() == 1 ? Optional.of(spooled.get(0)) : Optional.empty();
        }
    }

    /** The parent-supplied invocation, positional so the launcher's argument vector is fixed. */
    private record Invocation(
            Path cycleRoot,
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            String workItemId,
            String requiredCapability,
            String goalId,
            String agentRunId) {

        static Invocation of(String[] arguments) {
            return new Invocation(
                    absolute(arguments[0]),
                    absolute(arguments[1]),
                    absolute(arguments[2]),
                    absolute(arguments[3]),
                    arguments[4],
                    arguments[5],
                    arguments[6],
                    arguments[7]);
        }

        private static Path absolute(String value) {
            return Path.of(value).toAbsolutePath().normalize();
        }
    }
}
