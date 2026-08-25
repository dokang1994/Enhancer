package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

final class ModelWorkFixtures {
    static final String MESSAGE_ID = "00000000-0000-0000-0000-000000000d01";
    static final String WORK_ITEM_ID = "00000000-0000-0000-0000-000000000d02";
    static final String INDEPENDENT_CAPABILITY = "independent-scheduler-capability";

    private ModelWorkFixtures() {}

    static WorkItem workItem() {
        return new WorkItem(WORK_ITEM_ID, INDEPENDENT_CAPABILITY, envelope());
    }

    static MessageEnvelope envelope() {
        return envelope(profile());
    }

    static MessageEnvelope envelope(ModelExecutionProfile profile) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "model-work-correlation",
                Optional.of("00000000-0000-0000-0000-000000000d03"),
                "model-work-logical-run",
                "durable-model-work-test",
                Instant.parse("2026-08-25T04:05:06.007000008Z"),
                payload(profile));
    }

    static ModelWorkPayload payload() {
        return payload(profile());
    }

    static ModelWorkPayload payload(ModelExecutionProfile profile) {
        return new ModelWorkPayload(
                new ApprovedTaskRevision(
                        "durable-model-work-test",
                        "CURRENT_TASK.md",
                        "a".repeat(64)),
                "b".repeat(64),
                Set.of("model-invoke", "read-file"),
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/model-prompt.md",
                        "c".repeat(64),
                        profile));
    }

    static ModelExecutionProfile profile() {
        return profile("profile-required-capability");
    }

    static ModelExecutionProfile profile(String requiredCapability) {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                requiredCapability,
                "reasoning-standard",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                32_768,
                new ModelTokenBudget(4_096, 2_048, 8_192),
                new ModelCostBudget("USD", 25_000L),
                Duration.ofMillis(45_250),
                ModelDataClassification.CONFIDENTIAL);
    }
}
