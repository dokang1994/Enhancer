package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DurableAgentRunRetryControllerTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000003001";
    private static final String FIRST_RUN_ID =
            "00000000-0000-0000-0000-000000003002";
    private static final String SECOND_RUN_ID =
            "00000000-0000-0000-0000-000000003003";
    private static final String OTHER_RUN_ID =
            "00000000-0000-0000-0000-000000003004";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003005";
    private static final String OTHER_WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003006";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000003007";

    @TempDir
    Path storageRoot;

    @Test
    void recordsAndPublishesTheRetryDecisionOnlyAfterItIsDurable()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(storageRoot.resolve("ordered"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder = new RuntimeEventRecorder(
                eventStore,
                reference -> {
                    RuntimeEventStream persisted = eventStore.resolve(GOAL_ID);
                    RuntimeEvent latest = persisted.events()
                            .get(persisted.events().size() - 1);
                    assertEquals(
                            RuntimeEventPublicationReference.from(latest),
                            reference);
                    assertEquals(1,
                            fixture.runtimeStore().state().retryDecisions().size());
                    if (latest.kind() == RuntimeEventKind.RETRY_STARTED) {
                        assertEquals(
                                RuntimeGoalStatus.ACTIVE,
                                fixture.runtimeStore().state().goal().status());
                        assertEquals(
                                SECOND_RUN_ID,
                                fixture.runtimeStore().state().agentRun()
                                        .orElseThrow()
                                        .agentRunId());
                    }
                    published.add(reference);
                });
        Instant occurredAt = Instant.parse("2026-08-03T06:00:00Z");
        DurableAgentRunRetryController controller = eventAwareController(
                fixture,
                Clock.fixed(occurredAt, ZoneOffset.UTC),
                recorder);
        long before = fixture.runtimeStore().state().revision();

        AgentRunRetryDecisionRecord decision = controller.recordDecision(
                GOAL_ID, AgentRunRetryPolicy.of(3));

        RuntimeEventStream stream = eventStore.resolve(GOAL_ID);
        assertEquals(1, stream.revision());
        RuntimeEvent event = stream.events().get(0);
        assertEquals(RuntimeEventKind.RETRY_DECISION_RECORDED, event.kind());
        assertEquals(occurredAt, event.occurredAt());
        assertEquals(GOAL_ID, event.binding().goalId());
        assertEquals(WORK_ITEM_ID, event.binding().workItemId());
        assertEquals(workItem().taskRevision(), event.binding().taskRevision());
        assertEquals(workItem().snapshotId(), event.binding().snapshotId());
        assertEquals(workItem().logicalRunId(), event.binding().logicalRunId());
        assertEquals(
                workItem().workMessage().correlationId(),
                event.binding().correlationId());
        assertEquals(FIRST_RUN_ID, event.agentRunId());
        assertEquals(
                Optional.of("00000000-0000-0000-0000-000000003008"),
                event.causationId());
        assertEquals("durable-agent-run-retry-controller", event.producerId());
        assertEquals(
                new RuntimeEventDetail.RetryDecisionRecorded(
                        true, Optional.empty()),
                event.detail());
        assertEquals(
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RETRY_DECISION,
                                "agent-runtime/"
                                        + GOAL_ID
                                        + "/retry-decision/"
                                        + FIRST_RUN_ID,
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUNTIME_STATE,
                                "agent-runtime/"
                                        + GOAL_ID
                                        + "/revision/"
                                        + (before + 1),
                                Optional.empty())),
                event.authoritativeReferences());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(event)),
                published);
        assertEquals(
                decision,
                fixture.runtimeStore().state().retryDecisions().get(0));

        controller.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);
        RuntimeEventStream afterRetry = eventStore.resolve(GOAL_ID);
        assertEquals(2, afterRetry.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.RETRY_DECISION_RECORDED,
                        RuntimeEventKind.RETRY_STARTED),
                afterRetry.events().stream()
                        .map(RuntimeEvent::kind)
                        .toList());
        RuntimeEvent retryStarted = afterRetry.events().get(1);
        assertEquals(occurredAt, retryStarted.occurredAt());
        assertEquals(GOAL_ID, retryStarted.binding().goalId());
        assertEquals(WORK_ITEM_ID, retryStarted.binding().workItemId());
        assertEquals(SECOND_RUN_ID, retryStarted.agentRunId());
        assertEquals(
                Optional.of("00000000-0000-0000-0000-000000003008"),
                retryStarted.causationId());
        assertEquals("durable-agent-run-retry-controller",
                retryStarted.producerId());
        assertEquals(
                new RuntimeEventDetail.RetryStarted(FIRST_RUN_ID),
                retryStarted.detail());
        assertEquals(
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RETRY_DECISION,
                                "agent-runtime/"
                                        + GOAL_ID
                                        + "/retry-decision/"
                                        + FIRST_RUN_ID,
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUNTIME_STATE,
                                "agent-runtime/"
                                        + GOAL_ID
                                        + "/agent-run/"
                                        + SECOND_RUN_ID,
                                Optional.empty())),
                retryStarted.authoritativeReferences());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(event),
                        RuntimeEventPublicationReference.from(retryStarted)),
                published);
    }

    @Test
    void exactActiveReplayAfterRetryStartedPublicationFailureKeepsFirstOccurrence()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(storageRoot.resolve("retry-publication"));
        List<RuntimeEventPublicationReference> attempts = new ArrayList<>();
        RuntimeEventPublisher publisher = reference -> {
            attempts.add(reference);
            if (attempts.size() == 2) {
                throw new IOException("simulated retry-started publication failure");
            }
        };
        Instant firstOccurrence = Instant.parse("2026-08-03T07:20:00Z");
        DurableAgentRunRetryController first = eventAwareController(
                fixture,
                Clock.fixed(firstOccurrence, ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, publisher));
        first.recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        assertThrows(
                IOException.class,
                () -> first.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));
        RuntimeEvent persisted = eventStore.resolve(GOAL_ID).events().get(1);
        assertEquals(RuntimeEventKind.RETRY_STARTED, persisted.kind());
        assertEquals(firstOccurrence, persisted.occurredAt());

        DurableAgentRuntime.recover(
                        GOAL_ID,
                        fixture.runtimeStore(),
                        Clock.fixed(
                                Instant.parse("2026-08-03T07:21:00Z"),
                                ZoneOffset.UTC))
                .markReady(SECOND_RUN_ID);
        long readyRevision = fixture.runtimeStore().state().revision();
        DurableAgentRunRetryController restarted = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T08:20:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, publisher));

        restarted.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);

        assertEquals(readyRevision, fixture.runtimeStore().state().revision());
        assertEquals(2, eventStore.resolve(GOAL_ID).revision());
        assertEquals(persisted, eventStore.resolve(GOAL_ID).events().get(1));
        assertEquals(3, attempts.size());
        assertEquals(attempts.get(1), attempts.get(2));
    }

    @Test
    void replacementPersistenceFailureCreatesNoRetryStartedEvent()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(storageRoot.resolve("retry-runtime-failure"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunRetryController controller = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T07:30:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, published::add));
        controller.recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));
        fixture.runtimeStore().failNextUpdate();

        assertThrows(
                IOException.class,
                () -> controller.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));

        assertEquals(RuntimeGoalStatus.RETRY_PENDING,
                fixture.runtimeStore().state().goal().status());
        assertEquals(1, eventStore.resolve(GOAL_ID).revision());
        assertEquals(
                List.of(RuntimeEventKind.RETRY_DECISION_RECORDED),
                eventStore.resolve(GOAL_ID).events().stream()
                        .map(RuntimeEvent::kind)
                        .toList());
        assertEquals(1, published.size());
    }

    @Test
    void missingRetryStartedEventRepairsAfterLaterReplacementRevision()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        FailNextRuntimeEventStore eventStore = new FailNextRuntimeEventStore(
                new FileSystemRuntimeEventStore(
                        storageRoot.resolve("retry-append-recovery")));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunRetryController first = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T07:40:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, published::add));
        first.recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));
        eventStore.failNextAppend();

        assertThrows(
                IOException.class,
                () -> first.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));
        assertEquals(RuntimeGoalStatus.ACTIVE,
                fixture.runtimeStore().state().goal().status());
        assertEquals(1, eventStore.resolve(GOAL_ID).revision());

        DurableAgentRuntime.recover(
                        GOAL_ID,
                        fixture.runtimeStore(),
                        Clock.fixed(
                                Instant.parse("2026-08-03T07:41:00Z"),
                                ZoneOffset.UTC))
                .markReady(SECOND_RUN_ID);
        long readyRevision = fixture.runtimeStore().state().revision();
        Instant recoveredOccurrence = Instant.parse("2026-08-03T08:40:00Z");
        DurableAgentRunRetryController restarted = eventAwareController(
                fixture,
                Clock.fixed(recoveredOccurrence, ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, published::add));

        restarted.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);

        assertEquals(readyRevision, fixture.runtimeStore().state().revision());
        RuntimeEventStream stream = eventStore.resolve(GOAL_ID);
        assertEquals(2, stream.revision());
        RuntimeEvent started = stream.events().get(1);
        assertEquals(RuntimeEventKind.RETRY_STARTED, started.kind());
        assertEquals(recoveredOccurrence, started.occurredAt());
        assertEquals(SECOND_RUN_ID, started.agentRunId());
        assertEquals(2, published.size());
    }

    @Test
    void exactReentryAfterPublisherFailureKeepsTheFirstEventOccurrence()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(storageRoot.resolve("publication"));
        List<RuntimeEventPublicationReference> attempts = new ArrayList<>();
        AtomicInteger publications = new AtomicInteger();
        RuntimeEventPublisher failingPublisher = reference -> {
            attempts.add(reference);
            if (publications.getAndIncrement() == 0) {
                throw new IOException("simulated publication failure");
            }
        };
        Instant firstOccurrence = Instant.parse("2026-08-03T06:10:00Z");
        DurableAgentRunRetryController first = eventAwareController(
                fixture,
                Clock.fixed(firstOccurrence, ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, failingPublisher));

        assertThrows(
                IOException.class,
                () -> first.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));
        long decisionRevision = fixture.runtimeStore().state().revision();
        AgentRunRetryDecisionRecord decision =
                fixture.runtimeStore().state().retryDecisions().get(0);
        RuntimeEvent persisted = eventStore.resolve(GOAL_ID).events().get(0);
        assertEquals(firstOccurrence, persisted.occurredAt());

        DurableAgentRunRetryController restarted = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T07:10:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(eventStore, failingPublisher));
        assertEquals(
                decision,
                restarted.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));

        assertEquals(decisionRevision, fixture.runtimeStore().state().revision());
        assertEquals(1, eventStore.resolve(GOAL_ID).revision());
        assertEquals(
                List.of(persisted),
                eventStore.resolve(GOAL_ID).events());
        assertEquals(2, attempts.size());
        assertEquals(attempts.get(0), attempts.get(1));
    }

    @Test
    void runtimePersistenceFailureCreatesNoDecisionEvent()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(storageRoot.resolve("runtime-failure"));
        AtomicInteger publications = new AtomicInteger();
        DurableAgentRunRetryController controller = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T06:20:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(
                        eventStore,
                        reference -> publications.incrementAndGet()));
        fixture.runtimeStore().failNextUpdate();

        assertThrows(
                IOException.class,
                () -> controller.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));

        assertTrue(fixture.runtimeStore().state().retryDecisions().isEmpty());
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> eventStore.resolve(GOAL_ID));
        assertEquals(0, publications.get());
    }

    @Test
    void eventAppendFailureLeavesTheDurableDecisionRecoverable()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        AtomicInteger publications = new AtomicInteger();
        RuntimeEventStore failingStore = new RuntimeEventStore() {
            @Override
            public RuntimeEventAppendResult append(RuntimeEvent event)
                    throws IOException {
                throw new IOException("simulated event append failure");
            }

            @Override
            public RuntimeEventStream resolve(String goalId)
                    throws IOException {
                throw new MissingRuntimeEventStreamException(goalId);
            }
        };
        DurableAgentRunRetryController first = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T06:30:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(
                        failingStore,
                        reference -> publications.incrementAndGet()));

        assertThrows(
                IOException.class,
                () -> first.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));
        AgentRunRetryDecisionRecord decision =
                fixture.runtimeStore().state().retryDecisions().get(0);
        long decisionRevision = fixture.runtimeStore().state().revision();
        assertEquals(0, publications.get());

        FileSystemRuntimeEventStore recoveredStore =
                new FileSystemRuntimeEventStore(storageRoot.resolve("append-recovery"));
        DurableAgentRunRetryController restarted = eventAwareController(
                fixture,
                Clock.fixed(
                        Instant.parse("2026-08-03T06:31:00Z"),
                        ZoneOffset.UTC),
                new RuntimeEventRecorder(
                        recoveredStore,
                        reference -> publications.incrementAndGet()));
        assertEquals(
                decision,
                restarted.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));

        assertEquals(decisionRevision, fixture.runtimeStore().state().revision());
        assertEquals(1, recoveredStore.resolve(GOAL_ID).revision());
        assertEquals(1, publications.get());
    }

    @Test
    void recordsTheExactAdmittedDecisionBeforeApplyingAnyRetryAction()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        long before = fixture.runtimeStore().state().revision();

        AgentRunRetryDecisionRecord recorded = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        assertTrue(recorded.decision().isAdmitted());
        assertEquals(FIRST_RUN_ID, recorded.agentRunId());
        assertEquals(1, recorded.completedAttempts());
        assertEquals(3, recorded.maxAttempts());
        assertEquals(0, recorded.externalEffectLedgerRevision());
        assertEquals(0, recorded.externalEffectRecordCount());
        assertTrue(recorded.externalEffectLedgerSemanticSha256()
                .matches("[0-9a-f]{64}"));
        assertEquals(before + 1, fixture.runtimeStore().state().revision());
        assertEquals(RuntimeGoalStatus.RETRY_PENDING,
                fixture.runtimeStore().state().goal().status());
        assertEquals(1, fixture.runtimeStore().state().agentRuns().size());

        AgentRunRetryDecisionRecord replayed = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        assertEquals(recorded, replayed);
        assertEquals(before + 1, fixture.runtimeStore().state().revision());
    }

    @ParameterizedTest
    @EnumSource(ExternalEffectStatus.class)
    void preservesEveryExternalEffectDecisionOutcome(ExternalEffectStatus status)
            throws Exception {
        ExternalEffectLedgerState ledger = ledgerWith(status);
        AgentRunRetryDecisionRecord recorded = fixture(ledger).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        Optional<AgentRunRetryRefusalReason> expected = switch (status) {
            case COMPENSATED -> Optional.empty();
            case PREPARED -> Optional.of(
                    AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT);
            case REQUIRES_USER_RECOVERY -> Optional.of(
                    AgentRunRetryRefusalReason.EFFECT_REQUIRES_USER_RECOVERY);
            case APPLIED, DEDUPLICATED -> Optional.of(
                    AgentRunRetryRefusalReason.NON_COMPENSATED_EXTERNAL_EFFECT);
        };
        assertEquals(expected, recorded.decision().refusalReason());
    }

    @Test
    void recordsAttemptsExhaustedAndRejectsChangedPolicyOnReplay()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        AgentRunRetryDecisionRecord recorded = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(1));

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED),
                recorded.decision().refusalReason());
        long decidedRevision = fixture.runtimeStore().state().revision();
        assertThrows(IllegalArgumentException.class, () -> fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(2)));
        assertEquals(decidedRevision, fixture.runtimeStore().state().revision());
    }

    @Test
    void failsClosedForMissingOrMismatchedLedgerWithoutWritingRuntime()
            throws Exception {
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore(failedFirstAttempt());
        DurableAgentRunRetryController missingController = controller(
                runtimeStore, new MemoryExternalEffectLedgerStore(null));
        long before = runtimeStore.state().revision();

        assertThrows(MissingExternalEffectLedgerException.class, () ->
                missingController.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));
        assertEquals(before, runtimeStore.state().revision());

        ExternalEffectLedgerState wrongWorkItem =
                ExternalEffectLedgerState.initial(GOAL_ID)
                        .prepare(request("effect-1", WORK_ITEM_ID))
                        .recordOutcome(
                                "effect-1",
                                ExternalEffectStatus.COMPENSATED,
                                outcomeEvidence())
                        .prepare(request("effect-2", OTHER_WORK_ITEM_ID));
        DurableAgentRunRetryController mismatchedController = controller(
                runtimeStore,
                new MemoryExternalEffectLedgerStore(wrongWorkItem));
        assertThrows(IllegalArgumentException.class, () ->
                mismatchedController.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));
        assertEquals(before, runtimeStore.state().revision());
    }

    @Test
    void rejectsNonRetryPendingRuntimeInsteadOfInventingANonFailedDecision()
            throws Exception {
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore(AgentRuntimeState.initial(
                        GOAL_ID, workItem()));
        DurableAgentRunRetryController controller = controller(
                runtimeStore,
                new MemoryExternalEffectLedgerStore(
                        ExternalEffectLedgerState.initial(GOAL_ID)));

        assertThrows(IllegalStateException.class, () -> controller.recordDecision(
                GOAL_ID, AgentRunRetryPolicy.of(3)));
        assertTrue(runtimeStore.state().retryDecisions().isEmpty());
    }

    @Test
    void semanticDigestIsDeterministicAndSensitiveToOrderedRecordSemantics()
            throws Exception {
        ExternalEffectLedgerState firstOrder = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-a", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-a", ExternalEffectStatus.COMPENSATED, outcomeEvidence())
                .prepare(request("effect-b", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-b", ExternalEffectStatus.COMPENSATED, outcomeEvidence());
        ExternalEffectLedgerState secondOrder = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-b", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-b", ExternalEffectStatus.COMPENSATED, outcomeEvidence())
                .prepare(request("effect-a", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-a", ExternalEffectStatus.COMPENSATED, outcomeEvidence());

        String first = fixture(firstOrder).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3))
                .externalEffectLedgerSemanticSha256();
        String restarted = fixture(firstOrder).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3))
                .externalEffectLedgerSemanticSha256();
        String reordered = fixture(secondOrder).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3))
                .externalEffectLedgerSemanticSha256();

        assertEquals(first, restarted);
        assertNotEquals(first, reordered);
    }

    @Test
    void appliesAnAdmittedRetryWithTheCheckpointedIdentifierIdempotently()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        fixture.controller().recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        fixture.controller().beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);
        long appliedRevision = fixture.runtimeStore().state().revision();
        fixture.controller().beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);

        assertEquals(appliedRevision, fixture.runtimeStore().state().revision());
        assertEquals(RuntimeGoalStatus.ACTIVE,
                fixture.runtimeStore().state().goal().status());
        assertEquals(List.of(FIRST_RUN_ID, SECOND_RUN_ID),
                fixture.runtimeStore().state().agentRuns().stream()
                        .map(RuntimeAgentRun::agentRunId)
                        .toList());
        assertThrows(IllegalArgumentException.class, () -> fixture.controller()
                .beginAdmittedRetry(GOAL_ID, OTHER_RUN_ID));
        assertEquals(appliedRevision, fixture.runtimeStore().state().revision());
    }

    @Test
    void appliesARefusedDecisionAsIdempotentTerminalAbandonment()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        fixture.controller().recordDecision(GOAL_ID, AgentRunRetryPolicy.of(1));

        fixture.controller().abandonRefusedRetry(GOAL_ID);
        long abandonedRevision = fixture.runtimeStore().state().revision();
        fixture.controller().abandonRefusedRetry(GOAL_ID);

        assertEquals(abandonedRevision, fixture.runtimeStore().state().revision());
        assertEquals(RuntimeGoalStatus.FAILED,
                fixture.runtimeStore().state().goal().status());
        assertThrows(IllegalStateException.class, () -> fixture.controller()
                .beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));
    }

    @Test
    void recoversAfterActionPersistenceFailureWithoutRevisingDecision()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        AgentRunRetryDecisionRecord decision = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));
        fixture.runtimeStore().failNextUpdate();

        assertThrows(IOException.class, () -> fixture.controller()
                .beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));
        assertEquals(List.of(decision), fixture.runtimeStore().state().retryDecisions());
        assertEquals(RuntimeGoalStatus.RETRY_PENDING,
                fixture.runtimeStore().state().goal().status());

        DurableAgentRunRetryController restarted = controller(
                fixture.runtimeStore(), fixture.effectStore());
        restarted.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);
        assertEquals(RuntimeGoalStatus.ACTIVE,
                fixture.runtimeStore().state().goal().status());
    }

    private static Fixture fixture(ExternalEffectLedgerState ledger) {
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore(failedFirstAttempt());
        MemoryExternalEffectLedgerStore effectStore =
                new MemoryExternalEffectLedgerStore(ledger);
        return new Fixture(
                runtimeStore,
                effectStore,
                controller(runtimeStore, effectStore));
    }

    private static DurableAgentRunRetryController controller(
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore) {
        return new DurableAgentRunRetryController(
                runtimeStore, effectStore, new AgentRunRetryDecider());
    }

    private static DurableAgentRunRetryController eventAwareController(
            Fixture fixture,
            Clock clock,
            RuntimeEventRecorder recorder) {
        return new DurableAgentRunRetryController(
                fixture.runtimeStore(),
                fixture.effectStore(),
                new AgentRunRetryDecider(),
                clock,
                recorder);
    }

    private static AgentRuntimeState failedFirstAttempt() {
        AgentRuntimeState state = AgentRuntimeState.initial(GOAL_ID, workItem());
        state = state.beginAgentRun(FIRST_RUN_ID);
        state = state.markReady(FIRST_RUN_ID);
        state = state.acquireLease(
                FIRST_RUN_ID,
                "retry-controller-worker",
                Instant.parse("2026-07-22T04:00:00Z"),
                Duration.ofMinutes(5));
        state = state.completeExecution(
                FIRST_RUN_ID,
                "retry-controller-worker",
                1,
                Instant.parse("2026-07-22T04:01:00Z"));
        return state.recordAttemptResult(FIRST_RUN_ID, failedResult());
    }

    private static ExternalEffectLedgerState ledgerWith(
            ExternalEffectStatus status) {
        ExternalEffectLedgerState ledger = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-1", WORK_ITEM_ID));
        return status == ExternalEffectStatus.PREPARED
                ? ledger
                : ledger.recordOutcome(
                        "effect-1", status, outcomeEvidence());
    }

    private static ExternalEffectRequest request(String key, String workItemId) {
        return new ExternalEffectRequest(
                key,
                GOAL_ID,
                FIRST_RUN_ID,
                workItemId,
                "retry-controller-adapter",
                "publish-artifact",
                "c".repeat(64));
    }

    private static ExternalEffectOutcomeEvidence outcomeEvidence() {
        return new ExternalEffectOutcomeEvidence(
                "evidence/00000000-0000-0000-0000-000000001220/"
                        + "00000000-0000-0000-0000-000000001221",
                "e".repeat(64));
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "retry-controller",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-retry-controller",
                        Optional.empty(),
                        "logical-run-retry-controller",
                        "retry-controller-test",
                        Instant.parse("2026-07-22T03:55:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "add-durable-agentrun-retry-controller",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static MessageEnvelope failedResult() {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000003008",
                "correlation-retry-controller",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-retry-controller",
                "retry-controller-result",
                Instant.parse("2026-07-22T04:02:00Z"),
                new ResultPayload(
                        "add-durable-agentrun-retry-controller",
                        "run-record/retry-controller",
                        VerificationStatus.REJECTED));
    }

    private record Fixture(
            MemoryAgentRuntimeStateStore runtimeStore,
            MemoryExternalEffectLedgerStore effectStore,
            DurableAgentRunRetryController controller) {
    }

    private static final class FailNextRuntimeEventStore
            implements RuntimeEventStore {
        private final RuntimeEventStore delegate;
        private boolean failNextAppend;

        private FailNextRuntimeEventStore(RuntimeEventStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public RuntimeEventAppendResult append(RuntimeEvent event)
                throws IOException {
            if (failNextAppend) {
                failNextAppend = false;
                throw new IOException("simulated retry-started append failure");
            }
            return delegate.append(event);
        }

        @Override
        public RuntimeEventStream resolve(String goalId) throws IOException {
            return delegate.resolve(goalId);
        }

        void failNextAppend() {
            failNextAppend = true;
        }
    }

    private static final class MemoryAgentRuntimeStateStore
            implements AgentRuntimeStateStore {
        private AgentRuntimeState state;
        private boolean failNextUpdate;

        private MemoryAgentRuntimeStateStore(AgentRuntimeState state) {
            this.state = state;
        }

        @Override
        public void create(AgentRuntimeState initialState) throws IOException {
            if (state != null) {
                throw new IOException("runtime already exists");
            }
            state = initialState;
        }

        @Override
        public void update(AgentRuntimeState nextState) throws IOException {
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IOException("simulated persistence failure");
            }
            if (state == null || nextState.revision() != state.revision() + 1) {
                throw new IOException("revision does not advance by one");
            }
            state = nextState;
        }

        @Override
        public AgentRuntimeState resolve(String goalId) throws IOException {
            if (state == null || !state.goal().goalId().equals(goalId)) {
                throw new MissingAgentRuntimeStateException(goalId);
            }
            return state;
        }

        AgentRuntimeState state() {
            return state;
        }

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }

    private static final class MemoryExternalEffectLedgerStore
            implements ExternalEffectLedgerStore {
        private ExternalEffectLedgerState state;

        private MemoryExternalEffectLedgerStore(ExternalEffectLedgerState state) {
            this.state = state;
        }

        @Override
        public void create(ExternalEffectLedgerState initialState)
                throws IOException {
            if (state != null) {
                throw new IOException("ledger already exists");
            }
            state = initialState;
        }

        @Override
        public void update(ExternalEffectLedgerState nextState)
                throws IOException {
            if (state == null || nextState.revision() != state.revision() + 1) {
                throw new IOException("revision does not advance by one");
            }
            state = nextState;
        }

        @Override
        public ExternalEffectLedgerState resolve(String goalId)
                throws IOException {
            if (state == null) {
                throw new MissingExternalEffectLedgerException(goalId);
            }
            return state;
        }
    }
}
