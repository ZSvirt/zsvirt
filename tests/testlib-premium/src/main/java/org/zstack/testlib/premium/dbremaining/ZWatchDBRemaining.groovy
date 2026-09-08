package org.zstack.testlib.premium.dbremaining

import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.sns.*
import org.zstack.sns.platform.http.SNSHttpEndpointVO
import org.zstack.sns.system.SNSSystemAlarmTopicManager
import org.zstack.testlib.dbremaining.AllowedDBRemaining
import org.zstack.zwatch.alarm.*
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateVO
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmVO
import org.zstack.zwatch.alarm.system.SystemAlarmManager

import static org.zstack.sns.system.SystemHTTPTopicAndEndpointCreator.systemHttpEndpointName

class ZWatchDBRemaining extends AllowedDBRemaining {
    @Deprecated
    private static final String TICKET_TOPIC_UUID = "0db3c1800dd64df5a2c3a34c008e37b7"

    public static HashSet<String> systemAlarmNames = new HashSet<>([
        SystemAlarmManager.DATA_DIR_CAPACITY_ALARM_NAME,
        SystemAlarmManager.PRIMARY_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME,
        SystemAlarmManager.BACKUP_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME,
        SystemAlarmManager.HOST_ROOT_USED_CAPACITY_ALARM_NAME,
        SystemAlarmManager.PRIMARY_STORAGE_PHYSICAL_AVAILABLE_CAPACITY_ALARM_NAME,
        SystemAlarmManager.MANAGEMENT_NODE_DB_SYNCHRONIZATION_ALARM_NAME,
        SystemAlarmManager.MANAGEMENT_NODE_DB_FENCER_IP_UNREACHABLE_ALARM_NAME,
        SystemAlarmManager.HOST_CPU_AVERAGE_USED_UTILIZATION_ALARM_NAME,
        SystemAlarmManager.VM_CPU_AVERAGE_USED_UTILIZATION_ALARM_NAME,
        SystemAlarmManager.VM_CPU_AVERAGE_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_NAME,
        SystemAlarmManager.VM_MEMORY_USED_UTILIZATION_IN_GUEST_TOOLS_ALARM_NAME,
        SystemAlarmManager.VM_MEMORY_USED_ALARM_NAME,
        SystemAlarmManager.HOST_CPU_TEMPERATURE_ALARM_NAME,
        SystemAlarmManager.HOST_SSD_LIFE_LEFT_ALARM_NAME,
        SystemAlarmManager.HOST_SSD_TEMPERATURE_ALARM_NAME,
        SystemAlarmManager.HOST_MEMORY_USED_ALARM_NAME,
        SystemAlarmManager.HOST_GPU_TEMPERATURE_ALARM_NAME,
        SystemAlarmManager.SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PROCESS_PHYSICAL_MEMORY_USAGE_NAME,
        SystemAlarmManager.DISASTER_RECOVERY_STORAGE_AVAILABLE_CAPACITY_ALARM_NAME,
    ])

