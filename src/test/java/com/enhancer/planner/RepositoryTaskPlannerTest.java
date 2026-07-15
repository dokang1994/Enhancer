package com.enhancer.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepositoryTaskPlannerTest {
    private final RepositoryTaskPlanner planner = new RepositoryTaskPlanner();

    @Test
    void proposesTheFirstNextDeliveryGateWithStructuredScopeAndAcceptanceCriteria() {
        ProjectContext context = context(
                "# Current Task\n\n## Status\n\nCompleted\n",
                "# Roadmap\n\n## Delivery Gate 1: Tool Execution\n\nStatus: Integrated\n\n"
                        + "## Delivery Gate 2: Evidence Persistence\n\nStatus: Specified - Next\n\n"
                        + "Required capabilities:\n\n"
                        + "- Store complete evidence\n- Validate evidence integrity\n\n"
                        + "Exit criteria:\n\n"
                        + "- Missing evidence is rejected\n- Corruption is detected\n\n"
                        + "## Delivery Gate 3: Loop Integration\n\nStatus: Specified\n");

        TaskProposal proposal = planner.propose(context).orElseThrow();

        assertEquals("Delivery Gate 2: Evidence Persistence", proposal.title());
        assertEquals(List.of("Store complete evidence", "Validate evidence integrity"), proposal.scope());
        assertEquals(
                List.of("Missing evidence is rejected", "Corruption is detected"),
                proposal.acceptanceCriteria());
        assertEquals(ProposalState.PROPOSAL, proposal.state());
        assertFalse(proposal.acceptanceCriteria().isEmpty());
        assertFalse(proposal.outOfScope().isEmpty());
    }

    @Test
    void doesNotOverrideAnActiveCurrentTask() {
        ProjectContext context = context(
                "# Current Task\n\n## Status\n\nIn Progress\n",
                "# Roadmap\n\n## Delivery Gate 2: Evidence\n\nStatus: Specified - Next\n");

        Optional<TaskProposal> proposal = planner.propose(context);

        assertTrue(proposal.isEmpty());
    }

    @Test
    void marksMissingDeliveryGateScopeAsRisk() {
        ProjectContext context = context(
                "# Current Task\n\n## Status\n\nCompleted\n",
                "# Roadmap\n\n## Delivery Gate 2: Evidence\n\nStatus: Specified - Next\n");

        TaskProposal proposal = planner.propose(context).orElseThrow();

        assertTrue(proposal.scope().isEmpty());
        assertTrue(proposal.risks().stream().anyMatch(risk -> risk.contains("does not define scope")));
    }

    @Test
    void proposesTheCurrentNextGateFromTheActualEnhancerRoadmap() throws IOException {
        Path roadmapPath = Path.of(System.getProperty("user.dir")).resolve("ROADMAP.md");
        String roadmap = Files.readString(roadmapPath, StandardCharsets.UTF_8);

        TaskProposal proposal = planner.propose(context(
                "# Current Task\n\n## Status\n\nCompleted\n",
                roadmap)).orElseThrow();

        assertEquals("Delivery Gate 7: Event Bus And IPC Foundation", proposal.title());
        assertTrue(proposal.scope().contains(
                "typed domain events and versioned message envelopes;"));
        assertTrue(proposal.acceptanceCriteria().contains(
                "authorization and provenance survive every hop;"));
    }

    private ProjectContext context(String currentTask, String roadmap) {
        return new ProjectContext(List.of(
                new ProjectDocument("CURRENT_TASK.md", 1, currentTask),
                new ProjectDocument("ROADMAP.md", 2, roadmap)));
    }
}
