package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProtectedRemoteCapabilityCatalogFactSourceSnapshotTest {
    private static final Instant EVALUATION_TIME =
            Instant.parse("2026-10-23T00:01:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void rejectsSymbolicInstallationCatalogAndBindingPathsAtOwnedBoundary()
            throws Exception {
        Path realRoot = Files.createDirectory(temporaryRoot.resolve("real-root"));
        Files.createDirectory(catalogDirectory(realRoot));
        writeCanonicalBinding(realRoot, "1".repeat(64));

        Path rootLink = temporaryRoot.resolve("root-link");
        if (!createSymbolicLink(rootLink, realRoot)) {
            return;
        }
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
                refusal(bindingPath(rootLink)));

        Path catalogCase = Files.createDirectory(
                temporaryRoot.resolve("catalog-link-case"));
        Path realCatalogs = Files.createDirectory(
                temporaryRoot.resolve("real-catalogs"));
        if (!createSymbolicLink(catalogDirectory(catalogCase), realCatalogs)) {
            return;
        }
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
                refusal(bindingPath(catalogCase)));

        Path bindingCase = Files.createDirectory(
                temporaryRoot.resolve("binding-link-case"));
        Files.createDirectory(catalogDirectory(bindingCase));
        Path realBinding = temporaryRoot.resolve("real-binding");
        Files.write(realBinding, RemoteCapabilityCatalogBindingCodec.create(
                catalogDirectory(bindingCase), "2".repeat(64)).canonicalBytes());
        if (!createSymbolicLink(bindingPath(bindingCase), realBinding)) {
            return;
        }
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_UNAVAILABLE_OR_UNSAFE,
                refusal(bindingPath(bindingCase)));
    }

    @Test
    void classifiesNonRegularSizeAndCanonicalFailuresInOrder() throws IOException {
        Path root = Files.createDirectory(temporaryRoot.resolve("shape-case"));
        Files.createDirectory(catalogDirectory(root));
        Files.createDirectory(bindingPath(root));
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_UNAVAILABLE_OR_UNSAFE,
                refusal(bindingPath(root)));

        Files.delete(bindingPath(root));
        Files.write(bindingPath(root), new byte[4097]);
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_OUTSIDE_SIZE_BOUND,
                refusal(bindingPath(root)));

        Files.write(bindingPath(root), new byte[4096]);
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.BINDING_INVALID,
                refusal(bindingPath(root)));

        Path foreignDirectory = Files.createDirectories(
                temporaryRoot.resolve("foreign")
                        .resolve(RemoteCapabilityCatalogBindingCodec
                                .CATALOG_DIRECTORY_NAME));
        byte[] foreign = RemoteCapabilityCatalogBindingCodec.create(
                foreignDirectory, "3".repeat(64)).canonicalBytes();
        Files.write(bindingPath(root), foreign);
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.BINDING_INVALID,
                refusal(bindingPath(root)));

        RemoteCapabilityCatalogBinding local =
                RemoteCapabilityCatalogBindingCodec.create(
                        catalogDirectory(root), "4".repeat(64));
        String mismatch = new String(
                local.canonicalBytes(), StandardCharsets.UTF_8)
                .replace("4".repeat(64) + ".conf", "5".repeat(64) + ".conf");
        Files.writeString(bindingPath(root), mismatch, StandardCharsets.UTF_8);
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.BINDING_INVALID,
                refusal(bindingPath(root)));
    }

    @Test
    void rejectsFixedCatalogDirectoryThroughWindowsJunction() throws Exception {
        assumeTrue(isWindows(), "directory junction regression is Windows-specific");
        Path root = Files.createDirectory(temporaryRoot.resolve("junction-case"));
        Path target = Files.createDirectory(temporaryRoot.resolve("junction-target"));
        createJunction(catalogDirectory(root), target);

        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
                refusal(bindingPath(root)));
    }

    private ProtectedRemoteCapabilityCatalogFactSourceRefusalReason refusal(
            Path binding) {
        return assertInstanceOf(
                ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                new ProtectedRemoteCapabilityCatalogFactSource(
                        binding.toAbsolutePath().normalize())
                        .acquire(
                                "candidate-a",
                                "destination-a",
                                EVALUATION_TIME))
                .reason();
    }

    private static void writeCanonicalBinding(Path root, String digest)
            throws IOException {
        Files.write(
                bindingPath(root),
                RemoteCapabilityCatalogBindingCodec.create(
                        catalogDirectory(root), digest).canonicalBytes());
    }

    private static boolean createSymbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false,
                    "symbolic links unavailable: "
                            + exception.getClass().getSimpleName());
            return false;
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
        assertEquals(0, process.exitValue(),
                "junction creation failed: " + output);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static Path bindingPath(Path root) {
        return root.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
    }

    private static Path catalogDirectory(Path root) {
        return root.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
    }
}
