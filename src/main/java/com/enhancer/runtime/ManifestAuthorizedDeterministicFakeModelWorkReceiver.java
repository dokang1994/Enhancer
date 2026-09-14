package com.enhancer.runtime;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.TransportMessage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Receives one typed deterministic-fake envelope using only pre-existing manifest authority.
 *
 * <p>The caller supplies no queue identity, capacity, capability, priority, or destination
 * authority. All are derived and validated before the first possible Scheduler queue mutation,
 * and one fresh real Message Bus connects the exact envelope to durable admission.
 */
final class ManifestAuthorizedDeterministicFakeModelWorkReceiver {
    private static final String SUBSCRIBER_ID =
            "manifest-authorized-deterministic-fake-model-work-admission";

    private final SubmissionManifestStore manifestStore;
    private final SchedulerQueueStore queueStore;

    ManifestAuthorizedDeterministicFakeModelWorkReceiver(
            SubmissionManifestStore manifestStore,
            SchedulerQueueStore queueStore) {
        this.manifestStore = Objects.requireNonNull(
                manifestStore, "manifestStore must not be null");
        this.queueStore = Objects.requireNonNull(
                queueStore, "queueStore must not be null");
    }

    DurableWorkMessageReceiveResult receive(TransportMessage message)
            throws IOException {
        Objects.requireNonNull(message, "message must not be null");
        if (!(message.envelope().payload() instanceof ModelWorkPayload)) {
            throw new IllegalArgumentException(
                    "spooled message payload must be ModelWork");
        }

        String submissionId = message.envelope().messageId();
        DurableSubmissionManifest manifest = manifestStore.resolve(submissionId);
        requireAuthorized(message, manifest);

        QueueResolution resolution = resolveOrCreateQueue(manifest);
        DurableSingleWorkerSchedulerQueue queue = resolution.queue();
        long before = queue.revision();
        DeliveryDestination destination = DeliveryDestination.queue(manifest.queueId());
        InProcessMessageBus bus = new InProcessMessageBus();
        AtomicReference<RuntimeException> handlerFailure = new AtomicReference<>();
        DurableWorkItemAdmissionHandler handler = new DurableWorkItemAdmissionHandler(
                manifest.requiredCapability(),
                manifest.priority(),
                queue);
        bus.subscribe(
                destination,
                SUBSCRIBER_ID,
                envelope -> {
                    try {
                        handler.handle(envelope);
                    } catch (RuntimeException failure) {
                        handlerFailure.set(failure);
                        throw failure;
                    }
                });
        List<DeliveryOutcome> outcomes = bus.publish(
                destination, manifest.workMessage());
        if (outcomes.size() != 1
                || !outcomes.get(0).destination().equals(destination)
                || !outcomes.get(0).subscriberId().equals(Optional.of(SUBSCRIBER_ID))
                || !outcomes.get(0).messageId().equals(submissionId)
                || outcomes.get(0).status() != DeliveryStatus.DELIVERED) {
            RuntimeException failure = handlerFailure.get();
            if (failure instanceof UncheckedIOException unchecked) {
                throw unchecked.getCause();
            }
            throw new IllegalStateException(
                    "Message Bus delivery did not reach durable Scheduler admission",
                    failure);
        }
        long after = queue.revision();
        if (after != before && after != before + 1) {
            throw new IllegalStateException(
                    "durable Scheduler admission changed the queue by an unexpected revision");
        }
        DurableWorkMessageReceiveStatus status = after == before
                ? DurableWorkMessageReceiveStatus.REPLAYED
                : DurableWorkMessageReceiveStatus.ADMITTED;
        return new DurableWorkMessageReceiveResult(
                status,
                queue.queueId(),
                after,
                DurableWorkItemAdmissionHandler.workItemIdFor(submissionId),
                manifest.priority());
    }

    private void requireAuthorized(
            TransportMessage message,
            DurableSubmissionManifest manifest) {
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(message.envelope().messageId());
        require(manifest.workMessage().payload() instanceof ModelWorkPayload,
                "submission manifest payload must be ModelWork");
        require(manifest.submissionId().equals(identities.submissionId()),
                "submission identity does not match the manifest");
        require(manifest.queueId().equals(identities.queueId()),
                "derived queue identity does not match the manifest");
        require(manifest.requiredCapability().equals(
                        DeterministicFakeModelSubmissionCapabilitySource.requiredCapability()),
                "manifest capability is not the closed deterministic-fake capability");
        require(manifest.workMessage().correlationId().equals(identities.correlationId()),
                "derived correlation identity does not match the manifest");
        require(manifest.workMessage().logicalRunId().equals(identities.logicalRunId()),
                "derived logical-run identity does not match the manifest");
        require(manifest.workMessage().causationId().isEmpty(),
                "typed submission manifest causation must remain absent");
        require(manifest.workMessage().equals(message.envelope()),
                "transport envelope does not match the submission manifest");
        require(message.destination().equals(
                        DeliveryDestination.queue(manifest.queueId())),
                "transport destination does not match the manifest queue");
    }

    private QueueResolution resolveOrCreateQueue(
            DurableSubmissionManifest manifest) throws IOException {
        try {
            SchedulerQueueState existing = queueStore.resolve(manifest.queueId());
            if (existing.maxWorkItems() != manifest.maxWorkItems()) {
                throw new IllegalArgumentException(
                        "existing Scheduler queue capacity does not match submission manifest");
            }
            return new QueueResolution(
                    DurableSingleWorkerSchedulerQueue.openForAdmission(
                            existing, queueStore),
                    false);
        } catch (MissingSchedulerQueueStateException exception) {
            return new QueueResolution(
                    DurableSingleWorkerSchedulerQueue.create(
                            manifest.queueId(),
                            manifest.maxWorkItems(),
                            queueStore),
                    true);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record QueueResolution(
            DurableSingleWorkerSchedulerQueue queue,
            boolean created) {
    }
}
