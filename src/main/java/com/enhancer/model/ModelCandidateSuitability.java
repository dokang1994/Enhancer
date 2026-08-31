package com.enhancer.model;

import java.util.Objects;

/** Pure field-free first-match suitability evaluation for the closed fake candidate. */
public final class ModelCandidateSuitability {

    public ModelCandidateSuitabilityDecision evaluate(
            ModelInvocationAdmissionDecision.Admitted admitted,
            DeterministicFakeModelCandidate candidate) {
        Objects.requireNonNull(admitted, "admitted must not be null");
        Objects.requireNonNull(candidate, "candidate must not be null");

        ModelExecutionProfile profile = admitted.profiledRequest().executionProfile();
        if (!candidate.modelClass().equals(profile.modelClass())) {
            return rejected(ModelCandidateSuitabilityRejectionReason.MODEL_CLASS_UNSUPPORTED);
        }
        if (!candidate.requiredCapability().equals(profile.requiredCapability())) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.REQUIRED_CAPABILITY_UNSUPPORTED);
        }
        if (profile.reasoningRequirement() != candidate.maximumReasoningRequirement()) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.REASONING_REQUIREMENT_UNSUPPORTED);
        }
        return rejected(ModelCandidateSuitabilityRejectionReason.TOKEN_SEMANTICS_UNAVAILABLE);
    }

    private static ModelCandidateSuitabilityDecision.Rejected rejected(
            ModelCandidateSuitabilityRejectionReason reason) {
        return new ModelCandidateSuitabilityDecision.Rejected(reason);
    }
}
