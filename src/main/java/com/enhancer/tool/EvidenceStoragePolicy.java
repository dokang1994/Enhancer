package com.enhancer.tool;

public record EvidenceStoragePolicy(long maxContentBytes) {
    public static final long MAX_SUPPORTED_CONTENT_BYTES = 64L * 1024 * 1024;

    public EvidenceStoragePolicy {
        if (maxContentBytes <= 0) {
            throw new IllegalArgumentException("maxContentBytes must be positive");
        }
        if (maxContentBytes > MAX_SUPPORTED_CONTENT_BYTES) {
            throw new IllegalArgumentException(
                    "maxContentBytes exceeds the supported in-memory limit");
        }
    }
}
