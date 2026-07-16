package com.enhancer.verification;

import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.StoredEvidence;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicReadFileVerifierTest {
    @TempDir
    Path storageRoot;

    @Test
    void verifiesCompleteInlineEvidenceAgainstAnExternalExpectation() {
        String content = "governed-content";
        VerificationDecision decision = verifier().verify(request(
                VerificationEvidence.capture("read succeeded", content, Optional.empty()),
                sha256(content)));

        assertEquals(VerificationStatus.VERIFIED, decision.status());
        assertEquals(VerificationCode.VERIFIED, decision.code());
    }

    @Test
    void resolvesAndVerifiesTruncatedEvidence() throws Exception {
        FileSystemEvidenceStore store = store();
        String runId = store.createRun();
        String content = "large-content\n".repeat(600);
        StoredEvidence stored = store.persist(runId, content);
        VerificationEvidence evidence = VerificationEvidence.capture(
                "read succeeded",
                content,
                Optional.of(stored.reference()));

        VerificationDecision decision = new DeterministicReadFileVerifier(store)
                .verify(request(evidence, sha256(content)));

        assertEquals(VerificationStatus.VERIFIED, decision.status());
    }

    @Test
    void reportsMissingReferencedEvidenceAsUnverified() {
        String content = "large-content\n".repeat(600);
        String missingReference = "evidence/" + UUID.randomUUID() + "/" + UUID.randomUUID();
        VerificationEvidence evidence = VerificationEvidence.capture(
                "read succeeded",
                content,
                Optional.of(missingReference));

        VerificationDecision decision = verifier().verify(request(evidence, sha256(content)));

        assertEquals(VerificationStatus.UNVERIFIED, decision.status());
        assertEquals(VerificationCode.MISSING_EVIDENCE, decision.code());
    }

    @Test
    void rejectsCorruptedReferencedEvidence() throws Exception {
        FileSystemEvidenceStore store = store();
        String runId = store.createRun();
        String content = "large-content\n".repeat(600);
        StoredEvidence stored = store.persist(runId, content);
        Path artifact = storageRoot
                .resolve(stored.runId())
                .resolve(stored.evidenceId() + ".evidence");
        byte[] bytes = Files.readAllBytes(artifact);
        bytes[bytes.length - 1] ^= 0x01;
        Files.write(artifact, bytes);

        VerificationEvidence evidence = VerificationEvidence.capture(
                "read succeeded",
                content,
                Optional.of(stored.reference()));
        VerificationDecision decision = new DeterministicReadFileVerifier(store)
                .verify(request(evidence, sha256(content)));

        assertEquals(VerificationStatus.REJECTED, decision.status());
        assertEquals(VerificationCode.CORRUPTED_EVIDENCE, decision.code());
    }

    @Test
    void rejectsContentThatDoesNotMatchTheExternalExpectation() {
        VerificationDecision decision = verifier().verify(request(
                VerificationEvidence.capture("read succeeded", "actual", Optional.empty()),
                sha256("expected")));

        assertEquals(VerificationStatus.REJECTED, decision.status());
        assertEquals(VerificationCode.CONTENT_MISMATCH, decision.code());
    }

    @Test
    void decisionStatusAndReasonCodeMustAgree() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationDecision(
                        VerificationStatus.VERIFIED,
                        VerificationCode.CONTENT_MISMATCH,
                        "invalid"));
    }

    private DeterministicReadFileVerifier verifier() {
        return new DeterministicReadFileVerifier(store());
    }

    private FileSystemEvidenceStore store() {
        return new FileSystemEvidenceStore(
                storageRoot,
                new EvidenceStoragePolicy(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES));
    }

    private VerificationRequest request(VerificationEvidence evidence, String expectedDigest) {
        ApprovedTask task = new ApprovedTask(
                "verification-task",
                "Verify a governed read",
                "Approved by test owner",
                Set.of("read-file"),
                "CURRENT_TASK.md");
        ToolRequest toolRequest = new ToolRequest(
                "read-file",
                UUID.randomUUID().toString(),
                Map.of("path", "target.txt"));
        ToolResult result = new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                evidence);
        return new VerificationRequest(task, toolRequest, result, expectedDigest);
    }

    private String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
