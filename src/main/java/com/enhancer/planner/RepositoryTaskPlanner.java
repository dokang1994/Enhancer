package com.enhancer.planner;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RepositoryTaskPlanner {
    private static final String CURRENT_TASK = "CURRENT_TASK.md";
    private static final String ROADMAP = "ROADMAP.md";

    public Optional<TaskProposal> propose(ProjectContext context) {
        Objects.requireNonNull(context, "context must not be null");

        String currentTask = requiredContent(context, CURRENT_TASK);
        String currentStatus = section(currentTask, "Status")
                .orElseThrow(() -> new PlanningException("CURRENT_TASK.md does not define a Status section"));
        if (!firstContentLine(currentStatus).equalsIgnoreCase("Completed")) {
            return Optional.empty();
        }

        ReadyPhase phase = firstReadyPhase(requiredContent(context, ROADMAP));
        List<String> risks = new ArrayList<>();
        if (phase.scope().isEmpty()) {
            risks.add("The ready roadmap phase does not define scope items.");
        }
        risks.add("Dependencies and detailed acceptance criteria are not defined by the roadmap phase.");

        return Optional.of(new TaskProposal(
                phase.title(),
                "The current task is completed and this is the first roadmap phase marked Ready.",
                phase.scope(),
                List.of(
                        "The selected roadmap phase has a focused implementation and tests.",
                        "Project state and handoff documents reflect the result."),
                List.of(
                        "Items from later roadmap phases",
                        "Automatic proposal acceptance or execution"),
                risks,
                ProposalState.PROPOSAL));
    }

    private String requiredContent(ProjectContext context, String path) {
        return context.documents().stream()
                .filter(document -> document.path().equals(path))
                .map(ProjectDocument::content)
                .findFirst()
                .orElseThrow(() -> new PlanningException("Project context is missing required document: " + path));
    }

    private ReadyPhase firstReadyPhase(String roadmap) {
        String normalized = normalize(roadmap);
        String[] lines = normalized.split("\n", -1);

        for (int index = 0; index < lines.length; index++) {
            if (!lines[index].startsWith("## Phase ")) {
                continue;
            }

            int end = index + 1;
            while (end < lines.length && !lines[end].startsWith("## Phase ")) {
                end++;
            }

            List<String> phaseLines = List.of(lines).subList(index + 1, end);
            boolean ready = phaseLines.stream().anyMatch(line -> line.trim().equalsIgnoreCase("Status: Ready"));
            if (ready) {
                List<String> scope = phaseLines.stream()
                        .map(String::trim)
                        .filter(line -> line.startsWith("- "))
                        .map(line -> line.substring(2).trim())
                        .filter(line -> !line.isEmpty())
                        .toList();
                return new ReadyPhase(lines[index].substring(3).trim(), scope);
            }

            index = end - 1;
        }

        throw new PlanningException("ROADMAP.md does not contain a phase marked Ready");
    }

    private Optional<String> section(String markdown, String heading) {
        String normalized = normalize(markdown);
        String marker = "## " + heading;
        int start = normalized.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }

        start += marker.length();
        int end = normalized.indexOf("\n## ", start);
        return Optional.of(normalized.substring(start, end < 0 ? normalized.length() : end).trim());
    }

    private String firstContentLine(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElseThrow(() -> new PlanningException("CURRENT_TASK.md Status section is empty"));
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private record ReadyPhase(String title, List<String> scope) {
    }
}
