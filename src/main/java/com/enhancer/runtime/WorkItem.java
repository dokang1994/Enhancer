package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable Scheduler admission of one existing Gate 7 work envelope. The retained envelope is
 * the source of run, task, snapshot, and Tool-scope provenance; this value creates no authority.
 */
public record WorkItem(
        String workItemId,
        String requiredCapability,
        MessageEnvelope workMessage) {

    public static final int MAX_CAPABILITY_CHARACTERS = 256;

    public WorkItem {
        workItemId = canonicalUuid(workItemId);
        requiredCapability = boundedCapability(requiredCapability);
        Objects.requireNonNull(workMessage, "workMessage must not be null");
        if (!(workMessage.payload() instanceof WorkPayload)
                && !(workMessage.payload() instanceof ModelWorkPayload)) {
            throw new IllegalArgumentException(
                    "workMessage must carry a WorkPayload or ModelWorkPayload");
        }
        if (workItemId.equals(workMessage.messageId())) {
            throw new IllegalArgumentException(
                    "workItemId must not equal the work message identity");
        }
    }

    public String logicalRunId() {
        return workMessage.logicalRunId();
    }

    public ApprovedTaskRevision taskRevision() {
        if (workMessage.payload() instanceof WorkPayload payload) {
            return payload.taskRevision();
        }
        return modelWorkPayload().taskRevision();
    }

    public String snapshotId() {
        if (workMessage.payload() instanceof WorkPayload payload) {
            return payload.snapshotId();
        }
        return modelWorkPayload().snapshotId();
    }

    public Set<String> allowedTools() {
        if (workMessage.payload() instanceof WorkPayload payload) {
            return payload.allowedTools();
        }
        return modelWorkPayload().allowedTools();
    }

    public java.util.Optional<WorkPayload.ExecutionInput> executionInput() {
        return workMessage.payload() instanceof WorkPayload payload
                ? payload.executionInput()
                : java.util.Optional.empty();
    }

    public java.util.Optional<ModelWorkPayload.ModelInvocationExecutionInput>
            modelExecutionInput() {
        return workMessage.payload() instanceof ModelWorkPayload payload
                ? java.util.Optional.of(payload.executionInput())
                : java.util.Optional.empty();
    }

    public boolean isModelWork() {
        return workMessage.payload() instanceof ModelWorkPayload;
    }

    private ModelWorkPayload modelWorkPayload() {
        return (ModelWorkPayload) workMessage.payload();
    }

    private static String canonicalUuid(String value) {
        Objects.requireNonNull(value, "workItemId must not be null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException("workItemId must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "workItemId must be a canonical UUID",
                    exception);
        }
        return value;
    }

    private static String boundedCapability(String value) {
        Objects.requireNonNull(value, "requiredCapability must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "requiredCapability must not be blank");
        }
        if (value.length() > MAX_CAPABILITY_CHARACTERS) {
            throw new IllegalArgumentException(
                    "requiredCapability must not exceed "
                            + MAX_CAPABILITY_CHARACTERS + " characters");
        }
        return value;
    }
}
