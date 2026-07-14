package com.enhancer.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.EvidenceRecorder;
import com.enhancer.tool.EvidenceRetentionPolicy;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ReadFileTool;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentRunControllerIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void readsRepositoryApprovalAndTransitionsARealToolResult() throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        writeProject(projectRoot);
        ProjectContext context = new ProjectContextReader().read(projectRoot);
        ApprovedTask approvedTask = new ApprovedTaskReader().read(context);

        FileSystemEvidenceStore store = new FileSystemEvidenceStore(
                temporaryRoot.resolve("evidence"),
                new EvidenceRetentionPolicy(
                        EvidenceRetentionPolicy.MAX_SUPPORTED_CONTENT_BYTES,
                        Duration.ofDays(30)));
        String runId = store.createRun();
        ToolRequest request = new ToolRequest(
                ReadFileTool.NAME,
                runId,
                Map.of(ReadFileTool.PATH_ARGUMENT, "target.txt"));
        ExecutionPolicy policy = new ExecutionPolicy(
                projectRoot,
                Set.of(ReadFileTool.NAME),
                Set.of(),
                EvidenceRetentionPolicy.MAX_SUPPORTED_CONTENT_BYTES,
                Duration.ofSeconds(2),
                CancellationToken.none());

        try (ToolExecutor executor = new ToolExecutor(List.of(
                new ReadFileTool(new EvidenceRecorder(store))))) {
            AgentRunController controller = new AgentRunController(
                    executor,
                    policy,
                    ToolFailureClassifier.standard());

            AgentRunResult run = controller.run(
                    AgentRunState.ready(approvedTask, request),
                    new AgentLoop(5, 3));

            ToolResult result = run.state().lastResult().orElseThrow();
            assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION, run.stopReason());
            assertEquals(ToolResultStatus.SUCCESS, result.status());
            assertEquals("Read file successfully", result.evidence().summary());

            String expected = Files.readString(
                    projectRoot.resolve("target.txt"),
                    StandardCharsets.UTF_8);
            if (result.evidence().truncated()) {
                assertEquals(
                        expected,
                        store.resolve(result.evidence().fullOutputReference().orElseThrow()).content());
            } else {
                assertEquals(expected, result.evidence().outputTail());
            }
        }
    }

    private void writeProject(Path projectRoot) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = document == RequiredProjectDocument.CURRENT_TASK
                    ? "# Current Task\n\n"
                            + "## Status\n\nIn Progress\n\n"
                            + "## Task\n\nRead the governed target.\n\n"
                            + "## Task ID\n\nintegration-read-1\n\n"
                            + "## Approval\n\nApproved by the integration-test owner.\n\n"
                            + "## Allowed Tools\n\n- read-file\n"
                    : "# " + document.name() + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
        Files.writeString(
                projectRoot.resolve("target.txt"),
                "governed-content\n".repeat(600),
                StandardCharsets.UTF_8);
    }
}
