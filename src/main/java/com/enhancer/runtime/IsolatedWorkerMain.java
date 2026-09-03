package com.enhancer.runtime;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.CorruptedSpooledMessageException;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportOutcome;
import com.enhancer.bus.WorkPayload;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.model.GovernedModelPromptReader;
import com.enhancer.model.ModelInvocationAdmission;
import com.enhancer.run.FileSystemRunRecordStore;
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
 * <p>It reads the one work message its parent spooled. Legacy work runs the Gate 1-4 pipeline
 * through the same {@link AgentLoopAgentRunExecution} the in-process worker uses, persists a
 * RunRecord, and publishes a matching {@link ResultPayload}. Typed work composes the child-local
 * deterministic-fake pipeline from parent-supplied scalar configuration and publishes only Model
 * RunRecord v2. A result reference is only a claim for the parent to validate against the
 * resolved record.
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

    /** Reserved historical exit for a typed writer that is not connected. */
    public static final int EXIT_MODEL_EXECUTION_NOT_CONNECTED = 31;

    /** A RunRecord exists but the result could not be published for the parent to read. */
    public static final int EXIT_RESULT_NOT_PUBLISHED = 40;

    static final String WORK_SPOOL = "work";
    static final String RESULT_SPOOL = "result";
    static final String RESULT_DESTINATION = "isolated-worker-result";

    private IsolatedWorkerMain() {
    }

    public static void main(String[] arguments) {
        System.exit(run(arguments));
    }

    static int run(String[] arguments) {
        if (arguments == null) {
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
        boolean modelWork = work.envelope().payload() instanceof ModelWorkPayload;
        if (modelWork != invocation.modelConfiguration().isPresent()) {
            return EXIT_USAGE;
        }

        IsolatedWorkMessageHandler.Result result;
        try {
            FileSystemRunRecordStore runRecordStore =
                    new FileSystemRunRecordStore(invocation.runRecordRoot());
            FileSystemEvidenceStore evidenceStore = new FileSystemEvidenceStore(
                    invocation.evidenceRoot(),
                    new EvidenceStoragePolicy(
                            EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES));
            Clock clock = Clock.systemUTC();
            IsolatedWorkMessageHandler handler;
            if (modelWork) {
                SchedulerModelInvocationPreparer preparer =
                        new SchedulerModelInvocationPreparer(
                                new ExactActiveTaskResolver(
                                        new ProjectContextReader(),
                                        new ApprovedTaskReader()),
                                new GovernedModelPromptReader(),
                                new ModelInvocationAdmission());
                DeterministicFakeModelAttemptPipeline pipeline =
                        new DeterministicFakeModelAttemptPipeline(
                                preparer,
                                new DeterministicFakeModelGateway(),
                                evidenceStore,
                                runRecordStore,
                                clock);
                handler = new IsolatedWorkMessageHandler(
                        invocation.workItemId(),
                        invocation.requiredCapability(),
                        invocation.goalId(),
                        invocation.agentRunId(),
                        invocation.projectRoot(),
                        pipeline,
                        invocation.modelConfiguration().orElseThrow());
            } else {
                AgentLoopAgentRunExecution execution = new AgentLoopAgentRunExecution(
                        invocation.projectRoot(),
                        evidenceStore,
                        runRecordStore,
                        clock);
                handler = new IsolatedWorkMessageHandler(
                        invocation.workItemId(),
                        invocation.requiredCapability(),
                        invocation.goalId(),
                        invocation.agentRunId(),
                        execution,
                        runRecordStore);
            }
            InProcessMessageBus bus = new InProcessMessageBus();
            bus.subscribe(
                    DeliveryDestination.queue(WORK_SPOOL),
                    "isolated-work-executor",
                    handler);
            List<DeliveryOutcome> outcomes =
                    bus.publish(work.destination(), work.envelope());
            if (outcomes.size() != 1
                    || outcomes.get(0).status() != DeliveryStatus.DELIVERED) {
                return EXIT_EXECUTION_FAILED;
            }
            result = handler.acceptedResult().orElseThrow(
                    () -> new IllegalStateException(
                            "the delivered isolated work exposed no result"));
        } catch (RuntimeException failed) {
            return EXIT_EXECUTION_FAILED;
        }

        try {
            return publishResult(
                            invocation,
                            work,
                            result.reference(),
                            result.status())
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
                        taskId(work.envelope()),
                        reference,
                        status));
        TransportOutcome outcome = new FileSpoolMessageTransport(
                invocation.cycleRoot().resolve(RESULT_SPOOL), BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue(RESULT_DESTINATION), result));
        return outcome.status().isAccepted();
    }

    static String taskId(MessageEnvelope work) {
        if (work.payload() instanceof WorkPayload payload) {
            return payload.taskRevision().taskId();
        }
        if (work.payload() instanceof ModelWorkPayload payload) {
            return payload.taskRevision().taskId();
        }
        throw new IllegalArgumentException(
                "isolated work must carry WorkPayload or ModelWorkPayload");
    }

    /** Returns the single spooled message, or empty when there is none or more than one. */
    static Optional<Path> soleSpooledMessage(Path spoolRoot) throws IOException {
        if (!Files.isDirectory(spoolRoot, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try (var paths = Files.list(spoolRoot)) {
            List<Path> candidates = paths
                    .filter(path -> path.getFileName().toString()
                            .endsWith(FileSpoolMessageTransport.FILE_SUFFIX))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
            if (candidates.stream().anyMatch(path ->
                    !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))) {
                throw new IOException("the work spool contains a non-regular message point");
            }
            return candidates.size() == 1
                    ? Optional.of(candidates.get(0))
                    : Optional.empty();
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
            String agentRunId,
            Optional<ModelProcessExecutionConfiguration> modelConfiguration) {

        static Invocation of(String[] arguments) {
            Optional<ModelProcessExecutionConfiguration> configuration =
                    ModelProcessExecutionConfiguration.fromInvocationArguments(arguments);
            return new Invocation(
                    absolute(arguments[0]),
                    absolute(arguments[1]),
                    absolute(arguments[2]),
                    absolute(arguments[3]),
                    arguments[4],
                    arguments[5],
                    arguments[6],
                    arguments[7],
                    configuration);
        }

        private static Path absolute(String value) {
            return Path.of(value).toAbsolutePath().normalize();
        }
    }
}
