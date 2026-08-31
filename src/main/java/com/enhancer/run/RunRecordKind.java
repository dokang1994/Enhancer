package com.enhancer.run;

/** The two known payload kinds sharing the RunRecord artifact envelope. */
public enum RunRecordKind {
    RUN_RECORD_V1(1),
    MODEL_RUN_RECORD_V2(2);

    private final int payloadVersion;

    RunRecordKind(int payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    public int payloadVersion() {
        return payloadVersion;
    }
}
