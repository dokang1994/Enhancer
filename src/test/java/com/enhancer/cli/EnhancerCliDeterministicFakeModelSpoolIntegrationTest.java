package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.runtime.DurableSubmissionManifest;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliDeterministicFakeModelSpoolIntegrationTest {
    private static final String TASK_ID = "typed-model-spool";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f47";
    private static final String PROFILE = String.join("\n",
            "schemaVersion=model-execution-profile-v1",
            "requiredCapability=different-requirement",
            "modelClass=deterministic-fake",
            "localityRequirement=LOCAL_ONLY",
            "reasoningRequirement=STANDARD",
            "minimumContextTokens=16384",
            "tokenBudget.maxInputTokens=4096",
            "tokenBudget.maxOutputTokens=2048",
            "tokenBudget.maxTotalTokens=8192",
            "costBudget.currencyCode=USD",
            "costBudget.maxMicrounits=0",
            "maximumInvocationTimeMillis=30000",
            "dataClassification=PUBLIC") + "\n";

    @TempDir
    Path temporaryRoot;

    @Test
    void publishesManifestDerivedTypedPointAndPrintsOnlyBoundedOutcome()
            throws Exception {
        Layout layout = layout("success");
        prepareProject(layout.projectRoot(), PROFILE);

        Execution execution = execute(arguments(layout, "2"));

        assertEquals(0, execution.exitCode());
        assertEquals("", execution.stderr());
        assertTrue(execution.stdout().startsWith("status=ACCEPTED\nexitCode=0\n"));
        DurableSubmissionManifest manifest = new FileSystemSubmissionManifestStore(
                layout.submissionRoot()).resolve(SUBMISSION_ID);
        assertEquals(SUBMISSION_ID, value(execution.stdout(), "submissionId"));
        assertEquals(manifest.queueId(), value(execution.stdout(), "queueId"));
        assertEquals("true", value(execution.stdout(), "manifestCreated"));
        String messageFile = value(execution.stdout(), "messageFile");
        assertTrue(messageFile.endsWith(FileSpoolMessageTransport.FILE_SUFFIX));
        assertEquals(manifest.workMessage(), FileSpoolMessageTransport.read(
                layout.spoolRoot().resolve(messageFile)).envelope());
        assertTrue(execution.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        for (String secret : new String[] {
                "profiles/model.profile", "different-requirement",
                "prompts/request.txt", "a".repeat(64), "deterministic-echo"
        }) {
            assertFalse(execution.stdout().contains(secret));
        }
        assertFalse(Files.exists(layout.root().resolve("queue")));
    }

    @Test
    void reportsBackpressureWithoutAnotherPointAndRetainsExactManifestReplay()
            throws Exception {
        Layout layout = layout("backpressure");
        prepareProject(layout.projectRoot(), PROFILE);
        assertEquals(0, execute(arguments(layout, "1")).exitCode());

        Execution replay = execute(arguments(layout, "1"));

        assertEquals(0, replay.exitCode());
        assertEquals("BACKPRESSURED", value(replay.stdout(), "status"));
        assertEquals("false", value(replay.stdout(), "manifestCreated"));
        assertEquals("", value(replay.stdout(), "messageFile"));
        assertFalse(value(replay.stdout(), "reason").isBlank());
        try (var points = Files.list(layout.spoolRoot())) {
            assertEquals(1, points.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void invalidProfileFailsBeforeManifestOrSpoolAccess() throws Exception {
        Layout layout = layout("invalid-profile");
        prepareProject(layout.projectRoot(), "schemaVersion=secret\n");

        Execution execution = execute(arguments(layout, "1"));

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), execution.exitCode());
        assertTrue(execution.stderr().contains("profile input is invalid"));
        assertFalse(execution.stderr().contains("schemaVersion=secret"));
        assertFalse(Files.exists(layout.submissionRoot()));
        assertFalse(Files.exists(layout.spoolRoot()));
    }

    @Test
    void unavailableSpoolIsAnOrdinaryRedactedOutcomeAfterManifestPersistence()
            throws Exception {
        Layout layout = layout("unavailable");
        prepareProject(layout.projectRoot(), PROFILE);
        Files.createDirectories(layout.spoolRoot().getParent());
        Files.writeString(layout.spoolRoot(), "occupied", StandardCharsets.UTF_8);

        Execution execution = execute(arguments(layout, "1"));

        assertEquals(0, execution.exitCode());
        assertEquals("UNAVAILABLE", value(execution.stdout(), "status"));
        assertEquals("", value(execution.stdout(), "messageFile"));
        assertEquals("transport spool is unavailable",
                value(execution.stdout(), "reason"));
        assertFalse(execution.stdout().contains(layout.spoolRoot().toString()));
        assertEquals("occupied", Files.readString(layout.spoolRoot()));
        assertTrue(Files.isRegularFile(layout.submissionRoot().resolve(
                SUBMISSION_ID + ".submission-manifest")));
    }

    private Execution execute(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(Layout layout, String maximum) {
        return new String[] {
                "scheduler-spool-deterministic-fake-model-work",
                "--project-root", layout.projectRoot().toString(),
                "--submission-root", layout.submissionRoot().toString(),
                "--transport-spool-root", layout.spoolRoot().toString(),
                "--task-id", TASK_ID,
                "--submission-id", SUBMISSION_ID,
                "--max-work-items", "8",
                "--max-pending-publications", maximum,
                "--producer", "typed-model-publisher",
                "--target-path", "prompts/request.txt",
                "--expected-response-sha256", "a".repeat(64),
                "--model-execution-profile-file", "profiles/model.profile",
                "--priority", "EXPEDITED"
        };
    }

    private void prepareProject(Path projectRoot, String profile) throws Exception {
        String task = "# Current Task\n\n## Status\n\nIn Progress\n\n"
                + "## Task\n\nPublish typed model work.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? task
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
        Path profilePath = projectRoot.resolve("profiles/model.profile");
        Files.createDirectories(profilePath.getParent());
        Files.writeString(profilePath, profile, StandardCharsets.UTF_8);
    }

    private String value(String output, String name) {
        return output.lines()
                .filter(line -> line.startsWith(name + "="))
                .map(line -> line.substring(name.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(root, root.resolve("project"),
                root.resolve("submissions"), root.resolve("spool"));
    }

    private record Layout(
            Path root, Path projectRoot, Path submissionRoot, Path spoolRoot) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
