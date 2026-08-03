package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.JournaledMessage;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.RetryPolicy;
import com.enhancer.bus.WorkPayload;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeControlAdmissionIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000601";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000602";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000603";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000604";
    private static final DeliveryDestination CONTROL_QUEUE =
            DeliveryDestination.queue("runtime-controls");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-21T08:00:00Z"),
            ZoneOffset.UTC);

    @TempDir
    Path storageRoot;

    @Test
    void cancelAdmissionRecordsAfterSourceAndExactReplayRepublishesTheSameReference()
            throws Exception {
        Path runtimeRoot = storageRoot.resolve("runtime");
        Path eventRoot = storageRoot.resolve("events");
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        MessageEnvelope cancel = controlMessage(
                "00000000-0000-0000-0000-000000000605",
                ControlSignal.CANCEL,
                "stop safely");
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(eventRoot);
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(eventStore, published::add);

        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(
                        GOAL_ID, store, CLOCK, recorder));
        assertEquals(
                DeliveryStatus.DELIVERED,
                bus.publish(CONTROL_QUEUE, cancel).get(0).status());

        AgentRuntimeState persisted = store.resolve(GOAL_ID);
        RuntimeEventStream events = eventStore.resolve(GOAL_ID);
        assertEquals(3, persisted.revision());
        assertEquals(List.of(cancel), persisted.controlRequests());
        assertEquals(1, events.revision());
        RuntimeEvent event = events.events().get(0);
        assertEquals(
                RuntimeEventKind.CANCELLATION_REQUEST_RECORDED,
                event.kind());
        assertEquals(cancel.occurredAt(), event.occurredAt());
        assertEquals(GOAL_ID, event.binding().goalId());
        assertEquals(WORK_ITEM_ID, event.binding().workItemId());
        assertEquals(workItem().taskRevision(), event.binding().taskRevision());
        assertEquals(workItem().snapshotId(), event.binding().snapshotId());
        assertEquals(
                workItem().logicalRunId(), event.binding().logicalRunId());
        assertEquals(
                cancel.correlationId(), event.binding().correlationId());
        assertEquals(AGENT_RUN_ID, event.agentRunId());
        assertEquals(Optional.of(cancel.messageId()), event.causationId());
        assertEquals(
                new RuntimeEventDetail.CancellationRequestRecorded(
                        cancel.messageId()),
                event.detail());
        assertEquals(
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.CONTROL_MESSAGE,
                                "control-message/" + cancel.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUNTIME_STATE,
                                "agent-runtime/" + GOAL_ID + "/revision/3",
                                Optional.empty())),
                event.authoritativeReferences());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(event)),
                published);

        InProcessMessageBus restartedBus = new InProcessMessageBus();
        restartedBus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(
                        GOAL_ID, store, CLOCK, recorder));
        assertEquals(
                DeliveryStatus.DELIVERED,
                restartedBus.publish(CONTROL_QUEUE, cancel).get(0).status());
        assertEquals(3, store.resolve(GOAL_ID).revision());
        assertEquals(1, eventStore.resolve(GOAL_ID).revision());
        assertEquals(2, published.size());
        assertEquals(published.get(0), published.get(1));
    }

    @Test
    void pauseAndResumeRemainRequestOnlyWhenEventRecorderIsConnected()
            throws Exception {
        Path runtimeRoot = storageRoot.resolve("runtime");
        Path eventRoot = storageRoot.resolve("events");
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(eventRoot);
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(eventStore, published::add);
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(
                        GOAL_ID, store, CLOCK, recorder));

        assertEquals(
                DeliveryStatus.DELIVERED,
                bus.publish(
                                CONTROL_QUEUE,
                                controlMessage(
                                        "00000000-0000-0000-0000-000000000605",
                                        ControlSignal.PAUSE,
                                        "pause only"))
                        .get(0)
                        .status());
        assertEquals(
                DeliveryStatus.DELIVERED,
                bus.publish(
                                CONTROL_QUEUE,
                                controlMessage(
                                        "00000000-0000-0000-0000-000000000606",
                                        ControlSignal.RESUME,
                                        "resume only"))
                        .get(0)
                        .status());

        assertEquals(4, store.resolve(GOAL_ID).revision());
        assertTrue(published.isEmpty());
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> eventStore.resolve(GOAL_ID));
    }

    @Test
    void exactControlReplayRepairsAMissingCancellationEvent()
            throws Exception {
        Path runtimeRoot = storageRoot.resolve("runtime");
        Path eventRoot = storageRoot.resolve("events");
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        MessageEnvelope cancel = controlMessage(
                "00000000-0000-0000-0000-000000000605",
                ControlSignal.CANCEL,
                "repair missing event");
        new RuntimeControlAdmissionHandler(GOAL_ID, store, CLOCK)
                .handle(cancel);
        assertEquals(3, store.resolve(GOAL_ID).revision());

        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(eventRoot);
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> eventStore.resolve(GOAL_ID));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(eventStore, published::add);

        new RuntimeControlAdmissionHandler(GOAL_ID, store, CLOCK, recorder)
                .handle(cancel);

        assertEquals(3, store.resolve(GOAL_ID).revision());
        RuntimeEventStream repaired = eventStore.resolve(GOAL_ID);
        assertEquals(1, repaired.revision());
        assertEquals(1, repaired.events().size());
        assertEquals(
                RuntimeEventKind.CANCELLATION_REQUEST_RECORDED,
                repaired.events().get(0).kind());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(
                        repaired.events().get(0))),
                published);
    }

    @Test
    void queuePersistsExactOrderedControlsAndRestartReplayIsIdempotent()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        MessageEnvelope pause = controlMessage(
                "00000000-0000-0000-0000-000000000605",
                ControlSignal.PAUSE,
                "잠시 멈춤 🚀");
        MessageEnvelope resume = controlMessage(
                "00000000-0000-0000-0000-000000000606",
                ControlSignal.RESUME,
                "다시 시작 🧭");

        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(GOAL_ID, store, CLOCK));
        assertEquals(
                List.of(DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED),
                List.of(
                        bus.publish(CONTROL_QUEUE, pause).get(0).status(),
                        bus.publish(CONTROL_QUEUE, resume).get(0).status()));
        List<JournaledMessage> journal = bus.journal();

        DurableAgentRuntime recovered = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                CLOCK);
        assertEquals(List.of(pause, resume), recovered.controlRequests());
        assertEquals(RuntimeGoalStatus.ACTIVE, recovered.goal().status());
        assertEquals(
                RuntimeAgentRunStatus.READY,
                recovered.agentRun().orElseThrow().status());
        assertEquals(4, recovered.revision());

        InProcessMessageBus restartedBus = new InProcessMessageBus();
        restartedBus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(
                        GOAL_ID,
                        new FileSystemAgentRuntimeStateStore(storageRoot),
                        CLOCK));
        assertEquals(
                List.of(DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED),
                restartedBus.replay(journal).stream()
                        .map(outcome -> outcome.status())
                        .toList());
        DurableAgentRuntime replayed = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(storageRoot),
                CLOCK);
        assertEquals(4, replayed.revision());
        assertEquals(List.of(pause, resume), replayed.controlRequests());
    }

    @Test
    void storageFailureUsesBusRetryAndDeadLetterWithoutExposure()
            throws Exception {
        FileSystemAgentRuntimeStateStore durableStore =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), durableStore, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        FailingUpdateStore failingStore =
                new FailingUpdateStore(durableStore);
        InProcessMessageBus bus = new InProcessMessageBus(
                RetryPolicy.of(2));
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(
                        storageRoot.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(eventStore, published::add);
        bus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(
                        GOAL_ID, failingStore, CLOCK, recorder));

        assertEquals(
                DeliveryStatus.FAILED,
                bus.publish(
                        CONTROL_QUEUE,
                        controlMessage(
                                "00000000-0000-0000-0000-000000000605",
                                ControlSignal.CANCEL,
                                "persist first"))
                        .get(0)
                        .status());
        assertEquals(2, failingStore.updateAttempts);
        assertEquals(1, bus.deadLetters().size());
        assertEquals(2, bus.deadLetters().get(0).attempts());
        assertTrue(DurableAgentRuntime.recover(
                        GOAL_ID, durableStore, CLOCK)
                .controlRequests()
                .isEmpty());
        assertTrue(published.isEmpty());
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> eventStore.resolve(GOAL_ID));
    }

    @Test
    void controlAdmissionDoesNotReclaimAnExpiredLease()
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID,
                "worker-that-still-owns-state",
                Duration.ofMinutes(1));
        Clock afterExpiry = Clock.fixed(
                CLOCK.instant().plus(Duration.ofMinutes(2)),
                ZoneOffset.UTC);
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                CONTROL_QUEUE,
                "runtime-control-admission",
                new RuntimeControlAdmissionHandler(
                        GOAL_ID, store, afterExpiry));

        assertEquals(
                DeliveryStatus.DELIVERED,
                bus.publish(
                        CONTROL_QUEUE,
                        controlMessage(
                                "00000000-0000-0000-0000-000000000605",
                                ControlSignal.PAUSE,
                                "record only"))
                        .get(0)
                        .status());
        AgentRuntimeState stored = store.resolve(GOAL_ID);
        assertEquals(
                RuntimeAgentRunStatus.EXECUTING,
                stored.agentRun().orElseThrow().status());
        assertEquals(
                Optional.of(lease),
                stored.agentRun().orElseThrow().lease());
        assertEquals(4, stored.revision());
    }

    private static WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "persist-bound-runtime-control-requests",
                "CURRENT_TASK.md",
                "a".repeat(64));
        return new WorkItem(
                WORK_ITEM_ID,
                "runtime-worker",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-runtime-control",
                        Optional.empty(),
                        "logical-run-runtime-control",
                        "runtime-control-test",
                        Instant.parse("2026-07-21T07:30:00Z"),
                        new WorkPayload(
                                revision,
                                "b".repeat(64),
                                Set.of("read-file", "verify"))));
    }

    private static MessageEnvelope controlMessage(
            String messageId,
            ControlSignal signal,
            String reason) {
        return new MessageEnvelope(
                messageId,
                "correlation-runtime-control",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-runtime-control",
                "runtime-controller-untrusted",
                Instant.parse("2026-07-21T08:00:00.123456789Z"),
                new ControlPayload(signal, reason));
    }

    private static final class FailingUpdateStore
            implements AgentRuntimeStateStore {
        private final AgentRuntimeStateStore delegate;
        private int updateAttempts;

        private FailingUpdateStore(AgentRuntimeStateStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void create(AgentRuntimeState initialState)
                throws IOException {
            delegate.create(initialState);
        }

        @Override
        public void update(AgentRuntimeState nextState)
                throws IOException {
            updateAttempts++;
            throw new IOException("simulated control persistence failure");
        }

        @Override
        public AgentRuntimeState resolve(String goalId)
                throws IOException {
            return delegate.resolve(goalId);
        }
    }
}
