package org.zstack.zsv.telemetry.license;

import org.zstack.header.telemetry.TelemetryLicenseTypeProvider;
import org.zstack.zsv.telemetry.TelemetryConstant;

/**
 * Default / OSS fallback for {@link TelemetryLicenseTypeProvider}.
 * <p>
 * Why not call {@code LicenseManager} here: after the open-source split, license code is
 * removed from OSS builds and remains only in the enterprise edition. Referencing license
 * APIs from this telemetry stub would cause OSS compile failures. Enterprise overrides this
 * bean with a {@code primary="true"} implementation under {@code org.zstack.license} that
 * queries {@code LicenseManager}.
 */
public class DummyTelemetryLicenseTypeProvider implements TelemetryLicenseTypeProvider {
    @Override
    public String getLicenseType() {
        return TelemetryConstant.LICENSE_TYPE_PLACEHOLDER;
    }
}
