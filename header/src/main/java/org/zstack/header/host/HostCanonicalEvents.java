package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/22.
 */
public class HostCanonicalEvents {
    public static final String HOST_STATUS_CHANGED_PATH = "/host/status/change";
    public static final String HOST_DELETED_PATH = "/host/delete";
    public static final String HOST_DISCONNECTED_PATH = "/host/disconnected";
    public static final String HOST_HARDWARE_CHANGED_PATH = "/host/hardware/changed";
    public static final String HOST_CHECK_MOUNT_FAULT = "/host/mount/path/fault";
    public static final String HOST_CHECK_INITIALIZED_FAILED = "/host/check/initialized/failed";
    public static final String HOST_PHYSICAL_NIC_STATUS_UP = "/host/physicalNic/status/up";
    public static final String HOST_PHYSICAL_NIC_STATUS_DOWN = "/host/physicalNic/status/down";
    public static final String HOST_PHYSICAL_MEMORY_ECC_ERROR_TRIGGERED = "/host/physicalMemory/ecc/error/triggered";
    public static final String HOST_PHYSICAL_CPU_STATUS_ABNORMAL = "/host/physicalCpu/status/abnormal";
    public static final String HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL = "/host/physicalMemory/status/abnormal";
    public static final String HOST_PHYSICAL_FAN_STATUS_ABNORMAL = "/host/physicalFan/status/abnormal";
    public static final String HOST_PHYSICAL_DISK_STATUS_ABNORMAL = "/host/physicalDisk/status/abnormal";
    public static final String HOST_PHYSICAL_DISK_INSERT_TRIGGERED = "/host/physicalDisk/insert/triggered";
    public static final String HOST_PHYSICAL_DISK_REMOVE_TRIGGERED = "/host/physicalDisk/remove/triggered";
    public static final String HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL = "/host/physicalPowerSupply/status/abnormal";
    public static final String HOST_PHYSICAL_GPU_REMOVE_TRIGGERED = "/host/physicalGpu/remove/triggered";
    public static final String HOST_PHYSICAL_GPU_STATUS_ABNORMAL = "/host/physicalGpu/status/abnormal";
    public static final String HOST_PHYSICAL_VGPU_STATUS_ABNORMAL = "/host/physicalVGpu/status/abnormal";
    public static final String HOST_PHYSICAL_RAID_STATUS_ABNORMAL = "/host/physicalRaid/status/abnormal";
    public static final String HOST_PHYSICAL_HBA_STATE_ABNORMAL = "/host/physicalHBA/state/abnormal";
    public static final String HOST_PHYSICAL_VOLUME_STATE_ABNORMAL = "/host/physicalVolume/state/abnormal";

    public static final String HOST_PROCESS_PHYSICAL_MEMORY_USAGE_ABNORMAL = "/host/process/physicalMemory/usage/abnormal";
    public static final String HOST_STATUS_BUSY = "/host/status/busy";
    public static final String HOST_STATUS_RESUME_FROM_BUSY = "/host/status/resume-from-busy";

    @NeedJsonSchema
    public static class HostPhysicalHbaPortStateAbnormalData {
        private String hostUuid;
        private String name;
        private String portName;
        private String newPortState;
        private String oldPortState;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPortName() {
            return portName;
        }

