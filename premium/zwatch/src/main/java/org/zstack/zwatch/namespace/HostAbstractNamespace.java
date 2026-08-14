package org.zstack.zwatch.namespace;

import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shenjin
 * @date 2022/7/29 13:24
 */
public abstract class HostAbstractNamespace extends AbstractNamespace {
    protected static final List<Metric> metrics = new ArrayList<>();
    protected static final List<EventFamily> events = new ArrayList<>();

    protected HostAbstractNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public HostAbstractNamespace() {
        super();
    }

    public enum LabelNames {
        HostUuid,
        CPUNum,
        DiskDeviceLetter,
        NetworkDeviceLetter,
        NetworkServiceType,
        MountPoint,
        HypervisorType,
        VolumeGroupName,
        TargetId,
        SlotNumber,
        DiskGroup,
        PowerSupplyId,
        InterfaceName,
        InterfaceSpeed,
        FSType,
        Wwid,
        FanSpeedName,
        SerialNumber,
        PciDeviceAddress,
        VMUuid,
        MdevDeviceUuid,
        GpuStatus,
        GpuSerialNumber,
        Name,
        ManagementIp,
        Type,
        PortName,
        PortState
    }

    public static final Metric HostInfo = new InfoMetric("HostInfo", metrics,
            LabelNames.HostUuid, LabelNames.Name, LabelNames.ManagementIp
    );

