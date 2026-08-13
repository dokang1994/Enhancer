package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.CancellationTrustInstallationPermissionPolicy;
import com.enhancer.maintenance.installation.InstallationAccess;
import com.enhancer.maintenance.installation.InstallationArtifactKind;
import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import java.util.EnumSet;
import java.util.Set;

/** Exact Windows raw-right closure for the fixed normalized transaction policy. */
public final class WindowsInstallationRightsPolicy {
    private WindowsInstallationRightsPolicy() { }

    public static WindowsPrincipalArtifactAccess required(
            InstallationArtifactKind kind,
            WindowsObjectType objectType,
            InstallationPrincipalRole role) {
        Set<InstallationAccess> normalized =
                CancellationTrustInstallationPermissionPolicy.rule(kind, role).allowed();
        EnumSet<WindowsRawAccessRight> target = EnumSet.noneOf(
                WindowsRawAccessRight.class);
        EnumSet<WindowsRawAccessRight> parent = EnumSet.noneOf(
                WindowsRawAccessRight.class);
        for (InstallationAccess access : normalized) {
            addRawClosure(access, objectType, target, parent);
        }
        if (!target.isEmpty()) {
            target.add(WindowsRawAccessRight.READ_CONTROL);
        }
        return new WindowsPrincipalArtifactAccess(
                role, partition(target), partition(parent), normalized,
                complement(normalized, InstallationAccess.class));
    }

    private static void addRawClosure(
            InstallationAccess access,
            WindowsObjectType objectType,
            EnumSet<WindowsRawAccessRight> target,
            EnumSet<WindowsRawAccessRight> parent) {
        switch (access) {
            case READ -> {
                target.add(objectType == WindowsObjectType.DIRECTORY
                        ? WindowsRawAccessRight.LIST_DIRECTORY
                        : WindowsRawAccessRight.READ_DATA);
                target.add(WindowsRawAccessRight.READ_ATTRIBUTES);
                target.add(WindowsRawAccessRight.READ_EA);
            }
            case EXECUTE -> target.add(WindowsRawAccessRight.EXECUTE);
            case TRAVERSE -> target.add(WindowsRawAccessRight.TRAVERSE);
            case CREATE -> parent.add(objectType == WindowsObjectType.DIRECTORY
                    ? WindowsRawAccessRight.ADD_SUBDIRECTORY
                    : WindowsRawAccessRight.ADD_FILE);
            case WRITE -> {
                target.add(WindowsRawAccessRight.WRITE_DATA);
                target.add(WindowsRawAccessRight.APPEND_DATA);
                target.add(WindowsRawAccessRight.WRITE_EA);
                target.add(WindowsRawAccessRight.WRITE_ATTRIBUTES);
            }
            case REPLACE, RENAME -> {
                target.add(WindowsRawAccessRight.DELETE);
                parent.add(objectType == WindowsObjectType.DIRECTORY
                        ? WindowsRawAccessRight.ADD_SUBDIRECTORY
                        : WindowsRawAccessRight.ADD_FILE);
            }
            case DELETE -> target.add(WindowsRawAccessRight.DELETE);
            case DELETE_CHILD -> target.add(WindowsRawAccessRight.DELETE_CHILD);
            case CHANGE_OWNER -> target.add(WindowsRawAccessRight.WRITE_OWNER);
            case CHANGE_PERMISSIONS -> target.add(WindowsRawAccessRight.WRITE_DAC);
        }
    }

    private static WindowsRawAccessPartition partition(Set<WindowsRawAccessRight> allowed) {
        return new WindowsRawAccessPartition(allowed,
                complement(allowed, WindowsRawAccessRight.class));
    }

    private static <E extends Enum<E>> Set<E> complement(Set<E> allowed, Class<E> type) {
        EnumSet<E> denied = EnumSet.allOf(type);
        denied.removeAll(allowed);
        return Set.copyOf(denied);
    }
}
