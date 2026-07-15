package com.enhancer.bus;

import java.util.Objects;

public record ControlPayload(
        ControlSignal signal,
        String reason) implements MessagePayload {

    public static final int MAX_REASON_CHARACTERS = 512;

    public ControlPayload {
        Objects.requireNonNull(signal, "signal must not be null");
        reason = BusContractSupport.bounded(reason, "reason", MAX_REASON_CHARACTERS);
    }
}
