package com.enhancer.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.runtime.InstalledCancellationTrustMetadataLoader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CancellationTrustMaintenanceOperatorTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void installDelegatesAndPrintsOnlyBoundedPublicResult() throws Exception {
        Fixture fixture = fixture("install");
        Captured captured = execute(
                "install",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.candidate.toString());

        assertEquals(0, captured.exitCode());
        assertTrue(captured.stdout().contains("status=INSTALLED\n"));
        assertTrue(captured.stdout().contains("policyFile=" + fixture.trustDirectory));
        assertTrue(captured.stdout().contains("policySha256="));
        assertTrue(captured.stdout().contains("metadataSha256="));
        assertEquals("", captured.stderr());
        assertFalse(captured.stdout().contains("publicKeySubjectPublicKeyInfo"));
        assertTrue(Files.exists(fixture.metadataFile));
    }

    @Test
    void rotateAndExactReplayUseTheRequiredCurrentDigest() throws Exception {
        Fixture fixture = fixture("rotate");
        assertEquals(0, execute(
                "install",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.candidate.toString()).exitCode());
        byte[] currentMetadata = Files.readAllBytes(fixture.metadataFile);
        FileTime timestamp = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(fixture.metadataFile, timestamp);

        Captured replay = execute(
                "rotate",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.candidate.toString(),
                "--expected-current-metadata-sha256", sha256(currentMetadata));

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().startsWith("status=EXACT_REPLAY\n"));
        assertEquals("", replay.stderr());
        assertEquals(timestamp, Files.getLastModifiedTime(fixture.metadataFile));
        assertTrue(java.util.Arrays.equals(
                currentMetadata, Files.readAllBytes(fixture.metadataFile)));
    }

    @Test
    void rejectsEveryNonExactArgumentShapeBeforeMutation() throws Exception {
        Fixture fixture = fixture("arguments");
        String app = fixture.applicationJar.toString();
        String candidate = fixture.candidate.toString();
        String digest = "a".repeat(64);
        String[][] invalid = {
                {},
                {"INSTALL", "--application-jar", app, "--candidate-policy", candidate},
                {"install", "--application-jar", app},
                {"install", "--candidate-policy", candidate, "--application-jar", app},
                {"install", "--application-jar", app, "--candidate-policy", candidate,
                        "--application-jar", app},
                {"install", "--application-jar", app, "--candidate-policy", candidate,
                        "--expected-current-metadata-sha256", digest},
                {"rotate", "--application-jar", app, "--candidate-policy", candidate},
                {"rotate", "--application-jar", app, "--candidate-policy", candidate,
                        "--expected-current-metadata-sha256", digest.toUpperCase()},
                {"rotate", "--application-jar", app, "--candidate-policy", candidate,
                        "--expected-current-metadata-sha256", digest, "extra"},
                {"install", "--application-jar", "relative.jar",
                        "--candidate-policy", candidate},
                {"install", "--application-jar", app,
                        "--candidate-policy", "relative.conf"},
                {"install", "--application-jar", app, "--unknown", candidate}
        };
        for (String[] arguments : invalid) {
            Captured captured = execute(arguments);
            assertEquals(2, captured.exitCode(), java.util.Arrays.toString(arguments));
            assertEquals("", captured.stdout());
            assertTrue(captured.stderr().startsWith(
                    "status=ERROR\nexitCode=2\ncategory=CONFIGURATION\nreason="));
            assertFalse(captured.stderr().contains("Exception"));
            assertFalse(Files.exists(fixture.metadataFile));
        }
    }

    @Test
    void mapsExistingBindingContentionAndStaleCasToSafeRefusal() throws Exception {
        Fixture fixture = fixture("refusal");
        String[] install = {
                "install",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.candidate.toString()
        };
        assertEquals(0, execute(install).exitCode());

        Captured existing = execute(install);
        assertEquals(20, existing.exitCode());
        assertTrue(existing.stderr().contains("category=REFUSAL\n"));
        assertTrue(existing.stderr().contains("reason=EXISTING_BINDING\n"));

        Captured stale = execute(
                "rotate",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.otherCandidate.toString(),
                "--expected-current-metadata-sha256", "0".repeat(64));
        assertEquals(20, stale.exitCode());
        assertTrue(stale.stderr().contains("reason=STALE_CURRENT_METADATA\n"));

        Fixture contended = fixture("lock-contention");
        Path lockFile = contended.applicationJar.resolveSibling(
                CancellationTrustMaintenance.LOCK_FILE_NAME);
        try (FileChannel channel = FileChannel.open(
                    lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock heldLock = channel.lock()) {
            assertTrue(heldLock.isValid());
            Captured contention = execute(
                    "install",
                    "--application-jar", contended.applicationJar.toString(),
                    "--candidate-policy", contended.candidate.toString());
            assertEquals(20, contention.exitCode());
            assertTrue(contention.stderr().contains("reason=LOCK_CONTENDED\n"));
        }
    }

    @Test
    void mapsMalformedCandidateAndMissingInstallationToConfiguration() throws Exception {
        Fixture fixture = fixture("configuration");
        Files.writeString(fixture.candidate,
                "privateKey=forbidden\n", StandardCharsets.UTF_8);
        Captured malformed = execute(
                "install",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.candidate.toString());
        assertEquals(2, malformed.exitCode());
        assertTrue(malformed.stderr().contains("category=CONFIGURATION\n"));
        assertFalse(malformed.stderr().contains("privateKey"));

        Files.delete(fixture.applicationJar);
        Captured missing = execute(
                "install",
                "--application-jar", fixture.applicationJar.toString(),
                "--candidate-policy", fixture.otherCandidate.toString());
        assertEquals(2, missing.exitCode());
        assertTrue(missing.stderr().contains("reason=INVALID_INSTALLATION\n"));
    }

    @Test
    void typedFailureCannotExposeUnboundedDetail() {
        CancellationTrustMaintenanceException exception =
                new CancellationTrustMaintenanceException(
                        CancellationTrustMaintenanceFailureReason.PUBLICATION_FAILED,
                        "secret detail ".repeat(100));

        assertEquals(CancellationTrustMaintenanceFailureCategory.DURABILITY,
                exception.category());
        assertEquals(CancellationTrustMaintenanceFailureReason.PUBLICATION_FAILED,
                exception.reason());
        assertTrue(exception.getMessage().length() <= 256);
    }

    @Test
    void mapsInjectedDurabilityFailureWithoutLeakingDetail() throws Exception {
        Fixture fixture = fixture("durability");
        CancellationTrustMaintenance failing = new CancellationTrustMaintenance(
                phase -> {
                    if (phase == CancellationTrustMaintenancePhase.BEFORE_METADATA_SWITCH) {
                        throw new CancellationTrustMaintenanceException(
                                CancellationTrustMaintenanceFailureReason.PUBLICATION_FAILED,
                                "secret-path=" + fixture.candidate);
                    }
                });
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = new CancellationTrustMaintenanceOperator(failing).execute(
                new String[] {
                    "install",
                    "--application-jar", fixture.applicationJar.toString(),
                    "--candidate-policy", fixture.candidate.toString()
                },
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));

        assertEquals(70, exitCode);
        assertEquals("", stdout.toString(StandardCharsets.UTF_8));
        String failure = stderr.toString(StandardCharsets.UTF_8);
        assertEquals("status=ERROR\nexitCode=70\ncategory=DURABILITY\n"
                + "reason=PUBLICATION_FAILED\n", failure);
        assertFalse(failure.contains("secret-path"));
        assertTrue(failure.length() < 256);
    }

    @Test
    void rejectsControlCharactersAndOversizedPathsWithBoundedOutput() {
        String longPath = "C:\\" + "a".repeat(5000);
        Captured control = execute(
                "install",
                "--application-jar", "C:\\app\n.jar",
                "--candidate-policy", "C:\\candidate.conf");
        Captured oversized = execute(
                "install",
                "--application-jar", longPath,
                "--candidate-policy", "C:\\candidate.conf");

        for (Captured captured : java.util.List.of(control, oversized)) {
            assertEquals(2, captured.exitCode());
            assertEquals("", captured.stdout());
            assertTrue(captured.stderr().length() < 256);
            assertEquals(4, captured.stderr().lines().count());
        }
    }

    @Test
    void directJvmProcessPreservesConfigurationRefusalAndDurabilityExitCodes()
            throws Exception {
        ProcessResult usage = invokeProcess();
        assertEquals(2, usage.exitCode());
        assertTrue(usage.stderr().contains("exitCode=2\n"));

        Fixture existing = fixture("process-refusal");
        new CancellationTrustMaintenance().install(
                existing.applicationJar, existing.candidate);
        ProcessResult refusal = invokeProcess(
                "install",
                "--application-jar", existing.applicationJar.toString(),
                "--candidate-policy", existing.candidate.toString());
        assertEquals(20, refusal.exitCode());
        assertTrue(refusal.stderr().contains("reason=EXISTING_BINDING\n"));

        Fixture durability = fixture("process-durability");
        Files.createDirectory(durability.applicationJar.resolveSibling(
                CancellationTrustMaintenance.LOCK_FILE_NAME));
        ProcessResult failed = invokeProcess(
                "install",
                "--application-jar", durability.applicationJar.toString(),
                "--candidate-policy", durability.candidate.toString());
        assertEquals(70, failed.exitCode());
        assertTrue(failed.stderr().contains("reason=LOCK_FAILED\n"));
    }

    private Captured execute(String... arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new CancellationTrustMaintenanceOperator().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static ProcessResult invokeProcess(String... arguments) throws Exception {
        String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(CancellationTrustMaintenanceOperator.class.getName());
        command.addAll(java.util.List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        String stdout;
        String stderr;
        try (var output = process.getInputStream(); var error = process.getErrorStream()) {
            stdout = new String(output.readAllBytes(), StandardCharsets.UTF_8);
            stderr = new String(error.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, stdout, stderr);
    }

    private Fixture fixture(String name) throws Exception {
        Path root = temporaryRoot.resolve(name).toAbsolutePath().normalize();
        Path installation = Files.createDirectories(root.resolve("installation"));
        Path applicationJar = installation.resolve("enhancer.jar");
        Files.writeString(applicationJar, "application", StandardCharsets.UTF_8);
        Path trustDirectory = Files.createDirectory(installation.resolve(
                CancellationTrustMaintenance.TRUST_DIRECTORY_NAME));
        Path candidates = Files.createDirectory(root.resolve("candidates"));
        Path candidate = candidates.resolve("candidate.conf");
        Path otherCandidate = candidates.resolve("other.conf");
        Files.write(candidate, canonicalPolicy("policy-v1", "key-v1"));
        Files.write(otherCandidate, canonicalPolicy("policy-v2", "key-v2"));
        return new Fixture(
                applicationJar,
                trustDirectory,
                candidate,
                otherCandidate,
                applicationJar.resolveSibling(
                        InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME));
    }

    private record Fixture(
            Path applicationJar,
            Path trustDirectory,
            Path candidate,
            Path otherCandidate,
            Path metadataFile) { }

    private record Captured(int exitCode, String stdout, String stderr) { }

    private record ProcessResult(int exitCode, String stdout, String stderr) { }

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
