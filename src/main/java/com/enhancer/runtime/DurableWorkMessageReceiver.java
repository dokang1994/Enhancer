package com.enhancer.runtime;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import java.util.List;
import java.util.Objects;

/**
 * Receives one already-spooled Work envelope through the real Message Bus and reports only after
 * the durable Scheduler admission handler succeeds.
 */
public final class DurableWorkMessageReceiver {
    private static final String SUBSCRIBER_ID = "durable-scheduler-work-admission";

    private final DeliveryDestination expectedDestination;
    private final String requiredCapability;
    private final SchedulerPriority priority;
    private final DurableSingleWorkerSchedulerQueue queue;

    public DurableWorkMessageReceiver(
            DeliveryDestination expectedDestination,
            String requiredCapability,
            SchedulerPriority priority,
            DurableSingleWorkerSchedulerQueue queue) {
        this.expectedDestination = Objects.requireNonNull(
                expectedDestination, "expectedDestination must not be null");
        this.requiredCapability = Objects.requireNonNull(
                requiredCapability, "requiredCapability must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
    }

    public DurableWorkMessageReceiveResult receive(TransportMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        if (!message.destination().equals(expectedDestination)) {
            throw new IllegalArgumentException(
                    "spooled message destination does not match the expected queue");
        }
        if (!(message.envelope().payload() instanceof WorkPayload)) {
            throw new IllegalArgumentException("spooled message payload must be Work");
        }

        long before = queue.revision();
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                expectedDestination,
                SUBSCRIBER_ID,
                new DurableWorkItemAdmissionHandler(requiredCapability, priority, queue));
        List<DeliveryOutcome> outcomes =
                bus.publish(expectedDestination, message.envelope());
        if (outcomes.size() != 1
                || outcomes.get(0).status() != DeliveryStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Message Bus delivery did not reach durable Scheduler admission");
        }
        DurableWorkMessageReceiveStatus status = queue.revision() == before
                ? DurableWorkMessageReceiveStatus.REPLAYED
                : DurableWorkMessageReceiveStatus.ADMITTED;
        return new DurableWorkMessageReceiveResult(
                status,
                queue.queueId(),
                queue.revision(),
                DurableWorkItemAdmissionHandler.workItemIdFor(
                        message.envelope().messageId()),
                priority);
    }
}
