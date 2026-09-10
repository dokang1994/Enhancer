package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableWorkSubmissionServiceTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000272";

    @TempDir
    Path temporaryRoot;

    @Test
    void persistedManifestAdmissionSkipsManifestStoreAndPreservesCreationStatus()
            throws Exception {
        CountingManifestStore manifests = new CountingManifestStore();
        DurableWorkSubmissionService service =
                new DurableWorkSubmissionService(
                        manifests,
                        new FileSystemSchedulerQueueStore(
                                temporaryRoot.resolve("persisted-queues")));

        DurableSubmissionResult result =
                service.submitPersisted(manifest(), true);

        assertEquals(0, manifests.storeCalls());
        assertTrue(result.manifestCreated());
        assertTrue(result.queueCreated());
        assertTrue(result.workAdmitted());
        assertEquals(1L, result.queueRevision());
    }

    @Test
    void ordinarySubmissionStillStoresManifestExactlyOnce() throws Exception {
        CountingManifestStore manifests = new CountingManifestStore();
        DurableWorkSubmissionService service =
                new DurableWorkSubmissionService(
                        manifests,
                        new FileSystemSchedulerQueueStore(
                                temporaryRoot.resolve("ordinary-queues")));

        DurableSubmissionResult result = service.submit(manifest());

        assertEquals(1, manifests.storeCalls());
        assertTrue(result.manifestCreated());
        assertTrue(result.queueCreated());
        assertTrue(result.workAdmitted());
    }

    private DurableSubmissionManifest manifest() {
        return new DurableSubmissionManifest(
                QUEUE_ID,
                4,
                "deterministic-echo",
                ModelWorkFixtures.envelope(),
                SchedulerPriority.EXPEDITED);
    }

    private static final class CountingManifestStore
            implements SubmissionManifestStore {
        private int storeCalls;

        @Override
        public boolean storeIdempotently(DurableSubmissionManifest manifest) {
            storeCalls++;
            return true;
        }

        @Override
        public DurableSubmissionManifest resolve(String submissionId) {
            throw new AssertionError("resolve must not be called directly");
        }

        private int storeCalls() {
            return storeCalls;
        }
    }
}
