package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.runtime.DurableSubmissionManifest;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import com.enhancer.runtime.GeneratedSubmissionIdentities;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliDeterministicFakeModelSubmitIntegrationTest {

    private static final String TASK_ID = "typed-model-submit";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f31";
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
    void submitsOnceAndPrintsOnlyTheExactRetainedOutcome() throws Exception {
        Layout layout = layout("success");
        prepareGovernedProject(layout.projectRoot());
        writeProfile(layout.projectRoot(), PROFILE);

        Execution execution = execute(layout);

        assertEquals(0, execution.exitCode());
        assertEquals("", execution.stderr());
        DurableSubmissionManifest manifest = new FileSystemSubmissionManifestStore(
                layout.submissionRoot()).resolve(SUBMISSION_ID);
        ModelWorkPayload work = (ModelWorkPayload) manifest.workMessage().payload();
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID);
        assertEquals("deterministic-echo", manifest.requiredCapability());
        assertEquals("different-requirement",
                work.executionInput().executionProfile().requiredCapability());
        assertEquals(ModelExecutionProfile.SCHEMA_VERSION,
                work.executionInput().executionProfile().schemaVersion());
        assertEquals(List.of("model-invoke"),
                work.allowedTools().stream().sorted().toList());
        assertEquals(String.join("\n",
                "status=ADMITTED",
                "exitCode=0",
                "submissionId=" + SUBMISSION_ID,
                "queueId=" + identities.queueId(),
                "correlationId=" + identities.correlationId(),
                "logicalRunId=" + identities.logicalRunId(),
                "occurredAt=" + manifest.workMessage().occurredAt(),
                "queueRevision=1",
                "priority=EXPEDITED",
                "manifestCreated=true",
                "queueCreated=true",
                "workAdmitted=true",
                "workspaceSnapshotId=" + work.snapshotId()) + "\n", execution.stdout());
        assertTrue(execution.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(execution.stdout().contains("profiles/model.profile"));
        assertFalse(execution.stdout().contains("different-requirement"));
        assertFalse(execution.stdout().contains("prompts/request.txt"));
        assertFalse(execution.stdout().contains("a".repeat(64)));
    }

    @Test
    void rejectsTheProfileBeforeCreatingOrAccessingDurableRootsAndRedactsValues()
            throws Exception {
        Layout layout = layout("invalid-profile");
        Files.createDirectories(layout.projectRoot().resolve("profiles"));
        writeProfile(layout.projectRoot(),
                "schemaVersion=secret-profile-value\nrequiredCapability=secret-value\n");

        Execution execution = execute(layout);

        assertEquals(2, execution.exitCode());
        assertEquals("", execution.stdout());
        assertFalse(Files.exists(layout.submissionRoot()));
        assertFalse(Files.exists(layout.queueRoot()));
        assertTrue(execution.stderr().startsWith("status=ERROR\nexitCode=2\n"));
        assertTrue(execution.stderr().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(execution.stderr().contains("profiles/model.profile"));
        assertFalse(execution.stderr().contains("secret-profile-value"));
        assertFalse(execution.stderr().contains("secret-value"));
        assertFalse(execution.stderr().contains("requiredCapability"));
    }

    @Test
    void classifiesDurableFilesystemFailureAsInternalAfterProfileAcceptance()
            throws Exception {
        Layout layout = layout("durable-failure");
        prepareGovernedProject(layout.projectRoot());
        writeProfile(layout.projectRoot(), PROFILE);
        Files.createDirectories(layout.submissionRoot().getParent());
        Files.writeString(layout.submissionRoot(), "not-a-directory", StandardCharsets.UTF_8);

        Execution execution = execute(layout);

        assertEquals(70, execution.exitCode());
        assertEquals("", execution.stdout());
        assertTrue(execution.stderr().startsWith("status=ERROR\nexitCode=70\n"));
        assertFalse(Files.exists(layout.queueRoot()));
    }

    @Test
    void classifiesInvalidFirstUseProjectConfigurationBeforeDurableMutation()
            throws Exception {
        Layout layout = layout("project-configuration");
        writeProfile(layout.projectRoot(), PROFILE);

        Execution execution = execute(layout);

        assertEquals(2, execution.exitCode());
        assertEquals("", execution.stdout());
        assertTrue(execution.stderr().startsWith("status=ERROR\nexitCode=2\n"));
        assertFalse(Files.exists(layout.submissionRoot()));
        assertFalse(Files.exists(layout.queueRoot()));
    }

    private Execution execute(Layout layout) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(arguments(layout),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(Layout layout) {
        return new String[] {
                "scheduler-submit-deterministic-fake-model-work",
                "--project-root", layout.projectRoot().toString(),
                "--submission-root", layout.submissionRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--task-id", TASK_ID,
                "--submission-id", SUBMISSION_ID,
                "--max-work-items", "8",
                "--producer", "typed-model-cli",
                "--target-path", "prompts/request.txt",
                "--expected-response-sha256", "a".repeat(64),
                "--model-execution-profile-file", "profiles/model.profile",
                "--priority", "EXPEDITED"
        };
    }

    private void prepareGovernedProject(Path projectRoot) throws Exception {
        String currentTask = "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nSubmit typed model work.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? currentTask
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
        Path target = projectRoot.resolve("prompts/request.txt");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "prompt\n", StandardCharsets.UTF_8);
    }

    private void writeProfile(Path projectRoot, String content) throws Exception {
        Path profile = projectRoot.resolve("profiles/model.profile");
        Files.createDirectories(profile.getParent());
        Files.writeString(profile, content, StandardCharsets.UTF_8);
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(
                root.resolve("project"),
                root.resolve("submissions"),
                root.resolve("queue"));
    }

    private record Layout(Path projectRoot, Path submissionRoot, Path queueRoot) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
