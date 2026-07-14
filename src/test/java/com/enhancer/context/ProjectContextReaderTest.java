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
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    "content for " + document.path(),
                    StandardCharsets.UTF_8);
        }

        ProjectContext context = reader.read(projectRoot);

        List<String> expectedPaths = List.of(
                ".ai/constitution.md",
                ".ai/workflow.md",
                ".ai/coding_rules.md",
                ".ai/architecture.md",
                ".ai/prompt_rules.md",
                ".ai/memory.md",
                ".ai/skill_rules.md",
                "CONSTITUTION.md",
                "AGENTS.md",
                "ARCHITECTURE.md",
                "PROJECT_STATE.md",
                "ROADMAP.md",
                "CURRENT_TASK.md",
                "DECISION_LOG.md",
                "SESSION_HANDOFF.md");
        assertEquals(expectedPaths, context.documents().stream().map(ProjectDocument::path).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
                context.documents().stream().map(ProjectDocument::readOrder).toList());
        assertEquals("content for .ai/constitution.md", context.documents().get(0).content());
    }

    @Test
    void reportsTheFirstMissingAiDocument() {
        MissingProjectDocumentException exception = assertThrows(
                MissingProjectDocumentException.class,
                () -> reader.read(projectRoot));

        assertEquals(projectRoot.resolve(".ai/constitution.md"), exception.documentPath());
        assertTrue(exception.getMessage().contains(".ai"));
    }

    @Test
    void reportsTheFirstMissingRootDocumentAfterAiContext() throws IOException {
        for (String pathValue : List.of(
                ".ai/constitution.md",
                ".ai/workflow.md",
                ".ai/coding_rules.md",
                ".ai/architecture.md",
                ".ai/prompt_rules.md",
                ".ai/memory.md",
                ".ai/skill_rules.md")) {
            Path path = projectRoot.resolve(pathValue);
            Files.createDirectories(path.getParent());
            Files.writeString(path, pathValue, StandardCharsets.UTF_8);
        }
        Files.writeString(projectRoot.resolve("CONSTITUTION.md"), "constitution", StandardCharsets.UTF_8);

        MissingProjectDocumentException exception = assertThrows(
                MissingProjectDocumentException.class,
                () -> reader.read(projectRoot));

        assertEquals(projectRoot.resolve("AGENTS.md"), exception.documentPath());
        assertTrue(exception.getMessage().contains("AGENTS.md"));
    }

    @Test
    void readsTheActualEnhancerBootstrapContext() throws IOException {
        Path actualProjectRoot = Path.of(System.getProperty("user.dir"));

        ProjectContext context = reader.read(actualProjectRoot);

        assertEquals(".ai/constitution.md", context.documents().get(0).path());
        assertEquals(15, context.documents().size());
        assertTrue(context.documents().stream()
                .anyMatch(document -> document.path().equals("ROADMAP.md")));
    }
}
