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
import com.enhancer.run.ModelRunRecordStore;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the parent side of connection 3: work out through a spool, a child process, and a
 * validated result back. The launcher's own bounds live in {@link IsolatedWorkerLauncherTest}.
 */
class ProcessIsolatedAgentRunExecutionTest {
    private static final Duration GENEROUS = Duration.ofMinutes(2);

    @Test
    void refusesTypedModelWorkBeforeCreatingACycleOrLaunchingAChild()
            throws Exception {
        Fixture fixture = Fixture.create(temporaryRoot);
        AgentRunDispatch legacyDispatch = fixture.dispatch();
        AgentRunDispatch modelDispatch = new AgentRunDispatch(
                legacyDispatch.queueId(),
                ModelWorkFixtures.workItem(),
                legacyDispatch.goalId(),
                legacyDispatch.agentRunId(),
                legacyDispatch.lease());

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(modelDispatch));

        assertTrue(failure.getMessage().contains("Model RunRecord v2"));
        assertTrue(Files.notExists(fixture.cycleRoot()));
    }

    @Test
    void pointRecoversAnExactModelRecordWithoutWorkResultOrChildLaunch()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);

        String reference = fixture.execution(failIfLaunched()).execute(fixture.dispatch());

        assertEquals(
                AgentRunRecordIdentity.reference(
                        fixture.dispatch().goalId(), fixture.dispatch().agentRunId()),
                reference);
        assertTrue(Files.notExists(fixture.cycleRoot()));
    }

    @Test
    void validModelRecordTakesPrecedenceOverAPersistedProcessTimeout()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);
        fixture.persistTimeout();

        String reference = fixture.execution(failIfLaunched()).execute(fixture.dispatch());

        assertEquals(
                AgentRunRecordIdentity.reference(
                        fixture.dispatch().goalId(), fixture.dispatch().agentRunId()),
                reference);
    }

    @Test
    void modelResultWithoutItsWorkPointFailsBeforeRecordAcceptance()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);
        fixture.spoolResult();

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("work"), failure.getMessage());
    }

    @Test
    void exactModelWorkResultAndRecordClosureReturnsWithoutLaunching()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);
        fixture.spoolWork();
        fixture.spoolResult();

        String reference = fixture.execution(failIfLaunched()).execute(fixture.dispatch());

        assertEquals(
                AgentRunRecordIdentity.reference(
                        fixture.dispatch().goalId(), fixture.dispatch().agentRunId()),
                reference);
    }

    @Test
    void modelResultWithANonRegularExtraWorkPointFailsClosed()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);
        fixture.spoolWork();
        Files.createDirectory(fixture.cycleRoot()
                .resolve(IsolatedWorkerMain.WORK_SPOOL)
                .resolve("foreign.transport"));
        fixture.spoolResult();

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("regular"), failure.getMessage());
    }

    @Test
    void modelResultMustClaimTheDeterministicAgentRunReference()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);
        fixture.spoolWork();
        fixture.spoolResult("run-record/11111111-1111-1111-1111-111111111111");

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("deterministic"), failure.getMessage());
    }

    @Test
    void missingModelRecordLetsThePersistedTimeoutGovernWithoutLaunching()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, false);
        fixture.persistTimeout();

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("TIMED_OUT"), failure.getMessage());
    }

    @Test
    void missingModelRecordLaunchesTheTypedChildWithScalarConfiguration()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, false);
        AtomicReference<List<String>> launchedArguments = new AtomicReference<>();
        WorkerProcessLauncher launcher = (entryPoint, arguments, timeout) -> {
            launchedArguments.set(arguments);
            return IsolatedWorkerOutcome.completed(
                    IsolatedWorkerMain.EXIT_EXECUTION_FAILED);
        };

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(launcher).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("exited 30"));
        assertEquals(13, launchedArguments.get().size());
        assertEquals("1000", launchedArguments.get().get(8));
        assertEquals("20000", launchedArguments.get().get(9));
        assertEquals("65536", launchedArguments.get().get(10));
        assertEquals("2000", launchedArguments.get().get(11));
        assertEquals("0", launchedArguments.get().get(12));
        assertTrue(Files.exists(
                fixture.cycleRoot().resolve(IsolatedWorkerMain.WORK_SPOOL)));
    }

    @Test
    void crossKindDeterministicRecordIsNotHiddenByTimeout()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, false);
        fixture.persistLegacyAtDeterministicIdentity();
        fixture.persistTimeout();

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("kind"), failure.getMessage());
        assertTrue(!failure.getMessage().contains("TIMED_OUT"), failure.getMessage());
    }

    @Test
    void corruptDeterministicModelRecordIsNotHiddenByTimeout()
            throws Exception {
        ModelFixture fixture = ModelFixture.create(temporaryRoot, true);
        fixture.corruptRecord();
        fixture.persistTimeout();

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.execution(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(!failure.getMessage().contains("TIMED_OUT"), failure.getMessage());
    }

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
        assertTrue(fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId())
                .isEmpty(), "successful execution must not create a timeout fact");

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
    void legacyResultWithoutItsWorkPointAlsoFailsClosed() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        Path workSpool = fixture.cycleRoot().resolve(IsolatedWorkerMain.WORK_SPOOL);
        Files.delete(IsolatedWorkerMain.soleSpooledMessage(workSpool).orElseThrow());
        Files.delete(workSpool);

        IOException failure = assertThrows(
                IOException.class,
                () -> fixture.executionWith(failIfLaunched()).execute(fixture.dispatch()));

        assertTrue(failure.getMessage().contains("work"), failure.getMessage());
    }

    @Test
    void pointRecoversAChildPersistedRecordWhenResultPublicationWasLost()
            throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        Files.createDirectories(fixture.cycleRoot());
        Path blockedResultSpool =
                fixture.cycleRoot().resolve(IsolatedWorkerMain.RESULT_SPOOL);
        Files.writeString(blockedResultSpool, "block result spool\n");

        assertThrows(
                IOException.class,
                () -> fixture.execution().execute(fixture.dispatch()));
        assertEquals(1, fixture.runRecordStore().references().size());
        Files.delete(blockedResultSpool);

        String recovered = fixture.executionWith(failIfLaunched())
                .execute(fixture.dispatch());

        assertEquals(
                AgentRunRecordIdentity.reference(
                        fixture.dispatch().goalId(),
                        fixture.dispatch().agentRunId()),
                recovered);
        assertEquals(List.of(recovered), fixture.runRecordStore().references());
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
        assertTrue(refused.getMessage().contains("UNROUTED"), refused.getMessage());
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
    void persistsATypedTimeoutBeforeFailureAndReentryDoesNotLaunchAgain()
            throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        Instant occurredAt = Instant.parse("2026-08-04T10:05:00Z");
        AtomicInteger launches = new AtomicInteger();
        WorkerProcessLauncher timedOutLauncher = new WorkerProcessLauncher() {
            @Override
            public IsolatedWorkerOutcome run(
                    Class<?> entryPoint, List<String> arguments, Duration timeout) {
                launches.incrementAndGet();
                return IsolatedWorkerOutcome.refused(
                        IsolatedWorkerStatus.TIMED_OUT, "destroyed");
            }
        };

        IOException timedOut = assertThrows(IOException.class,
                () -> fixture.executionWith(
                        timedOutLauncher, Clock.fixed(occurredAt, java.time.ZoneOffset.UTC))
                        .execute(fixture.dispatch()));
        assertTrue(timedOut.getMessage().contains("did not complete"), timedOut.getMessage());
        ResolvedProcessTimeoutFact persisted = fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId())
                .orElseThrow();
        assertEquals(occurredAt, persisted.fact().occurredAt());
        assertEquals(fixture.dispatch().agentRunId(), persisted.fact().agentRunId());
        assertEquals(GENEROUS, persisted.fact().timeout());
        assertEquals("destroyed", persisted.fact().reason());
        assertEquals(1, launches.get());

        IOException replayed = assertThrows(IOException.class,
                () -> fixture.executionWith(failIfLaunched(), Clock.systemUTC())
                        .execute(fixture.dispatch()));
        assertTrue(replayed.getMessage().contains("did not complete"), replayed.getMessage());
        assertEquals(Optional.of(persisted), fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId()));
        assertEquals(1, launches.get());
    }

    @Test
    void startFailureAndCompletedFailurePersistNoProcessTimeout() throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);

        IOException startFailed = assertThrows(IOException.class,
                () -> fixture.executionWith(new WorkerProcessLauncher() {
                    @Override
                    public IsolatedWorkerOutcome run(
                            Class<?> entryPoint, List<String> arguments, Duration timeout) {
                        return IsolatedWorkerOutcome.refused(
                                IsolatedWorkerStatus.START_FAILED, "could not start");
                    }
                }).execute(fixture.dispatch()));
        assertTrue(startFailed.getMessage().contains("did not complete"),
                startFailed.getMessage());
        assertTrue(fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId())
                .isEmpty());

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
        assertTrue(fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId())
                .isEmpty());
    }

    @Test
    void recordsProcessTimeoutEventOnlyAfterTheFactIsDurable() throws Exception {
        Fixture fixture = Fixture.create(temporaryRoot);
        Instant occurredAt = Instant.parse("2026-08-04T10:10:00Z");
        FileSystemRuntimeEventStore eventStore = new FileSystemRuntimeEventStore(
                fixture.root().resolve("process-timeout-events"));
        List<RuntimeEventPublicationReference> publications = new ArrayList<>();
        RuntimeEventRecorder recorder = new RuntimeEventRecorder(
                eventStore,
                reference -> {
                    assertTrue(fixture.timeoutStore()
                            .find(
                                    fixture.dispatch().goalId(),
                                    fixture.dispatch().agentRunId())
                            .isPresent(), "the timeout fact must precede publication");
                    publications.add(reference);
                });

        assertThrows(IOException.class, () -> fixture.executionWithEvents(
                timedOutLauncher(),
                Clock.fixed(occurredAt, java.time.ZoneOffset.UTC),
                recorder).execute(fixture.dispatch()));

        RuntimeEventStream stream = eventStore.resolve(fixture.dispatch().goalId());
        assertEquals(1, stream.revision());
        RuntimeEvent event = stream.events().get(0);
        assertEquals(RuntimeEventKind.TIMEOUT_DETECTED, event.kind());
        assertEquals(
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.PROCESS),
                event.detail());
        assertEquals(occurredAt, event.occurredAt());
        assertEquals(fixture.dispatch().agentRunId(), event.agentRunId());
        assertEquals(
                Optional.of(fixture.dispatch().workItem().workMessage().messageId()),
                event.causationId());
        assertEquals("process-isolated-agent-run-execution", event.producerId());
        ResolvedProcessTimeoutFact fact = fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId())
                .orElseThrow();
        assertEquals(
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.PROCESS_TIMEOUT,
                        fact.reference(),
                        Optional.of(fact.sha256()))),
                event.authoritativeReferences());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(event)),
                publications);
    }

    @Test
    void persistedTimeoutRepairsAMissingEventWithoutAnotherLaunch() throws Exception {
        Fixture fixture = Fixture.create(temporaryRoot);
        Instant occurredAt = Instant.parse("2026-08-04T10:15:00Z");
        assertThrows(IOException.class, () -> fixture.executionWith(
                timedOutLauncher(),
                Clock.fixed(occurredAt, java.time.ZoneOffset.UTC))
                .execute(fixture.dispatch()));
        ResolvedProcessTimeoutFact fact = fixture.timeoutStore()
                .find(fixture.dispatch().goalId(), fixture.dispatch().agentRunId())
                .orElseThrow();
        FileSystemRuntimeEventStore eventStore = new FileSystemRuntimeEventStore(
                fixture.root().resolve("missing-process-timeout-event"));
        List<RuntimeEventPublicationReference> publications = new ArrayList<>();

        assertThrows(IOException.class, () -> fixture.executionWithEvents(
                failIfLaunched(),
                Clock.systemUTC(),
                new RuntimeEventRecorder(eventStore, publications::add))
                .execute(fixture.dispatch()));

        RuntimeEvent event = eventStore.resolve(fixture.dispatch().goalId())
                .events().get(0);
        assertEquals(occurredAt, event.occurredAt());
        assertEquals(fact.reference(), event.authoritativeReferences().get(0).reference());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(event)),
                publications);
    }

    @Test
    void processTimeoutPublicationFailureReplaysTheExactPersistedEvent()
            throws Exception {
        Fixture fixture = Fixture.create(temporaryRoot);
        FileSystemRuntimeEventStore eventStore = new FileSystemRuntimeEventStore(
                fixture.root().resolve("process-timeout-publication-recovery"));
        RuntimeEventRecorder failingRecorder = new RuntimeEventRecorder(
                eventStore,
                ignored -> {
                    throw new IOException("process timeout publication unavailable");
                });

        IOException publicationFailure = assertThrows(IOException.class, () ->
                fixture.executionWithEvents(
                        timedOutLauncher(), Clock.systemUTC(), failingRecorder)
                        .execute(fixture.dispatch()));
        assertTrue(publicationFailure.getMessage().contains("publication unavailable"));
        RuntimeEventStream persisted = eventStore.resolve(fixture.dispatch().goalId());
        List<RuntimeEventPublicationReference> replayed = new ArrayList<>();

        assertThrows(IOException.class, () -> fixture.executionWithEvents(
                failIfLaunched(),
                Clock.systemUTC(),
                new RuntimeEventRecorder(eventStore, replayed::add))
                .execute(fixture.dispatch()));

        RuntimeEventStream afterReplay = eventStore.resolve(fixture.dispatch().goalId());
        assertEquals(persisted.revision(), afterReplay.revision());
        assertEquals(persisted.binding(), afterReplay.binding());
        assertEquals(persisted.events(), afterReplay.events());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(
                        persisted.events().get(0))),
                replayed);
    }

    @Test
    void givesEachCycleItsOwnInvocationRoot() throws IOException {
        Fixture first = Fixture.create(temporaryRoot);
        Fixture second = Fixture.create(temporaryRoot);

        assertNotEquals(first.cycleRoot(), second.cycleRoot(),
                "distinct Goal and AgentRun identities must not share a spool");
    }

    @Test
    void cleanupAfterCheckpointIsIdempotentAndDeletesOnlyTheOwnedCycle()
            throws IOException {
        Fixture fixture = Fixture.create(temporaryRoot);
        fixture.execution().execute(fixture.dispatch());
        Path sibling = Files.createDirectories(
                fixture.cycleRoot().getParent().resolve("sibling-cycle"));
        Files.writeString(sibling.resolve("keep.txt"), "foreign cycle\n");

        fixture.execution().cleanupAfterCheckpoint(fixture.dispatch());
        fixture.execution().cleanupAfterCheckpoint(fixture.dispatch());

        assertTrue(Files.notExists(fixture.cycleRoot()));
        assertTrue(Files.exists(sibling.resolve("keep.txt")));
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

    private static WorkerProcessLauncher timedOutLauncher() {
        return new WorkerProcessLauncher() {
            @Override
            public IsolatedWorkerOutcome run(
                    Class<?> entryPoint, List<String> arguments, Duration timeout) {
                return IsolatedWorkerOutcome.refused(
                        IsolatedWorkerStatus.TIMED_OUT, "destroyed");
            }
        };
    }

    private record ModelFixture(
            Path root,
            Path projectRoot,
            AgentRunDispatch dispatch,
            FileSystemRunRecordStore recordStore,
            ModelProcessValidationTestFixture.Prepared prepared,
            Path cycleRoot) {

        static ModelFixture create(Path temporaryRoot, boolean persistRecord)
                throws Exception {
            Path root = Files.createDirectories(
                    temporaryRoot.resolve("model-cycle-" + UUID.randomUUID()));
            Path projectRoot = Files.createDirectories(root.resolve("project"));
            ModelProcessValidationTestFixture.Prepared prepared =
                    ModelProcessValidationTestFixture.valid(projectRoot);
            AgentRunDispatch dispatch = new AgentRunDispatch(
                    UUID.randomUUID().toString(),
                    prepared.workItem(),
                    ModelAttemptTestFixture.GOAL_ID,
                    ModelAttemptTestFixture.AGENT_RUN_ID,
                    Fixture.lease());
            FileSystemRunRecordStore store = new FileSystemRunRecordStore(
                    root.resolve("run-records"));
            if (persistRecord) {
                store.persistModel(
                        AgentRunRecordIdentity.recordId(
                                dispatch.goalId(), dispatch.agentRunId()),
                        prepared.resolved().record());
            }
            Path cycleRoot = root.resolve("invocations")
                    .resolve(dispatch.goalId())
                    .resolve(dispatch.agentRunId());
            return new ModelFixture(
                    root, projectRoot, dispatch, store, prepared, cycleRoot);
        }

        ProcessIsolatedAgentRunExecution execution(WorkerProcessLauncher launcher) {
            return new ProcessIsolatedAgentRunExecution(
                    root.resolve("invocations"),
                    projectRoot,
                    root.resolve("evidence"),
                    root.resolve("run-records"),
                    recordStore,
                    (ModelRunRecordStore) recordStore,
                    prepared.evidenceStore(),
                    prepared.configuration(),
                    launcher,
                    GENEROUS,
                    timeoutStore(),
                    Clock.systemUTC());
        }

        void spoolWork() {
            spool(
                    cycleRoot.resolve(IsolatedWorkerMain.WORK_SPOOL),
                    new TransportMessage(
                            DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL),
                            dispatch.workItem().workMessage()),
                    1);
        }

        void spoolResult() {
            spoolResult(AgentRunRecordIdentity.reference(
                    dispatch.goalId(), dispatch.agentRunId()));
        }

        void spoolResult(String reference) {
            MessageEnvelope work = dispatch.workItem().workMessage();
            MessageEnvelope result = new MessageEnvelope(
                    UUID.randomUUID().toString(),
                    work.correlationId(),
                    Optional.of(work.messageId()),
                    work.logicalRunId(),
                    "isolated-worker",
                    Instant.parse("2026-09-03T12:13:15Z"),
                    new ResultPayload(
                            dispatch.workItem().taskRevision().taskId(),
                            reference,
                            VerificationStatus.VERIFIED));
            spool(
                    cycleRoot.resolve(IsolatedWorkerMain.RESULT_SPOOL),
                    new TransportMessage(
                            DeliveryDestination.queue(
                                    IsolatedWorkerMain.RESULT_DESTINATION),
                            result),
                    1);
        }

        void persistTimeout() throws IOException {
            WorkItem work = dispatch.workItem();
            timeoutStore().persist(ProcessTimeoutFact.create(
                    Instant.parse("2026-09-03T12:14:00Z"),
                    new RuntimeEventBinding(
                            dispatch.goalId(),
                            work.workItemId(),
                            work.taskRevision(),
                            work.snapshotId(),
                            work.logicalRunId(),
                            work.workMessage().correlationId()),
                    dispatch.agentRunId(),
                    GENEROUS,
                    "destroyed"));
        }

        void persistLegacyAtDeterministicIdentity() throws IOException {
            recordStore.persist(
                    AgentRunRecordIdentity.recordId(
                            dispatch.goalId(), dispatch.agentRunId()),
                    prepared.resolved().record().lifecycleRecord());
        }

        void corruptRecord() throws IOException {
            String recordId = AgentRunRecordIdentity.recordId(
                    dispatch.goalId(), dispatch.agentRunId());
            Files.writeString(
                    root.resolve("run-records").resolve(recordId + ".run-record"),
                    "corrupt");
        }

        ProcessTimeoutFactStore timeoutStore() {
            return new FileSystemProcessTimeoutFactStore(
                    root.resolve("invocations").resolve(".process-timeouts"));
        }
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
            return executionWith(launcher, Clock.systemUTC());
        }

        ProcessIsolatedAgentRunExecution executionWith(
                WorkerProcessLauncher launcher, Clock clock) {
            return new ProcessIsolatedAgentRunExecution(
                    root.resolve("invocations"),
                    root.resolve("project"),
                    root.resolve("evidence"),
                    root.resolve("run-records"),
                    runRecordStore,
                    launcher,
                    GENEROUS,
                    timeoutStore(),
                    clock);
        }

        ProcessIsolatedAgentRunExecution executionWithEvents(
                WorkerProcessLauncher launcher,
                Clock clock,
                RuntimeEventRecorder recorder) {
            return new ProcessIsolatedAgentRunExecution(
                    root.resolve("invocations"),
                    root.resolve("project"),
                    root.resolve("evidence"),
                    root.resolve("run-records"),
                    runRecordStore,
                    launcher,
                    GENEROUS,
                    timeoutStore(),
                    clock,
                    recorder);
        }

        ProcessTimeoutFactStore timeoutStore() {
            return new FileSystemProcessTimeoutFactStore(
                    root.resolve("invocations").resolve(".process-timeouts"));
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
