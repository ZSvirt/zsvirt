package org.zstack.header.description.route;

import org.zstack.header.description.PackageDescriptionRegistry;

/**
 * Resolve API class -&gt; serviceId from {@link PackageDescriptionRegistry} tables filled by {@code apis()}.
 * Explicit class routes win; else deepest matching package route.
 */
public final class ApiRouteUtils {
    private ApiRouteUtils() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String resolveServiceIdFromRegistry(Class<?> apiClass) {
        if (apiClass == null) {
            return null;
        }
        String byApi = PackageDescriptionRegistry.apiServiceRoutes.get((Class) apiClass);
        if (byApi != null) {
            return byApi;
        }

        Package pkg = apiClass.getPackage();
        if (pkg == null) {
            return null;
        }
        String pkgName = pkg.getName();
        String bestServiceId = null;
        int bestLen = -1;
        for (ApiPackageRoute route : PackageDescriptionRegistry.packageServiceRoutes) {
            if (pkgName.equals(route.packageName) || pkgName.startsWith(route.packageName + ".")) {
                if (route.packageName.length() > bestLen) {
                    bestLen = route.packageName.length();
                    bestServiceId = route.serviceId;
                }
            }
        }
        return bestServiceId;
    }
}
