package com.enhancer.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.planner.ProposalState;
import com.enhancer.planner.RepositoryTaskPlanner;
import com.enhancer.planner.TaskProposal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssistedDevelopmentLoopTest {
    @TempDir
    Path projectRoot;

    private final AssistedDevelopmentLoop loop = new AssistedDevelopmentLoop(
            new ProjectContextReader(),
            new RepositoryTaskPlanner());

    @Test
    void returnsThePlannedProposalWhenTheCurrentTaskIsCompleted() throws IOException {
        writeProject(
                "# Current Task\n\n## Status\n\nCompleted\n",
                "# Roadmap\n\n## Delivery Gate 3: Agent Loop Integration\n\n"
                        + "Status: Specified - Next\n\n"
                        + "Scope:\n\n- Connect context reading and planning\n\n"
                        + "Exit criteria:\n\n- A proposal is returned\n");

        AssistedDevelopmentResult result = loop.run(projectRoot);

        assertEquals(AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE, result.outcome());
        assertEquals("Delivery Gate 3: Agent Loop Integration", result.proposal().orElseThrow().title());
    }

    @Test
    void preservesAnActiveCurrentTaskWithoutReturningAProposal() throws IOException {
        writeProject(
                "# Current Task\n\n## Status\n\nIn Progress\n",
                "# Roadmap\n\n## Delivery Gate 3: Agent Loop Integration\n\n"
                        + "Status: Specified - Next\n");

        AssistedDevelopmentResult result = loop.run(projectRoot);

        assertEquals(AssistedDevelopmentOutcome.ACTIVE_TASK_PRESERVED, result.outcome());
        assertTrue(result.proposal().isEmpty());
    }

    @Test
    void matchesTheActualEnhancerTaskState() throws IOException {
        Path actualProjectRoot = Path.of(System.getProperty("user.dir"));

        AssistedDevelopmentResult result = loop.run(actualProjectRoot);
        String currentTask = Files.readString(
                actualProjectRoot.resolve("CURRENT_TASK.md"),
                StandardCharsets.UTF_8);

        String normalizedCurrentTask = currentTask.replace("\r\n", "\n").replace('\r', '\n');
        if (normalizedCurrentTask.contains("## Status\n\nCompleted")) {
            assertEquals(AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE, result.outcome());
            assertEquals(
                    "Delivery Gate 5: First Operational CLI",
                    result.proposal().orElseThrow().title());
        } else {
            assertEquals(AssistedDevelopmentOutcome.ACTIVE_TASK_PRESERVED, result.outcome());
            assertTrue(result.proposal().isEmpty());
        }
    }

    @Test
    void rejectsAnOutcomeThatContradictsItsProposalPayload() {
        IllegalArgumentException missingProposal = assertThrows(
                IllegalArgumentException.class,
                () -> new AssistedDevelopmentResult(
                        AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE,
                        Optional.empty()));
        TaskProposal proposal = new TaskProposal(
                "Next task",
                "Reason",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                ProposalState.PROPOSAL);
        IllegalArgumentException unexpectedProposal = assertThrows(
                IllegalArgumentException.class,
                () -> new AssistedDevelopmentResult(
                        AssistedDevelopmentOutcome.ACTIVE_TASK_PRESERVED,
                        Optional.of(proposal)));

        assertTrue(missingProposal.getMessage().contains("requires a proposal"));
        assertTrue(unexpectedProposal.getMessage().contains("cannot contain a proposal"));
    }

    private void writeProject(String currentTask, String roadmap) throws IOException {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            String content = switch (document) {
                case CURRENT_TASK -> currentTask;
                case ROADMAP -> roadmap;
                default -> "# " + document.name() + "\n";
            };
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
    }
}
