package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.runtime.DeterministicFakeModelWorkPublicationResult;
import com.enhancer.runtime.FileSystemDeterministicFakeModelWorkPublisher;
import com.enhancer.runtime.SchedulerPriority;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliDeterministicFakeModelReceiveIntegrationTest {
    private static final String TASK_ID = "typed-receive-cli";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f87";

    @TempDir
    Path temporaryRoot;

    @Test
    void receivesThenReplaysAcknowledgedPointWithExactBoundedOutput()
            throws Exception {
        Path project = temporaryRoot.resolve("project");
        Path submissions = temporaryRoot.resolve("submissions");
        Path spool = temporaryRoot.resolve("spool");
        Path queue = temporaryRoot.resolve("queue");
        writeGovernedProject(project);
        DeterministicFakeModelWorkPublicationResult publication =
                new FileSystemDeterministicFakeModelWorkPublisher(
                        project, submissions, spool, 2).publish(
                                SUBMISSION_ID, TASK_ID, "receive-cli-test",
                                "prompts/request.txt", "a".repeat(64), profile(), 8,
                                SchedulerPriority.EXPEDITED);
        String file = publication.publication().messageFile().orElseThrow();
        String[] arguments = arguments(spool, file, submissions, queue);

        Execution admitted = execute(arguments);
        Execution replayed = execute(arguments);

        assertEquals(0, admitted.exitCode());
        assertEquals("", admitted.stderr());
        assertEquals("ADMITTED", value(admitted.stdout(), "status"));
        assertEquals("ACKNOWLEDGED", value(admitted.stdout(), "spoolStatus"));
        assertEquals("0", value(admitted.stdout(), "exitCode"));
        assertEquals(SUBMISSION_ID, value(admitted.stdout(), "submissionId"));
        assertEquals(SUBMISSION_ID, value(admitted.stdout(), "messageId"));
        assertEquals("1", value(admitted.stdout(), "queueRevision"));
        assertEquals("EXPEDITED", value(admitted.stdout(), "priority"));
        assertFalse(value(admitted.stdout(), "queueId").isBlank());
        assertFalse(value(admitted.stdout(), "workItemId").isBlank());
        assertTrue(value(admitted.stdout(), "acknowledgedFile").endsWith(".received"));
        assertTrue(admitted.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertEquals("REPLAYED", value(replayed.stdout(), "status"));
        assertEquals("ALREADY_ACKNOWLEDGED",
                value(replayed.stdout(), "spoolStatus"));
        assertEquals("1", value(replayed.stdout(), "queueRevision"));
        for (String forbidden : new String[] {
                project.toString(), submissions.toString(), spool.toString(),
                "different-requirement", "prompts/request.txt", "a".repeat(64),
                "deterministic-echo"
        }) {
            assertFalse(admitted.stdout().contains(forbidden));
        }
    }

    private String[] arguments(
            Path spool, String file, Path submissions, Path queue) {
        return new String[] {
                "scheduler-receive-deterministic-fake-model-work",
                "--transport-spool-root", spool.toString(),
                "--message-file", file,
                "--submission-root", submissions.toString(),
                "--queue-root", queue.toString()
        };
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

    private String value(String output, String name) {
        return output.lines().filter(line -> line.startsWith(name + "="))
                .map(line -> line.substring(name.length() + 1))
                .findFirst().orElseThrow();
    }

    private ModelExecutionProfile profile() {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "different-requirement",
                "deterministic-fake",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                16384,
                new ModelTokenBudget(4096, 2048, 8192),
                new ModelCostBudget("USD", 0),
                Duration.ofSeconds(30),
                ModelDataClassification.PUBLIC);
    }

    private void writeGovernedProject(Path projectRoot) throws Exception {
        String task = "# Current Task\n\n## Status\n\nIn Progress\n\n"
                + "## Task\n\nReceive typed model work.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by test owner.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? task : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
