package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.runtime.AgentRunEvidenceIdentity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemEvidenceRunNamespaceStoreIntegrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void createsAndExactReplaysOneDirectCanonicalRunNamespace() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        String runId = UUID.randomUUID().toString();
        EvidenceRunNamespaceStore store = store(storageRoot);

        store.ensureRun(runId);
        Path runDirectory = storageRoot.resolve(runId);
        Path retained = runDirectory.resolve("retained.txt");
        byte[] content = "retained".getBytes(StandardCharsets.UTF_8);
        Files.write(retained, content);
        FileTime retainedTime = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(retained, retainedTime);

        store.ensureRun(runId);

        assertTrue(Files.isDirectory(runDirectory));
        assertArrayEquals(content, Files.readAllBytes(retained));
        assertEquals(retainedTime, Files.getLastModifiedTime(retained));
        try (var paths = Files.list(runDirectory)) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    void ensuredNamespaceSupportsExistingLongEvidencePersistence() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        String runId = UUID.randomUUID().toString();
        FileSystemEvidenceStore concrete = concreteStore(storageRoot);
        EvidenceRunNamespaceStore namespaces = concrete;
        String output = "prefix-" + "x".repeat(
                VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS);

        namespaces.ensureRun(runId);
        VerificationEvidence evidence = new EvidenceRecorder(concrete)
                .capture(runId, "large result", output);

        assertTrue(evidence.truncated());
        assertEquals(
                output,
                concrete.resolve(
                        evidence.fullOutputReference().orElseThrow()).content());
    }

    @Test
    void shortInlineEvidenceNeedsNoNamespaceOrFilesystemWrite() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        String runId = AgentRunEvidenceIdentity.runId(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        VerificationEvidence evidence = new EvidenceRecorder(concreteStore(storageRoot))
                .capture(runId, "short result", "complete");

        assertFalse(evidence.truncated());
        assertTrue(evidence.fullOutputReference().isEmpty());
        assertFalse(Files.exists(storageRoot));
    }

    @Test
    void rejectsInvalidIdentityBeforeCreatingStorage() {
        Path storageRoot = tempDirectory.resolve("evidence");
        EvidenceRunNamespaceStore store = store(storageRoot);

        assertThrows(
                IllegalArgumentException.class,
                () -> store.ensureRun("../escape"));

        assertFalse(Files.exists(storageRoot));
    }

    @Test
    void rejectsRootAndRunFilesWithoutReplacement() throws IOException {
        Path rootFile = tempDirectory.resolve("root-file");
        byte[] rootContent = "root".getBytes(StandardCharsets.UTF_8);
        Files.write(rootFile, rootContent);
        String firstRun = UUID.randomUUID().toString();

        assertThrows(
                IOException.class,
                () -> store(rootFile).ensureRun(firstRun));
        assertArrayEquals(rootContent, Files.readAllBytes(rootFile));

        Path storageRoot = Files.createDirectory(tempDirectory.resolve("evidence"));
        String secondRun = UUID.randomUUID().toString();
        Path runFile = storageRoot.resolve(secondRun);
        byte[] runContent = "run".getBytes(StandardCharsets.UTF_8);
        Files.write(runFile, runContent);

        assertThrows(
                IOException.class,
                () -> store(storageRoot).ensureRun(secondRun));
        assertArrayEquals(runContent, Files.readAllBytes(runFile));
    }

    @Test
    void rejectsSymbolicRunNamespaceWithoutFollowingOrReplacingIt() throws IOException {
        Path storageRoot = Files.createDirectory(tempDirectory.resolve("evidence"));
        Path outside = Files.createDirectory(tempDirectory.resolve("outside"));
        String runId = UUID.randomUUID().toString();
        Path link = storageRoot.resolve(runId);
        try {
            Files.createSymbolicLink(link, outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic links unavailable on this host");
        }

        assertThrows(IOException.class, () -> store(storageRoot).ensureRun(runId));
        assertTrue(Files.isSymbolicLink(link));
    }

    @Test
    void rejectsAWindowsJunctionRunNamespaceWithoutWritingOutsideStorage()
            throws Exception {
        assumeTrue(isWindows(), "directory junction regression is Windows-specific");
        Path storageRoot = Files.createDirectory(tempDirectory.resolve("evidence"));
        Path outside = Files.createDirectory(tempDirectory.resolve("outside-junction"));
        Path retained = outside.resolve("retained.txt");
        Files.writeString(retained, "retained", StandardCharsets.UTF_8);
        String runId = UUID.randomUUID().toString();
        Path junction = storageRoot.resolve(runId);
        createJunction(junction, outside);

        assertThrows(IOException.class, () -> store(storageRoot).ensureRun(runId));
        assertEquals("retained", Files.readString(retained, StandardCharsets.UTF_8));
        try (var paths = Files.list(outside)) {
            assertEquals(1, paths.count());
        }
    }

    private static void createJunction(Path junction, Path target) throws Exception {
        String commandInterpreter = System.getenv().getOrDefault(
                "ComSpec", "C:\\Windows\\System32\\cmd.exe");
        Process process = new ProcessBuilder(
                commandInterpreter,
                "/d",
                "/c",
                "mklink",
                "/J",
                junction.toString(),
                target.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(
                process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(10, TimeUnit.SECONDS),
                "junction creation timed out");
        assertEquals(0, process.exitValue(), "junction creation failed: " + output);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private EvidenceRunNamespaceStore store(Path storageRoot) {
        return concreteStore(storageRoot);
    }

    private FileSystemEvidenceStore concreteStore(Path storageRoot) {
        return new FileSystemEvidenceStore(
                storageRoot,
                new EvidenceStoragePolicy(16 * 1024));
    }
}
