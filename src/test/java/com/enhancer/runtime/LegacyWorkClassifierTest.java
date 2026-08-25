package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LegacyWorkClassifierTest {
    private static final ApprovedTaskRevision REVISION = new ApprovedTaskRevision(
            "legacy-work-classification",
            "CURRENT_TASK.md",
            "a".repeat(64));

    @Test
    void readFileTakesPrecedenceOverModelInvokeInMixedLegacyScope() {
        assertEquals(
                LegacyWorkClassification.READ_FILE,
                LegacyWorkClassifier.classify(payload("model-invoke", "read-file")));
    }

    @Test
    void modelOnlyLegacyWorkRequiresAProfileAndNeitherToolIsInvalid() {
        assertEquals(
                LegacyWorkClassification.UNPROFILED_MODEL_WORK,
                LegacyWorkClassifier.classify(payload("model-invoke")));
        assertEquals(
                LegacyWorkClassification.INVALID,
                LegacyWorkClassifier.classify(payload("verify")));
    }

    private static WorkPayload payload(String... tools) {
        return new WorkPayload(
                REVISION,
                "b".repeat(64),
                Set.of(tools));
    }
}
