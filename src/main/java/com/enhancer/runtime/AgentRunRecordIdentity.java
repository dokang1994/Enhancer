package com.enhancer.runtime;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

/**
 * Versioned deterministic RunRecord identity for one durable Goal/AgentRun attempt.
 *
 * <p>The already-checkpointed Goal and AgentRun identities are the recovery authority. Their
 * framed, domain-separated digest produces one point-resolvable RunRecord UUID without scanning
 * the store or adding another post-persistence sidecar.
 */
public final class AgentRunRecordIdentity {
    public static final int DERIVATION_VERSION = 1;

    private static final byte[] DOMAIN =
            "enhancer:agent-run-record".getBytes(StandardCharsets.UTF_8);

    private AgentRunRecordIdentity() {
    }

    public static String reference(String goalId, String agentRunId) {
        return "run-record/" + recordId(goalId, agentRunId);
    }

    public static String recordId(String goalId, String agentRunId) {
        UUID goal = canonical(goalId, "goalId");
        UUID agentRun = canonical(agentRunId, "agentRunId");
        MessageDigest digest = sha256();
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(DERIVATION_VERSION)
                .array());
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(DOMAIN.length)
                .array());
        digest.update(DOMAIN);
        update(digest, goal);
        update(digest, agentRun);
        byte[] bytes = digest.digest();
        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x80);
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);
        ByteBuffer uuid = ByteBuffer.wrap(bytes);
        return new UUID(uuid.getLong(), uuid.getLong()).toString();
    }

    private static void update(MessageDigest digest, UUID identity) {
        digest.update(ByteBuffer.allocate(2 * Long.BYTES)
                .putLong(identity.getMostSignificantBits())
                .putLong(identity.getLeastSignificantBits())
                .array());
    }

    private static UUID canonical(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equals(value)) {
                throw new IllegalArgumentException(name + " must be a canonical UUID");
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    name + " must be a canonical UUID", exception);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
