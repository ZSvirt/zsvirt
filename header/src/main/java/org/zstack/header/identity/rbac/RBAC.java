package org.zstack.header.identity.rbac;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.core.StaticInit;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.description.ensemble.ResourceEnsembleMember;
import org.zstack.header.description.permission.Permission;
import org.zstack.header.description.role.Role;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.identity.role.RolePolicyEffect;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RolePolicyVO;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * RBAC execution / assembly layer.
 * <p>
 * Package-level <b>configuration</b> lives under {@code org.zstack.header.description.*}.
 * This class discovers {@link PackageDescription} at {@link StaticInit} and builds runtime
 * lookup structures.
 */
public class RBAC {
    public static final List<Permission> permissions = PackageDescriptionRegistry.permissions;
    public static final List<Role> roles = PackageDescriptionRegistry.roles;
    public static final List<Class<?>> readableResources = PackageDescriptionRegistry.readableResources;
    public static final List<Class<?>> attributeSupportResources = PackageDescriptionRegistry.attributeSupportResources;
    public static final List<ResourceEnsembleMember> ensembleMembers = PackageDescriptionRegistry.ensembleMembers;

    public static Map<String, ApiPermissionBucket> apiBuckets = new HashMap<>();

    public static List<RolePolicyVO> toStatements(Role role) {
        List<RolePolicyVO> results = new ArrayList<>(role.allowedActions.size() + role.excludedActions.size());

        for (String action : role.allowedActions) {
            RolePolicyVO policy = new RolePolicyVO();
            policy.setActions(RolePolicyStatement.parseAction(action));
            policy.setEffect(RolePolicyEffect.Allow);
            policy.setRoleUuid(role.uuid);
            results.add(policy);
        }

        for (String action : role.excludedActions) {
            RolePolicyVO policy = new RolePolicyVO();
            policy.setActions(RolePolicyStatement.parseAction(action));
            policy.setEffect(RolePolicyEffect.Exclude);
            policy.setRoleUuid(role.uuid);
            results.add(policy);
        }

        return results;
    }

    public static void checkMissingRBACInfo() {
        PolicyMatcher matcher = new PolicyMatcher();

        List<String> missingInPermission = new ArrayList<>();
        List<String> missingInRole = new ArrayList<>();
        List<String> invalidPermissionNames = new ArrayList<>();
        List<String> invalidRoleNames = new ArrayList<>();

        for (Permission permission : permissions) {
            if (permission.name == null || !permission.name.matches("[a-z0-9\\-]*")) {
                invalidPermissionNames.add(permission.name);
            }
        }

        for (Role role : roles) {
            if (role.name == null || !role.name.matches("[a-z0-9\\-]*")) {
                invalidRoleNames.add(role.name);
            }
        }

        APIMessage.apiMessageClasses.forEach(clz -> {
            if (clz.isAnnotationPresent(Deprecated.class) || clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                return;
            }

            String clzName = clz.getName();
            boolean has = permissions.parallelStream()
                    .anyMatch(p -> p.normalPolicies.stream().anyMatch(s -> matcher.match(s, clzName))
                            || p.adminOnlyPolicies.stream().anyMatch(s -> matcher.match(s, clzName)));

            if (!has) {
                missingInPermission.add(clzName);
            }

            has = roles.parallelStream().anyMatch(r -> r.allowedActions.parallelStream().anyMatch(ac -> matcher.match(ac, clzName))
                    || r.excludedActions.parallelStream().anyMatch(ac -> matcher.match(ac, clzName)));
            if (!has && !isAdminOnlyAPI(clzName)) {
                missingInRole.add(clzName);
            }
        });

        Collections.sort(missingInPermission);
        Collections.sort(missingInRole);
        Collections.sort(invalidPermissionNames);
        Collections.sort(invalidRoleNames);
        if (missingInPermission.isEmpty() && missingInRole.isEmpty()
                && invalidPermissionNames.isEmpty() && invalidRoleNames.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!missingInPermission.isEmpty()) {
            sb.append(String.format("Below APIs:\n %s not referred in any RBACInfo's permission\n", StringUtils.join(missingInPermission, "\n")));
        }

        if (!missingInRole.isEmpty()) {
            sb.append(String.format("Below APIs:\n %s not referred in any RBACInfo's role\n", StringUtils.join(missingInRole, "\n")));
        }

        if (!invalidPermissionNames.isEmpty()) {
            sb.append(String.format("Below Permission Names:\n %s are invalid. permission names must be lower case and connect by '-'\n",
                    StringUtils.join(invalidPermissionNames, "\n")));
        }

        if (!invalidRoleNames.isEmpty()) {
            sb.append(String.format("Below Role Names:\n %s are invalid. role names must be lower case and connect by '-'\n",
                    StringUtils.join(invalidRoleNames, "\n")));
        }

        throw new CloudRuntimeException(sb.toString());
    }

