package org.zstack.zwatch.alarm.system;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.alarm.AlarmActionVO;
import org.zstack.zwatch.alarm.AlarmActionVO_;
import org.zstack.zwatch.alarm.AlarmLabelVO;
import org.zstack.zwatch.alarm.AlarmLabelVO_;
import org.zstack.zwatch.alarm.AlarmVO;
import org.zstack.zwatch.alarm.AlarmVO_;
import org.zstack.zwatch.alarm.EventSubscriptionActionVO;
import org.zstack.zwatch.alarm.EventSubscriptionActionVO_;
import org.zstack.zwatch.alarm.EventSubscriptionLabelVO;
import org.zstack.zwatch.alarm.EventSubscriptionLabelVO_;
import org.zstack.zwatch.alarm.EventSubscriptionVO;
import org.zstack.zwatch.alarm.EventSubscriptionVO_;
import org.zstack.zwatch.migratedb.AlarmRecordsVO;
import org.zstack.zwatch.migratedb.AlarmRecordsVO_;
import org.zstack.zwatch.migratedb.AuditsVO;
import org.zstack.zwatch.migratedb.AuditsVO_;
import org.zstack.zwatch.migratedb.EventRecordsVO;
import org.zstack.zwatch.migratedb.EventRecordsVO_;

import static org.zstack.zwatch.alarm.system.SystemAlarmManager.*;

public class ZsvAlarmUuidModification {
    private static final CLogger logger = Utils.getLogger(ZsvAlarmUuidModification.class);
    public void run() {
        modifyAlarm("5z6gsgkc5kccpylj9ocgbd647p2700b7", HOST_CPU_AVERAGE_USED_UTILIZATION_ALARM_UUID);
        modifyAlarm("uhgfoh0soh6e1qai005elfa9c6h2s2y0", VM_CPU_AVERAGE_USED_UTILIZATION_ALARM_UUID);
        modifyAlarm("fuz2p4fa71urf4fd7cknoxsalvj60ynk", VM_MEMORY_USED_ALARM_UUID);
        modifyAlarm("d0b35ac37c58e358cb74e664532f1044", HOST_MEMORY_USED_ALARM_UUID);
        modifyEventSubscription("6nz3vn2e0rdwu5hzmuetzv37ak0nj248", VM_HA_START_EVENT_ALARM_UUID);
        modifyEventSubscription("ppfazo1y3tjvup4jfetxz36y3su98ngc", VM_STATE_IN_SHUTDOWN_EVENT_ALARM_UUID);
        modifyEventSubscription("rlwalvvqyoujj3ign3o309p2zulwbhwm", HOST_UNKNOWN_VM_DETECTED_EVENT_ALARM_UUID);
        modifyEventSubscription("krdu1hs2314kt18ttgqndaynxchs2ufc", MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_EVENT_ALARM_UUID);
        modifyEventSubscription("8tlwqj65mus1gdolu3w61yy35pvwinhz", PRIMARY_STORAGE_HOST_DISCONNECTED_EVENT_ALARM_UUID);
        modifyEventSubscription("g0eviogong06nubt1kj54z63pcka81sw", FAULT_MOUNT_POINT_ON_HOST_EVENT_ALARM_UUID);
        modifyEventSubscription("559ca06aa8bba6990d10c255e4c9ab5b", CLUSTER_QEMU_VERSION_MISMATCH_EVENT_ALARM_UUID);
        modifyAlarm("33198a88f22e4d19b5ff8ebaebb6ujm7", HOST_SSD_TEMPERATURE_ALARM_UUID);
        modifyEventSubscription("a678a66daed67879b5ef2166aaedc07b", HOST_PHYSICAL_GPU_STATUS_ABNORMAL_EVENT_ALARM_UUID);
        modifyEventSubscription("a678a66daed24779bbgf2166aaedc07b", HOST_PHYSICAL_VGPU_STATUS_ABNORMAL_EVENT_ALARM_UUID);
        modifyEventSubscription("a678a66daed24779b5ef2zaqaaedc07b", HOST_PHYSICAL_GPU_REMOVE_EVENT_ALARM_UUID);
        modifyAlarm("5z6gsgkc5kccpylj9234fd647p2700b7", HOST_GPU_TEMPERATURE_ALARM_NAME_UUID);
        modifyEventSubscription("a678a66da6093b5ef2166aaedc07b", HOST_PHYSICAL_RAID_STATUS_ABNORMAL_EVENT_ALARM_UUID);
    }

