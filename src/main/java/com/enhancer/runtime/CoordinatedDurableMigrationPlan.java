package com.enhancer.runtime;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record CoordinatedDurableMigrationPlan(
        Path stoppedOwnerFence,
        byte[] expectedFenceBytes,
        Path manifestRoot,
        List<String> submissionIds,
        Path queueRoot,
        String queueId,
        Path runtimeRoot,
        List<String> goalIds,
        List<Path> workSpoolPoints,
        List<Path> resultSpoolPoints,
        List<Path> ingressSpoolPoints,
        List<Path> bindingPoints) {

    public CoordinatedDurableMigrationPlan {
        stoppedOwnerFence = normalized(stoppedOwnerFence, "stoppedOwnerFence");
        expectedFenceBytes = Objects.requireNonNull(
                expectedFenceBytes, "expectedFenceBytes must not be null").clone();
        if (expectedFenceBytes.length == 0) {
            throw new IllegalArgumentException("expectedFenceBytes must not be empty");
        }
        manifestRoot = normalized(manifestRoot, "manifestRoot");
        queueRoot = normalized(queueRoot, "queueRoot");
        runtimeRoot = normalized(runtimeRoot, "runtimeRoot");
        submissionIds = distinctCopy(submissionIds, "submissionIds");
        goalIds = distinctCopy(goalIds, "goalIds");
        if (submissionIds.isEmpty() || goalIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "submissionIds and goalIds must name a complete closure");
        }
        queueId = SchedulerQueueState.requireCanonicalQueueId(queueId);
        for (String goalId : goalIds) {
            AgentRuntimeState.requireCanonicalGoalId(goalId);
        }
        workSpoolPoints = normalizedDistinctPaths(workSpoolPoints, "workSpoolPoints");
        resultSpoolPoints = normalizedDistinctPaths(resultSpoolPoints, "resultSpoolPoints");
        ingressSpoolPoints = normalizedDistinctPaths(ingressSpoolPoints, "ingressSpoolPoints");
        bindingPoints = normalizedDistinctPaths(bindingPoints, "bindingPoints");
        if (stoppedOwnerFence.startsWith(manifestRoot)
                || stoppedOwnerFence.startsWith(queueRoot)
                || stoppedOwnerFence.startsWith(runtimeRoot)) {
            throw new IllegalArgumentException(
                    "stoppedOwnerFence must be outside the named migration roots");
        }
    }

    @Override
    public byte[] expectedFenceBytes() {
        return expectedFenceBytes.clone();
    }

    private static Path normalized(Path value, String field) {
        return Objects.requireNonNull(value, field + " must not be null")
                .toAbsolutePath()
                .normalize();
    }

    private static <T> List<T> distinctCopy(List<T> values, String field) {
        List<T> copy = List.copyOf(Objects.requireNonNull(
                values, field + " must not be null"));
        if (new HashSet<>(copy).size() != copy.size()) {
            throw new IllegalArgumentException(field + " must not contain duplicates");
        }
        return copy;
    }

    private static List<Path> normalizedDistinctPaths(
            List<Path> values,
            String field) {
        return distinctCopy(Objects.requireNonNull(values, field + " must not be null")
                .stream()
                .map(value -> normalized(value, field + " entry"))
                .toList(), field);
    }
}
