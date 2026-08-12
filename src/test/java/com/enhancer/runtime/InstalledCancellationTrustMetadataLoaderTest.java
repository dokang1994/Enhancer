package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstalledCancellationTrustMetadataLoaderTest {
    private static final String PIN = "a".repeat(64);

    @TempDir
    Path temporaryRoot;

    @Test
    void canonicalSnapshotDefensivelyOwnsItsBytesAndDigest() throws Exception {
        Path applicationJar = applicationJar();
        Path policy = temporaryRoot.resolve("policy.conf").toAbsolutePath().normalize();
        Files.writeString(policy, "policy\n", StandardCharsets.UTF_8);
        byte[] content = canonical(policy, PIN).getBytes(StandardCharsets.UTF_8);
        Files.write(applicationJar.resolveSibling(
                InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME), content);

        InstalledCancellationTrustMetadataLoader.CanonicalSnapshot snapshot =
                new InstalledCancellationTrustMetadataLoader(
                        applicationJar).loadCanonicalSnapshot();
        byte[] exposed = snapshot.bytes();
        exposed[0] ^= 1;

        assertArrayEquals(content, snapshot.bytes());
        assertEquals(64, snapshot.sha256().length());
        assertThrows(IOException.class, () ->
                InstalledCancellationTrustMetadataLoader.parseCanonical(new byte[0]));
    }

    @Test
    void loadsTheOnlyFixedMetadataSiblingOfAnExactApplicationJar() throws Exception {
        Path applicationJar = applicationJar();
        Path policy = temporaryRoot.resolve("policy.conf").toAbsolutePath().normalize();
        Files.writeString(policy, "policy\n", StandardCharsets.UTF_8);
        Files.writeString(
                applicationJar.resolveSibling(
                        InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME),
                canonical(policy, PIN),
                StandardCharsets.UTF_8);

        InstalledCancellationTrustMetadata metadata =
                new InstalledCancellationTrustMetadataLoader(applicationJar).load();

        assertEquals(policy, metadata.policyFile());
        assertEquals(PIN, metadata.expectedSha256());
    }

    @Test
    void rejectsExplodedOrCallerRedirectedInstallationState() throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
                new InstalledCancellationTrustMetadataLoader(
                        temporaryRoot.toAbsolutePath().normalize()));
        Path nonJar = temporaryRoot.resolve("enhancer.bin").toAbsolutePath().normalize();
        Files.writeString(nonJar, "application", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () ->
                new InstalledCancellationTrustMetadataLoader(nonJar));
        assertThrows(IllegalArgumentException.class, () ->
                new InstalledCancellationTrustMetadataLoader(Path.of("enhancer.jar")));
    }

    @Test
    void rejectsMissingOversizedAndNoncanonicalMetadata() throws Exception {
        Path applicationJar = applicationJar();
        InstalledCancellationTrustMetadataLoader loader =
                new InstalledCancellationTrustMetadataLoader(applicationJar);
        assertThrows(IOException.class, loader::load);

        Path metadata = applicationJar.resolveSibling(
                InstalledCancellationTrustMetadataLoader.METADATA_FILE_NAME);
        Files.write(metadata, new byte[
                InstalledCancellationTrustMetadataLoader.MAX_METADATA_BYTES + 1]);
        assertThrows(IOException.class, loader::load);

        Path policy = temporaryRoot.resolve("policy.conf").toAbsolutePath().normalize();
        String[] invalid = {
                canonical(policy, PIN).replace("\n", "\r\n"),
                canonical(policy, PIN).replace("policySha256=", "unknown=x\npolicySha256="),
                canonical(policy, PIN).replace("policySha256=", "\npolicySha256="),
                canonical(policy, PIN).replace(PIN, PIN.toUpperCase()),
                canonical(Path.of("relative-policy.conf"), PIN),
                canonical(policy, PIN) + "trailing=true\n"
        };
        for (String candidate : invalid) {
            Files.writeString(metadata, candidate, StandardCharsets.UTF_8);
            assertThrows(IOException.class, loader::load, candidate);
        }
        Files.write(metadata, new byte[] {(byte) 0xc3, (byte) 0x28});
        assertThrows(IOException.class, loader::load);
    }

    private Path applicationJar() throws Exception {
        Path applicationJar = temporaryRoot.resolve("enhancer.jar")
                .toAbsolutePath().normalize();
        Files.writeString(applicationJar, "application", StandardCharsets.UTF_8);
        return applicationJar;
    }

    private static String canonical(Path policy, String pin) {
        return "enhancer-installed-cancellation-trust-v1\n"
                + "policyPath=" + policy + "\n"
                + "policySha256=" + pin + "\n";
    }
}
