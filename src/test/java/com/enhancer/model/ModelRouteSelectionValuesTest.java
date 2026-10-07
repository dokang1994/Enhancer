package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelRouteSelectionValuesTest {

    private static final String CAPABILITY = "repository-analysis";
    private static final String MODEL_CLASS = "reasoning-standard";
    private static final String CANDIDATE = "candidate-a";
    private static final String DESTINATION = "destination-a";
    private static final String SECRET_PROMPT = "never-render-router-secret";
    private static final Instant OBSERVED_AT = Instant.parse("2026-10-07T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-07T00:05:00Z");

    @Test
    void vocabulariesAndDecisionShapesAreClosedInRfcOrder() {
        assertArrayEquals(
                new ModelRouteSelectionRefusalReason[] {
                        ModelRouteSelectionRefusalReason.NO_ROUTE_OPTIONS,
                        ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH,
                        ModelRouteSelectionRefusalReason.SOURCE_CONTENT_BINDING_MISMATCH,
                        ModelRouteSelectionRefusalReason.PROFILE_BINDING_MISMATCH,
                        ModelRouteSelectionRefusalReason.CANDIDATE_BINDING_MISMATCH,
                        ModelRouteSelectionRefusalReason.DESTINATION_BINDING_MISMATCH,
                        ModelRouteSelectionRefusalReason.ELIGIBILITY_INVALID_OR_STALE,
                        ModelRouteSelectionRefusalReason.SUITABILITY_INVALID_OR_STALE,
                        ModelRouteSelectionRefusalReason.DUPLICATE_ROUTE_IDENTITY
                },
                ModelRouteSelectionRefusalReason.values());
        assertTrue(ModelRouteSelectionDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        ModelRouteSelectionDecision.Selected.class,
                        ModelRouteSelectionDecision.Refused.class
                },
                ModelRouteSelectionDecision.class.getPermittedSubclasses());
        assertTrue(ModelRouteSelectionDecision.Selected.class.isRecord());
        assertTrue(ModelRouteSelectionDecision.Refused.class.isRecord());
    }

    @Test
    void suitabilityAndOptionRetainExactAuthorityAndFactInstances() {
        Fixture fixture = new Fixture();

        assertSame(fixture.profiledRequest, fixture.suitability.profiledRequest());
        assertSame(fixture.eligibility, fixture.suitability.eligibility());
        assertSame(fixture.candidate, fixture.suitability.candidateFacts());
        assertSame(fixture.profiledRequest, fixture.option.profiledRequest());
        assertSame(fixture.eligibility, fixture.option.eligibility());
        assertSame(fixture.suitability, fixture.option.suitability());
        assertEquals(CANDIDATE, fixture.suitability.candidateIdentity());
        assertEquals(DESTINATION, fixture.suitability.destinationIdentity());
        assertEquals(new ModelTokenBudget(100, 50, 150),
                fixture.suitability.maximumTokenBudget());
    }

    @Test
    void optionSetDefensivelyCopiesZeroOneAndTwoHundredFiftySixOrderedOptions() {
        Fixture fixture = new Fixture();
        assertTrue(new ModelRouteOptionSet(List.of()).options().isEmpty());

        ArrayList<ModelRouteOption> mutable = new ArrayList<>();
        mutable.add(fixture.option);
        ModelRouteOptionSet one = new ModelRouteOptionSet(mutable);
        mutable.clear();
        assertEquals(List.of(fixture.option), one.options());
        assertThrows(UnsupportedOperationException.class,
                () -> one.options().add(fixture.option));

        List<ModelRouteOption> maximum =
                new ArrayList<>(Collections.nCopies(256, fixture.option));
        ModelRouteOptionSet bounded = new ModelRouteOptionSet(maximum);
        assertEquals(256, bounded.options().size());
        for (int ordinal = 0; ordinal < bounded.options().size(); ordinal++) {
            assertSame(fixture.option, bounded.options().get(ordinal));
        }
    }

    @Test
    void optionSetRejectsNullAndMoreThanTwoHundredFiftySixOptions() {
        Fixture fixture = new Fixture();
        assertThrows(NullPointerException.class, () -> new ModelRouteOptionSet(null));
        assertThrows(NullPointerException.class,
                () -> new ModelRouteOptionSet(java.util.Arrays.asList(fixture.option, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelRouteOptionSet(
                        new ArrayList<>(Collections.nCopies(257, fixture.option))));
    }

    @Test
    void decisionOrdinalShapeAndOpaqueSelectedRouteConstructionFailClosed() {
        ModelRouteSelectionDecision.Refused empty =
                new ModelRouteSelectionDecision.Refused(
                        ModelRouteSelectionRefusalReason.NO_ROUTE_OPTIONS, -1);
        ModelRouteSelectionDecision.Refused mismatch =
                new ModelRouteSelectionDecision.Refused(
                        ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH, 0);

        assertEquals(-1, empty.optionOrdinal());
        assertEquals(0, mismatch.optionOrdinal());
        assertThrows(IllegalArgumentException.class,
                () -> new ModelRouteSelectionDecision.Refused(
                        ModelRouteSelectionRefusalReason.NO_ROUTE_OPTIONS, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelRouteSelectionDecision.Refused(
                        ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelRouteSelectionDecision.Refused(
                        ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH, 256));

        assertTrue(Modifier.isFinal(SelectedModelRoute.class.getModifiers()));
        for (Constructor<?> constructor : SelectedModelRoute.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        }
    }

    @Test
    void valuesRejectNullShapeAndNeverRenderSensitiveOrAuthorityBearingData() {
        Fixture fixture = new Fixture();
        assertThrows(NullPointerException.class,
                () -> new ModelRouteOption(null, fixture.eligibility, fixture.suitability));
        assertThrows(NullPointerException.class,
                () -> new ModelRouteOption(
                        fixture.profiledRequest, null, fixture.suitability));
        assertThrows(NullPointerException.class,
                () -> new ModelRouteOption(
                        fixture.profiledRequest, fixture.eligibility, null));
        assertThrows(NullPointerException.class,
                () -> new ModelRouteSelectionDecision.Selected(null));
        assertThrows(NullPointerException.class,
                () -> new ModelRouteSelectionDecision.Refused(null, 0));

        for (Object value : List.of(
                fixture.suitability,
                fixture.option,
                new ModelRouteOptionSet(List.of(fixture.option)),
                new ModelRouteSelectionDecision.Refused(
                        ModelRouteSelectionRefusalReason.REQUEST_BINDING_MISMATCH, 0))) {
            String rendered = value.toString();
            assertFalse(rendered.contains(SECRET_PROMPT));
            assertFalse(rendered.contains("987654321"));
            assertFalse(rendered.contains("https://provider.invalid"));
            assertFalse(rendered.contains("credential-sentinel"));
        }
    }

    private static final class Fixture {
        private final ProfiledModelRequest profiledRequest = profiledRequest();
        private final ModelRouteCandidateFacts candidate = candidateFacts();
        private final ModelOutboundRouteAdmissionDecision.Eligible eligibility =
                new ModelOutboundRouteAdmissionDecision.Eligible(
                        proposal(candidate), ModelDataClassification.INTERNAL);
        private final RemoteModelCandidateSuitability suitability =
                RemoteModelCandidateSuitability.create(
                        RemoteModelCandidateSuitability.CONTRACT_VERSION,
                        profiledRequest,
                        eligibility,
                        candidate,
                        "remote-suitability-source",
                        "suitability-r1",
                        OBSERVED_AT,
                        EXPIRES_AT,
                        CANDIDATE,
                        DESTINATION,
                        "profile-token-budget-v1",
                        "capacity-evidence-a",
                        MODEL_CLASS,
                        CAPABILITY,
                        ModelReasoningRequirement.EXTENDED,
                        1_000,
                        new ModelTokenBudget(100, 50, 150),
                        Duration.ofSeconds(60),
                        "USD",
                        987_654_321,
                        ModelDataClassification.RESTRICTED);
        private final ModelRouteOption option =
                new ModelRouteOption(profiledRequest, eligibility, suitability);
    }

    private static ProfiledModelRequest profiledRequest() {
        return new ProfiledModelRequest(
                new ModelRequest(
                        "router-correlation",
                        SECRET_PROMPT,
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
                        new ModelCostBudget("USD", 1_000_000_000),
                        Duration.ofSeconds(20),
                        ModelDataClassification.INTERNAL));
    }

    private static ModelRouteCandidateFacts candidateFacts() {
        return ModelRouteCandidateFacts.create(
                ModelRouteCandidateFacts.SCHEMA_VERSION,
                CANDIDATE,
                "candidate-source",
                "candidate-r1",
                OBSERVED_AT,
                EXPIRES_AT,
                MODEL_CLASS,
                CAPABILITY,
                DESTINATION);
    }

    private static ModelOutboundRouteProposal proposal(ModelRouteCandidateFacts candidate) {
        ModelOutboundContent content = ModelOutboundContent.unchanged(SECRET_PROMPT);
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
        return new ModelOutboundRouteProposal(
                ModelOutboundRouteProposal.SCHEMA_VERSION,
                candidate,
                ModelDestinationPolicyFacts.create(
                        ModelDestinationPolicyFacts.SCHEMA_VERSION,
                        DESTINATION,
                        "outbound-policy-source",
                        "policy-r1",
                        OBSERVED_AT,
                        EXPIRES_AT,
                        Set.of(ModelDestinationAllowRule.create(
                                CANDIDATE,
                                DESTINATION,
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
                        EXPIRES_AT,
                        CANDIDATE,
                        DESTINATION,
                        MODEL_CLASS,
                        ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                        "schedule-a",
                        new ModelTokenBudget(10, 5, 15),
                        "USD",
                        0)));
    }
}
