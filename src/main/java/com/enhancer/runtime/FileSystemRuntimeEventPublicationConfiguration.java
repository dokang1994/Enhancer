package com.enhancer.runtime;

import java.nio.file.Path;
import java.util.Objects;

/** All-or-none filesystem inputs for durable runtime-event publication. */
public record FileSystemRuntimeEventPublicationConfiguration(
        Path runtimeEventRoot,
        Path publicationRoot,
        int maxPendingPublications) {

    public FileSystemRuntimeEventPublicationConfiguration {
        Objects.requireNonNull(
                runtimeEventRoot, "runtimeEventRoot must not be null");
        Objects.requireNonNull(
                publicationRoot, "publicationRoot must not be null");
        if (maxPendingPublications
                        < FileSystemRuntimeEventPublisher.MIN_PENDING_PUBLICATIONS
                || maxPendingPublications
                        > FileSystemRuntimeEventPublisher.MAX_PENDING_PUBLICATIONS) {
            throw new IllegalArgumentException(
                    "maxPendingPublications must be between "
                            + FileSystemRuntimeEventPublisher.MIN_PENDING_PUBLICATIONS
                            + " and "
                            + FileSystemRuntimeEventPublisher.MAX_PENDING_PUBLICATIONS);
        }
    }
}
