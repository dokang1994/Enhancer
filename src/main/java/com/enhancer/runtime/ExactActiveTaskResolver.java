package com.enhancer.runtime;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.ProjectDocument;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Resolves typed ModelWork only when its retained task identity matches repository authority. */
public final class ExactActiveTaskResolver {
    private final ProjectContextReader contextReader;
    private final ApprovedTaskReader taskReader;

    public ExactActiveTaskResolver(
            ProjectContextReader contextReader,
            ApprovedTaskReader taskReader) {
        this.contextReader = Objects.requireNonNull(
                contextReader, "contextReader must not be null");
        this.taskReader = Objects.requireNonNull(
                taskReader, "taskReader must not be null");
    }

    public ApprovedTask resolve(Path projectRoot, WorkItem workItem) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(workItem, "workItem must not be null");
        if (!workItem.isModelWork()) {
            throw mismatch(
                    ActiveTaskMismatchException.Reason.NOT_MODEL_WORK,
                    "exact active-task resolution requires typed ModelWork");
        }

        ProjectContext context = contextReader.read(projectRoot);
        ApprovedTask activeTask = taskReader.read(context);
        ApprovedTaskRevision retainedRevision = workItem.taskRevision();

        if (!activeTask.taskId().equals(retainedRevision.taskId())) {
            throw mismatch(
                    ActiveTaskMismatchException.Reason.TASK_ID,
                    "retained task ID does not match the active repository task");
        }
        if (!activeTask.sourceDocument().equals(retainedRevision.sourceDocument())) {
            throw mismatch(
                    ActiveTaskMismatchException.Reason.SOURCE_DOCUMENT,
                    "retained task source does not match the active repository task source");
        }

        List<ProjectDocument> sourceDocuments = context.documents().stream()
                .filter(document -> activeTask.sourceDocument().equals(document.path()))
                .toList();
        if (sourceDocuments.size() != 1) {
            throw mismatch(
                    ActiveTaskMismatchException.Reason.SOURCE_DOCUMENT,
                    "project context must contain exactly one active task source document");
        }
        String sourceSha256 = sha256(sourceDocuments.get(0).content());
        if (!sourceSha256.equals(retainedRevision.sourceSha256())) {
            throw mismatch(
                    ActiveTaskMismatchException.Reason.SOURCE_SHA256,
                    "retained task source digest does not match current repository content");
        }
        if (!activeTask.allowedTools().equals(workItem.allowedTools())) {
            throw mismatch(
                    ActiveTaskMismatchException.Reason.ALLOWED_TOOLS,
                    "retained Tool scope does not match the active repository task");
        }

        return activeTask;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private ActiveTaskMismatchException mismatch(
            ActiveTaskMismatchException.Reason reason,
            String message) {
        return new ActiveTaskMismatchException(reason, message);
    }
}
