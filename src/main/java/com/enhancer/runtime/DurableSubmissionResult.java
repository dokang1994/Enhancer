package com.enhancer.runtime;

/** Observable outcome of converging one durable submission prefix. */
public record DurableSubmissionResult(
        String submissionId,
        String queueId,
        boolean manifestCreated,
        boolean queueCreated,
        boolean workAdmitted,
        long queueRevision) {
}
