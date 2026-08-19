package com.enhancer.model;

import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.tool.CorruptedEvidenceException;
import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.MissingEvidenceException;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.IndependentVerifier;
import com.enhancer.verification.VerificationRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Digest-integrity verification for governed {@code model-invoke} results.
 *
 * <p>The persisted response evidence digest must match the recorded content
 * identity, and the complete content must match the external expectation. The
 * deterministic fake keeps that external expectation computable, and the check is
 * not weakened for nondeterministic responses: a response that cannot satisfy an
 * exact external digest is not Verified.
 */
public final class DeterministicModelInvokeVerifier implements IndependentVerifier {
    private final EvidenceStore evidenceStore;

    public DeterministicModelInvokeVerifier(EvidenceStore evidenceStore) {
        this.evidenceStore = Objects.requireNonNull(
                evidenceStore,
                "evidenceStore must not be null");
    }

    @Override
    public VerificationDecision verify(VerificationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.toolResult().status() != ToolResultStatus.SUCCESS) {
            return VerificationDecision.rejected(
                    VerificationCode.TOOL_RESULT_FAILURE,
                    "Tool result did not succeed");
        }
        if (!ModelInvokeTool.NAME.equals(request.toolRequest().toolName())) {
            return VerificationDecision.rejected(
                    VerificationCode.REQUEST_RESULT_MISMATCH,
                    "The deterministic model verifier accepts only model-invoke requests");
        }
        if (!request.toolRequest().toolName().equals(request.toolResult().toolName())) {
            return VerificationDecision.rejected(
                    VerificationCode.REQUEST_RESULT_MISMATCH,
                    "Tool result identity does not match the executed request");
        }

        VerificationEvidence evidence = request.toolResult().evidence();
        if (evidence.contentSha256().isEmpty()) {
            return VerificationDecision.unverified(
                    VerificationCode.MISSING_CONTENT_DIGEST,
                    "Tool evidence does not contain complete-content identity");
        }

        String content;
        if (evidence.truncated()) {
            String reference = evidence.fullOutputReference().orElseThrow();
            try {
                content = evidenceStore.resolve(reference).content();
            } catch (MissingEvidenceException exception) {
                return VerificationDecision.unverified(
                        VerificationCode.MISSING_EVIDENCE,
                        "Referenced complete evidence is missing");
            } catch (CorruptedEvidenceException | IllegalArgumentException exception) {
                return VerificationDecision.rejected(
                        VerificationCode.CORRUPTED_EVIDENCE,
                        "Referenced complete evidence is corrupted or invalid");
            } catch (IOException exception) {
                return VerificationDecision.unverified(
                        VerificationCode.EVIDENCE_UNAVAILABLE,
                        "Referenced complete evidence could not be resolved");
            }
        } else {
            content = evidence.outputTail();
        }

        if (content.length() != evidence.originalOutputLength()) {
            return VerificationDecision.rejected(
                    VerificationCode.INVALID_EVIDENCE,
                    "Resolved evidence length does not match Tool metadata");
        }

        String actualDigest = sha256(content);
        if (!actualDigest.equals(evidence.contentSha256().orElseThrow())) {
            return VerificationDecision.rejected(
                    VerificationCode.INVALID_EVIDENCE,
                    "Resolved evidence digest does not match Tool metadata");
        }
        if (!actualDigest.equals(request.expectedContentSha256())) {
            return VerificationDecision.rejected(
                    VerificationCode.CONTENT_MISMATCH,
                    "Complete content does not match the external expectation");
        }
        return VerificationDecision.verified(
                "Complete content matched Tool metadata and the external expectation");
    }

    private String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
