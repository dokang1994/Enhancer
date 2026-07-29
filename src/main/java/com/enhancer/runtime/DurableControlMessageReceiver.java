package com.enhancer.runtime;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryDestinationKind;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.MessageHandler;
import com.enhancer.bus.TransportMessage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Receives one already-spooled Control envelope through the real Message Bus and reports only
 * after the existing runtime admission handler persists the exact request.
 */
public final class DurableControlMessageReceiver {
    private static final String SUBSCRIBER_ID = "durable-runtime-control-admission";

    private final DeliveryDestination expectedDestination;
    private final String goalId;
    private final AgentRuntimeStateStore store;
    private final Clock clock;

    public DurableControlMessageReceiver(
            DeliveryDestination expectedDestination,
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock) {
        this.expectedDestination = Objects.requireNonNull(
                expectedDestination, "expectedDestination must not be null");
        if (expectedDestination.kind() != DeliveryDestinationKind.QUEUE) {
            throw new IllegalArgumentException(
                    "expectedDestination must be a queue");
        }
        this.goalId = AgentRuntimeState.requireCanonicalGoalId(goalId);
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DurableControlMessageReceiveResult receive(TransportMessage message)
            throws IOException {
        Objects.requireNonNull(message, "message must not be null");
        if (!message.destination().equals(expectedDestination)) {
            throw new IllegalArgumentException(
                    "spooled message destination does not match the expected queue");
        }
        if (!(message.envelope().payload() instanceof ControlPayload control)) {
            throw new IllegalArgumentException(
                    "spooled message payload must be Control");
        }

        long beforeRevision = store.resolve(goalId).revision();
        AtomicReference<RuntimeException> handlerFailure = new AtomicReference<>();
        MessageHandler durableHandler =
                new RuntimeControlAdmissionHandler(goalId, store, clock);
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(expectedDestination, SUBSCRIBER_ID, envelope -> {
            try {
                durableHandler.handle(envelope);
            } catch (RuntimeException failure) {
                handlerFailure.set(failure);
                throw failure;
            }
        });
        List<DeliveryOutcome> outcomes =
                bus.publish(expectedDestination, message.envelope());
        if (outcomes.size() != 1
                || outcomes.get(0).status() != DeliveryStatus.DELIVERED) {
            RuntimeException failure = handlerFailure.get();
            if (failure instanceof UncheckedIOException unchecked) {
                throw unchecked.getCause();
            }
            throw new IllegalStateException(
                    "Message Bus delivery did not reach durable control admission",
                    failure);
        }

        long afterRevision = store.resolve(goalId).revision();
        DurableControlMessageReceiveStatus status =
                afterRevision == beforeRevision
                        ? DurableControlMessageReceiveStatus.REPLAYED
                        : DurableControlMessageReceiveStatus.RECORDED;
        return new DurableControlMessageReceiveResult(
                status,
                goalId,
                afterRevision,
                message.envelope().messageId(),
                control.signal());
    }
}
