package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicModelInvokeVerifierTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void verifiesCompleteContentAgainstTheExternalExpectation() throws Exception {
        String content = "deterministic response content";
        VerificationDecision decision = verifier().verify(request(
                toolRequest(ModelInvokeTool.NAME),
                successResult(ModelInvokeTool.NAME, content),
                sha256(content)));

        assertEquals(VerificationStatus.VERIFIED, decision.status());
    }

    @Test
    void rejectsAContentMismatchAgainstTheExternalExpectation() throws Exception {
        VerificationDecision decision = verifier().verify(request(
                toolRequest(ModelInvokeTool.NAME),
                successResult(ModelInvokeTool.NAME, "actual content"),
                sha256("expected different content")));

        assertEquals(VerificationStatus.REJECTED, decision.status());
        assertEquals(VerificationCode.CONTENT_MISMATCH, decision.code());
    }

    @Test
    void acceptsOnlyModelInvokeRequests() throws Exception {
        VerificationDecision decision = verifier().verify(request(
                toolRequest("read-file"),
                successResult("read-file", "file content"),
                sha256("file content")));

        assertEquals(VerificationStatus.REJECTED, decision.status());
        assertEquals(VerificationCode.REQUEST_RESULT_MISMATCH, decision.code());
    }

    @Test
    void rejectsAFailedToolResultBeforeContentInspection() throws Exception {
        ToolResult failure = new ToolResult(
                ModelInvokeTool.NAME,
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                VerificationEvidence.capture(
                        "Model invocation failed",
                        "model failure BUDGET_EXCEEDED",
                        Optional.empty()));

        VerificationDecision decision = verifier().verify(request(
                toolRequest(ModelInvokeTool.NAME),
                failure,
                sha256("anything")));

        assertEquals(VerificationStatus.REJECTED, decision.status());
        assertEquals(VerificationCode.TOOL_RESULT_FAILURE, decision.code());
    }

    private DeterministicModelInvokeVerifier verifier() {
        return new DeterministicModelInvokeVerifier(new FileSystemEvidenceStore(
                temporaryRoot.resolve("evidence"),
                new EvidenceStoragePolicy(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)));
    }

    private VerificationRequest request(
            ToolRequest toolRequest,
            ToolResult toolResult,
            String expectedSha256) {
        ApprovedTask approvedTask = new ApprovedTask(
                "model-verifier-test",
                "Verify the governed model invocation.",
                "Approved by the verifier test owner.",
                Set.of(ModelInvokeTool.NAME, "read-file"),
                "CURRENT_TASK.md");
        return new VerificationRequest(
                approvedTask, toolRequest, toolResult, expectedSha256);
    }

    private ToolRequest toolRequest(String toolName) {
        return new ToolRequest(toolName, "correlation-1", Map.of());
    }

    private ToolResult successResult(String toolName, String content) {
        return new ToolResult(
                toolName,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture(
                        "Model invocation succeeded",
                        content,
                        Optional.empty()));
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
