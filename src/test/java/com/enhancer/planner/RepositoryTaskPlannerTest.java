package com.enhancer.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepositoryTaskPlannerTest {
    private final RepositoryTaskPlanner planner = new RepositoryTaskPlanner();

    @Test
    void proposesTheFirstReadyRoadmapPhase() {
        ProjectContext context = context(
                "# Current Task\n\n## Status\n\nCompleted\n",
                "# Roadmap\n\n## Phase 2: Context\n\nStatus: Implemented\n\n"
                        + "## Phase 3: Task Planner\n\nStatus: Ready\n\n"
                        + "- Generate candidate tasks\n- Add planning tests\n\n"
                        + "## Phase 4: Agent Loop\n\nStatus: Pending\n");

        TaskProposal proposal = planner.propose(context).orElseThrow();

        assertEquals("Phase 3: Task Planner", proposal.title());
        assertEquals(List.of("Generate candidate tasks", "Add planning tests"), proposal.scope());
        assertEquals(ProposalState.PROPOSAL, proposal.state());
        assertFalse(proposal.acceptanceCriteria().isEmpty());
        assertFalse(proposal.outOfScope().isEmpty());
    }

    @Test
    void doesNotOverrideAnActiveCurrentTask() {
        ProjectContext context = context(
                "# Current Task\n\n## Status\n\nIn Progress\n",
                "# Roadmap\n\n## Phase 3: Task Planner\n\nStatus: Ready\n");

        Optional<TaskProposal> proposal = planner.propose(context);

        assertTrue(proposal.isEmpty());
    }

    @Test
    void marksMissingRoadmapScopeAsRisk() {
        ProjectContext context = context(
                "# Current Task\n\n## Status\n\nCompleted\n",
                "# Roadmap\n\n## Phase 3: Task Planner\n\nStatus: Ready\n");

        TaskProposal proposal = planner.propose(context).orElseThrow();

        assertTrue(proposal.scope().isEmpty());
        assertTrue(proposal.risks().stream().anyMatch(risk -> risk.contains("does not define scope")));
    }

    private ProjectContext context(String currentTask, String roadmap) {
        return new ProjectContext(List.of(
                new ProjectDocument("CURRENT_TASK.md", 1, currentTask),
                new ProjectDocument("ROADMAP.md", 2, roadmap)));
    }
}
