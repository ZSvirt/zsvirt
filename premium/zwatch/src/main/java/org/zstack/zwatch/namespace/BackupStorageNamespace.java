package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.zwatch.alarm.AlarmVO;
import org.zstack.zwatch.alarm.EventSubscriptionVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.BackupStorageNamespaceEvent;
import org.zstack.zwatch.ruleengine.RuleEvaluationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.storage.backup.imagestore.ImageStoreSystemTags.*;
import static org.zstack.zwatch.alarm.AlarmSystemTags.ALARM_RESOURCE_NAME;
import static org.zstack.zwatch.alarm.EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_RESOURCE_NAME;
import static org.zstack.zwatch.utils.ParserUtils.getResourceUuid;

public class BackupStorageNamespace extends AbstractNamespace {
    public static final String NAME = "BackupStorage";

    public static final String DISASTER_RECOVERY_STORAGE_NAME_SPACE = "ZStack/DisasterRecoveryStorage";
    public static final String DISASTER_RECOVERY_STORAGE_NAME = "DisasterRecoveryStorage";

    @StaticInit
    static void staticInit() {
        new BackupStorageNamespaceEvent();
    }

    private static final List<Metric> metrics = new ArrayList<>();
    private static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> events = new ArrayList<>();

    public enum LabelNames {
        BackupStorageUuid,
        BackupStorageType,
        NetworkDeviceLetter,
        DiskDeviceLetter,
        CPUNum,
        Name,
    }

    public static final Metric BackupStorageInfo = new InfoMetric("BackupStorageInfo", metrics,
            LabelNames.BackupStorageUuid, LabelNames.Name
    );

