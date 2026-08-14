package org.zstack.premium.externalservice.prometheus;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by kayo on 2018/8/13.
 */
@GlobalConfigDefinition
public class PrometheusGlobalConfig {
    public static final String CATEGORY = "prometheus";

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "6", type = Long.class, description = "prometheus series data retention time on " +
            "its local storage, in months")
    public static GlobalConfig STORAGE_LOCAL_RETENTION = new GlobalConfig(CATEGORY, "storage.local.retention");

    @GlobalConfigDef(defaultValue = "64GB", description = "prometheus series data retention size on " +
            "its local storage")
    public static GlobalConfig STORAGE_LOCAL_RETENTION_SIZE = new GlobalConfig(CATEGORY, "storage.local.retention.size");

    @GlobalConfigDef(defaultValue = "2h", description = "Minimum duration of a data block before being persisted")
    public static GlobalConfig WAL_MINIMUM_DURATION_BEFORE_PERSISTED = new GlobalConfig(CATEGORY, "storage.tsdb.min-block-duration");

    @GlobalConfigDef(defaultValue = "false", description = "enable prometheus basic auth switch")
    public static GlobalConfig ENABLE_BASIC_AUTH = new GlobalConfig(CATEGORY, "enable.basic.auth");

    @GlobalConfigDef(defaultValue = "admin", description = "prometheus basic auth `username`")
    public static GlobalConfig BASIC_AUTH_USERNAME = new GlobalConfig(CATEGORY, "basic.auth.username");

    @GlobalConfigDef(defaultValue = "password", description = "prometheus basic auth `password`")
    public static GlobalConfig BASIC_AUTH_PASSWORD = new GlobalConfig(CATEGORY, "basic.auth.password");
}
