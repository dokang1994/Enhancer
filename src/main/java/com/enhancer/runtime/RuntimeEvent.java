package com.enhancer.runtime;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable derived fact about a transition already persisted by its authoritative owner.
 * This value grants no transition, Tool, policy, or publication authority.
 */
public record RuntimeEvent(
        String schemaVersion,
        String eventId,
        RuntimeEventKind kind,
        Instant occurredAt,
        RuntimeEventBinding binding,
        String agentRunId,
        Optional<String> causationId,
        String producerId,
        RuntimeEventDetail detail,
        List<RuntimeEventReference> authoritativeReferences) {

    public static final String SCHEMA_VERSION = "runtime-event-v1";
    public static final int MAX_PRODUCER_CHARACTERS = 256;
    public static final int MIN_REFERENCES = 1;
    public static final int MAX_REFERENCES = 4;
    private static final String IDENTITY_DOMAIN =
            "enhancer/runtime-event/runtime-event-v1";

    public RuntimeEvent {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "runtime event schema version is unsupported");
        }
        eventId = RuntimeIdentity.canonicalUuid(eventId, "eventId");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(binding, "binding must not be null");
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        Objects.requireNonNull(causationId, "causationId must not be null");
        causationId = causationId.map(
                value -> RuntimeIdentity.canonicalUuid(value, "causationId"));
        producerId = RuntimeEventContractSupport.bounded(
                producerId,
                "producerId",
                MAX_PRODUCER_CHARACTERS);
        Objects.requireNonNull(detail, "detail must not be null");
        if (kind != detail.kind()) {
            throw new IllegalArgumentException(
                    "runtime event kind must match its sealed detail");
        }
        Objects.requireNonNull(
                authoritativeReferences,
                "authoritativeReferences must not be null");
        if (authoritativeReferences.size() < MIN_REFERENCES
                || authoritativeReferences.size() > MAX_REFERENCES) {
            throw new IllegalArgumentException(
                    "runtime event must have one through four authoritative references");
        }
        List<RuntimeEventReference> checkedReferences = new ArrayList<>();
        for (RuntimeEventReference reference : authoritativeReferences) {
            checkedReferences.add(Objects.requireNonNull(
                    reference,
                    "authoritativeReferences must not contain null"));
        }
        authoritativeReferences = List.copyOf(checkedReferences);
        String expectedId = deriveEventId(
                kind,
                binding.goalId(),
                agentRunId,
                authoritativeReferences);
        if (!expectedId.equals(eventId)) {
            throw new IllegalArgumentException(
                    "eventId does not match the deterministic event identity");
        }
    }

    public static RuntimeEvent create(
            Instant occurredAt,
            RuntimeEventBinding binding,
            String agentRunId,
            Optional<String> causationId,
            String producerId,
            RuntimeEventDetail detail,
            List<RuntimeEventReference> authoritativeReferences) {
        Objects.requireNonNull(detail, "detail must not be null");
        Objects.requireNonNull(binding, "binding must not be null");
        Objects.requireNonNull(
                authoritativeReferences,
                "authoritativeReferences must not be null");
        String canonicalAgentRunId =
                RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        String eventId = deriveEventId(
                detail.kind(),
                binding.goalId(),
                canonicalAgentRunId,
                authoritativeReferences);
        return new RuntimeEvent(
                SCHEMA_VERSION,
                eventId,
                detail.kind(),
                occurredAt,
                binding,
                canonicalAgentRunId,
                causationId,
                producerId,
                detail,
                authoritativeReferences);
    }

    static String deriveEventId(
            RuntimeEventKind kind,
            String goalId,
            String agentRunId,
            List<RuntimeEventReference> references) {
        Objects.requireNonNull(kind, "kind must not be null");
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(
                goalId,
                "goalId");
        String canonicalAgentRunId = RuntimeIdentity.canonicalUuid(
                agentRunId,
                "agentRunId");
        Objects.requireNonNull(references, "references must not be null");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        RuntimeEventContractSupport.putFramed(digest, IDENTITY_DOMAIN);
        RuntimeEventContractSupport.putFramed(digest, kind.name());
        RuntimeEventContractSupport.putFramed(digest, canonicalGoalId);
        RuntimeEventContractSupport.putFramed(digest, canonicalAgentRunId);
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(references.size())
                .array());
        for (RuntimeEventReference reference : references) {
            RuntimeEventReference checked = Objects.requireNonNull(
                    reference,
                    "references must not contain null");
            RuntimeEventContractSupport.putFramed(
                    digest,
                    checked.kind().name());
            RuntimeEventContractSupport.putFramed(
                    digest,
                    checked.reference());
            digest.update((byte) (checked.sha256().isPresent() ? 1 : 0));
            checked.sha256().ifPresent(
                    value -> RuntimeEventContractSupport.putFramed(digest, value));
        }
        byte[] hash = digest.digest();
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
        ByteBuffer uuidBytes = ByteBuffer.wrap(hash);
        return new UUID(uuidBytes.getLong(), uuidBytes.getLong()).toString();
    }
}
