package org.zstack.zwatch.namespace;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;

public class VmCountNamespace extends AbstractNamespace {
    public static final String NAME = "VM";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

    public VmCountNamespace() {
        super();
    }

    public VmCountNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public static final Metric TotalVMCount = new CountMetric("TotalVMCount", metrics, false);
    public static final Metric RunningVMCount = new CountMetric("RunningVMCount", metrics, false);
    public static final Metric RunningVMInPercent = new PercentMetric("RunningVMInPercent", metrics, false);
    public static final Metric StoppedVMCount = new CountMetric("StoppedVMCount", metrics, false);
    public static final Metric StoppedVMInPercent = new PercentMetric("StoppedVMInPercent", metrics, false);
    public static final Metric OtherStateVMCount = new CountMetric("OtherStateVMCount", metrics, false);
    public static final Metric OtherStateVMInPercent = new PercentMetric("OtherStateVMInPercent", metrics, false);

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public List<EventFamily> getEvents() {
        return null;
    }

    @Override
    public String getResourceType() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }
}
