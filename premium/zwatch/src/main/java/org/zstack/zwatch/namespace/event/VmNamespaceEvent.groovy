package org.zstack.zwatch.namespace.event

import org.zstack.core.progress.TaskTracker
import org.zstack.ha.VmHaCanonicalEvents
import org.zstack.ha.VmHaTaskTracker
import org.zstack.header.vm.VmAbnormalLifeCycleStruct
import org.zstack.header.vm.VmCanonicalEvents
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmTracerCanonicalEvents
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.VmNamespace

class VmNamespaceEvent {
    VmNamespaceEvent() {
        VmNamespace.VMHAStarted.onCanonicalEvent(VmHaCanonicalEvents.VM_HA_STARTED_PATH) { VmHaCanonicalEvents.VMHaStartedData data ->
            if (data.error != null) {
                return new EventFamily.Event(data.vmUuid, data.hostUuid).setError(data.error.toString())
            } else {
                return new EventFamily.Event(data.vmUuid, data.hostUuid)
            }
        }.onEventBarrier(VmHaCanonicalEvents.VM_HA_STARTED_PATH) { VmHaCanonicalEvents.VMHaStartedData data ->
            if (data.error == null) {
                return null
            }

            return data.getVmUuid()
        }.onEventBarriersCleaner(VmHaCanonicalEvents.VM_HA_STARTED_PATH) { VmHaCanonicalEvents.VMHaStartedData data ->
            if (data.error == null) {
                return data.getVmUuid()
            }

            return null
        }

        VmNamespace.VMStateChanged.onEventBarriersCleaner(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH) { VmCanonicalEvents.VmStateChangedData data ->
            if (data.newState == VmInstanceState.Running.toString()) {
                return data.getVmUuid()
            }

            return null
        }

        VmNamespace.VMStateChangedOnHost.onCanonicalEvent(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH) { VmTracerCanonicalEvents.VmStateChangedOnHostData data ->
            return new EventFamily.Event(data.vmUuid, data.from.toString(), data.to.toString(), data.originalHostUuid, data.currentHostUuid)
        }.onEventBarriersCleaner(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH) { VmTracerCanonicalEvents.VmStateChangedOnHostData data ->
            if (data.to == VmInstanceState.Running) {
                return data.getVmUuid()
            }

            return null
        }

        VmNamespace.VMHAProcess.onTaskTracker(VmHaTaskTracker.TASK_NAME) { TaskTracker.Task task ->
            String error = task.parameters[TaskTracker.PARAM_ERROR]
            String process = task.parameters[VmHaTaskTracker.PARAM_PROCESS]
            if (error == null) {
                return new EventFamily.Event(task.resourceUuid, process, task.info)
            } else {
                return new EventFamily.Event(task.resourceUuid, process, task.info).setError(error)
            }
        }

        VmNamespace.VMStateInShutdown.onCanonicalEvent(VmTracerCanonicalEvents.VM_STATE_IN_SHUTDOWN_PATH) { VmTracerCanonicalEvents.VmStateInShutdownData data ->
            return new EventFamily.Event(data.vmUuid, data.getReason().getReadableDetails())
        }

        VmNamespace.VmCrash.onCanonicalEvent(VmCanonicalEvents.VM_LIBVIRT_REPORT_CRASH) { VmCanonicalEvents.VmCrashReportData data ->
            return new EventFamily.Event(data.vmUuid, data.getReason().getReadableDetails())
        }

        VmNamespace.VmAbnormalLifeCycleDetected.onCanonicalEvent(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH) { VmTracerCanonicalEvents.VmStateChangedOnHostData data ->
            VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation operation =
                    VmAbnormalLifeCycleStruct.getVmAbnormalLifeCycleOperation(
                            data.originalHostUuid,
                            data.from,
                            data.to,
                            data.originalHostUuid,
                            data.currentHostUuid)

            if (operation != VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost) {
                return null
            }

            return new EventFamily.Event(data.vmUuid,
                    data.from.toString(),
                    data.to.toString(),
                    data.originalHostUuid,
                    data.currentHostUuid,
                    operation.toString())
        }.onEventBarriersCleaner(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH) { VmTracerCanonicalEvents.VmStateChangedOnHostData data ->
            if (data.to == VmInstanceState.Running) {
                return data.getVmUuid()
            }

            return null
        }

        VmNamespace.VMInternalIpChanged.onCanonicalEvent(VmCanonicalEvents.VM_NIC_INFO_CHANGED_PATH) { VmCanonicalEvents.VmInternalIpChangedData data ->
            return new EventFamily.Event(data.vmUuid, data.l3NetworkUuid, data.oldInternalIp, data.newInternalIp, data.relateResourceUuid)
        }

        VmNamespace.VMInternalIpDuplicate.onCanonicalEvent(VmCanonicalEvents.VM_NIC_INFO_DUPLICATE_PATH) { VmCanonicalEvents.VmInternalIpDuplicateData data ->
            return new EventFamily.Event(data.vmUuid, data.l3NetworkUuid, data.internalIp, data.relateResourceUuid)
        }

        VmNamespace.VMInternalIpRangeConflict.onCanonicalEvent(VmCanonicalEvents.VM_NIC_INFO_IPRANGE_CONFLICT_PATH) { VmCanonicalEvents.VmInternalIpRangeConflictData data ->
            return new EventFamily.Event(data.vmUuid, data.l3NetworkUuid, data.internalIp)
        }
    }
}
