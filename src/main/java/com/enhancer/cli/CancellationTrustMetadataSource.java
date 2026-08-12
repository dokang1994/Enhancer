package com.enhancer.cli;

import com.enhancer.runtime.InstalledCancellationTrustMetadata;
import java.io.IOException;

/** Trusted CLI composition seam; never populated from request parsing. */
@FunctionalInterface
interface CancellationTrustMetadataSource {
    InstalledCancellationTrustMetadata load() throws IOException;
}
