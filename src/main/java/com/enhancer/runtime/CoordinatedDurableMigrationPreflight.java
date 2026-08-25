package com.enhancer.runtime;

import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Read-only complete-closure validation under an externally held stopped-owner fence. */
public final class CoordinatedDurableMigrationPreflight {

    public CoordinatedDurableMigrationPreflightResult inspect(
            CoordinatedDurableMigrationPlan plan) {
        try {
            if (!matchesFence(plan)) {
                return refused(
                        CoordinatedDurableMigrationRefusalCode.STOPPED_OWNER_FENCE_INVALID,
                        CoordinatedDurableMigrationRefusalDetail.STOPPED_OWNER_FENCE_REQUIRED);
            }

            FileSystemSubmissionManifestStore manifestStore =
                    new FileSystemSubmissionManifestStore(plan.manifestRoot());
            List<SubmissionManifestMigrationInspection> manifests = new ArrayList<>();
            for (String submissionId : plan.submissionIds()) {
                Optional<SubmissionManifestMigrationInspection> inspected =
                        manifestStore.inspectForMigration(submissionId);
                if (inspected.isEmpty()) {
                    return partial();
                }
                manifests.add(inspected.orElseThrow());
            }

            Optional<SchedulerQueueMigrationInspection> inspectedQueue =
                    new FileSystemSchedulerQueueStore(plan.queueRoot())
                            .inspectForMigration(plan.queueId());
            if (inspectedQueue.isEmpty()) {
                return partial();
            }
            SchedulerQueueMigrationInspection queue = inspectedQueue.orElseThrow();

            FileSystemAgentRuntimeStateStore runtimeStore =
                    new FileSystemAgentRuntimeStateStore(plan.runtimeRoot());
            List<AgentRuntimeMigrationInspection> runtimes = new ArrayList<>();
            for (String goalId : plan.goalIds()) {
                Optional<AgentRuntimeMigrationInspection> inspected =
                        runtimeStore.inspectForMigration(goalId);
                if (inspected.isEmpty()) {
                    return partial();
                }
                runtimes.add(inspected.orElseThrow());
            }

            CoordinatedDurableMigrationPreflightResult closure =
                    validateClosure(manifests, queue, runtimes);
            if (closure.status()
                    == CoordinatedDurableMigrationPreflightStatus.REFUSED) {
                return closure;
            }
            CoordinatedDurableMigrationPreflightResult points =
                    validateNamedPoints(plan, queue.state());
            if (points.status()
                    == CoordinatedDurableMigrationPreflightStatus.REFUSED) {
                return points;
            }
            if (!matchesFence(plan)
                    || sourcesDrifted(
                            plan,
                            manifests,
                            queue,
                            runtimes,
                            manifestStore,
                            runtimeStore)) {
                return refused(
                        CoordinatedDurableMigrationRefusalCode.SOURCE_INVALID,
                        CoordinatedDurableMigrationRefusalDetail.VALID_SOURCE_REQUIRED);
            }
            boolean alreadyCurrent = queue.alreadyCurrent()
                    && manifests.stream().allMatch(
                            SubmissionManifestMigrationInspection::alreadyCurrent)
                    && runtimes.stream().allMatch(
                            AgentRuntimeMigrationInspection::alreadyCurrent);
            return CoordinatedDurableMigrationPreflightResult.ready(alreadyCurrent);
        } catch (IOException | RuntimeException exception) {
            return refused(
                    CoordinatedDurableMigrationRefusalCode.SOURCE_INVALID,
                    CoordinatedDurableMigrationRefusalDetail.VALID_SOURCE_REQUIRED);
        }
    }

    private CoordinatedDurableMigrationPreflightResult validateClosure(
            List<SubmissionManifestMigrationInspection> manifests,
            SchedulerQueueMigrationInspection queue,
            List<AgentRuntimeMigrationInspection> runtimes) {
        Map<String, WorkItem> queuedById = new LinkedHashMap<>();
        for (QueuedWork admitted : queue.state().admittedWork()) {
            queuedById.put(admitted.workItem().workItemId(), admitted.workItem());
        }
        Map<String, WorkItem> manifestedById = new HashMap<>();
        for (SubmissionManifestMigrationInspection inspection : manifests) {
            DurableSubmissionManifest manifest = inspection.manifest();
            if (!manifest.queueId().equals(queue.state().queueId())) {
                return mismatch();
            }
            WorkItem workItem = new WorkItem(
                    DurableWorkItemAdmissionHandler.workItemIdFor(
                            manifest.submissionId()),
                    manifest.requiredCapability(),
                    manifest.workMessage());
            manifestedById.put(workItem.workItemId(), workItem);
        }
        Map<String, WorkItem> runtimeById = new HashMap<>();
        for (AgentRuntimeMigrationInspection inspection : runtimes) {
            WorkItem workItem = inspection.state().goal().workItem();
            runtimeById.put(workItem.workItemId(), workItem);
        }
        if (!queuedById.keySet().equals(manifestedById.keySet())
                || !queuedById.keySet().equals(runtimeById.keySet())) {
            return partial();
        }
        for (Map.Entry<String, WorkItem> entry : queuedById.entrySet()) {
            if (!entry.getValue().equals(manifestedById.get(entry.getKey()))
                    || !entry.getValue().equals(runtimeById.get(entry.getKey()))) {
                return mismatch();
            }
            CoordinatedDurableMigrationPreflightResult classification =
                    classify(entry.getValue());
            if (classification.status()
                    == CoordinatedDurableMigrationPreflightStatus.REFUSED) {
                return classification;
            }
        }
        return CoordinatedDurableMigrationPreflightResult.ready(false);
    }

