package org.zstack.header.description;

import org.zstack.header.description.ensemble.ResourceEnsembleMember;
import org.zstack.header.description.permission.Permission;
import org.zstack.header.description.role.Role;
import org.zstack.header.description.role.RoleBuilder;
import org.zstack.header.description.role.RoleContributor;
import org.zstack.header.description.route.ApiPackageRoute;
import org.zstack.header.message.APIMessageDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared registries filled by package fluent builders.
 * Execution (e.g. RBAC StaticInit) reads from here.
 */
public final class PackageDescriptionRegistry {
    private PackageDescriptionRegistry() {
    }

    public static final List<Permission> permissions = new ArrayList<>();
    public static final List<Role> roles = new ArrayList<>();
    public static final List<Class<?>> readableResources = new ArrayList<>();
    public static final List<Class<?>> attributeSupportResources = new ArrayList<>();
    public static final List<ResourceEnsembleMember> ensembleMembers = new ArrayList<>();

    /**
     * Expanded-permission expanders keyed by base API class.
     * Values are raw Functions registered via {@link #addExpandedPermission}; execution casts as needed.
     */
    public static final Map<Class<?>, List<Function<?, List<? extends APIMessageDefinition>>>> expandedPermissions =
            new HashMap<>();

    /**
     * API class -> serviceId, from:
     * <pre>
     * apis()
     *     .api(...Msg.class / multi)
     *     .toService(...SERVICE_ID)
     *     .build();
     * </pre>
     * Takes precedence over {@link #packageServiceRoutes} for that class.
     * Later registration overwrites the same key.
     */
    public static final Map<Class<? extends APIMessageDefinition>, String> apiServiceRoutes = new HashMap<>();

    /**
     * Package-prefix -> serviceId routes, from:
     * <pre>
     * apis()
     *     .inThisPackage()
     *     .toService(...SERVICE_ID)
     *     .build();
     * </pre>
     * Later registration of the same package name replaces the previous entry.
     */
    public static final List<ApiPackageRoute> packageServiceRoutes = new ArrayList<>();

    static final List<RoleContributor> roleContributors = new ArrayList<>();
    static final List<RoleBuilder> roleBuilders = new ArrayList<>();

    public static List<RoleContributor> getRoleContributors() {
        return roleContributors;
    }

    public static List<RoleBuilder> getRoleBuilders() {
        return roleBuilders;
    }

    /** For builders in this module that register contributors/builders. */
    public static void addRoleContributor(RoleContributor contributor) {
        roleContributors.add(contributor);
    }

    public static void addRoleBuilder(RoleBuilder builder) {
        roleBuilders.add(builder);
    }

    public static <MSG extends APIMessageDefinition> void addExpandedPermission(
            Class<MSG> apiClass,
            Function<MSG, List<? extends APIMessageDefinition>> function) {
        expandedPermissions.computeIfAbsent(apiClass, k -> new ArrayList<>()).add(function);
    }

    public static void addApiServiceRoute(Class<? extends APIMessageDefinition> apiClass, String serviceId) {
        apiServiceRoutes.put(apiClass, serviceId);
    }

    public static void addPackageServiceRoute(String packageName, String serviceId) {
        packageServiceRoutes.removeIf(r -> r.packageName.equals(packageName));
        packageServiceRoutes.add(new ApiPackageRoute(packageName, serviceId));
    }
}
