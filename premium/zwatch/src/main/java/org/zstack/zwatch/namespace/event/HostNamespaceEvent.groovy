package org.zstack.zwatch.namespace.event

import org.zstack.core.db.Q
import org.zstack.core.progress.TaskTracker
import org.zstack.ha.HostCheckAliveTaskTracker
import org.zstack.ha.VmHaCanonicalEvents
import org.zstack.header.host.HostCanonicalEvents
import org.zstack.header.host.HostStatus
import org.zstack.header.vm.VmTracerCanonicalEvents
import org.zstack.storage.device.hba.PortState
import org.zstack.storage.primary.sharedblock.SharedBlockGroupVO
import org.zstack.storage.primary.sharedblock.SharedBlockGroupVO_
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.HostNamespace

class HostNamespaceEvent {
    private static final int hostDisconnectedReportedInCache = 10000
    private static Map<String, Boolean> hostDisconnectedReportedMap = new LinkedHashMap<String, Boolean>(hostDisconnectedReportedInCache, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > hostDisconnectedReportedInCache
        }
    }


    HostNamespaceEvent() {
        HostNamespace.HostStatusChanged.onCanonicalEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH) { HostCanonicalEvents.HostStatusChangedData data ->
            return new EventFamily.Event(data.hostUuid, data.oldStatus, data.newStatus)
        }

        HostNamespace.HostConnected.onCanonicalEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH) { HostCanonicalEvents.HostStatusChangedData data ->
            if (data.newStatus == HostStatus.Connected.toString()) {
                return new EventFamily.Event(data.hostUuid, data.oldStatus, data.newStatus)
            }
            return null
        }.onRecoverReturnResourceId { HostCanonicalEvents.HostStatusChangedData data ->
            if (data.newStatus == HostStatus.Connected.toString()) {
                return data.hostUuid
            }
            return null
        }.onEventBarriersCleaner(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH) { HostCanonicalEvents.HostStatusChangedData data ->
            if (data.newStatus == HostStatus.Connected.toString()) {
                return data.hostUuid
            }
            return null
        }

        HostNamespace.HostUnknownVMDetected.onCanonicalEvent(VmTracerCanonicalEvents.STRANGER_VM_FOUND_PATH) { VmTracerCanonicalEvents.StrangerVmFoundData data ->
            return new EventFamily.Event(data.hostUuid, data.vmIdentity)
        }

        HostNamespace.HostVMOperateErrorDetected.onCanonicalEvent(VmTracerCanonicalEvents.VM_OPERATE_FAIL_ON_HYPERVISOR_PATH) { VmTracerCanonicalEvents.OperateFailOnHypervisorData data ->
            return new EventFamily.Event(data.hostUuid, data.vmUuid, data.operate, data.result);
        }

        HostNamespace.HostDisconnected.onCanonicalEvent(HostCanonicalEvents.HOST_DISCONNECTED_PATH) { HostCanonicalEvents.HostDisconnectedData data ->
            return new EventFamily.Event(data.hostUuid, data.reason.getReadableDetails())
        }.onEventBarrier(HostCanonicalEvents.HOST_DISCONNECTED_PATH) { HostCanonicalEvents.HostDisconnectedData data ->
            return data.getHostUuid()
        }

        HostNamespace.HostHardwareChanged.onCanonicalEvent(HostCanonicalEvents.HOST_HARDWARE_CHANGED_PATH) { HostCanonicalEvents.HostHardwareChangedData data ->
            return new EventFamily.Event(data.hostUuid, data.reason.getReadableDetails())
        }

        HostNamespace.HostCheckAlive.onTaskTracker(HostCheckAliveTaskTracker.TASK_NAME) { TaskTracker.Task task ->
            String process = task.parameters[HostCheckAliveTaskTracker.PARAM_PROCESS]
            return new EventFamily.Event(task.resourceUuid, process, task.info)
        }

        HostNamespace.VMHAHostSelfFencerTriggered.onCanonicalEvent(VmHaCanonicalEvents.VM_HA_HOST_SELF_FENCER_TRIGGERED_PATH) { VmHaCanonicalEvents.VMHaSelfFencerTriggeredData data ->
            return new EventFamily.Event(data.hostUuid, data.primaryStorageUuid, data.vmUuids, data.reason)
        }

        HostNamespace.FaultMountPointOnHost.onCanonicalEvent(HostCanonicalEvents.HOST_CHECK_MOUNT_FAULT) { HostCanonicalEvents.HostMountData data ->
            return new EventFamily.Event(data.hostUuid, data.details)
        }

        HostNamespace.FaultMountPointOnHost.onCanonicalEvent(HostCanonicalEvents.HOST_CHECK_INITIALIZED_FAILED) { HostCanonicalEvents.HostMountData data ->
            return new EventFamily.Event(data.hostUuid, data.details)
        }

        HostNamespace.HostPhysicalNicStatusUp.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_UP) { HostCanonicalEvents.HostPhysicalNicStatusData data ->
            return new EventFamily.Event(data.hostUuid, data.fromBond, data.interfaceName, data.ipAddress, data.interfaceStatus)
        }.onRecoverReturnResourceId { HostCanonicalEvents.HostPhysicalNicStatusData data ->
            if ("up".equalsIgnoreCase(data.interfaceStatus.replaceAll("\\s*", ""))) {
                return data.getHostUuid()
            }
            return null
        }.onEventBarrier(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_UP) { HostCanonicalEvents.HostPhysicalNicStatusData data ->
            return data.getHostUuid()
        }

        HostNamespace.HostPhysicalNicStatusDown.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_DOWN) { HostCanonicalEvents.HostPhysicalNicStatusData data ->
            return new EventFamily.Event(data.hostUuid, data.fromBond, data.interfaceName, data.ipAddress, data.interfaceStatus)
        }.onRecoverReturnResourceId { HostCanonicalEvents.HostPhysicalNicStatusData data ->
            if ("down".equalsIgnoreCase(data.interfaceStatus.replaceAll("\\s*", ""))) {
                return data.getHostUuid()
            }
            return null
        }.onEventBarrier(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_DOWN) { HostCanonicalEvents.HostPhysicalNicStatusData data ->
            return data.getHostUuid()
        }

        HostNamespace.HostPhysicalMemoryEccErrorTriggered.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_MEMORY_ECC_ERROR_TRIGGERED) { HostCanonicalEvents.HostPhysicalMemoryEccErrorData data ->
            return new EventFamily.Event(data.hostUuid, data.detail.getReadableDetails())
        }

        HostNamespace.HostPhysicalCpuStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_CPU_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalCpuStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.cpuName, data.status)
        }

        HostNamespace.HostPhysicalMemoryStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalMemoryStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.locator, data.status)
        }

        HostNamespace.HostPhysicalFanStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_FAN_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalFanStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.fanName, data.status)
        }

        HostNamespace.HostPhysicalDiskStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_DISK_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalDiskStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.serialNumber, data.enclosureId, data.slotNumber, data.status)
        }

        HostNamespace.HostPhysicalDiskInsertTriggered.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_DISK_INSERT_TRIGGERED) { HostCanonicalEvents.HostPhysicalDiskData data ->
            return new EventFamily.Event(data.hostUuid, data.serialNumber, data.enclosureId, data.slotNumber)
        }

        HostNamespace.HostPhysicalDiskRemoveTriggered.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_DISK_REMOVE_TRIGGERED) { HostCanonicalEvents.HostPhysicalDiskData data ->
            return new EventFamily.Event(data.hostUuid, data.serialNumber, data.enclosureId, data.slotNumber)
        }

        HostNamespace.HostPhysicalPowerSupplyStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalPowerSupplyStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.name, data.status)
        }

        HostNamespace.HostPhysicalGpuRemoveTriggered.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_GPU_REMOVE_TRIGGERED) { HostCanonicalEvents.HostPhysicalGpuRemoveTriggeredData data ->
            return new EventFamily.Event(data.hostUuid, data.pcideviceAddress)
        }

        HostNamespace.HostPhysicalGpuStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_GPU_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.pcideviceAddress, data.status)
        }

        HostNamespace.HostPhysicalVGpuStatusAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_VGPU_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalVGpuStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.status, data.name)
        }

        HostNamespace.HostPhysicalRaidStateAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_RAID_STATUS_ABNORMAL) { HostCanonicalEvents.HostPhysicalRaidStatusAbnormalData data ->
            return new EventFamily.Event(data.hostUuid, data.targetId, data.status)
        }

        HostNamespace.HostHbaPortStateAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_HBA_STATE_ABNORMAL) { HostCanonicalEvents.HostPhysicalHbaPortStateAbnormalData data ->
            if (data.newPortState.equalsIgnoreCase(PortState.Online.toString())) {
                return null
            }
            return new EventFamily.Event(data.hostUuid, data.name, data.portName, data.oldPortState, data.newPortState)
        }

        HostNamespace.HostSharedBlockStateAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_VOLUME_STATE_ABNORMAL) { HostCanonicalEvents.HostPhysicalVolumeStateAbnormalData data ->
            if (Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, data.vgName).isExists()) {
                return new EventFamily.Event(data.hostUuid, data.vgName, data.diskUuids, data.diskName, data.state)
            }
            return null
        }

        HostNamespace.HostProcessPhysicalMemoryUsageAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PROCESS_PHYSICAL_MEMORY_USAGE_ABNORMAL) { HostCanonicalEvents.HostProcessPhysicalMemoryUsageAlarmData data ->
            return new EventFamily.Event(data.hostUuid, data.processName, data.pid, data.memoryUsage)
        }
    }
}
