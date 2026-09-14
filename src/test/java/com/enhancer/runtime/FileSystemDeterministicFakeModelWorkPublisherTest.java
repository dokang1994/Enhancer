package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportStatus;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.model.ModelExecutionProfile;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemDeterministicFakeModelWorkPublisherTest {
    private static final String TASK_ID = "typed-model-publisher";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f37";

    @TempDir
    Path temporaryRoot;

    @Test
    void exposesOnlyThreeRootsOneTransportBoundAndEightSemanticInputs()
            throws Exception {
        assertTrue(Modifier.isPublic(
                FileSystemDeterministicFakeModelWorkPublisher.class.getModifiers()));
        assertTrue(Modifier.isFinal(
                FileSystemDeterministicFakeModelWorkPublisher.class.getModifiers()));
        assertTrue(Modifier.isPublic(
                FileSystemDeterministicFakeModelWorkPublisher.class.getConstructor(
                        Path.class, Path.class, Path.class, int.class).getModifiers()));
        assertEquals(1, Arrays.stream(
                        FileSystemDeterministicFakeModelWorkPublisher.class
                                .getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .count());

        Method publish = FileSystemDeterministicFakeModelWorkPublisher.class.getMethod(
                "publish",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                ModelExecutionProfile.class,
                int.class,
                SchedulerPriority.class);
        assertEquals(DeterministicFakeModelWorkPublicationResult.class,
                publish.getReturnType());
        assertArrayEquals(new String[] {"publish"},
                Arrays.stream(FileSystemDeterministicFakeModelWorkPublisher.class
                                .getDeclaredMethods())
                        .filter(method -> Modifier.isPublic(method.getModifiers()))
                        .map(Method::getName)
                        .sorted()
                        .toArray(String[]::new));
    }

    @Test
    void firstUsePersistsManifestThenPublishesOnlyItsExactDerivedRouteAndEnvelope()
            throws Exception {
        Path project = temporaryRoot.resolve("first-project");
        Path submissions = temporaryRoot.resolve("first-submissions");
        Path spool = temporaryRoot.resolve("first-spool");
        writeGovernedProject(project);

        DeterministicFakeModelWorkPublicationResult result = publisher(
                project, submissions, spool, 4).publish(
                        SUBMISSION_ID,
                        TASK_ID,
                        "typed-publisher-test",
                        "prompts/request.txt",
                        "a".repeat(64),
                        ModelWorkFixtures.profile("different-requirement"),
                        8,
                        SchedulerPriority.EXPEDITED);

        DurableSubmissionManifest manifest =
                new FileSystemSubmissionManifestStore(submissions).resolve(SUBMISSION_ID);
        String messageFile = result.publication().messageFile().orElseThrow();
        TransportMessage transported =
                FileSpoolMessageTransport.read(spool.resolve(messageFile));
        assertEquals(TransportStatus.ACCEPTED, result.publication().outcome().status());
        assertTrue(result.manifestCreated());
        assertEquals(SUBMISSION_ID, result.submissionId());
        assertEquals(manifest.queueId(), result.queueId());
        assertEquals(DeliveryDestination.queue(manifest.queueId()),
                transported.destination());
        assertEquals(manifest.workMessage(), transported.envelope());
        assertEquals(((com.enhancer.bus.ModelWorkPayload) manifest.workMessage().payload())
                .snapshotId(), result.workspaceSnapshotId());
        assertFalse(Files.exists(temporaryRoot.resolve("queue")));
    }

    @Test
    void manifestOnlyFailureReplaysWithoutProjectRecaptureAndKeepsManifestBytes()
            throws Exception {
        Path project = temporaryRoot.resolve("recovery-project");
        Path submissions = temporaryRoot.resolve("recovery-submissions");
        Path unavailable = Files.writeString(
                temporaryRoot.resolve("occupied-spool"), "occupied", StandardCharsets.UTF_8);
        writeGovernedProject(project);
        ModelExecutionProfile profile = ModelWorkFixtures.profile("different-requirement");

        DeterministicFakeModelWorkPublicationResult refused = publisher(
                project, submissions, unavailable, 1).publish(
                        SUBMISSION_ID, TASK_ID, "typed-publisher-test",
                        "prompts/request.txt", "a".repeat(64), profile, 8,
                        SchedulerPriority.EXPEDITED);
        Path manifestPath = submissions.resolve(SUBMISSION_ID + ".submission-manifest");
        byte[] before = Files.readAllBytes(manifestPath);
        Path recoveredSpool = temporaryRoot.resolve("recovered-spool");

        DeterministicFakeModelWorkPublicationResult replay = publisher(
                temporaryRoot.resolve("missing-project"),
                submissions,
                recoveredSpool,
                1).publish(
                        SUBMISSION_ID, TASK_ID, "typed-publisher-test",
                        "prompts/request.txt", "a".repeat(64), profile, 8,
                        SchedulerPriority.EXPEDITED);

        assertEquals(TransportStatus.UNAVAILABLE,
                refused.publication().outcome().status());
        assertTrue(refused.manifestCreated());
        assertEquals(TransportStatus.ACCEPTED, replay.publication().outcome().status());
        assertFalse(replay.manifestCreated());
        assertArrayEquals(before, Files.readAllBytes(manifestPath));
    }

    @Test
    void exactAcceptedRetriesCreateBoundedDuplicatePointsAndThenBackpressure()
            throws Exception {
        Path project = temporaryRoot.resolve("duplicate-project");
        Path submissions = temporaryRoot.resolve("duplicate-submissions");
        Path spool = temporaryRoot.resolve("duplicate-spool");
        writeGovernedProject(project);
        FileSystemDeterministicFakeModelWorkPublisher publisher = publisher(
                project, submissions, spool, 2);
        ModelExecutionProfile profile = ModelWorkFixtures.profile("different-requirement");

        DeterministicFakeModelWorkPublicationResult first = publisher.publish(
                SUBMISSION_ID, TASK_ID, "typed-publisher-test",
                "prompts/request.txt", "a".repeat(64), profile, 8,
                SchedulerPriority.EXPEDITED);
        Path manifestPath = submissions.resolve(SUBMISSION_ID + ".submission-manifest");
        byte[] manifestBeforeConflict = Files.readAllBytes(manifestPath);
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(
                SUBMISSION_ID, TASK_ID, "changed-producer",
                "prompts/request.txt", "a".repeat(64), profile, 8,
                SchedulerPriority.EXPEDITED));
        assertArrayEquals(manifestBeforeConflict, Files.readAllBytes(manifestPath));
        try (var points = Files.list(spool)) {
            assertEquals(1, points.filter(Files::isRegularFile).count());
        }
        DeterministicFakeModelWorkPublicationResult second = publisher.publish(
                SUBMISSION_ID, TASK_ID, "typed-publisher-test",
                "prompts/request.txt", "a".repeat(64), profile, 8,
                SchedulerPriority.EXPEDITED);
        DeterministicFakeModelWorkPublicationResult third = publisher.publish(
                SUBMISSION_ID, TASK_ID, "typed-publisher-test",
                "prompts/request.txt", "a".repeat(64), profile, 8,
                SchedulerPriority.EXPEDITED);

        assertEquals(TransportStatus.ACCEPTED, first.publication().outcome().status());
        assertEquals(TransportStatus.ACCEPTED, second.publication().outcome().status());
        assertFalse(second.manifestCreated());
        assertEquals(TransportStatus.BACKPRESSURED, third.publication().outcome().status());
        assertTrue(third.publication().messageFile().isEmpty());
        try (var points = Files.list(spool)) {
            assertEquals(2, points.filter(Files::isRegularFile).count());
        }
        byte[] firstBytes = Files.readAllBytes(spool.resolve(
                first.publication().messageFile().orElseThrow()));
        byte[] secondBytes = Files.readAllBytes(spool.resolve(
                second.publication().messageFile().orElseThrow()));
        assertArrayEquals(firstBytes, secondBytes);
    }

    private FileSystemDeterministicFakeModelWorkPublisher publisher(
            Path project, Path submissions, Path spool, int maximum) {
        return new FileSystemDeterministicFakeModelWorkPublisher(
                project, submissions, spool, maximum);
    }

    private void writeGovernedProject(Path projectRoot) throws Exception {
        String task = "# Current Task\n\n## Status\n\nIn Progress\n\n"
                + "## Task\n\nPublish typed model work.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? task
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }
}
