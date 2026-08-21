package com.enhancer.model;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ExecutionPolicy;
import java.util.Objects;

/**
 * Stateless pure admission boundary for one complete profiled model request.
 *
 * <p>An admitted result is local eligibility only. It grants no gateway, provider,
 * network, transmission, credential, task, Tool, or spend authority.
 */
public final class ModelInvocationAdmission {

    /** Returns the first matching RFC-0016 rejection or the exact admitted input. */
    public ModelInvocationAdmissionDecision evaluate(
            ProfiledModelRequest profiledRequest,
            ApprovedTask approvedTask,
            ExecutionPolicy executionPolicy,
            String authoritativeRequiredCapability) {
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");
        Objects.requireNonNull(
                authoritativeRequiredCapability,
                "authoritativeRequiredCapability must not be null");

        if (!approvedTask.allows(ModelInvokeTool.NAME)) {
            return rejected(ModelInvocationRejectionReason.TASK_TOOL_NOT_ALLOWED);
        }
        if (!executionPolicy.allows(ModelInvokeTool.NAME)) {
            return rejected(
                    ModelInvocationRejectionReason.EXECUTION_POLICY_TOOL_NOT_ALLOWED);
        }
        if (!authoritativeRequiredCapability.equals(
                profiledRequest.executionProfile().requiredCapability())) {
            return rejected(
                    ModelInvocationRejectionReason.REQUIRED_CAPABILITY_MISMATCH);
        }
        if (profiledRequest.request().timeout().compareTo(executionPolicy.timeout()) >= 0) {
            return rejected(
                    ModelInvocationRejectionReason
                            .GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY);
        }
        if (profiledRequest.executionProfile().localityRequirement()
                == ModelLocalityRequirement.POLICY_CONSTRAINED) {
            return rejected(ModelInvocationRejectionReason.OUTBOUND_POLICY_REQUIRED);
        }
        return new ModelInvocationAdmissionDecision.Admitted(profiledRequest);
    }

    private static ModelInvocationAdmissionDecision rejected(
            ModelInvocationRejectionReason reason) {
        return new ModelInvocationAdmissionDecision.Rejected(reason);
    }
}
