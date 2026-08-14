package org.zstack.zwatch.alarm.system;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.vo.ResourceVO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.alarm.AlarmSystemTags;
import org.zstack.zwatch.alarm.AlarmVO;
import org.zstack.zwatch.alarm.EventSubscriptionSystemTags;
import org.zstack.zwatch.alarm.EventSubscriptionVO;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.zwatch.alarm.system.AlarmNameMappingConstant.*;
import static org.zstack.zwatch.alarm.system.SystemAlarmManager.*;
import static org.zstack.zwatch.namespace.BackupStorageNamespace.BackupStorageConnected;
import static org.zstack.zwatch.namespace.BackupStorageNamespace.BackupStorageDisconnected;
import static org.zstack.zwatch.namespace.ClusterNamespace.ClusterQemuVersionMismatch;
import static org.zstack.zwatch.namespace.HaNamespace.MigrateVMFailedWithHostMaintain;
import static org.zstack.zwatch.namespace.HostNamespace.*;
import static org.zstack.zwatch.namespace.ManagementNodeNamespace.ManagementNodeJoin;
import static org.zstack.zwatch.namespace.ManagementNodeNamespace.ManagementNodeLeft;
import static org.zstack.zwatch.namespace.PrimaryStorageNamespace.*;
import static org.zstack.zwatch.namespace.SNSNamespace.SendSmsFailed;
import static org.zstack.zwatch.namespace.SchedulerNamespace.*;
import static org.zstack.zwatch.namespace.VRouterNamespace.*;

public class AlarmSystemTagUtils {

     protected static final CLogger logger = Utils.getLogger(AlarmSystemTagUtils.class);

    /**
     * persist alarm of language SystemTag
     *
     * @param resource     alarm (AlarmVO or EventSubscriptionVO)
     * @param languageToken language (cn or en...)
     * @param textToken    text
     */
    public static void persistSystemTagOfLanguage(ResourceVO resource, String languageToken, String textToken) {
        if (resource == null) return;
        if (StringUtils.isBlank(textToken)) {
            logger.warn(String.format("Invalid textToken resourceUuid = %s languageToken = %s textToken = %s",
                    resource.getUuid(),
                    languageToken,
                    textToken));
            return;
        }

        SystemTagCreator creator;
        if (resource instanceof AlarmVO) {
            boolean exists = cleanUpMismatchedAlarmLanguageTags(resource.getUuid(), languageToken, textToken);
            if (!exists) {
                creator = AlarmSystemTags.ALARM_TEXT.newSystemTagCreator(resource.getUuid());
                creator.setTagByTokens(map(
                        e(AlarmSystemTags.LANGUAGE_TOKEN, languageToken),
                        e(AlarmSystemTags.TEXT_TOKEN, textToken)
                ));
                creator.inherent = false;
                creator.create();
            }
        } else if (resource instanceof EventSubscriptionVO) {
            boolean exists = cleanUpMismatchedSubscriptionLanguageTags(resource.getUuid(), languageToken, textToken);
            if (!exists) {
                creator = EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_TEXT.newSystemTagCreator(resource.getUuid());
                creator.setTagByTokens(map(
                        e(EventSubscriptionSystemTags.LANGUAGE_TOKEN, languageToken),
                        e(EventSubscriptionSystemTags.TEXT_TOKEN, textToken)
                ));
                creator.inherent = false;
                creator.create();
            }
        }
    }

    /**
     * return true if matched tag exists
     */
    private static boolean cleanUpMismatchedAlarmLanguageTags(String alarmUuid, String languageToken, String textToken) {
        List<String> tags = AlarmSystemTags.ALARM_TEXT.getTags(alarmUuid);
        if (tags.isEmpty()) {
            return false;
        }

        boolean exists = false;
        for (String tag : tags) {
            Map<String, String> tokens = AlarmSystemTags.ALARM_TEXT.getTokensByTag(tag);
            String language = tokens.get(AlarmSystemTags.LANGUAGE_TOKEN);
            if (!language.equals(languageToken)) {
                continue;
            }

            String text = tokens.get(AlarmSystemTags.TEXT_TOKEN);
            if (text.equals(textToken)) {
                exists = true;
            } else {
                AlarmSystemTags.ALARM_TEXT.delete(alarmUuid, tag);
            }
        }

        return exists;
    }

