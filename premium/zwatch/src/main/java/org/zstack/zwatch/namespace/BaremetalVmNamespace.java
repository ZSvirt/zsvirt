package org.zstack.zwatch.namespace;

import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaremetalVmNamespace extends AbstractNamespace {
    public static final String NAME = "BaremetalVM";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> events = new ArrayList<>();
    private final AccountFilter filter = new AccountFilter(getName(), LabelNames.BaremetalVMUuid.name(),
            BaremetalInstanceVO.class
    );

    public BaremetalVmNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        BaremetalVMUuid,
        CPUNum,
        DiskDeviceLetter,
        NetworkDeviceLetter,
        MountPoint,
        FSType
    }

    // Data collected from within the vm operating system
    public static final Metric OperatingSystemCPUSystemUtilization = new PercentMetric(
            "OperatingSystemCPUSystemUtilization", metrics, false, LabelNames.BaremetalVMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUUserUtilization = new PercentMetric(
            "OperatingSystemCPUUserUtilization", metrics, false, LabelNames.BaremetalVMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUWaitUtilization = new PercentMetric(
            "OperatingSystemCPUWaitUtilization", metrics, false, LabelNames.BaremetalVMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUIdleUtilization = new PercentMetric(
            "OperatingSystemCPUIdleUtilization", metrics, false, LabelNames.BaremetalVMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUUsedUtilization = new PercentMetric(
            "OperatingSystemCPUUsedUtilization", metrics, false, LabelNames.BaremetalVMUuid, LabelNames.CPUNum);
    public static final Metric OperatingSystemCPUAverageSystemUtilization = new PercentMetric(
            "OperatingSystemCPUAverageSystemUtilization", metrics, false, LabelNames.BaremetalVMUuid);
    public static final Metric OperatingSystemCPUAverageUserUtilization = new PercentMetric(
            "OperatingSystemCPUAverageUserUtilization", metrics, false, LabelNames.BaremetalVMUuid);
    public static final Metric OperatingSystemCPUAverageWaitUtilization = new PercentMetric(
            "OperatingSystemCPUAverageWaitUtilization", metrics, false, LabelNames.BaremetalVMUuid);
    public static final Metric OperatingSystemCPUAverageIdleUtilization = new PercentMetric(
            "OperatingSystemCPUAverageIdleUtilization", metrics, false, LabelNames.BaremetalVMUuid);
    public static final Metric OperatingSystemCPUAverageUsedUtilization = new PercentMetric(
            "OperatingSystemCPUAverageUsedUtilization", metrics, false, LabelNames.BaremetalVMUuid);

    // Data collected from within the vm operating system
    public static final Metric DiskAllFreeCapacityInBytes = new ByteSizeMetric("DiskAllFreeCapacityInBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric DiskAllFreeCapacityInPercent = new PercentMetric("DiskAllFreeCapacityInPercent",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric DiskAllUsedCapacityInBytes = new ByteSizeMetric("DiskAllUsedCapacityInBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric DiskAllUsedCapacityInPercent = new PercentMetric("DiskAllUsedCapacityInPercent",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric DiskFreeCapacityInBytes = new PercentMetric("DiskFreeCapacityInBytes",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskFreeCapacityInPercent = new PercentMetric("DiskFreeCapacityInPercent", metrics,
            false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, HostNamespace.LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskUsedCapacityInBytes = new ByteSizeMetric("DiskUsedCapacityInBytes",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskUsedCapacityInPercent = new ByteSizeMetric("DiskUsedCapacityInPercent", metrics,
            false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskTotalCapacityInBytes = new ByteSizeMetric("DiskTotalCapacityInBytes",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskReadBytesPerSecond = new ByteRateMetric("DiskReadBytesPerSecond",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskReadRequestPerSecond = new OperationRateMetric("DiskReadRequestPerSecond", metrics,
            false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );
    public static final Metric DiskWriteBytesPerSecond = new ByteRateMetric("DiskWriteBytesPerSecond",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint,
            LabelNames.FSType
    );
    public static final Metric DiskWriteRequestPerSecond = new OperationRateMetric("DiskWriteRequestPerSecond", metrics,
            false, LabelNames.BaremetalVMUuid, LabelNames.DiskDeviceLetter, LabelNames.MountPoint, LabelNames.FSType
    );

    public static final Metric OperatingSystemNetworkInBytes = new ByteRateMetric("OperatingSystemNetworkInBytes",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric OperatingSystemNetworkAllInBytes = new ByteRateMetric("OperatingSystemNetworkAllInBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemNetworkInPackets = new PacketRateMetric("OperatingSystemNetworkInPackets",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric OperatingSystemNetworkAllInPackets = new PacketRateMetric(
            "OperatingSystemNetworkAllInPackets", metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemNetworkInErrors = new PacketRateMetric("OperatingSystemNetworkInErrors",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric OperatingSystemNetworkAllInErrors = new PacketRateMetric(
            "OperatingSystemNetworkAllInErrors",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemNetworkOutBytes = new ByteRateMetric("OperatingSystemNetworkOutBytes",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric OperatingSystemNetworkAllOutBytes = new ByteRateMetric(
            "OperatingSystemNetworkAllOutBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemNetworkOutPackets = new PacketRateMetric(
            "OperatingSystemNetworkOutPackets",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric OperatingSystemNetworkAllOutPackets = new PacketRateMetric(
            "OperatingSystemNetworkAllOutPackets",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemNetworkOutErrors = new PacketRateMetric("OperatingSystemNetworkOutErrors",
            metrics, false, LabelNames.BaremetalVMUuid, LabelNames.NetworkDeviceLetter
    );
    public static final Metric OperatingSystemNetworkAllOutErrors = new PacketRateMetric(
            "OperatingSystemNetworkAllOutErrors",
            metrics, false, LabelNames.BaremetalVMUuid
    );

    // Data collected from within the vm operating system
    public static final Metric OperatingSystemMemoryTotalBytes = new ByteSizeMetric("OperatingSystemMemoryTotalBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemMemoryFreeBytes = new ByteSizeMetric("OperatingSystemMemoryFreeBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemMemoryUsedBytes = new ByteSizeMetric("OperatingSystemMemoryUsedBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemMemoryAvailableBytes = new ByteSizeMetric(
            "OperatingSystemMemoryAvailableBytes",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemMemoryFreePercent = new PercentMetric("OperatingSystemMemoryFreePercent",
            metrics, false, LabelNames.BaremetalVMUuid
    );
    public static final Metric OperatingSystemMemoryUsedPercent = new PercentMetric("OperatingSystemMemoryUsedPercent",
            metrics, false, LabelNames.BaremetalVMUuid
    );

    @StaticInit
    static void staticInit() {
    }

    public BaremetalVmNamespace() {
        super();
    }

    @Override
    public String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m -> !disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return BaremetalInstanceVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.BaremetalVMUuid.toString();
    }
}
