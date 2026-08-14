package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.xdragon.XDragonHostVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author shenjin
 * @date 2022/8/23 13:38
 */
public class XDragonHostNamespace extends HostAbstractNamespace{
    public static final String NAME = "XDragonHost";

    public static final List<Metric> xdragonMetrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> xdragonEvents = new ArrayList<>();

    @StaticInit
    static void staticInit() {
    }

    public enum LabelNames {
        XDragonHostUuid,
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
        SerialNumber
    }

    //XDragon
    public static final Metric XDragonHostTotal = new CountMetric("XDragonHostTotal", xdragonMetrics, LabelNames.XDragonHostUuid);
    public static final Metric XDragonConnectedHostCount = new CountMetric("XDragonConnectedHostCount", xdragonMetrics, LabelNames.XDragonHostUuid);
    public static final Metric XDragonConnectedHostInPercent = new PercentMetric("XDragonConnectedHostInPercent",
            xdragonMetrics, LabelNames.XDragonHostUuid
    );
    public static final Metric XDragonDisconnectedHostCount = new CountMetric("XDragonDisconnectedHostCount", xdragonMetrics, LabelNames.XDragonHostUuid);
    public static final Metric XDragonDisconnectedHostInPercent = new PercentMetric("XDragonDisconnectedHostInPercent",
            xdragonMetrics, LabelNames.XDragonHostUuid
    );

    public XDragonHostNamespace() {
        super();
    }

    public XDragonHostNamespace(DatabaseDriver driver) {
        super(driver);
    }

    @Override
    public List<Metric> getMetrics() {
        List<Metric> allMetrics = new ArrayList<>();
        allMetrics.addAll(xdragonMetrics);
        allMetrics.addAll(metrics);

        return allMetrics.stream()
                .filter(m -> !disableMetrics.contains(m.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return xdragonEvents;
    }

    @Override
    public String getResourceType() {
        return XDragonHostVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.XDragonHostUuid.toString();
    }

    @Override
    public String getBasicIdentityLabelName() {
        return HostAbstractNamespace.LabelNames.HostUuid.toString();
    }

    @Override
    public String getSubNamespaceName() {
        return NAME;
    }

}
