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
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemRunRecordStoreIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x454E5234;
    private static final int HEADER_BYTES = Integer.BYTES + Long.BYTES + Integer.BYTES + 32;
    private static final String V1_GOLDEN_PAYLOAD_BASE64 =
            "AAAAAQAAAA1sb2dpY2FsLXJ1bi0xAAABn13sYAAAAAAPcnVuLXJlY29yZC10YXNr"
                    + "AAAAD1BlcnNpc3QgdGhlIHJ1bgAAABZBcHByb3ZlZCBieSB0ZXN0IG93bmVyAAAA"
                    + "AQAAAAlyZWFkLWZpbGUAAAAPQ1VSUkVOVF9UQVNLLm1kAAAACXJlYWQtZmlsZQAA"
                    + "AA1jb3JyZWxhdGlvbi0xAAAAAQAAAARwYXRoAAAACnRhcmdldC50eHQAAAAHQUxM"
                    + "T1dFRAAAAApDOi9wcm9qZWN0AAAAAQAAAAlyZWFkLWZpbGUAAAAAAAAAAAAAEAAA"
                    + "AAAAAAAD6AAAAAlyZWFkLWZpbGUAAAAHU1VDQ0VTUwAAAAAADnJlYWQgc3VjY2Vl"
                    + "ZGVkAAAAB2NvbnRlbnQAAAAHAAABAAAAQGVkNzAwMmI0MzllOWFjODQ1ZjIyMzU3"
                    + "ZDgyMmJhYzE0NDQ3MzBmYmRiNjAxNmQzZWM5NDMyMjk3YjllYzlmNzMBAAAAQGVk"
                    + "NzAwMmI0MzllOWFjODQ1ZjIyMzU3ZDgyMmJhYzE0NDQ3MzBmYmRiNjAxNmQzZWM5"
                    + "NDMyMjk3YjllYzlmNzMAAAAIVkVSSUZJRUQAAAAIVkVSSUZJRUQAAAAsY29tcGxl"
                    + "dGUgY29udGVudCBtYXRjaGVkIHRoZSBleHBlY3RlZCBkaWdlc3QAAAABAAAAFUFX"
                    + "QUlUSU5HX1ZFUklGSUNBVElPTgAAAAlDT01QTEVURUQ=";
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
    void decodesTheLiteralV1GoldenAndNewV1EncodingRemainsByteIdentical() throws Exception {
        byte[] goldenPayload = Base64.getDecoder().decode(V1_GOLDEN_PAYLOAD_BASE64);
        String literalId = "11111111-1111-1111-1111-111111111111";
        Instant storedAt = Instant.parse("2026-08-31T00:00:00Z");
        Files.createDirectories(storageRoot);
        Files.write(
                storageRoot.resolve(literalId + ".run-record"),
                envelope(storedAt.toEpochMilli(), goldenPayload));

        ResolvedRunRecord literal = new FileSystemRunRecordStore(storageRoot)
                .resolve("run-record/" + literalId);
        assertEquals(record(), literal.record());
        assertEquals(storedAt, literal.metadata().storedAt());

        String newId = "22222222-2222-2222-2222-222222222222";
        new FileSystemRunRecordStore(storageRoot).persist(newId, record());
        byte[] newlyEncoded = Files.readAllBytes(
                storageRoot.resolve(newId + ".run-record"));
        assertTrue(Arrays.equals(
                goldenPayload,
                Arrays.copyOfRange(newlyEncoded, HEADER_BYTES, newlyEncoded.length)));
    }

    @Test
    void pointPersistsExactReplayAndRefusesChangedIdentityReuse() throws Exception {
        String recordId = UUID.randomUUID().toString();
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord first = store.persist(recordId, record());
        byte[] firstBytes = Files.readAllBytes(
                storageRoot.resolve(recordId + ".run-record"));

        StoredRunRecord replay = new FileSystemRunRecordStore(storageRoot)
                .persist(recordId, record());

        assertEquals(first, replay);
        assertEquals(
                List.of("run-record/" + recordId),
                new FileSystemRunRecordStore(storageRoot).references());
        assertTrue(java.util.Arrays.equals(
                firstBytes,
                Files.readAllBytes(storageRoot.resolve(recordId + ".run-record"))));
        IOException conflict = assertThrows(
                IOException.class,
                () -> store.persist(recordId, record("Changed approval")));
        assertTrue(conflict.getMessage().contains("different RunRecord"));
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

    @Test
    void selectsRecentReferencesNewestFirstWithoutChangingCompleteListing() throws Exception {
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord first = store.persist(record());
        StoredRunRecord second = store.persist(record());
        StoredRunRecord third = store.persist(record());
        setModified(first, 1);
        setModified(second, 2);
        setModified(third, 3);

        assertEquals(List.of(third.reference(), second.reference()), store.recentReferences(2));
        assertEquals(3, store.references().size());
        assertThrows(IllegalArgumentException.class, () -> store.recentReferences(0));
        assertThrows(IllegalArgumentException.class, () -> store.recentReferences(4097));
    }

    private void setModified(StoredRunRecord stored, long seconds) throws Exception {
        Files.setLastModifiedTime(
                storageRoot.resolve(stored.recordId() + ".run-record"),
                FileTime.from(Instant.parse("2026-07-16T00:00:00Z").plusSeconds(seconds)));
    }

    private byte[] envelope(long storedAtMillis, byte[] payload) throws Exception {
        ByteBuffer digestInput = ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(payload);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(digestInput.array());
        return ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private RunRecord record() {
        return record("Approved by test owner");
    }

    private RunRecord record(String approval) {
        ApprovedTask task = new ApprovedTask(
                "run-record-task",
                "Persist the run",
                approval,
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
