package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Immutable result of running one isolated worker process.
 *
 * <p>An exit code exists only for {@link IsolatedWorkerStatus#COMPLETED}, so a destroyed or
 * unstartable child can never present a code that reads as a clean exit. A non-completed outcome
 * carries one bounded diagnostic reason instead; child output is never retained.
 */
public record IsolatedWorkerOutcome(
        IsolatedWorkerStatus status,
        OptionalInt exitCode,
        Optional<String> reason) {

    public static final int MAX_REASON_CHARACTERS = 1024;

    public IsolatedWorkerOutcome {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(exitCode, "exitCode must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (status == IsolatedWorkerStatus.COMPLETED) {
            if (exitCode.isEmpty()) {
                throw new IllegalArgumentException("a completed outcome must carry an exit code");
            }
            if (reason.isPresent()) {
                throw new IllegalArgumentException("a completed outcome must not carry a reason");
            }
        } else {
            if (exitCode.isPresent()) {
                throw new IllegalArgumentException(
                        "only a completed outcome may carry an exit code");
            }
            if (reason.isEmpty()) {
                throw new IllegalArgumentException(
                        "a non-completed outcome must carry a reason");
            }
            reason = reason.map(IsolatedWorkerOutcome::bounded);
        }
    }

    static IsolatedWorkerOutcome completed(int exitCode) {
        return new IsolatedWorkerOutcome(
                IsolatedWorkerStatus.COMPLETED, OptionalInt.of(exitCode), Optional.empty());
    }

    static IsolatedWorkerOutcome refused(IsolatedWorkerStatus status, String reason) {
        return new IsolatedWorkerOutcome(status, OptionalInt.empty(), Optional.of(reason));
    }

    private static String bounded(String value) {
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        return trimmed.length() <= MAX_REASON_CHARACTERS
                ? trimmed
                : trimmed.substring(0, MAX_REASON_CHARACTERS);
    }
}
