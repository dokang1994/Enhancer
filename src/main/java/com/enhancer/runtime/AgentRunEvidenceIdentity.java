package com.enhancer.runtime;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Pure deterministic evidence-run identity for one durable Goal/AgentRun attempt.
 *
 * <p>The domain is intentionally distinct from {@link AgentRunRecordIdentity}; the result is
 * suitable for the existing evidence-run UUID contract but grants no evidence or execution
 * authority.
 */
public final class AgentRunEvidenceIdentity {
    public static final int DERIVATION_VERSION = 1;

    private static final byte[] DOMAIN =
            "enhancer:agent-run-evidence".getBytes(StandardCharsets.UTF_8);

    private AgentRunEvidenceIdentity() {
    }

    public static String runId(String goalId, String agentRunId) {
        UUID goal = UUID.fromString(RuntimeIdentity.canonicalUuid(goalId, "goalId"));
        UUID agentRun = UUID.fromString(
                RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId"));
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

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
