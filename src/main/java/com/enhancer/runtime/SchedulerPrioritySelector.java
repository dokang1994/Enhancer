package com.enhancer.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure admission-ordered priority selector with a bounded expedited burst.
 *
 * <p>The caller supplies ready candidates only. This selector reads no queue or clock,
 * persists no fairness progress, and grants no authority. Its integration consumer is
 * the non-recovery path in {@link SingleWorkerSchedulerQueue#claimNext()}, which owns
 * the durable queue transition.
 */
public final class SchedulerPrioritySelector {
    public static final int MAX_EXPEDITED_BURST = 256;

    private SchedulerPrioritySelector() {
    }

    public static Optional<Selection> select(
            List<Candidate> readyCandidates,
            int consecutiveExpedited,
            int maximumExpeditedBurst) {
        Objects.requireNonNull(
                readyCandidates, "readyCandidates must not be null");
        if (readyCandidates.size() > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "readyCandidates must not exceed "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS
                            + " entries");
        }
        if (maximumExpeditedBurst < 1
                || maximumExpeditedBurst > MAX_EXPEDITED_BURST) {
            throw new IllegalArgumentException(
                    "maximumExpeditedBurst must be between 1 and "
                            + MAX_EXPEDITED_BURST);
        }
        if (consecutiveExpedited < 0
                || consecutiveExpedited > maximumExpeditedBurst) {
            throw new IllegalArgumentException(
                    "consecutiveExpedited must be between 0 and "
                            + "maximumExpeditedBurst");
        }

        Candidate oldestNormal = null;
        Candidate oldestExpedited = null;
        Set<String> identities = new HashSet<>();
        for (Candidate candidate : readyCandidates) {
            Objects.requireNonNull(
                    candidate, "readyCandidates must not contain null");
            if (!identities.add(candidate.workItemId())) {
                throw new IllegalArgumentException(
                        "readyCandidates must not contain duplicate work identities");
            }
            if (candidate.priority() == SchedulerPriority.NORMAL
                    && oldestNormal == null) {
                oldestNormal = candidate;
            } else if (candidate.priority() == SchedulerPriority.EXPEDITED
                    && oldestExpedited == null) {
                oldestExpedited = candidate;
            }
        }

        Candidate selected;
        int nextConsecutiveExpedited;
        if (oldestExpedited != null
                && (oldestNormal == null
                        || consecutiveExpedited < maximumExpeditedBurst)) {
            selected = oldestExpedited;
            nextConsecutiveExpedited = Math.min(
                    maximumExpeditedBurst, consecutiveExpedited + 1);
        } else if (oldestNormal != null) {
            selected = oldestNormal;
            nextConsecutiveExpedited = 0;
        } else {
            return Optional.empty();
        }
        return Optional.of(new Selection(
                selected.workItemId(),
                selected.priority(),
                nextConsecutiveExpedited));
    }

    public record Candidate(String workItemId, SchedulerPriority priority) {
        public Candidate {
            workItemId = RuntimeIdentity.canonicalUuid(
                    workItemId, "workItemId");
            Objects.requireNonNull(priority, "priority must not be null");
        }
    }

    public record Selection(
            String workItemId,
            SchedulerPriority priority,
            int nextConsecutiveExpedited) {
        public Selection {
            workItemId = RuntimeIdentity.canonicalUuid(
                    workItemId, "workItemId");
            Objects.requireNonNull(priority, "priority must not be null");
            if (nextConsecutiveExpedited < 0
                    || nextConsecutiveExpedited > MAX_EXPEDITED_BURST) {
                throw new IllegalArgumentException(
                        "nextConsecutiveExpedited is outside supported bounds");
            }
        }
    }
}
