package com.enhancer.workspace;

public record ApprovedTaskRevision(
        String taskId,
        String sourceDocument,
        String sourceSha256) {

    public static final int MAX_TASK_ID_CHARACTERS = 256;
    public static final int MAX_SOURCE_DOCUMENT_CHARACTERS = 1024;

    public ApprovedTaskRevision {
        taskId = WorkspaceContractSupport.bounded(
                taskId,
                "taskId",
                MAX_TASK_ID_CHARACTERS);
        sourceDocument = WorkspaceContractSupport.bounded(
                sourceDocument,
                "sourceDocument",
                MAX_SOURCE_DOCUMENT_CHARACTERS);
        sourceSha256 = WorkspaceContractSupport.sha256(
                sourceSha256,
                "sourceSha256");
    }
}