    public static HashSet<String> eventSubscriptionNames = new HashSet<>([
        SystemAlarmManager.PRIMARY_STORAGE_DISCONNECT_EVENT_ALARM_NAME,
        SystemAlarmManager.PRIMARY_STORAGE_CONNECTED_EVENT_ALARM_NAME,
        SystemAlarmManager.BACKUP_STORAGE_DISCONNECT_EVENT_ALARM_NAME,
        SystemAlarmManager.BACKUP_STORAGE_CONNECTED_EVENT_ALARM_NAME,
        SystemAlarmManager.MANAGEMENT_NODE_LEFT_EVENT_ALARM_NAME,
        SystemAlarmManager.MANAGEMENT_NODE_JOIN_EVENT_ALARM_NAME,
        SystemAlarmManager.MANAGEMENT_NODE_DB_FENCER_IP_UNREACHABLE_ALARM_NAME,
        SystemAlarmManager.MANAGEMENT_NODE_DB_SYNCHRONIZATION_ALARM_NAME,
        SystemAlarmManager.HOST_DISCONNECT_ALARM_NAME,
        SystemAlarmManager.HOST_CONNECTED_ALARM_NAME,
        SystemAlarmManager.VM_CRASH_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_NIC_STATUS_UP_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_NIC_STATUS_DOWN_EVENT_ALARM_NAME,
        SystemAlarmManager.VM_HA_START_EVENT_ALARM_NAME,
        SystemAlarmManager.VM_STATE_IN_SHUTDOWN_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_NAME,
        SystemAlarmManager.MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_NAME,
        SystemAlarmManager.PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_NAME,
        SystemAlarmManager.FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_NAME,
        SystemAlarmManager.ABNORMAL_RUNNING_VM_STATE_CHANGE_EVENT_ALARM_NAME,
        SystemAlarmManager.CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_NAME,
        SystemAlarmManager.VM_INTERNAL_IP_CHANGED_EVENT_ALARM_NAME,
        SystemAlarmManager.VM_INTERNAL_IP_DUPLICATE_EVENT_ALARM_NAME,
        SystemAlarmManager.VM_INTERNAL_IP_RANGE_CONFLICT_EVENT_ALARM_NAME,
        SystemAlarmManager.SECURITY_MACHINE_STATE_CHANGE_EVENT_ALARM_NAME,
        SystemAlarmManager.SECRET_RESOURCE_POOL_STATUS_CHANGE_EVENT_ALARM_NAME,
        SystemAlarmManager.KEY_PROVIDER_DEFAULT_SERVICE_UNAVAILABLE_EVENT_ALARM_NAME,
        SystemAlarmManager.KEY_PROVIDER_DEFAULT_SERVICE_RECOVERED_EVENT_ALARM_NAME,
        SystemAlarmManager.KEY_PROVIDER_CERTIFICATE_EXPIRING_EVENT_ALARM_NAME,
        SystemAlarmManager.KEY_PROVIDER_CERTIFICATE_RECOVERED_EVENT_ALARM_NAME,
        SystemAlarmManager.KEY_PROVIDER_RESOURCE_SERVICE_UNAVAILABLE_EVENT_ALARM_NAME,
        SystemAlarmManager.KEY_PROVIDER_RESOURCE_SERVICE_RECOVERED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_MEMORY_ECC_ERROR_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_CPU_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_DISK_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_DISK_INSERTED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_DISK_REMOVED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_HARDWARE_CHANGED_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PHYSICAL_RAID_STATUS_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_HBA_PORT_STATE_ABNORMAL_EVENT_ALARM_NAME,
        SystemAlarmManager.SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_SHARED_BLOCK_STATE_CHANGED_EVENT_ALARM_NAME,
        SystemAlarmManager.HOST_PROCESS_PHYSICAL_MEMORY_USAGE_NAME,
        SystemAlarmManager.SCHEDULER_JOB_GROUP_EXECUTED_RESULT_EVENT_ALARM_NAME,
        SystemAlarmManager.DISASTER_RECOVERY_STORAGE_CONNECTED_EVENT_ALARM_NAME,
        SystemAlarmManager.DISASTER_RECOVERY_STORAGE_DISCONNECT_EVENT_ALARM_NAME,
    ])

