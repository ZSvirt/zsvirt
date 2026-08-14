package org.zstack.zwatch.namespace;

import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;

public abstract class VmAbstractNamespace extends AbstractNamespace {
    protected static final List<Metric> metrics = new ArrayList<>();
    protected static final List<EventFamily> events = new ArrayList<>();

    protected VmAbstractNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        VMUuid,
        CPUNum,
        DiskDeviceLetter,
        NetworkDeviceLetter,
        MountPoint,
        FSType,
        MdevDeviceUuid,
        HostUuid,
        Name,
        DefaultIpv4,
    }

    public static final Metric VmInfo = new InfoMetric("VmInfo", metrics,
            LabelNames.VMUuid, LabelNames.Name, LabelNames.DefaultIpv4
    );

    public static final Metric CPUUsedUtilization = new PercentMetric("CPUUsedUtilization", metrics, false,
            LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric CPUAverageUsedUtilization = new PercentMetric("CPUAverageUsedUtilization", metrics,
            false, LabelNames.VMUuid
    );
    public static final Metric CPUIdleUtilization = new PercentMetric("CPUIdleUtilization", metrics, false,
            LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric CPUAllUsedUtilization = new PercentMetric("CPUAllUsedUtilization", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric CPUAllIdleUtilization = new PercentMetric("CPUAllIdleUtilization", metrics, false,
            LabelNames.VMUuid
    );

    // Data collected from within the vm operating system
    public static final Metric OperatingSystemCPUSystemUtilization = new PercentMetric(
            "OperatingSystemCPUSystemUtilization", metrics, false, LabelNames.VMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUUserUtilization = new PercentMetric(
            "OperatingSystemCPUUserUtilization", metrics, false, LabelNames.VMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUWaitUtilization = new PercentMetric(
            "OperatingSystemCPUWaitUtilization", metrics, false, LabelNames.VMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUIdleUtilization = new PercentMetric(
            "OperatingSystemCPUIdleUtilization", metrics, false, LabelNames.VMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUUsedUtilization = new PercentMetric(
            "OperatingSystemCPUUsedUtilization", metrics, false, LabelNames.VMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUAverageSystemUtilization = new PercentMetric(
            "OperatingSystemCPUAverageSystemUtilization", metrics, false, LabelNames.VMUuid);
    public static final Metric OperatingSystemCPUAverageUserUtilization = new PercentMetric(
            "OperatingSystemCPUAverageUserUtilization", metrics, false, LabelNames.VMUuid);
    public static final Metric OperatingSystemCPUAverageWaitUtilization = new PercentMetric(
            "OperatingSystemCPUAverageWaitUtilization", metrics, false, LabelNames.VMUuid);
    public static final Metric OperatingSystemCPUAverageIdleUtilization = new PercentMetric(
            "OperatingSystemCPUAverageIdleUtilization", metrics, false, LabelNames.VMUuid);
    public static final Metric OperatingSystemCPUAverageUsedUtilization = new PercentMetric(
            "OperatingSystemCPUAverageUsedUtilization", metrics, false, LabelNames.VMUuid);

    public static final Metric DiskReadOps = new OperationRateMetric("DiskReadOps", metrics, false, LabelNames.VMUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllReadOps = new OperationRateMetric("DiskAllReadOps", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric DiskWriteOps = new OperationRateMetric("DiskWriteOps", metrics, false, LabelNames.VMUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllWriteOps = new OperationRateMetric("DiskAllWriteOps", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric DiskReadBytes = new ByteRateMetric("DiskReadBytes", metrics, false, LabelNames.VMUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllReadBytes = new ByteRateMetric("DiskAllReadBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric DiskWriteBytes = new ByteRateMetric("DiskWriteBytes", metrics, false, LabelNames.VMUuid,
            LabelNames.DiskDeviceLetter
    );
    public static final Metric DiskAllWriteBytes = new ByteRateMetric("DiskAllWriteBytes", metrics, false,
            LabelNames.VMUuid
    );

    // Data collected from within the vm operating system
    public static final Metric DiskAllFreeCapacityInBytes = new ByteSizeMetric("DiskAllFreeCapacityInBytes", metrics,
            false, LabelNames.VMUuid
    );
    public static final Metric DiskAllFreeCapacityInPercent = new PercentMetric("DiskAllFreeCapacityInPercent", metrics,
            false, LabelNames.VMUuid
    );
    public static final Metric DiskAllUsedCapacityInBytes = new ByteSizeMetric("DiskAllUsedCapacityInBytes", metrics,
            false, LabelNames.VMUuid
    );
    public static final Metric DiskAllUsedCapacityInPercent = new PercentMetric("DiskAllUsedCapacityInPercent", metrics,
            false, LabelNames.VMUuid
    );
    public static final Metric DiskFreeCapacityInBytes = new ByteSizeMetric("DiskFreeCapacityInBytes", metrics, false,
            LabelNames.VMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskFreeCapacityInPercent = new PercentMetric("DiskFreeCapacityInPercent", metrics,
            false, LabelNames.VMUuid, LabelNames.DiskDeviceLetter, HostNamespace.LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskUsedCapacityInBytes = new ByteSizeMetric("DiskUsedCapacityInBytes", metrics, false,
            LabelNames.VMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskUsedCapacityInPercent = new PercentMetric("DiskUsedCapacityInPercent", metrics,
            false, LabelNames.VMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );

    public static final Metric NetworkInBytes = new ByteRateMetric("NetworkInBytes", metrics, false, LabelNames.VMUuid,
            LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInBytes = new ByteRateMetric("NetworkAllInBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkInPackets = new PacketRateMetric("NetworkInPackets", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInPackets = new PacketRateMetric("NetworkAllInPackets", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkInErrors = new PacketRateMetric("NetworkInErrors", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInErrors = new PacketRateMetric("NetworkAllInErrors", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkInDroppedBytes = new PacketRateMetric("NetworkInDroppedBytes", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllInDroppedBytes = new PacketRateMetric("NetworkAllInDroppedBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkOutBytes = new ByteRateMetric("NetworkOutBytes", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutBytes = new ByteRateMetric("NetworkAllOutBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkOutPackets = new PacketRateMetric("NetworkOutPackets", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutPackets = new PacketRateMetric("NetworkAllOutPackets", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkOutErrors = new PacketRateMetric("NetworkOutErrors", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutErrors = new PacketRateMetric("NetworkAllOutErrors", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric NetworkOutDroppedBytes = new PacketRateMetric("NetworkOutDroppedBytes", metrics, false,
            LabelNames.VMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric NetworkAllOutDroppedBytes = new PacketRateMetric("NetworkAllOutDroppedBytes", metrics, false,
            LabelNames.VMUuid
    );

    public static final Metric MemoryFreeBytes = new ByteSizeMetric("MemoryFreeBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric MemoryFreeInPercent = new PercentMetric("MemoryFreeInPercent", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric MemoryUsedBytes = new ByteSizeMetric("MemoryUsedBytes", metrics, LabelNames.VMUuid);
    public static final Metric MemoryUsedInPercent = new PercentMetric("MemoryUsedInPercent", metrics, false,
            LabelNames.VMUuid
    );

    public static final Metric MemoryMinorPageFaults = new PercentMetric("MemoryMinorPageFaults", metrics, false,
            LabelNames.VMUuid
    );

    // Data collected from within the vm operating system
    public static final Metric OperatingSystemMemoryTotalBytes = new ByteSizeMetric("OperatingSystemMemoryTotalBytes",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric OperatingSystemMemoryFreeBytes = new ByteSizeMetric("OperatingSystemMemoryFreeBytes",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric OperatingSystemMemoryUsedBytes = new ByteSizeMetric("OperatingSystemMemoryUsedBytes",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric OperatingSystemMemoryAvailableBytes = new ByteSizeMetric(
            "OperatingSystemMemoryAvailableBytes", metrics, false, LabelNames.VMUuid);
    public static final Metric OperatingSystemMemoryFreePercent = new PercentMetric("OperatingSystemMemoryFreePercent",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric OperatingSystemMemoryUsedPercent = new PercentMetric("OperatingSystemMemoryUsedPercent",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric ZWatchAgentVersion = new StateMetric("ZWatchAgentVersion", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric AgentLastResponseTimestampDelta = new StateMetric("AgentLastResponseTimestampDelta", metrics, false,
            LabelNames.VMUuid
    );
    //pv panic enabled state
    public static final Metric ZWatchAgentFeaturePvpanic = new StateMetric("ZWatchAgentFeaturePvpanic", metrics,
            false, LabelNames.VMUuid
    );

    // Data collected from within the vrouter
    public static final Metric VRouterCPUSystemUtilization = new PercentMetric("VRouterCPUSystemUtilization", metrics,
            false, LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric VRouterCPUUserUtilization = new PercentMetric("VRouterCPUUserUtilization", metrics,
            false, LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric VRouterCPUWaitUtilization = new PercentMetric("VRouterCPUWaitUtilization", metrics,
            false, LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric VRouterCPUIdleUtilization = new PercentMetric("VRouterCPUIdleUtilization", metrics,
            false, LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric VRouterCPUUsedUtilization = new PercentMetric("VRouterCPUUsedUtilization", metrics,
            false, LabelNames.VMUuid, LabelNames.CPUNum
    );
    public static final Metric VRouterCPUAverageSystemUtilization = new PercentMetric(
            "VRouterCPUAverageSystemUtilization", metrics, false, LabelNames.VMUuid);
    public static final Metric VRouterCPUAverageUserUtilization = new PercentMetric("VRouterCPUAverageUserUtilization",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric VRouterCPUAverageWaitUtilization = new PercentMetric("VRouterCPUAverageWaitUtilization",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric VRouterCPUAverageIdleUtilization = new PercentMetric("VRouterCPUAverageIdleUtilization",
            metrics, false, LabelNames.VMUuid
    );
    public static final Metric VRouterCPUAverageUsedUtilization = new PercentMetric("VRouterCPUAverageUsedUtilization",
            metrics, false, LabelNames.VMUuid
    );

    public static final Metric VRouterDiskAllFreeCapacityInBytes = new ByteSizeMetric(
            "VRouterDiskAllFreeCapacityInBytes", metrics, false, LabelNames.VMUuid);
    public static final Metric VRouterDiskAllFreeCapacityInPercent = new PercentMetric(
            "VRouterDiskAllFreeCapacityInPercent", metrics, false, LabelNames.VMUuid);
    public static final Metric VRouterDiskAllUsedCapacityInBytes = new ByteSizeMetric(
            "VRouterDiskAllUsedCapacityInBytes", metrics, false, LabelNames.VMUuid);
    public static final Metric VRouterDiskAllUsedCapacityInPercent = new PercentMetric(
            "VRouterDiskAllUsedCapacityInPercent", metrics, false, LabelNames.VMUuid);
    public static final Metric VRouterDiskFreeCapacityInBytes = new ByteSizeMetric("VRouterDiskFreeCapacityInBytes",
            metrics, false, LabelNames.VMUuid, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric VRouterDiskFreeCapacityInPercent = new PercentMetric("VRouterDiskFreeCapacityInPercent",
            metrics, false, LabelNames.VMUuid, HostNamespace.LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric VRouterDiskUsedCapacityInBytes = new ByteSizeMetric("VRouterDiskUsedCapacityInBytes",
            metrics, false, LabelNames.VMUuid, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric VRouterDiskUsedCapacityInPercent = new PercentMetric("VRouterDiskUsedCapacityInPercent",
            metrics, false, LabelNames.VMUuid, LabelNames.MountPoint, LabelNames.FSType
    );

    public static final Metric VRouterMemoryTotalBytes = new ByteSizeMetric("VRouterMemoryTotalBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric VRouterMemoryFreeBytes = new ByteSizeMetric("VRouterMemoryFreeBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric VRouterMemoryUsedBytes = new ByteSizeMetric("VRouterMemoryUsedBytes", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric VRouterMemoryAvailableBytes = new ByteSizeMetric("VRouterMemoryAvailableBytes", metrics,
            false, LabelNames.VMUuid
    );
    public static final Metric VRouterMemoryFreePercent = new PercentMetric("VRouterMemoryFreePercent", metrics, false,
            LabelNames.VMUuid
    );
    public static final Metric VRouterMemoryUsedPercent = new PercentMetric("VRouterMemoryUsedPercent", metrics, false,
            LabelNames.VMUuid
    );

    // Collecting data using the ps command, ps --no-headers -o '%cpu' -p vm_pid
    // Samples:
    //      cpu_occupied_by_vm{vmUuid="xxx", hostUuid="xxx"} 200.0
    //      cpu_occupied_by_vm{vmUuid="xxx", hostUuid="xxx"} 80.0
    public static final Metric CPUOccupiedByVM = new CountMetric("CPUOccupiedByVm", metrics, false, LabelNames.VMUuid);
    public static final Metric MemoryOccupiedByVM = new ByteSizeMetric("MemoryOccupiedByVm", metrics, false,
            LabelNames.VMUuid
    );

    // pvpanic_enable_in_domain_xml: Whether the pvpanic attribute of the VM enabled in the domain XML
    // if pvpanic enable, collect 1.0;  if not, collect 0.0
    public static final Metric PVPanicEnableInDomainXML = new CountMetric("PVPanicEnableInDomainXML",
            metrics, false, LabelNames.VMUuid);

    public static final Metric ReclaimedMemoryInBytes = new PercentMetric("ReclaimedMemoryInBytes", metrics,
            LabelNames.VMUuid
    );

    public enum EventLabelNames2 {
        SourceHostUuid,
        DestinationHostUuid,
        OldState,
        NewState,
        HAProcess,
        HADetails,
        Error,
        FTDetails,
        ResourceUuid,
        VMSchedulerName,
        VmAbnormalLifeCycleOperation,
    }

    public static final EventFamily VMHAProcess = new EventFamily("VMHAProcess", events, EventLabelNames2.HAProcess,
            EventLabelNames2.HADetails
    );
    public static final EventFamily VMStateChanged = new EventFamily("VMStateChanged", events,
            EventLabelNames2.OldState, EventLabelNames2.NewState
    );
    public static final EventFamily VMHAStarted = new EventFamily("VMHAStarted", events,
            EventLabelNames2.DestinationHostUuid
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily VMStateChangedOnHost = new EventFamily("VMStateChangedOnHost", events,
            EventLabelNames2.OldState, EventLabelNames2.NewState, EventLabelNames2.SourceHostUuid,
            EventLabelNames2.DestinationHostUuid
    );
    public static final EventFamily VMStateInShutdown = new EventFamily("VMStateInShutdown", events,
            EventLabelNames2.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily VmCrash = new EventFamily("VmCrash", events,
            EventLabelNames2.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily VmAbnormalLifeCycleDetected = new EventFamily("VmAbnormalLifeCycleDetected",
            events,
            EventLabelNames2.OldState,
            EventLabelNames2.NewState,
            EventLabelNames2.SourceHostUuid,
            EventLabelNames2.DestinationHostUuid,
            EventLabelNames2.VmAbnormalLifeCycleOperation
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public enum EventLabelNames3 {
        VMUuid,
        L3NetworkUuid,
        oldInternalIp,
        newInternalIp,
        relateResourceUuid,
        internalIp,
    }
    public static final EventFamily VMInternalIpChanged = new EventFamily("VMInternalIpChanged", events,
            EventLabelNames3.L3NetworkUuid,
            EventLabelNames3.oldInternalIp,
            EventLabelNames3.newInternalIp,
            EventLabelNames3.relateResourceUuid
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Normal);

    public static final EventFamily VMInternalIpDuplicate = new EventFamily("VMInternalIpDuplicate", events,
            EventLabelNames3.L3NetworkUuid,
            EventLabelNames3.internalIp,
            EventLabelNames3.relateResourceUuid
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Normal);

    public static final EventFamily VMInternalIpRangeConflict = new EventFamily("VMInternalIpRangeConflict", events,
            EventLabelNames3.L3NetworkUuid,
            EventLabelNames3.internalIp
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Normal);

    public VmAbstractNamespace() {
        super();
    }

    @Override
    public Metric getInfoMetric() {
        return VmInfo;
    }
}
