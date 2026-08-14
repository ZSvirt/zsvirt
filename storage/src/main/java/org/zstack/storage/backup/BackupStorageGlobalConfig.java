package org.zstack.storage.backup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 */
@GlobalConfigDefinition
public class BackupStorageGlobalConfig {
    public static final String CATEGORY = "backupStorage";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");

    @GlobalConfigValidation
    @BindResourceConfig(BackupStorageVO.class)
    public static GlobalConfig RESERVED_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity");

    @GlobalConfigValidation
    public static GlobalConfig AUTO_RECONNECT_ON_ERROR = new GlobalConfig(CATEGORY, "connection.autoReconnectOnError");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig AUTO_RECONNECT_ON_ERROR_MAX_ATTEMPT_NUM = new GlobalConfig(CATEGORY, "connection.autoReconnectOnError.maxAttemptsNum");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig MAXIMUM_PING_FAILURE = new GlobalConfig(CATEGORY, "ping.maxFailure");

    @GlobalConfigValidation(min = -1)
    public static GlobalConfig SLEEP_TIME_AFTER_PING_FAILURE = new GlobalConfig(CATEGORY, "ping.sleepPeriodAfterFailure");
}
