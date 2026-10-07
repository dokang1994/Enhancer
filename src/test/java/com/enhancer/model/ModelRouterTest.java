package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelRouterTest {

    private static final String CAPABILITY = "repository-analysis";
    private static final String MODEL_CLASS = "reasoning-standard";
    private static final String PROMPT = "Route this exact approved prompt.";
    private static final Instant OBSERVED_AT = Instant.parse("2026-10-07T00:00:00Z");
    private static final Instant EVALUATION_TIME = Instant.parse("2026-10-07T00:01:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-07T00:05:00Z");

    @Test
    void rejectsNullProgrammingInputsAndEmptyOptions() {
        Fixture fixture = new Fixture("candidate-a", "destination-a");
        ModelRouter router = new ModelRouter();

        assertThrows(NullPointerException.class,
                () -> router.evaluate(null, new ModelRouteOptionSet(List.of()), EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> router.evaluate(fixture.request, null, EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> router.evaluate(
                        fixture.request, new ModelRouteOptionSet(List.of()), null));
        assertRefused(
                ModelRouteSelectionRefusalReason.NO_ROUTE_OPTIONS,
                -1,
                router.evaluate(
                        fixture.request,
                        new ModelRouteOptionSet(List.of()),
                        EVALUATION_TIME));
    }

    @Test
    void refusesEveryRequestSourceProfileCandidateAndDestinationMismatch() {
        Fixture fixture = new Fixture("candidate-a", "destination-a");
        ProfiledModelRequest equalButDistinct = new ProfiledModelRequest(
                fixture.request.request(), fixture.request.executionProfile());
        ModelRouteOption requestMismatch = new ModelRouteOption(
                equalButDistinct,
                fixture.eligibility,
                fixture.suitability(
                        RemoteModelCandidateSuitability.CONTRACT_VERSION,
                        equalButDistinct,
                        fixture.eligibility,
                        fixture.candidate,
                        EXPIRES_AT,
                        fixture.candidateIdentity,
                        fixture.destinationIdentity,
                        MODEL_CLASS,
                        CAPABILITY,
                        ModelReasoningRequirement.EXTENDED,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        0,
                        ModelDataClassification.RESTRICTED));
        assertRefused(
                ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH,
                0,
                evaluate(fixture.request, requestMismatch, EVALUATION_TIME));

        ProfiledModelRequest differentPrompt = profiledRequest("different exact prompt");
        ModelRouteOption sourceMismatch = new ModelRouteOption(
                differentPrompt,
                fixture.eligibility,
                fixture.suitability(
                        RemoteModelCandidateSuitability.CONTRACT_VERSION,
                        differentPrompt,
                        fixture.eligibility,
                        fixture.candidate,
                        EXPIRES_AT,
                        fixture.candidateIdentity,
                        fixture.destinationIdentity,
                        MODEL_CLASS,
                        CAPABILITY,
                        ModelReasoningRequirement.EXTENDED,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        0,
                        ModelDataClassification.RESTRICTED));
        assertRefused(
                ModelRouteSelectionRefusalReason.SOURCE_CONTENT_BINDING_MISMATCH,
                0,
                evaluate(differentPrompt, sourceMismatch, EVALUATION_TIME));

        assertRefused(
                ModelRouteSelectionRefusalReason.PROFILE_BINDING_MISMATCH,
                0,
                evaluate(
                        fixture.request,
                        fixture.option(fixture.suitabilityWithModelClass("reasoning-other")),
                        EVALUATION_TIME));

        ModelOutboundRouteAdmissionDecision.Eligible equalEligibility =
                new ModelOutboundRouteAdmissionDecision.Eligible(
                        fixture.proposal, ModelDataClassification.INTERNAL);
        assertRefused(
                ModelRouteSelectionRefusalReason.CANDIDATE_BINDING_MISMATCH,
                0,
                evaluate(
                        fixture.request,
                        fixture.option(fixture.suitability(
                                RemoteModelCandidateSuitability.CONTRACT_VERSION,
                                fixture.request,
                                equalEligibility,
                                fixture.candidate,
                                EXPIRES_AT,
                                fixture.candidateIdentity,
                                fixture.destinationIdentity,
                                MODEL_CLASS,
                                CAPABILITY,
                                ModelReasoningRequirement.EXTENDED,
                                1_000,
                                new ModelTokenBudget(100, 50, 150),
                                Duration.ofSeconds(60),
                                "USD",
                                0,
                                ModelDataClassification.RESTRICTED)),
                        EVALUATION_TIME));

        assertRefused(
                ModelRouteSelectionRefusalReason.DESTINATION_BINDING_MISMATCH,
                0,
                evaluate(
                        fixture.request,
                        fixture.option(fixture.suitabilityWithDestination("destination-b")),
                        EVALUATION_TIME));
    }

    @Test
    void refusesEqualButNotIdenticalCandidateFacts() {
        Fixture fixture = new Fixture("candidate-a", "destination-a");
        ModelRouteCandidateFacts equalCandidate = candidateFacts(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                fixture.candidateIdentity,
                fixture.destinationIdentity,
                EXPIRES_AT);

        assertEquals(fixture.candidate, equalCandidate);
        assertRefused(
                ModelRouteSelectionRefusalReason.CANDIDATE_BINDING_MISMATCH,
                0,
                evaluate(
                        fixture.request,
                        fixture.option(fixture.suitability(
                                RemoteModelCandidateSuitability.CONTRACT_VERSION,
                                fixture.request,
                                fixture.eligibility,
                                equalCandidate,
                                EXPIRES_AT,
                                fixture.candidateIdentity,
                                fixture.destinationIdentity,
                                MODEL_CLASS,
                                CAPABILITY,
                                ModelReasoningRequirement.EXTENDED,
                                1_000,
                                new ModelTokenBudget(100, 50, 150),
                                Duration.ofSeconds(60),
                                "USD",
                                0,
                                ModelDataClassification.RESTRICTED)),
                        EVALUATION_TIME));
    }

    @Test
    void refusesInvalidOrStaleEligibilityBeforeInvalidOrStaleSuitability() {
        Fixture unsupportedCandidate = new Fixture(
                "candidate-a",
                "destination-a",
                "model-route-candidate-facts-v2",
                EXPIRES_AT);
        assertRefused(
                ModelRouteSelectionRefusalReason.ELIGIBILITY_INVALID_OR_STALE,
                0,
                evaluate(
                        unsupportedCandidate.request,
                        unsupportedCandidate.option(),
                        EVALUATION_TIME));

        Fixture fixture = new Fixture("candidate-a", "destination-a");
        assertRefused(
                ModelRouteSelectionRefusalReason.ELIGIBILITY_INVALID_OR_STALE,
                0,
                evaluate(fixture.request, fixture.option(), EXPIRES_AT));

        RemoteModelCandidateSuitability staleAtEvaluation = fixture.suitability(
                RemoteModelCandidateSuitability.CONTRACT_VERSION,
                fixture.request,
                fixture.eligibility,
                fixture.candidate,
                EVALUATION_TIME,
                fixture.candidateIdentity,
                fixture.destinationIdentity,
                MODEL_CLASS,
                CAPABILITY,
                ModelReasoningRequirement.EXTENDED,
                1_000,
                new ModelTokenBudget(100, 50, 150),
                Duration.ofSeconds(60),
                "USD",
                0,
                ModelDataClassification.RESTRICTED);
        assertRefused(
                ModelRouteSelectionRefusalReason.SUITABILITY_INVALID_OR_STALE,
                0,
                evaluate(
                        fixture.request,
                        fixture.option(staleAtEvaluation),
                        EVALUATION_TIME));

        assertRefused(
                ModelRouteSelectionRefusalReason.SUITABILITY_INVALID_OR_STALE,
                0,
                evaluate(
                        fixture.request,
                        fixture.option(fixture.suitabilityWithVersion(
                                "remote-model-candidate-suitability-v2")),
                        EVALUATION_TIME));
    }

    @Test
    void refusesSuitabilityThatCannotSatisfyTheExactProfile() {
        Fixture fixture = new Fixture("candidate-a", "destination-a");
        RemoteModelCandidateSuitability insufficient = fixture.suitability(
                RemoteModelCandidateSuitability.CONTRACT_VERSION,
                fixture.request,
                fixture.eligibility,
                fixture.candidate,
                EXPIRES_AT,
                fixture.candidateIdentity,
                fixture.destinationIdentity,
                MODEL_CLASS,
                CAPABILITY,
                ModelReasoningRequirement.MINIMAL,
                14,
                new ModelTokenBudget(9, 5, 14),
                Duration.ofSeconds(19),
                "EUR",
                1,
                ModelDataClassification.PUBLIC);

        assertRefused(
                ModelRouteSelectionRefusalReason.SUITABILITY_INVALID_OR_STALE,
                0,
                evaluate(fixture.request, fixture.option(insufficient), EVALUATION_TIME));
    }

    @Test
    void validatesTheWholeSetInReasonOrderAndLowestOrdinalBeforeSelecting() {
        Fixture fixture = new Fixture("candidate-a", "destination-a");
        RemoteModelCandidateSuitability invalidSuitability =
                fixture.suitabilityWithVersion("remote-model-candidate-suitability-v2");
        ProfiledModelRequest equalButDistinct = new ProfiledModelRequest(
                fixture.request.request(), fixture.request.executionProfile());
        ModelRouteOption laterRequestMismatch = new ModelRouteOption(
                equalButDistinct,
                fixture.eligibility,
                fixture.suitability(
                        RemoteModelCandidateSuitability.CONTRACT_VERSION,
                        equalButDistinct,
                        fixture.eligibility,
                        fixture.candidate,
                        EXPIRES_AT,
                        fixture.candidateIdentity,
                        fixture.destinationIdentity,
                        MODEL_CLASS,
                        CAPABILITY,
                        ModelReasoningRequirement.EXTENDED,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        0,
                        ModelDataClassification.RESTRICTED));

        assertRefused(
                ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH,
                1,
                new ModelRouter().evaluate(
                        fixture.request,
                        new ModelRouteOptionSet(List.of(
                                fixture.option(invalidSuitability),
                                laterRequestMismatch)),
                        EVALUATION_TIME));

        assertRefused(
                ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH,
                1,
                new ModelRouter().evaluate(
                        fixture.request,
                        new ModelRouteOptionSet(List.of(
                                fixture.option(), laterRequestMismatch, laterRequestMismatch)),
                        EVALUATION_TIME));
    }

    @Test
    void refusesAdjacentNonAdjacentAndMaximumSizedDuplicateRoutes() {
        Fixture first = new Fixture("candidate-a", "destination-a");
        Fixture middle = new Fixture("candidate-b", "destination-b");

        assertRefused(
                ModelRouteSelectionRefusalReason.DUPLICATE_ROUTE_IDENTITY,
                1,
                new ModelRouter().evaluate(
                        first.request,
                        new ModelRouteOptionSet(List.of(first.option(), first.option())),
                        EVALUATION_TIME));
        assertRefused(
                ModelRouteSelectionRefusalReason.DUPLICATE_ROUTE_IDENTITY,
                2,
                new ModelRouter().evaluate(
                        first.request,
                        new ModelRouteOptionSet(List.of(
                                first.option(), middle.optionFor(first.request), first.option())),
                        EVALUATION_TIME));

        List<ModelRouteOption> maximum =
                new ArrayList<>(Collections.nCopies(256, first.option()));
        assertRefused(
                ModelRouteSelectionRefusalReason.DUPLICATE_ROUTE_IDENTITY,
                1,
                new ModelRouter().evaluate(
                        first.request,
                        new ModelRouteOptionSet(maximum),
                        EVALUATION_TIME));
    }

    @Test
    void observationIsInclusiveAndSuccessfulSelectionRetainsEveryExactIdentity() {
        Fixture fixture = new Fixture("candidate-a", "destination-a");
        ModelRouteOption option = fixture.option();
        ModelRouteSelectionDecision.Selected selected = assertInstanceOf(
                ModelRouteSelectionDecision.Selected.class,
                evaluate(fixture.request, option, OBSERVED_AT));
        SelectedModelRoute route = selected.selectedRoute();

        assertSame(fixture.request, route.profiledRequest());
        assertSame(option, route.selectedOption());
        assertSame(fixture.eligibility, route.eligibility());
        assertSame(option.suitability(), route.suitability());
        assertSame(fixture.candidate, route.candidateFacts());
        assertSame(fixture.proposal.destinationPolicyFacts(), route.destinationFacts());
        assertEquals(0, route.optionOrdinal());
        assertEquals(SelectedModelRoute.CONTRACT_VERSION, route.contractVersion());
        assertFalse(route.toString().contains(PROMPT));
        assertFalse(selected.toString().contains(PROMPT));
    }

    private static ModelRouteSelectionDecision evaluate(
            ProfiledModelRequest request,
            ModelRouteOption option,
            Instant evaluationTime) {
        return new ModelRouter().evaluate(
                request, new ModelRouteOptionSet(List.of(option)), evaluationTime);
    }

    private static void assertRefused(
            ModelRouteSelectionRefusalReason reason,
            int ordinal,
            ModelRouteSelectionDecision decision) {
        ModelRouteSelectionDecision.Refused refused = assertInstanceOf(
                ModelRouteSelectionDecision.Refused.class, decision);
        assertEquals(reason, refused.reason());
        assertEquals(ordinal, refused.optionOrdinal());
    }

    private static final class Fixture {
        private final String candidateIdentity;
        private final String destinationIdentity;
        private final ProfiledModelRequest request;
        private final ModelRouteCandidateFacts candidate;
        private final ModelOutboundRouteProposal proposal;
        private final ModelOutboundRouteAdmissionDecision.Eligible eligibility;

        private Fixture(String candidateIdentity, String destinationIdentity) {
            this(
                    candidateIdentity,
                    destinationIdentity,
                    ModelRouteCandidateFacts.SCHEMA_VERSION,
                    EXPIRES_AT);
        }

        private Fixture(
                String candidateIdentity,
                String destinationIdentity,
                String candidateSchemaVersion,
                Instant factsExpireAt) {
            this.candidateIdentity = candidateIdentity;
            this.destinationIdentity = destinationIdentity;
            this.request = profiledRequest(PROMPT);
            this.candidate = candidateFacts(
                    candidateSchemaVersion,
                    candidateIdentity,
                    destinationIdentity,
                    factsExpireAt);
            this.proposal = proposal(
                    candidate,
                    candidateIdentity,
                    destinationIdentity,
                    factsExpireAt,
                    PROMPT);
            this.eligibility = new ModelOutboundRouteAdmissionDecision.Eligible(
                    proposal, ModelDataClassification.INTERNAL);
        }

        private ModelRouteOption option() {
            return option(suitabilityWithVersion(
                    RemoteModelCandidateSuitability.CONTRACT_VERSION));
        }

        private ModelRouteOption optionFor(ProfiledModelRequest exactRequest) {
            return new ModelRouteOption(
                    exactRequest,
                    eligibility,
                    suitability(
                            RemoteModelCandidateSuitability.CONTRACT_VERSION,
                            exactRequest,
                            eligibility,
                            candidate,
                            EXPIRES_AT,
                            candidateIdentity,
                            destinationIdentity,
                            MODEL_CLASS,
                            CAPABILITY,
                            ModelReasoningRequirement.EXTENDED,
                            1_000,
                            new ModelTokenBudget(100, 50, 150),
                            Duration.ofSeconds(60),
                            "USD",
                            0,
                            ModelDataClassification.RESTRICTED));
        }

        private ModelRouteOption option(RemoteModelCandidateSuitability exactSuitability) {
            return new ModelRouteOption(request, eligibility, exactSuitability);
        }

        private RemoteModelCandidateSuitability suitabilityWithVersion(String version) {
            return suitability(
                    version,
                    request,
                    eligibility,
                    candidate,
                    EXPIRES_AT,
                    candidateIdentity,
                    destinationIdentity,
                    MODEL_CLASS,
                    CAPABILITY,
                    ModelReasoningRequirement.EXTENDED,
                    1_000,
                    new ModelTokenBudget(100, 50, 150),
                    Duration.ofSeconds(60),
                    "USD",
                    0,
                    ModelDataClassification.RESTRICTED);
        }

        private RemoteModelCandidateSuitability suitabilityWithModelClass(
                String suitabilityModelClass) {
            return suitability(
                    RemoteModelCandidateSuitability.CONTRACT_VERSION,
                    request,
                    eligibility,
                    candidate,
                    EXPIRES_AT,
                    candidateIdentity,
                    destinationIdentity,
                    suitabilityModelClass,
                    CAPABILITY,
                    ModelReasoningRequirement.EXTENDED,
                    1_000,
                    new ModelTokenBudget(100, 50, 150),
                    Duration.ofSeconds(60),
                    "USD",
                    0,
                    ModelDataClassification.RESTRICTED);
        }

        private RemoteModelCandidateSuitability suitabilityWithDestination(
                String suitabilityDestination) {
            return suitability(
                    RemoteModelCandidateSuitability.CONTRACT_VERSION,
                    request,
                    eligibility,
                    candidate,
                    EXPIRES_AT,
                    candidateIdentity,
                    suitabilityDestination,
                    MODEL_CLASS,
                    CAPABILITY,
                    ModelReasoningRequirement.EXTENDED,
                    1_000,
                    new ModelTokenBudget(100, 50, 150),
                    Duration.ofSeconds(60),
                    "USD",
                    0,
                    ModelDataClassification.RESTRICTED);
        }

        private RemoteModelCandidateSuitability suitability(
                String version,
                ProfiledModelRequest proofRequest,
                ModelOutboundRouteAdmissionDecision.Eligible proofEligibility,
                ModelRouteCandidateFacts proofCandidate,
                Instant proofExpiresAt,
                String proofCandidateIdentity,
                String proofDestinationIdentity,
                String proofModelClass,
                String proofCapability,
                ModelReasoningRequirement maximumReasoning,
                long maximumContext,
                ModelTokenBudget maximumTokens,
                Duration maximumInvocationTime,
                String costCurrency,
                long maximumCost,
                ModelDataClassification maximumClassification) {
            return RemoteModelCandidateSuitability.create(
                    version,
                    proofRequest,
                    proofEligibility,
                    proofCandidate,
                    "remote-suitability-source",
                    "suitability-r1",
                    OBSERVED_AT,
                    proofExpiresAt,
                    proofCandidateIdentity,
                    proofDestinationIdentity,
                    "profile-token-budget-v1",
                    "capacity-evidence-a",
                    proofModelClass,
                    proofCapability,
                    maximumReasoning,
                    maximumContext,
                    maximumTokens,
                    maximumInvocationTime,
                    costCurrency,
                    maximumCost,
                    maximumClassification);
        }
    }

    private static ProfiledModelRequest profiledRequest(String prompt) {
        return new ProfiledModelRequest(
                new ModelRequest(
                        "router-correlation",
                        prompt,
                        MODEL_CLASS,
                        Duration.ofSeconds(30),
                        4096),
                new ModelExecutionProfile(
                        ModelExecutionProfile.SCHEMA_VERSION,
                        CAPABILITY,
                        MODEL_CLASS,
                        ModelLocalityRequirement.POLICY_CONSTRAINED,
                        ModelReasoningRequirement.STANDARD,
                        15,
                        new ModelTokenBudget(10, 5, 15),
                        new ModelCostBudget("USD", 0),
                        Duration.ofSeconds(20),
                        ModelDataClassification.INTERNAL));
    }

    private static ModelRouteCandidateFacts candidateFacts(
            String schemaVersion,
            String candidateIdentity,
            String destinationIdentity,
            Instant expiresAt) {
        return ModelRouteCandidateFacts.create(
                schemaVersion,
                candidateIdentity,
                "candidate-source",
                "candidate-r1",
                OBSERVED_AT,
                expiresAt,
                MODEL_CLASS,
                CAPABILITY,
                destinationIdentity);
    }

    private static ModelOutboundRouteProposal proposal(
            ModelRouteCandidateFacts candidate,
            String candidateIdentity,
            String destinationIdentity,
            Instant expiresAt,
            String prompt) {
        ModelOutboundContent content = ModelOutboundContent.unchanged(prompt);
        ModelContentInspectionFacts inspection = ModelContentInspectionFacts.create(
                ModelContentInspectionFacts.SCHEMA_VERSION,
                "scanner-a",
                "scanner-r1",
                "ruleset-a",
                "ruleset-r1",
                OBSERVED_AT,
                expiresAt,
                content.sourcePromptDigest(),
                ModelDataClassification.INTERNAL,
                ModelInspectionCoverage.COMPLETE,
                ModelContentInspectionStatus.CLEAR,
                Set.of());
        return new ModelOutboundRouteProposal(
                ModelOutboundRouteProposal.SCHEMA_VERSION,
                candidate,
                ModelDestinationPolicyFacts.create(
                        ModelDestinationPolicyFacts.SCHEMA_VERSION,
                        destinationIdentity,
                        "outbound-policy-source",
                        "policy-r1",
                        OBSERVED_AT,
                        expiresAt,
                        Set.of(ModelDestinationAllowRule.create(
                                candidateIdentity,
                                destinationIdentity,
                                Set.of(ModelOutboundPurpose.MODEL_INFERENCE),
                                Set.of(ModelRetentionUse.REQUEST_PROCESSING),
                                Duration.ofMinutes(1),
                                ModelDataClassification.INTERNAL,
                                "pricing-source"))),
                ModelOutboundPurpose.MODEL_INFERENCE,
                new ModelRetentionFacts(
                        Set.of(ModelRetentionUse.REQUEST_PROCESSING), Duration.ofSeconds(30)),
                content,
                Optional.of(inspection),
                Optional.of(inspection),
                Optional.empty(),
                Optional.of(ModelRoutePricingFacts.create(
                        ModelRoutePricingFacts.SCHEMA_VERSION,
                        "pricing-source",
                        "pricing-r1",
                        OBSERVED_AT,
                        expiresAt,
                        candidateIdentity,
                        destinationIdentity,
                        MODEL_CLASS,
                        ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                        "schedule-a",
                        new ModelTokenBudget(10, 5, 15),
                        "USD",
                        0)));
    }
}