    public static final Metric TotalAvailableCapacityInBytes = new ByteSizeMetric("TotalAvailableCapacityInBytes",
            metrics
    );
    public static final Metric TotalAvailableCapacityInPercent = new ByteSizeMetric("TotalAvailableCapacityInPercent",
            metrics
    );
    public static final Metric AvailableCapacityInBytes = new ByteSizeMetric("AvailableCapacityInBytes", metrics,
            LabelNames.BackupStorageUuid, LabelNames.BackupStorageType, LabelNames.Name
    );
    public static final Metric AvailableCapacityInPercent = new PercentMetric("AvailableCapacityInPercent", metrics,
            LabelNames.BackupStorageUuid, LabelNames.BackupStorageType, LabelNames.Name
    );
    public static final Metric TotalUsedCapacityInBytes = new ByteSizeMetric("TotalUsedCapacityInBytes", metrics);
    public static final Metric TotalUsedCapacityInPercent = new PercentMetric("TotalUsedCapacityInPercent", metrics);
    public static final Metric UsedCapacityInBytes = new ByteSizeMetric("UsedCapacityInBytes", metrics,
            LabelNames.BackupStorageUuid, LabelNames.BackupStorageType, LabelNames.Name
    );
    public static final Metric UsedCapacityInPercent = new PercentMetric("UsedCapacityInPercent", metrics,
            LabelNames.BackupStorageUuid, LabelNames.BackupStorageType, LabelNames.Name
    );
    public static final Metric TotalLockedCapacityInBytes = new ByteSizeMetric("TotalLockedCapacityInBytes", metrics);
    public static final Metric TotalLockedCapacityInPercent = new PercentMetric("TotalLockedCapacityInPercent",
            metrics
    );
    public static final Metric NetworkInBytes = new ByteRateMetric("NetworkInBytes", metrics,
            LabelNames.BackupStorageUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInBytes = new ByteRateMetric("NetworkAllInBytes", metrics,
            LabelNames.BackupStorageUuid
    );
    public static final Metric NetworkInPackets = new PacketRateMetric("NetworkInPackets", metrics,
            LabelNames.BackupStorageUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInPackets = new PacketRateMetric("NetworkAllInPackets", metrics,
            LabelNames.BackupStorageUuid
    );
    public static final Metric NetworkInErrors = new PacketRateMetric("NetworkInErrors", metrics,
            LabelNames.BackupStorageUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInErrors = new PacketRateMetric("NetworkAllInErrors", metrics,
            LabelNames.BackupStorageUuid
    );
    public static final Metric NetworkOutBytes = new ByteRateMetric("NetworkOutBytes", metrics,
            LabelNames.BackupStorageUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutBytes = new ByteRateMetric("NetworkAllOutBytes", metrics,
            LabelNames.BackupStorageUuid
    );
    public static final Metric NetworkOutPackets = new PacketRateMetric("NetworkOutPackets", metrics,
            LabelNames.BackupStorageUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutPackets = new PacketRateMetric("NetworkAllOutPackets", metrics,
            LabelNames.BackupStorageUuid
    );
    public static final Metric NetworkOutErrors = new PacketRateMetric("NetworkOutErrors", metrics,
            LabelNames.BackupStorageUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutErrors = new PacketRateMetric("NetworkAllOutErrors", metrics,
            LabelNames.BackupStorageUuid
    );
    public static final Metric CPUIdleUtilization = new PercentMetric("CPUIdleUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid, BackupStorageNamespace.LabelNames.CPUNum
    );
    public static final Metric CPUSystemUtilization = new PercentMetric("CPUSystemUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid, BackupStorageNamespace.LabelNames.CPUNum
    );
    public static final Metric CPUUserUtilization = new PercentMetric("CPUUserUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid, BackupStorageNamespace.LabelNames.CPUNum
    );
    public static final Metric CPUWaitUtilization = new PercentMetric("CPUWaitUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid, BackupStorageNamespace.LabelNames.CPUNum
    );
    public static final Metric CPUAllIdleUtilization = new PercentMetric("CPUAllIdleUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric CPUUsedUtilization = new PercentMetric("CPUUsedUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid, BackupStorageNamespace.LabelNames.CPUNum
    );
    public static final Metric CPUAllUsedUtilization = new PercentMetric("CPUAllUsedUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric CPUAverageUsedUtilization = new PercentMetric("CPUAverageUsedUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric CPUAverageUserUtilization = new PercentMetric("CPUAverageUserUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric CPUAverageWaitUtilization = new PercentMetric("CPUAverageWaitUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric CPUAverageSystemUtilization = new PercentMetric("CPUAverageSystemUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric CPUAverageIdleUtilization = new PercentMetric("CPUAverageIdleUtilization", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric MemoryFreeBytes = new ByteSizeMetric("MemoryFreeBytes", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid);
    public static final Metric MemoryFreeInPercent = new PercentMetric("MemoryFreeInPercent", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric MemoryUsedBytes = new ByteSizeMetric("MemoryUsedBytes", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid);
    public static final Metric MemoryUsedInPercent = new PercentMetric("MemoryUsedInPercent", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric DiskReadOps = new OperationRateMetric("DiskReadOps", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid,
            BackupStorageNamespace.LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllReadOps = new OperationRateMetric("DiskAllReadOps", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid);
    public static final Metric DiskWriteOps = new OperationRateMetric("DiskWriteOps", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid,
            BackupStorageNamespace.LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllWriteOps = new OperationRateMetric("DiskAllWriteOps", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );
    public static final Metric DiskReadBytes = new ByteRateMetric("DiskReadBytes", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid,
            BackupStorageNamespace.LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllReadBytes = new ByteRateMetric("DiskAllReadBytes", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid);
    public static final Metric DiskWriteBytes = new ByteRateMetric("DiskWriteBytes", metrics, BackupStorageNamespace.LabelNames.BackupStorageUuid,
            BackupStorageNamespace.LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllWriteBytes = new ByteRateMetric("DiskAllWriteBytes", metrics,
            BackupStorageNamespace.LabelNames.BackupStorageUuid
    );


    public BackupStorageNamespace() {
        super();
    }

    public BackupStorageNamespace(DatabaseDriver driver) {
        super(driver);
    }

    @Override
    public Metric getInfoMetric() {
        return BackupStorageInfo;
    }

    public enum EventLabelNames {
        Error,
        OldStatus,
        NewStatus,
    }

    public static final EventFamily BackupStorageDisconnected = new EventFamily("BackupStorageDisconnected", events,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily BackupStorageConnected = new EventFamily("BackupStorageConnected", events,
            EventLabelNames.OldStatus, EventLabelNames.NewStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m ->!disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return BackupStorageVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.BackupStorageUuid.toString();
    }

    @Override
    public boolean resourceEventIsMatch(String resourceUuid, String eventUuid) {
        if (resourceUuid == null) {
            return true;
        }
        List<String> resourceTags = getTags(resourceUuid, ImageStoreBackupStorageVO.class.getSimpleName());
        List<String> eventTags = getTags(eventUuid, EventSubscriptionVO.class.getSimpleName());
        if (resourceTags.contains(ALLOW_BACKUP_TOKEN)) {
            return true;
        }

        if (eventTags.stream().anyMatch(tag -> ALARM_RESOURCE_NAME.isMatch(tag))) {
            return resourceTags.contains(ONLY_FOR_BACKUP_TOKEN) || resourceTags.contains(IS_REMOTE_BACKUP_TOKEN);
        }

        return !resourceTags.contains(ONLY_FOR_BACKUP_TOKEN) && !resourceTags.contains(IS_REMOTE_BACKUP_TOKEN);
    }

    @Override
    public List<RuleEvaluationResult> filterRuleEvaluationResults(String alarmUuid, List<RuleEvaluationResult> results) {
        return results.stream().filter(result -> {
            String resourceUuid = getResourceUuid(result, getIdentityLabelName());
            if (resourceUuid == null) {
                return true;
            }
            return resourceAlarmIsMatch(resourceUuid, alarmUuid);
        }).collect(Collectors.toList());
    }

    @Override
    public boolean resourceAlarmIsMatch(String resourceUuid, String alarmUuid) {
        if (resourceUuid == null) {
            return true;
        }
        List<String> resourceTags = getTags(resourceUuid, ImageStoreBackupStorageVO.class.getSimpleName());
        List<String> alarmTags = getTags(alarmUuid, AlarmVO.class.getSimpleName());
        if (resourceTags.contains(ALLOW_BACKUP_TOKEN)) {
            return true;
        }

        if (alarmTags.stream().anyMatch(tag -> ALARM_RESOURCE_NAME.isMatch(tag))) {
            return resourceTags.contains(ONLY_FOR_BACKUP_TOKEN) || resourceTags.contains(IS_REMOTE_BACKUP_TOKEN);
        }

        return !resourceTags.contains(ONLY_FOR_BACKUP_TOKEN) && !resourceTags.contains(IS_REMOTE_BACKUP_TOKEN);
    }

    @Override
    public String getAlarmNameSpace(String resourceUuid, String alarmUuid, String namespace) {
        List<String> alarmTags = getTags(alarmUuid, AlarmVO.class.getSimpleName());
        if (alarmTags.stream().anyMatch(tag -> ALARM_RESOURCE_NAME.isMatch(tag))) {
            return DISASTER_RECOVERY_STORAGE_NAME_SPACE;
        }
        return namespace;
    }

    @Override
    public String getAlarmI18nMetric(String resourceUuid, String alarmUuid, String i18nMetric) {
        List<String> alarmTags = getTags(alarmUuid, AlarmVO.class.getSimpleName());
        if (alarmTags.stream().anyMatch(tag -> ALARM_RESOURCE_NAME.isMatch(tag))) {
            return i18nMetric.replace("镜像", "备份");
        }
        return i18nMetric;
    }

    @Override
    public String getEventNameSpace(String resourceUuid, String eventUuid, String namespace) {
        List<String> eventVOTags = getTags(eventUuid, EventSubscriptionVO.class.getSimpleName());
        if (eventVOTags.stream().anyMatch(tag -> EVENT_SUBSCRIPTION_RESOURCE_NAME.isMatch(tag))) {
            return DISASTER_RECOVERY_STORAGE_NAME_SPACE;
        }
        return namespace;
    }
}
