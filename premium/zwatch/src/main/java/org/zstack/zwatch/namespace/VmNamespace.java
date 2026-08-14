package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.VmNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VmNamespace extends VmAbstractNamespace {
    public static final String NAME = "VM";

    private static final List<Metric> vmInstanceMetrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private final AccountFilter filter = new AccountFilter(getName(), LabelNames.VMUuid.name(), VmInstanceVO.class);

    public VmNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        VMUuid,
        CPUNum,
        DiskDeviceLetter,
        NetworkDeviceLetter,
        MountPoint,
        FSType,
        PciDeviceAddress,
        SerialNumber,
        GpuStatus,
    }

    public enum EventLabelNames {
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
    }

    public static final Metric TotalNetworkInBytesIn5Min = new ByteRateMetric("TotalNetworkInBytesIn5Min", metrics, false, VmAbstractNamespace.LabelNames.VMUuid,
            VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkInPacketsIn5Min = new PacketRateMetric("TotalNetworkInPacketsIn5Min", metrics, false,
            VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkOutBytesIn5Min = new ByteRateMetric("TotalNetworkOutBytesIn5Min", metrics, false,
            VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkOutPacketsIn5Min = new PacketRateMetric("TotalNetworkOutPacketsIn5Min", metrics, false,
            VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkInBytesIn1Min = new ByteRateMetric("TotalNetworkInBytesIn1Min", metrics, false, VmAbstractNamespace.LabelNames.VMUuid,
            VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkInPacketsIn1Min = new PacketRateMetric("TotalNetworkInPacketsIn1Min", metrics, false,
            VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkOutBytesIn1Min = new ByteRateMetric("TotalNetworkOutBytesIn1Min", metrics, false,
            VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );
    public static final Metric TotalNetworkOutPacketsIn1Min = new PacketRateMetric("TotalNetworkOutPacketsIn1Min", metrics, false,
            VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.NetworkDeviceLetter
    );

    public static final Metric GpuPowerDraw = new CountMetric("GpuPowerDraw", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);
    public static final Metric GpuTemperature = new TemperatureMetric("GpuTemperature", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);
    public static final Metric GpuFanSpeed = new PercentMetric("GpuFanSpeed", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);
    public static final Metric GpuUtilization = new PercentMetric("GpuUtilization", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);

    public static final Metric GpuStatus = new PercentMetric("GpuStatus", metrics, LabelNames.PciDeviceAddress,
            LabelNames.GpuStatus, LabelNames.SerialNumber, LabelNames.VMUuid);
    public static final Metric GpuMemoryUtilization = new PercentMetric("GpuMemoryUtilization", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);
    public static final Metric GpuPciRxThroughputInBytes = new ByteSizeMetric("GpuPciRxThroughputInBytes", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);
    public static final Metric GpuPciTxThroughputInBytes = new ByteSizeMetric("GpuPciTxThroughputInBytes", metrics,
            LabelNames.PciDeviceAddress, LabelNames.SerialNumber, LabelNames.VMUuid);

    public static final Metric VGpuMemoryUtilization = new PercentMetric("VGpuMemoryUtilization", metrics,
            VmAbstractNamespace.LabelNames.MdevDeviceUuid, VmAbstractNamespace.LabelNames.VMUuid, VmAbstractNamespace.LabelNames.HostUuid);

    public static final Metric VGpuUtilization = new PercentMetric("VGpuUtilization", metrics, VmAbstractNamespace.LabelNames.HostUuid,
            VmAbstractNamespace.LabelNames.MdevDeviceUuid, VmAbstractNamespace.LabelNames.VMUuid);

    @StaticInit
    static void staticInit() {
        new VmNamespaceEvent();
    }

    public VmNamespace() {
        super();
    }

    @Override
    public String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        List<Metric> allMetrics = new ArrayList<>();
        allMetrics.addAll(metrics);
        allMetrics.addAll(vmInstanceMetrics);

        return metrics.stream().filter(m -> !disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.VMUuid.toString();
    }
}
