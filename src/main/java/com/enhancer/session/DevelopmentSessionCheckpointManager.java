package com.enhancer.session;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Application boundary for starting, advancing, inspecting, and retiring one checkpoint. */
public final class DevelopmentSessionCheckpointManager {
    private static final String ACTIVE_STATUS = "In Progress";

    private final Path projectRoot;
    private final DevelopmentTaskContractReader taskReader;
    private final DevelopmentSessionArtifactCollector artifactCollector;
    private final FileSystemDevelopmentSessionCheckpointStore store;
    private final Supplier<String> runIdentity;

    public DevelopmentSessionCheckpointManager(Path projectRoot) throws IOException {
        this(projectRoot, () -> UUID.randomUUID().toString());
    }

    DevelopmentSessionCheckpointManager(
            Path projectRoot,
            Supplier<String> runIdentity) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Path realRoot = projectRoot.toRealPath();
        this.projectRoot = realRoot;
        this.taskReader = new DevelopmentTaskContractReader();
        this.artifactCollector = new DevelopmentSessionArtifactCollector(realRoot);
        this.store = new FileSystemDevelopmentSessionCheckpointStore(
                realRoot.resolve(".enhancer/session-checkpoint"));
        this.runIdentity = Objects.requireNonNull(
                runIdentity, "runIdentity must not be null");
    }

    public DevelopmentSessionCheckpoint start(
            String currentStep,
            String nextAction,
            List<String> artifactPaths) throws IOException {
        if (store.find().isPresent()) {
            throw conflict("an active development-session checkpoint already exists");
        }
        DevelopmentTaskContract task = taskReader.read(projectRoot);
        if (!ACTIVE_STATUS.equals(task.status())) {
            throw conflict("CURRENT_TASK.md must be In Progress to start a checkpoint");
        }
        DevelopmentSessionCheckpoint checkpoint = new DevelopmentSessionCheckpoint(
                runIdentity.get(),
                task.taskId(),
                task.sha256(),
                1,
                DevelopmentSessionCheckpointState.STEP_PENDING,
                currentStep,
                Optional.empty(),
                nextAction,
                List.of(),
                artifactCollector.capture(artifactPaths));
        store.create(checkpoint);
        return checkpoint;
    }

    public DevelopmentSessionCheckpoint record(
            String runId,
            long expectedRevision,
            DevelopmentSessionCheckpointState state,
            String currentStep,
            String nextAction,
            List<String> evidenceReferences,
            List<String> artifactPaths) throws IOException {
        DevelopmentSessionCheckpoint current = requiredCheckpoint();
        requireWriter(current, runId, expectedRevision);
        DevelopmentTaskContract task = taskReader.read(projectRoot);
        requireTaskContract(current, task);
        Optional<String> lastSuccessful =
                state == DevelopmentSessionCheckpointState.STEP_SUCCEEDED
                        ? Optional.of(currentStep)
                        : current.lastSuccessfulStep();
        DevelopmentSessionCheckpoint updated = new DevelopmentSessionCheckpoint(
                current.runId(),
                current.taskId(),
                current.taskContractSha256(),
                current.revision() + 1,
                state,
                currentStep,
                lastSuccessful,
                nextAction,
                evidenceReferences,
                artifactCollector.capture(artifactPaths));
        store.update(runId, expectedRevision, updated);
        return updated;
    }

    public Optional<DevelopmentSessionCheckpointInspection> inspect() throws IOException {
        Optional<DevelopmentSessionCheckpoint> found = store.find();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        DevelopmentSessionCheckpoint checkpoint = found.orElseThrow();
        DevelopmentTaskContract task = taskReader.read(projectRoot);
        boolean taskMatches = checkpoint.taskId().equals(task.taskId())
                && checkpoint.taskContractSha256().equals(task.sha256());
        List<String> paths = checkpoint.artifacts().stream()
                .map(DevelopmentSessionArtifact::path)
                .toList();
        List<DevelopmentSessionArtifact> currentArtifacts = artifactCollector.capture(paths);
        List<String> mismatches = new ArrayList<>();
        for (int index = 0; index < checkpoint.artifacts().size(); index++) {
            DevelopmentSessionArtifact recorded = checkpoint.artifacts().get(index);
            DevelopmentSessionArtifact current = currentArtifacts.get(index);
            if (!recorded.equals(current)) {
                mismatches.add(recorded.path());
            }
        }
        return Optional.of(new DevelopmentSessionCheckpointInspection(
                checkpoint,
                taskMatches,
                mismatches));
    }

    public void clear(String runId, long expectedRevision) throws IOException {
        DevelopmentSessionCheckpoint current = requiredCheckpoint();
        requireWriter(current, runId, expectedRevision);
        if (current.state() != DevelopmentSessionCheckpointState.STABLE) {
            throw conflict("checkpoint must be stable before it can be cleared");
        }
        DevelopmentSessionCheckpointInspection inspection = inspect().orElseThrow();
        if (!inspection.taskContractMatches()) {
            throw conflict("active task contract changed after checkpointing");
        }
        if (!inspection.artifactMismatches().isEmpty()) {
            throw conflict("recorded artifacts changed after the stable checkpoint");
        }
        store.clear(runId, expectedRevision);
    }

    private DevelopmentSessionCheckpoint requiredCheckpoint() throws IOException {
        return store.find().orElseThrow(() ->
                conflict("no active development-session checkpoint exists"));
    }

    private void requireWriter(
            DevelopmentSessionCheckpoint checkpoint,
            String runId,
            long expectedRevision)
            throws DevelopmentSessionCheckpointConflictException {
        if (!checkpoint.runId().equals(runId)) {
            throw conflict("checkpoint belongs to a different run");
        }
        if (checkpoint.revision() != expectedRevision) {
            throw conflict("checkpoint revision is stale");
        }
    }

    private void requireTaskContract(
            DevelopmentSessionCheckpoint checkpoint,
            DevelopmentTaskContract task)
            throws DevelopmentSessionCheckpointConflictException {
        if (!checkpoint.taskId().equals(task.taskId())
                || !checkpoint.taskContractSha256().equals(task.sha256())) {
            throw conflict("active task contract changed after checkpointing");
        }
    }

    private DevelopmentSessionCheckpointConflictException conflict(String message) {
        return new DevelopmentSessionCheckpointConflictException(message);
    }
}
