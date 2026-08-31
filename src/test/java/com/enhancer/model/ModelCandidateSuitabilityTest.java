package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ModelCandidateSuitabilityTest {

    private final ModelCandidateSuitability evaluator = new ModelCandidateSuitability();
    private final DeterministicFakeModelCandidate candidate =
            DeterministicFakeModelCandidate.bind(new DeterministicFakeModelGateway());

    @Test
    void rejectsInDeterministicFirstMatchOrder() {
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.MODEL_CLASS_UNSUPPORTED,
                admitted("other-model", "other-capability", ModelReasoningRequirement.EXTENDED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.REQUIRED_CAPABILITY_UNSUPPORTED,
                admitted("deterministic-fake", "other-capability", ModelReasoningRequirement.EXTENDED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.REASONING_REQUIREMENT_UNSUPPORTED,
                admitted("deterministic-fake", "deterministic-echo", ModelReasoningRequirement.STANDARD));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.REASONING_REQUIREMENT_UNSUPPORTED,
                admitted("deterministic-fake", "deterministic-echo", ModelReasoningRequirement.EXTENDED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.TOKEN_SEMANTICS_UNAVAILABLE,
                admitted("deterministic-fake", "deterministic-echo", ModelReasoningRequirement.MINIMAL));
    }

    @Test
    void tokenSemanticsStopPrecedesAllLaterProfilePredicates() {
        ModelInvocationAdmissionDecision.Admitted freePublic = admitted(
                "deterministic-fake",
                "deterministic-echo",
                ModelReasoningRequirement.MINIMAL,
                2,
                new ModelTokenBudget(1, 1, 2),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC);
        ModelInvocationAdmissionDecision.Admitted paidRestricted = admitted(
                "deterministic-fake",
                "deterministic-echo",
                ModelReasoningRequirement.MINIMAL,
                100,
                new ModelTokenBudget(10, 20, 30),
                new ModelCostBudget("KRW", 50_000),
                ModelDataClassification.RESTRICTED);

        assertRejected(
                ModelCandidateSuitabilityRejectionReason.TOKEN_SEMANTICS_UNAVAILABLE,
                freePublic);
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.TOKEN_SEMANTICS_UNAVAILABLE,
                paidRestricted);
    }

    @Test
    void everyCurrentProfilePartitionFailsClosed() {
        for (String modelClass : new String[] {"deterministic-fake", "other-model"}) {
            for (String capability : new String[] {"deterministic-echo", "other-capability"}) {
                for (ModelReasoningRequirement reasoning : ModelReasoningRequirement.values()) {
                    assertFalse(evaluator.evaluate(
                                    admitted(modelClass, capability, reasoning), candidate)
                            instanceof ModelCandidateSuitabilityDecision.Suitable);
                }
            }
        }
    }

    @Test
    void rejectsNullInputsAsProgrammingErrors() {
        ModelInvocationAdmissionDecision.Admitted admitted = admitted(
                "deterministic-fake",
                "deterministic-echo",
                ModelReasoningRequirement.MINIMAL);

        assertThrows(NullPointerException.class, () -> evaluator.evaluate(null, candidate));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(admitted, null));
    }

    @Test
    void exposesOnlyTheExactFieldFreeEvaluationBoundary() throws Exception {
        Class<ModelCandidateSuitability> type = ModelCandidateSuitability.class;
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertEquals(0, type.getDeclaredFields().length);
        assertArrayEquals(new Class<?>[0], type.getInterfaces());

        Method[] publicDeclaredMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toArray(Method[]::new);
        assertEquals(1, publicDeclaredMethods.length);
        Method evaluate = type.getDeclaredMethod(
                "evaluate",
                ModelInvocationAdmissionDecision.Admitted.class,
                DeterministicFakeModelCandidate.class);
        assertEquals(ModelCandidateSuitabilityDecision.class, evaluate.getReturnType());
    }

    @Test
    void sealsTheExactIdentityRetainingDecisionShapesAndReasonOrder() {
        assertTrue(ModelCandidateSuitabilityDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                    ModelCandidateSuitabilityDecision.Suitable.class,
                    ModelCandidateSuitabilityDecision.Rejected.class
                },
                ModelCandidateSuitabilityDecision.class.getPermittedSubclasses());
        assertRecordShape(
                ModelCandidateSuitabilityDecision.Suitable.class,
                new String[] {"admitted", "candidate"},
                new Class<?>[] {
                    ModelInvocationAdmissionDecision.Admitted.class,
                    DeterministicFakeModelCandidate.class
                });
        assertRecordShape(
                ModelCandidateSuitabilityDecision.Rejected.class,
                new String[] {"reason"},
                new Class<?>[] {ModelCandidateSuitabilityRejectionReason.class});

        ModelInvocationAdmissionDecision.Admitted admitted = admitted(
                "deterministic-fake",
                "deterministic-echo",
                ModelReasoningRequirement.MINIMAL);
        ModelCandidateSuitabilityDecision.Suitable suitable =
                new ModelCandidateSuitabilityDecision.Suitable(admitted, candidate);
        assertSame(admitted, suitable.admitted());
        assertSame(candidate, suitable.candidate());
        assertThrows(
                NullPointerException.class,
                () -> new ModelCandidateSuitabilityDecision.Suitable(null, candidate));
        assertThrows(
                NullPointerException.class,
                () -> new ModelCandidateSuitabilityDecision.Suitable(admitted, null));
        assertThrows(
                NullPointerException.class,
                () -> new ModelCandidateSuitabilityDecision.Rejected(null));

        assertArrayEquals(
                new ModelCandidateSuitabilityRejectionReason[] {
                    ModelCandidateSuitabilityRejectionReason.MODEL_CLASS_UNSUPPORTED,
                    ModelCandidateSuitabilityRejectionReason.REQUIRED_CAPABILITY_UNSUPPORTED,
                    ModelCandidateSuitabilityRejectionReason.REASONING_REQUIREMENT_UNSUPPORTED,
                    ModelCandidateSuitabilityRejectionReason.TOKEN_SEMANTICS_UNAVAILABLE,
                    ModelCandidateSuitabilityRejectionReason.CONTEXT_CAPACITY_INSUFFICIENT,
                    ModelCandidateSuitabilityRejectionReason.INPUT_TOKEN_CAPACITY_INSUFFICIENT,
                    ModelCandidateSuitabilityRejectionReason.OUTPUT_TOKEN_CAPACITY_INSUFFICIENT,
                    ModelCandidateSuitabilityRejectionReason.TOTAL_TOKEN_CAPACITY_INSUFFICIENT,
                    ModelCandidateSuitabilityRejectionReason.FREE_ONLY_COST_REQUIRED,
                    ModelCandidateSuitabilityRejectionReason.DATA_CLASSIFICATION_UNSUPPORTED
                },
                ModelCandidateSuitabilityRejectionReason.values());
    }

    private void assertRejected(
            ModelCandidateSuitabilityRejectionReason expected,
            ModelInvocationAdmissionDecision.Admitted admitted) {
        ModelCandidateSuitabilityDecision.Rejected rejected = assertInstanceOf(
                ModelCandidateSuitabilityDecision.Rejected.class,
                evaluator.evaluate(admitted, candidate));
        assertEquals(expected, rejected.reason());
    }

    private static void assertRecordShape(
            Class<?> type, String[] expectedNames, Class<?>[] expectedTypes) {
        assertTrue(type.isRecord());
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        RecordComponent[] components = type.getRecordComponents();
        assertArrayEquals(
                expectedNames,
                Arrays.stream(components).map(RecordComponent::getName).toArray(String[]::new));
        assertArrayEquals(
                expectedTypes,
                Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new));
    }

    private static ModelInvocationAdmissionDecision.Admitted admitted(
            String modelClass,
            String capability,
            ModelReasoningRequirement reasoning) {
        return admitted(
                modelClass,
                capability,
                reasoning,
                2,
                new ModelTokenBudget(1, 1, 2),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC);
    }

    private static ModelInvocationAdmissionDecision.Admitted admitted(
            String modelClass,
            String capability,
            ModelReasoningRequirement reasoning,
            long minimumContextTokens,
            ModelTokenBudget tokenBudget,
            ModelCostBudget costBudget,
            ModelDataClassification classification) {
        ModelRequest request = new ModelRequest(
                "candidate-test",
                "prompt",
                modelClass,
                Duration.ofSeconds(2),
                1_024);
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                capability,
                modelClass,
                ModelLocalityRequirement.LOCAL_ONLY,
                reasoning,
                minimumContextTokens,
                tokenBudget,
                costBudget,
                Duration.ofSeconds(1),
                classification);
        return new ModelInvocationAdmissionDecision.Admitted(
                new ProfiledModelRequest(request, profile));
    }
}
