package com.enhancer.runtime;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryDestinationKind;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.FileSpoolPublicationOutcome;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Publishes one untrusted Control intent bound only to an existing active Goal's retained Work
 * envelope. Reading state and transport acceptance grant no authority to apply the signal.
 */
public final class ControlSpoolPublisher {
    private final AgentRuntimeStateStore store;
    private final FileSpoolMessageTransport transport;
    private final DeliveryDestination destination;

    public ControlSpoolPublisher(
            AgentRuntimeStateStore store,
            FileSpoolMessageTransport transport,
            DeliveryDestination destination) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.transport = Objects.requireNonNull(
                transport, "transport must not be null");
        this.destination = Objects.requireNonNull(
                destination, "destination must not be null");
        if (destination.kind() != DeliveryDestinationKind.QUEUE) {
            throw new IllegalArgumentException("destination must be a queue");
        }
    }

    public FileSpoolPublicationOutcome publish(
            String goalId,
            String messageId,
            String producer,
            Instant occurredAt,
            ControlSignal signal,
            String reason) throws IOException {
        AgentRuntimeState state =
                store.resolve(AgentRuntimeState.requireCanonicalGoalId(goalId));
        RuntimeAgentRun run = state.agentRun().orElseThrow(() ->
                new IllegalStateException(
                        "Control publication requires an active Goal and AgentRun"));
        if (state.goal().status() != RuntimeGoalStatus.ACTIVE
                || run.status().isTerminal()) {
            throw new IllegalStateException(
                    "Control publication requires an active Goal and non-terminal AgentRun");
        }
        MessageEnvelope work = state.goal().workItem().workMessage();
        MessageEnvelope control = new MessageEnvelope(
                messageId,
                work.correlationId(),
                Optional.of(work.messageId()),
                work.logicalRunId(),
                producer,
                occurredAt,
                new ControlPayload(signal, reason));
        return transport.sendWithReference(
                new TransportMessage(destination, control));
    }
}
