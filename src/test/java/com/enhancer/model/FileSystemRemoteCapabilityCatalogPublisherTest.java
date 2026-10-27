package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemRemoteCapabilityCatalogPublisherTest {
    private static final String INSTALLATION = "installation-a";
    private static final String CANDIDATE = "candidate-input-a";
    private static final String EVIDENCE = "evidence-a";

    @TempDir
    Path temporaryRoot;

    private Path installationRoot;
    private Path catalogDirectory;
    private Path candidateFile;

    @BeforeEach
    void setUp() throws IOException {
        installationRoot = Files.createDirectory(temporaryRoot.resolve("installation"))
                .toRealPath();
        catalogDirectory = Files.createDirectory(installationRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME)).toRealPath();
        candidateFile = temporaryRoot.resolve("candidate.conf").toAbsolutePath().normalize();
        Files.write(candidateFile, RemoteModelCapabilityCatalogTestFixture.oneRecord());
    }

    @Test
    void installPublishesCatalogFirstBindingLastAndExactReplayWritesNothing()
            throws Exception {
        FileSystemRemoteCapabilityCatalogPublisher publisher = publisher(
                point -> { });
        RemoteCapabilityCatalogPublicationResult.Succeeded installed = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                publisher.publish(installIntent()));
        assertEquals(RemoteCapabilityCatalogPublicationStatus.INSTALLED,
                installed.status());

        Path binding = installationRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
        byte[] bindingBytes = Files.readAllBytes(binding);
        Path catalog = catalogDirectory.resolve(
                "enhancer-remote-capability-catalog-" + installed.catalogSha256()
                        + ".conf");
        assertTrue(Files.isRegularFile(catalog));
        assertArrayEquals(RemoteModelCapabilityCatalogTestFixture.oneRecord(),
                Files.readAllBytes(catalog));

        RemoteCapabilityCatalogPublicationResult.Succeeded replay = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                publisher.publish(installIntent()));
        assertEquals(RemoteCapabilityCatalogPublicationStatus.EXACT_REPLAY,
                replay.status());
        assertArrayEquals(bindingBytes, Files.readAllBytes(binding));
    }

    @Test
    void rotateUsesBindingDigestCasAndRejectsStaleIntentWithoutMutation()
            throws Exception {
        FileSystemRemoteCapabilityCatalogPublisher first = publisher(point -> { });
        RemoteCapabilityCatalogPublicationResult.Succeeded installed = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                first.publish(installIntent()));
        byte[] second = RemoteModelCapabilityCatalogTestFixture.replace(
                RemoteModelCapabilityCatalogTestFixture.oneRecord(),
                "catalogIdentity=local-capability-catalog",
                "catalogIdentity=rotated-capability-catalog");
        Files.write(candidateFile, second);

        RemoteCapabilityCatalogPublicationResult.Refused stale = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Refused.class,
                first.publish(rotateIntent("0".repeat(64))));
        assertEquals(RemoteCapabilityCatalogPublicationRefusalReason
                        .EXPECTED_CURRENT_MISMATCH,
                stale.reason());

        RemoteCapabilityCatalogPublicationResult.Succeeded rotated = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                first.publish(rotateIntent(installed.bindingSha256())));
        assertEquals(RemoteCapabilityCatalogPublicationStatus.ROTATED, rotated.status());
        assertEquals(RemoteModelCapabilityCatalogTestFixture.sha256(second),
                rotated.catalogSha256());
    }

    @Test
    void evidenceRefusalAndLockContentionMutateNoAuthority() throws Exception {
        FileSystemRemoteCapabilityCatalogPublisher refused = newPublisher(
                request -> RemoteCapabilityCatalogEvidenceDecision.REFUSED,
                point -> { });
        assertReason(refused.publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .EVIDENCE_NOT_APPROVED_OR_MISMATCHED);
        assertTrue(Files.notExists(installationRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.BINDING_NAME)));

        Path lockPath = installationRoot.resolve(RemoteCapabilityCatalogBindingCodec.LOCK_NAME);
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertReason(publisher(point -> { }).publish(installIntent()),
                    RemoteCapabilityCatalogPublicationRefusalReason
                            .MAINTENANCE_LOCK_UNAVAILABLE_OR_CONTENDED);
        }
    }

    @Test
    void evidenceVerifierFailureFailsClosedWithoutMutation() {
        FileSystemRemoteCapabilityCatalogPublisher publisher = newPublisher(
                request -> {
                    throw new IllegalStateException("evidence-sentinel");
                },
                point -> { });

        assertReason(publisher.publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .EVIDENCE_NOT_APPROVED_OR_MISMATCHED);
        assertTrue(Files.notExists(bindingPath()));
    }

    @Test
    void currentBindingRequiresItsSelectedCatalogToRemainValid() throws Exception {
        FileSystemRemoteCapabilityCatalogPublisher publisher = publisher(point -> { });
        RemoteCapabilityCatalogPublicationResult.Succeeded installed = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                publisher.publish(installIntent()));
        Files.delete(catalogPath(installed.catalogSha256()));

        assertReason(publisher.publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .CURRENT_BINDING_INVALID_OR_UNSAFE);
    }

    @Test
    void conflictingImmutableCatalogRefusesWithoutBindingPublication()
            throws Exception {
        String digest = RemoteModelCapabilityCatalogTestFixture.sha256(
                RemoteModelCapabilityCatalogTestFixture.oneRecord());
        Files.write(catalogPath(digest), new byte[] {1, 2, 3});

        assertReason(publisher(point -> { }).publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .IMMUTABLE_CATALOG_CONFLICT);
        assertTrue(Files.notExists(bindingPath()));
    }

    @Test
    void candidateMutationDuringReadHasItsOwnRefusal() throws Exception {
        byte[] changed = Arrays.copyOf(
                RemoteModelCapabilityCatalogTestFixture.oneRecord(),
                RemoteModelCapabilityCatalogTestFixture.oneRecord().length + 1);

        assertReason(publisher(point -> {
            if (point == RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_CANDIDATE_SNAPSHOT_READ) {
                writeInjected(candidateFile, changed);
            }
        }).publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .CANDIDATE_CHANGED_DURING_READ);
        assertTrue(Files.notExists(bindingPath()));
    }

    @Test
    void rotateRechecksCurrentBindingImmediatelyBeforeSwitch() throws Exception {
        FileSystemRemoteCapabilityCatalogPublisher installer = publisher(point -> { });
        RemoteCapabilityCatalogPublicationResult.Succeeded installed = assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                installer.publish(installIntent()));
        byte[] requestedBytes = catalogBytes("requested-capability-catalog");
        Files.write(candidateFile, requestedBytes);

        byte[] driftBytes = catalogBytes("drifted-capability-catalog");
        String driftDigest = RemoteModelCapabilityCatalogTestFixture.sha256(driftBytes);
        Files.write(catalogPath(driftDigest), driftBytes);
        RemoteCapabilityCatalogBinding driftBinding =
                RemoteCapabilityCatalogBindingCodec.create(catalogDirectory, driftDigest);

        FileSystemRemoteCapabilityCatalogPublisher publisher = publisher(point -> {
            if (point == RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_CATALOG_PUBLICATION) {
                writeInjected(bindingPath(), driftBinding.canonicalBytes());
            }
        });
        assertReason(publisher.publish(rotateIntent(installed.bindingSha256())),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .EXPECTED_CURRENT_MISMATCH);
        assertArrayEquals(driftBinding.canonicalBytes(), Files.readAllBytes(bindingPath()));
    }

    @Test
    void postSwitchCatalogVerificationFailureIsUnknown() throws Exception {
        String digest = RemoteModelCapabilityCatalogTestFixture.sha256(
                RemoteModelCapabilityCatalogTestFixture.oneRecord());
        assertReason(publisher(point -> {
            if (point == RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_BINDING_PUBLICATION) {
                deleteInjected(catalogPath(digest));
            }
        }).publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .POST_PUBLICATION_STATE_UNKNOWN);
        assertTrue(Files.isRegularFile(bindingPath()));
    }

    @Test
    void catalogPhaseFailureKeepsBindingAbsentAndPostBindingFailureIsUnknown()
            throws Exception {
        assertReason(publisher(point -> {
            if (point == RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_CATALOG_PUBLICATION) {
                throw new RemoteCapabilityCatalogPublicationFaultInjector.Fault();
            }
        }).publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .ATOMIC_PUBLICATION_UNSUPPORTED_OR_FAILED);
        assertTrue(Files.notExists(installationRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.BINDING_NAME)));

        assertReason(publisher(point -> {
            if (point == RemoteCapabilityCatalogPublicationFaultInjector.Point
                    .AFTER_BINDING_PUBLICATION) {
                throw new RemoteCapabilityCatalogPublicationFaultInjector.Fault();
            }
        }).publish(installIntent()),
                RemoteCapabilityCatalogPublicationRefusalReason
                        .POST_PUBLICATION_STATE_UNKNOWN);
        assertTrue(Files.isRegularFile(installationRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.BINDING_NAME)));
    }

    private FileSystemRemoteCapabilityCatalogPublisher publisher(
            RemoteCapabilityCatalogPublicationFaultInjector faults) {
        return newPublisher(
                request -> RemoteCapabilityCatalogEvidenceDecision.APPROVED,
                faults);
    }

    private FileSystemRemoteCapabilityCatalogPublisher newPublisher(
            RemoteCapabilityCatalogEvidenceVerifier verifier,
            RemoteCapabilityCatalogPublicationFaultInjector faults) {
        return new FileSystemRemoteCapabilityCatalogPublisher(
                INSTALLATION,
                installationRoot,
                identity -> candidateFile,
                verifier,
                faults);
    }

    private RemoteCapabilityCatalogPublicationIntent installIntent() {
        return new RemoteCapabilityCatalogPublicationIntent(
                RemoteCapabilityCatalogPublicationOperation.INSTALL,
                INSTALLATION, CANDIDATE, EVIDENCE, Optional.empty());
    }

    private RemoteCapabilityCatalogPublicationIntent rotateIntent(String digest) {
        return new RemoteCapabilityCatalogPublicationIntent(
                RemoteCapabilityCatalogPublicationOperation.ROTATE,
                INSTALLATION, CANDIDATE, EVIDENCE, Optional.of(digest));
    }

    private Path bindingPath() {
        return installationRoot.resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
    }

    private Path catalogPath(String digest) {
        return catalogDirectory.resolve(
                "enhancer-remote-capability-catalog-" + digest + ".conf");
    }

    private static byte[] catalogBytes(String identity) {
        return RemoteModelCapabilityCatalogTestFixture.replace(
                RemoteModelCapabilityCatalogTestFixture.oneRecord(),
                "catalogIdentity=local-capability-catalog",
                "catalogIdentity=" + identity);
    }

    private static void writeInjected(Path path, byte[] bytes)
            throws RemoteCapabilityCatalogPublicationFaultInjector.Fault {
        try {
            Files.write(path, bytes);
        } catch (IOException exception) {
            throw new RemoteCapabilityCatalogPublicationFaultInjector.Fault();
        }
    }

    private static void deleteInjected(Path path)
            throws RemoteCapabilityCatalogPublicationFaultInjector.Fault {
        try {
            Files.delete(path);
        } catch (IOException exception) {
            throw new RemoteCapabilityCatalogPublicationFaultInjector.Fault();
        }
    }

    private static void assertReason(
            RemoteCapabilityCatalogPublicationResult result,
            RemoteCapabilityCatalogPublicationRefusalReason reason) {
        assertEquals(reason, assertInstanceOf(
                RemoteCapabilityCatalogPublicationResult.Refused.class, result).reason());
    }
}
