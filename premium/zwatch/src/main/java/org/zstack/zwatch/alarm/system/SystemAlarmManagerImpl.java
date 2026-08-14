package org.zstack.zwatch.alarm.system;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.sns.system.SNSSystemAlarmTopicManager;
import org.zstack.tag.SystemTagCreator;
import org.zstack.zwatch.ZWatchGlobalProperty;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.header.AlarmDatabasePrepareHandler;
import org.zstack.zwatch.namespace.*;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.zwatch.alarm.system.AlarmSystemTagUtils.*;
import static org.zstack.zwatch.namespace.BackupStorageNamespace.DISASTER_RECOVERY_STORAGE_NAME;

public class SystemAlarmManagerImpl implements SystemAlarmManager,
        PrepareDbInitialValueExtensionPoint,
        GlobalApiMessageInterceptor,
        AlarmDatabasePrepareHandler {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private SystemNamespace systemNamespace;
    @Autowired
    private HostNamespace hostNamespace;
    @Autowired
    private ClusterNamespace clusterNamespace;
    @Autowired
    private BackupStorageNamespace backupStorageNamespace;
    @Autowired
    private PrimaryStorageNamespace primaryStorageNamespace;
    @Autowired
    private ManagementNodeNamespace managementNodeNamespace;
    @Autowired
    private VRouterNamespace vRouterNamespace;
    @Autowired
    private VmNamespace vmNamespace;
    @Autowired
    private SNSNamespace snsNamespace;
    @Autowired
    private HaNamespace haNamespace;
    @Autowired
    private SchedulerNamespace schedulerNamespace;

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                PredefinedSystemAlarmBuilder builder = new PredefinedSystemAlarmBuilder();
                builder.uuid(DATA_DIR_CAPACITY_ALARM_UUID)
                        .name(DATA_DIR_CAPACITY_ALARM_NAME)
                        .nameCN(DATA_DIR_CAPACITY_ALARM_NAME_CN)
                        .metricName(SystemNamespace.ManagementServerDirUsedCapacityInPercent.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(70)
                        .period(60)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(systemNamespace.getName())
                        .label(new Label(SystemNamespace.LabelNames.DirPath.toString(), Label.Operator.Equal, CoreGlobalProperty.DATA_DIR))
                        .build();
                builder.reset();

                builder.uuid(HOST_ROOT_USED_CAPACITY_ALARM_UUID)
                        .name(HOST_ROOT_USED_CAPACITY_ALARM_NAME)
                        .nameCN(HOST_ROOT_USED_CAPACITY_ALARM_NAME_CN)
                        .metricName(HostNamespace.DiskRootUsedCapacityInPercent.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(600)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(hostNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(BACKUP_STORAGE_AVAILABLE_CAPACITY_ALARM_UUID)
                        .name(BACKUP_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME)
                        .nameCN(BACKUP_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME_CN)
                        .metricName(BackupStorageNamespace.AvailableCapacityInPercent.getName())
                        .comparisonOperator(ComparisonOperator.LessThan)
                        .threshold(20)
                        .period(600)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(backupStorageNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(PRIMARY_STORAGE_AVAILABLE_CAPACITY_ALARM_UUID)
                        .name(PRIMARY_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME)
                        .nameCN(PRIMARY_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME_CN)
                        .metricName(PrimaryStorageNamespace.AvailableCapacityInPercent.getName())
                        .comparisonOperator(ComparisonOperator.LessThan)
                        .threshold(20)
                        .period(600)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(primaryStorageNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(PRIMARY_STORAGE_PHYSICAL_AVAILABLE_CAPACITY_ALARM_UUID)
                        .name(PRIMARY_STORAGE_PHYSICAL_AVAILABLE_CAPACITY_ALARM_NAME)
                        .nameCN(PRIMARY_STORAGE_PHYSICAL_AVAILABLE_CAPACITY_ALARM_NAME_CN)
                        .metricName(PrimaryStorageNamespace.AvailablePhysicalCapacityInPercent.getName())
                        .comparisonOperator(ComparisonOperator.LessThan)
                        .threshold(20)
                        .period(600)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(primaryStorageNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(MANAGEMENT_NODE_DB_FENCER_IP_UNREACHABLE_ALARM_UUID)
                        .name(MANAGEMENT_NODE_DB_FENCER_IP_UNREACHABLE_ALARM_NAME)
                        .nameCN(MANAGEMENT_NODE_DB_FENCER_IP_UNREACHABLE_ALARM_NAME_CN)
                        .metricName(ManagementNodeNamespace.DbFencerIpReachable.getName())
                        .comparisonOperator(ComparisonOperator.LessThan)
                        .threshold(1)
                        .period((int) TimeUnit.MINUTES.toSeconds(10))
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(managementNodeNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(MANAGEMENT_NODE_DB_SYNCHRONIZATION_ALARM_UUID)
                        .name(MANAGEMENT_NODE_DB_SYNCHRONIZATION_ALARM_NAME)
                        .nameCN(MANAGEMENT_NODE_DB_SYNCHRONIZATION_ALARM_NAME_CN)
                        .metricName(ManagementNodeNamespace.TimeNeededToSyncDB.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThan)
                        .threshold(0)
                        .period((int) TimeUnit.HOURS.toSeconds(1))
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .namespace(managementNodeNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(HOST_CPU_AVERAGE_USED_UTILIZATION_ALARM_UUID)
                        .name(HOST_CPU_AVERAGE_USED_UTILIZATION_ALARM_NAME)
                        .nameCN(HOST_CPU_AVERAGE_USED_UTILIZATION_ALARM_NAME_CN)
                        .metricName(HostNamespace.CPUAverageUsedUtilization.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(300)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(hostNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(VM_CPU_AVERAGE_USED_UTILIZATION_ALARM_UUID)
                        .name(VM_CPU_AVERAGE_USED_UTILIZATION_ALARM_NAME)
                        .nameCN(VM_CPU_AVERAGE_USED_UTILIZATION_ALARM_NAME_CN)
                        .metricName(VmNamespace.CPUAverageUsedUtilization.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(300)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(vmNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(VM_CPU_AVERAGE_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_UUID)
                        .name(VM_CPU_AVERAGE_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_NAME)
                        .nameCN(VM_CPU_AVERAGE_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_NAME_CN)
                        .metricName(VmNamespace.OperatingSystemCPUAverageUsedUtilization.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(300)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(vmNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(VM_MEMORY_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_UUID)
                        .name(VM_MEMORY_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_NAME)
                        .nameCN(VM_MEMORY_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_NAME_CN)
                        .metricName(VmNamespace.OperatingSystemMemoryUsedPercent.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(300)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(vmNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(VM_MEMORY_USED_ALARM_UUID)
                        .name(VM_MEMORY_USED_ALARM_NAME)
                        .nameCN(VM_MEMORY_USED_ALARM_NAME_CN)
                        .metricName(VmNamespace.MemoryUsedInPercent.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(300)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(vmNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(HOST_MEMORY_USED_ALARM_UUID)
                        .name(HOST_MEMORY_USED_ALARM_NAME)
                        .nameCN(HOST_MEMORY_USED_ALARM_NAME_CN)
                        .metricName(HostNamespace.MemoryUsedInPercent.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period(300)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(hostNamespace.getName())
                        .build();
                builder.reset();

                builder.uuid(DISASTER_RECOVERY_STORAGE_AVAILABLE_CAPACITY_ALARM_UUID)
                        .name(DISASTER_RECOVERY_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME)
                        .nameCN(DISASTER_RECOVERY_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME_CN)
                        .metricName(BackupStorageNamespace.UsedCapacityInPercent.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(80)
                        .period((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(30))
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .namespace(backupStorageNamespace.getName())
                        .build();
                SystemTagCreator creator = AlarmSystemTags.ALARM_RESOURCE_NAME.newSystemTagCreator(DISASTER_RECOVERY_STORAGE_AVAILABLE_CAPACITY_ALARM_UUID);
                creator.setTagByTokens(map(e(AlarmSystemTags.NAME_TOKEN, DISASTER_RECOVERY_STORAGE_NAME)));
                creator.inherent = false;
                creator.ignoreIfExisting = true;
                creator.create();
                builder.reset();

                builder.uuid(HOST_CPU_TEMPERATURE_ALARM_UUID)
                        .name(HOST_CPU_TEMPERATURE_ALARM_NAME)
                        .nameCN(HOST_CPU_TEMPERATURE_ALARM_NAME_CN)
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(5))
                        .namespace(hostNamespace.getName())
                        .metricName(HostNamespace.CpuTemperature.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .threshold(90)
                        .period(300)
                        .repeatCount(-1)
                        .alarmType(AlarmType.Any)
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                builder.reset();

                builder.uuid(HOST_SSD_LIFE_LEFT_ALARM_UUID)
                        .name(HOST_SSD_LIFE_LEFT_ALARM_NAME)
                        .nameCN(HOST_SSD_LIFE_LEFT_ALARM_NAME_CN)
                        .namespace(hostNamespace.getName())
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(5))
                        .threshold(20)
                        .period(300)
                        .metricName(HostNamespace.SSDLifeLeft.getName())
                        .comparisonOperator(ComparisonOperator.LessThanOrEqualTo)
                        .alarmType(AlarmType.Any)
                        .repeatCount(-1)
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                builder.reset();

                builder.uuid(HOST_SSD_TEMPERATURE_ALARM_UUID)
                        .name(HOST_SSD_TEMPERATURE_ALARM_NAME)
                        .nameCN(HOST_SSD_TEMPERATURE_ALARM_NAME_CN)
                        .namespace(hostNamespace.getName())
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(5))
                        .threshold(70)
                        .period(300)
                        .metricName(HostNamespace.SSDTemperature.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .alarmType(AlarmType.Any)
                        .repeatCount(-1)
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                builder.reset();

                builder.uuid(HOST_GPU_TEMPERATURE_ALARM_NAME_UUID)
                        .name(HOST_GPU_TEMPERATURE_ALARM_NAME)
                        .nameCN(HOST_GPU_TEMPERATURE_ALARM_NAME_CN)
                        .namespace(hostNamespace.getName())
                        .repeatInterval((int) TimeUnit.MINUTES.toSeconds(5))
                        .threshold(80)
                        .period(300)
                        .metricName(HostNamespace.GpuTemperature.getName())
                        .comparisonOperator(ComparisonOperator.GreaterThanOrEqualTo)
                        .alarmType(AlarmType.Any)
                        .repeatCount(-1)
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                builder.reset();

                PredefinedEventSubscriptionBuilder eventBuilder = new PredefinedEventSubscriptionBuilder();

                eventBuilder.uuid(DISASTER_RECOVERY_STORAGE_DISCONNECT_EVENT_ALARM_UUID)
                        .name(DISASTER_RECOVERY_STORAGE_DISCONNECT_EVENT_ALARM_NAME)
                        .nameCN(DISASTER_RECOVERY_STORAGE_DISCONNECT_EVENT_ALARM_NAME_CN)
                        .eventName(BackupStorageNamespace.BackupStorageDisconnected.getName())
                        .namespace(backupStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                creator = EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_RESOURCE_NAME.newSystemTagCreator(DISASTER_RECOVERY_STORAGE_DISCONNECT_EVENT_ALARM_UUID);
                creator.setTagByTokens(map(e(AlarmSystemTags.NAME_TOKEN, DISASTER_RECOVERY_STORAGE_NAME)));
                creator.inherent = false;
                creator.ignoreIfExisting = true;
                creator.create();

                eventBuilder.reset();

                eventBuilder.uuid(DISASTER_RECOVERY_STORAGE_CONNECTED_EVENT_ALARM_UUID)
                        .name(DISASTER_RECOVERY_STORAGE_CONNECTED_EVENT_ALARM_NAME)
                        .nameCN(DISASTER_RECOVERY_STORAGE_CONNECTED_EVENT_ALARM_NAME_CN)
                        .eventName(BackupStorageNamespace.BackupStorageConnected.getName())
                        .namespace(backupStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                creator = EventSubscriptionSystemTags.EVENT_SUBSCRIPTION_RESOURCE_NAME.newSystemTagCreator(DISASTER_RECOVERY_STORAGE_CONNECTED_EVENT_ALARM_UUID);
                creator.setTagByTokens(map(e(AlarmSystemTags.NAME_TOKEN, DISASTER_RECOVERY_STORAGE_NAME)));
                creator.inherent = false;
                creator.ignoreIfExisting = true;
                creator.create();

                eventBuilder.reset();
                builder.reset();

                eventBuilder.uuid(SCHEDULER_JOB_GROUP_EXECUTED_RESULT_EVENT_ALARM_UUID)
                        .name(SCHEDULER_JOB_GROUP_EXECUTED_RESULT_EVENT_ALARM_NAME)
                        .nameCN(SCHEDULER_JOB_GROUP_EXECUTED_RESULT_EVENT_ALARM_NAME_CN)
                        .eventName(SchedulerNamespace.SchedulerJobGroupExecutedResult.getName())
                        .namespace(schedulerNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(BACKUP_STORAGE_DISCONNECT_EVENT_ALARM_UUID)
                        .name(BACKUP_STORAGE_DISCONNECT_EVENT_ALARM_NAME)
                        .nameCN(BACKUP_STORAGE_DISCONNECT_EVENT_ALARM_NAME_CN)
                        .eventName(BackupStorageNamespace.BackupStorageDisconnected.getName())
                        .namespace(backupStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(BACKUP_STORAGE_CONNECTED_EVENT_ALARM_UUID)
                        .name(BACKUP_STORAGE_CONNECTED_EVENT_ALARM_NAME)
                        .nameCN(BACKUP_STORAGE_CONNECTED_EVENT_ALARM_NAME_CN)
                        .eventName(BackupStorageNamespace.BackupStorageConnected.getName())
                        .namespace(backupStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(PRIMARY_STORAGE_DISCONNECT_EVENT_ALARM_UUID)
                        .name(PRIMARY_STORAGE_DISCONNECT_EVENT_ALARM_NAME)
                        .nameCN(PRIMARY_STORAGE_DISCONNECT_EVENT_ALARM_NAME_CN)
                        .eventName(PrimaryStorageNamespace.PrimaryStorageDisconnected.getName())
                        .namespace(primaryStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();

                eventBuilder.uuid(PRIMARY_STORAGE_CONNECTED_EVENT_ALARM_UUID)
                        .name(PRIMARY_STORAGE_CONNECTED_EVENT_ALARM_NAME)
                        .nameCN(PRIMARY_STORAGE_CONNECTED_EVENT_ALARM_NAME_CN)
                        .eventName(PrimaryStorageNamespace.PrimaryStorageConnected.getName())
                        .namespace(primaryStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(MANAGEMENT_NODE_LEFT_EVENT_ALARM_UUID)
                        .name(MANAGEMENT_NODE_LEFT_EVENT_ALARM_NAME)
                        .nameCN(MANAGEMENT_NODE_LEFT_EVENT_ALARM_NAME_CN)
                        .eventName(ManagementNodeNamespace.ManagementNodeLeft.getName())
                        .namespace(managementNodeNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(MANAGEMENT_NODE_JOIN_EVENT_ALARM_UUID)
                        .name(MANAGEMENT_NODE_JOIN_EVENT_ALARM_NAME)
                        .nameCN(MANAGEMENT_NODE_JOIN_EVENT_ALARM_NAME_CN)
                        .eventName(ManagementNodeNamespace.ManagementNodeJoin.getName())
                        .namespace(managementNodeNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_CONNECTED_ALARM_UUID)
                        .name(HOST_CONNECTED_ALARM_NAME)
                        .nameCN(HOST_CONNECTED_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostConnected.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_DISCONNECT_ALARM_UUID)
                        .name(HOST_DISCONNECT_ALARM_NAME)
                        .nameCN(HOST_DISCONNECT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostDisconnected.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(VM_CRASH_EVENT_ALARM_UUID)
                        .name(VM_CRASH_EVENT_ALARM_NAME)
                        .nameCN(VM_CRASH_EVENT_ALARM_NAME_CN)
                        .eventName(VmNamespace.VmCrash.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_NIC_STATUS_UP_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_NIC_STATUS_UP_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_NIC_STATUS_UP_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalNicStatusUp.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_NIC_STATUS_DOWN_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_NIC_STATUS_DOWN_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_NIC_STATUS_DOWN_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalNicStatusDown.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(VM_HA_START_EVENT_ALARM_UUID)
                        .name(VM_HA_START_EVENT_ALARM_NAME)
                        .nameCN(VM_HA_START_EVENT_ALARM_NAME_CN)
                        .eventName(VmAbstractNamespace.VMHAStarted.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(VM_STATE_IN_SHUTDOWN_EVENT_ALARM_UUID)
                        .name(VM_STATE_IN_SHUTDOWN_EVENT_ALARM_NAME)
                        .nameCN(VM_STATE_IN_SHUTDOWN_EVENT_ALARM_NAME_CN)
                        .eventName(VmAbstractNamespace.VMStateInShutdown.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_UUID)
                        .name(HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_NAME)
                        .nameCN(HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostUnknownVMDetected.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_UUID)
                        .name(MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_NAME)
                        .nameCN(MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_NAME_CN)
                        .eventName(HaNamespace.MigrateVMFailedWithHostMaintain.getName())
                        .namespace(haNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_UUID)
                        .name(PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_NAME)
                        .nameCN(PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_NAME_CN)
                        .eventName(PrimaryStorageNamespace.PrimaryStorageHostDisconnected.getName())
                        .namespace(primaryStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_UUID)
                        .name(SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME)
                        .nameCN(SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME_CN)
                        .eventName(PrimaryStorageNamespace.SharedBlockStateAbnormal.getName())
                        .namespace(primaryStorageNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_UUID)
                        .name(HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME)
                        .nameCN(HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostSharedBlockStateAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_UUID)
                        .name(FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_NAME)
                        .nameCN(FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.FaultMountPointOnHost.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(ABNORMAL_RUNNING_VM_STATE_CHANGE_EVENT_ALARM_UUID)
                        .name(ABNORMAL_RUNNING_VM_STATE_CHANGE_EVENT_ALARM_NAME)
                        .nameCN(ABNORMAL_RUNNING_VM_STATE_CHANGE_EVENT_ALARM_NAME_CN)
                        .eventName(VmNamespace.VmAbnormalLifeCycleDetected.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_UUID)
                        .name(CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_NAME)
                        .nameCN(CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_NAME_CN)
                        .eventName(ClusterNamespace.ClusterQemuVersionMismatch.getName())
                        .namespace(clusterNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(VM_INTERNAL_IP_CHANGED_EVENT_ALARM_UUID)
                        .name(VM_INTERNAL_IP_CHANGED_EVENT_ALARM_NAME)
                        .nameCN(VM_INTERNAL_IP_CHANGED_EVENT_ALARM_NAME_CN)
                        .eventName(VmNamespace.VMInternalIpChanged.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(VM_INTERNAL_IP_DUPLICATE_EVENT_ALARM_UUID)
                        .name(VM_INTERNAL_IP_DUPLICATE_EVENT_ALARM_NAME)
                        .nameCN(VM_INTERNAL_IP_DUPLICATE_EVENT_ALARM_NAME_CN)
                        .eventName(VmNamespace.VMInternalIpDuplicate.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(VM_INTERNAL_IP_RANGE_CONFLICT_EVENT_ALARM_UUID)
                        .name(VM_INTERNAL_IP_RANGE_CONFLICT_EVENT_ALARM_NAME)
                        .nameCN(VM_INTERNAL_IP_RANGE_CONFLICT_EVENT_ALARM_NAME_CN)
                        .eventName(VmNamespace.VMInternalIpRangeConflict.getName())
                        .namespace(vmNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_HBA_PORT_STATE_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_HBA_PORT_STATE_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_HBA_PORT_STATE_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostHbaPortStateAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .state(EventSubscriptionState.Enabled)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_MEMORY_ECC_ERROR_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_MEMORY_ECC_ERROR_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_MEMORY_ECC_ERROR_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalMemoryEccErrorTriggered.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_CPU_STATUS_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_CPU_STATUS_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_CPU_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalCpuStatusAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalMemoryStatusAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_DISK_STATUS_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_DISK_STATUS_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_DISK_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalDiskStatusAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();


                eventBuilder.uuid(HOST_PHYSICAL_DISK_INSERTED_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_DISK_INSERTED_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_DISK_INSERTED_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalDiskInsertTriggered.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Normal)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_DISK_REMOVED_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_DISK_REMOVED_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_DISK_REMOVED_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalDiskRemoveTriggered.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();
                
                eventBuilder.uuid(HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalGpuStatusAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalVGpuStatusAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalGpuRemoveTriggered.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PHYSICAL_RAID_STATUS_ABNORMAL_EVENT_ALARM_UUID)
                        .name(HOST_PHYSICAL_RAID_STATUS_ABNORMAL_EVENT_ALARM_NAME)
                        .nameCN(HOST_PHYSICAL_RAID_STATUS_ABNORMAL_EVENT_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostPhysicalRaidStateAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_HARDWARE_CHANGED_ALARM_UUID)
                        .name(HOST_HARDWARE_CHANGED_ALARM_NAME)
                        .nameCN(HOST_HARDWARE_CHANGED_ALARM_NAME_CN)
                        .eventName(HostNamespace.HostHardwareChanged.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Emergent)
                        .build();
                eventBuilder.reset();

                eventBuilder.uuid(HOST_PROCESS_PHYSICAL_MEMORY_USAGE_UUID)
                        .name(HOST_PROCESS_PHYSICAL_MEMORY_USAGE_NAME)
                        .nameCN(HOST_PROCESS_PHYSICAL_MEMORY_USAGE_NAME_CN)
                        .eventName(HostNamespace.HostProcessPhysicalMemoryUsageAbnormal.getName())
                        .namespace(hostNamespace.getName())
                        .emergencyLevel(EmergencyLevel.Important)
                        .build();
                eventBuilder.reset();

                List<AlarmVO> notSystemTagAlarms = sql("select alarm from AlarmVO alarm where alarm.uuid not in (select resourceUuid from SystemTagVO where resourceType='AlarmVO')",
                        AlarmVO.class)
                        .list();
                for (AlarmVO alarm : notSystemTagAlarms) {
                    String textToken = ACTIVE_ALARM_CN_SYSTEM_TAG.containsKey(alarm.getName())
                            ? ACTIVE_ALARM_CN_SYSTEM_TAG.get(alarm.getName())
                            : alarm.getName();
                    AlarmSystemTagUtils.persistSystemTagOfLanguage(alarm, AlarmSystemTags.CN, textToken);
                }

                List<EventSubscriptionVO> notSystemTagEvents = sql("select event from EventSubscriptionVO event where event.uuid not in (select resourceUuid from SystemTagVO where resourceType='EventSubscriptionVO')",
                        EventSubscriptionVO.class)
                        .list();
                for (EventSubscriptionVO event : notSystemTagEvents) {
                    // Because before the upgrade，Some versions(4.7.21) of Event have an empty name
                    if (StringUtils.isBlank(event.getName())) {
                        sql(EventSubscriptionVO.class)
                                .set(EventSubscriptionVO_.name, EVENT_SUBSCRIPTION_NAME_MAP.get(event.getEventName()))
                                .eq(EventSubscriptionVO_.uuid, event.getUuid())
                                .update();
                    }

                    String textToken = EVENT_SUBSCRIPTION_CN_SYSTEM_TAG_MAP.get(event.getEventName());
                    AlarmSystemTagUtils.persistSystemTagOfLanguage(event, EventSubscriptionSystemTags.CN, textToken);
                }

                flush();
            }
        }.execute();

        if (ZWatchGlobalProperty.ZWATCH_SYSTEM_ALARM_UUID_MODIFICATION) {
            new ZsvAlarmUuidModification().run();
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIDeleteAlarmMsg.class, APIRemoveActionFromAlarmMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteAlarmMsg) {
            if (((APIDeleteAlarmMsg) msg).getAlarmUuid().equals(DATA_DIR_CAPACITY_ALARM_UUID)) {
                throw new OperationFailureException(operr("alarm[uuid:%s] is a system alarm which cannot be deleted", DATA_DIR_CAPACITY_ALARM_UUID));
            }
        } else if (msg instanceof APIRemoveActionFromAlarmMsg) {
            validate((APIRemoveActionFromAlarmMsg) msg);
        }

        return msg;
    }

    private void validate(APIRemoveActionFromAlarmMsg msg) {
        if (msg.getActionUuid().equals(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID) && msg.getAlarmUuid().equals(DATA_DIR_CAPACITY_ALARM_UUID)) {
            throw new ApiMessageInterceptionException(operr("removing system topic[uuid:%s] from system alarm[uuid:%s] is forbidden",
                    SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID, DATA_DIR_CAPACITY_ALARM_UUID));
        }
    }
}
