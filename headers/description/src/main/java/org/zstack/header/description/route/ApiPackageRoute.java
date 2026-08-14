package org.zstack.header.description.route;

/**
 * Package-scoped API service routing rule registered via {@link ApiRouteBuilder#inThisPackage()}.
 */
public final class ApiPackageRoute {
    /** Package name of the declaring PackageDescription implementation (and subtree). */
    public final String packageName;
    public final String serviceId;

    public ApiPackageRoute(String packageName, String serviceId) {
        this.packageName = packageName;
        this.serviceId = serviceId;
    }
}
