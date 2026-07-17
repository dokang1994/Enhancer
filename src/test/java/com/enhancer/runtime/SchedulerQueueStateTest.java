package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerQueueStateTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000301";
    private static final String FIRST_ID =
            "00000000-0000-0000-0000-000000000311";
    private static final String SECOND_ID =
            "00000000-0000-0000-0000-000000000312";

    @Test
    void partitionsAdmittedWorkAcrossVerifiedAndFailedDispositions() {
        SchedulerQueueState state = new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID, SECOND_ID),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID));

        assertEquals(Set.of(FIRST_ID), state.completedWorkItemIds());
        assertEquals(Set.of(SECOND_ID), state.failedWorkItemIds());
    }

    @Test
    void rejectsWorkItemThatIsBothCompletedAndFailed() {
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(FIRST_ID)));
    }

    @Test
    void rejectsFailedWorkThatWasNeverAdmitted() {
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID)));
    }
}
