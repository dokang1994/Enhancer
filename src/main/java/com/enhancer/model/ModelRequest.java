package com.enhancer.model;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One immutable bounded model invocation request.
 *
 * <p>The model class is a stable capability label owned by this repository, never a
 * provider model name, so no provider wire vocabulary reaches a persisted type. The
 * timeout and maximum response length are the RFC-0013 budget stub.
 */
public record ModelRequest(
        String correlationId,
        String prompt,
        String modelClass,
        Duration timeout,
        int maxResponseLength) {

    public static final int MAX_CORRELATION_ID_CHARACTERS = 128;
    public static final int MAX_PROMPT_CHARACTERS = 262_144;
    public static final int MAX_MODEL_CLASS_CHARACTERS = 64;
    public static final Duration MAX_TIMEOUT = Duration.ofMinutes(5);
    public static final int MAX_RESPONSE_LENGTH = 262_144;

    private static final Pattern MODEL_CLASS =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public ModelRequest {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        if (correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
        if (correlationId.length() > MAX_CORRELATION_ID_CHARACTERS) {
            throw new IllegalArgumentException(
                    "correlationId exceeds " + MAX_CORRELATION_ID_CHARACTERS + " characters");
        }
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (prompt.length() > MAX_PROMPT_CHARACTERS) {
            throw new IllegalArgumentException(
                    "prompt exceeds " + MAX_PROMPT_CHARACTERS + " characters");
        }
        if (modelClass.length() > MAX_MODEL_CLASS_CHARACTERS
                || !MODEL_CLASS.matcher(modelClass).matches()) {
            throw new IllegalArgumentException(
                    "modelClass must be a stable lowercase hyphenated label of at most "
                            + MAX_MODEL_CLASS_CHARACTERS + " characters");
        }
        if (timeout.isZero() || timeout.isNegative() || timeout.toMillis() <= 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException(
                    "timeout exceeds the supported bound " + MAX_TIMEOUT);
        }
        if (maxResponseLength <= 0) {
            throw new IllegalArgumentException("maxResponseLength must be positive");
        }
        if (maxResponseLength > MAX_RESPONSE_LENGTH) {
            throw new IllegalArgumentException(
                    "maxResponseLength exceeds the supported bound " + MAX_RESPONSE_LENGTH);
        }
    }
}
