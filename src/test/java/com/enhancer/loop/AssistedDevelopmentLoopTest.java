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
                "# Roadmap\n\n## Phase 4: Assisted Development Loop\n\nStatus: Ready\n\n"
                        + "- Connect context reading and planning\n");

        AssistedDevelopmentResult result = loop.run(projectRoot);

        assertEquals(AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE, result.outcome());
        assertEquals("Phase 4: Assisted Development Loop", result.proposal().orElseThrow().title());
    }

    @Test
    void preservesAnActiveCurrentTaskWithoutReturningAProposal() throws IOException {
        writeProject(
                "# Current Task\n\n## Status\n\nIn Progress\n",
                "# Roadmap\n\n## Phase 4: Assisted Development Loop\n\nStatus: Ready\n");

        AssistedDevelopmentResult result = loop.run(projectRoot);

        assertEquals(AssistedDevelopmentOutcome.ACTIVE_TASK_PRESERVED, result.outcome());
        assertTrue(result.proposal().isEmpty());
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
            Files.writeString(projectRoot.resolve(document.path()), content, StandardCharsets.UTF_8);
        }
    }
}