    public static final Metric CPUIdleUtilization = new PercentMetric("CPUIdleUtilization", metrics,
            LabelNames.HostUuid, LabelNames.CPUNum
    );
    public static final Metric CPUSystemUtilization = new PercentMetric("CPUSystemUtilization", metrics,
            LabelNames.HostUuid, LabelNames.CPUNum
    );
    public static final Metric CPUUserUtilization = new PercentMetric("CPUUserUtilization", metrics,
            LabelNames.HostUuid, LabelNames.CPUNum
    );
    public static final Metric CPUWaitUtilization = new PercentMetric("CPUWaitUtilization", metrics,
            LabelNames.HostUuid, LabelNames.CPUNum
    );
    public static final Metric CPUAllIdleUtilization = new PercentMetric("CPUAllIdleUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric CPUUsedUtilization = new PercentMetric("CPUUsedUtilization", metrics,
            LabelNames.HostUuid, LabelNames.CPUNum
    );
    public static final Metric CPUAllUsedUtilization = new PercentMetric("CPUAllUsedUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric CPUAverageUsedUtilization = new PercentMetric("CPUAverageUsedUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric CPUAverageUserUtilization = new PercentMetric("CPUAverageUserUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric CPUAverageWaitUtilization = new PercentMetric("CPUAverageWaitUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric CPUAverageSystemUtilization = new PercentMetric("CPUAverageSystemUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric CPUAverageIdleUtilization = new PercentMetric("CPUAverageIdleUtilization", metrics,
            LabelNames.HostUuid
    );
    public static final Metric MemoryFreeBytes = new ByteSizeMetric("MemoryFreeBytes", metrics, LabelNames.HostUuid);
    public static final Metric MemoryFreeInPercent = new PercentMetric("MemoryFreeInPercent", metrics,
            LabelNames.HostUuid
    );
    public static final Metric MemoryUsedBytes = new ByteSizeMetric("MemoryUsedBytes", metrics, LabelNames.HostUuid);
    public static final Metric MemoryUsedInPercent = new PercentMetric("MemoryUsedInPercent", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskReadOps = new OperationRateMetric("DiskReadOps", metrics, LabelNames.HostUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllReadOps = new OperationRateMetric("DiskAllReadOps", metrics, LabelNames.HostUuid);
    public static final Metric DiskWriteOps = new OperationRateMetric("DiskWriteOps", metrics, LabelNames.HostUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllWriteOps = new OperationRateMetric("DiskAllWriteOps", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskReadBytes = new ByteRateMetric("DiskReadBytes", metrics, LabelNames.HostUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllReadBytes = new ByteRateMetric("DiskAllReadBytes", metrics, LabelNames.HostUuid);
    public static final Metric DiskWriteBytes = new ByteRateMetric("DiskWriteBytes", metrics, LabelNames.HostUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllWriteBytes = new ByteRateMetric("DiskAllWriteBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkInBytes = new ByteRateMetric("NetworkInBytes", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInBytes = new ByteRateMetric("NetworkAllInBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkAllInBytesByServiceType = new ByteRateMetric("NetworkAllInBytesByServiceType", metrics, LabelNames.HostUuid,
            LabelNames.NetworkServiceType
    );
    public static final Metric NetworkInPackets = new PacketRateMetric("NetworkInPackets", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInPackets = new PacketRateMetric("NetworkAllInPackets", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkAllInPacketsByServiceType = new ByteRateMetric("NetworkAllInPacketsByServiceType", metrics, LabelNames.HostUuid,
            LabelNames.NetworkServiceType
    );
    public static final Metric NetworkInErrors = new PacketRateMetric("NetworkInErrors", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkInDropped = new PacketRateMetric("NetworkInDropped", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInErrors = new PacketRateMetric("NetworkAllInErrors", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkAllInErrorsByServiceType = new ByteRateMetric("NetworkAllInErrorsByServiceType", metrics, LabelNames.HostUuid,
            LabelNames.NetworkServiceType
    );
    public static final Metric NetworkOutBytes = new ByteRateMetric("NetworkOutBytes", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutBytes = new ByteRateMetric("NetworkAllOutBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkAllOutBytesByServiceType = new ByteRateMetric("NetworkAllOutBytesByServiceType", metrics, LabelNames.HostUuid,
            LabelNames.NetworkServiceType
    );
    public static final Metric NetworkOutPackets = new PacketRateMetric("NetworkOutPackets", metrics,
            LabelNames.HostUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutPackets = new PacketRateMetric("NetworkAllOutPackets", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkAllOutPacketsByServiceType = new ByteRateMetric("NetworkAllOutPacketsByServiceType", metrics, LabelNames.HostUuid,
            LabelNames.NetworkServiceType
    );
    public static final Metric NetworkOutErrors = new PacketRateMetric("NetworkOutErrors", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkOutDropped = new PacketRateMetric("NetworkOutDropped", metrics, LabelNames.HostUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutErrors = new PacketRateMetric("NetworkAllOutErrors", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkAllOutErrorsByServiceType = new ByteRateMetric("NetworkAllOutErrorsByServiceType", metrics, LabelNames.HostUuid,
            LabelNames.NetworkServiceType
    );
    public static final Metric NetworkConntrackCount = new CountMetric("NetworkConntrackCount", metrics,
            LabelNames.HostUuid
    );
    public static final Metric NetworkConntrackInPercent = new PercentMetric("NetworkConntrackInPercent", metrics,
            LabelNames.HostUuid
    );

    public static final Metric DiskTotalCapacityInBytes = new ByteSizeMetric("DiskTotalCapacityInBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskAllFreeCapacityInBytes = new ByteSizeMetric("DiskAllFreeCapacityInBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskAllFreeCapacityInPercent = new PercentMetric("DiskAllFreeCapacityInPercent", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskAllUsedCapacityInBytes = new ByteSizeMetric("DiskAllUsedCapacityInBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskAllUsedCapacityInPercent = new PercentMetric("DiskAllUsedCapacityInPercent", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskFreeCapacityInBytes = new ByteSizeMetric("DiskCapacityInBytes", metrics,
            LabelNames.HostUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskFreeCapacityInPercent = new PercentMetric("DiskFreeCapacityInPercent", metrics,
            LabelNames.HostUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskUsedCapacityInBytes = new ByteSizeMetric("DiskUsedCapacityInBytes", metrics,
            LabelNames.HostUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskUsedCapacityInPercent = new PercentMetric("DiskUsedCapacityInPercent", metrics,
            LabelNames.HostUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskZStackUsedCapacityInBytes = new ByteSizeMetric("DiskZStackUsedCapacityInBytes",
            metrics, LabelNames.HostUuid
    );
    public static final Metric DiskZStackUsedCapacityInPercent = new PercentMetric("DiskZStackUsedCapacityInPercent",
            metrics, LabelNames.HostUuid
    );
    public static final Metric DiskTransUsedCapacityInBytes = new ByteSizeMetric("DiskTransUsedCapacityInBytes",
            metrics, LabelNames.HostUuid
    );
    public static final Metric DiskTransUsedCapacityInPercent = new PercentMetric("DiskTransUsedCapacityInPercent",
            metrics, LabelNames.HostUuid
    );
    public static final Metric DiskRootUsedCapacityInBytes = new ByteSizeMetric("DiskRootUsedCapacityInBytes", metrics,
            LabelNames.HostUuid
    );
    public static final Metric DiskRootUsedCapacityInPercent = new PercentMetric("DiskRootUsedCapacityInPercent",
            metrics, LabelNames.HostUuid
    );
    public static final Metric DiskLatency = new CountMetric("DiskLatency", metrics, LabelNames.HostUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskLatencyWwid = new CountMetric("DiskLatencyWwid", metrics, LabelNames.HostUuid,
            LabelNames.Wwid
    );
    public static final Metric DiskWriteOpsWwid = new OperationRateMetric("DiskWriteOpsWwid", metrics,
            LabelNames.HostUuid, LabelNames.Wwid
    );
    public static final Metric DiskReadOpsWwid = new OperationRateMetric("DiskReadOpsWwid", metrics,
            LabelNames.HostUuid, LabelNames.Wwid
    );
    public static final Metric DiskWriteBytesWwid = new ByteRateMetric("DiskWriteBytesWwid", metrics,
            LabelNames.HostUuid, LabelNames.Wwid
    );
    public static final Metric DiskReadBytesWwid = new ByteRateMetric("DiskReadBytesWwid", metrics, LabelNames.HostUuid,
            LabelNames.Wwid
    );
    public static final Metric VolumeGroupCapacityInbytes = new ByteSizeMetric("VolumeGroupCapacityInbytes", metrics,
            LabelNames.HostUuid, LabelNames.VolumeGroupName
    );
    public static final Metric VolumeGroupFreeCapacityInbytes = new ByteSizeMetric("VolumeGroupFreeCapacityInbytes",
            metrics, LabelNames.HostUuid, LabelNames.VolumeGroupName
    );
    public static final Metric VolumeGroupUsedCapacityInPercent = new PercentMetric("VolumeGroupUsedCapacityInPercent",
            metrics, LabelNames.HostUuid, LabelNames.VolumeGroupName
    );
    public static final Metric RaidState = new StateMetric("RaidState", metrics, LabelNames.HostUuid,
            LabelNames.TargetId
    );
    public static final Metric PhysicalDiskState = new StateMetric("PhysicalDiskState", metrics,
            LabelNames.HostUuid, LabelNames.SlotNumber, LabelNames.DiskGroup
    );
    public static final Metric PhysicalDiskTemperature = new TemperatureMetric("PhysicalDiskTemperature", metrics,
            LabelNames.HostUuid, LabelNames.SlotNumber, LabelNames.DiskGroup
    );
    public static final Metric PowerSupply = new StateMetric("PowerSupply", metrics, LabelNames.HostUuid,
            LabelNames.PowerSupplyId
    );
    public static Metric PowerSupplyCurrentOutputPower = new CountMetric("PowerSupplyCurrentOutputPower", metrics, LabelNames.HostUuid, LabelNames.PowerSupplyId);
    public static final Metric IpmiStatus = new StateMetric("IpmiStatus", metrics, LabelNames.HostUuid);
    public static final Metric PhysicalNetworkInterface = new StateMetric("PhysicalNetworkInterface", metrics,
            LabelNames.HostUuid, LabelNames.InterfaceName, LabelNames.InterfaceSpeed
    );
    public static final Metric DiskXfsFragInPercent = new PercentMetric("DiskXfsFragInPercent", metrics,
            LabelNames.HostUuid
    );

    public static final Metric HostTotal = new CountMetric("HostTotal", metrics);
    public static final Metric ConnectedHostCount = new CountMetric("ConnectedHostCount", metrics);
    public static final Metric ConnectedHostInPercent = new PercentMetric("ConnectedHostInPercent", metrics);
    public static final Metric DisconnectedHostCount = new CountMetric("DisconnectedHostCount", metrics);
    public static final Metric DisconnectedHostInPercent = new PercentMetric("DisconnectedHostInPercent", metrics);

    public static final Metric CPUCapacityTotal = new CountMetric("CPUCapacityTotal", metrics);
    public static final Metric CPUUsedCapacityCount = new CountMetric("CPUUsedCapacityCount", metrics);
    public static final Metric CPULockedCapacityCount = new CountMetric("CPULockedCapacityCount", metrics);
    public static final Metric CPUUsedCapacityInPercent = new PercentMetric("CPUUsedCapacityInPercent", metrics);
    public static final Metric CPULockedCapacityInPercent = new PercentMetric("CPULockedCapacityInPercent", metrics);
    public static final Metric CPUAvailableCapacityCount = new CountMetric("CPUAvailableCapacityCount", metrics);
    public static final Metric CPUAvailableCapacityInPercent = new PercentMetric("CPUAvailableCapacityInPercent",
            metrics
    );
    public static final Metric CPUUsedCapacityPerHostCount = new CountMetric("CPUUsedCapacityPerHostCount", metrics,
            LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric CPUUsedCapacityPerHostInPercent = new PercentMetric("CPUUsedCapacityPerHostInPercent",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric CPUAvailableCapacityPerHostCount = new CountMetric("CPUAvailableCapacityPerHostCount",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric CPUAvailableCapacityPerHostInPercent = new PercentMetric(
            "CPUAvailableCapacityPerHostInPercent",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );

    public static final Metric MemoryCapacityTotal = new ByteSizeMetric("MemoryCapacityTotal", metrics);
    public static final Metric MemoryUsedCapacityInBytes = new ByteSizeMetric("MemoryUsedCapacityInBytes", metrics);
    public static final Metric MemoryUsedCapacityInPercent = new PercentMetric("MemoryUsedCapacityInPercent", metrics);
    public static final Metric MemoryLockedCapacityInBytes = new ByteSizeMetric("MemoryLockedCapacityInBytes", metrics);
    public static final Metric MemoryLockedCapacityInPercent = new PercentMetric("MemoryLockedCapacityInPercent",
            metrics
    );
    public static final Metric MemoryAvailableCapacityInBytes = new ByteSizeMetric("MemoryAvailableCapacityInBytes",
            metrics
    );
    public static final Metric MemoryAvailableCapacityInPercent = new PercentMetric("MemoryAvailableCapacityInPercent",
            metrics
    );
    public static final Metric MemoryUsedCapacityPerHostInBytes = new ByteSizeMetric("MemoryUsedCapacityPerHostInBytes",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric MemoryUsedCapacityPerHostInPercent = new PercentMetric(
            "MemoryUsedCapacityPerHostInPercent",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric MemoryAvailableCapacityPerHostInBytes = new ByteSizeMetric(
            "MemoryAvailableCapacityPerHostInBytes",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric MemoryAvailableCapacityPerHostInPercent = new PercentMetric(
            "MemoryAvailableCapacityPerHostInPercent",
            metrics, LabelNames.HostUuid, LabelNames.HypervisorType, LabelNames.Name, LabelNames.ManagementIp
    );
    public static final Metric FanSpeedRpm = new CountRateMetric("FanSpeedRpm", metrics, LabelNames.HostUuid,
            LabelNames.FanSpeedName
    );
    public static final Metric FanSpeedState = new StateMetric("FanSpeedState", metrics, LabelNames.HostUuid,
            LabelNames.FanSpeedName
    );
    public static final Metric CpuTemperature = new TemperatureMetric("CpuTemperature", metrics, LabelNames.HostUuid,
            LabelNames.CPUNum
    );
    public static Metric CpuStatus = new StateMetric("CpuStatus", metrics, LabelNames.HostUuid, LabelNames.CPUNum);
    public static Metric PhysicalMemoryStatus = new StateMetric("PhysicalMemoryStatus", metrics, LabelNames.HostUuid,
            LabelNames.SlotNumber
    );
    public static final Metric SSDLifeLeft = new CountMetric("SSDLifeLeft", metrics, LabelNames.HostUuid,
            LabelNames.DiskDeviceLetter, LabelNames.SerialNumber
    );
    public static final Metric SSDTemperature = new CountMetric("SSDTemperature", metrics, HostNamespace.LabelNames.HostUuid,
            HostNamespace.LabelNames.DiskDeviceLetter, HostNamespace.LabelNames.SerialNumber
    );
    public static final Metric BlockDeviceUsedCapacityInBytes = new ByteSizeMetric("BlockDeviceUsedCapacityInBytes",
            metrics, LabelNames.HostUuid, LabelNames.DiskDeviceLetter
    );
    public static final Metric BlockDeviceUsedCapacityInPercent = new PercentMetric("BlockDeviceUsedCapacityInPercent",
            metrics, LabelNames.HostUuid, LabelNames.DiskDeviceLetter
    );

    public static final Metric SharedPagesMemoryInBytes = new PercentMetric("SharedPagesMemoryInBytes", metrics,
            LabelNames.HostUuid
    );

    public static final Metric ReclaimedMemoryInBytes = new PercentMetric("ReclaimedMemoryInBytes", metrics,
            LabelNames.HostUuid
    );

    public static final Metric GpuPowerDraw = new CountMetric("GpuPowerDraw", metrics, LabelNames.HostUuid,
            LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);
    public static final Metric GpuTemperature = new TemperatureMetric("GpuTemperature", metrics, LabelNames.HostUuid,
            LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);
    public static final Metric GpuFanSpeed = new PercentMetric("GpuFanSpeed", metrics, LabelNames.HostUuid,
            LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);
    public static final Metric GpuUtilization = new PercentMetric("GpuUtilization", metrics, LabelNames.HostUuid,
            LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);

    public static final Metric GpuStatus = new PercentMetric("GpuStatus", metrics, LabelNames.HostUuid,
            LabelNames.PciDeviceAddress, LabelNames.GpuStatus, LabelNames.GpuSerialNumber);
    public static final Metric GpuMemoryUtilization = new PercentMetric("GpuMemoryUtilization", metrics,
            LabelNames.HostUuid, LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);
    public static final Metric GpuPciRxThroughputInBytes = new ByteSizeMetric("GpuPciRxThroughputInBytes", metrics,
            LabelNames.HostUuid, LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);
    public static final Metric GpuPciTxThroughputInBytes = new ByteSizeMetric("GpuPciTxThroughputInBytes", metrics,
            LabelNames.HostUuid, LabelNames.PciDeviceAddress, LabelNames.GpuSerialNumber);
    public static final Metric VGpuUtilization = new PercentMetric("VGpuUtilization", metrics, LabelNames.HostUuid,
            LabelNames.MdevDeviceUuid, LabelNames.VMUuid);
    public static final Metric VGpuMemoryUtilization = new PercentMetric("VGpuMemoryUtilization", metrics,
            LabelNames.HostUuid, LabelNames.MdevDeviceUuid, LabelNames.VMUuid);


    public static final Metric  IPMIPhysicalMemoryStatus = new StateMetric("IPMIPhysicalMemoryStatus", metrics,
            LabelNames.HostUuid, LabelNames.Name, LabelNames.Type);

    public static final Metric IPMIPowerSupply = new StateMetric("IPMIPowerSupply", metrics, LabelNames.HostUuid,
            LabelNames.Name, LabelNames.Type);

    public static final Metric IPMIFanSpeedState = new StateMetric("IPMIFanSpeedState", metrics, LabelNames.HostUuid,
            LabelNames.Name, LabelNames.Type);

    public static final Metric IPMIFanSpeedRpm = new CountRateMetric("IPMIFanSpeedRpm", metrics, LabelNames.HostUuid,
            LabelNames.Name, LabelNames.Type);

    public static final Metric IPMITemperatureState = new StateMetric("IPMITemperatureState", metrics, LabelNames.HostUuid,
            LabelNames.Name, LabelNames.Type);

    public static final Metric IPMITemperatureCelsius = new TemperatureMetric("IPMITemperatureCelsius", metrics, LabelNames.HostUuid,
            LabelNames.Name, LabelNames.Type);
    public static final Metric FcHbaLinkFailureTotal = new CountMetric("FcHbaLinkFailureTotal", metrics, LabelNames.HostUuid,
            LabelNames.Name);
    public static final Metric FcHbaLossOfSignalTotal = new CountMetric("FcHbaLossOfSignalTotal", metrics, LabelNames.HostUuid,
            LabelNames.Name);

    public static final Metric PhysicalVolumeStatus = new StateMetric("PhysicalVolumeStatus", metrics, LabelNames.HostUuid,
            LabelNames.Wwid, LabelNames.DiskDeviceLetter);

    public static final Metric PhysicalVolumeState = new StateMetric("PhysicalVolumeState", metrics, LabelNames.HostUuid,
            LabelNames.Wwid, LabelNames.DiskDeviceLetter);

    public enum EventLabelNames {
        OldStatus,
        NewStatus,
        UnknownVMIdentity,
        CheckProcess,
        Details,
        Error,
        PrimaryStorageUuid,
        VmUuidsString,
        Reason,
        FaultMountPoint,
        ErrorVMOperation,
        PhysicalNicHostUuid,
        PhysicalNicFromBond,
        PhysicalNicName,
        PhysicalNicAddr,
        PhysicalNicStatus,
        HostUuid,
        TargetId,
        HbaPortName,
        Name,
        Pid,
        MemoryUsage,
    }

    public static final EventFamily HostStatusChanged = new EventFamily("HostStatusChanged", events,
            EventLabelNames.OldStatus, EventLabelNames.NewStatus
    );
    public static final EventFamily HostUnknownVMDetected = new EventFamily("HostUnknownVMDetected", events,
            EventLabelNames.UnknownVMIdentity
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily HostVMOperateErrorDetected = new EventFamily("HostVMOperateErrorDetected", events,
            EventLabelNames.VmUuidsString,
            EventLabelNames.ErrorVMOperation, EventLabelNames.Details
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily HostDisconnected = new EventFamily("HostDisconnected", events,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily HostHardwareChanged = new EventFamily("HostHardwareChanged", events,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Important);
    public static final EventFamily HostCheckAlive = new EventFamily("HostCheckAlive", events,
            EventLabelNames.CheckProcess, EventLabelNames.Details
    );
    public static final EventFamily VMHAHostSelfFencerTriggered = new EventFamily("VMHAHostSelfFencerTriggered", events,
            EventLabelNames.PrimaryStorageUuid,
            EventLabelNames.VmUuidsString, EventLabelNames.Reason
    );
    public static final EventFamily FaultMountPointOnHost = new EventFamily("FaultMountPointOnHost", events,
            EventLabelNames.FaultMountPoint
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily HostConnected = new EventFamily("HostConnected", events, EventLabelNames.OldStatus,
            EventLabelNames.NewStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);
    public static final EventFamily HostPhysicalNicStatusUp = new EventFamily("HostPhysicalNicStatusUp", events,
            EventLabelNames.PhysicalNicFromBond,
            EventLabelNames.PhysicalNicName, EventLabelNames.PhysicalNicAddr,
            EventLabelNames.PhysicalNicStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Normal);
    public static final EventFamily HostPhysicalNicStatusDown = new EventFamily("HostPhysicalNicStatusDown", events,
            EventLabelNames.PhysicalNicFromBond,
            EventLabelNames.PhysicalNicName, EventLabelNames.PhysicalNicAddr,
            EventLabelNames.PhysicalNicStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalMemoryEccErrorTriggered = new EventFamily("HostPhysicalMemoryEccErrorTriggered", events, HostNamespace.EventLabelNames.Details).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalCpuStatusAbnormal = new EventFamily("HostPhysicalCpuStatusAbnormal", events, HostNamespace.EventLabelNames.CpuName, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalMemoryStatusAbnormal = new EventFamily("HostPhysicalMemoryStatusAbnormal", events, HostNamespace.EventLabelNames.Locator, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalFanStatusAbnormal = new EventFamily("HostPhysicalFanStatusAbnormal", events, HostNamespace.EventLabelNames.FanName, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalDiskStatusAbnormal = new EventFamily("HostPhysicalDiskStatusAbnormal", events, HostNamespace.EventLabelNames.SerialNumber, HostNamespace.EventLabelNames.EnclosureId, HostNamespace.EventLabelNames.SlotNumber, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalDiskInsertTriggered = new EventFamily("HostPhysicalDiskInsertTriggered", events, HostNamespace.EventLabelNames.SerialNumber, HostNamespace.EventLabelNames.EnclosureId, HostNamespace.EventLabelNames.SlotNumber).setEmergencyLevel(EventFamily.EmergencyLevel.Normal);

    public static final EventFamily HostPhysicalDiskRemoveTriggered = new EventFamily("HostPhysicalDiskRemoveTriggered", events, HostNamespace.EventLabelNames.SerialNumber, HostNamespace.EventLabelNames.EnclosureId, HostNamespace.EventLabelNames.SlotNumber).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalPowerSupplyStatusAbnormal = new EventFamily("HostPhysicalPowerSupplyStatusAbnormal",
            events, HostNamespace.EventLabelNames.Name, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalGpuStatusAbnormal = new EventFamily("HostPhysicalGpuStatusAbnormal", events,
            HostNamespace.EventLabelNames.PciDeviceAddress, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostPhysicalVGpuStatusAbnormal = new EventFamily("HostPhysicalVGpuStatusAbnormal", events,
            HostNamespace.EventLabelNames.Name, HostNamespace.EventLabelNames.NewStatus).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily HostPhysicalGpuRemoveTriggered = new EventFamily("HostPhysicalGpuRemoveTriggered", events,
            HostNamespace.EventLabelNames.PciDeviceAddress).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily HostPhysicalRaidStateAbnormal = new EventFamily("HostPhysicalRaidStateAbnormal", events,
            EventLabelNames.TargetId, HostNamespace.EventLabelNames.NewStatus)
            .setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostHbaPortStateAbnormal = new EventFamily("HostHbaPortStateAbnormal", events,
            EventLabelNames.Name,
            EventLabelNames.HbaPortName,
            EventLabelNames.OldStatus,
            EventLabelNames.NewStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Important);

    public static final EventFamily HostSharedBlockStateAbnormal = new EventFamily("HostSharedBlockStateAbnormal", events,
            PrimaryStorageNamespace.EventLabelNames.HostUuid, PrimaryStorageNamespace.EventLabelNames.Wwid,
            PrimaryStorageNamespace.EventLabelNames.Disk, PrimaryStorageNamespace.EventLabelNames.State).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily HostProcessPhysicalMemoryUsageAbnormal = new EventFamily("HostProcessPhysicalMemoryUsageAbnormal", events,
            EventLabelNames.Name,
            EventLabelNames.Pid,
            EventLabelNames.MemoryUsage
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Important);

    @Override
    public Metric getInfoMetric() {
        return HostInfo;
    }
}
