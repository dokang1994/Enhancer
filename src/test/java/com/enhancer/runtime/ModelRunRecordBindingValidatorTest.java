package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.model.ModelRequest;
import com.enhancer.run.ModelRunRecord;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.ResolvedModelRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelRunRecordBindingValidatorTest {
    @TempDir
    Path projectRoot;

    @Test
    void acceptsOnlyTheExactDeterministicIdentityAndCompleteModelBinding()
            throws Exception {
        ModelProcessValidationTestFixture.Prepared fixture =
                ModelProcessValidationTestFixture.valid(projectRoot);
        ModelRunRecordBindingValidator validator =
                new ModelRunRecordBindingValidator(fixture.evidenceStore());

        VerificationStatus status = validator.requireBinding(
                fixture.resolved(),
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                projectRoot,
                fixture.configuration());

        assertEquals(VerificationStatus.VERIFIED, status);
    }

    @Test
    void rejectsIndependentCapabilityAndPolicyDrift() throws Exception {
        ModelProcessValidationTestFixture.Prepared fixture =
                ModelProcessValidationTestFixture.valid(projectRoot);
        ModelRunRecord original = fixture.resolved().record();
        ModelRunRecordBindingValidator validator =
                new ModelRunRecordBindingValidator(fixture.evidenceStore());
        ResolvedModelRunRecord capabilityDrift = new ResolvedModelRunRecord(
                fixture.resolved().metadata(),
                new ModelRunRecord(
                        original.workItemId(),
                        "foreign-capability",
                        original.workMessage(),
                        original.modelRequest(),
                        original.lifecycleRecord()));

        IOException capabilityFailure = assertThrows(
                IOException.class,
                () -> validator.requireBinding(
                        capabilityDrift,
                        ModelAttemptTestFixture.GOAL_ID,
                        ModelAttemptTestFixture.AGENT_RUN_ID,
                        fixture.workItem(),
                        projectRoot,
                        fixture.configuration()));
        assertTrue(capabilityFailure.getMessage().contains("capability"));

        RunRecord lifecycle = original.lifecycleRecord();
        RunRecord changedPolicy = new RunRecord(
                lifecycle.logicalRunId(),
                lifecycle.recordedAt(),
                lifecycle.approvedTask(),
                lifecycle.toolRequest(),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        lifecycle.policyDecision().projectRoot(),
                        Set.of("model-invoke"),
                        Set.of("foreign-tool"),
                        lifecycle.policyDecision().maxReadBytes(),
                        lifecycle.policyDecision().timeoutMillis()),
                lifecycle.toolResult(),
                lifecycle.expectedContentSha256(),
                lifecycle.verification(),
                lifecycle.iterations(),
                lifecycle.workerStopReason(),
                lifecycle.finalStopReason());
        ResolvedModelRunRecord policyDrift = new ResolvedModelRunRecord(
                fixture.resolved().metadata(),
                new ModelRunRecord(
                        original.workItemId(),
                        original.requiredCapability(),
                        original.workMessage(),
                        original.modelRequest(),
                        changedPolicy));

        IOException policyFailure = assertThrows(
                IOException.class,
                () -> validator.requireBinding(
                        policyDrift,
                        ModelAttemptTestFixture.GOAL_ID,
                        ModelAttemptTestFixture.AGENT_RUN_ID,
                        fixture.workItem(),
                        projectRoot,
                        fixture.configuration()));
        assertTrue(policyFailure.getMessage().contains("policy"));
    }

    @Test
    void configurationSnapshotsDeniedToolsAndRequiresMillisecondPrecision() {
        java.util.LinkedHashSet<String> denied = new java.util.LinkedHashSet<>();
        denied.add("read-file");
        ModelProcessExecutionConfiguration configuration =
                new ModelProcessExecutionConfiguration(
                        ModelProcessValidationTestFixture.LIMITS,
                        denied,
                        ModelProcessValidationTestFixture.MAXIMUM_READ_BYTES,
                        ModelProcessValidationTestFixture.TOOL_TIMEOUT);
        denied.add("model-invoke");

        assertEquals(Set.of("read-file"), configuration.deniedTools());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ModelProcessExecutionConfiguration(
                        ModelProcessValidationTestFixture.LIMITS,
                        Set.of(),
                        ModelProcessValidationTestFixture.MAXIMUM_READ_BYTES,
                        java.time.Duration.ofNanos(1_000_001)));
    }

    @Test
    void refusesACompletedRecordWhenExplicitConfigurationDeniesModelInvoke()
            throws Exception {
        ModelProcessValidationTestFixture.Prepared fixture =
                ModelProcessValidationTestFixture.valid(projectRoot);
        ModelRunRecord original = fixture.resolved().record();
        RunRecord lifecycle = original.lifecycleRecord();
        RunRecord forgedAllowed = new RunRecord(
                lifecycle.logicalRunId(),
                lifecycle.recordedAt(),
                lifecycle.approvedTask(),
                lifecycle.toolRequest(),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        lifecycle.policyDecision().projectRoot(),
                        Set.of("model-invoke"),
                        Set.of("model-invoke"),
                        lifecycle.policyDecision().maxReadBytes(),
                        lifecycle.policyDecision().timeoutMillis()),
                lifecycle.toolResult(),
                lifecycle.expectedContentSha256(),
                lifecycle.verification(),
                lifecycle.iterations(),
                lifecycle.workerStopReason(),
                lifecycle.finalStopReason());
        ResolvedModelRunRecord forged = new ResolvedModelRunRecord(
                fixture.resolved().metadata(),
                new ModelRunRecord(
                        original.workItemId(),
                        original.requiredCapability(),
                        original.workMessage(),
                        original.modelRequest(),
                        forgedAllowed));
        ModelProcessExecutionConfiguration denied =
                new ModelProcessExecutionConfiguration(
                        fixture.configuration().invocationLimits(),
                        Set.of("model-invoke"),
                        fixture.configuration().maximumReadBytes(),
                        fixture.configuration().toolTimeout());

        IOException failure = assertThrows(
                IOException.class,
                () -> new ModelRunRecordBindingValidator(fixture.evidenceStore())
                        .requireBinding(
                                forged,
                                ModelAttemptTestFixture.GOAL_ID,
                                ModelAttemptTestFixture.AGENT_RUN_ID,
                                fixture.workItem(),
                                projectRoot,
                                denied));

        assertTrue(failure.getMessage().contains("denied"), failure.getMessage());
    }

    @Test
    void rejectsEvidenceCorrelationAndIndependentVerificationDrift()
            throws Exception {
        ModelProcessValidationTestFixture.Prepared fixture =
                ModelProcessValidationTestFixture.valid(projectRoot);
        ModelRunRecord original = fixture.resolved().record();
        RunRecord lifecycle = original.lifecycleRecord();
        ModelRequest request = new ModelRequest(
                "foreign-evidence-run",
                original.modelRequest().prompt(),
                original.modelRequest().modelClass(),
                original.modelRequest().timeout(),
                original.modelRequest().maxResponseLength());
        RunRecord correlationDrift = copyLifecycle(
                lifecycle,
                new ToolRequest(
                        lifecycle.toolRequest().toolName(),
                        request.correlationId(),
                        lifecycle.toolRequest().arguments()),
                lifecycle.toolResult(),
                lifecycle.expectedContentSha256(),
                lifecycle.verification(),
                lifecycle.workerStopReason(),
                lifecycle.finalStopReason());
        ResolvedModelRunRecord changedCorrelation = resolved(
                fixture,
                new ModelRunRecord(
                        original.workItemId(),
                        original.requiredCapability(),
                        original.workMessage(),
                        request,
                        correlationDrift));
        ModelRunRecordBindingValidator validator =
                new ModelRunRecordBindingValidator(fixture.evidenceStore());

        IOException correlationFailure = assertThrows(
                IOException.class,
                () -> validate(validator, changedCorrelation, fixture));
        assertTrue(correlationFailure.getMessage().contains("correlation"));

        VerificationDecision forgedDecision = VerificationDecision.rejected(
                VerificationCode.CONTENT_MISMATCH, "forged historical decision");
        RunRecord decisionDrift = copyLifecycle(
                lifecycle,
                lifecycle.toolRequest(),
                lifecycle.toolResult(),
                lifecycle.expectedContentSha256(),
                forgedDecision,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.AWAITING_VERIFICATION);
        ResolvedModelRunRecord changedDecision = resolved(
                fixture,
                new ModelRunRecord(
                        original.workItemId(),
                        original.requiredCapability(),
                        original.workMessage(),
                        original.modelRequest(),
                        decisionDrift));

        IOException verificationFailure = assertThrows(
                IOException.class,
                () -> validate(validator, changedDecision, fixture));
        assertTrue(verificationFailure.getMessage().contains("verification"));
    }

    @Test
    void acceptsOnlyCanonicalCodeOnlyFailedLifecycleEvidence()
            throws Exception {
        ModelProcessValidationTestFixture.Prepared fixture =
                ModelProcessValidationTestFixture.valid(projectRoot);
        ModelRunRecord original = fixture.resolved().record();
        RunRecord lifecycle = original.lifecycleRecord();
        String diagnostic = "tool-failure-code=TEMPORARY_FAILURE";
        ToolResult canonicalFailure = new ToolResult(
                lifecycle.toolResult().toolName(),
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(ToolFailureCode.TEMPORARY_FAILURE),
                VerificationEvidence.capture(
                        "Model result materialization failed",
                        diagnostic,
                        Optional.empty()));
        RunRecord failed = copyLifecycle(
                lifecycle,
                lifecycle.toolRequest(),
                canonicalFailure,
                Optional.empty(),
                VerificationDecision.notPerformed(diagnostic),
                AgentLoopStopReason.FAILED,
                AgentLoopStopReason.FAILED);
        ResolvedModelRunRecord canonical = resolved(
                fixture,
                new ModelRunRecord(
                        original.workItemId(),
                        original.requiredCapability(),
                        original.workMessage(),
                        original.modelRequest(),
                        failed));
        ModelRunRecordBindingValidator validator =
                new ModelRunRecordBindingValidator(fixture.evidenceStore());

        assertEquals(VerificationStatus.NOT_PERFORMED,
                validate(validator, canonical, fixture));

        ToolResult rawFailure = new ToolResult(
                canonicalFailure.toolName(),
                canonicalFailure.status(),
                canonicalFailure.exitCode(),
                canonicalFailure.failureCode(),
                VerificationEvidence.capture(
                        "Model result materialization failed",
                        "secret provider diagnostic",
                        Optional.empty()));
        RunRecord raw = copyLifecycle(
                failed,
                failed.toolRequest(),
                rawFailure,
                failed.expectedContentSha256(),
                failed.verification(),
                failed.workerStopReason(),
                failed.finalStopReason());

        IOException failure = assertThrows(
                IOException.class,
                () -> validate(
                        validator,
                        resolved(fixture, new ModelRunRecord(
                                original.workItemId(),
                                original.requiredCapability(),
                                original.workMessage(),
                                original.modelRequest(),
                                raw)),
                        fixture));
        assertTrue(failure.getMessage().contains("sanitized"));
    }

    @Test
    void validatesLongEvidenceOnlyFromTheExactAgentRunNamespace()
            throws Exception {
        Path evidenceRoot = projectRoot.resolve("evidence-store");
        ModelProcessValidationTestFixture.Prepared fixture =
                ModelProcessValidationTestFixture.validWithLongEvidence(
                        projectRoot, evidenceRoot);
        ModelRunRecordBindingValidator validator =
                new ModelRunRecordBindingValidator(fixture.evidenceStore());

        assertEquals(
                VerificationStatus.VERIFIED,
                validate(validator, fixture.resolved(), fixture));

        String reference = fixture.resolved().record().lifecycleRecord()
                .toolResult().evidence().fullOutputReference().orElseThrow();
        String[] identity = reference.split("/");
        Files.writeString(
                evidenceRoot.resolve(identity[1]).resolve(identity[2] + ".evidence"),
                "changed evidence");

        assertThrows(
                IOException.class,
                () -> validate(validator, fixture.resolved(), fixture));
    }

    private VerificationStatus validate(
            ModelRunRecordBindingValidator validator,
            ResolvedModelRunRecord resolved,
            ModelProcessValidationTestFixture.Prepared fixture) throws IOException {
        return validator.requireBinding(
                resolved,
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                fixture.workItem(),
                projectRoot,
                fixture.configuration());
    }

    private static ResolvedModelRunRecord resolved(
            ModelProcessValidationTestFixture.Prepared fixture,
            ModelRunRecord record) {
        return new ResolvedModelRunRecord(fixture.resolved().metadata(), record);
    }

    private static RunRecord copyLifecycle(
            RunRecord source,
            ToolRequest request,
            ToolResult result,
            Optional<String> expectedDigest,
            VerificationDecision verification,
            AgentLoopStopReason workerStop,
            AgentLoopStopReason finalStop) {
        return new RunRecord(
                source.logicalRunId(),
                source.recordedAt(),
                source.approvedTask(),
                request,
                source.policyDecision(),
                result,
                expectedDigest,
                verification,
                source.iterations(),
                workerStop,
                finalStop);
    }
}
