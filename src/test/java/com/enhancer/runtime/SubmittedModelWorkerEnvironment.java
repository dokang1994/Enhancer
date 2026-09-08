package com.enhancer.runtime;

import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Test-owned producer-to-existing-worker composition for RFC-0024 integration evidence. */
final class SubmittedModelWorkerEnvironment {
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000e21";
    private static final String OWNER_ID =
            "00000000-0000-0000-0000-000000000e22";
    private static final String PRODUCER = "test-owned-deterministic-model-submission";
    private static final String PROMPT = "submitted typed process integration prompt";
    private static final Clock SUBMISSION_CLOCK = Clock.fixed(
            Instant.parse("2026-09-07T07:00:00Z"), ZoneOffset.UTC);
    private static final Clock WORKER_CLOCK = Clock.fixed(
            Instant.parse("2026-09-07T07:01:00Z"), ZoneOffset.UTC);

    private final Path projectRoot;
    private final Path evidenceRoot;
    private final Path recordRoot;
    private final Path invocationRoot;
    private final Path runtimeRoot;
    private final Path queueRoot;
    private final Path checkpointRoot;
    private final Path effectRoot;
    private final Path eventRoot;
    private final Path publicationRoot;
    private final FileSystemSubmissionManifestStore manifestStore;
    private final FileSystemSchedulerQueueStore queueStore;
    private final FileSystemAgentRuntimeStateStore runtimeStore;
    private final FileSystemPendingFinalizationStore checkpointStore;
    private final FileSystemExternalEffectLedgerStore effectStore;
    private final FileSystemRunRecordStore runRecordStore;
    private final DeterministicFakeModelSchedulerConfiguration configuration;
    private final DeterministicFakeModelSubmissionRequest request;