    /**
     * modify SystemTagVO, AlarmActionVO, AlarmRecordsVO, AlarmLabelVO, AuditsVO
     * delete old AlarmVO (ResourceVO)
     */
    private void modifyAlarm(String originalUuid, String newUuid) {
        boolean originalUuidExist = Q.New(AlarmVO.class)
                .eq(AlarmVO_.uuid, originalUuid)
                .isExists();
        if (!originalUuidExist) {
            return;
        }

        logger.debug(String.format("modify alarm UUID: %s -> %s", originalUuid, newUuid));

        // modify AlarmActionVO: delete actions related to newUuid, and update actions related to originalUuid to newUuid
        // Note: entity with composite primary key should use hardDelete()
        SQL.New(AlarmActionVO.class)
                .eq(AlarmActionVO_.alarmUuid, newUuid)
                .hardDelete();
        SQL.New(AlarmActionVO.class)
                .eq(AlarmActionVO_.alarmUuid, originalUuid)
                .set(AlarmActionVO_.alarmUuid, newUuid)
                .update();

        // modify SystemTagVO: delete system tag related to newUuid, and update system tag related to originalUuid to newUuid
        SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, newUuid)
                .delete();
        SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, originalUuid)
                .set(SystemTagVO_.resourceUuid, newUuid)
                .update();

        // modify AlarmRecordsVO: update alarmUuid column
        SQL.New(AlarmRecordsVO.class)
                .eq(AlarmRecordsVO_.alarmUuid, originalUuid)
                .set(AlarmRecordsVO_.alarmUuid, newUuid)
                .update();

        // modify AuditsVO: delete audits related to newUuid, and update audits related to originalUuid to newUuid
        SQL.New(AuditsVO.class)
                .eq(AuditsVO_.resourceUuid, newUuid)
                .delete();
        SQL.New(AuditsVO.class)
                .eq(AuditsVO_.resourceUuid, originalUuid)
                .set(AuditsVO_.resourceUuid, newUuid)
                .update();

        // modify AlarmLabelVO: update alarmUuid column
        SQL.New(AlarmLabelVO.class)
                .eq(AlarmLabelVO_.alarmUuid, originalUuid)
                .set(AlarmLabelVO_.alarmUuid, newUuid)
                .update();

        // modify new AlarmVO by original AlarmVO properties
        // then, remove old AlarmVO
        AlarmVO old = Q.New(AlarmVO.class)
                .eq(AlarmVO_.uuid, originalUuid)
                .find();
        SQL.New(AlarmVO.class)
                .eq(AlarmVO_.uuid, newUuid)
                .set(AlarmVO_.name, old.getName())
                .set(AlarmVO_.description, old.getDescription())
                .set(AlarmVO_.comparisonOperator, old.getComparisonOperator())
                .set(AlarmVO_.period, old.getPeriod())
                .set(AlarmVO_.repeatInterval, old.getRepeatInterval())
                .set(AlarmVO_.repeatCount, old.getRepeatCount())
                .set(AlarmVO_.threshold, old.getThreshold())
                .set(AlarmVO_.enableRecovery, old.isEnableRecovery())
                .set(AlarmVO_.status, old.getStatus())
                .set(AlarmVO_.state, old.getState())
                .set(AlarmVO_.emergencyLevel, old.getEmergencyLevel())
                .update();
        SQL.New(AlarmVO.class)
                .eq(AlarmVO_.uuid, originalUuid)
                .delete();
    }

    /**
     * modify SystemTagVO, EventSubscriptionActionVO, EventRecordsVO, EventSubscriptionLabelVO, AuditsVO
     * delete old EventSubscriptionVO (ResourceVO)
     */
    private void modifyEventSubscription(String originalUuid, String newUuid) {
        boolean originalUuidExist = Q.New(EventSubscriptionVO.class)
                .eq(EventSubscriptionVO_.uuid, originalUuid)
                .isExists();
        if (!originalUuidExist) {
            return;
        }

        logger.debug(String.format("modify event subscription UUID: %s -> %s", originalUuid, newUuid));

        // modify EventSubscriptionActionVO:
        //     delete actions related to newUuid, and update actions related to originalUuid to newUuid
        // Note: entity with composite primary key should use hardDelete()
        SQL.New(EventSubscriptionActionVO.class)
                .eq(EventSubscriptionActionVO_.subscriptionUuid, newUuid)
                .hardDelete();
        SQL.New(EventSubscriptionActionVO.class)
                .eq(EventSubscriptionActionVO_.subscriptionUuid, originalUuid)
                .set(EventSubscriptionActionVO_.subscriptionUuid, newUuid)
                .update();

        // modify SystemTagVO: delete system tag related to newUuid, and update system tag related to originalUuid to newUuid
        SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, newUuid)
                .delete();
        SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, originalUuid)
                .set(SystemTagVO_.resourceUuid, newUuid)
                .update();

        // modify EventRecordsVO: update subscriptionUuid column
        SQL.New(EventRecordsVO.class)
                .eq(EventRecordsVO_.subscriptionUuid, originalUuid)
                .set(EventRecordsVO_.subscriptionUuid, newUuid)
                .update();

        // modify AuditsVO: delete audits related to newUuid, and update audits related to originalUuid to newUuid
        SQL.New(AuditsVO.class)
                .eq(AuditsVO_.resourceUuid, newUuid)
                .delete();
        SQL.New(AuditsVO.class)
                .eq(AuditsVO_.resourceUuid, originalUuid)
                .set(AuditsVO_.resourceUuid, newUuid)
                .update();

        // modify EventSubscriptionLabelVO: update subscriptionUuid column
        SQL.New(EventSubscriptionLabelVO.class)
                .eq(EventSubscriptionLabelVO_.subscriptionUuid, originalUuid)
                .set(EventSubscriptionLabelVO_.subscriptionUuid, newUuid)
                .update();

        // modify new EventSubscriptionVO by original EventSubscriptionVO properties
        // then, remove old EventSubscriptionVO
        EventSubscriptionVO old = Q.New(EventSubscriptionVO.class)
                .eq(EventSubscriptionVO_.uuid, originalUuid)
                .find();
        SQL.New(EventSubscriptionVO.class)
                .eq(EventSubscriptionVO_.uuid, newUuid)
                .set(EventSubscriptionVO_.name, old.getName())
                .set(EventSubscriptionVO_.state, old.getState())
                .set(EventSubscriptionVO_.emergencyLevel, old.getEmergencyLevel())
                .update();
        SQL.New(EventSubscriptionVO.class)
                .eq(EventSubscriptionVO_.uuid, originalUuid)
                .delete();
    }
}
