package org.zstack.zwatch.namespace;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.core.StaticInit;
import org.zstack.header.host.HostVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.HostNetworkMetricFilter;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.HostNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HostNamespace extends HostAbstractNamespace implements Component{
    public static final String NAME = "Host";

    @Autowired
    protected PluginRegistry pluginRgty;

    @StaticInit
    static void staticInit() {
        new HostNamespaceEvent();
    }

    private static final List<Metric> hostMetrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

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
    }

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
        CpuName,
        Locator,
        FanName,
        SerialNumber,
        EnclosureId,
        SlotNumber,
        Name,
        PciDeviceAddress,
    }

    private void initMetricFilter() {
        for (HostNetworkMetricFilter ext : pluginRgty.getExtensionList(HostNetworkMetricFilter.class)) {
            NetworkInBytes.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllInBytes.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllInBytesByServiceType.addLabelFilter(ext.getFilterName(), ext);
            NetworkInPackets.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllInPackets.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllInPacketsByServiceType.addLabelFilter(ext.getFilterName(), ext);
            NetworkInErrors.addLabelFilter(ext.getFilterName(), ext);
            NetworkInDropped.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllInErrors.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllInErrorsByServiceType.addLabelFilter(ext.getFilterName(), ext);
            NetworkOutBytes.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllOutBytes.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllOutBytesByServiceType.addLabelFilter(ext.getFilterName(), ext);
            NetworkOutPackets.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllOutPackets.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllOutPacketsByServiceType.addLabelFilter(ext.getFilterName(), ext);
            NetworkOutErrors.addLabelFilter(ext.getFilterName(), ext);
            NetworkOutDropped.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllOutErrors.addLabelFilter(ext.getFilterName(), ext);
            NetworkAllOutErrorsByServiceType.addLabelFilter(ext.getFilterName(), ext);
        }
    }

    public HostNamespace() {
        super();
    }

    public HostNamespace(DatabaseDriver driver) {
        super(driver);
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        List<Metric> allMetrics = new ArrayList<>();
        allMetrics.addAll(metrics);
        allMetrics.addAll(hostMetrics);
        return metrics.stream().filter(m -> !disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return HostVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.HostUuid.toString();
    }

    @Override
    public boolean start() {
        initMetricFilter();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
