package org.zstack.header.telemetry;

/**
 * Provides {@code license_type} for telemetry report / check-update payloads.
 * <p>
 * Why an SPI (interface + default stub + enterprise primary bean) instead of calling
 * {@code LicenseManager} from the telemetry module directly:
 * open-source builds will drop the license subsystem later and keep only the enterprise
 * edition with license code. Direct compile-time dependencies on license types/classes
 * from telemetry would break OSS compilation. The default stub (no license imports) stays
 * with telemetry; enterprise registers a {@code primary="true"} bean under license that
 * queries {@code LicenseManager}.
 */
public interface TelemetryLicenseTypeProvider {
    String getLicenseType();
}