    private CoordinatedDurableMigrationPreflightResult classify(WorkItem workItem) {
        if (workItem.workMessage().payload() instanceof ModelWorkPayload) {
            return CoordinatedDurableMigrationPreflightResult.ready(false);
        }
        LegacyWorkClassification classification = LegacyWorkClassifier.classify(
                (WorkPayload) workItem.workMessage().payload());
        return switch (classification) {
            case READ_FILE -> CoordinatedDurableMigrationPreflightResult.ready(false);
            case UNPROFILED_MODEL_WORK -> refused(
                    CoordinatedDurableMigrationRefusalCode
                            .UNMIGRATABLE_LEGACY_MODEL_WORK,
                    CoordinatedDurableMigrationRefusalDetail.PROFILE_REQUIRED);
            case INVALID -> refused(
                    CoordinatedDurableMigrationRefusalCode.INVALID_LEGACY_WORK,
                    CoordinatedDurableMigrationRefusalDetail.EXECUTABLE_TOOL_REQUIRED);
        };
    }

    private CoordinatedDurableMigrationPreflightResult validateNamedPoints(
            CoordinatedDurableMigrationPlan plan,
            SchedulerQueueState queue) throws IOException {
        Set<?> workMessages = queue.admittedWork().stream()
                .map(work -> work.workItem().workMessage())
                .collect(java.util.stream.Collectors.toSet());
        for (Path point : concat(plan.workSpoolPoints(), plan.ingressSpoolPoints())) {
            TransportMessage message = readPoint(point);
            if (!(message.envelope().payload() instanceof WorkPayload)
                    && !(message.envelope().payload() instanceof ModelWorkPayload)
                    || !workMessages.contains(message.envelope())) {
                return mismatch();
            }
        }
        for (Path point : plan.resultSpoolPoints()) {
            if (!(readPoint(point).envelope().payload() instanceof ResultPayload)) {
                return mismatch();
            }
        }
        for (Path point : plan.bindingPoints()) {
            if (!Files.isRegularFile(point, LinkOption.NOFOLLOW_LINKS)) {
                return partial();
            }
            Files.readAllBytes(point);
        }
        return CoordinatedDurableMigrationPreflightResult.ready(false);
    }

    private TransportMessage readPoint(Path point) throws IOException {
        if (!Files.isRegularFile(point, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("named migration point is missing");
        }
        return FileSpoolMessageTransport.read(point);
    }

    private boolean sourcesDrifted(
            CoordinatedDurableMigrationPlan plan,
            List<SubmissionManifestMigrationInspection> manifests,
            SchedulerQueueMigrationInspection queue,
            List<AgentRuntimeMigrationInspection> runtimes,
            FileSystemSubmissionManifestStore manifestStore,
            FileSystemAgentRuntimeStateStore runtimeStore) throws IOException {
        for (int index = 0; index < manifests.size(); index++) {
            byte[] current = manifestStore.inspectForMigration(
                            plan.submissionIds().get(index))
                    .orElseThrow()
                    .sourceBytes();
            if (!MessageDigest.isEqual(manifests.get(index).sourceBytes(), current)) {
                return true;
            }
        }
        byte[] currentQueue = new FileSystemSchedulerQueueStore(plan.queueRoot())
                .inspectForMigration(plan.queueId()).orElseThrow().sourceBytes();
        if (!MessageDigest.isEqual(queue.sourceBytes(), currentQueue)) {
            return true;
        }
        for (int index = 0; index < runtimes.size(); index++) {
            byte[] current = runtimeStore.inspectForMigration(plan.goalIds().get(index))
                    .orElseThrow().sourceBytes();
            if (!MessageDigest.isEqual(runtimes.get(index).sourceBytes(), current)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesFence(CoordinatedDurableMigrationPlan plan)
            throws IOException {
        return Files.isRegularFile(plan.stoppedOwnerFence(), LinkOption.NOFOLLOW_LINKS)
                && MessageDigest.isEqual(
                        plan.expectedFenceBytes(),
                        Files.readAllBytes(plan.stoppedOwnerFence()));
    }

    private List<Path> concat(List<Path> first, List<Path> second) {
        List<Path> combined = new ArrayList<>(first);
        combined.addAll(second);
        return List.copyOf(combined);
    }

    private CoordinatedDurableMigrationPreflightResult partial() {
        return refused(
                CoordinatedDurableMigrationRefusalCode.PARTIAL_CLOSURE,
                CoordinatedDurableMigrationRefusalDetail.COMPLETE_CLOSURE_REQUIRED);
    }

    private CoordinatedDurableMigrationPreflightResult mismatch() {
        return refused(
                CoordinatedDurableMigrationRefusalCode.CROSS_STORE_MISMATCH,
                CoordinatedDurableMigrationRefusalDetail.EXACT_BINDING_REQUIRED);
    }

    private CoordinatedDurableMigrationPreflightResult refused(
            CoordinatedDurableMigrationRefusalCode code,
            CoordinatedDurableMigrationRefusalDetail detail) {
        return CoordinatedDurableMigrationPreflightResult.refused(code, detail);
    }
}
