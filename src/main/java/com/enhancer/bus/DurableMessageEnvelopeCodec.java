package com.enhancer.bus;

import java.util.Objects;

/**
 * Canonical binary retention boundary for one message envelope embedded by durable stores.
 *
 * <p>The implementation deliberately reuses the transport codec so typed ModelWork has one
 * canonical profile encoding across the process spool, submission manifest, Scheduler queue,
 * and AgentRuntime state. The synthetic destination is an encoding detail and is verified on
 * decode; callers receive only the unchanged envelope.
 */
public final class DurableMessageEnvelopeCodec {
    public static final int MAX_ENCODED_BYTES = MessageEnvelopeCodec.MAX_MESSAGE_BYTES + 40;

    private static final DeliveryDestination DURABLE_DESTINATION =
            DeliveryDestination.queue("durable-message-envelope");

    private final MessageEnvelopeCodec codec = new MessageEnvelopeCodec();

    public byte[] encode(MessageEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        return codec.encode(new TransportMessage(DURABLE_DESTINATION, envelope));
    }

    public MessageEnvelope decode(byte[] encoded) throws CorruptedSpooledMessageException {
        Objects.requireNonNull(encoded, "encoded must not be null");
        TransportMessage decoded = codec.decode(encoded);
        if (!DURABLE_DESTINATION.equals(decoded.destination())) {
            throw new CorruptedSpooledMessageException(
                    "durable message envelope destination is invalid");
        }
        return decoded.envelope();
    }
}
