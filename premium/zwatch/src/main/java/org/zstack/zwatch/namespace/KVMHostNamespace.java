package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.kvm.KVMHostVO;
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
 * @date 2022/8/23 13:37
 */
public class KVMHostNamespace extends HostAbstractNamespace{
    public static final String NAME = "KVMHost";

    public static final List<Metric> kvmMetrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> kvmEvents = new ArrayList<>();

    @StaticInit
    static void staticInit() {
    }

    public enum LabelNames {
        KVMHostUuid,
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

    //KVM
    public static final Metric KVMHostTotal = new CountMetric("KVMHostTotal", kvmMetrics, LabelNames.KVMHostUuid);
    public static final Metric KVMConnectedHostCount = new CountMetric("KVMConnectedHostCount", kvmMetrics, LabelNames.KVMHostUuid);
    public static final Metric KVMConnectedHostInPercent = new PercentMetric("KVMConnectedHostInPercent", kvmMetrics, LabelNames.KVMHostUuid);
    public static final Metric KVMDisconnectedHostCount = new CountMetric("KVMDisconnectedHostCount", kvmMetrics, LabelNames.KVMHostUuid);
    public static final Metric KVMDisconnectedHostInPercent = new PercentMetric("KVMDisconnectedHostInPercent",
            kvmMetrics, LabelNames.KVMHostUuid
    );

    public KVMHostNamespace() {
        super();
    }

    public KVMHostNamespace(DatabaseDriver driver) {
        super(driver);
    }

    @Override
    public List<Metric> getMetrics() {
        List<Metric> allMetrics = new ArrayList<>();
        allMetrics.addAll(kvmMetrics);
        allMetrics.addAll(metrics);

        return allMetrics.stream()
                .filter(m -> !disableMetrics.contains(m.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return kvmEvents;
    }

    @Override
    public String getResourceType() {
        return KVMHostVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.KVMHostUuid.toString();
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
