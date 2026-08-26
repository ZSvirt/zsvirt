package org.zstack.header.description;

import org.zstack.header.description.apis.ApiCapabilities;
import org.zstack.header.description.apis.ApiPackageCapabilities;
import org.zstack.header.description.ensemble.ResourceEnsembleMember;
import org.zstack.header.description.permission.Permission;
import org.zstack.header.description.query.ExpandedQuery;
import org.zstack.header.description.query.ExpandedQueryAlias;
import org.zstack.header.description.role.Role;
import org.zstack.header.description.role.RoleBuilder;
import org.zstack.header.description.role.RoleContributor;
import org.zstack.header.message.APIMessageDefinition;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
     * Inventory expanded-query hops from {@code expandedQuery()...build()}.
     */
    public static final List<ExpandedQuery> expandedQueries = new ArrayList<>();

    /**
     * Inventory expanded-query aliases from {@code expandedQuery()...build()}.
     */
    public static final List<ExpandedQueryAlias> expandedQueryAliases = new ArrayList<>();

    /**
     * Expanded-permission expanders keyed by base API class.
     * Values are raw Functions registered via {@link #addExpandedPermission}; execution casts as needed.
     */
    public static final Map<Class<?>, List<Function<?, List<? extends APIMessageDefinition>>>> expandedPermissions =
            new HashMap<>();

    /**
     * Class -&gt; capabilities, from {@code apis().api(...).xxx.build()}.
     * Duplicate class registration is an error (no merge).
     */
    public static final Map<Class<? extends APIMessageDefinition>, ApiCapabilities> apiCapabilitiesByClass =
            new HashMap<>();

    /**
     * Package-prefix capabilities, from {@code apis().inPackage(...).xxx.build()}.
     * The same package name may appear more than once. Kept longest-prefix first.
     */
    public static final List<ApiPackageCapabilities> apiCapabilitiesByPackage = new ArrayList<>();

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

    public static void addExpandedQuery(ExpandedQuery query) {
        DebugUtils.Assert(query != null, "expanded query cannot be null");
        expandedQueries.add(query);
    }

    public static void addExpandedQueryAlias(ExpandedQueryAlias alias) {
        DebugUtils.Assert(alias != null, "expanded query alias cannot be null");
        expandedQueryAliases.add(alias);
    }

    public static <MSG extends APIMessageDefinition> void addExpandedPermission(
            Class<MSG> apiClass,
            Function<MSG, List<? extends APIMessageDefinition>> function) {
        expandedPermissions.computeIfAbsent(apiClass, k -> new ArrayList<>()).add(function);
    }

    public static void addApiCapabilities(Class<? extends APIMessageDefinition> apiClass, ApiCapabilities capabilities) {
        DebugUtils.Assert(apiClass != null, "api class cannot be null");
        DebugUtils.Assert(capabilities != null, "capabilities cannot be null");

        ApiCapabilities last = apiCapabilitiesByClass.get(apiClass), next;

        try {
            next = (last == null) ? capabilities : last.merge(capabilities);
        } catch (Exception e) {
            throw new RuntimeException("failed to merge ApiCapabilities for class " + apiClass.getSimpleName(), e);
        }
        apiCapabilitiesByClass.put(apiClass, next);
    }

    public static void addPackageCapabilities(String packageName, ApiCapabilities capabilities) {
        DebugUtils.Assert(packageName != null && !packageName.isEmpty(), "packageName cannot be empty");
        DebugUtils.Assert(capabilities != null, "capabilities cannot be null");
        ApiPackageCapabilities row = new ApiPackageCapabilities(packageName, capabilities);
        int i = 0;
        while (i < apiCapabilitiesByPackage.size()
                && apiCapabilitiesByPackage.get(i).packageName.length() >= packageName.length()) {
            i++;
        }
        apiCapabilitiesByPackage.add(i, row);
    }

    /**
     * Class-level capabilities, or {@code null} if this API was not declared with {@code apis().api(...)}.
     */
    @SuppressWarnings({"rawtypes"})
    public static ApiCapabilities getApiCapabilities(Class<?> apiClass) {
        if (apiClass == null) {
            return null;
        }
        return apiCapabilitiesByClass.get((Class) apiClass);
    }

    /**
     * Matching path for {@code apiClass}: class-level capabilities first (if any),
     * then package-level rows, longest prefix first. Duplicate package names yield
     * multiple elements. Unset fields are left for later elements to fill.
     */
    public static Iterator<ApiCapabilities> resolve(Class<?> apiClass) {
        if (apiClass == null) {
            return Collections.emptyIterator();
        }
        return new MatchingCapabilitiesIterator(apiClass);
    }

    private static boolean packageMatches(String apiPackage, String prefix) {
        return apiPackage.equals(prefix) || apiPackage.startsWith(prefix + ".");
    }

    private static final class MatchingCapabilitiesIterator implements Iterator<ApiCapabilities> {
        private final ApiCapabilities classCaps;
        private final String apiPackage;
        private boolean classEmitted;
        private int index;
        private ApiCapabilities next;

        MatchingCapabilitiesIterator(Class<?> apiClass) {
            this.classCaps = getApiCapabilities(apiClass);
            Package pkg = apiClass.getPackage();
            this.apiPackage = pkg == null ? null : pkg.getName();
            advance();
        }

        private void advance() {
            next = null;
            if (!classEmitted) {
                classEmitted = true;
                if (classCaps != null) {
                    next = classCaps;
                    return;
                }
            }
            if (apiPackage == null) {
                return;
            }
            while (index < apiCapabilitiesByPackage.size()) {
                ApiPackageCapabilities row = apiCapabilitiesByPackage.get(index++);
                if (packageMatches(apiPackage, row.packageName)) {
                    next = row.capabilities;
                    return;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ApiCapabilities next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            ApiCapabilities caps = next;
            advance();
            return caps;
        }
    }
}
