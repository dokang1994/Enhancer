package com.enhancer.verification;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import java.util.Objects;
import java.util.regex.Pattern;

public record VerificationRequest(
        ApprovedTask approvedTask,
        ToolRequest toolRequest,
        ToolResult toolResult,
        String expectedContentSha256) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public VerificationRequest {
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(toolRequest, "toolRequest must not be null");
        Objects.requireNonNull(toolResult, "toolResult must not be null");
        Objects.requireNonNull(
                expectedContentSha256,
                "expectedContentSha256 must not be null");
        if (!approvedTask.allows(toolRequest.toolName())) {
            throw new IllegalArgumentException(
                    "verification request Tool is outside approved task scope");
        }
        if (!SHA_256.matcher(expectedContentSha256).matches()) {
            throw new IllegalArgumentException(
                    "expectedContentSha256 must be 64 lowercase hexadecimal characters");
        }
    }
}
