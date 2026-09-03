package com.enhancer.runtime;

import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.model.DeterministicFakeExactRequestDecision;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelRequest;
import com.enhancer.run.ModelRunRecord;
import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.RunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.verification.IndependentVerifier;
import com.enhancer.verification.VerificationRequest;
import java.io.IOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Constructs one complete model lifecycle and persists only Model RunRecord v2. */
final class ModelRunRecordFinalizer {
    private final IndependentVerifier verifier;
    private final ModelRunRecordStore recordStore;
    private final Clock clock;

    ModelRunRecordFinalizer(
            IndependentVerifier verifier,
            ModelRunRecordStore recordStore,
            Clock clock) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.recordStore = Objects.requireNonNull(
                recordStore, "recordStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    Published finalizeRun(
            String goalId,
            String agentRunId,
            WorkItem workItem,
            SchedulerModelInvocationPreparation preparation,
            DeterministicFakeExactRequestDecision.Ready ready,
            ToolRequest toolRequest,
            ToolResult toolResult) throws IOException {
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(preparation, "preparation must not be null");
        Objects.requireNonNull(ready, "ready must not be null");
        Objects.requireNonNull(toolRequest, "toolRequest must not be null");
        Objects.requireNonNull(toolResult, "toolResult must not be null");
        requireExactChain(workItem, preparation, ready, toolRequest);

        String expectedDigest = workItem.modelExecutionInput().orElseThrow()
                .expectedResponseSha256();
        VerificationDecision verification;
        Optional<String> lifecycleExpectedDigest;
        AgentLoopStopReason workerStop;
        AgentLoopStopReason finalStop;
        if (toolResult.status() == ToolResultStatus.SUCCESS) {
            verification = Objects.requireNonNull(
                    verifier.verify(new VerificationRequest(
                            preparation.approvedTask(),
                            toolRequest,
                            toolResult,
                            expectedDigest)),
                    "verifier must not return null");
            if (verification.status() == VerificationStatus.NOT_PERFORMED) {
                throw new IllegalStateException(
                        "successful model result requires performed verification");
            }
            lifecycleExpectedDigest = Optional.of(expectedDigest);
            workerStop = AgentLoopStopReason.AWAITING_VERIFICATION;
            finalStop = verification.status() == VerificationStatus.VERIFIED
                    ? AgentLoopStopReason.COMPLETED
                    : AgentLoopStopReason.AWAITING_VERIFICATION;
        } else {
            verification = VerificationDecision.notPerformed(
                    "tool-failure-code=" + toolResult.failureCode().orElseThrow().name());
            lifecycleExpectedDigest = Optional.empty();
            workerStop = AgentLoopStopReason.FAILED;
            finalStop = AgentLoopStopReason.FAILED;
        }

        ModelRequest modelRequest = preparation.profiledRequest().request();
        RunRecord lifecycle = new RunRecord(
                workItem.logicalRunId(),
                clock.instant().truncatedTo(ChronoUnit.MILLIS),
                preparation.approvedTask(),
                toolRequest,
                PolicyDecision.from(preparation.executionPolicy(), toolRequest),
                toolResult,
                lifecycleExpectedDigest,
                verification,
                1,
                workerStop,
                finalStop);
        ModelRunRecord record = new ModelRunRecord(
                workItem.workItemId(),
                workItem.requiredCapability(),
                workItem.workMessage(),
                modelRequest,
                lifecycle);
        StoredRunRecord stored = recordStore.persistModel(
                AgentRunRecordIdentity.recordId(goalId, agentRunId), record);
        return new Published(record, stored);
    }

    private static void requireExactChain(
            WorkItem workItem,
            SchedulerModelInvocationPreparation preparation,
            DeterministicFakeExactRequestDecision.Ready ready,
            ToolRequest toolRequest) {
        if (ready.executionPolicy() != preparation.executionPolicy()
                || ready.suitable().admitted() != preparation.admissionDecision()
                || ready.suitable().admitted().profiledRequest()
                        != preparation.profiledRequest()) {
            throw new IllegalArgumentException(
                    "model finalization requires the exact prepared identity chain");
        }
        ModelRequest request = preparation.profiledRequest().request();
        Map<String, String> expectedArguments = Map.of(
                ModelInvokeTool.PROMPT_PATH_ARGUMENT,
                workItem.modelExecutionInput().orElseThrow().targetPath(),
                ModelInvokeTool.MODEL_CLASS_ARGUMENT,
                request.modelClass(),
                ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT,
                Long.toString(request.timeout().toMillis()),
                ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT,
                Integer.toString(request.maxResponseLength()));
        if (!ModelInvokeTool.NAME.equals(toolRequest.toolName())
                || !request.correlationId().equals(toolRequest.correlationId())
                || !expectedArguments.equals(toolRequest.arguments())) {
            throw new IllegalArgumentException(
                    "model finalization requires the exact prepared Tool request");
        }
    }

    record Published(ModelRunRecord record, StoredRunRecord storedRecord) {
        Published {
            Objects.requireNonNull(record, "record must not be null");
            Objects.requireNonNull(storedRecord, "storedRecord must not be null");
        }
    }
}
