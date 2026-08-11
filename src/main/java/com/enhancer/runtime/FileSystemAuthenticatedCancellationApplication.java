package com.enhancer.runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Supported filesystem composition for authenticated cancellation.
 * Authorization is supplied only through the injected trusted port.
 */
public final class FileSystemAuthenticatedCancellationApplication {
    private final AuthenticatedCancellationApplication delegate;

    public FileSystemAuthenticatedCancellationApplication(
            Path runtimeStateRoot,
            Clock clock,
            ControlRequestAuthorizer authorizer) {
        this.delegate = new AuthenticatedCancellationApplication(
                new FileSystemAgentRuntimeStateStore(runtimeStateRoot),
                clock,
                authorizer);
    }

    public FileSystemAuthenticatedCancellationApplication(
            Path runtimeStateRoot,
            Clock clock,
            ControlRequestAuthorizer authorizer,
            FileSystemRuntimeEventPublicationConfiguration eventConfiguration) {
        FileSystemRuntimeEventPublicationConfiguration configuration =
                Objects.requireNonNull(
                        eventConfiguration,
                        "eventConfiguration must not be null");
        this.delegate = new AuthenticatedCancellationApplication(
                new FileSystemAgentRuntimeStateStore(runtimeStateRoot),
                clock,
                authorizer,
                new RuntimeEventRecorder(
                        new FileSystemRuntimeEventStore(
                                configuration.runtimeEventRoot()),
                        new FileSystemRuntimeEventPublisher(
                                configuration.publicationRoot(),
                                configuration.maxPendingPublications())));
    }

    public CancellationApplicationRecord apply(
            String goalId,
            String controlMessageId) throws IOException {
        return delegate.apply(goalId, controlMessageId);
    }
}
