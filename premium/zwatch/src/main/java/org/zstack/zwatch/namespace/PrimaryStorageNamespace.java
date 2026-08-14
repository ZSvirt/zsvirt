package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.ByteSizeMetric;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.metric.InfoMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.PrimaryStorageNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PrimaryStorageNamespace extends AbstractNamespace {
    public static final String NAME = "PrimaryStorage";

    @StaticInit
    static void staticInit() {
        new PrimaryStorageNamespaceEvent();
        CephPrimaryStoragePoolNamespace.init();
        LocalPrimaryStorageHostNamespace.init();
    }

    protected static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    protected static final List<EventFamily> events = new ArrayList<>();

    public enum LabelNames {
        PrimaryStorageUuid,
        PrimaryStorageType,
        Name,
    }

    public static final Metric PrimaryStorageInfo = new InfoMetric("PrimaryStorageInfo", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.Name
    );

    public static final Metric TotalCapacityInBytes = new ByteSizeMetric("TotalCapacityInBytes", metrics);
    public static final Metric TotalAvailableCapacityInBytes = new ByteSizeMetric("TotalAvailableCapacityInBytes", metrics);
    public static final Metric TotalAvailableCapacityInPercent = new PercentMetric("TotalAvailableCapacityInPercent", metrics);
    public static final Metric TotalUsedCapacityInBytes = new ByteSizeMetric("TotalUsedCapacityInBytes", metrics);
    public static final Metric TotalUsedCapacityInPercent = new PercentMetric("TotalUsedCapacityInPercent", metrics);
    public static final Metric TotalLockedCapacityInBytes = new ByteSizeMetric("TotalLockedCapacityInBytes", metrics);
    public static final Metric TotalLockedCapacityInPercent = new PercentMetric("TotalLockedCapacityInPercent", metrics);

    public static final Metric TotalPhysicalCapacityInBytes = new ByteSizeMetric("TotalPhysicalCapacityInBytes",
            metrics, LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric AvailableCapacityInBytes = new ByteSizeMetric("AvailableCapacityInBytes", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric AvailableCapacityInPercent = new PercentMetric("AvailableCapacityInPercent", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric UsedCapacityInBytes = new ByteSizeMetric("UsedCapacityInBytes", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric UsedCapacityInPercent = new PercentMetric("UsedCapacityInPercent", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric AvailablePhysicalCapacityInBytes = new ByteSizeMetric("AvailablePhysicalCapacityInBytes",
            metrics, LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric AvailablePhysicalCapacityInPercent = new PercentMetric(
            "AvailablePhysicalCapacityInPercent", metrics, LabelNames.PrimaryStorageUuid,
            LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric UsedPhysicalCapacityInBytes = new ByteSizeMetric("UsedPhysicalCapacityInBytes", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric UsedPhysicalCapacityInPercent = new PercentMetric("UsedPhysicalCapacityInPercent",
            metrics, LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric RootVolumeCount = new CountMetric("RootVolumeCount", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric DataVolumeCount = new CountMetric("DataVolumeCount", metrics,
            LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric SnapshotCount = new CountMetric("SnapshotCount", metrics, LabelNames.PrimaryStorageUuid,
            LabelNames.PrimaryStorageType, LabelNames.Name
    );
    public static final Metric TimeDurationRequiredForPrimaryStorageForecastUsageExceedingThresholdUsage =
            new CountMetric("TimeDurationRequiredForPrimaryStorageForecastUsageExceedingThresholdUsage",
                    metrics, LabelNames.PrimaryStorageUuid, LabelNames.PrimaryStorageType, LabelNames.Name
    );

    public enum EventLabelNames {
        Error,
        HostUuid,
        OldStatus,
        NewStatus,
        Wwid,
        Disk,
        State,
    }

    public static final EventFamily SharedBlockStateAbnormal = new EventFamily("SharedBlockStateAbnormal", events, EventLabelNames.HostUuid, EventLabelNames.Wwid, EventLabelNames.Disk, EventLabelNames.State).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily PrimaryStorageDisconnected = new EventFamily("PrimaryStorageDisconnected", events,
            EventLabelNames.Error
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);
    public static final EventFamily PrimaryStorageConnected = new EventFamily("PrimaryStorageConnected", events,
            EventLabelNames.OldStatus, EventLabelNames.NewStatus
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);

    public static final EventFamily PrimaryStorageHostDisconnected = new EventFamily("PrimaryStorageHostDisconnected",
            events, EventLabelNames.Error, EventLabelNames.HostUuid
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public PrimaryStorageNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public PrimaryStorageNamespace() {
        super();
    }

    @Override
    public Metric getInfoMetric() {
        return PrimaryStorageInfo;
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m ->!disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return PrimaryStorageVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.PrimaryStorageUuid.toString();
    }
}
