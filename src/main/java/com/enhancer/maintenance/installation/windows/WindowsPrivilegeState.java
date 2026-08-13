package com.enhancer.maintenance.installation.windows;

/** Explicit token privilege state; omission is never interpreted as denial. */
public enum WindowsPrivilegeState {
    ABSENT,
    REMOVED,
    DISABLED,
    ENABLED
}
