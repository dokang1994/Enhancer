package com.enhancer.model;

import com.enhancer.tool.ExecutionPolicy;
import java.util.Objects;

/** Field-free same-identity invocation seam for one prepared deterministic-fake request. */
public final class DeterministicFakeExactRequestInvoker {

    public DeterministicFakeExactRequestInvocationResult invoke(
            DeterministicFakeExactRequestDecision.Ready ready) {
        Objects.requireNonNull(ready, "ready must not be null");

        ModelRequest request = ready.suitable().admitted().profiledRequest().request();
        ExecutionPolicy policy = ready.executionPolicy();
        if (!policy.allows("model-invoke")) {
            return DeterministicFakeExactRequestInvocationResult.refused(
                    ready,
                    DeterministicFakeExactRequestInvocationRejectionReason
                            .EXECUTION_POLICY_TOOL_NOT_ALLOWED);
        }
        if (request.timeout().compareTo(policy.timeout()) >= 0) {
            return DeterministicFakeExactRequestInvocationResult.refused(
                    ready,
                    DeterministicFakeExactRequestInvocationRejectionReason
                            .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY);
        }
        if (policy.cancellationToken().isCancellationRequested()) {
            return DeterministicFakeExactRequestInvocationResult.refused(
                    ready,
                    DeterministicFakeExactRequestInvocationRejectionReason
                            .CANCELLATION_REQUESTED);
        }

        try {
            ModelResponse response =
                    ready.suitable().candidate().gateway().invoke(request);
            return DeterministicFakeExactRequestInvocationResult.succeeded(ready, response);
        } catch (ModelGatewayException exception) {
            return DeterministicFakeExactRequestInvocationResult.gatewayFailed(
                    ready, exception.code());
        }
    }
}
