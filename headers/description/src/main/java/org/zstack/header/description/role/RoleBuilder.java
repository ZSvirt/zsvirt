package org.zstack.header.description.role;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.message.APIMessageDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoleBuilder {
    private final Role role = new Role();
    private final List<String> permissionsByNames = new ArrayList<>();
    private final String basePermission;

    public RoleBuilder(PackageDescription description) {
        basePermission = description.permissionName();
        role.name = basePermission;
    }

    public RoleBuilder uuid(String v) {
        role.uuid = v;
        return this;
    }

    public RoleBuilder name(String v) {
        role.name = v;
        return this;
    }

    public RoleBuilder actions(String... vs) {
        role.allowedActions.addAll(Arrays.asList(vs));
        return this;
    }

    @SafeVarargs
    public final RoleBuilder actions(Class<? extends APIMessageDefinition>... clzs) {
        for (Class<?> clz : clzs) {
            role.allowedActions.add(clz.getName());
        }
        return this;
    }

    public RoleBuilder permissionsByName(String... pnames) {
        permissionsByNames.addAll(Arrays.asList(pnames));
        return this;
    }

    public RoleBuilder permissionBaseOnThis() {
        return permissionsByName(this.basePermission);
    }

    public RoleBuilder excludeActions(String... vs) {
        Collections.addAll(role.excludedActions, vs);
        return this;
    }

    @SafeVarargs
    public final RoleBuilder excludeActions(Class<? extends APIMessageDefinition>... clzs) {
        for (Class<?> clz : clzs) {
            role.excludedActions.add(clz.getName());
        }
        return this;
    }

    public List<String> getPermissionsByNames() {
        return permissionsByNames;
    }

    public Role getRole() {
        return role;
    }

    public void build() {
        PackageDescriptionRegistry.addRoleBuilder(this);
    }
}
