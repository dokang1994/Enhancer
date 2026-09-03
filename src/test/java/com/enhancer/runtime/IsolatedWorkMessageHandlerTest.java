package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.enhancer.run.RunRecordStore;
import org.junit.jupiter.api.Test;

class IsolatedWorkMessageHandlerTest {
    @Test
    void typedPayloadKindCannotFallThroughToLegacyExecution() {
        WorkItem workItem = ModelWorkFixtures.workItem();
        AgentLoopAgentRunExecution execution = mock(AgentLoopAgentRunExecution.class);
        RunRecordStore recordStore = mock(RunRecordStore.class);
        IsolatedWorkMessageHandler handler = new IsolatedWorkMessageHandler(
                workItem.workItemId(),
                workItem.requiredCapability(),
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID,
                execution,
                recordStore);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> handler.handle(workItem.workMessage()));

        assertTrue(failure.getMessage().contains("intentionally disconnected"));
        verifyNoInteractions(execution, recordStore);
        assertTrue(handler.acceptedResult().isEmpty());
    }
}
