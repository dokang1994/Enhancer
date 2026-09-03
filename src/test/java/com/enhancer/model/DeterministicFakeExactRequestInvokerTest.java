package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeterministicFakeExactRequestInvokerTest {

    private static final Path MODEL_SOURCE = Path.of("src/main/java/com/enhancer/model");

    private final DeterministicFakeExactRequestInvoker invoker =
            new DeterministicFakeExactRequestInvoker();

    @Test
    void rejectsNullAndAppliesToolTimeoutCancellationFirstMatchOrder() {
        assertThrows(NullPointerException.class, () -> invoker.invoke(null));

        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        AtomicInteger deniedCancellationChecks = new AtomicInteger();
        ExecutionPolicy denied = policy(
                Set.of(),
                Set.of(),
                Duration.ofSeconds(1),
                () -> {
                    deniedCancellationChecks.incrementAndGet();
                    return true;
                });
        assertRefused(
                DeterministicFakeExactRequestInvocationRejectionReason
                        .EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                ready(gateway, "denied", Duration.ofSeconds(2), denied));
        assertEquals(0, deniedCancellationChecks.get());
        verifyNoInteractions(gateway);

        AtomicInteger timeoutCancellationChecks = new AtomicInteger();
        ExecutionPolicy equalTimeout = policy(
                Set.of("model-invoke"),
                Set.of(),
                Duration.ofSeconds(2),
                () -> {
                    timeoutCancellationChecks.incrementAndGet();
                    return true;
                });
        assertRefused(
                DeterministicFakeExactRequestInvocationRejectionReason
                        .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                ready(gateway, "equal-timeout", Duration.ofSeconds(2), equalTimeout));
        assertEquals(0, timeoutCancellationChecks.get());
        verifyNoInteractions(gateway);

        AtomicInteger cancellationChecks = new AtomicInteger();
        ExecutionPolicy cancelled = policy(
                Set.of("model-invoke"),
                Set.of(),
                Duration.ofSeconds(3),
                () -> {
                    cancellationChecks.incrementAndGet();
                    return true;
                });
        assertRefused(
                DeterministicFakeExactRequestInvocationRejectionReason.CANCELLATION_REQUESTED,
                ready(gateway, "cancelled", Duration.ofSeconds(2), cancelled));
        assertEquals(1, cancellationChecks.get());
        verifyNoInteractions(gateway);
    }

    @Test
    void requiresTheRequestTimeoutToFitStrictlyInsideThePolicy() throws Exception {
        DeterministicFakeModelGateway equalGateway = mock(DeterministicFakeModelGateway.class);
        ExecutionPolicy equalPolicy = policy(
                Set.of("model-invoke"),
                Set.of(),
                Duration.ofSeconds(2),
                CancellationToken.none());
        assertRefused(
                DeterministicFakeExactRequestInvocationRejectionReason
                        .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                ready(equalGateway, "equal", Duration.ofSeconds(2), equalPolicy));
        verifyNoInteractions(equalGateway);

        DeterministicFakeModelGateway greaterGateway = mock(DeterministicFakeModelGateway.class);
        ExecutionPolicy smallerPolicy = policy(
                Set.of("model-invoke"),
                Set.of(),
                Duration.ofMillis(1_999),
                CancellationToken.none());
        assertRefused(
                DeterministicFakeExactRequestInvocationRejectionReason
                        .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                ready(greaterGateway, "greater", Duration.ofSeconds(2), smallerPolicy));
        verifyNoInteractions(greaterGateway);

        DeterministicFakeModelGateway insideGateway = mock(DeterministicFakeModelGateway.class);
        ExecutionPolicy outerPolicy = policy(
                Set.of("model-invoke"),
                Set.of(),
                Duration.ofSeconds(2),
                CancellationToken.none());
        DeterministicFakeExactRequestDecision.Ready inside = ready(
                insideGateway, "inside", Duration.ofMillis(1_999), outerPolicy);
        ModelRequest request = request(inside);
        ModelResponse response = new ModelResponse(
                "inside-response", "deterministic-fake", new ModelUsage(1, 1));
        when(insideGateway.invoke(same(request))).thenReturn(response);

        DeterministicFakeExactRequestInvocationResult.Succeeded succeeded =
                assertInstanceOf(
                        DeterministicFakeExactRequestInvocationResult.Succeeded.class,
                        invoker.invoke(inside));
        assertSame(inside, succeeded.ready());
        assertSame(response, succeeded.response());
        verify(insideGateway, times(1)).invoke(same(request));
    }

    @Test
    void invokesTheExactCandidateBoundGatewayWithTheExactRequestOnce() throws Exception {
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        ExecutionPolicy policy = allowedPolicy("exact-success-root");
        DeterministicFakeExactRequestDecision.Ready ready = ready(
                gateway, "exact-request", Duration.ofSeconds(2), policy);
        ModelRequest request = request(ready);
        ModelResponse response = new ModelResponse(
                "untrusted-response", "deterministic-fake", new ModelUsage(13, 14));
        when(gateway.invoke(same(request))).thenReturn(response);

        DeterministicFakeExactRequestInvocationResult.Succeeded succeeded =
                assertInstanceOf(
                        DeterministicFakeExactRequestInvocationResult.Succeeded.class,
                        invoker.invoke(ready));

        assertSame(ready, succeeded.ready());
        assertSame(response, succeeded.response());
        assertSame(policy, succeeded.ready().executionPolicy());
        assertSame(request, request(succeeded.ready()));
        assertSame(gateway, succeeded.ready().suitable().candidate().gateway());
        verify(gateway, times(1)).invoke(same(request));
    }

    @Test
    void invokesTheRealDeterministicFakeOnlyThroughTheReadyIdentityChain()
            throws Exception {
        DeterministicFakeModelGateway gateway = new DeterministicFakeModelGateway();
        DeterministicFakeExactRequestDecision.Ready ready = ready(
                gateway,
                "A\uD83D\uDE00B",
                Duration.ofSeconds(2),
                allowedPolicy("real-fake-root"));

        DeterministicFakeExactRequestInvocationResult.Succeeded succeeded =
                assertInstanceOf(
                        DeterministicFakeExactRequestInvocationResult.Succeeded.class,
                        invoker.invoke(ready));
        assertSame(ready, succeeded.ready());
        assertTrue(succeeded.response().text().startsWith("deterministic-fake-v1\n"));
        assertTrue(succeeded.response().text().endsWith("echo=A\uD83D\uDE00B"));
    }

    @Test
    void preservesEveryGatewayFailureCodeWithoutReasonText() throws Exception {
        String secretReason = "secret-gateway-reason";
        for (ModelFailureCode code : ModelFailureCode.values()) {
            DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
            DeterministicFakeExactRequestDecision.Ready ready = ready(
                    gateway,
                    "gateway-failure-" + code,
                    Duration.ofSeconds(2),
                    allowedPolicy("gateway-failure-root"));
            ModelRequest request = request(ready);
            doThrow(new ModelGatewayException(code, secretReason))
                    .when(gateway)
                    .invoke(same(request));

            DeterministicFakeExactRequestInvocationResult.GatewayFailed failed =
                    assertInstanceOf(
                            DeterministicFakeExactRequestInvocationResult.GatewayFailed.class,
                            invoker.invoke(ready));
            assertSame(ready, failed.ready());
            assertSame(code, failed.failureCode());
            assertFalse(failed.toString().contains(secretReason));
            verify(gateway, times(1)).invoke(same(request));
        }
    }

    @Test
    void propagatesUncheckedProgrammingFailuresUnchanged() throws Exception {
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        DeterministicFakeExactRequestDecision.Ready ready = ready(
                gateway,
                "unchecked-failure",
                Duration.ofSeconds(2),
                allowedPolicy("unchecked-failure-root"));
        ModelRequest request = request(ready);
        IllegalStateException failure = new IllegalStateException("secret-unchecked-reason");
        doThrow(failure).when(gateway).invoke(same(request));

        IllegalStateException propagated = assertThrows(
                IllegalStateException.class,
                () -> invoker.invoke(ready));
        assertSame(failure, propagated);
        verify(gateway, times(1)).invoke(same(request));
    }

    @Test
    void exposesOnlyAFieldFreeReadyInvocationAndPrivateOpaqueResults()
            throws Exception {
        Class<DeterministicFakeExactRequestInvoker> invokerType =
                DeterministicFakeExactRequestInvoker.class;
        assertTrue(Modifier.isPublic(invokerType.getModifiers()));
        assertTrue(Modifier.isFinal(invokerType.getModifiers()));
        assertEquals(0, invokerType.getDeclaredFields().length);
        Method[] publicMethods = Arrays.stream(invokerType.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toArray(Method[]::new);
        assertEquals(1, publicMethods.length);
        Method invoke = invokerType.getDeclaredMethod(
                "invoke", DeterministicFakeExactRequestDecision.Ready.class);
        assertEquals(DeterministicFakeExactRequestInvocationResult.class, invoke.getReturnType());

        Class<DeterministicFakeExactRequestInvocationResult> resultType =
                DeterministicFakeExactRequestInvocationResult.class;
        assertTrue(Modifier.isPublic(resultType.getModifiers()));
        assertTrue(Modifier.isAbstract(resultType.getModifiers()));
        assertTrue(resultType.isSealed());
        assertEquals(
                Set.of(
                        DeterministicFakeExactRequestInvocationResult.Succeeded.class,
                        DeterministicFakeExactRequestInvocationResult.Refused.class,
                        DeterministicFakeExactRequestInvocationResult.GatewayFailed.class),
                Set.of(resultType.getPermittedSubclasses()));
        assertFalse(Serializable.class.isAssignableFrom(resultType));
        assertEquals(0, Arrays.stream(resultType.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .count());

        assertOpaqueVariant(
                DeterministicFakeExactRequestInvocationResult.Succeeded.class,
                DeterministicFakeExactRequestDecision.Ready.class,
                ModelResponse.class);
        assertOpaqueVariant(
                DeterministicFakeExactRequestInvocationResult.Refused.class,
                DeterministicFakeExactRequestDecision.Ready.class,
                DeterministicFakeExactRequestInvocationRejectionReason.class);
        assertOpaqueVariant(
                DeterministicFakeExactRequestInvocationResult.GatewayFailed.class,
                DeterministicFakeExactRequestDecision.Ready.class,
                ModelFailureCode.class);
        assertArrayEquals(
                new DeterministicFakeExactRequestInvocationRejectionReason[] {
                    DeterministicFakeExactRequestInvocationRejectionReason
                            .EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                    DeterministicFakeExactRequestInvocationRejectionReason
                            .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                    DeterministicFakeExactRequestInvocationRejectionReason
                            .CANCELLATION_REQUESTED
                },
                DeterministicFakeExactRequestInvocationRejectionReason.values());
    }

    @Test
    void rendersNoPromptResponsePolicyPathOrGatewayReason() throws Exception {
        String secretPrompt = "secret-invocation-prompt";
        String secretResponse = "secret-invocation-response";
        String secretReason = "secret-invocation-gateway-reason";

        DeterministicFakeModelGateway successGateway = mock(DeterministicFakeModelGateway.class);
        DeterministicFakeExactRequestDecision.Ready successReady = ready(
                successGateway,
                secretPrompt,
                Duration.ofSeconds(2),
                allowedPolicy("secret-success-policy-root"));
        ModelRequest successRequest = request(successReady);
        when(successGateway.invoke(same(successRequest))).thenReturn(new ModelResponse(
                secretResponse, "deterministic-fake", new ModelUsage(1, 1)));
        DeterministicFakeExactRequestInvocationResult.Succeeded succeeded =
                assertInstanceOf(
                        DeterministicFakeExactRequestInvocationResult.Succeeded.class,
                        invoker.invoke(successReady));
        assertRedacted(succeeded.toString(), secretPrompt, secretResponse,
                "secret-success-policy-root");

        DeterministicFakeModelGateway refusedGateway = mock(DeterministicFakeModelGateway.class);
        ExecutionPolicy refusedPolicy = policy(
                Set.of(), Set.of(), Duration.ofSeconds(3), CancellationToken.none());
        DeterministicFakeExactRequestDecision.Ready refusedReady = ready(
                refusedGateway, secretPrompt, Duration.ofSeconds(2), refusedPolicy);
        DeterministicFakeExactRequestInvocationResult.Refused refused = assertInstanceOf(
                DeterministicFakeExactRequestInvocationResult.Refused.class,
                invoker.invoke(refusedReady));
        assertRedacted(refused.toString(), secretPrompt, secretResponse,
                "secret-policy-root");

        DeterministicFakeModelGateway failedGateway = mock(DeterministicFakeModelGateway.class);
        DeterministicFakeExactRequestDecision.Ready failedReady = ready(
                failedGateway,
                secretPrompt,
                Duration.ofSeconds(2),
                allowedPolicy("secret-failed-policy-root"));
        ModelRequest failedRequest = request(failedReady);
        doThrow(new ModelGatewayException(ModelFailureCode.TIMED_OUT, secretReason))
                .when(failedGateway)
                .invoke(same(failedRequest));
        DeterministicFakeExactRequestInvocationResult.GatewayFailed failed =
                assertInstanceOf(
                        DeterministicFakeExactRequestInvocationResult.GatewayFailed.class,
                        invoker.invoke(failedReady));
        assertRedacted(failed.toString(), secretPrompt, secretReason,
                "secret-failed-policy-root");
    }

    @Test
    void sourcePinsExactIdentityPathOrderSingleCallAndNarrowExceptionMapping()
            throws IOException {
        String source = Files.readString(
                MODEL_SOURCE.resolve("DeterministicFakeExactRequestInvoker.java"),
                StandardCharsets.UTF_8);
        assertEquals(1, occurrences(
                source, "ready.suitable().admitted().profiledRequest().request()"));
        assertEquals(1, occurrences(
                source, "ready.suitable().candidate().gateway().invoke(request)"));
        int tool = source.indexOf("policy.allows(\"model-invoke\")");
        int timeout = source.indexOf("request.timeout().compareTo(policy.timeout()) >= 0");
        int cancellation = source.indexOf(
                "policy.cancellationToken().isCancellationRequested()");
        int gateway = source.indexOf(
                "ready.suitable().candidate().gateway().invoke(request)");
        assertTrue(tool >= 0
                && tool < timeout
                && timeout < cancellation
                && cancellation < gateway);
        assertTrue(source.contains("catch (ModelGatewayException exception)"));
        assertTrue(source.contains("exception.code()"));

        for (String forbidden : List.of(
                "new ModelRequest",
                ".prompt()",
                "ModelInvokeTool",
                "ToolRequest",
                "ToolExecutor",
                "ToolResult",
                "VerificationEvidence",
                "RunRecord",
                "getMessage()",
                "getCause()",
                "printStackTrace",
                "catch (Exception",
                "catch (RuntimeException",
                "catch (Throwable",
                "java.io",
                "java.nio.file",
                "java.net",
                "com.enhancer.runtime")) {
            assertFalse(source.contains(forbidden), () -> "invoker must not contain " + forbidden);
        }
        assertFalse(source.matches("(?s).*\\bModelGateway\\b.*"));
    }

    private void assertRefused(
            DeterministicFakeExactRequestInvocationRejectionReason expected,
            DeterministicFakeExactRequestDecision.Ready ready) {
        DeterministicFakeExactRequestInvocationResult.Refused refused = assertInstanceOf(
                DeterministicFakeExactRequestInvocationResult.Refused.class,
                invoker.invoke(ready));
        assertSame(ready, refused.ready());
        assertSame(expected, refused.reason());
    }

    private static DeterministicFakeExactRequestDecision.Ready ready(
            DeterministicFakeModelGateway gateway,
            String prompt,
            Duration requestTimeout,
            ExecutionPolicy policy) {
        ModelTokenBudget budget = new ModelTokenBudget(1_000, 2_000, 3_000);
        ModelRequest request = new ModelRequest(
                "exact-invoker-test",
                prompt,
                "deterministic-fake",
                requestTimeout,
                ModelRequest.MAX_RESPONSE_LENGTH);
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
                DeterministicFakeModelCandidate.bind(gateway);
        ModelCandidateSuitabilityDecision.Suitable suitable = assertInstanceOf(
                ModelCandidateSuitabilityDecision.Suitable.class,
                new ModelCandidateSuitability().evaluate(admitted, candidate));
        return assertInstanceOf(
                DeterministicFakeExactRequestDecision.Ready.class,
                new DeterministicFakeExactRequestPreparation().evaluate(suitable, policy));
    }

    private static ModelRequest request(DeterministicFakeExactRequestDecision.Ready ready) {
        return ready.suitable().admitted().profiledRequest().request();
    }

    private static ExecutionPolicy allowedPolicy(String rootName) {
        return new ExecutionPolicy(
                Path.of(rootName),
                Set.of("model-invoke"),
                Set.of(),
                1,
                Duration.ofSeconds(3),
                CancellationToken.none());
    }

    private static ExecutionPolicy policy(
            Set<String> allowedTools,
            Set<String> deniedTools,
            Duration timeout,
            CancellationToken cancellationToken) {
        return new ExecutionPolicy(
                Path.of("secret-policy-root"),
                allowedTools,
                deniedTools,
                1,
                timeout,
                cancellationToken);
    }

    private static void assertOpaqueVariant(Class<?> type, Class<?>... constructorParameters) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertFalse(type.isRecord());
        assertFalse(Serializable.class.isAssignableFrom(type));
        Field[] fields = type.getDeclaredFields();
        assertEquals(2, fields.length);
        for (Field field : fields) {
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
        }
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
        assertArrayEquals(constructorParameters, constructors[0].getParameterTypes());
    }

    private static void assertRedacted(String rendered, String... secrets) {
        for (String secret : secrets) {
            assertFalse(rendered.contains(secret), () -> "rendering exposed " + secret);
        }
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
