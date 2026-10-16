package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RemoteModelCandidateSuitabilityEvaluatorTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-10-16T00:00:00Z");
    private static final Instant EVALUATION_TIME = Instant.parse("2026-10-16T00:01:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-16T00:05:00Z");

    private final RemoteModelCandidateSuitabilityEvaluator evaluator =
            new RemoteModelCandidateSuitabilityEvaluator();

    @Test
    void everyRefusalIsReturnedAloneInRfcOrder() {
        for (RemoteModelCandidateSuitabilityRefusalReason reason
                : RemoteModelCandidateSuitabilityRefusalReason.values()) {
            assertRefused(new Scenario().breakFor(reason), reason);
        }
    }

    @Test
    void everyAdjacentCombinedFailureReturnsTheEarlierReason() {
        RemoteModelCandidateSuitabilityRefusalReason[] reasons =
                RemoteModelCandidateSuitabilityRefusalReason.values();
        for (int index = 0; index < reasons.length - 1; index++) {
            Scenario scenario = new Scenario()
                    .breakFor(reasons[index + 1])
                    .breakFor(reasons[index]);
            assertRefused(scenario, reasons[index]);
        }
    }

    @Test
    void successfulEvaluationRetainsExactAuthorityInstancesAndCapabilityValues() {
        Inputs inputs = new Scenario().inputs();

        RemoteModelCandidateSuitabilityDecision.Suitable suitable = assertInstanceOf(
                RemoteModelCandidateSuitabilityDecision.Suitable.class,
                evaluator.evaluate(
                        inputs.profiledRequest(),
                        inputs.eligibility(),
                        inputs.capabilityFacts(),
                        EVALUATION_TIME));
        RemoteModelCandidateSuitability proof = suitable.suitability();

        assertSame(inputs.profiledRequest(), proof.profiledRequest());
        assertSame(inputs.eligibility(), proof.eligibility());
        assertSame(inputs.eligibility().proposal().candidateFacts(), proof.candidateFacts());
        assertEquals("capability-source", proof.suitabilitySourceIdentity());
        assertEquals("capability-r1", proof.sourceRevision());
        assertEquals("capacity-evidence-a", proof.capacityEvidenceIdentity());
        assertEquals("candidate-a", proof.candidateIdentity());
        assertEquals("destination-a", proof.destinationIdentity());
        assertEquals(new ModelTokenBudget(10, 5, 20), proof.maximumTokenBudget());
        assertEquals(ModelDataClassification.INTERNAL,
                proof.maximumDataClassification());
    }

    @Test
    void allInputsAreRequiredProgrammingValues() {
        Inputs inputs = new Scenario().inputs();

        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(
                        null, inputs.eligibility(), inputs.capabilityFacts(), EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(
                        inputs.profiledRequest(), null, inputs.capabilityFacts(), EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(
                        inputs.profiledRequest(), inputs.eligibility(), null, EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(
                        inputs.profiledRequest(), inputs.eligibility(),
                        inputs.capabilityFacts(), null));
    }

    @Test
    void observationIsInclusiveAndExpiryIsExclusiveForAllThreeFactKinds() {
        Inputs atObservation = new Scenario().inputs();
        assertInstanceOf(
                RemoteModelCandidateSuitabilityDecision.Suitable.class,
                evaluator.evaluate(
                        atObservation.profiledRequest(),
                        atObservation.eligibility(),
                        atObservation.capabilityFacts(),
                        OBSERVED_AT));

        Scenario candidateExpiry = new Scenario();
        candidateExpiry.evaluationTime = EXPIRES_AT;
        candidateExpiry.capabilityExpiresAt = EXPIRES_AT.plusSeconds(1);
        candidateExpiry.pricingExpiresAt = EXPIRES_AT.plusSeconds(1);
        assertRefused(candidateExpiry,
                RemoteModelCandidateSuitabilityRefusalReason.CANDIDATE_FACTS_INVALID_OR_STALE);

        Scenario capabilityExpiry = new Scenario();
        capabilityExpiry.evaluationTime = EXPIRES_AT;
        capabilityExpiry.candidateExpiresAt = EXPIRES_AT.plusSeconds(1);
        capabilityExpiry.pricingExpiresAt = EXPIRES_AT.plusSeconds(1);
        assertRefused(capabilityExpiry,
                RemoteModelCandidateSuitabilityRefusalReason.CAPABILITY_FACTS_INVALID_OR_STALE);

        Scenario pricingExpiry = new Scenario();
        pricingExpiry.evaluationTime = EXPIRES_AT;
        pricingExpiry.candidateExpiresAt = EXPIRES_AT.plusSeconds(1);
        pricingExpiry.capabilityExpiresAt = EXPIRES_AT.plusSeconds(1);
        assertRefused(pricingExpiry,
                RemoteModelCandidateSuitabilityRefusalReason.PRICING_FACTS_INVALID_OR_STALE);
    }

    @Test
    void everyRouteProfileAndPricingBindingFailsClosed() {
        Scenario candidate = new Scenario();
        candidate.capabilityCandidate = "candidate-b";
        assertRefused(candidate,
                RemoteModelCandidateSuitabilityRefusalReason.CANDIDATE_BINDING_MISMATCH);

        Scenario destination = new Scenario();
        destination.capabilityDestination = "destination-b";
        assertRefused(destination,
                RemoteModelCandidateSuitabilityRefusalReason.DESTINATION_BINDING_MISMATCH);

        Scenario capability = new Scenario();
        capability.profileCapability = "different-capability";
        assertRefused(capability,
                RemoteModelCandidateSuitabilityRefusalReason.PROFILE_BINDING_MISMATCH);

        Scenario modelClass = new Scenario();
        modelClass.profileModelClass = "different-model";
        assertRefused(modelClass,
                RemoteModelCandidateSuitabilityRefusalReason.PROFILE_BINDING_MISMATCH);

        for (Scenario pricing : new Scenario[] {
                new Scenario().withPricingCandidate("candidate-b"),
                new Scenario().withPricingDestination("destination-b"),
                new Scenario().withPricingModelClass("different-model"),
                new Scenario().withPricingSource("other-pricing-source"),
                new Scenario().withCapabilitySchedule("schedule-b"),
                new Scenario().withCapabilityCostBasis(new ModelTokenBudget(9, 5, 20))
        }) {
            assertRefused(pricing,
                    RemoteModelCandidateSuitabilityRefusalReason.PRICING_BINDING_MISMATCH);
        }
    }

    @Test
    void capacityReasoningTimeoutClassificationAndCostUseExactBoundaries() {
        assertSuitable(new Scenario());

        Scenario above = new Scenario();
        above.maximumContextTokens = 21;
        above.maximumInputTokens = 11;
        above.maximumOutputTokens = 6;
        above.maximumTotalTokens = 21;
        above.maximumInvocationTime = Duration.ofSeconds(21);
        above.maximumReasoning = ModelReasoningRequirement.EXTENDED;
        above.maximumClassification = ModelDataClassification.RESTRICTED;
        assertSuitable(above);

        Scenario reasoning = new Scenario();
        reasoning.requiredReasoning = ModelReasoningRequirement.EXTENDED;
        reasoning.maximumReasoning = ModelReasoningRequirement.STANDARD;
        assertRefused(reasoning,
                RemoteModelCandidateSuitabilityRefusalReason.REASONING_REQUIREMENT_UNSUPPORTED);

        Scenario classification = new Scenario();
        classification.effectiveClassification = ModelDataClassification.RESTRICTED;
        classification.maximumClassification = ModelDataClassification.CONFIDENTIAL;
        assertRefused(classification,
                RemoteModelCandidateSuitabilityRefusalReason.DATA_CLASSIFICATION_UNSUPPORTED);

        Scenario zeroCost = new Scenario();
        zeroCost.profileMaximumCost = 0;
        zeroCost.capabilityMaximumCost = 0;
        assertSuitable(zeroCost);

        Scenario exactNonzeroCost = new Scenario();
        exactNonzeroCost.pricingWorstCaseCost = 1;
        exactNonzeroCost.capabilityMaximumCost = 1;
        exactNonzeroCost.profileMaximumCost = 1;
        assertSuitable(exactNonzeroCost);

        Scenario understated = new Scenario();
        understated.pricingWorstCaseCost = 2;
        understated.capabilityMaximumCost = 1;
        assertRefused(understated,
                RemoteModelCandidateSuitabilityRefusalReason.COST_CEILING_EXCEEDED);
    }

    private void assertSuitable(Scenario scenario) {
        Inputs inputs = scenario.inputs();
        assertInstanceOf(
                RemoteModelCandidateSuitabilityDecision.Suitable.class,
                evaluator.evaluate(
                        inputs.profiledRequest(),
                        inputs.eligibility(),
                        inputs.capabilityFacts(),
                        scenario.evaluationTime));
    }

    private void assertRefused(
            Scenario scenario,
            RemoteModelCandidateSuitabilityRefusalReason expected) {
        Inputs inputs = scenario.inputs();
        RemoteModelCandidateSuitabilityDecision.Refused refused = assertInstanceOf(
                RemoteModelCandidateSuitabilityDecision.Refused.class,
                evaluator.evaluate(
                        inputs.profiledRequest(),
                        inputs.eligibility(),
                        inputs.capabilityFacts(),
                        scenario.evaluationTime));
        assertEquals(expected, refused.reason(), expected.name());
    }

    private record Inputs(
            ProfiledModelRequest profiledRequest,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            RemoteModelCandidateCapabilityFacts capabilityFacts) {
    }

    private static final class Scenario {
        private Instant evaluationTime = EVALUATION_TIME;
        private String requestPrompt = "remote-suitability-secret";
        private String contentSourcePrompt = requestPrompt;
        private String candidateSchema = ModelRouteCandidateFacts.SCHEMA_VERSION;
        private Instant candidateObservedAt = OBSERVED_AT;
        private Instant candidateExpiresAt = EXPIRES_AT;
        private String capabilitySchema = RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION;
        private Instant capabilityObservedAt = OBSERVED_AT;
        private Instant capabilityExpiresAt = EXPIRES_AT;
        private String capabilityCandidate = "candidate-a";
        private String capabilityDestination = "destination-a";
        private String profileModelClass = "reasoning-standard";
        private String profileCapability = "repository-analysis";
        private String pricingSchema = ModelRoutePricingFacts.SCHEMA_VERSION;
        private Instant pricingObservedAt = OBSERVED_AT;
        private Instant pricingExpiresAt = EXPIRES_AT;
        private boolean pricingPresent = true;
        private String pricingCandidate = "candidate-a";
        private String pricingDestination = "destination-a";
        private String pricingModelClass = "reasoning-standard";
        private String pricingSource = "pricing-source";
        private String capabilityTokenSemantics =
                ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY;
        private String capabilitySchedule = "schedule-a";
        private ModelTokenBudget capabilityCostBasis;
        private ModelReasoningRequirement requiredReasoning =
                ModelReasoningRequirement.STANDARD;
        private ModelReasoningRequirement maximumReasoning =
                ModelReasoningRequirement.STANDARD;
        private long maximumContextTokens = 20;
        private long maximumInputTokens = 10;
        private long maximumOutputTokens = 5;
        private long maximumTotalTokens = 20;
        private Duration maximumInvocationTime = Duration.ofSeconds(20);
        private ModelDataClassification effectiveClassification =
                ModelDataClassification.INTERNAL;
        private ModelDataClassification maximumClassification =
                ModelDataClassification.INTERNAL;
        private String capabilityCurrency = "USD";
        private long pricingWorstCaseCost = 0;
        private long capabilityMaximumCost = 1_000;
        private long profileMaximumCost = 1_000;

        private Scenario breakFor(RemoteModelCandidateSuitabilityRefusalReason reason) {
            switch (reason) {
                case SOURCE_CONTENT_BINDING_MISMATCH ->
                        contentSourcePrompt = "different-source";
                case CANDIDATE_FACTS_INVALID_OR_STALE -> candidateSchema = "unknown-v1";
                case CAPABILITY_FACTS_INVALID_OR_STALE -> capabilitySchema = "unknown-v1";
                case CANDIDATE_BINDING_MISMATCH -> capabilityCandidate = "candidate-b";
                case DESTINATION_BINDING_MISMATCH -> capabilityDestination = "destination-b";
                case PROFILE_BINDING_MISMATCH -> profileModelClass = "different-model";
                case PRICING_FACTS_INVALID_OR_STALE -> pricingSchema = "unknown-v1";
                case PRICING_BINDING_MISMATCH -> capabilitySchedule = "schedule-b";
                case TOKEN_SEMANTICS_MISMATCH ->
                        capabilityTokenSemantics = "different-token-semantics";
                case REASONING_REQUIREMENT_UNSUPPORTED ->
                        maximumReasoning = ModelReasoningRequirement.MINIMAL;
                case CONTEXT_CAPACITY_INSUFFICIENT -> maximumContextTokens = 19;
                case INPUT_TOKEN_CAPACITY_INSUFFICIENT -> maximumInputTokens = 9;
                case OUTPUT_TOKEN_CAPACITY_INSUFFICIENT -> maximumOutputTokens = 4;
                case TOTAL_TOKEN_CAPACITY_INSUFFICIENT -> maximumTotalTokens = 19;
                case INVOCATION_TIME_UNSUPPORTED ->
                        maximumInvocationTime = Duration.ofMillis(19_999);
                case DATA_CLASSIFICATION_UNSUPPORTED ->
                        maximumClassification = ModelDataClassification.PUBLIC;
                case COST_CURRENCY_MISMATCH -> capabilityCurrency = "EUR";
                case COST_CEILING_EXCEEDED -> capabilityMaximumCost = 1_001;
            }
            return this;
        }

        private Scenario withPricingCandidate(String value) {
            pricingCandidate = value;
            return this;
        }

        private Scenario withPricingDestination(String value) {
            pricingDestination = value;
            return this;
        }

        private Scenario withPricingModelClass(String value) {
            pricingModelClass = value;
            return this;
        }

        private Scenario withPricingSource(String value) {
            pricingSource = value;
            return this;
        }

        private Scenario withCapabilitySchedule(String value) {
            capabilitySchedule = value;
            return this;
        }

        private Scenario withCapabilityCostBasis(ModelTokenBudget value) {
            capabilityCostBasis = value;
            return this;
        }

        private Inputs inputs() {
            ModelTokenBudget requiredBudget = new ModelTokenBudget(10, 5, 20);
            ProfiledModelRequest profiledRequest = new ProfiledModelRequest(
                    new ModelRequest(
                            "suitability-correlation",
                            requestPrompt,
                            profileModelClass,
                            Duration.ofSeconds(30),
                            4_096),
                    new ModelExecutionProfile(
                            ModelExecutionProfile.SCHEMA_VERSION,
                            profileCapability,
                            profileModelClass,
                            ModelLocalityRequirement.POLICY_CONSTRAINED,
                            requiredReasoning,
                            20,
                            requiredBudget,
                            new ModelCostBudget("USD", profileMaximumCost),
                            Duration.ofSeconds(20),
                            ModelDataClassification.INTERNAL));
            ModelRouteCandidateFacts candidate = ModelRouteCandidateFacts.create(
                    candidateSchema,
                    "candidate-a",
                    "candidate-source",
                    "candidate-r1",
                    candidateObservedAt,
                    candidateExpiresAt,
                    "reasoning-standard",
                    "repository-analysis",
                    "destination-a");
            ModelOutboundContent content = ModelOutboundContent.unchanged(contentSourcePrompt);
            ModelContentInspectionFacts inspection = ModelContentInspectionFacts.create(
                    ModelContentInspectionFacts.SCHEMA_VERSION,
                    "scanner-a",
                    "scanner-r1",
                    "ruleset-a",
                    "ruleset-r1",
                    OBSERVED_AT,
                    EXPIRES_AT,
                    content.sourcePromptDigest(),
                    effectiveClassification,
                    ModelInspectionCoverage.COMPLETE,
                    ModelContentInspectionStatus.CLEAR,
                    Set.of());
            ModelDestinationAllowRule rule = ModelDestinationAllowRule.create(
                    "candidate-a",
                    "destination-a",
                    Set.of(ModelOutboundPurpose.MODEL_INFERENCE),
                    Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                    Duration.ofMinutes(1),
                    ModelDataClassification.RESTRICTED,
                    "pricing-source");
            Optional<ModelRoutePricingFacts> pricing = pricingPresent
                    ? Optional.of(ModelRoutePricingFacts.create(
                            pricingSchema,
                            pricingSource,
                            "pricing-r1",
                            pricingObservedAt,
                            pricingExpiresAt,
                            pricingCandidate,
                            pricingDestination,
                            pricingModelClass,
                            ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                            "schedule-a",
                            requiredBudget,
                            "USD",
                            pricingWorstCaseCost))
                    : Optional.empty();
            ModelOutboundRouteProposal proposal = new ModelOutboundRouteProposal(
                    ModelOutboundRouteProposal.SCHEMA_VERSION,
                    candidate,
                    ModelDestinationPolicyFacts.create(
                            ModelDestinationPolicyFacts.SCHEMA_VERSION,
                            "destination-a",
                            "outbound-policy-source",
                            "policy-r1",
                            OBSERVED_AT,
                            EXPIRES_AT,
                            Set.of(rule)),
                    ModelOutboundPurpose.MODEL_INFERENCE,
                    new ModelRetentionFacts(
                            Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                            Duration.ofSeconds(30)),
                    content,
                    Optional.of(inspection),
                    Optional.of(inspection),
                    Optional.empty(),
                    pricing);
            ModelOutboundRouteAdmissionDecision.Eligible eligibility =
                    new ModelOutboundRouteAdmissionDecision.Eligible(
                            proposal, effectiveClassification);
            RemoteModelCandidateCapabilityFacts capabilityFacts =
                    RemoteModelCandidateCapabilityFacts.create(
                            capabilitySchema,
                            "capability-source",
                            "capability-r1",
                            "capacity-evidence-a",
                            capabilityObservedAt,
                            capabilityExpiresAt,
                            capabilityCandidate,
                            capabilityDestination,
                            "reasoning-standard",
                            "repository-analysis",
                            capabilityTokenSemantics,
                            capabilitySchedule,
                            capabilityCostBasis == null ? requiredBudget : capabilityCostBasis,
                            maximumReasoning,
                            maximumContextTokens,
                            new ModelTokenBudget(
                                    maximumInputTokens,
                                    maximumOutputTokens,
                                    maximumTotalTokens),
                            maximumInvocationTime,
                            capabilityCurrency,
                            capabilityMaximumCost,
                            maximumClassification);
            return new Inputs(profiledRequest, eligibility, capabilityFacts);
        }
    }
}
