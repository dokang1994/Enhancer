package com.enhancer.runtime;

import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.model.DeterministicModelInvokeVerifier;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelRequest;
import com.enhancer.run.ModelRunRecord;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.ResolvedModelRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.ResolvedEvidence;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.VerificationRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Exact parent-side validation for one deterministic Model RunRecord v2. */
final class ModelRunRecordBindingValidator {
    private final EvidenceStore evidenceStore;

    ModelRunRecordBindingValidator(EvidenceStore evidenceStore) {
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore, "evidenceStore must not be null");
    }

    VerificationStatus requireBinding(
            ResolvedModelRunRecord resolved,
            String goalId,
            String agentRunId,
            WorkItem workItem,
            Path projectRoot,
            ModelProcessExecutionConfiguration configuration) throws IOException {
        Objects.requireNonNull(resolved, "resolved must not be null");
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        if (!workItem.isModelWork()) {
            throw new IOException("model record validation requires typed ModelWork");
        }

        String expectedReference = AgentRunRecordIdentity.reference(goalId, agentRunId);
        String expectedRecordId = AgentRunRecordIdentity.recordId(goalId, agentRunId);
        requireEqual(
                expectedReference,
                resolved.metadata().reference(),
                "record reference");
        requireEqual(
                expectedRecordId,
                resolved.metadata().recordId(),
                "record identity");

        ModelRunRecord record = resolved.record();
        requireEqual(workItem.workItemId(), record.workItemId(), "WorkItem identity");
        requireEqual(
                workItem.requiredCapability(),
                record.requiredCapability(),
                "independent capability");
        requireEqual(
                workItem.workMessage(), record.workMessage(), "complete Work envelope");

        ModelWorkPayload payload = (ModelWorkPayload) workItem.workMessage().payload();
        ModelRequest request = record.modelRequest();
        requireEqual(
                AgentRunEvidenceIdentity.runId(goalId, agentRunId),
                request.correlationId(),
                "evidence correlation identity");
        requireEqual(
                payload.executionInput().executionProfile().modelClass(),
                request.modelClass(),
                "request model class");
        requireEqual(
                configuration.invocationLimits().gatewayTimeout(),
                request.timeout(),
                "request timeout");
        requireEqual(
                configuration.invocationLimits().maximumResponseCharacters(),
                request.maxResponseLength(),
                "request response ceiling");

        RunRecord lifecycle = record.lifecycleRecord();
        if (configuration.deniedTools().contains(ModelInvokeTool.NAME)) {
            throw new IOException(
                    "completed Model RunRecord conflicts with explicitly denied model-invoke");
        }
        PolicyDecision expectedPolicy = new PolicyDecision(
                PolicyDecisionStatus.ALLOWED,
                projectRoot.toAbsolutePath().normalize().toString(),
                Set.of(ModelInvokeTool.NAME),
                configuration.deniedTools(),
                configuration.maximumReadBytes(),
                configuration.toolTimeout().toMillis());
        requireEqual(expectedPolicy, lifecycle.policyDecision(), "execution policy");
        if (lifecycle.iterations() != 1) {
            throw new IOException("model lifecycle must contain exactly one iteration");
        }

        ToolResult result = lifecycle.toolResult();
        if (result.status() == ToolResultStatus.SUCCESS) {
            requireSuccessfulLifecycle(
                    lifecycle,
                    payload.executionInput().expectedResponseSha256(),
                    goalId,
                    agentRunId);
        } else {
            requireFailedLifecycle(lifecycle);
        }
        return lifecycle.verification().status();
    }

    private void requireSuccessfulLifecycle(
            RunRecord lifecycle,
            String expectedDigest,
            String goalId,
            String agentRunId) throws IOException {
        requireEqual(
                Optional.of(expectedDigest),
                lifecycle.expectedContentSha256(),
                "lifecycle expected digest");
        if (lifecycle.verification().status() == VerificationStatus.NOT_PERFORMED) {
            throw new IOException("successful model lifecycle requires performed verification");
        }
        requireEqual(
                AgentLoopStopReason.AWAITING_VERIFICATION,
                lifecycle.workerStopReason(),
                "successful worker stop reason");
        AgentLoopStopReason expectedFinal =
                lifecycle.verification().status() == VerificationStatus.VERIFIED
                        ? AgentLoopStopReason.COMPLETED
                        : AgentLoopStopReason.AWAITING_VERIFICATION;
        requireEqual(
                expectedFinal,
                lifecycle.finalStopReason(),
                "successful final stop reason");
        requireEvidenceNamespace(
                lifecycle.toolResult().evidence(), goalId, agentRunId);

        VerificationDecision fresh = new DeterministicModelInvokeVerifier(evidenceStore)
                .verify(new VerificationRequest(
                        lifecycle.approvedTask(),
                        lifecycle.toolRequest(),
                        lifecycle.toolResult(),
                        expectedDigest));
        requireEqual(fresh, lifecycle.verification(), "independent verification");
    }

    private void requireEvidenceNamespace(
            VerificationEvidence evidence,
            String goalId,
            String agentRunId) throws IOException {
        if (evidence.fullOutputReference().isEmpty()) {
            return;
        }
        String runId = AgentRunEvidenceIdentity.runId(goalId, agentRunId);
        String reference = evidence.fullOutputReference().orElseThrow();
        if (!reference.startsWith("evidence/" + runId + "/")) {
            throw new IOException(
                    "model evidence reference does not match the AgentRun evidence namespace");
        }
        ResolvedEvidence resolved = evidenceStore.resolve(reference);
        requireEqual(runId, resolved.metadata().runId(), "evidence run identity");
        requireEqual(reference, resolved.metadata().reference(), "evidence reference");
    }

    private static void requireFailedLifecycle(RunRecord lifecycle) throws IOException {
        ToolResult result = lifecycle.toolResult();
        ToolFailureCode code = result.failureCode().orElseThrow(() ->
                new IOException("failed model result names no failure code"));
        String diagnostic = "tool-failure-code=" + code.name();
        VerificationEvidence expectedEvidence = VerificationEvidence.capture(
                "Model result materialization failed", diagnostic, Optional.empty());
        requireEqual(expectedEvidence, result.evidence(), "sanitized failure evidence");
        requireEqual(
                VerificationDecision.notPerformed(diagnostic),
                lifecycle.verification(),
                "failed lifecycle verification");
        requireEqual(
                Optional.empty(),
                lifecycle.expectedContentSha256(),
                "failed lifecycle expected digest");
        requireEqual(
                AgentLoopStopReason.FAILED,
                lifecycle.workerStopReason(),
                "failed worker stop reason");
        requireEqual(
                AgentLoopStopReason.FAILED,
                lifecycle.finalStopReason(),
                "failed final stop reason");
    }

    private static void requireEqual(Object expected, Object actual, String label)
            throws IOException {
        if (!Objects.equals(expected, actual)) {
            throw new IOException("the Model RunRecord " + label
                    + " does not match the dispatched work");
        }
    }
}
