package com.enhancer.runtime;

import com.enhancer.bus.FileSpoolMessageTransport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Prepares and publishes one explicit stopped-owner durable migration closure.
 * Spool and binding points are immutable ordered validation points; only legacy
 * manifest, queue, and AgentRuntime artifacts receive current-schema candidates.
 */
public final class CoordinatedDurableMigrationCutover {
    private static final Hook NO_OP_HOOK = new Hook() {
    };

    private final Hook hook;

    public CoordinatedDurableMigrationCutover() {
        this(NO_OP_HOOK);
    }

    CoordinatedDurableMigrationCutover(Hook hook) {
        this.hook = Objects.requireNonNull(hook, "hook must not be null");
    }

    public Result execute(CoordinatedDurableMigrationPlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        CoordinatedDurableMigrationPreflightResult preflight =
                new CoordinatedDurableMigrationPreflight().inspect(plan);
        if (preflight.status()
                == CoordinatedDurableMigrationPreflightStatus.REFUSED) {
            return Result.refused(
                    preflight.refusalCode().orElseThrow(),
                    preflight.refusalDetail().orElseThrow());
        }
        if (preflight.status()
                == CoordinatedDurableMigrationPreflightStatus.ALREADY_CURRENT) {
            return Result.alreadyCurrent();
        }

        List<Path> candidates = new ArrayList<>();
        try {
            FileSystemSubmissionManifestStore manifestStore =
                    new FileSystemSubmissionManifestStore(plan.manifestRoot());
            List<SubmissionManifestMigrationInspection> manifests =
                    new ArrayList<>();
            for (String submissionId : plan.submissionIds()) {
                manifests.add(manifestStore.inspectForMigration(submissionId)
                        .orElseThrow(() -> new IOException(
                                "named manifest disappeared after preflight")));
            }
            FileSystemSchedulerQueueStore queueStore =
                    new FileSystemSchedulerQueueStore(plan.queueRoot());
            SchedulerQueueMigrationInspection queue =
                    queueStore.inspectForMigration(plan.queueId())
                            .orElseThrow(() -> new IOException(
                                    "named queue disappeared after preflight"));
            FileSystemAgentRuntimeStateStore runtimeStore =
                    new FileSystemAgentRuntimeStateStore(plan.runtimeRoot());
            List<AgentRuntimeMigrationInspection> runtimes = new ArrayList<>();
            for (String goalId : plan.goalIds()) {
                runtimes.add(runtimeStore.inspectForMigration(goalId)
                        .orElseThrow(() -> new IOException(
                                "named AgentRuntime disappeared after preflight")));
            }

            PointSnapshot fence = snapshot(plan.stoppedOwnerFence());
            if (!MessageDigest.isEqual(
                    plan.expectedFenceBytes(), fence.bytes())) {
                throw new IOException("stopped-owner fence changed after preflight");
            }
            List<PointSnapshot> bindings = snapshots(plan.bindingPoints(), false);
            List<PointSnapshot> resultSpool = snapshots(
                    plan.resultSpoolPoints(), true);
            List<PointSnapshot> workSpool = snapshots(
                    plan.workSpoolPoints(), true);
            List<PointSnapshot> ingressSpool = snapshots(
                    plan.ingressSpoolPoints(), true);

            List<RuntimeCandidate> runtimeCandidates = new ArrayList<>();
            for (AgentRuntimeMigrationInspection runtime : runtimes) {
                if (!runtime.alreadyCurrent()) {
                    Path candidate = runtimeStore
                            .prepareCoordinatedMigrationCandidate(runtime);
                    candidates.add(candidate);
                    runtimeCandidates.add(new RuntimeCandidate(runtime, candidate));
                }
            }
            Optional<QueueCandidate> queueCandidate = Optional.empty();
            if (!queue.alreadyCurrent()) {
                Path candidate = queueStore.prepareCoordinatedMigrationCandidate(queue);
                candidates.add(candidate);
                queueCandidate = Optional.of(new QueueCandidate(queue, candidate));
            }
            List<ManifestCandidate> manifestCandidates = new ArrayList<>();
            for (SubmissionManifestMigrationInspection manifest : manifests) {
                if (!manifest.alreadyCurrent()) {
                    Path candidate = manifestStore
                            .prepareCoordinatedMigrationCandidate(manifest);
                    candidates.add(candidate);
                    manifestCandidates.add(new ManifestCandidate(
                            manifest, candidate));
                }
            }

            revalidateFenceAndBindings(plan, fence, bindings);
            hook.afterCandidatesPrepared(List.copyOf(candidates));

            publishImmutablePoints(
                    PublicationPoint.RESULT_SPOOL,
                    resultSpool,
                    plan,
                    fence,
                    bindings);
            publishImmutablePoints(
                    PublicationPoint.WORK_SPOOL,
                    workSpool,
                    plan,
                    fence,
                    bindings);
            for (RuntimeCandidate candidate : runtimeCandidates) {
                Path source = runtimeStore.artifactPath(
                        candidate.inspection().state().goal().goalId());
                hook.beforePublication(PublicationPoint.AGENT_RUNTIME, source);
                revalidateFenceAndBindings(plan, fence, bindings);
                runtimeStore.publishCoordinatedMigrationCandidate(
                        candidate.inspection(), candidate.path());
                hook.afterPublication(PublicationPoint.AGENT_RUNTIME, source);
            }
            if (queueCandidate.isPresent()) {
                QueueCandidate candidate = queueCandidate.orElseThrow();
                Path source = queueStore.artifactPath(
                        candidate.inspection().state().queueId());
                hook.beforePublication(PublicationPoint.SCHEDULER_QUEUE, source);
                revalidateFenceAndBindings(plan, fence, bindings);
                queueStore.publishCoordinatedMigrationCandidate(
                        candidate.inspection(), candidate.path());
                hook.afterPublication(PublicationPoint.SCHEDULER_QUEUE, source);
            }
            for (ManifestCandidate candidate : manifestCandidates) {
                Path source = manifestStore.artifactPath(
                        candidate.inspection().manifest().submissionId());
                hook.beforePublication(
                        PublicationPoint.SUBMISSION_MANIFEST, source);
                revalidateFenceAndBindings(plan, fence, bindings);
                manifestStore.publishCoordinatedMigrationCandidate(
                        candidate.inspection(), candidate.path());
                hook.afterPublication(
                        PublicationPoint.SUBMISSION_MANIFEST, source);
            }
            publishImmutablePoints(
                    PublicationPoint.INGRESS_SPOOL,
                    ingressSpool,
                    plan,
                    fence,
                    bindings);

            revalidateFenceAndBindings(plan, fence, bindings);
            CoordinatedDurableMigrationPreflightResult completed =
                    new CoordinatedDurableMigrationPreflight().inspect(plan);
            if (completed.status()
                    != CoordinatedDurableMigrationPreflightStatus.ALREADY_CURRENT) {
                throw new IOException(
                        "published closure did not resolve as exactly current");
            }
            return Result.migrated();
        } catch (IOException | RuntimeException exception) {
            return Result.refused(
                    CoordinatedDurableMigrationRefusalCode.SOURCE_INVALID,
                    CoordinatedDurableMigrationRefusalDetail.VALID_SOURCE_REQUIRED);
        } finally {
            for (Path candidate : candidates) {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                    // A later explicit cleanup/re-entry increment owns recovery here.
                }
            }
        }
    }

    private void publishImmutablePoints(
            PublicationPoint point,
            List<PointSnapshot> snapshots,
            CoordinatedDurableMigrationPlan plan,
            PointSnapshot fence,
            List<PointSnapshot> bindings) throws IOException {
        for (PointSnapshot snapshot : snapshots) {
            hook.beforePublication(point, snapshot.path());
            revalidateFenceAndBindings(plan, fence, bindings);
            revalidate(snapshot);
            FileSpoolMessageTransport.read(snapshot.path());
            hook.afterPublication(point, snapshot.path());
        }
    }

    private List<PointSnapshot> snapshots(
            List<Path> points,
            boolean decodeSpool) throws IOException {
        List<PointSnapshot> snapshots = new ArrayList<>();
        for (Path point : points) {
            PointSnapshot snapshot = snapshot(point);
            if (decodeSpool) {
                FileSpoolMessageTransport.read(point);
            }
            snapshots.add(snapshot);
        }
        return List.copyOf(snapshots);
    }

    private PointSnapshot snapshot(Path point) throws IOException {
        if (!Files.isRegularFile(point, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("named migration point is not a regular file");
        }
        return new PointSnapshot(point, Files.readAllBytes(point));
    }

    private void revalidateFenceAndBindings(
            CoordinatedDurableMigrationPlan plan,
            PointSnapshot fence,
            List<PointSnapshot> bindings) throws IOException {
        revalidate(fence);
        if (!MessageDigest.isEqual(
                plan.expectedFenceBytes(), fence.bytes())) {
            throw new IOException("stopped-owner fence is invalid");
        }
        for (PointSnapshot binding : bindings) {
            revalidate(binding);
        }
    }

    private void revalidate(PointSnapshot snapshot) throws IOException {
        if (!Files.isRegularFile(snapshot.path(), LinkOption.NOFOLLOW_LINKS)
                || !MessageDigest.isEqual(
                        snapshot.bytes(), Files.readAllBytes(snapshot.path()))) {
            throw new IOException("named migration point changed");
        }
    }

    public enum Status {
        MIGRATED,
        ALREADY_CURRENT,
        REFUSED
    }

    public record Result(
            Status status,
            Optional<CoordinatedDurableMigrationRefusalCode> refusalCode,
            Optional<CoordinatedDurableMigrationRefusalDetail> refusalDetail) {
        public Result {
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(refusalCode, "refusalCode must not be null");
            Objects.requireNonNull(refusalDetail, "refusalDetail must not be null");
        }

        static Result migrated() {
            return new Result(Status.MIGRATED, Optional.empty(), Optional.empty());
        }

        static Result alreadyCurrent() {
            return new Result(
                    Status.ALREADY_CURRENT, Optional.empty(), Optional.empty());
        }

        static Result refused(
                CoordinatedDurableMigrationRefusalCode code,
                CoordinatedDurableMigrationRefusalDetail detail) {
            return new Result(
                    Status.REFUSED, Optional.of(code), Optional.of(detail));
        }
    }

    enum PublicationPoint {
        RESULT_SPOOL,
        WORK_SPOOL,
        AGENT_RUNTIME,
        SCHEDULER_QUEUE,
        SUBMISSION_MANIFEST,
        INGRESS_SPOOL
    }

    interface Hook {
        default void afterCandidatesPrepared(List<Path> candidates)
                throws IOException {
        }

        default void beforePublication(PublicationPoint point, Path source)
                throws IOException {
        }

        default void afterPublication(PublicationPoint point, Path source)
                throws IOException {
        }
    }

    private record PointSnapshot(Path path, byte[] bytes) {
        private PointSnapshot {
            path = Objects.requireNonNull(path, "path must not be null");
            bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    private record RuntimeCandidate(
            AgentRuntimeMigrationInspection inspection,
            Path path) {
    }

    private record QueueCandidate(
            SchedulerQueueMigrationInspection inspection,
            Path path) {
    }

    private record ManifestCandidate(
            SubmissionManifestMigrationInspection inspection,
            Path path) {
    }
}
