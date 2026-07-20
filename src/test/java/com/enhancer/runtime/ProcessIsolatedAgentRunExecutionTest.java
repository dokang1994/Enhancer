package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the parent side of connection 3: work out through a spool, a child process, and a
 * validated result back. The launcher's own bounds live in {@link IsolatedWorkerLauncherTest}.
 */
class ProcessIsolatedAgentRunExecutionTest {
    private static final Duration GENEROUS = Duration.ofMinutes(2);

    @TempDir
    Path temporaryRoot;

    @Test
    void runsTheWorkInAChildAndReturnsTheResolvableRunRecordReference() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);

        String reference = fixture.execution().execute(fixture.dispatch());

        assertTrue(reference.startsWith("run-record/"), "the reference must be resolvable");
        assertEquals(
                VerificationStatus.VERIFIED,
                fixture.runRecordStore().resolve(reference).record().verification().status(),
                "the child ran the real pipeline against a digest-matching target");

        // The work crossed out and the result crossed back through the cycle's own spools.
        assertTrue(Files.isDirectory(fixture.cycleRoot().resolve(IsolatedWorkerMain.WORK_SPOOL)));
        assertTrue(Files.isDirectory(fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL)));
    }

    @Test
    void returnsAnAlreadyPublishedResultWithoutLaunchingASecondChild() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        String reference = fixture.execution().execute(fixture.dispatch());

        ProcessIsolatedAgentRunExecution recovering =
                fixture.executionWith(failIfLaunched());

        assertEquals(reference, recovering.execute(fixture.dispatch()));
    }

    @Test
    void refusesForeignWorkAlreadyInTheCycleBeforeLaunchingAChild() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        MessageEnvelope expected = fixture.dispatch().workItem().workMessage();
        MessageEnvelope foreign = new MessageEnvelope(
                UUID.randomUUID().toString(),
                expected.correlationId(),
                expected.causationId(),
                expected.logicalRunId(),
                expected.producer(),
                expected.occurredAt(),
                expected.payload());
        spool(
                fixture.cycleRoot().resolve(IsolatedWorkerMain.WORK_SPOOL),
                new TransportMessage(
                        DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL), foreign),
                1);

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(refused.getMessage().contains("dispatched work"), refused.getMessage());
    }

    @Test
    void refusesWorkWithTheWrongDestinationBeforeLaunchingAChild() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        spool(
                fixture.cycleRoot().resolve(IsolatedWorkerMain.WORK_SPOOL),
                new TransportMessage(
                        DeliveryDestination.queue("foreign-work"),
                        fixture.dispatch().workItem().workMessage()),
                1);

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(refused.getMessage().contains("destination"), refused.getMessage());
    }

    @Test
    void refusesAResultThatDoesNotCorrelateToTheDispatchedWork() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        String reference = fixture.execution().execute(fixture.dispatch());
        Path resultSpool = fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL);

        // Same reference and same claimed status, but produced for a different cycle. Accepting
        // it would let an earlier or foreign run stand in for this one.
        republish(resultSpool, envelope -> new MessageEnvelope(
                UUID.randomUUID().toString(),
                "a-different-correlation",
                envelope.causationId(),
                envelope.logicalRunId(),
                "isolated-worker",
                envelope.occurredAt(),
                new ResultPayload(
                        fixture.taskId(), reference, VerificationStatus.VERIFIED)));

        IOException refused = assertThrows(IOException.class,
                () -> fixture.execution().execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("correlation identity"), refused.getMessage());
    }

    @Test
    void refusesAResultWhoseClaimedStatusDisagreesWithTheRunRecord() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        String reference = fixture.execution().execute(fixture.dispatch());
        Path resultSpool = fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL);

        // The RunRecord is the authority. A child that reports a verdict the record does not
        // carry must not be able to promote its own run.
        republish(resultSpool, envelope -> new MessageEnvelope(
                envelope.messageId(),
                envelope.correlationId(),
                envelope.causationId(),
                envelope.logicalRunId(),
                envelope.producer(),
                envelope.occurredAt(),
                new ResultPayload(fixture.taskId(), reference, VerificationStatus.REJECTED)));

        IOException refused = assertThrows(IOException.class,
                () -> fixture.execution().execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("claimed verification status"),
                refused.getMessage());
    }

    @Test
    void refusesAResultPublishedToTheWrongDestination() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        Path resultSpool = fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL);

        republishTransport(resultSpool, message -> new TransportMessage(
                DeliveryDestination.queue("foreign-result"),
                message.envelope()));

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("destination"), refused.getMessage());
    }

    @Test
    void refusesAResolvedRunRecordForAnotherSourceDocument() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        String foreignReference = fixture.recordFor(
                "FOREIGN_TASK.md",
                "TARGET.md",
                fixture.targetDigest());

        republishReference(
                fixture, foreignReference, VerificationStatus.VERIFIED);

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("source document"), refused.getMessage());
    }

    @Test
    void refusesAResolvedRunRecordForAnotherTask() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        String foreignReference = fixture.recordFor(
                "foreign-task",
                "TARGET.md",
                "TARGET.md",
                fixture.targetDigest());

        republishReference(
                fixture, foreignReference, VerificationStatus.VERIFIED);

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("task identity"), refused.getMessage());
    }

    @Test
    void refusesAResolvedRunRecordForAnotherExecutionTarget() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        fixture.writeProjectFile("OTHER_TARGET.md", "isolated worker target\n");
        String foreignReference = fixture.recordFor(
                "TARGET.md",
                "OTHER_TARGET.md",
                fixture.targetDigest());

        republishReference(
                fixture, foreignReference, VerificationStatus.VERIFIED);

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("execution target"), refused.getMessage());
    }

    @Test
    void refusesAResolvedRunRecordForAnotherExpectedDigest() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        String foreignReference = fixture.recordFor(
                "TARGET.md",
                "TARGET.md",
                "0".repeat(64));

        republishReference(
                fixture, foreignReference, VerificationStatus.REJECTED);

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("expected digest"), refused.getMessage());
    }

    @Test
    void refusesSeveralResultsBeforeLaunchingAChild() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        Path resultSpool = fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL);
        Path existing = IsolatedWorkerMain.soleSpooledMessage(resultSpool).orElseThrow();
        Files.copy(existing, resultSpool.resolve("duplicate.transport"));

        IOException refused = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("result spool"), refused.getMessage());
    }

    @Test
    void keepsTheLeaseFreeExecutionSeamInsideTheRuntimePackage()
            throws NoSuchMethodException {
        int modifiers = AgentLoopAgentRunExecution.class
                .getDeclaredMethod(
                        "executeWork", WorkItem.class, String.class, String.class)
                .getModifiers();

        assertTrue(!Modifier.isPublic(modifiers), "executeWork must not be public");
    }

    @Test
    void failsClosedWhenTheChildDoesNotPublishAResult() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);

        ProcessIsolatedAgentRunExecution silent = fixture.executionWith(
                new WorkerProcessLauncher() {
                    @Override
                    public IsolatedWorkerOutcome run(
                            Class<?> entryPoint, List<String> arguments, Duration timeout) {
                        return IsolatedWorkerOutcome.completed(
                                IsolatedWorkerMain.EXIT_RESULT_PUBLISHED);
                    }
                });

        IOException refused = assertThrows(IOException.class,
                () -> silent.execute(fixture.dispatch()));
        assertTrue(refused.getMessage().contains("published no valid result"),
                refused.getMessage());
    }

    @Test
    void failsClosedWhenTheChildIsDestroyedOrExitsNonZero() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);

        IOException timedOut = assertThrows(IOException.class,
                () -> fixture.executionWith(new WorkerProcessLauncher() {
                    @Override
                    public IsolatedWorkerOutcome run(
                            Class<?> entryPoint, List<String> arguments, Duration timeout) {
                        return IsolatedWorkerOutcome.refused(
                                IsolatedWorkerStatus.TIMED_OUT, "destroyed");
                    }
                }).execute(fixture.dispatch()));
        assertTrue(timedOut.getMessage().contains("did not complete"), timedOut.getMessage());

        IOException failed = assertThrows(IOException.class,
                () -> fixture.executionWith(new WorkerProcessLauncher() {
                    @Override
                    public IsolatedWorkerOutcome run(
                            Class<?> entryPoint, List<String> arguments, Duration timeout) {
                        return IsolatedWorkerOutcome.completed(
                                IsolatedWorkerMain.EXIT_EXECUTION_FAILED);
                    }
                }).execute(fixture.dispatch()));
        assertTrue(failed.getMessage().contains("without publishing a result"),
                failed.getMessage());
    }

    @Test
    void givesEachCycleItsOwnInvocationRoot() throws IOException {
        Fixture first = Fixture.create(temporaryRoot);
        Fixture second = Fixture.create(temporaryRoot);

        assertNotEquals(first.cycleRoot(), second.cycleRoot(),
                "distinct Goal and AgentRun identities must not share a spool");
    }

    private static void republish(
            Path resultSpool, java.util.function.UnaryOperator<MessageEnvelope> rewrite)
            throws IOException {
        republishTransport(resultSpool, message -> new TransportMessage(
                DeliveryDestination.queue(IsolatedWorkerMain.RESULT_DESTINATION),
                rewrite.apply(message.envelope())));
    }

    private static void republishReference(
            Fixture fixture, String reference, VerificationStatus status) throws IOException {
        Path resultSpool = fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL);
        republish(resultSpool, envelope -> new MessageEnvelope(
                envelope.messageId(),
                envelope.correlationId(),
                envelope.causationId(),
                envelope.logicalRunId(),
                envelope.producer(),
                envelope.occurredAt(),
                new ResultPayload(fixture.taskId(), reference, status)));
    }

    private static void republishTransport(
            Path resultSpool,
            java.util.function.UnaryOperator<TransportMessage> rewrite)
            throws IOException {
        Path existing = IsolatedWorkerMain.soleSpooledMessage(resultSpool).orElseThrow();
        TransportMessage original = FileSpoolMessageTransport.read(existing);
        Files.delete(existing);
        spool(resultSpool, rewrite.apply(original), 1);
    }

    private static void spool(Path root, TransportMessage message, int capacity) {
        new FileSpoolMessageTransport(root, BackpressurePolicy.of(capacity)).send(message);
    }

    private static WorkerProcessLauncher failIfLaunched() {
        return new WorkerProcessLauncher() {
            @Override
            public IsolatedWorkerOutcome run(
                    Class<?> entryPoint, List<String> arguments, Duration timeout) {
                throw new AssertionError("a child must not be launched");
            }
        };
    }

    /** One dispatched cycle with real filesystem stores and a real target to read. */
    private record Fixture(
            Path root,
            AgentRunDispatch dispatch,
            RunRecordStore runRecordStore,
            ProcessIsolatedAgentRunExecution execution,
            Path cycleRoot,
            String taskId,
            String targetDigest) {

        static Fixture create(Path temporaryRoot) throws IOException {
            Path root = Files.createDirectories(
                    temporaryRoot.resolve("cycle-" + UUID.randomUUID()));
            Path projectRoot = Files.createDirectories(root.resolve("project"));
            Files.writeString(projectRoot.resolve("TARGET.md"), "isolated worker target\n");
            String digest = sha256(projectRoot.resolve("TARGET.md"));

            String taskId = "gate-8-isolated-execution";
            MessageEnvelope work = new MessageEnvelope(
                    UUID.randomUUID().toString(),
                    "correlation-" + UUID.randomUUID(),
                    Optional.empty(),
                    "run-" + UUID.randomUUID(),
                    "scheduler",
                    Instant.parse("2026-07-20T09:00:00Z"),
                    new WorkPayload(
                            new ApprovedTaskRevision(taskId, "TARGET.md", digest),
                            "b".repeat(64),
                            Set.of("read-file"),
                            Optional.of(new WorkPayload.ExecutionInput("TARGET.md", digest))));
            WorkItem workItem = new WorkItem(
                    UUID.randomUUID().toString(), "read-file", work);
            AgentRunDispatch dispatch = new AgentRunDispatch(
                    UUID.randomUUID().toString(),
                    workItem,
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    lease());

            Path invocationRoot = root.resolve("invocations");
            RunRecordStore runRecordStore =
                    new FileSystemRunRecordStore(root.resolve("run-records"));
            Fixture fixture = new Fixture(
                    root,
                    dispatch,
                    runRecordStore,
                    null,
                    invocationRoot.resolve(dispatch.goalId()).resolve(dispatch.agentRunId()),
                    taskId,
                    digest);
            return new Fixture(
                    root,
                    dispatch,
                    runRecordStore,
                    fixture.executionWith(new IsolatedWorkerLauncher()),
                    fixture.cycleRoot(),
                    taskId,
                    digest);
        }

        ProcessIsolatedAgentRunExecution executionWith(WorkerProcessLauncher launcher) {
            return new ProcessIsolatedAgentRunExecution(
                    root.resolve("invocations"),
                    root.resolve("project"),
                    root.resolve("evidence"),
                    root.resolve("run-records"),
                    runRecordStore,
                    launcher,
                    GENEROUS);
        }

        String recordFor(
                String sourceDocument,
                String targetPath,
                String expectedDigest)
                throws IOException {
            return recordFor(taskId, sourceDocument, targetPath, expectedDigest);
        }

        String recordFor(
                String recordTaskId,
                String sourceDocument,
                String targetPath,
                String expectedDigest)
                throws IOException {
            Path source = root.resolve("project").resolve(sourceDocument);
            if (!Files.exists(source)) {
                Files.writeString(source, "foreign task source\n");
            }
            MessageEnvelope work = new MessageEnvelope(
                    UUID.randomUUID().toString(),
                    "correlation-" + UUID.randomUUID(),
                    Optional.empty(),
                    "run-" + UUID.randomUUID(),
                    "scheduler",
                    Instant.parse("2026-07-20T09:00:00Z"),
                    new WorkPayload(
                            new ApprovedTaskRevision(
                                    recordTaskId, sourceDocument, sha256(source)),
                            "b".repeat(64),
                            Set.of("read-file"),
                            Optional.of(new WorkPayload.ExecutionInput(
                                    targetPath, expectedDigest))));
            WorkItem workItem = new WorkItem(
                    UUID.randomUUID().toString(), "read-file", work);
            return new AgentLoopAgentRunExecution(
                    root.resolve("project"),
                    new FileSystemEvidenceStore(
                            root.resolve("evidence"),
                            new EvidenceStoragePolicy(
                                    EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)),
                    runRecordStore,
                    Clock.systemUTC())
                    .executeWork(
                            workItem,
                            UUID.randomUUID().toString(),
                            UUID.randomUUID().toString());
        }

        void writeProjectFile(String relative, String content) throws IOException {
            Files.writeString(root.resolve("project").resolve(relative), content);
        }

        private static AgentRunLease lease() {
            return new AgentRunLease(
                    UUID.randomUUID().toString(),
                    1L,
                    Instant.parse("2026-07-20T09:00:00Z"),
                    Instant.parse("2026-07-20T10:00:00Z"));
        }

        private static String sha256(Path file) throws IOException {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(Files.readString(file, StandardCharsets.UTF_8)
                                .getBytes(StandardCharsets.UTF_8)));
            } catch (java.security.NoSuchAlgorithmException unavailable) {
                throw new IllegalStateException(unavailable);
            }
        }
    }
}
