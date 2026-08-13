package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Complete bounded token identity evidence for one installation role. */
public record WindowsPrincipalTokenEvidence(
        InstallationPrincipalRole role,
        WindowsSid userSid,
        List<WindowsTokenGroupEvidence> groups,
        Map<WindowsTokenPrivilege, WindowsPrivilegeState> privileges) {
    private static final Set<String> FORBIDDEN_IDENTITIES = Set.of(
            "S-1-5-18", "S-1-5-19", "S-1-5-20");
    private static final Set<String> FORBIDDEN_GROUPS = Set.of(
            "S-1-5-32-544", "S-1-5-32-548", "S-1-5-32-549",
            "S-1-5-32-550", "S-1-5-32-551");

    public WindowsPrincipalTokenEvidence {
        role = Objects.requireNonNull(role, "role must not be null");
        userSid = Objects.requireNonNull(userSid, "userSid must not be null");
        boolean unprivileged = role == InstallationPrincipalRole.OPERATOR
                || role == InstallationPrincipalRole.RUNTIME;
        if (unprivileged && FORBIDDEN_IDENTITIES.contains(userSid.canonicalValue())) {
            throw new IllegalArgumentException("built-in service identity is forbidden");
        }
        groups = List.copyOf(Objects.requireNonNull(groups, "groups must not be null"));
        if (groups.size() > 256) {
            throw new IllegalArgumentException("groups exceed supported bounds");
        }
        Set<WindowsSid> uniqueGroups = new HashSet<>();
        for (WindowsTokenGroupEvidence group : groups) {
            WindowsTokenGroupEvidence checked = Objects.requireNonNull(
                    group, "group must not be null");
            if (!uniqueGroups.add(checked.sid())) {
                throw new IllegalArgumentException("group SIDs must be unique");
            }
            if (unprivileged && checked.enabled() && FORBIDDEN_GROUPS.contains(
                    checked.sid().canonicalValue())) {
                throw new IllegalArgumentException("dangerous group is forbidden");
            }
        }
        Map<WindowsTokenPrivilege, WindowsPrivilegeState> checked =
                Objects.requireNonNull(privileges, "privileges must not be null");
        if (!checked.keySet().equals(EnumSet.allOf(WindowsTokenPrivilege.class))) {
            throw new IllegalArgumentException("privilege evidence must be complete");
        }
        EnumMap<WindowsTokenPrivilege, WindowsPrivilegeState> copied =
                new EnumMap<>(WindowsTokenPrivilege.class);
        for (WindowsTokenPrivilege privilege : WindowsTokenPrivilege.values()) {
            WindowsPrivilegeState state = Objects.requireNonNull(
                    checked.get(privilege), "privilege state must not be null");
            if (unprivileged && state == WindowsPrivilegeState.ENABLED) {
                throw new IllegalArgumentException("dangerous privilege must not be enabled");
            }
            copied.put(privilege, state);
        }
        privileges = Map.copyOf(copied);
    }
}
