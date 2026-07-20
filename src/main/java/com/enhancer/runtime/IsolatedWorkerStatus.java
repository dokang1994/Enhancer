package com.enhancer.runtime;

/**
 * Terminal outcome of one isolated worker process. These values are deliberately distinct from
 * an exit code: only {@link #COMPLETED} means the child chose how it ended.
 */
public enum IsolatedWorkerStatus {
    /** The child exited on its own within the timeout; its exit code is retained. */
    COMPLETED,

    /** The child overran its timeout and was forcibly destroyed; it has no meaningful exit code. */
    TIMED_OUT,

    /** The child could not be started at all, so nothing ran. */
    START_FAILED
}
