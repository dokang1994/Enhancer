package com.enhancer.bus;

import com.enhancer.verification.VerificationStatus;
import java.util.Objects;

public record ResultPayload(
        String taskId,
        String runRecordReference,
        VerificationStatus verificationStatus) implements MessagePayload {

    public ResultPayload {
        taskId = BusContractSupport.bounded(
                taskId,
                "taskId",
                BusContractSupport.MAX_IDENTITY_CHARACTERS);
        runRecordReference = BusContractSupport.bounded(
                runRecordReference,
                "runRecordReference",
                BusContractSupport.MAX_REFERENCE_CHARACTERS);
        Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    }
}
