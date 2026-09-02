package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void rejectsEveryPredicateInDeterministicFirstMatchOrder() {
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
                ModelCandidateSuitabilityRejectionReason.CONTEXT_CAPACITY_INSUFFICIENT,
                admitted(
                        524_290,
                        new ModelTokenBudget(262_145, 262_145, 524_290),
                        new ModelCostBudget("USD", 1),
                        ModelDataClassification.RESTRICTED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.INPUT_TOKEN_CAPACITY_INSUFFICIENT,
                admitted(
                        524_288,
                        new ModelTokenBudget(262_145, 262_143, 524_288),
                        new ModelCostBudget("USD", 1),
                        ModelDataClassification.RESTRICTED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.OUTPUT_TOKEN_CAPACITY_INSUFFICIENT,
                admitted(
                        524_288,
                        new ModelTokenBudget(262_143, 262_145, 524_288),
                        new ModelCostBudget("USD", 1),
                        ModelDataClassification.RESTRICTED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.TOTAL_TOKEN_CAPACITY_INSUFFICIENT,
                admitted(
                        524_131,
                        new ModelTokenBudget(262_000, 262_131, 524_131),
                        new ModelCostBudget("USD", 1),
                        ModelDataClassification.RESTRICTED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.FREE_ONLY_COST_REQUIRED,
                admitted(
                        2,
                        new ModelTokenBudget(1, 1, 2),
                        new ModelCostBudget("KRW", 1),
                        ModelDataClassification.RESTRICTED));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.DATA_CLASSIFICATION_UNSUPPORTED,
                admitted(
                        2,
                        new ModelTokenBudget(1, 1, 2),
                        new ModelCostBudget("USD", 0),
                        ModelDataClassification.INTERNAL));
    }

    @Test
    void acceptsEachExactThresholdAndRejectsOneTokenAboveIt() {
        assertSuitable(admitted(
                524_288,
                new ModelTokenBudget(1, 1, 2),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.CONTEXT_CAPACITY_INSUFFICIENT,
                admitted(
                        524_289,
                        new ModelTokenBudget(1, 1, 2),
                        new ModelCostBudget("USD", 0),
                        ModelDataClassification.PUBLIC));

        assertSuitable(admitted(
                262_145,
                new ModelTokenBudget(262_144, 1, 262_145),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.INPUT_TOKEN_CAPACITY_INSUFFICIENT,
                admitted(
                        262_146,
                        new ModelTokenBudget(262_145, 1, 262_146),
                        new ModelCostBudget("USD", 0),
                        ModelDataClassification.PUBLIC));

        assertSuitable(admitted(
                262_145,
                new ModelTokenBudget(1, 262_144, 262_145),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.OUTPUT_TOKEN_CAPACITY_INSUFFICIENT,
                admitted(
                        262_146,
                        new ModelTokenBudget(1, 262_145, 262_146),
                        new ModelCostBudget("USD", 0),
                        ModelDataClassification.PUBLIC));

        assertSuitable(admitted(
                524_130,
                new ModelTokenBudget(262_000, 262_130, 524_130),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC));
        assertRejected(
                ModelCandidateSuitabilityRejectionReason.TOTAL_TOKEN_CAPACITY_INSUFFICIENT,
                admitted(
                        524_131,
                        new ModelTokenBudget(262_000, 262_131, 524_131),
                        new ModelCostBudget("USD", 0),
                        ModelDataClassification.PUBLIC));
    }

    @Test
    void returnsSuitableWithTheExactAdmittedAndCandidateInstances() {
        ModelInvocationAdmissionDecision.Admitted admitted = admitted(
                524_288,
                new ModelTokenBudget(261_986, 262_144, 524_130),
                new ModelCostBudget("USD", 0),
                ModelDataClassification.PUBLIC);

        ModelCandidateSuitabilityDecision.Suitable suitable = assertInstanceOf(
                ModelCandidateSuitabilityDecision.Suitable.class,
                evaluator.evaluate(admitted, candidate));

        assertSame(admitted, suitable.admitted());
        assertSame(candidate, suitable.candidate());
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

    private void assertSuitable(ModelInvocationAdmissionDecision.Admitted admitted) {
        ModelCandidateSuitabilityDecision.Suitable suitable = assertInstanceOf(
                ModelCandidateSuitabilityDecision.Suitable.class,
                evaluator.evaluate(admitted, candidate));
        assertSame(admitted, suitable.admitted());
        assertSame(candidate, suitable.candidate());
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
            long minimumContextTokens,
            ModelTokenBudget tokenBudget,
            ModelCostBudget costBudget,
            ModelDataClassification classification) {
        return admitted(
                "deterministic-fake",
                "deterministic-echo",
                ModelReasoningRequirement.MINIMAL,
                minimumContextTokens,
                tokenBudget,
                costBudget,
                classification);
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
