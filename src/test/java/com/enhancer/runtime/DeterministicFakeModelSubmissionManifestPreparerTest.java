package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicFakeModelSubmissionManifestPreparerTest {
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000271";
    private static final String TASK_ID = "prepare-model-submission-manifest";
    private static final String PRODUCER = "manifest-preparer-test";
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-09-10T07:00:00.123456789Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void firstUsePersistsTheExactManifestWithoutAnyQueueDependency()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        writeGovernedProject(projectRoot);
        Path manifestRoot = temporaryRoot.resolve("manifests");
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(manifestRoot);
        ModelExecutionProfile profile =
                ModelWorkFixtures.profile("profile-only-capability");

        PreparedDeterministicFakeModelSubmission prepared =
                preparer(projectRoot, manifests, Clock.fixed(
                        OCCURRED_AT, ZoneOffset.UTC)).prepare(request(profile));

        assertTrue(prepared.manifestCreated());
        assertEquals(
                manifests.resolve(SUBMISSION_ID),
                prepared.manifest());
        assertEquals(
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID).queueId(),
                prepared.manifest().queueId());
        assertEquals(4, prepared.manifest().maxWorkItems());
        assertEquals(
                "deterministic-echo",
                prepared.manifest().requiredCapability());
        assertEquals(SchedulerPriority.EXPEDITED, prepared.manifest().priority());
        assertEquals(
                profile,
                prepared.manifest().workMessage().payload()
                        instanceof com.enhancer.bus.ModelWorkPayload payload
                                ? payload.executionInput().executionProfile()
                                : null);
        assertFalse(Files.exists(temporaryRoot.resolve("queues")));
    }

    @Test
    void exactReplayUsesTheStoredManifestBeforeRepositoryOrClockAndDoesNotRewrite()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("replay-project");
        writeGovernedProject(projectRoot);
        Path manifestRoot = temporaryRoot.resolve("replay-manifests");
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(manifestRoot);
        DeterministicFakeModelSubmissionRequest request =
                request(ModelWorkFixtures.profile("profile-only-capability"));
        PreparedDeterministicFakeModelSubmission first =
                preparer(projectRoot, manifests, Clock.fixed(
                        OCCURRED_AT, ZoneOffset.UTC)).prepare(request);
        Path artifact = manifestRoot.resolve(
                SUBMISSION_ID + ".submission-manifest");
        byte[] before = Files.readAllBytes(artifact);

        PreparedDeterministicFakeModelSubmission replay =
                preparer(
                        temporaryRoot.resolve("missing-project"),
                        manifests,
                        new ThrowingClock()).prepare(request);

        assertFalse(replay.manifestCreated());
        assertEquals(first.manifest(), replay.manifest());
        assertArrayEquals(before, Files.readAllBytes(artifact));
    }

    @Test
    void replayConflictFailsBeforeClockAndLeavesTheManifestUnchanged()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("conflict-project");
        writeGovernedProject(projectRoot);
        Path manifestRoot = temporaryRoot.resolve("conflict-manifests");
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(manifestRoot);
        ModelExecutionProfile profile =
                ModelWorkFixtures.profile("profile-only-capability");
        preparer(projectRoot, manifests, Clock.fixed(
                OCCURRED_AT, ZoneOffset.UTC)).prepare(request(profile));
        Path artifact = manifestRoot.resolve(
                SUBMISSION_ID + ".submission-manifest");
        byte[] before = Files.readAllBytes(artifact);
        DeterministicFakeModelSubmissionRequest changed =
                new DeterministicFakeModelSubmissionRequest(
                        SUBMISSION_ID,
                        TASK_ID,
                        "changed-producer",
                        "prompts/model.txt",
                        "a".repeat(64),
                        profile,
                        4,
                        SchedulerPriority.EXPEDITED);

        assertThrows(
                IllegalArgumentException.class,
                () -> preparer(
                        temporaryRoot.resolve("missing-conflict-project"),
                        manifests,
                        new ThrowingClock()).prepare(changed));
        assertArrayEquals(before, Files.readAllBytes(artifact));
    }

    private DeterministicFakeModelSubmissionManifestPreparer preparer(
            Path projectRoot,
            SubmissionManifestStore manifests,
            Clock clock) {
        return new DeterministicFakeModelSubmissionManifestPreparer(
                projectRoot,
                manifests,
                clock,
                new ProjectContextReader(),
                new ApprovedTaskReader(),
                new RepositoryMemorySnapshotCollector());
    }

    private DeterministicFakeModelSubmissionRequest request(
            ModelExecutionProfile profile) {
        return new DeterministicFakeModelSubmissionRequest(
                SUBMISSION_ID,
                TASK_ID,
                PRODUCER,
                "prompts/model.txt",
                "a".repeat(64),
                profile,
                4,
                SchedulerPriority.EXPEDITED);
    }

    private void writeGovernedProject(Path projectRoot) throws Exception {
        String task = "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nPrepare one typed manifest.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the test owner.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? task
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private static final class ThrowingClock extends Clock {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            throw new AssertionError("clock must not be consulted");
        }
    }
}
