package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

final class ProtectedRemoteModelRouteOptionPreparationTestFixture {
    static final Instant OBSERVED_AT = Instant.parse("2026-10-23T00:00:00Z");
    static final Instant EVALUATION_TIME = Instant.parse("2026-10-23T00:01:00Z");
    static final Instant EXPIRES_AT = Instant.parse("2026-10-23T00:05:00Z");
    static final String BINDING_SHA_256 = "b".repeat(64);

    final ProfiledModelRequest profiledRequest;
    final ModelOutboundRouteAdmissionDecision.Eligible eligibility;
    final RemoteModelCandidateCapabilityFacts capabilityFacts;
    final RemoteModelCandidateSuitability suitability;

    ProtectedRemoteModelRouteOptionPreparationTestFixture() {
        ModelTokenBudget tokenBudget = new ModelTokenBudget(10, 5, 15);
        profiledRequest = new ProfiledModelRequest(
                new ModelRequest(
                        "route-preparation-correlation",
                        "protected route preparation prompt sentinel",
                        "reasoning-standard",
                        Duration.ofSeconds(30),
                        4_096),
                new ModelExecutionProfile(
                        ModelExecutionProfile.SCHEMA_VERSION,
                        "repository-analysis",
                        "reasoning-standard",
                        ModelLocalityRequirement.POLICY_CONSTRAINED,
                        ModelReasoningRequirement.STANDARD,
                        20,
                        tokenBudget,
                        new ModelCostBudget("USD", 987_654_321),
                        Duration.ofSeconds(20),
                        ModelDataClassification.INTERNAL));

        ModelRouteCandidateFacts candidateFacts = ModelRouteCandidateFacts.create(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                "candidate-a",
                "candidate-source",
                "candidate-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                "reasoning-standard",
                "repository-analysis",
                "destination-a");
        ModelOutboundContent content = ModelOutboundContent.unchanged(
                profiledRequest.request().prompt());
        ModelContentInspectionFacts inspection = ModelContentInspectionFacts.create(
                ModelContentInspectionFacts.SCHEMA_VERSION,
                "scanner-a",
                "scanner-r1",
                "ruleset-a",
                "ruleset-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                content.sourcePromptDigest(),
                ModelDataClassification.INTERNAL,
                ModelInspectionCoverage.COMPLETE,
                ModelContentInspectionStatus.CLEAR,
                Set.of());
        ModelDestinationAllowRule allowRule = ModelDestinationAllowRule.create(
                "candidate-a",
                "destination-a",
                Set.of(ModelOutboundPurpose.MODEL_INFERENCE),
                Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                Duration.ofMinutes(1),
                ModelDataClassification.RESTRICTED,
                "pricing-source");
        ModelRoutePricingFacts pricingFacts = ModelRoutePricingFacts.create(
                ModelRoutePricingFacts.SCHEMA_VERSION,
                "pricing-source",
                "pricing-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                "candidate-a",
                "destination-a",
                "reasoning-standard",
                ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                "schedule-a",
                tokenBudget,
                "USD",
                1);
        ModelOutboundRouteProposal proposal = new ModelOutboundRouteProposal(
                ModelOutboundRouteProposal.SCHEMA_VERSION,
                candidateFacts,
                ModelDestinationPolicyFacts.create(
                        ModelDestinationPolicyFacts.SCHEMA_VERSION,
                        "destination-a",
                        "outbound-policy-source",
                        "policy-r1",
                        OBSERVED_AT,
                        EXPIRES_AT,
                        Set.of(allowRule)),
                ModelOutboundPurpose.MODEL_INFERENCE,
                new ModelRetentionFacts(
                        Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                        Duration.ofSeconds(30)),
                content,
                Optional.of(inspection),
                Optional.of(inspection),
                Optional.empty(),
                Optional.of(pricingFacts));
        eligibility = new ModelOutboundRouteAdmissionDecision.Eligible(
                proposal, ModelDataClassification.INTERNAL);
        capabilityFacts = new PinnedFileRemoteModelCandidateCapabilityFactSourceTestFacts()
                .facts();
        suitability = assertInstanceOf(
                RemoteModelCandidateSuitabilityDecision.Suitable.class,
                new RemoteModelCandidateSuitabilityEvaluator().evaluate(
                        profiledRequest,
                        eligibility,
                        capabilityFacts,
                        EVALUATION_TIME))
                .suitability();
    }

    ModelRouteOption option() {
        return new ModelRouteOption(profiledRequest, eligibility, suitability);
    }
}
