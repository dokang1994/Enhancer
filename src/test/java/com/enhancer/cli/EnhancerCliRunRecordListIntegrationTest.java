package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliRunRecordListIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void listsTheBoundedRecentPrefixAndReturnedReferenceReplays() throws Exception {
        Path recordRoot = temporaryRoot.resolve("records");
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(recordRoot);
        StoredRunRecord oldest = store.persist(record("logical-run-oldest"));
        StoredRunRecord middle = store.persist(record("logical-run-middle"));
        StoredRunRecord newest = store.persist(record("logical-run-newest"));
        setModified(recordRoot, oldest, 1);
        setModified(recordRoot, middle, 2);
        setModified(recordRoot, newest, 3);
        List<String> expected = store.recentReferences(2);

        Captured listing = execute(new String[] {
                "run-record-list",
                "--run-record-root", recordRoot.toString(),
                "--limit", "2"
        });

        assertEquals(0, listing.exitCode());
        assertEquals("AVAILABLE", value(listing.stdout(), "status"));
        assertEquals("0", value(listing.stdout(), "exitCode"));
        assertEquals("2", value(listing.stdout(), "requestedLimit"));
        assertEquals("2", value(listing.stdout(), "returnedRecords"));
        assertEquals(expected.get(0), value(listing.stdout(), "runRecordReference.1"));
        assertEquals(expected.get(1), value(listing.stdout(), "runRecordReference.2"));
        assertFalse(listing.stdout().contains(oldest.reference()));
        assertTrue(listing.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertEquals("", listing.stderr());

        Captured replay = execute(new String[] {
                "replay",
                "--run-record-root", recordRoot.toString(),
                "--reference", expected.get(0)
        });
        assertEquals(0, replay.exitCode());
        assertEquals("run-record-list-test", value(replay.stdout(), "taskId"));
        assertEquals("VERIFIED", value(replay.stdout(), "verificationStatus"));
    }

    @Test
    void missingRecordRootIsEmptyAndIsNotCreated() {
        Path missingRoot = temporaryRoot.resolve("missing-records");

        Captured listing = execute(new String[] {
                "run-record-list",
                "--run-record-root", missingRoot.toString(),
                "--limit", "4"
        });

        assertEquals(0, listing.exitCode());
        assertEquals("EMPTY", value(listing.stdout(), "status"));
        assertEquals("4", value(listing.stdout(), "requestedLimit"));
        assertEquals("0", value(listing.stdout(), "returnedRecords"));
        assertFalse(Files.exists(missingRoot));
        assertEquals("", listing.stderr());
    }

    @Test
    void maximumListingRemainsBoundedAndDoesNotResolveArtifacts() throws Exception {
        Path recordRoot = temporaryRoot.resolve("bounded-records");
        Files.createDirectories(recordRoot);
        for (int index = 1;
                index <= RunRecordListCliCommand.MAX_REFERENCES;
                index++) {
            String recordId = new UUID(0, index).toString();
            Files.write(
                    recordRoot.resolve(recordId + ".run-record"),
                    new byte[] {(byte) index});
        }

        Captured listing = execute(new String[] {
                "run-record-list",
                "--run-record-root", recordRoot.toString(),
                "--limit", Integer.toString(RunRecordListCliCommand.MAX_REFERENCES)
        });

        assertEquals(0, listing.exitCode());
        assertEquals("AVAILABLE", value(listing.stdout(), "status"));
        assertEquals(
                Integer.toString(RunRecordListCliCommand.MAX_REFERENCES),
                value(listing.stdout(), "returnedRecords"));
        assertTrue(listing.stdout().contains("runRecordReference.48=run-record/"));
        assertTrue(listing.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertEquals("", listing.stderr());
    }

    private void setModified(
            Path recordRoot,
            StoredRunRecord stored,
            long seconds) throws Exception {
        Files.setLastModifiedTime(
                recordRoot.resolve(stored.recordId() + ".run-record"),
                FileTime.from(Instant.parse("2026-07-23T00:00:00Z")
                        .plusSeconds(seconds)));
    }

    private RunRecord record(String logicalRunId) {
        ApprovedTask task = new ApprovedTask(
                "run-record-list-test",
                "List recent RunRecords",
                "Approved by integration test",
                Set.of("read-file"),
                "CURRENT_TASK.md");
        ToolRequest request = new ToolRequest(
                "read-file",
                logicalRunId + "-correlation",
                Map.of("path", "target.txt"));
        VerificationEvidence evidence = VerificationEvidence.capture(
                "read succeeded",
                "content",
                Optional.empty());
        return new RunRecord(
                logicalRunId,
                Instant.parse("2026-07-23T00:00:00Z"),
                task,
                request,
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        evidence),
                evidence.contentSha256(),
                VerificationDecision.verified("complete content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
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
        String prefix = key + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()))
                .findFirst()
                .orElseThrow();
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
