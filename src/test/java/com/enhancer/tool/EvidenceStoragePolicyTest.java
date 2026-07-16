package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EvidenceStoragePolicyTest {
    @Test
    void acceptsOnlySupportedPositiveContentBounds() {
        EvidenceStoragePolicy policy = new EvidenceStoragePolicy(4096);

        assertEquals(4096, policy.maxContentBytes());
        assertThrows(IllegalArgumentException.class, () -> new EvidenceStoragePolicy(0));
        assertThrows(IllegalArgumentException.class, () -> new EvidenceStoragePolicy(
                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES + 1));
    }
}
