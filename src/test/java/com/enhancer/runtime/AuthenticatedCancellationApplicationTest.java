package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
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

class AuthenticatedCancellationApplicationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000004101";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000004102";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000004103";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000004104";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000004105";
    private static final String AUTHORIZATION_ID =
            "00000000-0000-0000-0000-000000004106";
    private static final Clock AUTHORIZED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T14:00:00Z"), ZoneOffset.UTC);
    private static final Clock APPLIED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T14:00:01Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryRoot;

    @Test
    void eventAwareReplayRepairsMissingAppliedCancellationEvent()
            throws Exception {
        FileSystemAgentRuntimeStateStore store = readyRuntimeWith(
                controlMessage(ControlSignal.CANCEL));
        CancellationApplicationRecord source =
                new AuthenticatedCancellationApplication(
                        store,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) -> approved(request.messageId()))
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);
        long sourceRevision = store.resolve(GOAL_ID).revision();
        FileSystemRuntimeEventStore eventStore = new FileSystemRuntimeEventStore(
                temporaryRoot.resolve("missing-applied-event"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();

        CancellationApplicationRecord replayed =
                new AuthenticatedCancellationApplication(
                        store,
                        Clock.fixed(APPLIED_CLOCK.instant().plusSeconds(30), ZoneOffset.UTC),
                        (ignoredGoal, ignoredRequest) -> {
                            throw new AssertionError("durable replay must not reauthorize");
                        },
                        new RuntimeEventRecorder(eventStore, published::add))
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        assertEquals(source, replayed);
        assertEquals(sourceRevision, store.resolve(GOAL_ID).revision());
        RuntimeEvent event = eventStore.resolve(GOAL_ID).events().get(0);
        assertEquals(RuntimeEventKind.CANCELLATION_APPLIED, event.kind());
        assertEquals(source.appliedAt(), event.occurredAt());
        assertEquals(Optional.of(CONTROL_MESSAGE_ID), event.causationId());
        assertEquals(
                new RuntimeEventDetail.CancellationApplied(CONTROL_MESSAGE_ID),
                event.detail());
        assertEquals("authenticated-cancellation-application", event.producerId());
        assertEquals(
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.CONTROL_MESSAGE,
                                "control-message/" + CONTROL_MESSAGE_ID,
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.CONTROL_APPLICATION,
                                source.reference(),
                                Optional.empty())),
                event.authoritativeReferences());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(event)),
                published);
    }

    @Test
    void publicationFailureExactReplaysWithoutReauthorizationOrRuntimeRevision()
            throws Exception {
        FileSystemAgentRuntimeStateStore store = readyRuntimeWith(
                controlMessage(ControlSignal.CANCEL));
        FileSystemRuntimeEventStore eventStore = new FileSystemRuntimeEventStore(
                temporaryRoot.resolve("failed-applied-publication"));
        RuntimeEventRecorder failingRecorder = new RuntimeEventRecorder(
                eventStore,
                ignored -> {
                    throw new IOException("applied publication unavailable");
                });

        assertThrows(IOException.class, () ->
                new AuthenticatedCancellationApplication(
                        store,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) -> approved(request.messageId()),
                        failingRecorder)
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID));
        long sourceRevision = store.resolve(GOAL_ID).revision();
        RuntimeEventStream persisted = eventStore.resolve(GOAL_ID);
        List<RuntimeEventPublicationReference> replayed = new ArrayList<>();

        new AuthenticatedCancellationApplication(
                store,
                Clock.fixed(APPLIED_CLOCK.instant().plusSeconds(60), ZoneOffset.UTC),
                (ignoredGoal, ignoredRequest) -> {
                    throw new AssertionError("durable replay must not reauthorize");
                },
                new RuntimeEventRecorder(eventStore, replayed::add))
                .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        assertEquals(sourceRevision, store.resolve(GOAL_ID).revision());
        assertEquals(persisted.events(), eventStore.resolve(GOAL_ID).events());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(
                        persisted.events().get(0))),
                replayed);
    }

    @Test
    void authorizedCancelPersistsTerminalStateBeforeExposureAndExactReplay()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(temporaryRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, AUTHORIZED_CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "cancellation-owner", Duration.ofMinutes(5));
        runtime.recordControlRequest(controlMessage(ControlSignal.CANCEL));
        AtomicInteger authorizations = new AtomicInteger();
        ControlRequestAuthorizer authorizer = (goalId, request) -> {
            assertEquals(GOAL_ID, goalId);
            authorizations.incrementAndGet();
            return approved(request.messageId());
        };

        CancellationApplicationRecord applied =
                new AuthenticatedCancellationApplication(
                        store, APPLIED_CLOCK, authorizer)
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        AgentRuntimeState persisted = store.resolve(GOAL_ID);
        assertEquals(5, persisted.revision());
        assertEquals(RuntimeGoalStatus.CANCELLED, persisted.goal().status());
        assertEquals(RuntimeAgentRunStatus.CANCELLED,
                persisted.agentRun().orElseThrow().status());
        assertFalse(persisted.agentRun().orElseThrow().lease().isPresent());
        assertEquals(Optional.of(applied), persisted.cancellationApplication());
        assertEquals(CONTROL_MESSAGE_ID, applied.controlMessageId());
        assertEquals(AGENT_RUN_ID, applied.agentRunId());
        assertEquals("operator-17", applied.actorId());
        assertEquals(AUTHORIZED_CLOCK.instant(), applied.authorizedAt());
        assertEquals(APPLIED_CLOCK.instant(), applied.appliedAt());
        assertEquals(1, authorizations.get());
        DurableAgentRuntime cancelledRuntime = DurableAgentRuntime.recover(
                GOAL_ID, store, APPLIED_CLOCK);
        assertThrows(IllegalStateException.class, () ->
                cancelledRuntime.completeExecution(
                        AGENT_RUN_ID, lease.ownerId(), lease.fenceToken()));

        CancellationApplicationRecord replayed =
                new AuthenticatedCancellationApplication(
                        new FileSystemAgentRuntimeStateStore(temporaryRoot),
                        Clock.fixed(APPLIED_CLOCK.instant().plusSeconds(20), ZoneOffset.UTC),
                        (ignoredGoal, ignoredRequest) -> {
                            throw new AssertionError("exact replay must not reauthorize");
                        })
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        assertEquals(applied, replayed);
        assertEquals(5, store.resolve(GOAL_ID).revision());
    }

    @Test
    void denialAndMismatchedApprovalFailBeforeRuntimeMutation() throws Exception {
        FileSystemAgentRuntimeStateStore store = readyRuntimeWith(
                controlMessage(ControlSignal.CANCEL));
        long sourceRevision = store.resolve(GOAL_ID).revision();

        AuthenticatedCancellationApplication denied =
                new AuthenticatedCancellationApplication(
                        store,
                        APPLIED_CLOCK,
                        (ignoredGoal, ignoredRequest) ->
                                new ControlAuthorizationDecision.Denied(
                                "actor lacks cancellation authority"),
                        new RuntimeEventRecorder(
                                new FileSystemRuntimeEventStore(
                                        temporaryRoot.resolve("denied-event")),
                                ignored -> {
                                    throw new AssertionError(
                                            "denial must not publish an event");
                                }));
        assertThrows(ControlAuthorizationDeniedException.class, () ->
                denied.apply(GOAL_ID, CONTROL_MESSAGE_ID));
        assertEquals(sourceRevision, store.resolve(GOAL_ID).revision());
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> new FileSystemRuntimeEventStore(
                        temporaryRoot.resolve("denied-event"))
                        .resolve(GOAL_ID));

        AuthenticatedCancellationApplication mismatched =
                new AuthenticatedCancellationApplication(
                        store,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) ->
                                new ControlAuthorizationDecision.Approved(
                                AUTHORIZATION_ID,
                                "operator-17",
                                "00000000-0000-0000-0000-000000004999",
                                request.messageId(),
                                ControlSignal.CANCEL,
                                AUTHORIZED_CLOCK.instant()));
        assertThrows(IllegalArgumentException.class, () ->
                mismatched.apply(GOAL_ID, CONTROL_MESSAGE_ID));
        assertEquals(sourceRevision, store.resolve(GOAL_ID).revision());
    }

    @Test
    void nonCancelRequestFailsBeforeAuthorizerInvocation() throws Exception {
        FileSystemAgentRuntimeStateStore store = readyRuntimeWith(
                controlMessage(ControlSignal.PAUSE));
        long sourceRevision = store.resolve(GOAL_ID).revision();
        AtomicInteger authorizations = new AtomicInteger();

        AuthenticatedCancellationApplication application =
                new AuthenticatedCancellationApplication(
                        store,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) -> {
                            authorizations.incrementAndGet();
                            return approved(request.messageId());
                        });

        assertThrows(IllegalArgumentException.class, () ->
                application.apply(GOAL_ID, CONTROL_MESSAGE_ID));
        assertEquals(0, authorizations.get());
        assertEquals(sourceRevision, store.resolve(GOAL_ID).revision());
    }

    @Test
    void authorizedCancelTerminatesRetryPendingGoalWithoutRewritingFailedAttempt()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(temporaryRoot.resolve("retry"));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, AUTHORIZED_CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "failed-owner", Duration.ofMinutes(5));
        runtime.completeExecution(
                AGENT_RUN_ID, lease.ownerId(), lease.fenceToken());
        runtime.recordControlRequest(controlMessage(ControlSignal.CANCEL));
        runtime.recordResult(AGENT_RUN_ID, rejectedResult());

        CancellationApplicationRecord applied =
                new AuthenticatedCancellationApplication(
                        store,
                        APPLIED_CLOCK,
                        (ignoredGoal, request) -> approved(request.messageId()))
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);

        AgentRuntimeState persisted = store.resolve(GOAL_ID);
        assertEquals(RuntimeGoalStatus.CANCELLED, persisted.goal().status());
        assertEquals(RuntimeAgentRunStatus.FAILED,
                persisted.agentRun().orElseThrow().status());
        assertEquals(Optional.of(applied), persisted.cancellationApplication());
        assertThrows(IllegalStateException.class, () ->
                DurableAgentRuntime.recover(GOAL_ID, store, APPLIED_CLOCK)
                        .beginRetryAgentRun(
                                "00000000-0000-0000-0000-000000004107"));
    }

    private FileSystemAgentRuntimeStateStore readyRuntimeWith(MessageEnvelope request)
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(temporaryRoot.resolve("ready"));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, AUTHORIZED_CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        runtime.recordControlRequest(request);
        return store;
    }

    private ControlAuthorizationDecision.Approved approved(String controlMessageId) {
        return new ControlAuthorizationDecision.Approved(
                AUTHORIZATION_ID,
                "operator-17",
                GOAL_ID,
                controlMessageId,
                ControlSignal.CANCEL,
                AUTHORIZED_CLOCK.instant());
    }

    private WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-12-authenticated-cancel",
                "CURRENT_TASK.md",
                "7b6c9d8e0f11223344556677889900aabbccddeeff00112233445566778899aa");
        MessageEnvelope work = new MessageEnvelope(
                WORK_MESSAGE_ID,
                "correlation-authenticated-cancel",
                Optional.empty(),
                "logical-run-authenticated-cancel",
                "authenticated-cancel-test",
                Instant.parse("2026-08-04T13:50:00Z"),
                new WorkPayload(
                        revision,
                        "6a5b4c3d2e1f00112233445566778899aabbccddeeff00112233445566778899",
                        Set.of("read-file")));
        return new WorkItem(WORK_ITEM_ID, "read-file-worker", work);
    }

    private MessageEnvelope controlMessage(ControlSignal signal) {
        return new MessageEnvelope(
                CONTROL_MESSAGE_ID,
                "correlation-authenticated-cancel",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-authenticated-cancel",
                "untrusted-control-producer",
                Instant.parse("2026-08-04T13:55:00Z"),
                new ControlPayload(signal, "operator intent"));
    }

    private MessageEnvelope rejectedResult() {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000004108",
                "correlation-authenticated-cancel",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-authenticated-cancel",
                "authenticated-cancel-test",
                Instant.parse("2026-08-04T13:59:00Z"),
                new ResultPayload(
                        workItem().taskRevision().taskId(),
                        "run-record/00000000-0000-0000-0000-000000004109",
                        VerificationStatus.REJECTED));
    }
}
