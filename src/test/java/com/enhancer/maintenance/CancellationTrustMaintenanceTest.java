package com.enhancer.maintenance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.runtime.InstalledCancellationTrustMetadata;
import com.enhancer.runtime.InstalledCancellationTrustMetadataLoader;
import com.enhancer.runtime.PinnedFileCancellationGrantTrustPolicyLoader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CancellationTrustMaintenanceTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void installsPolicyFirstAndProductionLoadsTheFixedBinding() throws Exception {
        Fixture fixture = fixture();
        byte[] policy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        Path candidate = fixture.writeCandidate("candidate-v1.conf", policy);

        CancellationTrustMaintenanceResult result =
                new CancellationTrustMaintenance().install(fixture.applicationJar, candidate);

        assertEquals(CancellationTrustMaintenanceStatus.INSTALLED, result.status());
        assertEquals(sha256(policy), result.policySha256());
        assertEquals(fixture.trustDirectory.resolve(
                CancellationTrustMaintenance.POLICY_FILE_PREFIX
                        + sha256(policy)
                        + CancellationTrustMaintenance.POLICY_FILE_SUFFIX),
                result.policyFile());
        InstalledCancellationTrustMetadata metadata =
                new InstalledCancellationTrustMetadataLoader(fixture.applicationJar).load();
        assertEquals(result.policyFile(), metadata.policyFile());
        assertEquals(result.policySha256(), metadata.expectedSha256());
        assertEquals("cancel-policy-v1",
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        metadata.policyFile(), metadata.expectedSha256())
                        .load().policyRevision());
    }

    @Test
    void installRefusesExistingBindingAndLockContention() throws Exception {
        Fixture fixture = fixture();
        Path first = fixture.writeCandidate(
                "first.conf", canonicalPolicy("cancel-policy-v1", "primary-2026"));
        Path second = fixture.writeCandidate(
                "second.conf", canonicalPolicy("cancel-policy-v2", "secondary-2026"));
        CancellationTrustMaintenance maintenance = new CancellationTrustMaintenance();
        maintenance.install(fixture.applicationJar, first);
        assertThrows(IOException.class, () -> maintenance.install(
                fixture.applicationJar, second));

        Fixture contended = fixture("contended");
        Path candidate = contended.writeCandidate("candidate.conf",
                canonicalPolicy("cancel-policy-v1", "primary-2026"));
        Path lockFile = contended.applicationJar.resolveSibling(
                CancellationTrustMaintenance.LOCK_FILE_NAME);
        try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock heldLock = channel.lock()) {
            assertTrue(heldLock.isValid());
            assertThrows(IOException.class, () -> maintenance.install(
                    contended.applicationJar, candidate));
        }
    }

    @Test
    void rotatesWithCasAndRetainsTheOldContentAddressedPolicy() throws Exception {
        Fixture fixture = fixture();
        CancellationTrustMaintenance maintenance = new CancellationTrustMaintenance();
        byte[] oldPolicy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        byte[] newPolicy = canonicalPolicy("cancel-policy-v2", "secondary-2026");
        CancellationTrustMaintenanceResult installed = maintenance.install(
                fixture.applicationJar, fixture.writeCandidate("old.conf", oldPolicy));
        byte[] oldMetadata = Files.readAllBytes(fixture.metadataFile);

        CancellationTrustMaintenanceResult rotated = maintenance.rotate(
                fixture.applicationJar,
                fixture.writeCandidate("new.conf", newPolicy),
                sha256(oldMetadata));

        assertEquals(CancellationTrustMaintenanceStatus.ROTATED, rotated.status());
        assertTrue(Files.exists(installed.policyFile()));
        assertArrayEquals(oldPolicy, Files.readAllBytes(installed.policyFile()));
        assertEquals("cancel-policy-v2",
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        rotated.policyFile(), rotated.policySha256())
                        .load().policyRevision());
        assertThrows(IOException.class, () -> maintenance.rotate(
                fixture.applicationJar,
                fixture.writeCandidate("stale.conf", oldPolicy),
                sha256(oldMetadata)));
    }

    @Test
    void exactReplayDoesNotRewriteMetadataBytesOrTimestamp() throws Exception {
        Fixture fixture = fixture();
        CancellationTrustMaintenance maintenance = new CancellationTrustMaintenance();
        byte[] policy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        Path source = fixture.writeCandidate("candidate.conf", policy);
        maintenance.install(fixture.applicationJar, source);
        byte[] before = Files.readAllBytes(fixture.metadataFile);
        FileTime timestamp = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(fixture.metadataFile, timestamp);

        CancellationTrustMaintenanceResult replay = maintenance.rotate(
                fixture.applicationJar, source, sha256(before));

        assertEquals(CancellationTrustMaintenanceStatus.EXACT_REPLAY, replay.status());
        assertArrayEquals(before, Files.readAllBytes(fixture.metadataFile));
        assertEquals(timestamp, Files.getLastModifiedTime(fixture.metadataFile));
    }

    @Test
    void exactReplayRecoversALostSuccessfulRotationResponse() throws Exception {
        Fixture fixture = fixture();
        CancellationTrustMaintenance maintenance = new CancellationTrustMaintenance();
        byte[] oldPolicy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        byte[] newPolicy = canonicalPolicy("cancel-policy-v2", "secondary-2026");
        maintenance.install(fixture.applicationJar,
                fixture.writeCandidate("old.conf", oldPolicy));
        String oldMetadataSha256 = sha256(Files.readAllBytes(fixture.metadataFile));
        Path candidate = fixture.writeCandidate("new.conf", newPolicy);
        maintenance.rotate(fixture.applicationJar, candidate, oldMetadataSha256);
        byte[] newMetadata = Files.readAllBytes(fixture.metadataFile);
        FileTime timestamp = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(fixture.metadataFile, timestamp);

        CancellationTrustMaintenanceResult replay = maintenance.rotate(
                fixture.applicationJar, candidate, oldMetadataSha256);

        assertEquals(CancellationTrustMaintenanceStatus.EXACT_REPLAY, replay.status());
        assertArrayEquals(newMetadata, Files.readAllBytes(fixture.metadataFile));
        assertEquals(timestamp, Files.getLastModifiedTime(fixture.metadataFile));
    }

    @Test
    void rejectsPrivateFieldsAndSymbolicCandidateWhenSupported() throws Exception {
        Fixture fixture = fixture();
        byte[] canonical = canonicalPolicy("cancel-policy-v1", "primary-2026");
        byte[] privateField = new String(canonical, StandardCharsets.UTF_8)
                .replace("audience=", "privateKey=forbidden\naudience=")
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> new CancellationTrustMaintenance().install(
                fixture.applicationJar,
                fixture.writeCandidate("private.conf", privateField)));

        Path real = fixture.writeCandidate("real.conf", canonical);
        Path link = fixture.candidateDirectory.resolve("link.conf");
        try {
            Files.createSymbolicLink(link, real);
            assertThrows(IOException.class, () -> new CancellationTrustMaintenance().install(
                    fixture.applicationJar, link));
        } catch (UnsupportedOperationException | IOException exception) {
            assertFalse(Files.exists(fixture.metadataFile));
        }
    }

    @Test
    void installRejectsDifferentBytesAtTheDerivedDigestPath() throws Exception {
        Fixture fixture = fixture();
        byte[] candidateBytes = canonicalPolicy("cancel-policy-v1", "primary-2026");
        Path derived = fixture.trustDirectory.resolve(
                CancellationTrustMaintenance.POLICY_FILE_PREFIX
                        + sha256(candidateBytes)
                        + CancellationTrustMaintenance.POLICY_FILE_SUFFIX);
        byte[] conflictingBytes = canonicalPolicy(
                "cancel-policy-conflict", "conflicting-2026");
        Files.write(derived, conflictingBytes);

        assertThrows(IOException.class, () -> new CancellationTrustMaintenance().install(
                fixture.applicationJar,
                fixture.writeCandidate("candidate.conf", candidateBytes)));
        assertArrayEquals(conflictingBytes, Files.readAllBytes(derived));
        assertFalse(Files.exists(fixture.metadataFile));
    }

    @Test
    void rotateRefusesMissingOrCorruptCurrentBinding() throws Exception {
        Fixture missing = fixture("missing-current");
        byte[] candidateBytes = canonicalPolicy("cancel-policy-v2", "secondary-2026");
        CancellationTrustMaintenance maintenance = new CancellationTrustMaintenance();
        assertThrows(IOException.class, () -> maintenance.rotate(
                missing.applicationJar,
                missing.writeCandidate("candidate.conf", candidateBytes),
                "0".repeat(64)));

        Fixture corrupt = fixture("corrupt-current");
        Files.writeString(corrupt.metadataFile, "corrupt\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> maintenance.rotate(
                corrupt.applicationJar,
                corrupt.writeCandidate("candidate.conf", candidateBytes),
                sha256(Files.readAllBytes(corrupt.metadataFile))));
        assertEquals("corrupt\n", Files.readString(corrupt.metadataFile));
    }

    @Test
    void installRetainsCandidateAfterPrePublicationFailure() throws Exception {
        Fixture fixture = fixture();
        CancellationTrustMaintenance failing = new CancellationTrustMaintenance(
                phase -> {
                    if (phase
                            == CancellationTrustMaintenancePhase
                                    .AFTER_POLICY_CANDIDATE_FORCED) {
                        throw new IOException("injected policy publication failure");
                    }
                });

        assertThrows(IOException.class, () -> failing.install(
                fixture.applicationJar,
                fixture.writeCandidate("candidate.conf",
                        canonicalPolicy("cancel-policy-v1", "primary-2026"))));

        try (var paths = Files.list(fixture.trustDirectory)) {
            assertEquals(1L, paths.filter(path -> path.getFileName().toString()
                    .startsWith(".policy-candidate-")).count());
        }
        assertFalse(Files.exists(fixture.metadataFile));
    }

    @Test
    void everyPreSwitchInjectedFailureLeavesNoBindingOrTheOldBinding() throws Exception {
        Set<CancellationTrustMaintenancePhase> installPhases = Set.of(
                CancellationTrustMaintenancePhase.AFTER_LOCK_ACQUIRED,
                CancellationTrustMaintenancePhase.AFTER_CANDIDATE_VALIDATED,
                CancellationTrustMaintenancePhase.AFTER_POLICY_CANDIDATE_FORCED,
                CancellationTrustMaintenancePhase.AFTER_POLICY_PUBLISHED,
                CancellationTrustMaintenancePhase.AFTER_METADATA_CANDIDATE_VALIDATED,
                CancellationTrustMaintenancePhase.BEFORE_METADATA_SWITCH);
        for (CancellationTrustMaintenancePhase phase : installPhases) {
            Fixture fixture = fixture("install-" + phase.name());
            CancellationTrustMaintenance failing = new CancellationTrustMaintenance(
                    reached -> {
                        if (reached == phase) {
                            throw new IOException("injected " + phase);
                        }
                    });
            assertThrows(IOException.class, () -> failing.install(
                    fixture.applicationJar,
                    fixture.writeCandidate("candidate.conf",
                            canonicalPolicy("cancel-policy-v1", "primary-2026"))));
            assertFalse(Files.exists(fixture.metadataFile));
        }

        Fixture fixture = fixture("rotate");
        CancellationTrustMaintenance normal = new CancellationTrustMaintenance();
        byte[] oldPolicy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        normal.install(fixture.applicationJar,
                fixture.writeCandidate("old.conf", oldPolicy));
        byte[] oldMetadata = Files.readAllBytes(fixture.metadataFile);
        for (CancellationTrustMaintenancePhase phase : Set.of(
                CancellationTrustMaintenancePhase.AFTER_CURRENT_VALIDATED,
                CancellationTrustMaintenancePhase.AFTER_CANDIDATE_VALIDATED,
                CancellationTrustMaintenancePhase.AFTER_POLICY_CANDIDATE_FORCED,
                CancellationTrustMaintenancePhase.AFTER_POLICY_PUBLISHED,
                CancellationTrustMaintenancePhase.AFTER_METADATA_CANDIDATE_VALIDATED,
                CancellationTrustMaintenancePhase.BEFORE_METADATA_SWITCH)) {
            CancellationTrustMaintenance failing = new CancellationTrustMaintenance(
                    reached -> {
                        if (reached == phase) {
                            throw new IOException("injected " + phase);
                        }
                    });
            assertThrows(IOException.class, () -> failing.rotate(
                    fixture.applicationJar,
                    fixture.writeCandidate("new-" + phase.name() + ".conf",
                            canonicalPolicy("cancel-policy-v2", "secondary-2026")),
                    sha256(oldMetadata)));
            assertArrayEquals(oldMetadata, Files.readAllBytes(fixture.metadataFile));
            InstalledCancellationTrustMetadata current =
                    new InstalledCancellationTrustMetadataLoader(
                            fixture.applicationJar).load();
            assertArrayEquals(oldPolicy, Files.readAllBytes(current.policyFile()));
        }
    }

    @Test
    void finalCasDetectsDriftBeforeSwitch() throws Exception {
        Fixture fixture = fixture();
        CancellationTrustMaintenance normal = new CancellationTrustMaintenance();
        byte[] oldPolicy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        normal.install(fixture.applicationJar,
                fixture.writeCandidate("old.conf", oldPolicy));
        byte[] expectedMetadata = Files.readAllBytes(fixture.metadataFile);
        CancellationTrustMaintenance drifting = new CancellationTrustMaintenance(
                phase -> {
                    if (phase == CancellationTrustMaintenancePhase.BEFORE_FINAL_CAS) {
                        Files.writeString(fixture.metadataFile, "drift\n",
                                StandardCharsets.UTF_8);
                    }
                });

        assertThrows(IOException.class, () -> drifting.rotate(
                fixture.applicationJar,
                fixture.writeCandidate("new.conf",
                        canonicalPolicy("cancel-policy-v2", "secondary-2026")),
                sha256(expectedMetadata)));
        assertEquals("drift\n", Files.readString(fixture.metadataFile));
    }

    @Test
    void lostResponseAfterVerifiedRotationRecoversAsExactReplay() throws Exception {
        Fixture fixture = fixture();
        CancellationTrustMaintenance normal = new CancellationTrustMaintenance();
        byte[] oldPolicy = canonicalPolicy("cancel-policy-v1", "primary-2026");
        byte[] newPolicy = canonicalPolicy("cancel-policy-v2", "secondary-2026");
        normal.install(fixture.applicationJar,
                fixture.writeCandidate("old.conf", oldPolicy));
        String expected = sha256(Files.readAllBytes(fixture.metadataFile));
        Path candidate = fixture.writeCandidate("new.conf", newPolicy);
        CancellationTrustMaintenance losesResponse = new CancellationTrustMaintenance(
                phase -> {
                    if (phase == CancellationTrustMaintenancePhase.AFTER_INSTALLED_VERIFIED) {
                        throw new IOException("lost successful response");
                    }
                });

        assertThrows(IOException.class, () -> losesResponse.rotate(
                fixture.applicationJar, candidate, expected));
        byte[] switched = Files.readAllBytes(fixture.metadataFile);
        CancellationTrustMaintenanceResult replay = normal.rotate(
                fixture.applicationJar, candidate, expected);

        assertEquals(CancellationTrustMaintenanceStatus.EXACT_REPLAY, replay.status());
        assertArrayEquals(switched, Files.readAllBytes(fixture.metadataFile));
    }

    private Fixture fixture() throws IOException {
        return fixture("default");
    }

    private Fixture fixture(String name) throws IOException {
        Path root = temporaryRoot.resolve(name).toAbsolutePath().normalize();
        Path installation = Files.createDirectories(root.resolve("installation"));
        Path applicationJar = installation.resolve("enhancer.jar");
        Files.writeString(applicationJar, "application", StandardCharsets.UTF_8);
        Path trustDirectory = Files.createDirectory(installation.resolve(
                CancellationTrustMaintenance.TRUST_DIRECTORY_NAME));
        Path candidateDirectory = Files.createDirectory(root.resolve("candidates"));
        return new Fixture(applicationJar, trustDirectory, candidateDirectory,
                applicationJar.resolveSibling(
                        InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME));
    }

    private record Fixture(
            Path applicationJar,
            Path trustDirectory,
            Path candidateDirectory,
            Path metadataFile) {
        private Path writeCandidate(String name, byte[] content) throws IOException {
            Path candidate = candidateDirectory.resolve(name);
            Files.write(candidate, content);
            return candidate;
        }
    }

    private static byte[] canonicalPolicy(String revision, String keyId) throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        return String.join("\n",
                "enhancer-cancellation-grant-trust-policy-v1",
                "configurationId=local-installation",
                "audience=enhancer-local-control",
                "policyRevision=" + revision,
                "maximumGrantLifetimeSeconds=900",
                "clockSkewSeconds=30",
                "trustedKeyCount=1",
                "trustedKey.0.issuerId=operations",
                "trustedKey.0.keyId=" + keyId,
                "trustedKey.0.subjectCount=1",
                "trustedKey.0.subject.0=operator-17",
                "trustedKey.0.publicKeySubjectPublicKeyInfo="
                        + Base64.getEncoder().encodeToString(publicKey),
                "trustedKey.0.publicKeySha256=" + sha256(publicKey),
                "trustedKey.0.validFrom=2026-01-01T00:00:00Z",
                "trustedKey.0.validUntil=2027-01-01T00:00:00Z",
                "trustedKey.0.revokedAt=-",
                "").getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
