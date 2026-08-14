package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.VRouterNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VRouterNamespace extends VmAbstractNamespace {
    public static final String NAME = "VRouter";

    private static final List<Metric> vRouterMetrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> vRouterEvents = new ArrayList<>();

    @StaticInit
    static void staticInit() {
        new VRouterNamespaceEvent();
    }

    public enum LabelNames {
        VMUuid,
        CPUNum,
        DiskDeviceLetter,
        NetworkDeviceLetter,
        MountPoint,
        FSType
    }

    public enum EventLabelNames {
        OldState,
        NewState,
        Error,
        OldStatus,
        NewStatus,
        DiskTotal,
        DiskUsed,
        DisKUsage,
        AbnormalFiles,
        ApplianceVmType
    }

    public static final EventFamily VRouterDisconnected = new EventFamily("VRouterDisconnected", vRouterEvents,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily VRouterPaused = new EventFamily("VRouterPaused", vRouterEvents,
            EventLabelNames.OldState, EventLabelNames.NewState, EventLabelNames.ApplianceVmType
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily VRouterConnected = new EventFamily("VRouterConnected", vRouterEvents,
            EventLabelNames.OldStatus, EventLabelNames.NewStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);
    public static final EventFamily VRouterServiceUnhealthy = new EventFamily("VRouterUnhealthy", vRouterEvents,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily VRouterServiceHealthy = new EventFamily("VRouterHealthy", vRouterEvents,
            EventLabelNames.NewStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);
    public static final EventFamily MasterVpcRouterChanged = new EventFamily("MasterVpcRouterChanged", vRouterEvents,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily VRouterAbnormalFilesExists = new EventFamily("VRouterAbnormalFilesExists",
            vRouterEvents, EventLabelNames.DiskTotal, EventLabelNames.DiskUsed, EventLabelNames.DisKUsage,
            EventLabelNames.AbnormalFiles
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public VRouterNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public VRouterNamespace() {
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
        allMetrics.addAll(vRouterMetrics);

        return allMetrics.stream()
                .filter(m -> !disableMetrics.contains(m.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return vRouterEvents;
    }

    @Override
    public String getResourceType() {
        return VirtualRouterVmVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.VMUuid.toString();
    }
}
