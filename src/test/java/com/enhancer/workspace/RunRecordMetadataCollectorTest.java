package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.VerificationDecision;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunRecordMetadataCollectorTest {
    private static final Instant RECORDED_AT = Instant.parse("2026-07-15T10:00:00Z");
    private static final Instant OBSERVED_AT = RECORDED_AT.plusSeconds(60);

    @TempDir
    Path temporaryRoot;

    @Test
    void observesEveryStoredRecordWithItsEnvelopeDigest() throws Exception {
        Path storageRoot = temporaryRoot.resolve("records");
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord first = store.persist(record("logical-run-1"));
        StoredRunRecord second = store.persist(record("logical-run-2"));
        Files.writeString(storageRoot.resolve("not-a-record.txt"), "ignored");

        List<String> references = store.references();
        assertEquals(2, references.size());
        assertTrue(references.contains(first.reference()));
        assertTrue(references.contains(second.reference()));
        assertEquals(references.stream().sorted().toList(), references);

        List<WorkspaceSourceObservation> observations = new RunRecordMetadataCollector()
                .observe(store, OBSERVED_AT);

        assertEquals(2, observations.size());
        for (WorkspaceSourceObservation observation : observations) {
            assertEquals(WorkspaceSourceKind.RUN_RECORD, observation.kind());
            assertEquals("run-record-store", observation.provenance());
            assertEquals(OBSERVED_AT, observation.observedAt());
            assertEquals(WorkspaceSourceState.AVAILABLE, observation.state());
            assertTrue(observation.contentSha256().isPresent());
            assertTrue(observation.sourceUpdatedAt().isPresent());
        }
        WorkspaceSourceObservation firstObservation = observations.stream()
                .filter(observation -> observation.sourceId().equals(first.reference()))
                .findFirst()
                .orElseThrow();
        assertEquals(Optional.of(first.sha256()), firstObservation.contentSha256());
    }

    @Test
    void surfacesACorruptedRecordAsUnavailableWithoutADigest() throws Exception {
        Path storageRoot = temporaryRoot.resolve("corrupted-records");
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord stored = store.persist(record("logical-run-1"));
        Path artifact = storageRoot.resolve(stored.recordId() + ".run-record");
        byte[] bytes = Files.readAllBytes(artifact);
        bytes[bytes.length - 1] ^= 0x1;
        Files.write(artifact, bytes);

        List<WorkspaceSourceObservation> observations = new RunRecordMetadataCollector()
                .observe(store, OBSERVED_AT);

        assertEquals(1, observations.size());
        WorkspaceSourceObservation observation = observations.get(0);
        assertEquals(stored.reference(), observation.sourceId());
        assertEquals(WorkspaceSourceState.UNAVAILABLE, observation.state());
        assertEquals(Optional.empty(), observation.contentSha256());
        assertTrue(observation.reason().isPresent());
    }

    @Test
    void returnsNoObservationsForAMissingOrEmptyStorageRoot() throws Exception {
        FileSystemRunRecordStore missing = new FileSystemRunRecordStore(
                temporaryRoot.resolve("never-created"));
        assertEquals(List.of(), missing.references());
        assertEquals(
                List.of(),
                new RunRecordMetadataCollector().observe(missing, OBSERVED_AT));
    }

    @Test
    void observationsComposeIntoASnapshotAlongsideRepositoryDocuments() throws Exception {
        Path storageRoot = temporaryRoot.resolve("compose-records");
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        store.persist(record("logical-run-1"));
        List<WorkspaceSourceObservation> runObservations = new RunRecordMetadataCollector()
                .observe(store, OBSERVED_AT);
        WorkspaceSourceObservation document = new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                "ARCHITECTURE.md",
                "context-reader",
                OBSERVED_AT,
                Optional.empty(),
                WorkspaceSourceState.AVAILABLE,
                Optional.of("a".repeat(64)),
                Optional.empty());

        WorkspaceSnapshot snapshot = WorkspaceSnapshot.capture(
                temporaryRoot,
                OBSERVED_AT,
                new ApprovedTaskRevision(
                        "gate-6-run-record-observation-test",
                        "CURRENT_TASK.md",
                        "f".repeat(64)),
                List.of(document, runObservations.get(0)));

        assertEquals(2, snapshot.observations().size());
        assertEquals(
                WorkspaceSourceKind.REPOSITORY_DOCUMENT,
                snapshot.observations().get(0).kind());
        assertEquals(
                WorkspaceSourceKind.RUN_RECORD,
                snapshot.observations().get(1).kind());
    }

    @Test
    void rejectsMissingInputs() {
        RunRecordMetadataCollector collector = new RunRecordMetadataCollector();
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(
                temporaryRoot.resolve("null-records"));

        assertThrows(NullPointerException.class, () -> collector.observe(null, OBSERVED_AT));
        assertThrows(NullPointerException.class, () -> collector.observe(store, null));
    }

    private RunRecord record(String logicalRunId) {
        return new RunRecord(
                logicalRunId,
                RECORDED_AT,
                new ApprovedTask(
                        "run-record-observation-test",
                        "Observe stored run records",
                        "Approved by the test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "correlation-1",
                        Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        VerificationEvidence.capture(
                                "read succeeded",
                                "content",
                                Optional.empty())),
                Optional.of("a".repeat(64)),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
    }
}
