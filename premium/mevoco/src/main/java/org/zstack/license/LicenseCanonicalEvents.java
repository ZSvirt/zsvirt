package org.zstack.license;

import org.zstack.header.message.NeedJsonSchema;

public class LicenseCanonicalEvents {
    public static final String LICENSE_TYPE_CHANGED_PATH = "/license/type/change";

    @NeedJsonSchema
    public static class LicenseTypeChangeData {
        boolean isCurEnterpriseLicense;

        public boolean isCurEnterpriseLicense() {
            return isCurEnterpriseLicense;
        }

        public void setCurEnterpriseLicense(boolean curEnterpriseLicense) {
            isCurEnterpriseLicense = curEnterpriseLicense;
        }
    }
}
