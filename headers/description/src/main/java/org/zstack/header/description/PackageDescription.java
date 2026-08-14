package org.zstack.header.description;

import org.zstack.header.description.apis.ApiDescriptionBuilder;
import org.zstack.header.description.ensemble.ResourceEnsembleContributorBuilder;
import org.zstack.header.description.permission.PermissionBuilder;
import org.zstack.header.description.resource.AttributeSupportResourceBuilder;
import org.zstack.header.description.resource.GlobalReadableResourceBuilder;
import org.zstack.header.description.role.RoleBuilder;
import org.zstack.header.description.role.RoleContributorBuilder;
import org.zstack.header.message.APIMessageDefinition;

import java.util.List;
import java.util.function.Function;

/**
 * Package-level description contract.
 * <p>
 * Implementations (typically named {@code RBACInfo}) declare configuration for a package
 * via fluent builders. <b>Execution</b> of the collected data (e.g. RBAC assembly) lives
 * outside this module.
 */
public interface PackageDescription {
    default PermissionBuilder permissionBuilder() {
        return new PermissionBuilder(this);
    }

    default RoleContributorBuilder roleContributorBuilder() {
        return new RoleContributorBuilder(this);
    }

    default void contributeNormalApiToOtherRole() {
        roleContributorBuilder().toOtherRole().actionsInThisPermission().build();
    }

    default RoleBuilder roleBuilder() {
        return new RoleBuilder(this);
    }

    default GlobalReadableResourceBuilder globalReadableResourceBuilder() {
        return new GlobalReadableResourceBuilder();
    }

    default AttributeSupportResourceBuilder attributeSupportResourceBuilder() {
        return new AttributeSupportResourceBuilder();
    }

    /**
     * Contribute a resource into a resource ensemble (child under a parent type).
     */
    default ResourceEnsembleContributorBuilder resourceEnsembleContributorBuilder() {
        return new ResourceEnsembleContributorBuilder();
    }

    /**
     * Fluent API description: service routing and attributes (configuration only).
     * Prefer multi-line chains at call sites, e.g.
     * <pre>
     * apis()
     *     .inThisPackage()
     *     .toService(HostConstant.SERVICE_ID)
     *     .build();
     *
     * apis()
     *     .api(APISomeMsg.class, APIOtherMsg.class)
     *     .toService(OtherConstant.SERVICE_ID)
     *     .build();
     *
     * apis()
     *     .api(APISomeMsg.class)
     *     .toService(OtherConstant.SERVICE_ID)
     *     .skipAudit()
     *     .build();
     * </pre>
     */
    default ApiDescriptionBuilder apis() {
        return new ApiDescriptionBuilder(this);
    }

    /**
     * Register expanders that map a base API to additional API messages for permission checks.
     * Configuration only; apply/check is done by the identity RBAC execution layer.
     */
    default <MSG extends APIMessageDefinition> void expandedPermission(
            Class<MSG> apiClass,
            Function<MSG, List<? extends APIMessageDefinition>> function) {
        PackageDescriptionRegistry.addExpandedPermission(apiClass, function);
    }

    String permissionName();
}
