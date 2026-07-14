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
    private static final String DELIVERY_GATE_HEADING = "## Delivery Gate ";
    private static final String NEXT_STATUS = "Status: Specified - Next";

    public Optional<TaskProposal> propose(ProjectContext context) {
        Objects.requireNonNull(context, "context must not be null");

        String currentTask = requiredContent(context, CURRENT_TASK);
        String currentStatus = section(currentTask, "Status")
                .orElseThrow(() -> new PlanningException("CURRENT_TASK.md does not define a Status section"));
        if (!firstContentLine(currentStatus).equalsIgnoreCase("Completed")) {
            return Optional.empty();
        }

        NextDeliveryGate gate = firstNextDeliveryGate(requiredContent(context, ROADMAP));
        List<String> risks = new ArrayList<>();
        if (gate.scope().isEmpty()) {
            risks.add("The next delivery gate does not define scope items.");
        }
        if (gate.exitCriteria().isEmpty()) {
            risks.add("The next delivery gate does not define exit criteria.");
        }
        risks.add("Dependencies may require additional validation before proposal acceptance.");

        return Optional.of(new TaskProposal(
                gate.title(),
                "The current task is completed and this is the first delivery gate marked Specified - Next.",
                gate.scope(),
                gate.exitCriteria(),
                List.of(
                        "Items from later delivery gates",
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

    private NextDeliveryGate firstNextDeliveryGate(String roadmap) {
        String normalized = normalize(roadmap);
        String[] lines = normalized.split("\n", -1);

        for (int index = 0; index < lines.length; index++) {
            if (!lines[index].startsWith(DELIVERY_GATE_HEADING)) {
                continue;
            }

            int end = index + 1;
            while (end < lines.length && !lines[end].startsWith("## ")) {
                end++;
            }

            List<String> gateLines = List.of(lines).subList(index + 1, end);
            boolean isNext = gateLines.stream()
                    .anyMatch(line -> line.trim().equalsIgnoreCase(NEXT_STATUS));
            if (isNext) {
                List<String> scope = bulletsAfterLabel(
                        gateLines,
                        List.of("Required capabilities:", "Required contracts:", "Scope:"));
                List<String> exitCriteria = bulletsAfterLabel(
                        gateLines,
                        List.of("Exit criteria:"));
                return new NextDeliveryGate(
                        lines[index].substring(3).trim(),
                        scope,
                        exitCriteria);
            }

            index = end - 1;
        }

        throw new PlanningException(
                "ROADMAP.md does not contain a delivery gate marked Specified - Next");
    }

    private List<String> bulletsAfterLabel(List<String> lines, List<String> labels) {
        int labelIndex = -1;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (labels.stream().anyMatch(label -> line.equalsIgnoreCase(label))) {
                labelIndex = index;
                break;
            }
        }
        if (labelIndex < 0) {
            return List.of();
        }

        List<String> bullets = new ArrayList<>();
        for (int index = labelIndex + 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!line.startsWith("- ")) {
                break;
            }
            String item = line.substring(2).trim();
            if (!item.isEmpty()) {
                bullets.add(item);
            }
        }
        return List.copyOf(bullets);
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

    private record NextDeliveryGate(
            String title,
            List<String> scope,
            List<String> exitCriteria) {
    }
}
