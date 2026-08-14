package org.zstack.zsv.telemetry;

public enum TelemetryErrors {
    GENERAL_ERROR(1000),

    // 13XX: certificate related (1300 unused)
    TELEMETRY_CERTIFICATE_IMPORT_FAILED(1301),

    // 2XXX: TELEMETRY_CONSENT_ERROR — consent / authorization API
    TELEMETRY_CONSENT_ERROR(2000),
    TELEMETRY_CONSENT_REQUIRED(2001),
    TELEMETRY_ALREADY_ENABLED(2002),
    TELEMETRY_CONSENT_UPDATE_FORBIDDEN(2003),
    TELEMETRY_READONLY_GLOBAL_CONFIG(2004),

    // 3XXX: TELEMETRY_COLLECT_ERROR — local data collection
    TELEMETRY_COLLECT_ERROR(3000),
    TELEMETRY_COLLECT_FAILED(3001),

    // 4XXX: TELEMETRY_UPLOAD_ERROR — cloud upload / check-update
    TELEMETRY_UPLOAD_ERROR(4000),
    TELEMETRY_CLOUD_UNREACHABLE(4001),
    TELEMETRY_UPLOAD_FAILED(4002),
    TELEMETRY_CHECK_UPDATE_RESPONSE_INVALID(4003),
    TELEMETRY_CHECK_UPDATE_VERSION_INVALID(4004),
    ;

    private final String code;

    TelemetryErrors(int id) {
        code = String.format("TELEMETRY.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
