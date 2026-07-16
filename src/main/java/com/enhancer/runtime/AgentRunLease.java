package com.enhancer.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable single-owner AgentRun lease. Possession grants only fence-checked lifecycle
 * transition authority already bounded by the retained WorkItem.
 */
public record AgentRunLease(
        String ownerId,
        long fenceToken,
        Instant issuedAt,
        Instant expiresAt) {

    public static final int MAX_OWNER_CHARACTERS = 256;
    public static final Duration MAX_DURATION = Duration.ofHours(24);
    private static final Duration MIN_DURATION = Duration.ofMillis(1);

    public AgentRunLease {
        validateOwner(ownerId);
        if (fenceToken <= 0) {
            throw new IllegalArgumentException(
                    "fenceToken must be positive");
        }
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        validateDuration(Duration.between(issuedAt, expiresAt));
    }

    static void validateRequest(
            String ownerId,
            Duration duration) {
        validateOwner(ownerId);
        validateDuration(duration);
    }

    static AgentRunLease issue(
            String ownerId,
            long fenceToken,
            Instant issuedAt,
            Duration duration) {
        validateDuration(duration);
        return new AgentRunLease(
                ownerId,
                fenceToken,
                issuedAt,
                issuedAt.plus(duration));
    }

    AgentRunLease renew(Instant renewedAt, Duration duration) {
        Objects.requireNonNull(renewedAt, "renewedAt must not be null");
        validateDuration(duration);
        AgentRunLease renewed = issue(
                ownerId,
                fenceToken,
                renewedAt,
                duration);
        if (!renewed.expiresAt.isAfter(expiresAt)) {
            throw new IllegalArgumentException(
                    "renewal must extend the current lease expiry");
        }
        return renewed;
    }

    public boolean isExpiredAt(Instant observedAt) {
        Objects.requireNonNull(
                observedAt, "observedAt must not be null");
        return !observedAt.isBefore(expiresAt);
    }

    void requireCurrent(
            String expectedOwnerId,
            long expectedFenceToken,
            Instant observedAt) {
        Objects.requireNonNull(
                expectedOwnerId, "expectedOwnerId must not be null");
        Objects.requireNonNull(
                observedAt, "observedAt must not be null");
        if (!ownerId.equals(expectedOwnerId)) {
            throw new IllegalArgumentException(
                    "lease owner does not match the current owner");
        }
        if (fenceToken != expectedFenceToken) {
            throw new IllegalArgumentException(
                    "fence token does not match the current lease");
        }
        if (isExpiredAt(observedAt)) {
            throw new IllegalStateException(
                    "AgentRun lease has expired");
        }
    }

    private static void validateDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.compareTo(MIN_DURATION) < 0
                || duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "lease duration must be between 1 millisecond and 24 hours");
        }
    }

    private static void validateOwner(String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException(
                    "ownerId must not be blank");
        }
        if (ownerId.length() > MAX_OWNER_CHARACTERS) {
            throw new IllegalArgumentException(
                    "ownerId must not exceed "
                            + MAX_OWNER_CHARACTERS + " characters");
        }
    }
}
