package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.TransportStatus;
import com.enhancer.context.RequiredProjectDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiverTest {
    private static final String TASK_ID = "typed-model-receive";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f77";

    @TempDir
    Path temporaryRoot;

    @Test
    void pendingThenAcknowledgedReplayAndDuplicatePointConvergeToOneWorkItem()
            throws Exception {
        Layout layout = layout("recovery");
        writeGovernedProject(layout.projectRoot());
        FileSystemDeterministicFakeModelWorkPublisher publisher =
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 4);
        DeterministicFakeModelWorkPublicationResult first = publish(publisher);
        String firstFile = first.publication().messageFile().orElseThrow();
        FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver receiver =
                new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot());

        ManifestAuthorizedModelWorkPointReceiveResult admitted =
                receiver.receive(firstFile);
        ManifestAuthorizedModelWorkPointReceiveResult acknowledgedReplay =
                receiver.receive(firstFile);
        DeterministicFakeModelWorkPublicationResult duplicate = publish(publisher);
        ManifestAuthorizedModelWorkPointReceiveResult duplicateReplay = receiver.receive(
                duplicate.publication().messageFile().orElseThrow());

        assertEquals(DurableWorkMessageReceiveStatus.ADMITTED,
                admitted.admission().status());
        assertEquals("ACKNOWLEDGED", admitted.spoolStatus());
        assertEquals(SUBMISSION_ID, admitted.submissionId());
        assertEquals(DurableWorkMessageReceiveStatus.REPLAYED,
                acknowledgedReplay.admission().status());
        assertEquals("ALREADY_ACKNOWLEDGED", acknowledgedReplay.spoolStatus());
        assertEquals(DurableWorkMessageReceiveStatus.REPLAYED,
                duplicateReplay.admission().status());
        assertFalse(Files.exists(layout.spoolRoot().resolve(firstFile)));
        assertTrue(Files.isRegularFile(layout.spoolRoot().resolve(
                firstFile.replace(".transport", ".received"))));
        SchedulerQueueState queue = new FileSystemSchedulerQueueStore(
                layout.queueRoot()).resolve(GeneratedSubmissionIdentities.derive(
                        SUBMISSION_ID).queueId());
        assertEquals(1L, queue.revision());
        assertEquals(1, queue.pendingWork().size());
    }

    @Test
    void existingEmptyQueueContinuesThroughAdmissionAndAcknowledgement()
            throws Exception {
        Layout layout = layout("queue-only");
        writeGovernedProject(layout.projectRoot());
        DeterministicFakeModelWorkPublicationResult publication = publish(
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 1));
        String queueId = GeneratedSubmissionIdentities.derive(SUBMISSION_ID).queueId();
        DurableSingleWorkerSchedulerQueue.create(
                queueId, 8, new FileSystemSchedulerQueueStore(layout.queueRoot()));

        ManifestAuthorizedModelWorkPointReceiveResult received =
                new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot())
                        .receive(publication.publication().messageFile().orElseThrow());

        assertEquals(DurableWorkMessageReceiveStatus.ADMITTED,
                received.admission().status());
        assertEquals(1L, received.admission().queueRevision());
        assertEquals("ACKNOWLEDGED", received.spoolStatus());
    }

    @Test
    void acknowledgementReleasesPendingPublicationCapacity() throws Exception {
        Layout layout = layout("capacity-release");
        writeGovernedProject(layout.projectRoot());
        FileSystemDeterministicFakeModelWorkPublisher publisher =
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 1);
        DeterministicFakeModelWorkPublicationResult first = publish(publisher);
        assertEquals(TransportStatus.ACCEPTED,
                first.publication().outcome().status());

        new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot())
                .receive(first.publication().messageFile().orElseThrow());
        DeterministicFakeModelWorkPublicationResult second = publish(publisher);

        assertEquals(TransportStatus.ACCEPTED,
                second.publication().outcome().status());
        assertTrue(second.publication().messageFile().isPresent());
    }

    @Test
    void symbolicPointFailsClosedWhenHostAllowsSymbolicLinks() throws Exception {
        Layout layout = layout("symbolic-point");
        writeGovernedProject(layout.projectRoot());
        DeterministicFakeModelWorkPublicationResult publication = publish(
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 1));
        String file = publication.publication().messageFile().orElseThrow();
        Path point = layout.spoolRoot().resolve(file);
        Path target = temporaryRoot.resolve("foreign-transport-point");
        Files.move(point, target);
        try {
            Files.createSymbolicLink(point, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic links unavailable on this host");
        }

        assertThrows(IllegalArgumentException.class,
                () -> new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot())
                        .receive(file));
        assertTrue(Files.isRegularFile(target));
        assertFalse(Files.exists(layout.queueRoot()));
    }

    @Test
    void pendingAndAcknowledgedCollisionFailsBeforeQueueMutation() throws Exception {
        Layout layout = layout("collision");
        writeGovernedProject(layout.projectRoot());
        DeterministicFakeModelWorkPublicationResult publication = publish(
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 2));
        String file = publication.publication().messageFile().orElseThrow();
        Path pending = layout.spoolRoot().resolve(file);
        Files.copy(pending, layout.spoolRoot().resolve(
                file.replace(".transport", ".received")));

        assertThrows(IllegalArgumentException.class,
                () -> new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot())
                        .receive(file));
        assertFalse(Files.exists(layout.queueRoot()));
    }

    @Test
    void oversizedPointIsBoundedAndNeverAcknowledgedOrAdmitted() throws Exception {
        Layout layout = layout("oversized");
        Files.createDirectories(layout.spoolRoot());
        String file = "00000000-0000-0000-0000-000000000f78.transport";
        Path pending = layout.spoolRoot().resolve(file);
        Files.write(pending, new byte[FileSpoolMessageTransport.MAX_FRAME_BYTES + 1]);

        assertThrows(IOException.class,
                () -> new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot())
                        .receive(file));
        assertTrue(Files.isRegularFile(pending));
        assertFalse(Files.exists(layout.spoolRoot().resolve(
                file.replace(".transport", ".received"))));
        assertFalse(Files.exists(layout.queueRoot()));
    }

    @Test
    void acknowledgementFailureLeavesPendingAfterAdmissionAndRetryConverges()
            throws Exception {
        Layout layout = layout("ack-failure");
        writeGovernedProject(layout.projectRoot());
        DeterministicFakeModelWorkPublicationResult publication = publish(
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 2));
        String file = publication.publication().messageFile().orElseThrow();
        FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver interrupted =
                new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(),
                        new FileSystemSubmissionManifestStore(layout.submissionRoot()),
                        new FileSystemSchedulerQueueStore(layout.queueRoot()),
                        (pending, acknowledged) -> {
                            throw new IOException("simulated ACK failure");
                        });

        assertThrows(IOException.class, () -> interrupted.receive(file));
        assertTrue(Files.isRegularFile(layout.spoolRoot().resolve(file)));
        SchedulerQueueState afterFailure = new FileSystemSchedulerQueueStore(
                layout.queueRoot()).resolve(GeneratedSubmissionIdentities.derive(
                        SUBMISSION_ID).queueId());
        assertEquals(1L, afterFailure.revision());

        ManifestAuthorizedModelWorkPointReceiveResult recovered =
                new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), layout.submissionRoot(), layout.queueRoot())
                        .receive(file);
        assertEquals(DurableWorkMessageReceiveStatus.REPLAYED,
                recovered.admission().status());
        assertEquals("ACKNOWLEDGED", recovered.spoolStatus());
        assertEquals(1L, new FileSystemSchedulerQueueStore(layout.queueRoot())
                .resolve(afterFailure.queueId()).revision());
    }

    @Test
    void missingManifestLeavesPendingAndQueueAbsent() throws Exception {
        Layout layout = layout("missing-manifest");
        writeGovernedProject(layout.projectRoot());
        DeterministicFakeModelWorkPublicationResult publication = publish(
                new FileSystemDeterministicFakeModelWorkPublisher(
                        layout.projectRoot(), layout.submissionRoot(),
                        layout.spoolRoot(), 2));
        String file = publication.publication().messageFile().orElseThrow();

        assertThrows(MissingSubmissionManifestException.class,
                () -> new FileSystemManifestAuthorizedDeterministicFakeModelWorkReceiver(
                        layout.spoolRoot(), temporaryRoot.resolve("empty-manifests"),
                        layout.queueRoot()).receive(file));
        assertTrue(Files.isRegularFile(layout.spoolRoot().resolve(file)));
        assertFalse(Files.exists(layout.spoolRoot().resolve(
                file.replace(".transport", ".received"))));
        assertFalse(Files.exists(layout.queueRoot()));
    }

    private DeterministicFakeModelWorkPublicationResult publish(
            FileSystemDeterministicFakeModelWorkPublisher publisher) throws Exception {
        return publisher.publish(
                SUBMISSION_ID, TASK_ID, "typed-receiver-test",
                "prompts/request.txt", "a".repeat(64),
                ModelWorkFixtures.profile("different-requirement"), 8,
                SchedulerPriority.EXPEDITED);
    }

    private void writeGovernedProject(Path projectRoot) throws Exception {
        String task = "# Current Task\n\n## Status\n\nIn Progress\n\n"
                + "## Task\n\nReceive typed model work.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by test owner.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? task : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(root.resolve("project"), root.resolve("submissions"),
                root.resolve("spool"), root.resolve("queue"));
    }

    private record Layout(
            Path projectRoot, Path submissionRoot, Path spoolRoot, Path queueRoot) {
    }
}