    /**
     * return true if matched tag exists
     */
    private static boolean cleanUpMismatchedSubscriptionLanguageTags(String subscriptionUuid, String languageToken, String textToken) {
        List<String> tags = EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_TEXT.getTags(subscriptionUuid);
        if (tags.isEmpty()) {
            return false;
        }

        boolean exists = false;
        for (String tag : tags) {
            Map<String, String> tokens = EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_TEXT.getTokensByTag(tag);
            String language = tokens.get(EventSubscriptionSystemTags.LANGUAGE_TOKEN);
            if (!language.equals(languageToken)) {
                continue;
            }

            String text = tokens.get(EventSubscriptionSystemTags.TEXT_TOKEN);
            if (text.equals(textToken)) {
                exists = true;
            } else {
                EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_TEXT.delete(subscriptionUuid, tag);
            }
        }
        return exists;
    }

    public static final Map<String, String> EVENT_SUBSCRIPTION_NAME_MAP = map(
            e(BackupStorageDisconnected.getName(), BACKUP_STORAGE_DISCONNECT_EVENT_ALARM_NAME),
            e(BackupStorageConnected.getName(), BACKUP_STORAGE_CONNECTED_EVENT_ALARM_NAME),
            e(PrimaryStorageDisconnected.getName(), PRIMARY_STORAGE_DISCONNECT_EVENT_ALARM_NAME),
            e(PrimaryStorageConnected.getName(), PRIMARY_STORAGE_CONNECTED_EVENT_ALARM_NAME),
            e(ManagementNodeLeft.getName(), MANAGEMENT_NODE_LEFT_EVENT_ALARM_NAME),
            e(ManagementNodeJoin.getName(), MANAGEMENT_NODE_JOIN_EVENT_ALARM_NAME),
            e(HostConnected.getName(), HOST_CONNECTED_ALARM_NAME),
            e(HostDisconnected.getName(), HOST_DISCONNECT_ALARM_NAME),
            e(VmCrash.getName(), VM_CRASH_EVENT_ALARM_NAME),
            e(HostPhysicalNicStatusUp.getName(), HOST_PHYSICAL_NIC_STATUS_UP_EVENT_ALARM_NAME),
            e(HostPhysicalNicStatusDown.getName(), HOST_PHYSICAL_NIC_STATUS_DOWN_EVENT_ALARM_NAME),
            e(VMHAStarted.getName(), VM_HA_START_EVENT_ALARM_NAME),
            e(VMStateInShutdown.getName(), VM_STATE_IN_SHUTDOWN_EVENT_ALARM_NAME),
            e(HostUnknownVMDetected.getName(), HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_NAME),
            e(MigrateVMFailedWithHostMaintain.getName(), MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_NAME),
            e(PrimaryStorageHostDisconnected.getName(), PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_NAME),
            e(FaultMountPointOnHost.getName(), FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_NAME),
            e(VmAbnormalLifeCycleDetected.getName(), ABNORMAL_RUNNING_VM_STATE_CHANGE_EVENT_ALARM_NAME),
            e(ClusterQemuVersionMismatch.getName(), CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_NAME),
            e(VMInternalIpChanged.getName(), VM_INTERNAL_IP_CHANGED_EVENT_ALARM_NAME),
            e(VMInternalIpDuplicate.getName(), VM_INTERNAL_IP_DUPLICATE_EVENT_ALARM_NAME),
            e(VMInternalIpRangeConflict.getName(), VM_INTERNAL_IP_RANGE_CONFLICT_EVENT_ALARM_NAME),
            e(HostHardwareChanged.getName(), HOST_HARDWARE_CHANGED_ALARM_NAME),

            e(HostPhysicalMemoryEccErrorTriggered.getName(), HOST_PHYSICAL_MEMORY_ECC_ERROR_EVENT_ALARM_NAME),
            e(HostPhysicalMemoryStatusAbnormal.getName(), HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL_EVENT_ALARM_NAME),
            e(HostPhysicalDiskRemoveTriggered.getName(), HOST_PHYSICAL_DISK_REMOVED_EVENT_ALARM_NAME),
            e(HostPhysicalDiskInsertTriggered.getName(), HOST_PHYSICAL_DISK_INSERTED_EVENT_ALARM_NAME),
            e(HostPhysicalDiskStatusAbnormal.getName(), HOST_PHYSICAL_DISK_STATUS_ABNORMAL_EVENT_ALARM_NAME),
            e(HostSharedBlockStateAbnormal.getName(), HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME),
            e(SharedBlockStateAbnormal.getName(), SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME),
            e(HostPhysicalCpuStatusAbnormal.getName(), HOST_PHYSICAL_CPU_STATUS_ABNORMAL_EVENT_ALARM_NAME),
            e(HostPhysicalPowerSupplyStatusAbnormal.getName(), HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL_EVENT_ALARM_NAME),
            e(HostPhysicalGpuStatusAbnormal.getName(), HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_NAME),
            e(HostPhysicalVGpuStatusAbnormal.getName(), HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_NAME),
            e(HostPhysicalGpuRemoveTriggered.getName(), HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_NAME),
            e(HostHbaPortStateAbnormal.getName(), HOST_HBA_PORT_STATE_ABNORMAL_EVENT_ALARM_NAME),
            e(HostProcessPhysicalMemoryUsageAbnormal.getName(), HOST_PROCESS_PHYSICAL_MEMORY_USAGE_NAME),

            e(VMStateChangedOnHost.getName(), VM.VM_STATE_CHANGED_ON_HOST_NAME),

            e(SchedulerJobGroupExecutedResult.getName(), Scheduler.SCHEDULER_JOB_GROUP_EXECUTED_RESULT_NAME)

    );

