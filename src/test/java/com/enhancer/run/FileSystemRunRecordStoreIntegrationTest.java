package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.VerificationCode;
import com.enhancer.verification.VerificationDecision;
import com.enhancer.verification.VerificationStatus;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemRunRecordStoreIntegrationTest {
    @TempDir
    Path storageRoot;

    @Test
    void persistsAndReplaysARecordThroughANewStoreInstance() throws Exception {
        RunRecord record = record();
        StoredRunRecord stored = new FileSystemRunRecordStore(storageRoot).persist(record);

        ResolvedRunRecord resolved = new FileSystemRunRecordStore(storageRoot)
                .resolve(stored.reference());

        assertEquals(record, resolved.record());
        assertEquals(stored.reference(), resolved.metadata().reference());
        assertEquals(stored.sha256(), resolved.metadata().sha256());
    }

    @Test
    void rejectsMissingAndCorruptedRecords() throws Exception {
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord stored = store.persist(record());
        Path artifact = storageRoot.resolve(stored.recordId() + ".run-record");
        byte[] bytes = Files.readAllBytes(artifact);
        bytes[bytes.length - 1] ^= 0x01;
        Files.write(artifact, bytes);

        assertThrows(CorruptedRunRecordException.class, () -> store.resolve(stored.reference()));
        assertThrows(
                MissingRunRecordException.class,
                () -> store.resolve("run-record/" + UUID.randomUUID()));
    }

    @Test
    void rejectsTimestampMetadataTampering() throws Exception {
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord stored = store.persist(record());
        Path artifact = storageRoot.resolve(stored.recordId() + ".run-record");
        byte[] envelope = Files.readAllBytes(artifact);
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        buffer.putLong(Integer.BYTES, buffer.getLong(Integer.BYTES) + 1);
        Files.write(artifact, envelope);

        CorruptedRunRecordException exception = assertThrows(
                CorruptedRunRecordException.class,
                () -> store.resolve(stored.reference()));

        assertTrue(exception.getMessage().contains("digest"));
    }

    @Test
    void rejectsMalformedUnicodeInsteadOfReplacingIt() {
        RunRecord valid = record();
        ApprovedTask task = new ApprovedTask(
                "run-record-task",
                "malformed-\uD800-text",
                "Approved by test owner",
                Set.of("read-file"),
                "CURRENT_TASK.md");
        RunRecord malformed = new RunRecord(
                "logical-run-1",
                Instant.parse("2026-07-14T00:00:00Z"),
                task,
                valid.toolRequest(),
                valid.policyDecision(),
                valid.toolResult(),
                valid.expectedContentSha256(),
                valid.verification(),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);

        IOException exception = assertThrows(
                IOException.class,
                () -> new FileSystemRunRecordStore(storageRoot).persist(malformed));

        assertTrue(exception.getMessage().contains("Unicode"));
    }

    private RunRecord record() {
        ApprovedTask task = new ApprovedTask(
                "run-record-task",
                "Persist the run",
                "Approved by test owner",
                Set.of("read-file"),
                "CURRENT_TASK.md");
        ToolRequest request = new ToolRequest(
                "read-file",
                "correlation-1",
                Map.of("path", "target.txt"));
        VerificationEvidence evidence = VerificationEvidence.capture(
                "read succeeded",
                "content",
                Optional.empty());
        ToolResult result = new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                evidence);
        PolicyDecision policy = new PolicyDecision(
                PolicyDecisionStatus.ALLOWED,
                "C:/project",
                Set.of("read-file"),
                Set.of(),
                4096,
                1000);
        VerificationDecision decision = new VerificationDecision(
                VerificationStatus.VERIFIED,
                VerificationCode.VERIFIED,
                "complete content matched the expected digest");
        return new RunRecord(
                "logical-run-1",
                Instant.parse("2026-07-14T00:00:00Z"),
                task,
                request,
                policy,
                result,
                evidence.contentSha256(),
                decision,
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
    }
}
