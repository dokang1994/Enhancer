package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Gate 7-to-Gate 8 adapter that persists one dependency-free delivered work envelope in a
 * durable Scheduler queue before reporting handler success.
 */
public final class DurableWorkItemAdmissionHandler implements MessageHandler {
    private static final long WORK_ITEM_DOMAIN_BIT = Long.MIN_VALUE;

    private final String requiredCapability;
    private final DurableSingleWorkerSchedulerQueue queue;

    public DurableWorkItemAdmissionHandler(
            String requiredCapability,
            DurableSingleWorkerSchedulerQueue queue) {
        this.requiredCapability = Objects.requireNonNull(
                requiredCapability, "requiredCapability must not be null");
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
    }

    @Override
    public void handle(MessageEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        WorkItem workItem = new WorkItem(
                workItemIdFor(envelope.messageId()),
                requiredCapability,
                envelope);
        try {
            queue.enqueue(new QueuedWork(workItem, Set.of()));
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "work item could not be persisted in the Scheduler queue",
                    exception);
        }
    }

    /**
     * Applies a fixed one-to-one UUID domain transform. The result is stable, canonical, and
     * always distinct from its source message identity.
     */
    static String workItemIdFor(String messageId) {
        UUID source = UUID.fromString(Objects.requireNonNull(
                messageId, "messageId must not be null"));
        return new UUID(
                source.getMostSignificantBits() ^ WORK_ITEM_DOMAIN_BIT,
                source.getLeastSignificantBits())
                .toString();
    }
}