    private SubmittedModelWorkerEnvironment(
            Path root,
            String profileCapability,
            Set<String> deniedTools) throws IOException {
        this.projectRoot = root.resolve("project");
        this.evidenceRoot = root.resolve("evidence");
        this.recordRoot = root.resolve("records");
        this.invocationRoot = root.resolve("invocations");
        this.runtimeRoot = root.resolve("runtime");
        this.queueRoot = root.resolve("queue");
        this.checkpointRoot = root.resolve("checkpoint");
        this.effectRoot = root.resolve("effects");
        this.eventRoot = root.resolve("runtime-events");
        this.publicationRoot = root.resolve("runtime-event-publications");
        writeGovernedProject();
        Path promptPath = projectRoot.resolve(ModelAttemptTestFixture.TARGET_PATH);
        Files.createDirectories(promptPath.getParent());
        Files.writeString(promptPath, PROMPT, StandardCharsets.UTF_8);

        this.manifestStore = new FileSystemSubmissionManifestStore(
                root.resolve("submissions"));
        this.queueStore = new FileSystemSchedulerQueueStore(queueRoot);
        this.runtimeStore = new FileSystemAgentRuntimeStateStore(runtimeRoot);
        this.checkpointStore = new FileSystemPendingFinalizationStore(
                checkpointRoot);
        this.effectStore = new FileSystemExternalEffectLedgerStore(
                effectRoot);
        this.runRecordStore = new FileSystemRunRecordStore(recordRoot);
        this.configuration = new DeterministicFakeModelSchedulerConfiguration(
                ModelProcessValidationTestFixture.LIMITS.gatewayTimeout(),
                ModelProcessValidationTestFixture.LIMITS.maximumResponseCharacters(),
                ModelProcessValidationTestFixture.MAXIMUM_READ_BYTES,
                ModelProcessValidationTestFixture.TOOL_TIMEOUT,
                deniedTools);
        this.request = new DeterministicFakeModelSubmissionRequest(
                SUBMISSION_ID,
                ModelAttemptTestFixture.TASK_ID,
                PRODUCER,
                ModelAttemptTestFixture.TARGET_PATH,
                ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(PROMPT)),
                profile(profileCapability),
                8,
                SchedulerPriority.NORMAL);
    }

    static SubmittedModelWorkerEnvironment verified(Path root) throws IOException {
        return new SubmittedModelWorkerEnvironment(
                root, "deterministic-echo", Set.of());
    }

    static SubmittedModelWorkerEnvironment capabilityMismatch(Path root)
            throws IOException {
        return new SubmittedModelWorkerEnvironment(
                root, "profile-only-capability", Set.of());
    }

    static SubmittedModelWorkerEnvironment deniedModelInvoke(Path root)
            throws IOException {
        return new SubmittedModelWorkerEnvironment(
                root, "deterministic-echo", Set.of("model-invoke"));
    }

    DurableSubmissionResult submit() throws IOException {
        return new DeterministicFakeModelSubmissionService(
                projectRoot,
                manifestStore,
                queueStore,
                SUBMISSION_CLOCK,
                new ProjectContextReader(),
                new ApprovedTaskReader(),
                new RepositoryMemorySnapshotCollector())
                .submit(request);
    }

    DurableSubmissionManifest manifest() throws IOException {
        return manifestStore.resolve(SUBMISSION_ID);
    }

    DurableSingleWorkerSchedulerQueue queue() throws IOException {
        return DurableSingleWorkerSchedulerQueue.recover(
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID).queueId(),
                queueStore);
    }

    SchedulerQueueState queueState() throws IOException {
        return queueStore.resolve(
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID).queueId());
    }

    DurableAgentRunWorker worker() throws IOException {
        return DurableAgentRunWorker.processIsolatedWithDeterministicFakeModel(
                queue(),
                runtimeStore,
                effectStore,
                checkpointStore,
                projectRoot,
                evidenceRoot,
                recordRoot,
                invocationRoot,
                runRecordStore,
                configuration,
                OWNER_ID,
                WORKER_CLOCK,
                Duration.ofSeconds(30),
                AgentRunRetryPolicy.of(2));
    }

    DurableAgentRunWorker worker(RuntimeEventRecorder eventRecorder) throws IOException {
        return DurableAgentRunWorker.processIsolatedWithDeterministicFakeModel(
                queue(),
                runtimeStore,
                effectStore,
                checkpointStore,
                projectRoot,
                evidenceRoot,
                recordRoot,
                invocationRoot,
                runRecordStore,
                configuration,
                OWNER_ID,
                WORKER_CLOCK,
                Duration.ofSeconds(30),
                AgentRunRetryPolicy.of(2),
                eventRecorder);
    }

    String[] schedulerArguments(String command) {
        List<String> arguments = new ArrayList<>(List.of(
                command,
                "--project-root", projectRoot.toString(),
                "--queue-root", queueRoot.toString(),
                "--queue-id", GeneratedSubmissionIdentities
                        .derive(SUBMISSION_ID).queueId(),
                "--runtime-root", runtimeRoot.toString(),
                "--external-effect-root", effectRoot.toString(),
                "--cycle-checkpoint-root", checkpointRoot.toString(),
                "--evidence-root", evidenceRoot.toString(),
                "--run-record-root", recordRoot.toString(),
                "--invocation-root", invocationRoot.toString(),
                "--owner-id", OWNER_ID,
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000"));
        if (command.equals("scheduler-drain")) {
            arguments.addAll(List.of("--max-cycles", "8"));
        } else if (command.equals("scheduler-service")) {
            arguments.addAll(List.of(
                    "--max-cycles", "8",
                    "--max-consecutive-idle-cycles", "1",
                    "--idle-wait-millis", "1"));
        }
        arguments.addAll(List.of(
                "--model-execution", "deterministic-fake-v2",
                "--model-gateway-timeout-millis", Long.toString(
                        configuration.gatewayTimeout().toMillis()),
                "--model-maximum-response-characters", Integer.toString(
                        configuration.maximumResponseCharacters()),
                "--model-maximum-read-bytes", Long.toString(
                        configuration.maximumReadBytes()),
                "--model-tool-timeout-millis", Long.toString(
                        configuration.toolTimeout().toMillis())));
        for (String deniedTool : configuration.deniedTools()) {
            arguments.add("--model-denied-tool");
            arguments.add(deniedTool);
        }
        return arguments.toArray(String[]::new);
    }

    String[] schedulerArgumentsWithRuntimeEvents(
            String command,
            int maximumPendingPublications) {
        List<String> arguments = new ArrayList<>(List.of(schedulerArguments(command)));
        arguments.addAll(List.of(
                "--runtime-event-root", eventRoot.toString(),
                "--runtime-event-publication-root", publicationRoot.toString(),
                "--max-pending-runtime-event-publications",
                Integer.toString(maximumPendingPublications)));
        return arguments.toArray(String[]::new);
    }

    String soleGoalId() throws IOException {
        try (var files = Files.list(runtimeRoot)) {
            String name = files.map(path -> path.getFileName().toString())
                    .filter(value -> value.endsWith(".agent-runtime"))
                    .findFirst()
                    .orElseThrow();
            return name.substring(0, name.length() - ".agent-runtime".length());
        }
    }

    String workItemId() {
        return DurableWorkItemAdmissionHandler.workItemIdFor(SUBMISSION_ID);
    }

    FileSystemAgentRuntimeStateStore runtimeStore() {
        return runtimeStore;
    }

    FileSystemPendingFinalizationStore checkpointStore() {
        return checkpointStore;
    }

    FileSystemExternalEffectLedgerStore effectStore() {
        return effectStore;
    }

    FileSystemRunRecordStore runRecordStore() {
        return runRecordStore;
    }

    Path eventRoot() {
        return eventRoot;
    }

    Path publicationRoot() {
        return publicationRoot;
    }

    long evidenceFileCount() throws IOException {
        return regularFileCount(evidenceRoot, null);
    }

    long resultPointFileCount() throws IOException {
        return regularFileCount(invocationRoot, IsolatedWorkerMain.RESULT_SPOOL);
    }

    private long regularFileCount(Path root, String requiredDirectory)
            throws IOException {
        if (!Files.exists(root)) {
            return 0L;
        }
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> requiredDirectory == null
                            || hasDirectory(path, requiredDirectory))
                    .count();
        }
    }

    private boolean hasDirectory(Path path, String directory) {
        Path relative = invocationRoot.relativize(path);
        for (Path component : relative) {
            if (component.toString().equals(directory)) {
                return true;
            }
        }
        return false;
    }

    private void writeGovernedProject() throws IOException {
        String currentTask = "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nExecute submitted deterministic model work.\n\n"
                + "## Task ID\n\n" + ModelAttemptTestFixture.TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the RFC-0024 integration fixture.\n\n"
                + "## Allowed Tools\n\n- model-invoke\n";
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

    private ModelExecutionProfile profile(String requiredCapability) {
        ModelTokenBudget tokenBudget = new ModelTokenBudget(20_000, 20_000, 40_000);
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                requiredCapability,
                "deterministic-fake",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.MINIMAL,
                tokenBudget.maxTotalTokens(),
                tokenBudget,
                new ModelCostBudget("USD", 0),
                Duration.ofSeconds(1),
                ModelDataClassification.PUBLIC);
    }
}
