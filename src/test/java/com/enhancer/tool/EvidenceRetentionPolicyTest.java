package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class EvidenceRetentionPolicyTest {
    @Test
    void preservesPositiveStorageAndRetentionLimits() {
        EvidenceRetentionPolicy policy = new EvidenceRetentionPolicy(
                1024,
                Duration.ofDays(30));

        assertEquals(1024, policy.maxContentBytes());
        assertEquals(Duration.ofDays(30), policy.retentionPeriod());
    }

    @Test
    void rejectsInvalidStorageAndRetentionLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceRetentionPolicy(0, Duration.ofDays(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceRetentionPolicy(1024, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceRetentionPolicy(
                        EvidenceRetentionPolicy.MAX_SUPPORTED_CONTENT_BYTES + 1,
                        Duration.ofDays(1)));
    }
}
