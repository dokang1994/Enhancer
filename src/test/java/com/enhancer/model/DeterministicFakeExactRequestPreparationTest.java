package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeterministicFakeExactRequestPreparationTest {

    private static final Path MODEL_SOURCE = Path.of("src/main/java/com/enhancer/model");

    private final DeterministicFakeExactRequestPreparation preparation =
            new DeterministicFakeExactRequestPreparation();

    @Test
    void retainsExactIdentitiesAndDerivedCountsForAsciiAndSupplementaryPrompts() {
        ExecutionPolicy policy = policy("exact-policy-root");
        ModelCandidateSuitabilityDecision.Suitable ascii = suitable(
                "a", 154, new ModelTokenBudget(1, 154, 155));

        DeterministicFakeExactRequestDecision.Ready asciiReady = assertInstanceOf(
                DeterministicFakeExactRequestDecision.Ready.class,
                preparation.evaluate(ascii, policy));
        assertSame(ascii, asciiReady.suitable());
        assertSame(policy, asciiReady.executionPolicy());
        assertEquals(1, asciiReady.inputTokens());
        assertEquals(154, asciiReady.predictedResponseUtf16Length());
        assertEquals(154, asciiReady.predictedOutputTokens());
        assertEquals(155, asciiReady.predictedTotalTokens());

        ModelCandidateSuitabilityDecision.Suitable supplementary = suitable(
                "\uD83D\uDE00", 155, new ModelTokenBudget(1, 154, 155));
        DeterministicFakeExactRequestDecision.Ready supplementaryReady =
                assertInstanceOf(
                        DeterministicFakeExactRequestDecision.Ready.class,
                        preparation.evaluate(supplementary, policy));
        assertSame(supplementary, supplementaryReady.suitable());
        assertSame(supplementary.admitted(), supplementaryReady.suitable().admitted());
        assertSame(supplementary.candidate(), supplementaryReady.suitable().candidate());
        assertEquals(1, supplementaryReady.inputTokens());
        assertEquals(155, supplementaryReady.predictedResponseUtf16Length());
        assertEquals(154, supplementaryReady.predictedOutputTokens());
        assertEquals(155, supplementaryReady.predictedTotalTokens());
    }

    @Test
    void rejectsNullAndEveryMalformedSurrogatePositionBeforeOtherBudgets() {
        ExecutionPolicy policy = policy("malformed-policy-root");
        ModelCandidateSuitabilityDecision.Suitable valid = suitable(
                "a", 154, new ModelTokenBudget(1, 154, 155));
        assertThrows(NullPointerException.class, () -> preparation.evaluate(null, policy));
        assertThrows(NullPointerException.class, () -> preparation.evaluate(valid, null));

        List<String> malformed = List.of(
                "\uD800",
                "\uDC00",
                "\uD800A",
                "A\uDC00",
                "prefix\uD800middle",
                "prefix\uDC00suffix",
                "valid\uD83D\uDE00\uD800");
        for (String prompt : malformed) {
            ModelCandidateSuitabilityDecision.Suitable suitable = suitable(
                    prompt, 1, new ModelTokenBudget(1, 1, 2));
            DeterministicFakeExactRequestDecision.Refused refused = assertInstanceOf(
                    DeterministicFakeExactRequestDecision.Refused.class,
                    preparation.evaluate(suitable, policy));
            assertSame(suitable, refused.suitable());
            assertSame(policy, refused.executionPolicy());
            assertEquals(
                    DeterministicFakeExactRequestRejectionReason.MALFORMED_PROMPT,
                    refused.reason());
            assertFalse(refused.toString().contains(prompt));
        }
    }

    @Test
    void appliesInputThenResponseLengthThenOutputFirstMatchOrder() {
        assertReason(
                DeterministicFakeExactRequestRejectionReason.INPUT_TOKEN_BUDGET_EXCEEDED,
                "aa",
                1,
                new ModelTokenBudget(1, 155, 156));
        assertReason(
                DeterministicFakeExactRequestRejectionReason
                        .PREDICTED_RESPONSE_UTF16_LENGTH_BUDGET_EXCEEDED,
                "a",
                153,
                new ModelTokenBudget(1, 153, 154));
        assertReason(
                DeterministicFakeExactRequestRejectionReason
                        .PREDICTED_OUTPUT_TOKEN_BUDGET_EXCEEDED,
                "a",
                154,
                new ModelTokenBudget(1, 153, 154));

        assertInstanceOf(
                DeterministicFakeExactRequestDecision.Ready.class,
                preparation.evaluate(
                        suitable("a", 154, new ModelTokenBudget(1, 154, 155)),
                        policy("equal-bound-policy-root")));
    }

    @Test
    void acceptsTheTightCandidateMaximumBoundary() {
        String prompt = "a".repeat(261_986);
        DeterministicFakeExactRequestDecision.Ready ready = assertInstanceOf(
                DeterministicFakeExactRequestDecision.Ready.class,
                preparation.evaluate(
                        suitable(
                                prompt,
                                ModelRequest.MAX_RESPONSE_LENGTH,
                                new ModelTokenBudget(261_986, 262_144, 524_130)),
                        policy("maximum-bound-policy-root")));

        assertEquals(261_986, ready.inputTokens());
        assertEquals(262_144, ready.predictedResponseUtf16Length());
        assertEquals(262_144, ready.predictedOutputTokens());
        assertEquals(524_130, ready.predictedTotalTokens());
    }

    @Test
    void keepsTheDefensiveTotalReasonWithoutForgingAnInvalidBudget() {
        assertArrayEquals(
                new DeterministicFakeExactRequestRejectionReason[] {
                    DeterministicFakeExactRequestRejectionReason.MALFORMED_PROMPT,
                    DeterministicFakeExactRequestRejectionReason
                            .INPUT_TOKEN_BUDGET_EXCEEDED,
                    DeterministicFakeExactRequestRejectionReason
                            .PREDICTED_RESPONSE_UTF16_LENGTH_BUDGET_EXCEEDED,
                    DeterministicFakeExactRequestRejectionReason
                            .PREDICTED_OUTPUT_TOKEN_BUDGET_EXCEEDED,
                    DeterministicFakeExactRequestRejectionReason
                            .PREDICTED_TOTAL_TOKEN_BUDGET_EXCEEDED
                },
                DeterministicFakeExactRequestRejectionReason.values());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ModelTokenBudget(1, 154, 154));

        ModelTokenBudget budget = new ModelTokenBudget(3, 5, 8);
        for (long actualInput = 0; actualInput <= budget.maxInputTokens(); actualInput++) {
            for (long predictedOutput = 0;
                    predictedOutput <= budget.maxOutputTokens();
                    predictedOutput++) {
                assertTrue(Math.addExact(actualInput, predictedOutput)
                        <= budget.maxTotalTokens());
            }
        }
    }

    @Test
    void exposesOnlyFieldFreeEvaluationAndPrivateOpaqueVariants() throws Exception {
        Class<DeterministicFakeExactRequestPreparation> preparationType =
                DeterministicFakeExactRequestPreparation.class;
        assertTrue(Modifier.isPublic(preparationType.getModifiers()));
        assertTrue(Modifier.isFinal(preparationType.getModifiers()));
        assertEquals(0, Arrays.stream(preparationType.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .count());
        Method[] publicMethods = Arrays.stream(preparationType.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toArray(Method[]::new);
        assertEquals(1, publicMethods.length);
        Method evaluate = preparationType.getDeclaredMethod(
                "evaluate",
                ModelCandidateSuitabilityDecision.Suitable.class,
                ExecutionPolicy.class);
        assertEquals(DeterministicFakeExactRequestDecision.class, evaluate.getReturnType());

        Class<DeterministicFakeExactRequestDecision> decisionType =
                DeterministicFakeExactRequestDecision.class;
        assertTrue(Modifier.isPublic(decisionType.getModifiers()));
        assertTrue(Modifier.isAbstract(decisionType.getModifiers()));
        assertTrue(decisionType.isSealed());
        assertEquals(
                Set.of(
                        DeterministicFakeExactRequestDecision.Ready.class,
                        DeterministicFakeExactRequestDecision.Refused.class),
                Set.of(decisionType.getPermittedSubclasses()));
        assertFalse(Serializable.class.isAssignableFrom(decisionType));
        assertEquals(0, Arrays.stream(decisionType.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .count());

        assertOpaqueVariant(
                DeterministicFakeExactRequestDecision.Ready.class,
                6,
                ModelCandidateSuitabilityDecision.Suitable.class,
                ExecutionPolicy.class,
                long.class,
                long.class,
                long.class,
                long.class);
        assertOpaqueVariant(
                DeterministicFakeExactRequestDecision.Refused.class,
                3,
                ModelCandidateSuitabilityDecision.Suitable.class,
                ExecutionPolicy.class,
                DeterministicFakeExactRequestRejectionReason.class);
    }

    @Test
    void rendersNoRetainedPromptCorrelationOrProjectPath() {
        String secret = "secret-prompt-correlation-value";
        ExecutionPolicy policy = policy("secret-project-root-value");
        ModelCandidateSuitabilityDecision.Suitable readySuitable = suitable(
                secret, ModelRequest.MAX_RESPONSE_LENGTH, new ModelTokenBudget(100, 400, 500));
        DeterministicFakeExactRequestDecision.Ready ready = assertInstanceOf(
                DeterministicFakeExactRequestDecision.Ready.class,
                preparation.evaluate(readySuitable, policy));
        ModelCandidateSuitabilityDecision.Suitable refusedSuitable = suitable(
                secret, 1, new ModelTokenBudget(100, 400, 500));
        DeterministicFakeExactRequestDecision.Refused refused = assertInstanceOf(
                DeterministicFakeExactRequestDecision.Refused.class,
                preparation.evaluate(refusedSuitable, policy));

        assertFalse(ready.toString().contains(secret));
        assertFalse(ready.toString().contains("secret-project-root-value"));
        assertFalse(ready.toString().contains(
                readySuitable.admitted().profiledRequest().request().correlationId()));
        assertFalse(refused.toString().contains(secret));
        assertFalse(refused.toString().contains("secret-project-root-value"));
        assertFalse(refused.toString().contains(
                refusedSuitable.admitted().profiledRequest().request().correlationId()));
    }

    @Test
    void sourcePinsOnePromptReadOneScalarScanAndTheRequiredOrder() throws IOException {
        String source = Files.readString(
                MODEL_SOURCE.resolve("DeterministicFakeExactRequestPreparation.java"),
                StandardCharsets.UTF_8);
        assertEquals(1, occurrences(source, "request.prompt()"));
        assertEquals(1, occurrences(source, ".count(prompt)"));
        assertEquals(1, occurrences(source, "responseUtf16Length(promptUtf16Length)"));
        assertEquals(1, occurrences(
                source, "responseTokenCount(promptUtf16Length, inputTokens)"));
        assertEquals(1, occurrences(
                source, "Math.addExact(inputTokens, predictedOutputTokens)"));

        int count = source.indexOf(".count(prompt)");
        int input = source.indexOf("inputTokens > budget.maxInputTokens()");
        int responseDerivation = source.indexOf("responseUtf16Length(promptUtf16Length)");
        int responseLength = source.indexOf(
                "predictedResponseUtf16Length > request.maxResponseLength()");
        int output = source.indexOf("predictedOutputTokens > budget.maxOutputTokens()");
        int totalDerivation = source.indexOf(
                "Math.addExact(inputTokens, predictedOutputTokens)");
        int total = source.indexOf("predictedTotalTokens > budget.maxTotalTokens()");
        int ready = source.indexOf("DeterministicFakeExactRequestDecision.ready(");
        assertTrue(count >= 0
                && count < input
                && input < responseDerivation
                && responseDerivation < responseLength
                && responseLength < output
                && output < totalDerivation
                && totalDerivation < total
                && total < ready);

        for (String forbidden : List.of(
                "new ModelRequest",
                "ModelUsage",
                "Normalizer",
                "getBytes(",
                ".invoke(",
                "ModelGateway",
                "GovernedModelPromptReader",
                "ToolExecutor",
                "ToolResult",
                "RunRecord",
                "java.io",
                "java.nio.file",
                "com.enhancer.runtime")) {
            assertFalse(source.contains(forbidden), () -> "preparation must not contain " + forbidden);
        }
    }

    private void assertReason(
            DeterministicFakeExactRequestRejectionReason expected,
            String prompt,
            int maxResponseLength,
            ModelTokenBudget budget) {
        ExecutionPolicy policy = policy("rejection-policy-root");
        ModelCandidateSuitabilityDecision.Suitable suitable = suitable(
                prompt, maxResponseLength, budget);
        DeterministicFakeExactRequestDecision.Refused refused = assertInstanceOf(
                DeterministicFakeExactRequestDecision.Refused.class,
                preparation.evaluate(suitable, policy));
        assertSame(suitable, refused.suitable());
        assertSame(policy, refused.executionPolicy());
        assertEquals(expected, refused.reason());
    }

    private static ModelCandidateSuitabilityDecision.Suitable suitable(
            String prompt,
            int maxResponseLength,
            ModelTokenBudget budget) {
        ModelRequest request = new ModelRequest(
                "exact-request-test",
                prompt,
                "deterministic-fake",
                Duration.ofSeconds(2),
                maxResponseLength);
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "deterministic-echo",
                "deterministic-fake",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.MINIMAL,
                budget.maxTotalTokens(),
                budget,
                new ModelCostBudget("USD", 0),
                Duration.ofSeconds(1),
                ModelDataClassification.PUBLIC);
        ModelInvocationAdmissionDecision.Admitted admitted =
                new ModelInvocationAdmissionDecision.Admitted(
                        new ProfiledModelRequest(request, profile));
        DeterministicFakeModelCandidate candidate =
                DeterministicFakeModelCandidate.bind(new DeterministicFakeModelGateway());
        return assertInstanceOf(
                ModelCandidateSuitabilityDecision.Suitable.class,
                new ModelCandidateSuitability().evaluate(admitted, candidate));
    }

    private static ExecutionPolicy policy(String rootName) {
        return new ExecutionPolicy(
                Path.of(rootName),
                Set.of("model-invoke"),
                Set.of(),
                1,
                Duration.ofSeconds(3),
                CancellationToken.none());
    }

    private static void assertOpaqueVariant(
            Class<?> type,
            int fieldCount,
            Class<?>... constructorParameters) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertFalse(type.isRecord());
        assertFalse(Serializable.class.isAssignableFrom(type));
        Field[] fields = type.getDeclaredFields();
        assertEquals(fieldCount, fields.length);
        for (Field field : fields) {
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
        }
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
        assertArrayEquals(constructorParameters, constructors[0].getParameterTypes());
    }

    private static int occurrences(String source, String fragment) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }
}
