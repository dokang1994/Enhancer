package com.enhancer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableSubmissionManifest;
import com.enhancer.runtime.DurableSubmissionResult;
import com.enhancer.runtime.DurableWorkSubmissionService;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.SchedulerQueueState;
import com.enhancer.runtime.SchedulerQueueStore;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableSubmissionRecoveryIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000a01";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000a02";
    private static final String CAUSATION_ID =
            "00000000-0000-0000-0000-000000000a03";
    private static final String ACTIVE_WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000a04";

    @TempDir
    Path temporaryRoot;

    @Test
    void restartsEveryDurableSubmissionPrefixIntoOneExactAdmission()
            throws Exception {
        verifyRestartAfterManifestPersistence(
                temporaryRoot.resolve("after-manifest"));
        verifyRestartAfterQueueCreation(
                temporaryRoot.resolve("after-queue"));
    }

    @Test
    void existingQueueCapacityMismatchFailsBeforeRecoveryCanMutateQueue()
            throws Exception {
        Path root = temporaryRoot.resolve("capacity-mismatch");
        Path queueRoot = root.resolve("queues");
        FileSystemSchedulerQueueStore queues =
                new FileSystemSchedulerQueueStore(queueRoot);
        DurableSubmissionManifest submission = submission("submission-test");
        DurableSingleWorkerSchedulerQueue existing =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 4, queues);
        existing.enqueue(new QueuedWork(
                new WorkItem(
                        ACTIVE_WORK_ITEM_ID,
                        "read-file-worker",
                        submission.workMessage()),
                Set.of()));
        assertTrue(existing.claimNext().isPresent());
        SchedulerQueueState before = queues.resolve(QUEUE_ID);
        assertEquals(2L, before.revision());
        assertTrue(before.activeWork().isPresent());
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(root.resolve("manifests"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new DurableWorkSubmissionService(manifests, queues)
                        .submit(submission));
        assertEquals(submission, manifests.resolve(MESSAGE_ID));
        SchedulerQueueState after = queues.resolve(QUEUE_ID);
        assertEquals(before.revision(), after.revision());
        assertEquals(before.maxWorkItems(), after.maxWorkItems());
        assertEquals(before.pendingWork(), after.pendingWork());
        assertEquals(before.activeWork(), after.activeWork());
    }

    private void verifyRestartAfterManifestPersistence(Path root) throws Exception {
        Path manifestRoot = root.resolve("manifests");
        Path queueRoot = root.resolve("queues");
        DurableSubmissionManifest submission = submission("submission-test");
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(manifestRoot);
        FileSystemSchedulerQueueStore queues =
                new FileSystemSchedulerQueueStore(queueRoot);

        DurableWorkSubmissionService interrupted = new DurableWorkSubmissionService(
                manifests,
                new FailingSchedulerQueueStore(queues, Failure.CREATE));
        assertThrows(IOException.class, () -> interrupted.submit(submission));
        assertEquals(submission, manifests.resolve(MESSAGE_ID));
        assertThrows(IOException.class, () -> queues.resolve(QUEUE_ID));

        DurableSubmissionResult result =
                new DurableWorkSubmissionService(manifests, queues).submit(submission);
        assertFalse(result.manifestCreated());
        assertTrue(result.queueCreated());
        assertTrue(result.workAdmitted());
        assertOneAdmission(queueRoot, 1L);
        assertExactReplayAndChangedContentFailClosed(
                submission, manifests, queues, queueRoot, 1L);
    }

    private void verifyRestartAfterQueueCreation(Path root) throws Exception {
        Path manifestRoot = root.resolve("manifests");
        Path queueRoot = root.resolve("queues");
        DurableSubmissionManifest submission = submission("submission-test");
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(manifestRoot);
        FileSystemSchedulerQueueStore queues =
                new FileSystemSchedulerQueueStore(queueRoot);

        DurableWorkSubmissionService interrupted = new DurableWorkSubmissionService(
                manifests,
                new FailingSchedulerQueueStore(queues, Failure.UPDATE));
        assertThrows(IOException.class, () -> interrupted.submit(submission));
        assertEquals(submission, manifests.resolve(MESSAGE_ID));
        DurableSingleWorkerSchedulerQueue empty =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, queues);
        assertEquals(0L, empty.revision());
        assertEquals(0, empty.pendingCount());

        DurableSubmissionResult result =
                new DurableWorkSubmissionService(manifests, queues).submit(submission);
        assertFalse(result.manifestCreated());
        assertFalse(result.queueCreated());
        assertTrue(result.workAdmitted());
        assertOneAdmission(queueRoot, 1L);
        assertExactReplayAndChangedContentFailClosed(
                submission, manifests, queues, queueRoot, 1L);
    }

    private void assertExactReplayAndChangedContentFailClosed(
            DurableSubmissionManifest submission,
            FileSystemSubmissionManifestStore manifests,
            FileSystemSchedulerQueueStore queues,
            Path queueRoot,
            long expectedRevision) throws Exception {
        DurableSubmissionResult replay =
                new DurableWorkSubmissionService(manifests, queues).submit(submission);
        assertFalse(replay.manifestCreated());
        assertFalse(replay.queueCreated());
        assertFalse(replay.workAdmitted());
        assertOneAdmission(queueRoot, expectedRevision);

        DurableSubmissionManifest changed = submission("changed-producer");
        assertThrows(
                IllegalArgumentException.class,
                () -> new DurableWorkSubmissionService(manifests, queues)
                        .submit(changed));
        assertOneAdmission(queueRoot, expectedRevision);
    }

    private void assertOneAdmission(Path queueRoot, long expectedRevision)
            throws Exception {
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(queueRoot));
        assertEquals(expectedRevision, recovered.revision());
        assertEquals(8, recovered.maxWorkItems());
        assertEquals(1, recovered.pendingCount());
    }

    private DurableSubmissionManifest submission(String producer) {
        MessageEnvelope envelope = new MessageEnvelope(
                MESSAGE_ID,
                "submission-recovery-correlation",
                Optional.of(CAUSATION_ID),
                "submission-recovery-logical-run",
                producer,
                Instant.parse("2026-07-22T15:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "persist-durable-submission-intent-before-queue-admission",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "CURRENT_TASK.md",
                                "c".repeat(64)))));
        return new DurableSubmissionManifest(
                QUEUE_ID,
                8,
                "read-file-worker",
                envelope);
    }

    private enum Failure {
        CREATE,
        UPDATE
    }

    private static final class FailingSchedulerQueueStore
            implements SchedulerQueueStore {
        private final SchedulerQueueStore delegate;
        private final Failure failure;

        private FailingSchedulerQueueStore(
                SchedulerQueueStore delegate,
                Failure failure) {
            this.delegate = delegate;
            this.failure = failure;
        }

        @Override
        public void create(SchedulerQueueState initialState) throws IOException {
            if (failure == Failure.CREATE) {
                throw new IOException("simulated interruption before queue creation");
            }
            delegate.create(initialState);
        }

        @Override
        public void update(SchedulerQueueState nextState) throws IOException {
            if (failure == Failure.UPDATE) {
                throw new IOException("simulated interruption before work admission");
            }
            delegate.update(nextState);
        }

        @Override
        public SchedulerQueueState resolve(String queueId) throws IOException {
            return delegate.resolve(queueId);
        }
    }
}