    public static final Map<String, String> EVENT_SUBSCRIPTION_CN_SYSTEM_TAG_MAP = map(
            e(BackupStorageDisconnected.getName(), BACKUP_STORAGE_DISCONNECT_EVENT_ALARM_NAME_CN),
            e(BackupStorageConnected.getName(), BACKUP_STORAGE_CONNECTED_EVENT_ALARM_NAME_CN),
            e(PrimaryStorageDisconnected.getName(), PRIMARY_STORAGE_DISCONNECT_EVENT_ALARM_NAME_CN),
            e(PrimaryStorageConnected.getName(), PRIMARY_STORAGE_CONNECTED_EVENT_ALARM_NAME_CN),
            e(ManagementNodeLeft.getName(), MANAGEMENT_NODE_LEFT_EVENT_ALARM_NAME_CN),
            e(ManagementNodeJoin.getName(), MANAGEMENT_NODE_JOIN_EVENT_ALARM_NAME_CN),
            e(HostConnected.getName(), HOST_CONNECTED_ALARM_NAME_CN),
            e(HostDisconnected.getName(), HOST_DISCONNECT_ALARM_NAME_CN),
            e(VmCrash.getName(), VM_CRASH_EVENT_ALARM_NAME_CN),
            e(HostPhysicalNicStatusUp.getName(), HOST_PHYSICAL_NIC_STATUS_UP_EVENT_ALARM_NAME_CN),
            e(HostPhysicalNicStatusDown.getName(), HOST_PHYSICAL_NIC_STATUS_DOWN_EVENT_ALARM_NAME_CN),
            e(VMHAStarted.getName(), VM_HA_START_EVENT_ALARM_NAME_CN),
            e(VMStateInShutdown.getName(), VM_STATE_IN_SHUTDOWN_EVENT_ALARM_NAME_CN),
            e(HostUnknownVMDetected.getName(), HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_NAME_CN),
            e(MigrateVMFailedWithHostMaintain.getName(), MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_NAME_CN),
            e(PrimaryStorageHostDisconnected.getName(), PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_NAME_CN),
            e(FaultMountPointOnHost.getName(), FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_NAME_CN),
            e(VmAbnormalLifeCycleDetected.getName(), ABNORMAL_RUNNING_VM_STATE_CHANGE_EVENT_ALARM_NAME_CN),
            e(ClusterQemuVersionMismatch.getName(), CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_NAME_CN),
            e(VMInternalIpChanged.getName(), VM_INTERNAL_IP_CHANGED_EVENT_ALARM_NAME_CN),
            e(VMInternalIpDuplicate.getName(), VM_INTERNAL_IP_DUPLICATE_EVENT_ALARM_NAME_CN),
            e(VMInternalIpRangeConflict.getName(), VM_INTERNAL_IP_RANGE_CONFLICT_EVENT_ALARM_NAME_CN),
            e(HostHardwareChanged.getName(), HOST_HARDWARE_CHANGED_ALARM_NAME_CN),

            e(HostPhysicalMemoryEccErrorTriggered.getName(), HOST_PHYSICAL_MEMORY_ECC_ERROR_EVENT_ALARM_NAME_CN),
            e(HostPhysicalMemoryStatusAbnormal.getName(), HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostPhysicalDiskRemoveTriggered.getName(), HOST_PHYSICAL_DISK_REMOVED_EVENT_ALARM_NAME_CN),
            e(HostPhysicalDiskInsertTriggered.getName(), HOST_PHYSICAL_DISK_INSERTED_EVENT_ALARM_NAME_CN),
            e(HostPhysicalDiskStatusAbnormal.getName(), HOST_PHYSICAL_DISK_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostSharedBlockStateAbnormal.getName(), HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME_CN),
            e(SharedBlockStateAbnormal.getName(), SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME_CN),
            e(HostPhysicalCpuStatusAbnormal.getName(), HOST_PHYSICAL_CPU_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostPhysicalPowerSupplyStatusAbnormal.getName(), HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostPhysicalGpuRemoveTriggered.getName(), HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_NAME_CN),
            e(HostPhysicalGpuStatusAbnormal.getName(), HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostPhysicalVGpuStatusAbnormal.getName(), HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostHbaPortStateAbnormal.getName(), HOST_HBA_PORT_STATE_ABNORMAL_EVENT_ALARM_NAME_CN),
            e(HostProcessPhysicalMemoryUsageAbnormal.getName(), HOST_PROCESS_PHYSICAL_MEMORY_USAGE_NAME_CN),

            e(VMStateChangedOnHost.getName(), VM.VM_STATE_CHANGED_ON_HOST_NAME_CN),

            e(SchedulerJobGroupExecutedResult.getName(), Scheduler.SCHEDULER_JOB_GROUP_EXECUTED_RESULT_NAME_CN)
    );