    @Override
    void remaining() {
        table {
            tableVOClass = SNSTopicVO.class
            checker = { List<SNSTopicVO> vos ->
                return vos.findAll { it.name != "api" && it.name != "system-alarm" && it.name != "topic-for-ticket" }
            }
        }

        table {
            tableVOClass = SNSSubscriberVO.class
            checker = { List<SNSSubscriberVO> vos ->
                return vos.findAll { it.topicUuid != SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID &&
                        it.topicUuid != TICKET_TOPIC_UUID }
            }
        }

        table {
            tableVOClass = SNSApplicationEndpointVO.class
            checker = { List<SNSApplicationEndpointVO> vos ->
                return vos.findAll { it.name != "system-alarm-endpoint" && it.name != systemHttpEndpointName }
            }
        }

        table {
            tableVOClass = SNSHttpEndpointVO.class
            checker = { List<SNSHttpEndpointVO> vos ->
                return vos.findAll { it.name != "system-alarm-endpoint" && it.name != systemHttpEndpointName }
            }
        }

        table {
            tableVOClass = SNSApplicationPlatformVO.class

            checker = { List<SNSApplicationPlatformVO> vos ->
                return vos.findAll { it.name != "system" }
            }
        }

        table {
            tableVOClass = AlarmVO.class

            checker = { List<AlarmVO> vos ->
                return vos.findAll { !systemAlarmNames.contains(it.name) }
            }
        }


        table {
            tableVOClass = EventSubscriptionVO.class

            checker = { List<EventSubscriptionVO> vos ->
                return vos.findAll { vo -> !eventSubscriptionNames.contains(vo.name) }
            }
        }

        table {
            tableVOClass = ActiveAlarmVO.class

            checker = { List<ActiveAlarmVO> vos ->
                return vos.findAll { vo -> true }
            }
        }

        table {
            tableVOClass = EventSubscriptionActionVO.class

            checker = { List<EventSubscriptionActionVO> vos ->
                List<EventSubscriptionVO> evos = Q.New(EventSubscriptionVO.class)
                        .in(EventSubscriptionVO_.@name, eventSubscriptionNames)
                        .list()
                if (!evos.isEmpty()) {
                    return vos.findAll { vo -> !evos.any { it.uuid == vo.subscriptionUuid }}
                } else {
                    return vos
                }
            }
        }

        table {
            tableVOClass = AccountResourceRefVO.class

            checker = { List<AccountResourceRefVO> vos ->
                List resourceTypes = [SNSApplicationEndpointVO.class.simpleName, SNSTopicVO.class.simpleName]

                return vos.findAll {
                    it.type == AccessLevel.SharePublic && !resourceTypes.contains(it.resourceType)
                }
            }
        }

        table {
            tableVOClass = AlarmActionVO.class

            checker = { List<AlarmActionVO> vos ->
                List<AlarmVO> avos = Q.New(AlarmVO.class)
                        .in(AlarmVO_.@name, systemAlarmNames)
                        .list()
                if (!avos.isEmpty()) {
                    return vos.findAll { vo -> !avos.any { it.uuid == vo.alarmUuid } }
                } else {
                    return vos
                }
            }
        }

        table {
            tableVOClass = AlarmLabelVO.class

            checker = { List<AlarmLabelVO> vos ->
                AlarmVO avo = Q.New(AlarmVO.class).eq(AlarmVO_.@name, SystemAlarmManager.DATA_DIR_CAPACITY_ALARM_NAME).find()
                if (avo != null) {
                    return vos.findAll { it.alarmUuid != avo.uuid }
                } else {
                    return vos
                }
            }
        }

        table {
            tableVOClass = AccountResourceRefVO.class

            checker = { List<AccountResourceRefVO> vos ->

                def allNames = new HashSet<>()
                allNames.addAll(systemAlarmNames)
                allNames.addAll(eventSubscriptionNames)

                List<String> alarmUuids = Q.New(AlarmVO.class)
                        .select(AlarmVO_.@uuid)
                        .in(AlarmVO_.@name, allNames)
                        .listValues()
                List<String> activeAlarmUuids = []
                List<String> subscriptionUuids = Q.New(EventSubscriptionVO.class)
                        .select(EventSubscriptionVO_.uuid)
                        .in(EventSubscriptionVO_.@name, allNames)
                        .listValues()
                List<String> topicUuids = Q.New(SNSTopicVO.class)
                        .select(SNSTopicVO_.uuid)
                        .in(SNSTopicVO_.@name, ["api", "system-alarm", "topic-for-ticket"])
                        .listValues()
                List<String> endpointUuids = Q.New(SNSApplicationEndpointVO.class)
                        .select(SNSApplicationEndpointVO_.uuid)
                        .in(SNSApplicationEndpointVO_.@name, ["system-alarm-endpoint", systemHttpEndpointName])
                        .listValues()
                String platformUuid = Q.New(SNSApplicationPlatformVO.class)
                        .select(SNSApplicationPlatformVO_.uuid)
                        .eq(SNSApplicationPlatformVO_.@name, "system")
                        .findValue()

                List<String> uuids = alarmUuids
                uuids.addAll(subscriptionUuids)
                uuids.addAll(endpointUuids)
                uuids.add(platformUuid)
                uuids.addAll(topicUuids)
                uuids.addAll(activeAlarmUuids)

                return vos.findAll { !uuids.contains(it.resourceUuid) }
            }
        }

        table {
            tableVOClass = ActiveAlarmTemplateVO.class
            noLimitRows = true
        }
    }
}
