package org.zstack.header.description.route;

import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.description.apis.ApiCapabilities;

import java.util.Iterator;

/**
 * Resolve API -&gt; serviceId from {@link PackageDescriptionRegistry#resolve(Class)}:
 * first non-null {@code serviceId} on the matching path (class, then longest package prefix).
 */
public final class ApiRouteUtils {
    private ApiRouteUtils() {
    }

    public static String resolveServiceIdFromRegistry(Class<?> apiClass) {
        Iterator<ApiCapabilities> it = PackageDescriptionRegistry.resolve(apiClass);
        while (it.hasNext()) {
            String serviceId = it.next().serviceId;
            if (serviceId != null && !serviceId.isEmpty()) {
                return serviceId;
            }
        }
        return null;
    }
}
