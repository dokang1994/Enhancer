package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkItemDispositionTest {
    @Test
    void onlyVerifiedCompletionSatisfiesDependencies() {
        assertTrue(WorkItemDisposition.VERIFIED_COMPLETED.satisfiesDependencies());
        assertFalse(WorkItemDisposition.FAILED.satisfiesDependencies());
    }
}
