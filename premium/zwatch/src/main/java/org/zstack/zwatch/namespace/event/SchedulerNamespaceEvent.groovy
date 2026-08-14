package org.zstack.zwatch.namespace.event

import org.zstack.header.scheduler.SchedulerCanonicalEvents
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.SchedulerNamespace

/**
 * Created by kayo on 2018/4/4.
 */
class SchedulerNamespaceEvent {
    SchedulerNamespaceEvent() {
        Closure buildEventFromData = { SchedulerCanonicalEvents.SchedulerExecutedData data ->
            if (!data.isSuccess()) {
                return new EventFamily.Event(data.jobUuid, data.schedulerName, data.getError().getReadableDetails() , String.valueOf(data.isSuccess()))
            }

            return new EventFamily.Event(data.jobUuid, data.schedulerName, data.resultMessage, String.valueOf(data.isSuccess()))
        }

        SchedulerNamespace.VMStartScheduler.onCanonicalEvent(SchedulerCanonicalEvents.VM_START_SCHEDULER_PATH, buildEventFromData)

        SchedulerNamespace.VMStopScheduler.onCanonicalEvent(SchedulerCanonicalEvents.VM_STOP_SCHEDULER_PATH, buildEventFromData)

        SchedulerNamespace.VMRebootScheduler.onCanonicalEvent(SchedulerCanonicalEvents.VM_REBOOT_SCHEDULER_PATH, buildEventFromData)

        SchedulerNamespace.VolumeSnapshotScheduler.onCanonicalEvent(SchedulerCanonicalEvents.VOLUME_SNAPSHOT_SCHEDULER_PATH, buildEventFromData)

        SchedulerNamespace.VolumeBackupScheduler.onCanonicalEvent(SchedulerCanonicalEvents.VOLUME_BACKUP_SCHEDULER_PATH, buildEventFromData)

        SchedulerNamespace.DatabaseBackupScheduler.onCanonicalEvent(SchedulerCanonicalEvents.DATABASE_BACKUP_SCHEDULER_PATH, buildEventFromData)

        SchedulerNamespace.SchedulerJobGroupExecutedResult.onCanonicalEvent(SchedulerCanonicalEvents.GROUP_SCHEDULER_PATH) { SchedulerCanonicalEvents.SchedulerGroupExecutedData data ->
            return new EventFamily.Event(data.jobGroupUuid, data.schedulerGroupName, data.totalCount.toString(), data.failedCount.toString(), data.errors.join(";\n"))
        }
    }
}
