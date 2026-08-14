package org.zstack.compute.host;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.zone.ZoneVO;

/**
 */
@GlobalConfigDefinition
public class HostGlobalConfig {
    public static final String CATEGORY = "host";

    @GlobalConfigValidation
    public static GlobalConfig SIMULTANEOUSLY_LOAD = new GlobalConfig(CATEGORY, "load.all");
    @GlobalConfigValidation
    public static GlobalConfig AUTO_RECONNECT_ON_ERROR = new GlobalConfig(CATEGORY, "connection.autoReconnectOnError");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_LOAD_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "load.parallelismDegree");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_TRACK_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig PING_HOST_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig PING_HOST_TIMEOUT = new GlobalConfig(CATEGORY, "ping.timeout");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig MAXIMUM_PING_FAILURE = new GlobalConfig(CATEGORY, "ping.maxFailure");
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig SLEEP_TIME_AFTER_PING_FAILURE = new GlobalConfig(CATEGORY, "ping.sleepPeriodAfterFailure");
    @GlobalConfigValidation
    public static GlobalConfig IGNORE_ERROR_ON_MAINTENANCE_MODE = new GlobalConfig(CATEGORY, "maintenanceMode.ignoreError");
    @GlobalConfigValidation(min = 1, max = 1000)
    @BindResourceConfig({HostVO.class, ClusterVO.class, ZoneVO.class})
    public static GlobalConfig HOST_CPU_OVER_PROVISIONING_RATIO = new GlobalConfig(CATEGORY, "cpu.overProvisioning.ratio");
    @GlobalConfigValidation
    public static GlobalConfig RECONNECT_ALL_ON_BOOT = new GlobalConfig(CATEGORY, "reconnectAllOnBoot");
    @GlobalConfigValidation
    public static GlobalConfig HOST_UPDATE_OS_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "update.os.parallelismDegree");
    @GlobalConfigValidation(max = 4096)
    public static GlobalConfig BATCH_ADD_HOST_LIMIT = new GlobalConfig(CATEGORY, "add.batchLimit");
    @GlobalConfigValidation(validValues = {"JustMigrate", "StopVmOnMigrationFailure"})
    public static GlobalConfig HOST_MAINTENANCE_POLICY = new GlobalConfig(CATEGORY, "host.maintenance.policy");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig AUTO_RECONNECT_ON_ERROR_MAX_ATTEMPT_NUM = new GlobalConfig(CATEGORY, "connection.autoReconnectOnError.maxAttemptsNum");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig REPORT_HOST_CAPACITY_INTERVAL = new GlobalConfig(CATEGORY, "reportHostCapacityInterval");
    @GlobalConfigValidation(min = 0, max = 65535)
    public static GlobalConfig HOST_PORT_ALLOCATION_START_PORT = new GlobalConfig(CATEGORY, "host.port.allocate.start.port");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_POWER_REFRESH_INTERVAL = new GlobalConfig(CATEGORY, "host.power.refresh.interval");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig SYNC_HOST_HW_MONITOR_INTERVAL = new GlobalConfig(CATEGORY, "sync.host.hw.monitor.interval");

    @GlobalConfigValidation
    @GlobalConfigDef(type = String.class, defaultValue = "10501:10999", description = "nbd port range")
    public static GlobalConfig NBD_PORT_RANGE = new GlobalConfig(CATEGORY, "nbd.port.range");
}
