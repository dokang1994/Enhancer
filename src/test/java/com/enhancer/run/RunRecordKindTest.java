package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RunRecordKindTest {

    @Test
    void exposesOnlyTheTwoKnownPayloadKinds() {
        assertArrayEquals(
                new RunRecordKind[] {
                    RunRecordKind.RUN_RECORD_V1,
                    RunRecordKind.MODEL_RUN_RECORD_V2
                },
                RunRecordKind.values());
        assertEquals(1, RunRecordKind.RUN_RECORD_V1.payloadVersion());
        assertEquals(2, RunRecordKind.MODEL_RUN_RECORD_V2.payloadVersion());
    }

    @Test
    void kindMismatchIsDistinctFromCorruptionAndRetainsItsContext() {
        UnsupportedRunRecordKindException failure =
                new UnsupportedRunRecordKindException(
                        "run-record/11111111-1111-1111-1111-111111111111",
                        RunRecordKind.RUN_RECORD_V1,
                        RunRecordKind.MODEL_RUN_RECORD_V2);

        assertTrue(failure instanceof IOException);
        assertNotEquals(CorruptedRunRecordException.class, failure.getClass());
        assertEquals(
                "run-record/11111111-1111-1111-1111-111111111111",
                failure.reference());
        assertEquals(RunRecordKind.RUN_RECORD_V1, failure.expectedKind());
        assertEquals(RunRecordKind.MODEL_RUN_RECORD_V2, failure.actualKind());
    }
}
