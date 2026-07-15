package com.enhancer.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.FinalizedAgentRun;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.EvidenceRetentionPolicy;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ReadFileTool;
import com.enhancer.tool.Tool;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.verification.DeterministicReadFileVerifier;
import com.enhancer.verification.VerificationCode;
import com.enhancer.verification.VerificationRequest;
import com.enhancer.verification.VerificationStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentRunFinalizerIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void verifiedEvidenceCompletesOnlyAfterTheRunRecordIsDurable() throws Exception {
        String content = "governed-content\n".repeat(600);
        RunFixture fixture = executeRead(content);
        FileSystemRunRecordStore records = new FileSystemRunRecordStore(
                temporaryRoot.resolve("records"));
        AgentRunFinalizer finalizer = finalizer(fixture.evidenceStore(), records);

        FinalizedAgentRun finalized = finalizer.finalizeRun(
                fixture.workerRun(),
                Optional.of(verificationRequest(fixture.workerRun(), sha256(content))));

        assertEquals(AgentLoopStatus.COMPLETED, finalized.state().status());
        assertEquals(AgentLoopStopReason.COMPLETED, finalized.stopReason());
        assertEquals(VerificationStatus.VERIFIED, finalized.verification().status());
        ResolvedRunRecord replayed = new FileSystemRunRecordStore(
                temporaryRoot.resolve("records"))
                .resolve(finalized.storedRecord().reference());
        assertEquals(finalized.record(), replayed.record());
        assertEquals(AgentLoopStopReason.COMPLETED, replayed.record().finalStopReason());
        assertEquals(
                Optional.of(sha256(content)),
                replayed.record().expectedContentSha256());
    }

    @Test
    void rejectedEvidenceCannotPromoteCompletionButIsStillRecorded() throws Exception {
        RunFixture fixture = executeRead("actual-content");
        AgentRunFinalizer finalizer = finalizer(
                fixture.evidenceStore(),
                new FileSystemRunRecordStore(temporaryRoot.resolve("records")));

        FinalizedAgentRun finalized = finalizer.finalizeRun(
                fixture.workerRun(),
                Optional.of(verificationRequest(fixture.workerRun(), sha256("expected-content"))));

        assertEquals(AgentLoopStatus.AWAITING_VERIFICATION, finalized.state().status());
        assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION, finalized.stopReason());
        assertEquals(VerificationStatus.REJECTED, finalized.verification().status());
        assertEquals(VerificationCode.CONTENT_MISMATCH, finalized.verification().code());
    }

    @Test
    void recordPersistenceFailurePreventsCompletionFromBeingReturned() throws Exception {
        RunFixture fixture = executeRead("content");
        RunRecordStore failingStore = new RunRecordStore() {
            @Override
            public StoredRunRecord persist(RunRecord record) throws IOException {
                throw new IOException("storage unavailable");
            }

            @Override
            public ResolvedRunRecord resolve(String reference) throws IOException {
                throw new IOException("storage unavailable");
            }
        };
        AgentRunFinalizer finalizer = finalizer(fixture.evidenceStore(), failingStore);

        assertThrows(
                IOException.class,
                () -> finalizer.finalizeRun(
                        fixture.workerRun(),
                        Optional.of(verificationRequest(
                                fixture.workerRun(),
                                sha256("content")))));
    }

    @Test
    void missingCompleteEvidenceRemainsUnverifiedAndIsRecorded() throws Exception {
        String content = "missing-content\n".repeat(600);
        String missingReference = "evidence/" + UUID.randomUUID() + "/" + UUID.randomUUID();
        Tool tool = fixedTool(new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture(
                        "read succeeded",
                        content,
                        Optional.of(missingReference))));
        ExecutionPolicy policy = policy(Files.createDirectories(
                temporaryRoot.resolve("missing-project")));
        AgentRunResult workerRun = execute(tool, policy, new AgentLoop(5, 3));
        AgentRunFinalizer finalizer = finalizer(
                evidenceStore(),
                new FileSystemRunRecordStore(temporaryRoot.resolve("missing-records")));

        FinalizedAgentRun finalized = finalizer.finalizeRun(
                workerRun,
                Optional.of(verificationRequest(workerRun, sha256(content))));

        assertEquals(VerificationStatus.UNVERIFIED, finalized.verification().status());
        assertEquals(VerificationCode.MISSING_EVIDENCE, finalized.verification().code());
        assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION, finalized.stopReason());
    }

    @Test
    void stagnatedWorkerRunIsRecordedWithoutFabricatedVerification() throws Exception {
        Tool retrying = new Tool() {
            @Override
            public String name() {
                return "read-file";
            }

            @Override
            public ToolResult execute(ToolRequest request, ExecutionPolicy policy) {
                return new ToolResult(
                        name(),
                        ToolResultStatus.FAILURE,
                        OptionalInt.empty(),
                        Optional.of(ToolFailureCode.TEMPORARY_FAILURE),
                        VerificationEvidence.capture(
                                "temporary failure",
                                "same failure",
                                Optional.empty()));
            }
        };
        ExecutionPolicy policy = policy(temporaryRoot.resolve("project"));
        AgentRunResult workerRun;
        try (ToolExecutor executor = new ToolExecutor(List.of(retrying))) {
            workerRun = new AgentRunController(
                    executor,
                    policy,
                    ToolFailureClassifier.standard())
                    .run(AgentRunState.ready(approvedTask(), request(UUID.randomUUID().toString())),
                            new AgentLoop(10, 3));
        }
        FileSystemEvidenceStore evidenceStore = evidenceStore();
        AgentRunFinalizer finalizer = finalizer(
                evidenceStore,
                new FileSystemRunRecordStore(temporaryRoot.resolve("records")));

        FinalizedAgentRun finalized = finalizer.finalizeRun(
                workerRun,
                Optional.empty());

        assertEquals(AgentLoopStopReason.STAGNATED, finalized.stopReason());
        assertEquals(VerificationStatus.NOT_PERFORMED, finalized.verification().status());
        assertEquals(AgentLoopStatus.RUNNING, finalized.state().status());
    }

    @Test
    void failedAndIterationLimitedRunsAreRecordedWithoutVerification() throws Exception {
        ExecutionPolicy policy = policy(Files.createDirectories(
                temporaryRoot.resolve("terminal-project")));
        ToolResult terminalFailure = new ToolResult(
                "read-file",
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(ToolFailureCode.INVALID_REQUEST),
                VerificationEvidence.capture(
                        "invalid request",
                        "invalid",
                        Optional.empty()));
        AgentRunResult failed = execute(
                fixedTool(terminalFailure),
                policy,
                new AgentLoop(5, 3));

        AtomicInteger attempts = new AtomicInteger();
        Tool changingRetry = new Tool() {
            @Override
            public String name() {
                return "read-file";
            }

            @Override
            public ToolResult execute(ToolRequest request, ExecutionPolicy ignored) {
                return new ToolResult(
                        name(),
                        ToolResultStatus.FAILURE,
                        OptionalInt.empty(),
                        Optional.of(ToolFailureCode.TEMPORARY_FAILURE),
                        VerificationEvidence.capture(
                                "temporary failure",
                                "attempt-" + attempts.incrementAndGet(),
                                Optional.empty()));
            }
        };
        AgentRunResult limited = execute(changingRetry, policy, new AgentLoop(2, 3));
        AgentRunFinalizer finalizer = finalizer(
                evidenceStore(),
                new FileSystemRunRecordStore(temporaryRoot.resolve("terminal-records")));

        FinalizedAgentRun failedFinal = finalizer.finalizeRun(
                failed,
                Optional.empty());
        FinalizedAgentRun limitedFinal = finalizer.finalizeRun(
                limited,
                Optional.empty());

        assertEquals(AgentLoopStopReason.FAILED, failedFinal.stopReason());
        assertEquals(VerificationStatus.NOT_PERFORMED, failedFinal.verification().status());
        assertEquals(AgentLoopStopReason.MAX_ITERATIONS, limitedFinal.stopReason());
        assertEquals(VerificationStatus.NOT_PERFORMED, limitedFinal.verification().status());
    }

    private RunFixture executeRead(String content) throws Exception {
        Path projectRoot = Files.createDirectories(temporaryRoot.resolve("project-" + UUID.randomUUID()));
        Files.writeString(projectRoot.resolve("target.txt"), content, StandardCharsets.UTF_8);
        FileSystemEvidenceStore evidenceStore = evidenceStore();
        String runId = evidenceStore.createRun();
        ExecutionPolicy policy = policy(projectRoot);
        AgentRunResult workerRun;
        try (ToolExecutor executor = new ToolExecutor(List.of(
                new ReadFileTool(new EvidenceRecorder(evidenceStore))))) {
            workerRun = new AgentRunController(
                    executor,
                    policy,
                    ToolFailureClassifier.standard())
                    .run(AgentRunState.ready(approvedTask(), request(runId)), new AgentLoop(5, 3));
        }
        return new RunFixture(workerRun, evidenceStore);
    }

    private AgentRunResult execute(
            Tool tool,
            ExecutionPolicy policy,
            AgentLoop loop) {
        try (ToolExecutor executor = new ToolExecutor(List.of(tool))) {
            return new AgentRunController(
                    executor,
                    policy,
                    ToolFailureClassifier.standard())
                    .run(AgentRunState.ready(
                            approvedTask(),
                            request(UUID.randomUUID().toString())), loop);
        }
    }

    private Tool fixedTool(ToolResult result) {
        return new Tool() {
            @Override
            public String name() {
                return "read-file";
            }

            @Override
            public ToolResult execute(ToolRequest request, ExecutionPolicy policy) {
                return result;
            }
        };
    }

    private AgentRunFinalizer finalizer(
            FileSystemEvidenceStore evidenceStore,
            RunRecordStore runRecordStore) {
        return new AgentRunFinalizer(
                new DeterministicReadFileVerifier(evidenceStore),
                runRecordStore,
                Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC));
    }

    private VerificationRequest verificationRequest(AgentRunResult run, String expectedDigest) {
        return new VerificationRequest(
                run.state().approvedTask(),
                run.state().executedRequest(),
                run.state().lastResult().orElseThrow(),
                expectedDigest);
    }

    private ApprovedTask approvedTask() {
        return new ApprovedTask(
                "gate-4-test",
                "Read and verify a governed file",
                "Approved by test owner",
                Set.of("read-file"),
                "CURRENT_TASK.md");
    }

    private ToolRequest request(String correlationId) {
        return new ToolRequest(
                "read-file",
                correlationId,
                Map.of(ReadFileTool.PATH_ARGUMENT, "target.txt"));
    }

    private ExecutionPolicy policy(Path projectRoot) {
        return new ExecutionPolicy(
                projectRoot,
                Set.of("read-file"),
                Set.of(),
                EvidenceRetentionPolicy.MAX_SUPPORTED_CONTENT_BYTES,
                Duration.ofSeconds(2),
                CancellationToken.none());
    }

    private FileSystemEvidenceStore evidenceStore() {
        return new FileSystemEvidenceStore(
                temporaryRoot.resolve("evidence"),
                new EvidenceRetentionPolicy(
                        EvidenceRetentionPolicy.MAX_SUPPORTED_CONTENT_BYTES,
                        Duration.ofDays(30)));
    }

    private String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record RunFixture(
            AgentRunResult workerRun,
            FileSystemEvidenceStore evidenceStore) {
    }
}
