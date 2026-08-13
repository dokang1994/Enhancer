package com.enhancer.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.runtime.InstalledCancellationTrustMetadataLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CancellationTrustMaintenanceDistributionIntegrationTest {
    private static final String DISTRIBUTION_NAME =
            "enhancer-cancellation-trust-maintenance";
    private static final Path PROJECT = Path.of(System.getProperty("user.dir"));
    private static final Path INSTALLATION = PROJECT.resolve("build/install")
            .resolve(DISTRIBUTION_NAME);

    @TempDir
    Path temporaryRoot;

    @Test
    void installedLayoutContainsOnlyTheOperatorLauncherAndRuntimeLibraries()
            throws Exception {
        Path copiedInstallation = copiedInstallation("layout");
        Path bin = copiedInstallation.resolve("bin");
        Path lib = copiedInstallation.resolve("lib");

        assertTrue(Files.isDirectory(bin));
        assertTrue(Files.isDirectory(lib));
        try (var files = Files.list(bin)) {
            assertEquals(
                    List.of(DISTRIBUTION_NAME, DISTRIBUTION_NAME + ".bat"),
                    files.map(path -> path.getFileName().toString()).sorted().toList());
        }
        try (var files = Files.list(lib)) {
            List<String> libraries = files.map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
            assertTrue(libraries.stream().anyMatch(name ->
                    name.startsWith("enhancer-") && name.endsWith(".jar")));
        }

        String unix = Files.readString(bin.resolve(DISTRIBUTION_NAME),
                StandardCharsets.UTF_8);
        String windows = Files.readString(bin.resolve(DISTRIBUTION_NAME + ".bat"),
                StandardCharsets.UTF_8);
        for (String script : List.of(unix, windows)) {
            assertTrue(script.contains(
                    "com.enhancer.maintenance.CancellationTrustMaintenanceOperator"));
            assertFalse(script.contains("com.enhancer.cli.EnhancerCli"));
            assertFalse(script.contains("--application-jar"));
            assertFalse(script.contains("--candidate-policy"));
            assertFalse(script.contains("--expected-current-metadata-sha256"));
        }
    }

    @Test
    void installedLauncherPreservesConfigurationRefusalAndDurabilityExitCodes()
            throws Exception {
        ProcessResult configuration = invokeLauncher();
        assertEquals(2, configuration.exitCode());
        assertEquals("", configuration.stdout());
        assertTrue(configuration.stderr().contains("exitCode=2\n"));

        Fixture existing = fixture("existing");
        assertEquals(0, invokeLauncher(
                "install",
                "--application-jar", existing.applicationJar().toString(),
                "--candidate-policy", existing.candidatePolicy().toString()).exitCode());
        ProcessResult refusal = invokeLauncher(
                "install",
                "--application-jar", existing.applicationJar().toString(),
                "--candidate-policy", existing.candidatePolicy().toString());
        assertEquals(20, refusal.exitCode());
        assertTrue(refusal.stderr().contains("reason=EXISTING_BINDING\n"));

        Fixture durability = fixture("durability");
        Files.createDirectory(durability.applicationJar().resolveSibling(
                CancellationTrustMaintenance.LOCK_FILE_NAME));
        ProcessResult failed = invokeLauncher(
                "install",
                "--application-jar", durability.applicationJar().toString(),
                "--candidate-policy", durability.candidatePolicy().toString());
        assertEquals(70, failed.exitCode());
        assertTrue(failed.stderr().contains("reason=LOCK_FAILED\n"));
    }

    @Test
    void installedLauncherInstallsAndExactReplaysOnlyInTheTemporaryTree()
            throws Exception {
        Fixture fixture = fixture("success");
        ProcessResult installed = invokeLauncher(
                "install",
                "--application-jar", fixture.applicationJar().toString(),
                "--candidate-policy", fixture.candidatePolicy().toString());

        assertEquals(0, installed.exitCode());
        assertTrue(installed.stdout().startsWith("status=INSTALLED\n"));
        assertEquals("", installed.stderr());
        assertTrue(Files.isRegularFile(fixture.metadataFile()));
        byte[] metadata = Files.readAllBytes(fixture.metadataFile());

        ProcessResult replay = invokeLauncher(
                "rotate",
                "--application-jar", fixture.applicationJar().toString(),
                "--candidate-policy", fixture.candidatePolicy().toString(),
                "--expected-current-metadata-sha256", sha256(metadata));

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().startsWith("status=EXACT_REPLAY\n"));
        assertEquals("", replay.stderr());
        assertTrue(java.util.Arrays.equals(metadata,
                Files.readAllBytes(fixture.metadataFile())));
        assertTrue(fixture.applicationJar().startsWith(temporaryRoot));
        assertTrue(fixture.candidatePolicy().startsWith(temporaryRoot));
        assertTrue(fixture.metadataFile().startsWith(temporaryRoot));
    }

    private ProcessResult invokeLauncher(String... arguments) throws Exception {
        Path script = copiedInstallation("launcher-" + System.nanoTime())
                .resolve("bin").resolve(
                isWindows() ? DISTRIBUTION_NAME + ".bat" : DISTRIBUTION_NAME);
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");
        }
        command.add(script.toString());
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        builder.environment().remove("JAVA_OPTS");
        builder.environment().remove("ENHANCER_CANCELLATION_TRUST_MAINTENANCE_OPTS");
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("_JAVA_OPTIONS");
        builder.environment().remove("JDK_JAVA_OPTIONS");
        Process process = builder.start();
        String stdout = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        String stderr = new String(process.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        return new ProcessResult(process.waitFor(), stdout, stderr);
    }

    private Path copiedInstallation(String name) throws Exception {
        Path destination = temporaryRoot.resolve("distribution-" + name)
                .toAbsolutePath()
                .normalize();
        try (var paths = Files.walk(INSTALLATION)) {
            for (Path source : paths.toList()) {
                Path target = destination.resolve(INSTALLATION.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target,
                            StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        if (!isWindows()) {
            assertTrue(destination.resolve("bin").resolve(DISTRIBUTION_NAME)
                    .toFile().setExecutable(true, true));
        }
        return destination;
    }

    private Fixture fixture(String name) throws Exception {
        Path root = temporaryRoot.resolve(name).toAbsolutePath().normalize();
        Path application = Files.createDirectories(root.resolve("application"));
        Path applicationJar = application.resolve("enhancer.jar");
        Files.writeString(applicationJar, "test-application", StandardCharsets.UTF_8);
        Files.createDirectory(application.resolve(
                CancellationTrustMaintenance.TRUST_DIRECTORY_NAME));
        Path candidates = Files.createDirectory(root.resolve("candidates"));
        Path candidate = candidates.resolve("candidate.conf");
        Files.write(candidate, canonicalPolicy(name));
        return new Fixture(
                applicationJar,
                candidate,
                application.resolve(InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME));
    }

    private static byte[] canonicalPolicy(String suffix) throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        return String.join("\n",
                "enhancer-cancellation-grant-trust-policy-v1",
                "configurationId=packaged-operator-test",
                "audience=enhancer-local-control",
                "policyRevision=policy-" + suffix,
                "maximumGrantLifetimeSeconds=900",
                "clockSkewSeconds=30",
                "trustedKeyCount=1",
                "trustedKey.0.issuerId=operations",
                "trustedKey.0.keyId=key-" + suffix,
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

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private record Fixture(
            Path applicationJar,
            Path candidatePolicy,
            Path metadataFile) { }

    private record ProcessResult(int exitCode, String stdout, String stderr) { }
}
