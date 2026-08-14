package org.zstack.zwatch.namespace;

import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;

public class CephPrimaryStoragePoolNamespace extends PrimaryStorageNamespace {
    public enum LabelNames {
        PrimaryStorageUuid,
        PoolName,
        PoolUuid
    }

    public static final Metric PoolAvailableCapacityInPercent = new PercentMetric("PoolAvailableCapacityInPercent",
            metrics, true, LabelNames.PrimaryStorageUuid, LabelNames.PoolName, LabelNames.PoolUuid
    );
    public static final Metric PoolUsedCapacityInPercent = new PercentMetric("PoolUsedCapacityInPercent", metrics, true,
            LabelNames.PrimaryStorageUuid, LabelNames.PoolName, LabelNames.PoolUuid
    );
    public static final Metric PoolVirtualAvailableCapacityInPercent = new PercentMetric("PoolVirtualAvailableCapacityInPercent", metrics, true,
            LabelNames.PrimaryStorageUuid, LabelNames.PoolName, LabelNames.PoolUuid
    );
    public static final Metric TimeDurationRequiredForCephPoolForecastUsageExceedingThresholdUsage =
            new CountMetric("TimeDurationRequiredForCephPoolForecastUsageExceedingThresholdUsage",
                    metrics, LabelNames.PrimaryStorageUuid, LabelNames.PoolName, LabelNames.PoolUuid
    );

    static void init() {}
}
