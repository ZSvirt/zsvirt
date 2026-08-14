package org.zstack.zwatch;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class ZWatchGlobalConfig {
    public static final String CATEGORY = "zwatch";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig EVALUATION_INTERVAL = new GlobalConfig(CATEGORY, "evaluation.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig EVALUATION_THREADS_NUM = new GlobalConfig(CATEGORY, "evaluation.threadNum");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig ALARM_REPEAT_INTERVAL = new GlobalConfig(CATEGORY, "alarm.repeatInterval");
    @GlobalConfigValidation
    public static GlobalConfig MANAGEMENT_NODE_DIR_TO_MONITOR = new GlobalConfig(CATEGORY, "managementServerDirectoriesToMonitor");
    @GlobalConfigValidation(min = 9) // minimal 10s
    public static GlobalConfig SCRAPE_INTERVAL = new GlobalConfig(CATEGORY, "scrape.interval");
    @GlobalConfigValidation(min = 10) // minimal 10s
    public static GlobalConfig MAXIMUM_RESOLUTION_SAMPLES = new GlobalConfig(CATEGORY, "resolution.defaultAlgorithm.maxSamples");

    @GlobalConfigValidation(min = -1)
    public static GlobalConfig COUNT_EVENTDATA_AND_ALARMDATA_CACHE_EXPIRE_TIME = new GlobalConfig(CATEGORY, "countCacheExpireSecTime");

    @GlobalConfigValidation(min = -1)
    public static GlobalConfig MINIMUM_COUNT_AMOUNT_ALLOWED_ADDED_TO_CACHE = new GlobalConfig(CATEGORY, "minimumCountAmountAllowedAddedToCache");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "60", type = Long.class, description = "metric push interval in seconds")
    public static GlobalConfig METRIC_PUSH_INTERVAL = new GlobalConfig(CATEGORY, "pushMetric.interval");

    @GlobalConfigValidation(min = 100)
    @GlobalConfigDef(defaultValue = "200", type = Long.class, description = "the number of metric per batch")
    public static GlobalConfig METRIC_PUSH_SLICE_SIZE = new GlobalConfig(CATEGORY, "pushMetric.sliceSize");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable thirdpartyAlert")
    public static GlobalConfig THIRDPARTY_ALERT_ENABLE = new GlobalConfig(CATEGORY, "thirdpartyAlert.enable");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "60", type = Long.class, description = "the interval to pull third-party platform alert, in seconds")
    public static GlobalConfig THIRDPARTY_ALERT_PULL_INTERVAL = new GlobalConfig(CATEGORY, "thirdpartyAlert.pullInterval");

    @GlobalConfigValidation(min = 60)
    @GlobalConfigDef(defaultValue = "7200", type = Long.class, description = "the duration to query third-party platform alert, in seconds")
    public static GlobalConfig THIRDPARTY_ALERT_QUERY_DURATION = new GlobalConfig(CATEGORY, "thirdpartyAlert.queryDuration");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "10000", type = Long.class, description = "the number of third-party alert retention by per third-party platform")
    public static GlobalConfig THIRDPARTY_ALERT_RETENTION_AMOUNT = new GlobalConfig(CATEGORY, "thirdpartyAlert.alert.retention.amount");

    @GlobalConfigValidation(min = 60)
    @GlobalConfigDef(defaultValue = "21600", type = Long.class, description = "trigger alarm time period, in seconds")
    public static GlobalConfig THIRDPARTY_ALERT_ALARM_TIME_PERIOD = new GlobalConfig(CATEGORY, "thirdpartyAlert.alarmTimePeriod");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable case sensitive search in audit")
    public static GlobalConfig CASE_SENSITIVE_SEARCH = new GlobalConfig(CATEGORY, "case.sensitive.search");

    @GlobalConfigValidation(min = 1, max = 1000)
    @GlobalConfigDef(defaultValue = "500", type = Long.class, description = "the maximum number of instance in a monitor group")
    public static GlobalConfig MONITOR_GROUP_MAXIMUM_INSTANCE_NUM = new GlobalConfig(CATEGORY, "monitorGroup.maximumInstanceNum");

    @GlobalConfigDef(defaultValue = "1800", type = Long.class, description = "the interval to cleanup monitor group instances, in seconds")
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig MONITOR_GROUP_CLEANUP_INSTANCE_INTERVAL = new GlobalConfig(CATEGORY, "monitorGroup.cleanupInstanceInterval");

    @GlobalConfigValidation(min = 1, max = 100)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "the maximum number of rule in a monitor template")
    public static GlobalConfig MONITOR_TEMPLATE_MAXIMUM_RULE_NUM = new GlobalConfig(CATEGORY, "monitorTemplate.maximumRuleNum");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(defaultValue = "50", type = Long.class, description = "the limit of pagination to query third-party platform alert")
    public static GlobalConfig THIRDPARTY_ALERT_QUERY_PAGINATION_LIMIT = new GlobalConfig(CATEGORY, "thirdpartyAlert.queryPaginationLimit");

    @GlobalConfigDef(defaultValue = "-1", type = Long.class, description = "data retention time,in days")
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig AUDIT_RETENTION_DURATION = new GlobalConfig(CATEGORY, "audit.retention.duration");

    @GlobalConfigDef(defaultValue = "-1", type = Long.class, description = "data retention quantity")
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig AUDIT_RETENTION_THRESHOLD = new GlobalConfig(CATEGORY, "audit.retention.threshold");

    @GlobalConfigDef(defaultValue = "90", type = Long.class, description = "data retention time,in days")
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig ALARM_RETENTION_DURATION = new GlobalConfig(CATEGORY, "alarm.retention.duration");

    @GlobalConfigDef(defaultValue = "-1", type = Long.class, description = "data retention quantity")
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig ALARM_RETENTION_THRESHOLD = new GlobalConfig(CATEGORY, "alarm.retention.threshold");

    @GlobalConfigDef(defaultValue = "90", type = Long.class, description = "data retention time,in days")
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig EVENT_RETENTION_DURATION = new GlobalConfig(CATEGORY, "event.retention.duration");

    @GlobalConfigDef(defaultValue = "-1", type = Long.class, description = "data retention quantity")
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig EVENT_RETENTION_THRESHOLD = new GlobalConfig(CATEGORY, "event.retention.threshold");

}
