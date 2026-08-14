package org.zstack.header.description.apis;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.message.APIMessageDefinition;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for API descriptions: service routing and attributes such as skipAudit.
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
 *
 * apis()
 *     .inPackage("org.example.api")
 *     .api(APISomeMsg.class)
 *     .toService(OtherConstant.SERVICE_ID)
 *     .skipAudit()
 *     .build();
 * </pre>
 */
public class ApiDescriptionBuilder {
    // which APIs this description applies to
    private final PackageDescription description;
    private final List<String> packageNames = new ArrayList<>();
    private final List<Class<? extends APIMessageDefinition>> apiClasses = new ArrayList<>();

    // capabilities of those APIs
    private String serviceId;
    private boolean skipAudit;

    public ApiDescriptionBuilder(PackageDescription description) {
        this.description = description;
    }

    /**
     * Select APIs under the declaring class's package (including subpackages).
     * When multiple package rules match, the deepest (longest) package name wins for {@link #toService(String)}.
     * For {@link #skipAudit()}, see that method: declare the deepest package that should skip.
     */
    public ApiDescriptionBuilder inThisPackage() {
        this.packageNames.add(description.getClass().getPackage().getName());
        return this;
    }

    /**
     * Select APIs under {@code packageName} (including subpackages).
     * When combined with {@link #skipAudit()}, pass the deepest package that
     * should skip; a parent prefix matches every subpackage on the lookup path.
     */
    public ApiDescriptionBuilder inPackage(String packageName) {
        DebugUtils.Assert(packageName != null && !packageName.isEmpty(), "packageName cannot be empty");
        this.packageNames.add(packageName);
        return this;
    }

    /**
     * Select one or more API classes. Class capabilities are the first segment of
     * the matching path; unset fields fall through to package rules.
     */
    @SafeVarargs
    public final ApiDescriptionBuilder api(Class<? extends APIMessageDefinition>... classes) {
        DebugUtils.Assert(classes != null && classes.length > 0, "api classes cannot be empty");
        for (Class<? extends APIMessageDefinition> clz : classes) {
            DebugUtils.Assert(clz != null, "api class cannot be null");
            this.apiClasses.add(clz);
        }
        return this;
    }

    public ApiDescriptionBuilder toService(String serviceId) {
        DebugUtils.Assert(serviceId != null && !serviceId.isEmpty(), "serviceId cannot be empty");
        this.serviceId = serviceId;
        return this;
    }

    /**
     * Skip API audit for the selected APIs.
     * <p>
     * With {@link #inPackage(String)} / {@link #inThisPackage()}, declare the
     * deepest package that should skip. Lookup walks matching prefixes from
     * longest to shortest and stops skipAudit at the first {@code true}, so a
     * shallow parent covers all of its subpackages.
     */
    public ApiDescriptionBuilder skipAudit() {
        this.skipAudit = true;
        return this;
    }

    public void build() {
        DebugUtils.Assert(!apiClasses.isEmpty() || !packageNames.isEmpty(),
                "call inThisPackage(), inPackage(name), or api(Class...) before build()");

        ApiCapabilities capabilities = new ApiCapabilities(
                serviceId,
                skipAudit ? Boolean.TRUE : null);
        if (capabilities.isEmpty()) {
            return;
        }

        for (Class<? extends APIMessageDefinition> apiClass : apiClasses) {
            PackageDescriptionRegistry.addApiCapabilities(apiClass, capabilities);
        }
        for (String packageName : packageNames) {
            PackageDescriptionRegistry.addPackageCapabilities(packageName, capabilities);
        }
    }
}
