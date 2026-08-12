package com.enhancer.maintenance;

import java.io.IOException;

@FunctionalInterface
interface CancellationTrustMaintenanceFaultInjector {
    void after(CancellationTrustMaintenancePhase phase) throws IOException;
}
