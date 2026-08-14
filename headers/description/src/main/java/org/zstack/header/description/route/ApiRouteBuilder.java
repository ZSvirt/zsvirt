package org.zstack.header.description.route;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.message.APIMessageDefinition;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fluent builder for API -> serviceId routing rules.
 * <p>
 * Configuration only; descriptor execution may read {@link PackageDescriptionRegistry} later.
 * Prefer multi-line call sites, e.g.
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
 * </pre>
 */
public class ApiRouteBuilder {
    private final PackageDescription description;
    private String packageName;
    private final List<Class<? extends APIMessageDefinition>> apiClasses = new ArrayList<>();
    private String serviceId;

    public ApiRouteBuilder(PackageDescription description) {
        this.description = description;
    }

    /**
     * Route APIs under the declaring class's package (including subpackages) to a service.
     * When multiple package rules match, the deepest (longest) package name wins.
     */
    public ApiRouteBuilder inThisPackage() {
        this.packageName = description.getClass().getPackage().getName();
        this.apiClasses.clear();
        return this;
    }

    public ApiRouteBuilder inPackage(String packageName) {
        this.packageName = packageName;
        this.apiClasses.clear();
        return this;
    }

    /**
     * Route one or more API classes. Each takes precedence over any package rule for that class.
     */
    @SafeVarargs
    public final ApiRouteBuilder api(Class<? extends APIMessageDefinition>... classes) {
        DebugUtils.Assert(classes != null && classes.length > 0, "api classes cannot be empty");
        this.apiClasses.clear();
        Collections.addAll(this.apiClasses, classes);
        for (Class<? extends APIMessageDefinition> clz : this.apiClasses) {
            DebugUtils.Assert(clz != null, "api class cannot be null");
        }
        this.packageName = null;
        return this;
    }

    public ApiRouteBuilder toService(String serviceId) {
        DebugUtils.Assert(serviceId != null && !serviceId.isEmpty(), "serviceId cannot be empty");
        this.serviceId = serviceId;
        return this;
    }

    public void build() {
        DebugUtils.Assert(serviceId != null && !serviceId.isEmpty(),
                "toService(serviceId) is required before build()");
        DebugUtils.Assert(!apiClasses.isEmpty() || packageName != null,
                "call inThisPackage(), inPackage(name), or api(Class...) before build()");

        if (!apiClasses.isEmpty()) {
            for (Class<? extends APIMessageDefinition> apiClass : apiClasses) {
                PackageDescriptionRegistry.addApiServiceRoute(apiClass, serviceId);
            }
        } else {
            PackageDescriptionRegistry.addPackageServiceRoute(packageName, serviceId);
        }
    }
}
