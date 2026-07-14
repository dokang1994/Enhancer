package com.enhancer.loop;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.planner.RepositoryTaskPlanner;
import com.enhancer.planner.TaskProposal;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class AssistedDevelopmentLoop {
    private final ProjectContextReader contextReader;
    private final RepositoryTaskPlanner taskPlanner;

    public AssistedDevelopmentLoop(
            ProjectContextReader contextReader,
            RepositoryTaskPlanner taskPlanner) {
        this.contextReader = Objects.requireNonNull(contextReader, "contextReader must not be null");
        this.taskPlanner = Objects.requireNonNull(taskPlanner, "taskPlanner must not be null");
    }

    public AssistedDevelopmentResult run(Path projectRoot) throws IOException {
        ProjectContext context = contextReader.read(projectRoot);
        Optional<TaskProposal> proposal = taskPlanner.propose(context);

        return proposal
                .map(AssistedDevelopmentResult::proposalAvailable)
                .orElseGet(AssistedDevelopmentResult::activeTaskPreserved);
    }
}
