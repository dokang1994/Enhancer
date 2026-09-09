package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelExecutionProfileFileReaderTest {

    private static final String VALID_PROFILE = String.join("\n",
            "schemaVersion=model-execution-profile-v1",
            "requiredCapability=deterministic-echo",
            "modelClass=deterministic-fake",
            "localityRequirement=LOCAL_ONLY",
            "reasoningRequirement=STANDARD",
            "minimumContextTokens=16384",
            "tokenBudget.maxInputTokens=4096",
            "tokenBudget.maxOutputTokens=2048",
            "tokenBudget.maxTotalTokens=8192",
            "costBudget.currencyCode=USD",
            "costBudget.maxMicrounits=25000000",
            "maximumInvocationTimeMillis=30000",
            "dataClassification=CONFIDENTIAL") + "\n";

    @TempDir
    Path temporaryRoot;

    @Test
    void readsEveryExactProfileComponentFromTheCanonicalDocument() throws Exception {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("project"));
        Files.writeString(projectRoot.resolve("profile.txt"), VALID_PROFILE,
                StandardCharsets.UTF_8);

        ModelExecutionProfile profile = reader().read(projectRoot, Path.of("profile.txt"));

        assertEquals(new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "deterministic-echo",
                "deterministic-fake",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                16_384,
                new ModelTokenBudget(4096, 2048, 8192),
                new ModelCostBudget("USD", 25_000_000L),
                Duration.ofMillis(30_000),
                ModelDataClassification.CONFIDENTIAL), profile);
    }

    @Test
    void acceptsTheCanonicalZeroCostBoundary() throws Exception {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("zero-cost"));
        Files.writeString(projectRoot.resolve("profile.txt"), VALID_PROFILE.replace(
                "costBudget.maxMicrounits=25000000",
                "costBudget.maxMicrounits=0"), StandardCharsets.UTF_8);

        ModelExecutionProfile profile = reader().read(projectRoot, Path.of("profile.txt"));

        assertEquals(0, profile.costBudget().maxMicrounits());
    }

    @Test
    void rejectsEveryNonCanonicalDocumentShape() throws Exception {
        for (String invalid : new String[] {
                VALID_PROFILE.substring(0, VALID_PROFILE.length() - 1),
                VALID_PROFILE.replace("\n", "\r\n"),
                "\ufeff" + VALID_PROFILE,
                VALID_PROFILE.replace("modelClass=", "modelClass ="),
                VALID_PROFILE.replace("modelClass=deterministic-fake\n", ""),
                VALID_PROFILE.replace(
                        "modelClass=deterministic-fake\n",
                        "modelClass=deterministic-fake\nmodelClass=other\n"),
                VALID_PROFILE.replace(
                        "modelClass=deterministic-fake\n",
                        "unknown=value\nmodelClass=deterministic-fake\n"),
                VALID_PROFILE.replace(
                        "requiredCapability=deterministic-echo\nmodelClass=deterministic-fake\n",
                        "modelClass=deterministic-fake\nrequiredCapability=deterministic-echo\n"),
                VALID_PROFILE + "\n",
                "# comment\n" + VALID_PROFILE,
                VALID_PROFILE.replace("USD", "USD\u0000")
        }) {
            Path projectRoot = Files.createTempDirectory(temporaryRoot, "shape-");
            Files.writeString(projectRoot.resolve("profile.txt"), invalid,
                    StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class,
                    () -> reader().read(projectRoot, Path.of("profile.txt")));
        }
    }

    @Test
    void rejectsNonCanonicalNumbersAndOverflow() throws Exception {
        for (String invalidValue : new String[] {
                "0", "+1", "01", "-1", "1.0", "1e3", "١", Long.MAX_VALUE + "0"
        }) {
            assertInvalidReplacement("minimumContextTokens=16384",
                    "minimumContextTokens=" + invalidValue);
        }
        for (String invalidValue : new String[] {
                "+0", "00", "-1", "1.0", Long.MAX_VALUE + "0"
        }) {
            assertInvalidReplacement("costBudget.maxMicrounits=25000000",
                    "costBudget.maxMicrounits=" + invalidValue);
        }
    }

    @Test
    void delegatesClosedVocabularyAndRelationshipValidationToExistingValues()
            throws Exception {
        for (String invalid : new String[] {
                VALID_PROFILE.replace("model-execution-profile-v1",
                        "model-execution-profile-v2"),
                VALID_PROFILE.replace("deterministic-echo", "Upper"),
                VALID_PROFILE.replace("deterministic-fake", "unknown_model"),
                VALID_PROFILE.replace("LOCAL_ONLY", "REMOTE_ALLOWED"),
                VALID_PROFILE.replace("STANDARD", "standard"),
                VALID_PROFILE.replace("minimumContextTokens=16384",
                        "minimumContextTokens=1"),
                VALID_PROFILE.replace("tokenBudget.maxTotalTokens=8192",
                        "tokenBudget.maxTotalTokens=4096"),
                VALID_PROFILE.replace("USD", "usd"),
                VALID_PROFILE.replace("maximumInvocationTimeMillis=30000",
                        "maximumInvocationTimeMillis=300001"),
                VALID_PROFILE.replace("CONFIDENTIAL", "SECRET")
        }) {
            Path projectRoot = Files.createTempDirectory(temporaryRoot, "value-");
            Files.writeString(projectRoot.resolve("profile.txt"), invalid,
                    StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class,
                    () -> reader().read(projectRoot, Path.of("profile.txt")));
        }
    }

    @Test
    void rejectsMalformedUtf8AndContentBeyondTheConsumptionBound() throws Exception {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("bounded"));
        Path profile = projectRoot.resolve("profile.txt");
        Files.write(profile, new byte[] {(byte) 0xc3, (byte) 0x28});
        assertThrows(IOException.class, () -> reader().read(projectRoot, profile.getFileName()));

        Files.write(profile, "x".repeat(
                ModelExecutionProfileFileReader.MAX_PROFILE_BYTES + 1)
                .getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> reader().read(projectRoot, profile.getFileName()));
    }

    @Test
    void rejectsInvalidUnavailableAndNonRegularPaths() throws Exception {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("paths"));
        Path outside = Files.writeString(temporaryRoot.resolve("outside.txt"), VALID_PROFILE,
                StandardCharsets.UTF_8);
        Path directory = Files.createDirectory(projectRoot.resolve("directory"));

        assertThrows(IllegalArgumentException.class,
                () -> reader().read(projectRoot, Path.of("../outside.txt")));
        assertThrows(IllegalArgumentException.class,
                () -> reader().read(projectRoot, Path.of("directory/../profile.txt")));
        assertThrows(IllegalArgumentException.class,
                () -> reader().read(projectRoot, Path.of("")));
        assertThrows(IllegalArgumentException.class,
                () -> reader().read(projectRoot, outside.toAbsolutePath()));
        assertThrows(IOException.class,
                () -> reader().read(projectRoot, Path.of("missing.txt")));
        assertThrows(IOException.class,
                () -> reader().read(projectRoot, directory.getFileName()));
    }

    @Test
    void rejectsASymbolicProfileEvenWhenItsTargetIsContained() throws Exception {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("symbolic"));
        Path target = Files.writeString(projectRoot.resolve("real.txt"), VALID_PROFILE,
                StandardCharsets.UTF_8);
        Path link = projectRoot.resolve("profile.txt");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic-link creation is unavailable: " + exception);
        }

        assertThrows(IOException.class, () -> reader().read(projectRoot, link.getFileName()));
    }

    @Test
    void rejectsAWindowsJunctionThatEscapesTheRealProjectRoot() throws Exception {
        assumeTrue(isWindows(), "directory junction regression is Windows-specific");
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("junction-project"));
        Path outside = Files.createDirectory(temporaryRoot.resolve("junction-outside"));
        Files.writeString(outside.resolve("profile.txt"), VALID_PROFILE,
                StandardCharsets.UTF_8);
        Path junction = projectRoot.resolve("outside");
        createJunction(junction, outside);

        assertThrows(IllegalArgumentException.class,
                () -> reader().read(projectRoot, Path.of("outside/profile.txt")));
    }

    private void assertInvalidReplacement(String existing, String replacement)
            throws Exception {
        Path projectRoot = Files.createTempDirectory(temporaryRoot, "number-");
        Files.writeString(projectRoot.resolve("profile.txt"),
                VALID_PROFILE.replace(existing, replacement), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> reader().read(projectRoot, Path.of("profile.txt")));
    }

    private static ModelExecutionProfileFileReader reader() {
        return new ModelExecutionProfileFileReader();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private static void createJunction(Path junction, Path target) throws Exception {
        Process process = new ProcessBuilder(
                "cmd.exe", "/c", "mklink", "/J", junction.toString(), target.toString())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(10, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assumeTrue(completed, "junction creation timed out");
        assertEquals(0, process.exitValue(), "junction creation failed: " + output);
    }
}
