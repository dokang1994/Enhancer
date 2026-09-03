package com.enhancer.runtime;

import com.enhancer.model.DeterministicFakeExactRequestDecision;
import com.enhancer.model.DeterministicFakeExactRequestInvocationResult;
import com.enhancer.model.ModelFailureCode;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelRequest;
import com.enhancer.model.ModelResponse;
import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.EvidenceRunNamespaceStore;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.Tool;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

/** One-shot materialization of an already returned deterministic-fake outcome. */
final class DeterministicFakeReturnedOutcomeTool implements Tool {
    private final DeterministicFakeExactRequestInvocationResult returnedOutcome;
    private final DeterministicFakeExactRequestDecision.Ready ready;
    private final ToolRequest request;
    private final EvidenceRunNamespaceStore evidenceStore;
    private final EvidenceRecorder evidenceRecorder;
    private final AtomicBoolean used = new AtomicBoolean();

    DeterministicFakeReturnedOutcomeTool(
            DeterministicFakeExactRequestInvocationResult returnedOutcome,
            String promptPath,
            EvidenceRunNamespaceStore evidenceStore) {
        this.returnedOutcome = requireReturnedOutcome(returnedOutcome);
        this.ready = ready(returnedOutcome);
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore, "evidenceStore must not be null");
        this.evidenceRecorder = new EvidenceRecorder(evidenceStore);
        ModelRequest modelRequest = modelRequest(ready);
        this.request = new ToolRequest(
                ModelInvokeTool.NAME,
                modelRequest.correlationId(),
                Map.of(
                        ModelInvokeTool.PROMPT_PATH_ARGUMENT,
                        Objects.requireNonNull(promptPath, "promptPath must not be null"),
                        ModelInvokeTool.MODEL_CLASS_ARGUMENT,
                        modelRequest.modelClass(),
                        ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT,
                        Long.toString(modelRequest.timeout().toMillis()),
                        ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT,
                        Integer.toString(modelRequest.maxResponseLength())));
    }

    ToolRequest request() {
        return request;
    }

    @Override
    public String name() {
        return ModelInvokeTool.NAME;
    }

    @Override
    public ToolResult execute(ToolRequest actualRequest, ExecutionPolicy actualPolicy)
            throws IOException {
        if (!used.compareAndSet(false, true)) {
            throw new IllegalStateException("returned model outcome Tool is one-shot");
        }
        if (actualRequest != request) {
            throw new IllegalArgumentException("exact Tool request instance is required");
        }
        if (actualPolicy != ready.executionPolicy()) {
            throw new IllegalArgumentException("exact execution policy instance is required");
        }

        if (returnedOutcome
                instanceof DeterministicFakeExactRequestInvocationResult.GatewayFailed failed) {
            return failure(map(failed.failureCode()));
        }

        DeterministicFakeExactRequestInvocationResult.Succeeded succeeded =
                (DeterministicFakeExactRequestInvocationResult.Succeeded) returnedOutcome;
        ModelResponse response = succeeded.response();
        if (!validStructure(response, ready)) {
            return failure(ToolFailureCode.INVALID_RESULT);
        }
        if (response.text().length() > VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS) {
            evidenceStore.ensureRun(request.correlationId());
        }
        return new ToolResult(
                ModelInvokeTool.NAME,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                evidenceRecorder.capture(
                        request.correlationId(),
                        "Model invocation returned a structurally valid response",
                        response.text()));
    }

    static ToolResult sanitize(ToolResult result) {
        Objects.requireNonNull(result, "result must not be null");
        if (result.status() == ToolResultStatus.SUCCESS) {
            return result;
        }
        return failure(result.failureCode().orElseThrow());
    }

    private static DeterministicFakeExactRequestInvocationResult requireReturnedOutcome(
            DeterministicFakeExactRequestInvocationResult outcome) {
        Objects.requireNonNull(outcome, "returnedOutcome must not be null");
        if (outcome instanceof DeterministicFakeExactRequestInvocationResult.Refused) {
            throw new IllegalArgumentException(
                    "a refused pre-call outcome cannot be materialized");
        }
        return outcome;
    }

    private static DeterministicFakeExactRequestDecision.Ready ready(
            DeterministicFakeExactRequestInvocationResult outcome) {
        if (outcome instanceof DeterministicFakeExactRequestInvocationResult.Succeeded succeeded) {
            return succeeded.ready();
        }
        return ((DeterministicFakeExactRequestInvocationResult.GatewayFailed) outcome).ready();
    }

    private static ModelRequest modelRequest(
            DeterministicFakeExactRequestDecision.Ready ready) {
        return ready.suitable().admitted().profiledRequest().request();
    }

    private static boolean validStructure(
            ModelResponse response,
            DeterministicFakeExactRequestDecision.Ready ready) {
        ModelRequest request = modelRequest(ready);
        return response.modelClass().equals(request.modelClass())
                && response.text().length() == ready.predictedResponseUtf16Length()
                && response.usage().inputUnits() == request.prompt().length()
                && response.usage().outputUnits() == response.text().length()
                && response.text().length() <= request.maxResponseLength();
    }

    private static ToolFailureCode map(ModelFailureCode code) {
        return switch (code) {
            case TIMED_OUT -> ToolFailureCode.TIMED_OUT;
            case PROVIDER_UNAVAILABLE -> ToolFailureCode.TEMPORARY_FAILURE;
            case RESPONSE_INVALID -> ToolFailureCode.INVALID_RESULT;
            case BUDGET_EXCEEDED -> ToolFailureCode.TOOL_REPORTED_FAILURE;
        };
    }

    private static ToolResult failure(ToolFailureCode code) {
        return new ToolResult(
                ModelInvokeTool.NAME,
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(code),
                VerificationEvidence.capture(
                        "Model result materialization failed",
                        "tool-failure-code=" + code.name(),
                        Optional.empty()));
    }
}
