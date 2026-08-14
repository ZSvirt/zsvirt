package org.zstack.zwatch.namespace;

import org.zstack.header.volume.VolumeVO;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VolumeNamespace extends AbstractNamespace {
    public static final String NAME = "Volume";
    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> events = new ArrayList<>();

    public enum LabelNames {
        VolumeUuid
    }

    public static final Metric TotalVolumeCount = new CountMetric("TotalVolumeCount", metrics);
    public static final Metric RootVolumeCount = new CountMetric("RootVolumeCount", metrics);
    public static final Metric RootVolumeInPercent = new PercentMetric("RootVolumeInPercent", metrics);
    public static final Metric DataVolumeCount = new CountMetric("DataVolumeCount", metrics);
    public static final Metric DataVolumeInPercent = new PercentMetric("DataVolumeInPercent", metrics);
    public static final Metric ReadyDataVolumeCount = new CountMetric("ReadyDataVolumeCount", metrics);
    public static final Metric ReadyDataVolumeInPercent = new PercentMetric("ReadyDataVolumeInPercent", metrics);
    public static final Metric TotalVolumeSnapshotCount = new CountMetric("TotalVolumeSnapshotCount", metrics);
    public static final Metric RootVolumeSnapshotCount = new CountMetric("RootVolumeSnapshotCount", metrics);
    public static final Metric RootVolumeSnapshotInPercent = new PercentMetric("RootVolumeSnapshotInPercent", metrics);
    public static final Metric DataVolumeSnapshotCount = new CountMetric("DataVolumeSnapshotCount", metrics);
    public static final Metric DataVolumeSnapshotInPercent = new PercentMetric("DataVolumeSnapshotInPercent", metrics);
    public static final Metric VolumeActualSizeInPercent = new PercentMetric("VolumeActualSizeInPercent", metrics,
            LabelNames.VolumeUuid
    );
    public static final Metric VolumeXfsFragCount = new CountMetric("VolumeXfsFragCount", metrics,
            LabelNames.VolumeUuid
    );

    public VolumeNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public VolumeNamespace() {
        super();
    }

    @Override
    protected String getSubNamespaceName() {
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
        return VolumeVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.VolumeUuid.toString();
    }
}
