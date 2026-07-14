package com.enhancer.loop;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ApprovedTaskReader {
    private static final String CURRENT_TASK_PATH = "CURRENT_TASK.md";

    public ApprovedTask read(ProjectContext context) {
        Objects.requireNonNull(context, "context must not be null");
        List<ProjectDocument> matches = context.documents().stream()
                .filter(document -> CURRENT_TASK_PATH.equals(document.path()))
                .toList();
        if (matches.size() != 1) {
            throw invalid("context must contain exactly one " + CURRENT_TASK_PATH);
        }

        Map<String, List<String>> sections = sections(matches.get(0).content());
        String status = scalar(sections, "Status");
        if (!"In Progress".equals(status)) {
            throw invalid("CURRENT_TASK.md status must be In Progress");
        }

        String taskId = scalar(sections, "Task ID");
        String description = scalar(sections, "Task");
        String approval = scalar(sections, "Approval");
        Set<String> allowedTools = bullets(sections, "Allowed Tools");
        try {
            return new ApprovedTask(
                    taskId,
                    description,
                    approval,
                    allowedTools,
                    CURRENT_TASK_PATH);
        } catch (IllegalArgumentException exception) {
            throw invalid(exception.getMessage());
        }
    }

    private Map<String, List<String>> sections(String content) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentSection = null;
        for (String line : content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            if (line.startsWith("## ")) {
                currentSection = line.substring(3).trim();
                if (currentSection.isBlank() || sections.putIfAbsent(
                        currentSection,
                        new ArrayList<>()) != null) {
                    throw invalid("CURRENT_TASK.md contains a blank or duplicate section");
                }
            } else if (currentSection != null) {
                sections.get(currentSection).add(line);
            }
        }
        return sections;
    }

    private String scalar(Map<String, List<String>> sections, String name) {
        List<String> values = nonBlankLines(sections, name);
        if (values.size() != 1) {
            throw invalid("CURRENT_TASK.md section " + name + " must contain one value");
        }
        return values.get(0);
    }

    private Set<String> bullets(Map<String, List<String>> sections, String name) {
        List<String> values = nonBlankLines(sections, name);
        Set<String> bullets = new LinkedHashSet<>();
        for (String value : values) {
            if (!value.startsWith("- ") || value.substring(2).isBlank()) {
                throw invalid("CURRENT_TASK.md section " + name + " must contain bullet values");
            }
            bullets.add(stripCode(value.substring(2).trim()));
        }
        if (bullets.isEmpty()) {
            throw invalid("CURRENT_TASK.md section " + name + " must not be empty");
        }
        return Set.copyOf(bullets);
    }

    private List<String> nonBlankLines(Map<String, List<String>> sections, String name) {
        List<String> lines = sections.get(name);
        if (lines == null) {
            throw invalid("CURRENT_TASK.md is missing section " + name);
        }
        return lines.stream().map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private String stripCode(String value) {
        if (value.length() >= 2 && value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private InvalidApprovedTaskException invalid(String message) {
        return new InvalidApprovedTaskException(message);
    }
}