    private static Permission findPermissionByName(String name) {
        Optional<Permission> opt = permissions.stream()
                .filter(p -> p.name != null && p.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find permission[name:%s]", name));
        }

        return opt.get();
    }

    private static Role findRoleByName(String name) {
        Optional<Role> opt = roles.stream().filter(r -> r.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find role[name:%s]", name));
        }
        return opt.get();
    }

    @StaticInit
    public static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(PackageDescription.class).forEach(dclz -> {
            if (dclz.isInterface() || java.lang.reflect.Modifier.isAbstract(dclz.getModifiers())) {
                return;
            }
            PackageDescription pd;
            try {
                pd = dclz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });

        buildApiBuckets();

        PackageDescriptionRegistry.getRoleBuilders().forEach(rb -> {
            rb.getPermissionsByNames().forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                rb.getRole().allowedActions.addAll(CollectionUtils.transform(permission.normalAPIs, Class::getName));
                rb.getRole().allowedActions.addAll(permission.normalPolicies);
            });

            roles.add(rb.getRole());
        });

        PackageDescriptionRegistry.getRoleContributors().forEach(rc -> {
            Role role = findRoleByName(rc.roleName);
            rc.normalActionsByPermissionName.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                role.allowedActions.addAll(CollectionUtils.transform(permission.normalAPIs, Class::getName));
                role.allowedActions.addAll(permission.normalPolicies);
            });
            role.allowedActions.addAll(rc.actions);
        });
    }

    @Deprecated
    static class ExpendedFieldPermission {
        String fieldName;
        Class apiClass;
    }

    public static boolean isResourceGlobalReadable(Class clz) {
        return readableResources.stream().anyMatch(r -> r.isAssignableFrom(clz))
                || !OwnedByAccount.class.isAssignableFrom(clz);
    }

    public static boolean isValidAPI(String apiName) {
        return apiBuckets.containsKey(apiName);
    }

    public static boolean isAdminOnlyAPI(String apiName) {
        return apiBuckets.get(apiName).adminOnly;
    }

    /**
     * Execution facade: read expanded-permission expanders registered in
     * {@link PackageDescriptionRegistry#expandedPermissions}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Function<APIMessage, List<APIMessage>>> expendPermissionCheckList(Class<?> apiClass) {
        final List list = PackageDescriptionRegistry.expandedPermissions.get(apiClass);
        if (list == null) {
            return null;
        }
        return (List<Function<APIMessage, List<APIMessage>>>) list;
    }

    public static class ApiPermissionBucket {
        public final Permission permission;
        public final boolean adminOnly;

        public ApiPermissionBucket(Permission permission, boolean adminOnly) {
            this.permission = permission;
            this.adminOnly = adminOnly;
        }
    }

    private static void buildApiBuckets() {
        List<Pair<String, Permission>> matchingList = new ArrayList<>();

        for (Permission permission : permissions) {
            for (Class<?> apiClass : permission.normalAPIs) {
                apiBuckets.put(apiClass.getName(), new ApiPermissionBucket(permission, false));
            }
            for (Class<?> apiClass : permission.adminOnlyAPIs) {
                apiBuckets.put(apiClass.getName(), new ApiPermissionBucket(permission, true));
            }

            for (String normalAPI : permission.normalPolicies) {
                matchingList.add(new Pair<>(normalAPI, permission));
            }
            for (String adminOnlyAPI : permission.adminOnlyPolicies) {
                matchingList.add(new Pair<>(adminOnlyAPI, permission));
            }
        }
        matchingList.sort(Comparator.comparingInt(it -> -it.first().length()));

        final PolicyMatcher matcher = new PolicyMatcher();
        for (Class<?> api : APIMessage.apiMessageClasses) {
            if (apiBuckets.containsKey(api.getName())) {
                continue;
            }

            String apiName = api.getName();
            Pair<String, Permission> matched = matchingList.stream()
                    .filter(pair -> matcher.match(pair.first(), apiName))
                    .findFirst()
                    .orElseThrow(() -> new CloudRuntimeException("failed to find matched permission for API:" + apiName));
            Permission permission = matched.second();
            apiBuckets.put(apiName,
                    new ApiPermissionBucket(permission, permission.adminOnlyPolicies.contains(matched.first())));
        }
    }
}
