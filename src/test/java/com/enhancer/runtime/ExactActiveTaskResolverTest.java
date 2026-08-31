package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.context.MissingProjectDocumentException;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.ProjectDocument;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.loop.InvalidApprovedTaskException;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExactActiveTaskResolverTest {
    private static final String TASK_ID = "exact-active-task";
    private static final Set<String> ALLOWED_TOOLS = Set.of("model-invoke", "read-file");
    private static final String MESSAGE_ID = "00000000-0000-0000-0000-000000000e01";
    private static final String WORK_ITEM_ID = "00000000-0000-0000-0000-000000000e02";

    @TempDir
    Path projectRoot;

    @Test
    void resolvesTheExactApprovedTaskFromOneDecodedProjectContext() throws IOException {
        String content = currentTask("In Progress")
                + "\r\n## Notes\r\n\r\nUnicode proves decoded content: 한글  \r\n";
        writeRequiredDocuments(content);
        ProjectContextReader contextReader = mock(ProjectContextReader.class);
        ApprovedTaskReader taskReader = mock(ApprovedTaskReader.class);
        ProjectContext context = new ProjectContext(Set.of(
                new ProjectDocument("CURRENT_TASK.md", 13, content)).stream().toList());
        ApprovedTask approvedTask = new ApprovedTask(
                TASK_ID,
                "Resolve the exact active task.",
                "Approved for the active task.",
                ALLOWED_TOOLS,
                "CURRENT_TASK.md");
        when(contextReader.read(projectRoot)).thenReturn(context);
        when(taskReader.read(context)).thenReturn(approvedTask);
        ExactActiveTaskResolver resolver = new ExactActiveTaskResolver(contextReader, taskReader);

        ApprovedTask resolved = resolver.resolve(
                projectRoot,
                modelWork(TASK_ID, "CURRENT_TASK.md", sha256(content), ALLOWED_TOOLS));

        assertSame(approvedTask, resolved);
        verify(contextReader).read(projectRoot);
        verify(taskReader).read(context);
    }

    @Test
    void rereadsAuthorityAndHashesTheFullDecodedDocumentOnEveryResolution() throws IOException {
        String original = currentTask("In Progress") + "\n## Notes\n\nfirst\n";
        writeRequiredDocuments(original);
        ExactActiveTaskResolver resolver = realResolver();

        ApprovedTask first = resolver.resolve(
                projectRoot,
                modelWork(TASK_ID, "CURRENT_TASK.md", sha256(original), ALLOWED_TOOLS));

        String changed = original.replace("first", "second with trailing spaces  ");
        Files.writeString(
                projectRoot.resolve("CURRENT_TASK.md"),
                changed,
                StandardCharsets.UTF_8);
        ActiveTaskMismatchException stale = assertThrows(
                ActiveTaskMismatchException.class,
                () -> resolver.resolve(
                        projectRoot,
                        modelWork(TASK_ID, "CURRENT_TASK.md", sha256(original), ALLOWED_TOOLS)));
        ApprovedTask refreshed = resolver.resolve(
                projectRoot,
                modelWork(TASK_ID, "CURRENT_TASK.md", sha256(changed), ALLOWED_TOOLS));

        assertEquals(ActiveTaskMismatchException.Reason.SOURCE_SHA256, stale.reason());
        assertEquals(first, refreshed);
    }

    @Test
    void rejectsEachRetainedIdentityOrScopeMismatchWithAStableReason() throws IOException {
        String content = currentTask("In Progress");
        writeRequiredDocuments(content);
        ExactActiveTaskResolver resolver = realResolver();

        assertReason(
                ActiveTaskMismatchException.Reason.TASK_ID,
                () -> resolver.resolve(projectRoot, modelWork(
                        "different-task", "CURRENT_TASK.md", sha256(content), ALLOWED_TOOLS)));
        assertReason(
                ActiveTaskMismatchException.Reason.SOURCE_DOCUMENT,
                () -> resolver.resolve(projectRoot, modelWork(
                        TASK_ID, "ROADMAP.md", sha256(content), ALLOWED_TOOLS)));
        assertReason(
                ActiveTaskMismatchException.Reason.SOURCE_SHA256,
                () -> resolver.resolve(projectRoot, modelWork(
                        TASK_ID, "CURRENT_TASK.md", "a".repeat(64), ALLOWED_TOOLS)));
        assertReason(
                ActiveTaskMismatchException.Reason.ALLOWED_TOOLS,
                () -> resolver.resolve(projectRoot, modelWork(
                        TASK_ID,
                        "CURRENT_TASK.md",
                        sha256(content),
                        Set.of("model-invoke", "search"))));
    }

    @Test
    void rejectsLegacyWorkBeforeReadingTheProject() {
        WorkPayload payload = new WorkPayload(
                new ApprovedTaskRevision(TASK_ID, "CURRENT_TASK.md", "a".repeat(64)),
                "b".repeat(64),
                Set.of("read-file"));
        WorkItem legacy = new WorkItem(
                WORK_ITEM_ID,
                "legacy-capability",
                envelope(payload));

        assertReason(
                ActiveTaskMismatchException.Reason.NOT_MODEL_WORK,
                () -> realResolver().resolve(projectRoot.resolve("missing"), legacy));
    }

    @Test
    void preservesReaderFailuresInsteadOfReclassifyingThem() throws IOException {
        String inactive = currentTask("Completed");
        writeRequiredDocuments(inactive);

        assertThrows(
                InvalidApprovedTaskException.class,
                () -> realResolver().resolve(
                        projectRoot,
                        modelWork(TASK_ID, "CURRENT_TASK.md", sha256(inactive), ALLOWED_TOOLS)));

        Path missingRoot = projectRoot.resolve("missing-project");
        Files.createDirectory(missingRoot);
        assertThrows(
                MissingProjectDocumentException.class,
                () -> realResolver().resolve(
                        missingRoot,
                        modelWork(TASK_ID, "CURRENT_TASK.md", sha256(inactive), ALLOWED_TOOLS)));
    }

    private ExactActiveTaskResolver realResolver() {
        return new ExactActiveTaskResolver(new ProjectContextReader(), new ApprovedTaskReader());
    }

    private WorkItem modelWork(
            String taskId,
            String sourceDocument,
            String sourceSha256,
            Set<String> allowedTools) {
        ModelWorkPayload payload = new ModelWorkPayload(
                new ApprovedTaskRevision(taskId, sourceDocument, sourceSha256),
                "b".repeat(64),
                allowedTools,
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/model-prompt.md",
                        "c".repeat(64),
                        ModelWorkFixtures.profile()));
        return new WorkItem(
                WORK_ITEM_ID,
                ModelWorkFixtures.INDEPENDENT_CAPABILITY,
                envelope(payload));
    }

    private MessageEnvelope envelope(com.enhancer.bus.MessagePayload payload) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "exact-task-correlation",
                Optional.empty(),
                "exact-task-logical-run",
                "exact-task-test",
                Instant.parse("2026-08-31T01:02:03.004000005Z"),
                payload);
    }

    private void writeRequiredDocuments(String currentTask) throws IOException {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? currentTask
                            : "content for " + document.path(),
                    StandardCharsets.UTF_8);
        }
    }

    private String currentTask(String status) {
        return "# Current Task\n\n"
                + "## Status\n\n" + status + "\n\n"
                + "## Task\n\nResolve the exact active task.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved for the active task.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n- read-file\n";
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private void assertReason(
            ActiveTaskMismatchException.Reason reason,
            ThrowingOperation operation) {
        ActiveTaskMismatchException exception = assertThrows(
                ActiveTaskMismatchException.class,
                operation::run);
        assertEquals(reason, exception.reason());
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws IOException;
    }
}
