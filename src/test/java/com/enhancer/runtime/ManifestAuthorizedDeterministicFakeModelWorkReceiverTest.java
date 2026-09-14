package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManifestAuthorizedDeterministicFakeModelWorkReceiverTest {
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f57";
    private static final GeneratedSubmissionIdentities IDENTITIES =
            GeneratedSubmissionIdentities.derive(SUBMISSION_ID);

    @Test
    void derivesAllAdmissionAuthorityFromManifestAndDistinguishesExactReplay()
            throws Exception {
        DurableSubmissionManifest manifest = manifest();
        FixedManifestStore manifests = new FixedManifestStore(manifest);
        RecordingQueueStore queues = new RecordingQueueStore();
        ManifestAuthorizedDeterministicFakeModelWorkReceiver receiver =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        manifests, queues);

        DurableWorkMessageReceiveResult admitted = receiver.receive(message());
        DurableWorkMessageReceiveResult replayed =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        manifests, queues).receive(message());

        assertEquals(DurableWorkMessageReceiveStatus.ADMITTED, admitted.status());
        assertEquals(DurableWorkMessageReceiveStatus.REPLAYED, replayed.status());
        assertEquals(IDENTITIES.queueId(), admitted.queueId());
        assertEquals(SchedulerPriority.EXPEDITED, admitted.priority());
        assertEquals(1L, admitted.queueRevision());
        assertEquals(DurableWorkItemAdmissionHandler.workItemIdFor(SUBMISSION_ID),
                admitted.workItemId());
        assertEquals(1, queues.createCalls);
        assertEquals(1, queues.updateCalls);
        assertEquals(1L, queues.state.revision());
        assertEquals(1, queues.state.pendingWork().size());
    }

    @Test
    void refusesRouteEnvelopeKindAndDerivedIdentityBeforeQueueMutation()
            throws Exception {
        assertRefused(manifest(), new TransportMessage(
                DeliveryDestination.queue("foreign-route"), envelope()));
        assertRefused(manifest(), new TransportMessage(
                destination(), changedEnvelope()));
        assertRefused(manifest(), new TransportMessage(
                destination(), controlEnvelope()));

        DurableSubmissionManifest wrongQueue = new DurableSubmissionManifest(
                "00000000-0000-0000-0000-000000000f58",
                8,
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability(),
                envelope(),
                SchedulerPriority.EXPEDITED);
        assertRefused(wrongQueue, message());

        GeneratedSubmissionIdentities other = GeneratedSubmissionIdentities.derive(
                "00000000-0000-0000-0000-000000000f59");
        MessageEnvelope wrongDerivedEnvelope = new MessageEnvelope(
                SUBMISSION_ID,
                other.correlationId(),
                Optional.empty(),
                other.logicalRunId(),
                "typed-receiver-test",
                Instant.parse("2026-09-14T01:02:03Z"),
                ModelWorkFixtures.payload());
        assertRefused(new DurableSubmissionManifest(
                IDENTITIES.queueId(),
                8,
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability(),
                wrongDerivedEnvelope,
                SchedulerPriority.EXPEDITED),
                new TransportMessage(destination(), wrongDerivedEnvelope));
    }

    @Test
    void refusesCapabilityAndExistingCapacityConflictWithoutQueueMutation()
            throws Exception {
        DurableSubmissionManifest wrongCapability = new DurableSubmissionManifest(
                IDENTITIES.queueId(),
                8,
                "caller-selected-capability",
                envelope(),
                SchedulerPriority.EXPEDITED);
        assertRefused(wrongCapability, message());

        RecordingQueueStore queues = new RecordingQueueStore();
        queues.state = SchedulerQueueState.initial(IDENTITIES.queueId(), 4);
        ManifestAuthorizedDeterministicFakeModelWorkReceiver receiver =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        new FixedManifestStore(manifest()), queues);

        assertThrows(IllegalArgumentException.class, () -> receiver.receive(message()));
        assertEquals(0, queues.createCalls);
        assertEquals(0, queues.updateCalls);
        assertEquals(0L, queues.state.revision());
        assertEquals(4, queues.state.maxWorkItems());
    }

    @Test
    void replayAgainstActiveExactWorkDoesNotPerformWorkerRecoveryOrChangeRevision()
            throws Exception {
        RecordingQueueStore queues = new RecordingQueueStore();
        ManifestAuthorizedDeterministicFakeModelWorkReceiver receiver =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        new FixedManifestStore(manifest()), queues);
        receiver.receive(message());
        DurableSingleWorkerSchedulerQueue workerQueue =
                DurableSingleWorkerSchedulerQueue.recover(
                        IDENTITIES.queueId(), queues);
        workerQueue.claimNext().orElseThrow();
        long activeRevision = queues.state.revision();

        DurableWorkMessageReceiveResult replayed = receiver.receive(message());

        assertEquals(DurableWorkMessageReceiveStatus.REPLAYED, replayed.status());
        assertEquals(activeRevision, queues.state.revision());
        assertTrue(queues.state.activeWork().isPresent());
        assertEquals(0, queues.state.pendingWork().size());
    }

    @Test
    void missingOrUnreadableManifestFailsBeforeQueueAccess() {
        RecordingQueueStore missingQueues = new RecordingQueueStore();
        ManifestAuthorizedDeterministicFakeModelWorkReceiver missing =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        new FailingManifestStore(
                                new MissingSubmissionManifestException(SUBMISSION_ID)),
                        missingQueues);
        assertThrows(MissingSubmissionManifestException.class,
                () -> missing.receive(message()));
        assertEquals(0, missingQueues.resolveCalls);
        assertEquals(0, missingQueues.createCalls);
        assertEquals(0, missingQueues.updateCalls);

        RecordingQueueStore unreadableQueues = new RecordingQueueStore();
        ManifestAuthorizedDeterministicFakeModelWorkReceiver unreadable =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        new FailingManifestStore(new IOException("corrupt manifest")),
                        unreadableQueues);
        assertThrows(IOException.class, () -> unreadable.receive(message()));
        assertEquals(0, unreadableQueues.resolveCalls);
        assertEquals(0, unreadableQueues.createCalls);
        assertEquals(0, unreadableQueues.updateCalls);
    }

    private void assertRefused(
            DurableSubmissionManifest manifest,
            TransportMessage message) {
        RecordingQueueStore queues = new RecordingQueueStore();
        ManifestAuthorizedDeterministicFakeModelWorkReceiver receiver =
                new ManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        new FixedManifestStore(manifest), queues);
        assertThrows(IllegalArgumentException.class, () -> receiver.receive(message));
        assertEquals(0, queues.resolveCalls);
        assertEquals(0, queues.createCalls);
        assertEquals(0, queues.updateCalls);
    }

    private DurableSubmissionManifest manifest() {
        return new DurableSubmissionManifest(
                IDENTITIES.queueId(),
                8,
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability(),
                envelope(),
                SchedulerPriority.EXPEDITED);
    }

    private TransportMessage message() {
        return new TransportMessage(destination(), envelope());
    }

    private DeliveryDestination destination() {
        return DeliveryDestination.queue(IDENTITIES.queueId());
    }

    private MessageEnvelope envelope() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                IDENTITIES.correlationId(),
                Optional.empty(),
                IDENTITIES.logicalRunId(),
                "typed-receiver-test",
                Instant.parse("2026-09-14T01:02:03Z"),
                ModelWorkFixtures.payload());
    }

    private MessageEnvelope changedEnvelope() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                IDENTITIES.correlationId(),
                Optional.empty(),
                IDENTITIES.logicalRunId(),
                "changed-producer",
                Instant.parse("2026-09-14T01:02:03Z"),
                ModelWorkFixtures.payload());
    }

    private MessageEnvelope controlEnvelope() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                IDENTITIES.correlationId(),
                Optional.empty(),
                IDENTITIES.logicalRunId(),
                "typed-receiver-test",
                Instant.parse("2026-09-14T01:02:03Z"),
                new ControlPayload(ControlSignal.CANCEL, "wrong kind"));
    }

    private record FixedManifestStore(DurableSubmissionManifest manifest)
            implements SubmissionManifestStore {
        @Override
        public boolean storeIdempotently(DurableSubmissionManifest ignored) {
            throw new AssertionError("receiver must not store manifests");
        }

        @Override
        public DurableSubmissionManifest resolve(String submissionId) {
            assertEquals(SUBMISSION_ID, submissionId);
            return manifest;
        }
    }

    private record FailingManifestStore(IOException failure)
            implements SubmissionManifestStore {
        @Override
        public boolean storeIdempotently(DurableSubmissionManifest ignored) {
            throw new AssertionError("receiver must not store manifests");
        }

        @Override
        public DurableSubmissionManifest resolve(String submissionId) throws IOException {
            throw failure;
        }
    }

    private static final class RecordingQueueStore implements SchedulerQueueStore {
        private SchedulerQueueState state;
        private int resolveCalls;
        private int createCalls;
        private int updateCalls;

        @Override
        public void create(SchedulerQueueState initialState) {
            createCalls++;
            state = initialState;
        }

        @Override
        public void update(SchedulerQueueState nextState) {
            updateCalls++;
            state = nextState;
        }

        @Override
        public SchedulerQueueState resolve(String queueId) throws IOException {
            resolveCalls++;
            if (state == null || !state.queueId().equals(queueId)) {
                throw new MissingSchedulerQueueStateException(queueId);
            }
            return state;
        }
    }
}
