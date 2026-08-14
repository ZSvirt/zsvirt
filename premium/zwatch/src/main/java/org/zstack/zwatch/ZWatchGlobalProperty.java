package org.zstack.zwatch;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ZWatchGlobalProperty {
    @GlobalProperty(name = "zwatch.migrateFromOldMonitorImplementation", defaultValue = "false")
    public static boolean MIGRATE_FROM_OLD_MONITOR_IMPLEMENTATION;
    @GlobalProperty(name="zwatch.cleanupStalePrometheusConfigInterval", defaultValue = "60")
    public static int CLEAN_UP_STALE_PROMETHEUS_STATIC_CONFIG_INTERVAL;
    @GlobalProperty(name="zwatch.disable.metrics", defaultValue = "")
    public static String DISABLE_METRICS_LIST;

    @GlobalProperty(name="kvmhost.pushgateway.webListenPort", defaultValue = "9092")
    public static String  KVMHOST_PUSHGATEWAY_LISTEN_PORT;
    @GlobalProperty(name="kvmhost.pushgateway.persistence.file", defaultValue = "AUTO")
    public static String  KVMHOST_PUSHGATEWAY_PERSISTENCE_FILE;
    @GlobalProperty(name="kvmhost.pushgateway.persistence.interval", defaultValue = "1m")
    public static String  KVMHOST_PUSHGATEWAY_PERSISTENCE_INTERVAL;
    @GlobalProperty(name="kvmhost.pushgateway.metric.timeToLive", defaultValue = "60s")
    public static String  KVMHOST_PUSHGATEWAY_METRIC_TIME_TO_LIVE;

    @GlobalProperty(name="zwatch.retention_threshold_buffer", defaultValue = "10000")
    public static int RETENTION_THRESHOLD_BUFFER;

    @GlobalProperty(name="kvmhost.ipmi.webListenPort", defaultValue = "9290")
    public static String  KVMHOST_IPMI_LISTEN_PORT;

    @GlobalProperty(name="zwatch.system.alarm.uuid.modification", defaultValue = "false")
    public static boolean ZWATCH_SYSTEM_ALARM_UUID_MODIFICATION;
}
