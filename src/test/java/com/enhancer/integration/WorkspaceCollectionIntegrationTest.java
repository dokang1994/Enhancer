package com.enhancer.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.brain.GraphNode;
import com.enhancer.brain.MemoryFreshness;
import com.enhancer.brain.ProjectBrainGraph;
import com.enhancer.brain.ProjectBrainView;
import com.enhancer.brain.RepositoryMemoryEntry;
import com.enhancer.brain.RunEvidenceGraphProducer;
import com.enhancer.brain.TaskImpact;
import com.enhancer.brain.TaskImpactQuery;
import com.enhancer.cli.CliExitCode;
import com.enhancer.cli.EnhancerCli;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.verification.VerificationStatus;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceState;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceCollectionIntegrationTest {
    private static final String TASK_ID = "gate-6-collection-test";

    @TempDir
    Path temporaryRoot;

    @Test
    void collectsARealSnapshotThatExplainsAGovernedRunAndExposesDivergence()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        String targetContent = "workspace-collection-evidence\n".repeat(4);
        writeGovernedProject(projectRoot, activeTask("Collect the Workspace snapshot."),
                targetContent);

        Path evidenceRoot = temporaryRoot.resolve("evidence");
        Path runRecordRoot = temporaryRoot.resolve("run-records");
        Captured run = execute(new String[] {
                "run",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--target-path", "target.txt",
                "--expected-sha256", sha256(targetContent),
                "--evidence-root", evidenceRoot.toString(),
                "--run-record-root", runRecordRoot.toString()
        });
        assertEquals(CliExitCode.COMPLETED.code(), run.exitCode());
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(runRecordRoot)
                .resolve(value(run.stdout(), "runRecordReference"));

        ProjectContext memory = new ProjectContextReader().read(projectRoot);
        ApprovedTask approvedTask = new ApprovedTaskReader().read(memory);
        WorkspaceSnapshot snapshot = new RepositoryMemorySnapshotCollector().collect(
                projectRoot,
                Instant.now(),
                approvedTask,
                memory);

        assertEquals(
                RequiredProjectDocument.values().length,
                snapshot.observations().size());
        assertTrue(snapshot.observations().stream().allMatch(observation ->
                observation.kind() == WorkspaceSourceKind.REPOSITORY_DOCUMENT
                        && observation.state() == WorkspaceSourceState.AVAILABLE
                        && observation.contentSha256().isPresent()));

        ProjectBrainView view = ProjectBrainView.compose(snapshot, memory, resolved.record());
        assertEquals(snapshot.snapshotId(), view.snapshotId());
        assertEquals(TASK_ID, view.approvedTaskRevision().taskId());
        assertEquals(TASK_ID, view.run().taskId());
        assertEquals(VerificationStatus.VERIFIED, view.run().verificationStatus());
        assertTrue(view.repositoryMemory().stream()
                .allMatch(entry -> entry.freshness() == MemoryFreshness.SNAPSHOT_MATCHED));

        Files.writeString(
                projectRoot.resolve("CURRENT_TASK.md"),
                activeTask("Collect the Workspace snapshot after an edit."),
                StandardCharsets.UTF_8);
        ProjectContext changedMemory = new ProjectContextReader().read(projectRoot);
        ProjectBrainView changedView = ProjectBrainView.compose(
                snapshot,
                changedMemory,
                resolved.record());

        List<String> diverged = changedView.repositoryMemory().stream()
                .filter(entry -> entry.freshness() == MemoryFreshness.SNAPSHOT_DIVERGED)
                .map(RepositoryMemoryEntry::path)
                .toList();
        assertEquals(List.of("CURRENT_TASK.md"), diverged);

        ProjectBrainGraph graph = new RunEvidenceGraphProducer().produce(
                snapshot,
                resolved,
                Instant.now());
        assertEquals(snapshot.snapshotId(), graph.sourceSnapshotId());
        assertEquals(
                RequiredProjectDocument.values().length + 2,
                graph.nodes().size());

        TaskImpact impact = new TaskImpactQuery().query(graph, TASK_ID);
        assertEquals(snapshot.snapshotId(), impact.sourceSnapshotId());
        assertEquals(
                List.of(resolved.metadata().reference()),
                impact.executions().stream().map(GraphNode::nodeId).toList());
        assertEquals(List.of(), impact.decisions());
        assertEquals(List.of(), impact.modifiedArtifacts());
        assertTrue(!impact.rebuildRequired());
    }

    private void writeGovernedProject(
            Path projectRoot,
            String currentTask,
            String targetContent) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = document == RequiredProjectDocument.CURRENT_TASK
                    ? currentTask
                    : "# " + document.name() + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
        Files.writeString(
                projectRoot.resolve("target.txt"),
                targetContent,
                StandardCharsets.UTF_8);
    }

    private Captured execute(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String value(String output, String key) {
        return output.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String activeTask(String description) {
        return "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\n" + description + "\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
