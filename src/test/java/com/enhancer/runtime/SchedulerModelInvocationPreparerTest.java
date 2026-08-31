package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.GovernedModelPromptReader;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelInvocationAdmission;
import com.enhancer.model.ModelInvocationAdmissionDecision;
import com.enhancer.model.ModelInvocationRejectionReason;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

class SchedulerModelInvocationPreparerTest {
    private static final String MESSAGE_ID = "00000000-0000-0000-0000-000000000f01";
    private static final String WORK_ITEM_ID = "00000000-0000-0000-0000-000000000f02";
    private static final String CAPABILITY = "repository-analysis";
    private static final String MODEL_CLASS = "reasoning-standard";
    private static final String TARGET = "prompts/model.txt";
    private static final String CORRELATION = "attempt-evidence-run";

    @TempDir
    Path projectRoot;

    @Test
    void invocationLimitsRetainOnlyExplicitExistingModelRequestBounds() {
        SchedulerModelInvocationLimits limits = new SchedulerModelInvocationLimits(
                Duration.ofSeconds(4), 65_536);

        assertEquals(Duration.ofSeconds(4), limits.gatewayTimeout());
        assertEquals(65_536, limits.maximumResponseCharacters());
        assertEquals(2, SchedulerModelInvocationLimits.class.getRecordComponents().length);
        assertThrows(NullPointerException.class,
                () -> new SchedulerModelInvocationLimits(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerModelInvocationLimits(Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerModelInvocationLimits(Duration.ofNanos(1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerModelInvocationLimits(
                        ModelRequest.MAX_TIMEOUT.plusMillis(1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerModelInvocationLimits(Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerModelInvocationLimits(
                        Duration.ofSeconds(1), ModelRequest.MAX_RESPONSE_LENGTH + 1));
    }

    @Test
    void preparesOneExactFreshRequestPolicyAndAdmissionDecision() throws IOException {
        ExactActiveTaskResolver taskResolver = mock(ExactActiveTaskResolver.class);
        GovernedModelPromptReader promptReader = mock(GovernedModelPromptReader.class);
        ModelInvocationAdmission admission = mock(ModelInvocationAdmission.class);
        ApprovedTask approvedTask = approvedTask(Set.of("model-invoke", "read-file"));
        ModelExecutionProfile profile = profile(
                CAPABILITY, ModelLocalityRequirement.LOCAL_ONLY, Duration.ofSeconds(3));
        WorkItem workItem = modelWork(CAPABILITY, profile, Set.of("model-invoke", "read-file"));
        when(taskResolver.resolve(projectRoot, workItem)).thenReturn(approvedTask);
        when(promptReader.readFile(eq(TARGET), any(ExecutionPolicy.class)))
                .thenReturn("one-read prompt");
        when(admission.evaluate(any(), same(approvedTask), any(), eq(CAPABILITY)))
                .thenAnswer(invocation -> new ModelInvocationAdmissionDecision.Admitted(
                        invocation.getArgument(0)));
        SchedulerModelInvocationPreparer preparer = new SchedulerModelInvocationPreparer(
                taskResolver, promptReader, admission);
        CancellationToken cancellationToken = () -> false;

        SchedulerModelInvocationPreparation prepared = preparer.prepare(
                projectRoot,
                workItem,
                CORRELATION,
                new SchedulerModelInvocationLimits(Duration.ofSeconds(4), 65_536),
                Set.of("write-file"),
                8_192,
                Duration.ofSeconds(5),
                cancellationToken);

        assertSame(approvedTask, prepared.approvedTask());
        assertEquals(projectRoot.toAbsolutePath().normalize(),
                prepared.executionPolicy().projectRoot());
        assertEquals(Set.of("model-invoke"), prepared.executionPolicy().allowedTools());
        assertEquals(Set.of("write-file"), prepared.executionPolicy().deniedTools());
        assertEquals(8_192, prepared.executionPolicy().maxReadBytes());
        assertEquals(Duration.ofSeconds(5), prepared.executionPolicy().timeout());
        assertSame(cancellationToken, prepared.executionPolicy().cancellationToken());
        assertSame(profile, prepared.profiledRequest().executionProfile());
        assertEquals(CORRELATION, prepared.profiledRequest().request().correlationId());
        assertEquals("one-read prompt", prepared.profiledRequest().request().prompt());
        assertEquals(MODEL_CLASS, prepared.profiledRequest().request().modelClass());
        assertEquals(Duration.ofSeconds(4), prepared.profiledRequest().request().timeout());
        assertEquals(65_536, prepared.profiledRequest().request().maxResponseLength());
        ModelInvocationAdmissionDecision.Admitted admitted = assertInstanceOf(
                ModelInvocationAdmissionDecision.Admitted.class,
                prepared.admissionDecision());
        assertSame(prepared.profiledRequest(), admitted.profiledRequest());

        InOrder order = inOrder(taskResolver, promptReader, admission);
        order.verify(taskResolver).resolve(projectRoot, workItem);
        order.verify(promptReader).readFile(TARGET, prepared.executionPolicy());
        order.verify(admission).evaluate(
                same(prepared.profiledRequest()),
                same(approvedTask),
                same(prepared.executionPolicy()),
                eq(CAPABILITY));
    }

    @Test
    void reevaluatesAndRereadsOncePerAttemptWithoutCaching() throws IOException {
        ExactActiveTaskResolver taskResolver = mock(ExactActiveTaskResolver.class);
        GovernedModelPromptReader promptReader = mock(GovernedModelPromptReader.class);
        ApprovedTask approvedTask = approvedTask(Set.of("model-invoke"));
        WorkItem workItem = modelWork(
                CAPABILITY,
                profile(CAPABILITY, ModelLocalityRequirement.LOCAL_ONLY, Duration.ofSeconds(3)),
                Set.of("model-invoke"));
        when(taskResolver.resolve(projectRoot, workItem)).thenReturn(approvedTask);
        when(promptReader.readFile(eq(TARGET), any(ExecutionPolicy.class)))
                .thenReturn("first snapshot", "second snapshot");
        SchedulerModelInvocationPreparer preparer = new SchedulerModelInvocationPreparer(
                taskResolver, promptReader, new ModelInvocationAdmission());

        SchedulerModelInvocationPreparation first = prepare(preparer, workItem, Set.of(), 5);
        SchedulerModelInvocationPreparation second = prepare(preparer, workItem, Set.of(), 5);

        assertEquals("first snapshot", first.profiledRequest().request().prompt());
        assertEquals("second snapshot", second.profiledRequest().request().prompt());
        assertNotSame(first.executionPolicy(), second.executionPolicy());
        assertNotSame(first.profiledRequest(), second.profiledRequest());
        assertNotSame(first.admissionDecision(), second.admissionDecision());
        verify(taskResolver, times(2)).resolve(projectRoot, workItem);
        verify(promptReader, times(2)).readFile(eq(TARGET), any(ExecutionPolicy.class));
    }

    @Test
    void preservesEveryRfc0016RejectionReason() throws IOException {
        assertRejected(
                ModelInvocationRejectionReason.TASK_TOOL_NOT_ALLOWED,
                approvedTask(Set.of("read-file")),
                modelWork(CAPABILITY, localProfile(CAPABILITY), Set.of("model-invoke")),
                Set.of(),
                5);
        assertRejected(
                ModelInvocationRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED,
                approvedTask(Set.of("model-invoke")),
                modelWork(CAPABILITY, localProfile(CAPABILITY), Set.of("model-invoke")),
                Set.of("model-invoke"),
                5);
        assertRejected(
                ModelInvocationRejectionReason.REQUIRED_CAPABILITY_MISMATCH,
                approvedTask(Set.of("model-invoke")),
                modelWork("different-capability", localProfile(CAPABILITY), Set.of("model-invoke")),
                Set.of(),
                5);
        assertRejected(
                ModelInvocationRejectionReason.GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
                approvedTask(Set.of("model-invoke")),
                modelWork(CAPABILITY, localProfile(CAPABILITY), Set.of("model-invoke")),
                Set.of(),
                4);
        assertRejected(
                ModelInvocationRejectionReason.OUTBOUND_POLICY_REQUIRED,
                approvedTask(Set.of("model-invoke")),
                modelWork(
                        CAPABILITY,
                        profile(
                                CAPABILITY,
                                ModelLocalityRequirement.POLICY_CONSTRAINED,
                                Duration.ofSeconds(3)),
                        Set.of("model-invoke")),
                Set.of(),
                5);
    }

    @Test
    void rejectsLegacyWorkBeforePromptOrAdmissionActivity() {
        GovernedModelPromptReader promptReader = mock(GovernedModelPromptReader.class);
        ModelInvocationAdmission admission = mock(ModelInvocationAdmission.class);
        SchedulerModelInvocationPreparer preparer = new SchedulerModelInvocationPreparer(
                new ExactActiveTaskResolver(
                        new com.enhancer.context.ProjectContextReader(),
                        new com.enhancer.loop.ApprovedTaskReader()),
                promptReader,
                admission);
        WorkItem legacy = new WorkItem(
                WORK_ITEM_ID,
                CAPABILITY,
                envelope(new WorkPayload(
                        new ApprovedTaskRevision("task", "CURRENT_TASK.md", "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("model-invoke"))));

        ActiveTaskMismatchException exception = assertThrows(
                ActiveTaskMismatchException.class,
                () -> prepare(preparer, legacy, Set.of(), 5));

        assertEquals(ActiveTaskMismatchException.Reason.NOT_MODEL_WORK, exception.reason());
        verifyNoInteractions(promptReader, admission);
    }

    @Test
    void rfc0015FailureStopsBeforeAdmissionWithoutClamping() throws IOException {
        ExactActiveTaskResolver taskResolver = mock(ExactActiveTaskResolver.class);
        GovernedModelPromptReader promptReader = mock(GovernedModelPromptReader.class);
        ModelInvocationAdmission admission = mock(ModelInvocationAdmission.class);
        ApprovedTask approvedTask = approvedTask(Set.of("model-invoke"));
        WorkItem workItem = modelWork(
                CAPABILITY,
                profile(CAPABILITY, ModelLocalityRequirement.LOCAL_ONLY, Duration.ofSeconds(5)),
                Set.of("model-invoke"));
        when(taskResolver.resolve(projectRoot, workItem)).thenReturn(approvedTask);
        when(promptReader.readFile(eq(TARGET), any(ExecutionPolicy.class)))
                .thenReturn("prompt");
        SchedulerModelInvocationPreparer preparer = new SchedulerModelInvocationPreparer(
                taskResolver, promptReader, admission);

        assertThrows(
                IllegalArgumentException.class,
                () -> prepare(preparer, workItem, Set.of(), 6));

        verify(promptReader).readFile(eq(TARGET), any(ExecutionPolicy.class));
        verify(admission, never()).evaluate(any(), any(), any(), any());
    }

    private void assertRejected(
            ModelInvocationRejectionReason expected,
            ApprovedTask approvedTask,
            WorkItem workItem,
            Set<String> deniedTools,
            int toolTimeoutSeconds) throws IOException {
        ExactActiveTaskResolver taskResolver = mock(ExactActiveTaskResolver.class);
        GovernedModelPromptReader promptReader = mock(GovernedModelPromptReader.class);
        when(taskResolver.resolve(projectRoot, workItem)).thenReturn(approvedTask);
        when(promptReader.readFile(eq(TARGET), any(ExecutionPolicy.class)))
                .thenReturn("prompt");
        SchedulerModelInvocationPreparer preparer = new SchedulerModelInvocationPreparer(
                taskResolver, promptReader, new ModelInvocationAdmission());

        SchedulerModelInvocationPreparation result = prepare(
                preparer, workItem, deniedTools, toolTimeoutSeconds);

        ModelInvocationAdmissionDecision.Rejected rejected = assertInstanceOf(
                ModelInvocationAdmissionDecision.Rejected.class,
                result.admissionDecision());
        assertEquals(expected, rejected.reason());
    }

    private SchedulerModelInvocationPreparation prepare(
            SchedulerModelInvocationPreparer preparer,
            WorkItem workItem,
            Set<String> deniedTools,
            int toolTimeoutSeconds) throws IOException {
        return preparer.prepare(
                projectRoot,
                workItem,
                CORRELATION,
                new SchedulerModelInvocationLimits(Duration.ofSeconds(4), 65_536),
                deniedTools,
                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES,
                Duration.ofSeconds(toolTimeoutSeconds),
                CancellationToken.none());
    }

    private ApprovedTask approvedTask(Set<String> allowedTools) {
        return new ApprovedTask(
                "task",
                "Prepare the model invocation.",
                "Approved for this task.",
                allowedTools,
                "CURRENT_TASK.md");
    }

    private WorkItem modelWork(
            String requiredCapability,
            ModelExecutionProfile profile,
            Set<String> allowedTools) {
        ModelWorkPayload payload = new ModelWorkPayload(
                new ApprovedTaskRevision("task", "CURRENT_TASK.md", "a".repeat(64)),
                "b".repeat(64),
                allowedTools,
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        TARGET,
                        "c".repeat(64),
                        profile));
        return new WorkItem(WORK_ITEM_ID, requiredCapability, envelope(payload));
    }

    private MessageEnvelope envelope(com.enhancer.bus.MessagePayload payload) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "scheduler-preparation-correlation",
                Optional.empty(),
                "scheduler-preparation-run",
                "scheduler-preparation-test",
                Instant.parse("2026-08-31T05:06:07.008000009Z"),
                payload);
    }

    private ModelExecutionProfile localProfile(String requiredCapability) {
        return profile(
                requiredCapability,
                ModelLocalityRequirement.LOCAL_ONLY,
                Duration.ofSeconds(3));
    }

    private ModelExecutionProfile profile(
            String requiredCapability,
            ModelLocalityRequirement locality,
            Duration maximumInvocationTime) {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                requiredCapability,
                MODEL_CLASS,
                locality,
                ModelReasoningRequirement.STANDARD,
                32_768,
                new ModelTokenBudget(4_096, 2_048, 8_192),
                new ModelCostBudget("USD", 25_000),
                maximumInvocationTime,
                ModelDataClassification.CONFIDENTIAL);
    }
}
