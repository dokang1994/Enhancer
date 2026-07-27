package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentRunRecordIdentityTest {
    @Test
    void derivesOneStableRunRecordReferenceFromGoalAndAgentRun() {
        String goalId = UUID.randomUUID().toString();
        String agentRunId = UUID.randomUUID().toString();

        String first = AgentRunRecordIdentity.reference(goalId, agentRunId);
        String second = AgentRunRecordIdentity.reference(goalId, agentRunId);

        assertEquals(first, second);
        assertEquals("run-record/" + AgentRunRecordIdentity.recordId(goalId, agentRunId), first);
        assertNotEquals(
                first,
                AgentRunRecordIdentity.reference(goalId, UUID.randomUUID().toString()));
        assertNotEquals(
                first,
                AgentRunRecordIdentity.reference(UUID.randomUUID().toString(), agentRunId));
    }

    @Test
    void rejectsNonCanonicalRuntimeIdentities() {
        String canonical = UUID.randomUUID().toString();

        assertThrows(
                IllegalArgumentException.class,
                () -> AgentRunRecordIdentity.recordId("not-a-uuid", canonical));
        assertThrows(
                IllegalArgumentException.class,
                () -> AgentRunRecordIdentity.recordId(canonical, "NOT-A-UUID"));
    }
}
