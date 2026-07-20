package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentLoopAgentRunExecutionTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000002001";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000002002";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000002003";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000002011";
    private static final String MESSAGE_ID = "00000000-0000-0000-0000-000000002012";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000002021";
    private static final String TASK_ID = "gate-8-agentloop-execution-port";
    private static final String SOURCE_DOCUMENT = "CURRENT_TASK.md";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private Path projectRoot;
    private FileSystemEvidenceStore evidenceStore;
    private FileSystemRunRecordStore runRecordStore;
    private String approvedDigest;

    @BeforeEach
    void setUp() throws Exception {
        projectRoot = Files.createDirectories(tempDir.resolve("project"));
        byte[] content = "Approved task document content.\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve(SOURCE_DOCUMENT), content);
        approvedDigest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        evidenceStore = new FileSystemEvidenceStore(
                tempDir.resolve("evidence"),
                new EvidenceStoragePolicy(
                        EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES));
        runRecordStore = new FileSystemRunRecordStore(tempDir.resolve("records"));
    }

    @Test
    void executesTheApprovedSourceDocumentToAVerifiedRunRecord() throws Exception {
        String reference = execution().execute(
                dispatch(SOURCE_DOCUMENT, approvedDigest));

        assertTrue(reference.startsWith("run-record/"));
        ResolvedRunRecord resolved = runRecordStore.resolve(reference);
        assertEquals(VerificationStatus.VERIFIED,
                resolved.record().verification().status());
        assertEquals(AgentLoopStopReason.COMPLETED,
                resolved.record().finalStopReason());
        assertEquals(TASK_ID, resolved.record().approvedTask().taskId());
        assertEquals(SOURCE_DOCUMENT,
                resolved.record().approvedTask().sourceDocument());
        assertEquals(Optional.of(approvedDigest),
                resolved.record().expectedContentSha256());
    }

    @Test
    void recordsADigestMismatchAsARejectedRunRecord() throws Exception {
        String reference = execution().execute(
                dispatch(SOURCE_DOCUMENT, "a".repeat(64)));

        ResolvedRunRecord resolved = runRecordStore.resolve(reference);
        assertEquals(VerificationStatus.REJECTED,
                resolved.record().verification().status());
        assertNotEquals(AgentLoopStopReason.COMPLETED,
                resolved.record().finalStopReason());
    }

    @Test
    void recordsAMissingTargetAsANonVerifiedRunRecord() throws Exception {
        String reference = execution().execute(
                dispatch("MISSING_DOCUMENT.md", "b".repeat(64)));

        ResolvedRunRecord resolved = runRecordStore.resolve(reference);
        assertNotEquals(VerificationStatus.VERIFIED,
                resolved.record().verification().status());
        assertNotEquals(AgentLoopStopReason.COMPLETED,
                resolved.record().finalStopReason());
    }

    @Test
    void executesADeclaredArbitraryTargetDistinctFromTheSourceDocument()
            throws Exception {
        byte[] target = "Arbitrary governed target file.\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(projectRoot.resolve("docs"));
        Files.write(projectRoot.resolve("docs/target.md"), target);
        String targetDigest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(target));

        String reference = execution().execute(dispatchWithInput(
                new WorkPayload.ExecutionInput("docs/target.md", targetDigest)));

        ResolvedRunRecord resolved = runRecordStore.resolve(reference);
        assertEquals(VerificationStatus.VERIFIED,
                resolved.record().verification().status());
        assertEquals(AgentLoopStopReason.COMPLETED,
                resolved.record().finalStopReason());
        // The approved task binding stays the source document, not the target.
        assertEquals(SOURCE_DOCUMENT,
                resolved.record().approvedTask().sourceDocument());
        assertEquals(Optional.of(targetDigest),
                resolved.record().expectedContentSha256());
    }

    private AgentLoopAgentRunExecution execution() {
        return new AgentLoopAgentRunExecution(
                projectRoot, evidenceStore, runRecordStore, CLOCK);
    }

    private AgentRunDispatch dispatch(String sourceDocument, String sourceSha256) {
        return dispatch(sourceDocument, sourceSha256, Optional.empty());
    }

    private AgentRunDispatch dispatchWithInput(
            WorkPayload.ExecutionInput executionInput) {
        return dispatch(SOURCE_DOCUMENT, approvedDigest, Optional.of(executionInput));
    }

    private AgentRunDispatch dispatch(
            String sourceDocument,
            String sourceSha256,
            Optional<WorkPayload.ExecutionInput> executionInput) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, sourceDocument, sourceSha256);
        MessageEnvelope envelope = new MessageEnvelope(
                MESSAGE_ID,
                "correlation-agentloop-port-1",
                Optional.empty(),
                "logical-run-agentloop-port-1",
                "agentloop-port-test",
                Instant.parse("2026-07-20T10:00:00Z"),
                new WorkPayload(
                        revision, "c".repeat(64), Set.of("read-file"), executionInput));
        AgentRunLease lease = new AgentRunLease(
                OWNER_ID,
                1L,
                CLOCK.instant(),
                CLOCK.instant().plus(Duration.ofMinutes(5)));
        return new AgentRunDispatch(
                QUEUE_ID,
                new WorkItem(WORK_ID, "read-file-worker", envelope),
                GOAL_ID,
                AGENT_RUN_ID,
                lease);
    }
}
