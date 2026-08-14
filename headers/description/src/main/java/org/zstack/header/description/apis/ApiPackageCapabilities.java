package org.zstack.header.description.apis;

/**
 * One package-prefix row in {@link org.zstack.header.description.PackageDescriptionRegistry}.
 * The same {@code packageName} may appear more than once; lookup picks fields along the path.
 */
public final class ApiPackageCapabilities {
    public final String packageName;
    public final ApiCapabilities capabilities;

    public ApiPackageCapabilities(String packageName, ApiCapabilities capabilities) {
        this.packageName = packageName;
        this.capabilities = capabilities;
    }
}
