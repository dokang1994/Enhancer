package com.enhancer.runtime;

/** Outcome of receiving one retained transport Work message into the durable queue. */
public enum DurableWorkMessageReceiveStatus {
    ADMITTED,
    REPLAYED
}
