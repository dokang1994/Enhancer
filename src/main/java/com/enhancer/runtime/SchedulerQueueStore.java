package com.enhancer.runtime;

import java.io.IOException;

/**
 * Durable boundary for one current Scheduler queue snapshot. Implementations do not grant
 * execution authority and must reject missing or invalid state rather than inventing defaults.
 */
public interface SchedulerQueueStore {
    void create(SchedulerQueueState initialState) throws IOException;

    void update(SchedulerQueueState nextState) throws IOException;

    SchedulerQueueState resolve(String queueId) throws IOException;
}
