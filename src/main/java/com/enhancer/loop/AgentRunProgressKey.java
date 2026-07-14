package com.enhancer.loop;

import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.VerificationEvidence;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

final class AgentRunProgressKey {
    private AgentRunProgressKey() {
    }

    static String pending(ApprovedTask approvedTask, ToolRequest request) {
        MessageDigest digest = sha256();
        update(digest, "pending");
        updateApprovedTask(digest, approvedTask);
        updateRequest(digest, request);
        return "pending:" + hex(digest.digest());
    }

    static String result(ApprovedTask approvedTask, ToolRequest request, ToolResult result) {
        MessageDigest digest = sha256();
        update(digest, "result");
        updateApprovedTask(digest, approvedTask);
        updateRequest(digest, request);
        update(digest, result.toolName());
        update(digest, result.status().name());
        update(digest, result.exitCode().isPresent()
                ? Integer.toString(result.exitCode().orElseThrow())
                : "absent");
        update(digest, result.failureCode().isPresent()
                ? result.failureCode().orElseThrow().name()
                : "absent");

        VerificationEvidence evidence = result.evidence();
        update(digest, Integer.toString(evidence.originalOutputLength()));
        update(digest, Boolean.toString(evidence.truncated()));
        if (evidence.contentSha256().isPresent()) {
            update(digest, evidence.contentSha256().orElseThrow());
        } else {
            update(digest, evidence.outputTail());
        }
        return "tool-result:" + hex(digest.digest());
    }

    private static void updateApprovedTask(MessageDigest digest, ApprovedTask approvedTask) {
        update(digest, approvedTask.taskId());
        update(digest, approvedTask.description());
        update(digest, approvedTask.approvalEvidence());
        update(digest, approvedTask.sourceDocument());
        approvedTask.allowedTools().stream().sorted().forEach(tool -> update(digest, tool));
    }

    private static void updateRequest(MessageDigest digest, ToolRequest request) {
        update(digest, request.toolName());
        update(digest, request.correlationId());
        for (Map.Entry<String, String> argument : new TreeMap<>(request.arguments()).entrySet()) {
            update(digest, argument.getKey());
            update(digest, argument.getValue());
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String hex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }
}
