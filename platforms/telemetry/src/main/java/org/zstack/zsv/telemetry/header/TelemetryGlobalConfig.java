package org.zstack.zsv.telemetry.header;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.zsv.telemetry.TelemetryConstant;

@GlobalConfigDefinition
public class TelemetryGlobalConfig {
    public static final String CATEGORY = "telemetry";

    @GlobalConfigDef(
            defaultValue = "None",
            description = "UTC consent timestamp when granted, or None if not granted; writable only via APIUpdateTelemetryConsent",
            validatorRegularExpression = TelemetryConstant.CONSENT_GRANTED_AT_PATTERN
    )
    @GlobalConfigValidation
    public static GlobalConfig CONSENT_GRANTED_AT = new GlobalConfig(CATEGORY, "consent.granted.at");

    @GlobalConfigDef(defaultValue = "None", description = "Management node UUID written on first startup, never changed afterward")
    @GlobalConfigValidation
    public static GlobalConfig SOURCE_ID = new GlobalConfig(CATEGORY, "source.id");

    @GlobalConfigDef(defaultValue = "None", description = "Random salt for SHA-256 anonymization, generated on first startup, never changed afterward")
    @GlobalConfigValidation
    public static GlobalConfig ANONYMIZATION_SALT = new GlobalConfig(CATEGORY, "anonymization.salt");

    @GlobalConfigDef(
            defaultValue = "None",
            description = "Telemetry Cloud base URL (domain, not IP); must be https:// or None",
            validatorRegularExpression = TelemetryConstant.CLOUD_BASE_URL_PATTERN
    )
    @GlobalConfigValidation
    public static GlobalConfig CLOUD_BASE_URL = new GlobalConfig(CATEGORY, "cloud.base.url");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(type = Long.class, defaultValue = "86400", description = "Collection interval in seconds")
    public static GlobalConfig COLLECT_INTERVAL_SECONDS = new GlobalConfig(CATEGORY, "collect.interval.seconds");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(type = Long.class, defaultValue = "86400", description = "Upload interval in seconds")
    public static GlobalConfig UPLOAD_INTERVAL_SECONDS = new GlobalConfig(CATEGORY, "upload.interval.seconds");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(type = Long.class, defaultValue = "52428800", description = "Local telemetry data size limit in bytes")
    public static GlobalConfig LOCAL_MAX_BYTES = new GlobalConfig(CATEGORY, "local.max.bytes");
}
