package com.enhancer.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectContextReaderTest {
    @TempDir
    Path projectRoot;

    private final ProjectContextReader reader = new ProjectContextReader();

    @Test
    void readsRequiredDocumentsInStartupOrder() throws IOException {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Files.writeString(
                    projectRoot.resolve(document.path()),
                    "content for " + document.path(),
                    StandardCharsets.UTF_8);
        }

        ProjectContext context = reader.read(projectRoot);

        List<String> expectedPaths = List.of(
                "CONSTITUTION.md",
                "AGENTS.md",
                "ARCHITECTURE.md",
                "PROJECT_STATE.md",
                "ROADMAP.md",
                "CURRENT_TASK.md",
                "DECISION_LOG.md",
                "SESSION_HANDOFF.md");
        assertEquals(expectedPaths, context.documents().stream().map(ProjectDocument::path).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8),
                context.documents().stream().map(ProjectDocument::readOrder).toList());
        assertEquals("content for CONSTITUTION.md", context.documents().get(0).content());
    }

    @Test
    void reportsTheFirstMissingRequiredDocument() throws IOException {
        Files.writeString(projectRoot.resolve("CONSTITUTION.md"), "constitution", StandardCharsets.UTF_8);

        MissingProjectDocumentException exception = assertThrows(
                MissingProjectDocumentException.class,
                () -> reader.read(projectRoot));

        assertEquals(projectRoot.resolve("AGENTS.md"), exception.documentPath());
        assertTrue(exception.getMessage().contains("AGENTS.md"));
    }
}
