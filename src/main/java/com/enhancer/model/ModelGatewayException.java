package com.enhancer.model;

import java.util.Objects;

/** A typed gateway failure carrying exactly one bounded {@link ModelFailureCode}. */
public final class ModelGatewayException extends Exception {
    public static final int MAX_REASON_CHARACTERS = 1024;

    private static final long serialVersionUID = 1L;

    private final ModelFailureCode code;

    public ModelGatewayException(ModelFailureCode code, String reason) {
        super(validatedReason(reason));
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public ModelFailureCode code() {
        return code;
    }

    private static String validatedReason(String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (reason.length() > MAX_REASON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "reason exceeds " + MAX_REASON_CHARACTERS + " characters");
        }
        return reason;
    }
}
