package com.enhancer.bus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable versioned message envelope. It carries identities, provenance, and one typed
 * payload; possessing an envelope grants no authority, and later delivery code must validate
 * its contents against repository authority rather than trust the sender.
 */
public record MessageEnvelope(
        String messageId,
        String correlationId,
        Optional<String> causationId,
        String logicalRunId,
        String producer,
        Instant occurredAt,
        MessagePayload payload) {

    public static final String ENVELOPE_VERSION = "message-envelope-v1";

    public MessageEnvelope {
        messageId = BusContractSupport.canonicalUuid(messageId, "messageId");
        correlationId = BusContractSupport.bounded(
                correlationId,
                "correlationId",
                BusContractSupport.MAX_IDENTITY_CHARACTERS);
        Objects.requireNonNull(causationId, "causationId must not be null");
        causationId = causationId.map(value ->
                BusContractSupport.canonicalUuid(value, "causationId"));
        logicalRunId = BusContractSupport.bounded(
                logicalRunId,
                "logicalRunId",
                BusContractSupport.MAX_IDENTITY_CHARACTERS);
        producer = BusContractSupport.bounded(
                producer,
                "producer",
                BusContractSupport.MAX_IDENTITY_CHARACTERS);
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        String message = messageId;
        causationId.ifPresent(cause -> {
            if (cause.equals(message)) {
                throw new IllegalArgumentException(
                        "causationId must not equal messageId");
            }
        });
    }
}
