package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelInvocationAdmissionTest {

    private static final String REQUIRED_CAPABILITY = "repository-analysis";
    private static final String MODEL_CLASS = "reasoning-standard";

    @Test
    void admitsTheExactLocalProfiledRequest() {
        ProfiledModelRequest profiledRequest = profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                4096,
                2048,
                ModelDataClassification.INTERNAL,
                0);

        ModelInvocationAdmissionDecision decision = new ModelInvocationAdmission().evaluate(
                profiledRequest,
                approvedTask(Set.of(ModelInvokeTool.NAME)),
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31)),
                REQUIRED_CAPABILITY);

        ModelInvocationAdmissionDecision.Admitted admitted = assertInstanceOf(
                ModelInvocationAdmissionDecision.Admitted.class, decision);
        assertSame(profiledRequest, admitted.profiledRequest());
    }

    @Test
    void rejectsEveryMissingInputAsAProgrammingError() {
        ProfiledModelRequest profiledRequest = validProfiledRequest();
        ApprovedTask approvedTask = approvedTask(Set.of(ModelInvokeTool.NAME));
        ExecutionPolicy executionPolicy = executionPolicy(
                Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31));
        ModelInvocationAdmission admission = new ModelInvocationAdmission();

        assertThrows(NullPointerException.class, () -> admission.evaluate(
                null, approvedTask, executionPolicy, REQUIRED_CAPABILITY));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                profiledRequest, null, executionPolicy, REQUIRED_CAPABILITY));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                profiledRequest, approvedTask, null, REQUIRED_CAPABILITY));
        assertThrows(NullPointerException.class, () -> admission.evaluate(
                profiledRequest, approvedTask, executionPolicy, null));
    }

    @Test
    void rejectsWhenTheApprovedTaskDoesNotAllowModelInvocation() {
        assertRejected(
                ModelInvocationRejectionReason.TASK_TOOL_NOT_ALLOWED,
                validProfiledRequest(),
                approvedTask(Set.of("read-file")),
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31)),
                REQUIRED_CAPABILITY);
    }

    @Test
    void mapsPolicyAllowlistOmissionAndExplicitDenialToTheSameReason() {
        ApprovedTask approvedTask = approvedTask(Set.of(ModelInvokeTool.NAME));

        assertRejected(
                ModelInvocationRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                validProfiledRequest(),
                approvedTask,
                executionPolicy(Set.of("read-file"), Set.of(), Duration.ofSeconds(31)),
                REQUIRED_CAPABILITY);
        assertRejected(
                ModelInvocationRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                validProfiledRequest(),
                approvedTask,
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME),
                        Set.of(ModelInvokeTool.NAME),
                        Duration.ofSeconds(31)),
                REQUIRED_CAPABILITY);
    }

    @Test
    void rejectsAnAuthoritativeCapabilityMismatch() {
        assertRejected(
                ModelInvocationRejectionReason.REQUIRED_CAPABILITY_MISMATCH,
                validProfiledRequest(),
                approvedTask(Set.of(ModelInvokeTool.NAME)),
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31)),
                "code-review");
    }

    @Test
    void enforcesTheStrictPolicyTimeoutBoundaryWithoutConversion() {
        ApprovedTask approvedTask = approvedTask(Set.of(ModelInvokeTool.NAME));
        ProfiledModelRequest equalProfileAndRequestTime = profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                2,
                1,
                ModelDataClassification.INTERNAL,
                0);

        assertRejected(
                ModelInvocationRejectionReason.GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                equalProfileAndRequestTime,
                approvedTask,
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(30)),
                REQUIRED_CAPABILITY);
        assertRejected(
                ModelInvocationRejectionReason.GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                equalProfileAndRequestTime,
                approvedTask,
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofMillis(29_999)),
                REQUIRED_CAPABILITY);

        ModelInvocationAdmissionDecision admitted = new ModelInvocationAdmission().evaluate(
                equalProfileAndRequestTime,
                approvedTask,
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofMillis(30_001)),
                REQUIRED_CAPABILITY);
        assertInstanceOf(ModelInvocationAdmissionDecision.Admitted.class, admitted);
    }

    @Test
    void requiresLaterOutboundPolicyForPolicyConstrainedLocality() {
        assertRejected(
                ModelInvocationRejectionReason.OUTBOUND_POLICY_REQUIRED,
                profiledRequest(
                        ModelLocalityRequirement.POLICY_CONSTRAINED,
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(30),
                        2,
                        1,
                        ModelDataClassification.PUBLIC,
                        0),
                approvedTask(Set.of(ModelInvokeTool.NAME)),
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31)),
                REQUIRED_CAPABILITY);
    }

    @Test
    void returnsTheFirstMatchingReasonInTheAcceptedOrder() {
        ProfiledModelRequest constrained = profiledRequest(
                ModelLocalityRequirement.POLICY_CONSTRAINED,
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                2,
                1,
                ModelDataClassification.RESTRICTED,
                1);
        ApprovedTask deniedTask = approvedTask(Set.of("read-file"));
        ApprovedTask allowedTask = approvedTask(Set.of(ModelInvokeTool.NAME));
        ExecutionPolicy deniedPolicy = executionPolicy(
                Set.of("read-file"), Set.of(), Duration.ofSeconds(30));
        ExecutionPolicy timeoutPolicy = executionPolicy(
                Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(30));
        ExecutionPolicy passingPolicy = executionPolicy(
                Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31));

        assertRejected(
                ModelInvocationRejectionReason.TASK_TOOL_NOT_ALLOWED,
                constrained, deniedTask, deniedPolicy, "code-review");
        assertRejected(
                ModelInvocationRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                constrained, allowedTask, deniedPolicy, "code-review");
        assertRejected(
                ModelInvocationRejectionReason.REQUIRED_CAPABILITY_MISMATCH,
                constrained, allowedTask, timeoutPolicy, "code-review");
        assertRejected(
                ModelInvocationRejectionReason.GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                constrained, allowedTask, timeoutPolicy, REQUIRED_CAPABILITY);
        assertRejected(
                ModelInvocationRejectionReason.OUTBOUND_POLICY_REQUIRED,
                constrained, allowedTask, passingPolicy, REQUIRED_CAPABILITY);
    }

    @Test
    void keepsCharacterAndTokenMagnitudesIndependent() {
        assertAdmitted(profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                1,
                1000,
                ModelDataClassification.INTERNAL,
                0));
        assertAdmitted(profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                100_000,
                1,
                ModelDataClassification.INTERNAL,
                0));
    }

    @Test
    void restrictedClassificationAndPositiveCostCreateNoUnrelatedRejection() {
        assertAdmitted(profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                4096,
                2048,
                ModelDataClassification.RESTRICTED,
                500_000));
    }

    @Test
    void exposesOnlyTheAcceptedPureContractShapes() throws Exception {
        assertTrue(Modifier.isPublic(ModelInvocationAdmission.class.getModifiers()));
        assertTrue(Modifier.isFinal(ModelInvocationAdmission.class.getModifiers()));
        assertEquals(0, ModelInvocationAdmission.class.getDeclaredFields().length);
        assertArrayEquals(new Class<?>[0], ModelInvocationAdmission.class.getInterfaces());

        Method evaluate = ModelInvocationAdmission.class.getDeclaredMethod(
                "evaluate",
                ProfiledModelRequest.class,
                ApprovedTask.class,
                ExecutionPolicy.class,
                String.class);
        assertEquals(ModelInvocationAdmissionDecision.class, evaluate.getReturnType());
        assertTrue(Modifier.isPublic(evaluate.getModifiers()));

        assertTrue(ModelInvocationAdmissionDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                    ModelInvocationAdmissionDecision.Admitted.class,
                    ModelInvocationAdmissionDecision.Rejected.class
                },
                ModelInvocationAdmissionDecision.class.getPermittedSubclasses());
        assertRecordShape(
                ModelInvocationAdmissionDecision.Admitted.class,
                new String[] {"profiledRequest"},
                new Class<?>[] {ProfiledModelRequest.class});
        assertRecordShape(
                ModelInvocationAdmissionDecision.Rejected.class,
                new String[] {"reason"},
                new Class<?>[] {ModelInvocationRejectionReason.class});
        assertArrayEquals(
                new ModelInvocationRejectionReason[] {
                    ModelInvocationRejectionReason.TASK_TOOL_NOT_ALLOWED,
                    ModelInvocationRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                    ModelInvocationRejectionReason.REQUIRED_CAPABILITY_MISMATCH,
                    ModelInvocationRejectionReason.GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                    ModelInvocationRejectionReason.OUTBOUND_POLICY_REQUIRED
                },
                ModelInvocationRejectionReason.values());
    }

    private static void assertRecordShape(
            Class<?> type,
            String[] componentNames,
            Class<?>[] componentTypes) {
        assertTrue(type.isRecord());
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertArrayEquals(
                componentNames,
                Arrays.stream(type.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                componentTypes,
                Arrays.stream(type.getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));
    }

    private static void assertAdmitted(ProfiledModelRequest profiledRequest) {
        ModelInvocationAdmissionDecision decision = new ModelInvocationAdmission().evaluate(
                profiledRequest,
                approvedTask(Set.of(ModelInvokeTool.NAME)),
                executionPolicy(
                        Set.of(ModelInvokeTool.NAME), Set.of(), Duration.ofSeconds(31)),
                REQUIRED_CAPABILITY);
        ModelInvocationAdmissionDecision.Admitted admitted = assertInstanceOf(
                ModelInvocationAdmissionDecision.Admitted.class, decision);
        assertSame(profiledRequest, admitted.profiledRequest());
    }

    private static void assertRejected(
            ModelInvocationRejectionReason expectedReason,
            ProfiledModelRequest profiledRequest,
            ApprovedTask approvedTask,
            ExecutionPolicy executionPolicy,
            String authoritativeRequiredCapability) {
        ModelInvocationAdmissionDecision decision = new ModelInvocationAdmission().evaluate(
                profiledRequest,
                approvedTask,
                executionPolicy,
                authoritativeRequiredCapability);
        ModelInvocationAdmissionDecision.Rejected rejected = assertInstanceOf(
                ModelInvocationAdmissionDecision.Rejected.class, decision);
        assertEquals(expectedReason, rejected.reason());
    }

    private static ProfiledModelRequest validProfiledRequest() {
        return profiledRequest(
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                4096,
                2048,
                ModelDataClassification.INTERNAL,
                0);
    }

    private static ProfiledModelRequest profiledRequest(
            ModelLocalityRequirement localityRequirement,
            Duration maximumInvocationTime,
            Duration requestTimeout,
            int maxResponseLength,
            long maxOutputTokens,
            ModelDataClassification dataClassification,
            long maxCostMicrounits) {
        long maxInputTokens = 1;
        long maxTotalTokens = maxInputTokens + maxOutputTokens;
        ModelExecutionProfile profile = new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                REQUIRED_CAPABILITY,
                MODEL_CLASS,
                localityRequirement,
                ModelReasoningRequirement.STANDARD,
                maxTotalTokens,
                new ModelTokenBudget(maxInputTokens, maxOutputTokens, maxTotalTokens),
                new ModelCostBudget("USD", maxCostMicrounits),
                maximumInvocationTime,
                dataClassification);
        ModelRequest request = new ModelRequest(
                "correlation-1",
                "Analyze the repository.",
                MODEL_CLASS,
                requestTimeout,
                maxResponseLength);
        return new ProfiledModelRequest(request, profile);
    }

    private static ApprovedTask approvedTask(Set<String> allowedTools) {
        return new ApprovedTask(
                "admit-model-invocation",
                "Evaluate one complete profiled request.",
                "User continuation authority.",
                allowedTools,
                "CURRENT_TASK.md");
    }

    private static ExecutionPolicy executionPolicy(
            Set<String> allowedTools,
            Set<String> deniedTools,
            Duration timeout) {
        return new ExecutionPolicy(
                Path.of("."),
                allowedTools,
                deniedTools,
                1024,
                timeout,
                CancellationToken.none());
    }
}
