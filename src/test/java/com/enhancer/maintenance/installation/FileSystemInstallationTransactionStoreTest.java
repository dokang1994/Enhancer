package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemInstallationTransactionStoreTest {
    private static final String PERMISSION_POLICY_SHA256 = "f".repeat(64);

    @TempDir
    Path root;

    @Test
    void createsResolvesAcrossInstancesAndExactReplayDoesNotRewrite() throws Exception {
        InstallationTransactionState initial = initialState("activation-new");
        FileSystemInstallationTransactionStore first =
                new FileSystemInstallationTransactionStore(root);

        InstallationTransactionStore.Mutation created = first.create(initial);
        Path artifact = transactionPath(initial.plan().transactionId());
        byte[] bytes = Files.readAllBytes(artifact);
        FileTime marker = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(artifact, marker);
        InstallationTransactionStore.Mutation replayed =
                new FileSystemInstallationTransactionStore(root).create(initial);

        assertEquals(InstallationTransactionStore.MutationDisposition.CREATED,
                created.disposition());
        assertEquals(InstallationTransactionStore.MutationDisposition.EXACT_REPLAY,
                replayed.disposition());
        assertEquals(initial,
                new FileSystemInstallationTransactionStore(root)
                        .resolve(initial.plan().transactionId()));
        assertArrayEquals(bytes, Files.readAllBytes(artifact));
        assertEquals(marker, Files.getLastModifiedTime(artifact));
    }

    @Test
    void compareAndExchangeAdvancesOnceAndRejectsStaleOrInvalidState() throws Exception {
        InstallationTransactionState initial = initialState("activation-new");
        InstallationTransactionState succeeded = initial.markSucceeded(evidence(initial, "1"));
        FileSystemInstallationTransactionStore store =
                new FileSystemInstallationTransactionStore(root);
        store.create(initial);

        InstallationTransactionStore.Mutation advanced = store.compareAndExchange(
                initial.plan().transactionId(), initial.revision(), succeeded);
        Path artifact = transactionPath(initial.plan().transactionId());
        byte[] advancedBytes = Files.readAllBytes(artifact);
        FileTime marker = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(artifact, marker);
        InstallationTransactionStore.Mutation replayed = store.compareAndExchange(
                initial.plan().transactionId(), initial.revision(), succeeded);

        assertEquals(InstallationTransactionStore.MutationDisposition.ADVANCED,
                advanced.disposition());
        assertEquals(InstallationTransactionStore.MutationDisposition.EXACT_REPLAY,
                replayed.disposition());
        assertArrayEquals(advancedBytes, Files.readAllBytes(artifact));
        assertEquals(marker, Files.getLastModifiedTime(artifact));

        InstallationTransactionStoreException stale = assertThrows(
                InstallationTransactionStoreException.class,
                () -> store.compareAndExchange(
                        initial.plan().transactionId(), initial.revision(),
                        initial.markSucceeded(evidence(initial, "2"))));
        assertEquals(InstallationTransactionStoreException.Reason.REVISION_CONFLICT,
                stale.reason());

        InstallationTransactionState nextSucceeded = succeeded.beginNext()
                .markSucceeded(evidence(succeeded.beginNext(), "3"));
        InstallationTransactionStoreException invalid = assertThrows(
                InstallationTransactionStoreException.class,
                () -> store.compareAndExchange(
                        initial.plan().transactionId(), succeeded.revision(), nextSucceeded));
        assertEquals(InstallationTransactionStoreException.Reason.INVALID_TRANSITION,
                invalid.reason());
        assertEquals(succeeded, store.resolve(initial.plan().transactionId()));
    }

    @Test
    void createConflictAndStableLockContentionAreTypedAndMutationFree() throws Exception {
        InstallationTransactionState initial = initialState("activation-new");
        FileSystemInstallationTransactionStore store =
                new FileSystemInstallationTransactionStore(root);
        store.create(initial);

        InstallationTransactionStoreException conflict = assertThrows(
                InstallationTransactionStoreException.class,
                () -> store.create(initialState("activation-other")));
        assertEquals(InstallationTransactionStoreException.Reason.TRANSACTION_CONFLICT,
                conflict.reason());

        UUID otherTransaction = UUID.fromString("00000000-0000-0000-0000-000000000999");
        Path lockPath = root.resolve(InstallationRecordFileNames.transactionLock(
                otherTransaction));
        try (FileChannel channel = FileChannel.open(
                    lockPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
                FileLock heldLock = channel.tryLock()) {
            assertEquals(true, heldLock.isValid());
            InstallationTransactionState other = stateWithTransaction(otherTransaction);
            FileSystemInstallationTransactionStore second =
                    new FileSystemInstallationTransactionStore(root);
            InstallationTransactionStoreException contended = assertThrows(
                    InstallationTransactionStoreException.class,
                    () -> second.create(other));
            assertEquals(InstallationTransactionStoreException.Reason.LOCK_CONTENDED,
                    contended.reason());
            assertEquals(false, Files.exists(
                    transactionPath(otherTransaction), LinkOption.NOFOLLOW_LINKS));
        }
    }

    @Test
    void rejectsCorruptOversizedNonRegularAndInvalidRoots() throws Exception {
        InstallationTransactionState initial = initialState("activation-new");
        FileSystemInstallationTransactionStore store =
                new FileSystemInstallationTransactionStore(root);
        store.create(initial);
        Path artifact = transactionPath(initial.plan().transactionId());
        Files.write(artifact, new byte[] {0, 1, 2}, StandardOpenOption.TRUNCATE_EXISTING);

        assertReason(InstallationTransactionStoreException.Reason.CORRUPT_STATE,
                () -> store.resolve(initial.plan().transactionId()));

        Files.write(artifact,
                new byte[InstallationIntegrityEnvelope.HEADER_BYTES
                        + InstallationTransactionFileFormat.MAX_BODY_BYTES + 1],
                StandardOpenOption.TRUNCATE_EXISTING);
        assertReason(InstallationTransactionStoreException.Reason.CAPACITY_EXCEEDED,
                () -> store.resolve(initial.plan().transactionId()));

        Files.delete(artifact);
        Files.createDirectory(artifact);
        assertReason(InstallationTransactionStoreException.Reason.CORRUPT_STATE,
                () -> store.resolve(initial.plan().transactionId()));

        Files.delete(artifact);
        UUID foreignId = UUID.fromString("00000000-0000-0000-0000-000000000999");
        Files.write(artifact, InstallationTransactionFileFormat.encode(
                stateWithTransaction(foreignId)));
        assertReason(InstallationTransactionStoreException.Reason.CORRUPT_STATE,
                () -> store.resolve(initial.plan().transactionId()));

        UUID lockedId = UUID.fromString("00000000-0000-0000-0000-000000000998");
        Files.createDirectory(root.resolve(
                InstallationRecordFileNames.transactionLock(lockedId)));
        assertReason(InstallationTransactionStoreException.Reason.STORE_UNAVAILABLE,
                () -> store.create(stateWithTransaction(lockedId)));

        assertReason(InstallationTransactionStoreException.Reason.STORE_UNAVAILABLE,
                () -> new FileSystemInstallationTransactionStore(Path.of("relative-root")));
    }

    private Path transactionPath(UUID transactionId) {
        return root.resolve(InstallationRecordFileNames.transaction(transactionId));
    }

    private static InstallationTransactionState initialState(String activation) {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        return InstallationTransactionState.start(
                plan,
                environment(plan),
                "release-v1",
                PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"),
                activation);
    }

    private static InstallationTransactionState stateWithTransaction(UUID transactionId) {
        CancellationTrustInstallationPlan source =
                CancellationTrustInstallationPlanTest.validPlan();
        CancellationTrustInstallationPlan plan = new CancellationTrustInstallationPlan(
                transactionId,
                source.operation(),
                source.principals(),
                source.installationRoot(),
                source.applicationJar(),
                source.runtimeDistributionRoot(),
                source.operatorDistributionRoot(),
                source.operatorCandidateInbox(),
                source.activationPoint(),
                source.auditRoot(),
                source.sourceManifestSha256(),
                source.applicationJarSha256(),
                source.runtimeDistributionSha256(),
                source.operatorDistributionSha256(),
                source.permissionPolicyRevision(),
                source.policySha256(),
                source.requestedMetadataSha256(),
                source.expectedCurrentMetadataSha256());
        return InstallationTransactionState.start(
                plan,
                environment(plan),
                "release-v1",
                PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"),
                "activation-new");
    }

    private static InstallationEnvironmentEvidence environment(
            CancellationTrustInstallationPlan plan) {
        return new InstallationEnvironmentEvidence(
                plan.transactionId(),
                "fake-adapter",
                "fake-v1",
                plan.principals(),
                "fake-filesystem",
                true,
                true);
    }

    private static InstallationPhaseEvidence evidence(
            InstallationTransactionState pending,
            String digit) {
        return new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                pending.plan().transactionId(),
                pending.phase(),
                pending.revision(),
                digit.repeat(64),
                Optional.empty());
    }

    private static void assertReason(
            InstallationTransactionStoreException.Reason expected,
            Throwing action) {
        InstallationTransactionStoreException failure = assertThrows(
                InstallationTransactionStoreException.class, action::run);
        assertEquals(expected, failure.reason());
    }

    private interface Throwing {
        void run() throws Exception;
    }
}
