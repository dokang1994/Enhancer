package com.enhancer.maintenance.installation;

/** Platform-neutral effective operation used by the fixed permission policy. */
public enum InstallationAccess {
    READ,
    EXECUTE,
    TRAVERSE,
    CREATE,
    WRITE,
    REPLACE,
    RENAME,
    DELETE,
    DELETE_CHILD,
    CHANGE_OWNER,
    CHANGE_PERMISSIONS
}
