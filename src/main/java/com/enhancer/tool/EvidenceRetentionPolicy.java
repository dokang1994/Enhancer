package com.enhancer.tool;

import java.time.Duration;
import java.util.Objects;

public record EvidenceRetentionPolicy(
        long maxContentBytes,
        Duration retentionPeriod) {

    public static final long MAX_SUPPORTED_CONTENT_BYTES = 64L * 1024 * 1024;

    public EvidenceRetentionPolicy {
        Objects.requireNonNull(retentionPeriod, "retentionPeriod must not be null");
        if (maxContentBytes <= 0) {
            throw new IllegalArgumentException("maxContentBytes must be positive");
        }
        if (maxContentBytes > MAX_SUPPORTED_CONTENT_BYTES) {
            throw new IllegalArgumentException("maxContentBytes exceeds the supported in-memory limit");
        }
        if (retentionPeriod.isZero() || retentionPeriod.isNegative()) {
            throw new IllegalArgumentException("retentionPeriod must be positive");
        }
    }
}
