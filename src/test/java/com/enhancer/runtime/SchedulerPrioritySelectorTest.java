package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulerPrioritySelectorTest {
    private static final String FIRST =
            "00000000-0000-0000-0000-000000001001";
    private static final String SECOND =
            "00000000-0000-0000-0000-000000001002";
    private static final String THIRD =
            "00000000-0000-0000-0000-000000001003";

    @Test
    void expeditedPrecedesAnOlderNormalCandidateBelowTheBurst() {
        SchedulerPrioritySelector.Selection selected = select(
                List.of(
                        candidate(FIRST, SchedulerPriority.NORMAL),
                        candidate(SECOND, SchedulerPriority.EXPEDITED)),
                0,
                2);

        assertEquals(SECOND, selected.workItemId());
        assertEquals(SchedulerPriority.EXPEDITED, selected.priority());
        assertEquals(1, selected.nextConsecutiveExpedited());
    }

    @Test
    void exhaustedBurstForcesTheOldestReadyNormalCandidate() {
        SchedulerPrioritySelector.Selection selected = select(
                List.of(
                        candidate(FIRST, SchedulerPriority.EXPEDITED),
                        candidate(SECOND, SchedulerPriority.NORMAL),
                        candidate(THIRD, SchedulerPriority.NORMAL)),
                2,
                2);

        assertEquals(SECOND, selected.workItemId());
        assertEquals(SchedulerPriority.NORMAL, selected.priority());
        assertEquals(0, selected.nextConsecutiveExpedited());
    }

    @Test
    void oldestReadyCandidateWinsWithinEachPriorityClass() {
        SchedulerPrioritySelector.Selection expedited = select(
                List.of(
                        candidate(FIRST, SchedulerPriority.NORMAL),
                        candidate(SECOND, SchedulerPriority.EXPEDITED),
                        candidate(THIRD, SchedulerPriority.EXPEDITED)),
                0,
                3);
        SchedulerPrioritySelector.Selection normal = select(
                List.of(
                        candidate(FIRST, SchedulerPriority.EXPEDITED),
                        candidate(SECOND, SchedulerPriority.NORMAL),
                        candidate(THIRD, SchedulerPriority.NORMAL)),
                1,
                1);

        assertEquals(SECOND, expedited.workItemId());
        assertEquals(SECOND, normal.workItemId());
    }

    @Test
    void onlyExpeditedWorkRemainsSelectableAndCapsProgressAtTheBurst() {
        SchedulerPrioritySelector.Selection selected = select(
                List.of(
                        candidate(FIRST, SchedulerPriority.EXPEDITED),
                        candidate(SECOND, SchedulerPriority.EXPEDITED)),
                4,
                4);

        assertEquals(FIRST, selected.workItemId());
        assertEquals(4, selected.nextConsecutiveExpedited());
    }

    @Test
    void selectingNormalWorkResetsFairnessProgress() {
        SchedulerPrioritySelector.Selection selected = select(
                List.of(candidate(FIRST, SchedulerPriority.NORMAL)),
                3,
                4);

        assertEquals(FIRST, selected.workItemId());
        assertEquals(0, selected.nextConsecutiveExpedited());
    }

    @Test
    void emptyReadyCandidatesReturnEmpty() {
        assertTrue(SchedulerPrioritySelector.select(List.of(), 0, 4).isEmpty());
    }

    @Test
    void rejectsInvalidBoundsIdentitiesAndDuplicateCandidates() {
        assertThrows(IllegalArgumentException.class, () ->
                SchedulerPrioritySelector.select(
                        List.of(candidate(FIRST, SchedulerPriority.NORMAL)),
                        0,
                        0));
        assertThrows(IllegalArgumentException.class, () ->
                SchedulerPrioritySelector.select(
                        List.of(candidate(FIRST, SchedulerPriority.NORMAL)),
                        -1,
                        2));
        assertThrows(IllegalArgumentException.class, () ->
                SchedulerPrioritySelector.select(
                        List.of(candidate(FIRST, SchedulerPriority.NORMAL)),
                        3,
                        2));
        assertThrows(IllegalArgumentException.class, () ->
                SchedulerPrioritySelector.select(
                        List.of(
                                candidate(FIRST, SchedulerPriority.NORMAL),
                                candidate(FIRST, SchedulerPriority.EXPEDITED)),
                        0,
                        2));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerPrioritySelector.Candidate(
                        "not-a-uuid", SchedulerPriority.NORMAL));
        assertThrows(NullPointerException.class, () ->
                new SchedulerPrioritySelector.Candidate(FIRST, null));
    }

    private SchedulerPrioritySelector.Selection select(
            List<SchedulerPrioritySelector.Candidate> candidates,
            int consecutiveExpedited,
            int maximumExpeditedBurst) {
        return SchedulerPrioritySelector.select(
                        candidates,
                        consecutiveExpedited,
                        maximumExpeditedBurst)
                .orElseThrow();
    }

    private SchedulerPrioritySelector.Candidate candidate(
            String workItemId,
            SchedulerPriority priority) {
        return new SchedulerPrioritySelector.Candidate(workItemId, priority);
    }
}
