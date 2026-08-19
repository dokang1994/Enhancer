package com.enhancer.model;

import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.Tool;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Executes one governed model invocation through the provider-neutral gateway port.
 *
 * <p>The tool runs only when the approved task scope and the execution policy both
 * allow {@code model-invoke}; the existing executor enforces isolation, the policy
 * allowlist, cancellation, and the per-tool timeout. The declared gateway timeout
 * must fit strictly inside that policy timeout. Every gateway failure maps to one
 * bounded typed {@link ToolResult} failure, and response text is persisted through
 * the existing evidence envelope as untrusted data that grants no authority.
 */
public final class ModelInvokeTool implements Tool {
    public static final String NAME = "model-invoke";
    public static final String PROMPT_ARGUMENT = "prompt";
    public static final String MODEL_CLASS_ARGUMENT = "model-class";
    public static final String TIMEOUT_MILLIS_ARGUMENT = "timeout-millis";
    public static final String MAX_RESPONSE_LENGTH_ARGUMENT = "max-response-length";

    private final ModelGateway gateway;
    private final EvidenceRecorder evidenceRecorder;

    public ModelInvokeTool(ModelGateway gateway, EvidenceRecorder evidenceRecorder) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
        this.evidenceRecorder = Objects.requireNonNull(
                evidenceRecorder,
                "evidenceRecorder must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request, ExecutionPolicy policy) throws IOException {
        ModelRequest modelRequest = modelRequest(request, policy);

        ModelResponse response;
        try {
            response = gateway.invoke(modelRequest);
        } catch (ModelGatewayException exception) {
            return gatewayFailure(exception);
        }

        VerificationEvidence evidence = evidenceRecorder.capture(
                request.correlationId(),
                "Model invocation succeeded for model class "
                        + response.modelClass(),
                response.text());
        return new ToolResult(
                NAME,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                evidence);
    }

    private ModelRequest modelRequest(ToolRequest request, ExecutionPolicy policy) {
        String prompt = requiredArgument(request, PROMPT_ARGUMENT);
        String modelClass = requiredArgument(request, MODEL_CLASS_ARGUMENT);
        Duration timeout = Duration.ofMillis(
                numericArgument(request, TIMEOUT_MILLIS_ARGUMENT));
        int maxResponseLength = Math.toIntExact(
                numericArgument(request, MAX_RESPONSE_LENGTH_ARGUMENT));

        if (timeout.compareTo(policy.timeout()) >= 0) {
            throw new IllegalArgumentException(
                    "gateway timeout must fit strictly inside the policy timeout "
                            + policy.timeout());
        }
        return new ModelRequest(
                request.correlationId(),
                prompt,
                modelClass,
                timeout,
                maxResponseLength);
    }

    private static String requiredArgument(ToolRequest request, String name) {
        String value = request.arguments().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " argument is required");
        }
        return value;
    }

    private static long numericArgument(ToolRequest request, String name) {
        String value = requiredArgument(request, name);
        long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " argument must be a positive integer",
                    exception);
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException(
                    name + " argument must be a positive integer");
        }
        return parsed;
    }

    private static ToolResult gatewayFailure(ModelGatewayException exception) {
        ToolFailureCode failureCode = switch (exception.code()) {
            case TIMED_OUT -> ToolFailureCode.TIMED_OUT;
            case PROVIDER_UNAVAILABLE -> ToolFailureCode.TEMPORARY_FAILURE;
            case RESPONSE_INVALID -> ToolFailureCode.INVALID_RESULT;
            case BUDGET_EXCEEDED -> ToolFailureCode.TOOL_REPORTED_FAILURE;
        };
        return new ToolResult(
                NAME,
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(failureCode),
                VerificationEvidence.capture(
                        "Model invocation failed",
                        "model failure " + exception.code().name()
                                + ": " + exception.getMessage(),
                        Optional.empty()));
    }
}
