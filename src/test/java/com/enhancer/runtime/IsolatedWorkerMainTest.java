package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportStatus;
import com.enhancer.bus.WorkPayload;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IsolatedWorkerMainTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void typedWorkExecutesTheChildLocalPipelineAndPublishesV2()
            throws Exception {
        Path cycleRoot = temporaryRoot.resolve("model-cycle");
        Path projectRoot = temporaryRoot.resolve("model-project");
        Path evidenceRoot = temporaryRoot.resolve("model-evidence");
        Path runRecordRoot = temporaryRoot.resolve("model-run-records");
        Files.createDirectories(projectRoot);
        String currentTask = "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nExecute one typed deterministic model attempt.\n\n"
                + "## Task ID\n\n" + ModelAttemptTestFixture.TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the test task.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? currentTask
                            : "content for " + document.path(),
                    StandardCharsets.UTF_8);
        }
        String prompt = "child local typed model prompt";
        Path promptPath = projectRoot.resolve(ModelAttemptTestFixture.TARGET_PATH);
        Files.createDirectories(promptPath.getParent());
        Files.writeString(promptPath, prompt, StandardCharsets.UTF_8);
        WorkItem template = ModelAttemptTestFixture.admitted(
                projectRoot,
                prompt,
                ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(prompt)),
                new DeterministicFakeModelGateway()).workItem();
        ModelWorkPayload templatePayload =
                (ModelWorkPayload) template.workMessage().payload();
        ModelWorkPayload payload = new ModelWorkPayload(
                new ApprovedTaskRevision(
                        ModelAttemptTestFixture.TASK_ID,
                        "CURRENT_TASK.md",
                        sha256(currentTask)),
                templatePayload.snapshotId(),
                templatePayload.allowedTools(),
                templatePayload.executionInput());
        MessageEnvelope templateEnvelope = template.workMessage();
        WorkItem workItem = new WorkItem(
                template.workItemId(),
                template.requiredCapability(),
                new MessageEnvelope(
                        templateEnvelope.messageId(),
                        templateEnvelope.correlationId(),
                        templateEnvelope.causationId(),
                        templateEnvelope.logicalRunId(),
                        templateEnvelope.producer(),
                        templateEnvelope.occurredAt(),
                        payload));
        new FileSpoolMessageTransport(
                        cycleRoot.resolve(IsolatedWorkerMain.WORK_SPOOL),
                        BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL),
                        workItem.workMessage()));

        int exitCode = IsolatedWorkerMain.run(new String[] {
            cycleRoot.toString(),
            projectRoot.toString(),
            evidenceRoot.toString(),
            runRecordRoot.toString(),
            workItem.workItemId(),
            workItem.requiredCapability(),
            ModelAttemptTestFixture.GOAL_ID,
            ModelAttemptTestFixture.AGENT_RUN_ID,
            "1000",
            "20000",
            "65536",
            "2000",
            "0"
        });

        assertEquals(IsolatedWorkerMain.EXIT_RESULT_PUBLISHED, exitCode);
        String reference = AgentRunRecordIdentity.reference(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID);
        assertEquals(
                VerificationStatus.VERIFIED,
                new FileSystemRunRecordStore(runRecordRoot)
                        .resolveModel(reference)
                        .record()
                        .lifecycleRecord()
                        .verification()
                        .status());
        TransportMessage published = FileSpoolMessageTransport.read(
                IsolatedWorkerMain.soleSpooledMessage(
                                cycleRoot.resolve(IsolatedWorkerMain.RESULT_SPOOL))
                        .orElseThrow());
        assertEquals(
                reference,
                ((ResultPayload) published.envelope().payload())
                        .runRecordReference());
        assertEquals(
                workItem.taskRevision().taskId(),
                IsolatedWorkerMain.taskId(workItem.workMessage()));
    }

    @Test
    void rejectsANonRegularExtraWorkPointInsteadOfIgnoringIt()
            throws IOException {
        Path workSpool = Files.createDirectories(
                temporaryRoot.resolve("non-regular-work"));
        Files.writeString(workSpool.resolve("work.transport"), "unread payload");
        Files.createDirectory(workSpool.resolve("foreign.transport"));

        assertThrows(
                IOException.class,
                () -> IsolatedWorkerMain.soleSpooledMessage(workSpool));
    }

    @Test
    void refusesForeignWorkDestinationThroughTheMessageBusBeforeExecution()
            throws IOException {
        Path cycleRoot = temporaryRoot.resolve("cycle");
        Path projectRoot = temporaryRoot.resolve("project");
        Path evidenceRoot = temporaryRoot.resolve("evidence");
        Path runRecordRoot = temporaryRoot.resolve("run-records");
        Files.createDirectories(projectRoot);
        Path target = projectRoot.resolve("TARGET.md");
        Files.writeString(target, "isolated work\n", StandardCharsets.UTF_8);
        String digest = sha256(target);

        MessageEnvelope work = new MessageEnvelope(
                UUID.randomUUID().toString(),
                "correlation-" + UUID.randomUUID(),
                Optional.empty(),
                "run-" + UUID.randomUUID(),
                "scheduler",
                Instant.parse("2026-07-29T12:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "isolated-work-task", "TARGET.md", digest),
                        "a".repeat(64),
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "TARGET.md", digest))));
        assertEquals(
                TransportStatus.ACCEPTED,
                new FileSpoolMessageTransport(
                                cycleRoot.resolve(IsolatedWorkerMain.WORK_SPOOL),
                                BackpressurePolicy.of(1))
                        .send(new TransportMessage(
                                DeliveryDestination.queue("foreign-work"), work))
                        .status());

        int exitCode = IsolatedWorkerMain.run(new String[] {
            cycleRoot.toString(),
            projectRoot.toString(),
            evidenceRoot.toString(),
            runRecordRoot.toString(),
            UUID.randomUUID().toString(),
            "read-file",
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        });

        assertEquals(IsolatedWorkerMain.EXIT_EXECUTION_FAILED, exitCode);
        assertFalse(Files.exists(runRecordRoot));
        assertFalse(Files.exists(cycleRoot.resolve(IsolatedWorkerMain.RESULT_SPOOL)));
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }
}
