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
        if (!candidate.tokenSemanticsAvailable()) {
            return rejected(ModelCandidateSuitabilityRejectionReason.TOKEN_SEMANTICS_UNAVAILABLE);
        }
        if (profile.minimumContextTokens() > candidate.maximumContextTokens()) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.CONTEXT_CAPACITY_INSUFFICIENT);
        }
        if (profile.tokenBudget().maxInputTokens() > candidate.maximumInputTokens()) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.INPUT_TOKEN_CAPACITY_INSUFFICIENT);
        }
        if (profile.tokenBudget().maxOutputTokens() > candidate.maximumOutputTokens()) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.OUTPUT_TOKEN_CAPACITY_INSUFFICIENT);
        }
        if (profile.tokenBudget().maxTotalTokens() > candidate.maximumTotalTokens()) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.TOTAL_TOKEN_CAPACITY_INSUFFICIENT);
        }
        if (profile.costBudget().maxMicrounits() != 0L) {
            return rejected(ModelCandidateSuitabilityRejectionReason.FREE_ONLY_COST_REQUIRED);
        }
        if (profile.dataClassification().compareTo(
                        candidate.maximumDataClassification())
                > 0) {
            return rejected(
                    ModelCandidateSuitabilityRejectionReason.DATA_CLASSIFICATION_UNSUPPORTED);
        }
        return new ModelCandidateSuitabilityDecision.Suitable(admitted, candidate);
    }

    private static ModelCandidateSuitabilityDecision.Rejected rejected(
            ModelCandidateSuitabilityRejectionReason reason) {
        return new ModelCandidateSuitabilityDecision.Rejected(reason);
    }
}
