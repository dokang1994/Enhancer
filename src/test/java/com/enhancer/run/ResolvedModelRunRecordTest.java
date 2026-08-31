package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ResolvedModelRunRecordTest {

    @Test
    void retainsExactlyMetadataAndTheModelRecord() {
        StoredRunRecord metadata = new StoredRunRecord(
                "11111111-1111-1111-1111-111111111111",
                "run-record/11111111-1111-1111-1111-111111111111",
                Instant.EPOCH,
                1,
                "a".repeat(64));
        ModelRunRecord record = ModelRunRecordTestFixture.record();

        ResolvedModelRunRecord resolved = new ResolvedModelRunRecord(metadata, record);

        assertSame(metadata, resolved.metadata());
        assertSame(record, resolved.record());
        assertArrayEquals(
                new String[] {"metadata", "record"},
                Arrays.stream(ResolvedModelRunRecord.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new Class<?>[] {StoredRunRecord.class, ModelRunRecord.class},
                Arrays.stream(ResolvedModelRunRecord.class.getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));
    }

    @Test
    void rejectsEitherMissingValue() {
        StoredRunRecord metadata = new StoredRunRecord(
                "11111111-1111-1111-1111-111111111111",
                "run-record/11111111-1111-1111-1111-111111111111",
                Instant.EPOCH,
                1,
                "a".repeat(64));
        ModelRunRecord record = ModelRunRecordTestFixture.record();

        assertThrows(NullPointerException.class, () -> new ResolvedModelRunRecord(null, record));
        assertThrows(NullPointerException.class, () -> new ResolvedModelRunRecord(metadata, null));
    }
}
