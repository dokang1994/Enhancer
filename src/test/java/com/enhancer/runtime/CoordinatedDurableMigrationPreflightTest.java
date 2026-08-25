package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoordinatedDurableMigrationPreflightTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000002521";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000002522";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000002523";
    private static final String ABSENT_GOAL_ID =
            "00000000-0000-0000-0000-000000002524";

    @TempDir
    Path temporaryRoot;

    @Test
    void exactCurrentReadFileClosureIsAlreadyCurrentWithoutWrites()
            throws Exception {
        Fixture fixture = fixture(Set.of("read-file"));

        CoordinatedDurableMigrationPreflightResult result =
                new CoordinatedDurableMigrationPreflight()
                        .inspect(fixture.plan());

        assertEquals(CoordinatedDurableMigrationPreflightStatus.ALREADY_CURRENT,
                result.status());
        assertEquals(Optional.empty(), result.refusalCode());
        assertUnchanged(fixture);
    }

    @Test
    void unprofiledModelWorkRefusesWithRequiredPairBeforeAnyWrite()
            throws Exception {
        Fixture fixture = fixture(Set.of("model-invoke"));

        CoordinatedDurableMigrationPreflightResult result =
                new CoordinatedDurableMigrationPreflight()
                        .inspect(fixture.plan());

        assertEquals(CoordinatedDurableMigrationPreflightStatus.REFUSED,
                result.status());
        assertEquals(Optional.of(
                        CoordinatedDurableMigrationRefusalCode
                                .UNMIGRATABLE_LEGACY_MODEL_WORK),
                result.refusalCode());
        assertEquals(Optional.of(
                        CoordinatedDurableMigrationRefusalDetail.PROFILE_REQUIRED),
                result.refusalDetail());
        assertUnchanged(fixture);
    }

    @Test
    void mixedReadFileScopeUsesReadFilePrecedence() throws Exception {
        Fixture fixture = fixture(Set.of("model-invoke", "read-file"));

        CoordinatedDurableMigrationPreflightResult result =
                new CoordinatedDurableMigrationPreflight()
                        .inspect(fixture.plan());

        assertEquals(CoordinatedDurableMigrationPreflightStatus.ALREADY_CURRENT,
                result.status());
        assertUnchanged(fixture);
    }

    @Test
    void missingNamedRuntimeRefusesAsAPartialClosureWithoutWrites()
            throws Exception {
        Fixture fixture = fixture(Set.of("read-file"));
        CoordinatedDurableMigrationPlan source = fixture.plan();
        CoordinatedDurableMigrationPlan partial = new CoordinatedDurableMigrationPlan(
                source.stoppedOwnerFence(),
                source.expectedFenceBytes(),
                source.manifestRoot(),
                source.submissionIds(),
                source.queueRoot(),
                source.queueId(),
                source.runtimeRoot(),
                List.of(ABSENT_GOAL_ID),
                source.workSpoolPoints(),
                source.resultSpoolPoints(),
                source.ingressSpoolPoints(),
                source.bindingPoints());

        CoordinatedDurableMigrationPreflightResult result =
                new CoordinatedDurableMigrationPreflight().inspect(partial);

        assertEquals(CoordinatedDurableMigrationPreflightStatus.REFUSED,
                result.status());
        assertEquals(Optional.of(
                        CoordinatedDurableMigrationRefusalCode.PARTIAL_CLOSURE),
                result.refusalCode());
        assertEquals(Optional.of(
                        CoordinatedDurableMigrationRefusalDetail
                                .COMPLETE_CLOSURE_REQUIRED),
                result.refusalDetail());
        assertUnchanged(fixture);
    }

    @Test
    void exactIdentityWithDifferentRuntimeContentRefusesCrossStoreMismatch()
            throws Exception {
        Fixture fixture = fixture(Set.of("read-file"), true);

        CoordinatedDurableMigrationPreflightResult result =
                new CoordinatedDurableMigrationPreflight()
                        .inspect(fixture.plan());

        assertEquals(CoordinatedDurableMigrationPreflightStatus.REFUSED,
                result.status());
        assertEquals(Optional.of(
                        CoordinatedDurableMigrationRefusalCode.CROSS_STORE_MISMATCH),
                result.refusalCode());
        assertEquals(Optional.of(
                        CoordinatedDurableMigrationRefusalDetail.EXACT_BINDING_REQUIRED),
                result.refusalDetail());
        assertUnchanged(fixture);
    }

    private Fixture fixture(Set<String> tools) throws Exception {
        return fixture(tools, false);
    }

    private Fixture fixture(Set<String> tools, boolean mismatchedRuntime)
            throws Exception {
        Path manifestRoot = temporaryRoot.resolve("manifests");
        Path queueRoot = temporaryRoot.resolve("queue");
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path fence = temporaryRoot.resolve("stopped-owner.fence");
        byte[] fenceBytes = "externally-held-stopped-owner-fence".getBytes();
        Files.write(fence, fenceBytes);

        MessageEnvelope message = new MessageEnvelope(
                MESSAGE_ID,
                "coordinated-preflight-correlation",
                Optional.empty(),
                "coordinated-preflight-run",
                "coordinated-preflight-test",
                Instant.parse("2026-08-25T07:08:09Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "coordinated-preflight",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        tools));
        DurableSubmissionManifest manifest = new DurableSubmissionManifest(
                QUEUE_ID,
                8,
                "independent-capability",
                message,
                SchedulerPriority.NORMAL);
        new FileSystemSubmissionManifestStore(manifestRoot)
                .storeIdempotently(manifest);
        WorkItem workItem = new WorkItem(
                DurableWorkItemAdmissionHandler.workItemIdFor(MESSAGE_ID),
                manifest.requiredCapability(),
                message);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(queueRoot));
        queue.admitIdempotently(new QueuedWork(workItem, List.of()));
        WorkItem runtimeWorkItem = workItem;
        if (mismatchedRuntime) {
            runtimeWorkItem = new WorkItem(
                    workItem.workItemId(),
                    workItem.requiredCapability(),
                    new MessageEnvelope(
                            message.messageId(),
                            message.correlationId(),
                            message.causationId(),
                            message.logicalRunId(),
                            "different-runtime-producer",
                            message.occurredAt(),
                            message.payload()));
        }
        new FileSystemAgentRuntimeStateStore(runtimeRoot)
                .create(AgentRuntimeState.initial(GOAL_ID, runtimeWorkItem));

        Path manifestArtifact = manifestRoot.resolve(
                MESSAGE_ID + ".submission-manifest");
        Path queueArtifact = queueRoot.resolve(QUEUE_ID + ".scheduler-queue");
        Path runtimeArtifact = runtimeRoot.resolve(GOAL_ID + ".agent-runtime");
        CoordinatedDurableMigrationPlan plan =
                new CoordinatedDurableMigrationPlan(
                        fence,
                        fenceBytes,
                        manifestRoot,
                        List.of(MESSAGE_ID),
                        queueRoot,
                        QUEUE_ID,
                        runtimeRoot,
                        List.of(GOAL_ID),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of());
        return new Fixture(
                plan,
                List.of(manifestArtifact, queueArtifact, runtimeArtifact, fence),
                List.of(
                        Files.readAllBytes(manifestArtifact),
                        Files.readAllBytes(queueArtifact),
                        Files.readAllBytes(runtimeArtifact),
                        Files.readAllBytes(fence)),
                List.of(
                        Files.getLastModifiedTime(manifestArtifact),
                        Files.getLastModifiedTime(queueArtifact),
                        Files.getLastModifiedTime(runtimeArtifact),
                        Files.getLastModifiedTime(fence)));
    }

    private static void assertUnchanged(Fixture fixture) throws Exception {
        for (int index = 0; index < fixture.artifacts().size(); index++) {
            Path artifact = fixture.artifacts().get(index);
            assertArrayEquals(
                    fixture.bytes().get(index), Files.readAllBytes(artifact));
            assertEquals(
                    fixture.times().get(index), Files.getLastModifiedTime(artifact));
        }
    }

    private record Fixture(
            CoordinatedDurableMigrationPlan plan,
            List<Path> artifacts,
            List<byte[]> bytes,
            List<FileTime> times) {
    }
}
