package org.zstack.header.description.audits;

import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.description.apis.ApiCapabilities;

import java.util.Iterator;

/**
 * API audit attributes from {@link PackageDescriptionRegistry#resolve(Class)}.
 */
public final class ApiAudits {
    private ApiAudits() {
    }

    /**
     * First {@code skipAudit=true} on the matching path (class, then longest package prefix).
     */
    public static boolean shouldSkipAudit(Class<?> apiClass) {
        Iterator<ApiCapabilities> it = PackageDescriptionRegistry.resolve(apiClass);
        while (it.hasNext()) {
            if (Boolean.TRUE.equals(it.next().skipAudit)) {
                return true;
            }
        }
        return false;
    }
}
