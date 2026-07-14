package com.enhancer.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.tool.ToolRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApprovedTaskReaderTest {
    private final ApprovedTaskReader reader = new ApprovedTaskReader();

    @Test
    void readsAnActiveRepositoryApprovalAndItsToolScope() {
        ApprovedTask task = reader.read(context(currentTask(
                "In Progress",
                "task-123",
                "Inspect the governed file.",
                "Approved by the owner for this task.",
                List.of("read-file", "search"))));

        assertEquals("task-123", task.taskId());
        assertEquals("Inspect the governed file.", task.description());
        assertEquals("Approved by the owner for this task.", task.approvalEvidence());
        assertEquals(Set.of("read-file", "search"), task.allowedTools());
        assertEquals("CURRENT_TASK.md", task.sourceDocument());
        assertThrows(UnsupportedOperationException.class, () -> task.allowedTools().add("git"));
    }

    @Test
    void rejectsInactiveMissingOrAmbiguousApprovalDocuments() {
        assertThrows(
                InvalidApprovedTaskException.class,
                () -> reader.read(context(currentTask(
                        "Completed",
                        "task-123",
                        "Inspect.",
                        "Approved.",
                        List.of("read-file")))));
        assertThrows(
                InvalidApprovedTaskException.class,
                () -> reader.read(context(currentTask(
                        "In Progress",
                        "task-123",
                        "Inspect.",
                        " ",
                        List.of("read-file")))));
        assertThrows(
                InvalidApprovedTaskException.class,
                () -> reader.read(new ProjectContext(List.of())));
    }

    @Test
    void rejectsARequestOutsideTheRepositoryApprovedToolScope() {
        ApprovedTask task = reader.read(context(currentTask(
                "In Progress",
                "task-123",
                "Inspect.",
                "Approved.",
                List.of("read-file"))));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AgentRunState.ready(
                        task,
                        new ToolRequest("git-write", "run-1", Map.of())));

        assertTrue(exception.getMessage().contains("approved Tool scope"));
    }

    private ProjectContext context(String currentTask) {
        return new ProjectContext(List.of(
                new ProjectDocument("CURRENT_TASK.md", 1, currentTask)));
    }

    private String currentTask(
            String status,
            String taskId,
            String task,
            String approval,
            List<String> allowedTools) {
        String tools = allowedTools.stream()
                .map(tool -> "- " + tool)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return "# Current Task\n\n"
                + "## Status\n\n" + status + "\n\n"
                + "## Task\n\n" + task + "\n\n"
                + "## Task ID\n\n" + taskId + "\n\n"
                + "## Approval\n\n" + approval + "\n\n"
                + "## Allowed Tools\n\n" + tools + "\n";
    }
}