        public void setPortName(String portName) {
            this.portName = portName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNewPortState() {
            return newPortState;
        }

        public void setNewPortState(String newPortState) {
            this.newPortState = newPortState;
        }

        public String getOldPortState() {
            return oldPortState;
        }

        public void setOldPortState(String oldPortState) {
            this.oldPortState = oldPortState;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalGpuRemoveTriggeredData {
        private String hostUuid;
        private String pcideviceAddress;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPcideviceAddress() {
            return pcideviceAddress;
        }

        public void setPcideviceAddress(String pcideviceAddress) {
            this.pcideviceAddress = pcideviceAddress;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalVGpuStatusAbnormalData {
        private String hostUuid;
        private String uuid;
        private String name;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }


    @NeedJsonSchema
    public static class HostPhysicalGpuStatusAbnormalData {
        private String hostUuid;
        private String pcideviceAddress;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPcideviceAddress() {
            return pcideviceAddress;
        }

        public void setPcideviceAddress(String pcideviceAddress) {
            this.pcideviceAddress = pcideviceAddress;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalCpuStatusAbnormalData {
        private String hostUuid;
        private String cpuName;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getCpuName() {
            return cpuName;
        }

        public void setCpuName(String cpuName) {
            this.cpuName = cpuName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalMemoryStatusAbnormalData {
        private String hostUuid;
        private String locator;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getLocator() {
            return locator;
        }

        public void setLocator(String locator) {
            this.locator = locator;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalFanStatusAbnormalData {
        private String hostUuid;
        private String fanName;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getFanName() {
            return fanName;
        }

        public void setFanName(String fanName) {
            this.fanName = fanName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalPowerSupplyStatusAbnormalData {
        private String hostUuid;
        private String name;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalRaidStatusAbnormalData {
        private String hostUuid;
        private String targetId;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalDiskStatusAbnormalData {
        private String hostUuid;
        private String serialNumber;
        private String enclosureId;
        private String slotNumber;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getEnclosureId() {
            return enclosureId;
        }

        public void setEnclosureId(String enclosureId) {
            this.enclosureId = enclosureId;
        }

        public String getSlotNumber() {
            return slotNumber;
        }

        public void setSlotNumber(String slotNumber) {
            this.slotNumber = slotNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalDiskData {
        private String hostUuid;
        private String serialNumber;
        private String enclosureId;
        private String slotNumber;
        private String name;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getEnclosureId() {
            return enclosureId;
        }

        public void setEnclosureId(String enclosureId) {
            this.enclosureId = enclosureId;
        }

        public String getSlotNumber() {
            return slotNumber;
        }

        public void setSlotNumber(String slotNumber) {
            this.slotNumber = slotNumber;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalVolumeStateAbnormalData {
        private String hostUuid;
        private String diskName;
        private String diskUuids; // example: scsi-360014058e50754920324473a4c85c767;wwn-0x60014058e50754920324473a4c85c767
        private String state;
        private String vgName;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getDiskName() {
            return diskName;
        }

        public void setDiskName(String diskName) {
            this.diskName = diskName;
        }

        public String getDiskUuids() {
            return diskUuids;
        }

        public void setDiskUuids(String diskUuids) {
            this.diskUuids = diskUuids;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getVgName() {
            return vgName;
        }

        public void setVgName(String vgName) {
            this.vgName = vgName;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalMemoryEccErrorData {
        private String hostUuid;
        private ErrorCode detail;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public ErrorCode getDetail() {
            return detail;
        }

        public void setDetail(ErrorCode detail) {
            this.detail = detail;
        }
    }

    public static class HostDisconnectedData {
        public String hostUuid;
        public ErrorCode reason;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }

    public static class HostHardwareChangedData {
        public String hostUuid;
        public ErrorCode reason;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }

    @NeedJsonSchema
    public static class HostStatusChangedData {
        private String hostUuid;
        private String oldStatus;
        private String newStatus;
        private HostInventory inventory;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public HostInventory getInventory() {
            return inventory;
        }

        public void setInventory(HostInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class HostDeletedData {
        private String hostUuid;
        private HostInventory inventory;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public HostInventory getInventory() {
            return inventory;
        }

        public void setInventory(HostInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class HostMountData {
        public String hostUuid;
        public String psUuid;
        public String details;
        public Long eventTime = System.currentTimeMillis();
    }

    @NeedJsonSchema
    public static class HostPhysicalNicStatusData {
        public String hostUuid;
        public String hostAddr;
        public String fromBond;
        public String interfaceName;
        public String ipAddress;
        public String interfaceStatus;
        public Long eventTime = System.currentTimeMillis();


        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getHostAddr() {
            return hostAddr;
        }

        public void setHostAddr(String hostAddr) {
            this.hostAddr = hostAddr;
        }

        public String getFromBond() {
            return fromBond;
        }

        public void setFromBond(String fromBond) {
            this.fromBond = fromBond;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getInterfaceStatus() {
            return interfaceStatus;
        }

        public void setInterfaceStatus(String interfaceStatus) {
            this.interfaceStatus = interfaceStatus;
        }
    }

    @NeedJsonSchema
    public static class HostProcessPhysicalMemoryUsageAlarmData {
        private String hostUuid;
        private String pid;
        private String processName;
        private String memoryUsage;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public String getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(String memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
    }

    @NeedJsonSchema
    public static class HostPingSkipData {
        private String hostUuid;
        private long busyUntil;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public long getBusyUntil() {
            return busyUntil;
        }

        public void setBusyUntil(long busyUntil) {
            this.busyUntil = busyUntil;
        }
    }
}
