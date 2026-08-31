package com.enhancer.runtime;

import com.enhancer.model.ModelRequest;
import java.time.Duration;
import java.util.Objects;

/** Explicit Scheduler-owned resource limits for one prepared model request. */
public record SchedulerModelInvocationLimits(
        Duration gatewayTimeout,
        int maximumResponseCharacters) {

    public SchedulerModelInvocationLimits {
        Objects.requireNonNull(gatewayTimeout, "gatewayTimeout must not be null");
        if (gatewayTimeout.isZero()
                || gatewayTimeout.isNegative()
                || gatewayTimeout.toMillis() <= 0) {
            throw new IllegalArgumentException("gatewayTimeout must be positive");
        }
        if (gatewayTimeout.compareTo(ModelRequest.MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException(
                    "gatewayTimeout exceeds the supported bound "
                            + ModelRequest.MAX_TIMEOUT);
        }
        if (maximumResponseCharacters <= 0) {
            throw new IllegalArgumentException(
                    "maximumResponseCharacters must be positive");
        }
        if (maximumResponseCharacters > ModelRequest.MAX_RESPONSE_LENGTH) {
            throw new IllegalArgumentException(
                    "maximumResponseCharacters exceeds the supported bound "
                            + ModelRequest.MAX_RESPONSE_LENGTH);
        }
    }
}
