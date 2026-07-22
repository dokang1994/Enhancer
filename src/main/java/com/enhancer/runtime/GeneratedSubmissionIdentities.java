package com.enhancer.runtime;

import java.util.Objects;
import java.util.UUID;

/**
 * The stable identities generated from one caller-retained submission UUID.
 *
 * <p>The submission UUID is the sole replay key and the work message identity. The queue,
 * correlation, and logical-run identities are derived from it through fixed, versioned,
 * domain-separated one-to-one UUID transforms, so the same key always names the same generated
 * identities across restarts and fresh stores without a second durable record. Changing any
 * transform constant is a breaking change and must advance {@link #DERIVATION_VERSION}.
 */
public record GeneratedSubmissionIdentities(
        String submissionId,
        String queueId,
        String correlationId,
        String logicalRunId) {

    /** Version of the domain-separation scheme below; bump when any domain constant changes. */
    public static final int DERIVATION_VERSION = 1;

    private static final long QUEUE_DOMAIN = 0x9E3779B97F4A7C15L;
    private static final long CORRELATION_DOMAIN = 0xC2B2AE3D27D4EB4FL;
    private static final long LOGICAL_RUN_DOMAIN = 0x165667B19E3779F9L;

    public GeneratedSubmissionIdentities {
        submissionId = canonicalSubmissionId(submissionId);
        Objects.requireNonNull(queueId, "queueId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(logicalRunId, "logicalRunId must not be null");
    }

    /** Derives the fixed generated identities for one canonical submission UUID. */
    public static GeneratedSubmissionIdentities derive(String submissionId) {
        String canonical = canonicalSubmissionId(submissionId);
        UUID source = UUID.fromString(canonical);
        return new GeneratedSubmissionIdentities(
                canonical,
                domainTransform(source, QUEUE_DOMAIN),
                domainTransform(source, CORRELATION_DOMAIN),
                domainTransform(source, LOGICAL_RUN_DOMAIN));
    }

    private static String domainTransform(UUID source, long domain) {
        return new UUID(
                source.getMostSignificantBits() ^ domain,
                source.getLeastSignificantBits())
                .toString();
    }

    static String canonicalSubmissionId(String value) {
        Objects.requireNonNull(value, "submissionId must not be null");
        try {
            String canonical = UUID.fromString(value).toString();
            if (!canonical.equals(value)) {
                throw new IllegalArgumentException(
                        "submissionId must be a canonical UUID");
            }
            return canonical;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "submissionId must be a canonical UUID", exception);
        }
    }
}
