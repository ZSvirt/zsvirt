package org.zstack.header.description.role;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.description.PackageDescriptionRegistry;

import java.util.Arrays;

public class RoleContributorBuilder {
    private final RoleContributor contributor = new RoleContributor();
    private final String basePermission;

    public RoleContributorBuilder(PackageDescription description) {
        this.basePermission = description.permissionName();
    }

    public RoleContributorBuilder actionsByPermissionName(String v) {
        contributor.normalActionsByPermissionName.add(v);
        return this;
    }

    public RoleContributorBuilder actionsInThisPermission() {
        return actionsByPermissionName(basePermission);
    }

    public RoleContributorBuilder actions(String... vs) {
        contributor.actions.addAll(Arrays.asList(vs));
        return this;
    }

    public RoleContributorBuilder actions(Class... clzs) {
        for (Class clz : clzs) {
            contributor.actions.add(clz.getName());
        }
        return this;
    }

    public RoleContributorBuilder roleName(String v) {
        contributor.roleName = v;
        return this;
    }

    /**
     * Maps to the system "other" role.
     */
    public RoleContributorBuilder toOtherRole() {
        return roleName("other");
    }

    public RoleContributor build() {
        PackageDescriptionRegistry.addRoleContributor(contributor);
        return contributor;
    }
}
