package com.enhancer.runtime;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.ModelInvocationAdmissionDecision;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.tool.ExecutionPolicy;
import java.util.Objects;

/** Invocation-local preparation that stops before candidate suitability or execution. */
public record SchedulerModelInvocationPreparation(
        ApprovedTask approvedTask,
        ExecutionPolicy executionPolicy,
        ProfiledModelRequest profiledRequest,
        ModelInvocationAdmissionDecision admissionDecision) {

    public SchedulerModelInvocationPreparation {
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");
        Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        Objects.requireNonNull(admissionDecision, "admissionDecision must not be null");
        if (admissionDecision instanceof ModelInvocationAdmissionDecision.Admitted admitted
                && admitted.profiledRequest() != profiledRequest) {
            throw new IllegalArgumentException(
                    "admitted decision must retain the exact profiledRequest instance");
        }
    }
}
