package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentRunEvidenceIdentityTest {
    @Test
    void preservesTheVersionedDerivationVector() {
        assertEquals(
                "6b12fa0b-d5e6-84be-9038-a3e0d4cd086a",
                AgentRunEvidenceIdentity.runId(
                        "00000000-0000-0000-0000-000000000001",
                        "00000000-0000-0000-0000-000000000002"));
    }

    @Test
    void derivesOneStableCanonicalDomainSeparatedIdentityPerAgentRun() {
        String goalId = UUID.randomUUID().toString();
        String agentRunId = UUID.randomUUID().toString();

        String first = AgentRunEvidenceIdentity.runId(goalId, agentRunId);
        String second = AgentRunEvidenceIdentity.runId(goalId, agentRunId);

        assertEquals(first, second);
        assertEquals(UUID.fromString(first).toString(), first);
        assertNotEquals(AgentRunRecordIdentity.recordId(goalId, agentRunId), first);
        assertNotEquals(
                first,
                AgentRunEvidenceIdentity.runId(
                        goalId,
                        UUID.randomUUID().toString()));
        assertNotEquals(
                first,
                AgentRunEvidenceIdentity.runId(
                        UUID.randomUUID().toString(),
                        agentRunId));
        assertNotEquals(
                first,
                AgentRunEvidenceIdentity.runId(agentRunId, goalId));
    }

    @Test
    void rejectsNonCanonicalRuntimeIdentities() {
        String canonical = UUID.randomUUID().toString();

        assertThrows(
                NullPointerException.class,
                () -> AgentRunEvidenceIdentity.runId(null, canonical));
        assertThrows(
                IllegalArgumentException.class,
                () -> AgentRunEvidenceIdentity.runId("not-a-uuid", canonical));
        assertThrows(
                IllegalArgumentException.class,
                () -> AgentRunEvidenceIdentity.runId(canonical, canonical.toUpperCase()));
    }
}