    public static final Map<String, String> ACTIVE_ALARM_CN_SYSTEM_TAG = map(
            e(ActiveAlarm.HOST_CPU_AVERAGE_USED_UTILIZATION, ActiveAlarm.HOST_CPU_AVERAGE_USED_UTILIZATION_CN),
            e(ActiveAlarm.HOST_DISK_ALL_USED_CAPACITY_IN_PERCENT, ActiveAlarm.HOST_DISK_ALL_USED_CAPACITY_IN_PERCENT_CN),
            e(ActiveAlarm.HOST_MEMORY_USED_IN_PERCENT, ActiveAlarm.HOST_MEMORY_USED_IN_PERCENT_CN),

            e(ActiveAlarm.VM_CPU_AVERAGE_USED_UTILIZATION, ActiveAlarm.VM_CPU_AVERAGE_USED_UTILIZATION_CN),
            e(ActiveAlarm.VM_DISK_ALL_USED_CAPACITY_IN_PERCENT, ActiveAlarm.VM_DISK_ALL_USED_CAPACITY_IN_PERCENT_CN),
            e(ActiveAlarm.VM_MEMORY_USED_IN_PERCENT, ActiveAlarm.VM_MEMORY_USED_IN_PERCENT_CN),
            e(ActiveAlarm.VM_OPERATING_SYSTEM_CPU_AVERAGE_USED_UTILIZATION, ActiveAlarm.VM_OPERATING_SYSTEM_CPU_AVERAGE_USED_UTILIZATION_CN),
            e(ActiveAlarm.VM_OPERATING_SYSTEM_MEMORY_USED_PERCENT, ActiveAlarm.VM_OPERATING_SYSTEM_MEMORY_USED_PERCENT_CN),

            e(ActiveAlarm.VROUTER_CPU_AVERAGE_USED_UTILIZATION, ActiveAlarm.VROUTER_CPU_AVERAGE_USED_UTILIZATION_CN),
            e(ActiveAlarm.VROUTER_DISK_ALL_USED_CAPACITY_IN_PERCENT, ActiveAlarm.VROUTER_DISK_ALL_USED_CAPACITY_IN_PERCENT_CN),

            e(ActiveAlarm.VROUTER_MEMORY_USED_PERCENT, ActiveAlarm.VROUTER_MEMORY_USED_PERCENT_CN)

    );
}
