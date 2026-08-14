package org.zstack.header.description.permission;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.message.APIMessageDefinition;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionBuilder {
    Permission permission = new Permission();
    Package currentPackage;
    boolean defaultAdminOnly = false;

    private final List<Class<? extends APIMessageDefinition>> normalAPIList = new ArrayList<>();
    private final List<Class<? extends APIMessageDefinition>> adminOnlyAPIList = new ArrayList<>();
    private final List<String> normalAPITexts = new ArrayList<>();
    private final List<String> adminOnlyAPITexts = new ArrayList<>();

    public PermissionBuilder(PackageDescription description) {
        currentPackage = description.getClass().getPackage();
        permission.name = description.permissionName();
        permission.basePackage = this.currentPackage.getName();
    }

    public PermissionBuilder normalAPIs(String... vs) {
        Collections.addAll(normalAPITexts, vs);
        return this;
    }

    public PermissionBuilder adminOnlyAPIs(String... vs) {
        Collections.addAll(adminOnlyAPITexts, vs);
        return this;
    }

    public PermissionBuilder adminOnlyForAll() {
        defaultAdminOnly = true;
        return this;
    }

    @SafeVarargs
    public final PermissionBuilder normalAPIs(Class<? extends APIMessageDefinition>... clzs) {
        Collections.addAll(normalAPIList, clzs);
        return this;
    }

    @SafeVarargs
    public final PermissionBuilder adminOnlyAPIs(Class<? extends APIMessageDefinition>... clzs) {
        Collections.addAll(adminOnlyAPIList, clzs);
        return this;
    }

    @Deprecated
    public PermissionBuilder targetResources(Class<?>... clzs) {
        Collections.addAll(permission.targetResources, clzs);
        return this;
    }

    public PermissionBuilder communityAvailable() {
        permission.requirementList.add(PermissionAvailability.COMMUNITY_AVAILABLE);
        return this;
    }

    public PermissionBuilder zsvBasicAvailable() {
        permission.requirementList.add(PermissionAvailability.ZSV_BASIC_AVAILABLE);
        return this;
    }

    public PermissionBuilder zsvProAvailable() {
        permission.requirementList.add(PermissionAvailability.ZSV_PRO_AVAILABLE);
        return this;
    }

    public PermissionBuilder zsvAdvancedAvailable() {
        permission.requirementList.add(PermissionAvailability.ZSV_ADVANCED_AVAILABLE);
        return this;
    }

    public PermissionBuilder productName(String product) {
        permission.productList.add(product);
        return this;
    }

    public Permission build() {
        String packagePermission = permission.basePackage + ".**";
        normalAPITexts.remove(packagePermission);
        adminOnlyAPITexts.remove(packagePermission);

        if (defaultAdminOnly) {
            adminOnlyAPITexts.add(packagePermission);
        } else {
            normalAPITexts.add(packagePermission);
        }

        permission.normalPolicies.addAll(normalAPITexts);
        permission.adminOnlyPolicies.addAll(adminOnlyAPITexts);
        permission.normalAPIs.addAll(normalAPIList);
        permission.adminOnlyAPIs.addAll(adminOnlyAPIList);

        DebugUtils.Assert(
                PackageDescriptionRegistry.permissions.stream()
                        .noneMatch(it -> it.name != null && it.name.equals(permission.name)),
                String.format("package description already has a permission named: %s", permission.name));

        PackageDescriptionRegistry.permissions.add(permission);
        return permission;
    }
}
