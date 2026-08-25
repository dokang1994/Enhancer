package com.enhancer.runtime;

import com.enhancer.bus.WorkPayload;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.tool.ReadFileTool;
import java.util.Objects;

/** Pure deterministic classifier used only by explicit stopped-owner migration. */
final class LegacyWorkClassifier {
    private LegacyWorkClassifier() {}

    static LegacyWorkClassification classify(WorkPayload payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.allowedTools().contains(ReadFileTool.NAME)) {
            return LegacyWorkClassification.READ_FILE;
        }
        if (payload.allowedTools().contains(ModelInvokeTool.NAME)) {
            return LegacyWorkClassification.UNPROFILED_MODEL_WORK;
        }
        return LegacyWorkClassification.INVALID;
    }
}
