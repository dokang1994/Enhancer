package com.enhancer.runtime;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable identity-only description of an external effect. The semantic digest identifies
 * adapter-owned operation content without retaining that content or credentials in the ledger.
 */
public record ExternalEffectRequest(
        String idempotencyKey,
        String goalId,
        String agentRunId,
        String workItemId,
        String adapterId,
        String operationName,
        String operationSha256) {

    public static final int MAX_KEY_CHARACTERS = 256;
    public static final int MAX_ADAPTER_CHARACTERS = 256;
    public static final int MAX_OPERATION_CHARACTERS = 256;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public ExternalEffectRequest {
        idempotencyKey = boundedNonBlank(
                idempotencyKey, "idempotencyKey", MAX_KEY_CHARACTERS);
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        workItemId = RuntimeIdentity.canonicalUuid(workItemId, "workItemId");
        if (goalId.equals(agentRunId)
                || goalId.equals(workItemId)
                || agentRunId.equals(workItemId)) {
            throw new IllegalArgumentException(
                    "goalId, agentRunId, and workItemId must be distinct");
        }
        adapterId = boundedNonBlank(
                adapterId, "adapterId", MAX_ADAPTER_CHARACTERS);
        operationName = boundedNonBlank(
                operationName, "operationName", MAX_OPERATION_CHARACTERS);
        Objects.requireNonNull(
                operationSha256, "operationSha256 must not be null");
        if (!SHA_256.matcher(operationSha256).matches()) {
            throw new IllegalArgumentException(
                    "operationSha256 must be 64 lowercase hexadecimal characters");
        }
    }

    private static String boundedNonBlank(
            String value,
            String field,
            int maximumCharacters) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (value.length() > maximumCharacters) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maximumCharacters + " characters");
        }
        return value;
    }
}
