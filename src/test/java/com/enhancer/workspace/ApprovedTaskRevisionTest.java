package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ApprovedTaskRevisionTest {
    @Test
    void preservesBoundedTaskAndSourceRevisionIdentity() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-6-workspace-snapshot-contract",
                "CURRENT_TASK.md",
                "a".repeat(64));

        assertEquals("gate-6-workspace-snapshot-contract", revision.taskId());
        assertEquals("CURRENT_TASK.md", revision.sourceDocument());
        assertEquals("a".repeat(64), revision.sourceSha256());
    }

    @Test
    void rejectsBlankOversizedOrMalformedRevisionFields() {
        assertThrows(IllegalArgumentException.class, () -> new ApprovedTaskRevision(
                " ", "CURRENT_TASK.md", "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new ApprovedTaskRevision(
                "task", " ", "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new ApprovedTaskRevision(
                "task", "CURRENT_TASK.md", "NOT-A-DIGEST"));
        assertThrows(IllegalArgumentException.class, () -> new ApprovedTaskRevision(
                "x".repeat(ApprovedTaskRevision.MAX_TASK_ID_CHARACTERS + 1),
                "CURRENT_TASK.md",
                "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new ApprovedTaskRevision(
                "task",
                "x".repeat(ApprovedTaskRevision.MAX_SOURCE_DOCUMENT_CHARACTERS + 1),
                "a".repeat(64)));
    }
}
