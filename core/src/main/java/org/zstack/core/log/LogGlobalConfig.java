package org.zstack.core.log;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class LogGlobalConfig {
    public static final String CATEGORY = "managementServer";

    @GlobalConfigValidation
    public static GlobalConfig LOG_DELETE_LAST_MODIFIED = new GlobalConfig(CATEGORY, "log.delete.lastModified");

    @GlobalConfigValidation
    public static GlobalConfig LOG_DELETE_ACCUMULATED_FILE_SIZE = new GlobalConfig(CATEGORY, "log.delete.accumulatedFileSize");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "sync custom log configuration interval")
    public static GlobalConfig SYNC_CUSTOM_LOG_CONFIGURATION_TASK_INTERVAL = new GlobalConfig(CATEGORY, "log.syncCustomLogConfigurationTaskInterval");
}
